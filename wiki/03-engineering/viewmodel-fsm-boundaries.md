---
title: ViewModel and FSM Boundaries
updated: 2026-05-01
---

# ViewModel and FSM Boundaries

## View

The View owns UI timing and rendering.

It should:

- render the current `State`,
- send `Event`s when the user acts,
- execute UI-only behaviors such as focus changes and local sheet state,
- perform navigation or snackbar display when directed by state/effect policy.

## ViewModel

The ViewModel owns Android integration.

It should:

- expose `StateFlow<State>`,
- receive events through `onEvent`,
- call the state machine,
- update state,
- execute commands in `viewModelScope`,
- convert command results into internal events,
- integrate with `SavedStateHandle` when needed.

It should not contain the full transition table for complex flows if that table can live in a plain Kotlin state machine.

## StateMachine

The FSM owns transition rules.

It should:

- be plain Kotlin,
- have no Android dependency,
- avoid `suspend` in `transition`,
- avoid direct repository/use case calls,
- return commands instead of performing side effects,
- define behavior for invalid or irrelevant events.

## UseCase / Repository

Use cases and repositories own actual business operations and data access.

They should not know about Compose, `ViewModel`, or UI rendering details.

## Boundary Rule

If the question is "which state is valid after this business event?", it belongs in the FSM.

If the question is "how do we execute this Android lifecycle-aware async operation?", it belongs in the ViewModel.

If the question is "how should this widget look or behave locally?", it belongs in the View.
