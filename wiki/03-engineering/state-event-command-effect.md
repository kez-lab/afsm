---
title: State, Event, Command, Effect
updated: 2026-05-01
---

# State, Event, Command, Effect

## State

`State` represents the current business-significant phase of a screen or flow.

Good examples:

- `Editing`
- `Validating`
- `Submitting`
- `Failed`
- `Completed`
- `PermissionRequired`
- `Uploading`
- `Paused`

Avoid modeling every UI detail as FSM state. Focus on states that change valid events, business rules, or durable behavior.

## Event

`Event` represents something that happened.

External events usually come from UI:

- `EmailChanged`
- `SubmitRequested`
- `RetryRequested`
- `PermissionResultReceived`

Internal events usually come from command results:

- `LoginSucceeded`
- `LoginFailed`
- `UploadProgressChanged`
- `TimeoutReached`

## Command

`Command` is work the pure FSM cannot execute directly.

Examples:

- call a use case,
- save to database,
- start polling,
- stop polling,
- start a timer,
- request a domain refresh.

The `ViewModel` executes commands and feeds results back into the FSM as events.

## Effect

`Effect` is optional. Use it for one-shot UI-side actions that cannot be represented cleanly as durable state.

Examples:

- navigate,
- show snackbar,
- launch permission request,
- open external activity.

Use effects sparingly. If something can be represented as state without one-shot delivery risk, prefer state.

## Practical Rule

- State answers: "What phase are we in?"
- Event answers: "What just happened?"
- Command answers: "What work must the ViewModel run?"
- Effect answers: "What one-time UI action must the View run?"
