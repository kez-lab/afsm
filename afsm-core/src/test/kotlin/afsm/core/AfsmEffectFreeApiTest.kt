package afsm.core

import kotlin.test.Test
import kotlin.test.assertEquals

class AfsmEffectFreeApiTest {
    @Test
    fun `machine public contract needs only state event and command types`() {
        val machine: AfsmDefaultMachine<FlowState, FlowEvent, FlowCommand> =
            afsmMachine {
                initial(FlowPhase.Idle, Unit)

                phase(FlowPhase.Idle) {
                    on<FlowEvent.Start> {
                        command { FlowCommand.Load }
                        transitionTo(FlowPhase.Loading)
                    }
                }

                phase(FlowPhase.Loading)
            }

        val result: AfsmTransition<FlowState, FlowCommand> =
            machine.transition(machine.initialState, FlowEvent.Start)

        assertEquals(FlowPhase.Loading, result.state.phase)
        assertEquals(listOf(FlowCommand.Load), result.commands)
    }
}

private typealias FlowState = AfsmState<FlowPhase, Unit>

private sealed interface FlowPhase {
    data object Idle : FlowPhase
    data object Loading : FlowPhase
}

private sealed interface FlowEvent {
    data object Start : FlowEvent
}

private sealed interface FlowCommand {
    data object Load : FlowCommand
}
