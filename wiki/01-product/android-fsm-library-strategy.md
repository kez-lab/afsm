---
title: Android FSM Library Strategy
updated: 2026-05-11
---

# Android FSM Library Strategy

## Product Goal

Build `Afsm`, an Android-focused FSM library that helps teams make complex ViewModel-driven UI flows explicit, testable, and lifecycle-aware without fighting official Android architecture guidance.

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
-> new state + commands/effects
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
   - State, Event, Command, and optional Effect are first-class types.
   - Invalid transitions are intentionally handled.
   - Transition logs are easy to inspect in debug builds.

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
- `AfsmMachine<P, X, E, C, F>` for graphable phase/context machines
- `AfsmTransition<S, C, F>`
- invalid transition policy
- command handling abstraction
- ViewModel runner/composition helper
- debug transition logger
- coroutine test helpers
- one or two reference sample flows
- documentation explaining when not to use the library

## Success Criteria

- A complex sample flow can be understood from its transition table/tests without jumping across UI/ViewModel files.
- FSM transition tests are Android-free.
- ViewModel integration is minimal and idiomatic.
- The library works with Compose but is not Compose-only.
- The public API is small enough to explain on one documentation page.
