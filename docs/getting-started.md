# Getting Started

This guide builds the smallest useful Android Afsm feature. Complete compiled
versions live in `consumer-smoke/app/src/main/kotlin/.../DraftQuickstart.kt`.

## 1. Add the modules

Afsm is currently verified through Maven Local, not publicly released.

```kotlin
dependencies {
    implementation("io.github.afsm:afsm-core:0.1.0-SNAPSHOT")
    implementation("io.github.afsm:afsm-runtime:0.1.0-SNAPSHOT")
    implementation("io.github.afsm:afsm-viewmodel:0.1.0-SNAPSHOT")
    testImplementation("io.github.afsm:afsm-test:0.1.0-SNAPSHOT")
}
```

## 2. Decide whether a machine helps

Use Afsm when the feature has meaningful phases and rules that depend on the
current phase. For a simple loading/content/error screen, a normal
`ViewModel + StateFlow` is usually easier.

For a draft editor, start with this flow:

```text
Editing --SaveClicked--> Saving --SaveCompleted--> Saved
                           |
                           +--SaveFailed--> Editing
```

## 3. Define the flow types

Keep these in a product-role file such as `DraftFlow.kt`.

```kotlin
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
    data object SaveCompleted : DraftEvent
    data class SaveFailed(val message: String) : DraftEvent
}

sealed interface DraftCommand {
    data class Save(val title: String) : DraftCommand
}
```

The public vocabulary is `State`, `Event`, and `Command`. `Phase` and `Data`
form the state shape.

## 4. Write the machine

```kotlin
val draftMachine: AfsmDefaultMachine<DraftState, DraftEvent, DraftCommand> =
    afsmMachine {
        initial(DraftPhase.Editing, DraftData())

        phase(DraftPhase.Editing) {
            on<DraftEvent.TitleChanged> {
                updateData { data, event ->
                    data.copy(title = event.value, errorMessage = null)
                }
            }

            on<DraftEvent.SaveClicked> {
                case("valid title", condition = { data.title.isNotBlank() }) {
                    transitionTo(DraftPhase.Saving)
                }
                case("missing title", condition = { data.title.isBlank() }) {
                    updateData { copy(errorMessage = "Title is required.") }
                }
            }
        }

        phase(DraftPhase.Saving) {
            onEnter {
                command("Save") { DraftCommand.Save(data.title) }
            }

            on<DraftEvent.SaveCompleted> {
                transitionTo(DraftPhase.Saved)
            }

            on<DraftEvent.SaveFailed> {
                updateData { data, event -> data.copy(errorMessage = event.message) }
                transitionTo(DraftPhase.Editing)
            }
        }

        phase(DraftPhase.Saved)
    }
```

Use `case` only because `SaveClicked` has two real, named conditional outcomes.
For an unconditional rule, write `updateData`, `command`, or `transitionTo`
directly in `on<Event>`.

## 5. Test the pure machine

```kotlin
@Test
fun `valid draft starts save command`() {
    val editing = AfsmState(
        phase = DraftPhase.Editing,
        data = DraftData(title = "Plan"),
    )

    draftMachine.transition(editing, DraftEvent.SaveClicked)
        .assertTransitioned()
        .assertPhase(DraftPhase.Saving)
        .assertCommands(DraftCommand.Save("Plan"))
}
```

Entry commands are included in the result of the transition that enters the
phase. Test invalid input, retry, duplicate, and stale-result behavior as
separate cases.

## 6. Host it in ViewModel

```kotlin
class DraftViewModel(
    private val repository: DraftRepository,
) : ViewModel() {
    private val host = afsmHost(
        machine = draftMachine,
        commandHandler = { command: DraftCommand, dispatchEvent ->
            when (command) {
                is DraftCommand.Save -> repository.save(command.title).fold(
                    onSuccess = { dispatchEvent(DraftEvent.SaveCompleted) },
                    onFailure = { error ->
                        dispatchEvent(
                            DraftEvent.SaveFailed(
                                error.message ?: "Draft save failed.",
                            ),
                        )
                    },
                )
            }
        },
    )

    val state: StateFlow<DraftState> = host.state

    fun updateTitle(value: String) = host.dispatch(DraftEvent.TitleChanged(value))
    fun save() = host.dispatch(DraftEvent.SaveClicked)
}
```

`dispatchEvent` is the command handler's capability for returning results to the
serialized machine. It is not a generic UI callback.

Expose feature verbs to UI instead of `fun onEvent(event: DraftEvent)`. This
keeps machine events internal and makes the sample look like ordinary Android
code.

## 7. Connect Compose

```kotlin
@Composable
fun DraftRoute(viewModel: DraftViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    DraftScreen(
        title = state.data.title,
        errorMessage = state.data.errorMessage,
        isSaving = state.phase == DraftPhase.Saving,
        onTitleChange = viewModel::updateTitle,
        onSave = viewModel::save,
    )
}
```

Keep focus, scroll, animation, sheet, and snackbar host state in Compose unless
they change business flow. Keep completed product outcomes in machine state.

## 8. Read machine, graph, and tests together

Once the machine becomes non-trivial, generate its `.mmd` diagram. Use the
graph for whole-flow topology, machine code for exact local rules, and tests for
payload and `Handled`/`Ignored`/`Invalid` details.

Continue with [Modeling rules](modeling-rules.md),
[Testing](testing-guide.md), and [Graph generation](graph-generation.md).
