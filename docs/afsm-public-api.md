# Afsm Public API

This page describes the current pre-release public API surface.

For Android restoration, one-shot effects, command failures, request ids, and
queue pressure policy, read
[restoration-effect-command-policy.md](restoration-effect-command-policy.md).

For complete Android examples, read [examples.md](examples.md),
[auth-walkthrough.md](auth-walkthrough.md),
[checkout-walkthrough.md](checkout-walkthrough.md), and
[product-editor-walkthrough.md](product-editor-walkthrough.md). For `.mmd`
setup, read [graph-generation.md](graph-generation.md).

## Android-First Path

Most Android feature code only needs three public types:

```kotlin
typealias ScreenState = AfsmState<ScreenPhase, ScreenData>
typealias ScreenMachine = AfsmMachine<ScreenState, ScreenEvent, ScreenCommand, ScreenEffect>

object ScreenStateMachine : ScreenMachine by screenMachine()
```

Machines that do not emit host-executed work should use `AfsmNoCommand` as
their `Command` type. Machines that do not emit UI one-shot output should use
`AfsmNoEffect` as their `Effect` type.

```kotlin
typealias ToggleMachine =
    AfsmMachine<ToggleState, ToggleEvent, AfsmNoCommand, AfsmNoEffect>
```

The ViewModel hosts that machine:

```kotlin
private val host = afsmHost(
    machine = ScreenStateMachine,
    commandHandler = { command: ScreenCommand, dispatch ->
        // repository/use-case work
        // dispatch(result event)
    },
)
```

Start with `afsmMachine { ... }`, `AfsmState<Phase, Data>`, and
`ViewModel.afsmHost(...)`. Treat `AfsmReducer`, `AfsmTransition`, and
`AfsmDecision` as advanced reference concepts unless you are writing a custom
reducer or library integration.

Everyday DSL choices:

| Situation | Use |
|---|---|
| The business step changes | `transitionTo(Phase.X)` |
| The same step only updates form/error data | `updateData { ... }` |
| An event has named alternatives | `case(label, condition = ...) { ... }` |
| Repository, database, timer, or SDK work must run | `command(label) { ... }`, often in `onEnter` |
| Optional navigation/snackbar/close behavior is needed | `effect(label) { ... }` |
| An expected duplicate or stale event should be harmless | `ignore(reason)`, used sparingly |

## Coordinates

Required dependencies:

```kotlin
implementation("io.github.afsm:afsm-core:0.1.0-SNAPSHOT")
implementation("io.github.afsm:afsm-runtime:0.1.0-SNAPSHOT")
implementation("io.github.afsm:afsm-viewmodel:0.1.0-SNAPSHOT")
```

Optional test, Compose, and graph tooling:

```kotlin
testImplementation("io.github.afsm:afsm-test:0.1.0-SNAPSHOT")
```

```kotlin
implementation("io.github.afsm:afsm-compose:0.1.0-SNAPSHOT")
```

Add `afsm-compose` only for Compose routes that collect machine effects. A
machine that never emits UI one-shots should use `AfsmNoEffect` and does not
need the Compose helper module.

```kotlin
plugins {
    id("com.google.devtools.ksp")
    id("io.github.afsm.graph") version "0.1.0-SNAPSHOT"
}
```

Generate local artifacts:

```bash
./gradlew publishToMavenLocal
./gradlew -p afsm-graph-gradle-plugin publishToMavenLocal # optional graph plugin
```

Verify from a separate Android consumer build:

```bash
./scripts/verify-consumer-smoke.sh
```

The graph Gradle plugin adds `io.github.afsm:afsm-graph-ksp:0.1.0-SNAPSHOT`
to the app module by default when `com.google.devtools.ksp` is applied. That
default is generated from the graph plugin version, so a published
`io.github.afsm.graph` plugin and its KSP processor stay on the same Afsm
version unless the consumer explicitly overrides `processorDependency`.

## afsm-core

### AfsmTransition

`AfsmTransition` is the result of reducing one `state + event` pair. Create it
through `Afsm` helpers or `AfsmTransition` factory functions, not a raw public
constructor.

```kotlin
Afsm.transitioned(state, commands, effects)
Afsm.ignore(state, reason)
Afsm.invalid(state, reason)
AfsmTransition.handled(state, commands, effects, reason) // low-level reducer escape hatch
```

