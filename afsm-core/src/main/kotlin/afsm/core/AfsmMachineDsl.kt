package afsm.core

import kotlin.reflect.KClass

/**
 * Builds an executable Afsm machine definition.
 *
 * This DSL is intentionally plain Kotlin and Android-free. Android ViewModels
 * should host the returned [AfsmMachine] or [AfsmReducer] reference from a
 * ViewModel.
 *
 * Type parameters:
 *
 * - [P]: finite phase type. This is the node type that appears in generated
 *   state diagrams.
 * - [D]: extended data type. This is immutable data carried across phases.
 * - [E]: event type. Events are user intents or command results.
 * - [C]: command type. Commands are host-executed work emitted by transitions
 *   or entry/exit handlers.
 *
 * The [build] block declares the default initial state, phase scopes, event
 * branches, entry/exit handlers, and graph metadata. The returned
 * [AfsmDefaultMachine] is both executable transition logic and an
 * [AfsmGraphSource]. Use the `initialPhase` overload below when runtime data
 * must come from the host.
 */
public fun <P : Any, D : Any, E : Any, C : Any> afsmMachine(
    build: AfsmMachineBuilder<P, D, E, C>.() -> Unit,
): AfsmDefaultMachine<AfsmState<P, D>, E, C> {
    val builder = AfsmMachineBuilder<P, D, E, C>()
    builder.build()
    return builder.buildDefaultMachine()
}

/**
 * Builds a graphable executable machine whose runtime state must be supplied by
 * its host.
 *
 * Use this overload when the initial [phase][initialPhase] is known for graph
 * topology, but durable data comes from navigation, a deep link, restoration,
 * or another runtime source. The returned [AfsmMachine] intentionally has no
 * default state, so Android hosting requires an explicit `initialState`.
 */
public fun <P : Any, D : Any, E : Any, C : Any> afsmMachine(
    initialPhase: P,
    build: AfsmMachineBuilder<P, D, E, C>.() -> Unit,
): AfsmMachine<AfsmState<P, D>, E, C> {
    val builder = AfsmMachineBuilder<P, D, E, C>()
    builder.build()
    return builder.buildMachine(initialPhase = initialPhase)
}

@AfsmDslMarker
public class AfsmMachineBuilder<P : Any, D : Any, E : Any, C : Any> {
    private var initialState: AfsmState<P, D>? = null
    private val states = mutableListOf<AfsmStateDefinition<P, D, E, C>>()

