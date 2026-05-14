package afsm.runtime

import afsm.core.Afsm
import afsm.core.AfsmDecision
import afsm.core.AfsmNoEffect
import afsm.core.AfsmReducer
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AfsmHostTest {
    @Test
    fun `dispatch processes external and command events in FIFO order without reentrancy`() = runTest {
        val hostScope = newHostScope()
        val host: AfsmHost<TraceState, TraceEvent, TraceCommand, AfsmNoEffect> = AfsmHost(
            initialState = TraceState(),
            reducer = AfsmReducer { state: TraceState, event: TraceEvent ->
                when (event) {
                    TraceEvent.A -> Afsm.transitionTo(
                        state = state.record("A"),
                        commands = listOf(TraceCommand.DispatchC),
                    )

                    TraceEvent.B -> Afsm.transitionTo(
                        state = state.record("B"),
                    )

                    TraceEvent.C -> Afsm.transitionTo(
                        state = state.record("C"),
                    )
                }
            },
            commandHandler = AfsmCommandHandler { command: TraceCommand, dispatch ->
                when (command) {
                    TraceCommand.DispatchC -> dispatch(TraceEvent.C)
                }
            },
            scope = hostScope,
        )

        host.dispatch(TraceEvent.A)
        host.dispatch(TraceEvent.B)

        advanceUntilIdle()

        assertEquals(listOf("A", "B", "C"), host.state.value.entries)
        hostScope.cancel()
    }

    @Test
    fun `transition emits state then effects then executes commands`() = runTest {
        val hostScope = newHostScope()
        val timeline = mutableListOf<String>()
        lateinit var host: AfsmHost<EffectState, EffectEvent, EffectCommand, EffectOutput>
        host = AfsmHost(
            initialState = EffectState.Idle,
            reducer = AfsmReducer { state: EffectState, event: EffectEvent ->
                when (event) {
                    EffectEvent.Start -> Afsm.transitionTo(
                        state = EffectState.Started,
                        commands = listOf(EffectCommand.Complete),
                        effects = listOf(EffectOutput.ShowStarted),
                    )

                    EffectEvent.Completed -> Afsm.transitionTo(
                        state = EffectState.Completed,
                    )
                }
            },
            commandHandler = AfsmCommandHandler { command: EffectCommand, dispatch ->
                when (command) {
                    EffectCommand.Complete -> {
                        timeline += "command:${host.state.value}"
                        dispatch(EffectEvent.Completed)
                    }
                }
            },
            scope = hostScope,
        )

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            host.effects.collect { effect ->
                timeline += "effect:$effect"
            }
        }

        host.dispatch(EffectEvent.Start)
        advanceUntilIdle()

        assertEquals(EffectState.Completed, host.state.value)
        assertEquals(
            listOf(
                "effect:${EffectOutput.ShowStarted}",
                "command:${EffectState.Started}",
            ),
            timeline,
        )
        hostScope.cancel()
    }

    @Test
    fun `long running command does not block later external events`() = runTest {
        val hostScope = newHostScope()
        val commandGate = CompletableDeferred<Unit>()
        val host: AfsmHost<ResponsiveState, ResponsiveEvent, ResponsiveCommand, AfsmNoEffect> = AfsmHost(
            initialState = ResponsiveState.Idle,
            reducer = AfsmReducer { _: ResponsiveState, event: ResponsiveEvent ->
                when (event) {
                    ResponsiveEvent.Start -> Afsm.transitionTo(
                        state = ResponsiveState.Working,
                        commands = listOf(ResponsiveCommand.LongRunning),
                    )

                    ResponsiveEvent.Edit -> Afsm.transitionTo(
                        state = ResponsiveState.EditedWhileWorking,
                    )

                    ResponsiveEvent.Done -> Afsm.transitionTo(
                        state = ResponsiveState.Done,
                    )
                }
            },
            commandHandler = AfsmCommandHandler { command: ResponsiveCommand, dispatch ->
                when (command) {
                    ResponsiveCommand.LongRunning -> {
                        commandGate.await()
                        dispatch(ResponsiveEvent.Done)
                    }
                }
            },
            scope = hostScope,
        )

        host.dispatch(ResponsiveEvent.Start)
        advanceUntilIdle()

        assertEquals(ResponsiveState.Working, host.state.value)

        host.dispatch(ResponsiveEvent.Edit)
        advanceUntilIdle()

        assertEquals(ResponsiveState.EditedWhileWorking, host.state.value)

        commandGate.complete(Unit)
        advanceUntilIdle()

        assertEquals(ResponsiveState.Done, host.state.value)
        hostScope.cancel()
    }

    @Test
    fun `command queue overflow fails fast instead of suspending event processing`() = runTest {
        val exceptions = mutableListOf<Throwable>()
        val hostScope = newHostScope(
            handler = CoroutineExceptionHandler { _, throwable ->
                exceptions += throwable
            },
        )
        val host: AfsmHost<PressureState, PressureEvent, PressureCommand, AfsmNoEffect> = AfsmHost(
            initialState = PressureState.Idle,
            reducer = AfsmReducer { _: PressureState, _: PressureEvent ->
                Afsm.transitionTo(
                    state = PressureState.Queued,
                    commands = listOf(
                        PressureCommand.First,
                        PressureCommand.Second,
                        PressureCommand.Third,
                    ),
                )
            },
            commandHandler = AfsmCommandHandler { _: PressureCommand, _ -> },
            scope = hostScope,
            config = AfsmConfig(commandQueueCapacity = 1),
        )

        host.dispatch(PressureEvent.Start)
        advanceUntilIdle()

        val thrown = assertIs<AfsmCommandQueueOverflowException>(exceptions.single())
        assertEquals(PressureCommand.Third, thrown.diagnostic.command)
        assertEquals("commandQueueCapacity=1", thrown.diagnostic.reason)
        assertEquals(PressureState.Queued, host.state.value)
        hostScope.cancel()
    }

    @Test
    fun `Stayed keeps same state but may execute cleanup commands`() = runTest {
        val hostScope = newHostScope()
        val handledCommands = mutableListOf<NoEffectCommand>()
        val host: AfsmHost<NoEffectState, NoEffectEvent, NoEffectCommand, AfsmNoEffect> = AfsmHost(
            initialState = NoEffectState.Submitting,
            reducer = AfsmReducer { state: NoEffectState, event: NoEffectEvent ->
                when (event) {
                    NoEffectEvent.CancelRequested -> Afsm.stay(
                        state = state,
                        commands = listOf(NoEffectCommand.CancelRequest),
                        reason = "cancel accepted while request is in flight",
                    )
                }
            },
            commandHandler = AfsmCommandHandler { command: NoEffectCommand, _ ->
                handledCommands += command
            },
            scope = hostScope,
        )

        host.dispatch(NoEffectEvent.CancelRequested)
        advanceUntilIdle()

        assertEquals(NoEffectState.Submitting, host.state.value)
        assertEquals(listOf(NoEffectCommand.CancelRequest), handledCommands)
        hostScope.cancel()
    }

    @Test
    fun `default effects are not replayed to late collectors`() = runTest {
        val hostScope = newHostScope()
        val host: AfsmHost<EffectState, EffectEvent, EffectCommand, EffectOutput> = AfsmHost(
            initialState = EffectState.Idle,
            reducer = AfsmReducer { _: EffectState, event: EffectEvent ->
                when (event) {
                    EffectEvent.Start -> Afsm.transitionTo(
                        state = EffectState.Started,
                        effects = listOf(EffectOutput.ShowStarted),
                    )

                    EffectEvent.Completed -> Afsm.transitionTo(
                        state = EffectState.Completed,
                    )
                }
            },
            commandHandler = AfsmCommandHandler.none(),
            scope = hostScope,
        )

        host.dispatch(EffectEvent.Start)
        advanceUntilIdle()

        val lateEffects = mutableListOf<EffectOutput>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            host.effects.collect { effect ->
                lateEffects += effect
            }
        }
        advanceUntilIdle()

        assertTrue(lateEffects.isEmpty())

        host.dispatch(EffectEvent.Start)
        advanceUntilIdle()

        assertEquals(listOf(EffectOutput.ShowStarted), lateEffects)
        hostScope.cancel()
    }

    @Test
    fun `Ignored keeps current state and executes no outputs`() = runTest {
        val hostScope = newHostScope()
        val diagnostics = mutableListOf<AfsmDiagnostic>()
        val handledCommands = mutableListOf<DecisionCommand>()
        val emittedEffects = mutableListOf<DecisionEffect>()
        val host: AfsmHost<DecisionState, DecisionEvent, DecisionCommand, DecisionEffect> = AfsmHost(
            initialState = DecisionState("current"),
            reducer = AfsmReducer { _: DecisionState, _: DecisionEvent ->
                Afsm.ignore(
                    state = DecisionState("current"),
                    reason = "stale event",
                )
            },
            commandHandler = AfsmCommandHandler { command: DecisionCommand, _ ->
                handledCommands += command
            },
            scope = hostScope,
            config = AfsmConfig(
                logger = AfsmLogger { diagnostic ->
                    diagnostics += diagnostic
                },
            ),
        )

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            host.effects.collect { effect ->
                emittedEffects += effect
            }
        }

        host.dispatch(DecisionEvent.Any)
        advanceUntilIdle()

        assertEquals(DecisionState("current"), host.state.value)
        assertTrue(handledCommands.isEmpty())
        assertTrue(emittedEffects.isEmpty())
        assertTrue(diagnostics.isEmpty())
        hostScope.cancel()
    }

    @Test
    fun `Invalid with Record policy keeps state and records diagnostic`() = runTest {
        val hostScope = newHostScope()
        val diagnostics = mutableListOf<AfsmDiagnostic>()
        val handledCommands = mutableListOf<DecisionCommand>()
        val host: AfsmHost<DecisionState, DecisionEvent, DecisionCommand, DecisionEffect> = AfsmHost(
            initialState = DecisionState("current"),
            reducer = AfsmReducer { _: DecisionState, _: DecisionEvent ->
                Afsm.invalid(
                    state = DecisionState("current"),
                    reason = "result before request",
                )
            },
            commandHandler = AfsmCommandHandler { command: DecisionCommand, _ ->
                handledCommands += command
            },
            scope = hostScope,
            config = AfsmConfig(
                invalidTransitionPolicy = AfsmInvalidTransitionPolicy.Record,
                logger = AfsmLogger { diagnostic ->
                    diagnostics += diagnostic
                },
            ),
        )

        host.dispatch(DecisionEvent.Any)
        advanceUntilIdle()

        assertEquals(DecisionState("current"), host.state.value)
        assertTrue(handledCommands.isEmpty())
        assertEquals("result before request", diagnostics.single().reason)
        hostScope.cancel()
    }

    @Test
    fun `Invalid with Throw policy fails runtime processing coroutine`() = runTest {
        val exceptions = mutableListOf<Throwable>()
        val hostScope = newHostScope(
            handler = CoroutineExceptionHandler { _, throwable ->
                exceptions += throwable
            },
        )
        val host: AfsmHost<DecisionState, DecisionEvent, DecisionCommand, DecisionEffect> = AfsmHost(
            initialState = DecisionState("current"),
            reducer = AfsmReducer { _: DecisionState, _: DecisionEvent ->
                Afsm.invalid(
                    state = DecisionState("current"),
                    reason = "programmer error",
                )
            },
            commandHandler = AfsmCommandHandler.none(),
            scope = hostScope,
            config = AfsmConfig(
                invalidTransitionPolicy = AfsmInvalidTransitionPolicy.Throw,
            ),
        )

        host.dispatch(DecisionEvent.Any)
        advanceUntilIdle()

        val thrown = assertIs<AfsmInvalidTransitionException>(exceptions.single())
        assertEquals("programmer error", thrown.diagnostic.reason)
        hostScope.cancel()
    }

    @Test
    fun `Command failure with Record policy records diagnostic and keeps host alive`() = runTest {
        val hostScope = newHostScope()
        val diagnostics = mutableListOf<AfsmDiagnostic>()
        val host: AfsmHost<DecisionState, DecisionEvent, DecisionCommand, DecisionEffect> = AfsmHost(
            initialState = DecisionState("current"),
            reducer = AfsmReducer { state: DecisionState, _: DecisionEvent ->
                if (state.value == "current") {
                    Afsm.transitionTo(
                        state = DecisionState("afterFirst"),
                        commands = listOf(DecisionCommand.ShouldNotRun),
                    )
                } else {
                    Afsm.transitionTo(
                        state = DecisionState("afterSecond"),
                    )
                }
            },
            commandHandler = AfsmCommandHandler { _: DecisionCommand, _ ->
                error("command boom")
            },
            scope = hostScope,
            config = AfsmConfig(
                commandFailurePolicy = AfsmCommandFailurePolicy.Record,
                logger = AfsmLogger { diagnostic ->
                    diagnostics += diagnostic
                },
            ),
        )

        host.dispatch(DecisionEvent.Any)
        host.dispatch(DecisionEvent.Any)
        advanceUntilIdle()

        assertEquals(DecisionState("afterSecond"), host.state.value)
        assertEquals(DecisionCommand.ShouldNotRun, diagnostics.single().command)
        assertEquals("command boom", diagnostics.single().reason)
        hostScope.cancel()
    }

    @Test
    fun `Command failure with Throw policy fails runtime processing coroutine`() = runTest {
        val exceptions = mutableListOf<Throwable>()
        val hostScope = newHostScope(
            handler = CoroutineExceptionHandler { _, throwable ->
                exceptions += throwable
            },
        )
        val host: AfsmHost<DecisionState, DecisionEvent, DecisionCommand, DecisionEffect> = AfsmHost(
            initialState = DecisionState("current"),
            reducer = AfsmReducer { _: DecisionState, _: DecisionEvent ->
                Afsm.transitionTo(
                    state = DecisionState("afterFailure"),
                    commands = listOf(DecisionCommand.ShouldNotRun),
                )
            },
            commandHandler = AfsmCommandHandler { _: DecisionCommand, _ ->
                error("command boom")
            },
            scope = hostScope,
            config = AfsmConfig(
                commandFailurePolicy = AfsmCommandFailurePolicy.Throw,
            ),
        )

        host.dispatch(DecisionEvent.Any)
        advanceUntilIdle()

        assertIs<IllegalStateException>(exceptions.single())
        assertEquals(DecisionState("afterFailure"), host.state.value)
        hostScope.cancel()
    }

    @Test
    fun `config rejects non-positive queue capacities`() {
        assertFailsWith<IllegalArgumentException> {
            AfsmConfig(eventQueueCapacity = 0)
        }
        assertFailsWith<IllegalArgumentException> {
            AfsmConfig(commandQueueCapacity = 0)
        }
    }

    private fun TestScope.newHostScope(
        handler: CoroutineExceptionHandler? = null,
    ): CoroutineScope {
        val dispatcher = StandardTestDispatcher(testScheduler)
        return if (handler == null) {
            CoroutineScope(dispatcher)
        } else {
            CoroutineScope(dispatcher + handler)
        }
    }

    private data class TraceState(
        val entries: List<String> = emptyList(),
    ) {
        fun record(entry: String): TraceState = copy(entries = entries + entry)
    }

    private enum class TraceEvent {
        A,
        B,
        C,
    }

    private enum class TraceCommand {
        DispatchC,
    }

    private enum class EffectState {
        Idle,
        Started,
        Completed,
    }

    private enum class EffectEvent {
        Start,
        Completed,
    }

    private enum class EffectCommand {
        Complete,
    }

    private enum class EffectOutput {
        ShowStarted,
    }

    private enum class NoEffectState {
        Submitting,
    }

    private enum class NoEffectEvent {
        CancelRequested,
    }

    private enum class NoEffectCommand {
        CancelRequest,
    }

    private enum class ResponsiveState {
        Idle,
        Working,
        EditedWhileWorking,
        Done,
    }

    private enum class ResponsiveEvent {
        Start,
        Edit,
        Done,
    }

    private enum class ResponsiveCommand {
        LongRunning,
    }

    private enum class PressureState {
        Idle,
        Queued,
    }

    private enum class PressureEvent {
        Start,
    }

    private enum class PressureCommand {
        First,
        Second,
        Third,
    }

    private data class DecisionState(
        val value: String,
    )

    private enum class DecisionEvent {
        Any,
    }

    private enum class DecisionCommand {
        ShouldNotRun,
    }

    private enum class DecisionEffect {
        ShouldNotEmit,
    }
}
