package io.github.afsm.ide

import kotlin.test.Test
import kotlin.test.assertEquals

class AfsmDiagramLayoutCodecTest {
    @Test
    fun `round trips node control and routed transition positions`() {
        val layout = AfsmDiagramLayout(
            nodes = mapOf("Editing" to AfsmDiagramPoint(120.0, 80.0)),
            controls = mapOf("Editing→Submitting→Login" to AfsmDiagramPoint(260.0, 42.0)),
            routes = mapOf(
                "0:Editing→Submitting→Login" to AfsmDiagramRoute(
                    points = listOf(
                        AfsmDiagramPoint(320.0, 100.0),
                        AfsmDiagramPoint(380.0, 100.0),
                    ),
                    label = AfsmDiagramPoint(350.0, 86.0),
                ),
            ),
        )

        assertEquals(layout, AfsmDiagramLayoutCodec.decode(AfsmDiagramLayoutCodec.encode(layout)))
    }
}
