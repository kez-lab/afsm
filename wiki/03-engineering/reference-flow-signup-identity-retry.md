---
title: Historical Reference Flow - Signup Identity Retry
updated: 2026-07-17
status: historical-reference-superseded-output-model
---

# Reference Flow - Signup Identity Retry

> Historical pre-implementation reference flow. Its Effect policy was
> superseded by
> [[afsm-output-model-simplification|Afsm Output Model Simplification]]. Use
> [[afsm-example-catalog|Afsm Example Catalog]] for current reference flows.
> The scenario remains useful design history; its proposed API is not current.

## Purpose

This is the first reference flow for the Android FSM library.

The flow is intentionally more complex than a login screen. It includes user input, server validation, external identity verification, app return handling, timeout, cancellation, retry, and final completion. It should prove that the library improves flow traceability while preserving Android `ViewModel`, `StateFlow`, Compose, and lifecycle guidance.

## Product Scenario

A user creates an account and must complete identity verification before the account can be activated.

High-level path:

```text
Enter account info
-> submit signup
-> server creates pending signup session
-> request identity verification
-> launch external identity verification
-> wait for callback/poll result
-> success: complete signup
-> failure/timeout/cancel: retry or edit input
```

## Why This Flow

This flow exercises the library's core value:

- several business-significant states,
- async command execution,
- external UI effect,
- invalid transitions,
- retry behavior,
- terminal success,
- UI behavior versus business logic boundary,
- minimal saved-state restoration policy.

## Boundary Assumptions

Assumption: Identity verification is performed by an external app, browser, WebView, or provider SDK that must be launched by the UI layer.

Assumption: The ViewModel can call domain use cases to create a pending signup session, request a verification token, poll/check verification status, and complete signup.

Assumption: The FSM should not hold Android types such as `Intent`, `ActivityResultLauncher`, `Context`, `Lifecycle`, or `NavController`.

## State Policy

`State` represents the business-significant phase of the signup flow.

State should be durable enough to render the screen and determine valid events. It should not contain UI element implementation details such as focus state, sheet animation state, `SnackbarHostState`, or `NavController`.

### Proposed State Model

```kotlin
sealed interface SignupState {
    data class Editing(
        val input: SignupInput = SignupInput(),
        val fieldErrors: FieldErrors = FieldErrors(),
    ) : SignupState

    data class SubmittingAccount(
        val input: SignupInput,
    ) : SignupState

    data class ReadyForIdentityVerification(
        val sessionId: SignupSessionId,
        val input: SignupInput,
        val retryCount: Int = 0,
    ) : SignupState

    data class RequestingIdentityVerification(
        val sessionId: SignupSessionId,
        val input: SignupInput,
        val retryCount: Int,
    ) : SignupState

    data class AwaitingIdentityResult(
        val sessionId: SignupSessionId,
        val verificationRequestId: VerificationRequestId,
        val input: SignupInput,
        val retryCount: Int,
    ) : SignupState

    data class IdentityVerificationFailed(
        val sessionId: SignupSessionId,
        val input: SignupInput,
        val reason: IdentityFailureReason,
        val retryCount: Int,
        val canRetry: Boolean,
    ) : SignupState

    data class CompletingSignup(
        val sessionId: SignupSessionId,
        val input: SignupInput,
    ) : SignupState

    data class Completed(
        val accountId: AccountId,
    ) : SignupState
}
```

### State Notes

- `Editing` owns user-entered signup fields and local validation errors.
- `SubmittingAccount` means account/session creation is in flight.
- `ReadyForIdentityVerification` is a stable state before launching the external verification step.
- `RequestingIdentityVerification` means the app is asking the backend/provider for a verification request.
- `AwaitingIdentityResult` means the verification request has been launched and the app awaits callback or polling result.
- `IdentityVerificationFailed` preserves retry information and explains the failure.
- `CompletingSignup` means identity succeeded and final account activation is in flight.
- `Completed` is a terminal business state. UI may navigate away when it observes this state.

## Event Policy

`Event` represents something that happened. Events are inputs to the FSM.

External events come from UI, app lifecycle callbacks, activity result callbacks, or navigation-level callers. Internal events come from command results.

### Proposed Event Model

```kotlin
sealed interface SignupEvent {
    data class NameChanged(val value: String) : SignupEvent
    data class EmailChanged(val value: String) : SignupEvent
    data class PasswordChanged(val value: String) : SignupEvent
    data object SubmitSignupRequested : SignupEvent

    data class SignupSessionCreated(
        val sessionId: SignupSessionId,
    ) : SignupEvent

    data class SignupSessionCreationFailed(
        val reason: SignupFailureReason,
    ) : SignupEvent

    data object StartIdentityVerificationRequested : SignupEvent

    data class IdentityVerificationRequestCreated(
        val verificationRequestId: VerificationRequestId,
        val launchToken: VerificationLaunchToken,
    ) : SignupEvent

    data class IdentityVerificationRequestFailed(
        val reason: IdentityFailureReason,
    ) : SignupEvent

    data class ExternalIdentityResultReceived(
        val result: ExternalIdentityResult,
    ) : SignupEvent

    data object IdentityPollingTick : SignupEvent

    data object IdentityVerificationSucceeded : SignupEvent

    data class IdentityVerificationFailed(
        val reason: IdentityFailureReason,
    ) : SignupEvent

    data object RetryIdentityVerificationRequested : SignupEvent
    data object EditInputRequested : SignupEvent
    data object CancelRequested : SignupEvent

    data class SignupCompleted(
        val accountId: AccountId,
    ) : SignupEvent

    data class SignupCompletionFailed(
        val reason: SignupFailureReason,
    ) : SignupEvent
}
```

