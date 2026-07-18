# Afsm

![Status](https://img.shields.io/badge/status-internal%20beta-orange)
![Kotlin](https://img.shields.io/badge/kotlin-2.0.21-7F52FF?logo=kotlin)
![Android](https://img.shields.io/badge/android-AGP%208.10.1-3DDC84?logo=android)
![Distribution](https://img.shields.io/badge/distribution-Maven%20Local-lightgrey)

**English** | [한국어](README.ko.md) | [Documentation (EN/KO)](docs/index.html)

Afsm helps Android teams make complex screen flows easier to read, verify, and
change safely. It moves business-flow rules scattered across `ViewModel`
`state.copy(...)` calls, coroutines, callbacks, and tests into one plain Kotlin
state machine, while Android `ViewModel` remains the lifecycle, `StateFlow`,
saved-state, repository, and UI adapter.

Use Afsm for screens with meaningful phases, retries, concurrent or stale async
results, and rules that depend on the current phase. Keep ordinary
`ViewModel + StateFlow` for simple screens when it is clearer.

## Why I Started Afsm

Complex Android screens often reach a point where every individual handler looks
reasonable but the complete flow exists everywhere and nowhere at once. To
answer “what can happen now?”, a reviewer has to reconstruct rules across the
`ViewModel`, UI callbacks, repository calls, result callbacks, and tests.

Afsm began as an attempt to make those answers local and executable. It does not
replace `ViewModel`, hide Kotlin `copy()`, or require an app-wide MVI
architecture. It gives one complex feature a readable business-flow model that
can be executed, tested, and rendered as a state diagram.

## Three Concepts

The public machine vocabulary is deliberately small:

| Concept | Meaning |
|---|---|
| `State` | Current `Phase` plus durable business `Data` |
| `Event` | An input to the machine: user intent or external-work result |
| `Command` | A value asking the Android host to perform external work |

`Phase` and `Data` normally use `AfsmState<Phase, Data>`. A screen that never
starts external work can use `AfsmNoCommand`.

### Why Command Is Separate

The pure machine must not call a suspend repository, database, timer, or SDK.
Instead it returns a `Command`; `ViewModel` executes it and dispatches the result
as a new `Event`.

```text
UI intent -> Event -> pure machine -> State + Command
                                      |
                                      v
                              ViewModel executes work
                                      |
                                      v
                                  result Event
```

This separation keeps transitions deterministic and JVM-testable. It also
prevents external work from being accidentally restarted merely because state
was recollected or restored.

Afsm has no separate `Effect` output channel. Product completion belongs in
state. UI-only actions initiated by the UI, such as closing an editor after a
Done click, stay direct UI callbacks. A route can react to a durable completion
state with `LaunchedEffect` when navigation is required.

## Minimal Machine

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
}

sealed interface DraftCommand {
    data class SaveDraft(val title: String) : DraftCommand
}

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
        }

        phase(DraftPhase.Saved)
    }
```

Use ordinary Kotlin inside a rule. Use `case(...)` only when one event has
multiple named conditional outcomes that should appear in the generated graph.

## Android Boundary

The machine uses events internally; the Compose UI does not need to know those
event types. Expose verb-named feature methods from the `ViewModel`:

```kotlin
class DraftViewModel(
    private val repository: DraftRepository,
) : ViewModel() {
    private val host = afsmHost(
        machine = draftMachine,
        commandHandler = { command: DraftCommand, dispatchEvent ->
            when (command) {
                is DraftCommand.SaveDraft -> {
                    repository.save(command.title)
                    dispatchEvent(DraftEvent.DraftSaveCompleted)
                }
            }
        },
    )

    val state: StateFlow<DraftState> = host.state

    fun updateTitle(value: String) = host.dispatch(DraftEvent.TitleChanged(value))
    fun save() = host.dispatch(DraftEvent.SaveClicked)
}
```

```kotlin
@Composable
fun DraftRoute(viewModel: DraftViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    DraftScreen(
        title = state.data.title,
        onTitleChange = viewModel::updateTitle,
        onSave = viewModel::save,
    )
}
```

This is intentional Android code, not a requirement to expose one generic
`onEvent(Event)` MVI boundary.

## Why Machine, Graph, and Tests Are All Needed

Phase-local lambdas keep each rule close to the state where it is valid, but a
large machine is not the best whole-flow overview. Afsm therefore treats three
artifacts as one reading contract:

| Artifact | Best question it answers |
|---|---|
| generated `.mmd` graph | What is the complete topology, including named conditions and entry commands? |
| machine source | What exact data, guards, commands, and ordering does this rule use? |
| transition tests | What payload details and graph-invisible `Handled`, `Ignored`, or `Invalid` behavior are proven? |

The graph is not decoration and it is not a substitute for code. It compensates
for the locality tradeoff of phase-scoped rules by giving reviewers a generated,
whole-machine map. Because it is generated from the executable machine, drift is
checked by the build rather than maintained by hand.

## First-Use Path

1. Decide whether the screen is complex enough to justify a machine.
2. Draw phases and important transitions before writing DSL.
3. Define `State`, `Event`, and `Command` in a product-role `*Flow.kt` file.
4. Implement phase-local rules and use `case` only for real conditions.
5. Test the pure machine first.
6. Host it in a normal Android `ViewModel` and expose verb-named methods.
7. Generate and review the `.mmd` graph beside the machine and tests.

Start with [Getting started](docs/getting-started.md), then read
[Modeling rules](docs/modeling-rules.md) and [Graph generation](docs/graph-generation.md).

## Modules

| Module | Purpose |
|---|---|
| `afsm-core` | Pure Kotlin machine, DSL, topology, Mermaid rendering |
| `afsm-runtime` | Serialized event processing and command execution |
| `afsm-viewmodel` | `ViewModel.afsmHost(...)` integration |
| `afsm-test` | Transition assertion helpers |
| `afsm-graph-ksp` | Generated graph registry |
| `afsm-graph-gradle-plugin` | `.mmd` export task |
| `sample-shop` | Android reference flows |
| `consumer-smoke` | External Maven Local compile and behavior gate |

## Build and Verify

```bash
./gradlew :afsm-core:test :afsm-runtime:test :afsm-test:test
./gradlew :sample-shop:testDebugUnitTest :sample-shop:generateAfsmMmd
./scripts/verify-release-local.sh --no-daemon
```

Afsm has not been publicly released. APIs may change when usability or safety
evidence shows that a better design serves real Android teams.

See [Public API](docs/afsm-public-api.md), [Testing](docs/testing-guide.md),
[Examples](docs/examples.md), [Auth](docs/auth-walkthrough.md),
[Checkout](docs/checkout-walkthrough.md), and
[Product editor](docs/product-editor-walkthrough.md).
