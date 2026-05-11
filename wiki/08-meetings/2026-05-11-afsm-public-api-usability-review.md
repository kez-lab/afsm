---
title: Afsm Public API Usability Review
updated: 2026-05-11
---

# Afsm Public API Usability Review

This meeting reviewed whether the current Afsm public API is becoming too
complex for Android developers.

## Verdict

Afsm's direction is sound for complex Android flow screens, but the current
public surface feels heavier than necessary because docs and samples expose too
many concepts at once.

The API should be positioned as an Android executable extended statechart DSL:

- `Phase`: finite state diagram node.
- `Context`: extended state data carried across phases, not `android.content.Context`.
- `State`: Android-facing snapshot, usually `AfsmState<Phase, Context>`.
- `Event`: user input or command result.
- `Command`: host-executed work emitted by the machine.
- `Effect`: optional UI one-shot output.

## Agreed Strengths

- `AfsmState<Phase, Context>` maps well to extended finite state machine and
  statechart references.
- `afsmMachine { ... }` makes ProductEditor-like complex flows easier to read
  than scattered ViewModel `copy(...)` updates.
- `Command` is the right separation for host-executed work; it keeps the machine
  deterministic and Android-free.
- `ViewModel.afsmHost(...)` is the correct Android lifecycle boundary.

## Usability Risks

- README and API docs expose `AfsmReducer`, `AfsmGraphReducer`, `AfsmMachine`,
  topology, KSP, `Phase`, `Context`, `Command`, and `Effect` too early.
- `AfsmGraphReducer` is useful, but should mostly appear as a feature-boundary
  alias pattern instead of a first concept.
- `initialState` on graphable machine boundaries is convenient for static
  screens, but dynamic initial state from navigation args or `SavedStateHandle`
  remains important.
- Generated `.mmd` currently reflects phase/event topology. Guard, command, and
  effect metadata are not automatically inferred from runtime code.
- Internal `stay(...)` edges can make generated graphs noisy for text-input
  updates and validation fallbacks.

## Runtime Risks

- Current command execution awaits command handlers inside the event processing
  loop, so long-running commands can delay later UI events.
- The event queue is currently unbounded.
- Invalid transitions default to record-only behavior with no logger, which can
  hide programmer mistakes in development.

## Hardening Plan

1. Add a simpler `ViewModel.afsmHost(machine, ...)` overload so graphable
   machines can be hosted without repeating `initialState` and `reducer`.
2. Revisit runtime command execution so commands do not unnecessarily block
   event processing.
3. Make runtime defaults safer for public use, especially invalid transition
   handling and event buffering.
4. Rewrite README around one recommended happy path: `afsmMachine { ... }` for
   complex graphable flows.
5. Move low-level reducer, graph metadata, and pre-release migration history to
   reference docs instead of first-page onboarding.
6. Document graph generation limits clearly: phase/event topology is automatic;
   command/effect/guard details require explicit metadata unless a later API
   infers them.

