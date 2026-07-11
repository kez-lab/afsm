package afsm.sample.shop.feature.checkout

import afsm.core.AfsmTopologyTransition
import afsm.core.toMmd
import afsm.sample.shop.core.model.OrderReceipt
import afsm.sample.shop.core.model.Product
import afsm.test.assertCommands
import afsm.test.assertEffects
import afsm.test.assertHandled
import afsm.test.assertIgnored
import afsm.test.assertInvalid
import afsm.test.assertNoEffects
import afsm.test.assertNoOutputs
import afsm.test.assertPhase
import afsm.test.assertTransitioned
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CheckoutStateMachineTest {
    private val machine = checkoutStateMachine
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

        result
            .assertTransitioned()
            .assertPhase(CheckoutPhase.ProductLoading)
            .assertCommands(CheckoutCommand.LoadProduct(product.id))

        val duplicate = machine.transition(
            state = result.state,
            event = CheckoutEvent.ScreenEntered,
        )

        duplicate.assertIgnored()
    }

    @Test
    fun `pay and retry before product load stay idle and explain the prerequisite`() {
        val initialState = checkoutState(productId = product.id)

        val pay = machine.transition(initialState, CheckoutEvent.PayClicked)
        val retry = machine.transition(initialState, CheckoutEvent.RetryClicked)

        pay
            .assertHandled()
            .assertPhase(CheckoutPhase.Idle)
            .assertNoOutputs()
        retry
            .assertHandled()
            .assertPhase(CheckoutPhase.Idle)
            .assertNoOutputs()
        assertEquals("Product is required before payment.", pay.state.data.errorMessage)
        assertEquals("Product is required before payment.", retry.state.data.errorMessage)
    }

    @Test
    fun `product loaded enters ready phase with product data`() {
        val result = machine.transition(
            state = checkoutState(
                productId = product.id,
                phase = CheckoutPhase.ProductLoading,
            ),
            event = CheckoutEvent.ProductLoaded(product),
        )

        result.assertPhase(CheckoutPhase.ProductReady)
        assertEquals(product, result.state.data.product)
        assertEquals(null, result.state.data.errorMessage)
    }

    @Test
    fun `product unavailable enters a terminal phase that rejects payment`() {
        val unavailable = machine.transition(
            state = checkoutState(
                productId = product.id,
                phase = CheckoutPhase.ProductLoading,
            ),
            event = CheckoutEvent.ProductUnavailable,
        )

        unavailable
            .assertTransitioned()
            .assertPhase(CheckoutPhase.ProductUnavailable)

        machine.transition(unavailable.state, CheckoutEvent.PayClicked)
            .assertInvalid()
            .assertPhase(CheckoutPhase.ProductUnavailable)
            .assertNoOutputs()
    }

    @Test
    fun `pay clicked with loaded product enters payment phase and emits submit command`() {
        val state = checkoutState(
            productId = product.id,
            phase = CheckoutPhase.ProductReady,
            data = CheckoutData(
                productId = product.id,
                product = product,
            ),
        )

        val result = machine.transition(state, CheckoutEvent.PayClicked)

        result
            .assertTransitioned()
            .assertPhase(CheckoutPhase.PaymentInProgress(requestId = 1))
            .assertCommands(
                CheckoutCommand.SubmitPayment(
                    requestId = 1,
                    product = product,
                ),
            )
        assertEquals(1, result.state.data.nextPaymentRequestId)
    }

    @Test
    fun `duplicate pay and retry while payment is in flight are ignored`() {
        val state = checkoutState(
            productId = product.id,
            phase = CheckoutPhase.PaymentInProgress(requestId = 1),
            data = CheckoutData(
                productId = product.id,
                product = product,
                nextPaymentRequestId = 1,
            ),
        )

        machine.transition(state, CheckoutEvent.PayClicked)
            .assertIgnored()
            .assertPhase(CheckoutPhase.PaymentInProgress(requestId = 1))
            .assertNoOutputs()
        machine.transition(state, CheckoutEvent.RetryClicked)
            .assertIgnored()
            .assertPhase(CheckoutPhase.PaymentInProgress(requestId = 1))
            .assertNoOutputs()
    }

    @Test
    fun `pay clicked without product handles without phase change ready and records error`() {
        val state = checkoutState(
            productId = product.id,
            phase = CheckoutPhase.ProductReady,
            data = CheckoutData(
                productId = product.id,
                product = null,
            ),
        )

        val result = machine.transition(state, CheckoutEvent.PayClicked)

        result
            .assertHandled()
            .assertPhase(CheckoutPhase.ProductReady)
            .assertNoOutputs()
        assertEquals("Product is required before payment.", result.state.data.errorMessage)
    }

    @Test
    fun `payment failure enters failure phase and retry can emit payment command`() {
        val failed = machine.transition(
            state = checkoutState(
                productId = product.id,
                phase = CheckoutPhase.PaymentInProgress(requestId = 1),
                data = CheckoutData(
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

        failed.assertPhase(CheckoutPhase.PaymentFailed)
        assertEquals("Mock payment declined.", failed.state.data.errorMessage)

        val retry = machine.transition(failed.state, CheckoutEvent.RetryClicked)

        retry
            .assertTransitioned()
            .assertPhase(CheckoutPhase.PaymentInProgress(requestId = 2))
            .assertCommands(
                CheckoutCommand.SubmitPayment(
                    requestId = 2,
                    product = product,
                ),
            )
        assertEquals(2, retry.state.data.nextPaymentRequestId)
    }

    @Test
    fun `retry clicked without product handles without phase change failed and records error`() {
        val state = checkoutState(
            productId = product.id,
            phase = CheckoutPhase.PaymentFailed,
            data = CheckoutData(
                productId = product.id,
                product = null,
                errorMessage = "Previous failure.",
            ),
        )

        val result = machine.transition(state, CheckoutEvent.RetryClicked)

        result
            .assertHandled()
            .assertPhase(CheckoutPhase.PaymentFailed)
            .assertNoOutputs()
        assertEquals("Product is required before payment.", result.state.data.errorMessage)
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
                data = CheckoutData(
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

        result
            .assertTransitioned()
            .assertPhase(CheckoutPhase.Completed(orderId = 42))
            .assertEffects(CheckoutEffect.PaymentCompleted(orderId = 42))

        val duplicatePay = machine.transition(result.state, CheckoutEvent.PayClicked)
        val duplicateRetry = machine.transition(result.state, CheckoutEvent.RetryClicked)

        duplicatePay.assertIgnored()
        duplicateRetry.assertIgnored()
    }

    @Test
    fun `late payment results after completion are ignored`() {
        val completed = checkoutState(
            productId = product.id,
            phase = CheckoutPhase.Completed(orderId = 42),
            data = CheckoutData(
                productId = product.id,
                product = product,
                nextPaymentRequestId = 1,
            ),
        )
        val receipt = OrderReceipt(
            orderId = 42,
            productId = product.id,
            totalCents = product.priceCents,
        )

        machine.transition(
            completed,
            CheckoutEvent.PaymentSucceeded(requestId = 1, receipt = receipt),
        ).assertIgnored()
            .assertPhase(CheckoutPhase.Completed(orderId = 42))
            .assertNoEffects()
        machine.transition(
            completed,
            CheckoutEvent.PaymentFailed(requestId = 1, message = "late failure"),
        ).assertIgnored()
            .assertPhase(CheckoutPhase.Completed(orderId = 42))
            .assertNoOutputs()
    }

    @Test
    fun `restored unknown payment status rejects automatic retry`() {
        val state = checkoutState(
            productId = product.id,
            phase = CheckoutPhase.PaymentStatusUnknown(requestId = 9),
            data = CheckoutData(
                productId = product.id,
                nextPaymentRequestId = 9,
                errorMessage = "Payment status is unknown. Check your orders before trying again.",
            ),
        )

        machine.transition(state, CheckoutEvent.RetryClicked)
            .assertInvalid()
            .assertPhase(CheckoutPhase.PaymentStatusUnknown(requestId = 9))
            .assertNoOutputs()

        val renderState = state.toRenderState()
        assertEquals(null, renderState.primaryAction)
        assertEquals(
            "Payment status is unknown. Check your orders before trying again.",
            renderState.errorMessage,
        )
    }

    @Test
    fun `stale payment failure result is ignored`() {
        val result = machine.transition(
            state = checkoutState(
                productId = product.id,
                phase = CheckoutPhase.PaymentInProgress(requestId = 2),
                data = CheckoutData(
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

        result
            .assertIgnored()
            .assertPhase(CheckoutPhase.PaymentInProgress(requestId = 2))
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
                data = CheckoutData(
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

        result
            .assertIgnored()
            .assertPhase(CheckoutPhase.PaymentInProgress(requestId = 2))
            .assertNoEffects()
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
                conditionLabel = "product loaded",
            ) in transitions,
        )
        assertTrue(
            AfsmTopologyTransition(
                from = "PaymentInProgress",
                event = "PaymentSucceeded",
                to = "Completed",
                conditionLabel = "matching request",
                effectLabels = listOf("PaymentCompleted"),
            ) in transitions,
        )

        val mmd = machine.topology.toMmd()

        assertTrue("Idle --> ProductLoading: ScreenEntered" in mmd)
        assertTrue("ProductReady --> PaymentInProgress: PayClicked [product loaded]" in mmd)
        assertTrue("ProductReady --> ProductReady: PayClicked [missing product]" in mmd)
        assertTrue("PaymentInProgress --> Completed: PaymentSucceeded [matching request]" in mmd)
        assertTrue("state PaymentStatusUnknown" in mmd)
    }
}
