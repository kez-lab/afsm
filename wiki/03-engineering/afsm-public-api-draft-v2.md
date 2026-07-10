---
title: Afsm Public API Draft v2
updated: 2026-07-10
---

# Afsm Public API Draft v2

## Status

Historical implementation-candidate draft. It is superseded by
[[afsm-v3-executable-dsl|Afsm v3 Executable DSL]] and the implemented public
surface in `docs/afsm-public-api.md`. In particular, `Stayed`, `stay(...)`, the
older state/context vocabulary, and several temporary type names described here
were removed before publication. The validation notes below explain the path to
the current implementation; they are not the current API contract.

Validation note:

- `AfsmNoEffect` and `AfsmTransition<S, C, F>` compile in `afsm-core` with feature-local typealiases.
- Compile check command: `./gradlew :afsm-core:compileTestKotlin`.
- Details: [[afsm-core-compile-validation|Afsm Core Compile Validation]].

Runtime validation note:

- `AfsmHost` has been implemented in `afsm-runtime` with serialized FIFO dispatch, sequential command execution, best-effort effect delivery, and invalid transition policies.
- Verification command: `./gradlew test --no-daemon`.
- Details: [[afsm-runtime-dispatch-loop|Afsm Runtime Dispatch Loop]].

ViewModel validation note:

- `ViewModel.afsmHost(...)` has been implemented in `afsm-viewmodel` as a thin AndroidX Lifecycle integration over `AfsmHost`.
- The helper supplies `viewModelScope`, does not require a base ViewModel, and leaves DI/SavedStateHandle decisions to feature code.
- Details: [[afsm-viewmodel-integration|Afsm ViewModel Integration]].

## Naming Decision

Use `Afsm` as the public API prefix.

Meaning:

```text
Afsm = Android State Machine
```

Rationale:

- The product is specifically for Android teams.
- The name should communicate Android alignment, not a generic FSM utility.
- Kotlin type names remain idiomatic when written as `AfsmStateMachine`, not `AFSMStateMachine`.
- Gradle artifact names remain lowercase: `afsm-core`, `afsm-runtime`, `afsm-viewmodel`, `afsm-test`.

## Dependency Boundary

### `afsm-core`

Allowed dependencies:

- Kotlin stdlib only.

Not allowed:

- Android SDK,
- AndroidX,
- kotlinx-coroutines,
- Compose,
- serialization,
- DI frameworks,
- annotation processors or KSP.

### `afsm-runtime`

Allowed dependencies:

- Kotlin stdlib,
- kotlinx-coroutines-core.

### `afsm-viewmodel`

Allowed dependencies:

- `afsm-core`,
- `afsm-runtime`,
- AndroidX Lifecycle ViewModel.

### `afsm-test`

Allowed dependencies:

- `afsm-core`,
- `afsm-runtime`,
- kotlinx-coroutines-test.

Avoid third-party assertion or Flow testing libraries in MVP.

## Core API Candidate

### State Machine

```kotlin
public fun interface AfsmStateMachine<S : Any, E : Any, C : Any, F : Any> {
    public fun transition(
        state: S,
        event: E,
    ): AfsmTransition<S, C, F>
}
```

Rules:

- `transition` is synchronous.
- `transition` must be deterministic.
- `transition` must not perform side effects.
- Any work outside pure state transition must be returned as command or effect.

### Transition

```kotlin
public data class AfsmTransition<out S : Any, out C : Any, out F : Any>(
    public val state: S,
    public val commands: List<C> = emptyList(),
    public val effects: List<F> = emptyList(),
    public val decision: AfsmDecision = AfsmDecision.Transitioned,
)
```

Decision:

- Keep `effects` in core.
- Keep `AfsmTransition` as a data class for MVP.

Rationale:

- The transition result is the central inspectable artifact.
- Commands and effects being visible in the transition table is important to Afsm's value proposition.
- A data class is useful in tests and logs.

Risk:

- Public data class constructor shape becomes API surface.
- Adding fields later is source and binary sensitive.

Mitigation:

- Treat `state`, `commands`, `effects`, and `decision` as the stable minimal set.

### Decision

```kotlin
public sealed interface AfsmDecision {
    public data object Transitioned : AfsmDecision
    public data class Stayed(val reason: String? = null) : AfsmDecision
    public data class Ignored(val reason: String? = null) : AfsmDecision
    public data class Invalid(val reason: String? = null) : AfsmDecision
}
```

Semantics:

- `Transitioned`: event was accepted and produced forward progress. State may or may not be structurally unequal, but the transition represents normal flow progress.
- `Stayed`: event was accepted, state intentionally stayed the same, and commands/effects may still be emitted.
- `Ignored`: event was irrelevant or stale, and no commands/effects should run.
- `Invalid`: event violates the flow model or indicates a programmer error, and no commands/effects should run.

Examples:

- duplicate submit during in-flight request: `Ignored`
- cancellation where UI owns exit but cleanup command must run: `Stayed`
- late async result after terminal completion: `Ignored`
- success event received before any request was started: `Invalid`

### No Effect Marker

