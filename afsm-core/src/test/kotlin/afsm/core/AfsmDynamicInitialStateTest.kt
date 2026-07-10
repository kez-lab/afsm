package afsm.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AfsmDynamicInitialStateTest {
    @Test
    fun `static machine exposes a genuine default state`() {
        val machine: AfsmDefaultMachine<DynamicState, DynamicEvent, DynamicCommand, AfsmNoEffect> =
            afsmMachine {
                initial(
                    phase = DynamicPhase.Idle,
                    data = DynamicData(productId = 7),
                )
                dynamicFlow()
            }

        assertEquals(DynamicState(DynamicPhase.Idle, DynamicData(productId = 7)), machine.initialState)
        assertEquals("Idle", machine.topology.initialStateId)
    }

    @Test
    fun `dynamic machine needs only an initial phase and accepts real runtime data`() {
        val machine: AfsmMachine<DynamicState, DynamicEvent, DynamicCommand, AfsmNoEffect> =
            afsmMachine(initialPhase = DynamicPhase.Idle) {
                dynamicFlow()
            }

        assertEquals("Idle", machine.topology.initialStateId)

        val result = machine.transition(
            state = DynamicState(
                phase = DynamicPhase.Idle,
                data = DynamicData(productId = 42),
            ),
            event = DynamicEvent.ScreenEntered,
        )

        assertEquals(DynamicPhase.Loading, result.state.phase)
        assertEquals(listOf(DynamicCommand.LoadProduct(productId = 42)), result.commands)
    }

    @Test
    fun `dynamic machine rejects a contradictory default state declaration`() {
        val error = assertFailsWith<IllegalArgumentException> {
            afsmMachine<
                DynamicPhase,
                DynamicData,
                DynamicEvent,
                DynamicCommand,
                AfsmNoEffect,
                >(initialPhase = DynamicPhase.Idle) {
                initial(
                    phase = DynamicPhase.Idle,
                    data = DynamicData(productId = 0),
                )
                dynamicFlow()
            }
        }

        assertEquals(
            "Afsm machine with initialPhase must not also declare initial phase and data.",
            error.message,
        )
    }
}

private fun AfsmMachineBuilder<
    DynamicPhase,
    DynamicData,
    DynamicEvent,
    DynamicCommand,
    AfsmNoEffect,
    >.dynamicFlow() {
    phase(DynamicPhase.Idle) {
        on<DynamicEvent.ScreenEntered> {
            transitionTo(DynamicPhase.Loading)
        }
    }

    phase(DynamicPhase.Loading) {
        onEnter {
            command(label = "LoadProduct") {
                DynamicCommand.LoadProduct(productId = data.productId)
            }
        }
    }
}

private typealias DynamicState = AfsmState<DynamicPhase, DynamicData>

private sealed interface DynamicPhase {
    data object Idle : DynamicPhase
    data object Loading : DynamicPhase
}

private data class DynamicData(
    val productId: Long,
)

private sealed interface DynamicEvent {
    data object ScreenEntered : DynamicEvent
}

private sealed interface DynamicCommand {
    data class LoadProduct(val productId: Long) : DynamicCommand
}
