package afsm.consumer.smoke

import afsm.core.AfsmGraph
import afsm.core.AfsmDefaultMachine
import afsm.core.AfsmNoEffect
import afsm.core.AfsmState
import afsm.core.afsmMachine
import afsm.viewmodel.afsmHost
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow

const val DraftTitleKey = "draftTitle"

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
    data class DraftSaveFailed(val message: String) : DraftEvent
}

sealed interface DraftCommand {
    data class SaveDraft(val title: String) : DraftCommand
}

@AfsmGraph(
    id = "DraftQuickstart",
    fileName = "DraftQuickstart.mmd",
)
val draftStateMachine: AfsmDefaultMachine<
    DraftState,
    DraftEvent,
    DraftCommand,
    AfsmNoEffect,
    > = afsmMachine {
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

        on<DraftEvent.DraftSaveFailed> {
            updateData { data, event ->
                data.copy(errorMessage = event.message)
            }
            transitionTo(DraftPhase.Editing)
        }
    }

    phase(DraftPhase.Saved)
}

interface DraftRepository {
    suspend fun save(title: String): Result<Unit>
}

fun draftStateFromSavedState(savedStateHandle: SavedStateHandle): DraftState {
    return DraftState(
        phase = DraftPhase.Editing,
        data = DraftData(
            title = savedStateHandle.get<String>(DraftTitleKey).orEmpty(),
        ),
    )
}

class DraftViewModel(
    private val repository: DraftRepository,
    initialState: DraftState = draftStateMachine.initialState,
) : ViewModel() {
    private val host = afsmHost(
        machine = draftStateMachine,
        initialState = initialState,
        commandHandler = { command: DraftCommand, dispatch ->
            when (command) {
                is DraftCommand.SaveDraft -> repository.save(command.title).fold(
                    onSuccess = {
                        dispatch(DraftEvent.DraftSaveCompleted)
                    },
                    onFailure = { error ->
                        dispatch(
                            DraftEvent.DraftSaveFailed(
                                error.message ?: "Draft save failed.",
                            ),
                        )
                    },
                )
            }
        },
    )

    val state: StateFlow<DraftState> = host.state

    fun onEvent(event: DraftEvent) {
        host.dispatch(event)
    }
}
