package afsm.sample.shop.feature.editor

import afsm.core.AfsmState
import afsm.core.AfsmTransition

data class ProductDraftForm(
    val title: String = "",
    val description: String = "",
    val priceText: String = "",
)

data class ProductDraft(
    val form: ProductDraftForm = ProductDraftForm(),
    val reviewAttempt: Int = 0,
)

data class ProductEditorContext(
    val draft: ProductDraft = ProductDraft(),
    val errorMessage: String? = null,
)

typealias ProductEditorState = AfsmState<ProductEditorPhase, ProductEditorContext>

fun productEditorState(
    phase: ProductEditorPhase = ProductEditorPhase.EditingDraft,
    context: ProductEditorContext = ProductEditorContext(),
): ProductEditorState {
    return AfsmState(
        phase = phase,
        context = context,
    )
}

sealed interface ProductEditorPhase {
    data object EditingDraft : ProductEditorPhase

    data object SavingDraft : ProductEditorPhase

    data object DraftSaved : ProductEditorPhase

    data object ImageUploadInProgress : ProductEditorPhase

    data class ReviewSubmissionInProgress(
        val uploadToken: String,
    ) : ProductEditorPhase

    data class Rejected(
        val reason: String,
    ) : ProductEditorPhase

    data object Approved : ProductEditorPhase

    data object PublishInProgress : ProductEditorPhase

    data class Published(
        val productId: Long,
        val title: String,
    ) : ProductEditorPhase
}

data class ProductEditorRenderState(
    val form: ProductDraftForm,
    val statusText: String,
    val showDraftFields: Boolean = true,
    val fieldsEnabled: Boolean,
    val isProcessing: Boolean,
    val primaryAction: ProductEditorPrimaryAction? = null,
    val secondaryAction: ProductEditorSecondaryAction? = null,
    val reviewNote: String? = null,
    val publishedTitle: String? = null,
    val errorMessage: String? = null,
)

enum class ProductEditorPrimaryAction {
    SubmitForReview,
    ResubmitForReview,
    Publish,
    Done,
}

enum class ProductEditorSecondaryAction {
    SaveDraft,
    ContinueEditing,
}

sealed interface ProductEditorEvent {
    data class TitleChanged(val value: String) : ProductEditorEvent

    data class DescriptionChanged(val value: String) : ProductEditorEvent

    data class PriceChanged(val value: String) : ProductEditorEvent

    data object SaveDraftClicked : ProductEditorEvent

    data object ContinueEditingClicked : ProductEditorEvent

    data object SubmitClicked : ProductEditorEvent

    data object ResubmitClicked : ProductEditorEvent

    data object PublishClicked : ProductEditorEvent

    data object DoneClicked : ProductEditorEvent

    data object DraftSaveCompleted : ProductEditorEvent

    data class ImageUploadSucceeded(val uploadToken: String) : ProductEditorEvent

    data class ImageUploadFailed(val message: String) : ProductEditorEvent

    data object ReviewApproved : ProductEditorEvent

    data class ReviewRejected(val reason: String) : ProductEditorEvent

    data class PublishSucceeded(val productId: Long) : ProductEditorEvent

    data class PublishFailed(val message: String) : ProductEditorEvent
}

sealed interface ProductEditorCommand {
    data class SaveDraft(val draft: ProductDraft) : ProductEditorCommand

    data class StartImageUpload(val draft: ProductDraft) : ProductEditorCommand

    data class StartReviewSubmission(
        val draft: ProductDraft,
        val uploadToken: String,
    ) : ProductEditorCommand

    data class StartProductPublish(val draft: ProductDraft) : ProductEditorCommand
}

sealed interface ProductEditorEffect {
    data object CloseEditor : ProductEditorEffect
}

typealias ProductEditorTransition =
    AfsmTransition<ProductEditorState, ProductEditorCommand, ProductEditorEffect>

fun ProductEditorState.draftOrNull(): ProductDraft? {
    return when (phase) {
        is ProductEditorPhase.Published -> null
        else -> context.draft
    }
}

fun ProductEditorState.toRenderState(): ProductEditorRenderState {
    val draft = draftOrNull()
    val form = draft?.form ?: ProductDraftForm()

    return when (val currentPhase = phase) {
        ProductEditorPhase.EditingDraft -> ProductEditorRenderState(
            form = form,
            statusText = "Editing draft",
            fieldsEnabled = true,
            isProcessing = false,
            primaryAction = ProductEditorPrimaryAction.SubmitForReview,
            secondaryAction = ProductEditorSecondaryAction.SaveDraft,
            errorMessage = context.errorMessage,
        )

        ProductEditorPhase.SavingDraft -> ProductEditorRenderState(
            form = form,
            statusText = "Saving draft",
            fieldsEnabled = false,
            isProcessing = true,
        )

        ProductEditorPhase.DraftSaved -> ProductEditorRenderState(
            form = form,
            statusText = "Draft saved",
            fieldsEnabled = false,
            isProcessing = false,
            primaryAction = ProductEditorPrimaryAction.SubmitForReview,
            secondaryAction = ProductEditorSecondaryAction.ContinueEditing,
            errorMessage = context.errorMessage,
        )

        ProductEditorPhase.ImageUploadInProgress -> ProductEditorRenderState(
            form = form,
            statusText = "Uploading mock images",
            fieldsEnabled = false,
            isProcessing = true,
        )

        is ProductEditorPhase.ReviewSubmissionInProgress -> ProductEditorRenderState(
            form = form,
            statusText = "Submitting for review",
            fieldsEnabled = false,
            isProcessing = true,
        )

        is ProductEditorPhase.Rejected -> ProductEditorRenderState(
            form = form,
            statusText = "Review rejected",
            fieldsEnabled = true,
            isProcessing = false,
            primaryAction = ProductEditorPrimaryAction.ResubmitForReview,
            secondaryAction = ProductEditorSecondaryAction.ContinueEditing,
            reviewNote = currentPhase.reason,
            errorMessage = context.errorMessage,
        )

        ProductEditorPhase.Approved -> ProductEditorRenderState(
            form = form,
            statusText = "Review approved",
            fieldsEnabled = false,
            isProcessing = false,
            primaryAction = ProductEditorPrimaryAction.Publish,
            secondaryAction = ProductEditorSecondaryAction.ContinueEditing,
            errorMessage = context.errorMessage,
        )

        ProductEditorPhase.PublishInProgress -> ProductEditorRenderState(
            form = form,
            statusText = "Publishing product",
            fieldsEnabled = false,
            isProcessing = true,
        )

        is ProductEditorPhase.Published -> ProductEditorRenderState(
            form = form,
            statusText = "Product published",
            showDraftFields = false,
            fieldsEnabled = false,
            isProcessing = false,
            primaryAction = ProductEditorPrimaryAction.Done,
            publishedTitle = currentPhase.title,
        )
    }
}

fun ProductDraftForm.priceCentsOrNull(): Long? {
    val normalized = priceText.trim()
    if (normalized.isBlank()) {
        return null
    }

    val parts = normalized.split(".")
    return when (parts.size) {
        1 -> parts[0].toLongOrNull()?.times(100)
        2 -> {
            val dollars = parts[0].toLongOrNull() ?: return null
            val cents = parts[1].padEnd(2, '0').take(2).toLongOrNull() ?: return null
            dollars * 100 + cents
        }

        else -> null
    }
}
