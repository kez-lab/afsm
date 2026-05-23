package afsm.core

public object Afsm {
    public fun <S : Any, C : Any, F : Any> transitioned(
        state: S,
        commands: List<C> = emptyList(),
        effects: List<F> = emptyList(),
    ): AfsmTransition<S, C, F> {
        return AfsmTransition.transitioned(
            state = state,
            commands = commands,
            effects = effects,
        )
    }

    public fun <S : Any, C : Any, F : Any> ignore(
        state: S,
        reason: String? = null,
    ): AfsmTransition<S, C, F> {
        return AfsmTransition.ignored(
            state = state,
            reason = reason,
        )
    }

    public fun <S : Any, C : Any, F : Any> invalid(
        state: S,
        reason: String? = null,
    ): AfsmTransition<S, C, F> {
        return AfsmTransition.invalid(
            state = state,
            reason = reason,
        )
    }
}
