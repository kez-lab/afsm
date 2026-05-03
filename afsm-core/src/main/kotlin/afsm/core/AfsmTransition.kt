package afsm.core

public data class AfsmTransition<out S : Any, out C : Any, out F : Any>(
    public val state: S,
    public val commands: List<C> = emptyList(),
    public val effects: List<F> = emptyList(),
    public val decision: AfsmDecision = AfsmDecision.Transitioned,
)
