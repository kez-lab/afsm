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
     */
    public val eventQueueCapacity: Int = 64,
    /**
     * Receives diagnostics for recorded invalid transitions and defensive drops.
     */
    public val logger: AfsmLogger =
        AfsmLogger.None,
) {
    init {
        require(eventQueueCapacity > 0) { "eventQueueCapacity must be > 0." }
    }
}
