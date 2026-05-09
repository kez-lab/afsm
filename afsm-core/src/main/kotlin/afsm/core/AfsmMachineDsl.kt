package afsm.core

/**
 * Builds an executable statechart-style Afsm machine.
 *
 * This DSL is intentionally plain Kotlin and Android-free. Android ViewModels
 * should host the returned [AfsmMachine] and execute emitted commands.
 */
public fun <P : Any, X : Any, E : Any, A : Any, F : Any> afsmMachine(
    build: AfsmMachineBuilder<P, X, E, A, F>.() -> Unit,
): AfsmMachine<P, X, E, A, F> {
    val builder = AfsmMachineBuilder<P, X, E, A, F>()
    builder.build()
    return builder.buildMachine()
}

public class AfsmMachineBuilder<P : Any, X : Any, E : Any, A : Any, F : Any> {
    private var initialSnapshot: AfsmSnapshot<P, X>? = null
    private val states = mutableListOf<AfsmStateDefinition<P, X, E, A, F>>()

    /**
     * Sets the initial finite phase and extended context for the machine.
     */
    public fun initial(
        phase: P,
        context: X,
    ) {
        initialSnapshot = AfsmSnapshot(
            phase = phase,
            context = context,
        )
    }

    /**
     * Declares behavior for an exact phase value, typically a data object phase.
     */
    public fun state(
        phase: P,
        build: AfsmStateBuilder<P, X, E, A, F, P>.() -> Unit,
    ) {
        addState(
            matcher = { currentPhase ->
                if (currentPhase == phase) currentPhase else null
            },
            build = build,
        )
    }

    /**
     * Declares behavior for any phase instance of [PS], typically a payload phase.
     */
    public inline fun <reified PS : P> state(
        noinline build: AfsmStateBuilder<P, X, E, A, F, PS>.() -> Unit,
    ) {
        addState(
            matcher = { phase -> phase as? PS },
            build = build,
        )
    }

    @PublishedApi
    internal fun <PS : P> addState(
        matcher: (P) -> PS?,
        build: AfsmStateBuilder<P, X, E, A, F, PS>.() -> Unit,
    ) {
        val builder = AfsmStateBuilder<P, X, E, A, F, PS>(matcher)
        builder.build()
        states += builder.buildDefinition()
    }

    internal fun buildMachine(): AfsmMachine<P, X, E, A, F> {
        val initial = requireNotNull(initialSnapshot) {
            "Afsm machine requires an initial phase and context."
        }

        return AfsmDslMachine(
            initialSnapshot = initial,
            states = states.toList(),
        )
    }
}

public class AfsmStateBuilder<P : Any, X : Any, E : Any, A : Any, F : Any, PS : P> internal constructor(
    private val matcher: (P) -> PS?,
) {
    private val entryHandlers = mutableListOf<AfsmEntryHandler<P, X, A, F>>()
    private val eventHandlers = mutableListOf<AfsmEventHandler<P, X, E, A, F>>()

    /**
     * Runs when this phase is entered through [AfsmEventScope.transitionTo].
     */
    public fun onEnter(
        handler: AfsmEntryScope<P, X, A, F, PS>.() -> Unit,
    ) {
        entryHandlers += { phase, execution ->
            val typedPhase = matcher(phase)
            if (typedPhase != null) {
                AfsmEntryScope<P, X, A, F, PS>(
                    phase = typedPhase,
                    execution = execution,
                ).handler()
            }
        }
    }

    /**
     * Handles events whose runtime type is [EV] while the machine is in this phase.
     */
    public inline fun <reified EV : E> on(
        noinline handler: AfsmEventScope<P, X, E, A, F, PS, EV>.() -> Unit,
    ) {
        addEventHandler(
            eventMatcher = { event -> event as? EV },
            handler = handler,
        )
    }

    @PublishedApi
    internal fun <EV : E> addEventHandler(
        eventMatcher: (E) -> EV?,
        handler: AfsmEventScope<P, X, E, A, F, PS, EV>.() -> Unit,
    ) {
        eventHandlers += { phase, event, execution ->
            val typedPhase = matcher(phase)
            val typedEvent = eventMatcher(event)

            if (typedPhase != null && typedEvent != null) {
                AfsmEventScope<P, X, E, A, F, PS, EV>(
                    phase = typedPhase,
                    event = typedEvent,
                    execution = execution,
                ).handler()
                true
            } else {
                false
            }
        }
    }

    internal fun buildDefinition(): AfsmStateDefinition<P, X, E, A, F> {
        return AfsmStateDefinition(
            matcher = matcher,
            entryHandlers = entryHandlers.toList(),
            eventHandlers = eventHandlers.toList(),
        )
    }
}

