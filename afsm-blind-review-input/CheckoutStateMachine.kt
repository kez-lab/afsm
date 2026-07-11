package afsm.sample.shop.feature.checkout

import afsm.core.AfsmGraph
import afsm.core.AfsmMachine
import afsm.core.afsmMachine

@AfsmGraph(
    id = "Checkout",
    fileName = "CheckoutStateMachine.mmd",
)
internal val checkoutStateMachine:
    AfsmMachine<CheckoutState, CheckoutEvent, CheckoutCommand, CheckoutEffect> =
    afsmMachine(initialPhase = CheckoutPhase.Idle) {
        phase(CheckoutPhase.Idle) {
            on<CheckoutEvent.ScreenEntered> {
                transitionTo(CheckoutPhase.ProductLoading)
            }

            on<CheckoutEvent.PayClicked> {
                updateData {
                    copy(errorMessage = "Product is required before payment.")
                }
            }

            on<CheckoutEvent.RetryClicked> {
                updateData {
                    copy(errorMessage = "Product is required before payment.")
                }
            }
        }

        phase(CheckoutPhase.ProductLoading) {
            onEnter {
                updateData { copy(errorMessage = null) }
                command(label = "LoadProduct") {
                    CheckoutCommand.LoadProduct(data.productId)
                }
            }

            on<CheckoutEvent.ScreenEntered> {
                ignore(reason = "Product load is already in flight.")
            }

            on<CheckoutEvent.ProductLoaded> {
                case {
                    updateData { data, event ->
                        data.copy(
                            product = event.product,
                            errorMessage = null,
                        )
                    }
                    transitionTo(CheckoutPhase.ProductReady)
                }
            }

            on<CheckoutEvent.ProductUnavailable> {
                case {
                    updateData {
                        copy(
                            product = null,
                            errorMessage = "Product is no longer available.",
                        )
                    }
                    transitionTo(CheckoutPhase.ProductUnavailable)
                }
            }

        }

        phase(CheckoutPhase.ProductReady) {
            on<CheckoutEvent.PayClicked> {
                case(
                    label = "product loaded",
                    condition = { data.hasLoadedProduct() },
                ) {
                    updateData {
                        copy(
                            nextPaymentRequestId = nextPaymentRequestId + 1,
                            errorMessage = null,
                        )
                    }
                    transitionTo<CheckoutPhase.PaymentInProgress> {
                        CheckoutPhase.PaymentInProgress(
                            requestId = data.nextPaymentRequestId,
                        )
                    }
                }

                case(
                    label = "missing product",
                    condition = { data.isMissingProduct() },
                ) {
                    updateData {
                        copy(errorMessage = "Product is required before payment.")
                    }
                }
            }

        }

        phase<CheckoutPhase.PaymentInProgress> {
            onEnter {
                command(label = "SubmitPayment") {
                    val product = requireNotNull(data.product) {
                        "PaymentInProgress requires a loaded product."
                    }
                    CheckoutCommand.SubmitPayment(
                        requestId = phase.requestId,
                        product = product,
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
                case(
                    label = "matching request",
                    condition = { phase.requestId == event.requestId },
                ) {
                    updateData { copy(errorMessage = null) }
                    effect(label = "PaymentCompleted") {
                        CheckoutEffect.PaymentCompleted(event.receipt.orderId)
                    }
                    transitionTo<CheckoutPhase.Completed> {
                        CheckoutPhase.Completed(
                            orderId = event.receipt.orderId,
                        )
                    }
                }

                ignore(
                    reason = "Stale payment success result.",
                    condition = { phase.requestId != event.requestId },
                )
            }

            on<CheckoutEvent.PaymentFailed> {
                case(
                    label = "matching request",
                    condition = { phase.requestId == event.requestId },
                ) {
                    updateData { data, event ->
                        data.copy(errorMessage = event.message)
                    }
                    transitionTo(CheckoutPhase.PaymentFailed)
                }

                ignore(
                    reason = "Stale payment failure result.",
                    condition = { phase.requestId != event.requestId },
                )
            }
        }

        phase(CheckoutPhase.PaymentFailed) {
            on<CheckoutEvent.RetryClicked> {
                case(
                    label = "product loaded",
                    condition = { data.hasLoadedProduct() },
                ) {
                    updateData {
                        copy(
                            nextPaymentRequestId = nextPaymentRequestId + 1,
                            errorMessage = null,
                        )
                    }
                    transitionTo<CheckoutPhase.PaymentInProgress> {
                        CheckoutPhase.PaymentInProgress(
                            requestId = data.nextPaymentRequestId,
                        )
                    }
                }

                case(
                    label = "missing product",
                    condition = { data.isMissingProduct() },
                ) {
                    updateData {
                        copy(errorMessage = "Product is required before payment.")
                    }
                }
            }

        }

        phase(CheckoutPhase.ProductUnavailable)

        phase<CheckoutPhase.PaymentStatusUnknown>()

        phase<CheckoutPhase.Completed> {
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

private fun CheckoutData.hasLoadedProduct(): Boolean {
    return product != null
}

private fun CheckoutData.isMissingProduct(): Boolean {
    return product == null
}