### Event Rules

- UI field changes are valid mainly in `Editing` and selected failure states that allow editing.
- `SubmitSignupRequested` is valid from `Editing` when input is locally valid.
- `StartIdentityVerificationRequested` is valid from `ReadyForIdentityVerification`.
- `ExternalIdentityResultReceived` is valid from `AwaitingIdentityResult`.
- `IdentityPollingTick` is valid from `AwaitingIdentityResult`.
- `RetryIdentityVerificationRequested` is valid from `IdentityVerificationFailed` only when `canRetry == true`.
- `CancelRequested` should be valid from all non-terminal states but may produce different commands depending on current state.
- Internal success/failure events from commands should be ignored or diagnosed when received in incompatible states.

## Command Policy

`Command` represents non-pure work that the ViewModel or command runner executes. The FSM returns commands; it does not run them.

Commands are business or platform-adapter work, not UI rendering.

### Proposed Command Model

```kotlin
sealed interface SignupCommand {
    data class CreateSignupSession(
        val input: SignupInput,
    ) : SignupCommand

    data class RequestIdentityVerification(
        val sessionId: SignupSessionId,
    ) : SignupCommand

    data class CheckIdentityVerificationStatus(
        val sessionId: SignupSessionId,
        val verificationRequestId: VerificationRequestId,
    ) : SignupCommand

    data class CompleteSignup(
        val sessionId: SignupSessionId,
    ) : SignupCommand

    data class CancelSignupSession(
        val sessionId: SignupSessionId,
    ) : SignupCommand
}
```

### Command Execution Policy

- `CreateSignupSession` calls a use case that creates a pending signup session.
- `RequestIdentityVerification` calls a use case/provider adapter that creates a verification request.
- `CheckIdentityVerificationStatus` calls a use case that maps provider/backend status into internal events.
- `CompleteSignup` calls a use case that activates the account.
- `CancelSignupSession` calls a best-effort use case. Its failure should usually be logged, not block UI exit.

### Command Concurrency

Initial policy:

- command execution is serial per ViewModel unless a command is explicitly marked fire-and-forget later,
- duplicate `CreateSignupSession` should be prevented by state transition rules,
- polling/status checks must not overlap for the same `verificationRequestId`,
- cancellation of in-flight commands should be a ViewModel/runner concern, not pure FSM logic.

This policy is intentionally conservative for the first reference flow.

## Effect Policy

`Effect` is reserved for one-shot UI-side actions that cannot be represented as durable state without awkwardness.

The default design avoids ViewModel-owned one-off event streams for core business progress. Durable progress is modeled as `State`.

### Proposed Effect Model

```kotlin
sealed interface SignupEffect {
    data class LaunchIdentityVerification(
        val launchToken: VerificationLaunchToken,
    ) : SignupEffect

    data class ShowMessage(
        val message: UserMessage,
    ) : SignupEffect
}
```

### Effect Rules

- `LaunchIdentityVerification` must be executed by the UI because it may require `ActivityResultLauncher`, external intent, browser, provider SDK, or navigation APIs.
- `ShowMessage` is optional. Prefer modeling important messages as state if they must survive configuration changes.
- Navigation to the next product destination should normally be UI behavior triggered by observing `Completed`, not a required FSM effect.
- Effects must be opt-in in the library. Core FSM should work without an effect channel.

## Transition Sketch

