---
title: Afsm Public API Draft
updated: 2026-05-01
---

# Afsm Public API Draft

## Naming

Working name: `Afsm`

Recommendation: `Afsm` is acceptable if the project consistently defines it as **Android FSM**.

Use:

- library/product name: `Afsm`
- Gradle artifact prefix: `afsm-*`
- Kotlin package segment: `afsm`
- public type prefix: `Afsm`

Avoid:

- `AFSM` in Kotlin type names. It reads like a constant and does not match Kotlin style.
- unclear expansion. The first README should define `Afsm = Android FSM`.

Tradeoff:

- Good: short, searchable, technically precise, easy artifact naming.
- Risk: acronym is not self-explanatory. Documentation must make the value proposition clear immediately.

## Dependency Policy

The library should depend only on Kotlin and Android/AndroidX foundations.

Allowed:

- Kotlin stdlib
- kotlinx-coroutines-core
- kotlinx-coroutines-test for test helpers
- AndroidX Lifecycle ViewModel integration
- AndroidX Lifecycle runtime Compose only if a future Compose helper module is justified

Avoid in core/public API:

- third-party MVI frameworks
- reflection-based runtime magic
- annotation processors or KSP in the MVP
- serialization dependency in core
- DI framework dependency such as Hilt/Koin
- Compose dependency in core

## Module Plan

### `afsm-core`

Pure Kotlin. No Android dependency. Prefer no coroutine dependency.

Owns:

- state machine interface,
- transition result,
- decision/diagnostic model,
- optional command/effect result model,
- transition logging data types.

### `afsm-runtime`

Kotlin + coroutines. No Android dependency.

Owns:

- state holder runner,
- command execution orchestration,
- effect emission,
- command concurrency policy,
- transition logger hook.

This lets non-Android Kotlin tests and possible KMP experiments use the runtime without ViewModel.

### `afsm-viewmodel`

AndroidX ViewModel integration.

Owns:

- ViewModel-friendly host creation,
- `viewModelScope` integration,
- `SavedStateHandle` helper contracts if needed later.

This module must stay thin. It should not become the main architecture framework.

### `afsm-test`

Kotlin coroutine test utilities.

Owns:

- transition assertion helpers,
- command handler fakes,
- runtime test harness,
- ViewModel command execution test helpers.

Avoid depending on Turbine initially. Flow tests should use official coroutine/Flow testing APIs.

### `afsm-compose`

Optional later module.

Do not include in MVP unless the reference implementation proves repeated Compose code that is both useful and safe to abstract.

## Core API Draft

### State Machine

```kotlin
public fun interface AfsmStateMachine<S : Any, E : Any, C : Any, F : Any> {
    public fun transition(
        state: S,
        event: E,
    ): AfsmTransition<S, C, F>
}
```

Rationale:

- Pure synchronous transition function.
- No Android dependency.
- No suspend function. Side effects are returned as commands/effects.
- Uses explicit generic types for state, event, command, and effect.

### Transition

```kotlin
public data class AfsmTransition<out S : Any, out C : Any, out F : Any>(
    val state: S,
    val commands: List<C> = emptyList(),
    val effects: List<F> = emptyList(),
    val decision: AfsmDecision = AfsmDecision.Transitioned,
)
```

Rationale:

- `state` is always present.
- `commands` are work for the runtime/ViewModel to execute.
- `effects` are UI-side one-shot outputs, optional by convention.
- `decision` lets tests and debug logging distinguish real transitions from ignored/invalid events.

Open concern:

- Three generic output parameters are slightly verbose.
- The benefit is explicit `Command` and `Effect` separation without forcing one into the other.

### Decision

```kotlin
public sealed interface AfsmDecision {
    public data object Transitioned : AfsmDecision
    public data class Ignored(val reason: String? = null) : AfsmDecision
    public data class Invalid(val reason: String? = null) : AfsmDecision
}
```

Rationale:

- `Transitioned`: event was accepted and state may or may not have changed.
- `Ignored`: event was intentionally irrelevant, often due to late async result or duplicate click.
- `Invalid`: event indicates a programmer error, impossible UI path, or violated flow rule.

Initial policy:

- Do not throw by default in core.
- Let runtime/debug tooling decide whether `Invalid` throws, logs, or records diagnostics.

### Transition Builders

