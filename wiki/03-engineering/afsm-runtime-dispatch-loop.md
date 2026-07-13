---
title: Afsm Runtime Dispatch Loop
updated: 2026-07-13
---

# Afsm Runtime Dispatch Loop

## Summary

`afsm-runtime` is the plain-Kotlin host layer above `afsm-core`.

The module is Android-friendly but not Android-dependent:

- depends on `afsm-core`
- depends on `kotlinx-coroutines-core`
- does not depend on Android SDK, AndroidX, ViewModel, Compose, DI, serialization, or code generation

This keeps the runtime cohesive: it owns bounded event queueing, state
publication, effect publication, sequential command execution, privacy-aware
diagnostics, phase-owned invocation jobs, and host failure policies.

Android-specific lifetime ownership remains a caller concern for now. In ViewModel usage, the host should be attached to `viewModelScope`.

## Public Runtime Types

- `AfsmHost<S, E, C, F>`
- `AfsmCommandHandler<C, E>`
- `AfsmConfig`
- `AfsmCommandExecutionPolicy`
- `AfsmCommandFailurePolicy`
- `AfsmInvalidTransitionPolicy`
- `AfsmEffectDelivery`
- `AfsmLogger`
- `AfsmDiagnostic`
- `AfsmDiagnosticCode`
- `AfsmDiagnosticDecision`
- `AfsmDiagnosticDataPolicy`
- `AfsmDiagnosticValues`
- `AfsmInvalidTransitionException`
- `AfsmEventQueueOverflowException`
- `AfsmCommandQueueOverflowException`

Companion core output types used by the runtime are `AfsmInvocationKey` and
`AfsmCommandInvocation.Start/Cancel` on
`AfsmTransition.commandInvocations`.

## Dispatch Contract

`AfsmHost.dispatch(event)` is non-suspending.

`AfsmCommandHandler.handle(command, dispatchEvent)` receives a result-event
dispatcher. `dispatchEvent(event)` queues the typed event through the same
serialized host event path. The explicit name distinguishes this capability
from a generic callback and from coroutine dispatcher configuration.

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
- Event and command queues are bounded by `AfsmConfig.eventQueueCapacity` and `AfsmConfig.commandQueueCapacity`, both defaulting to `64`.
- If the command queue fills, the host throws `AfsmCommandQueueOverflowException` instead of suspending the event processor indefinitely.
- If a command result cannot enter the full event queue, the host throws
  `AfsmEventQueueOverflowException` instead of blocking the command processor.
- A command result dispatched after host closure is dropped and recorded as a
  lifecycle diagnostic because the owning screen has ended.
- Long-running phase-owned loops should use `invoke`, not a never-ending
  ordinary command.

## Phase-Owned Invocation

`onEnter { invoke(key, label) { command } }` emits a keyed start operation.
When any accepted transition leaves that phase, the executable machine emits a
cancel operation before source `onExit` and target `onEnter` work.

The host starts invocation work through the same `AfsmCommandHandler`, but in a
tracked child job rather than the ordinary sequential queue. Phase exit and
host closure cancel that job. A cancelled job cannot send a result through its
Afsm-owned `dispatchEvent` capability, and `CancellationException` is not
recorded as command failure.

Cancellation is a request, not a join barrier. The host can start the target
phase invocation while old non-cancellable cleanup is still finishing. The
ordinary command queue capacity also does not limit invocation jobs, so DSL
machines should keep a small fixed set of phase-owned keys.

Invocation is local cooperative cancellation. Request ids, stale-result
handling, idempotency, and SDK-specific cancellation remain required when work
can outlive the local coroutine.

## Decision Handling

`AfsmDecision.Transitioned`:

- update state
- emit effects
- execute commands

`AfsmDecision.Handled`:

- publish the returned state
- emit effects
- execute commands
- represents an accepted event without a phase change

`AfsmDecision.Ignored`:

- keep current runtime state
- drop any accidental commands/effects returned by the transition
- log a diagnostic if ignored output or changed state is returned defensively

`AfsmDecision.Invalid`:

- keep current runtime state
- drop outputs
- `Record`: log diagnostic
- `Throw`: throw `AfsmInvalidTransitionException` from the runtime processing coroutine

## Diagnostic Privacy

`AfsmDiagnosticDataPolicy.TypesOnly` is the default. Every diagnostic exposes:

- a stable `AfsmDiagnosticCode`,
- a value-free `AfsmDiagnosticDecision`,
- a fixed library message,
- simple state/event/command/failure type names,
- Afsm-owned metadata such as queue capacity.

Raw state, event, command, reason, and throwable objects are discarded before
the configured logger receives the default diagnostic. They are grouped under
nullable `diagnostic.values` only when the host explicitly selects
`AfsmDiagnosticDataPolicy.IncludeValues`. That opt-in requires an
application-owned privacy and redaction boundary.

The diagnostic and grouped-values constructors are runtime-owned. No
compatibility getter preserves the old raw top-level fields.

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
    reducer = signupStateMachine,
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
./gradlew :afsm-runtime:check :afsm-runtime:apiCheck --no-daemon
./scripts/verify-release-local.sh --no-daemon
```

Result:

```text
BUILD SUCCESSFUL
```

Verified cases:

- external events and command-dispatched events preserve FIFO order
- command-dispatched events do not re-enter transition processing recursively
- accepted transitions emit state before effects and commands
- `Handled` may update data and execute commands/effects without changing phase
- `Ignored` keeps runtime state and drops accidental outputs
- `Invalid` with `Record` logs diagnostics and drops outputs
- `Invalid` with `Throw` fails the runtime processing coroutine
- default invalid and command-failure diagnostics do not retain credential-like
  state, event, command, reason, exception message, or throwable values
- `IncludeValues` restores grouped raw access only after explicit configuration
- command queue overflow fails fast instead of suspending event processing
- command-result event overflow fails fast instead of suspending command processing
- default effects are not replayed to late collectors
- invalid event and command queue capacities are rejected at construction time
- command handling remains sequential while later UI events can still be reduced
- phase-owned invocation jobs run beside ordinary sequential commands and are
  cancelled on phase exit or host closure
- a cancelled invocation cannot dispatch a late result through its Afsm-owned
  `dispatchEvent` capability, even from non-cancellable cleanup

## Design Note

The test scope issue is intentional architecture feedback: `AfsmHost` is a long-lived runtime object. It should be attached to a lifecycle-owning scope, not to a short scenario block that expects all child coroutines to complete.

For Android this maps naturally to `viewModelScope`.

For JVM tests, use a dedicated test `CoroutineScope` and advance the shared test scheduler.

## Follow-Up

Command-result event overflow now fails fast with
`AfsmEventQueueOverflowException` when a full bounded event queue rejects a
command result event. If the host has already closed, command result events are
dropped and logged because the Android screen lifecycle has ended.

Bounded phase-owned invocation now covers local cooperative cancellation.
Do not generalize it into actors, restart strategies, or hierarchy without
pilot evidence. The remaining runtime evidence boundary is production-like
remote/SDK work: request ids, idempotency, and application cancellation
contracts are still required when local coroutine cancellation is insufficient.
