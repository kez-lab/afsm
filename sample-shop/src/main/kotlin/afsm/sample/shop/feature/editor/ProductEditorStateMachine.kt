package afsm.sample.shop.feature.editor

import afsm.core.AfsmGraph
import afsm.core.AfsmMachine
import afsm.core.afsmMachine

@AfsmGraph(
    id = "ProductEditor",
    fileName = "ProductEditorStateMachine.mmd",
)
internal val productEditorStateMachine:
    AfsmMachine<ProductEditorState, ProductEditorEvent, ProductEditorCommand, ProductEditorEffect> =
    afsmMachine {
        initial(
            phase = ProductEditorPhase.EditingDraft,
            data = ProductEditorData(),
        )

        phase(ProductEditorPhase.EditingDraft) {
            on<ProductEditorEvent.TitleChanged> {
                updateData { data, event -> data.updateDraft(event) }
            }

            on<ProductEditorEvent.DescriptionChanged> {
                updateData { data, event -> data.updateDraft(event) }
            }

            on<ProductEditorEvent.PriceChanged> {
                updateData { data, event -> data.updateDraft(event) }
            }

            on<ProductEditorEvent.SaveDraftClicked> {
                transitionTo(ProductEditorPhase.SavingDraft)
            }

            on<ProductEditorEvent.SubmitClicked> {
                case(
                    label = "valid draft",
                    condition = { data.canStartReviewSubmission() },
                ) {
                    updateData { normalizeDraftForSubmit() }
                    transitionTo(ProductEditorPhase.ImageUploadInProgress)
                }

                case(
                    label = "invalid draft",
                    condition = { data.hasReviewSubmissionError() },
                ) {
                    updateData { withReviewSubmissionError() }
                }
            }
        }

        phase(ProductEditorPhase.SavingDraft) {
            onEnter {
                updateData { copy(errorMessage = null) }
                command(label = "SaveDraft") {
                    ProductEditorCommand.SaveDraft(data.draft)
                }
            }

            on<ProductEditorEvent.DraftSaveCompleted> {
                transitionTo(ProductEditorPhase.DraftSaved)
            }
        }

        phase(ProductEditorPhase.DraftSaved) {
            onEnter {
                updateData { copy(errorMessage = null) }
            }

            on<ProductEditorEvent.ContinueEditingClicked> {
                transitionTo(ProductEditorPhase.EditingDraft)
            }

            on<ProductEditorEvent.SubmitClicked> {
                case(
                    label = "valid draft",
                    condition = { data.canStartReviewSubmission() },
                ) {
                    updateData { normalizeDraftForSubmit() }
                    transitionTo(ProductEditorPhase.ImageUploadInProgress)
                }

                case(
                    label = "invalid draft",
                    condition = { data.hasReviewSubmissionError() },
                ) {
                    updateData { withReviewSubmissionError() }
                }
            }

            on<ProductEditorEvent.TitleChanged> {
                case {
                    updateData { data, event -> data.updateDraft(event) }
                    transitionTo(ProductEditorPhase.EditingDraft)
                }
            }

            on<ProductEditorEvent.DescriptionChanged> {
                case {
                    updateData { data, event -> data.updateDraft(event) }
                    transitionTo(ProductEditorPhase.EditingDraft)
                }
            }

            on<ProductEditorEvent.PriceChanged> {
                case {
                    updateData { data, event -> data.updateDraft(event) }
                    transitionTo(ProductEditorPhase.EditingDraft)
                }
            }
        }

        phase(ProductEditorPhase.ImageUploadInProgress) {
            onEnter {
                command(label = "StartImageUpload") {
                    ProductEditorCommand.StartImageUpload(data.draft)
                }
            }

            on<ProductEditorEvent.ImageUploadSucceeded> {
                case {
                    updateData {
                        copy(
                            draft = draft.copy(
                                reviewAttempt = draft.reviewAttempt + 1,
                            ),
                            errorMessage = null,
                        )
                    }
                    transitionTo<ProductEditorPhase.ReviewSubmissionInProgress> {
                        ProductEditorPhase.ReviewSubmissionInProgress(
                            uploadToken = event.uploadToken,
                        )
                    }
                }
            }

            on<ProductEditorEvent.ImageUploadFailed> {
                case {
                    updateData { data, event ->
                        data.copy(errorMessage = event.message)
                    }
                    transitionTo(ProductEditorPhase.EditingDraft)
                }
            }
        }

        phase<ProductEditorPhase.ReviewSubmissionInProgress> {
            onEnter {
                command(label = "StartReviewSubmission") {
                    ProductEditorCommand.StartReviewSubmission(
                        draft = data.draft,
                        uploadToken = phase.uploadToken,
                    )
                }
            }

            on<ProductEditorEvent.ReviewApproved> {
                case {
                    updateData { copy(errorMessage = null) }
                    transitionTo(ProductEditorPhase.Approved)
                }
            }

            on<ProductEditorEvent.ReviewRejected> {
                case {
                    updateData { copy(errorMessage = null) }
                    transitionTo<ProductEditorPhase.Rejected> {
                        ProductEditorPhase.Rejected(
                            reason = event.reason,
                        )
                    }
                }
            }
        }

        phase<ProductEditorPhase.Rejected> {
            on<ProductEditorEvent.TitleChanged> {
                updateData { data, event -> data.updateDraft(event) }
            }

            on<ProductEditorEvent.DescriptionChanged> {
                updateData { data, event -> data.updateDraft(event) }
            }

            on<ProductEditorEvent.PriceChanged> {
                updateData { data, event -> data.updateDraft(event) }
            }

            on<ProductEditorEvent.ResubmitClicked> {
                case(
                    label = "valid draft",
                    condition = { data.canStartReviewSubmission() },
                ) {
                    updateData { normalizeDraftForSubmit() }
                    transitionTo(ProductEditorPhase.ImageUploadInProgress)
                }

                case(
                    label = "invalid draft",
                    condition = { data.hasReviewSubmissionError() },
                ) {
                    updateData { withReviewSubmissionError() }
                }
            }

            on<ProductEditorEvent.ContinueEditingClicked> {
                transitionTo(ProductEditorPhase.EditingDraft)
            }
        }

        phase(ProductEditorPhase.Approved) {
            on<ProductEditorEvent.PublishClicked> {
                transitionTo(ProductEditorPhase.PublishInProgress)
            }

            on<ProductEditorEvent.ContinueEditingClicked> {
                transitionTo(ProductEditorPhase.EditingDraft)
            }
        }

        phase(ProductEditorPhase.PublishInProgress) {
            onEnter {
                updateData { copy(errorMessage = null) }
                command(label = "StartProductPublish") {
                    ProductEditorCommand.StartProductPublish(data.draft)
                }
            }

            on<ProductEditorEvent.PublishSucceeded> {
                case {
                    updateData { copy(errorMessage = null) }
                    transitionTo<ProductEditorPhase.Published> {
                        ProductEditorPhase.Published(
                            productId = event.productId,
                            title = data.draft.form.title.trim(),
                        )
                    }
                }
            }

            on<ProductEditorEvent.PublishFailed> {
                case {
                    updateData { data, event ->
                        data.copy(errorMessage = event.message)
                    }
                    transitionTo(ProductEditorPhase.Approved)
                }
            }
        }

        phase<ProductEditorPhase.Published> {
            on<ProductEditorEvent.DoneClicked> {
                effect(label = "CloseEditor") { ProductEditorEffect.CloseEditor }
            }
        }
}

private fun ProductEditorData.updateDraft(
    event: ProductEditorEvent,
): ProductEditorData {
    return copy(
        draft = draft.updateForm(event),
        errorMessage = null,
    )
}

private fun ProductEditorData.normalizeDraftForSubmit(): ProductEditorData {
    return copy(
        draft = draft.normalized(),
        errorMessage = null,
    )
}

private fun ProductEditorData.withReviewSubmissionError(): ProductEditorData {
    return copy(errorMessage = draft.form.validationError())
}

private fun ProductEditorData.canStartReviewSubmission(): Boolean {
    return draft.form.validationError() == null
}

private fun ProductEditorData.hasReviewSubmissionError(): Boolean {
    return !canStartReviewSubmission()
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
