---
title: Library Delivery Plan
updated: 2026-05-01
---

# Library Delivery Plan

## Stage 0: Problem Definition

Deliverables:

- Product strategy page.
- Clear target users and non-goals.
- Official Android guidance constraints.
- First sample flow selected: [[reference-flow-signup-identity-retry|Signup Identity Retry]].

Exit criteria:

- The team can explain why this is a library, not just an app-local convention.

## Stage 1: Reference Implementation

Build one full feature without creating a broad framework.

Deliverables:

- sample feature contract: `State`, `Event`, `Command`, optional `Effect`
- pure `StateMachine`
- ViewModel integration
- Compose route/screen integration
- transition tests
- ViewModel command tests

Exit criteria:

- Repetition and pain points are visible enough to justify extracting runtime helpers.

## Stage 2: Runtime Core

Extract the smallest reusable core.

Candidate modules:

- `fsm-core`: pure Kotlin transition types and policies.
- `fsm-viewmodel`: AndroidX ViewModel integration.
- `fsm-test`: coroutine and transition testing helpers.
- `fsm-compose`: optional Compose helpers, if they are truly useful.

Exit criteria:

- The reference sample uses the extracted modules without losing readability.

## Stage 3: Developer Experience

Make the library pleasant to use.

Deliverables:

- debug transition logger,
- readable transition traces,
- invalid transition diagnostics,
- recommended file layout,
- sample app,
- API docs,
- migration guide from ad hoc MVVM/UDF.

Exit criteria:

- A new developer can implement a second sample flow by following docs, not by reading the source of the first sample.

## Stage 4: Hardening

Validate edge cases.

Topics:

- command cancellation,
- parallel command policy,
- duplicate event handling,
- process restoration,
- lifecycle collection,
- effect delivery policy,
- testing hot `StateFlow`,
- binary compatibility policy.

Exit criteria:

- Public API changes are intentional and documented.

## Stage 5: Release Readiness

Prepare the library as a product.

Deliverables:

- semantic versioning policy,
- changelog,
- artifact publishing plan,
- README,
- docs site or markdown docs,
- sample app screenshots or walkthrough,
- issue templates,
- contribution guidelines.

Exit criteria:

- The library can be evaluated by an external Android engineer without private context.