    /**
     * Sets the initial finite phase and extended data for the machine.
     *
     * [phase] is the first graph state exposed through
     * [AfsmDefaultMachine.initialState].
     * [data] is the initial extended data associated with that phase.
     *
     * Calling [initial] does not run the target phase's `onEnter` handler.
     * Startup work should be triggered by an explicit event such as
     * `ScreenEntered` when a feature needs that behavior.
     */
    public fun initial(
        phase: P,
        data: D,
    ) {
        initialState = AfsmState(
            phase = phase,
            data = data,
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
    public fun phase(
        phase: P,
        build: AfsmPhaseBuilder<P, D, E, C, P>.() -> Unit,
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
     * `phase(phase) { }`, but reads better in first-use examples.
     */
    public fun phase(phase: P) {
        phase(phase = phase) {
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
    public inline fun <reified PS : P> phase(
        noinline build: AfsmPhaseBuilder<P, D, E, C, PS>.() -> Unit,
    ) {
        phase(
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
    public inline fun <reified PS : P> phase() {
        phase(
            phaseType = PS::class,
            build = {},
        )
    }

    /**
     * Declares behavior for any phase instance of [phaseType].
     *
     * Prefer the reified `phase<Phase> { ... }` overload in ordinary Kotlin
     * feature code. Use this overload when the phase class is only available as
     * a [KClass], for example from shared tooling or generated code.
     */
    public fun <PS : P> phase(
        phaseType: KClass<PS>,
        build: AfsmPhaseBuilder<P, D, E, C, PS>.() -> Unit,
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
        build: AfsmPhaseBuilder<P, D, E, C, PS>.() -> Unit,
    ) {
        val builder = AfsmPhaseBuilder<P, D, E, C, PS>(
            stateLabel = label,
            matcher = matcher,
        )
        builder.build()
        states += builder.buildDefinition()
    }

    internal fun buildDefaultMachine(): AfsmDefaultMachine<AfsmState<P, D>, E, C> {
        val initial = requireNotNull(initialState) {
            "Afsm machine requires an initial phase and data."
        }
        val builtStates = states.toList()

        validateDefinitions(
            initialPhase = initial.phase,
            states = builtStates,
        )

        return AfsmDefaultDslMachine(
            initialState = initial,
            states = builtStates,
        )
    }

    internal fun buildMachine(
        initialPhase: P,
    ): AfsmMachine<AfsmState<P, D>, E, C> {
        require(initialState == null) {
            "Afsm machine with initialPhase must not also declare initial phase and data."
        }
        val builtStates = states.toList()

        validateDefinitions(
            initialPhase = initialPhase,
            states = builtStates,
        )

        return AfsmDslMachine(
            initialPhase = initialPhase,
            states = builtStates,
        )
    }

    private fun validateDefinitions(
        initialPhase: P,
        states: List<AfsmStateDefinition<P, D, E, C>>,
    ) {
        val errors = mutableListOf<String>()

        if (states.isEmpty()) {
            errors += "At least one phase must be declared."
        }

        states.groupBy { it.label }
            .filterValues { it.size > 1 }
            .keys
            .forEach { label ->
                errors += "Duplicate phase declaration: $label."
            }

        states.forEach { state ->
            state.invocationKeys
                .groupingBy { key -> key }
                .eachCount()
                .filterValues { count -> count > 1 }
                .keys
                .forEach { key ->
                    errors +=
                        "Duplicate invocation key ${key.value} in phase ${state.label}."
                }
        }

        if (states.none { it.matcher(initialPhase) != null }) {
            errors += "Initial phase ${afsmLabelForValue(initialPhase)} has no matching phase declaration."
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
                    errors += "Transition ${transition.from} -- ${transition.event} --> ${transition.to} targets an undeclared phase."
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
public class AfsmPhaseBuilder<P : Any, D : Any, E : Any, C : Any, PS : P> internal constructor(
    private val stateLabel: String,
    private val matcher: (P) -> PS?,
) {
    private val entryHandlers = mutableListOf<AfsmEntryHandler<P, D, C>>()
    private val exitHandlers = mutableListOf<AfsmExitHandler<P, D, C>>()
    private val entryCommandLabels = mutableListOf<String>()
    private val exitCommandLabels = mutableListOf<String>()
    private val invocationKeys = mutableListOf<AfsmInvocationKey>()
    private val eventDefinitions = mutableListOf<AfsmEventDefinition<P, D, E, C>>()

    /**
     * Declares actions that run when this phase is entered through a phase change.
     *
     * The [handler] block is declaration code: calls such as
     * `command(label = "Load") { ... }` record both graph metadata and runtime
     * work in one place. The command factory lambdas run later, when the
     * transition is accepted and real [AfsmPhaseActionScope.data] is
     * available.
     *
     * Entry actions run after the accepted case actions and only for
     * phase-changing branches such as [AfsmEventBranchScope.transitionTo]. They
     * do not run for no-transition cases, [AfsmEventBranchScope.ignore], or
     * [AfsmEventBranchScope.invalid]. Initial state construction also does not
     * run `onEnter`; trigger startup work with an explicit event such as
     * `ScreenEntered`.
     */
    public fun onEnter(
        handler: AfsmEntryScope<P, D, C, PS>.() -> Unit,
    ) {
        val scope = AfsmEntryScope<P, D, C, PS>(
            phaseMatcher = matcher,
        )
        scope.handler()
        val builtHandler = scope.buildHandler()

        entryCommandLabels += builtHandler.commandLabels
        invocationKeys += builtHandler.invocationKeys
        exitCommandLabels += builtHandler.invocationKeys.map { key ->
            "cancel ${key.value}"
        }
        entryHandlers += builtHandler.handler
    }

    /**
     * Declares actions that run when this phase is exited by a phase change.
     *
     * The [handler] block is declaration code for short sequential cleanup and
     * data work. Long-running work declared with `onEnter { invoke(...) }`
     * is cancelled automatically and does not need an exit command.
     *
     * Exit actions run before the accepted case actions, target payload phase
     * factory, and target phase `onEnter` handler. The execution order is:
     *
     * ```text
     * source onExit -> case actions -> target phase factory -> target onEnter
     * ```
     */
    public fun onExit(
        handler: AfsmExitScope<P, D, C, PS>.() -> Unit,
    ) {
        val scope = AfsmExitScope<P, D, C, PS>(
            phaseMatcher = matcher,
        )
        scope.handler()
        val builtHandler = scope.buildHandler()

        exitCommandLabels += builtHandler.commandLabels
        exitHandlers += builtHandler.handler
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
        noinline build: AfsmEventBranchScope<P, D, E, C, PS, EV>.() -> Unit,
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
        build: AfsmEventBranchScope<P, D, E, C, PS, EV>.() -> Unit,
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
        build: AfsmEventBranchScope<P, D, E, C, PS, EV>.() -> Unit,
    ) {
        val builder = AfsmEventBranchScope<P, D, E, C, PS, EV>(
            stateLabel = stateLabel,
            eventLabel = eventLabel,
            phaseMatcher = matcher,
            eventMatcher = eventMatcher,
        )
        builder.build()
        eventDefinitions += builder.buildDefinition()
    }

    internal fun buildDefinition(): AfsmStateDefinition<P, D, E, C> {
        return AfsmStateDefinition(
            label = stateLabel,
            matcher = matcher,
            entryHandlers = entryHandlers.toList(),
            exitHandlers = exitHandlers.toList(),
            entryCommandLabels = entryCommandLabels.toList(),
            exitCommandLabels = exitCommandLabels.toList(),
            invocationKeys = invocationKeys.toList(),
            eventDefinitions = eventDefinitions.toList(),
        )
    }
}

@AfsmDslMarker
public class AfsmEventBranchScope<P : Any, D : Any, E : Any, C : Any, PS : P, EV : E> internal constructor(
    private val stateLabel: String,
    private val eventLabel: String,
    private val phaseMatcher: (P) -> PS?,
    private val eventMatcher: (E) -> EV?,
) {
    private val branches = mutableListOf<AfsmEventBranch<P, D, E, C>>()
    private val transitions = mutableListOf<AfsmTopologyTransition>()
    private val directBranch = AfsmEventCaseScope<P, D, E, C, PS, EV>()
    private var hasDirectActions = false
    private var hasDecisionBranches = false

    /**
     * Declares one conditional runtime case for this event in the current phase.
     *
     * Use `case` when an event has domain alternatives that should be explicit
     * in code and generated diagrams, for example `valid draft`, `invalid form`,
     * or `matching request`.
     *
     * [label] is a human-readable condition name. For phase-changing cases it is
     * rendered as the transition condition label. For non-transition cases it is
     * rendered as part of the internal transition label.
     *
     * [condition] is required and evaluated at runtime with typed [phase], [event], and
     * [AfsmConditionScope.data]. Conditions are read-only: they can inspect the
     * current phase, event, and data, but cannot update data or emit outputs.
     * If [condition] returns `false`, Afsm tries the next declared case in this
     * `on<Event>` block.
     *
     * [build] declares what the accepted case does. Calling
     * [AfsmEventCaseScope.transitionTo] changes phase. If no transition target is
     * declared, the case handles the event in the current phase. Data updates,
     * commands are declared as separate statements so
     * `transitionTo(...)` keeps one meaning: phase change.
     */
    public fun case(
        label: String? = null,
        condition: AfsmConditionScope<P, D, E, PS, EV>.() -> Boolean,
        build: AfsmEventCaseScope<P, D, E, C, PS, EV>.() -> Unit,
    ) {
        hasDecisionBranches = true
        val builder = AfsmEventCaseScope<P, D, E, C, PS, EV>()
        builder.build()
        addBuiltCase(
            builtCase = builder.buildCase(),
            conditionLabel = label,
            condition = condition,
        )
    }

    /**
     * Handles this event by updating data without changing phase.
     *
     * Use this when the event only changes extended state such as form fields
     * or errors. Because no phase target is declared, the machine remains in
     * the current phase.
     *
     * Direct statements in the same `on<Event>` block compose one
     * unconditional branch. Use [case] only when the update is conditional.
     */
    public fun updateData(
        update: D.() -> D,
    ) {
        addDirectAction { updateData(update) }
    }

    /**
     * Handles this event by updating data with access to the typed event.
     *
     * Use this overload for input events whose payload changes data. The
     * first lambda parameter is the current data; the second is the typed
     * event payload.
     *
     * Direct statements in the same `on<Event>` block compose one
     * unconditional branch. Use [case] only when the update is conditional.
     */
    public fun updateData(
        update: (D, EV) -> D,
    ) {
        addDirectAction { updateData(update) }
    }

    /**
     * Emits host-executed work from the unconditional branch for this event.
     *
     * Direct statements in the same `on<Event>` block compose one branch and
     * their actions execute in declaration order.
     */
    public fun command(
        label: String? = null,
        command: AfsmTransitionScope<P, D, E, C, PS, EV>.() -> C,
    ) {
        addDirectAction { command(label = label, command = command) }
    }

    /**
     * Handles this event by changing to [phase].
     *
     * Other direct actions in the same `on<Event>` block are part of the same
     * unconditional branch. Use [case] only for a conditional phase change.
     */
    public fun transitionTo(phase: P) {
        addDirectAction { transitionTo(phase) }
    }

    /**
     * Handles this event by changing to a payload phase created from runtime data.
     *
     * Other direct actions in the same `on<Event>` block are part of the same
     * unconditional branch. Use [case] only for a conditional phase change.
     */
    public inline fun <reified TP : P> transitionTo(
        noinline phase: AfsmPhaseFactoryScope<P, D, E, PS, EV>.() -> TP,
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
        phase: AfsmPhaseFactoryScope<P, D, E, PS, EV>.() -> TP,
    ) {
        addDirectAction {
            transitionTo(
                phaseType = phaseType,
                phase = phase,
            )
        }
    }

    /**
     * Declares an expected no-op event without changing state or graph topology.
     *
     * Use `ignore` sparingly for events that can legitimately arrive in this
     * phase and are harmless, such as a duplicate submit while already
     * submitting or a stale async result after retry. Do not enumerate every
     * impossible event with `ignore`; omitted handlers are invalid by default.
     *
     * [reason] is diagnostic text surfaced through [AfsmDecision.Ignored].
     * [condition] optionally limits when the ignored decision applies.
     *
     * Ignored branches produce no topology edge and any state or command output
     * is dropped by the runtime.
     */
    public fun ignore(
        reason: String? = null,
        condition: AfsmConditionScope<P, D, E, PS, EV>.() -> Boolean = { true },
    ) {
        hasDecisionBranches = true
        addDecisionBranch(
            decision = AfsmDecision.Ignored(reason),
            condition = condition,
        )
    }

    /**
     * Declares an invalid transition without changing graph topology.
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
        condition: AfsmConditionScope<P, D, E, PS, EV>.() -> Boolean = { true },
    ) {
        hasDecisionBranches = true
        addDecisionBranch(
            decision = AfsmDecision.Invalid(reason),
            condition = condition,
        )
    }

    private fun addBranch(
        targetLabel: String,
        eventLabelOverride: String = eventLabel,
        targetFactory: (AfsmPhaseFactoryScope<P, D, E, PS, EV>.() -> P?)?,
        conditionLabel: String?,
        commandLabels: List<String>,
        kind: AfsmTopologyTransitionKind,
        isFallback: Boolean,
        condition: AfsmConditionScope<P, D, E, PS, EV>.() -> Boolean,
        block: AfsmTransitionScope<P, D, E, C, PS, EV>.() -> Unit,
    ) {
        transitions += AfsmTopologyTransition(
            from = stateLabel,
            event = eventLabelOverride,
            to = targetLabel,
            conditionLabel = conditionLabel,
            commandLabels = commandLabels,
            kind = kind,
            isFallback = isFallback,
        )
        branches += AfsmEventBranch { currentPhase, currentEvent, execution ->
            val typedPhase = phaseMatcher(currentPhase) ?: return@AfsmEventBranch AfsmBranchResult.Unmatched
            val typedEvent = eventMatcher(currentEvent) ?: return@AfsmEventBranch AfsmBranchResult.Unmatched
            val scope = AfsmTransitionScope<P, D, E, C, PS, EV>(
                phase = typedPhase,
                event = typedEvent,
                execution = execution,
            )
            val conditionScope = AfsmConditionScope<P, D, E, PS, EV>(
                phase = typedPhase,
                event = typedEvent,
                readData = { execution.data },
            )

            if (!conditionScope.condition()) {
                return@AfsmEventBranch AfsmBranchResult.Unmatched
            }

            AfsmBranchResult.Matched(
                targetFactory = targetFactory?.let { factory ->
                    {
                        val phaseFactoryScope = AfsmPhaseFactoryScope<P, D, E, PS, EV>(
                            phase = typedPhase,
                            event = typedEvent,
                            readData = { execution.data },
                        )
                        phaseFactoryScope.factory()
                    }
                },
                decision = null,
                execute = {
                    scope.block()
                },
            )
        }
    }

    private fun addDirectAction(
        action: AfsmEventCaseScope<P, D, E, C, PS, EV>.() -> Unit,
    ) {
        hasDirectActions = true
        directBranch.action()
    }

    private fun addBuiltCase(
        builtCase: AfsmBuiltEventCase<P, D, E, C, PS, EV>,
        conditionLabel: String?,
        condition: AfsmConditionScope<P, D, E, PS, EV>.() -> Boolean,
    ) {
        addBranch(
            targetLabel = builtCase.targetLabel ?: stateLabel,
            targetFactory = builtCase.targetFactory,
            conditionLabel = conditionLabel,
            commandLabels = builtCase.commandLabels,
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

    internal fun addDecisionBranch(
        decision: AfsmDecision,
        condition: AfsmConditionScope<P, D, E, PS, EV>.() -> Boolean,
    ) {
        branches += AfsmEventBranch { currentPhase, currentEvent, execution ->
            val typedPhase = phaseMatcher(currentPhase) ?: return@AfsmEventBranch AfsmBranchResult.Unmatched
            val typedEvent = eventMatcher(currentEvent) ?: return@AfsmEventBranch AfsmBranchResult.Unmatched
            val conditionScope = AfsmConditionScope<P, D, E, PS, EV>(
                phase = typedPhase,
                event = typedEvent,
                readData = { execution.data },
            )

            if (!conditionScope.condition()) {
                return@AfsmEventBranch AfsmBranchResult.Unmatched
            }

            AfsmBranchResult.Matched(
                targetFactory = null,
                decision = decision,
                execute = {},
            )
        }
    }

    internal fun buildDefinition(): AfsmEventDefinition<P, D, E, C> {
        if (hasDirectActions && hasDecisionBranches) {
            throw AfsmDefinitionException(
                "Event $eventLabel in phase $stateLabel cannot mix direct actions " +
                    "with conditional case, ignore, or invalid branches.",
            )
        }

        if (hasDirectActions) {
            addBuiltCase(
                builtCase = directBranch.buildCase(),
                conditionLabel = null,
                condition = { true },
            )
        }

        return AfsmEventDefinition(
            eventLabel = eventLabel,
            eventMatcher = { event -> eventMatcher(event) != null },
            branches = branches.toList(),
            transitions = transitions.toList(),
        )
    }
}

@AfsmDslMarker
public class AfsmEventCaseScope<P : Any, D : Any, E : Any, C : Any, PS : P, EV : E> internal constructor() {
    internal var targetLabel: String? = null
    internal var targetFactory: (AfsmPhaseFactoryScope<P, D, E, PS, EV>.() -> P?)? = null
    internal val actions = mutableListOf<AfsmTransitionScope<P, D, E, C, PS, EV>.() -> Unit>()
    internal val commandLabels = mutableListOf<String>()

    /**
     * Changes the finite phase for this accepted case.
     *
     * `transitionTo` only records the target phase. Put data updates,
     * commands in separate statements in the same case.
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
     * Use this when the target phase carries event or existing state data, such as an
     * upload token or order id. The [phase] factory runs after source `onExit`
     * and accepted case actions, so it observes data updates made earlier in
     * the case.
     */
    public inline fun <reified TP : P> transitionTo(
        noinline phase: AfsmPhaseFactoryScope<P, D, E, PS, EV>.() -> TP,
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
        phase: AfsmPhaseFactoryScope<P, D, E, PS, EV>.() -> TP,
    ) {
        setTarget(
            label = afsmLabelForClass(phaseType),
            factory = phase,
        )
    }

    /**
     * Replaces data without changing phase by itself.
     */
    public fun updateData(update: D.() -> D) {
        actions += {
            updateData(update)
        }
    }

    /**
     * Replaces data with access to the typed event payload.
     */
    public fun updateData(update: (D, EV) -> D) {
        actions += {
            val nextData = update(data, event)
            updateData { nextData }
        }
    }

    /**
     * Emits host-executed work from this accepted case.
     *
     * [label] is optional graph metadata. Runtime work is produced by [command].
     */
    public fun command(
        label: String? = null,
        command: AfsmTransitionScope<P, D, E, C, PS, EV>.() -> C,
    ) {
        label?.let { commandLabels += it }
        actions += {
            command(command())
        }
    }

    internal fun buildCase(): AfsmBuiltEventCase<P, D, E, C, PS, EV> {
        return AfsmBuiltEventCase(
            targetLabel = targetLabel,
            targetFactory = targetFactory,
            actions = actions.toList(),
            commandLabels = commandLabels.toList(),
        )
    }

    private fun setTarget(
        label: String,
        factory: AfsmPhaseFactoryScope<P, D, E, PS, EV>.() -> P,
    ) {
        check(targetLabel == null) {
            "Only one transition target can be declared in one Afsm case."
        }
        targetLabel = label
        targetFactory = factory
    }
}

internal data class AfsmBuiltEventCase<P : Any, D : Any, E : Any, C : Any, PS : P, EV : E>(
    val targetLabel: String?,
    val targetFactory: (AfsmPhaseFactoryScope<P, D, E, PS, EV>.() -> P?)?,
    val actions: List<AfsmTransitionScope<P, D, E, C, PS, EV>.() -> Unit>,
    val commandLabels: List<String>,
)

/**
 * Read-only runtime data available to a `case(condition = ...)` predicate.
 *
 * Conditions should decide whether a branch is eligible. They can inspect the
 * typed source [phase], typed [event], and latest [data], but cannot update
 * data or emit commands.
 */
@AfsmDslMarker
public class AfsmConditionScope<P : Any, D : Any, E : Any, PS : P, EV : E> internal constructor(
    public val phase: PS,
    public val event: EV,
    private val readData: () -> D,
) {
    public val data: D
        get() = readData()
}

/**
 * Read-only runtime data available to `transitionTo<PayloadPhase> { ... }`.
 *
 * The factory should only create the target phase. It observes data after
 * source `onExit` and accepted case actions have run, but it cannot mutate the
 * transition.
 */
@AfsmDslMarker
public class AfsmPhaseFactoryScope<P : Any, D : Any, E : Any, PS : P, EV : E> internal constructor(
    public val phase: PS,
    public val event: EV,
    private val readData: () -> D,
) {
    public val data: D
        get() = readData()
}

/**
 * Runtime data available to deferred entry and exit action factories.
 *
 * [phase] is the phase instance being entered or exited. For payload phases
 * declared with `phase<PayloadPhase>`, it is typed as that payload phase.
 * [data] is the latest extended data at the moment the deferred action
 * factory runs.
 */
@AfsmDslMarker
public class AfsmPhaseActionScope<P : Any, D : Any, PS : P> internal constructor(
    public val phase: PS,
    private val readData: () -> D,
) {
    public val data: D
        get() = readData()
}

/**
 * Declaration scope for actions that run when a phase is entered.
 *
 * This block is evaluated when the machine is built. `command(label = ...)`
 * records topology labels immediately and defers its value factory until
 * runtime, so diagram metadata and runtime behavior stay declared in the same
 * statement.
 */
@AfsmDslMarker
public class AfsmEntryScope<P : Any, D : Any, C : Any, PS : P> internal constructor(
    private val phaseMatcher: (P) -> PS?,
) {
    private val actions = mutableListOf<(PS, AfsmDslExecution<P, D, C>) -> Unit>()
    private val commandLabels = mutableListOf<String>()
    private val invocationKeys = mutableListOf<AfsmInvocationKey>()

    /**
     * Replaces data when the phase is entered.
     *
     * The [update] receiver is the runtime data after source `onExit` and
     * accepted case actions have run.
     */
    public fun updateData(update: D.() -> D) {
        actions += { _, execution ->
            execution.data = execution.data.update()
        }
    }

    /**
     * Replaces data with access to the runtime phase payload.
     */
    public fun updateData(update: (D, PS) -> D) {
        actions += { phase, execution ->
            execution.data = update(execution.data, phase)
        }
    }

    /**
     * Emits host-executed work when the phase is entered.
     *
     * [label] is optional topology metadata and should usually match the
     * command's business name. The [command] factory runs at runtime with access
     * to the entered [AfsmPhaseActionScope.phase] and latest
     * [AfsmPhaseActionScope.data].
     */
    public fun command(
        label: String? = null,
        command: AfsmPhaseActionScope<P, D, PS>.() -> C,
    ) {
        label?.let { commandLabels += it }
        actions += { phase, execution ->
            val data = AfsmPhaseActionScope<P, D, PS>(
                phase = phase,
                readData = { execution.data },
            )
            execution.commands += data.command()
        }
    }

    /**
     * Starts long-running command work owned by the entered phase.
     *
     * The runtime executes the command in a tracked child job and cancels it
     * automatically when the machine leaves this phase. Use ordinary [command]
     * for short sequential work. Request ids are still required when remote or
     * non-cooperative work can outlive local coroutine cancellation.
     */
    public fun invoke(
        key: AfsmInvocationKey,
        label: String? = null,
        command: AfsmPhaseActionScope<P, D, PS>.() -> C,
    ) {
        invocationKeys += key
        commandLabels += "invoke ${label ?: key.value}"
        actions += { phase, execution ->
            val data = AfsmPhaseActionScope<P, D, PS>(
                phase = phase,
                readData = { execution.data },
            )
            execution.commandInvocations += AfsmCommandInvocation.Start(
                key = key,
                command = data.command(),
            )
        }
    }

    internal fun buildHandler(): AfsmBuiltPhaseHandler<P, D, C> {
        return AfsmBuiltPhaseHandler(
            commandLabels = commandLabels.toList(),
            invocationKeys = invocationKeys.toList(),
            handler = { phase, execution ->
                val typedPhase = phaseMatcher(phase)
                if (typedPhase != null) {
                    actions.forEach { action ->
                        action(typedPhase, execution)
                    }
                }
            },
        )
    }
}

/**
 * Declaration scope for actions that run when a phase is exited.
 *
 * This block follows the same deferred factory model as [AfsmEntryScope].
 */
@AfsmDslMarker
public class AfsmExitScope<P : Any, D : Any, C : Any, PS : P> internal constructor(
    private val phaseMatcher: (P) -> PS?,
) {
    private val actions = mutableListOf<(PS, AfsmDslExecution<P, D, C>) -> Unit>()
    private val commandLabels = mutableListOf<String>()

    /**
     * Replaces data before the accepted event case runs.
     */
    public fun updateData(update: D.() -> D) {
        actions += { _, execution ->
            execution.data = execution.data.update()
        }
    }

    /**
     * Replaces data with access to the runtime phase payload.
     */
    public fun updateData(update: (D, PS) -> D) {
        actions += { phase, execution ->
            execution.data = update(execution.data, phase)
        }
    }

    /**
     * Emits short sequential host cleanup work when this phase is left.
     *
     * This command cannot interrupt an ordinary command already running in the
     * sequential processor. Use `onEnter { invoke(...) }` when long-running
     * work must be cancelled by leaving its owning phase.
     */
    public fun command(
        label: String? = null,
        command: AfsmPhaseActionScope<P, D, PS>.() -> C,
    ) {
        label?.let { commandLabels += it }
        actions += { phase, execution ->
            val data = AfsmPhaseActionScope<P, D, PS>(
                phase = phase,
                readData = { execution.data },
            )
            execution.commands += data.command()
        }
    }

    internal fun buildHandler(): AfsmBuiltPhaseHandler<P, D, C> {
        return AfsmBuiltPhaseHandler(
            commandLabels = commandLabels.toList(),
            invocationKeys = emptyList(),
            handler = { phase, execution ->
                val typedPhase = phaseMatcher(phase)
                if (typedPhase != null) {
                    actions.forEach { action ->
                        action(typedPhase, execution)
                    }
                }
            },
        )
    }
}

internal data class AfsmBuiltPhaseHandler<P : Any, D : Any, C : Any>(
    val commandLabels: List<String>,
    val invocationKeys: List<AfsmInvocationKey>,
    val handler: AfsmEntryHandler<P, D, C>,
)

/**
 * Runtime scope for a branch declared inside `on<Event>`.
 *
 * [phase] is the current source phase, typed as the enclosing state scope. [event]
 * is the incoming event, typed as the enclosing event scope.
 */
@AfsmDslMarker
public class AfsmTransitionScope<P : Any, D : Any, E : Any, C : Any, PS : P, EV : E> internal constructor(
    public val phase: PS,
    public val event: EV,
    private val execution: AfsmDslExecution<P, D, C>,
) {
    /**
     * Current extended data after earlier data updates in this transition
     * pipeline.
     *
     * For a phase-changing transition this is after source `onExit` has run.
     * For a no-transition case this starts as the current state's data.
     */
    public val data: D
        get() = execution.data

    /**
     * Replaces the current data with an immutable copy.
     *
     * The [update] receiver is the current data. Return the next data
     * value. Subsequent commands or entry handlers observe the updated
     * data through [data].
     */
    public fun updateData(update: D.() -> D) {
        execution.data = execution.data.update()
    }

    /**
     * Emits host-executed work, such as a repository call or timer start.
     *
     * [command] is appended to the accepted transition output. Prefer emitting
     * long-running cancellable work through `onEnter { invoke(...) }` when the
     * work is owned by a phase; emit an ordinary command from a case when short
     * sequential work belongs specifically to one event branch.
     */
    public fun command(command: C) {
        execution.commands += command
    }

}

internal data class AfsmStateDefinition<P : Any, D : Any, E : Any, C : Any>(
    val label: String,
    val matcher: (P) -> Any?,
    val entryHandlers: List<AfsmEntryHandler<P, D, C>>,
    val exitHandlers: List<AfsmExitHandler<P, D, C>>,
    val entryCommandLabels: List<String>,
    val exitCommandLabels: List<String>,
    val invocationKeys: List<AfsmInvocationKey>,
    val eventDefinitions: List<AfsmEventDefinition<P, D, E, C>>,
)

internal data class AfsmEventDefinition<P : Any, D : Any, E : Any, C : Any>(
    val eventLabel: String,
    val eventMatcher: (E) -> Boolean,
    val branches: List<AfsmEventBranch<P, D, E, C>>,
    val transitions: List<AfsmTopologyTransition>,
)

internal fun interface AfsmEventBranch<P : Any, D : Any, E : Any, C : Any> {
    fun tryHandle(
        phase: P,
        event: E,
        execution: AfsmDslExecution<P, D, C>,
    ): AfsmBranchResult<P>
}

internal sealed interface AfsmBranchResult<out P : Any> {
    data object Unmatched : AfsmBranchResult<Nothing>

    data class Matched<P : Any>(
        val targetFactory: (() -> P?)?,
        val decision: AfsmDecision?,
        val execute: () -> Unit,
    ) : AfsmBranchResult<P>
}

internal typealias AfsmEntryHandler<P, D, C> =
    (phase: P, execution: AfsmDslExecution<P, D, C>) -> Unit

internal typealias AfsmExitHandler<P, D, C> =
    (phase: P, execution: AfsmDslExecution<P, D, C>) -> Unit

private open class AfsmDslMachine<P : Any, D : Any, E : Any, C : Any>(
    initialPhase: P,
    private val states: List<AfsmStateDefinition<P, D, E, C>>,
) : AfsmMachine<AfsmState<P, D>, E, C> {
    override val topology: AfsmTopology = AfsmTopology(
        states = states.map { state ->
            AfsmTopologyState(
                id = state.label,
                entryCommandLabels = state.entryCommandLabels,
                exitCommandLabels = state.exitCommandLabels,
            )
        },
        transitions = states.flatMap { state ->
            state.eventDefinitions.flatMap { eventDefinition ->
                eventDefinition.transitions
            }
        }.distinct(),
        initialStateId = states.firstOrNull { state ->
            state.matcher(initialPhase) != null
        }?.label,
    )

    override fun transition(
        state: AfsmState<P, D>,
        event: E,
    ): AfsmTransition<AfsmState<P, D>, C> {
        val stateDefinition = states.firstOrNull { definition ->
            definition.matcher(state.phase) != null
        } ?: return Afsm.invalid(
            state = state,
            reason = "No phase declaration matched the current phase.",
        )

        val eventDefinition = stateDefinition.eventDefinitions.firstOrNull { definition ->
            definition.eventMatcher(event)
        } ?: return Afsm.invalid(
            state = state,
            reason = "No event handler matched the current phase and event.",
        )

        val execution = AfsmDslExecution<P, D, C>(
            data = state.data,
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

        val targetFactory = branchResult.targetFactory
        if (targetFactory != null) {
            applyExitHandlers(
                sourceDefinition = stateDefinition,
                sourcePhase = state.phase,
                execution = execution,
            )
        }

        branchResult.execute()

        val targetPhase = targetFactory?.invoke()
        if (targetPhase != null) {
            applyEntryHandlers(
                targetPhase = targetPhase,
                execution = execution,
            )
        }

        val nextState = AfsmState(
            phase = targetPhase ?: state.phase,
            data = execution.data,
        )

        return when (val decision = branchResult.decision) {
            null -> {
                if (targetPhase != null) {
                    AfsmTransition.transitioned(
                        state = nextState,
                        commands = execution.commands.toList(),
                        commandInvocations = execution.commandInvocations.toList(),
                    )
                } else {
                    AfsmTransition.handled(
                        state = nextState,
                        commands = execution.commands.toList(),
                        commandInvocations = execution.commandInvocations.toList(),
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

            is AfsmDecision.Handled -> AfsmTransition.handled(
                state = nextState,
                reason = decision.reason,
            )

            AfsmDecision.Transitioned -> AfsmTransition.transitioned(
                state = nextState,
                commands = execution.commands.toList(),
                commandInvocations = execution.commandInvocations.toList(),
            )
        }
    }

    private fun applyExitHandlers(
        sourceDefinition: AfsmStateDefinition<P, D, E, C>,
        sourcePhase: P,
        execution: AfsmDslExecution<P, D, C>,
    ) {
        sourceDefinition.invocationKeys.forEach { key ->
            execution.commandInvocations += AfsmCommandInvocation.Cancel(key)
        }
        sourceDefinition.exitHandlers.forEach { handler ->
            handler(sourcePhase, execution)
        }
    }

    private fun applyEntryHandlers(
        targetPhase: P,
        execution: AfsmDslExecution<P, D, C>,
    ) {
        val targetState = states.firstOrNull { definition ->
            definition.matcher(targetPhase) != null
        } ?: return

        targetState.entryHandlers.forEach { handler ->
            handler(targetPhase, execution)
        }
    }
}

private class AfsmDefaultDslMachine<P : Any, D : Any, E : Any, C : Any>(
    override val initialState: AfsmState<P, D>,
    states: List<AfsmStateDefinition<P, D, E, C>>,
) : AfsmDslMachine<P, D, E, C>(
    initialPhase = initialState.phase,
    states = states,
), AfsmDefaultMachine<AfsmState<P, D>, E, C>

internal class AfsmDslExecution<P : Any, D : Any, C : Any>(
    var data: D,
    val commands: MutableList<C> = mutableListOf(),
    val commandInvocations: MutableList<AfsmCommandInvocation<C>> = mutableListOf(),
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
