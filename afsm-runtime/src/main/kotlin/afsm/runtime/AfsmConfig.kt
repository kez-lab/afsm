package afsm.runtime

public class AfsmConfig(
    /**
     * Controls how the runtime reacts when the state machine marks a transition
     * as [afsm.core.AfsmDecision.Invalid].
     */
    public val invalidTransitionPolicy: AfsmInvalidTransitionPolicy =
        AfsmInvalidTransitionPolicy.Throw,
    /**
     * Commands are executed one at a time, separately from event reduction.
     * This preserves predictable command ordering without blocking later UI
     * events from being reduced while a command is suspended.
     */
    public val commandExecutionPolicy: AfsmCommandExecutionPolicy =
        AfsmCommandExecutionPolicy.Sequential,
    /**
     * Unexpected command failures are thrown by default so programming errors
     * are visible during development. Domain failures should usually be
     * converted to events by the command handler instead of thrown.
     */
    public val commandFailurePolicy: AfsmCommandFailurePolicy =
        AfsmCommandFailurePolicy.Throw,
    /**
     * Controls one-shot effect buffering. Effects are not durable state.
     */
    public val effectDelivery: AfsmEffectDelivery =
        AfsmEffectDelivery.Default,
    /**
     * Maximum number of events that can be queued by non-suspending dispatch.
     *
     * Command result events also use this queue. If a command result event
     * cannot be queued, the host throws [AfsmEventQueueOverflowException]
     * instead of suspending the sequential command processor indefinitely.
     */
    public val eventQueueCapacity: Int = 64,
    /**
     * Maximum number of accepted commands that can wait for the sequential
     * command processor.
     *
     * Afsm commands are emitted by accepted transitions and are processed
     * separately from event reduction. Keeping this queue bounded prevents
     * accidental unbounded memory growth when a machine emits commands faster
     * than the host can execute them. If the queue fills, the host throws
     * [AfsmCommandQueueOverflowException] instead of suspending the event
     * processor indefinitely.
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
     * Receives diagnostics for recorded invalid transitions, command failures,
     * queue failures, and defensive drops.
     */
    public val logger: AfsmLogger =
        AfsmLogger.None,
) {
    init {
        require(eventQueueCapacity > 0) { "eventQueueCapacity must be > 0." }
        require(commandQueueCapacity > 0) { "commandQueueCapacity must be > 0." }
    }
}
