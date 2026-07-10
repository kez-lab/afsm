---
title: Checkout Product Goal Fit Review
updated: 2026-07-10
status: conditional-pass
---

# Checkout Product Goal Fit Review

## Question

Can an Android developer explain Checkout's complete business flow by reading
only its executable machine, generated graph, and transition tests?

This is a direct product-goal check. Afsm succeeds only if those artifacts make
a complex flow easier to read and verify than scattered ViewModel mutations.

## Review Constraint

The review used only:

1. `CheckoutStateMachine.kt`
2. generated `CheckoutStateMachine.mmd`
3. `CheckoutStateMachineTest.kt`

It deliberately did not read the Contract, ViewModel, Compose screen, or
walkthrough while reconstructing the flow.

## Reconstructed Flow

| Concern | What can be explained from the three artifacts |
|---|---|
| Start | The graph begins at `Idle`; runtime state supplies the product id. |
| Product load | `ScreenEntered` enters `ProductLoading`; entry emits `LoadProduct`; success enters `ProductReady`; unavailable product enters `ProductUnavailable`. |
| Payment | `PayClicked` with a loaded product increments the request id, enters `PaymentInProgress`, and emits `SubmitPayment` on entry. |
| Completion | A matching success enters durable `Completed` state and emits `PaymentCompleted`. |
| Failure and retry | A matching failure enters `PaymentFailed`; retry increments the request id and submits again. |
| Concurrency safety | Stale success/failure results compare request ids and are ignored. Duplicate in-flight and post-completion actions are intentionally ignored in the machine. |

## What Works

- The 24-line Mermaid graph gives the main path and recovery path faster than
  reading imperative ViewModel branches.
- The machine is the single source for phase rules, guards, data updates,
  commands, effect, and topology.
- Entry commands sit next to the phases that own the work.
- Request-id matching and stale-result handling are explicit rather than hidden
  inside coroutine callbacks.
- Tests use business-language names and verify state, command, effect, and
  decision separately.

## Gaps

The result is a conditional pass, not a full pass.

The graph intentionally omits handled self-updates, `ignore(...)`, and invalid
events because they are not phase transitions. That keeps the diagram useful,
but it means the tests must carry the executable negative-policy explanation.
Current tests do not yet demonstrate every important graph-invisible rule:

- pay/retry before the product is loaded,
- invalid input in terminal `ProductUnavailable`,
- duplicate pay/retry while payment is in flight,
- late payment results after completion.

The exact type definitions and UI rendering cannot be recovered under this
reading constraint, but they are not required to explain the business-flow
topology. ViewModel command execution and lifecycle behavior remain separate
integration concerns.

## Product Verdict

The artifact split is directionally correct:

- graph for macro flow,
- machine for executable rules and outputs,
- tests for scenarios and graph-invisible decisions.

Afsm currently meets the core readability goal for Checkout's main and recovery
flows. It does not yet fully meet the stronger claim that all safety rules can
be verified from the same three-artifact reading path.

## Next Acceptance Slice

Add focused Checkout transition tests for the four missing negative-policy
areas. Do not add noisy no-op edges to the main Mermaid graph. Re-run the same
three-artifact review after the tests pass.

Human first-use preference remains unverified and must not be inferred from
this repository review.
