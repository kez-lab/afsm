# Afsm Implementation Pattern

Use this as a shape reference, not as a version source. Preserve the consuming
project's package names, build conventions, dependencies, behavior, and Afsm
version.

## Contents

1. [Dependencies](#dependencies)
2. [Flow model](#flow-model)
3. [Machine](#machine)
4. [Pure transition test](#pure-transition-test)
5. [ViewModel host](#viewmodel-host)
6. [Compose boundary](#compose-boundary)
7. [Graph generation](#graph-generation)
8. [Verification](#verification)

## Dependencies

Afsm's repository-current snapshot uses these module roles:

```kotlin
dependencies {
    implementation("io.github.afsm:afsm-core:$afsmVersion")
    implementation("io.github.afsm:afsm-runtime:$afsmVersion")
    implementation("io.github.afsm:afsm-viewmodel:$afsmVersion")
    testImplementation("io.github.afsm:afsm-test:$afsmVersion")
}
```

Afsm is currently verified through Maven Local rather than a public release.
Do not copy `0.1.0-SNAPSHOT` or add `mavenLocal()` unless the consumer has
explicitly chosen that distribution path.

## Flow Model

Keep feature flow types in a product-role file such as `DraftFlow.kt`:

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
    data class SaveDraft(val title: String) : DraftCommand
}
```

## Machine

```kotlin
@AfsmGraph(id = "Draft", fileName = "DraftStateMachine.mmd")
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
                    updateData {
                        copy(errorMessage = "Title is required.")
                    }
                }
            }
        }

        phase(DraftPhase.Saving) {
            onEnter {
                command("SaveDraft") {
                    DraftCommand.SaveDraft(data.title)
                }
            }
            on<DraftEvent.SaveCompleted> {
                transitionTo(DraftPhase.Saved)
            }
            on<DraftEvent.SaveFailed> {
                updateData { data, event ->
                    data.copy(errorMessage = event.message)
                }
                transitionTo(DraftPhase.Editing)
            }
        }

        phase(DraftPhase.Saved)
    }
```

Use `AfsmMachine` with an explicit host `initialState` when runtime data or
restoration determines the starting state.

## Pure Transition Test

```kotlin
@Test
fun `valid draft enters saving and emits save command`() {
    val editing = DraftState(
        phase = DraftPhase.Editing,
        data = DraftData(title = "Plan"),
    )

    draftMachine.transition(editing, DraftEvent.SaveClicked)
        .assertTransitioned()
        .assertPhase(DraftPhase.Saving)
        .assertCommands(DraftCommand.SaveDraft("Plan"))
}
```

Add focused tests for applicable validation, failure, retry, duplicate,
stale-result, invalid-event, and restored-state behavior.

## ViewModel Host

```kotlin
class DraftViewModel(
    private val repository: DraftRepository,
    initialState: DraftState = draftMachine.initialState,
) : ViewModel() {
    private val host = afsmHost(
        machine = draftMachine,
        initialState = initialState,
        commandHandler = { command: DraftCommand, dispatchEvent ->
            when (command) {
                is DraftCommand.SaveDraft ->
                    repository.save(command.title).fold(
                        onSuccess = {
                            dispatchEvent(DraftEvent.SaveCompleted)
                        },
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

    fun updateTitle(value: String) =
        host.dispatch(DraftEvent.TitleChanged(value))

    fun save() = host.dispatch(DraftEvent.SaveClicked)
}
```

ViewModel tests should call `updateTitle()` and `save()`, advance the test
dispatcher, then assert dependency calls and the resulting state. Do not
duplicate the complete transition matrix at this boundary.

## Compose Boundary

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

Keep focus, scrolling, animation, sheets, and snackbar host state in UI unless
they alter the business flow.

## Graph Generation

Use the same Afsm version for the graph plugin and KSP processor:

```kotlin
plugins {
    id("com.google.devtools.ksp")
    id("io.github.afsm.graph") version "<AFSM_VERSION>"
}

dependencies {
    ksp("io.github.afsm:afsm-graph-ksp:<AFSM_VERSION>")
}
```

Run the module's graph tasks:

```bash
./gradlew :feature:generateAfsmMmd   # write diagrams into build/
./gradlew :feature:updateAfsmMmd     # copy them into the committed baseline
./gradlew :feature:verifyAfsmMmd     # fail when the baseline is out of date
```

Commit the baseline directory (`afsmGraph.checkedInDir`, default
`afsm-graph/`) so graph drift is reviewed like any other source change.
`verifyAfsmMmd` joins `check` whenever that directory exists. Never hand-edit
generated build output or the baseline; regenerate it instead.

## Verification

Adapt task names to the consumer:

```bash
./gradlew :feature:testDebugUnitTest
./gradlew :feature:verifyAfsmMmd
./gradlew :feature:compileDebugKotlin
```

Use the consuming repository's normal broader gate after focused checks pass.
The Afsm source repository itself uses
`./scripts/verify-release-local.sh --no-daemon` for library release
verification; do not assume that script exists in an external project.
