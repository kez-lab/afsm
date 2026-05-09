package afsm.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AfsmExecutableDslCompileCheckTest {
    @Test
    fun `executable DSL compiles and runs ProductEditor style transitions`() {
        val machine = productEditorMachine()

        val result = machine.transition(
            state = AfsmChartState(
                phase = DslProductEditorPhase.EditingDraft,
                context = DslProductEditorContext(
                    draft = DslProductDraft(
                        title = "  Travel Mug  ",
                        description = "  Leakproof mug for commuting.  ",
                        priceText = "24.50",
                    ),
                ),
            ),
            event = DslProductEditorEvent.SubmitClicked,
        )

        val normalizedDraft = DslProductDraft(
            title = "Travel Mug",
            description = "Leakproof mug for commuting.",
            priceText = "24.50",
        )

        assertEquals(DslProductEditorPhase.ImageUploadInProgress, result.state.phase)
        assertEquals(normalizedDraft, result.state.context.draft)
        assertEquals(listOf(DslProductEditorAction.StartImageUpload(normalizedDraft)), result.commands)
        assertIs<AfsmDecision.Transitioned>(result.decision)
    }

    @Test
    fun `typed phase scope exposes phase payload to entry actions`() {
        val machine = productEditorMachine()
        val draft = DslProductDraft(
            title = "Travel Mug",
            description = "Leakproof mug for commuting.",
            priceText = "24.50",
        )

        val result = machine.transition(
            state = AfsmChartState(
                phase = DslProductEditorPhase.ImageUploadInProgress,
                context = DslProductEditorContext(draft = draft),
            ),
            event = DslProductEditorEvent.ImageUploadSucceeded(uploadToken = "upload-1"),
        )

        val reviewedDraft = draft.copy(reviewAttempt = 1)

        assertEquals(
            DslProductEditorPhase.ReviewSubmissionInProgress(uploadToken = "upload-1"),
            result.state.phase,
        )
        assertEquals(reviewedDraft, result.state.context.draft)
        assertEquals(
            listOf(
                DslProductEditorAction.StartReviewSubmission(
                    draft = reviewedDraft,
                    uploadToken = "upload-1",
                ),
            ),
            result.commands,
        )
    }

    @Test
    fun `guard otherwise branch can update context without leaving phase`() {
        val machine = productEditorMachine()

        val result = machine.transition(
            state = AfsmChartState(
                phase = DslProductEditorPhase.EditingDraft,
                context = DslProductEditorContext(
                    draft = DslProductDraft(
                        title = "Travel Mug",
                        description = "short",
                        priceText = "24.50",
                    ),
                ),
            ),
            event = DslProductEditorEvent.SubmitClicked,
        )

        assertEquals(DslProductEditorPhase.EditingDraft, result.state.phase)
        assertEquals("Description must be at least 10 characters.", result.state.context.errorMessage)
        assertEquals(emptyList(), result.commands)
        assertIs<AfsmDecision.Stayed>(result.decision)
    }

    @Test
    fun `effect can be emitted without changing phase`() {
        val machine = productEditorMachine()
        val phase = DslProductEditorPhase.Published(
            productId = 10,
            title = "Travel Mug",
        )

        val result = machine.transition(
            state = AfsmChartState(
                phase = phase,
                context = DslProductEditorContext(),
            ),
            event = DslProductEditorEvent.DoneClicked,
        )

        assertEquals(phase, result.state.phase)
        assertEquals(listOf(DslProductEditorEffect.CloseEditor), result.effects)
        assertIs<AfsmDecision.Stayed>(result.decision)
    }

    @Test
    fun `ignore and invalid branches preserve decisions without topology edges`() {
        val machine = afsmStateChart<
            DslProductEditorPhase,
            DslProductEditorContext,
            DslProductEditorEvent,
            DslProductEditorAction,
            DslProductEditorEffect,
            > {
            initial(
                phase = DslProductEditorPhase.EditingDraft,
                context = DslProductEditorContext(),
            )

            state(DslProductEditorPhase.EditingDraft) {
                on<DslProductEditorEvent.SaveDraftClicked> {
                    ignore(reason = "Draft save is disabled.")
                }

                on<DslProductEditorEvent.DoneClicked> {
                    invalid(reason = "Editor cannot close before publish.")
                }
            }
        }

        val ignored = machine.transition(
            state = machine.initialState,
            event = DslProductEditorEvent.SaveDraftClicked,
        )
        val invalid = machine.transition(
            state = machine.initialState,
            event = DslProductEditorEvent.DoneClicked,
        )

        assertEquals(AfsmDecision.Ignored("Draft save is disabled."), ignored.decision)
        assertEquals(AfsmDecision.Invalid("Editor cannot close before publish."), invalid.decision)
        assertEquals(emptyList(), machine.topology.transitions)
    }

    @Test
    fun `topology deduplicates identical declared edges`() {
        val machine = afsmStateChart<
            DslProductEditorPhase,
            DslProductEditorContext,
            DslProductEditorEvent,
            DslProductEditorAction,
            DslProductEditorEffect,
            > {
            initial(
                phase = DslProductEditorPhase.EditingDraft,
                context = DslProductEditorContext(),
            )

            state(DslProductEditorPhase.EditingDraft) {
                on<DslProductEditorEvent.SubmitClicked> {
                    transitionTo(
                        phase = DslProductEditorPhase.ImageUploadInProgress,
                        guard = { context.draft.title.isNotBlank() },
                    )

                    transitionTo(
                        phase = DslProductEditorPhase.ImageUploadInProgress,
                        guard = { context.draft.description.isNotBlank() },
                    )
                }
            }
        }

        assertEquals(
            listOf(
                AfsmTopologyTransition(
                    from = "EditingDraft",
                    event = "SubmitClicked",
                    to = "ImageUploadInProgress",
                ),
            ),
            machine.topology.transitions,
        )
    }

    @Test
    fun `topology can be exported without sample events`() {
        val machine = productEditorMachine()

        assertEquals(
            listOf(
                AfsmTopologyTransition(
                    from = "EditingDraft",
                    event = "TitleChanged",
                    to = "EditingDraft",
                ),
                AfsmTopologyTransition(
                    from = "EditingDraft",
                    event = "SaveDraftClicked",
                    to = "SavingDraft",
                ),
                AfsmTopologyTransition(
                    from = "EditingDraft",
                    event = "SubmitClicked",
                    to = "ImageUploadInProgress",
                ),
                AfsmTopologyTransition(
                    from = "EditingDraft",
                    event = "SubmitClicked [otherwise]",
                    to = "EditingDraft",
                ),
                AfsmTopologyTransition(
                    from = "SavingDraft",
                    event = "DraftSaved",
                    to = "DraftSaved",
                ),
                AfsmTopologyTransition(
                    from = "ImageUploadInProgress",
                    event = "ImageUploadSucceeded",
                    to = "ReviewSubmissionInProgress",
                ),
                AfsmTopologyTransition(
                    from = "Published",
                    event = "DoneClicked",
                    to = "Published",
                ),
            ),
            machine.topology.transitions,
        )

        val mmd = machine.topology.toMmd()

        assertTrue("EditingDraft --> ImageUploadInProgress: SubmitClicked" in mmd)
        assertTrue("ImageUploadInProgress --> ReviewSubmissionInProgress: ImageUploadSucceeded" in mmd)
    }

    private fun productEditorMachine(): AfsmStateChart<
        DslProductEditorPhase,
        DslProductEditorContext,
        DslProductEditorEvent,
        DslProductEditorAction,
        DslProductEditorEffect,
        > {
        return afsmStateChart {
            initial(
                phase = DslProductEditorPhase.EditingDraft,
                context = DslProductEditorContext(),
            )

            state(DslProductEditorPhase.EditingDraft) {
                on<DslProductEditorEvent.TitleChanged> {
                    stay {
                        updateContext {
                            copy(
                                draft = draft.withTitle(event.value),
                                errorMessage = null,
                            )
                        }
                    }
                }

                on<DslProductEditorEvent.SaveDraftClicked> {
                    transitionTo(DslProductEditorPhase.SavingDraft)
                }

                on<DslProductEditorEvent.SubmitClicked> {
                    transitionTo(
                        phase = DslProductEditorPhase.ImageUploadInProgress,
                        guard = { context.draft.validationMessage() == null },
                    ) {
                        updateContext {
                            copy(
                                draft = draft.normalized(),
                                errorMessage = null,
                            )
                        }
                    }

                    otherwise {
                        updateContext {
                            copy(errorMessage = draft.validationMessage())
                        }
                    }
                }
            }

            state(DslProductEditorPhase.SavingDraft) {
                onEnter {
                    action(DslProductEditorAction.SaveDraft(context.draft))
                }

                on<DslProductEditorEvent.DraftSaved> {
                    transitionTo(DslProductEditorPhase.DraftSaved)
                }
            }

            state(DslProductEditorPhase.ImageUploadInProgress) {
                onEnter {
                    action(DslProductEditorAction.StartImageUpload(context.draft))
                }

                on<DslProductEditorEvent.ImageUploadSucceeded> {
                    transitionTo<DslProductEditorPhase.ReviewSubmissionInProgress>(
                        phase = {
                            DslProductEditorPhase.ReviewSubmissionInProgress(
                                uploadToken = event.uploadToken,
                            )
                        },
                    ) {
                        updateContext {
                            copy(
                                draft = draft.copy(reviewAttempt = draft.reviewAttempt + 1),
                                errorMessage = null,
                            )
                        }
                    }
                }
            }

            state<DslProductEditorPhase.ReviewSubmissionInProgress> {
                onEnter {
                    action(
                        DslProductEditorAction.StartReviewSubmission(
                            draft = context.draft,
                            uploadToken = phase.uploadToken,
                        ),
                    )
                }
            }

            state<DslProductEditorPhase.Published> {
                on<DslProductEditorEvent.DoneClicked> {
                    stay {
                        effect(DslProductEditorEffect.CloseEditor)
                    }
                }
            }
        }
    }
}

