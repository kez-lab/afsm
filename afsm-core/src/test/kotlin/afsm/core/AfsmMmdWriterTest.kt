package afsm.core

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AfsmMmdWriterTest {
    @Test
    fun `writeAll writes one mmd file per graph entry`() {
        val outputDir = Files.createTempDirectory("afsm-mmd-writer").toFile()
        val registry = object : AfsmGraphRegistry {
            override val entries = listOf(
                AfsmGraphEntry(
                    id = "First",
                    fileName = "First.mmd",
                    createTopology = {
                        AfsmTopology(
                            states = listOf(AfsmTopologyState("Idle")),
                            transitions = emptyList(),
                        )
                    },
                ),
                AfsmGraphEntry(
                    id = "Second",
                    fileName = "nested/Second.mmd",
                    createTopology = {
                        AfsmTopology(
                            states = listOf(
                                AfsmTopologyState("Idle"),
                                AfsmTopologyState("Done"),
                            ),
                            transitions = listOf(
                                AfsmTopologyTransition(
                                    from = "Idle",
                                    event = "Finish",
                                    to = "Done",
                                ),
                            ),
                        )
                    },
                ),
            )
        }

        AfsmMmdWriter.writeAll(
            registry = registry,
            outputDir = outputDir,
        )

        assertTrue(outputDir.resolve("First.mmd").isFile)
        assertTrue(outputDir.resolve("nested/Second.mmd").isFile)
        assertEquals(
            """
            stateDiagram-v2
              state Idle
            """.trimIndent(),
            outputDir.resolve("First.mmd").readText().trimEnd(),
        )
        assertEquals(
            """
            stateDiagram-v2
              state Idle
              state Done
              Idle --> Done: Finish
            """.trimIndent(),
            outputDir.resolve("nested/Second.mmd").readText().trimEnd(),
        )
    }
}
