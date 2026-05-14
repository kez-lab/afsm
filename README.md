# Afsm

Afsm is an Android-focused finite state machine toolkit for complex `ViewModel` flows.

Use Afsm when a screen has meaningful phases, retries, async results, invalid transitions, or multi-step behavior. Do not force it onto simple product lists, detail pages, likes, review lists, or basic loading/content/error screens where ordinary `ViewModel + StateFlow` is clearer.

Recommended reading order:

1. Build the minimal machine below.
2. Read [docs/modeling-rules.md](docs/modeling-rules.md) before modeling a real screen.
3. Use `sample-shop` Auth as the smallest real example.
4. Read the generated ProductEditor `.mmd` graph before the ProductEditor source.

## Install

Repository-local development:

```kotlin
dependencies {
    implementation(project(":afsm-core"))
    implementation(project(":afsm-runtime"))
    implementation(project(":afsm-viewmodel"))
    implementation(project(":afsm-compose")) // optional Compose helpers
}
```

Maven Local evaluation:

```bash
./gradlew publishToMavenLocal
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
    implementation("io.github.afsm:afsm-compose:0.1.0-SNAPSHOT") // optional

    ksp("io.github.afsm:afsm-graph-ksp:0.1.0-SNAPSHOT") // optional MMD generation
}
```

Android consumers must enable AndroidX:

```properties
android.useAndroidX=true
```

`io.github.afsm` is the current pre-release group id. Final Maven Central coordinates still need product approval.

## Minimal Machine

The core mental model:

| Concept | Meaning |
|---|---|
| `Phase` | Finite state diagram node, such as `Editing` or `Saving` |
| `Context` | Extended state data carried across phases, not `android.content.Context` |
| `State` | Android-facing snapshot, normally `AfsmState<Phase, Context>` |
| `Event` | User input or command result |
| `Command` | Host-executed work, such as repository calls or timers |
| `Effect` | Optional UI one-shot output |

Define a small machine first. Do not start with graph/KSP.

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

object DraftStateMachine :
    AfsmMachine<DraftState, DraftEvent, DraftCommand, AfsmNoEffect> by draftMachine()

private fun draftMachine() =
    afsmMachine<DraftPhase, DraftContext, DraftEvent, DraftCommand, AfsmNoEffect> {
        initial(
            phase = DraftPhase.Editing,
            context = DraftContext(),
        )

        state(DraftPhase.Editing) {
            on<DraftEvent.TitleChanged> {
                stay {
                    updateContext { copy(title = event.value) }
                }
            }

            on<DraftEvent.SaveClicked> {
                transitionTo(DraftPhase.Saving)
            }
        }

        state(DraftPhase.Saving) {
            onEnter(commandLabels = listOf("SaveDraft")) {
                command(DraftCommand.SaveDraft(context.title))
            }

            on<DraftEvent.Saved> {
                transitionTo(DraftPhase.Saved)
            }
        }

        state(DraftPhase.Saved) {
        }
    }
```

`transitionTo(...)` changes phase. `stay(...)` handles an event without changing phase.

Phase-changing transitions run:

```text
onExit -> transition block -> onEnter
```

Initial state construction does not run `onEnter`. Trigger startup work with an explicit event such as `ScreenEntered`.

## ViewModel

```kotlin
class DraftViewModel(
    private val repository: DraftRepository,
) : ViewModel() {
    private val host = afsmHost(
        machine = DraftStateMachine,
        commandHandler = AfsmCommandHandler { command: DraftCommand, dispatch ->
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

If the starting state comes from navigation arguments, a deep link, repository restoration, or `SavedStateHandle`, keep the same `machine` language and pass an explicit initial state:

```kotlin
private val host = afsmHost(
    machine = DraftStateMachine,
    initialState = restoredDraftState,
    commandHandler = draftCommandHandler,
)
```

If you intentionally use a custom non-graphable `AfsmReducer`, name it as a
reducer at the call site:

```kotlin
private val host = afsmHost(
    reducer = CheckoutStateMachine(),
    initialState = CheckoutState(productId = productId),
    commandHandler = checkoutCommandHandler,
)
```

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

## Test First

State machine tests are plain JVM tests.

```kotlin
@Test
fun `SaveClicked enters Saving and emits SaveDraft`() {
    val result = DraftStateMachine.transition(
        state = DraftState(
            phase = DraftPhase.Editing,
            context = DraftContext(title = "Plan"),
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

## Optional MMD Graphs

After the machine works, opt into graph generation.

```kotlin
private typealias DraftMachineType =
    AfsmMachine<DraftState, DraftEvent, DraftCommand, AfsmNoEffect>

@AfsmGraph(
    id = "Draft",
    fileName = "DraftStateMachine.mmd",
)
object DraftStateMachine : DraftMachineType by draftMachine()
```

Add an export test in the Android app module:

```kotlin
class AfsmMmdExportTest {
    @Test
    fun `writes afsm graphs`() {
        val outputDir = File(
            System.getProperty("afsm.mmd.outputDir")
                ?: "build/generated/afsm/mmd",
        )

        AfsmMmdWriter.writeAll(
            registry = AfsmGeneratedGraphRegistry,
            outputDir = outputDir,
            options = AfsmMmdOptions.Flow,
        )
    }
}
```

Wire a Gradle task:

```kotlin
tasks.withType<Test>().configureEach {
    systemProperty(
        "afsm.mmd.outputDir",
        layout.buildDirectory.dir("generated/afsm/mmd").get().asFile.absolutePath,
    )
    outputs.dir(layout.buildDirectory.dir("generated/afsm/mmd"))
}

tasks.register("generateAfsmMmd") {
    group = "documentation"
    description = "Generates Afsm state machine .mmd graph files."
    dependsOn("testDebugUnitTest")
    outputs.dir(layout.buildDirectory.dir("generated/afsm/mmd"))
}
```

Sample output:

```bash
./gradlew :sample-shop:generateAfsmMmd
```

```text
sample-shop/build/generated/afsm/mmd/AuthStateMachine.mmd
sample-shop/build/generated/afsm/mmd/ProductEditorStateMachine.mmd
```

`AfsmMmdOptions.Flow` hides ordinary internal self-loops such as text changes. Use `AfsmMmdOptions.Full` when you need the complete topology.

## Runtime Policies

- `dispatch(event)` is non-suspending and serialized through FIFO event processing.
- `tryDispatch(event)` returns `false` when the event queue is closed or full.
- Events use a bounded queue, default `64`.
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
./scripts/verify-release-local.sh
```

`consumer-smoke` is intentionally a separate Gradle build. It verifies that an Android project can resolve the Maven Local artifacts without project-module shortcuts.

See [docs/afsm-public-api.md](docs/afsm-public-api.md), [docs/sample-shop-afsm-guide.md](docs/sample-shop-afsm-guide.md), [docs/release-readiness.md](docs/release-readiness.md), [CHANGELOG.md](CHANGELOG.md), and [CONTRIBUTING.md](CONTRIBUTING.md).
