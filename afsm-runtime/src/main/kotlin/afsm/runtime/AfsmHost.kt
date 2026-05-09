package afsm.runtime

import afsm.core.AfsmDecision
import afsm.core.AfsmReducer
import afsm.core.AfsmTransition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

public class AfsmHost<S : Any, E : Any, C : Any, F : Any>(
    initialState: S,
    private val reducer: AfsmReducer<S, E, C, F>,
    private val commandHandler: AfsmCommandHandler<C, E>,
    scope: CoroutineScope,
    private val config: AfsmConfig = AfsmConfig(),
) {
    private val eventQueue = Channel<E>(Channel.UNLIMITED)
    private val _state = MutableStateFlow(initialState)
    private val _effects = MutableSharedFlow<F>(
        replay = config.effectDelivery.replay,
        extraBufferCapacity = config.effectDelivery.extraBufferCapacity,
        onBufferOverflow = config.effectDelivery.onBufferOverflow,
    )

    private val processor: Job = scope.launch(start = CoroutineStart.UNDISPATCHED) {
        processEvents()
    }

    public val state: StateFlow<S> = _state.asStateFlow()

    /**
     * Best-effort one-shot effects emitted by accepted transitions.
     *
     * Critical UI actions that must survive recreation should be modeled as
     * durable state plus an acknowledgement event instead of effect-only output.
     */
    public val effects: Flow<F> = _effects.asSharedFlow()

    init {
        processor.invokeOnCompletion { cause ->
            eventQueue.close(cause)
        }
    }

    /**
     * Queues an event for serialized processing.
     *
     * This method is intentionally non-suspending so Android UI callbacks can
     * call it directly from click/listener handlers.
     */
    public fun dispatch(event: E) {
        val result = eventQueue.trySend(event)
        if (result.isFailure) {
            throw ClosedSendChannelException("AfsmHost is closed and cannot accept events.")
        }
    }

    /**
     * Stops the host. Android ViewModel users normally let viewModelScope own
     * the lifetime instead of calling this directly.
     */
    public fun close() {
        eventQueue.close()
        processor.cancel()
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
            is AfsmDecision.Stayed -> applyAcceptedTransition(
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
        transition: AfsmTransition<S, C, F>,
    ) {
        _state.value = transition.state

        for (effect in transition.effects) {
            _effects.emit(effect)
        }

        when (config.commandExecutionPolicy) {
            AfsmCommandExecutionPolicy.Sequential -> {
                for (command in transition.commands) {
                    executeCommand(
                        state = transition.state,
                        event = event,
                        command = command,
                        transition = transition,
                    )
                }
            }
        }
    }

    private suspend fun executeCommand(
        state: S,
        event: E,
        command: C,
        transition: AfsmTransition<S, C, F>,
    ) {
        try {
            commandHandler.handle(command) { nextEvent ->
                eventQueue.send(nextEvent)
            }
        } catch (throwable: CancellationException) {
            throw throwable
        } catch (throwable: Throwable) {
            val diagnostic = AfsmDiagnostic(
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

    private fun recordDroppedIgnoredOutputsIfNeeded(
        state: S,
        event: E,
        transition: AfsmTransition<S, C, F>,
    ) {
        val hasDroppedOutputs =
            transition.commands.isNotEmpty() || transition.effects.isNotEmpty()
        val changedState = transition.state != state

        if (!hasDroppedOutputs && !changedState) {
            return
        }

        config.logger.log(
            AfsmDiagnostic(
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
        transition: AfsmTransition<S, C, F>,
    ) {
        val decision = transition.decision as AfsmDecision.Invalid
        val diagnostic = AfsmDiagnostic(
            state = state,
            event = event,
            decision = decision,
            reason = decision.reason,
            message = decision.reason ?: "Invalid Afsm transition.",
        )

        when (config.invalidTransitionPolicy) {
            AfsmInvalidTransitionPolicy.Record -> config.logger.log(diagnostic)
            AfsmInvalidTransitionPolicy.Throw -> throw AfsmInvalidTransitionException(diagnostic)
        }
    }
}
