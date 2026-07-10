---
title: Checkout Process Restoration Experiment
updated: 2026-07-10
status: candidate-b-implemented
---

# Checkout Process Restoration Experiment

## Evidence-Backed Problem

Checkout is the reference for dynamic initial state and payment safety, but its
ViewModel currently receives only a constructor `productId`, keeps no
`SavedStateHandle`, and always dispatches `ScreenEntered` during initialization.

That leaves two contradictions with the accepted restoration policy:

- restoring durable `Completed(orderId)` would immediately dispatch an invalid
  `ScreenEntered` event,
- restoring every non-completed flow as `Idle` would hide an interrupted
  payment and could invite an unsafe duplicate submission.

The Draft consumer proves only a text-field key and does not exercise a
representative non-idempotent flow.

## Invariants

- Persist minimal business keys, not a serialized `CheckoutState`, product
  object, coroutine, command, or effect.
- A restored state must not run `onEnter` implicitly.
- Durable completion must render after recreation without loading a product or
  replaying `PaymentCompleted`.
- An interrupted payment must not be retried or converted to a harmless-looking
  ready state automatically.
- Cold navigation still loads the requested product through the explicit
  `ScreenEntered` event.
- `SavedStateHandle` remains feature/ViewModel code; plain Kotlin machine and
  core modules stay Android-free.

## Candidate A: Product Id Only

Persist `productId`, restore `Idle`, and reload on every recreation.

Verdict: insufficient for Checkout. It is acceptable for a load-only screen but
erases the distinction between an ordinary reload and an unresolved payment.

## Candidate B: Minimal Stable And Pending Keys

Persist only:

- `productId`,
- `completedOrderId` after a successful payment result,
- `pendingPaymentRequestId` immediately before calling the payment repository.

Restoration priority:

1. `completedOrderId` -> `Completed(orderId)` with no startup event or effect,
2. pending request -> `PaymentStatusUnknown(requestId)` with no automatic load,
   retry, or submit,
3. otherwise -> `Idle`, followed by explicit `ScreenEntered` product loading.

On a normal success, write `completedOrderId` before dispatching
`PaymentSucceeded`, then remove the pending key. On a normal failure, remove the
pending key before dispatching `PaymentFailed`.

`PaymentStatusUnknown` is intentionally conservative. The sample cannot prove
the server outcome after process death, so it must not offer an automatic retry.
A production app should query an idempotent payment backend or order store
before leaving this phase.

Verdict: selected prototype. It is small enough for the sample and makes the
unsafe uncertainty visible in the product flow.

## Candidate C: Serialize Full Checkout State

Write phase, product, errors, request counters, and payload phases into
`SavedStateHandle`.

Verdict: rejected. It stores repository-derived data, couples persistence to
every machine field, and can resurrect an in-flight command-shaped phase
without its work.

## Candidate D: Generic Afsm Restoration Helper

Add a public serializer/snapshot contract to `afsm-viewmodel`.

Verdict: defer. One Checkout prototype should establish the feature policy and
repeated boilerplate before a library abstraction is justified.

## Prototype Surface

- Add feature-owned key constants and `checkoutStateFromSavedState(...)`.
- Add `PaymentStatusUnknown(requestId)` as a restoration-only phase visible in
  the machine and render state.
- Add a SavedState-aware sample ViewModel factory path.
- Make Checkout startup dispatch conditional on a restored `Idle` phase.
- Persist/clear payment keys around the real command handler.

## Acceptance Criteria

- Cold navigation still loads exactly once with the route product id.
- Restored completion starts in `Completed`, emits no effect, and executes no
  product/payment command.
- A saved pending request starts in `PaymentStatusUnknown`, executes no command,
  and exposes no pay/retry action.
- Successful payment leaves `completedOrderId` and no pending key.
- Ordinary payment failure removes the pending key and remains retryable.
- Machine tests, ViewModel tests, render-state tests, graph generation, sample
  build, and the full local release gate pass.
- Public docs state that the unknown phase requires backend status recovery in
  a production payment flow.

## Evidence Boundary

This prototype can prove feature-owned stable snapshot policy and protection
against automatic duplicate work. It cannot simulate OS process death or prove
a real payment backend's idempotency/status-query contract.

## Implementation Result

Candidate B is implemented in `sample-shop`:

- Checkout restores from the three feature-owned keys with
  `completed > pending > fresh` priority,
- `PaymentStatusUnknown(requestId)` is a declared restoration-only machine
  phase with no pay/retry handler,
- fresh `Idle` alone receives `ScreenEntered`,
- pending is written immediately before repository payment work and cleared on
  normal success/failure,
- completion is stored before the success event is dispatched,
- the route uses the AndroidX SavedState-aware ViewModel factory DSL,
- focused tests, full sample tests, graph generation, APK assemble, and the full
  local release gate pass.

On-device launch remains unverified because the official Android CLI could not
discover the booted emulator. Real payment status recovery remains a backend
contract, not an Afsm runtime claim.
