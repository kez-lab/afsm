package afsm.sample.shop.feature.editor

import afsm.sample.shop.core.data.ProductRepository
import afsm.sample.shop.core.data.SessionRepository
import afsm.viewmodel.afsmHost
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlin.coroutines.cancellation.CancellationException

class ProductEditorViewModel(
    private val productRepository: ProductRepository,
    private val sessionRepository: SessionRepository,
    private val imageUploader: ProductImageUploader,
) : ViewModel() {
    private val host = afsmHost(
        machine = productEditorStateMachine,
        commandHandler = { command: ProductEditorCommand, dispatchEvent ->
            when (command) {
                is ProductEditorCommand.SaveDraft -> {
                    delay(120)
                    dispatchEvent(ProductEditorEvent.DraftSaveCompleted)
                }

                is ProductEditorCommand.StartImageUpload -> {
                    try {
                        val uploadToken = imageUploader.upload(command.draft)
                        dispatchEvent(ProductEditorEvent.ImageUploadSucceeded(uploadToken))
                    } catch (cancellation: CancellationException) {
                        throw cancellation
                    } catch (_: Exception) {
                        dispatchEvent(
                            ProductEditorEvent.ImageUploadFailed(
                                message = "Image upload failed.",
                            ),
                        )
                    }
                }

                is ProductEditorCommand.StartReviewSubmission -> {
                    delay(250)
                    if (command.draft.reviewAttempt == 1) {
                        dispatchEvent(
                            ProductEditorEvent.ReviewRejected(
                                "Mock reviewer asks for one resubmission.",
                            ),
                        )
                    } else {
                        dispatchEvent(ProductEditorEvent.ReviewApproved)
                    }
                }

                is ProductEditorCommand.StartProductPublish -> {
                    val form = command.draft.form
                    val priceCents = form.priceCentsOrNull()
                    if (priceCents == null) {
                        dispatchEvent(ProductEditorEvent.PublishFailed("Enter a valid price."))
                    } else {
                        val productId = productRepository.addProduct(
                            title = form.title,
                            description = form.description,
                            priceCents = priceCents,
                            sellerUserId = sessionRepository.currentSession()?.userId,
                        )
                        dispatchEvent(ProductEditorEvent.PublishSucceeded(productId))
                    }
                }
            }
        },
    )

    val state: StateFlow<ProductEditorState> = host.state

    fun updateTitle(value: String) = dispatch(ProductEditorEvent.TitleChanged(value))

    fun updateDescription(value: String) = dispatch(ProductEditorEvent.DescriptionChanged(value))

    fun updatePrice(value: String) = dispatch(ProductEditorEvent.PriceChanged(value))

    fun saveDraft() = dispatch(ProductEditorEvent.SaveDraftClicked)

    fun continueEditing() = dispatch(ProductEditorEvent.ContinueEditingClicked)

    fun submitForReview() = dispatch(ProductEditorEvent.SubmitClicked)

    fun resubmitForReview() = dispatch(ProductEditorEvent.ResubmitClicked)

    fun publish() = dispatch(ProductEditorEvent.PublishClicked)

    fun cancelUpload() = dispatch(ProductEditorEvent.CancelUploadClicked)

    private fun dispatch(event: ProductEditorEvent) = host.dispatch(event)
}
