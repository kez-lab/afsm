package afsm.core

/**
 * Executable plain Kotlin statechart.
 *
 * The chart receives the current [AfsmState] and an event, then returns an
 * [AfsmTransition] containing the next chart state plus host-executed commands and
 * optional UI effects.
 */
public interface AfsmStateChart<P : Any, X : Any, E : Any, A : Any, F : Any> :
    AfsmStateMachine<AfsmState<P, X>, E, A, F>,
    AfsmGraphSource {
    public val initialState: AfsmState<P, X>

    /**
     * Static state and transition metadata declared by this chart.
     */
    public override val topology: AfsmTopology

    public override fun transition(
        state: AfsmState<P, X>,
        event: E,
    ): AfsmTransition<AfsmState<P, X>, A, F>
}