| Current state | Event | New state | Command | Effect |
|---|---|---|---|---|
| `Editing` | field changed | `Editing` | none | none |
| `Editing` | `SubmitSignupRequested` with invalid input | `Editing(fieldErrors)` | none | optional message |
| `Editing` | `SubmitSignupRequested` with valid input | `SubmittingAccount` | `CreateSignupSession` | none |
| `SubmittingAccount` | `SignupSessionCreated` | `ReadyForIdentityVerification` | none | none |
| `SubmittingAccount` | `SignupSessionCreationFailed` | `Editing(fieldErrors or general error)` | none | optional message |
| `ReadyForIdentityVerification` | `StartIdentityVerificationRequested` | `RequestingIdentityVerification` | `RequestIdentityVerification` | none |
| `RequestingIdentityVerification` | `IdentityVerificationRequestCreated` | `AwaitingIdentityResult` | none or first status check | `LaunchIdentityVerification` |
| `RequestingIdentityVerification` | request failed | `IdentityVerificationFailed` | none | optional message |
| `AwaitingIdentityResult` | external success callback | `CompletingSignup` | `CompleteSignup` | none |
| `AwaitingIdentityResult` | external cancel/failure callback | `IdentityVerificationFailed` | none | optional message |
| `AwaitingIdentityResult` | polling tick | `AwaitingIdentityResult` | `CheckIdentityVerificationStatus` | none |
| `AwaitingIdentityResult` | verification succeeded | `CompletingSignup` | `CompleteSignup` | none |
| `AwaitingIdentityResult` | verification timeout/failure | `IdentityVerificationFailed` | none | optional message |
| `IdentityVerificationFailed` | retry requested and allowed | `RequestingIdentityVerification` | `RequestIdentityVerification` | none |
| `IdentityVerificationFailed` | edit input requested | `Editing` | none | none |
| `CompletingSignup` | `SignupCompleted` | `Completed` | none | none |
| `CompletingSignup` | completion failed | `IdentityVerificationFailed` or `ReadyForIdentityVerification` | none | optional message |
| non-terminal | `CancelRequested` | terminal exit handled by UI or prior state | optional `CancelSignupSession` | optional message |

## Invalid Transition Policy

For the reference implementation:

- production default: ignore irrelevant late events and log at debug level,
- debug mode: record invalid transition diagnostics,
- tests: assert important invalid transitions explicitly.

Examples:

- `SignupSessionCreated` received while already `Completed`: ignore and log.
- `IdentityVerificationSucceeded` received while in `Editing`: invalid diagnostic.
- `SubmitSignupRequested` while `SubmittingAccount`: ignore or return same state with diagnostic.

The library should expose invalid transition policy as configuration later, but the reference flow should start with explicit handling in the state machine.

## Saved State Policy

Do not serialize full `SignupState` by default.

Save minimal restoration data:

- current coarse step key,
- `SignupInput` if it is lightweight and user-entered,
- `SignupSessionId` if a pending session exists,
- `VerificationRequestId` if awaiting identity result,
- retry count if needed for business rules.

After process recreation:

- rebuild complex state through repository/use case checks,
- verify whether pending signup session is still valid,
- verify whether identity verification is pending, succeeded, failed, or expired,
- resume into `ReadyForIdentityVerification`, `AwaitingIdentityResult`, `IdentityVerificationFailed`, or `Completed` based on authoritative backend/domain state.

## ViewModel Responsibilities

The ViewModel should:

- expose `StateFlow<SignupState>`,
- accept `SignupEvent`,
- call `SignupStateMachine.transition`,
- update state atomically,
- execute returned commands in `viewModelScope`,
- emit command result events back into the FSM,
- expose effects through the chosen effect policy if effects are enabled,
- integrate `SavedStateHandle` only for minimal restoration keys.

The ViewModel should not:

- contain transition rules scattered across private methods,
- hold `Context`, `NavController`, or `ActivityResultLauncher`,
- directly launch identity UI,
- serialize large state into `SavedStateHandle`.

## UI Responsibilities

The UI should:

- render each `SignupState`,
- send user-originated events,
- launch external identity verification when `LaunchIdentityVerification` is produced,
- feed external result callbacks back as `ExternalIdentityResultReceived`,
- navigate away when observing `Completed`,
- own UI-only state such as focus, keyboard, snackbar host, scroll, and local dialogs.

## Tests Required

### FSM Tests

- `Editing` invalid input stays `Editing` with field errors.
- `Editing` valid submit moves to `SubmittingAccount` and emits `CreateSignupSession`.
- session creation success moves to `ReadyForIdentityVerification`.
- identity start emits `RequestIdentityVerification`.
- verification request success moves to `AwaitingIdentityResult` and emits launch effect if effects are enabled.
- external success moves to `CompletingSignup` and emits `CompleteSignup`.
- external failure moves to `IdentityVerificationFailed`.
- retry from failure increments retry count and emits `RequestIdentityVerification`.
- retry is rejected when `canRetry == false`.
- late async results in incompatible states are ignored or diagnosed according to policy.
- completion success moves to `Completed`.

### ViewModel Tests

- command execution calls the correct use case.
- command success dispatches the correct internal event.
- command failure dispatches the correct failure event.
- `StateFlow` exposes the final current state.
- in-flight command behavior is deterministic under `runTest`.
- `SavedStateHandle` restoration seeds the expected initial state or restoration command.

### UI Tests

- each major state renders expected controls.
- buttons emit expected events.
- identity launch effect invokes the UI launcher.
- external result callback is mapped to `ExternalIdentityResultReceived`.
- `Completed` triggers navigation at the UI boundary.

## Library API Questions Exposed By This Flow

- Should `AfsmTransition` include effects in core, or should effect handling be a ViewModel integration concern?
- Should command execution be configured globally or per command type?
- Should invalid transition diagnostics be part of the core result type?
- Should saved state restoration be a library helper or sample-level guidance?
- Should retry policy be modeled inside domain types, FSM state, or command handler?
