# Effect-Free Checkout First-Use Input

Facilitator-only routing for the current two-stage Checkout review.

## Snapshot

- Source commit: `a28bc6e9647f6c9c6e70d18aa791b61785077d90`
- Prepared: 2026-07-17
- Stage 1: machine, generated graph, and transition tests only
- Stage 2: README, Checkout flow types, ViewModel, restoration, and Compose
  boundary
- Integrity: `MANIFEST.sha256`

The older `afsm-blind-review-input/` directory is an immutable snapshot of the
pre-redesign Effect API used by the 2026-07-11 AI review. Do not substitute it
for this input.

## Release Order

1. Give the participant only `stage-1/`. Start timing when the first artifact
   opens. Do not expose this facilitator README, Stage 2, the repository wiki,
   or other source files.
2. Lock the eleven answers and Stage 1 ratings.
3. Give the participant `stage-2/` while keeping Stage 1 available. Start a new
   timer and keep the no-coaching rule.
4. Lock the eight answers and five Stage 2 ratings before debriefing.
5. Record the session with `facilitator-session-record.md`, then preserve the
   completed record and verbatim answers under a new immutable
   `raw/verification/YYYY-MM-DD-.../` directory.

For a strong isolation claim, copy each stage outside the Afsm checkout before
release or share it as a separate attachment. Opening these directories from
inside the repository is only folder-scoped input; the parent repository may
remain discoverable. Record the actual delivery method.

## Evidence Boundary

This directory proves only that a current, fixed participant input exists. It
is not a human result, does not prove that the redesign resolved the reported
problems, and does not satisfy the production-like pilot gate.
