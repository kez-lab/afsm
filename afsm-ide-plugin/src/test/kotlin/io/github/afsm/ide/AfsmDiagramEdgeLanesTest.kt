package io.github.afsm.ide

import kotlin.test.Test
import kotlin.test.assertEquals

class AfsmDiagramEdgeLanesTest {
    @Test
    fun `parallel transitions receive separate symmetric lanes`() {
        val lanes = AfsmDiagramEdgeLanes.assign(
            listOf(
                AfsmDiagramTransition("Editing", "Submitting", "Login"),
                AfsmDiagramTransition("Editing", "Submitting", "Register"),
                AfsmDiagramTransition("Submitting", "Authenticated", "Success"),
            ),
        )

        assertEquals(listOf(-0.5, 0.5, 0.0), lanes.map { it.lane })
    }

    @Test
    fun `opposite direction transitions use separate physical lanes`() {
        val transitions = listOf(
            AfsmDiagramTransition("Editing", "Submitting", "Login"),
            AfsmDiagramTransition("Editing", "Submitting", "Register"),
            AfsmDiagramTransition("Submitting", "Editing", "Failed"),
        )

        val lanes = AfsmDiagramEdgeLanes.assign(transitions)

        assertEquals(3, lanes.map { it.physicalLane }.distinct().size)
    }
}