private sealed interface DslProductEditorPhase {
    data object EditingDraft : DslProductEditorPhase
    data object SavingDraft : DslProductEditorPhase
    data object DraftSaved : DslProductEditorPhase
    data object ImageUploadInProgress : DslProductEditorPhase

    data class ReviewSubmissionInProgress(
        val uploadToken: String,
    ) : DslProductEditorPhase

    data class Published(
        val productId: Long,
        val title: String,
    ) : DslProductEditorPhase
}

private data class DslProductEditorContext(
    val draft: DslProductDraft = DslProductDraft(),
    val errorMessage: String? = null,
)

private data class DslProductDraft(
    val title: String = "",
    val description: String = "",
    val priceText: String = "",
    val reviewAttempt: Int = 0,
) {
    fun withTitle(value: String): DslProductDraft {
        return copy(title = value)
    }

    fun normalized(): DslProductDraft {
        return copy(
            title = title.trim(),
            description = description.trim(),
            priceText = priceText.trim(),
        )
    }

    fun validationMessage(): String? {
        return when {
            title.isBlank() -> "Title is required."
            description.trim().length < 10 -> "Description must be at least 10 characters."
            priceText.toBigDecimalOrNull() == null -> "Enter a valid price."
            else -> null
        }
    }
}

private sealed interface DslProductEditorEvent {
    data class TitleChanged(val value: String) : DslProductEditorEvent
    data object SaveDraftClicked : DslProductEditorEvent
    data object DraftSaved : DslProductEditorEvent
    data object SubmitClicked : DslProductEditorEvent
    data class ImageUploadSucceeded(val uploadToken: String) : DslProductEditorEvent
    data object DoneClicked : DslProductEditorEvent
}

private sealed interface DslProductEditorAction {
    data class SaveDraft(val draft: DslProductDraft) : DslProductEditorAction
    data class StartImageUpload(val draft: DslProductDraft) : DslProductEditorAction

    data class StartReviewSubmission(
        val draft: DslProductDraft,
        val uploadToken: String,
    ) : DslProductEditorAction
}

private sealed interface DslProductEditorEffect {
    data object CloseEditor : DslProductEditorEffect
}
