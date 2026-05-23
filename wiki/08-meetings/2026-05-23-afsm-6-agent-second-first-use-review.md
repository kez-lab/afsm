---
title: Afsm 6-Agent Second First-Use Review
updated: 2026-05-23
---

# Afsm 6-Agent Second First-Use Review

## Prompt

Read the whole project again from the perspective of an Android developer
seeing Afsm for the first time. Identify where understanding cost remains, then
turn the review into an improvement plan and implementation.

## Review Roles

- Beginner Android app developer: can I start from README/getting-started
  without knowing statecharts?
- Senior Android architect: does the API still fit ViewModel/UDF boundaries?
- Kotlin API reviewer: are DSL receivers and signatures safe and idiomatic?
- Sample-app POC reviewer: can I copy the right sample for the right problem?
- Documentation reviewer: does the reading path build concepts in the right
  order?
- CTO/adoption reviewer: would a team pilot Afsm on a complex screen?

## Shared Findings

- The public model is closer, but first-use docs still exposed too much
  Checkout/payment domain before the small mental model was stable.
- `case(...)` needed to be explained as a graphable `if` branch, not as a
  framework-only concept.
- Conditions and payload phase factories should be read-only. Letting those
  lambdas receive the full transition scope implied users could mutate state
  from predicates or phase construction.
- Phase payload rules needed to be explicit: payloads should identify a phase
  instance, while durable render/data values stay in `Data`.
- ProductEditor should be labeled as an advanced graph stress test, not as the
  first copy-paste sample.
- Checkout used too many `ignore(...)` declarations. That taught event-matrix
  enumeration instead of the intended rule: omit invalid events and only ignore
  expected duplicate/stale events.
- Public ViewModel examples should expose explicit `StateFlow<State>` and
  `Flow<Effect>` types so Android integration is visible.

## Accepted Changes

- Make `case(condition = ...)`, `updateData(condition = ...)`, `ignore(...)`,
  and `invalid(...)` use a read-only `AfsmConditionScope`.
- Make `transitionTo<PayloadPhase> { ... }` use a read-only
  `AfsmPhaseFactoryScope`.
- Rewrite `docs/getting-started.md` around a minimal Draft flow before
  Checkout.
- Add API choice tables that explain when to use `transitionTo`, `updateData`,
  `case`, `command`, `effect`, and `ignore`.
- Reduce Checkout no-op declarations to expected duplicates, stale async
  results, and terminal duplicate actions.
- Add explicit public `state` / `effects` types in sample ViewModels.
- Update public KDoc and docs so `ignore` means expected harmless no-op, while
  omitted handlers remain invalid by default.

## Deferred

- A dedicated pilot checklist page may still be useful before external trials,
  but `docs/release-readiness.md` already contains the internal beta adoption
  contract.
- SavedStateHandle restoration can use the existing dynamic `initialState`
  API, but a concrete restore sample is still future work.
- Command handler extraction remains a sample-level choice; adding a library
  helper now may increase API surface before repeated consumer evidence exists.

## CTO Synthesis

Afsm is more pilotable after this pass because the beginner path now starts
with one small statechart and the DSL's mutating/read-only scopes are clearer.
The project is still internal beta. External adoption still needs a real
consumer pilot, final release identity, and restoration examples, but the
current direction is simpler and more Android-readable than the previous pass.
