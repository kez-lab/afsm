---
title: Afsm 10-Agent Usability POC
updated: 2026-05-11
---

# Afsm 10-Agent Usability POC

Ten sub-agents reviewed Afsm as Android developers trying to adopt the library
for real feature work. The review covered junior onboarding, senior migration,
team standardization, dynamic initial state, graph generation, runtime
concurrency, TDD, Kotlin API ergonomics, commerce app usage, and OSS adoption
from README/public docs only.

## Verdict

Afsm should continue in its current direction, but it should not be positioned
as a general ViewModel replacement.

The correct product shape is:

> Afsm is an Android-oriented executable statechart toolkit for complex
> transaction and flow screens.

Use it for signup/identity verification, product registration/review/publish,
checkout/payment retry, refund/cancel, upload, and other flows where valid
phases and invalid transitions matter.

Avoid it for catalog lists, detail display, likes, review lists, and simple
loading/content/error screens where ordinary `ViewModel + StateFlow` is clearer.

## Common Findings

- `afsmMachine { state { on<Event> { transitionTo(...) } } }` is the right
  primary authoring model for complex graphable flows.
- `ViewModel.afsmHost(machine = ...)` is a strong Android integration point.
- `Phase + Context` is useful, but onboarding must say plainly:
  `State = Phase + Context snapshot`.
- `Command` as host-executed work is conceptually right, but stale command
  results and cancellation need official patterns.
- First-time users see too many concepts if README starts with graph/KSP,
  `AfsmGraphReducer`, and `AfsmNoEffect`.
- Generated MMD is valuable, but current diagrams need initial nodes, better
  filtering of internal self-loops, and better command/effect/guard labeling.
- TDD is a strength because machines can be tested as plain Kotlin reducers,
  but the project needs a testing guide/codelab.

## P0 Before Public Release

1. Rework README onboarding to:
   install -> minimal machine -> ViewModel -> tests -> optional MMD graph.
2. Add or design `afsmHost(machine = ..., initialState = ...)` for dynamic
   initial state from nav args or `SavedStateHandle`.
3. Define stale command result and cancellation guidance, preferably with a
   request/correlation-id sample.
4. Decide command queue capacity/backpressure policy or document the current
   command queue risk explicitly.
5. Provide a Compose lifecycle-aware effect collection helper or official
   copy-paste pattern.
6. Make external-project MMD generation clear, either through a Gradle plugin,
   a documented task template, or a better public API.

## P1 Improvements

- Reconsider whether `AfsmGraphReducer` should remain user-facing. Reviewers
  repeatedly found the name accurate but awkward. A possible direction is:
  `AfsmReducer<S,E,C,F>` for low-level reducers, `AfsmMachine<S,E,C,F>` for
  graphable state machines, and `AfsmPhaseMachine<P,X,E,C,F>` for DSL-built
  `AfsmState<Phase, Context>` machines.
- Add `AfsmMmdOptions`, including a default flow view that hides internal
  text-input self-loops.
- Add initial-state rendering to MMD, for example `[*] --> EditingDraft`.
- Add entry/exit command metadata to topology/MMD.
- Add graph diagnostics for duplicate `from/event/to` branches without labels.
- Document payload phase vs context guidance: required data for a phase may
  belong in a payload phase; durable screen data belongs in context.
- Add a TDD guide covering valid transitions, invalid transitions, command
  emission, effect emission, and stale command result regression tests.

## P2 Improvements

- Consider `AfsmConfig.debug()` and `AfsmConfig.production(logger)` presets.
- Consider an `afsm-test` module only after real test helper repetition is
  proven.
- Keep `ignore(...)` strict and explicit, but consider small helpers for common
  expected no-op cases if samples grow noisy.
- Before stable release, review data-class public API models because
  `copy/componentN` become part of the ABI.

## Final Structure Direction

Do not restart the design. The current executable DSL is the best discovered
direction so far.

The next structure should reduce first-contact complexity rather than add a
new abstraction layer:

- `AfsmReducer`: low-level custom-state escape hatch.
- Graphable public machine boundary: rename/spike away from
  `AfsmGraphReducer` if it improves user language.
- `AfsmState<Phase, Context>`: standard Android-facing state snapshot for
  graphable phase/context flows.
- `afsmMachine { ... }`: primary way to author complex statecharts.
- `afsmHost(...)`: ViewModel lifecycle adapter with static and dynamic initial
  state overloads.
- MMD graph generation: optional and valuable, but must be documented after
  the minimal machine path.

## Follow-Up Resolution

Implemented after the review:

- `AfsmGraphReducer` was removed from the public API and replaced by
  `AfsmMachine<State, Event, Command, Effect>`.
- The DSL-built phase/context machine is now named
  `AfsmPhaseMachine<Phase, Context, Event, Command, Effect>`.
- `afsm-compose` now provides `CollectAfsmEffects(...)`.
- `afsmHost(machine = ..., initialState = ...)` now supports dynamic initial
  state while preserving graph metadata.
- `AfsmConfig.commandQueueCapacity` bounds command backpressure.
- Checkout now documents and tests request-id-based stale command result
  handling.
- MMD generation now has initial nodes, richer labels, entry/exit metadata, and
  `AfsmMmdOptions.Flow` / `Full`.
