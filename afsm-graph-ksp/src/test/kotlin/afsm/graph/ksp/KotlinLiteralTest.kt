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
}
