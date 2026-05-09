package afsm.viewmodel

import afsm.core.Afsm
import afsm.core.AfsmNoEffect
import afsm.core.AfsmStateMachine
import afsm.core.AfsmTransition
import afsm.runtime.AfsmCommandHandler
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
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

        viewModel.onEvent(CounterEvent.IncrementClicked)
        mainDispatcher.scheduler.advanceUntilIdle()

        assertEquals(CounterState(count = 1, persisted = true), viewModel.state.value)
        assertEquals(
            listOf<CounterCommand>(CounterCommand.PersistCount(1)),
            viewModel.handledCommands,
        )
    }

    @Test
    fun `ViewModel helper exposes effects from the hosted state machine`() = runTest {
        val effects = mutableListOf<CounterEffect>()
        val viewModel = CounterViewModel()

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effects.collect { effect ->
                effects += effect
            }
        }

        viewModel.onEvent(CounterEvent.DoneClicked)
        mainDispatcher.scheduler.advanceUntilIdle()
        advanceUntilIdle()

        assertEquals(CounterState(count = 0, persisted = false, done = true), viewModel.state.value)
        assertEquals(listOf<CounterEffect>(CounterEffect.NavigateDone), effects)
    }

    private class CounterViewModel : ViewModel() {
        val handledCommands = mutableListOf<CounterCommand>()

        private val host = afsmHost(
            initialState = CounterState(),
            stateMachine = CounterStateMachine(),
            commandHandler = AfsmCommandHandler { command: CounterCommand, dispatch ->
                handledCommands += command
                when (command) {
                    is CounterCommand.PersistCount -> {
                        dispatch(CounterEvent.CountPersisted)
                    }
                }
            },
        )

        val state: StateFlow<CounterState> = host.state
        val effects: Flow<CounterEffect> = host.effects

        fun onEvent(event: CounterEvent) {
            host.dispatch(event)
        }
    }

    private class CounterStateMachine :
        AfsmStateMachine<CounterState, CounterEvent, CounterCommand, CounterEffect> {
        override fun transition(
            state: CounterState,
            event: CounterEvent,
        ): AfsmTransition<CounterState, CounterCommand, CounterEffect> = when (event) {
            CounterEvent.IncrementClicked -> {
                val nextCount = state.count + 1
                Afsm.transitionTo(
                    state = state.copy(count = nextCount),
                    commands = listOf(CounterCommand.PersistCount(nextCount)),
                )
            }

            CounterEvent.CountPersisted -> Afsm.transitionTo(
                state = state.copy(persisted = true),
            )

            CounterEvent.DoneClicked -> Afsm.transitionTo(
                state = state.copy(done = true),
                effects = listOf(CounterEffect.NavigateDone),
            )
        }
    }

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

    private sealed interface CounterEffect {
        data object NavigateDone : CounterEffect
    }

    @Suppress("unused")
    private class NoCommandViewModel : ViewModel() {
        private val host = afsmHost<CounterState, CounterEvent, CounterCommand, AfsmNoEffect>(
            initialState = CounterState(),
            stateMachine = AfsmStateMachine { state: CounterState, _: CounterEvent ->
                Afsm.transitionTo(state = state)
            },
        )

        val state: StateFlow<CounterState> = host.state
    }
}
