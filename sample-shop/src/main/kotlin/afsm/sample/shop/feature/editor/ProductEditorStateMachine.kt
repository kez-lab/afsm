package afsm.sample.shop.feature.editor

import afsm.core.AfsmPhaseEntry
import afsm.core.AfsmPhaseEntryPolicy
import afsm.core.AfsmPhasedStateMachine

class ProductEditorStateMachine : AfsmPhasedStateMachine<
    ProductEditorState,
    ProductEditorPhase,
    ProductEditorContext,
    ProductEditorEvent,
    ProductEditorCommand,
    ProductEditorEffect,
    >(
    entryPolicy = ProductEditorPhaseEntryPolicy(),
) {
    override fun ProductEditorTransitionScope.reduce(
        event: ProductEditorEvent,
    ): ProductEditorTransition {
        return when (val phase = state.phase) {
            ProductEditorPhase.EditingDraft -> reduceEditingDraft(event)
            ProductEditorPhase.SavingDraft -> reduceSavingDraft(event)
            ProductEditorPhase.DraftSaved -> reduceDraftSaved(event)
            ProductEditorPhase.ImageUploadInProgress -> reduceImageUploadInProgress(event)
            is ProductEditorPhase.ReviewSubmissionInProgress -> reduceReviewSubmissionInProgress(event)
            is ProductEditorPhase.Rejected -> reduceRejected(phase, event)
            ProductEditorPhase.Approved -> reduceApproved(event)
            ProductEditorPhase.PublishInProgress -> reducePublishInProgress(event)
            is ProductEditorPhase.Published -> reducePublished(event)
        }
    }

    private fun ProductEditorTransitionScope.reduceEditingDraft(
        event: ProductEditorEvent,
    ): ProductEditorTransition {
        return when (event) {
            is ProductEditorEvent.TitleChanged,
            is ProductEditorEvent.DescriptionChanged,
            is ProductEditorEvent.PriceChanged -> transitionTo(ProductEditorPhase.EditingDraft)

            ProductEditorEvent.SaveDraftClicked -> transitionTo(ProductEditorPhase.SavingDraft)

            ProductEditorEvent.SubmitClicked -> submitDraft(
                invalidTarget = ProductEditorPhase.EditingDraft,
            )

            ProductEditorEvent.ContinueEditingClicked,
            ProductEditorEvent.ResubmitClicked,
            ProductEditorEvent.PublishClicked,
            ProductEditorEvent.DoneClicked,
            ProductEditorEvent.DraftSaved,
            is ProductEditorEvent.ImageUploadSucceeded,
            is ProductEditorEvent.ImageUploadFailed,
            ProductEditorEvent.ReviewApproved,
            is ProductEditorEvent.ReviewRejected,
            is ProductEditorEvent.PublishSucceeded,
            is ProductEditorEvent.PublishFailed -> invalid(
                reason = "Event is not valid while editing a draft.",
            )
        }
    }

    private fun ProductEditorTransitionScope.reduceSavingDraft(
        event: ProductEditorEvent,
    ): ProductEditorTransition {
        return when (event) {
            ProductEditorEvent.DraftSaved -> transitionTo(ProductEditorPhase.DraftSaved)

            is ProductEditorEvent.TitleChanged,
            is ProductEditorEvent.DescriptionChanged,
            is ProductEditorEvent.PriceChanged,
            ProductEditorEvent.SaveDraftClicked,
            ProductEditorEvent.ContinueEditingClicked,
            ProductEditorEvent.SubmitClicked,
            ProductEditorEvent.ResubmitClicked,
            ProductEditorEvent.PublishClicked,
            ProductEditorEvent.DoneClicked,
            is ProductEditorEvent.ImageUploadSucceeded,
            is ProductEditorEvent.ImageUploadFailed,
            ProductEditorEvent.ReviewApproved,
            is ProductEditorEvent.ReviewRejected,
            is ProductEditorEvent.PublishSucceeded,
            is ProductEditorEvent.PublishFailed -> ignore(
                reason = "Draft save command is running.",
            )
        }
    }

    private fun ProductEditorTransitionScope.reduceDraftSaved(
        event: ProductEditorEvent,
    ): ProductEditorTransition {
        return when (event) {
            ProductEditorEvent.ContinueEditingClicked -> transitionTo(ProductEditorPhase.EditingDraft)

            ProductEditorEvent.SubmitClicked -> submitDraft(
                invalidTarget = ProductEditorPhase.EditingDraft,
            )

            is ProductEditorEvent.TitleChanged,
            is ProductEditorEvent.DescriptionChanged,
            is ProductEditorEvent.PriceChanged -> transitionTo(ProductEditorPhase.EditingDraft)

            ProductEditorEvent.SaveDraftClicked,
            ProductEditorEvent.ResubmitClicked,
            ProductEditorEvent.PublishClicked,
            ProductEditorEvent.DoneClicked,
            ProductEditorEvent.DraftSaved,
            is ProductEditorEvent.ImageUploadSucceeded,
            is ProductEditorEvent.ImageUploadFailed,
            ProductEditorEvent.ReviewApproved,
            is ProductEditorEvent.ReviewRejected,
            is ProductEditorEvent.PublishSucceeded,
            is ProductEditorEvent.PublishFailed -> invalid(
                reason = "Event is not valid after draft save.",
            )
        }
    }

    private fun ProductEditorTransitionScope.reduceImageUploadInProgress(
        event: ProductEditorEvent,
    ): ProductEditorTransition {
        return when (event) {
            is ProductEditorEvent.ImageUploadSucceeded -> transitionTo(
                ProductEditorPhase.ReviewSubmissionInProgress(
                    uploadToken = event.uploadToken,
                ),
            )

            is ProductEditorEvent.ImageUploadFailed -> transitionTo(ProductEditorPhase.EditingDraft)

            is ProductEditorEvent.TitleChanged,
            is ProductEditorEvent.DescriptionChanged,
            is ProductEditorEvent.PriceChanged,
            ProductEditorEvent.SaveDraftClicked,
            ProductEditorEvent.ContinueEditingClicked,
            ProductEditorEvent.SubmitClicked,
            ProductEditorEvent.ResubmitClicked,
            ProductEditorEvent.PublishClicked,
            ProductEditorEvent.DoneClicked,
            ProductEditorEvent.DraftSaved,
            ProductEditorEvent.ReviewApproved,
            is ProductEditorEvent.ReviewRejected,
            is ProductEditorEvent.PublishSucceeded,
            is ProductEditorEvent.PublishFailed -> ignore(
                reason = "Image upload command is running.",
            )
        }
    }

    private fun ProductEditorTransitionScope.reduceReviewSubmissionInProgress(
        event: ProductEditorEvent,
    ): ProductEditorTransition {
        return when (event) {
            ProductEditorEvent.ReviewApproved -> transitionTo(ProductEditorPhase.Approved)

            is ProductEditorEvent.ReviewRejected -> transitionTo(
                ProductEditorPhase.Rejected(reason = event.reason),
            )

            is ProductEditorEvent.TitleChanged,
            is ProductEditorEvent.DescriptionChanged,
            is ProductEditorEvent.PriceChanged,
            ProductEditorEvent.SaveDraftClicked,
            ProductEditorEvent.ContinueEditingClicked,
            ProductEditorEvent.SubmitClicked,
            ProductEditorEvent.ResubmitClicked,
            ProductEditorEvent.PublishClicked,
            ProductEditorEvent.DoneClicked,
            ProductEditorEvent.DraftSaved,
            is ProductEditorEvent.ImageUploadSucceeded,
            is ProductEditorEvent.ImageUploadFailed,
            is ProductEditorEvent.PublishSucceeded,
            is ProductEditorEvent.PublishFailed -> ignore(
                reason = "Review submission command is running.",
            )
        }
    }

    private fun ProductEditorTransitionScope.reduceRejected(
        phase: ProductEditorPhase.Rejected,
        event: ProductEditorEvent,
    ): ProductEditorTransition {
        return when (event) {
            is ProductEditorEvent.TitleChanged,
            is ProductEditorEvent.DescriptionChanged,
            is ProductEditorEvent.PriceChanged -> transitionTo(phase)

            ProductEditorEvent.ResubmitClicked -> submitDraft(
                invalidTarget = phase,
            )

            ProductEditorEvent.ContinueEditingClicked -> transitionTo(ProductEditorPhase.EditingDraft)

            ProductEditorEvent.SaveDraftClicked,
            ProductEditorEvent.SubmitClicked,
            ProductEditorEvent.PublishClicked,
            ProductEditorEvent.DoneClicked,
            ProductEditorEvent.DraftSaved,
            is ProductEditorEvent.ImageUploadSucceeded,
            is ProductEditorEvent.ImageUploadFailed,
            ProductEditorEvent.ReviewApproved,
            is ProductEditorEvent.ReviewRejected,
            is ProductEditorEvent.PublishSucceeded,
            is ProductEditorEvent.PublishFailed -> invalid(
                reason = "Event is not valid while review is rejected.",
            )
        }
    }

    private fun ProductEditorTransitionScope.reduceApproved(
        event: ProductEditorEvent,
    ): ProductEditorTransition {
        return when (event) {
            ProductEditorEvent.PublishClicked -> transitionTo(ProductEditorPhase.PublishInProgress)

            ProductEditorEvent.ContinueEditingClicked -> transitionTo(ProductEditorPhase.EditingDraft)

            is ProductEditorEvent.TitleChanged,
            is ProductEditorEvent.DescriptionChanged,
            is ProductEditorEvent.PriceChanged,
            ProductEditorEvent.SaveDraftClicked,
            ProductEditorEvent.SubmitClicked,
            ProductEditorEvent.ResubmitClicked,
            ProductEditorEvent.DoneClicked,
            ProductEditorEvent.DraftSaved,
            is ProductEditorEvent.ImageUploadSucceeded,
            is ProductEditorEvent.ImageUploadFailed,
            ProductEditorEvent.ReviewApproved,
            is ProductEditorEvent.ReviewRejected,
            is ProductEditorEvent.PublishSucceeded,
            is ProductEditorEvent.PublishFailed -> invalid(
                reason = "Event is not valid after approval.",
            )
        }
    }

    private fun ProductEditorTransitionScope.reducePublishInProgress(
        event: ProductEditorEvent,
    ): ProductEditorTransition {
        return when (event) {
            is ProductEditorEvent.PublishSucceeded -> transitionTo(
                ProductEditorPhase.Published(
                    productId = event.productId,
                    title = state.context.draft.form.title.trim(),
                ),
            )

            is ProductEditorEvent.PublishFailed -> transitionTo(ProductEditorPhase.Approved)

            is ProductEditorEvent.TitleChanged,
            is ProductEditorEvent.DescriptionChanged,
            is ProductEditorEvent.PriceChanged,
            ProductEditorEvent.SaveDraftClicked,
            ProductEditorEvent.ContinueEditingClicked,
            ProductEditorEvent.SubmitClicked,
            ProductEditorEvent.ResubmitClicked,
            ProductEditorEvent.PublishClicked,
            ProductEditorEvent.DoneClicked,
            ProductEditorEvent.DraftSaved,
            is ProductEditorEvent.ImageUploadSucceeded,
            is ProductEditorEvent.ImageUploadFailed,
            ProductEditorEvent.ReviewApproved,
            is ProductEditorEvent.ReviewRejected -> ignore(
                reason = "Publish command is running.",
            )
        }
    }

    private fun ProductEditorTransitionScope.reducePublished(
        event: ProductEditorEvent,
    ): ProductEditorTransition {
        return when (event) {
            ProductEditorEvent.DoneClicked -> stay(
                effects = listOf(ProductEditorEffect.CloseEditor),
            )

            is ProductEditorEvent.TitleChanged,
            is ProductEditorEvent.DescriptionChanged,
            is ProductEditorEvent.PriceChanged,
            ProductEditorEvent.SaveDraftClicked,
            ProductEditorEvent.ContinueEditingClicked,
            ProductEditorEvent.SubmitClicked,
            ProductEditorEvent.ResubmitClicked,
            ProductEditorEvent.PublishClicked,
            ProductEditorEvent.DraftSaved,
            is ProductEditorEvent.ImageUploadSucceeded,
            is ProductEditorEvent.ImageUploadFailed,
            ProductEditorEvent.ReviewApproved,
            is ProductEditorEvent.ReviewRejected,
            is ProductEditorEvent.PublishSucceeded,
            is ProductEditorEvent.PublishFailed -> ignore(
                reason = "Product is already published.",
            )
        }
    }

    private fun ProductEditorTransitionScope.submitDraft(
        invalidTarget: ProductEditorPhase,
    ): ProductEditorTransition {
        return if (state.context.draft.form.validationError() == null) {
            transitionTo(ProductEditorPhase.ImageUploadInProgress)
        } else {
            transitionTo(invalidTarget)
        }
    }
}

