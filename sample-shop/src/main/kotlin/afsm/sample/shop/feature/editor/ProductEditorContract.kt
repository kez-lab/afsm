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
