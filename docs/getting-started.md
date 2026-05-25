# Afsm Getting Started

This is the first document to read when adding Afsm to an Android screen.
The Draft machine and ViewModel in this guide are mirrored by `consumer-smoke`,
which compiles them against the published Maven Local artifacts during the
release gate.

Afsm is for complex `ViewModel` flows. If the screen is only loading a list,
showing a detail page, toggling a like, or submitting a one-step form, ordinary
`ViewModel + StateFlow` is usually clearer.

## Before You Paste Code

Add the three required modules to your Android feature/app module:

```kotlin
dependencies {
    implementation("io.github.afsm:afsm-core:0.1.0-SNAPSHOT")
    implementation("io.github.afsm:afsm-runtime:0.1.0-SNAPSHOT")
    implementation("io.github.afsm:afsm-viewmodel:0.1.0-SNAPSHOT")

    testImplementation("io.github.afsm:afsm-test:0.1.0-SNAPSHOT")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
}
```

Do not add `afsm-compose` for the first Draft screen. Add it only when a
machine emits UI one-shot effects and a Compose route needs
`CollectAfsmEffects(...)`:

```kotlin
implementation("io.github.afsm:afsm-compose:0.1.0-SNAPSHOT")
```

If a later ViewModel reads `SavedStateHandle` directly, add the AndroidX
saved-state artifact in that Android module:

```kotlin
implementation("androidx.lifecycle:lifecycle-viewmodel-savedstate:2.10.0")
```

If this is a Compose screen and the app module does not already have lifecycle
Compose, add it for `collectAsStateWithLifecycle()`:

```kotlin
implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
```

For Maven Local snapshots, make sure the consuming build has `mavenLocal()` in
`settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        mavenLocal()
        google()
        mavenCentral()
    }
}
```

Android consumers must also enable AndroidX:

```properties
android.useAndroidX=true
```

Keep the first Afsm code split into two files:

| File | Put here |
|---|---|
| `DraftStateMachine.kt` | `Phase`, `Data`, `State`, `Event`, `Command`, and `afsmMachine { ... }` |
| `DraftViewModel.kt` | `afsmHost(...)`, command execution, `StateFlow`, and `onEvent(...)` |

Compose route and screen files stay ordinary UI code. Add them when you connect
the ViewModel to UI rendering.

The state machine file needs:

```kotlin
import afsm.core.AfsmMachine
import afsm.core.AfsmNoEffect
import afsm.core.AfsmState
import afsm.core.afsmMachine
```

The ViewModel file needs:

```kotlin
import afsm.viewmodel.afsmHost
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow
```

The Draft machine uses `AfsmNoEffect` because it has no navigation, snackbar,
or close-screen one-shot output. Keep that marker until the screen has a real
UI-side effect to collect.

Do not start with graph generation. Add KSP and `@AfsmGraph` only after the
machine is useful and tested.

## The 10-Minute Model

Use these words in this order:

| Term | Meaning |
|---|---|
| `Phase` | The node in the state diagram |
| `Data` | Durable screen data carried across phases |
| `State` | The Android-facing snapshot: `AfsmState<Phase, Data>` |
| `Event` | User input or command result |
| `Command` | Work the ViewModel host must execute |
| `Effect` | Optional UI one-shot output |

`Data` is not `android.content.Context`. It is normal immutable screen data,
such as form fields, loaded product, request id, or validation message.

Use phase constructor payload only for a value that identifies that exact phase
instance, such as `requestId`, `uploadToken`, or `orderId`. Keep durable form,
loaded product, retry count, and error data in `Data` so it survives phase
changes without being copied through every phase.

## The Everyday API Choices

| Situation | Use |
|---|---|
| The business step changes | `transitionTo(Phase.X)` |
| The same step only updates form/error data | `updateData { ... }` |
| An event has named alternatives | `case(label, condition = ...) { ... }` |
| Repository, database, timer, or SDK work must run | `command(label) { ... }`, often in `onEnter` |
| Optional navigation/snackbar/close behavior is needed | `effect(label) { ... }` |
| An expected duplicate or stale event should be harmless | `ignore(reason)`, used sparingly |

