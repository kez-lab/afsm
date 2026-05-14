package afsm.sample.shop.feature.checkout

import afsm.core.AfsmGraph
import afsm.core.AfsmMachine
import afsm.core.afsmMachine

private typealias CheckoutMachine =
    AfsmMachine<CheckoutState, CheckoutEvent, CheckoutCommand, CheckoutEffect>

@AfsmGraph(
    id = "Checkout",
    fileName = "CheckoutStateMachine.mmd",
)
internal object CheckoutStateMachine : CheckoutMachine by checkoutMachine()

private fun checkoutMachine(): CheckoutMachine {
    return afsmMachine {
        initial(
            phase = CheckoutPhase.Idle,
            context = CheckoutContext(productId = 0),
        )

        state(CheckoutPhase.Idle) {
            on<CheckoutEvent.ScreenEntered> {
                transitionTo(CheckoutPhase.ProductLoading)
            }

            on<CheckoutEvent.PayClicked> {
                stay {
                    updateContext {
                        copy(errorMessage = "Product is required before payment.")
                    }
                }
            }

            on<CheckoutEvent.RetryClicked> {
                stay {
                    updateContext {
                        copy(errorMessage = "Product is required before payment.")
                    }
                }
            }
        }

        state(CheckoutPhase.ProductLoading) {
            onEnter(commandLabels = listOf("LoadProduct")) {
                updateContext { copy(errorMessage = null) }
                command(CheckoutCommand.LoadProduct(context.productId))
            }

            on<CheckoutEvent.ScreenEntered> {
                ignore(reason = "Product load is already in flight.")
            }

            on<CheckoutEvent.ProductLoaded> {
                transitionTo(CheckoutPhase.ProductReady) {
                    updateContext {
                        copy(
                            product = event.product,
                            errorMessage = null,
                        )
                    }
                }
            }

            on<CheckoutEvent.ProductUnavailable> {
                transitionTo(CheckoutPhase.ProductUnavailable) {
                    updateContext {
                        copy(
                            product = null,
                            errorMessage = "Product is no longer available.",
                        )
                    }
                }
            }

            on<CheckoutEvent.PayClicked> {
                ignore(reason = "Cannot pay before product load finishes.")
            }

            on<CheckoutEvent.RetryClicked> {
                ignore(reason = "Cannot retry before product load finishes.")
            }
        }

        state(CheckoutPhase.ProductReady) {
            on<CheckoutEvent.PayClicked> {
                transitionTo<CheckoutPhase.PaymentInProgress>(
                    phase = {
                        CheckoutPhase.PaymentInProgress(
                            requestId = context.nextPaymentRequestId + 1,
                        )
                    },
                    guardLabel = "product loaded",
                    guard = { context.product != null },
                ) {
                    updateContext {
                        copy(
                            nextPaymentRequestId = nextPaymentRequestId + 1,
                            errorMessage = null,
                        )
                    }
                }

                otherwise(label = "missing product") {
                    updateContext {
                        copy(errorMessage = "Product is required before payment.")
                    }
                }
            }

            on<CheckoutEvent.RetryClicked> {
                ignore(reason = "Retry is only valid after a payment failure.")
            }

            on<CheckoutEvent.PaymentSucceeded> {
                ignore(reason = "Payment result arrived without an active request.")
            }

            on<CheckoutEvent.PaymentFailed> {
                ignore(reason = "Payment result arrived without an active request.")
            }
        }

        state<CheckoutPhase.PaymentInProgress> {
            onEnter(commandLabels = listOf("SubmitPayment")) {
                context.product?.let { product ->
                    command(
                        CheckoutCommand.SubmitPayment(
                            requestId = phase.requestId,
                            product = product,
                        ),
                    )
                }
            }

            on<CheckoutEvent.PayClicked> {
                ignore(reason = "Duplicate payment event while payment is in flight.")
            }

            on<CheckoutEvent.RetryClicked> {
                ignore(reason = "Duplicate retry event while payment is in flight.")
            }

            on<CheckoutEvent.PaymentSucceeded> {
                transitionTo<CheckoutPhase.Completed>(
                    phase = {
                        CheckoutPhase.Completed(
                            orderId = event.receipt.orderId,
                        )
                    },
                    effectLabels = listOf("PaymentCompleted"),
                    guardLabel = "matching request",
                    guard = { phase.requestId == event.requestId },
                ) {
                    updateContext { copy(errorMessage = null) }
                    effect(CheckoutEffect.PaymentCompleted(event.receipt.orderId))
                }

                ignore(
                    reason = "Stale payment success result.",
                    guard = { phase.requestId != event.requestId },
                )
            }

            on<CheckoutEvent.PaymentFailed> {
                transitionTo(
                    phase = CheckoutPhase.PaymentFailed,
                    guardLabel = "matching request",
                    guard = { phase.requestId == event.requestId },
                ) {
                    updateContext {
                        copy(errorMessage = event.message)
                    }
                }

                ignore(
                    reason = "Stale payment failure result.",
                    guard = { phase.requestId != event.requestId },
                )
            }
        }

        state(CheckoutPhase.PaymentFailed) {
            on<CheckoutEvent.RetryClicked> {
                transitionTo<CheckoutPhase.PaymentInProgress>(
                    phase = {
                        CheckoutPhase.PaymentInProgress(
                            requestId = context.nextPaymentRequestId + 1,
                        )
                    },
                    guardLabel = "product loaded",
                    guard = { context.product != null },
                ) {
                    updateContext {
                        copy(
                            nextPaymentRequestId = nextPaymentRequestId + 1,
                            errorMessage = null,
                        )
                    }
                }

                otherwise(label = "missing product") {
                    updateContext {
                        copy(errorMessage = "Product is required before payment.")
                    }
                }
            }

            on<CheckoutEvent.PayClicked> {
                ignore(reason = "Use retry after payment failure.")
            }

            on<CheckoutEvent.PaymentSucceeded> {
                ignore(reason = "Payment result arrived without an active request.")
            }

            on<CheckoutEvent.PaymentFailed> {
                ignore(reason = "Payment result arrived without an active request.")
            }
        }

        state(CheckoutPhase.ProductUnavailable) {
            on<CheckoutEvent.ScreenEntered> {
                ignore(reason = "Product is unavailable.")
            }

            on<CheckoutEvent.PayClicked> {
                ignore(reason = "Cannot pay for an unavailable product.")
            }

            on<CheckoutEvent.RetryClicked> {
                ignore(reason = "Cannot retry payment for an unavailable product.")
            }
        }

        state<CheckoutPhase.Completed> {
            on<CheckoutEvent.PayClicked> {
                ignore(reason = "Checkout is already complete.")
            }

            on<CheckoutEvent.RetryClicked> {
                ignore(reason = "Checkout is already complete.")
            }

            on<CheckoutEvent.PaymentSucceeded> {
                ignore(reason = "Checkout is already complete.")
            }

            on<CheckoutEvent.PaymentFailed> {
                ignore(reason = "Checkout is already complete.")
            }
        }
    }
}
