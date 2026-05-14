---
title: Open Questions
updated: 2026-05-14
---

# Open Questions

## Architecture

- Should UI one-shot actions be modeled as `Effect`, as terminal `State`, or through UI state flags depending on the case?
- Should state machines support hierarchical/nested state machines from the start?

## Android Integration

- How should process restoration be handled: reconstruct from `SavedStateHandle`, persist only selected state, or require domain reload?
- Which FSM states are safe to restore directly, and which should be reconstructed from a minimal key plus repository data?

## Scope

- What should the first public API look like before it becomes too framework-like?
- Should a dedicated `afsm-test` module exist, or should testing stay as documentation and plain Kotlin assertions until repeated helper needs are proven?
- What sample flows best prove the library's value to external Android teams?

Resolved:

- First real reference flow: signup + identity verification + retry.
- The first complex app-level validation is a shopping sample with auth, product, review, like, and checkout retry flows.
- Public example ladder: README minimal Draft, Auth, Checkout, ProductEditor, and non-Afsm data screens as anti-examples.
- Checkout is now the mid-size graphable example for dynamic initial state, retry, stale command results, durable completion, and render-state mapping.

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
- Should the DSL support nested/hierarchical states in v3 MVP or defer them?
- Should the DSL support invoked long-running services, cancellation, and timers in v3 MVP or model them as actions first?
- How should `onEnter` actions interact with process restoration to avoid accidentally restarting non-idempotent work?
- Should KSP `.mmd` generation ship first as generated unit-test infrastructure or as a dedicated Gradle plugin?
- Should `@AfsmGraph` live in `afsm-core` long term, or move to a smaller graph annotations module before public release?
- Should external MMD generation ship as a Gradle plugin, a documented task template, or a simple runtime writer API first?
- How should command-result event overflow be handled when the event queue is full: fail the command, reserve internal capacity, split command-result queues, or introduce a policy?
- What exact restoration policy should be documented for phases whose `onEnter` would normally start non-idempotent work?

Resolved:

