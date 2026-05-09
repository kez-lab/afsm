package afsm.core

import kotlin.reflect.KClass

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
            label = afsmLabelForValue(phase),
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
            label = afsmLabelForClass(PS::class),
            matcher = { phase -> phase as? PS },
            build = build,
        )
    }

    @PublishedApi
    internal fun <PS : P> addState(
        label: String,
        matcher: (P) -> PS?,
        build: AfsmStateBuilder<P, X, E, A, F, PS>.() -> Unit,
    ) {
        val builder = AfsmStateBuilder<P, X, E, A, F, PS>(
            stateLabel = label,
            matcher = matcher,
        )
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
    private val stateLabel: String,
    private val matcher: (P) -> PS?,
) {
    private val entryHandlers = mutableListOf<AfsmEntryHandler<P, X, A, F>>()
    private val eventDefinitions = mutableListOf<AfsmEventDefinition<P, X, E, A, F>>()

    /**
     * Runs when this phase is entered through an event branch transition.
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
     * Declares graphable branches for events whose runtime type is [EV].
     */
    public inline fun <reified EV : E> on(
        noinline build: AfsmEventBranchScope<P, X, E, A, F, PS, EV>.() -> Unit,
    ) {
        addEventDefinition(
            eventLabel = afsmLabelForClass(EV::class),
            eventMatcher = { event -> event as? EV },
            build = build,
        )
    }

    @PublishedApi
    internal fun <EV : E> addEventDefinition(
        eventLabel: String,
        eventMatcher: (E) -> EV?,
        build: AfsmEventBranchScope<P, X, E, A, F, PS, EV>.() -> Unit,
    ) {
        val builder = AfsmEventBranchScope<P, X, E, A, F, PS, EV>(
            stateLabel = stateLabel,
            eventLabel = eventLabel,
            phaseMatcher = matcher,
            eventMatcher = eventMatcher,
        )
        builder.build()
        eventDefinitions += builder.buildDefinition()
    }

    internal fun buildDefinition(): AfsmStateDefinition<P, X, E, A, F> {
        return AfsmStateDefinition(
            label = stateLabel,
            matcher = matcher,
            entryHandlers = entryHandlers.toList(),
            eventDefinitions = eventDefinitions.toList(),
        )
    }
}

