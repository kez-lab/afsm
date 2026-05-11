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

/**
 * Executable phase/context Afsm definition built by [afsmMachine].
 *
 * The machine receives the current [AfsmState] and an event, then returns an
 * [AfsmTransition] containing the next state plus host-executed commands and
 * optional UI effects.
 */
public interface AfsmPhaseMachine<P : Any, X : Any, E : Any, C : Any, F : Any> :
    AfsmMachine<AfsmState<P, X>, E, C, F> {
    public override val initialState: AfsmState<P, X>

    /**
     * Static state and transition metadata declared by this machine.
     */
    public override val topology: AfsmTopology

    public override fun transition(
        state: AfsmState<P, X>,
        event: E,
    ): AfsmTransition<AfsmState<P, X>, C, F>
}
