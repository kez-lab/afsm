package afsm.sample.shop.feature.editor

import afsm.sample.shop.core.data.ProductRepository
import afsm.sample.shop.core.data.SessionRepository
import afsm.viewmodel.afsmHost
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.delay

class ProductEditorViewModel(
    private val productRepository: ProductRepository,
    private val sessionRepository: SessionRepository,
) : ViewModel() {
    private val host = afsmHost(
        initialState = ProductEditorState.EditingDraft(),
        stateMachine = ProductEditorStateMachine(),
        commandHandler = { command: ProductEditorCommand, dispatch ->
            when (command) {
                is ProductEditorCommand.SaveDraft -> {
                    delay(120)
                    dispatch(ProductEditorEvent.DraftSaved)
                }

                is ProductEditorCommand.StartImageUpload -> {
                    delay(250)
                    dispatch(ProductEditorEvent.ImageUploadSucceeded("mock-upload-token"))
                }

                is ProductEditorCommand.StartReviewSubmission -> {
                    delay(250)
                    if (command.draft.reviewAttempt == 1) {
                        dispatch(
                            ProductEditorEvent.ReviewRejected(
                                "Mock reviewer asks for one resubmission.",
                            ),
                        )
                    } else {
                        dispatch(ProductEditorEvent.ReviewApproved)
                    }
                }

                is ProductEditorCommand.StartProductPublish -> {
                    val form = command.draft.form
                    val priceCents = form.priceCentsOrNull()
                    if (priceCents == null) {
                        dispatch(ProductEditorEvent.PublishFailed("Enter a valid price."))
                    } else {
                        val productId = productRepository.addProduct(
                            title = form.title,
                            description = form.description,
                            priceCents = priceCents,
                            sellerUserId = sessionRepository.currentSession()?.userId,
                        )
                        dispatch(ProductEditorEvent.PublishSucceeded(productId))
                    }
                }
            }
        },
    )

    val state = host.state
    val effects = host.effects

    fun onEvent(event: ProductEditorEvent) {
        host.dispatch(event)
    }
}
