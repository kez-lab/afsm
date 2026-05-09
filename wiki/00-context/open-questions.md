---
title: Open Questions
updated: 2026-05-09
---

# Open Questions

## Architecture

- Should UI one-shot actions be modeled as `Effect`, as terminal `State`, or through UI state flags depending on the case?
- How strict should invalid transitions be: ignored result, explicit error, or debug-only assertion?
- Should state machines support hierarchical/nested state machines from the start?

## Android Integration

- How should process restoration be handled: reconstruct from `SavedStateHandle`, persist only selected state, or require domain reload?
- Which FSM states are safe to restore directly, and which should be reconstructed from a minimal key plus repository data?
- Should commands be cancellable by default when a new event arrives, or should cancellation be explicit per command?

## Scope

- What should the first public API look like before it becomes too framework-like?
- Which modules should exist at MVP: core only, ViewModel integration, Compose helpers, test helpers?
- What sample flows best prove the library's value to external Android teams?

Resolved:

- First real reference flow: signup + identity verification + retry.
- The first complex app-level validation is a shopping sample with auth, product, review, like, and checkout retry flows.

## Product

- Is the initial audience internal app teams, external OSS users, or both?
- Should the first release optimize for minimum API surface or maximum developer convenience?
- What is the distribution target: local module, Maven artifact, GitHub OSS library, or private package?

## Reference Flow

- Should `AfsmTransition` carry effects directly, or should effects be a ViewModel integration concern?
- Should retry policy be configured by the library or modeled by each feature state machine?
- Should saved state restoration be implemented as a reusable helper in v1, or kept as sample guidance?

## Public API

- Should invalid transition `Throw` policy be core behavior or test/debug helper behavior?
- Should `AfsmConfig` be a data class, regular class, or builder-like API for binary/API stability?
- Should the executable DSL live in `afsm-core`, `afsm-dsl`, or another module before public release?
- How should graph extraction represent invalid/ignored branches declared in the executable DSL?
- Should graph labels default to type names, require explicit human labels, or support both?
- Before public API freeze, should `Command` be renamed to `Action` or `TransitionAction` to better communicate transition outputs?
- If renamed, should `AfsmTransition<S, C, F>` become `AfsmTransition<S, A, F>`, and should `commands` become `actions`?
- Should the DSL support nested/hierarchical states in v3 MVP or defer them?
- Should the DSL support invoked long-running services, cancellation, and timers in v3 MVP or model them as actions first?
- How should `onEnter` actions interact with process restoration to avoid accidentally restarting non-idempotent work?
- Should KSP `.mmd` generation ship first as generated unit-test infrastructure or as a dedicated Gradle plugin?
- Should `@AfsmGraph` live in `afsm-core` long term, or move to a smaller graph annotations module before public release?

Resolved:

- `AfsmTransition<S, C, F>` is acceptable if feature-local typealiases are documented as the standard convention.
- `Ignored` is overloaded; the API should add `AfsmDecision.Stayed` and `Afsm.stay(...)` before implementation.
- Use `Afsm` as public type prefix because the product name is Android State Machine.
- Use `AfsmNoEffect` sealed interface as the no-effect marker candidate.
- Use non-suspending fire-and-queue `AfsmHost.dispatch(event)` with serialized FIFO event processing.
- Use best-effort `Flow<F>` effect delivery with no replay by default.
- `AfsmNoEffect` and `AfsmTransition<S, C, F>` compile cleanly in `afsm-core` when used with feature-local typealiases and both no-effect and effectful flows.
- Provide a small reusable `afsm-runtime` module after `afsm-core`; keep Android ViewModel integration in a later module.
- MVP command execution policy is sequential and verified by `afsm-runtime` tests.
- MVP includes `afsm-runtime`.
- `afsm-viewmodel` exists as a thin AndroidX integration module with `ViewModel.afsmHost(...)`.
- Compose helpers and test helpers remain future modules.
- A Compose lifecycle-aware effect collection helper is now worth evaluating after the sample app showed repeated effect collection wiring in routes.
- Product registration is now a stronger reference than simple auth for explaining extended FSM self-transitions versus phase transitions.
- `Command` should be explained as a transition action/output, not as a user interaction event.
- ProductEditor naming cleanup has been applied and verified; graph generation now works through executable DSL topology and `.mmd` export.
- KSP graph generation should discover annotated `StateMachine` classes, generate a registry, then execute compiled `AfsmGraphSource.topology.toMmd()`; it should not parse DSL bodies or create graph-only models.
- The first `afsm-graph-ksp` slice now works for two real graphable state machines: `AuthStateMachine` and `ProductEditorStateMachine`.
- The current v3 direction is a scoped executable DSL where the machine definition is both runtime behavior and graph source.
- A minimal executable DSL and interpreter spike compiles and passes ProductEditor-like `afsm-core` tests.
- `AfsmMachine.topology` and `.mmd` export now work without sample events for declared branches; action labels, guard labels, entry node rendering, and duplicate declaration diagnostics remain unresolved.
- Real `sample-shop` ProductEditor has been migrated from the phased helper to the executable DSL and has focused unit coverage plus topology assertions.
- The phased-state API was removed from `afsm-core` after the executable DSL migration; it remains only as historical learning.
- In the phased profile, meaningful flow operations such as draft save should remain explicit phases like `SavingDraft` and `DraftSaved`; do not hide them as context-only flags just to reduce state count.
- Context-only updates should be reserved for actual data updates; ProductEditor's current public style is executable DSL branches plus `updateContext`, not entry-policy-driven reducers.
- The phased-state helper is superseded as the public v3 recommendation because `when + PhaseEntryPolicy` remains too convention-heavy for graph-synchronized FSM authoring.
