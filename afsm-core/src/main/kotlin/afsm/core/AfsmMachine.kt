package afsm.core

/**
 * Executable plain Kotlin state machine.
 *
 * The machine receives the current [AfsmSnapshot] and an event, then returns an
 * [AfsmTransition] containing the next snapshot plus host-executed commands and
 * optional UI effects.
 */
public interface AfsmMachine<P : Any, X : Any, E : Any, A : Any, F : Any> {
    public val initialSnapshot: AfsmSnapshot<P, X>

    /**
     * Static state and transition metadata declared by this machine.
     */
    public val topology: AfsmTopology

    public fun transition(
        snapshot: AfsmSnapshot<P, X>,
        event: E,
    ): AfsmTransition<AfsmSnapshot<P, X>, A, F>
}
