---
title: Open Questions
updated: 2026-05-01
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

- Is `AfsmTransition<S, C, F>` too verbose for users who do not use effects?
- Should `AfsmHost.dispatch` be synchronous, suspending, or fire-and-forget?
- Should the MVP include `afsm-runtime`, or only `afsm-core` plus a sample ViewModel pattern?
- Should invalid transition `Throw` policy be core behavior or test/debug helper behavior?
