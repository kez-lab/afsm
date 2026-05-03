package afsm.core

public fun interface AfsmStateMachine<S : Any, E : Any, C : Any, F : Any> {
    public fun transition(
        state: S,
        event: E,
    ): AfsmTransition<S, C, F>
}
