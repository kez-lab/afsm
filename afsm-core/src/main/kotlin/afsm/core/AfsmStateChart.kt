package afsm.core

@Deprecated(
    message = "Use AfsmMachine for the DSL-built executable machine.",
    replaceWith = ReplaceWith("AfsmMachine<P, X, E, C, F>"),
)
public typealias AfsmStateChart<P, X, E, C, F> = AfsmMachine<P, X, E, C, F>
