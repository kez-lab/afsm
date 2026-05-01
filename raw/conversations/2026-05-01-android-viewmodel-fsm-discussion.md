# Android ViewModel FSM Discussion

Date: 2026-05-01

## Context

The discussion started from a concern about modern Android development patterns:

- Moving business logic into `ViewModel` is beneficial for separation.
- Observing state from the View is beneficial for rendering and lifecycle handling.
- However, the user interaction flow becomes harder to read: user interaction -> business logic -> view update is split across View, ViewModel, state streams, and effects.

## User Concern

The core concern:

> Even with `UiAction`, `UiState`, and `UiEffect`, understanding the flow still requires checking the View to see when an Action is emitted, then checking the ViewModel to see what happens, then checking the View again to see how state updates are rendered. Is that discomfort real or just a minor personal preference?

## Discussion Summary

The answer reached in the conversation:

- This is not a trivial preference.
- It is a real locality and traceability cost introduced by MVVM/UDF.
- MVVM/UDF improves separation, lifecycle behavior, and testability, but it fragments the local reading path.
- The problem should not be dismissed as "bad understanding of MVVM"; it is a real design tradeoff.

## Direction Reached

The discussion converged on this direction:

- Keep Android `ViewModel`.
- Add an explicit FSM layer for complex screens and flows.
- Do not turn the `ViewModel` itself into the FSM.
- Instead, let the `ViewModel` own and execute a plain Kotlin FSM.

Recommended responsibility split:

- View: render state, capture user timing, send events, execute pure UI behavior.
- ViewModel: lifecycle adapter, state holder, event bridge, command executor.
- StateMachine: pure state transition model.
- UseCase/Repository: business operations and data access.

## Proposed Flow

```text
User Interaction
-> Ui/Event
-> ViewModel
-> StateMachine.transition(currentState, event)
-> NewState + Command
-> ViewModel executes Command
-> Async result becomes an internal Event
-> StateMachine.transition(...)
-> View renders new State
```

## Core Model

The conversation proposed these core concepts:

- `State`: current business/screen flow phase.
- `Event`: something that happened, either from UI or from internal async results.
- `Command`: work the ViewModel should execute outside the pure transition function.
- `Effect`: optional one-shot UI-side action, used sparingly.

## Example State Set

Login example:

- `Editing`
- `Submitting`
- `Failed`
- `LoggedIn`

Events:

- `EmailChanged`
- `PasswordChanged`
- `SubmitRequested`
- `LoginSucceeded`
- `LoginFailed`

Command:

- `ExecuteLogin(email, password)`

## Important Design Judgment

FSM is most useful when a screen is actually flow-like:

- signup
- payment
- identity verification
- permission-gated flows
- order state
- onboarding
- search/filter/paging
- chat connection state
- upload/download progress
- multi-step forms

FSM may be overkill for simple screens with one request and simple loading/content/error states.

## Key Principle

Use FSM for business-significant state, not for every UI detail.

Put in FSM when:

- the state changes business flow,
- it should be tested,
- it should survive rotation/process restoration conceptually,
- it controls which events are valid.

Keep in UI when:

- it is focus state,
- bottom sheet animation state,
- scroll state,
- snackbar host state,
- keyboard visibility,
- local layout interaction.