public class AfsmEventBranchScope<P : Any, X : Any, E : Any, A : Any, F : Any, PS : P, EV : E> internal constructor(
    private val stateLabel: String,
    private val eventLabel: String,
    private val phaseMatcher: (P) -> PS?,
    private val eventMatcher: (E) -> EV?,
) {
    private val branches = mutableListOf<AfsmEventBranch<P, X, E, A, F>>()
    private val transitions = mutableListOf<AfsmTopologyTransition>()

    /**
     * Declares a branch that transitions to a concrete phase value.
     */
    public fun transitionTo(
        phase: P,
        guard: AfsmTransitionScope<P, X, E, A, F, PS, EV>.() -> Boolean = { true },
        block: AfsmTransitionScope<P, X, E, A, F, PS, EV>.() -> Unit = {},
    ) {
        addBranch(
            targetLabel = afsmLabelForValue(phase),
            targetFactory = { phase },
            guard = guard,
            block = block,
        )
    }

    /**
     * Declares a branch that transitions to a payload phase created from runtime data.
     */
    public inline fun <reified TP : P> transitionTo(
        noinline guard: AfsmTransitionScope<P, X, E, A, F, PS, EV>.() -> Boolean = { true },
        noinline phase: AfsmTransitionScope<P, X, E, A, F, PS, EV>.() -> TP,
        noinline block: AfsmTransitionScope<P, X, E, A, F, PS, EV>.() -> Unit = {},
    ) {
        addBranch(
            targetLabel = afsmLabelForClass(TP::class),
            targetFactory = phase,
            guard = guard,
            block = block,
        )
    }

    /**
     * Declares a branch that handles the event without changing the finite phase.
     */
    public fun stay(
        guard: AfsmTransitionScope<P, X, E, A, F, PS, EV>.() -> Boolean = { true },
        block: AfsmTransitionScope<P, X, E, A, F, PS, EV>.() -> Unit = {},
    ) {
        addBranch(
            targetLabel = stateLabel,
            targetFactory = { null },
            guard = guard,
            block = block,
        )
    }

    /**
     * Declares a final stayed branch for unmatched guards in this event handler.
     */
    public fun otherwise(
        block: AfsmTransitionScope<P, X, E, A, F, PS, EV>.() -> Unit = {},
    ) {
        addBranch(
            targetLabel = stateLabel,
            eventLabelOverride = "$eventLabel [otherwise]",
            targetFactory = { null },
            guard = { true },
            block = block,
        )
    }

    /**
     * Declares a handled event that should be ignored without changing state or graph topology.
     */
    public fun ignore(
        reason: String? = null,
        guard: AfsmTransitionScope<P, X, E, A, F, PS, EV>.() -> Boolean = { true },
    ) {
        addDecisionBranch(
            decision = AfsmDecision.Ignored(reason),
            guard = guard,
        )
    }

    /**
     * Declares a handled event that represents an invalid transition without changing graph topology.
     */
    public fun invalid(
        reason: String? = null,
        guard: AfsmTransitionScope<P, X, E, A, F, PS, EV>.() -> Boolean = { true },
    ) {
        addDecisionBranch(
            decision = AfsmDecision.Invalid(reason),
            guard = guard,
        )
    }

    @PublishedApi
    internal fun addBranch(
        targetLabel: String,
        eventLabelOverride: String = eventLabel,
        targetFactory: AfsmTransitionScope<P, X, E, A, F, PS, EV>.() -> P?,
        guard: AfsmTransitionScope<P, X, E, A, F, PS, EV>.() -> Boolean,
        block: AfsmTransitionScope<P, X, E, A, F, PS, EV>.() -> Unit,
    ) {
        transitions += AfsmTopologyTransition(
            from = stateLabel,
            event = eventLabelOverride,
            to = targetLabel,
        )
        branches += AfsmEventBranch { currentPhase, currentEvent, execution ->
            val typedPhase = phaseMatcher(currentPhase) ?: return@AfsmEventBranch AfsmBranchResult.Unmatched
            val typedEvent = eventMatcher(currentEvent) ?: return@AfsmEventBranch AfsmBranchResult.Unmatched
            val scope = AfsmTransitionScope<P, X, E, A, F, PS, EV>(
                phase = typedPhase,
                event = typedEvent,
                execution = execution,
            )

            if (!scope.guard()) {
                return@AfsmEventBranch AfsmBranchResult.Unmatched
            }

            scope.block()

            AfsmBranchResult.Matched(
                targetPhase = scope.targetFactory(),
                decision = null,
            )
        }
    }

    @PublishedApi
    internal fun addDecisionBranch(
        decision: AfsmDecision,
        guard: AfsmTransitionScope<P, X, E, A, F, PS, EV>.() -> Boolean,
    ) {
        branches += AfsmEventBranch { currentPhase, currentEvent, execution ->
            val typedPhase = phaseMatcher(currentPhase) ?: return@AfsmEventBranch AfsmBranchResult.Unmatched
            val typedEvent = eventMatcher(currentEvent) ?: return@AfsmEventBranch AfsmBranchResult.Unmatched
            val scope = AfsmTransitionScope<P, X, E, A, F, PS, EV>(
                phase = typedPhase,
                event = typedEvent,
                execution = execution,
            )

            if (!scope.guard()) {
                return@AfsmEventBranch AfsmBranchResult.Unmatched
            }

            AfsmBranchResult.Matched(
                targetPhase = null,
                decision = decision,
            )
        }
    }

    internal fun buildDefinition(): AfsmEventDefinition<P, X, E, A, F> {
        return AfsmEventDefinition(
            eventLabel = eventLabel,
            eventMatcher = { event -> eventMatcher(event) != null },
            branches = branches.toList(),
            transitions = transitions.toList(),
        )
    }
}

