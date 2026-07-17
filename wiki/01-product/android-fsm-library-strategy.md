---
title: Android FSM Library Strategy
updated: 2026-07-17
---

# Android FSM Library Strategy

## Product Goal

Afsm's primary goal is to turn implicit business-flow state changes scattered
across complex Android `ViewModel`s into explicit `Phase` and `Event`
transition rules that developers can read, test, graph, and verify from the
same executable machine definition.

Afsm does not try to hide Kotlin `copy()` or abstract state storage. It makes
screen flow a first-class model so a developer can determine directly from the
machine:

- the current `Phase`,
- which `Event` occurred and whether it is valid,
- how `Phase` and durable `Data` change,
- which sequential `Command`s or phase-owned command invocations follow,
- and which host-executed `Command`s follow.

Android `ViewModel` remains the lifecycle and UI integration adapter. Afsm
makes complex flow rules explicit without fighting official Android
architecture guidance or imposing FSM ceremony on simple screens.

Afsm does not own a one-shot UI `Effect` channel. Business outcomes are state;
UI-originated UI actions are direct callbacks; UI behavior following an async
outcome reacts to state, with feature-owned acknowledgement only when needed.
This keeps Afsm focused on business flow instead of expanding into a full MVI
UI framework.

The active outcome-based execution plan is maintained in
[[../06-project/long-term-goal|Afsm Long-Term Goal]].

Afsm has not been publicly released. Every current API, DSL operation,
terminology choice, module boundary, sample, and test fixture is provisional.
Pre-release compatibility must not block a redesign that makes complex Android
flows materially easier to read, author, or model safely. Stabilization begins
only after evidence shows that the selected design is worth preserving.

## Target Users

- Android app teams using Kotlin, ViewModel, StateFlow, and Jetpack Compose.
- Teams that already feel MVVM/UDF improves separation but hurts flow traceability.
- Teams building complex flows such as login, signup, checkout, identity verification, permission gates, onboarding, upload/download, search/filter/paging, and retry-heavy screens.

## Core Promise

The library should make this flow easy to model, inspect, test, and run:

```text
UI event
-> ViewModel
-> AfsmReducer.transition(state, event)
-> new state + commands
-> ViewModel executes commands
-> results feed back as events
-> UI renders state
```

## Non-Goals

- Do not replace Android `ViewModel`.
- Do not become a full MVI framework with opinionated UI rendering.
- Do not force FSM ceremony onto simple loading/content/error screens.
- Do not own navigation directly unless a narrow integration layer is explicitly added.
- Do not store large screen state in `SavedStateHandle`.

## Product Positioning

The library should sit between two extremes:

- Too little structure: ad hoc ViewModel methods and scattered `MutableStateFlow.update` calls.
- Too much framework: heavy MVI/runtime systems that hide Android conventions or impose a full app architecture.

The intended position is a small, typed, Android-aligned FSM toolkit.

## Product Pillars

1. Explicit flow
   - State, Event, and Command are the first-class flow types.
   - Invalid transitions are intentionally handled.
   - The executable definition and generated graph expose transition topology.
   - Runtime failures and invalid transitions are observable through
     types-only diagnostics that do not expose raw domain values by default.
   - Long-running phase-owned work can be cancelled locally without ViewModel
     job registries or cancel commands queued behind the work itself.

2. Android alignment
   - ViewModel remains the screen-level state holder.
   - Compose/View UI remains responsible for UI behavior.
   - Lifecycle-aware state collection and coroutine guidance are respected.
   - Saved state policy is minimal and explicit.

3. Testability
   - Pure FSM transition tests run as plain JVM tests.
   - ViewModel command execution tests use coroutine test utilities.
   - StateFlow testing accounts for conflation and `stateIn` behavior.

4. Gradual adoption
   - Teams can start with one feature screen.
   - Runtime helpers can be adopted incrementally.
   - The library should not require migrating the entire app architecture.

## MVP Definition

The first usable library version should include:

- `AfsmReducer<S, E, C, F>`
- `AfsmMachine<S, E, C>` for graphable machines without an assumed default
- `AfsmDefaultMachine<S, E, C, F>` for graphable machines with a genuine
  reusable default state
- `afsmMachine<P, D, E, C> { ... }` for static phase/data machines, plus its
  `initialPhase` overload for machines whose initial data comes from the host
- `AfsmState<P, D>` as the standard `phase + data` state value
- `AfsmTransition<S, C>`
- invalid transition policy
- command handling abstraction
- ViewModel runner/composition helper
- runtime diagnostics logger
- coroutine test helpers
- one or two reference sample flows
- documentation explaining when not to use the library

## Success Criteria

- A complex sample flow can be understood from its machine, generated graph,
  and transition tests without jumping across UI/ViewModel files.
- Runtime behavior, transition tests, and generated topology are derived from
  the same executable machine definition.
- FSM transition tests are Android-free.
- ViewModel integration is minimal and idiomatic.
- The library works with Compose but is not Compose-only.
- The public API is small enough to explain on one documentation page.
- At least one Android developer with no prior Afsm context meets a pre-recorded
  machine/graph/tests comprehension rubric without facilitator coaching.
- At least one production-like feature pilot records comparative readability,
  authoring friction, safety findings, and rollback feasibility.
