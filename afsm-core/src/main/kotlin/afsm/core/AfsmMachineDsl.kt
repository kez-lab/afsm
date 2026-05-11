package afsm.core

import kotlin.reflect.KClass

/**
 * Builds an executable Afsm machine definition.
 *
 * This DSL is intentionally plain Kotlin and Android-free. Android ViewModels
 * should host the returned [AfsmMachine] through an [AfsmReducer].
 */
public fun <P : Any, X : Any, E : Any, C : Any, F : Any> afsmMachine(
    build: AfsmMachineBuilder<P, X, E, C, F>.() -> Unit,
): AfsmMachine<P, X, E, C, F> {
    val builder = AfsmMachineBuilder<P, X, E, C, F>()
    builder.build()
    return builder.buildMachine()
}

public class AfsmMachineBuilder<P : Any, X : Any, E : Any, C : Any, F : Any> {
    private var initialState: AfsmState<P, X>? = null
    private val states = mutableListOf<AfsmStateDefinition<P, X, E, C, F>>()

    /**
     * Sets the initial finite phase and extended context for the chart.
     */
    public fun initial(
        phase: P,
        context: X,
    ) {
        initialState = AfsmState(
            phase = phase,
            context = context,
        )
    }

    /**
     * Declares behavior for an exact phase value, typically a data object phase.
     */
    public fun state(
        phase: P,
        build: AfsmStateBuilder<P, X, E, C, F, P>.() -> Unit,
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
        noinline build: AfsmStateBuilder<P, X, E, C, F, PS>.() -> Unit,
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
        build: AfsmStateBuilder<P, X, E, C, F, PS>.() -> Unit,
    ) {
        val builder = AfsmStateBuilder<P, X, E, C, F, PS>(
            stateLabel = label,
            matcher = matcher,
        )
        builder.build()
        states += builder.buildDefinition()
    }

    internal fun buildMachine(): AfsmMachine<P, X, E, C, F> {
        val initial = requireNotNull(initialState) {
            "Afsm machine requires an initial phase and context."
        }
        val builtStates = states.toList()

        validateDefinitions(
            initial = initial,
            states = builtStates,
        )

        return AfsmDslMachine(
            initialState = initial,
            states = builtStates,
        )
    }

    private fun validateDefinitions(
        initial: AfsmState<P, X>,
        states: List<AfsmStateDefinition<P, X, E, C, F>>,
    ) {
        val errors = mutableListOf<String>()

        if (states.isEmpty()) {
            errors += "At least one state must be declared."
        }

        states.groupBy { it.label }
            .filterValues { it.size > 1 }
            .keys
            .forEach { label ->
                errors += "Duplicate state declaration: $label."
            }

        if (states.none { it.matcher(initial.phase) != null }) {
            errors += "Initial phase ${afsmLabelForValue(initial.phase)} has no matching state declaration."
        }

        val stateLabels = states.map { it.label }.toSet()
        states.forEach { state ->
            state.eventDefinitions
                .groupBy { it.eventLabel }
                .filterValues { it.size > 1 }
                .keys
                .forEach { eventLabel ->
                    errors += "Duplicate event handler in ${state.label}: $eventLabel."
                }

            state.eventDefinitions
                .flatMap { it.transitions }
                .filter { transition -> transition.to !in stateLabels }
                .forEach { transition ->
                    errors += "Transition ${transition.from} -- ${transition.event} --> ${transition.to} targets an undeclared state."
                }
        }

        if (errors.isNotEmpty()) {
            throw AfsmDefinitionException(
                errors.joinToString(
                    separator = "\n",
                    prefix = "Invalid Afsm machine definition:\n",
                ),
            )
        }
    }
}

