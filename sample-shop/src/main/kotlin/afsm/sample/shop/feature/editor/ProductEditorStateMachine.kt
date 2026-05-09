package afsm.sample.shop.feature.editor

import afsm.core.AfsmEventBranchScope
import afsm.core.AfsmGraph
import afsm.core.AfsmStateChart
import afsm.core.afsmStateChart

private typealias ProductEditorChart = AfsmStateChart<
    ProductEditorPhase,
    ProductEditorContext,
    ProductEditorEvent,
    ProductEditorCommand,
    ProductEditorEffect,
    >

@AfsmGraph(
    id = "ProductEditor",
    fileName = "ProductEditorStateMachine.mmd",
)
internal class ProductEditorStateMachine(
    chart: ProductEditorChart = productEditorChart(),
) : ProductEditorChart by chart

private fun productEditorChart(): ProductEditorChart {
    return afsmStateChart {
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
                submitDraft(
                    validTarget = ProductEditorPhase.ImageUploadInProgress,
                )

                otherwise {
                    updateContext { withValidationError() }
                }
            }
        }

        state(ProductEditorPhase.SavingDraft) {
            onEnter {
                updateContext { copy(errorMessage = null) }
                action(ProductEditorCommand.SaveDraft(context.draft))
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
                submitDraft(
                    validTarget = ProductEditorPhase.ImageUploadInProgress,
                    invalidTarget = ProductEditorPhase.EditingDraft,
                )
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
            onEnter {
                action(ProductEditorCommand.StartImageUpload(context.draft))
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
            onEnter {
                action(
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
                submitDraft(
                    validTarget = ProductEditorPhase.ImageUploadInProgress,
                )

                otherwise {
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
            onEnter {
                updateContext { copy(errorMessage = null) }
                action(ProductEditorCommand.StartProductPublish(context.draft))
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
                stay {
                    effect(ProductEditorEffect.CloseEditor)
                }
            }
        }
    }
}

private fun <PS : ProductEditorPhase, EV : ProductEditorEvent> AfsmEventBranchScope<
    ProductEditorPhase,
    ProductEditorContext,
    ProductEditorEvent,
    ProductEditorCommand,
    ProductEditorEffect,
    PS,
    EV,
    >.submitDraft(
    validTarget: ProductEditorPhase,
    invalidTarget: ProductEditorPhase? = null,
) {
    transitionTo(
        phase = validTarget,
        guard = { context.draft.form.validationError() == null },
    ) {
        updateContext {
            copy(
                draft = draft.normalized(),
                errorMessage = null,
            )
        }
    }

    invalidTarget?.let { target ->
        transitionTo(
            phase = target,
            guard = { context.draft.form.validationError() != null },
        ) {
            updateContext { withValidationError() }
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
