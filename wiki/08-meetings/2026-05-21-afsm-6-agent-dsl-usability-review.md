---
title: Afsm 6-Agent DSL Usability Review
updated: 2026-05-21
---

# Afsm 6-Agent DSL Usability Review

Scope: review the case-oriented public DSL after removing DSL-level `stay`,
`otherwise`, and `guardLabel`.

## Agreed Findings

- README and public API docs must teach `case(label, condition = ...)` earlier,
  including no-transition cases and the rule that no matching case is invalid.
- Public docs must not show duplicate command emission from both a case and the
  target phase `onEnter`.
- `transitionTo<PayloadPhase> { ... }` must follow source order. The payload
  phase factory should observe context updates made by `onExit` and accepted
  case actions.
- Named no-transition cases should appear in generated Flow `.mmd` output;
  otherwise docs and generated graphs disagree.
- Checkout and ProductEditor examples should avoid unconditional fallback-style
  validation cases and should declare explicit negative conditions.

## Implemented Follow-Up

- Payload phase factories now run after `onExit` and case actions.
- Flow `.mmd` includes named no-transition condition branches.
- Checkout `missing product` branches now declare `condition = { context.product == null }`.
- Public docs now use phase-only `transitionTo`, explain case ordering, and avoid
  duplicate command emission.

## Remaining Follow-Ups

- Done: add a Gradle option for `generateAfsmMmd` to choose Flow versus Full
  output.
- Reduce manual drift between `commandLabels` metadata and actual `command(...)`
  calls, especially in `onEnter`/`onExit`.
- Decide whether unlabeled conditional branches to the same target should warn
  or require labels instead of being silently deduplicated in topology.
- Done: rename `ProductEditorEvent.DraftSaved` to `DraftSaveCompleted` to avoid
  phase/event naming overlap.