The constructor is intentionally not public so `Ignored` and `Invalid` decisions
cannot accidentally carry commands, effects, or changed state output.
For graphable `afsmMachine { ... }` code, do not call a `stay` helper.
Handling a DSL case without `transitionTo(...)` produces a `Handled` decision.
If one accepted event needs multiple actions, keep them in the same
`case { ... }`. Top-level shorthand calls such as `updateData(...)` and
`transitionTo(...)` are separate alternatives; they are not merged into one
transition.

### AfsmDecision

| Decision | Meaning | Runtime behavior |
|---|---|---|
| `Transitioned` | State transition accepted | Publish state, emit effects, execute commands |
| `Handled` | Event handled without phase change | Publish state, emit effects, execute commands |
| `Ignored` | Event expected but intentionally no-op | Drop state/outputs and optionally log accidental outputs |
| `Invalid` | Event invalid in current state | Record or throw according to `AfsmInvalidTransitionPolicy` |

### AfsmReducer

```kotlin
fun interface AfsmReducer<S : Any, E : Any, C : Any, F : Any> {
    fun transition(state: S, event: E): AfsmTransition<S, C, F>
}
```

Use this for custom state shapes or low-level integrations.

### AfsmMachine

```kotlin
interface AfsmMachine<S : Any, E : Any, C : Any, F : Any> :
    AfsmReducer<S, E, C, F>,
    AfsmGraphSource {
    val initialState: S
    val topology: AfsmTopology
}
```

Use this at feature boundaries once the state type has been named.

```kotlin
typealias ProductEditorState =
    AfsmState<ProductEditorPhase, ProductEditorData>

private typealias ProductEditorMachine =
    AfsmMachine<ProductEditorState, ProductEditorEvent, ProductEditorCommand, ProductEditorEffect>
```

### AfsmState

```kotlin
data class AfsmState<P : Any, D : Any>(
    val phase: P,
    val data: D,
)
```

`phase` is the finite graph state. `data` is durable screen data carried across
phases. It is not `android.content.Context`.

### DSL

```kotlin
afsmMachine<Phase, Data, Event, Command, Effect> {
    initial(phase = Phase.Editing, data = FormData())

    phase(Phase.Editing) {
        on<Event.SubmitClicked> {
            case(
                label = "valid form",
                condition = { data.canSubmitForm() },
            ) {
                updateData { copy(errorMessage = null) }
                transitionTo(Phase.Submitting)
            }

            case(
                label = "invalid form",
                condition = { data.hasSubmitError() },
            ) {
                updateData { copy(errorMessage = "Invalid form") }
            }
        }
    }

    phase(Phase.Submitting) {
        onEnter {
            command(label = "Submit") {
                Command.Submit(data.form)
            }
        }
    }
}
```

Emit long-running work either from the accepted `case` or from the target
phase's `onEnter`, not both. Prefer `onEnter` when the work starts because the
machine entered a work phase such as `Submitting`.

| API | Meaning |
|---|---|
| `initial(phase, data)` | Initial state value; does not run `onEnter` |
| `phase(phase) { ... }` | Exact phase scope |
| `phase(phase)` | Exact phase declaration with no handlers, useful for terminal states |
| `phase<PayloadPhase> { ... }` | Payload phase scope |
| `phase<PayloadPhase>()` | Payload phase declaration with no handlers |
| `phase(phaseType = PayloadPhase::class) { ... }` | Non-inline payload phase scope |
| `on<Event>()` | Event-specific branch scope |
| `on(eventType = Event::class)` | Non-inline event-specific branch scope |
| `case(label, condition = ...) { ... }` | Named branch for a domain condition |
| `transitionTo(phase)` | Phase change only |
| `transitionTo<PayloadPhase> { ... }` | Phase change to payload phase |
| `transitionTo(phaseType = PayloadPhase::class, phase = { ... })` | Non-inline payload phase change |
| `updateData { ... }` | Handles event by immutably updating data |
| `updateData { data, event -> ... }` | Data update that uses the typed event payload |
| `ignore(reason)` | Expected no-op event; no graph edge |
| `invalid(reason)` | Explicit invalid event; no graph edge |
| `command(label = ...) { ... }` | Host-executed work output from a case |
| `effect(label = ...) { ... }` | UI-side one-shot output from a case |
| `onEnter { ... }` | Declares actions that run after entering a phase |
| `onExit { ... }` | Declares actions that run before leaving a phase |
| `command(label = ...) { ... }` in `onEnter`/`onExit` | Host work plus optional graph label in one statement |
| `effect(label = ...) { ... }` in `onEnter`/`onExit` | UI one-shot plus optional graph label in one statement |

