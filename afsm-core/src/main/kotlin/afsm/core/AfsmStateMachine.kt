package afsm.core

@Deprecated(
    message = "Use AfsmReducer for the low-level state + event -> transition contract.",
    replaceWith = ReplaceWith("AfsmReducer<S, E, C, F>"),
)
public typealias AfsmStateMachine<S, E, C, F> = AfsmReducer<S, E, C, F>
