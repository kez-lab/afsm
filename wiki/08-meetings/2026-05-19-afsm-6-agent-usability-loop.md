---
title: Afsm 6-Agent Usability Loop
date: 2026-05-19
---

# Afsm 6-Agent Usability Loop

## Prompt

Run six Android-developer usability reviewers, implement based on their feedback,
then run six reviewers again.

## First Review Findings

- README first contact felt like maintainer verification, not first-use
  onboarding.
- Minimal Draft exposed graph metadata and heavy generics too early.
- `state(Phase.Saved) { }` looked like boilerplate for terminal states.
- Auth leaked `AuthState` into `AuthScreen`; Checkout had a clearer render-state
  boundary.
- ProductEditor needed a concrete explanation of transition-block versus target
  `onEnter` execution order.
- Graph generation still felt less automatic than the product goal because app
  modules need an export test and Gradle task.
- Internal beta adoption lacked a pilot contract.

## Implemented Changes

- Added no-block `state(phase)` and `state<PayloadPhase>()` DSL convenience for
  terminal or marker phases.
- Reworked README toward a first-use path: install, minimal machine, ViewModel,
  test, then graph generation.
- Renamed the README draft result event to `DraftSaveCompleted`.
- Moved graph metadata labels out of the minimal example.
- Added a dedicated `docs/graph-generation.md` copy-paste setup.
- Changed Auth UI to consume `AuthRenderState`, and made authenticated render
  state explicit.
- Added `CheckoutPrimaryAction` so UI actions are explicit render-state output.
- Documented ProductEditor transition execution order and payload phase lambda
  roles.
- Added an internal beta adoption contract and pilot checklist.

## Second Review Findings

Accepted improvements:

- First-use path is meaningfully simpler.
- `state(phase)` is worth the public API addition because it removes empty
  terminal-state blocks without adding a new concept.
- Auth/Checkout render-state direction is clearer for Android sample copying.
- ProductEditor order explanation is enough, with a small clarification needed
  for payload phase lambda versus transition block.
- Internal beta contract is sufficient for controlled pilots.

Follow-up fixes applied:

- Checkout payment-in-progress now keeps a disabled primary button so
  `Processing...` remains visible.
- `docs/examples.md` now uses `DraftSaveCompleted` consistently.
- Public API docs clarify exact singleton phase declarations versus payload
  phase scopes.
- `docs/sample-shop-afsm-guide.md` no longer implies KSP writes `.mmd` files by
  itself.

## Remaining Priority

The largest adoption blocker remains first-class graph generation ergonomics.
The current pre-release workflow is documented, but a dedicated Gradle plugin is
still the right direction before broad external adoption.
