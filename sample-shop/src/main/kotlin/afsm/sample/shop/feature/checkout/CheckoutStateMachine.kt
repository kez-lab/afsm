package afsm.sample.shop.feature.checkout

import afsm.core.Afsm
import afsm.core.AfsmStateMachine

class CheckoutStateMachine : AfsmStateMachine<CheckoutState, CheckoutEvent, CheckoutCommand, CheckoutEffect> {
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
                if (!state.isPaying) {
                    Afsm.invalid(state, reason = "Payment success arrived without a pending payment.")
                } else {
                    Afsm.transitionTo(
                        state = state.copy(
                            isPaying = false,
                            isComplete = true,
                            orderId = event.receipt.orderId,
                            errorMessage = null,
                        ),
                        effects = listOf(CheckoutEffect.PaymentCompleted(event.receipt.orderId)),
                    )
                }
            }

            is CheckoutEvent.PaymentFailed -> {
                if (!state.isPaying) {
                    Afsm.invalid(state, reason = "Payment failure arrived without a pending payment.")
                } else {
                    Afsm.transitionTo(
                        state = state.copy(
                            isPaying = false,
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
                Afsm.transitionTo(
                    state = state.copy(
                        isPaying = true,
                        errorMessage = null,
                    ),
                    commands = listOf(CheckoutCommand.SubmitPayment(product)),
                )
            }
        }
    }
}
