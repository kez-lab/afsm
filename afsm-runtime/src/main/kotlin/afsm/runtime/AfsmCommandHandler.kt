package afsm.runtime

public fun interface AfsmCommandHandler<C : Any, E : Any> {
    /**
     * Executes one command emitted by a state machine transition.
     *
     * Command results should be converted back into events through [dispatch].
     * The runtime queues dispatched events instead of re-entering the state
     * machine recursively.
     */
    public suspend fun handle(
        command: C,
        dispatch: suspend (E) -> Unit,
    )

    public companion object {
        public fun <C : Any, E : Any> none(): AfsmCommandHandler<C, E> {
            return AfsmCommandHandler { _, _ -> }
        }
    }
}
