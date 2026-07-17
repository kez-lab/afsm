package afsm.sample.shop.feature.editor

import afsm.core.AfsmCommandInvocation
import afsm.core.AfsmTopologyTransition
import afsm.core.toMmd
import afsm.test.assertCommandInvocations
import afsm.test.assertCommands
import afsm.test.assertHandled
import afsm.test.assertNoCommands
import afsm.test.assertPhase
import afsm.test.assertTransitioned
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProductEditorStateMachineTest {
    private val machine = productEditorStateMachine
    private val validDraft = ProductDraft(
        form = ProductDraftForm(
            title = "Travel Mug",
            description = "Leakproof mug for commuting.",
            priceText = "24.50",
        ),
    )

    @Test
    fun `title change updates draft data without changing editing phase`() {
        val state = productEditorState()

        val result = machine.transition(
            state = state,
            event = ProductEditorEvent.TitleChanged("Travel Mug"),
        )

        result
            .assertHandled()
            .assertPhase(ProductEditorPhase.EditingDraft)
        assertEquals("Travel Mug", result.state.data.draft.form.title)
    }

    @Test
    fun `save draft transitions to saving phase and phase entry emits save command`() {
        val state = productEditorState(
            data = ProductEditorData(draft = validDraft),
        )

        val result = machine.transition(state, ProductEditorEvent.SaveDraftClicked)

        result
            .assertTransitioned()
            .assertPhase(ProductEditorPhase.SavingDraft)
            .assertCommands(ProductEditorCommand.SaveDraft(validDraft))
        assertEquals(validDraft, result.state.data.draft)
    }

    @Test
    fun `draft saved result transitions to draft saved phase without carrying draft in phase`() {
        val state = productEditorState(
            phase = ProductEditorPhase.SavingDraft,
            data = ProductEditorData(draft = validDraft),
        )

        val result = machine.transition(state, ProductEditorEvent.DraftSaveCompleted)

        result
            .assertTransitioned()
            .assertPhase(ProductEditorPhase.DraftSaved)
        assertEquals(validDraft, result.state.data.draft)
    }

    @Test
    fun `editing after draft saved returns to editing phase and updates data draft`() {
        val state = productEditorState(
            phase = ProductEditorPhase.DraftSaved,
            data = ProductEditorData(draft = validDraft),
        )

        val result = machine.transition(
            state = state,
            event = ProductEditorEvent.PriceChanged("25.00"),
        )

        result
            .assertTransitioned()
            .assertPhase(ProductEditorPhase.EditingDraft)
        assertEquals("25.00", result.state.data.draft.form.priceText)
    }

    @Test
    fun `submit from editing transitions only by phase and starts image upload from data draft`() {
        val state = productEditorState(
            data = ProductEditorData(draft = validDraft),
        )

        val result = machine.transition(state, ProductEditorEvent.SubmitClicked)

        result
            .assertTransitioned()
            .assertPhase(ProductEditorPhase.ImageUploadInProgress)
            .assertNoCommands()
            .assertCommandInvocations(
                AfsmCommandInvocation.Start(
                    key = productEditorImageUploadInvocationKey,
                    command = ProductEditorCommand.StartImageUpload(validDraft),
                ),
            )
        assertEquals(validDraft, result.state.data.draft)
    }

    @Test
    fun `invalid draft keeps editing phase with review submission error in data`() {
        val state = productEditorState(
            data = ProductEditorData(
                draft = validDraft.copy(
                    form = validDraft.form.copy(description = "short"),
                ),
            ),
        )

        val result = machine.transition(state, ProductEditorEvent.SubmitClicked)

        result
            .assertHandled()
            .assertPhase(ProductEditorPhase.EditingDraft)
            .assertNoCommands()
        assertEquals(
            "Description must be at least 10 characters.",
            result.state.data.errorMessage,
        )
    }

    @Test
    fun `invalid submit from saved draft handles without phase change saved and records validation error`() {
        val state = productEditorState(
            phase = ProductEditorPhase.DraftSaved,
            data = ProductEditorData(
                draft = validDraft.copy(
                    form = validDraft.form.copy(description = "short"),
                ),
            ),
        )

        val result = machine.transition(state, ProductEditorEvent.SubmitClicked)

        result
            .assertHandled()
            .assertPhase(ProductEditorPhase.DraftSaved)
            .assertNoCommands()
        assertEquals(
            "Description must be at least 10 characters.",
            result.state.data.errorMessage,
        )
    }

    @Test
    fun `image upload failure returns to editing phase with error in data`() {
        val state = productEditorState(
            phase = ProductEditorPhase.ImageUploadInProgress,
            data = ProductEditorData(draft = validDraft),
        )

        val result = machine.transition(
            state = state,
            event = ProductEditorEvent.ImageUploadFailed("Upload failed."),
        )

        result
            .assertTransitioned()
            .assertPhase(ProductEditorPhase.EditingDraft)
            .assertCommandInvocations(
                AfsmCommandInvocation.Cancel(productEditorImageUploadInvocationKey),
            )
        assertEquals("Upload failed.", result.state.data.errorMessage)
        assertEquals(validDraft, result.state.data.draft)
    }

    @Test
    fun `image upload success increments review attempt in data and submits review command`() {
        val state = productEditorState(
            phase = ProductEditorPhase.ImageUploadInProgress,
            data = ProductEditorData(draft = validDraft),
        )

        val result = machine.transition(
            state = state,
            event = ProductEditorEvent.ImageUploadSucceeded("upload-1"),
        )

        val reviewedDraft = validDraft.copy(reviewAttempt = 1)
        result
            .assertTransitioned()
            .assertPhase(ProductEditorPhase.ReviewSubmissionInProgress(uploadToken = "upload-1"))
            .assertCommands(
                ProductEditorCommand.StartReviewSubmission(
                    draft = reviewedDraft,
                    uploadToken = "upload-1",
                ),
            )
            .assertCommandInvocations(
                AfsmCommandInvocation.Cancel(productEditorImageUploadInvocationKey),
            )
        assertEquals(reviewedDraft, result.state.data.draft)
    }

    @Test
    fun `cancel upload returns to editing and cancels phase-owned invocation`() {
        val state = productEditorState(
            phase = ProductEditorPhase.ImageUploadInProgress,
            data = ProductEditorData(draft = validDraft),
        )

        val result = machine.transition(
            state = state,
            event = ProductEditorEvent.CancelUploadClicked,
        )

        result
            .assertTransitioned()
            .assertPhase(ProductEditorPhase.EditingDraft)
            .assertNoCommands()
            .assertCommandInvocations(
                AfsmCommandInvocation.Cancel(productEditorImageUploadInvocationKey),
            )
        assertEquals(validDraft, result.state.data.draft)
    }

    @Test
    fun `rejected draft edit handles without phase change rejected and updates data draft`() {
        val state = productEditorState(
            phase = ProductEditorPhase.Rejected(
                reason = "Mock reviewer asks for one resubmission.",
            ),
            data = ProductEditorData(draft = validDraft),
        )

        val result = machine.transition(
            state = state,
            event = ProductEditorEvent.DescriptionChanged("Updated description."),
        )

        result
            .assertHandled()
            .assertPhase(state.phase)
        assertEquals("Updated description.", result.state.data.draft.form.description)
    }

    @Test
    fun `rejected draft can be resubmitted through upload again without passing draft through phase`() {
        val reviewedDraft = validDraft.copy(reviewAttempt = 1)
        val state = productEditorState(
            phase = ProductEditorPhase.Rejected(
                reason = "Mock reviewer asks for one resubmission.",
            ),
            data = ProductEditorData(draft = reviewedDraft),
        )

        val result = machine.transition(state, ProductEditorEvent.ResubmitClicked)

        result
            .assertTransitioned()
            .assertPhase(ProductEditorPhase.ImageUploadInProgress)
            .assertNoCommands()
            .assertCommandInvocations(
                AfsmCommandInvocation.Start(
                    key = productEditorImageUploadInvocationKey,
                    command = ProductEditorCommand.StartImageUpload(reviewedDraft),
                ),
            )
        assertEquals(reviewedDraft, result.state.data.draft)
    }

    @Test
    fun `approved draft publishes product through phase entry command`() {
        val reviewedDraft = validDraft.copy(reviewAttempt = 2)
        val state = productEditorState(
            phase = ProductEditorPhase.Approved,
            data = ProductEditorData(draft = reviewedDraft),
        )

        val result = machine.transition(state, ProductEditorEvent.PublishClicked)

        result
            .assertTransitioned()
            .assertPhase(ProductEditorPhase.PublishInProgress)
            .assertCommands(ProductEditorCommand.StartProductPublish(reviewedDraft))
        assertEquals(reviewedDraft, result.state.data.draft)
    }

    @Test
    fun `render state hides internal phase details from product editor ui`() {
        val state = productEditorState(
            phase = ProductEditorPhase.Rejected(
                reason = "Mock reviewer asks for one resubmission.",
            ),
            data = ProductEditorData(draft = validDraft),
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
            data = ProductEditorData(draft = validDraft),
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
    fun `processing render states disable draft fields and only upload exposes cancel`() {
        val processingPhases = listOf(
            ProductEditorPhase.SavingDraft,
            ProductEditorPhase.ImageUploadInProgress,
            ProductEditorPhase.ReviewSubmissionInProgress(uploadToken = "upload-1"),
            ProductEditorPhase.PublishInProgress,
        )

        processingPhases.forEach { phase ->
            val renderState = productEditorState(
                phase = phase,
                data = ProductEditorData(draft = validDraft),
            ).toRenderState()

            assertEquals(true, renderState.showDraftFields)
            assertEquals(false, renderState.fieldsEnabled)
            assertEquals(true, renderState.isProcessing)
            assertEquals(null, renderState.primaryAction)
            val expectedSecondaryAction = when (phase) {
                ProductEditorPhase.ImageUploadInProgress ->
                    ProductEditorSecondaryAction.CancelUpload

                else -> null
            }
            assertEquals(expectedSecondaryAction, renderState.secondaryAction)
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
                event = "CancelUploadClicked",
                to = "EditingDraft",
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

        val uploadState = machine.topology.states.single { state ->
            state.id == "ImageUploadInProgress"
        }
        assertEquals(listOf("invoke StartImageUpload"), uploadState.entryCommandLabels)
        assertEquals(
            listOf("cancel product-editor/image-upload"),
            uploadState.exitCommandLabels,
        )

        assertTrue("EditingDraft --> ImageUploadInProgress: SubmitClicked [valid draft]" in mmd)
        assertTrue("ImageUploadInProgress --> EditingDraft: CancelUploadClicked" in mmd)
        assertTrue("Approved --> PublishInProgress: PublishClicked" in mmd)
    }
}