public class AfsmStateBuilder<P : Any, X : Any, E : Any, C : Any, F : Any, PS : P> internal constructor(
    private val stateLabel: String,
    private val matcher: (P) -> PS?,
) {
    private val entryHandlers = mutableListOf<AfsmEntryHandler<P, X, C, F>>()
    private val exitHandlers = mutableListOf<AfsmExitHandler<P, X, C, F>>()
    private val eventDefinitions = mutableListOf<AfsmEventDefinition<P, X, E, C, F>>()

    /**
     * Runs when this phase is entered through an event branch transition.
     */
    public fun onEnter(
        handler: AfsmEntryScope<P, X, C, F, PS>.() -> Unit,
    ) {
        entryHandlers += { phase, execution ->
            val typedPhase = matcher(phase)
            if (typedPhase != null) {
                AfsmEntryScope<P, X, C, F, PS>(
                    phase = typedPhase,
                    execution = execution,
                ).handler()
            }
        }
    }

    /**
     * Runs when this phase is exited by a phase-changing transition.
     */
    public fun onExit(
        handler: AfsmExitScope<P, X, C, F, PS>.() -> Unit,
    ) {
        exitHandlers += { phase, execution ->
            val typedPhase = matcher(phase)
            if (typedPhase != null) {
                AfsmExitScope<P, X, C, F, PS>(
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
        noinline build: AfsmEventBranchScope<P, X, E, C, F, PS, EV>.() -> Unit,
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
        build: AfsmEventBranchScope<P, X, E, C, F, PS, EV>.() -> Unit,
    ) {
        val builder = AfsmEventBranchScope<P, X, E, C, F, PS, EV>(
            stateLabel = stateLabel,
            eventLabel = eventLabel,
            phaseMatcher = matcher,
            eventMatcher = eventMatcher,
        )
        builder.build()
        eventDefinitions += builder.buildDefinition()
    }

    internal fun buildDefinition(): AfsmStateDefinition<P, X, E, C, F> {
        return AfsmStateDefinition(
            label = stateLabel,
            matcher = matcher,
            entryHandlers = entryHandlers.toList(),
            exitHandlers = exitHandlers.toList(),
            eventDefinitions = eventDefinitions.toList(),
        )
    }
}

public class AfsmEventBranchScope<P : Any, X : Any, E : Any, C : Any, F : Any, PS : P, EV : E> internal constructor(
    private val stateLabel: String,
    private val eventLabel: String,
    private val phaseMatcher: (P) -> PS?,
    private val eventMatcher: (E) -> EV?,
) {
    private val branches = mutableListOf<AfsmEventBranch<P, X, E, C, F>>()
    private val transitions = mutableListOf<AfsmTopologyTransition>()

    /**
     * Declares a branch that transitions to a concrete phase value.
     */
    public fun transitionTo(
        phase: P,
        guardLabel: String? = null,
        commandLabels: List<String> = emptyList(),
        effectLabels: List<String> = emptyList(),
        guard: AfsmTransitionScope<P, X, E, C, F, PS, EV>.() -> Boolean = { true },
        block: AfsmTransitionScope<P, X, E, C, F, PS, EV>.() -> Unit = {},
    ) {
        addBranch(
            targetLabel = afsmLabelForValue(phase),
            targetFactory = { phase },
            guardLabel = guardLabel,
            commandLabels = commandLabels,
            effectLabels = effectLabels,
            kind = AfsmTopologyTransitionKind.External,
            isFallback = false,
            guard = guard,
            block = block,
        )
    }

    /**
     * Declares a branch that transitions to a payload phase created from runtime data.
     */
    public inline fun <reified TP : P> transitionTo(
        noinline phase: AfsmTransitionScope<P, X, E, C, F, PS, EV>.() -> TP,
        guardLabel: String? = null,
        commandLabels: List<String> = emptyList(),
        effectLabels: List<String> = emptyList(),
        noinline guard: AfsmTransitionScope<P, X, E, C, F, PS, EV>.() -> Boolean = { true },
        noinline block: AfsmTransitionScope<P, X, E, C, F, PS, EV>.() -> Unit = {},
    ) {
        addBranch(
            targetLabel = afsmLabelForClass(TP::class),
            targetFactory = phase,
            guardLabel = guardLabel,
            commandLabels = commandLabels,
            effectLabels = effectLabels,
            kind = AfsmTopologyTransitionKind.External,
            isFallback = false,
            guard = guard,
            block = block,
        )
    }

    /**
     * Declares a branch that handles the event without changing the finite phase.
     */
    public fun stay(
        guardLabel: String? = null,
        commandLabels: List<String> = emptyList(),
        effectLabels: List<String> = emptyList(),
        guard: AfsmTransitionScope<P, X, E, C, F, PS, EV>.() -> Boolean = { true },
        block: AfsmTransitionScope<P, X, E, C, F, PS, EV>.() -> Unit = {},
    ) {
        addBranch(
            targetLabel = stateLabel,
            targetFactory = { null },
            guardLabel = guardLabel,
            commandLabels = commandLabels,
            effectLabels = effectLabels,
            kind = AfsmTopologyTransitionKind.Internal,
            isFallback = false,
            guard = guard,
            block = block,
        )
    }

    /**
     * Declares a final stayed branch for unmatched guards in this event handler.
     */
    public fun otherwise(
        commandLabels: List<String> = emptyList(),
        effectLabels: List<String> = emptyList(),
        block: AfsmTransitionScope<P, X, E, C, F, PS, EV>.() -> Unit = {},
    ) {
        addBranch(
            targetLabel = stateLabel,
            eventLabelOverride = "$eventLabel [otherwise]",
            targetFactory = { null },
            guardLabel = null,
            commandLabels = commandLabels,
            effectLabels = effectLabels,
            kind = AfsmTopologyTransitionKind.Internal,
            isFallback = true,
            guard = { true },
            block = block,
        )
    }

    /**
     * Declares a handled event that should be ignored without changing state or graph topology.
     */
    public fun ignore(
        reason: String? = null,
        guard: AfsmTransitionScope<P, X, E, C, F, PS, EV>.() -> Boolean = { true },
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
        guard: AfsmTransitionScope<P, X, E, C, F, PS, EV>.() -> Boolean = { true },
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
        targetFactory: AfsmTransitionScope<P, X, E, C, F, PS, EV>.() -> P?,
        guardLabel: String?,
        commandLabels: List<String>,
        effectLabels: List<String>,
        kind: AfsmTopologyTransitionKind,
        isFallback: Boolean,
        guard: AfsmTransitionScope<P, X, E, C, F, PS, EV>.() -> Boolean,
        block: AfsmTransitionScope<P, X, E, C, F, PS, EV>.() -> Unit,
    ) {
        transitions += AfsmTopologyTransition(
            from = stateLabel,
            event = eventLabelOverride,
            to = targetLabel,
            guardLabel = guardLabel,
            commandLabels = commandLabels,
            effectLabels = effectLabels,
            kind = kind,
            isFallback = isFallback,
        )
        branches += AfsmEventBranch { currentPhase, currentEvent, execution ->
            val typedPhase = phaseMatcher(currentPhase) ?: return@AfsmEventBranch AfsmBranchResult.Unmatched
            val typedEvent = eventMatcher(currentEvent) ?: return@AfsmEventBranch AfsmBranchResult.Unmatched
            val scope = AfsmTransitionScope<P, X, E, C, F, PS, EV>(
                phase = typedPhase,
                event = typedEvent,
                execution = execution,
            )

            if (!scope.guard()) {
                return@AfsmEventBranch AfsmBranchResult.Unmatched
            }

            val targetPhase = scope.targetFactory()

            AfsmBranchResult.Matched(
                targetPhase = targetPhase,
                decision = null,
                execute = {
                    scope.block()
                },
            )
        }
    }

    @PublishedApi
    internal fun addDecisionBranch(
        decision: AfsmDecision,
        guard: AfsmTransitionScope<P, X, E, C, F, PS, EV>.() -> Boolean,
    ) {
        branches += AfsmEventBranch { currentPhase, currentEvent, execution ->
            val typedPhase = phaseMatcher(currentPhase) ?: return@AfsmEventBranch AfsmBranchResult.Unmatched
            val typedEvent = eventMatcher(currentEvent) ?: return@AfsmEventBranch AfsmBranchResult.Unmatched
            val scope = AfsmTransitionScope<P, X, E, C, F, PS, EV>(
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
                execute = {},
            )
        }
    }

    internal fun buildDefinition(): AfsmEventDefinition<P, X, E, C, F> {
        return AfsmEventDefinition(
            eventLabel = eventLabel,
            eventMatcher = { event -> eventMatcher(event) != null },
            branches = branches.toList(),
            transitions = transitions.toList(),
        )
    }
}

public class AfsmEntryScope<P : Any, X : Any, C : Any, F : Any, PS : P> internal constructor(
    public val phase: PS,
    private val execution: AfsmDslExecution<P, X, C, F>,
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
    public fun command(command: C) {
        execution.commands += command
    }

    /**
     * Emits a UI-side one-shot effect.
     */
    public fun effect(effect: F) {
        execution.effects += effect
    }
}

public class AfsmExitScope<P : Any, X : Any, C : Any, F : Any, PS : P> internal constructor(
    public val phase: PS,
    private val execution: AfsmDslExecution<P, X, C, F>,
) {
    /**
     * Current extended context before the transition block and target entry handlers run.
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
     * Emits host-executed work, such as canceling a running request or timer.
     */
    public fun command(command: C) {
        execution.commands += command
    }

    /**
     * Emits a UI-side one-shot effect.
     */
    public fun effect(effect: F) {
        execution.effects += effect
    }
}

public class AfsmTransitionScope<P : Any, X : Any, E : Any, C : Any, F : Any, PS : P, EV : E> internal constructor(
    public val phase: PS,
    public val event: EV,
    private val execution: AfsmDslExecution<P, X, C, F>,
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
    public fun command(command: C) {
        execution.commands += command
    }

    /**
     * Emits a UI-side one-shot effect.
     */
    public fun effect(effect: F) {
        execution.effects += effect
    }
}

internal data class AfsmStateDefinition<P : Any, X : Any, E : Any, C : Any, F : Any>(
    val label: String,
    val matcher: (P) -> Any?,
    val entryHandlers: List<AfsmEntryHandler<P, X, C, F>>,
    val exitHandlers: List<AfsmExitHandler<P, X, C, F>>,
    val eventDefinitions: List<AfsmEventDefinition<P, X, E, C, F>>,
)

internal data class AfsmEventDefinition<P : Any, X : Any, E : Any, C : Any, F : Any>(
    val eventLabel: String,
    val eventMatcher: (E) -> Boolean,
    val branches: List<AfsmEventBranch<P, X, E, C, F>>,
    val transitions: List<AfsmTopologyTransition>,
)

internal fun interface AfsmEventBranch<P : Any, X : Any, E : Any, C : Any, F : Any> {
    fun tryHandle(
        phase: P,
        event: E,
        execution: AfsmDslExecution<P, X, C, F>,
    ): AfsmBranchResult<P>
}

internal sealed interface AfsmBranchResult<out P : Any> {
    data object Unmatched : AfsmBranchResult<Nothing>

    data class Matched<P : Any>(
        val targetPhase: P?,
        val decision: AfsmDecision?,
        val execute: () -> Unit,
    ) : AfsmBranchResult<P>
}

internal typealias AfsmEntryHandler<P, X, C, F> =
    (phase: P, execution: AfsmDslExecution<P, X, C, F>) -> Unit

internal typealias AfsmExitHandler<P, X, C, F> =
    (phase: P, execution: AfsmDslExecution<P, X, C, F>) -> Unit

private class AfsmDslMachine<P : Any, X : Any, E : Any, C : Any, F : Any>(
    override val initialState: AfsmState<P, X>,
    private val states: List<AfsmStateDefinition<P, X, E, C, F>>,
) : AfsmMachine<P, X, E, C, F> {
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
        state: AfsmState<P, X>,
        event: E,
    ): AfsmTransition<AfsmState<P, X>, C, F> {
        val stateDefinition = states.firstOrNull { definition ->
            definition.matcher(state.phase) != null
        } ?: return Afsm.invalid(
            state = state,
            reason = "No state definition matched the current phase.",
        )

        val eventDefinition = stateDefinition.eventDefinitions.firstOrNull { definition ->
            definition.eventMatcher(event)
        } ?: return Afsm.invalid(
            state = state,
            reason = "No event handler matched the current phase and event.",
        )

        val execution = AfsmDslExecution<P, X, C, F>(
            context = state.context,
        )

        val branchResult = eventDefinition.branches.firstNotNullOfOrNull { branch ->
            when (val result = branch.tryHandle(state.phase, event, execution)) {
                AfsmBranchResult.Unmatched -> null
                is AfsmBranchResult.Matched -> result
            }
        } ?: return Afsm.invalid(
            state = state,
            reason = "No branch matched the current phase and event.",
        )

        val targetPhase = branchResult.targetPhase
        if (targetPhase != null) {
            applyExitHandlers(
                sourceDefinition = stateDefinition,
                sourcePhase = state.phase,
                execution = execution,
            )
        }

        branchResult.execute()

        if (targetPhase != null) {
            applyEntryHandlers(
                targetPhase = targetPhase,
                execution = execution,
            )
        }

        val nextState = AfsmState(
            phase = targetPhase ?: state.phase,
            context = execution.context,
        )

        return AfsmTransition(
            state = nextState,
            commands = execution.commands.toList(),
            effects = execution.effects.toList(),
            decision = branchResult.decision ?: if (targetPhase != null) {
                AfsmDecision.Transitioned
            } else {
                AfsmDecision.Stayed()
            },
        )
    }

    private fun applyExitHandlers(
        sourceDefinition: AfsmStateDefinition<P, X, E, C, F>,
        sourcePhase: P,
        execution: AfsmDslExecution<P, X, C, F>,
    ) {
        sourceDefinition.exitHandlers.forEach { handler ->
            handler(sourcePhase, execution)
        }
    }

    private fun applyEntryHandlers(
        targetPhase: P,
        execution: AfsmDslExecution<P, X, C, F>,
    ) {
        val targetState = states.firstOrNull { definition ->
            definition.matcher(targetPhase) != null
        } ?: return

        targetState.entryHandlers.forEach { handler ->
            handler(targetPhase, execution)
        }
    }
}

internal class AfsmDslExecution<P : Any, X : Any, C : Any, F : Any>(
    var context: X,
    val commands: MutableList<C> = mutableListOf(),
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
