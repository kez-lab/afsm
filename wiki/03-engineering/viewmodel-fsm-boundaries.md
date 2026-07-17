---
title: ViewModel and FSM Boundaries
updated: 2026-07-17
---

# ViewModel and FSM Boundaries

## View

The View owns UI timing and rendering.

It should:

- render the current `State`,
- call feature-named ViewModel methods such as `submit()`, `pay()`, `retry()`,
  or `updateTitle(value)` when the user acts,
- execute UI-only behaviors such as focus changes and local sheet state,
- perform navigation and other UI behavior from direct UI callbacks or durable
  business state.

The UI does not need to construct machine `Event` values or expose a generic
`onEvent(Event)` boundary. Machine events remain typed inputs behind the
ViewModel adapter.

## ViewModel

The ViewModel owns Android integration.

It should:

- expose `StateFlow<State>`,
- expose ordinary feature verbs to the UI and translate them into machine
  events,
- create an `AfsmHost` owned by `viewModelScope`,
- provide the command handler that executes repository/use-case work,
- convert command results into internal machine events through
  `dispatchEvent`,
- integrate with `SavedStateHandle` when needed.

`AfsmHost` serializes events, publishes accepted state, and schedules command
work. The ViewModel supplies Android lifetime, runtime inputs, and external-work
implementation; it should not duplicate the full transition table.

## StateMachine

The FSM owns transition rules.

It should:

- be plain Kotlin,
- have no Android dependency,
- avoid `suspend` in `transition`,
- avoid direct repository/use case calls,
- return typed `Command` values instead of performing external work,
- define behavior for invalid or irrelevant events.

Business completion belongs in durable `State`. The machine does not emit a
separate best-effort UI Effect stream.

## UseCase / Repository

Use cases and repositories own actual business operations and data access.

They should not know about Compose, `ViewModel`, or UI rendering details.

## Boundary Rule

If the question is "which state is valid after this business event?", it
belongs in the FSM.

If the question is "how do we execute this Android lifecycle-aware async
operation and return its result?", it belongs in the ViewModel command handler
and repository/use case.

If the question is "how should this widget look or behave locally?", it belongs
in the View.

If the question is "should this business outcome survive re-collection or
recreation?", model it as durable state and define feature-owned restoration.
