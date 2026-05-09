---
title: Decision Log
updated: 2026-05-09
---

# Decision Log

## [2026-05-01] Use ViewModel-backed FSM for complex Android flows

Decision: Keep Android `ViewModel`, but model complex screen/business flows with plain Kotlin finite state machines.

Rationale:

- MVVM/UDF improves separation and lifecycle behavior but fragments the local reading path.
- The traceability discomfort is real, not merely personal preference.
- FSMs make valid states, events, invalid transitions, and async command boundaries explicit.
- Keeping FSMs plain Kotlin preserves fast unit tests and reduces Android coupling.

Consequences:

- Future complex screens should define `State`, `Event`, `Command`, and optional `Effect`.
- `ViewModel` should execute the FSM and commands, not become a large transition table itself.
- Simple screens may continue using ordinary `UiState` and ViewModel functions.

Source: [Android ViewModel FSM Discussion](../../raw/conversations/2026-05-01-android-viewmodel-fsm-discussion.md)

## [2026-05-01] Use LLM Wiki for architecture memory

Decision: Maintain architecture context as an LLM Wiki with immutable raw sources and concise synthesized wiki pages.

Rationale:

- The project is early and architecture reasoning should compound across future Codex sessions.
- Raw conversations are too long for practical repeated reading.
- Wiki pages make the current direction, decisions, and open questions durable.

Consequences:

- Keep raw source material in `raw/`.
- Update `wiki/index.md` and `wiki/log.md` whenever durable knowledge changes.
- Use `AGENTS.md` to point Codex at the relevant wiki pages.

## [2026-05-01] Product goal is an Android FSM library

Decision: The final target is a reusable Android-focused FSM library, not merely documentation or an app-local pattern.

Rationale:

- The recurring pain is broad: MVVM/UDF improves separation but obscures flow traceability in complex Android screens.
- A library can standardize state/event/command modeling, ViewModel execution, transition diagnostics, and test helpers.
- The library should remain Android-aligned instead of replacing official ViewModel and UI layer guidance.

Consequences:

- Product strategy, API design, sample apps, documentation, testing utilities, and release planning are first-class workstreams.
- The first implementation should still start with a reference flow before extracting too much abstraction.
- Public API minimalism is a major design constraint.

## [2026-05-01] First reference flow is signup identity verification with retry

Decision: Use a signup + identity verification + retry flow as the first reference flow.

Rationale:

- It is complex enough to demonstrate meaningful FSM value beyond simple loading/content/error.
- It exercises external UI effects, async command results, retry policy, invalid transitions, process restoration questions, and navigation boundaries.
- It is common enough for Android teams to understand without deep domain explanation.

Consequences:

- Stage 1 implementation should build this flow before extracting reusable runtime helpers.
- API design questions should be evaluated against this flow first.
- Sample and documentation should use this flow to teach State/Event/Command/Effect boundaries.

## [2026-05-01] Use Afsm as the working library name

Decision: Use `Afsm` as the working product/API name, expanded as Android FSM.

Rationale:

- The name is short and maps directly to the product category.
- Kotlin public types can use `Afsm` idiomatically, while artifacts can use lowercase `afsm-*`.
- The risk is acronym ambiguity, so README and documentation must define it immediately.

Consequences:

- Public API drafts use `AfsmStateMachine`, `AfsmTransition`, `AfsmHost`, and related names.
- Package and artifact naming should use lowercase `afsm`.
- The name can still be revisited before external release if user testing shows confusion.

## [2026-05-03] Afsm public API v2 candidate

Decision: Draft v2 keeps the `Afsm` public prefix and proposes `AfsmDecision.Stayed`, `Afsm.stay(...)`, `AfsmNoEffect`, best-effort no-replay effect delivery, non-suspending serialized dispatch, and sequential command execution as MVP candidates.

Rationale:

- The CEO confirmed that the API should communicate Android State Machine directly.
- Signup pseudo-implementation review showed generic verbosity is acceptable with typealiases, but `Ignored` was semantically overloaded.
- Android lifecycle review showed effect delivery and dispatch semantics must be explicit before implementation.

Consequences:

- Implementation should not start until this v2 candidate is explicitly accepted.
- The next design/implementation pass should validate `AfsmNoEffect` and dispatch/effect behavior with real Kotlin tests.

## [2026-05-03] Start afsm-core as plain Kotlin core module

Decision: Create the first implementation skeleton as a Kotlin/JVM `afsm-core` module with Kotlin stdlib as the only core dependency.

Rationale:

- The v2 API candidate needs real compiler feedback before runtime or ViewModel work.
- `afsm-core` should remain free of Android, AndroidX, coroutine, Compose, serialization, DI, and code generation dependencies.
- Plain Kotlin compile checks are enough to validate public type ergonomics before adding runtime behavior.

Consequences:

- The initial source package is `afsm.core`.
- `AfsmTransition<S, C, F>`, `AfsmNoEffect`, `AfsmDecision`, `AfsmStateMachine`, and the `Afsm` builder object are now concrete public source files.
- Runtime dispatch, command execution, effect delivery, ViewModel integration, and test helper APIs remain outside `afsm-core` for later tasks.

## [2026-05-09] Add afsm-runtime as coroutine-based runtime module

Decision: Implement `afsm-runtime` as a small Kotlin coroutine runtime that depends on `afsm-core` and `kotlinx-coroutines-core`, but not on Android or AndroidX.

Rationale:

- Dispatch serialization, command execution, effects, and diagnostics are reusable runtime mechanics, not ViewModel-specific behavior.
- Android developers can still use the runtime naturally by attaching `AfsmHost` to `viewModelScope`.
- Keeping AndroidX out of runtime preserves a clean module boundary before adding `afsm-viewmodel`.
- Sequential command execution is easier to reason about and test than parallel/cancel-latest behavior in the MVP.

