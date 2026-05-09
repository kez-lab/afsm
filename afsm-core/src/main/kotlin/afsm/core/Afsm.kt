package afsm.core

public object Afsm {
    public fun <S : Any, C : Any, F : Any> transitionTo(
        state: S,
        commands: List<C> = emptyList(),
        effects: List<F> = emptyList(),
    ): AfsmTransition<S, C, F> {
        return AfsmTransition(
            state = state,
            commands = commands,
            effects = effects,
            decision = AfsmDecision.Transitioned,
        )
    }

    public fun <S : Any, C : Any, F : Any> stay(
        state: S,
        commands: List<C> = emptyList(),
        effects: List<F> = emptyList(),
        reason: String? = null,
    ): AfsmTransition<S, C, F> {
        return AfsmTransition(
            state = state,
            commands = commands,
            effects = effects,
            decision = AfsmDecision.Stayed(reason),
        )
    }

    public fun <S : Any, C : Any, F : Any> ignore(
        state: S,
        reason: String? = null,
    ): AfsmTransition<S, C, F> {
        return AfsmTransition(
            state = state,
            decision = AfsmDecision.Ignored(reason),
        )
    }

    public fun <S : Any, C : Any, F : Any> invalid(
        state: S,
        reason: String? = null,
    ): AfsmTransition<S, C, F> {
        return AfsmTransition(
            state = state,
            decision = AfsmDecision.Invalid(reason),
        )
    }

}
