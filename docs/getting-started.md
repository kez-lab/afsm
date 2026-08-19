# Getting Started

This guide builds the smallest useful Android Afsm feature. Complete compiled
versions live in `consumer-smoke/app/src/main/kotlin/.../DraftQuickstart.kt`.

For installation, the five-minute Draft path, API lookup, and guide navigation
in English or Korean, start with the [bilingual documentation hub](index.html).

## 1. Add the modules

Afsm is available from Maven Central. No extra repository is required.

```kotlin
dependencies {
    implementation("io.github.afsm:afsm-core:0.1.0")
    implementation("io.github.afsm:afsm-runtime:0.1.0")
    implementation("io.github.afsm:afsm-viewmodel:0.1.0")
    testImplementation("io.github.afsm:afsm-test:0.1.0")
}
```

## 2. Decide whether a machine helps

Use Afsm when the feature has meaningful phases and rules that depend on the
current phase. For a simple loading/content/error screen, a normal
`ViewModel + StateFlow` is usually easier.

For a draft editor, start with this flow:

```text
Editing --SaveClicked--> Saving --DraftSaveCompleted--> Saved
                           |
                           +--DraftSaveFailed--> Editing
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
    data object DraftSaveCompleted : DraftEvent
    data class DraftSaveFailed(val message: String) : DraftEvent
}

sealed interface DraftCommand {
    data class SaveDraft(val title: String) : DraftCommand
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
                command("SaveDraft") { DraftCommand.SaveDraft(data.title) }
            }

            on<DraftEvent.DraftSaveCompleted> {
                transitionTo(DraftPhase.Saved)
            }

            on<DraftEvent.DraftSaveFailed> {
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
        .assertCommands(DraftCommand.SaveDraft("Plan"))
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
        commandHandler = { command: DraftCommand, send ->
            when (command) {
                is DraftCommand.SaveDraft -> repository.save(command.title).fold(
                    onSuccess = { send(DraftEvent.DraftSaveCompleted) },
                    onFailure = { error ->
                        send(
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

    fun updateTitle(value: String) = host.send(DraftEvent.TitleChanged(value))
    fun save() = host.send(DraftEvent.SaveClicked)
}
```

`send` is the command handler's capability for returning results to the
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

## 8. Add restoration only after the safe state is clear

Do not restore an in-flight command phase just because the process died there.
Construct the safest business state first, then let the user or a verified
business result start the next command.

For the Draft sample:

- persisted saved draft -> `Saved`,
- editable unsaved draft -> `Editing`,
- uncertain save outcome -> `Editing` with a recoverable message, or a
  feature-specific unknown phase if the backend can confirm completion.

Initial state construction does not run `onEnter`, so restoring `Saving` would
not automatically save again. That is intentional: unsafe work should restart
only from an explicit event after the feature defines its recovery rule.

## 9. Read machine, graph, and tests together

Once the machine becomes non-trivial, generate its `.mmd` diagram. Use the
graph for whole-flow topology, machine code for exact local rules, and tests for
payload and `Handled`/`Ignored`/`Invalid` details.

Continue with [Modeling rules](modeling-rules.md),
[Testing](testing-guide.md), [Restoration, Command, and UI policy](restoration-command-ui-policy.md),
and [Graph generation](graph-generation.md).
