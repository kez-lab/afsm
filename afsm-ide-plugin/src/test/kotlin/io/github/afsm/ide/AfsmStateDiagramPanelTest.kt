package io.github.afsm.ide

import java.awt.image.BufferedImage
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AfsmStateDiagramPanelTest {
    @Test
    fun `renders the parsed graph without a browser component`() {
        val diagram = AfsmMmdDiagramParser.parse(
            """
                stateDiagram-v2
                  [*] --> Editing
                  state Editing
                  state Done
                  Editing --> Done: FinishClicked
            """.trimIndent(),
        )
        val panel = AfsmStateDiagramPanel().apply {
            show(diagram)
            setSize(800, 400)
        }
        val image = BufferedImage(800, 400, BufferedImage.TYPE_INT_ARGB)

        panel.paint(image.graphics)

        val background = image.getRGB(799, 399)
        assertTrue(
            (0 until image.width).any { x ->
                (0 until image.height).any { y -> image.getRGB(x, y) != background }
            },
        )
        assertEquals(null, panel.toolTipText, "Empty canvas should not show a persistent instruction tooltip")
    }

    @Test
    fun `zoom changes the scrollable canvas and actual size restores it`() {
        val panel = AfsmStateDiagramPanel().apply {
            show(simpleDiagram())
        }
        val widthAt100 = panel.preferredSize.width

        panel.zoomIn()
        assertTrue(panel.preferredSize.width > widthAt100)

        panel.actualSize()
        assertEquals(widthAt100, panel.preferredSize.width)
    }

    @Test
    fun `manual node movement expands the scrollable canvas`() {
        val panel = AfsmStateDiagramPanel().apply {
            show(
                diagram = simpleDiagram(),
                layout = AfsmDiagramLayout(
                    nodes = mapOf("Done" to AfsmDiagramPoint(1_800.0, 600.0)),
                ),
            )
        }

        assertTrue(panel.preferredSize.width > 2_000)
        assertTrue(panel.preferredSize.height > 700)
    }

    @Test
    fun `dense auth graph produces a reviewable unclipped canvas image`() {
        val diagram = AfsmMmdDiagramParser.parse(
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
        val panel = AfsmStateDiagramPanel().apply {
            show(diagram)
            setSize(preferredSize)
        }
        val image = BufferedImage(panel.width, panel.height, BufferedImage.TYPE_INT_ARGB)

        panel.paint(image.graphics)

        val output = Path.of("build/reports/visual-tests/auth-state-machine.png")
        Files.createDirectories(output.parent)
        ImageIO.write(image, "png", output.toFile())
        assertTrue(image.width >= 640)
        assertTrue(image.height >= 360)
        assertTrue((0 until image.width).any { x -> image.getRGB(x, 0) != image.getRGB(0, 0) }.not())
    }

    private fun simpleDiagram(): AfsmStateDiagram = AfsmMmdDiagramParser.parse(
        """
            stateDiagram-v2
              [*] --> Editing
              state Editing
              state Done
              Editing --> Done: FinishClicked
        """.trimIndent(),
    )
}
