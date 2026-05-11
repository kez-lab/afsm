---
title: Afsm Runtime Dispatch Loop
updated: 2026-05-11
---

# Afsm Runtime Dispatch Loop

## Summary

`afsm-runtime` now provides the first executable runtime layer above `afsm-core`.

The module is Android-friendly but not Android-dependent:

- depends on `afsm-core`
- depends on `kotlinx-coroutines-core`
- does not depend on Android SDK, AndroidX, ViewModel, Compose, DI, serialization, or code generation

This keeps the runtime cohesive: it owns event queueing, state publication, effect publication, command execution, diagnostics, and invalid transition policy.

Android-specific lifetime ownership remains a caller concern for now. In ViewModel usage, the host should be attached to `viewModelScope`.

## Public Runtime Types

- `AfsmHost<S, E, C, F>`
- `AfsmCommandHandler<C, E>`
- `AfsmConfig`
- `AfsmCommandExecutionPolicy`
- `AfsmInvalidTransitionPolicy`
- `AfsmEffectDelivery`
- `AfsmLogger`
- `AfsmDiagnostic`
- `AfsmInvalidTransitionException`

## Dispatch Contract

`AfsmHost.dispatch(event)` is non-suspending.

The host owns an internal FIFO event queue:

```text
dispatch(A)
dispatch(B)

process A
-> transition
-> update state
-> emit effects
-> enqueue commands

process B

command processor executes command from A
-> command dispatches C

process C
```

Important behavior:

- Only one transition runs at a time.
- Command-dispatched events are queued, not handled recursively.
- Commands execute sequentially in the order emitted by one transition.
- Command execution does not block later event reduction.
- Long-running loops should not be modeled as never-ending commands.

## Decision Handling

`AfsmDecision.Transitioned`:

- update state
- emit effects
- execute commands

`AfsmDecision.Stayed`:

- publish the returned state
- emit effects
- execute commands
- intended for accepted same-state events such as cleanup

`AfsmDecision.Ignored`:

- keep current runtime state
- drop any accidental commands/effects returned by the transition
- log a diagnostic if ignored output or changed state is returned defensively

`AfsmDecision.Invalid`:

- keep current runtime state
- drop outputs
- `Record`: log diagnostic
- `Throw`: throw `AfsmInvalidTransitionException` from the runtime processing coroutine

## Effect Delivery

Effects are exposed as `Flow<F>`.

Default delivery:

```text
replay = 0
extraBufferCapacity = 1
overflow = DROP_OLDEST
```

This is intentionally best-effort:

- no replay avoids relaunching one-shot UI work after recreation
- a small buffer reduces loss during immediate collector gaps
- critical actions that must survive recreation should be state-driven with an acknowledgement event

## Android Developer Usage Shape

The intended ViewModel-side shape is:

```kotlin
private val host = AfsmHost(
    initialState = initialState,
    stateMachine = signupStateMachine,
    commandHandler = signupCommandHandler,
    scope = viewModelScope,
)

val state: StateFlow<SignupState> = host.state
val effects: Flow<SignupEffect> = host.effects

fun onEvent(event: SignupEvent) {
    host.dispatch(event)
}
```

This keeps the user-facing mental model small:

- View calls `onEvent(...)`.
- ViewModel delegates to `host.dispatch(...)`.
- StateMachine owns transition rules.
- CommandHandler owns side-effectful work and maps results back to events.
- UI collects `state` and lifecycle-aware `effects`.

## Verification

Command:

```bash
./gradlew test --no-daemon
```

Result:

```text
BUILD SUCCESSFUL
```

Verified cases:

- external events and command-dispatched events preserve FIFO order
- command-dispatched events do not re-enter transition processing recursively
- accepted transitions emit state before effects and commands
- `Stayed` may execute cleanup commands while keeping state
- `Ignored` keeps runtime state and drops accidental outputs
- `Invalid` with `Record` logs diagnostics and drops outputs
- `Invalid` with `Throw` fails the runtime processing coroutine

## Design Note

The test scope issue is intentional architecture feedback: `AfsmHost` is a long-lived runtime object. It should be attached to a lifecycle-owning scope, not to a short scenario block that expects all child coroutines to complete.

For Android this maps naturally to `viewModelScope`.

For JVM tests, use a dedicated test `CoroutineScope` and advance the shared test scheduler.

## Follow-Up

Next recommended task: build `afsm-viewmodel` as a thin AndroidX Lifecycle integration module that supplies `viewModelScope` and proves the runtime API feels natural inside a real ViewModel.
