# Afsm Public API

Status: public pre-1.0 beta. Afsm may make breaking API changes when usability
or safety evidence justifies them; releases carry API, documentation, and
migration updates together.

## Core types

```kotlin
data class AfsmState<P : Any, D : Any>(
    val phase: P,
    val data: D,
)

fun interface AfsmReducer<S : Any, E : Any, C : Any> {
    fun transition(state: S, event: E): AfsmTransition<S, C>
}

interface AfsmMachine<S : Any, E : Any, C : Any> :
    AfsmReducer<S, E, C>,
    AfsmGraphSource

interface AfsmDefaultMachine<S : Any, E : Any, C : Any> :
    AfsmMachine<S, E, C> {
    val initialState: S
}
```

Use `AfsmMachine` when runtime input or restoration supplies the initial state.
Use `AfsmDefaultMachine` when a genuine static default exists.

## DSL

```kotlin
afsmMachine<Phase, Data, Event, Command> {
    initial(Phase.Idle, Data())

    phase(Phase.Idle) {
        on<Event.Start> {
            updateData { copy(error = null) }
            command("StartWork") { Command.StartWork }
            transitionTo(Phase.Working)
        }
    }
}
```

Main operations:

| API | Purpose |
|---|---|
| `initial(phase, data)` | Define a static initial state |
| `phase(value) { ... }` | Register rules for an object-like phase |
| `phase<Phase.Payload> { ... }` | Register rules for a payload phase |
| `on<Event.Type> { ... }` | Handle an event in the current phase |
| `updateData { ... }` | Update extended state data |
| `transitionTo(...)` | Change phase |
| `command(label) { ... }` | Emit host-executed work |
| `case(label, condition) { ... }` | Name a real conditional branch |
| `ignore(reason, condition?)` | Accept an intentional no-op |
| `invalid(reason, condition?)` | Explicitly reject an event |
| `onEnter`, `onExit` | Attach phase lifecycle data/command work |
| `invoke(key, label) { ... }` | Start phase-owned cancellable command work |

## Transition result

```kotlin
class AfsmTransition<S : Any, C : Any> {
    val state: S
    val commands: List<C>
    val commandInvocations: List<AfsmCommandInvocation<C>>
    val decision: AfsmDecision
}
```

`Transitioned` means phase changed. `Handled` means an accepted rule kept the
phase. `Ignored` is a recognized no-op. `Invalid` is rejected by the current
flow contract.

## Runtime

```kotlin
class AfsmHost<S : Any, E : Any, C : Any>(
    initialState: S,
    reducer: AfsmReducer<S, E, C>,
    commandHandler: AfsmCommandHandler<C, E>,
    scope: CoroutineScope,
    config: AfsmConfig = AfsmConfig(),
) {
    val state: StateFlow<S>
    val isActive: Boolean
    fun send(event: E)
    fun trySend(event: E): Boolean
    operator fun invoke(event: E)
    fun close()
}
```

The host serializes events, publishes accepted state before scheduling command
work, executes commands sequentially, and returns command results through the
handler's `send` capability.

`isActive` is `false` once the host stopped accepting events. A host stops when
it is closed, when the owning scope is cancelled, or when a throwing policy
ended a processing coroutine; the last case also records an
`AfsmDiagnosticCode.HostStopped` diagnostic.

### Failure policies

```kotlin
class AfsmConfig(
    val invalidTransitionPolicy: AfsmInvalidTransitionPolicy = Record,
    val commandExecutionPolicy: AfsmCommandExecutionPolicy = Sequential,
    val commandFailurePolicy: AfsmCommandFailurePolicy = Record,
    val overflowPolicy: AfsmOverflowPolicy = Record,
    val commandContext: CoroutineContext = EmptyCoroutineContext,
    val eventQueueCapacity: Int = 64,
    val commandQueueCapacity: Int = 64,
    val diagnosticDataPolicy: AfsmDiagnosticDataPolicy = TypesOnly,
    val logger: AfsmLogger = AfsmLogger.None,
) {
    companion object {
        fun strict(/* ... */): AfsmConfig
    }
}
```

| Policy | Covers | `Record` | `Throw` |
|---|---|---|---|
| `invalidTransitionPolicy` | `Invalid` decisions, reducer exceptions, duplicate invocation keys | diagnostic, host stays usable | exception stops the host |
| `commandFailurePolicy` | exceptions a command handler did not model | diagnostic, host stays usable | exception stops the host |
| `overflowPolicy` | full event and command queues | diagnostic, work dropped | overflow exception |

`AfsmConfig.strict()` selects `Throw` for all three. Use it in debug builds and
tests; keep the recording defaults in release builds so one flow mistake cannot
freeze a shipped screen.

`commandContext` is added to the coroutine context while a command handler runs.
Event reduction always stays on the host scope so transitions stay serialized.

Diagnostics carry a stable `AfsmDiagnosticCode`: `InvalidTransition`,
`ReducerFailure`, `IgnoredTransitionOutputDropped`, `CommandFailure`,
`DuplicateInvocationKey`, `EventDropped`, `CommandQueueOverflow`,
`CommandResultQueueOverflow`, `CommandResultDroppedHostClosed`, and
`HostStopped`.

## ViewModel integration

```kotlin
fun <S : Any, E : Any, C : Any> ViewModel.afsmHost(
    machine: AfsmDefaultMachine<S, E, C>,
    commandHandler: AfsmCommandHandler<C, E> = AfsmCommandHandler.none(),
    config: AfsmConfig = AfsmConfig(),
): AfsmHost<S, E, C>
```

Overloads accept an explicit initial state with either `AfsmMachine` or
`AfsmReducer`.

Keep `StateFlow`, `viewModelScope`, repositories, `SavedStateHandle`, command
execution, and UI bridging in `ViewModel`. Expose verb-named methods to UI.

## Graph API

- `@AfsmGraph(id, fileName)` registers a graphable machine.
- `AfsmTopology` contains states, transitions, and optional initial state id.
- `topology.toMmd(options)` renders Mermaid.
- `AfsmMmdOptions.Flow` favors review topology.
- `AfsmMmdOptions.Full` includes more internal detail.

## Test API

`afsm-test` provides fluent assertions for state, phase, data, commands,
command invocations, and decisions. See [Testing](testing-guide.md).

## Deliberately absent

There is no `Effect` generic, effect DSL statement, effect stream, effect
delivery policy, or Compose effect module. Durable business outcomes belong in
state; UI-only behavior stays at the UI boundary.
