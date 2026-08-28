package io.github.afsm.ide

import org.eclipse.elk.alg.layered.options.LayeredOptions
import org.eclipse.elk.core.RecursiveGraphLayoutEngine
import org.eclipse.elk.core.data.LayoutMetaDataService
import org.eclipse.elk.core.math.ElkPadding
import org.eclipse.elk.core.options.CoreOptions
import org.eclipse.elk.core.options.Direction
import org.eclipse.elk.core.options.EdgeRouting
import org.eclipse.elk.core.util.BasicProgressMonitor
import org.eclipse.elk.graph.ElkEdge
import org.eclipse.elk.graph.ElkLabel
import org.eclipse.elk.graph.ElkNode
import org.eclipse.elk.graph.util.ElkGraphUtil
import kotlin.math.max

internal enum class AfsmDiagramLayoutDirection {
    LEFT_TO_RIGHT,
    TOP_TO_BOTTOM,
}

internal interface AfsmDiagramLayoutEngine {
    fun layout(diagram: AfsmStateDiagram, direction: AfsmDiagramLayoutDirection): AfsmDiagramLayout
}

/**
 * Runs ELK's pure-Java layered algorithm and converts the result into Afsm's renderer-neutral layout model.
 * The renderer remains Java2D/Swing and does not depend on JCEF or Android rendering internals.
 */
internal class ElkAfsmDiagramLayoutEngine : AfsmDiagramLayoutEngine {
    override fun layout(
        diagram: AfsmStateDiagram,
        direction: AfsmDiagramLayoutDirection,
    ): AfsmDiagramLayout {
        ensureMetadataRegistered()
        val root = ElkGraphUtil.createGraph().apply {
            setProperty(CoreOptions.ALGORITHM, LayeredOptions.ALGORITHM_ID)
            setProperty(
                CoreOptions.DIRECTION,
                when (direction) {
                    AfsmDiagramLayoutDirection.LEFT_TO_RIGHT -> Direction.RIGHT
                    AfsmDiagramLayoutDirection.TOP_TO_BOTTOM -> Direction.DOWN
                },
            )
            setProperty(CoreOptions.EDGE_ROUTING, EdgeRouting.ORTHOGONAL)
            setProperty(CoreOptions.PADDING, ElkPadding(ROOT_PADDING))
            setProperty(CoreOptions.SEPARATE_CONNECTED_COMPONENTS, true)
            setProperty(CoreOptions.RANDOM_SEED, 1)
            setProperty(LayeredOptions.SPACING_NODE_NODE, NODE_SPACING)
            setProperty(LayeredOptions.SPACING_NODE_NODE_BETWEEN_LAYERS, LAYER_SPACING)
            setProperty(LayeredOptions.SPACING_EDGE_EDGE, EDGE_SPACING)
            setProperty(LayeredOptions.SPACING_EDGE_NODE, EDGE_NODE_SPACING)
            setProperty(LayeredOptions.SPACING_EDGE_LABEL, EDGE_LABEL_SPACING)
            setProperty(LayeredOptions.SPACING_LABEL_NODE, LABEL_NODE_SPACING)
            setProperty(LayeredOptions.SPACING_NODE_SELF_LOOP, SELF_LOOP_SPACING)
        }

        val nodesById = diagram.states.associate { state ->
            state.id to ElkGraphUtil.createNode(root).apply {
                identifier = state.id
                width = NODE_WIDTH
                height = NODE_HEIGHT + max(0, state.notes.size) * NOTE_LINE_HEIGHT
            }
        }
        val edges = diagram.transitions.mapIndexedNotNull { index, transition ->
            val from = nodesById[transition.from] ?: return@mapIndexedNotNull null
            val to = nodesById[transition.to] ?: return@mapIndexedNotNull null
            val key = AfsmDiagramKeys.transition(index, transition)
            val edge = ElkGraphUtil.createSimpleEdge(from, to).apply { identifier = key }
            val label = ElkGraphUtil.createLabel(transition.label, edge).apply {
                identifier = "$key:label"
                width = estimateLabelWidth(transition.label)
                height = if (AfsmTransitionLabel.parse(transition.label).let { it.guard != null || it.command != null }) {
                    DOUBLE_LABEL_HEIGHT
                } else {
                    LABEL_HEIGHT
                }
            }
            EdgeModel(key, edge, label)
        }

        RecursiveGraphLayoutEngine().layout(root, BasicProgressMonitor())

        val nodeLayout = nodesById.mapValues { (_, node) ->
            AfsmDiagramPoint(node.x + CONTENT_OFFSET, node.y + CONTENT_OFFSET)
        }
        val routes = edges.associate { model ->
            val points = model.edge.sections.flatMap { section ->
                buildList {
                    add(AfsmDiagramPoint(section.startX + CONTENT_OFFSET, section.startY + CONTENT_OFFSET))
                    section.bendPoints.forEach { bend ->
                        add(AfsmDiagramPoint(bend.x + CONTENT_OFFSET, bend.y + CONTENT_OFFSET))
                    }
                    add(AfsmDiagramPoint(section.endX + CONTENT_OFFSET, section.endY + CONTENT_OFFSET))
                }
            }.removeConsecutiveDuplicates()
            val label = model.label.takeIf { it.x.isFinite() && it.y.isFinite() }?.let {
                AfsmDiagramPoint(
                    x = it.x + it.width / 2.0 + CONTENT_OFFSET,
                    y = it.y + it.height / 2.0 + CONTENT_OFFSET,
                )
            }
            model.key to AfsmDiagramRoute(points = points, label = label)
        }
        return AfsmDiagramLayout(nodes = nodeLayout, routes = routes)
    }

    private fun List<AfsmDiagramPoint>.removeConsecutiveDuplicates(): List<AfsmDiagramPoint> =
        fold(mutableListOf()) { result, point ->
            if (result.lastOrNull() != point) result += point
            result
        }

    private fun estimateLabelWidth(label: String): Double =
        (label.length * LABEL_CHARACTER_WIDTH + LABEL_HORIZONTAL_PADDING).coerceIn(MIN_LABEL_WIDTH, MAX_LABEL_WIDTH)

    private data class EdgeModel(
        val key: String,
        val edge: ElkEdge,
        val label: ElkLabel,
    )

    private companion object {
        const val NODE_WIDTH = 220.0
        const val NODE_HEIGHT = 72.0
        const val NOTE_LINE_HEIGHT = 22.0
        const val LABEL_HEIGHT = 26.0
        const val DOUBLE_LABEL_HEIGHT = 44.0
        const val LABEL_CHARACTER_WIDTH = 7.4
        const val LABEL_HORIZONTAL_PADDING = 20.0
        const val MIN_LABEL_WIDTH = 64.0
        const val MAX_LABEL_WIDTH = 360.0
        const val ROOT_PADDING = 28.0
        const val CONTENT_OFFSET = 56.0
        const val NODE_SPACING = 84.0
        const val LAYER_SPACING = 180.0
        const val EDGE_SPACING = 28.0
        const val EDGE_NODE_SPACING = 36.0
        const val EDGE_LABEL_SPACING = 16.0
        const val LABEL_NODE_SPACING = 24.0
        const val SELF_LOOP_SPACING = 64.0

        private var metadataRegistered = false

        @Synchronized
        fun ensureMetadataRegistered() {
            if (metadataRegistered) return
            val service = LayoutMetaDataService.getInstance()
            if (service.getAlgorithmData(LayeredOptions.ALGORITHM_ID) == null) {
                service.registerLayoutMetaDataProviders(CoreOptions(), LayeredOptions())
            }
            metadataRegistered = true
        }
    }
}
