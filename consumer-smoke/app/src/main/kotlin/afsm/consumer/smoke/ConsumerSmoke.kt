package afsm.consumer.smoke

import afsm.core.AfsmGraph
import afsm.core.AfsmGraphReducer
import afsm.core.AfsmNoEffect
import afsm.core.AfsmState
import afsm.core.afsmMachine
import afsm.generated.AfsmGeneratedGraphRegistry
import afsm.runtime.AfsmCommandHandler
import afsm.viewmodel.afsmHost
import androidx.lifecycle.ViewModel

internal sealed interface SmokePhase {
    data object Editing : SmokePhase
    data object Saving : SmokePhase
    data object Saved : SmokePhase
}

internal data class SmokeContext(
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

internal typealias SmokeState = AfsmState<SmokePhase, SmokeContext>

private typealias SmokeMachine = AfsmGraphReducer<SmokeState, SmokeEvent, SmokeCommand, AfsmNoEffect>

@AfsmGraph(
    id = "ConsumerSmoke",
    fileName = "ConsumerSmoke.mmd",
)
internal object ConsumerSmokeMachine : SmokeMachine by smokeMachine()

private fun smokeMachine(): SmokeMachine {
    return afsmMachine {
        initial(
            phase = SmokePhase.Editing,
            context = SmokeContext(),
        )

        state(SmokePhase.Editing) {
            on<SmokeEvent.TitleChanged> {
                stay {
                    updateContext {
                        copy(title = event.value)
                    }
                }
            }

            on<SmokeEvent.SaveClicked> {
                transitionTo(SmokePhase.Saving)
            }
        }

        state(SmokePhase.Saving) {
            onEnter {
                command(SmokeCommand.SaveTitle(context.title))
            }

            on<SmokeEvent.Saved> {
                transitionTo(SmokePhase.Saved)
            }
        }

        state(SmokePhase.Saved) {
        }
    }
}

internal class ConsumerSmokeViewModel : ViewModel() {
    val host = afsmHost(
        machine = ConsumerSmokeMachine,
        commandHandler = AfsmCommandHandler { command, dispatch ->
            when (command) {
                is SmokeCommand.SaveTitle -> dispatch(SmokeEvent.Saved)
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
