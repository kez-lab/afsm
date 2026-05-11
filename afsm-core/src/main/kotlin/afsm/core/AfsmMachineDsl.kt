package afsm.core

import kotlin.reflect.KClass

/**
 * Builds an executable Afsm machine definition.
 *
 * This DSL is intentionally plain Kotlin and Android-free. Android ViewModels
 * should host the returned [AfsmPhaseMachine] through an [AfsmMachine] or
 * [AfsmReducer] reference.
 *
 * Type parameters:
 *
 * - [P]: finite phase type. This is the node type that appears in generated
 *   state diagrams.
 * - [X]: extended context type. This is immutable data carried across phases.
 * - [E]: event type. Events are user intents or command results.
 * - [C]: command type. Commands are host-executed work emitted by transitions
 *   or entry/exit handlers.
 * - [F]: effect type. Effects are optional UI-side one-shot outputs.
 *
 * The [build] block declares the initial state, phase scopes, event branches,
 * entry/exit handlers, and graph metadata. The returned [AfsmPhaseMachine] is
 * both executable transition logic and an [AfsmGraphSource].
 */
public fun <P : Any, X : Any, E : Any, C : Any, F : Any> afsmMachine(
    build: AfsmMachineBuilder<P, X, E, C, F>.() -> Unit,
): AfsmPhaseMachine<P, X, E, C, F> {
    val builder = AfsmMachineBuilder<P, X, E, C, F>()
    builder.build()
    return builder.buildMachine()
}

public class AfsmMachineBuilder<P : Any, X : Any, E : Any, C : Any, F : Any> {
    private var initialState: AfsmState<P, X>? = null
    private val states = mutableListOf<AfsmStateDefinition<P, X, E, C, F>>()