public class AfsmEntryScope<P : Any, X : Any, A : Any, F : Any, PS : P> internal constructor(
    public val phase: PS,
    private val execution: AfsmDslExecution<P, X, A, F>,
) {
    /**
     * Current extended context after previous transition assignments.
     */
    public val context: X
        get() = execution.context

    /**
     * Replaces the current context with an immutable copy.
     */
    public fun assign(update: X.() -> X) {
        execution.context = execution.context.update()
    }

    /**
     * Emits host-executed work, such as a repository call or timer start.
     */
    public fun action(action: A) {
        execution.actions += action
    }

    /**
     * Emits a UI-side one-shot effect.
     */
    public fun effect(effect: F) {
        execution.effects += effect
    }
}

public class AfsmEventScope<P : Any, X : Any, E : Any, A : Any, F : Any, PS : P, EV : E> internal constructor(
    public val phase: PS,
    public val event: EV,
    private val execution: AfsmDslExecution<P, X, A, F>,
) {
    private var guardEvaluated = false
    private var guardMatched = false

    /**
     * Current extended context after previous assignments in this transition.
     */
    public val context: X
        get() = execution.context

    /**
     * Replaces the current context with an immutable copy.
     */
    public fun assign(update: X.() -> X) {
        execution.context = execution.context.update()
    }

    /**
     * Selects the next finite phase. The target phase's entry handlers run after
     * this event handler finishes.
     */
    public fun transitionTo(phase: P) {
        execution.targetPhase = phase
    }

    /**
     * Emits host-executed work, such as a repository call or timer start.
     */
    public fun action(action: A) {
        execution.actions += action
    }

    /**
     * Emits a UI-side one-shot effect.
     */
    public fun effect(effect: F) {
        execution.effects += effect
    }

    /**
     * Runs [block] only when [predicate] matches and no earlier guard matched.
     */
    public fun guard(
        predicate: AfsmEventScope<P, X, E, A, F, PS, EV>.() -> Boolean,
        block: AfsmEventScope<P, X, E, A, F, PS, EV>.() -> Unit,
    ) {
        guardEvaluated = true
        if (!guardMatched && predicate()) {
            guardMatched = true
            block()
        }
    }

    /**
     * Runs when no guard matched in this event handler.
     */
    public fun otherwise(
        block: AfsmEventScope<P, X, E, A, F, PS, EV>.() -> Unit,
    ) {
        if (!guardEvaluated || !guardMatched) {
            block()
        }
    }
}

internal data class AfsmStateDefinition<P : Any, X : Any, E : Any, A : Any, F : Any>(
    val matcher: (P) -> Any?,
    val entryHandlers: List<AfsmEntryHandler<P, X, A, F>>,
    val eventHandlers: List<AfsmEventHandler<P, X, E, A, F>>,
)

internal typealias AfsmEntryHandler<P, X, A, F> =
    (phase: P, execution: AfsmDslExecution<P, X, A, F>) -> Unit

internal typealias AfsmEventHandler<P, X, E, A, F> =
    (phase: P, event: E, execution: AfsmDslExecution<P, X, A, F>) -> Boolean

private class AfsmDslMachine<P : Any, X : Any, E : Any, A : Any, F : Any>(
    override val initialSnapshot: AfsmSnapshot<P, X>,
    private val states: List<AfsmStateDefinition<P, X, E, A, F>>,
) : AfsmMachine<P, X, E, A, F> {
    override fun transition(
        snapshot: AfsmSnapshot<P, X>,
        event: E,
    ): AfsmTransition<AfsmSnapshot<P, X>, A, F> {
        val state = states.firstOrNull { definition ->
            definition.matcher(snapshot.phase) != null
        } ?: return Afsm.invalid(
            state = snapshot,
            reason = "No state definition matched the current phase.",
        )

        val execution = AfsmDslExecution<P, X, A, F>(
            context = snapshot.context,
        )

        val handled = state.eventHandlers.any { handler ->
            handler(snapshot.phase, event, execution)
        }

        if (!handled) {
            return Afsm.invalid(
                state = snapshot,
                reason = "No event handler matched the current phase and event.",
            )
        }

        val targetPhase = execution.targetPhase
        if (targetPhase != null) {
            applyEntryHandlers(
                targetPhase = targetPhase,
                execution = execution,
            )
        }

        val nextSnapshot = AfsmSnapshot(
            phase = targetPhase ?: snapshot.phase,
            context = execution.context,
        )

        return AfsmTransition(
            state = nextSnapshot,
            commands = execution.actions.toList(),
            effects = execution.effects.toList(),
            decision = if (targetPhase != null) {
                AfsmDecision.Transitioned
            } else {
                AfsmDecision.Stayed()
            },
        )
    }

    private fun applyEntryHandlers(
        targetPhase: P,
        execution: AfsmDslExecution<P, X, A, F>,
    ) {
        val targetState = states.firstOrNull { definition ->
            definition.matcher(targetPhase) != null
        } ?: return

        targetState.entryHandlers.forEach { handler ->
            handler(targetPhase, execution)
        }
    }
}

internal class AfsmDslExecution<P : Any, X : Any, A : Any, F : Any>(
    var context: X,
    var targetPhase: P? = null,
    val actions: MutableList<A> = mutableListOf(),
    val effects: MutableList<F> = mutableListOf(),
)
