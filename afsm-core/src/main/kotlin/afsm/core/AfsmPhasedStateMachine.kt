package afsm.core

public abstract class AfsmPhasedStateMachine<S, P, X, E, C, F>(
    private val entryPolicy: AfsmPhaseEntryPolicy<P, X, E, C, F>,
) : AfsmStateMachine<S, E, C, F>
    where S : Any,
          S : AfsmPhasedState<S, P, X>,
          P : Any,
          X : Any,
          E : Any,
          C : Any,
          F : Any {

    final override fun transition(
        state: S,
        event: E,
    ): AfsmTransition<S, C, F> {
        return Afsm.phased(
            state = state,
            event = event,
            entryPolicy = entryPolicy,
        ).reduce(event)
    }

    protected abstract fun AfsmPhasedTransitionScope<S, P, X, E, C, F>.reduce(
        event: E,
    ): AfsmTransition<S, C, F>
}