```kotlin
public object Afsm {
    public fun <S : Any, C : Any, F : Any> transitionTo(
        state: S,
        commands: List<C> = emptyList(),
        effects: List<F> = emptyList(),
    ): AfsmTransition<S, C, F>

    public fun <S : Any, C : Any, F : Any> ignore(
        state: S,
        reason: String? = null,
    ): AfsmTransition<S, C, F>

    public fun <S : Any, C : Any, F : Any> invalid(
        state: S,
        reason: String? = null,
    ): AfsmTransition<S, C, F>
}
```

Rationale:

- Keeps feature state machines readable.
- Avoids early DSL design.
- Leaves transition logic in ordinary Kotlin `when` expressions.

Example:

```kotlin
override fun transition(
    state: SignupState,
    event: SignupEvent,
): AfsmTransition<SignupState, SignupCommand, SignupEffect> {
    return when (state) {
        is SignupState.Editing -> when (event) {
            SignupEvent.SubmitSignupRequested -> {
                if (!state.input.isValid()) {
                    Afsm.transitionTo(
                        state = state.copy(fieldErrors = state.input.validate()),
                    )
                } else {
                    Afsm.transitionTo(
                        state = SignupState.SubmittingAccount(state.input),
                        commands = listOf(SignupCommand.CreateSignupSession(state.input)),
                    )
                }
            }

            else -> Afsm.ignore(state, "Event is not handled while editing")
        }

        else -> Afsm.invalid(state, "Unhandled state/event pair")
    }
}
```

## Runtime API Draft

### Command Handler

```kotlin
public fun interface AfsmCommandHandler<C : Any, E : Any> {
    public suspend fun handle(
        command: C,
        dispatch: suspend (E) -> Unit,
    )
}
```

Rationale:

- A command can emit zero, one, or multiple follow-up events.
- Command results feed back into the FSM as events.
- Command handler stays testable with `runTest`.

### Effect Handler

```kotlin
public fun interface AfsmEffectHandler<F : Any> {
    public suspend fun handle(effect: F)
}
```

Rationale:

- Runtime can route effects to a handler.
- Android UI may instead collect effects from a Flow.
- For MVP, prefer exposing effects as a Flow rather than requiring a handler.

### Runtime Host

```kotlin
public class AfsmHost<S : Any, E : Any, C : Any, F : Any>(
    initialState: S,
    private val stateMachine: AfsmStateMachine<S, E, C, F>,
    private val commandHandler: AfsmCommandHandler<C, E>,
    private val scope: CoroutineScope,
    private val config: AfsmConfig = AfsmConfig(),
) {
    public val state: StateFlow<S>
    public val effects: Flow<F>

    public fun dispatch(event: E)
}
```

Rationale:

- Works without Android.
- ViewModel can own an `AfsmHost`.
- State is exposed as `StateFlow`.
- Effects are exposed separately so UI can collect them lifecycle-aware.

Important constraint:

- The `dispatch` function should be main-safe and serialize transition application.
- Command execution policy must be explicit and deterministic.

### Config

```kotlin
public data class AfsmConfig(
    val invalidTransitionPolicy: AfsmInvalidTransitionPolicy =
        AfsmInvalidTransitionPolicy.Record,
    val commandExecutionPolicy: AfsmCommandExecutionPolicy =
        AfsmCommandExecutionPolicy.Sequential,
)
```

```kotlin
public enum class AfsmInvalidTransitionPolicy {
    Record,
    Throw,
}

public enum class AfsmCommandExecutionPolicy {
    Sequential,
}
```

Rationale:

- Start with sequential command execution only.
- Do not design parallel/cancel-latest policies until the reference flow proves the exact need.
- `Throw` is useful in tests/debug builds.

## ViewModel Integration Draft

MVP should prefer composition over inheritance.

Avoid requiring:

```kotlin
abstract class AfsmViewModel<...> : ViewModel()
```

Prefer:

```kotlin
class SignupViewModel(
    private val createSignupSession: CreateSignupSessionUseCase,
    private val requestIdentityVerification: RequestIdentityVerificationUseCase,
    private val completeSignup: CompleteSignupUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val host = AfsmHost(
        initialState = restoreInitialState(savedStateHandle),
        stateMachine = SignupStateMachine(),
        commandHandler = SignupCommandHandler(
            createSignupSession = createSignupSession,
            requestIdentityVerification = requestIdentityVerification,
            completeSignup = completeSignup,
        ),
        scope = viewModelScope,
    )

    val state: StateFlow<SignupState> = host.state
    val effects: Flow<SignupEffect> = host.effects

    fun onEvent(event: SignupEvent) {
        host.dispatch(event)
    }
}
```

