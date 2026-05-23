package afsm.core

/**
 * Graphable executable Afsm definition.
 *
 * Use this type at feature boundaries when the screen state type is already
 * named, for example `AfsmMachine<LoginState, LoginEvent, LoginCommand,
 * LoginEffect>`.
 */
public interface AfsmMachine<S : Any, E : Any, C : Any, F : Any> :
    AfsmReducer<S, E, C, F>,
    AfsmGraphSource {
    public val initialState: S
}
