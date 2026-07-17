---
title: Historical Afsm Goal Completion Audit 2026-07-11
type: maintenance
created: 2026-07-11
updated: 2026-07-17
status: historical-pre-effect-removal-audit
audited_commit: dd54490
---

# Afsm Goal Completion Audit 2026-07-11

> Historical pre-Effect-removal proof boundary. Use
> [[goal-completion-audit-2026-07-17|Afsm Goal Completion Audit 2026-07-17]]
> for the current requirement audit. The dated command/effect evidence below is
> preserved rather than rewritten.

## Verdict

Afsm has strong repository, external-consumer, and current emulator evidence,
but the long-term product Goal is not complete.

The missing proof is not another sample feature or green build. No real Android
developer has completed the no-coaching first-use task, and no feature outside
Afsm's repository fixtures has completed the production-like baseline,
implementation, reviewer comparison, safety matrix, and rollback drill.

Raw verification:
[Goal Completion Audit Evidence](../../raw/verification/2026-07-11-goal-completion-audit/README.md)

## Outcome Audit

| Goal outcome | Current evidence | Status | Evidence still required |
|---|---|---|---|
| 1. Readable complex flows | Checkout machine/graph/tests constrained review reconstructs main, failure, retry, stale, completion, and restoration behavior | Repository-proved, human-unproved | No-coaching Android developer answers, timing, misconceptions, and file hops |
| 2. Useful authoring | Draft onboarding compiles in the external consumer; Auth/Checkout/ProductEditor form an example ladder; simple screens remain anti-examples | Repository-proved, first-author unproved | A new developer implementing or modifying a representative flow and recording friction |
| 3. Safer behavior | Runtime and sample tests cover every named local safety concern; ProductEditor device cancel path passes | Local contract proved, remote boundary unproved | Real transport/SDK behavior and pilot-specific safety matrix |
| 4. Natural Android integration | ViewModel, StateFlow, viewModelScope, SavedStateHandle, Compose, repositories, coroutine tests, external consumer, and emulator app all pass | Repository/device-proved, real-app unproved | Integration cost and lifecycle behavior in a non-fixture application |
| 5. Coherent execution/test/graph | One executable machine drives runtime/test/topology; current full gate, KSP graph generation, API checks, Maven Local, and clean consumer pass | Proved for repository scope | Same consistency in the pilot application |
| 6. Real usability evidence | AI reviews, protocol dry run, device journey, and pilot protocol exist | Missing | Actual human first-use result and at least one valid production-like pilot |
| 7. Evidence-based API freeze | API dumps and checks exist as drift detection, but the surface remains explicitly provisional | Intentionally not eligible | Human/pilot evidence, resulting redesign loop, then explicit freeze/migration/release policy |
| 8. Durable knowledge | Raw evidence, canonical Wiki, current state, open questions, decisions, implementation log, QA, docs, and API dumps are synchronized | Proved for current evidence | Future human/pilot raw records and conclusions |

## Safety Proof Map

| Required concern | Authoritative current proof | Boundary |
|---|---|---|
| Invalid transition | `AfsmHostTest` Record/Throw tests; Auth invalid command-result test | Local runtime and fixtures |
| Expected duplicate event | Checkout duplicate pay/retry ignore test | Checkout semantics |
| Stale async result | Checkout stale success/failure request-id tests | Cooperative event model |
| Retry request identity | Checkout payment request-id transition tests | Feature-owned identity |
| Command cancellation | Phase-owned invocation runtime tests; ProductEditor ViewModel and device journey | Local coroutine only |
| Process recreation | Checkout completed/pending `SavedStateHandle` tests | Feature-owned keys; no real backend resolution |
| Durable completion | Checkout completed state plus restoration test | Checkout fixture |
| Effect delivery loss | Runtime late-collector no-replay test; Checkout restored completion without replay | Best-effort effects by design |
| Command failure | Runtime Record/Throw tests; external consumer command-failure fixture | Application mapping still feature-owned |
| Queue pressure | Command queue and result-event overflow tests | Bounded host queues; invocation capacity not generalized |
| Lifecycle-end result | Host-close result drop and invocation close-cancel tests | Afsm-owned callbacks |
| Diagnostic privacy | Credential-like default types-only tests and external consumer assertion | Explicit `IncludeValues` remains application risk |

## Current Verification

At audited commit `dd54490`:

```bash
./scripts/verify-release-local.sh --no-daemon
```

Passed the graph plugin tests, module/sample tests, graph generation, API
checks, Maven Local publication, and clean external consumer. The source code
had not changed since the previously verified uploader implementation; the
gate was rerun rather than inferred from that history.

The current ProductEditor Android journey also passes and is recorded under
`raw/verification/2026-07-11-product-editor-cancel-device-journey/`.

## Completion Blockers

### Human first use

Required input:

- an Android developer with no Afsm context,
- a facilitator who does not coach during the timed task,
- permission to retain anonymized answers, timing, misconceptions, and
  debrief notes.

The participant task and rubric are ready. The existing dry run is not a human
result.

### Production-like pilot

Required input:

- a complex feature outside Afsm fixtures,
- repository/path and immutable baseline commit,
- feature owner, reviewer, and rollback owner,
- pre-registered success/stop thresholds and evidence permission.

The pilot protocol is ready. No target or result exists.

## Next Decision Rule

Do not redesign invocation keys, capacities, hierarchy, graph module boundaries,
restoration helpers, or diagnostic attributes solely to keep the loop busy.
Those questions now require repeated authoring or pilot evidence.

After valid human/pilot evidence arrives:

1. classify the findings without changing thresholds,
2. select the largest repeated usability or safety problem,
3. run the normal spec -> TDD -> implementation -> full verification loop,
4. repeat real comparison if the finding changes the measured surface,
5. begin API freeze only when the evidence supports preserving the design.
