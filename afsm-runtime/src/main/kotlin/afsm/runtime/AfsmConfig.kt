package afsm.runtime

import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

public class AfsmConfig(
    /**
     * Controls how the runtime reacts when the state machine marks a transition
     * as [afsm.core.AfsmDecision.Invalid], and when the reducer itself throws.
     *
     * The default records a diagnostic and keeps the host usable, because a
     * single flow-modelling mistake should not stop a shipped screen from
     * accepting further events. Use [AfsmConfig.strict] during development to
     * surface the same mistakes as failures.
     */
    public val invalidTransitionPolicy: AfsmInvalidTransitionPolicy =
        AfsmInvalidTransitionPolicy.Record,
    /**
     * Commands are executed one at a time, separately from event reduction.
     * This preserves predictable command ordering without blocking later UI
     * events from being reduced while a command is suspended.
     */
    public val commandExecutionPolicy: AfsmCommandExecutionPolicy =
        AfsmCommandExecutionPolicy.Sequential,
    /**
     * Controls how the runtime reacts when a command handler throws.
     *
     * The default records a diagnostic and keeps the host alive. Domain
     * failures should still be converted to events by the command handler; this
     * policy only decides what happens to failures the handler did not model.
     */
    public val commandFailurePolicy: AfsmCommandFailurePolicy =
        AfsmCommandFailurePolicy.Record,
    /**
     * Controls how the runtime reacts when a bounded queue rejects work.
     *
     * The default records a diagnostic and drops the rejected event or command.
     */
    public val overflowPolicy: AfsmOverflowPolicy =
        AfsmOverflowPolicy.Record,
    /**
     * Extra coroutine context used while a command handler runs.
     *
     * The host runs on the context of the scope that owns it, which is the main
     * dispatcher for `viewModelScope`. Set this to `Dispatchers.IO` (or another
     * dispatcher) when command handlers call work that is not main-safe. Event
     * reduction always stays on the host scope so transitions stay serialized.
     */
    public val commandContext: CoroutineContext = EmptyCoroutineContext,
    /**
     * Maximum number of events that can be queued by non-suspending dispatch.
     *
     * Command result events also use this queue. If a command result event
     * cannot be queued, [overflowPolicy] decides whether it is dropped with a
     * diagnostic or reported as [AfsmEventQueueOverflowException].
     */
    public val eventQueueCapacity: Int = 64,
    /**
     * Maximum number of accepted commands that can wait for the sequential
     * command processor.
     *
     * Afsm commands are emitted by accepted transitions and are processed
     * separately from event reduction. Keeping this queue bounded prevents
     * accidental unbounded memory growth when a machine emits commands faster
     * than the host can execute them. If the queue fills, [overflowPolicy]
     * decides whether the command is dropped with a diagnostic or reported as
     * [AfsmCommandQueueOverflowException].
     */
    public val commandQueueCapacity: Int = 64,
    /**
     * Controls whether diagnostics retain raw domain values.
     *
     * [AfsmDiagnosticDataPolicy.TypesOnly] is the privacy-safe default.
     */
    public val diagnosticDataPolicy: AfsmDiagnosticDataPolicy =
        AfsmDiagnosticDataPolicy.TypesOnly,
    /**
     * Receives diagnostics for recorded invalid transitions, reducer failures,
     * command failures, queue overflow, host shutdown, and defensive drops.
     *
     * The default logger discards diagnostics. Supply a real logger so recorded
     * failures stay visible.
     */
    public val logger: AfsmLogger =
        AfsmLogger.None,
) {
    init {
        require(eventQueueCapacity > 0) { "eventQueueCapacity must be > 0." }
        require(commandQueueCapacity > 0) { "commandQueueCapacity must be > 0." }
    }

    public companion object {
        /**
         * Configuration that fails fast on every modelled error.
         *
         * Use this in debug builds and tests so invalid transitions, reducer
         * failures, unmodelled command failures, and queue overflow stop the
         * host instead of being recorded. Release builds should keep the
         * recording defaults.
         */
        public fun strict(
            commandContext: CoroutineContext = EmptyCoroutineContext,
            eventQueueCapacity: Int = 64,
            commandQueueCapacity: Int = 64,
            diagnosticDataPolicy: AfsmDiagnosticDataPolicy = AfsmDiagnosticDataPolicy.TypesOnly,
            logger: AfsmLogger = AfsmLogger.None,
        ): AfsmConfig {
            return AfsmConfig(
                invalidTransitionPolicy = AfsmInvalidTransitionPolicy.Throw,
                commandFailurePolicy = AfsmCommandFailurePolicy.Throw,
                overflowPolicy = AfsmOverflowPolicy.Throw,
                commandContext = commandContext,
                eventQueueCapacity = eventQueueCapacity,
                commandQueueCapacity = commandQueueCapacity,
                diagnosticDataPolicy = diagnosticDataPolicy,
                logger = logger,
            )
        }
    }
}
