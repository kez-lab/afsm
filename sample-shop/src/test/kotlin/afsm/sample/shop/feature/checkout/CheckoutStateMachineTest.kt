package afsm.sample.shop.feature.checkout

import afsm.core.AfsmDecision
import afsm.core.AfsmTopologyTransition
import afsm.core.toMmd
import afsm.sample.shop.core.model.OrderReceipt
import afsm.sample.shop.core.model.Product
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class CheckoutStateMachineTest {
    private val machine = CheckoutStateMachine
    private val product = Product(
        id = 7,
        title = "Studio Headphones",
        description = "Closed-back headphones.",
        priceCents = 12_900,
        sellerUserId = null,
    )

    @Test
    fun `screen entered loads product exactly once`() {
        val result = machine.transition(
            state = checkoutState(productId = product.id),
            event = CheckoutEvent.ScreenEntered,
        )

        assertEquals(AfsmDecision.Transitioned, result.decision)
        assertEquals(CheckoutPhase.ProductLoading, result.state.phase)
        assertEquals(listOf(CheckoutCommand.LoadProduct(product.id)), result.commands)

        val duplicate = machine.transition(
            state = result.state,
            event = CheckoutEvent.ScreenEntered,
        )

        assertIs<AfsmDecision.Ignored>(duplicate.decision)
    }

    @Test
    fun `product loaded enters ready phase with product context`() {
        val result = machine.transition(
            state = checkoutState(
                productId = product.id,
                phase = CheckoutPhase.ProductLoading,
            ),
            event = CheckoutEvent.ProductLoaded(product),
        )

        assertEquals(CheckoutPhase.ProductReady, result.state.phase)
        assertEquals(product, result.state.context.product)
        assertEquals(null, result.state.context.errorMessage)
    }

    @Test
    fun `pay clicked with loaded product enters payment phase and emits submit command`() {
        val state = checkoutState(
            productId = product.id,
            phase = CheckoutPhase.ProductReady,
            context = CheckoutContext(
                productId = product.id,
                product = product,
            ),
        )

        val result = machine.transition(state, CheckoutEvent.PayClicked)

        assertEquals(CheckoutPhase.PaymentInProgress(requestId = 1), result.state.phase)
        assertEquals(1, result.state.context.nextPaymentRequestId)
        assertEquals(
            listOf(
                CheckoutCommand.SubmitPayment(
                    requestId = 1,
                    product = product,
                ),
            ),
            result.commands,
        )
    }

    @Test
    fun `payment failure enters failure phase and retry can emit payment command`() {
        val failed = machine.transition(
            state = checkoutState(
                productId = product.id,
                phase = CheckoutPhase.PaymentInProgress(requestId = 1),
                context = CheckoutContext(
                    productId = product.id,
                    product = product,
                    nextPaymentRequestId = 1,
                ),
            ),
            event = CheckoutEvent.PaymentFailed(
                requestId = 1,
                message = "Mock payment declined.",
            ),
        )

        assertEquals(CheckoutPhase.PaymentFailed, failed.state.phase)
        assertEquals("Mock payment declined.", failed.state.context.errorMessage)

        val retry = machine.transition(failed.state, CheckoutEvent.RetryClicked)

        assertEquals(CheckoutPhase.PaymentInProgress(requestId = 2), retry.state.phase)
        assertEquals(2, retry.state.context.nextPaymentRequestId)
        assertEquals(
            listOf(
                CheckoutCommand.SubmitPayment(
                    requestId = 2,
                    product = product,
                ),
            ),
            retry.commands,
        )
    }

    @Test
    fun `payment success completes checkout and emits completion effect`() {
        val receipt = OrderReceipt(
            orderId = 42,
            productId = product.id,
            totalCents = product.priceCents,
        )

        val result = machine.transition(
            state = checkoutState(
                productId = product.id,
                phase = CheckoutPhase.PaymentInProgress(requestId = 1),
                context = CheckoutContext(
                    productId = product.id,
                    product = product,
                    nextPaymentRequestId = 1,
                ),
            ),
            event = CheckoutEvent.PaymentSucceeded(
                requestId = 1,
                receipt = receipt,
            ),
        )

        assertEquals(CheckoutPhase.Completed(orderId = 42), result.state.phase)
        assertEquals(listOf(CheckoutEffect.PaymentCompleted(orderId = 42)), result.effects)

        val duplicatePay = machine.transition(result.state, CheckoutEvent.PayClicked)
        val duplicateRetry = machine.transition(result.state, CheckoutEvent.RetryClicked)

        assertIs<AfsmDecision.Ignored>(duplicatePay.decision)
        assertIs<AfsmDecision.Ignored>(duplicateRetry.decision)
    }

    @Test
    fun `stale payment failure result is ignored`() {
        val result = machine.transition(
            state = checkoutState(
                productId = product.id,
                phase = CheckoutPhase.PaymentInProgress(requestId = 2),
                context = CheckoutContext(
                    productId = product.id,
                    product = product,
                    nextPaymentRequestId = 2,
                ),
            ),
            event = CheckoutEvent.PaymentFailed(
                requestId = 1,
                message = "late failure",
            ),
        )

        assertIs<AfsmDecision.Ignored>(result.decision)
        assertEquals(CheckoutPhase.PaymentInProgress(requestId = 2), result.state.phase)
    }

    @Test
    fun `stale payment success result is ignored`() {
        val receipt = OrderReceipt(
            orderId = 42,
            productId = product.id,
            totalCents = product.priceCents,
        )

        val result = machine.transition(
            state = checkoutState(
                productId = product.id,
                phase = CheckoutPhase.PaymentInProgress(requestId = 2),
                context = CheckoutContext(
                    productId = product.id,
                    product = product,
                    nextPaymentRequestId = 2,
                ),
            ),
            event = CheckoutEvent.PaymentSucceeded(
                requestId = 1,
                receipt = receipt,
            ),
        )

        assertIs<AfsmDecision.Ignored>(result.decision)
        assertEquals(CheckoutPhase.PaymentInProgress(requestId = 2), result.state.phase)
        assertEquals(emptyList(), result.effects)
    }

    @Test
    fun `topology exposes Checkout graph without sample events`() {
        val transitions = machine.topology.transitions

        assertTrue(
            AfsmTopologyTransition(
                from = "Idle",
                event = "ScreenEntered",
                to = "ProductLoading",
            ) in transitions,
        )
        assertTrue(
            AfsmTopologyTransition(
                from = "ProductReady",
                event = "PayClicked",
                to = "PaymentInProgress",
                guardLabel = "product loaded",
            ) in transitions,
        )
        assertTrue(
            AfsmTopologyTransition(
                from = "PaymentInProgress",
                event = "PaymentSucceeded",
                to = "Completed",
                guardLabel = "matching request",
                effectLabels = listOf("PaymentCompleted"),
            ) in transitions,
        )

        val mmd = machine.topology.toMmd()

        assertTrue("Idle --> ProductLoading: ScreenEntered" in mmd)
        assertTrue("ProductReady --> PaymentInProgress: PayClicked [product loaded]" in mmd)
        assertTrue("PaymentInProgress --> Completed: PaymentSucceeded [matching request]" in mmd)
    }
}