Consequences:

- `AfsmHost.dispatch(event)` is non-suspending and queues events for serialized FIFO processing.
- Commands execute sequentially and dispatch result events back into the same queue.
- Effects are best-effort `Flow<F>` outputs with no replay by default.
- `Ignored` and `Invalid` decisions keep the current runtime state and drop outputs.
- ViewModel integration remains a separate next module.

## [2026-05-09] Enforce spec-first TDD guardrails for AI agents

Decision: Project-scoped AI agents must treat tests as executable specification and must not weaken tests merely to make failing implementation pass.

Rationale:

- The library is intended to encode flow correctness, so verification integrity is part of the product quality bar.
- AI agents commonly misclassify implementation failures as test failures and silently rewrite the target.
- TDD-style behavior specification gives future implementation work a stable regression surface.

Consequences:

- `AGENTS.md` now includes mandatory failure triage and test change rules.
- Detailed process guidance lives in `wiki/07-llm/ai-engineering-guardrails.md`.
- Intentional behavior changes must update wiki/spec/decision material before tests are rewritten.
- Bug fixes should add or preserve regression tests before production code changes.

## [2026-05-09] Add afsm-viewmodel as thin AndroidX integration

Decision: Implement `afsm-viewmodel` as an Android library module with a single `ViewModel.afsmHost(...)` helper over `AfsmHost`.

Rationale:

- Android developers should not manually pass `viewModelScope` every time they adopt Afsm.
- Composition over inheritance keeps Hilt, Koin, manual DI, and feature-specific ViewModel constructors free.
- `lifecycle-viewmodel-ktx` is an Android artifact, so `afsm-viewmodel` should be an Android library module rather than a JVM module.
- Runtime behavior remains in `afsm-runtime`; `afsm-viewmodel` only owns lifecycle scope wiring.

Consequences:

- The project now uses Android Gradle Plugin for the ViewModel integration module.
- `ViewModel.afsmHost(...)` supplies `viewModelScope`.
- `SavedStateHandle`, navigation, and Compose helpers remain outside this module for now.
- AndroidX is enabled with `android.useAndroidX=true`.

## [2026-05-09] Add sample-shop as the first complex app validation

Decision: Add a `:sample-shop` Android app module that uses Compose, ViewModel, Room, Navigation Compose, and Afsm to validate real-world ergonomics.

Rationale:

- Public API drafts and pseudo implementations do not prove whether Android developers can use Afsm naturally in a multi-screen app.
- A shopping app exercises authentication, local persistence, likes, product creation, detail screens, review creation/listing, fake payment, failure, and retry.
- Afsm should be applied where state transition correctness matters, not forced into every data screen.

Consequences:

- Auth and checkout are Afsm-backed reference flows.
- Catalog, product editor, product detail, likes, and reviews remain ordinary ViewModel + Flow screens.
- Manual DI is used for now to keep dependencies focused on Android/Kotlin and avoid hiding Afsm usage behind a DI framework.
- The sample creates pressure for a public tutorial and possibly a Compose effect collection helper.

## [2026-05-09] Use explicit sealed phases for stronger reference FSMs

Decision: Rewrite sample-shop auth and product registration to use explicit sealed phase states instead of flat UI state for the core FSM examples.

Rationale:

- A flat state data class with fields like `isLoading` can compile, but it does not make the state diagram obvious.
- Android forms still need self-transitions for text changes, but those should happen inside named phases such as `Editing`.
- Product registration with review rejection, resubmission, approval, and publishing better demonstrates why Afsm is useful than simple CRUD.

Consequences:

- Auth uses `Editing`, `Submitting`, and `Authenticated`.
- Product registration uses `EditingDraft`, `SavingDraft`, `DraftSaved`, `UploadingImages`, `SubmittingForReview`, `Rejected`, `Approved`, `Publishing`, and `Published`.
- Text edits are treated as self-transitions inside editable phases.
- Product registration is now an Afsm-backed reference flow instead of an ordinary ViewModel screen.

## [2026-05-09] Explore topology-first API without replacing v2 yet

Decision: Treat `transition<From, Event, To>` as a v3 exploration, not an immediate replacement for the v2 reducer-style API.

Rationale:

- v2 is plain Kotlin and already works with `AfsmHost`, but it hides graph topology inside reducer bodies and helper functions.
- Automatic state diagram generation is much easier if the edge is declared explicitly.
- A new DSL-like API could feel framework-heavy, so it should be proven with ProductEditor before becoming a recommendation.

Consequences:

- v2 remains the current implemented low-level engine.
- v3 should first be prototyped as an optional topology-first authoring layer.
- ProductEditor is the reference flow for evaluating whether the readability and graph-generation benefits justify the added API surface.

## [2026-05-09] Treat Command as a transition action

Decision: Explain Afsm `Command` as a transition action/output emitted by the state machine and executed by the host, rather than as another event entering the machine.

Rationale:

- User confusion came from state names and command names describing the same work, for example `UploadingImages` plus `UploadImages`.
- A state should describe the current business phase, while a transition action should describe work to start because the transition happened.
- The standard state-machine reading is still valid: `CurrentState -- Event / Action --> NextState`.
- Merging actions into state flags would make durable state carry one-shot execution intent and create restoration/recollection hazards.

Consequences:

- Documentation should use "transition action" when teaching `Command`.
- ProductEditor should be renamed toward phase states such as `ImageUploadInProgress` and transition actions such as `StartImageUpload`.
- Before public API freeze, evaluate whether the public API should rename `Command`/`commands` to `Action`/`actions` or `TransitionAction`/`actions`.
- v3 should not force a DSL until terminology and naming are clearer in the existing plain Kotlin implementation.
