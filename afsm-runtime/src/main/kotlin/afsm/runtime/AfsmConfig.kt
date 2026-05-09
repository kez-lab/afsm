package afsm.runtime

public class AfsmConfig(
    /**
     * Controls how the runtime reacts when the state machine marks a transition
     * as [afsm.core.AfsmDecision.Invalid].
     */
    public val invalidTransitionPolicy: AfsmInvalidTransitionPolicy =
        AfsmInvalidTransitionPolicy.Record,
    /**
     * MVP runtime executes commands sequentially for predictable Android UI
     * behavior.
     */
    public val commandExecutionPolicy: AfsmCommandExecutionPolicy =
        AfsmCommandExecutionPolicy.Sequential,
    /**
     * Controls one-shot effect buffering. Effects are not durable state.
     */
    public val effectDelivery: AfsmEffectDelivery =
        AfsmEffectDelivery.Default,
    /**
     * Receives diagnostics for recorded invalid transitions and defensive drops.
     */
    public val logger: AfsmLogger =
        AfsmLogger.None,
)
