package afsm.sample.shop.feature.checkout

import afsm.sample.shop.core.data.PaymentRepository
import afsm.sample.shop.core.data.ProductRepository
import afsm.sample.shop.core.data.SessionRepository
import afsm.viewmodel.afsmHost
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

class CheckoutViewModel(
    productId: Long,
    private val productRepository: ProductRepository,
    private val paymentRepository: PaymentRepository,
    private val sessionRepository: SessionRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val initialState = checkoutStateFromSavedState(
        savedStateHandle = savedStateHandle,
        navigationProductId = productId,
    )

    private val host = afsmHost(
        machine = checkoutStateMachine,
        initialState = initialState,
        commandHandler = { command: CheckoutCommand, dispatchEvent ->
            when (command) {
                is CheckoutCommand.LoadProduct -> {
                    val product = productRepository.findProduct(command.productId)
                    if (product == null) {
                        dispatchEvent(CheckoutEvent.ProductUnavailable)
                    } else {
                        dispatchEvent(CheckoutEvent.ProductLoaded(product))
                    }
                }

                is CheckoutCommand.SubmitPayment -> {
                    val session = sessionRepository.currentSession()
                    if (session == null) {
                        dispatchEvent(
                            CheckoutEvent.PaymentFailed(
                                requestId = command.requestId,
                                message = "Login is required.",
                            ),
                        )
                    } else {
                        savedStateHandle[CheckoutPendingPaymentRequestIdKey] = command.requestId
                        paymentRepository.submitPayment(
                            session = session,
                            product = command.product,
                        ).fold(
                            onSuccess = { receipt ->
                                savedStateHandle[CheckoutCompletedOrderIdKey] = receipt.orderId
                                savedStateHandle.remove<Long>(CheckoutPendingPaymentRequestIdKey)
                                dispatchEvent(
                                    CheckoutEvent.PaymentSucceeded(
                                        requestId = command.requestId,
                                        receipt = receipt,
                                    ),
                                )
                            },
                            onFailure = { error ->
                                savedStateHandle.remove<Long>(CheckoutPendingPaymentRequestIdKey)
                                dispatchEvent(
                                    CheckoutEvent.PaymentFailed(
                                        requestId = command.requestId,
                                        message = error.message ?: "Payment failed.",
                                    ),
                                )
                            },
                        )
                    }
                }
            }
        },
    )

    val state: StateFlow<CheckoutState> = host.state
    val effects: Flow<CheckoutEffect> = host.effects

    init {
        if (initialState.phase == CheckoutPhase.Idle) {
            host.dispatch(CheckoutEvent.ScreenEntered)
        }
    }

    fun onEvent(event: CheckoutEvent) {
        host.dispatch(event)
    }
}
