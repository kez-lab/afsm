package afsm.core

/**
 * Backwards-compatible name for the original executable statechart state.
 */
@Deprecated(
    message = "Use AfsmState. Afsm state is always phase + context.",
    replaceWith = ReplaceWith("AfsmState<P, X>"),
)
public typealias AfsmChartState<P, X> = AfsmState<P, X>
