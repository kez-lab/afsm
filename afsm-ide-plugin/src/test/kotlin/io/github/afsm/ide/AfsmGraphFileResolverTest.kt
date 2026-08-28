package io.github.afsm.ide

import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AfsmGraphFileResolverTest {
    private val module = Path.of("/workspace/sample-shop")

    @Test
    fun `checked-in graph is the initial preview and generated graph follows refresh`() {
        val paths = AfsmGraphFileResolver.resolve(module, "Checkout.mmd")

        assertEquals(module.resolve("afsm-graph/Checkout.mmd"), paths.initialCandidates[0])
        assertEquals(
            module.resolve("build/generated/afsm/mmd/Checkout.mmd"),
            paths.initialCandidates[1],
        )
        assertEquals(paths.generated, paths.afterRefresh)
    }

    @Test
    fun `unsafe paths are rejected with the same boundary as the KSP processor`() {
        assertTrue(AfsmGraphFileResolver.isSafeMmdFileName("flow/Checkout.mmd"))
        assertFalse(AfsmGraphFileResolver.isSafeMmdFileName("../Checkout.mmd"))
        assertFalse(AfsmGraphFileResolver.isSafeMmdFileName("/tmp/Checkout.mmd"))
        assertFalse(AfsmGraphFileResolver.isSafeMmdFileName("Checkout.txt"))
    }
}