- `AfsmTransition<S, C, F>` is acceptable if feature-local typealiases are documented as the standard convention.
- Pre-release compatibility aliases were removed before public documentation, so only current API names should be used in new source and docs.
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
- `afsm-compose` exists as an optional thin Compose helper module; test helpers remain future work.
- A Compose lifecycle-aware effect collection helper is now worth evaluating after the sample app showed repeated effect collection wiring in routes.
- Product registration is now a stronger reference than simple auth for explaining extended FSM self-transitions versus phase transitions.
- `Command` should be explained as host-executed transition output, not as a user interaction event.
- ProductEditor naming cleanup has been applied and verified; graph generation now works through executable DSL topology and `.mmd` export.
- KSP graph generation should discover annotated `StateMachine` classes, generate a registry, then execute compiled `AfsmGraphSource.topology.toMmd()`; it should not parse DSL bodies or create graph-only models.
- The first `afsm-graph-ksp` slice now works for two real graphable state machines: `AuthStateMachine` and `ProductEditorStateMachine`.
- The current v3 direction is a scoped executable DSL where the machine definition is both runtime behavior and graph source.
- A minimal executable DSL and interpreter spike compiles and passes ProductEditor-like `afsm-core` tests.
- `AfsmMachine.topology` and `.mmd` export now work without sample events for declared branches; guard labels, command labels, effect labels, transition kind, fallback flags, and duplicate declaration diagnostics exist. Entry node rendering remains future work.
- Use `AfsmReducer<S, E, C, F>` for the low-level host contract, `AfsmMachine<S, E, C, F>` for graphable feature boundaries, and `AfsmPhaseMachine<P, X, E, C, F>` for the executable phase/context DSL machine.
- Remove pre-release compatibility aliases before writing public docs; `AfsmStateMachine`, `AfsmStateChart`, `afsmStateChart`, `AfsmStateChartMachine`, and `AfsmChartState` should not appear in the public API surface.
- Use `Command` consistently for host-executed transition outputs. Do not rename command outputs to action in the current API.
- `AfsmState<Phase, Context>` is the current standard state value for executable machines. Features should use a typealias and delegate directly to the machine; custom sealed UI states require a feature-owned `AfsmReducer` instead of a core adapter base.
- The DSL includes flat `onExit`; transition execution order is `onExit -> transition block -> onEnter` for phase-changing transitions.
- Initial state construction does not run `onEnter`; startup work should be triggered by an explicit event such as `ScreenEntered` or by a future dedicated `initialTransition` API if needed.
- `AfsmHost` command-handler exceptions use `AfsmCommandFailurePolicy`: `Throw` by default for programmer errors, `Record` when a resilient host should log and continue. `CancellationException` is always rethrown.
- MVP commands are not automatically cancelled by later events. Cancellation is explicit through feature commands/events, while future invoked-service support can add structured cancellation semantics.
- `ignore(...)` is intentional handled no-op behavior. Ordinary unhandled event/phase combinations should be omitted and become invalid decisions.
- Real `sample-shop` ProductEditor has been migrated from the phased helper to the executable DSL and has focused unit coverage plus topology assertions.
- The phased-state API was removed from `afsm-core` after the executable DSL migration; it remains only as historical learning.
- In the phased profile, meaningful flow operations such as draft save should remain explicit phases like `SavingDraft` and `DraftSaved`; do not hide them as context-only flags just to reduce state count.
- Context-only updates should be reserved for actual data updates; ProductEditor's current public style is executable DSL branches plus `updateContext`, not entry-policy-driven reducers.
- The phased-state helper is superseded as the public v3 recommendation because `when + PhaseEntryPolicy` remains too convention-heavy for graph-synchronized FSM authoring.
- `AfsmChartState` has been removed before public API stabilization; use `AfsmState`.
- Same-named factory functions conflict with Kotlin typealias constructors, so features should use lowercase factories such as `productEditorState()` when they need default initial state values.
- Do not add a shared `AfsmStateFactory` API yet; the spike showed it needs explicit `<Phase, Context>` type arguments for singleton phase hierarchies and does not justify the extra public concept.
- Invalid transitions should throw by default for public runtime use so flow bugs are visible during development. Resilient production hosts can opt into `AfsmInvalidTransitionPolicy.Record` with a logger.
- Command execution remains sequential, but it no longer blocks later event reduction; commands run through a separate command processor and dispatch results back into the event queue.
- Ten-agent POC review confirmed that Afsm should target complex transaction/flow screens, not simple data-display ViewModels.
- `AfsmGraphReducer` was removed before public release docs; `AfsmMachine<State, Event, Command, Effect>` is now the graphable feature-boundary API.
- `afsmHost(machine = ..., initialState = ...)` is the standard dynamic initial-state API for navigation arguments and `SavedStateHandle` reconstruction.
- `AfsmConfig.commandQueueCapacity` is configurable and defaults to `64`.
- The first documented stale command result pattern is request/correlation id; Checkout now models payment request ids and ignores stale results.
- Generated MMD defaults to `AfsmMmdOptions.Flow`, which hides ordinary internal self-loops; `AfsmMmdOptions.Full` remains available for complete topology debugging.
- `CollectAfsmEffects(...)` is the official optional Compose effect collection helper.
- Public API/ABI hardening removed `addState`, `addBranch`, `addEventDefinition`, and `afsmLabelForClass` from the API dump.
- `AfsmTransition` is factory-based; ignored/invalid transitions cannot carry public commands/effects.
- Checkout completed-payment state is now durable and duplicate pay/retry after completion is ignored.
- `.mmd` graph output file names must be safe relative `.mmd` paths.
- Command queue overflow now fails fast with `AfsmCommandQueueOverflowException`; machines should emit fewer/coarser commands or increase `commandQueueCapacity`.
- Default effect delivery has no replay; late collectors do not receive old one-shot effects.
- Restoration/effect/command policy is documented in `docs/restoration-effect-command-policy.md`; implementation helpers remain future work.
