package afsm.sample.shop.feature.editor

import afsm.core.Afsm
import afsm.core.AfsmStateMachine

class ProductEditorStateMachine :
    AfsmStateMachine<ProductEditorState, ProductEditorEvent, ProductEditorCommand, ProductEditorEffect> {
    override fun transition(
        state: ProductEditorState,
        event: ProductEditorEvent,
    ): ProductEditorTransition {
        return when (state) {
            is ProductEditorState.EditingDraft -> reduceEditing(state, event)
            is ProductEditorState.SavingDraft -> reduceSaving(state, event)
            is ProductEditorState.DraftSaved -> reduceDraftSaved(state, event)
            is ProductEditorState.ImageUploadInProgress -> reduceImageUploadInProgress(state, event)
            is ProductEditorState.ReviewSubmissionInProgress -> reduceReviewSubmissionInProgress(state, event)
            is ProductEditorState.Rejected -> reduceRejected(state, event)
            is ProductEditorState.Approved -> reduceApproved(state, event)
            is ProductEditorState.PublishInProgress -> reducePublishInProgress(state, event)
            is ProductEditorState.Published -> reducePublished(state, event)
        }
    }

    private fun reduceEditing(
        state: ProductEditorState.EditingDraft,
        event: ProductEditorEvent,
    ): ProductEditorTransition {
        return when (event) {
            is ProductEditorEvent.TitleChanged -> Afsm.transitionTo(
                state.copy(
                    draft = state.draft.updateForm { form -> form.copy(title = event.value) },
                    errorMessage = null,
                ),
            )

            is ProductEditorEvent.DescriptionChanged -> Afsm.transitionTo(
                state.copy(
                    draft = state.draft.updateForm { form -> form.copy(description = event.value) },
                    errorMessage = null,
                ),
            )

            is ProductEditorEvent.PriceChanged -> Afsm.transitionTo(
                state.copy(
                    draft = state.draft.updateForm { form -> form.copy(priceText = event.value) },
                    errorMessage = null,
                ),
            )

            ProductEditorEvent.SaveDraftClicked -> Afsm.transitionTo(
                state = ProductEditorState.SavingDraft(state.draft),
                commands = listOf(ProductEditorCommand.SaveDraft(state.draft)),
            )

            ProductEditorEvent.SubmitClicked -> startUpload(state.draft, state)

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
            is ProductEditorEvent.PublishFailed -> Afsm.invalid(
                state,
                reason = "Event is not valid while editing a draft.",
            )
        }
    }

    private fun reduceSaving(
        state: ProductEditorState.SavingDraft,
        event: ProductEditorEvent,
    ): ProductEditorTransition {
        return when (event) {
            ProductEditorEvent.DraftSaved -> Afsm.transitionTo(ProductEditorState.DraftSaved(state.draft))

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
            is ProductEditorEvent.PublishFailed -> Afsm.ignore(
                state,
                reason = "Draft save command is running.",
            )
        }
    }

    private fun reduceDraftSaved(
        state: ProductEditorState.DraftSaved,
        event: ProductEditorEvent,
    ): ProductEditorTransition {
        return when (event) {
            ProductEditorEvent.ContinueEditingClicked -> Afsm.transitionTo(
                ProductEditorState.EditingDraft(state.draft),
            )

            ProductEditorEvent.SubmitClicked -> startUpload(state.draft, state)

            is ProductEditorEvent.TitleChanged -> Afsm.transitionTo(
                ProductEditorState.EditingDraft(
                    draft = state.draft.updateForm { form -> form.copy(title = event.value) },
                ),
            )

            is ProductEditorEvent.DescriptionChanged -> Afsm.transitionTo(
                ProductEditorState.EditingDraft(
                    draft = state.draft.updateForm { form -> form.copy(description = event.value) },
                ),
            )

            is ProductEditorEvent.PriceChanged -> Afsm.transitionTo(
                ProductEditorState.EditingDraft(
                    draft = state.draft.updateForm { form -> form.copy(priceText = event.value) },
                ),
            )

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
            is ProductEditorEvent.PublishFailed -> Afsm.invalid(
                state,
                reason = "Event is not valid after draft save.",
            )
        }
    }

    private fun reduceImageUploadInProgress(
        state: ProductEditorState.ImageUploadInProgress,
        event: ProductEditorEvent,
    ): ProductEditorTransition {
        return when (event) {
            is ProductEditorEvent.ImageUploadSucceeded -> {
                val reviewedDraft = state.draft.copy(reviewAttempt = state.draft.reviewAttempt + 1)
                Afsm.transitionTo(
                    state = ProductEditorState.ReviewSubmissionInProgress(
                        draft = reviewedDraft,
                        uploadToken = event.uploadToken,
                    ),
                    commands = listOf(
                        ProductEditorCommand.StartReviewSubmission(
                            draft = reviewedDraft,
                            uploadToken = event.uploadToken,
                        ),
                    ),
                )
            }

            is ProductEditorEvent.ImageUploadFailed -> Afsm.transitionTo(
                ProductEditorState.EditingDraft(
                    draft = state.draft,
                    errorMessage = event.message,
                ),
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
            ProductEditorEvent.ReviewApproved,
            is ProductEditorEvent.ReviewRejected,
            is ProductEditorEvent.PublishSucceeded,
            is ProductEditorEvent.PublishFailed -> Afsm.ignore(
                state,
                reason = "Image upload command is running.",
            )
        }
    }

    private fun reduceReviewSubmissionInProgress(
        state: ProductEditorState.ReviewSubmissionInProgress,
        event: ProductEditorEvent,
    ): ProductEditorTransition {
        return when (event) {
            ProductEditorEvent.ReviewApproved -> Afsm.transitionTo(ProductEditorState.Approved(state.draft))

            is ProductEditorEvent.ReviewRejected -> Afsm.transitionTo(
                ProductEditorState.Rejected(
                    draft = state.draft,
                    reason = event.reason,
                ),
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
            is ProductEditorEvent.PublishFailed -> Afsm.ignore(
                state,
                reason = "Review submission command is running.",
            )
        }
    }

    private fun reduceRejected(
        state: ProductEditorState.Rejected,
        event: ProductEditorEvent,
    ): ProductEditorTransition {
        return when (event) {
            is ProductEditorEvent.TitleChanged -> Afsm.transitionTo(
                state.copy(
                    draft = state.draft.updateForm { form -> form.copy(title = event.value) },
                    errorMessage = null,
                ),
            )

            is ProductEditorEvent.DescriptionChanged -> Afsm.transitionTo(
                state.copy(
                    draft = state.draft.updateForm { form -> form.copy(description = event.value) },
                    errorMessage = null,
                ),
            )

            is ProductEditorEvent.PriceChanged -> Afsm.transitionTo(
                state.copy(
                    draft = state.draft.updateForm { form -> form.copy(priceText = event.value) },
                    errorMessage = null,
                ),
            )

            ProductEditorEvent.ResubmitClicked -> startUpload(state.draft, state)

            ProductEditorEvent.ContinueEditingClicked -> Afsm.transitionTo(
                ProductEditorState.EditingDraft(state.draft),
            )

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
            is ProductEditorEvent.PublishFailed -> Afsm.invalid(
                state,
                reason = "Event is not valid while review is rejected.",
            )
        }
    }

    private fun reduceApproved(
        state: ProductEditorState.Approved,
        event: ProductEditorEvent,
    ): ProductEditorTransition {
        return when (event) {
            ProductEditorEvent.PublishClicked -> Afsm.transitionTo(
                state = ProductEditorState.PublishInProgress(state.draft),
                commands = listOf(ProductEditorCommand.StartProductPublish(state.draft)),
            )

            ProductEditorEvent.ContinueEditingClicked -> Afsm.transitionTo(
                ProductEditorState.EditingDraft(state.draft),
            )

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
            is ProductEditorEvent.PublishFailed -> Afsm.invalid(
                state,
                reason = "Event is not valid after approval.",
            )
        }
    }

    private fun reducePublishInProgress(
        state: ProductEditorState.PublishInProgress,
        event: ProductEditorEvent,
    ): ProductEditorTransition {
        return when (event) {
            is ProductEditorEvent.PublishSucceeded -> Afsm.transitionTo(
                ProductEditorState.Published(
                    productId = event.productId,
                    title = state.draft.form.title.trim(),
                ),
            )

            is ProductEditorEvent.PublishFailed -> Afsm.transitionTo(
                ProductEditorState.Approved(state.draft),
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
            ProductEditorEvent.ReviewApproved,
            is ProductEditorEvent.ReviewRejected -> Afsm.ignore(
                state,
                reason = "Publish command is running.",
            )
        }
    }

    private fun reducePublished(
        state: ProductEditorState.Published,
        event: ProductEditorEvent,
    ): ProductEditorTransition {
        return when (event) {
            ProductEditorEvent.DoneClicked -> Afsm.stay(
                state = state,
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
            is ProductEditorEvent.PublishFailed -> Afsm.ignore(
                state,
                reason = "Product is already published.",
            )
        }
    }

    private fun startUpload(
        draft: ProductDraft,
        currentState: ProductEditorState,
    ): ProductEditorTransition {
        val validationError = draft.form.validationError()
        if (validationError != null) {
            return when (currentState) {
                is ProductEditorState.EditingDraft -> Afsm.stay(
                    currentState.copy(errorMessage = validationError),
                )

                is ProductEditorState.Rejected -> Afsm.stay(
                    currentState.copy(errorMessage = validationError),
                )

                else -> Afsm.invalid(currentState, reason = validationError)
            }
        }

        return Afsm.transitionTo(
            state = ProductEditorState.ImageUploadInProgress(draft.normalized()),
            commands = listOf(ProductEditorCommand.StartImageUpload(draft.normalized())),
        )
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
}
