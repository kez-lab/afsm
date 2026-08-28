package io.github.afsm.ide

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class AfsmDiagramLayoutEngineTest {
    private val diagram = AfsmMmdDiagramParser.parse(
        """
            stateDiagram-v2
              [*] --> Editing
              state Editing
              state Submitting
              state Authenticated
              Editing --> Submitting: SubmitClicked [login form] / Login
              Editing --> Submitting: SubmitClicked [register form] / Register
              Editing --> Editing: SubmitClicked [invalid form]
              Submitting --> Authenticated: AuthSucceeded
              Submitting --> Editing: AuthFailed
        """.trimIndent(),
    )

    @Test
    fun `left to right layout follows primary flow and routes every transition`() {
        val result = ElkAfsmDiagramLayoutEngine().layout(diagram, AfsmDiagramLayoutDirection.LEFT_TO_RIGHT)

        assertTrue(result.nodes.getValue("Editing").x < result.nodes.getValue("Submitting").x)
        assertTrue(result.nodes.getValue("Submitting").x < result.nodes.getValue("Authenticated").x)
        assertEquals(diagram.transitions.size, result.routes.size)
        assertTrue(result.routes.values.all { it.points.size >= 2 })
    }

    @Test
    fun `top to bottom layout follows primary flow vertically`() {
        val result = ElkAfsmDiagramLayoutEngine().layout(diagram, AfsmDiagramLayoutDirection.TOP_TO_BOTTOM)

        assertTrue(result.nodes.getValue("Editing").y < result.nodes.getValue("Submitting").y)
        assertTrue(result.nodes.getValue("Submitting").y < result.nodes.getValue("Authenticated").y)
    }

    @Test
    fun `parallel and reverse transitions keep distinct routed geometry`() {
        val result = ElkAfsmDiagramLayoutEngine().layout(diagram, AfsmDiagramLayoutDirection.LEFT_TO_RIGHT)
        val related = diagram.transitions.withIndex()
            .filter { (_, transition) -> setOf(transition.from, transition.to) == setOf("Editing", "Submitting") }
            .map { indexed -> result.routes.getValue(AfsmDiagramKeys.transition(indexed.index, indexed.value)) }

        assertEquals(3, related.size)
        assertEquals(3, related.map { it.points }.distinct().size)
        assertNotEquals(related[0].label, related[1].label)
    }
}