```kotlin
public sealed interface AfsmNoEffect
```

Usage:

```kotlin
private typealias LoginTransition =
    AfsmTransition<LoginState, LoginCommand, AfsmNoEffect>

private typealias LoginMachine =
    AfsmStateMachine<LoginState, LoginEvent, LoginCommand, AfsmNoEffect>
```

Decision:

- Use a sealed interface, not a data object.

Rationale:

- There should be no actual value of `AfsmNoEffect`.
- `List<AfsmNoEffect>` should always be empty by convention.
- This makes "this flow cannot emit effects" visible in the type system.

### Builder Object

```kotlin
public object Afsm {
    public fun <S : Any, C : Any, F : Any> transitionTo(
        state: S,
        commands: List<C> = emptyList(),
        effects: List<F> = emptyList(),
    ): AfsmTransition<S, C, F>

    public fun <S : Any, C : Any, F : Any> stay(
        state: S,
        commands: List<C> = emptyList(),
        effects: List<F> = emptyList(),
        reason: String? = null,
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

Builder semantics:

- `transitionTo`: emits `AfsmDecision.Transitioned`; outputs allowed.
- `stay`: emits `AfsmDecision.Stayed`; outputs allowed.
- `ignore`: emits `AfsmDecision.Ignored`; outputs not allowed.
- `invalid`: emits `AfsmDecision.Invalid`; outputs not allowed.

Decision:

- Do not add commands/effects to `ignore` or `invalid`.
- Use `stay` for accepted same-state events with cleanup commands.

## Runtime API Candidate

### Command Handler

```kotlin
public fun interface AfsmCommandHandler<C : Any, E : Any> {
    public suspend fun handle(
        command: C,
        dispatch: suspend (E) -> Unit,
    )
}
```

Rules:

- Commands are executed by runtime, not by the pure state machine.
- Command results must be converted back into events.
- A command may dispatch zero, one, or multiple events.
- Command handlers must rethrow `CancellationException`.
- Non-cancellation exceptions should be mapped to explicit failure events by feature code when possible.

### Host

```kotlin
public class AfsmHost<S : Any, E : Any, C : Any, F : Any>(
    initialState: S,
    stateMachine: AfsmStateMachine<S, E, C, F>,
    commandHandler: AfsmCommandHandler<C, E>,
    scope: CoroutineScope,
    config: AfsmConfig = AfsmConfig(),
) {
    public val state: StateFlow<S>
    public val effects: Flow<F>

    public fun dispatch(event: E)
}
```

Decision:

- `dispatch(event)` is non-suspending and fire-and-queue.
- The host serializes event processing internally.
- The host runs transitions in FIFO order.
- Command-dispatched events are enqueued; they must not re-enter transition processing recursively.
- MVP command execution policy is sequential.

Rationale:

- UI event handlers can call `dispatch` without launching their own coroutine.
- Reentrancy is predictable.
- Tests can advance coroutine scheduler and assert final state deterministically.

### Dispatch Serialization

Required behavior:

```text
dispatch(A)
dispatch(B)

queue: A, B

process A
-> transition
-> update state
-> emit effects
-> execute commands according to policy
-> command dispatches C

queue: B, C

