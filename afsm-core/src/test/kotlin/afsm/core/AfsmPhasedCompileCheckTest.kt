package afsm.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AfsmPhasedCompileCheckTest {
    @Test
    fun `updateContext with trailing lambda keeps phase and emits no outputs`() {
        val machine = ProductEditorStateMachine()
        val transition = machine.transition(
            state = ProductEditorState(
                context = ProductEditorContext(
                    draft = ProductDraft(title = "Old title", description = "mirrorless"),
                    errorMessage = "previous error",
                ),
            ),
            event = ProductEditorEvent.TitleChanged("New title"),
        )

        assertEquals(ProductEditorPhase.EditingDraft, transition.state.phase)
        assertEquals("New title", transition.state.context.draft.title)
        assertEquals("mirrorless", transition.state.context.draft.description)
        assertNull(transition.state.context.errorMessage)
        assertTrue(transition.commands.isEmpty())
        assertTrue(transition.effects.isEmpty())
        assertIs<AfsmDecision.Stayed>(transition.decision)
    }

    @Test
    fun `transitionTo phase applies entry policy and emits commands from the next context`() {
        val machine = ProductEditorStateMachine()
        val transition = machine.transition(
            state = ProductEditorState(
                context = ProductEditorContext(
                    draft = ProductDraft(title = "  Camera  ", description = "  mirrorless  "),
                    saveStatus = DraftSaveStatus.Saved,
                    errorMessage = "previous error",
                ),
            ),
            event = ProductEditorEvent.SubmitClicked,
        )

        assertEquals(ProductEditorPhase.ImageUploadInProgress, transition.state.phase)
        assertEquals("Camera", transition.state.context.draft.title)
        assertEquals("mirrorless", transition.state.context.draft.description)
        assertEquals(DraftSaveStatus.Idle, transition.state.context.saveStatus)
        assertNull(transition.state.context.errorMessage)
        assertEquals(
            listOf(
                ProductEditorCommand.StartImageUpload(
                    ProductDraft(title = "Camera", description = "mirrorless"),
                ),
            ),
            transition.commands,
        )
        assertTrue(transition.effects.isEmpty())
        assertIs<AfsmDecision.Transitioned>(transition.decision)
    }

    @Test
    fun `updateContext keeps the current phase and can emit secondary commands`() {
        val machine = ProductEditorStateMachine()
        val transition = machine.transition(
            state = ProductEditorState(
                context = ProductEditorContext(
                    draft = ProductDraft(title = "Camera", description = "mirrorless"),
                ),
            ),
            event = ProductEditorEvent.SaveDraftClicked,
        )

        assertEquals(ProductEditorPhase.EditingDraft, transition.state.phase)
        assertEquals(DraftSaveStatus.Saving, transition.state.context.saveStatus)
        assertEquals(
            listOf(ProductEditorCommand.SaveDraft(ProductDraft(title = "Camera", description = "mirrorless"))),
            transition.commands,
        )
        assertIs<AfsmDecision.Stayed>(transition.decision)
    }

    @Test
    fun `phase entry can use target phase values and triggering event`() {
        val machine = ProductEditorStateMachine()
        val transition = machine.transition(
            state = ProductEditorState(
                phase = ProductEditorPhase.ImageUploadInProgress,
                context = ProductEditorContext(
                    draft = ProductDraft(title = "Camera", description = "mirrorless", reviewAttempt = 1),
                ),
            ),
            event = ProductEditorEvent.ImageUploadSucceeded(uploadToken = "upload-1"),
        )

        assertEquals(
            ProductEditorPhase.ReviewSubmissionInProgress(uploadToken = "upload-1"),
            transition.state.phase,
        )
        assertEquals(2, transition.state.context.draft.reviewAttempt)
        assertEquals(
            listOf(
                ProductEditorCommand.StartReviewSubmission(
                    draft = ProductDraft(
                        title = "Camera",
                        description = "mirrorless",
                        reviewAttempt = 2,
                    ),
                    uploadToken = "upload-1",
                ),
            ),
            transition.commands,
        )
    }
}

private data class ProductEditorState(
    override val phase: ProductEditorPhase = ProductEditorPhase.EditingDraft,
    override val context: ProductEditorContext = ProductEditorContext(),
) : AfsmPhasedState<ProductEditorState, ProductEditorPhase, ProductEditorContext> {
    override fun with(
        phase: ProductEditorPhase,
        context: ProductEditorContext,
    ): ProductEditorState {
        return copy(
            phase = phase,
            context = context,
        )
    }
}

