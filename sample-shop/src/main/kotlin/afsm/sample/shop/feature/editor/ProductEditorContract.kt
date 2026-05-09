package afsm.sample.shop.feature.editor

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

sealed interface ProductEditorState {
    data class EditingDraft(
        val draft: ProductDraft = ProductDraft(),
        val errorMessage: String? = null,
    ) : ProductEditorState

    data class SavingDraft(
        val draft: ProductDraft,
    ) : ProductEditorState

    data class DraftSaved(
        val draft: ProductDraft,
    ) : ProductEditorState

    data class UploadingImages(
        val draft: ProductDraft,
    ) : ProductEditorState

    data class SubmittingForReview(
        val draft: ProductDraft,
        val uploadToken: String,
    ) : ProductEditorState

    data class Rejected(
        val draft: ProductDraft,
        val reason: String,
        val errorMessage: String? = null,
    ) : ProductEditorState

    data class Approved(
        val draft: ProductDraft,
    ) : ProductEditorState

    data class Publishing(
        val draft: ProductDraft,
    ) : ProductEditorState

    data class Published(
        val productId: Long,
        val title: String,
    ) : ProductEditorState
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

    data object DraftSaved : ProductEditorEvent

    data class ImageUploadSucceeded(val uploadToken: String) : ProductEditorEvent

    data class ImageUploadFailed(val message: String) : ProductEditorEvent

    data object ReviewApproved : ProductEditorEvent

    data class ReviewRejected(val reason: String) : ProductEditorEvent

    data class PublishSucceeded(val productId: Long) : ProductEditorEvent

    data class PublishFailed(val message: String) : ProductEditorEvent
}

sealed interface ProductEditorCommand {
    data class SaveDraft(val draft: ProductDraft) : ProductEditorCommand

    data class UploadImages(val draft: ProductDraft) : ProductEditorCommand

    data class SubmitForReview(
        val draft: ProductDraft,
        val uploadToken: String,
    ) : ProductEditorCommand

    data class PublishProduct(val draft: ProductDraft) : ProductEditorCommand
}

sealed interface ProductEditorEffect {
    data object CloseEditor : ProductEditorEffect
}

typealias ProductEditorTransition =
    AfsmTransition<ProductEditorState, ProductEditorCommand, ProductEditorEffect>

fun ProductEditorState.draftOrNull(): ProductDraft? {
    return when (this) {
        is ProductEditorState.EditingDraft -> draft
        is ProductEditorState.SavingDraft -> draft
        is ProductEditorState.DraftSaved -> draft
        is ProductEditorState.UploadingImages -> draft
        is ProductEditorState.SubmittingForReview -> draft
        is ProductEditorState.Rejected -> draft
        is ProductEditorState.Approved -> draft
        is ProductEditorState.Publishing -> draft
        is ProductEditorState.Published -> null
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
