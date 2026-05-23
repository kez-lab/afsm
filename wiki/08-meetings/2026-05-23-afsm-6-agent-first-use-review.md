---
title: Afsm 6-Agent First-Use Review
updated: 2026-05-23
---

# Afsm 6-Agent First-Use Review

## Prompt

Review the whole project from the perspective of an Android developer seeing
Afsm for the first time. Identify where understanding cost remains, then turn
the review into an improvement plan.

## Review Roles

- Beginner Android app developer: can I build one screen from README alone?
- Senior Android architect: does the API map cleanly to ViewModel/UDF?
- Kotlin API reviewer: do names and type signatures feel idiomatic?
- Sample-app POC reviewer: can I follow Auth, Checkout, and ProductEditor?
- Documentation reviewer: does the reading order teach the right model?
- CTO/adoption reviewer: would a team pilot this without too much ceremony?

## Shared Findings

- The runtime model is mostly understandable once a developer reaches the
  samples, but the first page exposed too much install, graph, and API detail
  before the mental model.
- `state(...)` in the DSL overloaded the word "state". Android developers read
  it as full UI state, while Afsm meant a finite graph node.
- `Context` overloaded Android `Context`. The concept is durable immutable
  screen data, so `Data` is easier to explain.
- `stay` / `Stayed` made no-phase-change handling sound like another state
  transition concept. In the DSL, handling an event without `transitionTo(...)`
  should be enough.
- `AfsmPhaseMachine` exposed a library-internal distinction that users did not
  need on day one. Feature code should see `AfsmMachine<State, Event, Command,
  Effect>`.
- `ignore(...)` should be taught as optional observability for expected no-ops,
  not as a requirement to enumerate every state/event combination.

## Accepted Changes

- Rename DSL `state(...)` scopes to `phase(...)`.
- Rename standard extended state from `context` to `data`.
- Rename low-level no-phase-change decision from `Stayed` to `Handled`.
- Remove public `AfsmPhaseMachine`.
- Add `docs/getting-started.md` as the Android-first entry point.
- Keep `transitionTo(...)` phase-only and keep `updateData(...)` as a separate
  action.

## Rejected Or Deferred

- Do not reintroduce a custom `stay(...)` beginner API.
- Do not hide data updates inside `transitionTo(...)`.
- Do not add a shared `AfsmStateFactory` until repeated real consumer code
  proves the small feature-local factory is a problem.

## CTO Synthesis

Afsm should be presented as a small Android-first statechart toolkit, not as a
generic reducer framework. The primary story is:

```text
Phase + Data
-> Event
-> updateData / transitionTo / command
-> ViewModel host
-> StateFlow + effects
```

The project remains internal beta. The current change makes the first-use path
clearer, but external adoption still needs a real consumer pilot and release
identity decisions.
