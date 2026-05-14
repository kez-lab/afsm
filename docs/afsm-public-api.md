# Afsm Public API

This page describes the current pre-release public API surface.

For Android restoration, one-shot effects, command failures, request ids, and
queue pressure policy, read
[restoration-effect-command-policy.md](restoration-effect-command-policy.md).

For complete Android examples, read [examples.md](examples.md) and
[checkout-walkthrough.md](checkout-walkthrough.md).

## Coordinates

```kotlin
implementation("io.github.afsm:afsm-core:0.1.0-SNAPSHOT")
implementation("io.github.afsm:afsm-runtime:0.1.0-SNAPSHOT")
implementation("io.github.afsm:afsm-viewmodel:0.1.0-SNAPSHOT")
implementation("io.github.afsm:afsm-compose:0.1.0-SNAPSHOT")
ksp("io.github.afsm:afsm-graph-ksp:0.1.0-SNAPSHOT")
```

Generate local artifacts:

```bash
./gradlew publishToMavenLocal
```

Verify from a separate Android consumer build:

```bash
./scripts/verify-consumer-smoke.sh
```

## afsm-core

### AfsmTransition

`AfsmTransition` is the result of reducing one `state + event` pair. Create it
through `Afsm` helpers or `AfsmTransition` factory functions, not a raw public
constructor.

```kotlin
Afsm.transitionTo(state, commands, effects)
Afsm.stay(state, commands, effects, reason)
Afsm.ignore(state, reason)
Afsm.invalid(state, reason)
```

The constructor is intentionally not public so `Ignored` and `Invalid` decisions
cannot accidentally carry commands, effects, or changed state output.

### AfsmDecision

| Decision | Meaning | Runtime behavior |
|---|---|---|
| `Transitioned` | State transition accepted | Publish state, emit effects, execute commands |
| `Stayed` | Event handled without phase change | Publish state, emit effects, execute commands |
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
    AfsmState<ProductEditorPhase, ProductEditorContext>

private typealias ProductEditorMachine =
    AfsmMachine<ProductEditorState, ProductEditorEvent, ProductEditorCommand, ProductEditorEffect>
```

### AfsmPhaseMachine

```kotlin
interface AfsmPhaseMachine<P : Any, X : Any, E : Any, C : Any, F : Any> :
    AfsmMachine<AfsmState<P, X>, E, C, F>
```

`afsmMachine { ... }` builds this type. Most app code should expose it through
a feature-local `AfsmMachine<State, Event, Command, Effect>` alias.

### AfsmState

```kotlin
data class AfsmState<P : Any, X : Any>(
    val phase: P,
    val context: X,
)
```

`phase` is the finite graph state. `context` is extended state data, not
`android.content.Context`.

### DSL

```kotlin
afsmMachine<Phase, Context, Event, Command, Effect> {
    initial(phase = Phase.Editing, context = Context())

    state(Phase.Editing) {
        on<Event.SubmitClicked> {
            transitionTo(
                phase = Phase.Submitting,
                guardLabel = "valid form",
                commandLabels = listOf("Submit"),
                guard = { context.form.isValid() },
            ) {
                updateContext { copy(errorMessage = null) }
            }

            otherwise(label = "invalid form") {
                updateContext { copy(errorMessage = "Invalid form") }
            }
        }
    }

    state(Phase.Submitting) {
        onEnter(commandLabels = listOf("Submit")) {
            command(Command.Submit(context.form))
        }
    }
}
```

| API | Meaning |
|---|---|
| `initial(phase, context)` | Initial state value; does not run `onEnter` |
| `state(phase)` | Exact phase scope |
| `state<PayloadPhase>()` | Payload phase scope |
| `state(phaseType = PayloadPhase::class)` | Non-inline payload phase scope |
| `on<Event>()` | Event-specific branch scope |
| `on(eventType = Event::class)` | Non-inline event-specific branch scope |
| `transitionTo(phase)` | Phase-changing transition |
| `transitionTo<PayloadPhase>(phase = { ... })` | Phase-changing transition to payload phase |
| `transitionTo(phaseType = PayloadPhase::class, phase = { ... })` | Non-inline payload transition |
| `stay { ... }` | Handled internal branch with no phase change |
| `otherwise(label = ...) { ... }` | Fallback internal branch after guards fail |
| `ignore(reason)` | Expected no-op event; no graph edge |
| `invalid(reason)` | Explicit invalid event; no graph edge |
| `updateContext { ... }` | Immutable extended-state update |
| `command(command)` | Host-executed work output |
| `effect(effect)` | UI-side one-shot output |
| `onEnter(commandLabels = ...) { ... }` | Runs after entering a phase |
| `onExit(commandLabels = ...) { ... }` | Runs before leaving a phase |

Phase-changing transition order:

```text
onExit -> transition block -> onEnter
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

`AfsmMmdOptions.Flow` hides ordinary internal self-loops. Use
`AfsmMmdOptions.Full` for complete topology.

`AfsmTopologyState` can include entry/exit command/effect labels. These labels
are metadata only; runtime commands/effects must still be emitted in DSL blocks.

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
- Commands are executed sequentially without blocking later event reduction.
- If the command queue fills, the host throws `AfsmCommandQueueOverflowException` instead of suspending the event processor indefinitely.
- Commands may dispatch follow-up events.
- Follow-up events are queued, not re-entered recursively.
- `tryDispatch(event)` returns `false` when the host is closed or the event queue is full.

### AfsmConfig

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
or snackbar display.

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

- `AfsmStateMachine` -> use `AfsmReducer`
- `AfsmStateChart` -> use `AfsmPhaseMachine` / `afsmMachine`
- `afsmStateChart` -> use `afsmMachine`
- `AfsmStateChartMachine` -> use `AfsmPhaseMachine`
- `AfsmChartState` -> use `AfsmState`
- `AfsmGraphReducer` -> use `AfsmMachine<State, Event, Command, Effect>`
