package afsm.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AfsmExecutableDslCompileCheckTest {
    @Test
    fun `executable DSL compiles and runs ProductEditor style transitions`() {
        val machine = productEditorMachine()

        val result = machine.transition(
            state = AfsmState(
                phase = DslProductEditorPhase.EditingDraft,
                data = DslProductEditorData(
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
        assertEquals(normalizedDraft, result.state.data.draft)
        assertEquals(listOf(DslProductEditorCommand.StartImageUpload(normalizedDraft)), result.commands)
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
            state = AfsmState(
                phase = DslProductEditorPhase.ImageUploadInProgress,
                data = DslProductEditorData(draft = draft),
            ),
            event = DslProductEditorEvent.ImageUploadSucceeded(uploadToken = "upload-1"),
        )

        val reviewedDraft = draft.copy(reviewAttempt = 1)

        assertEquals(
            DslProductEditorPhase.ReviewSubmissionInProgress(uploadToken = "upload-1"),
            result.state.phase,
        )
        assertEquals(reviewedDraft, result.state.data.draft)
        assertEquals(
            listOf(
                DslProductEditorCommand.StartReviewSubmission(
                    draft = reviewedDraft,
                    uploadToken = "upload-1",
                ),
            ),
            result.commands,
        )
    }

    @Test
    fun `failed named case can update data without leaving phase`() {
        val machine = productEditorMachine()

        val result = machine.transition(
            state = AfsmState(
                phase = DslProductEditorPhase.EditingDraft,
                data = DslProductEditorData(
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
        assertEquals("Description must be at least 10 characters.", result.state.data.errorMessage)
        assertEquals(emptyList(), result.commands)
        assertIs<AfsmDecision.Handled>(result.decision)
    }

    @Test
    fun `named cases separate conditions phase changes and data updates`() {
        val machine = productEditorMachine()

        val transitions = machine.topology.transitions

        assertTrue(
            AfsmTopologyTransition(
                from = "EditingDraft",
                event = "SubmitClicked",
                to = "ImageUploadInProgress",
                conditionLabel = "valid draft",
            ) in transitions,
        )
        assertTrue(
            AfsmTopologyTransition(
                from = "EditingDraft",
                event = "SubmitClicked",
                to = "EditingDraft",
                conditionLabel = "invalid draft",
                kind = AfsmTopologyTransitionKind.Internal,
            ) in transitions,
        )
    }

    @Test
    fun `top-level shorthand branches are alternatives not merged actions`() {
        val machine = afsmMachine<
            DslProductEditorPhase,
            DslProductEditorData,
            DslProductEditorEvent,
            DslProductEditorCommand,
            DslProductEditorEffect,
            > {
            initial(
                phase = DslProductEditorPhase.EditingDraft,
                data = DslProductEditorData(),
            )

            phase(DslProductEditorPhase.EditingDraft) {
                on<DslProductEditorEvent.SubmitClicked> {
                    updateData { copy(errorMessage = "handled first") }
                    transitionTo(DslProductEditorPhase.SavingDraft)
                }
            }

            phase(DslProductEditorPhase.SavingDraft)
        }

        val result = machine.transition(
            state = machine.initialState,
            event = DslProductEditorEvent.SubmitClicked,
        )

        assertEquals(DslProductEditorPhase.EditingDraft, result.state.phase)
        assertEquals("handled first", result.state.data.errorMessage)
        assertEquals(emptyList(), result.commands)
        assertIs<AfsmDecision.Handled>(result.decision)
    }

    @Test
    fun `effect can be emitted without changing phase`() {
        val machine = productEditorMachine()
        val phase = DslProductEditorPhase.Published(
            productId = 10,
            title = "Travel Mug",
        )

        val result = machine.transition(
            state = AfsmState(
                phase = phase,
                data = DslProductEditorData(),
            ),
            event = DslProductEditorEvent.DoneClicked,
        )

        assertEquals(phase, result.state.phase)
        assertEquals(listOf(DslProductEditorEffect.CloseEditor), result.effects)
        assertIs<AfsmDecision.Handled>(result.decision)
    }

    @Test
    fun `onExit runs before transition block and onEnter`() {
        val machine = afsmMachine<
            DslProductEditorPhase,
            String,
            DslProductEditorEvent,
            DslProductEditorCommand,
            DslProductEditorEffect,
            > {
            initial(
                phase = DslProductEditorPhase.EditingDraft,
                data = "",
            )

            phase(DslProductEditorPhase.EditingDraft) {
                onExit {
                    updateData { this + "exit;" }
                }

                on<DslProductEditorEvent.SubmitClicked> {
                    case {
                        updateData { this + "transition;" }
                        transitionTo(DslProductEditorPhase.SavingDraft)
                    }
                }
            }

            phase(DslProductEditorPhase.SavingDraft) {
                onEnter {
                    updateData { this + "enter;" }
                }
            }
        }

        val result = machine.transition(
            state = machine.initialState,
            event = DslProductEditorEvent.SubmitClicked,
        )

        assertEquals("exit;transition;enter;", result.state.data)
    }

    @Test
    fun `payload phase factory observes data after exit and case actions`() {
        val machine = afsmMachine<
            DslProductEditorPhase,
            String,
            DslProductEditorEvent,
            DslProductEditorCommand,
            DslProductEditorEffect,
            > {
            initial(
                phase = DslProductEditorPhase.EditingDraft,
                data = "",
            )

            phase(DslProductEditorPhase.EditingDraft) {
                onExit {
                    updateData { this + "exit;" }
                }

                on<DslProductEditorEvent.ImageUploadSucceeded> {
                    case {
                        updateData { this + "case;" }
                        transitionTo<DslProductEditorPhase.ReviewSubmissionInProgress> {
                            DslProductEditorPhase.ReviewSubmissionInProgress(
                                uploadToken = data,
                            )
                        }
                    }
                }
            }

            phase<DslProductEditorPhase.ReviewSubmissionInProgress> {
                onEnter {
                    updateData { data, phase ->
                        data + "enter:${phase.uploadToken};"
                    }
                }
            }
        }

        val result = machine.transition(
            state = machine.initialState,
            event = DslProductEditorEvent.ImageUploadSucceeded(uploadToken = "ignored"),
        )

        assertEquals(
            DslProductEditorPhase.ReviewSubmissionInProgress(uploadToken = "exit;case;"),
            result.state.phase,
        )
        assertEquals("exit;case;enter:exit;case;;", result.state.data)
    }

    @Test
    fun `machine build validates initial state and transition targets`() {
        val missingInitial = assertFailsWith<AfsmDefinitionException> {
            afsmMachine<
                DslProductEditorPhase,
                DslProductEditorData,
                DslProductEditorEvent,
                DslProductEditorCommand,
                DslProductEditorEffect,
                > {
                initial(
                    phase = DslProductEditorPhase.EditingDraft,
                    data = DslProductEditorData(),
                )

                phase(DslProductEditorPhase.SavingDraft) {
                }
            }
        }

        val unknownTarget = assertFailsWith<AfsmDefinitionException> {
            afsmMachine<
                DslProductEditorPhase,
                DslProductEditorData,
                DslProductEditorEvent,
                DslProductEditorCommand,
                DslProductEditorEffect,
                > {
                initial(
                    phase = DslProductEditorPhase.EditingDraft,
                    data = DslProductEditorData(),
                )

                phase(DslProductEditorPhase.EditingDraft) {
                    on<DslProductEditorEvent.SubmitClicked> {
                        transitionTo(DslProductEditorPhase.ImageUploadInProgress)
                    }
                }
            }
        }

        assertTrue("Initial phase EditingDraft" in missingInitial.message.orEmpty())
        assertTrue("targets an undeclared phase" in unknownTarget.message.orEmpty())
    }

    @Test
    fun `ignore and invalid branches preserve decisions without topology edges`() {
        val machine = afsmMachine<
            DslProductEditorPhase,
            DslProductEditorData,
            DslProductEditorEvent,
            DslProductEditorCommand,
            DslProductEditorEffect,
            > {
            initial(
                phase = DslProductEditorPhase.EditingDraft,
                data = DslProductEditorData(),
            )

            phase(DslProductEditorPhase.EditingDraft) {
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
        val machine = afsmMachine<
            DslProductEditorPhase,
            DslProductEditorData,
            DslProductEditorEvent,
            DslProductEditorCommand,
            DslProductEditorEffect,
            > {
            initial(
                phase = DslProductEditorPhase.EditingDraft,
                data = DslProductEditorData(),
            )

            phase(DslProductEditorPhase.EditingDraft) {
                on<DslProductEditorEvent.SubmitClicked> {
                    case(condition = { data.draft.title.isNotBlank() }) {
                        transitionTo(DslProductEditorPhase.ImageUploadInProgress)
                    }

                    case(condition = { data.draft.description.isNotBlank() }) {
                        transitionTo(DslProductEditorPhase.ImageUploadInProgress)
                    }
                }
            }

            phase(DslProductEditorPhase.ImageUploadInProgress)
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
                    kind = AfsmTopologyTransitionKind.Internal,
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
                    conditionLabel = "valid draft",
                ),
                AfsmTopologyTransition(
                    from = "EditingDraft",
                    event = "SubmitClicked",
                    to = "EditingDraft",
                    conditionLabel = "invalid draft",
                    kind = AfsmTopologyTransitionKind.Internal,
                ),
                AfsmTopologyTransition(
                    from = "SavingDraft",
                    event = "DraftSaveCompleted",
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
                    effectLabels = listOf("CloseEditor"),
                    kind = AfsmTopologyTransitionKind.Internal,
                ),
            ),
            machine.topology.transitions,
        )

        val mmd = machine.topology.toMmd()

        assertTrue("EditingDraft --> ImageUploadInProgress: SubmitClicked" in mmd)
        assertTrue("ImageUploadInProgress --> ReviewSubmissionInProgress: ImageUploadSucceeded" in mmd)
    }

    private fun productEditorMachine(): AfsmMachine<
        AfsmState<DslProductEditorPhase, DslProductEditorData>,
        DslProductEditorEvent,
        DslProductEditorCommand,
        DslProductEditorEffect,
        > {
        return afsmMachine {
            initial(
                phase = DslProductEditorPhase.EditingDraft,
                data = DslProductEditorData(),
            )

            phase(DslProductEditorPhase.EditingDraft) {
                on<DslProductEditorEvent.TitleChanged> {
                    updateData { data, event ->
                        data.copy(
                            draft = data.draft.withTitle(event.value),
                            errorMessage = null,
                        )
                    }
                }

                on<DslProductEditorEvent.SaveDraftClicked> {
                    transitionTo(DslProductEditorPhase.SavingDraft)
                }

                on<DslProductEditorEvent.SubmitClicked> {
                    case(
                        label = "valid draft",
                        condition = { data.draft.validationMessage() == null },
                    ) {
                        updateData {
                            copy(
                                draft = draft.normalized(),
                                errorMessage = null,
                            )
                        }
                        transitionTo(DslProductEditorPhase.ImageUploadInProgress)
                    }

                    case(
                        label = "invalid draft",
                        condition = { data.draft.validationMessage() != null },
                    ) {
                        updateData {
                            copy(errorMessage = draft.validationMessage())
                        }
                    }
                }
            }

            phase(DslProductEditorPhase.SavingDraft) {
                onEnter {
                    command(label = "SaveDraft") {
                        DslProductEditorCommand.SaveDraft(data.draft)
                    }
                }

                on<DslProductEditorEvent.DraftSaveCompleted> {
                    transitionTo(DslProductEditorPhase.DraftSaved)
                }
            }

            phase(DslProductEditorPhase.DraftSaved)

            phase(DslProductEditorPhase.ImageUploadInProgress) {
                onEnter {
                    command(label = "StartImageUpload") {
                        DslProductEditorCommand.StartImageUpload(data.draft)
                    }
                }

                on<DslProductEditorEvent.ImageUploadSucceeded> {
                    case {
                        updateData {
                            copy(
                                draft = draft.copy(reviewAttempt = draft.reviewAttempt + 1),
                                errorMessage = null,
                            )
                        }
                        transitionTo<DslProductEditorPhase.ReviewSubmissionInProgress> {
                            DslProductEditorPhase.ReviewSubmissionInProgress(
                                uploadToken = event.uploadToken,
                            )
                        }
                    }
                }
            }

            phase<DslProductEditorPhase.ReviewSubmissionInProgress> {
                onEnter {
                    command(label = "StartReviewSubmission") {
                        DslProductEditorCommand.StartReviewSubmission(
                            draft = data.draft,
                            uploadToken = phase.uploadToken,
                        )
                    }
                }
            }

            phase<DslProductEditorPhase.Published> {
                on<DslProductEditorEvent.DoneClicked> {
                    effect(label = "CloseEditor") { DslProductEditorEffect.CloseEditor }
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

private data class DslProductEditorData(
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
    data object DraftSaveCompleted : DslProductEditorEvent
    data object SubmitClicked : DslProductEditorEvent
    data class ImageUploadSucceeded(val uploadToken: String) : DslProductEditorEvent
    data object DoneClicked : DslProductEditorEvent
}

private sealed interface DslProductEditorCommand {
    data class SaveDraft(val draft: DslProductDraft) : DslProductEditorCommand
    data class StartImageUpload(val draft: DslProductDraft) : DslProductEditorCommand

    data class StartReviewSubmission(
        val draft: DslProductDraft,
        val uploadToken: String,
    ) : DslProductEditorCommand
}

private sealed interface DslProductEditorEffect {
    data object CloseEditor : DslProductEditorEffect
}
