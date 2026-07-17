package afsm.core

public fun interface AfsmReducer<S : Any, E : Any, C : Any> {
    public fun transition(
        state: S,
        event: E,
    ): AfsmTransition<S, C>
}
