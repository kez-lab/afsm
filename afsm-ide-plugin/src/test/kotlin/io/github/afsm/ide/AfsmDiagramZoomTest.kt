package io.github.afsm.ide

import kotlin.test.Test
import kotlin.test.assertEquals

class AfsmDiagramZoomTest {
    @Test
    fun `zoom stays within readable canvas bounds`() {
        assertEquals(0.35, AfsmDiagramZoom.clamp(0.1))
        assertEquals(1.0, AfsmDiagramZoom.clamp(1.0))
        assertEquals(2.5, AfsmDiagramZoom.clamp(4.0))
    }

    @Test
    fun `fit uses the smaller viewport ratio and leaves padding`() {
        assertEquals(0.72, AfsmDiagramZoom.fit(800, 600, 1000, 750), absoluteTolerance = 0.0001)
    }
}
