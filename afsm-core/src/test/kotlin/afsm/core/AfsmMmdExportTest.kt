package afsm.core

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class AfsmMmdExportTest {
    @Test
    fun `export writes one file per registered graph`() {
        val outputDir = Files.createTempDirectory("afsm-mmd-export").toFile()

        val written = AfsmMmdExport.export(
            outputDir = outputDir,
            registryClassName = TestGraphRegistry::class.java.name,
        )

        assertEquals(listOf("Sample.mmd"), written.map { it.name })
        assertTrue(written.single().readText().startsWith("stateDiagram-v2"))
    }

    @Test
    fun `export explains how to fix a missing registry`() {
        val outputDir = Files.createTempDirectory("afsm-mmd-export").toFile()

        val failure = assertFailsWith<IllegalStateException> {
            AfsmMmdExport.export(
                outputDir = outputDir,
                registryClassName = "afsm.generated.MissingRegistry",
            )
        }

        assertTrue(
            failure.message.orEmpty().contains("No Afsm graph registry was generated"),
            "Unexpected message: ${failure.message}",
        )
        assertTrue(
            failure.message.orEmpty().contains("@AfsmGraph"),
            "Unexpected message: ${failure.message}",
        )
    }

    @Test
    fun `export rejects an empty registry`() {
        val outputDir = Files.createTempDirectory("afsm-mmd-export").toFile()

        val failure = assertFailsWith<IllegalStateException> {
            AfsmMmdExport.export(
                outputDir = outputDir,
                registryClassName = EmptyGraphRegistry::class.java.name,
            )
        }

        assertTrue(
            failure.message.orEmpty().contains("No Afsm graph entries were registered"),
            "Unexpected message: ${failure.message}",
        )
    }

    @Test
    fun `options are parsed from the plugin argument`() {
        assertEquals(AfsmMmdOptions.Flow, AfsmMmdExport.parseOptions(null))
        assertEquals(AfsmMmdOptions.Flow, AfsmMmdExport.parseOptions("Flow"))
        assertEquals(AfsmMmdOptions.Full, AfsmMmdExport.parseOptions("Full"))
        assertFailsWith<IllegalStateException> { AfsmMmdExport.parseOptions("Sideways") }
    }

    object TestGraphRegistry : AfsmGraphRegistry {
        override val entries: List<AfsmGraphEntry> = listOf(
            AfsmGraphEntry(
                id = "Sample",
                fileName = "Sample.mmd",
                createTopology = {
                    AfsmTopology(
                        states = listOf(AfsmTopologyState(id = "Ready")),
                        transitions = emptyList(),
                        initialStateId = "Ready",
                    )
                },
            ),
        )
    }

    object EmptyGraphRegistry : AfsmGraphRegistry {
        override val entries: List<AfsmGraphEntry> = emptyList()
    }
}
