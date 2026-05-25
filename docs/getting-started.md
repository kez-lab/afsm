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
}
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

Keep the first screen split into two files:

| File | Put here |
|---|---|
| `DraftStateMachine.kt` | `Phase`, `Data`, `State`, `Event`, `Command`, and `afsmMachine { ... }` |
| `DraftViewModel.kt` | `afsmHost(...)`, command execution, `StateFlow`, and `onEvent(...)` |

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
            updateData { data, event ->
                data.copy(errorMessage = event.message)
            }
            transitionTo(DraftPhase.Editing)
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
) : ViewModel() {
    private val host = afsmHost(
        machine = DraftStateMachine,
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

If the starting state comes from navigation arguments, a deep link, repository
restoration, or `SavedStateHandle`, pass an explicit initial state:

```kotlin
private val host = afsmHost(
    machine = CheckoutStateMachine,
    initialState = checkoutState(productId = productId),
    commandHandler = checkoutCommandHandler,
)
```

## What To Read Next

1. [modeling-rules.md](modeling-rules.md) for when to use Afsm.
2. [auth-walkthrough.md](auth-walkthrough.md) for a small Android form.
3. [checkout-walkthrough.md](checkout-walkthrough.md) for retry and stale results.
4. [product-editor-walkthrough.md](product-editor-walkthrough.md) for a large transaction flow.
5. [graph-generation.md](graph-generation.md) only after the machine is useful.
