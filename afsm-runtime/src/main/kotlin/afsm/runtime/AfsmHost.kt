package afsm.runtime

import afsm.core.AfsmDecision
import afsm.core.AfsmCommandInvocation
import afsm.core.AfsmInvocationKey
import afsm.core.AfsmReducer
import afsm.core.AfsmTransition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.cancellation.CancellationException

/**
 * Runs an [AfsmReducer] with serialized event processing and host-executed
 * commands.
 *
 * Failure handling is policy driven through [AfsmConfig]. With the recording
 * defaults the host stays usable after invalid transitions, reducer failures,
 * command failures, and queue overflow: every one of them is reported through
 * [AfsmConfig.logger] instead of stopping event processing. Use
 * [AfsmConfig.strict] in debug builds to fail fast instead.
 *
 * When a policy is configured to throw, the failure stops the processing
 * coroutine and the host reports [isActive] as `false`. A final
 * [AfsmDiagnosticCode.HostStopped] diagnostic is recorded so a stopped host is
 * never silent.
 */
public class AfsmHost<S : Any, E : Any, C : Any>(
    initialState: S,
    private val reducer: AfsmReducer<S, E, C>,
    private val commandHandler: AfsmCommandHandler<C, E>,
    scope: CoroutineScope,
    private val config: AfsmConfig = AfsmConfig(),
) {
    private val eventQueue = Channel<E>(config.eventQueueCapacity)
    private val commandQueue = Channel<PendingCommand<S, E, C>>(config.commandQueueCapacity)
    private val _state = MutableStateFlow(initialState)

    /**
     * Supervises every host coroutine.
     *
     * Cancelling the owning [scope] still cancels the host, but a failing
     * command, invocation, or processor no longer cancels its siblings.
     *
     * The parent job is read as a nullable context element rather than through
     * `CoroutineContext.job`, because a scope is not required to carry one.
     * `GlobalScope` and hand-written [CoroutineScope] implementations have no
     * job, and hosting one of those must not fail at construction.
     */
    private val hostJob = SupervisorJob(parent = scope.coroutineContext[Job])
    private val hostScope = CoroutineScope(scope.coroutineContext + hostJob)

    private val processor: Job = hostScope.launch(start = CoroutineStart.UNDISPATCHED) {
        processEvents()
    }
    private val commandProcessor: Job = hostScope.launch(start = CoroutineStart.UNDISPATCHED) {
        processCommands()
    }
    private val invocationScope = CoroutineScope(
        hostScope.coroutineContext + SupervisorJob(parent = hostJob),
    )
    private val activeInvocations = mutableMapOf<AfsmInvocationKey, Job>()

    public val state: StateFlow<S> = _state.asStateFlow()

    /**
     * `false` once the host stopped accepting events, either because it was
     * closed, because the owning scope was cancelled, or because a `Throw`
     * policy stopped a processing coroutine.
     */
    public val isActive: Boolean
        get() = processor.isActive && commandProcessor.isActive

    init {
        // Queues are always closed without a cause. A failure is reported once,
        // by the processor that failed; propagating it into the sibling
        // processor would only report the same failure twice.
        processor.invokeOnCompletion { cause ->
            reportStopped(cause, "event processor")
            eventQueue.close()
            commandQueue.close()
        }
        commandProcessor.invokeOnCompletion { cause ->
            reportStopped(cause, "command processor")
            if (cause != null) {
                eventQueue.close()
            }
            commandQueue.close()
        }
    }

    /**
     * Queues an event for serialized processing.
     *
     * This method is intentionally non-suspending so Android UI callbacks can
     * call it directly from click/listener handlers.
     *
     * When the queue cannot accept the event, [AfsmConfig.overflowPolicy]
     * decides between recording a diagnostic and throwing.
     */
    public fun send(event: E) {
        if (trySend(event)) {
            return
        }

        val diagnostic = createDiagnostic(
            code = AfsmDiagnosticCode.EventDropped,
            state = _state.value,
            event = event,
            decision = null,
            reason = if (isActive) {
                "eventQueueCapacity=${config.eventQueueCapacity}"
            } else {
                "hostStopped"
            },
            message = "Afsm event queue rejected a sent event.",
            metadata = mapOf(
                "capacity" to config.eventQueueCapacity.toString(),
                "hostActive" to isActive.toString(),
            ),
        )

        when (config.overflowPolicy) {
            AfsmOverflowPolicy.Record -> config.logger.log(diagnostic)
            AfsmOverflowPolicy.Throw ->
                throw IllegalStateException("AfsmHost event queue rejected the event.")
        }
    }

    /**
     * Attempts to queue an event for serialized processing.
     *
     * Returns `false` when the host is closed or the event queue is full.
     */
    public fun trySend(event: E): Boolean {
        return eventQueue.trySend(event).isSuccess
    }

    /**
     * Queues an event for serialized processing.
     *
     * Enables function invocation syntax on the host: `host(event)`.
     */
    public operator fun invoke(event: E) {
        send(event)
    }

    @Deprecated(
        message = "Use send(event) instead.",
        replaceWith = ReplaceWith("send(event)"),
    )
    public fun dispatch(event: E) {
        send(event)
    }

    @Deprecated(
        message = "Use trySend(event) instead.",
        replaceWith = ReplaceWith("trySend(event)"),
    )
    public fun tryDispatch(event: E): Boolean {
        return trySend(event)
    }

    /**
     * Stops the host. Android ViewModel users normally let viewModelScope own
     * the lifetime instead of calling this directly.
     */
    public fun close() {
        eventQueue.close()
        commandQueue.close()
        hostJob.cancel()
    }

    private suspend fun processEvents() {
        for (event in eventQueue) {
            processEvent(event)
        }
    }

    private suspend fun processEvent(event: E) {
        val currentState = _state.value
        val transition = reduce(currentState, event) ?: return

        when (transition.decision) {
            AfsmDecision.Transitioned,
            is AfsmDecision.Handled -> applyAcceptedTransition(
                event = event,
                transition = transition,
            )

            is AfsmDecision.Ignored -> recordDroppedIgnoredOutputsIfNeeded(
                state = currentState,
                event = event,
                transition = transition,
            )

            is AfsmDecision.Invalid -> handleInvalidTransition(
                state = currentState,
                event = event,
                transition = transition,
            )
        }
    }

    /**
     * Reduces one event, converting an unexpected reducer failure into the
     * configured invalid-transition behavior.
     *
     * A reducer that throws is a flow programming error in the same family as
     * an invalid transition: recording it keeps the current state and the host
     * usable, throwing stops the host during development.
     */
    private fun reduce(
        state: S,
        event: E,
    ): AfsmTransition<S, C>? {
        return try {
            reducer.transition(state, event)
        } catch (throwable: CancellationException) {
            throw throwable
        } catch (throwable: Throwable) {
            val diagnostic = createDiagnostic(
                code = AfsmDiagnosticCode.ReducerFailure,
                state = state,
                event = event,
                decision = null,
                reason = throwable.message,
                message = "Afsm reducer failed.",
                throwable = throwable,
            )

            when (config.invalidTransitionPolicy) {
                AfsmInvalidTransitionPolicy.Record -> {
                    config.logger.log(diagnostic)
                    null
                }

                AfsmInvalidTransitionPolicy.Throw -> throw throwable
            }
        }
    }

    private suspend fun applyAcceptedTransition(
        event: E,
        transition: AfsmTransition<S, C>,
    ) {
        _state.value = transition.state

        for (invocation in transition.commandInvocations) {
            when (invocation) {
                is AfsmCommandInvocation.Start -> startInvocation(
                    state = transition.state,
                    event = event,
                    invocation = invocation,
                    transition = transition,
                )

                is AfsmCommandInvocation.Cancel -> cancelInvocation(invocation.key)
            }
        }

        when (config.commandExecutionPolicy) {
            AfsmCommandExecutionPolicy.Sequential -> {
                for (command in transition.commands) {
                    enqueueCommand(
                        state = transition.state,
                        event = event,
                        command = command,
                        transition = transition,
                    )
                }
            }
        }
    }

    private fun enqueueCommand(
        state: S,
        event: E,
        command: C,
        transition: AfsmTransition<S, C>,
    ) {
        if (commandQueue.trySend(PendingCommand(state, event, command, transition)).isSuccess) {
            return
        }

        val diagnostic = createDiagnostic(
            code = AfsmDiagnosticCode.CommandQueueOverflow,
            state = state,
            event = event,
            decision = transition.decision,
            reason = "commandQueueCapacity=${config.commandQueueCapacity}",
            message = "Afsm command queue rejected a command.",
            command = command,
            metadata = mapOf(
                "capacity" to config.commandQueueCapacity.toString(),
            ),
        )

        when (config.overflowPolicy) {
            AfsmOverflowPolicy.Record -> config.logger.log(diagnostic)
            AfsmOverflowPolicy.Throw -> throw AfsmCommandQueueOverflowException(diagnostic)
        }
    }

    private suspend fun processCommands() {
        for (pending in commandQueue) {
            executeCommand(
                state = pending.state,
                event = pending.event,
                command = pending.command,
                transition = pending.transition,
            )
        }
    }

    private suspend fun executeCommand(
        state: S,
        event: E,
        command: C,
        transition: AfsmTransition<S, C>,
        sendAllowed: () -> Boolean = { true },
    ) {
        val send: suspend (E) -> Unit = { nextEvent ->
            if (!sendAllowed()) {
                throw CancellationException(
                    "Afsm invocation result was rejected after cancellation.",
                )
            }
            enqueueCommandResultEvent(
                state = state,
                event = nextEvent,
                command = command,
                transition = transition,
            )
        }

        try {
            if (config.commandContext == EmptyCoroutineContext) {
                commandHandler.handle(command = command, send = send)
            } else {
                withContext(config.commandContext) {
                    commandHandler.handle(command = command, send = send)
                }
            }
        } catch (throwable: CancellationException) {
            throw throwable
        } catch (throwable: AfsmEventQueueOverflowException) {
            throw throwable
        } catch (throwable: Throwable) {
            val diagnostic = createDiagnostic(
                code = AfsmDiagnosticCode.CommandFailure,
                state = state,
                event = event,
                decision = transition.decision,
                reason = throwable.message,
                message = "Afsm command failed.",
                command = command,
                throwable = throwable,
            )

            when (config.commandFailurePolicy) {
                AfsmCommandFailurePolicy.Record -> config.logger.log(diagnostic)
                AfsmCommandFailurePolicy.Throw -> throw throwable
            }
        }
    }

    private fun startInvocation(
        state: S,
        event: E,
        invocation: AfsmCommandInvocation.Start<C>,
        transition: AfsmTransition<S, C>,
    ) {
        if (!handleDuplicateInvocation(state, event, invocation, transition)) {
            return
        }

        lateinit var job: Job
        synchronized(activeInvocations) {
            job = invocationScope.launch(start = CoroutineStart.LAZY) {
                executeCommand(
                    state = state,
                    event = event,
                    command = invocation.command,
                    transition = transition,
                    sendAllowed = { job.isActive },
                )
            }
            activeInvocations[invocation.key] = job
        }

        job.invokeOnCompletion {
            synchronized(activeInvocations) {
                if (activeInvocations[invocation.key] === job) {
                    activeInvocations.remove(invocation.key)
                }
            }
        }
        job.start()
    }

    /**
     * Resolves a start request for an invocation key that is still running.
     *
     * Returns `true` when the new invocation should start. Recording keeps the
     * host alive by cancelling the stale invocation so the phase that owns the
     * key wins; throwing surfaces the duplicate as a programming error.
     */
    private fun handleDuplicateInvocation(
        state: S,
        event: E,
        invocation: AfsmCommandInvocation.Start<C>,
        transition: AfsmTransition<S, C>,
    ): Boolean {
        synchronized(activeInvocations) {
            activeInvocations[invocation.key]?.takeIf { it.isActive }
        } ?: return true

        val diagnostic = createDiagnostic(
            code = AfsmDiagnosticCode.DuplicateInvocationKey,
            state = state,
            event = event,
            decision = transition.decision,
            reason = invocation.key.value,
            message = "Afsm invocation key was already active and the previous invocation was cancelled.",
            command = invocation.command,
            metadata = mapOf("invocationKey" to invocation.key.value),
        )

        return when (config.invalidTransitionPolicy) {
            AfsmInvalidTransitionPolicy.Record -> {
                config.logger.log(diagnostic)
                cancelInvocation(invocation.key)
                true
            }

            AfsmInvalidTransitionPolicy.Throw ->
                throw IllegalStateException(
                    "Afsm invocation key is already active: ${invocation.key.value}.",
                )
        }
    }

    private fun cancelInvocation(key: AfsmInvocationKey) {
        val job = synchronized(activeInvocations) {
            activeInvocations.remove(key)
        }
        job?.cancel(
            CancellationException("Afsm phase-owned invocation was cancelled."),
        )
    }

    private fun enqueueCommandResultEvent(
        state: S,
        event: E,
        command: C,
        transition: AfsmTransition<S, C>,
    ) {
        val result = eventQueue.trySend(event)
        if (result.isSuccess) {
            return
        }

        if (result.isClosed) {
            config.logger.log(
                createDiagnostic(
                    code = AfsmDiagnosticCode.CommandResultDroppedHostClosed,
                    state = state,
                    event = event,
                    decision = transition.decision,
                    reason = "eventQueueClosed",
                    message = "Afsm command result event was dropped because the host event queue is closed.",
                    command = command,
                ),
            )
            return
        }

        val diagnostic = createDiagnostic(
            code = AfsmDiagnosticCode.CommandResultQueueOverflow,
            state = state,
            event = event,
            decision = transition.decision,
            reason = "eventQueueCapacity=${config.eventQueueCapacity}",
            message = "Afsm event queue rejected a command result event.",
            command = command,
            metadata = mapOf(
                "capacity" to config.eventQueueCapacity.toString(),
            ),
        )

        when (config.overflowPolicy) {
            AfsmOverflowPolicy.Record -> config.logger.log(diagnostic)
            AfsmOverflowPolicy.Throw -> throw AfsmEventQueueOverflowException(diagnostic)
        }
    }

    private fun recordDroppedIgnoredOutputsIfNeeded(
        state: S,
        event: E,
        transition: AfsmTransition<S, C>,
    ) {
        val hasDroppedOutputs = transition.commands.isNotEmpty()
        val changedState = transition.state != state

        if (!hasDroppedOutputs && !changedState) {
            return
        }

        config.logger.log(
            createDiagnostic(
                code = AfsmDiagnosticCode.IgnoredTransitionOutputDropped,
                state = state,
                event = event,
                decision = transition.decision,
                reason = (transition.decision as AfsmDecision.Ignored).reason,
                message = "Ignored transition output was dropped.",
            ),
        )
    }

    private fun handleInvalidTransition(
        state: S,
        event: E,
        transition: AfsmTransition<S, C>,
    ) {
        val decision = transition.decision as AfsmDecision.Invalid
        val diagnostic = createDiagnostic(
            code = AfsmDiagnosticCode.InvalidTransition,
            state = state,
            event = event,
            decision = decision,
            reason = decision.reason,
            message = "Invalid Afsm transition.",
        )

        when (config.invalidTransitionPolicy) {
            AfsmInvalidTransitionPolicy.Record -> config.logger.log(diagnostic)
            AfsmInvalidTransitionPolicy.Throw -> throw AfsmInvalidTransitionException(diagnostic)
        }
    }

    /**
     * Reports a processing coroutine that stopped because of a failure.
     *
     * Normal cancellation (scope cancelled, [close] called) is not a failure
     * and is not reported.
     */
    private fun reportStopped(
        cause: Throwable?,
        processorName: String,
    ) {
        if (cause == null || cause is CancellationException) {
            return
        }

        config.logger.log(
            AfsmDiagnostic(
                code = AfsmDiagnosticCode.HostStopped,
                decision = null,
                message = "Afsm host stopped processing events.",
                stateType = _state.value.safeTypeName(),
                eventType = null,
                commandType = null,
                failureType = cause.safeTypeName(),
                metadata = mapOf("processor" to processorName),
                values = when (config.diagnosticDataPolicy) {
                    AfsmDiagnosticDataPolicy.TypesOnly -> null
                    AfsmDiagnosticDataPolicy.IncludeValues -> AfsmDiagnosticValues(
                        state = _state.value,
                        event = null,
                        command = null,
                        reason = cause.message,
                        throwable = cause,
                    )
                },
            ),
        )
    }

    private fun createDiagnostic(
        code: AfsmDiagnosticCode,
        state: S,
        event: E,
        decision: AfsmDecision?,
        message: String,
        command: C? = null,
        throwable: Throwable? = null,
        reason: String? = null,
        metadata: Map<String, String> = emptyMap(),
    ): AfsmDiagnostic {
        val values = when (config.diagnosticDataPolicy) {
            AfsmDiagnosticDataPolicy.TypesOnly -> null
            AfsmDiagnosticDataPolicy.IncludeValues -> AfsmDiagnosticValues(
                state = state,
                event = event,
                command = command,
                reason = reason,
                throwable = throwable,
            )
        }

        return AfsmDiagnostic(
            code = code,
            decision = decision?.toDiagnosticDecision(),
            message = message,
            stateType = state.safeTypeName(),
            eventType = event.safeTypeName(),
            commandType = command?.safeTypeName(),
            failureType = throwable?.safeTypeName(),
            metadata = metadata.toMap(),
            values = values,
        )
    }
}

private fun AfsmDecision.toDiagnosticDecision(): AfsmDiagnosticDecision = when (this) {
    AfsmDecision.Transitioned -> AfsmDiagnosticDecision.Transitioned
    is AfsmDecision.Handled -> AfsmDiagnosticDecision.Handled
    is AfsmDecision.Ignored -> AfsmDiagnosticDecision.Ignored
    is AfsmDecision.Invalid -> AfsmDiagnosticDecision.Invalid
}

private fun Any.safeTypeName(): String {
    return this::class.simpleName ?: "Unknown"
}

private data class PendingCommand<S : Any, E : Any, C : Any>(
    val state: S,
    val event: E,
    val command: C,
    val transition: AfsmTransition<S, C>,
)
