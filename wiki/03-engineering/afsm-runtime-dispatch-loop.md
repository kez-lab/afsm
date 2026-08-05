---
title: Afsm Runtime Dispatch Loop
updated: 2026-08-05
---

# Afsm Runtime Dispatch Loop

## Summary

`AfsmHost<State, Event, Command>` is an Android-independent coroutine runtime.
It serializes events, publishes accepted state, executes typed command work,
and returns command results to the same event queue.

## Dispatch Contract

```text
dispatch(event)
-> bounded FIFO event queue
-> reducer.transition(currentState, event)
-> inspect Decision
-> publish accepted state
-> start/cancel phase-owned invocations
-> enqueue ordinary commands
-> command handler may dispatchEvent(result)
```

- `dispatch` is non-suspending for Android callbacks. A rejected event is
  recorded as an `EventDropped` diagnostic under the default overflow policy and
  throws only under `AfsmOverflowPolicy.Throw`.
- `tryDispatch` returns false instead.
- External events and command-result events share serialized ordering.
- State is published before command execution.
- A suspended ordinary command does not block later event reduction.

## Decision Handling

- `Transitioned` and `Handled`: publish state and accept command work.
- `Ignored`: retain current state and drop any accidental changed state/command
  output; log a defensive diagnostic when necessary.
- `Invalid`: record a diagnostic by default, or throw under
  `AfsmInvalidTransitionPolicy.Throw`.
- A reducer that throws is reported as `ReducerFailure` and follows the same
  policy, so a guard bug cannot silently end event processing.

## Command Handling

Commands execute sequentially in acceptance order. The handler signature is:

```kotlin
suspend fun handle(
    command: C,
    dispatchEvent: suspend (E) -> Unit,
)
```

`dispatchEvent` explicitly means “return a typed result event to the host”, not
a generic callback and not direct reentrant reduction.

Unexpected handler exceptions follow `AfsmCommandFailurePolicy`. Domain
failures should normally become typed result events.

## Phase-Owned Invocation

`onEnter { invoke(key, label) { command } }` starts work separately from the
ordinary sequential command processor. Exiting the phase emits an internal
cancel operation. Host closure also cancels it.

Cancellation is local and cooperative. Remote or non-cooperative work still
requires request ids, idempotency, or backend cancellation.

Invocations run under a `SupervisorJob`, so a failing invocation cannot cancel
sibling invocations or the host. Starting a key that is still active cancels the
stale invocation and records `DuplicateInvocationKey`.

## Queue and Diagnostic Policy

- event and command queue default capacity: 64,
- overflow records a diagnostic and drops the rejected work by default, and
  fails fast with typed exceptions under `AfsmOverflowPolicy.Throw`,
- a processing coroutine that stops because of a failure records `HostStopped`
  and flips `AfsmHost.isActive` to false,
- command result after host close is recorded as a lifecycle drop,
- diagnostics expose safe codes/type names by default,
- raw state/event/command/reason/throwable values require
  `IncludeValues` opt-in.

## Deliberately Absent

The runtime has no UI output stream or delivery/buffering policy. Business
outcomes remain in state; UI behavior stays outside the runtime.

## Verification

Runtime tests cover FIFO ordering, state-before-command publication, later
event responsiveness, queue pressure, host closure, decision policies, command
failure, diagnostics, and invocation cancellation.

`AfsmHostResilienceTest` additionally proves that the recording defaults keep
the host usable: after a command failure, an invalid transition, a reducer
exception, a dropped event, a failing invocation, and a duplicate invocation
key, a later event is still reduced.
