---
title: FSM Runtime Roadmap
updated: 2026-07-10
---

# FSM Runtime Roadmap

## Status

Historical build-order plan. The core/runtime/ViewModel/test/Compose and graph
slices described here have since been implemented or superseded. Use
[[../00-context/current-state|Current State]] for implemented scope and
`docs/release-readiness.md` for the active release gate.

This page defines a conservative build order for the Android-specific FSM foundation.

## Phase 1: Per-Screen Pattern

Start without a heavy framework.

Deliverables:

- `AfsmTransition<S, C, F>`
- `AfsmStateMachine<S, E, C, F>`
- one sample feature using `State`, `Event`, `Command`
- plain JVM tests for transitions
- ViewModel tests for command execution

Goal: prove the architecture on one real flow before extracting a runtime.

## Phase 2: Small Runtime Helpers

Extract only repeated mechanics.

Possible helpers:

- `CommandHandler<C>`
- `FsmViewModel` base class or composition helper
- debug transition logger
- invalid transition policy
- command execution policy
- coroutine test utilities for command execution

Avoid early abstraction around every screen. The runtime should remove real repetition discovered in Phase 1.

## Phase 3: Android Integration Policy

Define Android-specific conventions.

Topics:

- lifecycle-aware state collection,
- `SavedStateHandle` restoration,
- minimal FSM restoration keys,
- command cancellation,
- effect delivery if needed,
- navigation integration,
- Compose route/screen split.
- dispatcher injection and `viewModelScope` testing policy.

## Phase 4: Advanced Flow Support

Add only when a real screen needs it.

Candidates:

- nested or hierarchical state machines,
- parallel state regions,
- typed transition logging,
- visual transition graph generation,
- dev-only invalid transition assertions.

## First Candidate Screen Criteria

Choose the first screen by complexity, not convenience.

Good candidates have:

- at least four meaningful states,
- async command results,
- retry or cancellation,
- invalid events in some states,
- clear business flow value.

Poor candidates:

- static screens,
- simple loading/content/error,
- screens with mostly visual state.