`case(...)` branches are evaluated in declaration order. The first branch whose
`condition` returns `true` handles the event. If no branch matches, the event is
invalid in the current phase. A named case becomes the generated transition's
condition label, including no-transition cases such as validation failures.

Conditions run with a read-only scope containing typed `phase`, typed `event`,
and current `data`. They cannot update data or emit outputs. Payload phase
factories in `transitionTo<PayloadPhase> { ... }` are also read-only and should
only create the target phase.

Entry and exit blocks are declaration blocks. `command(label = ...) { ... }`
and `effect(label = ...) { ... }` record graph labels when the machine is built,
then run their value factories later with `phase` and `data` from
`AfsmPhaseActionScope`.

Use `phase(phase)` for singleton/data-object phases. For payload phase classes,
prefer `phase<PayloadPhase>()` or `phase<PayloadPhase> { ... }` so the machine
matches any payload instance rather than one exact value.

Phase-changing transition order:

```text
onExit -> case actions -> target phase factory -> onEnter
```

### Topology and MMD

```kotlin
data class AfsmTopology(
    val states: List<AfsmTopologyState>,
    val transitions: List<AfsmTopologyTransition>,
    val initialStateId: String? = null,
)

fun AfsmTopology.toMmd(
    options: AfsmMmdOptions = AfsmMmdOptions.Flow,
): String
```

`AfsmMmdOptions.Flow` hides ordinary unlabeled internal self-loops. Named
condition, command, and effect edges remain visible. Use `AfsmMmdOptions.Full`
for complete topology.

`AfsmTopologyState` can include entry/exit command/effect labels. In the DSL,
those labels come from the same `command(label = ...) { ... }` and
`effect(label = ...) { ... }` statements that emit runtime outputs.

The `io.github.afsm.graph` Gradle plugin is the preferred Android app-module
entry point for `.mmd` output. It generates the export test internally and
registers `generateAfsmMmd`; app modules should not maintain a hand-written
`AfsmMmdExportTest`.

Set `afsmGraph { mmdOptions.set("Full") }` or run with
`-PafsmMmdOptions=Full` when you need full topology output.

## afsm-runtime

```kotlin
class AfsmHost<S : Any, E : Any, C : Any, F : Any>(
    initialState: S,
    reducer: AfsmReducer<S, E, C, F>,
    commandHandler: AfsmCommandHandler<C, E>,
    scope: CoroutineScope,
    config: AfsmConfig = AfsmConfig(),
)
```

Public outputs:

```kotlin
val state: StateFlow<S>
val effects: Flow<F>
fun dispatch(event: E)
fun tryDispatch(event: E): Boolean
fun close()
```

Runtime guarantees:

- `dispatch(event)` is non-suspending.
- Events are processed serially in FIFO order.
- Command result events use the same bounded event queue. If a command result
  cannot be queued, the host throws `AfsmEventQueueOverflowException` instead
  of blocking the sequential command processor. If the host is already closed,
  the result event is dropped and logged because the screen lifecycle has ended.
- Commands are executed sequentially without blocking later event reduction.
- If the command queue fills, the host throws `AfsmCommandQueueOverflowException` instead of suspending the event processor indefinitely.
- Commands may dispatch follow-up events.
- Follow-up events are queued, not re-entered recursively.
- `tryDispatch(event)` returns `false` when the host is closed or the event queue is full.

### AfsmConfig

Start with the default `AfsmConfig()` in ordinary feature ViewModels. Change it
only when the host runtime policy is intentionally different from the default
development behavior. Expected product failures should still be modeled as
typed result events, not as config changes.

```kotlin
class AfsmConfig(
    val invalidTransitionPolicy: AfsmInvalidTransitionPolicy =
        AfsmInvalidTransitionPolicy.Throw,
    val commandExecutionPolicy: AfsmCommandExecutionPolicy =
        AfsmCommandExecutionPolicy.Sequential,
    val commandFailurePolicy: AfsmCommandFailurePolicy =
        AfsmCommandFailurePolicy.Throw,
    val effectDelivery: AfsmEffectDelivery =
        AfsmEffectDelivery.Default,
    val eventQueueCapacity: Int = 64,
    val commandQueueCapacity: Int = 64,
    val logger: AfsmLogger =
        AfsmLogger.None,
)
```

Command failure policy:

- `Throw`: fail the processing coroutine. This is the default for programmer errors.
- `Record`: log `AfsmDiagnostic` and continue processing later events.
- `CancellationException` is always rethrown.

Command queue overflow:

- `AfsmCommandQueueOverflowException` means a machine emitted accepted commands faster than the bounded host queue could accept them.
- Prefer fewer, coarser commands or increase `commandQueueCapacity`.
- Do not use command overflow as domain failure handling; domain failures should still become typed result events.

## afsm-viewmodel

```kotlin
fun <S : Any, E : Any, C : Any, F : Any> ViewModel.afsmHost(
    machine: AfsmMachine<S, E, C, F>,
    commandHandler: AfsmCommandHandler<C, E> = AfsmCommandHandler.none(),
    config: AfsmConfig = AfsmConfig(),
): AfsmHost<S, E, C, F>

fun <S : Any, E : Any, C : Any, F : Any> ViewModel.afsmHost(
    machine: AfsmMachine<S, E, C, F>,
    initialState: S,
    commandHandler: AfsmCommandHandler<C, E> = AfsmCommandHandler.none(),
    config: AfsmConfig = AfsmConfig(),
): AfsmHost<S, E, C, F>

fun <S : Any, E : Any, C : Any, F : Any> ViewModel.afsmHost(
    reducer: AfsmReducer<S, E, C, F>,
    initialState: S,
    commandHandler: AfsmCommandHandler<C, E> = AfsmCommandHandler.none(),
    config: AfsmConfig = AfsmConfig(),
): AfsmHost<S, E, C, F>
```

Use `machine` for the standard path. Use `machine + initialState` when the
starting state is dynamic. Use `reducer + initialState` only for custom
non-graphable reducer escape hatches.

The signatures show `AfsmCommandHandler<C, E>` because that is the exact API
type. Kotlin callers should usually pass a direct lambda:

```kotlin
commandHandler = { command: ScreenCommand, dispatch ->
    // execute host work
    // dispatch(result event)
}
```

The default `AfsmCommandHandler.none()` is only for machines that never emit
commands. If a machine emits commands and the handler is omitted, those
commands are intentionally ignored.

## afsm-compose

```kotlin
@Composable
fun <F : Any> CollectAfsmEffects(
    effects: Flow<F>,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    onEffect: suspend (F) -> Unit,
)
```

Use this in route-level composables for UI one-shot behavior such as navigation
or snackbar display. Keep required product progress in state; effects are
best-effort one-shot outputs.

## afsm-test

`afsm-test` contains Kotlin-only helpers for common transition assertions:

```kotlin
result
    .assertTransitioned()
    .assertPhase(ScreenPhase.Saving)
    .assertCommands(ScreenCommand.Save)
```

Use these helpers in unit tests when they make the behavioral expectation
clearer than inspecting `result.decision`, `result.state.phase`,
`result.commands`, and `result.effects` directly.

Android `ViewModel` test rules, fake repositories, and dispatcher rules remain
consumer-owned test fixtures.

## afsm-graph-ksp

```kotlin
@AfsmGraph(
    id = "ProductEditor",
    fileName = "ProductEditorStateMachine.mmd",
)
object ProductEditorStateMachine : ProductEditorMachine by productEditorMachine()
```

KSP discovers annotated classes or objects that implement both `AfsmReducer`
and `AfsmGraphSource`. `AfsmMachine` satisfies both automatically.

MVP constructor policy:

- annotated classes must be `object`s, or
- classes must be constructible with no required constructor parameters.

## Removed Pre-Release Names

- `Afsm.transitionTo(...)` -> use `Afsm.transitioned(...)`
- `AfsmDecision.Stayed` -> use `AfsmDecision.Handled`
- `AfsmTransition.stayed(...)` -> use `AfsmTransition.handled(...)`
- DSL `state(...)` -> use `phase(...)`
- DSL `updateContext(...)` -> use `updateData(...)`
- `AfsmState.context` -> use `AfsmState.data`
- `AfsmPhaseMachine` -> use the returned `AfsmMachine<AfsmState<Phase, Data>, Event, Command, Effect>`
- `AfsmStateMachine` -> use `AfsmReducer`
- `AfsmStateChart` -> use `AfsmMachine` / `afsmMachine`
- `afsmStateChart` -> use `afsmMachine`
- `AfsmStateChartMachine` -> use `AfsmMachine`
- `AfsmChartState` -> use `AfsmState`
- `AfsmGraphReducer` -> use `AfsmMachine<State, Event, Command, Effect>`