public class AfsmEntryScope<P : Any, X : Any, A : Any, F : Any, PS : P> internal constructor(
    public val phase: PS,
    private val execution: AfsmDslExecution<P, X, A, F>,
) {
    /**
     * Current extended context after previous transition context updates.
     */
    public val context: X
        get() = execution.context

    /**
     * Replaces the current context with an immutable copy.
     */
    public fun updateContext(update: X.() -> X) {
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

public class AfsmTransitionScope<P : Any, X : Any, E : Any, A : Any, F : Any, PS : P, EV : E> internal constructor(
    public val phase: PS,
    public val event: EV,
    private val execution: AfsmDslExecution<P, X, A, F>,
) {
    /**
     * Current extended context after previous context updates in this transition.
     */
    public val context: X
        get() = execution.context

    /**
     * Replaces the current context with an immutable copy.
     */
    public fun updateContext(update: X.() -> X) {
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

internal data class AfsmStateDefinition<P : Any, X : Any, E : Any, A : Any, F : Any>(
    val label: String,
    val matcher: (P) -> Any?,
    val entryHandlers: List<AfsmEntryHandler<P, X, A, F>>,
    val eventDefinitions: List<AfsmEventDefinition<P, X, E, A, F>>,
)

internal data class AfsmEventDefinition<P : Any, X : Any, E : Any, A : Any, F : Any>(
    val eventLabel: String,
    val eventMatcher: (E) -> Boolean,
    val branches: List<AfsmEventBranch<P, X, E, A, F>>,
    val transitions: List<AfsmTopologyTransition>,
)

internal fun interface AfsmEventBranch<P : Any, X : Any, E : Any, A : Any, F : Any> {
    fun tryHandle(
        phase: P,
        event: E,
        execution: AfsmDslExecution<P, X, A, F>,
    ): AfsmBranchResult<P>
}

internal sealed interface AfsmBranchResult<out P : Any> {
    data object Unmatched : AfsmBranchResult<Nothing>

    data class Matched<P : Any>(
        val targetPhase: P?,
        val decision: AfsmDecision?,
    ) : AfsmBranchResult<P>
}

internal typealias AfsmEntryHandler<P, X, A, F> =
    (phase: P, execution: AfsmDslExecution<P, X, A, F>) -> Unit

private class AfsmDslMachine<P : Any, X : Any, E : Any, A : Any, F : Any>(
    override val initialSnapshot: AfsmSnapshot<P, X>,
    private val states: List<AfsmStateDefinition<P, X, E, A, F>>,
) : AfsmMachine<P, X, E, A, F> {
    override val topology: AfsmTopology = AfsmTopology(
        states = states.map { state ->
            AfsmTopologyState(id = state.label)
        },
        transitions = states.flatMap { state ->
            state.eventDefinitions.flatMap { eventDefinition ->
                eventDefinition.transitions
            }
        }.distinct(),
    )

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

        val eventDefinition = state.eventDefinitions.firstOrNull { definition ->
            definition.eventMatcher(event)
        } ?: return Afsm.invalid(
            state = snapshot,
            reason = "No event handler matched the current phase and event.",
        )

        val execution = AfsmDslExecution<P, X, A, F>(
            context = snapshot.context,
        )

        val branchResult = eventDefinition.branches.firstNotNullOfOrNull { branch ->
            when (val result = branch.tryHandle(snapshot.phase, event, execution)) {
                AfsmBranchResult.Unmatched -> null
                is AfsmBranchResult.Matched -> result
            }
        } ?: return Afsm.invalid(
            state = snapshot,
            reason = "No branch matched the current phase and event.",
        )

        val targetPhase = branchResult.targetPhase
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
            decision = branchResult.decision ?: if (targetPhase != null) {
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
    val actions: MutableList<A> = mutableListOf(),
    val effects: MutableList<F> = mutableListOf(),
)

@PublishedApi
internal fun afsmLabelForValue(value: Any): String {
    return value::class.simpleName ?: value.toString()
}

@PublishedApi
internal fun afsmLabelForClass(kClass: KClass<*>): String {
    return kClass.simpleName ?: kClass.toString()
}
