package afsm.sample.shop.feature.editor

import afsm.core.AfsmMmdWriter
import afsm.generated.AfsmGeneratedGraphRegistry
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class ProductEditorMmdExportTest {
    @Test
    fun `writes registered state graph mmd files`() {
        val outputDir = File(
            System.getProperty("afsm.mmd.outputDir")
                ?: "build/generated/afsm/mmd",
        )
        val outputFile = outputDir.resolve("ProductEditorStateMachine.mmd")
        val authOutputFile = outputDir.resolve("AuthStateMachine.mmd")

        AfsmMmdWriter.writeAll(
            registry = AfsmGeneratedGraphRegistry,
            outputDir = outputDir,
        )

        assertTrue(outputFile.isFile)
        assertTrue(authOutputFile.isFile)
        assertTrue("EditingDraft --> ImageUploadInProgress: SubmitClicked" in outputFile.readText())
        assertTrue("Editing --> Submitting: SubmitClicked" in authOutputFile.readText())
    }
}
