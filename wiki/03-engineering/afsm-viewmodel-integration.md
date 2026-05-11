---
title: Afsm ViewModel Integration
updated: 2026-05-11
---

# Afsm ViewModel Integration

## Summary

`afsm-viewmodel` is the first AndroidX-specific module.

It is intentionally thin:

- depends on `afsm-runtime`
- depends on AndroidX Lifecycle ViewModel
- does not add a base `AfsmViewModel`
- does not own DI, `SavedStateHandle`, navigation, or Compose policy

The only public helper for MVP is:

```kotlin
public fun <S : Any, E : Any, C : Any, F : Any> ViewModel.afsmHost(
    initialState: S,
    reducer: AfsmReducer<S, E, C, F>,
    commandHandler: AfsmCommandHandler<C, E> = AfsmCommandHandler.none(),
    config: AfsmConfig = AfsmConfig(),
): AfsmHost<S, E, C, F>
```

## Intended ViewModel Shape

```kotlin
class SignupViewModel(
    reducer: SignupReducer,
    commandHandler: SignupCommandHandler,
) : ViewModel() {
    private val host = afsmHost(
        initialState = SignupState.Editing(),
        reducer = reducer,
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
- the ViewModel can expose `StateFlow<State>` and `Flow<Effect>` directly from the host
- `onEvent(event)` can delegate to `host.dispatch(event)`
- command handling can dispatch follow-up events through the runtime queue
- effects emitted by the state machine are visible through the ViewModel
- no-command use compiles through the default `AfsmCommandHandler.none()`

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
- state machine generic types still benefit from typealiases or explicit class signatures
- no `SavedStateHandle` convenience exists yet

Conclusion:

Keep this module thin. Do not add more ViewModel framework until the signup reference flow proves a repeated need.
