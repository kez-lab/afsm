package afsm.sample.shop.feature.editor

import afsm.core.AfsmDecision
import afsm.core.AfsmTopologyTransition
import afsm.core.toMmd
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ProductEditorStateMachineTest {
    private val machine = ProductEditorStateMachine
    private val validDraft = ProductDraft(
        form = ProductDraftForm(
            title = "Travel Mug",
            description = "Leakproof mug for commuting.",
            priceText = "24.50",
        ),
    )

    @Test
    fun `title change updates draft context without changing editing phase`() {
        val state = productEditorState()

        val result = machine.transition(
            state = state,
            event = ProductEditorEvent.TitleChanged("Travel Mug"),
        )

        assertEquals(ProductEditorPhase.EditingDraft, result.state.phase)
        assertEquals("Travel Mug", result.state.context.draft.form.title)
    }

    @Test
    fun `save draft transitions to saving phase and phase entry emits save command`() {
        val state = productEditorState(
            context = ProductEditorContext(draft = validDraft),
        )

        val result = machine.transition(state, ProductEditorEvent.SaveDraftClicked)

        assertEquals(ProductEditorPhase.SavingDraft, result.state.phase)
        assertEquals(validDraft, result.state.context.draft)
        assertEquals(listOf(ProductEditorCommand.SaveDraft(validDraft)), result.commands)
    }

    @Test
    fun `draft saved result transitions to draft saved phase without carrying draft in phase`() {
        val state = productEditorState(
            phase = ProductEditorPhase.SavingDraft,
            context = ProductEditorContext(draft = validDraft),
        )

        val result = machine.transition(state, ProductEditorEvent.DraftSaveCompleted)

        assertEquals(ProductEditorPhase.DraftSaved, result.state.phase)
        assertEquals(validDraft, result.state.context.draft)
    }

    @Test
    fun `editing after draft saved returns to editing phase and updates context draft`() {
        val state = productEditorState(
            phase = ProductEditorPhase.DraftSaved,
            context = ProductEditorContext(draft = validDraft),
        )

        val result = machine.transition(
            state = state,
            event = ProductEditorEvent.PriceChanged("25.00"),
        )

        assertEquals(ProductEditorPhase.EditingDraft, result.state.phase)
        assertEquals("25.00", result.state.context.draft.form.priceText)
    }

    @Test
    fun `submit from editing transitions only by phase and starts image upload from context draft`() {
        val state = productEditorState(
            context = ProductEditorContext(draft = validDraft),
        )

        val result = machine.transition(state, ProductEditorEvent.SubmitClicked)

        assertEquals(ProductEditorPhase.ImageUploadInProgress, result.state.phase)
        assertEquals(validDraft, result.state.context.draft)
        assertEquals(listOf(ProductEditorCommand.StartImageUpload(validDraft)), result.commands)
    }

    @Test
    fun `invalid draft keeps editing phase with review submission error in context`() {
        val state = productEditorState(
            context = ProductEditorContext(
                draft = validDraft.copy(
                    form = validDraft.form.copy(description = "short"),
                ),
            ),
        )

        val result = machine.transition(state, ProductEditorEvent.SubmitClicked)

        assertEquals(ProductEditorPhase.EditingDraft, result.state.phase)
        assertEquals(
            "Description must be at least 10 characters.",
            result.state.context.errorMessage,
        )
        assertEquals(emptyList(), result.commands)
    }

    @Test
    fun `invalid submit from saved draft stays saved and records validation error`() {
        val state = productEditorState(
            phase = ProductEditorPhase.DraftSaved,
            context = ProductEditorContext(
                draft = validDraft.copy(
                    form = validDraft.form.copy(description = "short"),
                ),
            ),
        )

        val result = machine.transition(state, ProductEditorEvent.SubmitClicked)

        assertEquals(ProductEditorPhase.DraftSaved, result.state.phase)
        assertEquals(
            "Description must be at least 10 characters.",
            result.state.context.errorMessage,
        )
        assertEquals(emptyList(), result.commands)
    }

    @Test
    fun `image upload failure returns to editing phase with error in context`() {
        val state = productEditorState(
            phase = ProductEditorPhase.ImageUploadInProgress,
            context = ProductEditorContext(draft = validDraft),
        )

        val result = machine.transition(
            state = state,
            event = ProductEditorEvent.ImageUploadFailed("Upload failed."),
        )

        assertEquals(ProductEditorPhase.EditingDraft, result.state.phase)
        assertEquals("Upload failed.", result.state.context.errorMessage)
        assertEquals(validDraft, result.state.context.draft)
    }

    @Test
    fun `image upload success increments review attempt in context and submits review command`() {
        val state = productEditorState(
            phase = ProductEditorPhase.ImageUploadInProgress,
            context = ProductEditorContext(draft = validDraft),
        )

        val result = machine.transition(
            state = state,
            event = ProductEditorEvent.ImageUploadSucceeded("upload-1"),
        )

        val reviewedDraft = validDraft.copy(reviewAttempt = 1)
        assertEquals(
            ProductEditorPhase.ReviewSubmissionInProgress(uploadToken = "upload-1"),
            result.state.phase,
        )
        assertEquals(reviewedDraft, result.state.context.draft)
        assertEquals(
            listOf(
                ProductEditorCommand.StartReviewSubmission(
                    draft = reviewedDraft,
                    uploadToken = "upload-1",
                ),
            ),
            result.commands,
        )
    }

    @Test
    fun `rejected draft edit stays rejected and updates context draft`() {
        val state = productEditorState(
            phase = ProductEditorPhase.Rejected(
                reason = "Mock reviewer asks for one resubmission.",
            ),
            context = ProductEditorContext(draft = validDraft),
        )

        val result = machine.transition(
            state = state,
            event = ProductEditorEvent.DescriptionChanged("Updated description."),
        )

        assertEquals(state.phase, result.state.phase)
        assertEquals("Updated description.", result.state.context.draft.form.description)
    }

    @Test
    fun `rejected draft can be resubmitted through upload again without passing draft through phase`() {
        val reviewedDraft = validDraft.copy(reviewAttempt = 1)
        val state = productEditorState(
            phase = ProductEditorPhase.Rejected(
                reason = "Mock reviewer asks for one resubmission.",
            ),
            context = ProductEditorContext(draft = reviewedDraft),
        )

        val result = machine.transition(state, ProductEditorEvent.ResubmitClicked)

        assertEquals(ProductEditorPhase.ImageUploadInProgress, result.state.phase)
        assertEquals(reviewedDraft, result.state.context.draft)
        assertEquals(listOf(ProductEditorCommand.StartImageUpload(reviewedDraft)), result.commands)
    }

    @Test
    fun `approved draft publishes product through phase entry command`() {
        val reviewedDraft = validDraft.copy(reviewAttempt = 2)
        val state = productEditorState(
            phase = ProductEditorPhase.Approved,
            context = ProductEditorContext(draft = reviewedDraft),
        )

        val result = machine.transition(state, ProductEditorEvent.PublishClicked)

        assertEquals(ProductEditorPhase.PublishInProgress, result.state.phase)
        assertEquals(reviewedDraft, result.state.context.draft)
        assertEquals(
            listOf(ProductEditorCommand.StartProductPublish(reviewedDraft)),
            result.commands,
        )
    }

    @Test
    fun `done after published emits close editor effect`() {
        val state = productEditorState(
            phase = ProductEditorPhase.Published(
                productId = 100,
                title = "Travel Mug",
            ),
        )

        val result = machine.transition(state, ProductEditorEvent.DoneClicked)

        assertIs<AfsmDecision.Stayed>(result.decision)
        assertEquals(listOf(ProductEditorEffect.CloseEditor), result.effects)
    }

    @Test
    fun `render state hides internal phase details from product editor ui`() {
        val state = productEditorState(
            phase = ProductEditorPhase.Rejected(
                reason = "Mock reviewer asks for one resubmission.",
            ),
            context = ProductEditorContext(draft = validDraft),
        )

        val renderState = state.toRenderState()

        assertEquals(validDraft.form, renderState.form)
        assertEquals("Review rejected", renderState.statusText)
        assertEquals(true, renderState.showDraftFields)
        assertEquals(true, renderState.fieldsEnabled)
        assertEquals(false, renderState.isProcessing)
        assertEquals(ProductEditorPrimaryAction.ResubmitForReview, renderState.primaryAction)
        assertEquals(ProductEditorSecondaryAction.ContinueEditing, renderState.secondaryAction)
        assertEquals("Mock reviewer asks for one resubmission.", renderState.reviewNote)
    }

    @Test
    fun `published render state hides draft fields and exposes completion action`() {
        val state = productEditorState(
            phase = ProductEditorPhase.Published(
                productId = 100,
                title = "Travel Mug",
            ),
            context = ProductEditorContext(draft = validDraft),
        )

        val renderState = state.toRenderState()

        assertEquals(false, renderState.showDraftFields)
        assertEquals(false, renderState.fieldsEnabled)
        assertEquals(false, renderState.isProcessing)
        assertEquals(ProductEditorPrimaryAction.Done, renderState.primaryAction)
        assertEquals(null, renderState.secondaryAction)
        assertEquals("Travel Mug", renderState.publishedTitle)
    }

    @Test
    fun `processing render states hide actions and disable draft fields`() {
        val processingPhases = listOf(
            ProductEditorPhase.SavingDraft,
            ProductEditorPhase.ImageUploadInProgress,
            ProductEditorPhase.ReviewSubmissionInProgress(uploadToken = "upload-1"),
            ProductEditorPhase.PublishInProgress,
        )

        processingPhases.forEach { phase ->
            val renderState = productEditorState(
                phase = phase,
                context = ProductEditorContext(draft = validDraft),
            ).toRenderState()

            assertEquals(true, renderState.showDraftFields)
            assertEquals(false, renderState.fieldsEnabled)
            assertEquals(true, renderState.isProcessing)
            assertEquals(null, renderState.primaryAction)
            assertEquals(null, renderState.secondaryAction)
        }
    }

    @Test
    fun `topology exposes ProductEditor graph without sample events`() {
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
                from = "ImageUploadInProgress",
                event = "ImageUploadSucceeded",
                to = "ReviewSubmissionInProgress",
            ) in transitions,
        )
        assertTrue(
            AfsmTopologyTransition(
                from = "ReviewSubmissionInProgress",
                event = "ReviewRejected",
                to = "Rejected",
            ) in transitions,
        )
        assertTrue(
            AfsmTopologyTransition(
                from = "Approved",
                event = "PublishClicked",
                to = "PublishInProgress",
            ) in transitions,
        )

        val mmd = machine.topology.toMmd()

        assertTrue("EditingDraft --> ImageUploadInProgress: SubmitClicked [valid draft]" in mmd)
        assertTrue("Approved --> PublishInProgress: PublishClicked" in mmd)
    }
}
