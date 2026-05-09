package afsm.core

public class AfsmPhasedTransitionScope<S, P, X, E, C, F> internal constructor(
    public val state: S,
    private val event: E,
    private val entryPolicy: AfsmPhaseEntryPolicy<P, X, E, C, F>,
) where S : Any,
        S : AfsmPhasedState<S, P, X>,
        P : Any,
        X : Any,
        E : Any,
        C : Any,
        F : Any {

    public fun transitionTo(target: P): AfsmTransition<S, C, F> {
        val entry = entryPolicy.enter(
            from = state.phase,
            target = target,
            event = event,
            context = state.context,
        )

        return Afsm.transitionTo(
            state = state.with(
                phase = target,
                context = entry.context,
            ),
            commands = entry.commands,
            effects = entry.effects,
        )
    }

    public fun updateContext(
        update: (X) -> X,
    ): AfsmTransition<S, C, F> {
        val nextContext = update(state.context)

        return Afsm.stay(
            state = state.with(
                phase = state.phase,
                context = nextContext,
            ),
        )
    }

    public fun updateContext(
        update: (X) -> X,
        commands: (X) -> List<C> = { emptyList() },
        effects: (X) -> List<F> = { emptyList() },
        reason: String? = null,
    ): AfsmTransition<S, C, F> {
        val nextContext = update(state.context)

        return Afsm.stay(
            state = state.with(
                phase = state.phase,
                context = nextContext,
            ),
            commands = commands(nextContext),
            effects = effects(nextContext),
            reason = reason,
        )
    }

    public fun stay(
        commands: List<C> = emptyList(),
        effects: List<F> = emptyList(),
        reason: String? = null,
    ): AfsmTransition<S, C, F> {
        return Afsm.stay(
            state = state,
            commands = commands,
            effects = effects,
            reason = reason,
        )
    }

    public fun ignore(reason: String? = null): AfsmTransition<S, C, F> {
        return Afsm.ignore(
            state = state,
            reason = reason,
        )
    }

    public fun invalid(reason: String? = null): AfsmTransition<S, C, F> {
        return Afsm.invalid(
            state = state,
            reason = reason,
        )
    }
}
