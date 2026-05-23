package afsm.core

/**
 * Classification of how a state machine handled one dispatched event.
 *
 * The runtime uses this value to decide whether to publish state and outputs,
 * ignore the event, or apply the configured invalid-transition policy.
 */
public sealed interface AfsmDecision {
    /**
     * The event caused an accepted phase/state change.
     */
    public data object Transitioned : AfsmDecision

    /**
     * The event was accepted without a phase/state change.
     *
     * In the DSL this is produced by an accepted branch that updates data,
     * emits outputs, or simply handles the event without calling
     * `transitionTo(...)`.
     */
    public data class Handled(
        public val reason: String? = null,
    ) : AfsmDecision

    /**
     * The event was expected in this phase but intentionally did nothing.
     *
     * Use this for duplicate or stale events that are harmless and worth
     * documenting, such as a retry click while a retry command is already
     * running. Ignored decisions drop state, command, and effect output.
     */
    public data class Ignored(
        public val reason: String? = null,
    ) : AfsmDecision

    /**
     * The event is not valid in the current phase/state.
     *
     * Hosts apply [AfsmInvalidTransitionPolicy] to invalid decisions. The
     * default runtime policy throws so flow bugs are visible during
     * development.
     */
    public data class Invalid(
        public val reason: String? = null,
    ) : AfsmDecision
}
