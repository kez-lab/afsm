package afsm.core

/**
 * Executable plain Kotlin statechart.
 *
 * The chart receives the current [AfsmChartState] and an event, then returns an
 * [AfsmTransition] containing the next chart state plus host-executed commands and
 * optional UI effects.
 */
public interface AfsmStateChart<P : Any, X : Any, E : Any, A : Any, F : Any> {
    public val initialState: AfsmChartState<P, X>

    /**
     * Static state and transition metadata declared by this chart.
     */
    public val topology: AfsmTopology

    public fun transition(
        state: AfsmChartState<P, X>,
        event: E,
    ): AfsmTransition<AfsmChartState<P, X>, A, F>
}
