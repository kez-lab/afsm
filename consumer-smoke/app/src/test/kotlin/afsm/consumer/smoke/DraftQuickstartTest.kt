package afsm.consumer.smoke

import afsm.test.assertCommands
import afsm.test.assertData
import afsm.test.assertHandled
import afsm.test.assertNoCommands
import afsm.test.assertPhase
import afsm.test.assertTransitioned
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

        result
            .assertTransitioned()
            .assertPhase(DraftPhase.Saving)
            .assertCommands(DraftCommand.SaveDraft("Plan"))
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

        result
            .assertHandled()
            .assertPhase(DraftPhase.Editing)
            .assertData(DraftData(errorMessage = "Title is required."))
            .assertNoCommands()
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

        result
            .assertTransitioned()
            .assertPhase(DraftPhase.Editing)
            .assertData(
                DraftData(
                    title = "Plan",
                    errorMessage = "Network unavailable",
                ),
            )
    }
}
