# Afsm

Afsm is an Android-focused finite state machine toolkit for complex `ViewModel` flows.

It keeps Android architecture familiar:

```text
UI event
-> ViewModel
-> AfsmHost
-> AfsmReducer or AfsmMachine
-> new state + commands + optional effects
-> ViewModel executes commands
-> command results dispatch events
-> UI renders state
```

Afsm is useful when a screen has meaningful phases, retries, invalid transitions, async results, or multi-step behavior. It is intentionally not a full UI framework and should not be forced onto simple loading/content/error screens.

## Modules

| Module | Purpose | Android dependency |
|---|---|---|
| `afsm-core` | Pure Kotlin transition types, reducer contract, executable machine DSL, graph metadata | No |
| `afsm-runtime` | Coroutine host, serialized dispatch loop, command execution, effect delivery | No |
| `afsm-viewmodel` | Thin `ViewModel.afsmHost(...)` adapter backed by `viewModelScope` | Yes |
| `afsm-graph-ksp` | KSP discovery for `@AfsmGraph` machines | No Android runtime dependency |
| `sample-shop` | Compose + Room sample app proving real usage | Yes |
| `consumer-smoke` | Separate Android consumer build that resolves Afsm from Maven Local | Yes |

For repository-local development, depend on the project modules:

```kotlin
dependencies {
    implementation(project(":afsm-core"))
    implementation(project(":afsm-runtime"))
    implementation(project(":afsm-viewmodel"))
}
```

For local artifact evaluation, publish to Maven Local:

```bash
./gradlew publishToMavenLocal
```

Then consume the pre-release snapshot artifacts:

```kotlin
repositories {
    google()
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation("io.github.afsm:afsm-core:0.1.0-SNAPSHOT")
    implementation("io.github.afsm:afsm-runtime:0.1.0-SNAPSHOT")
    implementation("io.github.afsm:afsm-viewmodel:0.1.0-SNAPSHOT")

    ksp("io.github.afsm:afsm-graph-ksp:0.1.0-SNAPSHOT")
}
```

`io.github.afsm` is the current pre-release group id used for local publishing. Final Maven Central coordinates still need product approval.

## Recommended Path

For graphable Android flows, start with `afsmMachine { ... }`.

The core model is intentionally small:

| Concept | Meaning |
|---|---|
| `Phase` | Finite state diagram node, such as `Editing` or `Saving` |
| `Context` | Extended state data carried across phases, not `android.content.Context` |
| `Event` | User input or command result |
| `Command` | Host-executed work, such as repository calls or timers |

`State` is the Android-facing snapshot:

```kotlin
typealias DraftState = AfsmState<DraftPhase, DraftContext>
```

`Effect` is optional. Use it only for UI one-shot outputs such as navigation or
closing a screen.

## Quickstart

Define the feature contract:

```kotlin
sealed interface DraftPhase {
    data object Editing : DraftPhase
    data object Saving : DraftPhase
    data object Saved : DraftPhase
}

data class DraftContext(
    val title: String = "",
)

typealias DraftState = AfsmState<DraftPhase, DraftContext>

sealed interface DraftEvent {
    data class TitleChanged(val value: String) : DraftEvent
    data object SaveClicked : DraftEvent
    data object Saved : DraftEvent
}

sealed interface DraftCommand {
    data class SaveDraft(val title: String) : DraftCommand
}
```

Define the machine:

```kotlin
private typealias DraftMachine =
    AfsmGraphReducer<DraftState, DraftEvent, DraftCommand, AfsmNoEffect>

@AfsmGraph(
    id = "Draft",
    fileName = "DraftStateMachine.mmd",
)
object DraftStateMachine : DraftMachine by draftMachine()

private fun draftMachine(): DraftMachine {
    return afsmMachine {
        initial(
            phase = DraftPhase.Editing,
            context = DraftContext(),
        )

        state(DraftPhase.Editing) {
            on<DraftEvent.TitleChanged> {
                stay {
                    updateContext {
                        copy(title = event.value)
                    }
                }
            }

            on<DraftEvent.SaveClicked> {
                transitionTo(DraftPhase.Saving)
            }
        }

        state(DraftPhase.Saving) {
            onEnter {
                command(DraftCommand.SaveDraft(context.title))
            }

            on<DraftEvent.Saved> {
                transitionTo(DraftPhase.Saved)
            }
        }

        state(DraftPhase.Saved) {
        }
    }
}
```

