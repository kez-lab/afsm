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
- Product registration uses `EditingDraft`, `SavingDraft`, `DraftSaved`, `ImageUploadInProgress`, `ReviewSubmissionInProgress`, `Rejected`, `Approved`, `PublishInProgress`, and `Published`.
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
- ProductEditor has been renamed toward phase states such as `ImageUploadInProgress` and transition actions such as `StartImageUpload`.
- Before public API freeze, evaluate whether the public API should rename `Command`/`commands` to `Action`/`actions` or `TransitionAction`/`actions`.
- v3 should not force a DSL until terminology and naming are clearer in the existing plain Kotlin implementation.

## [2026-05-09] Refine v3 topology API toward from-state scopes

Decision: Refine the v3 topology-first design away from `transition<From, Event, To> { goTo(state, commands, effects) }` and toward a from-state-scoped topology companion plus plain Kotlin runtime reducers.

Rationale:

- Repeating `FromState` on every edge is noisy when the current typed state already defines the scope.
- `goTo(state, commands, effects)` kept the v2 transition-result-builder shape and did not address the CEO's concern that the API still felt unlike a state machine.
- A topology companion can provide explicit graph metadata while runtime behavior remains ordinary Kotlin `when` code with a typed state receiver.

Consequences:

- Superseded later: this intermediate shape was `from<FromState> { on<Event>().to<ToState>() }`.
- Runtime transition examples should use typed receiver functions such as `EditingDraft.transition(event)`.
- Transition actions should be chained from returned states, for example `ImageUploadInProgress(draft).withAction(StartImageUpload(draft))`, instead of passing `state`, `commands`, and `effects` together.
- A future prototype must verify that topology metadata and runtime reducers stay synchronized.

## [2026-05-09] Keep ProductEditor flow states as phases in the phased profile

Decision: In `State = Phase + Context`, keep meaningful flow states as `Phase` values and move only actual data into `Context`.

Rationale:

- The CEO asked for actual data such as `ProductDraft` to be separated from state constructors, not for business phases to be hidden as context fields.
- Demoting `SavingDraft` and `DraftSaved` into a `saveStatus` context field made ProductEditor less state-machine-like and harder to read.
- The intended authoring shape is reducer-visible `transitionTo(ProductEditorPhase.X)`, with entry policy hiding context update and command assembly.

Consequences:

- ProductEditor phases include `SavingDraft`, `DraftSaved`, `ImageUploadInProgress`, `ReviewSubmissionInProgress`, `Rejected`, `Approved`, `PublishInProgress`, and `Published`.
- `ProductDraft` and validation errors live in `ProductEditorContext`.
- `ProductEditorPhaseEntryPolicy` updates context and emits commands when phases are entered.
- Future phased samples should avoid context flags for durable flow states unless the value is purely data and not part of the state diagram.

## [2026-05-09] Prefer typed handlers over a topology DSL for v3

Decision: Supersede the `from/on/to` topology companion idea as the preferred v3 direction. Keep state machine authoring as plain Kotlin `when` code, and make graph extraction possible through concrete State/Event handler signatures plus `transitionTo` next-state extraction.

Rationale:

- The CEO had already rejected DSL-like structure as the main authoring style.
- `from<FromState> { on<Event>().to<ToState>() }` still feels like framework syntax even though it is cleaner than `transition<From, Event, To>`.
- `FromState` is available from a concrete state parameter or typed receiver.
- `Event` is available from a concrete event parameter.
- `ToState` is available from the `transitionTo(state = NextState(...))` argument, with optional `transitionTo<ToState>(...)` only if extraction needs help.
- This keeps Android/Kotlin developers in ordinary Kotlin control flow and preserves breakpoint/debug ergonomics.

Consequences:

- v3 documentation should recommend concrete handlers such as `submitClicked(state: EditingDraft, event: SubmitClicked)`.
- Helpers like `startUpload(draft, currentState: ProductEditorState)` are graph-hostile because they erase the concrete `FromState` and event.
- A future graph proof should first scan `ProductEditorStateMachine.kt` for handler signatures and `Afsm.transitionTo(...)` calls before introducing DSL or KSP.
- `transitionTo<From, Event, To>` should not be recommended because `From` and `Event` duplicate information already present in the handler signature.
- Superseded later by the executable statechart DSL direction.

## [2026-05-09] Keep v3 as a canonical synthesis page

Decision: Maintain `wiki/03-engineering/afsm-v3-topology-first-api.md` as the canonical v3 API direction page, despite the historical filename.

Rationale:

- Repeated corrections created a fragmented document that read like chat history instead of a current design.
- Future agents should not reconstruct the accepted v3 direction from logs, decision history, or superseded API sketches.
- At that time, the accepted direction was typed-handler Kotlin, not DSL-first topology registration. This was later superseded by the phased-state decision below.

Consequences:

- The v3 page title was then `Afsm v3 Typed Handler API`; it was later superseded by `Afsm v3 Phased State API` and then by `Afsm v3 Executable DSL`.
- Superseded ideas stay in one short section at the end of the page.
- Future design corrections must rewrite the canonical page directly and then append logs/decisions as supporting history.
- `AGENTS.md` and the wiki maintenance guide now include this canonical synthesis rule.

