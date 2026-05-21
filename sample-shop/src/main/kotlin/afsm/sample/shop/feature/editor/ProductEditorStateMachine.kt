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
                updateContext { context, event -> context.updateDraft(event) }
            }

            on<ProductEditorEvent.DescriptionChanged> {
                updateContext { context, event -> context.updateDraft(event) }
            }

            on<ProductEditorEvent.PriceChanged> {
                updateContext { context, event -> context.updateDraft(event) }
            }

            on<ProductEditorEvent.SaveDraftClicked> {
                transitionTo(ProductEditorPhase.SavingDraft)
            }

            on<ProductEditorEvent.SubmitClicked> {
                case(
                    label = "valid draft",
                    condition = { context.draft.form.validationError() == null },
                ) {
                    updateContext { normalizeDraftForSubmit() }
                    transitionTo(ProductEditorPhase.ImageUploadInProgress)
                }

                case(label = "invalid draft") {
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
                case(
                    label = "valid draft",
                    condition = { context.draft.form.validationError() == null },
                ) {
                    updateContext { normalizeDraftForSubmit() }
                    transitionTo(ProductEditorPhase.ImageUploadInProgress)
                }

                case(label = "invalid draft") {
                    updateContext { withValidationError() }
                }
            }

            on<ProductEditorEvent.TitleChanged> {
                case {
                    updateContext { context, event -> context.updateDraft(event) }
                    transitionTo(ProductEditorPhase.EditingDraft)
                }
            }

            on<ProductEditorEvent.DescriptionChanged> {
                case {
                    updateContext { context, event -> context.updateDraft(event) }
                    transitionTo(ProductEditorPhase.EditingDraft)
                }
            }

            on<ProductEditorEvent.PriceChanged> {
                case {
                    updateContext { context, event -> context.updateDraft(event) }
                    transitionTo(ProductEditorPhase.EditingDraft)
                }
            }
        }

        state(ProductEditorPhase.ImageUploadInProgress) {
            onEnter(commandLabels = listOf("StartImageUpload")) {
                command(ProductEditorCommand.StartImageUpload(context.draft))
            }

            on<ProductEditorEvent.ImageUploadSucceeded> {
                case {
                    updateContext {
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
                    updateContext { context, event ->
                        context.copy(errorMessage = event.message)
                    }
                    transitionTo(ProductEditorPhase.EditingDraft)
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
                case {
                    updateContext { copy(errorMessage = null) }
                    transitionTo(ProductEditorPhase.Approved)
                }
            }

            on<ProductEditorEvent.ReviewRejected> {
                case {
                    updateContext { copy(errorMessage = null) }
                    transitionTo<ProductEditorPhase.Rejected> {
                        ProductEditorPhase.Rejected(
                            reason = event.reason,
                        )
                    }
                }
            }
        }

        state<ProductEditorPhase.Rejected> {
            on<ProductEditorEvent.TitleChanged> {
                updateContext { context, event -> context.updateDraft(event) }
            }

            on<ProductEditorEvent.DescriptionChanged> {
                updateContext { context, event -> context.updateDraft(event) }
            }

            on<ProductEditorEvent.PriceChanged> {
                updateContext { context, event -> context.updateDraft(event) }
            }

            on<ProductEditorEvent.ResubmitClicked> {
                case(
                    label = "valid draft",
                    condition = { context.draft.form.validationError() == null },
                ) {
                    updateContext { normalizeDraftForSubmit() }
                    transitionTo(ProductEditorPhase.ImageUploadInProgress)
                }

                case(label = "invalid draft") {
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
                case {
                    updateContext { copy(errorMessage = null) }
                    transitionTo<ProductEditorPhase.Published> {
                        ProductEditorPhase.Published(
                            productId = event.productId,
                            title = context.draft.form.title.trim(),
                        )
                    }
                }
            }

            on<ProductEditorEvent.PublishFailed> {
                case {
                    updateContext { context, event ->
                        context.copy(errorMessage = event.message)
                    }
                    transitionTo(ProductEditorPhase.Approved)
                }
            }
        }

        state<ProductEditorPhase.Published> {
            on<ProductEditorEvent.DoneClicked> {
                effect(label = "CloseEditor") { ProductEditorEffect.CloseEditor }
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
