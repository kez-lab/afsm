package afsm.sample.shop.feature.editor

import afsm.sample.shop.core.data.ProductRepository
import afsm.sample.shop.core.data.SessionRepository
import afsm.viewmodel.afsmHost
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

class ProductEditorViewModel(
    private val productRepository: ProductRepository,
    private val sessionRepository: SessionRepository,
) : ViewModel() {
    private val host = afsmHost(
        machine = ProductEditorStateMachine,
        commandHandler = { command: ProductEditorCommand, dispatch ->
            when (command) {
                is ProductEditorCommand.SaveDraft -> {
                    delay(120)
                    dispatch(ProductEditorEvent.DraftSaveCompleted)
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

    val state: StateFlow<ProductEditorState> = host.state
    val effects: Flow<ProductEditorEffect> = host.effects

    fun onEvent(event: ProductEditorEvent) {
        host.dispatch(event)
    }
}
