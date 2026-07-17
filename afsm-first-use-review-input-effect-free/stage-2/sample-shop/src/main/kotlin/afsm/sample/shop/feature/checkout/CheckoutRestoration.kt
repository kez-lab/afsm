package afsm.sample.shop.feature.checkout

import androidx.lifecycle.SavedStateHandle

internal const val CheckoutProductIdKey = "checkout.productId"
internal const val CheckoutCompletedOrderIdKey = "checkout.completedOrderId"
internal const val CheckoutPendingPaymentRequestIdKey = "checkout.pendingPaymentRequestId"

internal fun checkoutStateFromSavedState(
    savedStateHandle: SavedStateHandle,
    navigationProductId: Long,
): CheckoutState {
    val productId = savedStateHandle.get<Long>(CheckoutProductIdKey) ?: navigationProductId
    savedStateHandle[CheckoutProductIdKey] = productId

    val completedOrderId = savedStateHandle.get<Long>(CheckoutCompletedOrderIdKey)
    if (completedOrderId != null) {
        savedStateHandle.remove<Long>(CheckoutPendingPaymentRequestIdKey)
        return checkoutState(
            productId = productId,
            phase = CheckoutPhase.Completed(orderId = completedOrderId),
        )
    }

    val pendingRequestId = savedStateHandle.get<Long>(CheckoutPendingPaymentRequestIdKey)
    if (pendingRequestId != null) {
        return checkoutState(
            productId = productId,
            phase = CheckoutPhase.PaymentStatusUnknown(requestId = pendingRequestId),
            data = CheckoutData(
                productId = productId,
                nextPaymentRequestId = pendingRequestId,
                errorMessage = CheckoutPaymentStatusUnknownMessage,
            ),
        )
    }

    return checkoutState(productId = productId)
}
