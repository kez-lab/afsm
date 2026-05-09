package afsm.sample.shop.feature.editor

import afsm.core.toMmd
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProductEditorMmdExportTest {
    @Test
    fun `writes ProductEditor state graph mmd`() {
        val outputDir = File(
            System.getProperty("afsm.mmd.outputDir")
                ?: "build/generated/afsm/mmd",
        )
        val outputFile = outputDir.resolve("ProductEditorStateMachine.mmd")
        val mmd = ProductEditorStateMachine().topology.toMmd()

        outputDir.mkdirs()
        outputFile.writeText("$mmd\n")

        assertTrue(outputFile.isFile)
        assertEquals(mmd, outputFile.readText().trimEnd())
    }
}
