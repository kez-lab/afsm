package afsm.core

/**
 * Result of reducing one `state + event` pair.
 *
 * Construct transitions through [Afsm] helpers or the factory functions on
 * [Companion]. Direct construction is intentionally unavailable so ignored and
 * invalid decisions cannot accidentally carry commands or changed
 * state output. Ordinary [commands] and phase-owned [commandInvocations] stay
 * separate so their execution and cancellation policies cannot be confused.
 */
public class AfsmTransition<out S : Any, out C : Any> private constructor(
    public val state: S,
    public val commands: List<C>,
    public val commandInvocations: List<AfsmCommandInvocation<C>>,
    public val decision: AfsmDecision,
) {
    public companion object {
        public fun <S : Any, C : Any> transitioned(
            state: S,
            commands: List<C> = emptyList(),
            commandInvocations: List<AfsmCommandInvocation<C>> = emptyList(),
        ): AfsmTransition<S, C> {
            return AfsmTransition(
                state = state,
                commands = commands,
                commandInvocations = commandInvocations,
                decision = AfsmDecision.Transitioned,
            )
        }

        public fun <S : Any, C : Any> handled(
            state: S,
            commands: List<C> = emptyList(),
            reason: String? = null,
            commandInvocations: List<AfsmCommandInvocation<C>> = emptyList(),
        ): AfsmTransition<S, C> {
            return AfsmTransition(
                state = state,
                commands = commands,
                commandInvocations = commandInvocations,
                decision = AfsmDecision.Handled(reason),
            )
        }

        public fun <S : Any, C : Any> ignored(
            state: S,
            reason: String? = null,
        ): AfsmTransition<S, C> {
            return AfsmTransition(
                state = state,
                commands = emptyList(),
                commandInvocations = emptyList(),
                decision = AfsmDecision.Ignored(reason),
            )
        }

        public fun <S : Any, C : Any> invalid(
            state: S,
            reason: String? = null,
        ): AfsmTransition<S, C> {
            return AfsmTransition(
                state = state,
                commands = emptyList(),
                commandInvocations = emptyList(),
                decision = AfsmDecision.Invalid(reason),
            )
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AfsmTransition<*, *>) return false

        return state == other.state &&
            commands == other.commands &&
            commandInvocations == other.commandInvocations &&
            decision == other.decision
    }

    override fun hashCode(): Int {
        var result = state.hashCode()
        result = 31 * result + commands.hashCode()
        result = 31 * result + commandInvocations.hashCode()
        result = 31 * result + decision.hashCode()
        return result
    }

    override fun toString(): String {
        return "AfsmTransition(state=$state, commands=$commands, commandInvocations=$commandInvocations, decision=$decision)"
    }
}
