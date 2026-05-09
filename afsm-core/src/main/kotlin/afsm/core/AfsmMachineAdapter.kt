package afsm.core

/**
 * Adapts a standard [AfsmMachine] to a feature-specific screen state.
 */
public abstract class AfsmMachineAdapter<
    S : Any,
    P : Any,
    X : Any,
    E : Any,
    C : Any,
    F : Any,
    >(
    private val machine: AfsmMachine<P, X, E, C, F>,
) : AfsmReducer<S, E, C, F>,
    AfsmGraphSource {

    final override val topology: AfsmTopology
        get() = machine.topology

    final override fun transition(
        state: S,
        event: E,
    ): AfsmTransition<S, C, F> {
        val transition = machine.transition(
            state = toAfsmState(state),
            event = event,
        )

        return AfsmTransition(
            state = toScreenState(transition.state),
            commands = transition.commands,
            effects = transition.effects,
            decision = transition.decision,
        )
    }

    protected abstract fun toAfsmState(state: S): AfsmState<P, X>

    protected abstract fun toScreenState(state: AfsmState<P, X>): S
}
