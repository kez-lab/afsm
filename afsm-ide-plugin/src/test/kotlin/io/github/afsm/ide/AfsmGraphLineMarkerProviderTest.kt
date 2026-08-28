package io.github.afsm.ide

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class AfsmGraphLineMarkerProviderTest : BasePlatformTestCase() {
    fun `test marker appears only on an actual AfsmGraph annotation`() {
        val file = myFixture.configureByText(
            "CheckoutMachine.kt",
            """
                import afsm.core.AfsmGraph

                // AfsmGraph in a comment must not have a marker.
                val label = "AfsmGraph"

                @AfsmGraph(fileName = "Checkout.mmd")
                internal val checkoutMachine = Unit
            """.trimIndent(),
        )
        val provider = AfsmGraphLineMarkerProvider()
        val source = file.text

        val importOffset = source.indexOf("AfsmGraph")
        val commentOffset = source.indexOf("AfsmGraph", importOffset + 1)
        val stringOffset = source.indexOf("AfsmGraph", commentOffset + 1)
        val annotationOffset = source.indexOf("AfsmGraph", stringOffset + 1)

        assertNull(provider.getLineMarkerInfo(requireNotNull(file.findElementAt(importOffset))))
        assertNull(provider.getLineMarkerInfo(requireNotNull(file.findElementAt(commentOffset))))
        assertNull(provider.getLineMarkerInfo(requireNotNull(file.findElementAt(stringOffset))))
        assertNotNull(provider.getLineMarkerInfo(requireNotNull(file.findElementAt(annotationOffset))))
    }

    fun `test annotation contributes one gutter marker across its PSI ancestors`() {
        val file = myFixture.configureByText(
            "CheckoutMachine.kt",
            """
                import afsm.core.AfsmGraph

                @AfsmGraph(fileName = "Checkout.mmd")
                internal val checkoutMachine = Unit
            """.trimIndent(),
        )
        val provider = AfsmGraphLineMarkerProvider()
        val annotationOffset = file.text.lastIndexOf("AfsmGraph")
        val annotationLeaf = requireNotNull(file.findElementAt(annotationOffset))

        val markerCount = generateSequence(annotationLeaf) { it.parent }
            .filter { it.text == "AfsmGraph" }
            .count { provider.getLineMarkerInfo(it) != null }

        assertEquals(1, markerCount)
    }
}
