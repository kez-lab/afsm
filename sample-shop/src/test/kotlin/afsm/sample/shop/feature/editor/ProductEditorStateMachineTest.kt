package afsm.sample.shop.feature.editor

import afsm.core.AfsmDecision
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

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
    fun `submit from editing validates draft and starts image upload`() {
        val state = ProductEditorState.EditingDraft(draft = validDraft)

        val result = machine.transition(state, ProductEditorEvent.SubmitClicked)

        assertEquals(ProductEditorState.UploadingImages(validDraft), result.state)
        assertEquals(listOf(ProductEditorCommand.UploadImages(validDraft)), result.commands)
    }

    @Test
    fun `invalid draft stays in editing with validation error`() {
        val state = ProductEditorState.EditingDraft(
            draft = validDraft.copy(
                form = validDraft.form.copy(description = "short"),
            ),
        )

        val result = machine.transition(state, ProductEditorEvent.SubmitClicked)

        assertIs<AfsmDecision.Stayed>(result.decision)
        assertEquals(
            "Description must be at least 10 characters.",
            (result.state as ProductEditorState.EditingDraft).errorMessage,
        )
        assertEquals(emptyList(), result.commands)
    }

    @Test
    fun `image upload success increments review attempt and submits review command`() {
        val state = ProductEditorState.UploadingImages(validDraft)

        val result = machine.transition(
            state = state,
            event = ProductEditorEvent.ImageUploadSucceeded("upload-1"),
        )

        val reviewedDraft = validDraft.copy(reviewAttempt = 1)
        assertEquals(
            ProductEditorState.SubmittingForReview(
                draft = reviewedDraft,
                uploadToken = "upload-1",
            ),
            result.state,
        )
        assertEquals(
            listOf(
                ProductEditorCommand.SubmitForReview(
                    draft = reviewedDraft,
                    uploadToken = "upload-1",
                ),
            ),
            result.commands,
        )
    }

    @Test
    fun `rejected draft can be resubmitted through upload again`() {
        val reviewedDraft = validDraft.copy(reviewAttempt = 1)
        val state = ProductEditorState.Rejected(
            draft = reviewedDraft,
            reason = "Mock reviewer asks for one resubmission.",
        )

        val result = machine.transition(state, ProductEditorEvent.ResubmitClicked)

        assertEquals(ProductEditorState.UploadingImages(reviewedDraft), result.state)
        assertEquals(listOf(ProductEditorCommand.UploadImages(reviewedDraft)), result.commands)
    }

    @Test
    fun `approved draft publishes product through command`() {
        val state = ProductEditorState.Approved(validDraft.copy(reviewAttempt = 2))

        val result = machine.transition(state, ProductEditorEvent.PublishClicked)

        assertEquals(ProductEditorState.Publishing(validDraft.copy(reviewAttempt = 2)), result.state)
        assertEquals(
            listOf(ProductEditorCommand.PublishProduct(validDraft.copy(reviewAttempt = 2))),
            result.commands,
        )
    }

    @Test
    fun `done after published emits close editor effect`() {
        val state = ProductEditorState.Published(
            productId = 100,
            title = "Travel Mug",
        )

        val result = machine.transition(state, ProductEditorEvent.DoneClicked)

        assertIs<AfsmDecision.Stayed>(result.decision)
        assertEquals(listOf(ProductEditorEffect.CloseEditor), result.effects)
    }
}
