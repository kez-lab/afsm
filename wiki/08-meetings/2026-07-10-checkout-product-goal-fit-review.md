---
title: Checkout Product Goal Fit Review
updated: 2026-07-10
status: repository-pass
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

## Initial Gaps

At the initial `56cda3b` baseline, the result was a conditional pass.

The graph intentionally omits handled self-updates, `ignore(...)`, and invalid
events because they are not phase transitions. That keeps the diagram useful,
but it means the tests must carry the executable negative-policy explanation.
Tests at that baseline did not demonstrate every important graph-invisible rule:

- pay/retry before the product is loaded,
- invalid input in terminal `ProductUnavailable`,
- duplicate pay/retry while payment is in flight,
- late payment results after completion.

The exact type definitions and UI rendering cannot be recovered under this
reading constraint, but they are not required to explain the business-flow
topology. ViewModel command execution and lifecycle behavior remain separate
integration concerns.

## Follow-Up Result

Commit `aa7bf3b` added focused transition tests for all four gaps without
changing production code or the Mermaid graph. The focused Checkout suite and
graph generation pass together.

On a repeated constrained read, the test names and assertions now make these
distinctions explicit:

- state-preserving prerequisite feedback is `Handled`,
- an event rejected by terminal `ProductUnavailable` is `Invalid`,
- expected duplicate or stale work is `Ignored`.

The later restoration slice adds one intentional orphan graph state:
`PaymentStatusUnknown`. Its machine test explains that it comes from restored
pending work and rejects retry. The three-artifact path therefore still explains
the important business policy without reading the ViewModel persistence keys;
the exact SavedStateHandle mapping remains an Android integration concern.

### Restoration Fresh-Use Follow-Up

What became clearer:

- interrupted payment uncertainty is a named phase instead of an invisible
  ViewModel fallback,
- the machine test proves that retry is invalid there,
- durable completion and one-shot effect replay have separate restoration
  behavior.

What became harder:

- the Mermaid graph shows `PaymentStatusUnknown` without an incoming edge, so
  the graph alone cannot say that Android restoration supplies it,
- the exact three saved keys remain outside the machine/graph/test
  comprehension constraint by design.

Repository verdict: keep the conservative phase. Adding fake runtime edges to
the graph would misrepresent executable events. The human first-use task now
asks directly about the orphan state; repeated confusion would justify a future
topology note/entry-source experiment. No such graph API is added from this AI
review alone.

## Product Verdict

The artifact split is directionally correct:

- graph for macro flow,
- machine for executable rules and outputs,
- tests for scenarios and graph-invisible decisions.

Afsm now passes this repository-based readability check for Checkout's main
flow, recovery flow, and important safety rules. The graph remains concise
while the machine and tests explain policies that do not produce graph edges.

## Next Acceptance Slice

The focused negative-policy slice is complete. The next product-evidence step
is a real Android developer first-use session using the same three-artifact
constraint and a recorded comprehension rubric.

Human first-use preference remains unverified and must not be inferred from
this repository review.
