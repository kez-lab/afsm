---
title: Historical Checkout ViewModel Integration Verification
updated: 2026-07-17
status: historical-four-channel-verification
---

# Checkout ViewModel Integration Verification

> Historical verification snapshot for the pre-2026-07-17 Checkout Effect
> channel. The repository scenarios remain useful evidence for product-id and
> repository-result wiring, but completion now exists only as durable state.
> Use
> [[afsm-output-model-simplification|Afsm Output Model Simplification]] and
> [[../05-qa/verification-report-2026-07-17-effect-free-output-model|Effect-Free Output Model Verification 2026-07-17]]
> for the maintained contract.

## Evidence Gap

Checkout is the reference flow for runtime-supplied initial state, async
commands, request ids, retries, durable completion, and a one-shot effect. Its
plain machine tests are strong, and `afsm-viewmodel` has a generic dynamic-state
fixture, but the sample had no `CheckoutViewModelTest` at the planning baseline.

The suite at that baseline did not prove that the real Android adapter:

- seeds Checkout with the navigation product id,
- executes `LoadProduct` and maps its result back to a typed event,
- executes `SubmitPayment` only with a session,
- exposes durable completed state and the completion effect,
- maps a missing session to the existing payment-failure branch.

This is an integration-evidence gap, not evidence of a production bug.

## Test Boundary

The new tests should exercise the real `CheckoutViewModel`, repositories, and
`ViewModel.afsmHost(...)` composition while replacing Room DAOs with small
in-memory fakes. They should use `Dispatchers.setMain` and coroutine test time;
Robolectric and a real database are unnecessary for this boundary.

Machine branch details already covered by `CheckoutStateMachineTest` must not be
duplicated wholesale.

## Required Scenarios

1. Constructor `productId` survives dynamic initialization, drives exactly the
   expected product lookup, and reaches `ProductReady` with loaded data.
2. With a valid session, `PayClicked` executes payment, reaches durable
   `Completed(orderId)`, persists the expected order input, and emits one
   `PaymentCompleted(orderId)` effect to an active collector.
3. Without a session, `PayClicked` reaches `PaymentFailed` with the accepted
   login-required message and never inserts an order.
4. A missing product reaches `ProductUnavailable` through the real load-command
   result mapping.

## Acceptance Criteria

- Focused Checkout ViewModel tests pass on the JVM.
- The tests use production repositories over fake DAO boundaries rather than
  reimplementing repository behavior in test-only ViewModel doubles.
- No production change is made unless a test exposes a classified bug.
- The existing Checkout machine suite and generated graph remain green.
- The relevant broader sample suite passes before the milestone closes.

## Evidence Boundary

Passing these tests will prove Android adapter wiring in the repository sample.
It will not prove process recreation, real Room behavior, Compose rendering, or
human pilot usefulness.

## Verification Result

Commit `62e73d1` added the four planned scenarios using production repositories
over recording DAO fakes. No production behavior changed.

The focused test, complete sample JVM suite plus graph generation, and full
local release gate all passed. The full gate included API checks, Maven Local
publication, and the clean external consumer build.

The planned Android adapter evidence gap is closed. Process recreation, real
Room integration, Compose rendering, and human pilot evidence remain outside
this verification boundary.
