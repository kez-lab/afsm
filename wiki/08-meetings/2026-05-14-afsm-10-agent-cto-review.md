---
title: Afsm 10-Agent CTO Review
updated: 2026-05-14
---

# Afsm 10-Agent CTO Review

Ten Android developer reviewers re-evaluated Afsm from adoption, architecture,
Kotlin API, testing, sample app, OSS, runtime, graph tooling, and junior
onboarding perspectives. A CTO reviewer then synthesized the feedback.

## Verdict

Afsm is approved for internal beta and focused evaluation on complex Android
flows. It is not ready for broad OSS or stable publication.

The core direction remains correct:

- plain Kotlin FSMs,
- Android `ViewModel` as lifecycle adapter,
- optional Compose effect helper,
- graphable executable DSL for complex flows.

The release blocker is not the product idea. The blockers are API stability,
runtime pressure behavior, restoration/effect policy, graph tooling hardening,
and OSS release identity.

## Accepted Feedback

- Position Afsm as a complex flow toolkit, not a general ViewModel replacement.
- Keep `AfsmPhaseMachine` out of onboarding; feature code should mostly expose
  `AfsmMachine<State, Event, Command, Effect>`.
- Treat `AfsmReducer` as a custom non-graphable escape hatch.
- Add modeling rules for phase/context, DSL/reducer, command/effect, and
  `stay`/`ignore`/`invalid`.
- Harden public API/ABI before any publication.
- Repair Checkout because completed payment must be durable state, not only
  effect delivery.
- Harden graph output paths before investing in a Gradle plugin.

## Rejected or Deferred

- Do not pursue hierarchical machines, invoked services, timers, or an
  `afsm-test` module before API/runtime hardening.
- Do not make effects durable by default. Anything that must survive lifecycle
  gaps should be state plus acknowledgement.
- Defer a dedicated graph Gradle plugin until path safety and KSP validation are
  stronger.
- Keep `Context` for now as a statechart term, but continue documenting that it
  is not Android `Context`.

## Execution Order

1. Public API/ABI hardening.
2. Runtime pressure and lifecycle tests.
3. Restoration/effect/command policy guide.
4. Sample repair: graphable Checkout, completion guard, ViewModel command-path
   tests, ProductEditor render mapper/walkthrough.
5. Graph tooling security and KSP compile tests.
6. OSS packaging: license, coordinates, SCM, signing, compatibility matrix.
7. Beginner onboarding and codelab polish.

## Follow-Up Implemented Immediately

- Removed internal DSL helper leaks from the public API dump.
- Added `AfsmDslMarker`.
- Made `AfsmTransition` factory-based so ignored/invalid transitions cannot
  carry commands/effects through the public API.
- Split `ViewModel.afsmHost(machine + initialState)` from the lower-level
  `reducer + initialState` escape hatch.
- Guarded completed Checkout from duplicate pay/retry events and rendered the
  completed state.
- Hardened `.mmd` file output paths against traversal/absolute paths.
- Added `docs/modeling-rules.md`.

## Remaining P0/P1 Work

- Add runtime saturation tests and decide command-result overflow behavior.
- Add restoration/effect policy documentation and examples.
- Convert Checkout into a graphable phase/context sample or clearly keep it as
  an escape-hatch reducer in onboarding.
- Add ViewModel command-path tests for Auth, ProductEditor, and Checkout.
- Add KSP compile-testing for invalid graph annotations.
- Add OSS release identity: license, final coordinates, SCM, signing, and
  compatibility matrix.
