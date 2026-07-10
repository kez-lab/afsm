package afsm.core

/**
 * Graphable executable Afsm definition.
 *
 * This contract owns transition behavior and graph topology. It does not imply
 * that a usable default state exists. Android features whose starting state
 * comes from navigation, a deep link, or restoration should use this type and
 * pass an explicit state to their host.
 */
public interface AfsmMachine<S : Any, E : Any, C : Any, F : Any> :
    AfsmReducer<S, E, C, F>,
    AfsmGraphSource

/**
 * An [AfsmMachine] that owns a genuine default state.
 *
 * Use this subtype for static flows whose default is valid without Android
 * runtime input. It enables the concise ViewModel host overload that starts
 * from [initialState].
 */
public interface AfsmDefaultMachine<S : Any, E : Any, C : Any, F : Any> :
    AfsmMachine<S, E, C, F> {
    public val initialState: S
}