`case(...)` is a graphable `if` branch. Branches are checked top-to-bottom; the
first matching branch handles the event. If no branch matches, the event is
invalid for the current phase.

## Build A Small Machine

Start with a simple Draft flow:

```text
Editing -- SaveClicked --> Saving -- DraftSaveCompleted --> Saved
Saving -- DraftSaveFailed --> Editing
```

1. Write the phase list first.

```kotlin
sealed interface DraftPhase {
    data object Editing : DraftPhase
    data object Saving : DraftPhase
    data object Saved : DraftPhase
}
```

2. Put durable screen data in `Data`.

```kotlin
data class DraftData(
    val title: String = "",
    val errorMessage: String? = null,
)

typealias DraftState = AfsmState<DraftPhase, DraftData>
```

3. Model user input and async results as events.

```kotlin
sealed interface DraftEvent {
    data class TitleChanged(val value: String) : DraftEvent
    data object SaveClicked : DraftEvent
    data object DraftSaveCompleted : DraftEvent
    data class DraftSaveFailed(val message: String) : DraftEvent
}
```

4. Model repository work as commands.

```kotlin
sealed interface DraftCommand {
    data class SaveDraft(val title: String) : DraftCommand
}
```

5. Name the machine type.

```kotlin
typealias DraftMachine =
    AfsmMachine<DraftState, DraftEvent, DraftCommand, AfsmNoEffect>

object DraftStateMachine : DraftMachine by draftMachine()
```

6. Write the machine in phase order.

```kotlin
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

        on<DraftEvent.DraftSaveFailed> {
            case {
                updateData { data, event ->
                    data.copy(errorMessage = event.message)
                }
                transitionTo(DraftPhase.Editing)
            }
        }
    }

    phase(DraftPhase.Saved)
}
```

Read one command-backed flow like this:

```text
SaveClicked
-> Editing accepts the event
-> transitionTo(Saving)
-> Saving.onEnter emits SaveDraft
-> ViewModel command handler calls repository
-> command handler dispatches DraftSaveCompleted or DraftSaveFailed
-> Saving transitions to Saved or back to Editing with an error message
```

Initial state construction does not run `onEnter`. The Draft example starts in
`Editing`, so no work is expected at construction time. For startup work, model
an explicit event such as `ScreenEntered`, dispatch it after the ViewModel or UI
is ready, and let that event transition to a loading phase whose `onEnter`
emits the command.

If a case does not call `transitionTo(...)`, it handles the event without a
phase change. Use that for form text changes and validation errors.

If one event must update data and change phase, keep both statements inside the
same `case { ... }`. Sibling calls such as `updateData(...)` followed by
`transitionTo(...)` are separate alternatives; the first matching alternative
handles the event.

## Add First JVM Tests

Test the pure machine before wiring the Android `ViewModel`. These tests are
mirrored by `consumer-smoke`, so the release gate verifies that the quickstart
behavior still works from Maven Local artifacts.

```kotlin
import afsm.test.assertCommands
import afsm.test.assertData
import afsm.test.assertHandled
import afsm.test.assertNoCommands
import afsm.test.assertPhase
import afsm.test.assertTransitioned
import org.junit.Test

class DraftStateMachineTest {
    @Test
    fun saveClickedEntersSavingAndEmitsSaveDraft() {
        val result = DraftStateMachine.transition(
            state = DraftState(
                phase = DraftPhase.Editing,
                data = DraftData(title = "Plan"),
            ),
            event = DraftEvent.SaveClicked,
        )

        result
            .assertTransitioned()
            .assertPhase(DraftPhase.Saving)
            .assertCommands(DraftCommand.SaveDraft("Plan"))
    }

    @Test
    fun saveClickedWithMissingTitleStaysEditingWithMessage() {
        val result = DraftStateMachine.transition(
            state = DraftState(
                phase = DraftPhase.Editing,
                data = DraftData(title = ""),
            ),
            event = DraftEvent.SaveClicked,
        )

        result
            .assertHandled()
            .assertPhase(DraftPhase.Editing)
            .assertData(DraftData(errorMessage = "Title is required."))
            .assertNoCommands()
    }

    @Test
    fun saveFailureReturnsToEditingWithMessage() {
        val result = DraftStateMachine.transition(
            state = DraftState(
                phase = DraftPhase.Saving,
                data = DraftData(title = "Plan"),
            ),
            event = DraftEvent.DraftSaveFailed("Network unavailable"),
        )

        result
            .assertTransitioned()
            .assertPhase(DraftPhase.Editing)
            .assertData(
                DraftData(
                    title = "Plan",
                    errorMessage = "Network unavailable",
                ),
            )
    }
}
```

