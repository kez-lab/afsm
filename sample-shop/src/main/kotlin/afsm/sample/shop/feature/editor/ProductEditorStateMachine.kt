package afsm.sample.shop.feature.editor

import afsm.core.AfsmGraph
import afsm.core.AfsmMachine
import afsm.core.afsmMachine

private typealias ProductEditorMachine =
    AfsmMachine<ProductEditorState, ProductEditorEvent, ProductEditorCommand, ProductEditorEffect>

@AfsmGraph(
    id = "ProductEditor",
    fileName = "ProductEditorStateMachine.mmd",
)
internal object ProductEditorStateMachine : ProductEditorMachine by productEditorMachine()

private fun productEditorMachine(): ProductEditorMachine {
    return afsmMachine {
        initial(
            phase = ProductEditorPhase.EditingDraft,
            context = ProductEditorContext(),
        )

        state(ProductEditorPhase.EditingDraft) {
            on<ProductEditorEvent.TitleChanged> {
                stay { updateContext { updateDraft(event) } }
            }

            on<ProductEditorEvent.DescriptionChanged> {
                stay { updateContext { updateDraft(event) } }
            }

            on<ProductEditorEvent.PriceChanged> {
                stay { updateContext { updateDraft(event) } }
            }

            on<ProductEditorEvent.SaveDraftClicked> {
                transitionTo(ProductEditorPhase.SavingDraft)
            }

            on<ProductEditorEvent.SubmitClicked> {
                transitionTo(
                    phase = ProductEditorPhase.ImageUploadInProgress,
                    guardLabel = "valid draft",
                    guard = { context.draft.form.validationError() == null },
                ) {
                    updateContext { normalizeDraftForSubmit() }
                }

                otherwise(label = "invalid draft") {
                    updateContext { withValidationError() }
                }
            }
        }

        state(ProductEditorPhase.SavingDraft) {
            onEnter(commandLabels = listOf("SaveDraft")) {
                updateContext { copy(errorMessage = null) }
                command(ProductEditorCommand.SaveDraft(context.draft))
            }

            on<ProductEditorEvent.DraftSaved> {
                transitionTo(ProductEditorPhase.DraftSaved)
            }
        }

        state(ProductEditorPhase.DraftSaved) {
            onEnter {
                updateContext { copy(errorMessage = null) }
            }

            on<ProductEditorEvent.ContinueEditingClicked> {
                transitionTo(ProductEditorPhase.EditingDraft)
            }

            on<ProductEditorEvent.SubmitClicked> {
                transitionTo(
                    phase = ProductEditorPhase.ImageUploadInProgress,
                    guardLabel = "valid draft",
                    guard = { context.draft.form.validationError() == null },
                ) {
                    updateContext { normalizeDraftForSubmit() }
                }

                otherwise(label = "invalid draft") {
                    updateContext { withValidationError() }
                }
            }

            on<ProductEditorEvent.TitleChanged> {
                transitionTo(ProductEditorPhase.EditingDraft) {
                    updateContext { updateDraft(event) }
                }
            }

            on<ProductEditorEvent.DescriptionChanged> {
                transitionTo(ProductEditorPhase.EditingDraft) {
                    updateContext { updateDraft(event) }
                }
            }

            on<ProductEditorEvent.PriceChanged> {
                transitionTo(ProductEditorPhase.EditingDraft) {
                    updateContext { updateDraft(event) }
                }
            }
        }

        state(ProductEditorPhase.ImageUploadInProgress) {
            onEnter(commandLabels = listOf("StartImageUpload")) {
                command(ProductEditorCommand.StartImageUpload(context.draft))
            }

            on<ProductEditorEvent.ImageUploadSucceeded> {
                transitionTo<ProductEditorPhase.ReviewSubmissionInProgress>(
                    phase = {
                        ProductEditorPhase.ReviewSubmissionInProgress(
                            uploadToken = event.uploadToken,
                        )
                    },
                ) {
                    updateContext {
                        copy(
                            draft = draft.copy(
                                reviewAttempt = draft.reviewAttempt + 1,
                            ),
                            errorMessage = null,
                        )
                    }
                }
            }

            on<ProductEditorEvent.ImageUploadFailed> {
                transitionTo(ProductEditorPhase.EditingDraft) {
                    updateContext {
                        copy(errorMessage = event.message)
                    }
                }
            }
        }

        state<ProductEditorPhase.ReviewSubmissionInProgress> {
            onEnter(commandLabels = listOf("StartReviewSubmission")) {
                command(
                    ProductEditorCommand.StartReviewSubmission(
                        draft = context.draft,
                        uploadToken = phase.uploadToken,
                    ),
                )
            }

            on<ProductEditorEvent.ReviewApproved> {
                transitionTo(ProductEditorPhase.Approved) {
                    updateContext { copy(errorMessage = null) }
                }
            }

            on<ProductEditorEvent.ReviewRejected> {
                transitionTo<ProductEditorPhase.Rejected>(
                    phase = {
                        ProductEditorPhase.Rejected(
                            reason = event.reason,
                        )
                    },
                ) {
                    updateContext { copy(errorMessage = null) }
                }
            }
        }

        state<ProductEditorPhase.Rejected> {
            on<ProductEditorEvent.TitleChanged> {
                stay { updateContext { updateDraft(event) } }
            }

            on<ProductEditorEvent.DescriptionChanged> {
                stay { updateContext { updateDraft(event) } }
            }

            on<ProductEditorEvent.PriceChanged> {
                stay { updateContext { updateDraft(event) } }
            }

            on<ProductEditorEvent.ResubmitClicked> {
                transitionTo(
                    phase = ProductEditorPhase.ImageUploadInProgress,
                    guardLabel = "valid draft",
                    guard = { context.draft.form.validationError() == null },
                ) {
                    updateContext { normalizeDraftForSubmit() }
                }

                otherwise(label = "invalid draft") {
                    updateContext { withValidationError() }
                }
            }

            on<ProductEditorEvent.ContinueEditingClicked> {
                transitionTo(ProductEditorPhase.EditingDraft)
            }
        }

        state(ProductEditorPhase.Approved) {
            on<ProductEditorEvent.PublishClicked> {
                transitionTo(ProductEditorPhase.PublishInProgress)
            }

            on<ProductEditorEvent.ContinueEditingClicked> {
                transitionTo(ProductEditorPhase.EditingDraft)
            }
        }

        state(ProductEditorPhase.PublishInProgress) {
            onEnter(commandLabels = listOf("StartProductPublish")) {
                updateContext { copy(errorMessage = null) }
                command(ProductEditorCommand.StartProductPublish(context.draft))
            }

            on<ProductEditorEvent.PublishSucceeded> {
                transitionTo<ProductEditorPhase.Published>(
                    phase = {
                        ProductEditorPhase.Published(
                            productId = event.productId,
                            title = context.draft.form.title.trim(),
                        )
                    },
                ) {
                    updateContext { copy(errorMessage = null) }
                }
            }

            on<ProductEditorEvent.PublishFailed> {
                transitionTo(ProductEditorPhase.Approved) {
                    updateContext {
                        copy(errorMessage = event.message)
                    }
                }
            }
        }

        state<ProductEditorPhase.Published> {
            on<ProductEditorEvent.DoneClicked> {
                stay(effectLabels = listOf("CloseEditor")) {
                    effect(ProductEditorEffect.CloseEditor)
                }
            }
        }
    }
}

private fun ProductEditorContext.updateDraft(
    event: ProductEditorEvent,
): ProductEditorContext {
    return copy(
        draft = draft.updateForm(event),
        errorMessage = null,
    )
}

private fun ProductEditorContext.normalizeDraftForSubmit(): ProductEditorContext {
    return copy(
        draft = draft.normalized(),
        errorMessage = null,
    )
}

private fun ProductEditorContext.withValidationError(): ProductEditorContext {
    return copy(errorMessage = draft.form.validationError())
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
