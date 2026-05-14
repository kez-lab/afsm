package afsm.sample.shop.feature.checkout

import afsm.core.AfsmDecision
import afsm.sample.shop.core.model.OrderReceipt
import afsm.sample.shop.core.model.Product
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class CheckoutStateMachineTest {
    private val machine = CheckoutStateMachine()
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
            state = CheckoutState(productId = product.id),
            event = CheckoutEvent.ScreenEntered,
        )

        assertEquals(AfsmDecision.Transitioned, result.decision)
        assertEquals(true, result.state.isLoadingProduct)
        assertEquals(listOf(CheckoutCommand.LoadProduct(product.id)), result.commands)

        val duplicate = machine.transition(
            state = result.state,
            event = CheckoutEvent.ScreenEntered,
        )

        assertIs<AfsmDecision.Ignored>(duplicate.decision)
    }

    @Test
    fun `pay clicked with loaded product enters paying and emits submit command`() {
        val state = CheckoutState(
            productId = product.id,
            product = product,
        )

        val result = machine.transition(state, CheckoutEvent.PayClicked)

        assertEquals(true, result.state.isPaying)
        assertEquals(1, result.state.activePaymentRequestId)
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
    fun `payment failure stays on checkout and retry can emit payment command`() {
        val failed = machine.transition(
            state = CheckoutState(
                productId = product.id,
                product = product,
                isPaying = true,
                nextPaymentRequestId = 1,
                activePaymentRequestId = 1,
            ),
            event = CheckoutEvent.PaymentFailed(
                requestId = 1,
                message = "Mock payment declined.",
            ),
        )

        assertEquals(false, failed.state.isPaying)
        assertEquals(null, failed.state.activePaymentRequestId)
        assertEquals("Mock payment declined.", failed.state.errorMessage)

        val retry = machine.transition(failed.state, CheckoutEvent.RetryClicked)

        assertEquals(true, retry.state.isPaying)
        assertEquals(2, retry.state.activePaymentRequestId)
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
            state = CheckoutState(
                productId = product.id,
                product = product,
                isPaying = true,
                nextPaymentRequestId = 1,
                activePaymentRequestId = 1,
            ),
            event = CheckoutEvent.PaymentSucceeded(
                requestId = 1,
                receipt = receipt,
            ),
        )

        assertEquals(true, result.state.isComplete)
        assertEquals(null, result.state.activePaymentRequestId)
        assertEquals(42, result.state.orderId)
        assertEquals(listOf(CheckoutEffect.PaymentCompleted(orderId = 42)), result.effects)

        val duplicatePay = machine.transition(result.state, CheckoutEvent.PayClicked)
        val duplicateRetry = machine.transition(result.state, CheckoutEvent.RetryClicked)

        assertIs<AfsmDecision.Ignored>(duplicatePay.decision)
        assertIs<AfsmDecision.Ignored>(duplicateRetry.decision)
    }

    @Test
    fun `stale payment failure result is ignored`() {
        val result = machine.transition(
            state = CheckoutState(
                productId = product.id,
                product = product,
                isPaying = true,
                nextPaymentRequestId = 2,
                activePaymentRequestId = 2,
            ),
            event = CheckoutEvent.PaymentFailed(
                requestId = 1,
                message = "late failure",
            ),
        )

        assertIs<AfsmDecision.Ignored>(result.decision)
        assertEquals(true, result.state.isPaying)
        assertEquals(2, result.state.activePaymentRequestId)
    }

    @Test
    fun `stale payment success result is ignored`() {
        val receipt = OrderReceipt(
            orderId = 42,
            productId = product.id,
            totalCents = product.priceCents,
        )

        val result = machine.transition(
            state = CheckoutState(
                productId = product.id,
                product = product,
                isPaying = true,
                nextPaymentRequestId = 2,
                activePaymentRequestId = 2,
            ),
            event = CheckoutEvent.PaymentSucceeded(
                requestId = 1,
                receipt = receipt,
            ),
        )

        assertIs<AfsmDecision.Ignored>(result.decision)
        assertEquals(true, result.state.isPaying)
        assertEquals(2, result.state.activePaymentRequestId)
        assertEquals(emptyList(), result.effects)
    }
}
