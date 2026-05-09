---
title: AI Engineering Guardrails
updated: 2026-05-09
---

# AI Engineering Guardrails

This page defines project-scoped rules for AI agents working on Afsm.

The goal is not to slow development down. The goal is to prevent the most damaging AI failure mode: changing the verification target when the implementation fails.

## Non-Negotiable Rule

Tests are executable specification.

An agent must not delete, skip, weaken, or rewrite tests only to make a failing build pass.

Acceptable test changes are limited to:

- the product/spec changed and the wiki or decision log was updated first,
- the old test contradicted the accepted spec,
- the old test had a harness/timing issue that did not represent product behavior,
- the test was expanded to cover more behavior while preserving the original assertion's intent.

## Required Failure Triage

When a test fails, classify it before editing code:

1. Implementation bug: production code violates the current spec. Fix production code.
2. Test harness issue: timing, coroutine scope, fake setup, fixture, or environment is invalid. Fix the harness without weakening the behavioral assertion.
3. Stale spec: the desired behavior changed. Update wiki/spec/decision first, then update tests.
4. Ambiguous requirement: stop and ask or write the assumption explicitly before changing tests.

Do not silently treat category 1 as category 2.

## TDD-Oriented Development Loop

For new behavior:

1. Read the relevant wiki/spec/API draft.
2. State the expected behavior in a focused test name.
3. Add or update the smallest test that expresses that behavior.
4. Run the focused test and confirm the failure when practical.
5. Implement the smallest production change.
6. Run the focused test.
7. Run the relevant module test suite.
8. Update wiki/log/decision pages if behavior or process changed.

For bug fixes:

1. Reproduce the bug with a failing regression test or an existing failing test.
2. Keep that regression test.
3. Fix production code.
4. Run focused and broader tests.
5. Document the regression if it teaches a durable rule.

## Test Change Policy

Before changing a test, answer these in the work notes or final summary:

- What behavior did the old test protect?
- Which accepted spec says that behavior is now wrong or incomplete?
- Does the new test preserve the original intent or intentionally replace it?
- Did any production behavior change because of this test update?

If these cannot be answered, do not change the test yet.

## Runtime and Coroutine Tests

Afsm uses long-lived coroutine hosts. Test scopes must model that lifetime honestly.

Rules:

- Use `runTest` and a shared `TestCoroutineScheduler`.
- Prefer `StandardTestDispatcher` for runtime ordering tests.
- Attach long-lived hosts to a dedicated test `CoroutineScope`, not to a short scenario block that expects all child jobs to finish.
- Cancel owned test scopes at the end of tests.
- Do not "fix" coroutine tests by removing ordering assertions unless the accepted runtime policy changed.

## Spec Sources

Prefer this order when resolving conflicts:

1. User instruction in the current thread.
2. Accepted decision pages under `wiki/06-project/decision-log.md`.
3. Current state and open questions under `wiki/00-context/`.
4. Engineering pages under `wiki/03-engineering/`.
5. Existing tests as executable spec.
6. Implementation details.

If implementation and tests disagree, implementation is not automatically right.

## Completion Bar

A task is not complete until:

- relevant tests pass,
- failing tests are not bypassed,
- durable behavior/process changes are synced to the wiki,
- the final summary names what was verified,
- the next concrete task is recommended.
