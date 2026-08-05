package afsm.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Definition-time guarantees that keep a machine's graph honest.
 */
class AfsmDefinitionValidationTest {
    private enum class EnumPhase {
        Editing,
        Saving,
        Saved,
    }

    private sealed interface Event {
        data object Save : Event

        data object Done : Event
    }

    private sealed interface WideEvent {
        data class Typed(val value: String) : WideEvent
    }

    @Test
    fun `enum phases are labelled by entry name`() {
        val machine = afsmMachine<EnumPhase, Unit, Event, AfsmNoCommand> {
            initial(EnumPhase.Editing, Unit)

            phase(EnumPhase.Editing) {
                on<Event.Save> { transitionTo(EnumPhase.Saving) }
            }
            phase(EnumPhase.Saving) {
                on<Event.Done> { transitionTo(EnumPhase.Saved) }
            }
            phase(EnumPhase.Saved)
        }

        assertEquals(
            listOf("Editing", "Saving", "Saved"),
            machine.topology.states.map { it.id },
        )
        assertEquals("Editing", machine.topology.initialStateId)
        assertEquals(
            listOf("Editing --Save--> Saving", "Saving --Done--> Saved"),
            machine.topology.transitions.map { "${it.from} --${it.event}--> ${it.to}" },
        )
    }

    @Test
    fun `enum phases transition at runtime`() {
        val machine = afsmMachine<EnumPhase, Unit, Event, AfsmNoCommand> {
            initial(EnumPhase.Editing, Unit)

            phase(EnumPhase.Editing) {
                on<Event.Save> { transitionTo(EnumPhase.Saving) }
            }
            phase(EnumPhase.Saving)
        }

        val transition = machine.transition(machine.initialState, Event.Save)

        assertEquals(EnumPhase.Saving, transition.state.phase)
        assertEquals(AfsmDecision.Transitioned, transition.decision)
    }

    @Test
    fun `an event handler shadowed by an earlier supertype handler fails the build`() {
        val failure = assertFailsWith<AfsmDefinitionException> {
            afsmMachine<EnumPhase, Unit, WideEvent, AfsmNoCommand> {
                initial(EnumPhase.Editing, Unit)

                phase(EnumPhase.Editing) {
                    on<WideEvent> { }
                    on<WideEvent.Typed> { transitionTo(EnumPhase.Saving) }
                }
                phase(EnumPhase.Saving)
            }
        }

        assertTrue(
            failure.message.orEmpty().contains("Typed") &&
                failure.message.orEmpty().contains("unreachable"),
            "Unexpected message: ${failure.message}",
        )
    }

    @Test
    fun `a specific handler declared before the broader handler is allowed`() {
        val machine = afsmMachine<EnumPhase, Unit, WideEvent, AfsmNoCommand> {
            initial(EnumPhase.Editing, Unit)

            phase(EnumPhase.Editing) {
                on<WideEvent.Typed> { transitionTo(EnumPhase.Saving) }
                on<WideEvent> { }
            }
            phase(EnumPhase.Saving)
        }

        val transition = machine.transition(
            machine.initialState,
            WideEvent.Typed("value"),
        )

        assertEquals(EnumPhase.Saving, transition.state.phase)
    }

    @Test
    fun `mixing direct actions with cases explains how to fix the handler`() {
        val failure = assertFailsWith<AfsmDefinitionException> {
            afsmMachine<EnumPhase, Unit, Event, AfsmNoCommand> {
                initial(EnumPhase.Editing, Unit)

                phase(EnumPhase.Editing) {
                    on<Event.Save> {
                        transitionTo(EnumPhase.Saving)
                        ignore(reason = "duplicate save")
                    }
                }
                phase(EnumPhase.Saving)
            }
        }

        assertTrue(
            failure.message.orEmpty().contains("case(...)"),
            "Unexpected message: ${failure.message}",
        )
    }
}
