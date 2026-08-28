package io.github.afsm.ide

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AfsmGraphSourceParserTest {
    @Test
    fun `explicit id and file name are read from a multiline annotation`() {
        val source = """
            @AfsmGraph(
                id = "Checkout",
                fileName = "flows/CheckoutStateMachine.mmd",
            )
            internal val checkoutStateMachine = afsmMachine { }
        """.trimIndent()

        val graph = AfsmGraphSourceParser.parseAt(source, source.indexOf("AfsmGraph"))

        assertEquals("Checkout", graph?.id)
        assertEquals("flows/CheckoutStateMachine.mmd", graph?.fileName)
        assertEquals("checkoutStateMachine", graph?.declarationName)
    }

    @Test
    fun `id and declaration name provide KSP-compatible defaults`() {
        val withId = """
            @AfsmGraph(id = "Checkout")
            object CheckoutMachine : AfsmGraphSource
        """.trimIndent()
        val withoutArguments = """
            @afsm.core.AfsmGraph
            internal val checkoutMachine = afsmMachine { }
        """.trimIndent()

        val idGraph = AfsmGraphSourceParser.parseAt(withId, withId.indexOf("AfsmGraph"))
        val declarationGraph = AfsmGraphSourceParser.parseAt(
            withoutArguments,
            withoutArguments.indexOf("AfsmGraph"),
        )

        assertEquals("Checkout.mmd", idGraph?.fileName)
        assertEquals("checkoutMachine", declarationGraph?.id)
        assertEquals("checkoutMachine.mmd", declarationGraph?.fileName)
    }

    @Test
    fun `import comment and string occurrences are not graph annotations`() {
        val source = """
            import afsm.core.AfsmGraph
            // AfsmGraph is documented here.
            val text = "AfsmGraph"
        """.trimIndent()

        var offset = source.indexOf("AfsmGraph")
        while (offset >= 0) {
            assertNull(AfsmGraphSourceParser.parseAt(source, offset))
            offset = source.indexOf("AfsmGraph", offset + 1)
        }
    }
}
