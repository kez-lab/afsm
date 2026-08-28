package io.github.afsm.ide

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AfsmMmdDiagramParserTest {
    @Test
    fun `parses Afsm generated states initial state work notes and transitions`() {
        val diagram = AfsmMmdDiagramParser.parse(
            """
                stateDiagram-v2
                  [*] --> Editing
                  state Editing
                  state Submitting
                  note right of Submitting
                    entry / Login
                    exit / cancel login
                  end note
                  Editing --> Submitting: SubmitClicked [valid]
                  Submitting --> Submitting: SubmitClicked [duplicate]
            """.trimIndent(),
        )

        assertEquals("Editing", diagram.initialStateId)
        assertEquals(listOf("Editing", "Submitting"), diagram.states.map { it.id })
        assertEquals(listOf("entry / Login", "exit / cancel login"), diagram.states[1].notes)
        assertEquals(
            listOf("Editing" to "Submitting", "Submitting" to "Submitting"),
            diagram.transitions.map { it.from to it.to },
        )
        assertEquals("SubmitClicked [valid]", diagram.transitions.first().label)
    }

    @Test
    fun `rejects arbitrary Mermaid instead of pretending to render it`() {
        assertFailsWith<IllegalArgumentException> {
            AfsmMmdDiagramParser.parse("graph TD\n  A --> B")
        }
    }
}