process B
process C
```

Rules:

- Only one transition may be applied at a time.
- State updates are atomic from the consumer's perspective.
- Events dispatched during command handling are appended to the queue.
- Events dispatched during effect collection are normal external events and are appended to the queue.
- Runtime must not call `transition` concurrently.

### Command Execution Policy

MVP policy:

```kotlin
public enum class AfsmCommandExecutionPolicy {
    Sequential,
}
```

Sequential means:

- commands emitted by one transition execute in list order,
- next queued event is not processed until those commands finish,
- events emitted by commands are queued for later processing,
- long-running polling loops should not be modeled as one never-ending command.

Rationale:

- Predictable behavior is more valuable than concurrency in MVP.
- Parallel/cancel-latest policies should be added only after another reference flow proves the need.

### Invalid Transition Policy

```kotlin
public enum class AfsmInvalidTransitionPolicy {
    Record,
    Throw,
}
```

Semantics:

- `Record`: keep state, do not execute outputs, publish diagnostics to logger.
- `Throw`: throw an exception from the runtime processing coroutine.

Decision:

- `afsm-core` does not throw by itself.
- Runtime applies invalid transition policy.
- Tests may use `Throw`.
- Production default should be `Record`.

### Config

```kotlin
public class AfsmConfig(
    public val invalidTransitionPolicy: AfsmInvalidTransitionPolicy =
        AfsmInvalidTransitionPolicy.Record,
    public val commandExecutionPolicy: AfsmCommandExecutionPolicy =
        AfsmCommandExecutionPolicy.Sequential,
    public val effectDelivery: AfsmEffectDelivery =
        AfsmEffectDelivery.Default,
)
```

Decision:

- Use a regular class for `AfsmConfig`, not a data class.

Rationale:

- Config is likely to grow.
- Public data classes expose `copy`, `componentN`, and constructor shape as stable API.

## Effect Delivery Candidate

### Effect Delivery Type

```kotlin
public class AfsmEffectDelivery private constructor(
    public val replay: Int,
    public val extraBufferCapacity: Int,
) {
    public companion object {
        public val Default: AfsmEffectDelivery
        public val Rendezvous: AfsmEffectDelivery
        public fun buffered(capacity: Int): AfsmEffectDelivery
    }
}
```

MVP default:

```text
replay = 0
extraBufferCapacity = 1
```

Behavior:

- Effects are not replayed to new collectors.
- Effects are buffered lightly to reduce loss during immediate collector gaps.
- Effects are not durable state.
- Critical effects that must survive recreation should be represented as state plus acknowledgement event, not as effect-only output.

### Effect Flow

Host exposes:

```kotlin
public val effects: Flow<F>
```

Implementation candidate:

```kotlin
private val _effects = MutableSharedFlow<F>(
    replay = config.effectDelivery.replay,
    extraBufferCapacity = config.effectDelivery.extraBufferCapacity,
)
```

Rules:

- `replay` must default to `0` to avoid relaunching one-shot UI actions after rotation.
- Consumers should collect effects lifecycle-aware.
- Libraries should document that effects are best-effort one-shots.
- For critical external launches, prefer durable state gating:

```text
State = AwaitingIdentityLaunch(...)
UI launches provider
UI dispatches IdentityLaunchConsumed
State = AwaitingIdentityResult(...)
```

Important:

- The signup reference flow may still use `LaunchIdentityVerification` as an effect for MVP.
- Before production release, test whether this effect can be lost during lifecycle transitions.

## ViewModel Integration Candidate

Prefer composition over inheritance.

```kotlin
public fun <S : Any, E : Any, C : Any, F : Any> ViewModel.afsmHost(
    initialState: S,
    stateMachine: AfsmStateMachine<S, E, C, F>,
    commandHandler: AfsmCommandHandler<C, E>,
    config: AfsmConfig = AfsmConfig(),
): AfsmHost<S, E, C, F>
```

Rules:

- The helper supplies `viewModelScope`.
- The helper does not impose a base `AfsmViewModel`.
- Hilt/Koin/manual DI remain app concerns.
- `SavedStateHandle` remains explicit feature code in MVP.

## Saved State Candidate

Do not ship a generic saved-state adapter in MVP.

MVP guidance:

- save minimal keys in `SavedStateHandle`,
- never serialize arbitrary full state by default,
- restore via feature-defined snapshot and domain refresh.

Signup sample should include:

```kotlin
data class SignupSavedSnapshot(
    val step: SignupStep,
    val input: SignupInput,
    val sessionId: SignupSessionId?,
    val verificationRequestId: VerificationRequestId?,
    val retryCount: Int,
)
```

Restore should produce one of:

- initial `SignupState`,
- initial `SignupState` plus restoration command,
- initial `SignupState` plus restoration event.

Decision:

- Keep this in sample code first.
- Extract saved-state API only after at least two reference flows repeat the same pattern.

## Example With Typealiases

```kotlin
private typealias SignupTransition =
    AfsmTransition<SignupState, SignupCommand, SignupEffect>

private typealias SignupMachine =
    AfsmStateMachine<SignupState, SignupEvent, SignupCommand, SignupEffect>

class SignupStateMachine(
    private val validator: SignupInputValidator,
) : SignupMachine {

    override fun transition(
        state: SignupState,
        event: SignupEvent,
    ): SignupTransition {
        return when (state) {
            is SignupState.Editing -> when (event) {
                SignupEvent.SubmitSignupRequested -> {
                    val errors = validator.validate(state.input)
                    if (errors.hasErrors) {
                        Afsm.stay(
                            state = state.copy(fieldErrors = errors),
                            reason = "Input validation failed",
                        )
                    } else {
                        Afsm.transitionTo(
                            state = SignupState.SubmittingAccount(state.input),
                            commands = listOf(
                                SignupCommand.CreateSignupSession(state.input),
                            ),
                        )
                    }
                }

                else -> Afsm.ignore(
                    state = state,
                    reason = "Event is not relevant while editing",
                )
            }

            else -> Afsm.invalid(
                state = state,
                reason = "Unhandled state/event pair",
            )
        }
    }
}
```

## Open Risks

- Effect delivery may still be too subtle for Android teams.
- Sequential command execution may be too restrictive for later polling/upload flows.
- `AfsmNoEffect` as a sealed interface needs actual compilation validation.
- `AfsmHost.dispatch` fire-and-queue behavior must be tested carefully under `runTest`.
- `AfsmConfig` regular class is safer for API evolution but less convenient than a data class.

## Implementation Approval Gate

Before implementation, CEO should explicitly approve:

- `Afsm` prefix on all public types,
- `AfsmDecision.Stayed`,
- `Afsm.stay(...)`,
- `AfsmNoEffect`,
- effects in core,
- `MutableSharedFlow`-style best-effort effect delivery with `replay = 0`,
- non-suspending serialized `dispatch(event)`,
- sequential command execution as MVP-only policy,
- no generic saved-state adapter in MVP.