`transitionTo(...)` changes phase. `stay(...)` handles an event without changing
phase. Phase-changing transitions run in this order:

```text
onExit -> transition block -> onEnter
```

Initial state construction does not automatically run `onEnter`. Trigger startup work with an explicit event such as `ScreenEntered`.

## ViewModel Integration

```kotlin
class DraftViewModel(
    private val repository: DraftRepository,
) : ViewModel() {
    private val host = afsmHost(
        machine = DraftStateMachine,
        commandHandler = { command: DraftCommand, dispatch ->
            when (command) {
                is DraftCommand.SaveDraft -> {
                    repository.save(command.title)
                    dispatch(DraftEvent.Saved)
                }
            }
        },
    )

    val state: StateFlow<DraftState> = host.state

    fun onEvent(event: DraftEvent) {
        host.dispatch(event)
    }
}
```

The UI renders `state` and sends events up. Navigation, snackbar display, focus, scroll, and animation state should stay in the UI unless they are part of the business flow.

If the initial state comes from navigation arguments or `SavedStateHandle`, use
the lower-level overload:

```kotlin
private val host = afsmHost(
    initialState = CheckoutState(productId = productId),
    reducer = CheckoutStateMachine(),
    commandHandler = checkoutCommandHandler,
)
```

## Graph Generation

Annotate graphable machines:

```kotlin
@AfsmGraph(
    id = "ProductEditor",
    fileName = "ProductEditorStateMachine.mmd",
)
object ProductEditorStateMachine : ProductEditorMachine by productEditorMachine()
```

The sample app generates Mermaid state diagrams with:

```bash
./gradlew :sample-shop:generateAfsmMmd
```

Output:

```text
sample-shop/build/generated/afsm/mmd/AuthStateMachine.mmd
sample-shop/build/generated/afsm/mmd/ProductEditorStateMachine.mmd
```

## Runtime Policies

- Dispatch is non-suspending and serialized through FIFO event processing.
- Commands execute sequentially without blocking later event reduction.
- Command results must dispatch typed events back into the host.
- Domain failures should become domain events, not thrown exceptions.
- Unexpected command exceptions use `AfsmCommandFailurePolicy`.
- Invalid transitions throw by default so flow bugs are visible during development.
- `CancellationException` is always rethrown.
- Effects are best-effort one-shot outputs with no replay by default.

## Advanced APIs

Use `AfsmReducer` directly when a screen has a custom state shape or does not
need generated state diagrams. Use `AfsmGraphReducer` as a feature-local alias
for graphable machines so app code can refer to `FeatureState` instead of
repeating `Phase + Context` generics.

Graph generation currently derives phase/event topology automatically. Guard,
command, and effect labels are explicit metadata and should be added only when
the diagram needs that detail.

## Verification

Current baseline:

```bash
./scripts/verify-release-local.sh
```

`consumer-smoke` is intentionally a separate Gradle build. It verifies that an Android project can resolve `afsm-core`, `afsm-runtime`, `afsm-viewmodel`, and `afsm-graph-ksp` from Maven Local without project-module shortcuts.

See [docs/afsm-public-api.md](docs/afsm-public-api.md) for the API reference, [docs/sample-shop-afsm-guide.md](docs/sample-shop-afsm-guide.md) for sample app notes, [docs/release-readiness.md](docs/release-readiness.md) for publication gates, [CHANGELOG.md](CHANGELOG.md) for release notes, and [CONTRIBUTING.md](CONTRIBUTING.md) for development rules.
