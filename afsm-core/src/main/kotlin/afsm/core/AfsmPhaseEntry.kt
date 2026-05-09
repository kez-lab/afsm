package afsm.core

public data class AfsmPhaseEntry<out X : Any, out C : Any, out F : Any>(
    public val context: X,
    public val commands: List<C> = emptyList(),
    public val effects: List<F> = emptyList(),
)
