package afsm.sample.shop.feature.editor

import afsm.core.AfsmEventBuilder
import afsm.core.AfsmMachine
import afsm.core.AfsmSnapshot
import afsm.core.AfsmStateMachine
import afsm.core.AfsmTopology
import afsm.core.afsmMachine

class ProductEditorStateMachine(
    private val machine: AfsmMachine<
        ProductEditorPhase,
        ProductEditorContext,
        ProductEditorEvent,
        ProductEditorCommand,
        ProductEditorEffect,
        > = productEditorMachine(),
) : AfsmStateMachine<ProductEditorState, ProductEditorEvent, ProductEditorCommand, ProductEditorEffect> {
    val topology: AfsmTopology
        get() = machine.topology

    override fun transition(
        state: ProductEditorState,
        event: ProductEditorEvent,
    ): ProductEditorTransition {
        val transition = machine.transition(
            snapshot = AfsmSnapshot(
                phase = state.phase,
                context = state.context,
            ),
            event = event,
        )

        return ProductEditorTransition(
            state = ProductEditorState(
                phase = transition.state.phase,
                context = transition.state.context,
            ),
            commands = transition.commands,
            effects = transition.effects,
            decision = transition.decision,
        )
    }
}

private fun productEditorMachine(): AfsmMachine<
    ProductEditorPhase,
    ProductEditorContext,
    ProductEditorEvent,
    ProductEditorCommand,
    ProductEditorEffect,
    > {
    return afsmMachine {
        initial(
            phase = ProductEditorPhase.EditingDraft,
            context = ProductEditorContext(),
        )

        state(ProductEditorPhase.EditingDraft) {
            on<ProductEditorEvent.TitleChanged> {
                stay { assign { updateDraft(event) } }
            }

            on<ProductEditorEvent.DescriptionChanged> {
                stay { assign { updateDraft(event) } }
            }

            on<ProductEditorEvent.PriceChanged> {
                stay { assign { updateDraft(event) } }
            }

            on<ProductEditorEvent.SaveDraftClicked> {
                transitionTo(ProductEditorPhase.SavingDraft)
            }

            on<ProductEditorEvent.SubmitClicked> {
                submitDraft(
                    validTarget = ProductEditorPhase.ImageUploadInProgress,
                )

                otherwise {
                    assign { withValidationError() }
                }
            }
        }

        state(ProductEditorPhase.SavingDraft) {
            onEnter {
                assign { copy(errorMessage = null) }
                action(ProductEditorCommand.SaveDraft(context.draft))
            }

            on<ProductEditorEvent.DraftSaved> {
                transitionTo(ProductEditorPhase.DraftSaved)
            }
        }

        state(ProductEditorPhase.DraftSaved) {
            onEnter {
                assign { copy(errorMessage = null) }
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
                    assign { updateDraft(event) }
                }
            }

            on<ProductEditorEvent.DescriptionChanged> {
                transitionTo(ProductEditorPhase.EditingDraft) {
                    assign { updateDraft(event) }
                }
            }

            on<ProductEditorEvent.PriceChanged> {
                transitionTo(ProductEditorPhase.EditingDraft) {
                    assign { updateDraft(event) }
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
                    assign {
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
                    assign {
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
                    assign { copy(errorMessage = null) }
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
                    assign { copy(errorMessage = null) }
                }
            }
        }

        state<ProductEditorPhase.Rejected> {
            on<ProductEditorEvent.TitleChanged> {
                stay { assign { updateDraft(event) } }
            }

            on<ProductEditorEvent.DescriptionChanged> {
                stay { assign { updateDraft(event) } }
            }

            on<ProductEditorEvent.PriceChanged> {
                stay { assign { updateDraft(event) } }
            }

            on<ProductEditorEvent.ResubmitClicked> {
                submitDraft(
                    validTarget = ProductEditorPhase.ImageUploadInProgress,
                )

                otherwise {
                    assign { withValidationError() }
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
                assign { copy(errorMessage = null) }
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
                    assign { copy(errorMessage = null) }
                }
            }

            on<ProductEditorEvent.PublishFailed> {
                transitionTo(ProductEditorPhase.Approved) {
                    assign {
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

private fun <PS : ProductEditorPhase, EV : ProductEditorEvent> AfsmEventBuilder<
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
        assign {
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
            assign { withValidationError() }
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
