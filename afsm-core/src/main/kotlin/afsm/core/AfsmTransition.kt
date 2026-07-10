package afsm.core

/**
 * Result of reducing one `state + event` pair.
 *
 * Construct transitions through [Afsm] helpers or the factory functions on
 * [Companion]. Direct construction is intentionally unavailable so ignored and
 * invalid decisions cannot accidentally carry commands, effects, or changed
 * state output. Ordinary [commands] and phase-owned [commandInvocations] stay
 * separate so their execution and cancellation policies cannot be confused.
 */
public class AfsmTransition<out S : Any, out C : Any, out F : Any> private constructor(
    public val state: S,
    public val commands: List<C>,
    public val effects: List<F>,
    public val commandInvocations: List<AfsmCommandInvocation<C>>,
    public val decision: AfsmDecision,
) {
    public companion object {
        public fun <S : Any, C : Any, F : Any> transitioned(
            state: S,
            commands: List<C> = emptyList(),
            effects: List<F> = emptyList(),
            commandInvocations: List<AfsmCommandInvocation<C>> = emptyList(),
        ): AfsmTransition<S, C, F> {
            return AfsmTransition(
                state = state,
                commands = commands,
                effects = effects,
                commandInvocations = commandInvocations,
                decision = AfsmDecision.Transitioned,
            )
        }

        public fun <S : Any, C : Any, F : Any> handled(
            state: S,
            commands: List<C> = emptyList(),
            effects: List<F> = emptyList(),
            reason: String? = null,
            commandInvocations: List<AfsmCommandInvocation<C>> = emptyList(),
        ): AfsmTransition<S, C, F> {
            return AfsmTransition(
                state = state,
                commands = commands,
                effects = effects,
                commandInvocations = commandInvocations,
                decision = AfsmDecision.Handled(reason),
            )
        }

        public fun <S : Any, C : Any, F : Any> ignored(
            state: S,
            reason: String? = null,
        ): AfsmTransition<S, C, F> {
            return AfsmTransition(
                state = state,
                commands = emptyList(),
                effects = emptyList(),
                commandInvocations = emptyList(),
                decision = AfsmDecision.Ignored(reason),
            )
        }

        public fun <S : Any, C : Any, F : Any> invalid(
            state: S,
            reason: String? = null,
        ): AfsmTransition<S, C, F> {
            return AfsmTransition(
                state = state,
                commands = emptyList(),
                effects = emptyList(),
                commandInvocations = emptyList(),
                decision = AfsmDecision.Invalid(reason),
            )
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AfsmTransition<*, *, *>) return false

        return state == other.state &&
            commands == other.commands &&
            effects == other.effects &&
            commandInvocations == other.commandInvocations &&
            decision == other.decision
    }

    override fun hashCode(): Int {
        var result = state.hashCode()
        result = 31 * result + commands.hashCode()
        result = 31 * result + effects.hashCode()
        result = 31 * result + commandInvocations.hashCode()
        result = 31 * result + decision.hashCode()
        return result
    }

    override fun toString(): String {
        return "AfsmTransition(state=$state, commands=$commands, effects=$effects, commandInvocations=$commandInvocations, decision=$decision)"
    }
}
