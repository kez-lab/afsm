package afsm.runtime

import afsm.core.AfsmDecision
import afsm.core.AfsmCommandInvocation
import afsm.core.AfsmInvocationKey
import afsm.core.AfsmReducer
import afsm.core.AfsmTransition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

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

    private val processor: Job = scope.launch(start = CoroutineStart.UNDISPATCHED) {
        processEvents()
    }
    private val commandProcessor: Job = scope.launch(start = CoroutineStart.UNDISPATCHED) {
        processCommands()
    }
    private val invocationScope = CoroutineScope(scope.coroutineContext + commandProcessor)
    private val activeInvocations = mutableMapOf<AfsmInvocationKey, Job>()

    public val state: StateFlow<S> = _state.asStateFlow()

    init {
        processor.invokeOnCompletion { cause ->
            eventQueue.close(cause)
            commandQueue.close(cause)
        }
        commandProcessor.invokeOnCompletion { cause ->
            if (cause != null) {
                eventQueue.close(cause)
            }
            commandQueue.close(cause)
        }
    }

    /**
     * Queues an event for serialized processing.
     *
     * This method is intentionally non-suspending so Android UI callbacks can
     * call it directly from click/listener handlers.
     */
    public fun dispatch(event: E) {
        if (!tryDispatch(event)) {
            throw IllegalStateException("AfsmHost event queue rejected the event.")
        }
    }

    /**
     * Attempts to queue an event for serialized processing.
     *
     * Returns `false` when the host is closed or the event queue is full.
     */
    public fun tryDispatch(event: E): Boolean {
        return eventQueue.trySend(event).isSuccess
    }

    /**
     * Stops the host. Android ViewModel users normally let viewModelScope own
     * the lifetime instead of calling this directly.
     */
    public fun close() {
        eventQueue.close()
        commandQueue.close()
        processor.cancel()
        commandProcessor.cancel()
    }

    private suspend fun processEvents() {
        for (event in eventQueue) {
            processEvent(event)
        }
    }

    private suspend fun processEvent(event: E) {
        val currentState = _state.value
        val transition = reducer.transition(currentState, event)

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
        val pending = PendingCommand(
            state = state,
            event = event,
            command = command,
            transition = transition,
        )

        if (commandQueue.trySend(pending).isSuccess) {
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

        throw AfsmCommandQueueOverflowException(diagnostic)
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
        dispatchAllowed: () -> Boolean = { true },
    ) {
        try {
            commandHandler.handle(
                command = command,
                dispatchEvent = { nextEvent ->
                    if (!dispatchAllowed()) {
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
                },
            )
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
        lateinit var job: Job
        synchronized(activeInvocations) {
            val existing = activeInvocations[invocation.key]
            check(existing == null || !existing.isActive) {
                "Afsm invocation key is already active: ${invocation.key.value}."
            }

            job = invocationScope.launch(start = CoroutineStart.LAZY) {
                executeCommand(
                    state = state,
                    event = event,
                    command = invocation.command,
                    transition = transition,
                    dispatchAllowed = { job.isActive },
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

        throw AfsmEventQueueOverflowException(diagnostic)
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

    private fun createDiagnostic(
        code: AfsmDiagnosticCode,
        state: S,
        event: E,
        decision: AfsmDecision,
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
            decision = decision.toDiagnosticDecision(),
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
