# Checkout First-Use Protocol Dry Run

Date: 2026-07-10

Repository baseline: `bdaf6a9` (`docs: define Checkout first-use protocol`)

## Purpose

Verify that a facilitator can prepare the participant session exactly as
documented. This dry run does not simulate or score a human participant.

## Graph Preparation

```bash
./gradlew :sample-shop:generateAfsmMmd --no-daemon
```

Result: `BUILD SUCCESSFUL`.

## Static Checks

- Participant task files present and non-empty: 3/3.
- Timed participant questions: 10.
- Rubric evidence checks present in the constrained artifacts: 9/9.
- Generated graph lines: 24.
- Generated graph SHA-256:
  `839ebc1cff3cc24dc83d4936274240b6e2a8d936665ef7449105b9841736c192`.
- Graph generation left the Git worktree unchanged.

The evidence checks covered the initial phase, `LoadProduct`, `SubmitPayment`,
`PaymentCompleted`, matching request ids, all three decision assertions, and
the product-unavailable branch.

## Evidence Boundary

This proves that the task can be prepared and that its answer rubric is grounded
in the three allowed artifacts. It provides no human timing, comprehension,
preference, hesitation, or adoption evidence.
