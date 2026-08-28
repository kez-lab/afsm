package io.github.afsm.ide

import java.awt.Dimension
import java.awt.Rectangle
import javax.swing.JLabel
import javax.swing.JPanel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AfsmGraphOverlayPaneTest {
    @Test
    fun `canvas keeps full width while actions and context float inside viewport`() {
        val canvas = JPanel()
        val actions = JPanel().apply { preferredSize = Dimension(210, 150) }
        val context = JLabel("Transition · Editing → Submitting").apply {
            preferredSize = Dimension(260, 36)
            isVisible = true
        }
        val fab = JPanel().apply { preferredSize = Dimension(44, 44) }
        val pane = AfsmGraphOverlayPane(canvas, actions, context, fab).apply {
            setSize(800, 600)
            doLayout()
        }

        assertEquals(Rectangle(0, 0, 800, 600), canvas.bounds)
        assertTrue(actions.bounds.maxX <= 784)
        assertTrue(actions.bounds.maxY <= fab.y - 8)
        assertTrue(context.bounds.maxX <= 784)
        assertTrue(context.y >= 16)
        assertTrue(fab.bounds.maxX <= 784)
        assertTrue(fab.bounds.maxY <= 584)
    }
}
