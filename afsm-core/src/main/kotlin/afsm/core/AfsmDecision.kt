package afsm.core

public sealed interface AfsmDecision {
    public data object Transitioned : AfsmDecision

    public data class Handled(
        public val reason: String? = null,
    ) : AfsmDecision

    public data class Ignored(
        public val reason: String? = null,
    ) : AfsmDecision

    public data class Invalid(
        public val reason: String? = null,
    ) : AfsmDecision
}
