---
title: Library Delivery Plan
updated: 2026-07-10
---

# Library Delivery Plan

## Status

Historical staged delivery plan. Most implementation and developer-experience
stages are complete for internal beta. Use
[[../00-context/current-state|Current State]] for the implemented surface,
[[../00-context/open-questions|Open Questions]] for undecided scope, and
`docs/release-readiness.md` for the active release and publication gates.

## Stage 0: Problem Definition

Deliverables:

- Product strategy page.
- Clear target users and non-goals.
- Official Android guidance constraints.
- First sample flow selected: [[reference-flow-signup-identity-retry|Signup Identity Retry]].
- Public API draft: [[afsm-public-api-draft|Afsm Public API Draft]].

Exit criteria:

- The team can explain why this is a library, not just an app-local convention.

## Stage 1: Reference Implementation

Build one full feature without creating a broad framework.

Deliverables:

- sample feature contract: `State`, `Event`, `Command`, optional `Effect`
- pure `AfsmReducer` or graphable `AfsmMachine`
- ViewModel integration
- Compose route/screen integration
- transition tests
- ViewModel command tests

Exit criteria:

- Repetition and pain points are visible enough to justify extracting runtime helpers.

## Stage 2: Runtime Core

Extract the smallest reusable core.

Candidate modules:

- `afsm-core`: pure Kotlin transition types and policies.
- `afsm-runtime`: coroutine-based host and command execution.
- `afsm-viewmodel`: AndroidX ViewModel integration.
- `afsm-test`: coroutine and transition testing helpers.
- `afsm-compose`: optional Compose helpers, if they are truly useful.

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
