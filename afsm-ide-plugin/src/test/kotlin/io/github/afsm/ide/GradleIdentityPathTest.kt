package io.github.afsm.ide

import kotlin.test.Test
import kotlin.test.assertEquals

class GradleIdentityPathTest {
    @Test
    fun `normalizes root modules and source-set modules without guessing from directories`() {
        assertEquals(":", GradleIdentityPath.normalize("afsm", isSourceSet = false))
        assertEquals(":", GradleIdentityPath.normalize("afsm:main", isSourceSet = true))
        assertEquals(
            ":sample-shop",
            GradleIdentityPath.normalize(":sample-shop", isSourceSet = false),
        )
        assertEquals(
            ":sample-shop",
            GradleIdentityPath.normalize(":sample-shop:main", isSourceSet = true),
        )
        assertEquals(
            ":feature:checkout",
            GradleIdentityPath.normalize(":feature:checkout:test", isSourceSet = true),
        )
    }

    @Test
    fun `builds the exact existing Afsm generation task path`() {
        assertEquals(":generateAfsmMmd", GradleIdentityPath.generationTask(":"))
        assertEquals(
            ":sample-shop:generateAfsmMmd",
            GradleIdentityPath.generationTask(":sample-shop"),
        )
    }
}
