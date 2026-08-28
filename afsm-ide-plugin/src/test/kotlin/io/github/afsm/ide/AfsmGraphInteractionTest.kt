package io.github.afsm.ide

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AfsmGraphInteractionTest {
    private val diagram = AfsmMmdDiagramParser.parse(
        """
            stateDiagram-v2
              [*] --> Editing
              state Editing
              state Submitting
              Editing --> Submitting: SubmitClicked [valid form] / Login
        """.trimIndent(),
    )

    @Test
    fun `inspect is default and route handles require selected transition in arrange mode`() {
        val transition = diagram.transitions.single()
        val key = AfsmDiagramKeys.transition(0, transition)
        val state = AfsmGraphInteractionState()

        assertEquals(AfsmCanvasMode.INSPECT, state.mode)
        assertFalse(state.canMoveElements)
        assertFalse(state.showsRouteHandles(key))

        state.mode = AfsmCanvasMode.ARRANGE
        state.selection = AfsmDiagramSelection.Transition(key)

        assertTrue(state.canMoveElements)
        assertTrue(state.showsRouteHandles(key))
    }

    @Test
    fun `search selects states before matching transition labels case insensitively`() {
        assertEquals(AfsmDiagramSelection.State("Submitting"), AfsmDiagramSearch.find(diagram, "submitting"))
        assertTrue(AfsmDiagramSearch.find(diagram, "VALID FORM") is AfsmDiagramSelection.Transition)
        assertNull(AfsmDiagramSearch.find(diagram, "missing"))
        assertNull(AfsmDiagramSearch.find(diagram, " "))
    }

    @Test
    fun `transition label separates event guard and command for visual hierarchy`() {
        assertEquals(
            AfsmTransitionLabel("SubmitClicked", "valid form", "Login"),
            AfsmTransitionLabel.parse("SubmitClicked [valid form] / Login"),
        )
        assertEquals(
            AfsmTransitionLabel("AuthSucceeded", null, null),
            AfsmTransitionLabel.parse("AuthSucceeded"),
        )
    }
}
