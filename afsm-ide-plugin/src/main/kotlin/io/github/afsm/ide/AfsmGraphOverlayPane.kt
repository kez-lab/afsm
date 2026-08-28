package io.github.afsm.ide

import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.JLayeredPane

internal class AfsmGraphOverlayPane(
    private val canvas: JComponent,
    private val actions: JComponent,
    private val context: JComponent,
    private val fab: JComponent,
) : JLayeredPane() {
    init {
        add(canvas, DEFAULT_LAYER)
        add(context, PALETTE_LAYER)
        add(actions, PALETTE_LAYER)
        add(fab, PALETTE_LAYER)
    }

    override fun isOptimizedDrawingEnabled(): Boolean = false

    override fun doLayout() {
        canvas.setBounds(0, 0, width, height)
        val fabSize = boundedSize(fab.preferredSize)
        val fabX = (width - EDGE_MARGIN - fabSize.width).coerceAtLeast(EDGE_MARGIN)
        val fabY = (height - EDGE_MARGIN - fabSize.height).coerceAtLeast(EDGE_MARGIN)
        fab.setBounds(fabX, fabY, fabSize.width, fabSize.height)

        val actionsSize = boundedSize(actions.preferredSize)
        val actionsX = (width - EDGE_MARGIN - actionsSize.width).coerceAtLeast(EDGE_MARGIN)
        val actionsY = (fabY - FLOATING_GAP - actionsSize.height).coerceAtLeast(EDGE_MARGIN)
        actions.setBounds(actionsX, actionsY, actionsSize.width, actionsSize.height)

        val contextSize = boundedSize(context.preferredSize)
        val contextX = (width - EDGE_MARGIN - contextSize.width).coerceAtLeast(EDGE_MARGIN)
        context.setBounds(contextX, EDGE_MARGIN, contextSize.width, contextSize.height)
    }

    private fun boundedSize(preferred: Dimension): Dimension = Dimension(
        preferred.width.coerceAtMost((width - EDGE_MARGIN * 2).coerceAtLeast(0)),
        preferred.height.coerceAtMost((height - EDGE_MARGIN * 2).coerceAtLeast(0)),
    )

    private companion object {
        const val EDGE_MARGIN = 16
        const val FLOATING_GAP = 8
    }
}
