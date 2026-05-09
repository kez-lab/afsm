---
title: Sample Shop Reference App
updated: 2026-05-09
---

# Sample Shop Reference App

The `:sample-shop` module is the first complex Android reference app for Afsm.

Its purpose is to test whether Afsm remains understandable when used inside a real Compose + ViewModel + Room application, not only in isolated pseudo-code.

## Scope

The sample covers:

- signup and login
- Room-backed users, products, favorites, reviews, and orders
- fake payment with failure and retry
- product list
- product registration
- product detail
- likes
- review registration
- review list

## Architecture

The app follows the current Afsm architecture rule:

- plain Kotlin state machines own flow rules
- `ViewModel` owns Android lifecycle bridging, `StateFlow`, `viewModelScope`, command execution, and repositories
- Compose screens render state and emit events/callbacks
- Room repositories are the local source of truth
- navigation passes IDs, not domain objects

Manual DI is used through `ShopAppContainer` to avoid introducing Hilt/Koin before the library API is stable.

## Afsm Usage

Afsm is used for screens where transition correctness is the core behavior:

- `feature/auth/AuthStateMachine.kt`
- `feature/editor/ProductEditorStateMachine.kt`
- `feature/checkout/CheckoutStateMachine.kt`

Ordinary ViewModel + Flow is used for data-oriented screens:

- `feature/catalog`
- `feature/product`

This is an intentional product signal. Afsm should clarify complex flows without forcing simple screens into FSM ceremony.

## Auth Flow

Auth files:

- `AuthContract.kt`
- `AuthStateMachine.kt`
- `AuthViewModel.kt`
- `AuthScreen.kt`

State model:

- `Editing`
- `Submitting`
- `Authenticated`

Flow:

```text
User input
-> AuthEvent
-> AuthStateMachine
-> AuthCommand.Login/Register
-> AuthViewModel command handler
-> AuthRepository
-> AuthSucceeded/AuthFailed
-> AuthState + AuthEffect.OpenCatalog
```

The sample validates that `ViewModel.afsmHost(...)` reads naturally inside a real Android `ViewModel`.

## Product Registration Flow

Product editor files:

- `ProductEditorContract.kt`
- `ProductEditorStateMachine.kt`
- `ProductEditorViewModel.kt`
- `ProductEditorScreen.kt`

State model:

- `EditingDraft`
- `SavingDraft`
- `DraftSaved`
- `ImageUploadInProgress`
- `ReviewSubmissionInProgress`
- `Rejected`
- `Approved`
- `PublishInProgress`
- `Published`

Flow:

```text
EditingDraft
-> SavingDraft
-> DraftSaved
-> ImageUploadInProgress
-> ReviewSubmissionInProgress
-> Rejected
-> ImageUploadInProgress
-> ReviewSubmissionInProgress
-> Approved
-> PublishInProgress
-> Published
```

This flow is now the stronger sample for explaining why Afsm exists.

The ProductEditor sample now uses the v3 executable DSL:

- `ProductEditorState = ProductEditorPhase + ProductEditorContext`.
- `ProductDraft` and validation errors live in `ProductEditorContext`.
- Flow phases remain explicit phase values; `SavingDraft` and `DraftSaved` are not hidden as context flags.
- Event branches are declared with `transitionTo(...)`, `transitionTo<PayloadPhase>(phase = { ... })`, `stay(...)`, and `otherwise(...)`.
- `onEnter` owns phase-entry command emission.
- `ProductEditorStateMachine` is annotated with `@AfsmGraph` and implements `AfsmGraphSource`.
- KSP generates `AfsmGeneratedGraphRegistry` from annotated state-machine classes.
- `./gradlew :sample-shop:generateAfsmMmd` writes registry entries such as `sample-shop/build/generated/afsm/mmd/ProductEditorStateMachine.mmd`.

Text edits are stayed branches inside editable phases, while submit/review/publish actions move between explicit phases.

Transition action naming:

- `ImageUploadInProgress` is paired with `StartImageUpload`.
- `ReviewSubmissionInProgress` is paired with `StartReviewSubmission`.
- `PublishInProgress` is paired with `StartProductPublish`.

This keeps phase state names separate from host-executed transition actions.

## Checkout Flow

Checkout files:

- `CheckoutContract.kt`
- `CheckoutStateMachine.kt`
- `CheckoutViewModel.kt`
- `CheckoutScreen.kt`

Flow:

```text
ScreenEntered
-> LoadProduct command
-> ProductLoaded/ProductUnavailable
-> PayClicked
-> SubmitPayment command
-> PaymentSucceeded/PaymentFailed
-> PaymentCompleted effect or retryable error state
```

The fake payment repository fails the first attempt for higher-priced products so retry behavior is visible without external services.

## Public API Feedback

Current feedback from the sample:

- `AfsmTransition<S, C, F>` is readable when each feature declares a local typealias.
- `Command` keeps transition functions pure and avoids suspend state machines.
- `Effect` is useful for navigation completion but should remain rare.
- `ViewModel.afsmHost(...)` is a good baseline API.
- A Compose/lifecycle effect collection helper or official snippet is now worth considering.

## Verification

Current verification:

```bash
./gradlew test :sample-shop:assembleDebug --warning-mode all --no-daemon
```

Result:

```text
BUILD SUCCESSFUL
```

Android CLI journey verification:

- [[../05-qa/verification-report-2026-05-09-sample-shop-fsm-smoke|Sample Shop FSM Smoke Verification]]
- [[../05-qa/verification-report-2026-05-09-product-editor-executable-dsl-smoke|ProductEditor Executable DSL Smoke Verification]]

## Next Gaps

- Add a public README-level tutorial using the Auth flow.
- Consider a small lifecycle-aware effect collection helper for Compose users.
