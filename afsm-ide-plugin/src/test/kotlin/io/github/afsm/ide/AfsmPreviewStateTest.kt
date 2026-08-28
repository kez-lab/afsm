package io.github.afsm.ide

import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AfsmPreviewStateTest {
    private val document = AfsmGraphDocument(
        source = "stateDiagram-v2",
        path = Path.of("/workspace/afsm-graph/Checkout.mmd"),
        origin = AfsmGraphOrigin.CHECKED_IN,
    )

    @Test
    fun `failed refresh keeps the last successful graph and marks it stale`() {
        val loading = AfsmPreviewState.startRefresh(AfsmPreviewState.Ready(document))
        val failed = AfsmPreviewState.failRefresh(loading, "Gradle task failed")

        val stale = assertIs<AfsmPreviewState.Stale>(failed)
        assertEquals(document, stale.document)
        assertEquals("Gradle task failed", stale.message)
    }

    @Test
    fun `first failure is an error because no successful graph can be retained`() {
        val loading = AfsmPreviewState.startRefresh(AfsmPreviewState.Empty)
        val failed = AfsmPreviewState.failRefresh(loading, "Gradle task failed")

        assertIs<AfsmPreviewState.Error>(failed)
    }
}