    /**
     * Sets the initial finite phase and extended context for the machine.
     *
     * [phase] is the first graph state exposed through [AfsmPhaseMachine.initialState].
     * [context] is the initial extended data associated with that phase.
     *
     * Calling [initial] does not run the target phase's `onEnter` handler.
     * Startup work should be triggered by an explicit event such as
     * `ScreenEntered` when a feature needs that behavior.
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
     *
     * Use this overload for singleton phases such as `Editing`, `Loading`, or
     * `Approved`. The [phase] value is also used to derive the state label in
     * topology and Mermaid output.
     *
     * The [build] block declares entry/exit handlers and event handlers that
     * apply only when the machine's current phase equals [phase].
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
     *
     * Use this overload for phase classes that carry data, for example
     * `ReviewSubmissionInProgress(uploadToken)`. The state scope exposes the
     * current phase as type [PS], so entry/exit handlers and transition blocks
     * can safely read the payload.
     *
     * The generated topology uses the [PS] class name as the state label.
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

    internal fun buildMachine(): AfsmPhaseMachine<P, X, E, C, F> {
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
    private val entryCommandLabels = mutableListOf<String>()
    private val entryEffectLabels = mutableListOf<String>()
    private val exitCommandLabels = mutableListOf<String>()
    private val exitEffectLabels = mutableListOf<String>()
    private val eventDefinitions = mutableListOf<AfsmEventDefinition<P, X, E, C, F>>()

    /**
     * Runs when this phase is entered through an event branch transition.
     *
     * [handler] runs after the matching transition block has executed and only
     * for phase-changing branches such as [AfsmEventBranchScope.transitionTo].
     * It does not run for [AfsmEventBranchScope.stay],
     * [AfsmEventBranchScope.otherwise], [AfsmEventBranchScope.ignore], or
     * [AfsmEventBranchScope.invalid].
     *
     * Use `onEnter` for work that logically starts because a phase was entered:
     * emitting a command, clearing an error, or initializing phase-specific
     * context. It is not run when the machine's [AfsmPhaseMachine.initialState] is
     * created.
     *
     * [commandLabels] and [effectLabels] are topology metadata for generated
     * diagrams. Runtime commands/effects must still be emitted from [handler].
     */
    public fun onEnter(
        commandLabels: List<String> = emptyList(),
        effectLabels: List<String> = emptyList(),
        handler: AfsmEntryScope<P, X, C, F, PS>.() -> Unit,
    ) {
        entryCommandLabels += commandLabels
        entryEffectLabels += effectLabels
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
     *
     * [handler] runs before the transition block and target phase `onEnter`
     * handler. The execution order is:
     *
     * ```text
     * source onExit -> transition block -> target onEnter
     * ```
     *
     * Use `onExit` for cleanup work that logically belongs to leaving a phase,
     * such as emitting a cancel command or clearing phase-local context.
     *
     * [commandLabels] and [effectLabels] are topology metadata for generated
     * diagrams. Runtime commands/effects must still be emitted from [handler].
     */
    public fun onExit(
        commandLabels: List<String> = emptyList(),
        effectLabels: List<String> = emptyList(),
        handler: AfsmExitScope<P, X, C, F, PS>.() -> Unit,
    ) {
        exitCommandLabels += commandLabels
        exitEffectLabels += effectLabels
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
     *
     * [build] is evaluated when the machine is built and declares the ordered
     * branches for this event in the current phase. At runtime, when the current
     * phase matches this state scope and the incoming event is [EV], Afsm checks
     * the declared branches in declaration order and executes the first matching
     * branch.
     *
     * Prefer a single success [AfsmEventBranchScope.transitionTo] plus
     * [AfsmEventBranchScope.otherwise] for validation failure. Multiple guarded
     * transition branches are allowed for real alternative destinations, but they
     * should read as mutually exclusive state-machine branches.
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
            entryCommandLabels = entryCommandLabels.toList(),
            entryEffectLabels = entryEffectLabels.toList(),
            exitCommandLabels = exitCommandLabels.toList(),
            exitEffectLabels = exitEffectLabels.toList(),
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
     *
     * [phase] is the target finite phase. If this branch matches, the returned
     * state uses [phase] with the current context after `onExit`, [block], and
     * target `onEnter` have run.
     *
     * [guard] is a runtime predicate. Afsm evaluates it when the event is
     * processed. If it returns `false`, this branch is skipped and later branches
     * in the same `on<Event>` scope may match.
     *
     * [guardLabel], [commandLabels], and [effectLabels] are topology metadata for
     * generated diagrams and documentation. They do not execute commands,
     * effects, or guards. Runtime commands and effects must be emitted from
     * [block], `onEnter`, or `onExit`.
     *
     * [block] is runtime transition logic. Use it to update context and emit
     * commands/effects that are specific to this transition. For phase-changing
     * branches, execution order is source `onExit`, then [block], then target
     * `onEnter`.
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
     *
     * Use this overload when the target phase is a class that needs data from the
     * current [AfsmTransitionScope], such as an event payload:
     *
     * ```kotlin
     * transitionTo<Phase.Reviewing>(
     *     phase = { Phase.Reviewing(uploadToken = event.uploadToken) },
     * )
     * ```
     *
     * [phase] creates the target phase instance at runtime. The reified [TP]
     * type is used for static topology labels, while the returned value is used
     * as the actual next phase.
     *
     * [guard] is a runtime predicate. If it returns `false`, the branch is
     * skipped. [guardLabel], [commandLabels], and [effectLabels] are diagram
     * metadata only. [block] performs runtime context updates and command/effect
     * emission for the transition.
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
     *
     * Use `stay` for self-handled events where the phase is still the same
     * business state, for example text input updates inside an editing phase.
     *
     * [guard] is a runtime predicate. If it returns `false`, the branch is
     * skipped. [guardLabel], [commandLabels], and [effectLabels] are topology
     * metadata only. [block] can update context and emit commands/effects.
     *
     * `stay` does not run `onExit` or `onEnter`; it returns an
     * [AfsmDecision.Stayed] decision when accepted.
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
     *
     * `otherwise` is a fallback branch. It always matches if earlier branches in
     * the same `on<Event>` scope did not match. It is useful for validation
     * failure or default handled behavior:
     *
     * ```kotlin
     * on<Event.SubmitClicked> {
     *     transitionTo(Phase.Submitting, guard = { context.form.isValid() })
     *
     *     otherwise {
     *         updateContext { copy(errorMessage = "Invalid form") }
     *     }
     * }
     * ```
     *
     * [label] replaces the default `otherwise` diagram label with a
     * domain-specific reason such as `invalid form`. [commandLabels] and
     * [effectLabels] are metadata for generated diagrams. [block] is runtime
     * logic. `otherwise` does not change phase and does not run `onExit` or
     * `onEnter`.
     */
    public fun otherwise(
        label: String? = null,
        commandLabels: List<String> = emptyList(),
        effectLabels: List<String> = emptyList(),
        block: AfsmTransitionScope<P, X, E, C, F, PS, EV>.() -> Unit = {},
    ) {
        addBranch(
            targetLabel = stateLabel,
            eventLabelOverride = if (label == null) {
                "$eventLabel [otherwise]"
            } else {
                "$eventLabel [$label]"
            },
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
     *
     * Use `ignore` when the event is expected in this phase but intentionally
     * does nothing, such as a duplicate submit while already submitting.
     *
     * [reason] is diagnostic text surfaced through [AfsmDecision.Ignored].
     * [guard] optionally limits when the ignored decision applies.
     *
     * Ignored branches produce no topology edge and any state/command/effect
     * output is dropped by the runtime.
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
     *
     * Use `invalid` when receiving this event in the current phase is a flow
     * error that should be reported according to the host's invalid-transition
     * policy.
     *
     * [reason] is diagnostic text surfaced through [AfsmDecision.Invalid].
     * [guard] optionally limits when the invalid decision applies.
     *
     * Invalid branches produce no topology edge and do not run entry/exit
     * handlers.
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

/**
 * Runtime scope for a phase `onEnter` handler.
 *
 * [phase] is the target phase instance being entered. For payload phases
 * declared with `state<PayloadPhase>`, it is typed as that payload phase.
 */
public class AfsmEntryScope<P : Any, X : Any, C : Any, F : Any, PS : P> internal constructor(
    public val phase: PS,
    private val execution: AfsmDslExecution<P, X, C, F>,
) {
    /**
     * Current extended context after source `onExit` and the transition block
     * have run.
     */
    public val context: X
        get() = execution.context

    /**
     * Replaces the current context with an immutable copy.
     *
     * The [update] receiver is the current context. Return the next context
     * value. The final state will contain the latest context after all transition
     * and entry handlers complete.
     */
    public fun updateContext(update: X.() -> X) {
        execution.context = execution.context.update()
    }

    /**
     * Emits host-executed work, such as a repository call or timer start.
     *
     * [command] is appended to the transition output. The runtime does not
     * execute it inside the pure machine; `AfsmHost` passes it to the configured
     * command handler after state/effects are published.
     */
    public fun command(command: C) {
        execution.commands += command
    }

    /**
     * Emits a UI-side one-shot effect.
     *
     * [effect] is appended to the transition output. Prefer durable state for
     * behavior that must survive recreation; effects are best-effort one-shot
     * outputs.
     */
    public fun effect(effect: F) {
        execution.effects += effect
    }
}

/**
 * Runtime scope for a phase `onExit` handler.
 *
 * [phase] is the source phase instance being exited. For payload phases
 * declared with `state<PayloadPhase>`, it is typed as that payload phase.
 */
public class AfsmExitScope<P : Any, X : Any, C : Any, F : Any, PS : P> internal constructor(
    public val phase: PS,
    private val execution: AfsmDslExecution<P, X, C, F>,
) {
    /**
     * Current extended context before the transition block and target entry
     * handlers run.
     */
    public val context: X
        get() = execution.context

    /**
     * Replaces the current context with an immutable copy.
     *
     * The [update] receiver is the current context. Return the next context
     * value that should be visible to the transition block.
     */
    public fun updateContext(update: X.() -> X) {
        execution.context = execution.context.update()
    }

    /**
     * Emits host-executed work, such as canceling a running request or timer.
     *
     * [command] is appended to the transition output and will be handled by the
     * host after the transition is accepted.
     */
    public fun command(command: C) {
        execution.commands += command
    }

    /**
     * Emits a UI-side one-shot effect.
     *
     * [effect] is appended to the transition output. Use sparingly for UI
     * behavior that should not be represented as durable state.
     */
    public fun effect(effect: F) {
        execution.effects += effect
    }
}

/**
 * Runtime scope for a branch declared inside `on<Event>`.
 *
 * [phase] is the current source phase, typed as the enclosing state scope. [event]
 * is the incoming event, typed as the enclosing event scope.
 */
public class AfsmTransitionScope<P : Any, X : Any, E : Any, C : Any, F : Any, PS : P, EV : E> internal constructor(
    public val phase: PS,
    public val event: EV,
    private val execution: AfsmDslExecution<P, X, C, F>,
) {
    /**
     * Current extended context after earlier context updates in this transition
     * pipeline.
     *
     * For a phase-changing transition this is after source `onExit` has run.
     * For a stayed branch this starts as the current state's context.
     */
    public val context: X
        get() = execution.context

    /**
     * Replaces the current context with an immutable copy.
     *
     * The [update] receiver is the current context. Return the next context
     * value. Subsequent commands, effects, or entry handlers observe the updated
     * context through [context].
     */
    public fun updateContext(update: X.() -> X) {
        execution.context = execution.context.update()
    }

    /**
     * Emits host-executed work, such as a repository call or timer start.
     *
     * [command] is appended to the accepted transition output. Prefer emitting
     * long-running work from `onEnter` when the work is tied to entering a phase;
     * emit from a transition block when the work belongs specifically to the
     * edge.
     */
    public fun command(command: C) {
        execution.commands += command
    }

    /**
     * Emits a UI-side one-shot effect.
     *
     * [effect] is appended to the accepted transition output. Effects are not
     * durable state and should not be used for information that must be restored
     * after process death.
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
    val entryCommandLabels: List<String>,
    val entryEffectLabels: List<String>,
    val exitCommandLabels: List<String>,
    val exitEffectLabels: List<String>,
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
) : AfsmPhaseMachine<P, X, E, C, F> {
    override val topology: AfsmTopology = AfsmTopology(
        states = states.map { state ->
            AfsmTopologyState(
                id = state.label,
                entryCommandLabels = state.entryCommandLabels,
                entryEffectLabels = state.entryEffectLabels,
                exitCommandLabels = state.exitCommandLabels,
                exitEffectLabels = state.exitEffectLabels,
            )
        },
        transitions = states.flatMap { state ->
            state.eventDefinitions.flatMap { eventDefinition ->
                eventDefinition.transitions
            }
        }.distinct(),
        initialStateId = states.firstOrNull { state ->
            state.matcher(initialState.phase) != null
        }?.label,
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

private fun afsmLabelForValue(value: Any): String {
    return value::class.simpleName ?: value.toString()
}

@PublishedApi
internal fun afsmLabelForClass(kClass: KClass<*>): String {
    return kClass.simpleName ?: kClass.toString()
}
