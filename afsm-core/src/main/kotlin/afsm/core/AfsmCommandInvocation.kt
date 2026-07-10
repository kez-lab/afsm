package afsm.core

/**
 * Lifecycle operation for long-running work owned by a machine phase.
 *
 * Ordinary [AfsmTransition.commands] remain sequential host work. Invocation
 * starts run in tracked child jobs and cancellation is applied directly by the
 * runtime when the owning phase exits.
 */
public sealed interface AfsmCommandInvocation<out C : Any> {
    public val key: AfsmInvocationKey

    public data class Start<C : Any>(
        override val key: AfsmInvocationKey,
        public val command: C,
    ) : AfsmCommandInvocation<C>

    public data class Cancel(
        override val key: AfsmInvocationKey,
    ) : AfsmCommandInvocation<Nothing>
}