Keep these tests focused on transition behavior: next phase, changed data,
emitted commands, emitted effects, and ignored or invalid decisions. The
`afsm-test` helpers keep those assertions focused on behavior instead of raw
transition structure. ViewModel tests should verify Android wiring, not
duplicate every state-machine branch.

## Host From ViewModel

The machine never calls repositories directly. The ViewModel host executes
commands and dispatches result events back into the machine.

This example assumes the repository reports expected save failures as a result:

```kotlin
interface DraftRepository {
    suspend fun save(title: String): Result<Unit>
}

class DraftViewModel(
    private val repository: DraftRepository,
    initialState: DraftState = DraftStateMachine.initialState,
) : ViewModel() {
    private val host = afsmHost(
        machine = DraftStateMachine,
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
```

Expected domain failures should become result events from the command handler.
Do not mutate `host.state` directly from repository callbacks.

Expose `host.effects` only when the feature has one-shot UI effects.

## Connect The First Compose Route

For the no-effect Draft screen, do not add `afsm-compose`. A normal Compose
route only needs lifecycle-aware state collection and event callbacks into the
ViewModel:

```kotlin
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun DraftRoute(
    viewModel: DraftViewModel,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    DraftScreen(
        state = state,
        onTitleChanged = { value ->
            viewModel.onEvent(DraftEvent.TitleChanged(value))
        },
        onSaveClick = {
            viewModel.onEvent(DraftEvent.SaveClicked)
        },
    )
}

@Composable
fun DraftScreen(
    state: DraftState,
    onTitleChanged: (String) -> Unit,
    onSaveClick: () -> Unit,
) {
    // Render state.data.title, state.data.errorMessage, and state.phase.
    // Send user actions through the callbacks.
}
```

Keep repository calls out of the route and screen. They stay in the ViewModel's
command handler, and the machine stays plain Kotlin.

## Add The First Effect Later

Keep the first Draft machine on `AfsmNoEffect` until the screen has real
route-level UI work such as optional navigation, a fire-and-forget snackbar, or
closing the screen. When that moment comes, change the effect type first:

```kotlin
sealed interface DraftEffect {
    data object CloseEditor : DraftEffect
}

typealias DraftMachine =
    AfsmMachine<DraftState, DraftEvent, DraftCommand, DraftEffect>
```

After this change, remove the `AfsmNoEffect` import from the machine file.

Then emit the effect from the transition that also records durable product
progress in state:

```kotlin
on<DraftEvent.DraftSaveCompleted> {
    case {
        transitionTo(DraftPhase.Saved)
        effect(label = "CloseEditor") { DraftEffect.CloseEditor }
    }
}
```

Do not make required progress effect-only. `DraftPhase.Saved` is the durable
business result; `DraftEffect.CloseEditor` is only a convenience for the
currently active route.

Expose effects from the ViewModel:

```kotlin
import kotlinx.coroutines.flow.Flow

val effects: Flow<DraftEffect> = host.effects
```

Add `afsm-compose` only in the UI module that collects the effect:

```kotlin
implementation("io.github.afsm:afsm-compose:0.1.0-SNAPSHOT")
```

