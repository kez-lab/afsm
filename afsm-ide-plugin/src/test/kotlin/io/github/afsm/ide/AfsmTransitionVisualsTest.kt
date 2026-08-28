package io.github.afsm.ide

import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AfsmTransitionVisualsTest {
    @Test
    fun `long horizontal route receives a forward direction marker`() {
        val markers = AfsmTransitionVisuals.directionMarkers(
            listOf(AfsmDiagramPoint(20.0, 40.0), AfsmDiagramPoint(220.0, 40.0)),
        )

        assertEquals(1, markers.size)
        assertEquals(AfsmDiagramPoint(120.0, 40.0), markers.single().point)
        assertEquals(0.0, markers.single().angle, absoluteTolerance = 0.0001)
    }

    @Test
    fun `top to bottom route marker follows the edge direction`() {
        val marker = AfsmTransitionVisuals.directionMarkers(
            listOf(AfsmDiagramPoint(80.0, 20.0), AfsmDiagramPoint(80.0, 260.0)),
        ).single()

        assertEquals(PI / 2.0, marker.angle, absoluteTolerance = 0.0001)
    }

    @Test
    fun `short route relies on the terminal arrow only`() {
        assertTrue(
            AfsmTransitionVisuals.directionMarkers(
                listOf(AfsmDiagramPoint(0.0, 0.0), AfsmDiagramPoint(40.0, 0.0)),
            ).isEmpty(),
        )
    }

    @Test
    fun `hover text keeps endpoint event guard and command together`() {
        val text = AfsmTransitionVisuals.hoverText(
            AfsmDiagramTransition(
                from = "Editing",
                to = "Submitting",
                label = "SubmitClicked [login form] / Login",
            ),
        )

        assertTrue(text.contains("Editing → Submitting"))
        assertTrue(text.contains("Event: SubmitClicked"))
        assertTrue(text.contains("Guard: login form"))
        assertTrue(text.contains("Command: Login"))
    }
}
