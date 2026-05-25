package afsm.consumer.smoke

import org.junit.Assert.assertEquals
import org.junit.Test

class DraftQuickstartTest {
    @Test
    fun saveClickedEntersSavingAndEmitsSaveDraft() {
        val result = DraftStateMachine.transition(
            state = DraftState(
                phase = DraftPhase.Editing,
                data = DraftData(title = "Plan"),
            ),
            event = DraftEvent.SaveClicked,
        )

        assertEquals(DraftPhase.Saving, result.state.phase)
        assertEquals(listOf(DraftCommand.SaveDraft("Plan")), result.commands)
    }

    @Test
    fun saveClickedWithMissingTitleStaysEditingWithMessage() {
        val result = DraftStateMachine.transition(
            state = DraftState(
                phase = DraftPhase.Editing,
                data = DraftData(title = ""),
            ),
            event = DraftEvent.SaveClicked,
        )

        assertEquals(DraftPhase.Editing, result.state.phase)
        assertEquals("Title is required.", result.state.data.errorMessage)
        assertEquals(emptyList<DraftCommand>(), result.commands)
    }

    @Test
    fun saveFailureReturnsToEditingWithMessage() {
        val result = DraftStateMachine.transition(
            state = DraftState(
                phase = DraftPhase.Saving,
                data = DraftData(title = "Plan"),
            ),
            event = DraftEvent.DraftSaveFailed("Network unavailable"),
        )

        assertEquals(DraftPhase.Editing, result.state.phase)
        assertEquals("Network unavailable", result.state.data.errorMessage)
    }
}
