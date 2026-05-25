---
title: Afsm ViewModel Integration
updated: 2026-05-25
---

# Afsm ViewModel Integration

## Summary

`afsm-viewmodel` is the first AndroidX-specific module.

It is intentionally thin:

- depends on `afsm-runtime`
- depends on AndroidX Lifecycle ViewModel
- does not add a base `AfsmViewModel`
- does not own DI, `SavedStateHandle`, navigation, or Compose policy

The standard graphable-machine helper is:

```kotlin
public fun <S : Any, E : Any, C : Any, F : Any> ViewModel.afsmHost(
    machine: AfsmMachine<S, E, C, F>,
    commandHandler: AfsmCommandHandler<C, E> = AfsmCommandHandler.none(),
    config: AfsmConfig = AfsmConfig(),
): AfsmHost<S, E, C, F>
```

Use the explicit initial-state overload when the state is dynamic:

```kotlin
public fun <S : Any, E : Any, C : Any, F : Any> ViewModel.afsmHost(
    machine: AfsmMachine<S, E, C, F>,
    initialState: S,
    commandHandler: AfsmCommandHandler<C, E> = AfsmCommandHandler.none(),
    config: AfsmConfig = AfsmConfig(),
): AfsmHost<S, E, C, F>
```

Use the lower-level reducer overload for custom non-graphable reducers:

```kotlin
public fun <S : Any, E : Any, C : Any, F : Any> ViewModel.afsmHost(
    reducer: AfsmReducer<S, E, C, F>,
    initialState: S,
    commandHandler: AfsmCommandHandler<C, E> = AfsmCommandHandler.none(),
    config: AfsmConfig = AfsmConfig(),
): AfsmHost<S, E, C, F>
```

Although the exact parameter type is `AfsmCommandHandler<C, E>`, Kotlin callers
normally pass a direct SAM lambda:

```kotlin
commandHandler = { command: SignupCommand, dispatch ->
    // execute repository/use-case work
    // dispatch(result event)
}
```

The default `AfsmCommandHandler.none()` intentionally ignores emitted commands.
Use it only for machines that never emit commands; command-emitting machines
should always pass an explicit handler.

## Intended ViewModel Shape

```kotlin
class SignupViewModel(
    machine: SignupMachine,
    commandHandler: SignupCommandHandler,
) : ViewModel() {
    private val host = afsmHost(
        machine = machine,
        commandHandler = commandHandler,
    )

    val state: StateFlow<SignupState> = host.state
    val effects: Flow<SignupEffect> = host.effects

    fun onEvent(event: SignupEvent) {
        host.dispatch(event)
    }
}
```

This keeps the Android developer's mental model small:

- define a pure state machine
- define command handling near ViewModel/use case wiring
- expose `state` and `effects`
- forward UI events to `host.dispatch(event)`

## Build Shape

`afsm-viewmodel` is an Android library module, not a plain JVM module.

Reason:

- `androidx.lifecycle:lifecycle-viewmodel-ktx` is distributed as an Android artifact.
- A plain Kotlin/JVM module cannot consume the AAR variant correctly.
- The helper's purpose is explicitly AndroidX Lifecycle integration, so Android library module boundaries are appropriate.

Build choices:

- Android Gradle Plugin: `8.10.1`
- Gradle wrapper: `8.11.1`
- compile SDK: `36`
- AndroidX Lifecycle: `2.10.0`
- Kotlin plugin: `2.0.21`

Official dependency note:

- AndroidX Lifecycle release docs list `2.10.0` as the stable Lifecycle version and show `lifecycle-viewmodel-ktx` as the ViewModel Kotlin dependency.
- AGP 8.10 release docs list Gradle `8.11.1` as the minimum/default compatible Gradle version and API 36 as supported.

## Verification

Commands:

```bash
./gradlew :afsm-viewmodel:testDebugUnitTest --no-daemon
./gradlew test --no-daemon
./gradlew test --warning-mode all --no-daemon
```

Result:

```text
BUILD SUCCESSFUL
```

Verified cases:

- a real `ViewModel` subclass can create `private val host = afsmHost(...)`
- a graphable machine can be hosted with `afsmHost(machine = StateMachine, ...)`
- a graphable machine can override its initial state with `afsmHost(machine = StateMachine, initialState = ...)`
- the ViewModel can expose `StateFlow<State>` and `Flow<Effect>` directly from the host
- `onEvent(event)` can delegate to `host.dispatch(event)`
- command handling can dispatch follow-up events through the runtime queue
- effects emitted by the state machine are visible through the ViewModel
- no-command use compiles through the default `AfsmCommandHandler.none()`, while
  command-emitting machines should pass an explicit handler

## Ergonomics Assessment

The helper is natural enough for MVP.

Good:

- no inheritance requirement
- no custom base class
- no Android UI object references
- `viewModelScope` is supplied automatically
- existing runtime semantics remain unchanged

Still verbose:

- feature code still declares `State`, `Event`, `Command`, and `Effect`
- no `SavedStateHandle` convenience exists yet

Conclusion:

Keep this module thin. The graphable-machine overload is the standard path;
dynamic state screens should use `machine + initialState` when they still want
graph metadata and the lower-level `initialState + reducer` overload only when
they intentionally opt out of graphability.
