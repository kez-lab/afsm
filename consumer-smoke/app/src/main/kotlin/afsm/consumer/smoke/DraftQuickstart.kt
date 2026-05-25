package afsm.consumer.smoke

import afsm.core.AfsmGraph
import afsm.core.AfsmMachine
import afsm.core.AfsmNoEffect
import afsm.core.AfsmState
import afsm.core.afsmMachine
import afsm.runtime.AfsmCommandHandler
import afsm.viewmodel.afsmHost
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow

sealed interface DraftPhase {
    data object Editing : DraftPhase
    data object Saving : DraftPhase
    data object Saved : DraftPhase
}

data class DraftData(
    val title: String = "",
    val errorMessage: String? = null,
)

typealias DraftState = AfsmState<DraftPhase, DraftData>

sealed interface DraftEvent {
    data class TitleChanged(val value: String) : DraftEvent
    data object SaveClicked : DraftEvent
    data object DraftSaveCompleted : DraftEvent
}

sealed interface DraftCommand {
    data class SaveDraft(val title: String) : DraftCommand
}

typealias DraftMachine =
    AfsmMachine<DraftState, DraftEvent, DraftCommand, AfsmNoEffect>

@AfsmGraph(
    id = "DraftQuickstart",
    fileName = "DraftQuickstart.mmd",
)
object DraftStateMachine : DraftMachine by draftMachine()

private fun draftMachine(): DraftMachine = afsmMachine {
    initial(
        phase = DraftPhase.Editing,
        data = DraftData(),
    )

    phase(DraftPhase.Editing) {
        on<DraftEvent.TitleChanged> {
            updateData { data, event ->
                data.copy(
                    title = event.value,
                    errorMessage = null,
                )
            }
        }

        on<DraftEvent.SaveClicked> {
            case(
                label = "valid title",
                condition = { data.title.isNotBlank() },
            ) {
                transitionTo(DraftPhase.Saving)
            }

            case(
                label = "missing title",
                condition = { data.title.isBlank() },
            ) {
                updateData { copy(errorMessage = "Title is required.") }
            }
        }
    }

    phase(DraftPhase.Saving) {
        onEnter {
            command(label = "SaveDraft") {
                DraftCommand.SaveDraft(data.title)
            }
        }

        on<DraftEvent.DraftSaveCompleted> {
            transitionTo(DraftPhase.Saved)
        }
    }

    phase(DraftPhase.Saved)
}

interface DraftRepository {
    suspend fun save(title: String)
}

class DraftViewModel(
    private val repository: DraftRepository,
) : ViewModel() {
    private val host = afsmHost(
        machine = DraftStateMachine,
        commandHandler = AfsmCommandHandler { command: DraftCommand, dispatch ->
            when (command) {
                is DraftCommand.SaveDraft -> {
                    repository.save(command.title)
                    dispatch(DraftEvent.DraftSaveCompleted)
                }
            }
        },
    )

    val state: StateFlow<DraftState> = host.state

    fun onEvent(event: DraftEvent) {
        host.dispatch(event)
    }
}
