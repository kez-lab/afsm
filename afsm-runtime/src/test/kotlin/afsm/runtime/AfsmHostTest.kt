package afsm.runtime

import afsm.core.Afsm
import afsm.core.AfsmDecision
import afsm.core.AfsmReducer
import afsm.core.AfsmTransition
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AfsmHostTest {
    @Test
    fun `command handler exposes dispatchEvent as the result event capability`() = runTest {
        val dispatchedEvents = mutableListOf<String>()
        val handler = AfsmCommandHandler<String, String> { _, dispatchEvent ->
            dispatchEvent("saved")
        }

        handler.handle(
            command = "save",
            dispatchEvent = { event -> dispatchedEvents += event },
        )

        assertEquals(listOf("saved"), dispatchedEvents)
    }

    @Test
    fun `dispatch processes external and command events in FIFO order without reentrancy`() = runTest {
        val hostScope = newHostScope()
        val host: AfsmHost<TraceState, TraceEvent, TraceCommand> = AfsmHost(
            initialState = TraceState(),
            reducer = AfsmReducer { state: TraceState, event: TraceEvent ->
                when (event) {
                    TraceEvent.A -> Afsm.transitioned(
                        state = state.record("A"),
                        commands = listOf(TraceCommand.DispatchC),
                    )

                    TraceEvent.B -> Afsm.transitioned(
                        state = state.record("B"),
                    )

                    TraceEvent.C -> Afsm.transitioned(
                        state = state.record("C"),
                    )
                }
            },
            commandHandler = AfsmCommandHandler { command: TraceCommand, dispatchEvent ->
                when (command) {
                    TraceCommand.DispatchC -> dispatchEvent(TraceEvent.C)
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
    fun `transition publishes state before executing commands`() = runTest {
        val hostScope = newHostScope()
        val timeline = mutableListOf<String>()
        lateinit var host: AfsmHost<WorkState, WorkEvent, WorkCommand>
        host = AfsmHost(
            initialState = WorkState.Idle,
            reducer = AfsmReducer { state: WorkState, event: WorkEvent ->
                when (event) {
                    WorkEvent.Start -> Afsm.transitioned(
                        state = WorkState.Started,
                        commands = listOf(WorkCommand.Complete),
                    )

                    WorkEvent.Completed -> Afsm.transitioned(
                        state = WorkState.Completed,
                    )
                }
            },
            commandHandler = AfsmCommandHandler { command: WorkCommand, dispatchEvent ->
                when (command) {
                    WorkCommand.Complete -> {
                        timeline += "command:${host.state.value}"
                        dispatchEvent(WorkEvent.Completed)
                    }
                }
            },
            scope = hostScope,
        )

        host.dispatch(WorkEvent.Start)
        advanceUntilIdle()

        assertEquals(WorkState.Completed, host.state.value)
        assertEquals(listOf("command:${WorkState.Started}"), timeline)
        hostScope.cancel()
    }

    @Test
    fun `long running command does not block later external events`() = runTest {
        val hostScope = newHostScope()
        val commandGate = CompletableDeferred<Unit>()
        val host: AfsmHost<ResponsiveState, ResponsiveEvent, ResponsiveCommand> = AfsmHost(
            initialState = ResponsiveState.Idle,
            reducer = AfsmReducer { _: ResponsiveState, event: ResponsiveEvent ->
                when (event) {
                    ResponsiveEvent.Start -> Afsm.transitioned(
                        state = ResponsiveState.Working,
                        commands = listOf(ResponsiveCommand.LongRunning),
                    )

                    ResponsiveEvent.Edit -> Afsm.transitioned(
                        state = ResponsiveState.EditedWhileWorking,
                    )

                    ResponsiveEvent.Done -> Afsm.transitioned(
                        state = ResponsiveState.Done,
                    )
                }
            },
            commandHandler = AfsmCommandHandler { command: ResponsiveCommand, dispatchEvent ->
                when (command) {
                    ResponsiveCommand.LongRunning -> {
                        commandGate.await()
                        dispatchEvent(ResponsiveEvent.Done)
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
        val host: AfsmHost<PressureState, PressureEvent, PressureCommand> = AfsmHost(
            initialState = PressureState.Idle,
            reducer = AfsmReducer { _: PressureState, _: PressureEvent ->
                Afsm.transitioned(
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
        assertEquals(AfsmDiagnosticCode.CommandQueueOverflow, thrown.diagnostic.code)
        assertEquals("PressureCommand", thrown.diagnostic.commandType)
        assertEquals(mapOf("capacity" to "1"), thrown.diagnostic.metadata)
        assertNull(thrown.diagnostic.values)
        assertEquals(PressureState.Queued, host.state.value)
        hostScope.cancel()
    }

    @Test
    fun `command result event overflow fails fast instead of suspending command processing`() = runTest {
        val exceptions = mutableListOf<Throwable>()
        val hostScope = newHostScope(
            handler = CoroutineExceptionHandler { _, throwable ->
                exceptions += throwable
            },
        )
        val host: AfsmHost<PressureState, PressureEvent, PressureCommand> = AfsmHost(
            initialState = PressureState.Idle,
            reducer = AfsmReducer { state: PressureState, event: PressureEvent ->
                when (event) {
                    PressureEvent.Start -> Afsm.transitioned(
                        state = PressureState.Queued,
                        commands = listOf(PressureCommand.First),
                    )

                    PressureEvent.ResultOne -> Afsm.transitioned(
                        state = state,
                    )

                    PressureEvent.ResultTwo -> Afsm.transitioned(
                        state = state,
                    )

                    PressureEvent.ResultThree -> Afsm.transitioned(
                        state = state,
                    )
                }
            },
            commandHandler = AfsmCommandHandler { _: PressureCommand, dispatchEvent ->
                dispatchEvent(PressureEvent.ResultOne)
                dispatchEvent(PressureEvent.ResultTwo)
                dispatchEvent(PressureEvent.ResultThree)
            },
            scope = hostScope,
            config = AfsmConfig(eventQueueCapacity = 1),
        )

        host.dispatch(PressureEvent.Start)
        advanceUntilIdle()

        val thrown = assertIs<AfsmEventQueueOverflowException>(exceptions.single())
        assertEquals(AfsmDiagnosticCode.CommandResultQueueOverflow, thrown.diagnostic.code)
        assertEquals("PressureEvent", thrown.diagnostic.eventType)
        assertEquals("PressureCommand", thrown.diagnostic.commandType)
        assertEquals(mapOf("capacity" to "1"), thrown.diagnostic.metadata)
        assertNull(thrown.diagnostic.values)
        assertEquals(PressureState.Queued, host.state.value)
        hostScope.cancel()
    }

    @Test
    fun `command result event after host close is dropped as lifecycle completion`() = runTest {
        val exceptions = mutableListOf<Throwable>()
        val diagnostics = mutableListOf<AfsmDiagnostic>()
        val commandStarted = CompletableDeferred<Unit>()
        val releaseCommand = CompletableDeferred<Unit>()
        val hostScope = newHostScope(
            handler = CoroutineExceptionHandler { _, throwable ->
                exceptions += throwable
            },
        )
        val host: AfsmHost<PressureState, PressureEvent, PressureCommand> = AfsmHost(
            initialState = PressureState.Idle,
            reducer = AfsmReducer { state: PressureState, event: PressureEvent ->
                when (event) {
                    PressureEvent.Start -> Afsm.transitioned(
                        state = PressureState.Queued,
                        commands = listOf(PressureCommand.First),
                    )

                    PressureEvent.ResultOne,
                    PressureEvent.ResultTwo,
                    PressureEvent.ResultThree -> Afsm.transitioned(
                        state = state,
                    )
                }
            },
            commandHandler = AfsmCommandHandler { _: PressureCommand, dispatchEvent ->
                commandStarted.complete(Unit)
                withContext(NonCancellable) {
                    releaseCommand.await()
                    dispatchEvent(PressureEvent.ResultOne)
                }
            },
            scope = hostScope,
            config = AfsmConfig(
                logger = AfsmLogger { diagnostic ->
                    diagnostics += diagnostic
                },
            ),
        )

        host.dispatch(PressureEvent.Start)
        advanceUntilIdle()
        commandStarted.await()

        host.close()
        releaseCommand.complete(Unit)
        advanceUntilIdle()

        assertTrue(
            exceptions.isEmpty(),
            "Expected no coroutine exceptions, got $exceptions",
        )
        assertEquals(PressureState.Queued, host.state.value)
        assertEquals(
            AfsmDiagnosticCode.CommandResultDroppedHostClosed,
            diagnostics.single().code,
        )
        assertEquals("PressureEvent", diagnostics.single().eventType)
        assertEquals("PressureCommand", diagnostics.single().commandType)
        assertNull(diagnostics.single().values)
        hostScope.cancel()
    }

    @Test
    fun `Handled transition keeps current state but may execute cleanup commands`() = runTest {
        val hostScope = newHostScope()
        val handledCommands = mutableListOf<NoWorkCommand>()
        val host: AfsmHost<NoWorkState, NoWorkEvent, NoWorkCommand> = AfsmHost(
            initialState = NoWorkState.Submitting,
            reducer = AfsmReducer { state: NoWorkState, event: NoWorkEvent ->
                when (event) {
                    NoWorkEvent.CancelRequested -> AfsmTransition.handled(
                        state = state,
                        commands = listOf(NoWorkCommand.CancelRequest),
                        reason = "cancel accepted while request is in flight",
                    )
                }
            },
            commandHandler = AfsmCommandHandler { command: NoWorkCommand, _ ->
                handledCommands += command
            },
            scope = hostScope,
        )

        host.dispatch(NoWorkEvent.CancelRequested)
        advanceUntilIdle()

        assertEquals(NoWorkState.Submitting, host.state.value)
        assertEquals(listOf(NoWorkCommand.CancelRequest), handledCommands)
        hostScope.cancel()
    }

    @Test
    fun `accepted transition remains available as durable current state`() = runTest {
        val hostScope = newHostScope()
        val host: AfsmHost<WorkState, WorkEvent, WorkCommand> = AfsmHost(
            initialState = WorkState.Idle,
            reducer = AfsmReducer { _: WorkState, event: WorkEvent ->
                when (event) {
                    WorkEvent.Start -> Afsm.transitioned(
                        state = WorkState.Started,
                    )

                    WorkEvent.Completed -> Afsm.transitioned(
                        state = WorkState.Completed,
                    )
                }
            },
            commandHandler = AfsmCommandHandler.none(),
            scope = hostScope,
        )

        host.dispatch(WorkEvent.Start)
        advanceUntilIdle()

        assertEquals(WorkState.Started, host.state.value)
        hostScope.cancel()
    }

    @Test
    fun `Ignored keeps current state and executes no outputs`() = runTest {
        val hostScope = newHostScope()
        val diagnostics = mutableListOf<AfsmDiagnostic>()
        val handledCommands = mutableListOf<DecisionCommand>()
        val host: AfsmHost<DecisionState, DecisionEvent, DecisionCommand> = AfsmHost(
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

        host.dispatch(DecisionEvent.Any)
        advanceUntilIdle()

        assertEquals(DecisionState("current"), host.state.value)
        assertTrue(handledCommands.isEmpty())
        assertTrue(diagnostics.isEmpty())
        hostScope.cancel()
    }

    @Test
    fun `Invalid with Record policy keeps state and records diagnostic`() = runTest {
        val hostScope = newHostScope()
        val diagnostics = mutableListOf<AfsmDiagnostic>()
        val handledCommands = mutableListOf<DecisionCommand>()
        val host: AfsmHost<DecisionState, DecisionEvent, DecisionCommand> = AfsmHost(
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
        assertEquals(AfsmDiagnosticCode.InvalidTransition, diagnostics.single().code)
        assertEquals(AfsmDiagnosticDecision.Invalid, diagnostics.single().decision)
        assertEquals("Invalid Afsm transition.", diagnostics.single().message)
        assertNull(diagnostics.single().values)
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
        val host: AfsmHost<DecisionState, DecisionEvent, DecisionCommand> = AfsmHost(
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
        assertEquals(AfsmDiagnosticCode.InvalidTransition, thrown.diagnostic.code)
        assertEquals("Invalid Afsm transition.", thrown.diagnostic.message)
        assertNull(thrown.diagnostic.values)
        hostScope.cancel()
    }

    @Test
    fun `Command failure with Record policy records diagnostic and keeps host alive`() = runTest {
        val hostScope = newHostScope()
        val diagnostics = mutableListOf<AfsmDiagnostic>()
        val host: AfsmHost<DecisionState, DecisionEvent, DecisionCommand> = AfsmHost(
            initialState = DecisionState("current"),
            reducer = AfsmReducer { state: DecisionState, _: DecisionEvent ->
                if (state.value == "current") {
                    Afsm.transitioned(
                        state = DecisionState("afterFirst"),
                        commands = listOf(DecisionCommand.ShouldNotRun),
                    )
                } else {
                    Afsm.transitioned(
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
        assertEquals(AfsmDiagnosticCode.CommandFailure, diagnostics.single().code)
        assertEquals("DecisionCommand", diagnostics.single().commandType)
        assertEquals("IllegalStateException", diagnostics.single().failureType)
        assertNull(diagnostics.single().values)
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
        val host: AfsmHost<DecisionState, DecisionEvent, DecisionCommand> = AfsmHost(
            initialState = DecisionState("current"),
            reducer = AfsmReducer { _: DecisionState, _: DecisionEvent ->
                Afsm.transitioned(
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

    private enum class WorkState {
        Idle,
        Started,
        Completed,
    }

    private enum class WorkEvent {
        Start,
        Completed,
    }

    private enum class WorkCommand {
        Complete,
    }

    private enum class NoWorkState {
        Submitting,
    }

    private enum class NoWorkEvent {
        CancelRequested,
    }

    private enum class NoWorkCommand {
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
        ResultOne,
        ResultTwo,
        ResultThree,
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

}
