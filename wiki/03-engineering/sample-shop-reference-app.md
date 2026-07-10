---
title: Sample Shop Reference App
updated: 2026-07-10
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

Current implementation:

- `AuthState` is a feature-local typealias for `AfsmState<AuthPhase, AuthData>`.
- `authStateMachine` is the executable `AfsmDefaultMachine` property directly.
- `authStateMachine` is annotated with `@AfsmGraph` and writes `AuthStateMachine.mmd` through the generated registry.
- `ignore(...)` and `invalid(...)` preserve existing ignored/invalid transition decisions without adding graph edges.
- Route-level effects are collected with `CollectAfsmEffects(...)` from `afsm-compose`.

State model:

- `Editing`
- `Submitting`
- `Authenticated`

Flow:

```text
User input
-> AuthEvent
-> authStateMachine
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

- `ProductEditorState` is a typealias to `AfsmState<ProductEditorPhase, ProductEditorData>`.
- `ProductDraft` and validation errors live in `ProductEditorData`.
- Flow phases remain explicit phase values; `SavingDraft` and `DraftSaved` are not hidden as data flags.
- Event branches use named `case(...)` blocks when there are domain alternatives; `transitionTo(...)` only changes phase.
- Graph-relevant submit/resubmit transitions remain inline in event branches; helpers should transform data, not hide phase movement.
- Validation failure uses an explicit no-transition `case(label = "invalid ...", condition = ...)` that updates data; it should not be represented as a second competing `transitionTo`.
- `onEnter` owns phase-entry work. Image upload uses keyed `invoke`, while
  shorter save/review/publish work remains ordinary sequential commands.
- `CancelUploadClicked` returns to `EditingDraft`; runtime phase exit cancels
  the cooperative upload without a cancel command or ViewModel `Job` map.
- `ProductImageUploader` is a feature-owned suspend boundary injected by the
  route. ViewModel tests use controllable start/cancel signals, map ordinary
  failures to fixed safe UI data, and rethrow cancellation.
- `productEditorStateMachine` is the annotated `AfsmDefaultMachine` property
  and implements `AfsmGraphSource` through `AfsmMachine`.
- KSP generates `AfsmGeneratedGraphRegistry` from annotated state-machine classes.
- `./gradlew :sample-shop:generateAfsmMmd` writes registry entries such as `sample-shop/build/generated/afsm/mmd/ProductEditorStateMachine.mmd`.
- MMD output includes the initial node, meaningful guard labels,
  command/effect labels, and `invoke` entry plus automatic cancellation exit
  notes.

Text edits are handled branches inside editable phases, while submit/review/publish actions move between explicit phases.

Transition action naming:

- `ImageUploadInProgress` is paired with `StartImageUpload`.
- `ReviewSubmissionInProgress` is paired with `StartReviewSubmission`.
- `PublishInProgress` is paired with `StartProductPublish`.

This keeps phase state names separate from host-executed transition actions.

## Checkout Flow

Checkout files:

- `CheckoutContract.kt`
- `CheckoutRestoration.kt`
- `CheckoutStateMachine.kt`
- `CheckoutViewModel.kt`
- `CheckoutScreen.kt`

Flow:

```text
ScreenEntered
-> ProductLoading phase
-> LoadProduct command
-> ProductLoaded/ProductUnavailable
-> ProductReady/ProductUnavailable phase
-> PayClicked
-> PaymentInProgress(requestId) phase
-> SubmitPayment command
-> PaymentSucceeded/PaymentFailed
-> Completed(orderId) phase + PaymentCompleted effect
   or PaymentFailed retry state

process recreation with pending payment
-> PaymentStatusUnknown(requestId) phase
-> no automatic command or retry action
```

The fake payment repository fails the first attempt for higher-priced products so retry behavior is visible without external services.

Checkout now carries a payment `requestId` through `SubmitPayment`,
`PaymentSucceeded`, and `PaymentFailed`. The state machine ignores stale command
results whose request id no longer matches the active payment request. This is
the first official sample pattern for long-running command result safety.

Checkout completion is now durable state plus effect. The screen renders the
completed order if the navigation effect is missed, and duplicate pay/retry
events after completion are ignored.

Checkout is now a graphable DSL machine:

- `CheckoutState` is `AfsmState<CheckoutPhase, CheckoutData>`.
- `checkoutStateMachine` is an `@AfsmGraph` top-level `AfsmMachine` property.
- `CheckoutViewModel` uses `afsmHost(machine = checkoutStateMachine,
  initialState = checkoutStateFromSavedState(...))`.
- `CheckoutViewModel` derives that explicit state from navigation plus
  `SavedStateHandle`, persisting only product, completed-order, and pending
  request ids.
- The machine declares only `initialPhase = CheckoutPhase.Idle`; it has no fake
  `productId = 0` default, and the no-state host overload is unavailable.
- `ProductLoading` emits `LoadProduct` from `onEnter`.
- `PaymentInProgress(requestId)` emits `SubmitPayment` from `onEnter`.
- `PaymentStatusUnknown(requestId)` is restoration-only and blocks automatic
  duplicate payment work until a production backend can resolve status.
- `CheckoutState.toRenderState()` keeps Compose rendering independent from internal phase details.
- `./gradlew :sample-shop:generateAfsmMmd` writes `sample-shop/build/generated/afsm/mmd/CheckoutStateMachine.mmd`.

## Public API Feedback

Current feedback from the sample:

- `AfsmTransition<S, C, F>` is readable when each feature declares a local typealias.
- Primary examples now hide raw transition type noise behind graphable machine
  properties. Static flows expose `AfsmDefaultMachine`; Checkout exposes base
  `AfsmMachine` because its initial data must come from navigation.
- `Command` keeps transition functions pure and avoids suspend state machines.
- `Effect` is useful for navigation completion but should remain rare.
- `ViewModel.afsmHost(...)` is a good baseline API.
- `ViewModel.afsmHost(machine = ..., initialState = ...)` is the preferred API when navigation arguments affect the first state.
- The standard `AfsmState<Phase, Data>` model removes ProductEditor adapter boilerplate while keeping the state diagram focused on phases.
- Auth now confirms the same direct `AfsmState<Phase, Data>` approach works for simpler flows too.
- `CollectAfsmEffects(...)` removes repeated lifecycle-effect collection wiring from Compose routes.
- Checkout is the mid-size public example for dynamic initial state, retry,
  request ids, durable completion, conservative process restoration, and
  render-state mapping.

## Verification

Current verification:

```bash
./scripts/verify-release-local.sh --warning-mode all
```

Result:

```text
BUILD SUCCESSFUL
```

`CheckoutViewModelTest` additionally verifies navigation-derived product state,
real repository command-result wiring over fake DAOs, missing-session failure,
durable completion, active effect delivery, completed/pending saved-state
restoration, and no automatic work from unknown payment status. The full
release gate including the clean external consumer passed.

Android CLI journey verification:

- [[../05-qa/verification-report-2026-05-09-sample-shop-fsm-smoke|Sample Shop FSM Smoke Verification]]
- [[../05-qa/verification-report-2026-05-09-product-editor-executable-dsl-smoke|ProductEditor Executable DSL Smoke Verification]]
- [[../05-qa/verification-report-2026-07-11-product-editor-upload-boundary|ProductEditor Upload Boundary Verification]]

## Next Gaps

- Consider a public codelab that walks through ProductEditor from contract to graph output after Checkout onboarding is stable.
- Replace the demo uploader with a real transport/SDK only inside the selected
  production-like pilot, with explicit remote cancellation and idempotency
  evidence.
