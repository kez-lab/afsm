package afsm.sample.shop.feature.checkout

import afsm.core.AfsmTransition
import afsm.sample.shop.core.model.OrderReceipt
import afsm.sample.shop.core.model.Product

data class CheckoutState(
    val productId: Long,
    val product: Product? = null,
    val isLoadingProduct: Boolean = false,
    val isPaying: Boolean = false,
    val isComplete: Boolean = false,
    val nextPaymentRequestId: Long = 0,
    val activePaymentRequestId: Long? = null,
    val orderId: Long? = null,
    val errorMessage: String? = null,
)

sealed interface CheckoutEvent {
    data object ScreenEntered : CheckoutEvent

    data class ProductLoaded(val product: Product) : CheckoutEvent

    data object ProductUnavailable : CheckoutEvent

    data object PayClicked : CheckoutEvent

    data object RetryClicked : CheckoutEvent

    data class PaymentSucceeded(
        val requestId: Long,
        val receipt: OrderReceipt,
    ) : CheckoutEvent

    data class PaymentFailed(
        val requestId: Long,
        val message: String,
    ) : CheckoutEvent
}

sealed interface CheckoutCommand {
    data class LoadProduct(val productId: Long) : CheckoutCommand

    data class SubmitPayment(
        val requestId: Long,
        val product: Product,
    ) : CheckoutCommand
}

sealed interface CheckoutEffect {
    data class PaymentCompleted(val orderId: Long) : CheckoutEffect
}

typealias CheckoutTransition = AfsmTransition<CheckoutState, CheckoutCommand, CheckoutEffect>
