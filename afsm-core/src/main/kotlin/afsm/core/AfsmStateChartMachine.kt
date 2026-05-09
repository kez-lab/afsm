package afsm.core

@Deprecated(
    message = "Use AfsmMachineAdapter.",
    replaceWith = ReplaceWith("AfsmMachineAdapter<S, P, X, E, C, F>"),
)
public typealias AfsmStateChartMachine<S, P, X, E, C, F> =
    AfsmMachineAdapter<S, P, X, E, C, F>
