package afsm.sample.shop.feature.editor

import afsm.sample.shop.core.data.ProductRepository
import afsm.sample.shop.core.data.SessionRepository
import afsm.sample.shop.app.shopAfsmConfig
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
        config = shopAfsmConfig(),
        commandHandler = { command: ProductEditorCommand, send ->
            when (command) {
                is ProductEditorCommand.SaveDraft -> {
                    delay(120)
                    send(ProductEditorEvent.DraftSaveCompleted)
                }

                is ProductEditorCommand.StartImageUpload -> {
                    try {
                        val uploadToken = imageUploader.upload(command.draft)
                        send(ProductEditorEvent.ImageUploadSucceeded(uploadToken))
                    } catch (cancellation: CancellationException) {
                        throw cancellation
                    } catch (_: Exception) {
                        send(
                            ProductEditorEvent.ImageUploadFailed(
                                message = "Image upload failed.",
                            ),
                        )
                    }
                }

                is ProductEditorCommand.StartReviewSubmission -> {
                    delay(250)
                    if (command.draft.reviewAttempt == 1) {
                        send(
                            ProductEditorEvent.ReviewRejected(
                                "Mock reviewer asks for one resubmission.",
                            ),
                        )
                    } else {
                        send(ProductEditorEvent.ReviewApproved)
                    }
                }

                is ProductEditorCommand.StartProductPublish -> {
                    val form = command.draft.form
                    val priceCents = form.priceCentsOrNull()
                    if (priceCents == null) {
                        send(ProductEditorEvent.PublishFailed("Enter a valid price."))
                    } else {
                        val productId = productRepository.addProduct(
                            title = form.title,
                            description = form.description,
                            priceCents = priceCents,
                            sellerUserId = sessionRepository.currentSession()?.userId,
                        )
                        send(ProductEditorEvent.PublishSucceeded(productId))
                    }
                }
            }
        },
    )

    val state: StateFlow<ProductEditorState> = host.state

    fun updateTitle(value: String) = send(ProductEditorEvent.TitleChanged(value))

    fun updateDescription(value: String) = send(ProductEditorEvent.DescriptionChanged(value))

    fun updatePrice(value: String) = send(ProductEditorEvent.PriceChanged(value))

    fun saveDraft() = send(ProductEditorEvent.SaveDraftClicked)

    fun continueEditing() = send(ProductEditorEvent.ContinueEditingClicked)

    fun submitForReview() = send(ProductEditorEvent.SubmitClicked)

    fun resubmitForReview() = send(ProductEditorEvent.ResubmitClicked)

    fun publish() = send(ProductEditorEvent.PublishClicked)

    fun cancelUpload() = send(ProductEditorEvent.CancelUploadClicked)

    private fun send(event: ProductEditorEvent) = host.send(event)
}