Rationale:

- Does not steal the app's ViewModel inheritance model.
- Works with Hilt and existing ViewModel constructors.
- Keeps Afsm as a tool, not an app architecture owner.

Potential helper:

```kotlin
public fun <S : Any, E : Any, C : Any, F : Any> ViewModel.afsmHost(
    initialState: S,
    stateMachine: AfsmStateMachine<S, E, C, F>,
    commandHandler: AfsmCommandHandler<C, E>,
    config: AfsmConfig = AfsmConfig(),
): AfsmHost<S, E, C, F>
```

This helper lives in `afsm-viewmodel` and only supplies `viewModelScope`.

## Saved State API Draft

Do not include a broad saved-state framework in MVP.

Initial helper should be small and explicit:

```kotlin
public fun interface AfsmStateRestorer<S : Any> {
    public fun restore(): S
}
```

Reference guidance:

- save minimal keys in `SavedStateHandle`,
- reconstruct complex state through use cases,
- do not serialize arbitrary FSM state by default.

Potential later API:

```kotlin
public interface AfsmSavedStateAdapter<S : Any> {
    public fun save(state: S)
    public fun restore(): S?
}
```

Do not add this until the reference flow proves a reusable shape.

## Test API Draft

### Transition Assertion Helpers

```kotlin
public fun <S : Any, C : Any, F : Any> AfsmTransition<S, C, F>.assertState(
    expected: S,
): AfsmTransition<S, C, F>
```

```kotlin
public fun <S : Any, C : Any, F : Any> AfsmTransition<S, C, F>.assertCommands(
    expected: List<C>,
): AfsmTransition<S, C, F>
```

```kotlin
public fun <S : Any, C : Any, F : Any> AfsmTransition<S, C, F>.assertEffects(
    expected: List<F>,
): AfsmTransition<S, C, F>
```

These helpers may be unnecessary if normal assertion libraries are enough. Avoid taking a dependency on a third-party assertion library.

### Command Handler Fake

```kotlin
public class RecordingAfsmCommandHandler<C : Any, E : Any>(
    private val responses: Map<C, List<E>> = emptyMap(),
) : AfsmCommandHandler<C, E> {
    public val recordedCommands: List<C>

    override suspend fun handle(
        command: C,
        dispatch: suspend (E) -> Unit,
    )
}
```

Rationale:

- Lets tests verify command execution without real use cases.
- Uses coroutines test APIs, not third-party flow libraries.

## API Principles

- Prefer ordinary Kotlin over DSL magic.
- Make invalid transitions visible.
- Keep ViewModel integration opt-in and compositional.
- Keep Compose optional.
- Keep command execution deterministic before adding concurrency features.
- Avoid public APIs that require app-wide migration.
- Optimize for reading transition logic and writing tests.

## MVP Public API Candidate

For the first implementation, build only:

- `AfsmStateMachine`
- `AfsmTransition`
- `AfsmDecision`
- `Afsm` transition builder object
- `AfsmCommandHandler`
- `AfsmHost`
- `AfsmConfig`
- `AfsmInvalidTransitionPolicy`
- `AfsmCommandExecutionPolicy.Sequential`
- optional `ViewModel.afsmHost(...)`

Do not build yet:

- DSL transition builder,
- annotation processing,
- graph visualization,
- nested state machine runtime,
- multiple command concurrency policies,
- saved-state adapter,
- Compose-specific APIs.

## Open API Questions

- Is `AfsmTransition<S, C, F>` too verbose for users who do not use effects?
- Should `effects` live in `afsm-core`, or should the core model only return commands?
- Should command execution be inside `afsm-runtime`, or should the MVP stop at core + sample ViewModel pattern?
- Should invalid transitions be part of result data or only logger diagnostics?
- Should `AfsmHost.dispatch` be synchronous, suspending, or fire-and-forget?
- Should `AfsmHost` enforce main-thread dispatch in Android integration or stay platform-agnostic?
