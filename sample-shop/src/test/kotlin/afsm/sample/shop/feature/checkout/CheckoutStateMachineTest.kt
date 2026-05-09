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
        assertEquals(listOf(CheckoutCommand.SubmitPayment(product)), result.commands)
    }

    @Test
    fun `payment failure stays on checkout and retry can emit payment command`() {
        val failed = machine.transition(
            state = CheckoutState(
                productId = product.id,
                product = product,
                isPaying = true,
            ),
            event = CheckoutEvent.PaymentFailed("Mock payment declined."),
        )

        assertEquals(false, failed.state.isPaying)
        assertEquals("Mock payment declined.", failed.state.errorMessage)

        val retry = machine.transition(failed.state, CheckoutEvent.RetryClicked)

        assertEquals(true, retry.state.isPaying)
        assertEquals(listOf(CheckoutCommand.SubmitPayment(product)), retry.commands)
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
            ),
            event = CheckoutEvent.PaymentSucceeded(receipt),
        )

        assertEquals(true, result.state.isComplete)
        assertEquals(42, result.state.orderId)
        assertEquals(listOf(CheckoutEffect.PaymentCompleted(orderId = 42)), result.effects)
    }

    @Test
    fun `payment result without pending payment is invalid`() {
        val result = machine.transition(
            state = CheckoutState(
                productId = product.id,
                product = product,
                isPaying = false,
            ),
            event = CheckoutEvent.PaymentFailed("late failure"),
        )

        assertIs<AfsmDecision.Invalid>(result.decision)
        assertEquals(false, result.state.isPaying)
    }
}
