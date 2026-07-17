package afsm.viewmodel

import afsm.core.Afsm
import afsm.core.AfsmDefaultMachine
import afsm.core.AfsmMachine
import afsm.core.AfsmReducer
import afsm.core.AfsmTopology
import afsm.core.AfsmTransition
import afsm.runtime.AfsmCommandHandler
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class AfsmViewModelTest {
    private val mainDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `ViewModel helper lets Android developers expose state and dispatch events with minimal adapter code`() = runTest {
        val viewModel = CounterViewModel()

        viewModel.increment()
        mainDispatcher.scheduler.advanceUntilIdle()

        assertEquals(CounterState(count = 1, persisted = true), viewModel.state.value)
        assertEquals(
            listOf<CounterCommand>(CounterCommand.PersistCount(1)),
            viewModel.handledCommands,
        )
    }

    @Test
    fun `ViewModel helper keeps completion as observable state`() = runTest {
        val viewModel = CounterViewModel()

        viewModel.done()
        mainDispatcher.scheduler.advanceUntilIdle()

        assertEquals(CounterState(count = 0, persisted = false, done = true), viewModel.state.value)
    }

    @Test
    fun `ViewModel helper can host a machine with dynamic initial state`() = runTest {
        val viewModel = DynamicInitialCounterViewModel(initialCount = 41)

        viewModel.increment()
        mainDispatcher.scheduler.advanceUntilIdle()

        assertEquals(CounterState(count = 42, persisted = true), viewModel.state.value)
        assertEquals(
            listOf<CounterCommand>(CounterCommand.PersistCount(42)),
            viewModel.handledCommands,
        )
    }

    private class CounterViewModel : ViewModel() {
        val handledCommands = mutableListOf<CounterCommand>()

        private val host = afsmHost(
            machine = CounterStateMachine,
            commandHandler = AfsmCommandHandler { command: CounterCommand, dispatchEvent ->
                handledCommands += command
                when (command) {
                    is CounterCommand.PersistCount -> {
                        dispatchEvent(CounterEvent.CountPersisted)
                    }
                }
            },
        )

        val state: StateFlow<CounterState> = host.state
        fun increment() = host.dispatch(CounterEvent.IncrementClicked)

        fun done() = host.dispatch(CounterEvent.DoneClicked)
    }

    private class DynamicInitialCounterViewModel(
        initialCount: Int,
    ) : ViewModel() {
        val handledCommands = mutableListOf<CounterCommand>()

        private val host = afsmHost(
            machine = DynamicCounterStateMachine,
            initialState = CounterState(count = initialCount),
            commandHandler = AfsmCommandHandler { command: CounterCommand, dispatchEvent ->
                handledCommands += command
                when (command) {
                    is CounterCommand.PersistCount -> {
                        dispatchEvent(CounterEvent.CountPersisted)
                    }
                }
            },
        )

        val state: StateFlow<CounterState> = host.state

        fun increment() = host.dispatch(CounterEvent.IncrementClicked)
    }

    private object CounterStateMachine :
        AfsmDefaultMachine<CounterState, CounterEvent, CounterCommand> {
        override val initialState: CounterState = CounterState()
        override val topology: AfsmTopology = AfsmTopology(
            states = emptyList(),
            transitions = emptyList(),
        )

        override fun transition(
            state: CounterState,
            event: CounterEvent,
        ): AfsmTransition<CounterState, CounterCommand> = when (event) {
            CounterEvent.IncrementClicked -> {
                val nextCount = state.count + 1
                Afsm.transitioned(
                    state = state.copy(count = nextCount),
                    commands = listOf(CounterCommand.PersistCount(nextCount)),
                )
            }

            CounterEvent.CountPersisted -> Afsm.transitioned(
                state = state.copy(persisted = true),
            )

            CounterEvent.DoneClicked -> Afsm.transitioned(
                state = state.copy(done = true),
            )
        }
    }

    private object DynamicCounterStateMachine :
        AfsmMachine<CounterState, CounterEvent, CounterCommand> by CounterStateMachine

    private data class CounterState(
        val count: Int = 0,
        val persisted: Boolean = false,
        val done: Boolean = false,
    )

    private sealed interface CounterEvent {
        data object IncrementClicked : CounterEvent
        data object CountPersisted : CounterEvent
        data object DoneClicked : CounterEvent
    }

    private sealed interface CounterCommand {
        data class PersistCount(val count: Int) : CounterCommand
    }

    @Suppress("unused")
    private class NoCommandViewModel : ViewModel() {
        private val host = afsmHost<CounterState, CounterEvent, CounterCommand>(
            initialState = CounterState(),
            reducer = AfsmReducer { state: CounterState, _: CounterEvent ->
                Afsm.transitioned(state = state)
            },
        )

        val state: StateFlow<CounterState> = host.state
    }
}
