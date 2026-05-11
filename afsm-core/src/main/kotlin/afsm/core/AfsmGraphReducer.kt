package afsm.core

/**
 * Reducer that also exposes its initial state and static graph metadata.
 *
 * Use this type when a machine should be accepted by [AfsmHost]-style runtime
 * code through [AfsmReducer] and by graph tooling through [AfsmGraphSource].
 * It is especially useful at feature boundaries where the state type is already
 * named, for example `AfsmGraphReducer<LoginState, LoginEvent, LoginCommand, LoginEffect>`.
 */
public interface AfsmGraphReducer<S : Any, E : Any, C : Any, F : Any> :
    AfsmReducer<S, E, C, F>,
    AfsmGraphSource {
    public val initialState: S
}
