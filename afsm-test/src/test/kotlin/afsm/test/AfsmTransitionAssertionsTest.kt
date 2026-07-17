package afsm.test

import afsm.core.AfsmCommandInvocation
import afsm.core.AfsmInvocationKey
import afsm.core.AfsmState
import afsm.core.AfsmTransition
import kotlin.test.Test
import kotlin.test.assertFailsWith

class AfsmTransitionAssertionsTest {
    @Test
    fun `assertions pass for transitioned state and command work`() {
        AfsmTransition.transitioned(
            state = AfsmState(phase = Phase.Saving, data = Data(title = "Plan")),
            commands = listOf(Command.Save("Plan")),
            commandInvocations = listOf(
                AfsmCommandInvocation.Start(
                    key = AfsmInvocationKey("draft/autosave"),
                    command = Command.Save("Plan"),
                ),
            ),
        )
            .assertTransitioned()
            .assertPhase(Phase.Saving)
            .assertData(Data(title = "Plan"))
            .assertCommands(Command.Save("Plan"))
            .assertCommandInvocations(
                AfsmCommandInvocation.Start(
                    key = AfsmInvocationKey("draft/autosave"),
                    command = Command.Save("Plan"),
                ),
            )
    }

    @Test
    fun `handled ignored and invalid assertions can verify reasons`() {
        AfsmTransition.handled<AfsmState<Phase, Data>, Command>(
            state = AfsmState(Phase.Editing, Data()),
            reason = "validation failed",
        ).assertHandled("validation failed")

        AfsmTransition.ignored<AfsmState<Phase, Data>, Command>(
            state = AfsmState(Phase.Saving, Data()),
            reason = "stale result",
        ).assertIgnored("stale result")

        AfsmTransition.invalid<AfsmState<Phase, Data>, Command>(
            state = AfsmState(Phase.Editing, Data()),
            reason = "event not accepted",
        ).assertInvalid("event not accepted")
    }

    @Test
    fun `assertion failure names the expected decision`() {
        val failure = assertFailsWith<AssertionError> {
            AfsmTransition.invalid<AfsmState<Phase, Data>, Command>(
                state = AfsmState(Phase.Editing, Data()),
            ).assertTransitioned()
        }

        check("Transitioned" in failure.message.orEmpty())
        check("Invalid" in failure.message.orEmpty())
    }

    private sealed interface Phase {
        data object Editing : Phase
        data object Saving : Phase
    }

    private data class Data(
        val title: String = "",
    )

    private sealed interface Command {
        data class Save(val title: String) : Command
    }
}
