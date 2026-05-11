package afsm.sample.shop.feature.checkout

import afsm.core.Afsm
import afsm.core.AfsmReducer

class CheckoutStateMachine : AfsmReducer<CheckoutState, CheckoutEvent, CheckoutCommand, CheckoutEffect> {
    override fun transition(
        state: CheckoutState,
        event: CheckoutEvent,
    ): CheckoutTransition {
        return when (event) {
            CheckoutEvent.ScreenEntered -> {
                if (state.product != null || state.isLoadingProduct) {
                    Afsm.ignore(state, reason = "Product is already loaded or loading.")
                } else {
                    Afsm.transitionTo(
                        state = state.copy(
                            isLoadingProduct = true,
                            errorMessage = null,
                        ),
                        commands = listOf(CheckoutCommand.LoadProduct(state.productId)),
                    )
                }
            }

            is CheckoutEvent.ProductLoaded -> {
                if (!state.isLoadingProduct) {
                    Afsm.invalid(state, reason = "Product loaded without a pending load command.")
                } else {
                    Afsm.transitionTo(
                        state = state.copy(
                            product = event.product,
                            isLoadingProduct = false,
                            errorMessage = null,
                        ),
                    )
                }
            }

            CheckoutEvent.ProductUnavailable -> {
                if (!state.isLoadingProduct) {
                    Afsm.invalid(state, reason = "Product unavailable without a pending load command.")
                } else {
                    Afsm.transitionTo(
                        state = state.copy(
                            isLoadingProduct = false,
                            errorMessage = "Product is no longer available.",
                        ),
                    )
                }
            }

            CheckoutEvent.PayClicked -> startPayment(state)

            CheckoutEvent.RetryClicked -> startPayment(state)

            is CheckoutEvent.PaymentSucceeded -> {
                if (state.activePaymentRequestId != event.requestId) {
                    Afsm.ignore(state, reason = "Stale payment success result.")
                } else {
                    Afsm.transitionTo(
                        state = state.copy(
                            isPaying = false,
                            isComplete = true,
                            activePaymentRequestId = null,
                            orderId = event.receipt.orderId,
                            errorMessage = null,
                        ),
                        effects = listOf(CheckoutEffect.PaymentCompleted(event.receipt.orderId)),
                    )
                }
            }

            is CheckoutEvent.PaymentFailed -> {
                if (state.activePaymentRequestId != event.requestId) {
                    Afsm.ignore(state, reason = "Stale payment failure result.")
                } else {
                    Afsm.transitionTo(
                        state = state.copy(
                            isPaying = false,
                            activePaymentRequestId = null,
                            errorMessage = event.message,
                        ),
                    )
                }
            }
        }
    }

    private fun startPayment(state: CheckoutState): CheckoutTransition {
        val product = state.product
        return when {
            state.isPaying -> {
                Afsm.ignore(state, reason = "Duplicate payment event while payment is in flight.")
            }

            state.isLoadingProduct -> {
                Afsm.ignore(state, reason = "Cannot pay before product load finishes.")
            }

            product == null -> {
                Afsm.stay(state.copy(errorMessage = "Product is required before payment."))
            }

            else -> {
                val requestId = state.nextPaymentRequestId + 1
                Afsm.transitionTo(
                    state = state.copy(
                        isPaying = true,
                        nextPaymentRequestId = requestId,
                        activePaymentRequestId = requestId,
                        errorMessage = null,
                    ),
                    commands = listOf(
                        CheckoutCommand.SubmitPayment(
                            requestId = requestId,
                            product = product,
                        ),
                    ),
                )
            }
        }
    }
}
