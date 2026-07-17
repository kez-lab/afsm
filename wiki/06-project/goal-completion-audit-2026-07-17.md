---
title: Afsm Goal Completion Audit 2026-07-17
type: maintenance
created: 2026-07-17
updated: 2026-07-17
status: not-complete-human-and-pilot-evidence-required
audited_commit: 8d268cc596268e0d6a2a8b4243e88c662e90d284
---

# Afsm Goal Completion Audit 2026-07-17

## Verdict

Afsm's current Effect-free implementation is coherent and strongly verified in
the repository, Maven Local consumer, generated graphs, API checks, and durable
knowledge layers. The long-term product goal is not complete.

The missing proof is real use: no no-AI Android developer has completed the
current two-stage first-use session, no first-time author has implemented or
modified a representative flow, and no feature outside repository fixtures has
completed the production-like baseline/comparison/safety/rollback protocol.

Raw verification:
[Goal Completion Audit Evidence](../../raw/verification/2026-07-17-goal-completion-audit/README.md).

## Outcome Audit

| Goal outcome | Current evidence | Status | Evidence still required |
|---|---|---|---|
| 1. Readable complex flows | Checkout machine/graph/tests, generated topology, old constrained AI review, and one informal human finding drove the Effect-free redesign | `partial` | Current no-coaching human answers, timing, misconceptions, artifact use, and comparison after redesign |
| 2. Useful authoring | Draft onboarding compiles in a clean consumer; examples cover increasing complexity and explicit anti-examples | `partial` | A first-time Android author implementing or changing a representative flow while recording setup, boilerplate, ordering, diagnostics, and graph friction |
| 3. Safer behavior | Named runtime/core/sample tests cover invalid, duplicate, stale, retry identity, local cancellation, restoration, completion, command failure, pressure, lifecycle end, and diagnostic privacy | `partial` | Real transport/SDK/backend boundaries and pilot-specific safety matrix; local cancellation is not remote cancellation |
| 4. Natural Android integration | ViewModel, StateFlow, viewModelScope, SavedStateHandle, Compose, repositories, coroutine tests, Maven Local consumer, and dated emulator journey exist | `partial` | Integration and lifecycle cost in a non-fixture Android feature |
| 5. Coherent execution/test/graph | Current release gate, API checks, generated graphs, KSP, publication, clean consumer, and Wiki lint pass | `proved-repository-scope` | The same consistency in a production-like pilot |
| 6. Real usability evidence | Informal human findings exist; a commit-pinned two-stage protocol and manifest are ready | `missing-required-proof` | Completed no-AI human session plus at least one valid production-like pilot |
| 7. Evidence-based API freeze | API dumps detect drift, but the surface is explicitly provisional and has already changed from evidence | `not-eligible` | Human/pilot evidence, resulting redesign loop, then an explicit freeze/migration/release decision |
| 8. Durable product knowledge | Raw evidence, canonical Wiki, current/open state, decisions, implementation logs, QA, public docs, API dumps, and 2026-07-17 Wiki lint agree | `proved-current-repository` | Future human/pilot inputs and conclusions must be added when they exist |

## Safety Proof Map

| Required concern | Current proof | Boundary |
|---|---|---|
| Invalid transition | `AfsmHostTest` Record/Throw; Auth invalid result; DSL decision tests | Local runtime and fixtures |
| Expected duplicate event | Checkout duplicate screen/pay/retry tests | Checkout semantics |
| Stale async result | Checkout stale payment success/failure tests | Request-id event model |
| Retry request identity | Checkout payment failure/retry tests | Feature-owned identity |
| Command cancellation | Phase-owned invocation runtime/core tests; ProductEditor machine/ViewModel/consumer/device evidence | Cooperative local coroutine only |
| Process recreation | Checkout completed/pending `SavedStateHandle` tests | Feature keys; real backend resolution absent |
| Durable completion | Checkout `Completed(orderId)` machine/ViewModel/restoration tests | Checkout fixture |
| Effect delivery loss | Effect API/channel removed; business outcomes are state; direct UI callbacks/state reaction documented and tested | No best-effort stream; acknowledgement-sensitive real UI still feature/pilot proof |
| Command failure | Runtime Record/Throw and external consumer fixture | Domain mapping remains feature-owned |
| Event/command pressure | Command queue and result-event overflow tests | Bounded host queues; active invocation capacity remains open |
| Lifecycle-end result | Host-close result drop and invocation close-cancel tests | Afsm-owned callbacks only |
| Diagnostic privacy | Types-only secret fixtures and explicit `IncludeValues` test | Application opt-in remains its risk |

## Current Verification

At audited commit `8d268cc596268e0d6a2a8b4243e88c662e90d284`:

```bash
./scripts/verify-release-local.sh --no-daemon
```

passed graph plugin tests, module/sample tests, graph generation, all API
checks, Maven Local publication, graph plugin publication, and the clean
external Android consumer.

The Effect-free Wiki lint and two-stage first-use manifest also pass. These are
implementation and preparation evidence, not comprehension or adoption
evidence.

## Completion Blockers

### Controlled Effect-free first use

Release only Stage 1, lock answers, then release Stage 2. Preserve participant
profile, both timers, verbatim answers/ratings, interventions, exact manifest,
delivery isolation, score, and debrief. An AI dry run remains AI evidence.

### Production-like pilot

Select a complex feature outside Draft/Auth/Checkout/ProductEditor, freeze its
real baseline, pre-register roles/thresholds/stop criteria, compare both
implementations, execute the safety matrix, and measure rollback. No target or
owner exists yet.

## Next Decision Rule

Do not redesign advanced runtime/API questions merely to generate activity
while the current measured surfaces await human evidence. After a valid session
or pilot:

1. classify findings without changing pre-registered gates,
2. select the largest repeated usability or safety problem,
3. update canonical intent before tests and implementation,
4. rerun the relevant human/pilot comparison if the measured surface changes,
5. begin API freeze only when the resulting evidence supports preservation.
