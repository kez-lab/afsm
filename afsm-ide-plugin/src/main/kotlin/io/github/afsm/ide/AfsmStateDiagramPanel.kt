package io.github.afsm.ide

import com.intellij.openapi.application.ApplicationManager
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Cursor
import java.awt.Dimension
import java.awt.Font
import java.awt.FontMetrics
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Point
import java.awt.Rectangle
import java.awt.RenderingHints
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.geom.Line2D
import java.awt.geom.Path2D
import java.awt.geom.Point2D
import java.awt.geom.QuadCurve2D
import java.awt.geom.Rectangle2D
import javax.swing.JPanel
import javax.swing.JViewport
import javax.swing.SwingUtilities
import javax.swing.ToolTipManager
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/** A deterministic, JCEF-free inspection canvas for Afsm's generated state diagram dialect. */
internal class AfsmStateDiagramPanel(
    private val layoutEngine: AfsmDiagramLayoutEngine = ElkAfsmDiagramLayoutEngine(),
) : JPanel() {
    private var diagram: AfsmStateDiagram? = null
    private var layout = AfsmDiagramLayout()
    private var direction = AfsmDiagramLayoutDirection.LEFT_TO_RIGHT
    private val interaction = AfsmGraphInteractionState()
    private var onLayoutChanged: (AfsmDiagramLayout) -> Unit = {}
    private var onZoomChanged: (Int) -> Unit = {}
    private var onSelectionChanged: (AfsmDiagramSelectionDetails?) -> Unit = {}
    private var renderedNodes: List<Node> = emptyList()
    private var renderedRoutes: Map<String, RenderedRoute> = emptyMap()
    private var renderedHandles: List<RouteHandle> = emptyList()
    private var drag: Drag? = null
    private var hoveredTransitionKey: String? = null
    private var zoom = 1.0
    private var contentSizeAt100 = Dimension(MIN_CANVAS_WIDTH, MIN_CANVAS_HEIGHT)

    init {
        background = JBColor.PanelBackground
        border = JBUI.Borders.empty()
        isFocusable = true
        ToolTipManager.sharedInstance().registerComponent(this)

        val mouseHandler = object : MouseAdapter() {
            override fun mousePressed(event: MouseEvent) {
                if (event.button != MouseEvent.BUTTON1) return
                requestFocusInWindow()
                val point = toModelPoint(event.point)

                if (interaction.canMoveElements) {
                    renderedHandles.minByOrNull { distance(point, it.point) }
                        ?.takeIf { distance(point, it.point) <= HANDLE_HIT_RADIUS }
                        ?.let { handle ->
                            drag = Drag.RoutePoint(handle.key, handle.index, handle.synthetic)
                            cursor = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR)
                            return
                        }
                }

                renderedNodes.lastOrNull { it.bounds.contains(point) }?.let { node ->
                    updateSelection(AfsmDiagramSelection.State(node.id))
                    if (interaction.canMoveElements) {
                        drag = Drag.Node(node.id, point.x - node.bounds.x, point.y - node.bounds.y)
                        cursor = Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR)
                    }
                    return
                }

                hitTransition(point)?.let { key ->
                    updateSelection(AfsmDiagramSelection.Transition(key))
                    return
                }

                updateSelection(null)
                viewport()?.let { viewport ->
                    drag = Drag.Pan(event.locationOnScreen, viewport.viewPosition)
                    cursor = Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR)
                }
            }

            override fun mouseReleased(event: MouseEvent) {
                if (drag is Drag.Node || drag is Drag.RoutePoint) onLayoutChanged(layout)
                drag = null
                cursor = Cursor.getDefaultCursor()
            }

            override fun mouseDragged(event: MouseEvent) = updateDrag(event)

            override fun mouseMoved(event: MouseEvent) {
                val point = toModelPoint(event.point)
                val hoveredTransition = hitTransition(point)
                if (hoveredTransitionKey != hoveredTransition) {
                    hoveredTransitionKey = hoveredTransition
                    rebuildRenderModel()
                    repaint()
                }
                cursor = when {
                    interaction.canMoveElements && renderedHandles.any {
                        distance(point, it.point) <= HANDLE_HIT_RADIUS
                    } -> Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR)
                    renderedNodes.any { it.bounds.contains(point) } ->
                        if (interaction.canMoveElements) Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR)
                        else Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                    hoveredTransition != null -> Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                    else -> Cursor.getDefaultCursor()
                }
            }

            override fun mouseExited(event: MouseEvent) {
                if (hoveredTransitionKey != null) {
                    hoveredTransitionKey = null
                    rebuildRenderModel()
                    repaint()
                }
                cursor = Cursor.getDefaultCursor()
            }

            override fun mouseWheelMoved(event: java.awt.event.MouseWheelEvent) {
                if (!event.isControlDown && !event.isMetaDown) return
                event.consume()
                val factor = if (event.preciseWheelRotation < 0) ZOOM_STEP else 1.0 / ZOOM_STEP
                zoomBy(factor, event.point)
            }
        }
        addMouseListener(mouseHandler)
        addMouseMotionListener(mouseHandler)
        addMouseWheelListener(mouseHandler)
    }

    fun show(diagram: AfsmStateDiagram) = show(diagram, AfsmDiagramLayout())

    fun show(
        diagram: AfsmStateDiagram,
        layout: AfsmDiagramLayout,
        onLayoutChanged: (AfsmDiagramLayout) -> Unit = {},
    ) {
        this.diagram = diagram
        this.onLayoutChanged = onLayoutChanged
        val automatic = automaticLayout(diagram)
        this.layout = automatic.copy(
            nodes = automatic.nodes + layout.nodes,
            controls = layout.controls,
            routes = automatic.routes + layout.routes,
        )
        updateSelection(null)
        rebuildRenderModel()
        refreshCanvasBounds()
        repaint()
    }

    fun setOnZoomChanged(listener: (Int) -> Unit) {
        onZoomChanged = listener
        listener(zoomPercent())
    }

    fun setOnSelectionChanged(listener: (AfsmDiagramSelectionDetails?) -> Unit) {
        onSelectionChanged = listener
        listener(selectionDetails())
    }

    fun setMode(mode: AfsmCanvasMode) {
        interaction.mode = mode
        drag = null
        rebuildRenderModel()
        repaint()
    }

    fun setDirection(direction: AfsmDiagramLayoutDirection) {
        this.direction = direction
    }

    fun autoLayout() {
        val model = diagram ?: return
        layout = automaticLayout(model)
        onLayoutChanged(layout)
        rebuildRenderModel()
        refreshCanvasBounds()
        repaint()
    }

    fun zoomIn() = zoomBy(ZOOM_STEP)

    fun zoomOut() = zoomBy(1.0 / ZOOM_STEP)

    fun actualSize() = setZoom(1.0)

    fun fitTo(viewportSize: Dimension) {
        setZoom(
            AfsmDiagramZoom.fit(
                viewportWidth = viewportSize.width,
                viewportHeight = viewportSize.height,
                contentWidth = contentSizeAt100.width,
                contentHeight = contentSizeAt100.height,
            ),
        )
        viewport()?.viewPosition = Point(0, 0)
    }

    fun resetLayout() = autoLayout()

    fun findAndSelect(query: String): Boolean {
        val model = diagram ?: return false
        val selection = AfsmDiagramSearch.find(model, query) ?: return false
        updateSelection(selection)
        focusSelection()
        return true
    }

    fun focusSelection() {
        val modelBounds = when (val selection = interaction.selection) {
            is AfsmDiagramSelection.State -> renderedNodes.firstOrNull { it.id == selection.id }?.bounds
            is AfsmDiagramSelection.Transition -> renderedRoutes[selection.key]?.bounds()
            null -> null
        } ?: return
        val scaled = Rectangle(
            ((modelBounds.x - FOCUS_PADDING) * zoom).roundToInt().coerceAtLeast(0),
            ((modelBounds.y - FOCUS_PADDING) * zoom).roundToInt().coerceAtLeast(0),
            ((modelBounds.width + FOCUS_PADDING * 2) * zoom).roundToInt().coerceAtLeast(1),
            ((modelBounds.height + FOCUS_PADDING * 2) * zoom).roundToInt().coerceAtLeast(1),
        )
        scrollRectToVisible(scaled)
    }

    override fun paintComponent(graphics: Graphics) {
        super.paintComponent(graphics)
        val model = diagram ?: return
        val g = graphics.create() as Graphics2D
        try {
            g.scale(zoom, zoom)
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
            rebuildRenderModel()

            renderedRoutes.values.forEach { route -> drawTransition(g, route) }
            model.initialStateId?.let { initial ->
                renderedNodes.firstOrNull { it.id == initial }?.let { drawInitial(g, it) }
            }
            renderedNodes.forEach { node -> drawNode(g, node) }
            renderedHandles.forEach { handle -> drawRouteHandle(g, handle) }
        } finally {
            g.dispose()
        }
    }

    override fun getToolTipText(event: MouseEvent): String? {
        val point = toModelPoint(event.point)
        renderedNodes.lastOrNull { it.bounds.contains(point) }?.let { node ->
            val model = diagram ?: return null
            val state = model.states.firstOrNull { it.id == node.id } ?: return null
            return htmlTooltip(
                title = state.id,
                rows = buildList {
                    if (model.initialStateId == state.id) add("Initial" to "Yes")
                    add("Incoming" to model.transitions.count { it.to == state.id }.toString())
                    add("Outgoing" to model.transitions.count { it.from == state.id }.toString())
                    state.notes.forEachIndexed { index, note -> add("Note ${index + 1}" to note) }
                },
            )
        }
        val transition = hitTransition(point)?.let(renderedRoutes::get)?.transition ?: return null
        val lines = AfsmTransitionVisuals.hoverText(transition).lineSequence().toList()
        return htmlTooltip(
            title = lines.first(),
            rows = lines.drop(1).map { line ->
                val separator = line.indexOf(':')
                if (separator < 0) "" to line else line.substring(0, separator) to line.substring(separator + 1).trim()
            },
        )
    }

    private fun automaticLayout(model: AfsmStateDiagram): AfsmDiagramLayout =
        runCatching { layoutEngine.layout(model, direction) }
            .getOrElse { fallbackLayout(model, direction) }

    private fun fallbackLayout(
        model: AfsmStateDiagram,
        direction: AfsmDiagramLayoutDirection,
    ): AfsmDiagramLayout {
        val nodes = model.states.mapIndexed { index, state ->
            val x = if (direction == AfsmDiagramLayoutDirection.LEFT_TO_RIGHT) {
                CONTENT_PADDING + index * (NODE_WIDTH + FALLBACK_GAP)
            } else {
                CONTENT_PADDING
            }
            val y = if (direction == AfsmDiagramLayoutDirection.TOP_TO_BOTTOM) {
                CONTENT_PADDING + index * (NODE_HEIGHT + FALLBACK_GAP)
            } else {
                CONTENT_PADDING
            }
            state.id to AfsmDiagramPoint(x, y)
        }.toMap()
        return AfsmDiagramLayout(nodes = nodes)
    }

    private fun rebuildRenderModel() {
        val model = diagram ?: return
        renderedNodes = model.states.mapIndexed { index, state ->
            val saved = layout.nodes[state.id] ?: AfsmDiagramPoint(
                CONTENT_PADDING + index * (NODE_WIDTH + FALLBACK_GAP),
                CONTENT_PADDING,
            )
            val position = AfsmDiagramPoint(
                saved.x.coerceAtLeast(CONTENT_PADDING),
                saved.y.coerceAtLeast(CONTENT_PADDING),
            )
            Node(
                id = state.id,
                notes = state.notes,
                bounds = Rectangle2D.Double(position.x, position.y, NODE_WIDTH, NODE_HEIGHT),
            )
        }
        val byId = renderedNodes.associateBy(Node::id)
        val laneAssignments = AfsmDiagramEdgeLanes.assign(model.transitions).mapIndexed { index, edge ->
            AfsmDiagramKeys.transition(index, edge.transition) to edge.lane
        }.toMap()
        renderedRoutes = model.transitions.mapIndexedNotNull { index, transition ->
            val from = byId[transition.from] ?: return@mapIndexedNotNull null
            val to = byId[transition.to] ?: return@mapIndexedNotNull null
            val key = AfsmDiagramKeys.transition(index, transition)
            val stored = layout.routes[key]
            val points = when {
                stored != null && stored.points.size >= 2 -> attachEndpoints(stored.points, from, to)
                else -> fallbackRoute(from, to, layout.controls[key], laneAssignments[key] ?: 0.0)
            }
            RenderedRoute(
                key = key,
                transition = transition,
                points = points,
                label = stored?.label ?: labelPoint(points),
                selected = interaction.selection == AfsmDiagramSelection.Transition(key),
                highlighted = key == hoveredTransitionKey || when (val selection = interaction.selection) {
                    is AfsmDiagramSelection.State ->
                        transition.from == selection.id || transition.to == selection.id
                    is AfsmDiagramSelection.Transition -> selection.key == key
                    null -> false
                },
            )
        }.associateBy(RenderedRoute::key)
        renderedHandles = renderedRoutes.values
            .filter { route -> interaction.showsRouteHandles(route.key) }
            .flatMap(::routeHandles)
    }

    private fun attachEndpoints(points: List<AfsmDiagramPoint>, from: Node, to: Node): List<AfsmDiagramPoint> {
        if (points.size < 2) return points
        val result = points.toMutableList()
        val fromTarget = points.getOrElse(1) { AfsmDiagramPoint(to.bounds.centerX, to.bounds.centerY) }
        val toSource = points.getOrElse(points.lastIndex - 1) {
            AfsmDiagramPoint(from.bounds.centerX, from.bounds.centerY)
        }
        result[0] = pointOnBoundaryTowards(from.bounds, fromTarget)
        result[result.lastIndex] = pointOnBoundaryTowards(to.bounds, toSource)
        return result
    }

    private fun fallbackRoute(
        from: Node,
        to: Node,
        savedControl: AfsmDiagramPoint?,
        lane: Double,
    ): List<AfsmDiagramPoint> {
        if (from.id == to.id) {
            val control = savedControl ?: AfsmDiagramPoint(
                from.bounds.centerX,
                from.bounds.y - SELF_LOOP_DISTANCE - kotlin.math.abs(lane) * 28.0,
            )
            val routeDirection = atan2(control.y - from.bounds.centerY, control.x - from.bounds.centerX)
            return listOf(
                pointOnBoundary(from.bounds, routeDirection - SELF_LOOP_SPREAD),
                control,
                pointOnBoundary(from.bounds, routeDirection + SELF_LOOP_SPREAD),
            )
        }
        val angle = atan2(to.bounds.centerY - from.bounds.centerY, to.bounds.centerX - from.bounds.centerX)
        val start = pointOnBoundary(from.bounds, angle)
        val end = pointOnBoundary(to.bounds, angle + Math.PI)
        val control = savedControl ?: AfsmDiagramPoint(
            x = (start.x + end.x) / 2 - sin(angle) * lane * CURVE_LANE_GAP,
            y = (start.y + end.y) / 2 + cos(angle) * lane * CURVE_LANE_GAP,
        )
        return listOf(start, control, end)
    }

    private fun routeHandles(route: RenderedRoute): List<RouteHandle> {
        if (route.points.size == 2) {
            return listOf(RouteHandle(route.key, 1, midpoint(route.points[0], route.points[1]), synthetic = true))
        }
        return (1 until route.points.lastIndex).map { index ->
            RouteHandle(route.key, index, route.points[index], synthetic = false)
        }
    }

    private fun drawNode(g: Graphics2D, node: Node) {
        val previousFont = g.font
        val selected = interaction.selection == AfsmDiagramSelection.State(node.id)
        val relatedToSelection = when (val selection = interaction.selection) {
            is AfsmDiagramSelection.State -> selection.id != node.id && diagram?.transitions.orEmpty().any {
                (it.from == selection.id && it.to == node.id) ||
                    (it.to == selection.id && it.from == node.id)
            }
            is AfsmDiagramSelection.Transition -> renderedRoutes[selection.key]?.transition?.let {
                node.id == it.from || node.id == it.to
            } ?: false
            else -> false
        }
        val relatedToHover = hoveredTransitionKey?.let(renderedRoutes::get)?.transition?.let {
            node.id == it.from || node.id == it.to
        } ?: false
        val related = relatedToSelection || relatedToHover
        val bounds = node.bounds
        g.color = if (selected || related) NODE_SELECTED_FILL else NODE_FILL
        g.fillRoundRect(bounds.x.toInt(), bounds.y.toInt(), bounds.width.toInt(), bounds.height.toInt(), 14, 14)
        g.color = if (selected || related) SELECTION_COLOR else NODE_BORDER
        g.stroke = BasicStroke(if (selected) 2.8f else if (related) 2.2f else 1.5f)
        g.drawRoundRect(bounds.x.toInt(), bounds.y.toInt(), bounds.width.toInt(), bounds.height.toInt(), 14, 14)
        g.color = foreground
        g.font = font.deriveFont(Font.BOLD, font.size2D + 1f)
        drawCentered(g, node.id, bounds.centerX, bounds.centerY)
        g.font = font.deriveFont(Font.PLAIN)
        node.notes.forEachIndexed { index, note ->
            g.color = NOTE_COLOR
            drawCentered(g, note, bounds.centerX, bounds.maxY + 20 + index * 20)
        }
        g.font = previousFont
    }

    private fun drawTransition(g: Graphics2D, route: RenderedRoute) {
        if (route.points.size < 2) return
        val selected = route.selected
        g.color = if (route.highlighted) SELECTION_COLOR else EDGE_COLOR
        g.stroke = BasicStroke(
            if (selected) 4.0f else if (route.highlighted) 3.2f else 2.2f,
            BasicStroke.CAP_ROUND,
            BasicStroke.JOIN_ROUND,
        )

        drawSourceMarker(g, route.points.first())

        if (route.points.size == 3 && layout.routes[route.key] == null) {
            val start = route.points[0]
            val control = route.points[1]
            val end = route.points[2]
            g.draw(QuadCurve2D.Double(start.x, start.y, control.x, control.y, end.x, end.y))
            drawFilledArrow(g, end.x, end.y, atan2(end.y - control.y, end.x - control.x), ARROW_SIZE)
            drawDirectionMarkers(g, sampleQuadratic(start, control, end))
        } else {
            val path = Path2D.Double().apply {
                moveTo(route.points.first().x, route.points.first().y)
                route.points.drop(1).forEach { point -> lineTo(point.x, point.y) }
            }
            g.draw(path)
            val end = route.points.last()
            val previous = route.points[route.points.lastIndex - 1]
            drawFilledArrow(g, end.x, end.y, atan2(end.y - previous.y, end.x - previous.x), ARROW_SIZE)
            drawDirectionMarkers(g, route.points)
        }
        drawTransitionLabel(g, route.transition.label, route.label, route.highlighted)
    }

    private fun drawTransitionLabel(
        g: Graphics2D,
        value: String,
        center: AfsmDiagramPoint,
        selected: Boolean,
    ) {
        val previousFont = g.font
        val parsed = AfsmTransitionLabel.parse(value)
        val eventText = "EVENT · ${parsed.event}"
        val secondary = listOfNotNull(parsed.guard?.let { "[$it]" }, parsed.command?.let { "/ $it" })
            .joinToString("  ")
        val eventFont = font.deriveFont(Font.BOLD)
        val secondaryFont = font.deriveFont(Font.PLAIN, max(10f, font.size2D - 1f))
        val eventMetrics = g.getFontMetrics(eventFont)
        val secondaryMetrics = g.getFontMetrics(secondaryFont)
        val width = max(
            eventMetrics.stringWidth(eventText),
            secondary.takeIf(String::isNotEmpty)?.let(secondaryMetrics::stringWidth) ?: 0,
        ) + LABEL_HORIZONTAL_PADDING * 2
        val height = if (secondary.isEmpty()) LABEL_SINGLE_HEIGHT else LABEL_DOUBLE_HEIGHT
        g.color = if (selected) LABEL_SELECTED_FILL else LABEL_FILL
        g.fillRoundRect(
            (center.x - width / 2.0).toInt(),
            (center.y - height / 2.0).toInt(),
            width,
            height,
            10,
            10,
        )
        g.color = if (selected) SELECTION_COLOR else EDGE_COLOR
        g.stroke = BasicStroke(if (selected) 1.6f else 1.0f)
        g.drawRoundRect(
            (center.x - width / 2.0).toInt(),
            (center.y - height / 2.0).toInt(),
            width,
            height,
            10,
            10,
        )
        if (secondary.isEmpty()) {
            g.font = eventFont
            drawCentered(g, eventText, center.x, center.y)
        } else {
            g.font = eventFont
            drawCentered(g, eventText, center.x, center.y - 8)
            g.font = secondaryFont
            g.color = SECONDARY_TEXT
            drawCentered(g, secondary, center.x, center.y + 10)
        }
        g.font = previousFont
    }

    private fun drawRouteHandle(g: Graphics2D, handle: RouteHandle) {
        g.color = HANDLE_FILL
        g.fillOval(
            (handle.point.x - HANDLE_RADIUS).toInt(),
            (handle.point.y - HANDLE_RADIUS).toInt(),
            HANDLE_RADIUS * 2,
            HANDLE_RADIUS * 2,
        )
        g.color = HANDLE_BORDER
        g.stroke = BasicStroke(1.5f)
        g.drawOval(
            (handle.point.x - HANDLE_RADIUS).toInt(),
            (handle.point.y - HANDLE_RADIUS).toInt(),
            HANDLE_RADIUS * 2,
            HANDLE_RADIUS * 2,
        )
    }

    private fun drawInitial(g: Graphics2D, node: Node) {
        val x = node.bounds.x - 32
        val y = node.bounds.centerY
        g.color = EDGE_COLOR
        g.fillOval((x - 4).toInt(), (y - 4).toInt(), 8, 8)
        g.drawLine((x + 4).toInt(), y.toInt(), node.bounds.x.toInt(), y.toInt())
        drawFilledArrow(g, node.bounds.x, y, 0.0, ARROW_SIZE)
    }

    private fun pointOnBoundaryTowards(bounds: Rectangle2D, target: AfsmDiagramPoint): AfsmDiagramPoint =
        pointOnBoundary(bounds, atan2(target.y - bounds.centerY, target.x - bounds.centerX))

    private fun pointOnBoundary(bounds: Rectangle2D, angle: Double): AfsmDiagramPoint {
        val dx = cos(angle)
        val dy = sin(angle)
        val scale = min(
            (bounds.width / 2) / max(0.01, kotlin.math.abs(dx)),
            (bounds.height / 2) / max(0.01, kotlin.math.abs(dy)),
        )
        return AfsmDiagramPoint(bounds.centerX + dx * scale, bounds.centerY + dy * scale)
    }

    private fun drawSourceMarker(g: Graphics2D, point: AfsmDiagramPoint) {
        g.fillOval(
            (point.x - SOURCE_MARKER_RADIUS).toInt(),
            (point.y - SOURCE_MARKER_RADIUS).toInt(),
            SOURCE_MARKER_RADIUS * 2,
            SOURCE_MARKER_RADIUS * 2,
        )
    }

    private fun drawDirectionMarkers(g: Graphics2D, points: List<AfsmDiagramPoint>) {
        AfsmTransitionVisuals.directionMarkers(points).forEach { marker ->
            drawFilledArrow(g, marker.point.x, marker.point.y, marker.angle, DIRECTION_MARKER_SIZE)
        }
    }

    private fun drawFilledArrow(g: Graphics2D, x: Double, y: Double, angle: Double, size: Double) {
        val wing = size * 0.72
        val backX = x - cos(angle) * size
        val backY = y - sin(angle) * size
        val normalX = -sin(angle) * wing
        val normalY = cos(angle) * wing
        val arrow = Path2D.Double().apply {
            moveTo(x, y)
            lineTo(backX + normalX, backY + normalY)
            lineTo(backX - normalX, backY - normalY)
            closePath()
        }
        g.fill(arrow)
    }

    private fun sampleQuadratic(
        start: AfsmDiagramPoint,
        control: AfsmDiagramPoint,
        end: AfsmDiagramPoint,
    ): List<AfsmDiagramPoint> = (0..QUADRATIC_SAMPLE_COUNT).map { index ->
        val t = index.toDouble() / QUADRATIC_SAMPLE_COUNT
        val inverse = 1.0 - t
        AfsmDiagramPoint(
            x = inverse * inverse * start.x + 2 * inverse * t * control.x + t * t * end.x,
            y = inverse * inverse * start.y + 2 * inverse * t * control.y + t * t * end.y,
        )
    }

    private fun drawCentered(g: Graphics2D, text: String, x: Double, y: Double) {
        val metrics: FontMetrics = g.fontMetrics
        g.drawString(text, (x - metrics.stringWidth(text) / 2.0).toFloat(), (y + metrics.ascent / 2.8).toFloat())
    }

    private fun updateDrag(event: MouseEvent) {
        when (val active = drag) {
            is Drag.Node -> {
                val point = toModelPoint(event.point)
                layout = layout.copy(
                    nodes = layout.nodes + (
                        active.id to AfsmDiagramPoint(
                            (point.x - active.offsetX).coerceAtLeast(CONTENT_PADDING),
                            (point.y - active.offsetY).coerceAtLeast(CONTENT_PADDING),
                        )
                    ),
                )
                rebuildRenderModel()
                refreshCanvasBounds()
                repaint()
            }
            is Drag.RoutePoint -> {
                val point = toModelPoint(event.point)
                val route = layout.routes[active.key] ?: renderedRoutes[active.key]?.let { rendered ->
                    AfsmDiagramRoute(points = rendered.points, label = rendered.label)
                } ?: return
                val points = route.points.toMutableList()
                val newPoint = AfsmDiagramPoint(
                    point.x.coerceAtLeast(CONTENT_PADDING),
                    point.y.coerceAtLeast(CONTENT_PADDING),
                )
                if (active.synthetic) points.add(active.index, newPoint) else points[active.index] = newPoint
                layout = layout.copy(
                    routes = layout.routes + (active.key to route.copy(points = points, label = null)),
                )
                drag = active.copy(synthetic = false)
                rebuildRenderModel()
                refreshCanvasBounds()
                repaint()
            }
            is Drag.Pan -> pan(event, active)
            null -> Unit
        }
    }

    private fun pan(event: MouseEvent, active: Drag.Pan) {
        val viewport = viewport() ?: return
        val current = event.locationOnScreen
        val maxX = max(0, width - viewport.extentSize.width)
        val maxY = max(0, height - viewport.extentSize.height)
        viewport.viewPosition = Point(
            (active.viewPosition.x + active.screenPoint.x - current.x).coerceIn(0, maxX),
            (active.viewPosition.y + active.screenPoint.y - current.y).coerceIn(0, maxY),
        )
    }

    private fun updateSelection(selection: AfsmDiagramSelection?) {
        if (interaction.selection == selection) return
        interaction.selection = selection
        rebuildRenderModel()
        onSelectionChanged(selectionDetails())
        repaint()
    }

    private fun selectionDetails(): AfsmDiagramSelectionDetails? {
        val model = diagram ?: return null
        return when (val selection = interaction.selection) {
            is AfsmDiagramSelection.State -> model.states.firstOrNull { it.id == selection.id }?.let { state ->
                AfsmDiagramSelectionDetails(
                    kind = "State",
                    title = state.id,
                    rows = buildList {
                        if (model.initialStateId == state.id) add("Initial" to "Yes")
                        add("Incoming" to model.transitions.count { it.to == state.id }.toString())
                        add("Outgoing" to model.transitions.count { it.from == state.id }.toString())
                        state.notes.forEachIndexed { index, note -> add("Note ${index + 1}" to note) }
                    },
                )
            }
            is AfsmDiagramSelection.Transition -> model.transitions.withIndex().firstOrNull { indexed ->
                AfsmDiagramKeys.transition(indexed.index, indexed.value) == selection.key
            }?.let { (_, transition) ->
                val label = AfsmTransitionLabel.parse(transition.label)
                AfsmDiagramSelectionDetails(
                    kind = "Transition",
                    title = "${transition.from} → ${transition.to}",
                    rows = buildList {
                        add("Event" to label.event)
                        label.guard?.let { add("Guard" to it) }
                        label.command?.let { add("Command" to it) }
                    },
                )
            }
            null -> null
        }
    }

    private fun hitTransition(point: Point2D): String? = renderedRoutes.values
        .firstOrNull { route -> transitionLabelBounds(route).contains(point) }
        ?.key
        ?: renderedRoutes.values.map { route -> route.key to routeDistance(point, route.points) }
        .minByOrNull { (_, distance) -> distance }
        ?.takeIf { (_, distance) -> distance <= EDGE_HIT_RADIUS }
        ?.first

    private fun transitionLabelBounds(route: RenderedRoute): Rectangle2D {
        val parsed = AfsmTransitionLabel.parse(route.transition.label)
        val eventText = "EVENT · ${parsed.event}"
        val secondary = listOfNotNull(parsed.guard?.let { "[$it]" }, parsed.command?.let { "/ $it" })
            .joinToString("  ")
        val eventMetrics = getFontMetrics(font.deriveFont(Font.BOLD))
        val secondaryMetrics = getFontMetrics(font.deriveFont(Font.PLAIN, max(10f, font.size2D - 1f)))
        val width = max(
            eventMetrics.stringWidth(eventText),
            secondary.takeIf(String::isNotEmpty)?.let(secondaryMetrics::stringWidth) ?: 0,
        ) + LABEL_HORIZONTAL_PADDING * 2
        val height = if (secondary.isEmpty()) LABEL_SINGLE_HEIGHT else LABEL_DOUBLE_HEIGHT
        return Rectangle2D.Double(
            route.label.x - width / 2.0,
            route.label.y - height / 2.0,
            width.toDouble(),
            height.toDouble(),
        )
    }

    private fun routeDistance(point: Point2D, points: List<AfsmDiagramPoint>): Double = points.zipWithNext()
        .minOfOrNull { (start, end) ->
            Line2D.ptSegDist(start.x, start.y, end.x, end.y, point.x, point.y)
        } ?: Double.MAX_VALUE

    private fun labelPoint(points: List<AfsmDiagramPoint>): AfsmDiagramPoint {
        if (points.isEmpty()) return AfsmDiagramPoint(CONTENT_PADDING, CONTENT_PADDING)
        val longest = points.zipWithNext().maxByOrNull { (start, end) ->
            Point2D.distanceSq(start.x, start.y, end.x, end.y)
        }
        return longest?.let { (start, end) -> midpoint(start, end) } ?: points.first()
    }

    private fun midpoint(start: AfsmDiagramPoint, end: AfsmDiagramPoint): AfsmDiagramPoint =
        AfsmDiagramPoint((start.x + end.x) / 2.0, (start.y + end.y) / 2.0)

    private fun zoomBy(factor: Double, anchor: Point? = null) = setZoom(zoom * factor, anchor)

    private fun setZoom(value: Double, anchor: Point? = null) {
        val next = AfsmDiagramZoom.clamp(value)
        if (next == zoom) return
        val viewport = viewport()
        val oldZoom = zoom
        val oldPosition = viewport?.viewPosition ?: Point()
        val viewportAnchor = if (viewport == null) {
            Point()
        } else if (anchor != null) {
            Point(anchor.x - oldPosition.x, anchor.y - oldPosition.y)
        } else {
            Point(viewport.extentSize.width / 2, viewport.extentSize.height / 2)
        }
        val modelAnchorX = (oldPosition.x + viewportAnchor.x) / oldZoom
        val modelAnchorY = (oldPosition.y + viewportAnchor.y) / oldZoom

        zoom = next
        updatePreferredSize()
        onZoomChanged(zoomPercent())
        repaint()

        if (viewport != null) {
            ApplicationManager.getApplication().invokeLater {
                val maxX = max(0, width - viewport.extentSize.width)
                val maxY = max(0, height - viewport.extentSize.height)
                viewport.viewPosition = Point(
                    (modelAnchorX * zoom - viewportAnchor.x).roundToInt().coerceIn(0, maxX),
                    (modelAnchorY * zoom - viewportAnchor.y).roundToInt().coerceIn(0, maxY),
                )
            }
        }
    }

    private fun refreshCanvasBounds() {
        rebuildRenderModel()
        val nodeMaxX = renderedNodes.maxOfOrNull { it.bounds.maxX } ?: MIN_CANVAS_WIDTH.toDouble()
        val nodeMaxY = renderedNodes.maxOfOrNull { node ->
            node.bounds.maxY + node.notes.size * 20 + 28
        } ?: MIN_CANVAS_HEIGHT.toDouble()
        val routePoints = renderedRoutes.values.flatMap(RenderedRoute::points)
        val routeMaxX = routePoints.maxOfOrNull(AfsmDiagramPoint::x) ?: 0.0
        val routeMaxY = routePoints.maxOfOrNull(AfsmDiagramPoint::y) ?: 0.0
        val labelMaxX = renderedRoutes.values.maxOfOrNull { it.label.x + MAX_LABEL_OVERFLOW } ?: 0.0
        val labelMaxY = renderedRoutes.values.maxOfOrNull { it.label.y + LABEL_DOUBLE_HEIGHT } ?: 0.0
        contentSizeAt100 = Dimension(
            max(MIN_CANVAS_WIDTH, ceil(maxOf(nodeMaxX, routeMaxX, labelMaxX) + CONTENT_PADDING).toInt()),
            max(MIN_CANVAS_HEIGHT, ceil(maxOf(nodeMaxY, routeMaxY, labelMaxY) + CONTENT_PADDING).toInt()),
        )
        updatePreferredSize()
    }

    private fun updatePreferredSize() {
        preferredSize = Dimension(
            ceil(contentSizeAt100.width * zoom).toInt(),
            ceil(contentSizeAt100.height * zoom).toInt(),
        )
        revalidate()
    }

    private fun htmlTooltip(title: String, rows: List<Pair<String, String>>): String = buildString {
        append("<html><b>")
        append(escapeHtml(title))
        append("</b>")
        rows.forEach { (name, value) ->
            append("<br>")
            if (name.isNotEmpty()) {
                append(escapeHtml(name))
                append(": ")
            }
            append(escapeHtml(value))
        }
        append("</html>")
    }

    private fun escapeHtml(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")

    private fun zoomPercent(): Int = (zoom * 100).roundToInt()

    private fun viewport(): JViewport? = SwingUtilities.getAncestorOfClass(JViewport::class.java, this) as? JViewport

    private fun toModelPoint(point: Point): Point2D.Double = Point2D.Double(point.x / zoom, point.y / zoom)

    private fun distance(point: Point2D, control: AfsmDiagramPoint): Double =
        Point2D.distance(point.x, point.y, control.x, control.y)

    private data class Node(val id: String, val notes: List<String>, val bounds: Rectangle2D)

    private data class RenderedRoute(
        val key: String,
        val transition: AfsmDiagramTransition,
        val points: List<AfsmDiagramPoint>,
        val label: AfsmDiagramPoint,
        val selected: Boolean,
        val highlighted: Boolean,
    ) {
        fun bounds(): Rectangle2D? {
            if (points.isEmpty()) return null
            val minX = points.minOf(AfsmDiagramPoint::x)
            val minY = points.minOf(AfsmDiagramPoint::y)
            val maxX = points.maxOf(AfsmDiagramPoint::x)
            val maxY = points.maxOf(AfsmDiagramPoint::y)
            return Rectangle2D.Double(minX, minY, max(1.0, maxX - minX), max(1.0, maxY - minY))
        }
    }

    private data class RouteHandle(
        val key: String,
        val index: Int,
        val point: AfsmDiagramPoint,
        val synthetic: Boolean,
    )

    private sealed interface Drag {
        data class Node(val id: String, val offsetX: Double, val offsetY: Double) : Drag
        data class RoutePoint(val key: String, val index: Int, val synthetic: Boolean) : Drag
        data class Pan(val screenPoint: Point, val viewPosition: Point) : Drag
    }

    private companion object {
        const val NODE_WIDTH = 220.0
        const val NODE_HEIGHT = 72.0
        const val FALLBACK_GAP = 220.0
        const val CONTENT_PADDING = 56.0
        const val CURVE_LANE_GAP = 110.0
        const val SELF_LOOP_SPREAD = 0.68
        const val SELF_LOOP_DISTANCE = 110.0
        const val HANDLE_RADIUS = 6
        const val HANDLE_HIT_RADIUS = 13.0
        const val EDGE_HIT_RADIUS = 9.0
        const val FOCUS_PADDING = 80.0
        const val MIN_CANVAS_WIDTH = 640
        const val MIN_CANVAS_HEIGHT = 360
        const val ZOOM_STEP = 1.2
        const val LABEL_HORIZONTAL_PADDING = 10
        const val LABEL_SINGLE_HEIGHT = 28
        const val LABEL_DOUBLE_HEIGHT = 46
        const val MAX_LABEL_OVERFLOW = 220.0
        const val SOURCE_MARKER_RADIUS = 4
        const val ARROW_SIZE = 11.0
        const val DIRECTION_MARKER_SIZE = 7.0
        const val QUADRATIC_SAMPLE_COUNT = 12
        val NODE_FILL = JBColor(Color(0xF4F7FC), Color(0x2B3342))
        val NODE_SELECTED_FILL = JBColor(Color(0xE6EEFF), Color(0x34435E))
        val NODE_BORDER = JBColor(Color(0x8B98AE), Color(0x71809A))
        val SELECTION_COLOR = JBColor(Color(0x2F65D9), Color(0x7FA0FF))
        val EDGE_COLOR = JBColor(Color(0x65708A), Color(0xA8B1C3))
        val NOTE_COLOR = JBColor(Color(0x526078), Color(0xBAC3D5))
        val SECONDARY_TEXT = JBColor(Color(0x5D687C), Color(0xAFB9CA))
        val LABEL_FILL = JBColor(Color(0xFAFBFE), Color(0x20242B))
        val LABEL_SELECTED_FILL = JBColor(Color(0xEDF2FF), Color(0x2C3546))
        val HANDLE_FILL = JBColor(Color(0xFFFFFF), Color(0x242932))
        val HANDLE_BORDER = JBColor(Color(0xD88720), Color(0xFFB951))
    }
}