private sealed interface ProductEditorPhase {
    data object EditingDraft : ProductEditorPhase
    data object ImageUploadInProgress : ProductEditorPhase

    data class ReviewSubmissionInProgress(
        val uploadToken: String,
    ) : ProductEditorPhase
}

private data class ProductEditorContext(
    val draft: ProductDraft = ProductDraft(),
    val saveStatus: DraftSaveStatus = DraftSaveStatus.Idle,
    val errorMessage: String? = null,
)

private data class ProductDraft(
    val title: String = "",
    val description: String = "",
    val reviewAttempt: Int = 0,
) {
    fun normalized(): ProductDraft {
        return copy(
            title = title.trim(),
            description = description.trim(),
        )
    }
}

private enum class DraftSaveStatus {
    Idle,
    Saving,
    Saved,
}

private sealed interface ProductEditorEvent {
    data class TitleChanged(val value: String) : ProductEditorEvent
    data object SaveDraftClicked : ProductEditorEvent
    data object SubmitClicked : ProductEditorEvent
    data class ImageUploadSucceeded(val uploadToken: String) : ProductEditorEvent
}

private sealed interface ProductEditorCommand {
    data class SaveDraft(val draft: ProductDraft) : ProductEditorCommand
    data class StartImageUpload(val draft: ProductDraft) : ProductEditorCommand

    data class StartReviewSubmission(
        val draft: ProductDraft,
        val uploadToken: String,
    ) : ProductEditorCommand
}

private sealed interface ProductEditorEffect {
    data object CloseEditor : ProductEditorEffect
}

private typealias ProductEditorTransition =
    AfsmTransition<ProductEditorState, ProductEditorCommand, ProductEditorEffect>

private typealias ProductEditorTransitionScope =
    AfsmPhasedTransitionScope<
        ProductEditorState,
        ProductEditorPhase,
        ProductEditorContext,
        ProductEditorEvent,
        ProductEditorCommand,
        ProductEditorEffect,
        >

private class ProductEditorStateMachine :
    AfsmStateMachine<ProductEditorState, ProductEditorEvent, ProductEditorCommand, ProductEditorEffect> {
    private val entryPolicy = ProductEditorPhaseEntryPolicy()

    override fun transition(
        state: ProductEditorState,
        event: ProductEditorEvent,
    ): ProductEditorTransition {
        return Afsm.phased(
            state = state,
            event = event,
            entryPolicy = entryPolicy,
        ).run {
            when (state.phase) {
                ProductEditorPhase.EditingDraft -> reduceEditingDraft(event)
                ProductEditorPhase.ImageUploadInProgress -> reduceImageUploadInProgress(event)
                is ProductEditorPhase.ReviewSubmissionInProgress -> ignore("Review submission is running.")
            }
        }
    }

    private fun ProductEditorTransitionScope.reduceEditingDraft(
        event: ProductEditorEvent,
    ): ProductEditorTransition {
        return when (event) {
            is ProductEditorEvent.TitleChanged -> updateContext { context ->
                context.copy(
                    draft = context.draft.copy(title = event.value),
                    errorMessage = null,
                )
            }

            ProductEditorEvent.SaveDraftClicked -> updateContext(
                update = { context ->
                    context.copy(saveStatus = DraftSaveStatus.Saving)
                },
                commands = { context ->
                    listOf(ProductEditorCommand.SaveDraft(context.draft))
                },
            )

            ProductEditorEvent.SubmitClicked -> transitionTo(ProductEditorPhase.ImageUploadInProgress)
            is ProductEditorEvent.ImageUploadSucceeded -> invalid("Upload cannot succeed before submission.")
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

            is ProductEditorEvent.TitleChanged,
            ProductEditorEvent.SaveDraftClicked,
            ProductEditorEvent.SubmitClicked -> ignore("Image upload is running.")
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
            ProductEditorPhase.EditingDraft -> AfsmPhaseEntry(context)

            ProductEditorPhase.ImageUploadInProgress -> {
                val draft = context.draft.normalized()

                AfsmPhaseEntry(
                    context = context.copy(
                        draft = draft,
                        saveStatus = DraftSaveStatus.Idle,
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
                    context = context.copy(draft = draft),
                    commands = listOf(
                        ProductEditorCommand.StartReviewSubmission(
                            draft = draft,
                            uploadToken = target.uploadToken,
                        ),
                    ),
                )
            }
        }
    }
}
