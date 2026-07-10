# Checkout Product-Goal Fit Review Evidence

Date: 2026-07-10

Repository baseline: `56cda3b` (`docs: document dynamic machine initialization`)

## Reading Constraint

The review intentionally read only these three Checkout artifacts:

- `sample-shop/src/main/kotlin/afsm/sample/shop/feature/checkout/CheckoutStateMachine.kt`
- `sample-shop/build/generated/afsm/mmd/CheckoutStateMachine.mmd`
- `sample-shop/src/test/kotlin/afsm/sample/shop/feature/checkout/CheckoutStateMachineTest.kt`

It did not use the Contract, ViewModel, Screen, walkthrough, or architecture
pages to reconstruct the flow.

## Reconstructed Flow

- The host supplies runtime data and the graph starts at `Idle`.
- `ScreenEntered` enters `ProductLoading`; entry emits `LoadProduct`.
- Load success enters `ProductReady`; unavailable product enters the terminal
  `ProductUnavailable` phase.
- Paying with a loaded product increments a request id, enters
  `PaymentInProgress`, and emits `SubmitPayment` on entry.
- Matching success enters durable `Completed` state and emits
  `PaymentCompleted`.
- Matching failure enters `PaymentFailed`; retry increments the request id and
  submits again.
- Stale payment results, duplicate in-flight actions, and duplicate actions
  after completion are intentionally ignored.

## Evidence Boundary

The machine and graph make the main path, guards, commands, effect, retry, and
request-id policy readable. Existing tests prove the main path and stale-result
safety.

The review is only a conditional pass because the graph intentionally omits
non-transition decisions and the tests do not yet cover every important
graph-invisible rule. In particular, the current tests do not demonstrate:

- `Idle` pay/retry handling before a product is loaded,
- `ProductUnavailable` as an invalid-event terminal phase,
- duplicate pay/retry events while payment is in flight,
- late payment results after completion.

This is repository-based evidence, not human first-use preference evidence.
