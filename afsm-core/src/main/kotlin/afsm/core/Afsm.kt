package afsm.core

/**
 * Low-level transition factory helpers for custom [AfsmReducer]
 * implementations.
 *
 * Most Android feature code should start with the graphable `afsmMachine { ... }`
 * DSL instead. In that DSL, use `transitionTo(...)`, `updateData(...)`,
 * `ignore(...)`, and `invalid(...)` from the scoped builder so runtime behavior
 * and graph topology stay in one declaration.
 */
public object Afsm {
    /**
     * Builds an accepted transition result for a custom reducer.
     *
     * [state] must be the reducer's next state. [commands] are ordinary
     * sequential host work, [commandInvocations] are keyed phase-owned work,
     * and [effects] are UI output. Graphable DSL code should use
     * `transitionTo(...)`, `command(...)`, `invoke(...)`, and `effect(...)`
     * instead.
     */
    public fun <S : Any, C : Any, F : Any> transitioned(
        state: S,
        commands: List<C> = emptyList(),
        effects: List<F> = emptyList(),
        commandInvocations: List<AfsmCommandInvocation<C>> = emptyList(),
    ): AfsmTransition<S, C, F> {
        return AfsmTransition.transitioned(
            state = state,
            commands = commands,
            effects = effects,
            commandInvocations = commandInvocations,
        )
    }

    /**
     * Builds an expected no-op result for a custom reducer.
     *
     * Use this for harmless duplicate or stale events. Graphable DSL code should
     * usually omit impossible event handlers, and use scoped `ignore(...)` only
     * for expected no-op events that are worth documenting.
     */
    public fun <S : Any, C : Any, F : Any> ignore(
        state: S,
        reason: String? = null,
    ): AfsmTransition<S, C, F> {
        return AfsmTransition.ignored(
            state = state,
            reason = reason,
        )
    }

    /**
     * Builds an invalid-transition result for a custom reducer.
     *
     * Hosts apply their configured invalid-transition policy to this decision.
     * Graphable DSL code should normally rely on omitted handlers being invalid
     * by default, and use scoped `invalid(...)` only when an explicit diagnostic
     * reason is useful.
     */
    public fun <S : Any, C : Any, F : Any> invalid(
        state: S,
        reason: String? = null,
    ): AfsmTransition<S, C, F> {
        return AfsmTransition.invalid(
            state = state,
            reason = reason,
        )
    }
}
