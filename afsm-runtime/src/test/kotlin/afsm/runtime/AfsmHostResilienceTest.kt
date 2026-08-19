package afsm.runtime

import afsm.core.Afsm
import afsm.core.AfsmCommandInvocation
import afsm.core.AfsmInvocationKey
import afsm.core.AfsmReducer
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.coroutines.CoroutineContext

/**
 * Behavior that keeps a hosted screen usable after a failure.
 *
 * Every recording default is verified by dispatching another event after the
 * failure: a host that stopped accepting events is the failure mode these tests
 * exist to prevent.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AfsmHostResilienceTest {
    @Test
    fun `default config keeps the host usable after a command handler failure`() = runTest {
        val diagnostics = mutableListOf<AfsmDiagnostic>()
        val hostScope = hostScope()
        val host = AfsmHost(
            initialState = CounterState(0),
            reducer = countingReducer(commandOnFirstEvent = true),
            commandHandler = AfsmCommandHandler { _: CounterCommand, _ ->
                error("network boom")
            },
            scope = hostScope,
            config = AfsmConfig(logger = { diagnostic -> diagnostics += diagnostic }),
        )

        host.send(CounterEvent)
        advanceUntilIdle()

        assertTrue(host.isActive)
        assertEquals(AfsmDiagnosticCode.CommandFailure, diagnostics.single().code)

        host.send(CounterEvent)
        advanceUntilIdle()

        assertEquals(CounterState(2), host.state.value)
        hostScope.cancel()
    }

    @Test
    fun `default config keeps the host usable after an invalid transition`() = runTest {
        val diagnostics = mutableListOf<AfsmDiagnostic>()
        val hostScope = hostScope()
        var reductions = 0
        val host = AfsmHost(
            initialState = CounterState(0),
            reducer = AfsmReducer { state: CounterState, _: CounterEvent ->
                reductions += 1
                if (reductions == 1) {
                    Afsm.invalid(state = state, reason = "not allowed here")
                } else {
                    Afsm.transitioned(state = CounterState(state.value + 1))
                }
            },
            commandHandler = AfsmCommandHandler.none(),
            scope = hostScope,
            config = AfsmConfig(logger = { diagnostic -> diagnostics += diagnostic }),
        )

        host.send(CounterEvent)
        host.send(CounterEvent)
        advanceUntilIdle()

        assertEquals(AfsmDiagnosticCode.InvalidTransition, diagnostics.single().code)
        assertEquals(CounterState(1), host.state.value)
        assertTrue(host.isActive)
        hostScope.cancel()
    }

    @Test
    fun `reducer failure is recorded and the host keeps the current state`() = runTest {
        val diagnostics = mutableListOf<AfsmDiagnostic>()
        val hostScope = hostScope()
        var reductions = 0
        val host = AfsmHost(
            initialState = CounterState(0),
            reducer = AfsmReducer { state: CounterState, _: CounterEvent ->
                reductions += 1
                if (reductions == 1) {
                    throw IllegalArgumentException("guard blew up")
                }
                Afsm.transitioned(state = CounterState(state.value + 1))
            },
            commandHandler = AfsmCommandHandler.none(),
            scope = hostScope,
            config = AfsmConfig(logger = { diagnostic -> diagnostics += diagnostic }),
        )

        host.send(CounterEvent)
        advanceUntilIdle()

        assertEquals(AfsmDiagnosticCode.ReducerFailure, diagnostics.single().code)
        assertEquals("IllegalArgumentException", diagnostics.single().failureType)
        assertEquals(CounterState(0), host.state.value)
        assertTrue(host.isActive)

        host.send(CounterEvent)
        advanceUntilIdle()

        assertEquals(CounterState(1), host.state.value)
        hostScope.cancel()
    }

    @Test
    fun `strict config stops the host and records a HostStopped diagnostic`() = runTest {
        val diagnostics = mutableListOf<AfsmDiagnostic>()
        val exceptions = mutableListOf<Throwable>()
        val hostScope = hostScope(
            CoroutineExceptionHandler { _, throwable -> exceptions += throwable },
        )
        val host = AfsmHost(
            initialState = CounterState(0),
            reducer = AfsmReducer { state: CounterState, _: CounterEvent ->
                Afsm.invalid(state = state, reason = "not allowed here")
            },
            commandHandler = AfsmCommandHandler.none(),
            scope = hostScope,
            config = AfsmConfig.strict(logger = { diagnostic -> diagnostics += diagnostic }),
        )

        host.send(CounterEvent)
        advanceUntilIdle()

        val thrown = exceptions.single()
        assertTrue(thrown is AfsmInvalidTransitionException)
        assertEquals(AfsmDiagnosticCode.InvalidTransition, thrown.diagnostic.code)
        assertFalse(host.isActive)

        // A throwing policy carries its diagnostic inside the exception, so the
        // logger only needs to learn that the host stopped.
        assertEquals(
            listOf(AfsmDiagnosticCode.HostStopped),
            diagnostics.map { it.code },
        )
        assertEquals(
            mapOf("processor" to "event processor"),
            diagnostics.single().metadata,
        )
        assertEquals("AfsmInvalidTransitionException", diagnostics.single().failureType)
        hostScope.cancel()
    }

    @Test
    fun `send records a dropped event instead of throwing when the queue is full`() = runTest {
        val diagnostics = mutableListOf<AfsmDiagnostic>()
        val hostScope = hostScope()
        val host = AfsmHost(
            initialState = CounterState(0),
            reducer = countingReducer(),
            commandHandler = AfsmCommandHandler.none(),
            scope = hostScope,
            config = AfsmConfig(
                eventQueueCapacity = 1,
                logger = { diagnostic -> diagnostics += diagnostic },
            ),
        )

        repeat(4) { host.send(CounterEvent) }
        advanceUntilIdle()

        assertTrue(diagnostics.isNotEmpty())
        assertTrue(diagnostics.all { it.code == AfsmDiagnosticCode.EventDropped })
        assertTrue(host.isActive)
        hostScope.cancel()
    }

    @Test
    fun `command handlers run in the configured command context`() = runTest {
        val observed = CompletableDeferred<String?>()
        val hostScope = hostScope()
        val host = AfsmHost(
            initialState = CounterState(0),
            reducer = countingReducer(commandOnFirstEvent = true),
            commandHandler = AfsmCommandHandler { _: CounterCommand, _ ->
                observed.complete(currentCoroutineContext()[CoroutineName]?.name)
            },
            scope = hostScope,
            config = AfsmConfig(commandContext = CoroutineName("afsm-commands")),
        )

        host.send(CounterEvent)
        advanceUntilIdle()

        assertEquals("afsm-commands", observed.getCompleted())
        hostScope.cancel()
    }

    @Test
    fun `a failing invocation does not cancel sibling invocations or the host`() = runTest {
        val diagnostics = mutableListOf<AfsmDiagnostic>()
        val survivorCancelled = CompletableDeferred<Unit>()
        val hostScope = hostScope()
        val failingKey = AfsmInvocationKey("failing")
        val survivingKey = AfsmInvocationKey("surviving")
        val host = AfsmHost(
            initialState = CounterState(0),
            reducer = AfsmReducer { state: CounterState, _: CounterEvent ->
                Afsm.transitioned(
                    state = CounterState(state.value + 1),
                    commandInvocations = listOf(
                        AfsmCommandInvocation.Start(survivingKey, CounterCommand.Survive),
                        AfsmCommandInvocation.Start(failingKey, CounterCommand.Fail),
                    ),
                )
            },
            commandHandler = AfsmCommandHandler { command: CounterCommand, _ ->
                when (command) {
                    CounterCommand.Fail -> error("invocation boom")
                    CounterCommand.Survive -> try {
                        awaitCancellation()
                    } finally {
                        survivorCancelled.complete(Unit)
                    }
                }
            },
            scope = hostScope,
            config = AfsmConfig(logger = { diagnostic -> diagnostics += diagnostic }),
        )

        host.send(CounterEvent)
        advanceUntilIdle()

        assertEquals(AfsmDiagnosticCode.CommandFailure, diagnostics.single().code)
        assertFalse(survivorCancelled.isCompleted)
        assertTrue(host.isActive)
        hostScope.cancel()
    }

    @Test
    fun `duplicate invocation key cancels the stale invocation and records a diagnostic`() = runTest {
        val diagnostics = mutableListOf<AfsmDiagnostic>()
        val started = mutableListOf<Int>()
        val cancelled = mutableListOf<Int>()
        val hostScope = hostScope()
        val key = AfsmInvocationKey("upload")
        var starts = 0
        val host = AfsmHost(
            initialState = CounterState(0),
            reducer = AfsmReducer { state: CounterState, _: CounterEvent ->
                Afsm.transitioned(
                    state = CounterState(state.value + 1),
                    commandInvocations = listOf(
                        AfsmCommandInvocation.Start(key, CounterCommand.Survive),
                    ),
                )
            },
            commandHandler = AfsmCommandHandler { _: CounterCommand, _ ->
                val id = starts++
                started += id
                try {
                    awaitCancellation()
                } finally {
                    cancelled += id
                }
            },
            scope = hostScope,
            config = AfsmConfig(logger = { diagnostic -> diagnostics += diagnostic }),
        )

        host.send(CounterEvent)
        advanceUntilIdle()
        host.send(CounterEvent)
        advanceUntilIdle()

        assertEquals(listOf(0, 1), started)
        assertEquals(listOf(0), cancelled)
        assertEquals(
            AfsmDiagnosticCode.DuplicateInvocationKey,
            diagnostics.single().code,
        )
        assertTrue(host.isActive)
        hostScope.cancel()
    }

    @Test
    fun `a scope without a Job can host a machine`() = runTest {
        // GlobalScope and hand-written CoroutineScope implementations carry no
        // Job element. Reading the parent job must stay optional so hosting one
        // of them does not fail at construction.
        val jobLessScope = object : CoroutineScope {
            override val coroutineContext: CoroutineContext =
                StandardTestDispatcher(testScheduler)
        }
        assertNull(jobLessScope.coroutineContext[Job])

        val host = AfsmHost(
            initialState = CounterState(0),
            reducer = countingReducer(),
            commandHandler = AfsmCommandHandler.none(),
            scope = jobLessScope,
            config = AfsmConfig(),
        )

        host.send(CounterEvent)
        advanceUntilIdle()

        assertEquals(CounterState(1), host.state.value)
        assertTrue(host.isActive)

        host.close()
        advanceUntilIdle()

        assertFalse(host.isActive)
    }

    private fun TestScope.hostScope(
        handler: CoroutineExceptionHandler? = null,
    ): CoroutineScope {
        val base = StandardTestDispatcher(testScheduler) + SupervisorJob()
        return CoroutineScope(if (handler == null) base else base + handler)
    }

    private fun countingReducer(
        commandOnFirstEvent: Boolean = false,
    ): AfsmReducer<CounterState, CounterEvent, CounterCommand> {
        return AfsmReducer { state: CounterState, _: CounterEvent ->
            Afsm.transitioned(
                state = CounterState(state.value + 1),
                commands = if (commandOnFirstEvent && state.value == 0) {
                    listOf(CounterCommand.Fail)
                } else {
                    emptyList()
                },
            )
        }
    }

    private data class CounterState(val value: Int)

    private object CounterEvent

    private enum class CounterCommand {
        Fail,
        Survive,
    }
}
