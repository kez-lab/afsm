package io.github.afsm.ide

import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.ui.UIUtil
import java.awt.Component
import java.awt.Container
import java.awt.image.BufferedImage
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO
import javax.swing.JButton
import kotlin.test.assertTrue

class AfsmGraphPreviewPanelVisualTest : BasePlatformTestCase() {
    fun testFullWidthPreviewProducesReadmeImage() {
        val repository = Path.of("..").toAbsolutePath().normalize()
        val moduleDirectory = repository.resolve("sample-shop")
        val graph = AfsmGraphSource(
            id = "Auth",
            fileName = "AuthStateMachine.mmd",
            declarationName = "authStateMachine",
        )
        val panel = AfsmGraphPreviewPanel(project).apply {
            setSize(1_440, 360)
        }
        Disposer.register(testRootDisposable, panel)
        layoutRecursively(panel)
        panel.showTarget(
            AfsmGraphTarget(
                rootProjectPath = repository,
                moduleDirectory = moduleDirectory,
                taskPath = ":sample-shop:generateAfsmMmd",
                graph = graph,
                paths = AfsmGraphFileResolver.resolve(moduleDirectory, graph.fileName),
            ),
        )

        val menuButton = waitForComponent<JButton>(panel) { it.text == "⋮" && it.isEnabled }
        layoutRecursively(panel)
        menuButton.doClick()
        val fitButton = waitForComponent<JButton>(panel) { it.text == "Fit" && it.isEnabled }
        layoutRecursively(panel)
        fitButton.doClick()
        repeat(3) { UIUtil.dispatchAllInvocationEvents() }
        layoutRecursively(panel)
        assertTrue(menuButton.isVisible && menuButton.width > 0 && menuButton.x > 0)
        assertTrue(fitButton.isVisible && fitButton.width > 0)

        val image = BufferedImage(panel.width, panel.height, BufferedImage.TYPE_INT_ARGB)
        panel.printAll(image.graphics)
        val output = Path.of("build/reports/visual-tests/afsm-graph-preview.png")
        Files.createDirectories(output.parent)
        ImageIO.write(image, "png", output.toFile())

        val background = image.getRGB(image.width - 1, image.height - 1)
        assertTrue(
            (0 until image.width step 4).any { x ->
                (0 until image.height step 4).any { y -> image.getRGB(x, y) != background }
            },
        )
    }

    private inline fun <reified T : Component> waitForComponent(
        root: Container,
        crossinline predicate: (T) -> Boolean,
    ): T {
        val deadline = System.nanoTime() + 5_000_000_000L
        while (System.nanoTime() < deadline) {
            UIUtil.dispatchAllInvocationEvents()
            descendants(root).filterIsInstance<T>().firstOrNull(predicate)?.let { return it }
            Thread.sleep(10)
        }
        error("Timed out waiting for ${T::class.java.simpleName}")
    }

    private fun descendants(root: Container): Sequence<Component> = sequence {
        root.components.forEach { component ->
            yield(component)
            if (component is Container) yieldAll(descendants(component))
        }
    }

    private fun layoutRecursively(component: Component) {
        if (component !is Container) return
        component.doLayout()
        component.components.forEach(::layoutRecursively)
    }
}
