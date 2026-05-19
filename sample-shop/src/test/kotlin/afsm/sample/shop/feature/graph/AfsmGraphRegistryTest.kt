package afsm.sample.shop.feature.graph

import afsm.generated.AfsmGeneratedGraphRegistry
import kotlin.test.Test
import kotlin.test.assertEquals

class AfsmGraphRegistryTest {
    @Test
    fun `sample shop registers all documented state machine graphs`() {
        val fileNames = AfsmGeneratedGraphRegistry.entries
            .map { entry -> entry.fileName }
            .toSet()

        assertEquals(
            setOf(
                "AuthStateMachine.mmd",
                "CheckoutStateMachine.mmd",
                "ProductEditorStateMachine.mmd",
            ),
            fileNames,
        )
    }
}