## [2026-05-09] Pivot v3 toward phased state with hidden entry policy

Decision: Make the current v3 API candidate a phased-state authoring profile: full Android screen state is modeled as `State = Phase + Context`, reducers transition by calling `transitionTo(Phase)`, and feature-local `PhaseEntryPolicy` performs phase-specific context updates plus command/effect emission.

Rationale:

- The CEO wants state diagrams to remain abstract and phase-oriented, not polluted by every `context.copy(...)` detail.
- Sealed state subtypes make graph nodes clear, but force every transition to manually preserve shared values such as draft, errors, save status, and retry counters.
- A plain `data class State(phase, context)` preserves the Android `UiState.copy(...)` ergonomics while `Phase` remains the finite node for diagrams.
- Hidden entry policy lets transitions read as "go to this phase" while still keeping context normalization and transition actions deterministic and testable.
- This avoids using durable state as a one-shot effect trigger because commands still leave through `AfsmTransition`.

Consequences:

- At that time, the canonical v3 page was `Afsm v3 Phased State API`. This was later superseded by `Afsm v3 Executable DSL`.
- `transitionTo(Phase)` is the preferred target experience for complex Android screen FSMs that need diagrams.
- `PhaseEntryPolicy` must remain feature-local and unit-tested so hidden context updates do not become invisible magic.
- Context-only updates such as form text changes or draft-save status should use `updateContext(...)`/`stay(...)` and should usually be omitted from the primary state diagram.
- The existing v2 runtime remains valid; phased state is an authoring profile layered over `AfsmTransition<S, C, F>` and `AfsmHost`.

## [2026-05-09] Make v3 an executable statechart DSL

Decision: Shift the recommended Afsm v3 public authoring model from `when + transitionTo(Phase) + PhaseEntryPolicy` to a scoped executable statechart DSL.

Rationale:

- The ProductEditor phased-helper spike improved data separation but still made behavior depend on conventions spread across reducers and entry policy.
- Graph extraction from `when` code would require fragile source inference and could drift from runtime behavior.
- Statechart references such as XState and SCXML center the model around explicit state scopes, event handlers, guards, entry/exit actions, and transition actions.
- Android guidance still requires `ViewModel` as the screen-level business state holder adapter, so the DSL must remain plain Kotlin and Android-free.

Consequences:

- The new canonical v3 page is `wiki/03-engineering/afsm-v3-executable-dsl.md`.
- The previous phased-state page is preserved only as superseded history.
- The v3 DSL must be executable; there must not be a separate graph-only DSL.
- The first implementation proof should be a Kotlin compile spike for `afsmMachine`, `state`, `on`, `guard`, `otherwise`, `updateContext`, `onEnter`, `action`, `effect`, and `transitionTo`.
- ProductEditor remains the reference flow for validating whether the DSL is readable and graphable.

## [2026-05-09] Keep graph output as generated `.mmd` files

Decision: Afsm graph output should be generated as `.mmd` files from executable machine topology, not as prose documentation beside the graph.

Rationale:

- The user expectation is that defining a state machine should produce a state graph artifact, not a separate explanatory document.
- `AfsmMachine.topology` already makes graph output a property of the machine definition, so the exporter should stay close to that runtime definition.
- The phased-state helper API added too much user-facing surface after the executable DSL direction became canonical.
- Names like `assign` and `AfsmEventBuilder` were unclear for Android developers; the API should prefer explicit context and branch terminology.

Consequences:

- `AfsmTopology.toMmd()` is the canonical graph renderer.
- `sample-shop` generates `sample-shop/build/generated/afsm/mmd/ProductEditorStateMachine.mmd` through `:sample-shop:generateAfsmMmd`.
- `AfsmPhasedStateMachine` and related phased helper classes are removed from `afsm-core`.
- The current event receiver type is `AfsmEventBranchScope`, meaning the scope behind `on<Event> { ... }` that declares ordered graphable branches.

## [2026-05-09] Use KSP for graph discovery, not graph interpretation

Decision: KSP-based graph generation should discover explicitly annotated Afsm machine providers and generate registry code. The compiled registry should instantiate the real machines and write `.mmd` files from `AfsmMachine.topology.toMmd()`.

Rationale:

- KSP is well-suited for symbol discovery and code generation, not for executing app code during symbol processing.
- Static parsing of DSL function bodies would duplicate the executable DSL and risk graph/runtime drift.
- Annotation-based registration gives stable graph ids and file names while avoiding accidental export of internal machines.
- Android modules already have a JVM unit-test runtime that can instantiate app code, making the first implementation spike practical without a custom Gradle classpath runner.

Consequences:

- Add `@AfsmGraph` as the first registration API candidate.
- Add an `afsm-graph-ksp` module candidate for `AfsmGraphProcessorProvider`.
- Generate a module-local `AfsmGeneratedGraphRegistry`.
- Prefer a generated unit-test writer for the MVP; evaluate a dedicated Gradle plugin after the registry proof works.
