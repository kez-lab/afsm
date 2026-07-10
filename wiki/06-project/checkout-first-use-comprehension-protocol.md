---
title: Checkout First-Use Comprehension Protocol
updated: 2026-07-10
status: dry-run-passed-awaiting-human
---

# Checkout First-Use Comprehension Protocol

## Purpose

Measure whether an Android developer with no prior Afsm experience can recover
Checkout's important business flow from only its machine, generated graph, and
transition tests. This tests the product claim directly; it is not a Kotlin or
FSM theory exam.

The participant receives
`docs/checkout-first-use-participant-task.md`. The facilitator keeps this page
private until scoring is complete because it contains the answer rubric.

## Participant Profile

- Android developer familiar with Kotlin, `ViewModel`, `StateFlow`, and
  repository/use-case calls.
- No previous contribution to Afsm and no prior reading of its docs or sample.
- Record years of Android experience and recent experience with MVI/UDF or
  state-machine libraries; do not exclude a participant based on either.

Run at least one session without AI assistance. AI-assisted sessions may be
useful separately, but their prompts and tools must be recorded and their
results must not be combined with unaided first-use timing.

## Session Rules

Before the participant arrives, use a clean checkout and generate the graph:

```bash
./gradlew :sample-shop:generateAfsmMmd --no-daemon
```

Confirm that all three task files exist, then hide the setup terminal so it
does not reveal a preferred artifact or command.

1. Use that prepared checkout without making further source changes.
2. Give only the participant task and the three listed files.
3. Start timing when the participant opens the first artifact.
4. Do not define Afsm terms, suggest a reading order, or point to lines.
5. Neutral clarification of the written task is allowed and must be logged.
6. Stop timing when the participant submits the ten answers.
7. Run the confidence/debrief questions only after the timed answers are fixed.

Screen recording is optional. Preserve no proprietary app data or personal
identifiers in repository evidence.

## Scoring Rubric

Score ten points before discussing the answers:

| Area | Points | Required understanding |
|---|---:|---|
| Initialization | 1 | Starts in `Idle`; real product id is supplied with runtime initial state from outside the machine. Naming the host API is not required. |
| Main path | 2 | `Idle -> ProductLoading -> ProductReady -> PaymentInProgress -> Completed`, including product-unavailable and payment-failure branches. |
| Work and outputs | 2 | `LoadProduct` and `SubmitPayment` are entry commands; completion is durable `Completed(orderId)` state plus one-shot `PaymentCompleted`. |
| Retry identity | 2 | Retry increments the request id; success/failure changes state only when its id matches; stale results are ignored. |
| Decision policy | 2 | Correct examples and meanings for state-preserving `Handled`, expected no-op `Ignored`, and impossible `Invalid`. |
| Artifact roles | 1 | Graph gives macro topology, machine gives exact executable rules, tests prove scenarios and non-graph decisions. |

Critical misconceptions override the numeric score:

- claiming the machine executes repository work itself,
- claiming an effect is durable completion state,
- claiming Checkout owns a valid default product id,
- claiming stale payment results are accepted or treated as fatal invalid
  transitions.

## Provisional Gates

Assumption: until real timing data exists, a successful first-use session means:

- at least 8/10 points,
- no critical misconception,
- timed answers completed within 20 minutes,
- self-rated overview and safety predictability both at least 4/5,
- no facilitator Afsm explanation during the timed part.

These thresholds are experiment gates, not validated customer benchmarks. Keep
raw scores and timing even when the participant fails; do not tune the rubric
after seeing a result.

## Evidence Record

For each session, add a new immutable raw evidence directory containing:

- date and anonymous participant id,
- experience profile,
- repository commit,
- start/end time and whether AI was used,
- verbatim written answers,
- score sheet and critical misconceptions,
- confidence ratings and debrief notes,
- facilitator interventions,
- accepted product finding or reason no change is justified.

Only after the raw record exists should canonical Wiki conclusions, product
decisions, or API experiments change.

## Interpretation

- One pass proves that the protocol is viable, not that Android teams broadly
  prefer Afsm.
- Repeated hesitation on the same term or artifact boundary is redesign
  evidence even if participants eventually pass.
- A failure should first be classified as product/API confusion, sample
  confusion, task ambiguity, or participant-environment issue.
- Repository and AI reviews remain supporting evidence and never substitute for
  this human record.

## Next Evidence Layer

After the comprehension session, run a production-like pilot on one isolated
complex feature module. Record the pre-Afsm mutation sites and flow tests,
implementation effort, review comprehension, defects or missed transitions,
rollback cost, and whether the team would keep the design.

## Preparation Dry Run

The facilitator setup passed on 2026-07-10 at `bdaf6a9`: graph generation
succeeded, all three files were present, ten timed questions were detected, and
every rubric area had evidence in the constrained artifacts. No human answers
or scores were produced. See
`raw/verification/2026-07-10-checkout-first-use-protocol-dry-run/README.md`.
