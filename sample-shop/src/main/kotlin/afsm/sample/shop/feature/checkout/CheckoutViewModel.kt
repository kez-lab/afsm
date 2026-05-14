package afsm.sample.shop.feature.checkout

import afsm.runtime.AfsmCommandHandler
import afsm.sample.shop.core.data.PaymentRepository
import afsm.sample.shop.core.data.ProductRepository
import afsm.sample.shop.core.data.SessionRepository
import afsm.viewmodel.afsmHost
import androidx.lifecycle.ViewModel

class CheckoutViewModel(
    productId: Long,
    private val productRepository: ProductRepository,
    private val paymentRepository: PaymentRepository,
    private val sessionRepository: SessionRepository,
) : ViewModel() {
    private val host = afsmHost(
        machine = CheckoutStateMachine,
        initialState = checkoutState(productId = productId),
        commandHandler = AfsmCommandHandler { command: CheckoutCommand, dispatch ->
            when (command) {
                is CheckoutCommand.LoadProduct -> {
                    val product = productRepository.findProduct(command.productId)
                    if (product == null) {
                        dispatch(CheckoutEvent.ProductUnavailable)
                    } else {
                        dispatch(CheckoutEvent.ProductLoaded(product))
                    }
                }

                is CheckoutCommand.SubmitPayment -> {
                    val session = sessionRepository.currentSession()
                    if (session == null) {
                        dispatch(
                            CheckoutEvent.PaymentFailed(
                                requestId = command.requestId,
                                message = "Login is required.",
                            ),
                        )
                    } else {
                        paymentRepository.submitPayment(
                            session = session,
                            product = command.product,
                        ).fold(
                            onSuccess = { receipt ->
                                dispatch(
                                    CheckoutEvent.PaymentSucceeded(
                                        requestId = command.requestId,
                                        receipt = receipt,
                                    ),
                                )
                            },
                            onFailure = { error ->
                                dispatch(
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

    val state = host.state
    val effects = host.effects

    init {
        host.dispatch(CheckoutEvent.ScreenEntered)
    }

    fun onEvent(event: CheckoutEvent) {
        host.dispatch(event)
    }
}
