package afsm.consumer.smoke

import afsm.core.AfsmGraph
import afsm.core.AfsmDefaultMachine
import afsm.core.AfsmNoEffect
import afsm.core.AfsmState
import afsm.core.afsmMachine
import afsm.generated.AfsmGeneratedGraphRegistry
import afsm.viewmodel.afsmHost
import androidx.lifecycle.ViewModel

internal sealed interface SmokePhase {
    data object Editing : SmokePhase
    data object Saving : SmokePhase
    data object Saved : SmokePhase
}

internal data class SmokeData(
    val title: String = "",
)

internal sealed interface SmokeEvent {
    data class TitleChanged(val value: String) : SmokeEvent
    data object SaveClicked : SmokeEvent
    data object Saved : SmokeEvent
}

internal sealed interface SmokeCommand {
    data class SaveTitle(val value: String) : SmokeCommand
}

internal typealias SmokeState = AfsmState<SmokePhase, SmokeData>

@AfsmGraph(
    id = "ConsumerSmoke",
    fileName = "ConsumerSmoke.mmd",
)
internal val consumerSmokeMachine:
    AfsmDefaultMachine<SmokeState, SmokeEvent, SmokeCommand, AfsmNoEffect> =
    afsmMachine {
        initial(
            phase = SmokePhase.Editing,
            data = SmokeData(),
        )

        phase(SmokePhase.Editing) {
            on<SmokeEvent.TitleChanged> {
                updateData { data, event ->
                    data.copy(title = event.value)
                }
            }

            on<SmokeEvent.SaveClicked> {
                transitionTo(SmokePhase.Saving)
            }
        }

        phase(SmokePhase.Saving) {
            onEnter {
                command(label = "SaveTitle") {
                    SmokeCommand.SaveTitle(data.title)
                }
            }

            on<SmokeEvent.Saved> {
                transitionTo(SmokePhase.Saved)
            }
        }

        phase(SmokePhase.Saved) {
        }
}

internal class ConsumerSmokeViewModel : ViewModel() {
    val host = afsmHost(
        machine = consumerSmokeMachine,
        commandHandler = { command: SmokeCommand, dispatchEvent ->
            when (command) {
                is SmokeCommand.SaveTitle -> dispatchEvent(SmokeEvent.Saved)
            }
        },
    )

    val state: SmokeState
        get() = host.state.value

    fun updateTitle(value: String) {
        host.dispatch(SmokeEvent.TitleChanged(value))
    }

    fun save() {
        host.dispatch(SmokeEvent.SaveClicked)
    }
}

internal fun generatedAfsmGraphCount(): Int {
    return AfsmGeneratedGraphRegistry.entries.size
}
