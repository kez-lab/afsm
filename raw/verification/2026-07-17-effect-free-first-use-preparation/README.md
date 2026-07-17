# Effect-Free Checkout First-Use Preparation Evidence

Date: 2026-07-17

Source commit: `a28bc6e9647f6c9c6e70d18aa791b61785077d90`

## Classification

This is facilitator-preparation and input-integrity evidence. It is not a human
first-use result. No participant opened the bundle, no answers or timings were
collected, and no usability gate was scored.

## Prepared Input

The fixed input lives at `afsm-first-use-review-input-effect-free/`:

- Stage 1 contains the participant task, Checkout machine, generated Mermaid
  graph, and transition tests.
- Stage 2 contains the Android-boundary task, current README, Checkout flow
  types, ViewModel, restoration policy, and Compose route/screen.
- `MANIFEST.sha256` fixes the exact content.
- `facilitator-session-record.md` records participant profile, both timers,
  verbatim answers, ratings, interventions, scoring, and evidence
  classification.

The pre-redesign `afsm-blind-review-input/` remains unchanged as historical AI
review evidence and is not a current participant input.

## Verification

The following checks passed:

```bash
./gradlew :sample-shop:generateAfsmMmd --no-daemon
cd afsm-first-use-review-input-effect-free
shasum -a 256 -c MANIFEST.sha256
```

- graph generation: pass (`52` actionable tasks, `1` executed),
- manifest: all twelve listed files pass,
- copied task/source/README files: byte-identical to source commit worktree,
- generated Stage 1 graph: byte-identical to the generated Checkout graph,
- Stage 1 input files: `4`,
- Stage 1 timed questions: `11`,
- Stage 2 input files: `6`,
- Stage 2 timed questions: `8`,
- Stage 2 ratings: `5`,
- Stage 1 removed-Effect terms/API: zero,
- Stage 2 `CheckoutScreen` feature Event construction: zero,
- Stage 2 ViewModel exposes `pay()` and `retry()`,
- Stage 2 README contains the machine/graph/tests reading-contract section.

The Android SDK XML version warning appeared during graph generation but did
not fail the build.

## Isolation Boundary

The repository-local folder makes the input easy to inspect but does not prove
that a tool or participant could not discover parent repository content. A
controlled session must release Stage 1 and Stage 2 separately, record the
actual delivery method, and avoid calling a repository-local AI session
upload-isolated or blind without access evidence.

## Result

The Effect-free review is ready to run. The original product findings remain
unresolved until a no-AI Android developer completes both stages without Afsm
coaching. A production-like pilot is still separately required.
