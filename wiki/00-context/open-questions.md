---
title: Open Questions
updated: 2026-05-03
---

# Open Questions

## Architecture

- Should the project provide a small reusable FSM runtime, or start with per-screen state machine classes and extract later?
- Should UI one-shot actions be modeled as `Effect`, as terminal `State`, or through UI state flags depending on the case?
- How strict should invalid transitions be: ignored result, explicit error, or debug-only assertion?
- Should state machines support hierarchical/nested state machines from the start?

## Android Integration

- How should process restoration be handled: reconstruct from `SavedStateHandle`, persist only selected state, or require domain reload?
- Which FSM states are safe to restore directly, and which should be reconstructed from a minimal key plus repository data?
- Should commands be cancellable by default when a new event arrives, or should cancellation be explicit per command?
- Should command execution be serialized, parallelized, or policy-driven?

## Scope

- What should the first public API look like before it becomes too framework-like?
- Which modules should exist at MVP: core only, ViewModel integration, Compose helpers, test helpers?
- What sample flows best prove the library's value to external Android teams?

Resolved:

- First real reference flow: signup + identity verification + retry.

## Product

- Is the initial audience internal app teams, external OSS users, or both?
- Should the first release optimize for minimum API surface or maximum developer convenience?
- What is the distribution target: local module, Maven artifact, GitHub OSS library, or private package?

## Reference Flow

- Should `AfsmTransition` carry effects directly, or should effects be a ViewModel integration concern?
- Should retry policy be configured by the library or modeled by each feature state machine?
- Should saved state restoration be implemented as a reusable helper in v1, or kept as sample guidance?

## Public API

- Should the MVP include `afsm-runtime`, or only `afsm-core` plus a sample ViewModel pattern?
- Should invalid transition `Throw` policy be core behavior or test/debug helper behavior?
- Should `AfsmConfig` be a data class, regular class, or builder-like API for binary/API stability?

Resolved:

- `AfsmTransition<S, C, F>` is acceptable if feature-local typealiases are documented as the standard convention.
- `Ignored` is overloaded; the API should add `AfsmDecision.Stayed` and `Afsm.stay(...)` before implementation.
- Use `Afsm` as public type prefix because the product name is Android State Machine.
- Use `AfsmNoEffect` sealed interface as the no-effect marker candidate.
- Use non-suspending fire-and-queue `AfsmHost.dispatch(event)` with serialized FIFO event processing.
- Use best-effort `Flow<F>` effect delivery with no replay by default.
- `AfsmNoEffect` and `AfsmTransition<S, C, F>` compile cleanly in `afsm-core` when used with feature-local typealiases and both no-effect and effectful flows.