Collect effects at the route level, next to lifecycle-aware state collection:

```kotlin
import afsm.compose.CollectAfsmEffects
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun DraftRoute(
    viewModel: DraftViewModel,
    onDone: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    CollectAfsmEffects(viewModel.effects) { effect ->
        when (effect) {
            DraftEffect.CloseEditor -> onDone()
        }
    }

    DraftScreen(
        state = state,
        onEvent = viewModel::onEvent,
    )
}
```

For effects that must survive lifecycle gaps, use state plus an acknowledgement
event instead of `Effect`. See
[restoration-effect-command-policy.md](restoration-effect-command-policy.md).

## Add First ViewModel Test

After the machine tests pass, add one ViewModel wiring test. Do not duplicate
every state-machine branch here. Prove that `onEvent(event)` reaches the hosted
machine, the command handler calls the repository, and command result events
update `state.value`.

Use `runTest`, `StandardTestDispatcher`, and `Dispatchers.setMain/resetMain`
around `viewModelScope` code. The complete Draft example is in
[testing-guide.md](testing-guide.md#viewmodel-tests), and the same pattern is
compiled in
[`consumer-smoke/app/src/test/kotlin/afsm/consumer/smoke/DraftViewModelTest.kt`](../consumer-smoke/app/src/test/kotlin/afsm/consumer/smoke/DraftViewModelTest.kt).

## Add Initial State From SavedStateHandle Later

If the starting state comes from navigation arguments, a deep link, repository
restoration, or `SavedStateHandle`, convert those small inputs into a feature
state before calling `afsmHost(...)`. Keep the machine graphable by passing the
same `machine` plus an explicit `initialState`.

For the Draft example, save only the title key:

```kotlin
import androidx.lifecycle.SavedStateHandle

const val DraftTitleKey = "draftTitle"

fun draftStateFromSavedState(savedStateHandle: SavedStateHandle): DraftState {
    return DraftState(
        phase = DraftPhase.Editing,
        data = DraftData(
            title = savedStateHandle.get<String>(DraftTitleKey).orEmpty(),
        ),
    )
}
```

Then pass that state into the ViewModel or factory that owns the host:

```kotlin
val viewModel = DraftViewModel(
    repository = repository,
    initialState = draftStateFromSavedState(savedStateHandle),
)
```

Test this path by constructing `SavedStateHandle` directly. Also assert that
restored initial state does not start work by itself:

```kotlin
@Test
fun savedStateHandleTitleSeedsInitialDraftStateWithoutStartingWork() = runTest {
    val mainDispatcher = StandardTestDispatcher(testScheduler)
    Dispatchers.setMain(mainDispatcher)
    try {
        val savedStateHandle = SavedStateHandle(
            mapOf(DraftTitleKey to "Restored plan"),
        )
        val repository = RecordingDraftRepository(Result.success(Unit))
        val viewModel = DraftViewModel(
            repository = repository,
            initialState = draftStateFromSavedState(savedStateHandle),
        )

        mainDispatcher.scheduler.advanceUntilIdle()

        assertEquals(DraftPhase.Editing, viewModel.state.value.phase)
        assertEquals(DraftData(title = "Restored plan"), viewModel.state.value.data)
        assertEquals(emptyList<String>(), repository.savedTitles)
    } finally {
        Dispatchers.resetMain()
    }
}
```

This is the same shape Checkout uses for a navigation `productId`; it starts
work only after an explicit `ScreenEntered` event moves the machine to a
loading phase.

## What To Read Next

1. [modeling-rules.md](modeling-rules.md) for when to use Afsm.
2. [auth-walkthrough.md](auth-walkthrough.md) for a small Android form.
3. [checkout-walkthrough.md](checkout-walkthrough.md) for retry and stale results.
4. [product-editor-walkthrough.md](product-editor-walkthrough.md) for a large transaction flow.
5. [testing-guide.md](testing-guide.md) for broader transition and ViewModel test coverage.
6. [graph-generation.md](graph-generation.md) only after the machine is useful.
