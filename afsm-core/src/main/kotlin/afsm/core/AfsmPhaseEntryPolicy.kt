package afsm.core

public fun interface AfsmPhaseEntryPolicy<P : Any, X : Any, E : Any, C : Any, F : Any> {
    public fun enter(
        from: P,
        target: P,
        event: E,
        context: X,
    ): AfsmPhaseEntry<X, C, F>
}
