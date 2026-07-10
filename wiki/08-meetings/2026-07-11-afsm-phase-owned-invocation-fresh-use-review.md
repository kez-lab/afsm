---
title: Afsm Phase-Owned Invocation Fresh-Use Review
updated: 2026-07-11
status: repository-pass
---

# Afsm Phase-Owned Invocation Fresh-Use Review

## Scope

Read ProductEditor machine, generated graph, transition tests, and ViewModel
cancellation test as an Android developer evaluating an interruptible upload.

This is a repository-based AI review, not a human first-use session or proof
that a real network/SDK operation honors cancellation.

## What Became Clearer

- `invoke` distinguishes long-lived phase-owned work from ordinary sequential
  commands at the declaration site.
- `CancelUploadClicked -> EditingDraft` is visible as a business-flow edge;
  there is no feature-level `CancelImageUpload` plumbing command.
- The graph note pairs `entry / invoke StartImageUpload` with
  `exit / cancel product-editor/image-upload`.
- `AfsmCommandInvocation.Start/Cancel` makes lifetime output directly
  assertable in pure machine tests.
- The ViewModel only executes `StartImageUpload`; it has no `Job` map or custom
  cancellation branch.
- Runtime tests separate local cancellation, ordinary command ordering, late
  dispatch rejection, and host closure.

## What Became Harder

- Advanced authors must learn `invoke`, `AfsmInvocationKey`,
  `commandInvocations`, and a second test assertion in addition to `command`.
- A string-backed key is explicit and graphable but adds ceremony when one phase
  owns only one invocation.
- Every phase exit emits `Cancel(key)` in pure transition output, including
  success/failure exits after the handler may already have completed; runtime
  cancellation is intentionally a no-op in that case.
- Cancellation requests do not join non-cancellable cleanup, and invocation
  jobs are outside the ordinary command queue capacity.
- Local coroutine cancellation cannot communicate whether remote work stopped,
  so request ids and idempotency guidance remain necessary.

## Verdict

Accept the bounded Candidate D implementation provisionally. It fixes a real
false promise in the previous cancel-command guidance and keeps complexity out
of Draft/Auth/Checkout. ProductEditor is an appropriate advanced sample because
the graph and UI both benefit from a visible cancel path.

Do not expand this into actors, restart strategies, hierarchy, or arbitrary
event-branch invocations without pilot evidence. Before API freeze, test whether
the explicit key is worth its ceremony or can be safely derived from phase plus
label.

## Remaining Evidence

- Human developer comprehension and preference for `invoke` terminology.
- Production-like upload or polling integration with a real cancellation API.
- Non-cooperative callback/SDK behavior and request-id fallback.
- Whether a phase-derived key can reduce authoring cost without hiding
  ownership or making graph output unstable.
