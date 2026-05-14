package afsm.graph.ksp

import kotlin.test.Test
import kotlin.test.assertEquals

class KotlinLiteralTest {
    @Test
    fun `kotlinLiteral escapes generated string values`() {
        assertEquals(
            "\"Line\\n\\\"quoted\\\"\\\\path\"",
            "Line\n\"quoted\"\\path".kotlinLiteral(),
        )
    }

    @Test
    fun `safe mmd file names reject path traversal and absolute paths`() {
        assertEquals(true, "ProductEditor.mmd".isSafeMmdFileName())
        assertEquals(true, "nested/ProductEditor.mmd".isSafeMmdFileName())

        assertEquals(false, "../ProductEditor.mmd".isSafeMmdFileName())
        assertEquals(false, "nested/../ProductEditor.mmd".isSafeMmdFileName())
        assertEquals(false, "/tmp/ProductEditor.mmd".isSafeMmdFileName())
        assertEquals(false, "\\tmp\\ProductEditor.mmd".isSafeMmdFileName())
        assertEquals(false, "ProductEditor.txt".isSafeMmdFileName())
        assertEquals(false, "nested//ProductEditor.mmd".isSafeMmdFileName())
    }
}