private class ProductEditorPhaseEntryPolicy :
    AfsmPhaseEntryPolicy<
        ProductEditorPhase,
        ProductEditorContext,
        ProductEditorEvent,
        ProductEditorCommand,
        ProductEditorEffect,
        > {
    override fun enter(
        from: ProductEditorPhase,
        target: ProductEditorPhase,
        event: ProductEditorEvent,
        context: ProductEditorContext,
    ): AfsmPhaseEntry<ProductEditorContext, ProductEditorCommand, ProductEditorEffect> {
        return when (target) {
            ProductEditorPhase.EditingDraft -> enterEditingDraft(event, context)

            ProductEditorPhase.SavingDraft -> AfsmPhaseEntry(
                context = context.copy(errorMessage = null),
                commands = listOf(ProductEditorCommand.SaveDraft(context.draft)),
            )

            ProductEditorPhase.DraftSaved -> AfsmPhaseEntry(
                context = context.copy(errorMessage = null),
            )

            ProductEditorPhase.ImageUploadInProgress -> {
                val draft = context.draft.normalized()

                AfsmPhaseEntry(
                    context = context.copy(
                        draft = draft,
                        errorMessage = null,
                    ),
                    commands = listOf(ProductEditorCommand.StartImageUpload(draft)),
                )
            }

            is ProductEditorPhase.ReviewSubmissionInProgress -> {
                val draft = context.draft.copy(
                    reviewAttempt = context.draft.reviewAttempt + 1,
                )

                AfsmPhaseEntry(
                    context = context.copy(
                        draft = draft,
                        errorMessage = null,
                    ),
                    commands = listOf(
                        ProductEditorCommand.StartReviewSubmission(
                            draft = draft,
                            uploadToken = target.uploadToken,
                        ),
                    ),
                )
            }

            is ProductEditorPhase.Rejected -> enterRejected(event, context)

            ProductEditorPhase.Approved -> AfsmPhaseEntry(
                context = context.copy(errorMessage = null),
            )

            ProductEditorPhase.PublishInProgress -> AfsmPhaseEntry(
                context = context.copy(errorMessage = null),
                commands = listOf(ProductEditorCommand.StartProductPublish(context.draft)),
            )

            is ProductEditorPhase.Published -> AfsmPhaseEntry(
                context = context.copy(errorMessage = null),
            )
        }
    }

    private fun enterEditingDraft(
        event: ProductEditorEvent,
        context: ProductEditorContext,
    ): AfsmPhaseEntry<ProductEditorContext, ProductEditorCommand, ProductEditorEffect> {
        return AfsmPhaseEntry(
            context = context.copy(
                draft = context.draft.updateForm(event),
                errorMessage = errorMessageFor(event, context),
            ),
        )
    }

    private fun enterRejected(
        event: ProductEditorEvent,
        context: ProductEditorContext,
    ): AfsmPhaseEntry<ProductEditorContext, ProductEditorCommand, ProductEditorEffect> {
        return AfsmPhaseEntry(
            context = context.copy(
                draft = context.draft.updateForm(event),
                errorMessage = errorMessageFor(event, context),
            ),
        )
    }

    private fun errorMessageFor(
        event: ProductEditorEvent,
        context: ProductEditorContext,
    ): String? {
        return when (event) {
            is ProductEditorEvent.ImageUploadFailed -> event.message
            ProductEditorEvent.SubmitClicked,
            ProductEditorEvent.ResubmitClicked -> context.draft.form.validationError()

            else -> null
        }
    }
}

private fun ProductDraft.updateForm(
    event: ProductEditorEvent,
): ProductDraft {
    return when (event) {
        is ProductEditorEvent.TitleChanged -> updateForm { form ->
            form.copy(title = event.value)
        }

        is ProductEditorEvent.DescriptionChanged -> updateForm { form ->
            form.copy(description = event.value)
        }

        is ProductEditorEvent.PriceChanged -> updateForm { form ->
            form.copy(priceText = event.value)
        }

        else -> this
    }
}

private fun ProductDraft.updateForm(
    update: (ProductDraftForm) -> ProductDraftForm,
): ProductDraft {
    return copy(form = update(form))
}

private fun ProductDraft.normalized(): ProductDraft {
    return copy(
        form = form.copy(
            title = form.title.trim(),
            description = form.description.trim(),
            priceText = form.priceText.trim(),
        ),
    )
}

private fun ProductDraftForm.validationError(): String? {
    val priceCents = priceCentsOrNull()
    return when {
        title.isBlank() -> "Title is required."
        description.trim().length < 10 -> "Description must be at least 10 characters."
        priceCents == null || priceCents <= 0 -> "Enter a valid price."
        else -> null
    }
}
