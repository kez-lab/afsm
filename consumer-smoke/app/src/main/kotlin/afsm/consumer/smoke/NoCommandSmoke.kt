package afsm.consumer.smoke

import afsm.core.AfsmDefaultMachine
import afsm.core.AfsmNoCommand
import afsm.core.AfsmState
import afsm.core.afsmMachine
import afsm.viewmodel.afsmHost
import androidx.lifecycle.ViewModel

internal sealed interface NoCommandPhase {
    data object Disabled : NoCommandPhase
    data object Enabled : NoCommandPhase
}

internal data class NoCommandData(
    val label: String = "Notifications",
)

internal sealed interface NoCommandEvent {
    data object ToggleClicked : NoCommandEvent
}

internal typealias NoCommandState = AfsmState<NoCommandPhase, NoCommandData>

private typealias NoCommandMachine =
    AfsmDefaultMachine<NoCommandState, NoCommandEvent, AfsmNoCommand>

internal object NoCommandSmokeMachine : NoCommandMachine by noCommandSmokeMachine()

private fun noCommandSmokeMachine(): NoCommandMachine {
    return afsmMachine {
        initial(
            phase = NoCommandPhase.Disabled,
            data = NoCommandData(),
        )

        phase(NoCommandPhase.Disabled) {
            on<NoCommandEvent.ToggleClicked> {
                transitionTo(NoCommandPhase.Enabled)
            }
        }

        phase(NoCommandPhase.Enabled) {
            on<NoCommandEvent.ToggleClicked> {
                transitionTo(NoCommandPhase.Disabled)
            }
        }
    }
}

internal fun noCommandSmokeTransitionCommands(): List<AfsmNoCommand> {
    return NoCommandSmokeMachine.transition(
        state = NoCommandSmokeMachine.initialState,
        event = NoCommandEvent.ToggleClicked,
    ).commands
}

internal class NoCommandSmokeViewModel : ViewModel() {
    private val host = afsmHost(machine = NoCommandSmokeMachine)

    val state: NoCommandState
        get() = host.state.value

    fun toggle() {
        host.dispatch(NoCommandEvent.ToggleClicked)
    }
}
