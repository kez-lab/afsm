package io.github.afsm.ide

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.GridLayout
import java.nio.file.Files
import java.nio.file.Path
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.JPanel

internal class AfsmGraphPreviewPanel(
    private val project: Project,
    private val taskRunner: AfsmGradleTaskRunner = ExternalSystemAfsmGradleTaskRunner(),
) : JPanel(BorderLayout()), Disposable {
    private val statusLabel = JBLabel("Select an @AfsmGraph annotation to preview its graph.")
    private val refreshButton = JButton("Refresh")
    private val autoLayoutButton = JButton("Auto Layout")
    private val modeButton = JButton("Inspect")
    private val directionButton = JButton("Left → Right")
    private val searchField = JBTextField().apply {
        emptyText.text = "Find state or transition"
        preferredSize = Dimension(210, preferredSize.height)
    }
    private val findButton = JButton("Find")
    private val focusButton = JButton("Focus")
    private val zoomOutButton = JButton("−")
    private val actualSizeButton = JButton("100%")
    private val zoomInButton = JButton("+")
    private val fitButton = JButton("Fit")
    private val layoutStore = AfsmDiagramLayoutStore(project)
    private val rawView = JBTextArea().apply {
        isEditable = false
        font = JBUI.Fonts.create("monospaced", 12)
        border = JBUI.Borders.empty(12)
        lineWrap = false
    }
    private val diagramView = AfsmStateDiagramPanel()
    private val diagramScrollPane = JBScrollPane(diagramView)
    private val selectionChip = JBLabel().apply {
        isOpaque = true
        background = JBColor(ColorPalette.CHIP_LIGHT, ColorPalette.CHIP_DARK)
        border = BorderFactory.createCompoundBorder(
            JBUI.Borders.customLine(JBColor.border()),
            JBUI.Borders.empty(8, 12),
        )
        isVisible = false
    }
    private val floatingMenuButton = JButton("⋮").apply {
        preferredSize = Dimension(44, 44)
        putClientProperty("JButton.buttonType", "roundRect")
        toolTipText = "Open graph canvas controls"
        accessibleContext.accessibleName = "Graph canvas controls"
    }
    private val floatingActionsPanel = createFloatingActionsPanel().apply {
        isVisible = false
    }
    private val diagramOverlay = AfsmGraphOverlayPane(
        canvas = diagramScrollPane,
        actions = floatingActionsPanel,
        context = selectionChip,
        fab = floatingMenuButton,
    )
    private val viewCards = CardLayout()
    private val viewPanel = JPanel(viewCards)

    private var currentTarget: AfsmGraphTarget? = null
    private var currentState: AfsmPreviewState = AfsmPreviewState.Empty
    private var targetSerial: Long = 0
    private var runningTaskPath: String? = null
    private var lastShownGraphKey: String? = null
    private var canvasMode = AfsmCanvasMode.INSPECT
    private var layoutDirection = AfsmDiagramLayoutDirection.LEFT_TO_RIGHT
    private var hasSelection = false

    init {
        border = JBUI.Borders.empty()
        val toolbarActions = JPanel(FlowLayout(FlowLayout.LEFT, 8, 6)).apply {
            add(refreshButton)
            add(searchField)
            add(findButton)
        }
        val toolbar = JPanel(BorderLayout()).apply {
            border = JBUI.Borders.customLineBottom(JBColor.border())
            add(toolbarActions, BorderLayout.CENTER)
            add(statusLabel.apply { border = JBUI.Borders.empty(0, 12, 6, 12) }, BorderLayout.SOUTH)
        }
        add(toolbar, BorderLayout.NORTH)

        viewPanel.add(JBScrollPane(rawView), RAW_CARD)
        viewPanel.add(diagramOverlay, DIAGRAM_CARD)
        add(viewPanel, BorderLayout.CENTER)

        refreshButton.isEnabled = false
        refreshButton.addActionListener { currentTarget?.let(::refresh) }
        modeButton.addActionListener {
            canvasMode = if (canvasMode == AfsmCanvasMode.INSPECT) {
                AfsmCanvasMode.ARRANGE
            } else {
                AfsmCanvasMode.INSPECT
            }
            diagramView.setMode(canvasMode)
            updateFloatingControlLabels()
        }
        directionButton.addActionListener {
            layoutDirection = if (layoutDirection == AfsmDiagramLayoutDirection.LEFT_TO_RIGHT) {
                AfsmDiagramLayoutDirection.TOP_TO_BOTTOM
            } else {
                AfsmDiagramLayoutDirection.LEFT_TO_RIGHT
            }
            diagramView.setDirection(layoutDirection)
            updateFloatingControlLabels()
        }
        autoLayoutButton.addActionListener {
            diagramView.autoLayout()
            fitGraph()
        }
        searchField.addActionListener { findSelection() }
        searchField.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(event: DocumentEvent) = clearSearchError()
            override fun removeUpdate(event: DocumentEvent) = clearSearchError()
            override fun changedUpdate(event: DocumentEvent) = clearSearchError()
        })
        findButton.addActionListener { findSelection() }
        focusButton.addActionListener { diagramView.focusSelection() }
        zoomOutButton.addActionListener { diagramView.zoomOut() }
        actualSizeButton.addActionListener { diagramView.actualSize() }
        zoomInButton.addActionListener { diagramView.zoomIn() }
        fitButton.addActionListener { fitGraph() }
        floatingMenuButton.addActionListener {
            floatingActionsPanel.isVisible = !floatingActionsPanel.isVisible
            floatingMenuButton.toolTipText = if (floatingActionsPanel.isVisible) {
                "Close graph canvas controls"
            } else {
                "Open graph canvas controls"
            }
            diagramOverlay.revalidate()
            diagramOverlay.repaint()
        }
        zoomOutButton.toolTipText = "Zoom out (Ctrl/⌘ + mouse wheel is also supported)"
        actualSizeButton.toolTipText = "Reset zoom to 100%"
        zoomInButton.toolTipText = "Zoom in (Ctrl/⌘ + mouse wheel is also supported)"
        fitButton.toolTipText = "Fit the whole graph in the preview"
        modeButton.toolTipText = "Toggle between Inspect and Arrange"
        directionButton.toolTipText = "Toggle the direction used by Auto Layout"
        autoLayoutButton.toolTipText = "Recalculate node and transition positions with the selected direction"
        searchField.toolTipText = "Search state names, transition events, guards, commands, or endpoints"
        findButton.toolTipText = "Select and reveal the first matching state or transition"
        focusButton.toolTipText = "Center the current selection in the canvas"
        diagramView.setOnZoomChanged { percent -> actualSizeButton.text = "$percent%" }
        diagramView.setOnSelectionChanged(::showSelectionDetails)
        updateFloatingControlLabels()
        setGraphControlsEnabled(false)
        showRaw("Select an @AfsmGraph annotation in a Kotlin file.")
    }

    fun showTarget(target: AfsmGraphTarget) {
        currentTarget = target
        targetSerial++
        val serial = targetSerial
        currentState = AfsmPreviewState.Loading(null)
        refreshButton.isEnabled = runningTaskPath == null
        setGraphControlsEnabled(false)
        statusLabel.text = "Loading ${target.graph.id}…"

        readInBackground(target.paths.initialCandidates, serial) { document ->
            if (document == null) {
                currentState = AfsmPreviewState.Error(
                    "No graph file exists yet. Run Refresh to execute ${target.taskPath}.",
                )
            } else {
                currentState = AfsmPreviewState.Ready(document)
            }
            renderState()
        }
    }

    fun showError(message: String) {
        currentTarget = null
        targetSerial++
        currentState = AfsmPreviewState.Error(message)
        refreshButton.isEnabled = false
        setGraphControlsEnabled(false)
        renderState()
    }

    private fun refresh(target: AfsmGraphTarget) {
        if (runningTaskPath != null) return
        runningTaskPath = target.taskPath
        currentState = AfsmPreviewState.startRefresh(currentState)
        refreshButton.isEnabled = false
        statusLabel.text = "Running ${target.taskPath}…"

        taskRunner.generate(project, target) { success ->
            runningTaskPath = null
            if (currentTarget != target) {
                refreshButton.isEnabled = currentTarget != null
                return@generate
            }
            if (!success) {
                currentState = AfsmPreviewState.failRefresh(
                    currentState,
                    "${target.taskPath} failed. See the Build tool window for details.",
                )
                renderState()
                return@generate
            }

            val serial = targetSerial
            readInBackground(listOf(target.paths.afterRefresh), serial) { document ->
                currentState = if (document == null) {
                    AfsmPreviewState.failRefresh(
                        currentState,
                        "${target.taskPath} succeeded but ${target.graph.fileName} was not generated.",
                    )
                } else {
                    AfsmPreviewState.Ready(document)
                }
                renderState()
            }
        }
    }

    private fun readInBackground(
        candidates: List<Path>,
        serial: Long,
        onRead: (AfsmGraphDocument?) -> Unit,
    ) {
        ApplicationManager.getApplication().executeOnPooledThread {
            val document = candidates.firstNotNullOfOrNull(::readDocument)
            ApplicationManager.getApplication().invokeLater {
                if (!project.isDisposed && serial == targetSerial) onRead(document)
            }
        }
    }

    private fun readDocument(path: Path): AfsmGraphDocument? {
        if (!Files.isRegularFile(path)) return null
        return runCatching {
            AfsmGraphDocument(
                source = Files.readString(path),
                path = path,
                origin = if (path.toString().contains("/afsm-graph/")) {
                    AfsmGraphOrigin.CHECKED_IN
                } else {
                    AfsmGraphOrigin.GENERATED
                },
            )
        }.getOrNull()
    }

    private fun renderState() {
        refreshButton.isEnabled = currentTarget != null && runningTaskPath == null
        when (val state = currentState) {
            AfsmPreviewState.Empty -> {
                statusLabel.text = "Select an @AfsmGraph annotation."
                showRaw("Select an @AfsmGraph annotation in a Kotlin file.")
            }
            is AfsmPreviewState.Loading -> {
                statusLabel.text = "Generating Afsm graph…"
                state.previous?.let(::showDocument)
            }
            is AfsmPreviewState.Ready -> showDocument(state.document)
            is AfsmPreviewState.Stale -> {
                showDocument(state.document)
                statusLabel.text = "Stale preview — ${state.message}"
            }
            is AfsmPreviewState.Error -> {
                statusLabel.text = "Afsm graph preview unavailable"
                showRaw(state.message)
            }
        }
    }

    private fun showDocument(document: AfsmGraphDocument) {
        val origin = when (document.origin) {
            AfsmGraphOrigin.CHECKED_IN -> "checked-in baseline"
            AfsmGraphOrigin.GENERATED -> "generated output"
        }
        statusLabel.text = "${currentTarget?.graph?.id ?: "Afsm"} · $origin · ${document.path.fileName}"
        statusLabel.toolTipText = document.path.toString()

        val diagram = runCatching { AfsmMmdDiagramParser.parse(document.source) }.getOrElse { error ->
            showRaw("Afsm state diagram renderer could not read this file: ${error.message}\n\n${document.source}")
            return
        }
        val graphKey = currentTarget?.let { "${it.moduleDirectory}/${it.graph.fileName}" }
            ?: document.path.toAbsolutePath().toString()
        diagramView.show(
            diagram = diagram,
            layout = layoutStore.load(graphKey),
            onLayoutChanged = { layoutStore.save(graphKey, it) },
        )
        setGraphControlsEnabled(true)
        viewCards.show(viewPanel, DIAGRAM_CARD)
        if (lastShownGraphKey != graphKey) {
            lastShownGraphKey = graphKey
            ApplicationManager.getApplication().invokeLater(::fitGraph)
        }
    }

    private fun showRaw(text: String) {
        setGraphControlsEnabled(false)
        rawView.text = text
        rawView.caretPosition = 0
        viewCards.show(viewPanel, RAW_CARD)
    }

    private fun fitGraph() {
        val extent = diagramScrollPane.viewport.extentSize
        if (extent.width > 0 && extent.height > 0) diagramView.fitTo(extent)
    }

    private fun findSelection() {
        if (diagramView.findAndSelect(searchField.text)) {
            searchField.putClientProperty("JComponent.outline", null)
        } else {
            searchField.putClientProperty("JComponent.outline", "error")
        }
    }

    private fun clearSearchError() {
        searchField.putClientProperty("JComponent.outline", null)
    }

    private fun showSelectionDetails(details: AfsmDiagramSelectionDetails?) {
        hasSelection = details != null
        if (details == null) {
            selectionChip.isVisible = false
            selectionChip.text = ""
            selectionChip.toolTipText = null
            focusButton.isEnabled = false
            diagramOverlay.repaint()
            return
        }
        val event = details.rows.firstOrNull { (name, _) -> name == "Event" }?.second
        selectionChip.text = buildString {
            append(details.kind)
            append(" · ")
            append(details.title)
            event?.let {
                append(" · ")
                append(it)
            }
        }
        selectionChip.toolTipText = details.rows.takeIf(List<*>::isNotEmpty)?.joinToString(
            prefix = "<html>",
            postfix = "</html>",
            separator = "<br>",
        ) { (name, value) -> "<b>${escapeHtml(name)}</b>: ${escapeHtml(value)}" }
        selectionChip.isVisible = true
        focusButton.isEnabled = true
        diagramOverlay.revalidate()
        diagramOverlay.repaint()
    }

    private fun setGraphControlsEnabled(enabled: Boolean) {
        modeButton.isEnabled = enabled
        directionButton.isEnabled = enabled
        autoLayoutButton.isEnabled = enabled
        searchField.isEnabled = enabled
        findButton.isEnabled = enabled
        focusButton.isEnabled = enabled && hasSelection
        zoomOutButton.isEnabled = enabled
        actualSizeButton.isEnabled = enabled
        zoomInButton.isEnabled = enabled
        fitButton.isEnabled = enabled
        floatingMenuButton.isEnabled = enabled
        if (!enabled) {
            floatingActionsPanel.isVisible = false
            selectionChip.isVisible = false
        }
    }

    private fun createFloatingActionsPanel(): JPanel {
        val modeRow = JPanel(FlowLayout(FlowLayout.CENTER, 6, 0)).apply {
            isOpaque = false
            add(modeButton)
            add(directionButton)
        }
        val layoutRow = JPanel(FlowLayout(FlowLayout.CENTER, 6, 0)).apply {
            isOpaque = false
            add(autoLayoutButton)
            add(focusButton)
        }
        val zoomRow = JPanel(FlowLayout(FlowLayout.CENTER, 6, 0)).apply {
            isOpaque = false
            add(zoomOutButton)
            add(actualSizeButton)
            add(zoomInButton)
            add(fitButton)
        }
        return JPanel(GridLayout(3, 1, 0, 6)).apply {
            isOpaque = true
            background = JBColor(ColorPalette.PALETTE_LIGHT, ColorPalette.PALETTE_DARK)
            border = BorderFactory.createCompoundBorder(
                JBUI.Borders.customLine(JBColor.border()),
                JBUI.Borders.empty(8),
            )
            add(modeRow)
            add(layoutRow)
            add(zoomRow)
        }
    }

    private fun updateFloatingControlLabels() {
        modeButton.text = if (canvasMode == AfsmCanvasMode.INSPECT) "Inspect" else "Arrange"
        directionButton.text = if (layoutDirection == AfsmDiagramLayoutDirection.LEFT_TO_RIGHT) {
            "Left → Right"
        } else {
            "Top ↓ Bottom"
        }
    }

    private fun escapeHtml(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")

    override fun dispose() {
        // No browser or external renderer resources are allocated.
    }

    private companion object {
        const val RAW_CARD = "raw"
        const val DIAGRAM_CARD = "diagram"

        object ColorPalette {
            const val CHIP_LIGHT = 0xF4F7FC
            const val CHIP_DARK = 0x2B3342
            const val PALETTE_LIGHT = 0xFAFBFE
            const val PALETTE_DARK = 0x20242B
        }
    }
}
