# Afsm

[![CI](https://github.com/kez-lab/afsm/actions/workflows/ci.yml/badge.svg)](https://github.com/kez-lab/afsm/actions/workflows/ci.yml)
![Status](https://img.shields.io/badge/status-internal%20beta-orange)
![Kotlin](https://img.shields.io/badge/kotlin-2.0.21-7F52FF?logo=kotlin)
![Android](https://img.shields.io/badge/android-AGP%208.10.1-3DDC84?logo=android)
![Distribution](https://img.shields.io/badge/distribution-Maven%20Local-lightgrey)

Afsm is an Android-focused finite state machine toolkit for complex `ViewModel`
flows. It keeps `ViewModel` as the Android lifecycle adapter and moves screen
flow rules into a plain Kotlin machine.

Use Afsm when a screen has meaningful phases, retries, async results, invalid
transitions, or multi-step behavior. Do not force it onto simple product lists,
detail pages, likes, review lists, or basic loading/content/error screens where
ordinary `ViewModel + StateFlow` is clearer.

## First Use Path

Start with [docs/getting-started.md](docs/getting-started.md) if this is your
first Afsm screen.

The short version:

1. Draw the phases first.
2. Put durable screen data in `Data`, not in every phase constructor.
3. Handle UI/repository results as `Event`.
4. Move between phases with `transitionTo(...)`.
5. Start repository work from `command(...)`, usually in `onEnter`.
6. Host the machine from a `ViewModel` with `afsmHost(...)`.

Use [docs/examples.md](docs/examples.md) to choose a real sample. Use
[docs/modeling-rules.md](docs/modeling-rules.md) before modeling a production
screen.

## Minimal Machine

The core mental model:

| Concept | Meaning |
|---|---|
| `Phase` | Finite state diagram node, such as `Editing` or `Saving` |
| `Data` | Extended state data carried across phases, not `android.content.Context` |
| `State` | Android-facing snapshot, normally `AfsmState<Phase, Data>` |
| `Event` | User input or command result |
| `Command` | Host-executed work, such as repository calls or timers |
| `Effect` | Optional UI one-shot output |

Daily choices:

| Situation | Use |
|---|---|
| The business step changes | `transitionTo(Phase.X)` |
| The same step only updates form/error data | `updateData { ... }` |
| An event has named alternatives | `case(label, condition = ...) { ... }` |
| Repository, database, timer, or SDK work must run | `command(label) { ... }`, often in `onEnter` |
| Optional navigation/snackbar/close behavior is needed | `effect(label) { ... }` |
| An expected duplicate or stale event should be harmless | `ignore(reason)`, used sparingly |

Define a small machine first. Do not start with graph/KSP.

```kotlin
import afsm.core.AfsmMachine
import afsm.core.AfsmNoEffect
import afsm.core.AfsmState
import afsm.core.afsmMachine

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
```

`case(...)` branches are checked in declaration order. The first branch whose
`condition` returns `true` handles the event; if none match, the event is
invalid for the current phase. `transitionTo(...)` changes phase. If an event
only updates data or emits an output, handle it with `updateData(...)` or
`effect(...)` without calling `transitionTo(...)`.

Phase-changing transitions run:

```text
onExit -> case actions -> target phase factory -> onEnter
```

Initial state construction does not run `onEnter`. Trigger startup work with an explicit event such as `ScreenEntered`.

## ViewModel

```kotlin
import afsm.runtime.AfsmCommandHandler
import afsm.viewmodel.afsmHost
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow

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
```

If the starting state comes from navigation arguments, a deep link, repository restoration, or `SavedStateHandle`, keep the same `machine` language and pass an explicit initial state:

```kotlin
private val host = afsmHost(
    machine = DraftStateMachine,
    initialState = restoredDraftState,
    commandHandler = draftCommandHandler,
)
```

## Install

Repository-local development:

```kotlin
dependencies {
    implementation(project(":afsm-core"))
    implementation(project(":afsm-runtime"))
    implementation(project(":afsm-viewmodel"))
    implementation(project(":afsm-compose")) // optional
    ksp(project(":afsm-graph-ksp")) // optional graph registry
}
```

Maven Local evaluation:

```bash
./gradlew publishToMavenLocal
./gradlew -p afsm-graph-gradle-plugin publishToMavenLocal # only needed for optional graph plugin
```

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
}
```

Optional Compose and graph tooling:

For Maven Local graph plugin resolution, include `mavenLocal()` in
`pluginManagement.repositories` in `settings.gradle.kts`.

```kotlin
plugins {
    id("com.google.devtools.ksp")
    id("io.github.afsm.graph") version "0.1.0-SNAPSHOT"
}

dependencies {
    implementation("io.github.afsm:afsm-compose:0.1.0-SNAPSHOT")
}
```

The graph plugin adds `afsm-graph-ksp` to the app module by default and
registers `generateAfsmMmd`.

The full local consumer check publishes the plugin and verifies graph generation
from an external Android build. It also compiles the Draft quickstart machine
and ViewModel from [docs/getting-started.md](docs/getting-started.md), so the
first-use example cannot drift from the published Maven Local artifacts.

```bash
./scripts/verify-consumer-smoke.sh --warning-mode all
```

Android consumers must enable AndroidX:

```properties
android.useAndroidX=true
```

`io.github.afsm` is the current pre-release group id. Final Maven Central
coordinates still need product approval.

## Current Status

Afsm is in private internal beta. The local release gate is green, Maven Local
evaluation works, and sample-shop demonstrates Auth, Checkout, and ProductEditor
flows. Stable OSS/Maven Central publishing is intentionally blocked until
license, final coordinates, SCM metadata, signing, and release ownership are
decided. Internal pilot rules are documented in
[docs/release-readiness.md](docs/release-readiness.md).

If you intentionally use a custom non-graphable `AfsmReducer`, name it as a
reducer at the call site:

```kotlin
private val host = afsmHost(
    reducer = LegacyCheckoutReducer(),
    initialState = restoredCheckoutState,
    commandHandler = checkoutCommandHandler,
)
```

## Test First

State machine tests are plain JVM tests.

```kotlin
@Test
fun `SaveClicked enters Saving and emits SaveDraft`() {
    val result = DraftStateMachine.transition(
        state = DraftState(
            phase = DraftPhase.Editing,
            data = DraftData(title = "Plan"),
        ),
        event = DraftEvent.SaveClicked,
    )

    assertEquals(DraftPhase.Saving, result.state.phase)
    assertEquals(listOf(DraftCommand.SaveDraft("Plan")), result.commands)
}
```

Good feature tests usually cover:

- valid phase transition,
- invalid transition,
- command emission,
- effect emission,
- stale command result handling.

See [docs/testing-guide.md](docs/testing-guide.md).

## Long Command Safety

Commands run outside the pure machine and may finish late. Model late results explicitly.

Recommended pattern:

- put a `requestId` or input snapshot in the command,
- return the same id in success/failure events,
- accept the result only if it matches the current active request,
- `ignore` stale results that belong to an older request.

The sample checkout flow uses this policy for mock payment retry.

## Compose Effects

For machines that emit UI effects, collect them at route level. The minimal
draft machine above uses `AfsmNoEffect`, so this pattern applies only when a
feature defines an effect type.

Collect effects at route level. Navigation, snackbar display, focus, scroll, and animation state should stay in UI unless they are part of the business flow.

```kotlin
@Composable
fun EditorRoute(
    viewModel: EditorViewModel,
    onDone: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    CollectAfsmEffects(viewModel.effects) { effect ->
        when (effect) {
            ProductEditorEffect.CloseEditor -> onDone()
        }
    }

    EditorScreen(
        state = state,
        onEvent = viewModel::onEvent,
    )
}
```

Effects are best-effort one-shot outputs. Anything that must survive recreation should be state plus an acknowledgement event.

## Optional MMD Graphs

After the machine works, opt into graph generation.

Current pre-release graph export uses KSP discovery plus the Afsm graph Gradle
plugin. You annotate graphable machines and run one task; the plugin generates
the small unit-test writer internally so app teams do not maintain export tests.
See [docs/graph-generation.md](docs/graph-generation.md) for the full setup.

```kotlin
private typealias DraftMachineType =
    AfsmMachine<DraftState, DraftEvent, DraftCommand, AfsmNoEffect>

@AfsmGraph(
    id = "Draft",
    fileName = "DraftStateMachine.mmd",
)
object DraftStateMachine : DraftMachineType by draftMachine()
```

Pass `label = ...` to `command(...)` or `effect(...)` only when you want that
output to appear in generated diagrams. The label and runtime output stay in the
same statement so the diagram is less likely to drift from behavior.

Apply the plugin:

```kotlin
plugins {
    id("com.google.devtools.ksp")
    id("io.github.afsm.graph") version "0.1.0-SNAPSHOT"
}

afsmGraph {
    variant.set("debug") // default
    outputDir.set(layout.buildDirectory.dir("generated/afsm/mmd")) // default
    mmdOptions.set("Flow") // default; use "Full" for every internal edge
}
```

Sample output:

```bash
./gradlew :sample-shop:generateAfsmMmd
```

```text
sample-shop/build/generated/afsm/mmd/AuthStateMachine.mmd
sample-shop/build/generated/afsm/mmd/CheckoutStateMachine.mmd
sample-shop/build/generated/afsm/mmd/ProductEditorStateMachine.mmd
```

`Flow` hides ordinary unlabeled internal self-loops such as text changes, but
keeps named condition, command, and effect edges. Use `Full` when you need every
internal edge:

```bash
./gradlew :sample-shop:generateAfsmMmd -PafsmMmdOptions=Full
```

## Runtime Policies

- `dispatch(event)` is non-suspending and serialized through FIFO event processing.
- `tryDispatch(event)` returns `false` when the event queue is closed or full.
- Events use a bounded queue, default `64`.
- Command result events use that same bounded event queue; if the queue is
  full, the host throws `AfsmEventQueueOverflowException` instead of suspending
  command processing. If the host is already closed, the result event is
  dropped and logged because the screen lifecycle has ended.
- Commands execute sequentially without blocking later event reduction.
- Commands also use a bounded queue, default `64`; if it fills, the host throws `AfsmCommandQueueOverflowException` instead of suspending the event processor.
- Command results should dispatch typed events back into the host.
- Domain failures should become domain events, not thrown exceptions.
- Unexpected command exceptions use `AfsmCommandFailurePolicy`.
- Invalid transitions throw by default so flow bugs are visible during development.
- `CancellationException` is always rethrown.
- Effects are best-effort one-shot outputs with no replay by default.

## Modules

| Module | Purpose | Android dependency |
|---|---|---|
| `afsm-core` | Pure Kotlin transition types, reducer contract, executable machine DSL, graph metadata | No |
| `afsm-runtime` | Coroutine host, serialized dispatch loop, command execution, effect delivery | No |
| `afsm-viewmodel` | Thin `ViewModel.afsmHost(...)` adapter backed by `viewModelScope` | Yes |
| `afsm-compose` | Lifecycle-aware Compose effect collection helper | Yes |
| `afsm-graph-ksp` | KSP discovery for `@AfsmGraph` machines | No Android runtime dependency |
| `sample-shop` | Compose + Room sample app proving real usage | Yes |
| `consumer-smoke` | Separate Android consumer build that resolves Afsm from Maven Local | Yes |

## Verification

```bash
./scripts/verify-release-local.sh --warning-mode all
```

`consumer-smoke` is intentionally a separate Gradle build. It verifies that an Android project can resolve the Maven Local artifacts without project-module shortcuts.

See [docs/examples.md](docs/examples.md), [docs/afsm-public-api.md](docs/afsm-public-api.md), [docs/restoration-effect-command-policy.md](docs/restoration-effect-command-policy.md), [docs/graph-generation.md](docs/graph-generation.md), [docs/auth-walkthrough.md](docs/auth-walkthrough.md), [docs/checkout-walkthrough.md](docs/checkout-walkthrough.md), [docs/product-editor-walkthrough.md](docs/product-editor-walkthrough.md), [docs/sample-shop-afsm-guide.md](docs/sample-shop-afsm-guide.md), [docs/release-readiness.md](docs/release-readiness.md), [CHANGELOG.md](CHANGELOG.md), and [CONTRIBUTING.md](CONTRIBUTING.md).
