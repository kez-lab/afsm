package afsm.runtime

public fun interface AfsmCommandHandler<C : Any, E : Any> {
    /**
     * Executes one command emitted by a state machine transition.
     *
     * Command results should be converted back into events through
     * [dispatchEvent]. The runtime queues dispatched events instead of
     * re-entering the state machine recursively. The same handler executes
     * phase-owned invocations; those implementations must cooperate with
     * coroutine cancellation.
     */
    public suspend fun handle(
        command: C,
        dispatchEvent: suspend (E) -> Unit,
    )

    public companion object {
        /**
         * Returns a command handler that intentionally ignores emitted commands.
         *
         * Use this only for machines that never emit commands. If a machine can
         * emit repository, database, timer, or SDK work, pass an explicit
         * `AfsmCommandHandler` or Kotlin SAM lambda to execute that work and
         * dispatch typed result events.
         */
        public fun <C : Any, E : Any> none(): AfsmCommandHandler<C, E> {
            return AfsmCommandHandler { _, _ -> }
        }
    }
}
