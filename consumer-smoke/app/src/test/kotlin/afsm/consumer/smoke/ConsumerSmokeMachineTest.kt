package afsm.consumer.smoke

import afsm.test.assertCommands
import afsm.test.assertHandled
import afsm.test.assertPhase
import afsm.test.assertTransitioned
import kotlin.test.Test

class ConsumerSmokeMachineTest {
    @Test
    fun `published test helpers assert consumer machine transitions`() {
        val editing = ConsumerSmokeMachine.transition(
            state = ConsumerSmokeMachine.initialState,
            event = SmokeEvent.TitleChanged("Plan"),
        )

        editing
            .assertHandled()
            .assertPhase(SmokePhase.Editing)

        ConsumerSmokeMachine.transition(
            state = editing.state,
            event = SmokeEvent.SaveClicked,
        )
            .assertTransitioned()
            .assertPhase(SmokePhase.Saving)
            .assertCommands(SmokeCommand.SaveTitle("Plan"))
    }
}
