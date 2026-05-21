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

@AfsmDslMarker
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
     * Declares an exact phase value with no entry, exit, or event handlers.
     *
     * This is useful for terminal or marker phases that only need to appear in
     * topology validation and generated diagrams. It is equivalent to
     * `state(phase) { }`, but reads better in first-use examples.
     */
    public fun state(phase: P) {
        state(phase = phase) {
        }
    }

    /**
     * Declares behavior for any phase instance of [PS], typically a payload phase.
     *
     * Use this overload for phase classes that carry data, for example
     * `ReviewSubmissionInProgress(uploadToken)`. The state scope exposes the
     * current phase as type [PS], so entry/exit handlers and cases can safely
     * read the payload.
     *
     * The generated topology uses the [PS] class name as the state label.
     */
    public inline fun <reified PS : P> state(
        noinline build: AfsmStateBuilder<P, X, E, C, F, PS>.() -> Unit,
    ) {
        state(
            phaseType = PS::class,
            build = build,
        )
    }

    /**
     * Declares any phase instance of [PS] with no entry, exit, or event handlers.
     *
     * Use this for terminal payload phase classes that should be valid targets
     * in topology and generated diagrams but do not handle further events.
     */
    public inline fun <reified PS : P> state() {
        state(
            phaseType = PS::class,
            build = {},
        )
    }

    /**
     * Declares behavior for any phase instance of [phaseType].
     *
     * Prefer the reified `state<Phase> { ... }` overload in ordinary Kotlin
     * feature code. Use this overload when the phase class is only available as
     * a [KClass], for example from shared tooling or generated code.
     */
    public fun <PS : P> state(
        phaseType: KClass<PS>,
        build: AfsmStateBuilder<P, X, E, C, F, PS>.() -> Unit,
    ) {
        addState(
            label = afsmLabelForClass(phaseType),
            matcher = { phase -> phase.castIfInstance(phaseType) },
            build = build,
        )
    }

    private fun <PS : P> addState(
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

@AfsmDslMarker
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
     * [handler] runs after the matching case actions have executed and only
     * for phase-changing branches such as [AfsmEventBranchScope.transitionTo].
     * It does not run for no-transition cases, [AfsmEventBranchScope.ignore],
     * or [AfsmEventBranchScope.invalid].
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
     * [handler] runs before the accepted case actions and target phase `onEnter`
     * handler. The execution order is:
     *
     * ```text
     * source onExit -> case actions -> target onEnter
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
     * Prefer named [AfsmEventBranchScope.case] branches for validation or
     * domain alternatives so code and generated diagrams say why a branch was
     * accepted. Multiple conditional transition branches are allowed for real
     * alternative destinations, but they should read as mutually exclusive
     * state-machine branches.
     */
    public inline fun <reified EV : E> on(
        noinline build: AfsmEventBranchScope<P, X, E, C, F, PS, EV>.() -> Unit,
    ) {
        on(
            eventType = EV::class,
            build = build,
        )
    }

    /**
     * Declares graphable branches for events whose runtime type is [eventType].
     *
     * Prefer the reified `on<Event> { ... }` overload in ordinary Kotlin
     * feature code. Use this overload when the event class is only available as
     * a [KClass], for example from shared tooling or generated code.
     */
    public fun <EV : E> on(
        eventType: KClass<EV>,
        build: AfsmEventBranchScope<P, X, E, C, F, PS, EV>.() -> Unit,
    ) {
        addEventDefinition(
            eventLabel = afsmLabelForClass(eventType),
            eventMatcher = { event -> event.castIfInstance(eventType) },
            build = build,
        )
    }

    private fun <EV : E> addEventDefinition(
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

@AfsmDslMarker
public class AfsmEventBranchScope<P : Any, X : Any, E : Any, C : Any, F : Any, PS : P, EV : E> internal constructor(
    private val stateLabel: String,
    private val eventLabel: String,
    private val phaseMatcher: (P) -> PS?,
    private val eventMatcher: (E) -> EV?,
) {
    private val branches = mutableListOf<AfsmEventBranch<P, X, E, C, F>>()
    private val transitions = mutableListOf<AfsmTopologyTransition>()

    /**
     * Declares one named runtime case for this event in the current phase.
     *
     * Use `case` when an event has domain alternatives that should be explicit
     * in code and generated diagrams, for example `valid draft`, `invalid form`,
     * or `matching request`.
     *
     * [label] is a human-readable condition name. For phase-changing cases it is
     * rendered as the transition condition label. For non-transition cases it is
     * rendered as part of the internal transition label.
     *
     * [condition] is evaluated at runtime with typed [phase], [event], and
     * [AfsmTransitionScope.context]. If it returns `false`, Afsm tries the next
     * declared case in this `on<Event>` block.
     *
     * [build] declares what the accepted case does. Calling
     * [AfsmEventCaseScope.transitionTo] changes phase. If no transition target is
     * declared, the case handles the event in the current phase. Context updates,
     * commands, and effects are declared as separate statements so
     * `transitionTo(...)` keeps one meaning: phase change.
     */
    public fun case(
        label: String? = null,
        condition: AfsmTransitionScope<P, X, E, C, F, PS, EV>.() -> Boolean = { true },
        build: AfsmEventCaseScope<P, X, E, C, F, PS, EV>.() -> Unit,
    ) {
        val builder = AfsmEventCaseScope<P, X, E, C, F, PS, EV>()
        builder.build()
        val builtCase = builder.buildCase()
        addBranch(
            targetLabel = builtCase.targetLabel ?: stateLabel,
            eventLabelOverride = if (builtCase.targetLabel == null && label != null) {
                "$eventLabel [$label]"
            } else {
                eventLabel
            },
            targetFactory = builtCase.targetFactory ?: { null },
            conditionLabel = if (builtCase.targetLabel != null) label else null,
            commandLabels = builtCase.commandLabels,
            effectLabels = builtCase.effectLabels,
            kind = if (builtCase.targetLabel != null) {
                AfsmTopologyTransitionKind.External
            } else {
                AfsmTopologyTransitionKind.Internal
            },
            isFallback = false,
            condition = condition,
            block = {
                builtCase.actions.forEach { action ->
                    action()
                }
            },
        )
    }

    /**
     * Handles this event by updating context without changing phase.
     *
     * Use this when the event only changes extended state such as form fields
     * or errors. Because no phase target is declared, the machine remains in
     * the current phase.
     */
    public fun updateContext(
        label: String? = null,
        condition: AfsmTransitionScope<P, X, E, C, F, PS, EV>.() -> Boolean = { true },
        update: X.() -> X,
    ) {
        case(
            label = label,
            condition = condition,
        ) {
            updateContext(update)
        }
    }

    /**
     * Handles this event by updating context with access to the typed event.
     *
     * Use this overload for input events whose payload changes context. The
     * first lambda parameter is the current context; the second is the typed
     * event payload.
     */
    public fun updateContext(
        label: String? = null,
        condition: AfsmTransitionScope<P, X, E, C, F, PS, EV>.() -> Boolean = { true },
        update: (X, EV) -> X,
    ) {
        case(
            label = label,
            condition = condition,
        ) {
            updateContext(update)
        }
    }

    /**
     * Handles this event by emitting a UI-side one-shot effect without changing phase.
     *
     * Use this for rare terminal UI actions such as closing a screen after a
     * durable terminal state has already been reached.
     */
    public fun effect(
        label: String? = null,
        effect: AfsmTransitionScope<P, X, E, C, F, PS, EV>.() -> F,
    ) {
        case(label = label) {
            effect(label = label, effect = effect)
        }
    }

    /**
     * Handles this event by changing to [phase].
     *
     * This convenience is for unconditional phase changes. If the event needs a
     * named condition, context update, command, or effect, use [case] and call
     * [AfsmEventCaseScope.transitionTo] inside that case.
     */
    public fun transitionTo(phase: P) {
        case {
            transitionTo(phase)
        }
    }

    /**
     * Handles this event by changing to a payload phase created from runtime data.
     *
     * This convenience is for unconditional phase changes. If the event needs a
     * named condition, context update, command, or effect, use [case].
     */
    public inline fun <reified TP : P> transitionTo(
        noinline phase: AfsmTransitionScope<P, X, E, C, F, PS, EV>.() -> TP,
    ) {
        transitionTo(
            phaseType = TP::class,
            phase = phase,
        )
    }

    /**
     * Handles this event by changing to a payload phase whose type is available
     * as a [KClass].
     */
    public fun <TP : P> transitionTo(
        phaseType: KClass<TP>,
        phase: AfsmTransitionScope<P, X, E, C, F, PS, EV>.() -> TP,
    ) {
        case {
            transitionTo(
                phaseType = phaseType,
                phase = phase,
            )
        }
    }

    /**
     * Declares a handled event that should be ignored without changing state or graph topology.
     *
     * Use `ignore` when the event is expected in this phase but intentionally
     * does nothing, such as a duplicate submit while already submitting.
     *
     * [reason] is diagnostic text surfaced through [AfsmDecision.Ignored].
     * [condition] optionally limits when the ignored decision applies.
     *
     * Ignored branches produce no topology edge and any state/command/effect
     * output is dropped by the runtime.
     */
    public fun ignore(
        reason: String? = null,
        condition: AfsmTransitionScope<P, X, E, C, F, PS, EV>.() -> Boolean = { true },
    ) {
        addDecisionBranch(
            decision = AfsmDecision.Ignored(reason),
            condition = condition,
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
     * [condition] optionally limits when the invalid decision applies.
     *
     * Invalid branches produce no topology edge and do not run entry/exit
     * handlers.
     */
    public fun invalid(
        reason: String? = null,
        condition: AfsmTransitionScope<P, X, E, C, F, PS, EV>.() -> Boolean = { true },
    ) {
        addDecisionBranch(
            decision = AfsmDecision.Invalid(reason),
            condition = condition,
        )
    }

    private fun addBranch(
        targetLabel: String,
        eventLabelOverride: String = eventLabel,
        targetFactory: AfsmTransitionScope<P, X, E, C, F, PS, EV>.() -> P?,
        conditionLabel: String?,
        commandLabels: List<String>,
        effectLabels: List<String>,
        kind: AfsmTopologyTransitionKind,
        isFallback: Boolean,
        condition: AfsmTransitionScope<P, X, E, C, F, PS, EV>.() -> Boolean,
        block: AfsmTransitionScope<P, X, E, C, F, PS, EV>.() -> Unit,
    ) {
        transitions += AfsmTopologyTransition(
            from = stateLabel,
            event = eventLabelOverride,
            to = targetLabel,
            conditionLabel = conditionLabel,
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

            if (!scope.condition()) {
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
        condition: AfsmTransitionScope<P, X, E, C, F, PS, EV>.() -> Boolean,
    ) {
        branches += AfsmEventBranch { currentPhase, currentEvent, execution ->
            val typedPhase = phaseMatcher(currentPhase) ?: return@AfsmEventBranch AfsmBranchResult.Unmatched
            val typedEvent = eventMatcher(currentEvent) ?: return@AfsmEventBranch AfsmBranchResult.Unmatched
            val scope = AfsmTransitionScope<P, X, E, C, F, PS, EV>(
                phase = typedPhase,
                event = typedEvent,
                execution = execution,
            )

            if (!scope.condition()) {
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

@AfsmDslMarker
public class AfsmEventCaseScope<P : Any, X : Any, E : Any, C : Any, F : Any, PS : P, EV : E> internal constructor() {
    internal var targetLabel: String? = null
    internal var targetFactory: (AfsmTransitionScope<P, X, E, C, F, PS, EV>.() -> P?)? = null
    internal val actions = mutableListOf<AfsmTransitionScope<P, X, E, C, F, PS, EV>.() -> Unit>()
    internal val commandLabels = mutableListOf<String>()
    internal val effectLabels = mutableListOf<String>()

    /**
     * Changes the finite phase for this accepted case.
     *
     * `transitionTo` only records the target phase. Put context updates,
     * commands, and effects in separate statements in the same case.
     */
    public fun transitionTo(phase: P) {
        setTarget(
            label = afsmLabelForValue(phase),
            factory = { phase },
        )
    }

    /**
     * Changes to a payload phase created from runtime data.
     *
     * Use this when the target phase carries event or context data, such as an
     * upload token or order id.
     */
    public inline fun <reified TP : P> transitionTo(
        noinline phase: AfsmTransitionScope<P, X, E, C, F, PS, EV>.() -> TP,
    ) {
        transitionTo(
            phaseType = TP::class,
            phase = phase,
        )
    }

    /**
     * Changes to a payload phase whose type is available as a [KClass].
     */
    public fun <TP : P> transitionTo(
        phaseType: KClass<TP>,
        phase: AfsmTransitionScope<P, X, E, C, F, PS, EV>.() -> TP,
    ) {
        setTarget(
            label = afsmLabelForClass(phaseType),
            factory = phase,
        )
    }

    /**
     * Replaces context without changing phase by itself.
     */
    public fun updateContext(update: X.() -> X) {
        actions += {
            updateContext(update)
        }
    }

    /**
     * Replaces context with access to the typed event payload.
     */
    public fun updateContext(update: (X, EV) -> X) {
        actions += {
            val nextContext = update(context, event)
            updateContext { nextContext }
        }
    }

    /**
     * Emits host-executed work from this accepted case.
     *
     * [label] is optional graph metadata. Runtime work is produced by [command].
     */
    public fun command(
        label: String? = null,
        command: AfsmTransitionScope<P, X, E, C, F, PS, EV>.() -> C,
    ) {
        label?.let { commandLabels += it }
        actions += {
            command(command())
        }
    }

    /**
     * Emits a UI-side one-shot effect from this accepted case.
     *
     * [label] is optional graph metadata. Runtime work is produced by [effect].
     */
    public fun effect(
        label: String? = null,
        effect: AfsmTransitionScope<P, X, E, C, F, PS, EV>.() -> F,
    ) {
        label?.let { effectLabels += it }
        actions += {
            effect(effect())
        }
    }

    internal fun buildCase(): AfsmBuiltEventCase<P, X, E, C, F, PS, EV> {
        return AfsmBuiltEventCase(
            targetLabel = targetLabel,
            targetFactory = targetFactory,
            actions = actions.toList(),
            commandLabels = commandLabels.toList(),
            effectLabels = effectLabels.toList(),
        )
    }

    private fun setTarget(
        label: String,
        factory: AfsmTransitionScope<P, X, E, C, F, PS, EV>.() -> P,
    ) {
        check(targetLabel == null) {
            "Only one transition target can be declared in one Afsm case."
        }
        targetLabel = label
        targetFactory = factory
    }
}

internal data class AfsmBuiltEventCase<P : Any, X : Any, E : Any, C : Any, F : Any, PS : P, EV : E>(
    val targetLabel: String?,
    val targetFactory: (AfsmTransitionScope<P, X, E, C, F, PS, EV>.() -> P?)?,
    val actions: List<AfsmTransitionScope<P, X, E, C, F, PS, EV>.() -> Unit>,
    val commandLabels: List<String>,
    val effectLabels: List<String>,
)

/**
 * Runtime scope for a phase `onEnter` handler.
 *
 * [phase] is the target phase instance being entered. For payload phases
 * declared with `state<PayloadPhase>`, it is typed as that payload phase.
 */
@AfsmDslMarker
public class AfsmEntryScope<P : Any, X : Any, C : Any, F : Any, PS : P> internal constructor(
    public val phase: PS,
    private val execution: AfsmDslExecution<P, X, C, F>,
) {
    /**
     * Current extended context after source `onExit` and case actions
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
@AfsmDslMarker
public class AfsmExitScope<P : Any, X : Any, C : Any, F : Any, PS : P> internal constructor(
    public val phase: PS,
    private val execution: AfsmDslExecution<P, X, C, F>,
) {
    /**
     * Current extended context before case actions and target entry
     * handlers run.
     */
    public val context: X
        get() = execution.context

    /**
     * Replaces the current context with an immutable copy.
     *
     * The [update] receiver is the current context. Return the next context
     * value that should be visible to the accepted case actions.
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
@AfsmDslMarker
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
     * For a no-transition case this starts as the current state's context.
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
     * emit from a case when the work belongs specifically to one event branch.
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

        return when (val decision = branchResult.decision) {
            null -> {
                if (targetPhase != null) {
                    AfsmTransition.transitioned(
                        state = nextState,
                        commands = execution.commands.toList(),
                        effects = execution.effects.toList(),
                    )
                } else {
                    AfsmTransition.stayed(
                        state = nextState,
                        commands = execution.commands.toList(),
                        effects = execution.effects.toList(),
                    )
                }
            }

            is AfsmDecision.Ignored -> AfsmTransition.ignored(
                state = nextState,
                reason = decision.reason,
            )

            is AfsmDecision.Invalid -> AfsmTransition.invalid(
                state = nextState,
                reason = decision.reason,
            )

            is AfsmDecision.Stayed -> AfsmTransition.stayed(
                state = nextState,
                reason = decision.reason,
            )

            AfsmDecision.Transitioned -> AfsmTransition.transitioned(
                state = nextState,
                commands = execution.commands.toList(),
                effects = execution.effects.toList(),
            )
        }
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

private fun afsmLabelForClass(kClass: KClass<*>): String {
    return kClass.simpleName ?: kClass.toString()
}

@Suppress("UNCHECKED_CAST")
private fun <T : Any> Any.castIfInstance(kClass: KClass<T>): T? {
    return if (kClass.isInstance(this)) this as T else null
}
