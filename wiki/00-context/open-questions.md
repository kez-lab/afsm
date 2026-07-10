---
title: Open Questions
updated: 2026-07-10
---

# Open Questions

This page contains only decisions that are still open after comparison with the
current code, tests, API dumps, public documentation, release checklist, and
local verification. Historical alternatives and completed decisions belong in
the engineering pages and `wiki/06-project/decision-log.md`.

## Public Release and Compatibility

- What exact remote publication identity should the first public release use:
  final group/artifact ids, license, repository target, SCM metadata,
  developer/organization metadata, signing credentials, and release owner?
- What source and binary compatibility promise should the first external beta
  make beyond the current pre-1.0 rule that breaking changes require API dumps,
  docs, examples, changelog, and migration notes in the same change?

## API and Module Boundaries

- Before external publication, should the executable DSL, `@AfsmGraph`, topology
  types, and `AfsmGraphSource` remain in `afsm-core`, or should graph concerns
  move to a smaller annotation/API module? This includes deciding whether every
  `AfsmMachine` should continue to be graphable by extending
  `AfsmGraphSource`.
- Should the current regular `AfsmConfig` constructor remain the public
  configuration surface at API freeze, or should a more evolution-friendly
  shape be adopted before external consumers depend on it?

## Advanced Runtime Scope

- Should the first public release explicitly remain flat-state only, or must it
  add hierarchical/parallel semantics? `AfsmTopologyState.parentId` exists as
  metadata, but the executable DSL and Mermaid renderer do not implement a
  nested runtime model.
- Should invoked-service semantics for timers, polling, uploads, or sockets be
  part of the first public release? The current v1 policy represents this work
  as commands and requires explicit cancellation commands plus request ids.

## Android Restoration

- Should v1 ship a reusable restoration helper beyond
  `afsmHost(machine, initialState)`, minimal `SavedStateHandle` conversion, and
  the documented stable-state restoration policy, or should restoration remain
  feature-owned guidance?

## Graph Tooling

- Should graph generation remain one selected Android unit-test variant per
  module, or support multi-variant and multi-module aggregation before broader
  external adoption?

## Resolved Current Policies

- Afsm targets controlled internal beta pilots on complex flow screens; it is
  not a general ViewModel replacement.
- Current distribution is Maven Local snapshot or direct project modules.
  Remote publication identity remains open, but the present delivery path does
  not.
- The current public authoring direction is the phase/data executable DSL in
  `afsm-core`; older v1/v2 drafts and phased-helper proposals are historical.
- Graphable features use a non-private stable top-level `val` machine. KSP
  references that property directly; delegated object/factory wrappers are no
  longer required, while eligible classes/objects remain supported.
- `AfsmMachine` owns transition rules/topology without promising a default
  state. `AfsmDefaultMachine` adds a genuine static default; dynamic features
  declare `initialPhase` and cannot use `afsmHost(machine)` without an explicit
  runtime state.
- `AfsmTransition` carries effects. Effects are best-effort with no replay by
  default, while required progress stays in state or state plus acknowledgement.
- Retry and stale-result policy is feature-owned through explicit phases,
  request ids, result events, and narrowly used `ignore(...)`; the library does
  not own a generic retry policy.
- Restoration reconstructs minimal stable state, does not restore in-flight
  work, and does not run `onEnter` during initial state construction.
- Invalid hosted transitions throw by default; resilient hosts may opt into
  `AfsmInvalidTransitionPolicy.Record`.
- Named no-transition condition cases appear in Flow graphs. `ignore(...)` and
  `invalid(...)` are runtime decisions without graph edges. State ids provide
  default labels, while condition/command/effect labels are explicit where
  useful.
- The example ladder is Draft, Auth, Checkout, ProductEditor, followed by
  ordinary non-Afsm data screens as anti-examples.
