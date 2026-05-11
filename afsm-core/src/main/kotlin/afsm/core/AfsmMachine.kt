package afsm.core

/**
 * Executable plain Kotlin Afsm definition.
 *
 * The machine receives the current [AfsmState] and an event, then returns an
 * [AfsmTransition] containing the next state plus host-executed commands and
 * optional UI effects.
 */
public interface AfsmMachine<P : Any, X : Any, E : Any, C : Any, F : Any> :
    AfsmGraphReducer<AfsmState<P, X>, E, C, F> {
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
