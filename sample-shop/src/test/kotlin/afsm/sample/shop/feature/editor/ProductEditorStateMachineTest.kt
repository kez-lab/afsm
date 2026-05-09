package afsm.sample.shop.feature.editor

import afsm.core.AfsmDecision
import afsm.core.AfsmTopologyTransition
import afsm.core.toMmd
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ProductEditorStateMachineTest {
    private val machine = ProductEditorStateMachine()
    private val validDraft = ProductDraft(
        form = ProductDraftForm(
            title = "Travel Mug",
            description = "Leakproof mug for commuting.",
            priceText = "24.50",
        ),
    )

    @Test
    fun `title change re-enters editing phase and updates draft in context`() {
        val state = ProductEditorState()

        val result = machine.transition(
            state = state,
            event = ProductEditorEvent.TitleChanged("Travel Mug"),
        )

        assertEquals(ProductEditorPhase.EditingDraft, result.state.phase)
        assertEquals("Travel Mug", result.state.context.draft.form.title)
    }

    @Test
    fun `save draft transitions to saving phase and phase entry emits save command`() {
        val state = ProductEditorState(
            context = ProductEditorContext(draft = validDraft),
        )

        val result = machine.transition(state, ProductEditorEvent.SaveDraftClicked)

        assertEquals(ProductEditorPhase.SavingDraft, result.state.phase)
        assertEquals(validDraft, result.state.context.draft)
        assertEquals(listOf(ProductEditorCommand.SaveDraft(validDraft)), result.commands)
    }

    @Test
    fun `draft saved result transitions to draft saved phase without carrying draft in phase`() {
        val state = ProductEditorState(
            phase = ProductEditorPhase.SavingDraft,
            context = ProductEditorContext(draft = validDraft),
        )

        val result = machine.transition(state, ProductEditorEvent.DraftSaved)

        assertEquals(ProductEditorPhase.DraftSaved, result.state.phase)
        assertEquals(validDraft, result.state.context.draft)
    }

    @Test
    fun `editing after draft saved returns to editing phase and updates context draft`() {
        val state = ProductEditorState(
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
        val state = ProductEditorState(
            context = ProductEditorContext(draft = validDraft),
        )

        val result = machine.transition(state, ProductEditorEvent.SubmitClicked)

        assertEquals(ProductEditorPhase.ImageUploadInProgress, result.state.phase)
        assertEquals(validDraft, result.state.context.draft)
        assertEquals(listOf(ProductEditorCommand.StartImageUpload(validDraft)), result.commands)
    }

    @Test
    fun `invalid draft re-enters editing phase with validation error in context`() {
        val state = ProductEditorState(
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
    fun `image upload failure returns to editing phase with error in context`() {
        val state = ProductEditorState(
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
        val state = ProductEditorState(
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
        val state = ProductEditorState(
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
        val state = ProductEditorState(
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
        val state = ProductEditorState(
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
        val state = ProductEditorState(
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
    fun `topology exposes ProductEditor graph without sample events`() {
        val transitions = machine.topology.transitions

        assertTrue(
            AfsmTopologyTransition(
                from = "EditingDraft",
                event = "SubmitClicked",
                to = "ImageUploadInProgress",
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

        assertTrue("EditingDraft --> ImageUploadInProgress: SubmitClicked" in mmd)
        assertTrue("Approved --> PublishInProgress: PublishClicked" in mmd)
    }
}
