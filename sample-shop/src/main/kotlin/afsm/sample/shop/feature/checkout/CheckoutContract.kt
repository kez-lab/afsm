package afsm.sample.shop.feature.checkout

import afsm.core.AfsmState
import afsm.core.AfsmTransition
import afsm.sample.shop.core.model.OrderReceipt
import afsm.sample.shop.core.model.Product

data class CheckoutData(
    val productId: Long,
    val product: Product? = null,
    val nextPaymentRequestId: Long = 0,
    val errorMessage: String? = null,
)

typealias CheckoutState = AfsmState<CheckoutPhase, CheckoutData>

internal const val CheckoutPaymentStatusUnknownMessage =
    "Payment status is unknown. Check your orders before trying again."

fun checkoutState(
    productId: Long,
    phase: CheckoutPhase = CheckoutPhase.Idle,
    data: CheckoutData = CheckoutData(productId = productId),
): CheckoutState {
    return AfsmState(
        phase = phase,
        data = data,
    )
}

sealed interface CheckoutPhase {
    data object Idle : CheckoutPhase

    data object ProductLoading : CheckoutPhase

    data object ProductReady : CheckoutPhase

    data class PaymentInProgress(
        val requestId: Long,
    ) : CheckoutPhase

    data object PaymentFailed : CheckoutPhase

    data object ProductUnavailable : CheckoutPhase

    data class PaymentStatusUnknown(
        val requestId: Long,
    ) : CheckoutPhase

    data class Completed(
        val orderId: Long,
    ) : CheckoutPhase
}

data class CheckoutRenderState(
    val product: Product?,
    val isLoadingProduct: Boolean,
    val isPaying: Boolean,
    val isComplete: Boolean,
    val orderId: Long?,
    val primaryAction: CheckoutPrimaryAction? = null,
    val errorMessage: String? = null,
)

enum class CheckoutPrimaryAction {
    Pay,
    RetryPayment,
}

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

fun CheckoutState.toRenderState(): CheckoutRenderState {
    return when (val currentPhase = phase) {
        CheckoutPhase.Idle -> CheckoutRenderState(
            product = data.product,
            isLoadingProduct = false,
            isPaying = false,
            isComplete = false,
            orderId = null,
            errorMessage = data.errorMessage,
        )

        CheckoutPhase.ProductLoading -> CheckoutRenderState(
            product = data.product,
            isLoadingProduct = true,
            isPaying = false,
            isComplete = false,
            orderId = null,
        )

        CheckoutPhase.ProductReady -> CheckoutRenderState(
            product = data.product,
            isLoadingProduct = false,
            isPaying = false,
            isComplete = false,
            orderId = null,
            primaryAction = CheckoutPrimaryAction.Pay,
            errorMessage = data.errorMessage,
        )

        is CheckoutPhase.PaymentInProgress -> CheckoutRenderState(
            product = data.product,
            isLoadingProduct = false,
            isPaying = true,
            isComplete = false,
            orderId = null,
            primaryAction = CheckoutPrimaryAction.Pay,
            errorMessage = data.errorMessage,
        )

        CheckoutPhase.PaymentFailed -> CheckoutRenderState(
            product = data.product,
            isLoadingProduct = false,
            isPaying = false,
            isComplete = false,
            orderId = null,
            primaryAction = CheckoutPrimaryAction.RetryPayment,
            errorMessage = data.errorMessage,
        )

        CheckoutPhase.ProductUnavailable -> CheckoutRenderState(
            product = null,
            isLoadingProduct = false,
            isPaying = false,
            isComplete = false,
            orderId = null,
            errorMessage = data.errorMessage ?: "Product is no longer available.",
        )

        is CheckoutPhase.PaymentStatusUnknown -> CheckoutRenderState(
            product = null,
            isLoadingProduct = false,
            isPaying = false,
            isComplete = false,
            orderId = null,
            errorMessage = data.errorMessage ?: CheckoutPaymentStatusUnknownMessage,
        )

        is CheckoutPhase.Completed -> CheckoutRenderState(
            product = data.product,
            isLoadingProduct = false,
            isPaying = false,
            isComplete = true,
            orderId = currentPhase.orderId,
        )
    }
}
