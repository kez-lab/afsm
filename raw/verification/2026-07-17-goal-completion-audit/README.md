# Afsm Goal Completion Audit Evidence 2026-07-17

Audited commit: `8d268cc596268e0d6a2a8b4243e88c662e90d284`

## Classification

This is a repository proof-boundary audit. It is not a human usability result
or production-like pilot.

## Current Release Gate

The following command passed at the audited commit:

```bash
./scripts/verify-release-local.sh --no-daemon
```

It passed:

- graph Gradle plugin tests,
- core/runtime/test/ViewModel/KSP/sample tests and graph generation (`83`
  actionable tasks in the main gate),
- all five API checks,
- Maven Local publication for the library modules,
- Maven Local graph plugin publication,
- a clean separate Android consumer compile, tests, and graph generation (`20`
  executed tasks).

The build emitted the existing Android SDK XML version and Gradle deprecation
warnings; neither failed a gate.

## Additional Current Checks

- Effect-free Wiki lint: canonical current/public legacy surface zero,
  ambiguous historical page heads zero, unindexed pages zero, broken wiki
  links zero.
- sample Auth/Checkout/ProductEditor screen Event construction: zero.
- Effect-free two-stage first-use manifest: pass.
- Git worktree before this audit record: clean.

## Safety Evidence Rechecked

Current named tests cover:

- invalid transitions: `AfsmHostTest` Record/Throw and Auth invalid-result tests,
- expected duplicate and stale results: Checkout duplicate and request-id tests,
- retry identity: Checkout failure/retry transition tests,
- cancellation: runtime phase-owned invocation plus ProductEditor machine,
  ViewModel, external consumer, and dated device journey,
- restoration and durable completion: Checkout machine/ViewModel saved-state
  tests,
- removed Effect loss: `AfsmEffectFreeApiTest`, durable completion tests, and
  no Effect runtime channel,
- command failures: runtime Record/Throw and clean consumer failure fixture,
- event/command pressure: host command/result-event overflow tests,
- lifecycle-end results: host-close drop and invocation-close cancellation,
- diagnostic privacy: types-only credential-like tests and explicit raw opt-in.

These prove local contracts. They do not prove a real backend/SDK cancellation
contract, production payment resolution, team adoption cost, or reviewer
preference.

## Missing Evidence

No no-AI Android developer has completed the commit-pinned two-stage
Effect-free first-use session. No feature outside the repository fixtures has a
pre-registered baseline, Afsm implementation, reviewer comparison, safety
matrix, verification, and rollback result under `raw/pilots/`.

Therefore the long-term goal is not complete.
