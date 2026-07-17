# Afsm Public API

Status: internal beta. Afsm is not publicly released and may make breaking API
changes when usability or safety evidence justifies them.

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
    fun dispatch(event: E)
    fun tryDispatch(event: E): Boolean
    fun close()
}
```

The host serializes events, publishes accepted state before scheduling command
work, executes commands sequentially, and returns command results through the
handler's `dispatchEvent` capability.

`AfsmConfig` controls invalid-transition policy, command failure policy, queue
capacities, diagnostic data policy, and logging.

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
