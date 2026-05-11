# Afsm Public API

This page describes the current public API surface intended for pre-release stabilization.

Current pre-release local Maven coordinates:

```kotlin
implementation("io.github.afsm:afsm-core:0.1.0-SNAPSHOT")
implementation("io.github.afsm:afsm-runtime:0.1.0-SNAPSHOT")
implementation("io.github.afsm:afsm-viewmodel:0.1.0-SNAPSHOT")
ksp("io.github.afsm:afsm-graph-ksp:0.1.0-SNAPSHOT")
```

Generate these artifacts with:

```bash
./gradlew publishToMavenLocal
```

Verify the published artifacts from a separate Android consumer build:

```bash
./scripts/verify-consumer-smoke.sh
```

Removed pre-release aliases:

- `AfsmStateMachine` -> use `AfsmReducer`
- `AfsmStateChart` -> use `AfsmMachine`
- `afsmStateChart` -> use `afsmMachine`
- `AfsmStateChartMachine` -> use `AfsmMachineAdapter`
- `AfsmChartState` -> use `AfsmState`

Because Afsm has not been published yet, these aliases are removed before the first public release instead of kept as deprecated API.

## afsm-core

### AfsmTransition

```kotlin
data class AfsmTransition<S : Any, C : Any, F : Any>(
    val state: S,
    val commands: List<C> = emptyList(),
    val effects: List<F> = emptyList(),
    val decision: AfsmDecision = AfsmDecision.Transitioned,
)
```

Use feature-local typealiases to keep signatures readable:

```kotlin
typealias CheckoutTransition =
    AfsmTransition<CheckoutState, CheckoutCommand, CheckoutEffect>
```

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

Use this for simple or custom state shapes.

### AfsmState

```kotlin
data class AfsmState<P : Any, X : Any>(
    val phase: P,
    val context: X,
)
```

Use this for graphable machines where `phase` is the finite graph state and `context` is extended data.

### AfsmMachine

```kotlin
interface AfsmMachine<P : Any, X : Any, E : Any, C : Any, F : Any> :
    AfsmReducer<AfsmState<P, X>, E, C, F>,
    AfsmGraphSource {
    val initialState: AfsmState<P, X>
    override val topology: AfsmTopology
}
```

Build it with:

```kotlin
afsmMachine<Phase, Context, Event, Command, Effect> {
    initial(phase = Phase.Editing, context = Context())

    state(Phase.Editing) {
        on<Event.SubmitClicked> {
            transitionTo(
                phase = Phase.Submitting,
                guardLabel = "valid form",
                guard = { context.form.isValid() },
            ) {
                updateContext { copy(errorMessage = null) }
            }

            otherwise {
                updateContext { copy(errorMessage = "Invalid form") }
            }
        }
    }

    state(Phase.Submitting) {
        onEnter {
            command(Command.Submit(context.form))
        }

        on<Event.SubmitSucceeded> {
            transitionTo(Phase.Completed)
        }
    }
}
```

### DSL Semantics

| API | Meaning |
|---|---|
| `initial(phase, context)` | Initial state value; does not run `onEnter` |
| `state(phase)` | Exact phase scope |
| `state<PayloadPhase>()` | Payload phase scope |
| `on<Event>()` | Event-specific branch scope |
| `transitionTo(phase)` | Phase-changing transition |
| `transitionTo<PayloadPhase>(phase = { ... })` | Phase-changing transition to payload phase |
| `stay { ... }` | Handled internal branch with no phase change |
| `otherwise { ... }` | Fallback internal branch after guards fail |
| `ignore(reason)` | Expected no-op event; no graph edge |
| `invalid(reason)` | Explicit invalid event; no graph edge |
| `updateContext { ... }` | Immutable extended-state update |
| `command(command)` | Host-executed work output |
| `effect(effect)` | UI-side one-shot output |
| `onEnter { ... }` | Runs after entering a phase |
| `onExit { ... }` | Runs before leaving a phase |

Phase-changing transition order:

```text
onExit -> transition block -> onEnter
```

### Definition Validation

`afsmMachine { ... }` throws `AfsmDefinitionException` when:

- no state is declared,
- the initial phase has no matching state declaration,
- a state label is duplicated,
- an event handler is duplicated within a state,
- a transition target has no matching state declaration.

### Topology

```kotlin
data class AfsmTopology(
    val states: List<AfsmTopologyState>,
    val transitions: List<AfsmTopologyTransition>,
)

fun AfsmTopology.toMmd(): String
```

`AfsmTopologyTransition` includes:

- `from`
- `event`
- `to`
- `guardLabel`
- `commandLabels`
- `effectLabels`
- `kind`
- `isFallback`

## afsm-runtime

### AfsmHost

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
fun close()
```

Runtime guarantees:

- `dispatch(event)` is non-suspending.
- Events are processed serially in FIFO order.
- Commands are executed sequentially.
- Commands may dispatch follow-up events.
- Follow-up events are queued, not re-entered recursively.

### AfsmCommandHandler

```kotlin
fun interface AfsmCommandHandler<C : Any, E : Any> {
    suspend fun handle(command: C, dispatch: suspend (E) -> Unit)
}
```

Command handlers should convert domain results and failures into typed events.

### AfsmConfig

```kotlin
class AfsmConfig(
    val invalidTransitionPolicy: AfsmInvalidTransitionPolicy =
        AfsmInvalidTransitionPolicy.Record,
    val commandExecutionPolicy: AfsmCommandExecutionPolicy =
        AfsmCommandExecutionPolicy.Sequential,
    val commandFailurePolicy: AfsmCommandFailurePolicy =
        AfsmCommandFailurePolicy.Throw,
    val effectDelivery: AfsmEffectDelivery =
        AfsmEffectDelivery.Default,
    val logger: AfsmLogger =
        AfsmLogger.None,
)
```

Command failure policy:

- `Throw`: fail the processing coroutine. This is the default for programmer errors.
- `Record`: log `AfsmDiagnostic` and continue processing later events.
- `CancellationException` is always rethrown.

## afsm-viewmodel

```kotlin
fun <S : Any, E : Any, C : Any, F : Any> ViewModel.afsmHost(
    initialState: S,
    reducer: AfsmReducer<S, E, C, F>,
    commandHandler: AfsmCommandHandler<C, E> = AfsmCommandHandler.none(),
    config: AfsmConfig = AfsmConfig(),
): AfsmHost<S, E, C, F>
```

The helper only supplies `viewModelScope`. It does not own navigation, DI, Compose policy, or `SavedStateHandle`.

## afsm-graph-ksp

```kotlin
@AfsmGraph(
    id = "ProductEditor",
    fileName = "ProductEditorStateMachine.mmd",
)
class ProductEditorStateMachine(
    machine: ProductEditorMachine = productEditorMachine(),
) : ProductEditorMachine by machine
```

KSP discovers annotated classes that implement both:

- `AfsmReducer<*, *, *, *>`
- `AfsmGraphSource`

and generates a module-local graph registry.

MVP constructor policy:

- annotated classes must be `object`s, or
- classes must be constructible with no required constructor parameters.

## Release API Decision

Pre-release aliases are removed before public documentation and before publishing. After the first published artifact, source/binary compatibility rules must become stricter and breaking renames should require a major version bump.
