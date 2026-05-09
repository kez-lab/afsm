package afsm.core

/**
 * Adapter base for feature state machines backed by an executable Afsm statechart.
 *
 * This keeps Android-facing state [S] as the public screen state while hiding
 * topology forwarding and chart-state transition plumbing.
 */
public abstract class AfsmStateChartMachine<
    S : Any,
    P : Any,
    X : Any,
    E : Any,
    C : Any,
    F : Any,
    >(
    private val chart: AfsmStateChart<P, X, E, C, F>,
) : AfsmStateMachine<S, E, C, F>,
    AfsmGraphSource {

    final override val topology: AfsmTopology
        get() = chart.topology

    final override fun transition(
        state: S,
        event: E,
    ): AfsmTransition<S, C, F> {
        val transition = chart.transition(
            state = toChartState(state),
            event = event,
        )

        return AfsmTransition(
            state = toScreenState(transition.state),
            commands = transition.commands,
            effects = transition.effects,
            decision = transition.decision,
        )
    }

    protected abstract fun toChartState(state: S): AfsmChartState<P, X>

    protected abstract fun toScreenState(state: AfsmChartState<P, X>): S
}
