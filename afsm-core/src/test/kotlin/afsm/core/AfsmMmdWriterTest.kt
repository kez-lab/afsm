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

    @Test
    fun `toMmd renders flow diagram with initial node entry notes and filtered internal transitions`() {
        val topology = AfsmTopology(
            states = listOf(
                AfsmTopologyState("Editing"),
                AfsmTopologyState(
                    id = "Saving",
                    entryCommandLabels = listOf("SaveDraft"),
                ),
                AfsmTopologyState("Saved"),
            ),
            transitions = listOf(
                AfsmTopologyTransition(
                    from = "Editing",
                    event = "TitleChanged",
                    to = "Editing",
                    kind = AfsmTopologyTransitionKind.Internal,
                ),
                AfsmTopologyTransition(
                    from = "Editing",
                    event = "SaveClicked",
                    to = "Saving",
                    guardLabel = "valid title",
                ),
                AfsmTopologyTransition(
                    from = "Editing",
                    event = "SaveClicked [invalid title]",
                    to = "Editing",
                    kind = AfsmTopologyTransitionKind.Internal,
                    isFallback = true,
                ),
                AfsmTopologyTransition(
                    from = "Saving",
                    event = "Saved",
                    to = "Saved",
                ),
            ),
            initialStateId = "Editing",
        )

        assertEquals(
            """
            stateDiagram-v2
              [*] --> Editing
              state Editing
              state Saving
              note right of Saving
                entry / SaveDraft
              end note
              state Saved
              Editing --> Saving: SaveClicked [valid title]
              Editing --> Editing: SaveClicked [invalid title]
              Saving --> Saved: Saved
            """.trimIndent(),
            topology.toMmd(),
        )

        assertTrue("TitleChanged" in topology.toMmd(AfsmMmdOptions.Full))
    }
}
