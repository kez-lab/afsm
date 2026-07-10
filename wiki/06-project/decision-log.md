---
title: Decision Log
updated: 2026-05-25
---

# Decision Log

## [2026-05-25] Add AfsmNoCommand marker for no-command machines

Decision: Add `AfsmNoCommand` as the public command marker for machines that
never emit host-executed work.

Rationale:

- `AfsmNoEffect` already gives no-effect machines a named type argument.
- Command-free flows otherwise need a fake empty command sealed interface.
- A marker keeps feature typealiases explicit without adding another DSL or
  host overload.

Consequences:

- Feature code can use `AfsmMachine<State, Event, AfsmNoCommand, Effect>` when
  there is no repository, timer, database, or SDK work.
- `consumer-smoke` must compile this marker from Maven Local artifacts so the
  public API remains externally usable.

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
- The first implementation proof should be a Kotlin compile spike for `afsmStateChart`, `state`, `on`, `guard`, `otherwise`, `updateContext`, `onEnter`, `action`, `effect`, and `transitionTo`.
- ProductEditor remains the reference flow for validating whether the DSL is readable and graphable.

## [2026-05-09] Keep graph output as generated `.mmd` files

Decision: Afsm graph output should be generated as `.mmd` files from executable machine topology, not as prose documentation beside the graph.

Rationale:

- The user expectation is that defining a state machine should produce a state graph artifact, not a separate explanatory document.
- `AfsmStateChart.topology` already makes graph output a property of the chart definition, so the exporter should stay close to that runtime definition.
- The phased-state helper API added too much user-facing surface after the executable DSL direction became canonical.
- Names like `assign` and `AfsmEventBuilder` were unclear for Android developers; the API should prefer explicit context and branch terminology.

Consequences:

- `AfsmTopology.toMmd()` is the canonical graph renderer.
- `sample-shop` generates `sample-shop/build/generated/afsm/mmd/ProductEditorStateMachine.mmd` through `:sample-shop:generateAfsmMmd`.
- `AfsmPhasedStateMachine` and related phased helper classes are removed from `afsm-core`.
- The current event receiver type is `AfsmEventBranchScope`, meaning the scope behind `on<Event> { ... }` that declares ordered graphable branches.

## [2026-05-09] Use KSP for graph discovery, not graph interpretation

Decision: KSP-based graph generation should discover explicitly annotated Afsm `StateMachine` classes and generate registry code. The compiled registry should instantiate the real state machines and write `.mmd` files from `AfsmGraphSource.topology.toMmd()`.

Rationale:

- KSP is well-suited for symbol discovery and code generation, not for executing app code during symbol processing.
- Static parsing of DSL function bodies would duplicate the executable DSL and risk graph/runtime drift.
- Class annotation keeps the user-facing API aligned with the thing Android developers recognize as the state machine.
- Annotation-based registration gives stable graph ids and file names while avoiding accidental export of internal machines.
- Android modules already have a JVM unit-test runtime that can instantiate app code, making the first implementation spike practical without a custom Gradle classpath runner.

Consequences:

- Add `@AfsmGraph` on `StateMachine` classes as the first registration API candidate.
- Add an `AfsmGraphSource` topology contract so graphable state machines expose `AfsmTopology` without forcing all reducers to do so.
- Add an `afsm-graph-ksp` module candidate for `AfsmGraphProcessorProvider`.
- Generate a module-local `AfsmGeneratedGraphRegistry`.
- Prefer a generated registry plus existing/generic writer for the MVP; evaluate a dedicated Gradle plugin after the registry proof works.

## [2026-05-09] Rename executable machine concepts to statechart concepts

Decision: Use `AfsmStateChart` for the DSL-built executable `Phase + Context` definition, and keep `AfsmStateMachine` as the host-facing reducer contract used by `AfsmHost` and Android `ViewModel` integration.

Rationale:

- `AfsmStateMachine` and `AfsmMachine` were too close in meaning and made it unclear which type an Android developer should implement.
- The DSL object is more accurately a statechart because it has finite phases, extended context, entry actions, guards, and topology metadata.
- Android-facing code should still receive one state object. At this point `AfsmChartState<Phase, Context>` combined the DSL runtime state and `AfsmStateChartMachine` adapted it to a feature screen state; this was superseded on 2026-05-10 by `AfsmState<Phase, Context>`.
- `topology = chart.topology` forwarding should be structural, not hand-written in every state-machine class.
- `ignore(...)` should remain an explicit handled no-op for observability and tests; omitted handlers should represent invalid/unhandled transitions.

Consequences:

- New code should use `AfsmStateChart`, `AfsmState`, and `afsmStateChart`; `AfsmChartState` is only a compatibility alias.
- `AfsmMachine`, `AfsmSnapshot`, and `afsmMachine` are removed from the current spike API rather than kept as aliases, so IDE completion does not keep surfacing the confusing names.
- Feature-local typealiases such as `ProductEditorChart` are the standard way to keep long generic lists out of user-facing code.
- Graphable feature machines should prefer `AfsmStateChartMachine` when they need to expose `AfsmStateMachine<S, E, C, F>` and `AfsmGraphSource` at the same time.

## [2026-05-10] Use AfsmState as the standard phase/context state

Decision: Promote `AfsmState<P, X>` to the standard Afsm state value for executable charts, where `P` is the finite phase and `X` is the extended context.

Rationale:

- The user expectation is that Afsm state should visibly be `phase + context`, not a hidden chart-only value.
- XState-style state plus context maps well to the ProductEditor problem: the graph is the phase, while form/draft/retry data lives in context.
- A concrete data class avoids the factory/copy problem that appears when `AfsmState` is only an interface implemented by feature data classes.
- If `AfsmStateChart` itself implements `AfsmStateMachine<AfsmState<P, X>, ...>` and `AfsmGraphSource`, a feature with the standard state shape can delegate directly to the chart and avoid adapter boilerplate.

Consequences:

- `AfsmChartState` is deprecated as a compatibility alias to `AfsmState`.
- ProductEditor now uses `typealias ProductEditorState = AfsmState<ProductEditorPhase, ProductEditorContext>`.
- ProductEditor no longer needs `toChartState` / `toScreenState` mapping; `ProductEditorStateMachine` delegates to its chart.
- Features that need custom Android-facing sealed states can still use `AfsmStateChartMachine` as an adapter.
- Kotlin does not allow a same-named factory next to a typealias constructor, so default initial states should use a feature-local lowercase factory such as `productEditorState()`.
- The public name `AfsmStateChart` remains an open product/API naming question before release.

## [2026-05-10] Rename public reducer and machine concepts

Decision: Use `AfsmReducer<S, E, C, F>` for the low-level `state + event -> transition` contract, and use `AfsmMachine<P, X, E, C, F>` for the DSL-built executable `phase + context` machine.

Rationale:

- The previous pair `AfsmStateMachine` and `AfsmMachine` made it unclear which type Android developers should implement or pass to `AfsmHost`.
- `Reducer` communicates the pure transition role more accurately and aligns with reducer-style references where side effects are kept outside the reducer.
- `Machine` is the better product-facing name for the statechart DSL because it owns executable topology, `initialState`, and `.mmd` graph metadata.
- The user explicitly accepted the Afsm name and asked to keep command terminology consistent.

Consequences:

- New code uses `AfsmReducer`, `AfsmMachine`, `afsmMachine`, and `AfsmMachineAdapter`.
- `AfsmStateMachine`, `AfsmStateChart`, `afsmStateChart`, and `AfsmStateChartMachine` remain only as deprecated compatibility aliases during the spike.
- `ViewModel.afsmHost(...)` now accepts a `reducer` parameter.
- KSP graph validation now requires `@AfsmGraph` classes to implement `AfsmReducer` and `AfsmGraphSource`.

## [2026-05-10] Use Command as the transition output term

Decision: Keep `Command` as the public term for host-executed work emitted by an Afsm transition or entry/exit handler.

Rationale:

- Elm uses commands as values returned to the runtime for external work, while Redux keeps async side effects outside reducers; Afsm follows the same separation by returning commands from transitions and executing them in `AfsmHost`.
- In Afsm, user and system inputs are `Event`; host-executed outputs are `Command`; UI-side one-shots are `Effect`.
- Using `Action` would collide with Redux/action vocabulary and with XState's broader action concept.

Consequences:

- DSL scopes expose `command(...)`, not `action(...)`.
- The DSL generic is consistently `C`, and samples use command types such as `StartImageUpload`.
- Phase names describe durable flow state, for example `ImageUploadInProgress`; command names describe host work, for example `StartImageUpload`.

## [2026-05-10] Define entry, exit, and initial semantics

Decision: Add flat `onExit` support and execute phase-changing transitions in the order `onExit -> transition block -> onEnter`. Initial state construction does not automatically run `onEnter`.

Rationale:

- SCXML specifies leaving-state executable content, transition executable content, then entering-state executable content.
- XState and KStateMachine both model entry/exit actions, and Android flows need a clean place to cancel timers, polling, uploads, and subscriptions when leaving a phase.
- Running `onEnter` automatically for initial state would make Android recreation dangerous for non-idempotent commands. Startup work should be explicit through events such as `ScreenEntered`, or a future dedicated initial-transition API if product review requires it.

Consequences:

- `stay(...)` is an internal handled branch and does not run exit/entry handlers.
- `transitionTo(...)` is a phase-changing branch and runs exit/entry handlers.
- `onExit` can emit commands/effects and update context before the transition block runs.

## [2026-05-10] Harden topology and command failure policy

Decision: Validate DSL definitions at build time, enrich topology metadata, and add configurable command failure handling to `AfsmHost`. MVP command cancellation remains explicit; later events do not automatically cancel already-running commands.

Rationale:

- Graph generation must fail early when the executable topology is inconsistent, rather than producing stale or misleading `.mmd` files.
- Topology needs more than `from/event/to` for useful tooling: transition kind, fallback branches, guard labels, command labels, and effect labels are now part of the model.
- Kotlin and Android coroutine guidance says `CancellationException` must not be swallowed. Unexpected command exceptions should be visible by default, while resilient hosts still need an opt-in logging mode.

Consequences:

- `afsmMachine { ... }` throws `AfsmDefinitionException` for missing initial state declarations, duplicate state declarations, duplicate event handlers within a state, and transitions to undeclared states.
- `AfsmTopologyTransition` now includes `guardLabel`, `commandLabels`, `effectLabels`, `kind`, and `isFallback`.
- `AfsmHost` defaults to `AfsmCommandFailurePolicy.Throw`.
- `AfsmCommandFailurePolicy.Record` logs an `AfsmDiagnostic` with the failed command and throwable, then keeps processing later events.
- `CancellationException` is always rethrown regardless of command failure policy.
- Features that need cancellation should model it explicitly with commands such as `CancelUpload` or domain events such as `UploadCancelled`; structured invoked-service cancellation is deferred.

## [2026-05-11] Remove pre-release compatibility aliases before public docs

Decision: Remove deprecated pre-release aliases from source before writing public README/API docs.

Removed aliases:

- `AfsmStateMachine` -> `AfsmReducer`
- `AfsmStateChart` -> `AfsmMachine`
- `afsmStateChart` -> `afsmMachine`
- `AfsmStateChartMachine` -> `AfsmMachineAdapter`
- `AfsmChartState` -> `AfsmState`

Rationale:

- Afsm has not been published yet, so there is no external binary compatibility contract to preserve.
- Keeping aliases would pollute IDE completion and make examples less decisive.
- Public docs should teach one vocabulary: reducer for low-level transition logic, machine for executable phase/context topology.

Consequences:

- Public source no longer exports the old aliases.
- `README.md` and `docs/afsm-public-api.md` document only current API names, with a short removed-alias mapping for migration context.
- After the first published artifact, breaking renames should require a deliberate versioning decision.

## [2026-05-11] Use Maven Local as the first publication gate

Decision: Treat `publishToMavenLocal` as the first release-readiness gate before Maven Central or other remote repository work.

Rationale:

- Maven Local validates Gradle publications, generated POM/module metadata, sources jars, and the Android AAR without requiring credentials or repository ownership decisions.
- Official Gradle publishing uses `maven-publish` and Maven publications; Android library variants expose publishable components through AGP's publishing DSL.
- Maven Central still needs product decisions for final group id, license, SCM URL, signing, and release automation.

Consequences:

- Current pre-release local coordinates are `io.github.afsm:afsm-core:0.1.0-SNAPSHOT`, `io.github.afsm:afsm-runtime:0.1.0-SNAPSHOT`, `io.github.afsm:afsm-viewmodel:0.1.0-SNAPSHOT`, and `io.github.afsm:afsm-graph-ksp:0.1.0-SNAPSHOT`.
- `afsm-viewmodel` publishes the `release` Android library variant as an AAR with sources.
- JVM modules publish jars, sources jars, and javadoc jars.
- Final Maven Central coordinates remain open until product approval.

## [2026-05-11] Add a separate Maven Local consumer smoke gate

Decision: Verify pre-release artifacts from a separate Android Gradle build before treating Maven Local publication as release-ready.

Rationale:

- Multi-module builds can hide publication mistakes because samples may still compile through `project(...)` dependencies.
- Afsm has both JVM jars and an Android AAR; a real Android consumer must resolve all of them through Gradle metadata and POMs.
- `afsm-graph-ksp` must work as an external KSP processor, not only as an included project.

Consequences:

- `consumer-smoke` is intentionally not included in the root `settings.gradle.kts`.
- `scripts/verify-consumer-smoke.sh` publishes Afsm to Maven Local and compiles the separate Android consumer.
- The consumer smoke covers `afsm-core`, `afsm-runtime`, `afsm-viewmodel`, and `afsm-graph-ksp`.
- Remote release work should keep this smoke green before publishing public artifacts.

## [2026-05-11] Enable Kotlin explicit API for public library modules

Decision: Enable Kotlin `explicitApi()` for `afsm-core`, `afsm-runtime`, `afsm-viewmodel`, and `afsm-graph-ksp`.

Rationale:

- Afsm is approaching a public library boundary; public declarations should not be accidental.
- Explicit API mode forces public visibility and return types to be intentional before binary compatibility validation is introduced.
- This is a low-cost compiler gate available through the Kotlin Gradle plugin without extra release tooling.

Consequences:

- Future public API additions must declare visibility and signatures clearly.
- Internal implementation helpers should remain `internal` or `private`.
- Binary API validation is still needed before the first public remote release.

## [2026-05-11] Add binary API validation before first public release

Decision: Add JetBrains `org.jetbrains.kotlinx.binary-compatibility-validator` to the root build and commit API dumps for published Afsm modules.

Rationale:

- Explicit API mode prevents accidental source-level public declarations, but it does not preserve binary compatibility across releases.
- API dumps provide a reviewable artifact when changing public ABI.
- The current project uses Kotlin `2.0.21`; Kotlin Gradle plugin built-in ABI validation starts later than that, so the maintained external validator is the pragmatic current gate.

Consequences:

- `apiCheck` becomes part of the local release gate.
- `sample-shop` is excluded because it is not a published library module.
- Published modules now have committed API baselines under their module-local `api/` directories.
- Some `@PublishedApi internal` DSL helpers appear in the ABI baseline because public inline/reified DSL functions can depend on them.

## [2026-05-11] Remove AfsmMachineAdapter before public API stabilization

Decision: Remove `AfsmMachineAdapter` from `afsm-core` and make `AfsmState<Phase, Context>` the single recommended state model for graphable `AfsmMachine` usage.

Rationale:

- The adapter encouraged two state models for one screen: a custom UI sealed state plus the executable machine's `AfsmState<Phase, Context>`.
- That mapping layer added boilerplate and made it harder to explain the primary Afsm path.
- `AfsmMachine` already implements `AfsmReducer<AfsmState<Phase, Context>, ...>` and `AfsmGraphSource`, so direct delegation is enough for graphable machines.

Consequences:

- Auth now follows the same state shape as ProductEditor through `typealias AuthState = AfsmState<AuthPhase, AuthContext>`.
- Custom sealed UI states remain possible, but teams must own a custom `AfsmReducer` wrapper instead of relying on a core adapter base.
- Public docs and API dumps no longer expose `AfsmMachineAdapter`.

## [2026-05-11] Reject shared AfsmStateFactory for now

Decision: Do not add a shared `AfsmStateFactory` API for `authState()` / `productEditorState()` style feature factories at this stage.

Rationale:

- The spike compiled only after declaring explicit type arguments such as `afsmStateFactory<AuthPhase, AuthContext>(...)`; otherwise Kotlin inferred singleton object phases too narrowly.
- The API would add a new public class plus factory function to remove only a small feature-local helper.
- A plain feature-local function is more obvious to Android/Kotlin users than a callable factory object stored in a `val`.

Consequences:

- Keep feature-local lowercase state factory functions such as `authState()` and `productEditorState()`.
- Revisit only if many more samples prove that this factory pattern becomes a repeated, meaningful burden.

## [2026-05-11] Add AfsmGraphReducer for feature-boundary graphable machines

Decision: Add `AfsmGraphReducer<S, E, C, F>` as the state-based graphable reducer boundary and make `AfsmMachine<P, X, E, C, F>` extend it.

Rationale:

- Feature code already names `State = AfsmState<Phase, Context>`, so forcing every feature-boundary machine alias to repeat both `Phase` and `Context` is unnecessary noise.
- `AfsmGraphReducer<State, Event, Command, Effect>` preserves the important runtime contract while hiding the lower-level phase/context split at the boundary.
- The interface is a small composition of existing responsibilities: reducer behavior, graph metadata, and initial state.
- Stateless sample machines can be singleton `object`s because dependencies live in command handlers, not in machines.

Consequences:

- Auth/ProductEditor now use `AfsmGraphReducer<FeatureState, FeatureEvent, FeatureCommand, FeatureEffect>` aliases.
- `AuthStateMachine` and `ProductEditorStateMachine` are singleton objects delegated to their DSL-built machines.
- ViewModels can use `StateMachine.initialState` instead of separate initial-state factory calls.
- `AfsmMachine` remains the DSL-built phase/context implementation type; `AfsmGraphReducer` is the feature-boundary type.

## [2026-05-11] Prioritize public API usability hardening

Decision: Present `afsmMachine { ... }` as the primary public path for complex graphable Android flows, and keep low-level reducer and graph metadata APIs as advanced/reference concepts.

Rationale:

- A five-perspective review found that the current API direction is sound, but first-time users see too many concepts if README introduces reducer, machine, graph reducer, topology, KSP, phase, context, command, and effect at once.
- The simplest useful story is `Phase + Context + Event + Command` hosted by `ViewModel.afsmHost(machine = ...)`.
- `AfsmGraphReducer` remains useful as a feature-boundary alias, but it should not be the first concept in onboarding.

Consequences:

- README now starts with the graphable DSL happy path instead of a low-level reducer example.
- `afsm-viewmodel` adds `ViewModel.afsmHost(machine = ...)` so standard machines can be hosted without repeating `initialState` and `reducer`.
- Public docs now describe `AfsmReducer` as an advanced/custom-state escape hatch.

## [2026-05-11] Harden runtime defaults for public use

Decision: Make invalid transitions fail fast by default, bound the default event queue, add `tryDispatch(event)`, and run sequential command handling outside the event reduction loop.

Rationale:

- Silent invalid transitions with `Record` plus no logger can hide flow bugs during development.
- A non-suspending public dispatch API should not be backed by an unbounded event queue by default.
- Android command handlers often suspend on repository, database, or timer work; those commands should not prevent later UI events from being reduced.

Consequences:

- `AfsmConfig.invalidTransitionPolicy` now defaults to `Throw`.
- `AfsmConfig.eventQueueCapacity` defaults to `64`.
- `AfsmHost.tryDispatch(event)` returns whether an event was accepted.
- `AfsmCommandExecutionPolicy.Sequential` now means commands execute one at a time in emission order, but through a separate command processor.

## [2026-05-11] Position Afsm as a complex flow toolkit

Decision: Continue with the executable DSL direction, but position Afsm as a toolkit for complex Android transaction/flow screens rather than a general replacement for ordinary ViewModel state management.

Rationale:

- Ten Android-developer POC reviews rated the direction as adoptable for complex flows, but too ceremonial for simple loading/content/error or data-observation screens.
- Reviewers consistently found `afsmMachine { ... }` and `ViewModel.afsmHost(machine = ...)` useful for ProductEditor, signup/identity, checkout/payment, and retry flows.
- Reviewers also consistently found first-contact complexity too high when graph/KSP, `AfsmGraphReducer`, `AfsmNoEffect`, and topology concepts appear before a minimal machine example.

Consequences:

- Public docs should use a minimal-first onboarding order: install, minimal machine, ViewModel hosting, testing, then optional MMD graph generation.
- Product/sample guidance should explicitly say where not to use Afsm.
- Before public release, resolve dynamic initial state hosting, stale command result/cancellation guidance, command queue/backpressure policy, Compose effect collection, and external MMD generation UX.
- `AfsmGraphReducer` should be treated as a naming/API spike candidate before stable public API freeze.

## [2026-05-11] Replace AfsmGraphReducer with AfsmMachine boundary

Decision: Remove the pre-release `AfsmGraphReducer` name and use `AfsmMachine<S, E, C, F>` as the graphable feature-boundary API. Rename the DSL-built phase/context implementation type to `AfsmPhaseMachine<P, X, E, C, F>`.

Rationale:

- The 10-agent POC review found `AfsmGraphReducer` accurate but awkward for first-contact Android developers.
- Users think in terms of "this screen has a state machine"; `AfsmMachine<State, Event, Command, Effect>` matches that language better than a graph/reducer compound name.
- The lower-level reducer escape hatch still exists as `AfsmReducer<S, E, C, F>`, so the new `AfsmMachine` name can own graphability, initial state, and topology metadata.
- Keeping the DSL-built object as `AfsmPhaseMachine` makes the phase/context specialization explicit without forcing every feature-boundary typealias to expose five generic parameters.

Consequences:

- Public docs now introduce `AfsmMachine<State, Event, Command, Effect>` for graphable machines and reserve `AfsmReducer` for custom non-graphable reducers.
- `afsmMachine { ... }` returns `AfsmPhaseMachine<Phase, Context, Event, Command, Effect>`, which is normally hidden behind a feature-local `AfsmMachine<State, Event, Command, Effect>` alias.
- `AfsmGraphReducer` is treated as removed pre-release history and should not be used in new docs or source.

## [2026-05-11] Complete first usability hardening pass

Decision: Close the P0 usability review items by adding small, focused helpers and documentation rather than a larger framework layer.

Rationale:

- The project needs lower first-contact complexity, not another abstraction tier.
- Repeated sample code showed that Compose effect collection and dynamic initial state hosting are common enough to justify small public helpers.
- Command backpressure and stale result handling should be explicit and testable before public release.
- MMD output should default to a readable flow diagram, while retaining a full topology option for debugging.

Consequences:

- `afsm-compose` now provides `CollectAfsmEffects(...)`.
- `afsm-viewmodel` now supports `afsmHost(machine = ..., initialState = ...)`.
- `AfsmConfig.commandQueueCapacity` bounds command queue growth.
- Checkout demonstrates request-id-based stale command result handling.
- `AfsmMmdOptions.Flow` is the default graph writer mode; `Full` remains available.
- README, API docs, sample guide, testing guide, release readiness, and consumer-smoke docs are aligned to the minimal-first onboarding path.

## [2026-05-14] Internal beta only after follow-up CTO review

Decision: Keep Afsm moving forward, but treat it as internal beta only until the second hardening sequence is complete.

Rationale:

- The Android developer reviews found the core product idea compelling for complex flows, especially ProductEditor-like transaction screens.
- The same reviews found stable release blockers in public ABI, runtime pressure behavior, restoration/effect policy, graph tooling safety, sample persuasiveness, and OSS packaging.
- Freezing the API before removing internal DSL leaks would make future cleanup unnecessarily expensive.

Consequences:

- `AfsmPhaseMachine` stays out of onboarding; feature code should expose `AfsmMachine<State, Event, Command, Effect>`.
- `AfsmReducer` remains a custom non-graphable escape hatch.
- Effects remain best-effort; durable flows must use state plus acknowledgement or state plus optional effect.
- The next release gate prioritizes API dump cleanliness, runtime pressure tests, restoration/effect/command policy, graph tooling hardening, and sample repair before OSS release identity work.

## [2026-05-14] Command queue overflow fails fast

Decision: A full host command queue throws `AfsmCommandQueueOverflowException` instead of suspending event processing.

Rationale:

- Android UI event dispatch should not appear frozen because a machine emitted too many commands.
- A full command queue is usually a modeling or capacity problem, not a recoverable domain failure.
- Explicit failure is easier to test and diagnose than coroutine backpressure across event and command processors.

Consequences:

- `AfsmConfig.commandQueueCapacity` remains bounded and defaults to `64`.
- Machines should emit fewer, coarser commands instead of large bursts of tiny commands.
- Domain failures must still be represented as typed events from command handlers, not as queue overflow.

## [2026-05-14] Use an example ladder for public usability

Decision: Present Afsm through a staged example ladder: minimal Draft, Auth, Checkout, ProductEditor, and non-Afsm data screens.

Rationale:

- Android developers need a fast path from smallest API shape to a persuasive complex-flow example.
- Checkout is the strongest mid-size example because it covers navigation argument initial state, loading, payment retry, request ids, durable completion, optional effect delivery, and render-state mapping.
- ProductEditor remains the complex transaction reference, but it is too large to be the first persuasive sample.
- Anti-examples are important: Afsm should not look like a general ViewModel replacement.

Consequences:

- `docs/examples.md` is the public sample map.
- `docs/auth-walkthrough.md`, `docs/checkout-walkthrough.md`, and `docs/product-editor-walkthrough.md` are the primary walkthrough set.
- Checkout must remain graphable and generated as `CheckoutStateMachine.mmd`.

## [2026-05-16] Keep GitHub CI aligned with the local release gate

Decision: GitHub CI should run `./scripts/verify-release-local.sh --warning-mode all` instead of maintaining a separate CI-only command list.

Rationale:

- Afsm's release confidence depends on tests, sample graph generation, API checks, Maven Local publication, and external consumer smoke together.
- A separate CI task list would drift from the local release gate and weaken the meaning of the README CI badge.
- Passing Gradle arguments through the verification scripts lets local and CI runs use the same warning policy.

Consequences:

- `.github/workflows/ci.yml` is the private-repo CI entrypoint.
- `scripts/verify-release-local.sh` and `scripts/verify-consumer-smoke.sh` forward extra Gradle arguments.
- README can advertise the CI badge as the same gate developers should run locally.

## [2026-05-19] Optimize first-use onboarding before adding larger abstractions

Decision: Improve Afsm usability through smaller onboarding and sample-boundary
cleanup before introducing another core abstraction.

Rationale:

- Six Android-developer reviewers found the first README path too close to
  maintainer verification and too far from copy-paste adoption.
- The no-block `state(phase)` overload removes visible terminal-state
  boilerplate while preserving the existing `state` concept.
- Render-state mapping is the right sample boundary: machines own flow state,
  while Compose screens should receive ordinary UI state.
- Graph generation needs better documentation now and a dedicated Gradle plugin
  later; adding more DSL concepts would not solve that adoption blocker.

Consequences:

- README now presents install -> minimal machine -> ViewModel -> test -> graph
  generation.
- `state(phase)` is part of the pre-release public API and API dump.
- Auth and Checkout samples use explicit render-state output for UI boundaries.
- ProductEditor docs explain `onExit -> transition block -> onEnter`.
- Internal beta pilots require an owner, target screen, success criteria, stop
  criteria, and upgrade verification command.

## [2026-05-19] Ship first graph Gradle plugin slice

Decision: Add `io.github.afsm.graph` as the first Gradle plugin slice for
Android app/library modules, replacing hand-written app export tests.

Rationale:

- Six-agent usability review found that graph generation still felt like a
  documented workaround because apps had to maintain an export test and Gradle
  task manually.
- The existing KSP registry already proves code-synced graph extraction; the
  next ergonomic problem is build wiring, not a new graph engine.
- Running the writer from a generated unit test keeps access to the module-local
  internal `AfsmGeneratedGraphRegistry` without introducing reflection or a
  separate JavaExec classpath problem.

Consequences:

- App modules apply `com.google.devtools.ksp` and `io.github.afsm.graph`.
- The plugin generates `AfsmGeneratedMmdExportTest`, configures the selected
  Android unit-test variant, and registers `generateAfsmMmd`.
- `sample-shop` no longer owns a hand-written graph export test.
- `consumer-smoke` applies the published plugin from Maven Local and verifies
  `.mmd` generation from external coordinates.
- Multi-variant output, multi-module aggregation, and processor compile-testing
  remain future hardening work.

## [2026-05-19] Keep complex sample UI behind render state

Decision: ProductEditor should map internal `ProductEditorState` to
`ProductEditorRenderState` before Compose rendering.

Rationale:

- ProductEditor is the most persuasive complex-flow sample, so it should show
  the intended Android boundary clearly.
- Reviewers found it inconsistent that Auth and Checkout hid phase details from
  Compose while ProductEditor still branched on `ProductEditorPhase` directly.
- The FSM phase model should be free to evolve for graph/transition correctness
  without forcing every UI layout branch to know internal phase classes.

Consequences:

- `ProductEditorScreen` now receives `ProductEditorRenderState`.
- UI actions are represented as small primary/secondary action enums and mapped
  to events at the screen boundary.
- State-machine tests now include a render-state mapping assertion for the
  rejected review state.

## [2026-05-19] Harden graph tooling through executable fixtures

Decision: Treat graph generation trust as an executable contract covered by KSP
functional tests and Gradle plugin TestKit fixtures, not only by sample app
happy paths.

Rationale:

- Six Android-developer reviewers found that external adoption risk now sits in
  incorrect annotation use, generated registry drift, Gradle classloader issues,
  and graph export task behavior.
- KSP validation is user-facing because it determines whether `@AfsmGraph`
  mistakes fail at compile time with useful messages.
- The Gradle plugin must not change ordinary Android unit-test behavior just to
  support graph export.

Consequences:

- `afsm-graph-ksp:test` now runs real Kotlin/KSP fixture builds for valid and
  invalid graph sources.
- `afsm-graph-gradle-plugin:test` now runs Android app/library fixture builds
  through Gradle TestKit.
- `generateAfsmMmd` loads the generated registry reflectively and fails clearly
  only when the graph export task is run without graph sources.
- Normal Android unit-test tasks exclude the generated graph export test.

## [2026-05-20] Synchronize graph plugin and processor versions

Decision: Generate the graph plugin's default `afsm-graph-ksp` dependency from
the shared Afsm version, and pass that same version into `consumer-smoke`.

Rationale:

- Six Android-developer reviewers identified stale Maven Local artifacts as a
  high-risk false-positive path after a version bump.
- External consumers should apply one `io.github.afsm.graph` plugin version and
  get the matching KSP processor by default.
- The release gate should verify the just-published local artifacts, not a
  previous snapshot that still happens to exist in Maven Local.

Consequences:

- `gradle.properties` now owns `afsmVersion`.
- The root build and graph plugin build both read that version.
- The graph plugin packages a processed resource containing the matching
  processor coordinate.
- `verify-consumer-smoke.sh` passes the root version into the separate consumer
  build.

## [2026-05-20] Fail fast on command-result event overflow

Decision: Command result events use the bounded event queue and throw
`AfsmEventQueueOverflowException` if a full queue rejects them. If the host is
already closed, command result events are dropped and logged as lifecycle
completion.

Rationale:

- Raw `eventQueue.send(nextEvent)` could suspend the single sequential command
  processor when the event queue was full.
- Event pressure is a runtime/modeling failure, not a domain failure that should
  be hidden behind a command result.
- This matches the existing fail-fast command queue overflow policy.

Consequences:

- Command handlers still dispatch typed events through the same callback.
- External `tryDispatch` remains available for UI callers that want a boolean.
- Runtime pressure docs now cover external events, command-result events, and
  accepted command queue overflow separately.
- ViewModel clearing while command work finishes is not treated as event
  pressure.

## [2026-05-21] Make the executable DSL case-oriented

Decision: Keep the public executable DSL as the primary authoring model, but
shift beginner-facing event handlers from `stay`/`otherwise` and
`transitionTo { updateData(...) }` toward named `case(...)` blocks plus
direct `updateData` actions.

Rationale:

- User feedback confirmed that the public DSL direction is acceptable, but the
  current branch vocabulary feels harder than Kotlin/Android developers expect.
- `transitionTo` should mean one thing: change the finite phase.
- A no-transition event does not need a `stay` verb; if no phase transition is
  declared, the current phase remains active.
- Named cases make conditions visible in both source code and generated `.mmd`
  graphs, avoiding anonymous predicates such as `data.draft.isValid()` that
  are hard for new users to understand.

Consequences:

- Public examples should prefer `case(label, condition = ...) { ... }`.
- Data changes should be separate actions through `updateData`, using the
  `updateData { data, event -> ... }` overload when the typed event
  payload is needed.
- `stay(...)` and `otherwise(...)` are no longer recommended for onboarding and
  should be considered removal/deprecation candidates before public API freeze.
- Graph generation still comes from the executable DSL because each case records
  its transition target and labels at build time.

## [2026-05-21] Use condition terminology in public topology

Decision: Rename public topology branch metadata from `guardLabel` to
`conditionLabel`, and keep public DSL parameters as `condition`.

Rationale:

- Android developers first meet the API through `case(label, condition = ...)`;
  generated topology should use the same word.
- `guard` is valid statechart vocabulary, but it adds one more concept for
  beginners while the DSL already exposes ordinary Kotlin boolean conditions.
- This is pre-release public API, and the user explicitly allowed breaking
  changes to improve usability.

Consequences:

- `AfsmTopologyTransition.conditionLabel` is the current public field.
- `ignore` and `invalid` use `condition = { ... }` for optional matching.
- Historical wiki notes may still mention guard vocabulary as superseded
  design history, but current public docs and examples should not.

## [2026-05-21] Prefer explicit negative conditions over fallback-style validation cases

Decision: In onboarding samples, validation failure branches should use named
`case(label = "...", condition = ...)` blocks instead of a final unconditional
case that acts like `otherwise`.

Rationale:

- The user specifically called out that hidden predicates such as
  `data.draft.isValid()` are hard to understand; a branch should state both
  its business label and its matching condition.
- Removing `otherwise` is less valuable if examples recreate it as
  `case(label = "invalid ...")` with no condition.
- Explicit success and failure predicates make the code easier to review against
  the generated state diagram.

Consequences:

- ProductEditor submit/resubmit and Auth submit examples now show both valid
  and invalid cases with explicit conditions.
- Truly unconditional event handling still uses direct helpers such as
  `transitionTo(...)`, `updateData(...)`, or `effect(...)`.

## [2026-05-21] Align payload phase factory order with DSL reading order

Decision: A payload phase factory declared by `transitionTo<PayloadPhase> { ... }`
runs after source `onExit` and accepted case actions, and before target
`onEnter`.

Rationale:

- Android developers read a `case` block top to bottom. If `updateData` appears
  before `transitionTo<PayloadPhase> { data... }`, the payload phase factory
  should observe that updated data.
- Running the target factory before case actions made Checkout duplicate request
  id calculations and created a hidden execution-order concept.
- The documented execution order now matches runtime behavior:
  `onExit -> case actions -> target phase factory -> onEnter`.

Consequences:

- Payload phase factories may use data updates made in `onExit` or earlier
  case actions.
- Target `onEnter` observes both the updated data and the created payload
  phase.
- Flow `.mmd` output includes named no-transition condition branches so
  validation and missing-data cases remain visible in generated diagrams.

## [2026-05-21] Expose Flow and Full graph modes through the Gradle plugin

Decision: `generateAfsmMmd` should support both review-friendly `Flow` output
and complete `Full` topology output through `afsmGraph.mmdOptions` and the
`-PafsmMmdOptions` Gradle property.

Rationale:

- The core API already exposes `AfsmMmdOptions.Flow` and `AfsmMmdOptions.Full`.
- Android developers use the Gradle task, not a hand-written export test, so
  the official entry point must expose the same choice.
- Review diagrams and audit/debug diagrams have different needs.

Consequences:

- Default `generateAfsmMmd` remains `Flow`.
- `./gradlew :app:generateAfsmMmd -PafsmMmdOptions=Full` generates complete
  topology without editing build files.
- `afsmGraph { mmdOptions.set("Full") }` can make Full output the module
  default.

## [2026-05-23] Keep entry and exit output labels with the runtime output declaration

Decision: Public DSL entry/exit actions should no longer accept separate
`commandLabels` or `effectLabels` parameters. Entry/exit outputs should be
declared as `command(label = "...") { ... }` or `effect(label = "...") { ... }`.

Rationale:

- Android developers should not have to write one statement for the state
  diagram and another nearby statement for runtime behavior.
- Separate metadata parameters create drift risk: a diagram can claim a command
  exists while the handler emits a different command or no command at all.
- The case DSL already uses `command(label = ...) { ... }`; entry/exit actions
  should follow the same shape.

Consequences:

- Generated entry/exit command/effect notes still exist, but labels come from
  the same command/effect declaration that emits the runtime output.
- `onEnter { command(label = "LoadProduct") { ... } }` is the recommended
  onboarding style.
- `Afsm.stay(...)` is removed from the beginner-facing helper object; graphable
  DSL code stays in the current phase by handling an event without
  `transitionTo(...)`. Low-level reducers can use `AfsmTransition.handled(...)`.

## [2026-05-23] Rename first-use DSL vocabulary to phase/data/handled

Decision: Apply a breaking first-use terminology cleanup before public API
freeze: DSL scopes are `phase(...)`, the standard extended state field is
`AfsmState.data`, the no-phase-change low-level decision is `Handled`, and the
DSL builder returns ordinary `AfsmMachine<AfsmState<Phase, Data>, Event,
Command, Effect>` without a public `AfsmPhaseMachine` type.

Rationale:

- Six first-time Android developer reviews converged on the same issue: users
  were not blocked by runtime behavior, but by API words that overloaded Android
  mental models.
- `state(...)` sounded like full UI state, while the DSL scope is actually the
  finite graph node. `phase(...)` says that more directly.
- `context` collided with `android.content.Context`; `data` reads as ordinary
  immutable screen data carried across phases.
- `stay`/`Stayed` made users think they needed to explicitly say "do not
  transition" for every handled event. In the DSL, omitting `transitionTo(...)`
  is the natural no-phase-change form.

Consequences:

- README and public docs now start with `docs/getting-started.md` and the
  `Phase + Data + Event + Command` model.
- `AfsmPhaseMachine` is removed from the public source surface; users see one
  graphable machine type at feature boundaries.
- API dumps must be regenerated because the change intentionally breaks the
  pre-release public surface.

## [2026-05-23] Make first-use DSL predicates read-only and Draft-first

Decision: Keep the public DSL as the primary authoring model, but separate
read-only decision scopes from mutating transition scopes and teach the API
through a minimal Draft flow before Checkout or ProductEditor.

Rationale:

- A second six-agent first-use review found that remaining confusion came from
  too much domain complexity too early, plus receiver scopes that implied
  predicates and payload phase factories could mutate transition output.
- `case(condition = ...)` should behave like a graphable `if` branch. It should
  inspect typed `phase`, typed `event`, and current `data`, not change the
  machine.
- `transitionTo<PayloadPhase> { ... }` should create only the target phase.
  Data updates, commands, and effects remain separate statements so
  `transitionTo` keeps one meaning: phase change.
- Checkout should demonstrate production policies, but the first learning
  example should be a small Draft machine.

Consequences:

- `AfsmConditionScope` and `AfsmPhaseFactoryScope` are public read-only DSL
  receiver types.
- The `afsm-core` API dump changes intentionally before public release.
- `docs/getting-started.md` is Draft-first; Checkout is the mid-size production
  sample; ProductEditor is the advanced graph stress test.
- `ignore(...)` remains available only for expected harmless no-ops. Omitted
  event handlers remain invalid by default.

## [2026-05-25] Add afsm-test as a test-only helper artifact

Decision: Add a dedicated `afsm-test` JVM artifact for transition assertion
helpers.

Rationale:

- First-time Android developers repeatedly start transition tests by inspecting
  `result.decision`, `result.state.phase`, `result.commands`, and
  `result.effects` directly.
- Those properties are the correct low-level model, but they make the first test
  read like Afsm internals instead of behavior.
- A test-only artifact can improve test readability without adding runtime,
  Android, or Compose coupling.

Consequences:

- `afsm-test` depends on `afsm-core` and Kotlin test APIs only.
- The first helpers stay focused on assertion readability:
  decision assertions, full state, `AfsmState` phase/data, commands, effects,
  and no-output checks.
- The module is published to Maven Local and covered by API validation and
  consumer-smoke before broader sample test migration.

## [2026-05-26] Disable hosted CI for cost control

Decision: Remove the GitHub Actions CI workflow and rely on local verification
before merges.

Rationale:

- The stacked PR merge flow triggered many hosted GitHub Actions runs at once.
- The immediate project priority is avoiding hosted-runner spend.
- The existing local release gate remains available through
  `scripts/verify-release-local.sh` when full verification is needed.

Consequences:

- PRs and pushes should not start the previous remote `Verify release gate`
  workflow.
- Maintainers should run the relevant local checks before merge.
- Broad release readiness should still use the local release gate or a
  deliberately re-enabled CI workflow when cost policy changes.

## [2026-07-10] Route all Codex project work through the LLM Wiki

Decision: Codex must use the LLM Wiki as the first retrieval and intent layer
for every project task, including basic project questions and edits, then verify
current implementation claims against code, tests, Git state, and command
output.

Rationale:

- The previous `AGENTS.md` required wiki reading mainly for architecture and
  Android/FSM topics, leaving ordinary project reading and modification without
  a consistent entry path.
- Starting from `wiki/index.md`, current state, and a task-specific canonical
  page avoids rediscovering decisions from raw evidence or broad code search.
- A change-type synchronization matrix makes it explicit when to update raw
  evidence, canonical synthesis, current state, decisions, implementation logs,
  QA pages, the index, and the chronological log.

Consequences:

- `AGENTS.md` now requires the common LLM Wiki reading path before broad search,
  project-level answers, or edits.
- `wiki/07-llm/codex-project-workflow.md` is the canonical Codex task-routing and
  synchronization guide.
- Purely mechanical edits still avoid wiki churn when they add no durable
  behavior, decision, evidence, status change, or reusable learning.

## [2026-07-10] Make explicit flow readability the primary product goal

Decision: Afsm's primary product goal is to turn implicit business-flow state
changes scattered across complex Android ViewModels into explicit `Phase` and
`Event` transition rules that can be read, tested, graphed, and verified from
the same executable machine definition.

Rationale:

- The user problem is not Kotlin `copy()` itself; it is the loss of locality and
  traceability when valid events, state changes, async work, and resulting UI
  behavior are distributed across ViewModel methods and UI collectors.
- Framing Afsm as state storage abstraction or a `copy()` replacement would
  encourage ceremonial adoption and hide the actual product value.
- One executable definition already connects the runtime machine, pure
  transition tests, and generated topology, making flow readability a concrete
  and verifiable promise.

Consequences:

- Product and README language should lead with explicit flow rather than the
  generic phrase "Android FSM toolkit."
- The current vocabulary remains `Phase`, `Data`, `Event`, `Command`, and
  optional `Effect`; host work must not be conflated with UI effects.
- Android `ViewModel` remains the lifecycle/UI adapter, and simple screens stay
  on ordinary `ViewModel + StateFlow` when an FSM would add ceremony.
- Success criteria should test whether the machine, transition tests, and graph
  make a complex flow understandable without reconstructing it from UI and
  ViewModel files.

## [2026-07-10] Operate Afsm as an outcome-based long-term goal

Decision: Afsm development should run as a continuous evidence-driven goal
covering product value, one executable definition, Android production fit,
public API stability, developer experience, real adoption evidence, release
readiness, and durable project knowledge.

Rationale:

- A single Checkout test pass or API slice is a milestone, not proof that the
  product has achieved its purpose.
- Feature-count and green-build completion criteria reward short-term output but
  can leave product value, adoption, lifecycle risk, and public support unclear.
- Outcome-based completion allows Codex to select bounded milestones while
  preserving the same product direction across many cycles.

Consequences:

- `wiki/06-project/long-term-goal.md` is the canonical completion and loop
  contract.
- Every cycle audits current evidence, selects the largest safe gap, implements
  and verifies one milestone, synchronizes durable knowledge, and re-audits.
- AI review and sample verification remain supporting evidence, not substitutes
  for a real or production-like pilot.
- Push, merge, deployment, publication, and public release remain separately
  authorized actions.

## [2026-07-10] Let usability outrank pre-release API compatibility

Decision: Because Afsm has not been publicly released, the current API, DSL,
terminology, module boundaries, samples, tests, and tooling are product
hypotheses rather than compatibility commitments. They may be broken or
rewritten when evidence supports a more readable, useful, safe, or Android-
idiomatic design.

This decision supersedes any interpretation of the earlier long-term-goal
decision that treated public API stability or preservation of the current
surface as a present objective.

Rationale:

- Stabilizing an unproven API can preserve accidental complexity and turn
  implementation investment into a false product constraint.
- The final goal is not to release the current design; it is to make Afsm
  genuinely useful to Android teams working on complex flows.
- Existing tests protect accepted behavior, but they must not freeze a product
  model that the user has intentionally replaced after usability review.

Consequences:

- Product/spec direction changes first, then tests, implementation, samples,
  graphs, docs, API dumps, and consumer fixtures move together.
- Compatibility aliases are not added by default during pre-release redesign.
- `Phase`, `Data`, `Event`, `Command`, and `Effect` remain current vocabulary,
  not mandatory final concepts.
- API freeze, ABI compatibility, and release preparation begin only after
  usability, safety, Android fit, and real-pilot evidence justify stabilization.

## [2026-07-10] Advance direct graphable properties after declaration prototypes

Decision: Reject the exact partial-generic function and inferred generic
feature-superclass declaration shapes. Keep their compiling staged/token/value
fallbacks as experiment evidence, but advance an explicitly typed top-level
`AfsmMachine` property with `@AfsmGraph` property discovery as the next
test-first hypothesis.

Rationale:

- Kotlin does not allow three explicit arguments on the proposed five-type
  function and does not infer five generic superclass arguments for an object.
- Staged type sets, named type tokens, and a feature container all compile and
  preserve behavior, but they replace existing ceremony with new framework
  concepts.
- The current class-only graph processor is what forces delegated singleton
  wrappers around an already executable `AfsmMachine` value.
- An explicit property type uses ordinary Kotlin expected-type inference and
  keeps the current ViewModel and transition-test boundaries unchanged.

Consequences:

- Candidate E must be specified and tested in KSP before any sample migration.
- No production API or canonical example changes in this prototype round.
- Draft, Auth, Checkout, graph generation, consumer smoke, diagnostics, and
  dynamic host initialization are required acceptance evidence.

## [2026-07-10] Use direct machine properties as the current authoring candidate

Decision: Adopt Candidate E for current pre-release examples and tooling:
graphable features expose one explicitly typed, stable top-level `val`
`AfsmMachine`, and `@AfsmGraph` annotates that property directly.

Rationale:

- It removes the machine alias, delegated singleton, and factory without adding
  type tokens, staged builders, a feature base class, reflection, or generated
  runtime APIs.
- KSP can validate and reference the real machine property directly.
- Draft, Auth, Checkout, ProductEditor, graph registry tests, and an external
  Maven Local consumer preserve behavior and graph output.
- Checkout dynamic initial state remains a ViewModel host concern, so the
  graphable machine identity stays stable.

Consequences:

- Public learning material should teach one state alias and one explicit
  machine property before the flow body.
- Eligible class/object graph sources remain supported for custom integration,
  but are not the normal feature declaration.
- This is not an API-freeze decision; fresh-user and real-pilot evidence may
  still replace the shape before release.

## [2026-07-10] Name direct machine properties as Kotlin values

Decision: Candidate E machine properties use lower camel case, for example
`draftStateMachine`, `authStateMachine`, and `checkoutStateMachine`.

Rationale:

- The declaration is now an ordinary top-level `val`, not a class or `object`.
- PascalCase preserves the visual shape of the removed singleton wrapper and
  makes readers misclassify the symbol before reaching `: AfsmMachine<...>`.
- Lower camel case keeps ViewModel hosting readable as value composition:
  `machine = checkoutStateMachine`.

Consequences:

- Sample, consumer, tests, public docs, and generated KSP property references
  move together before any API freeze.
- Graph ids/file names remain explicit and keep their existing PascalCase
  output names.

## [2026-07-10] Separate graph rules from genuine default state

Decision: `AfsmMachine<State, Event, Command, Effect>` owns transition behavior
and topology without promising an initial state. `AfsmDefaultMachine` extends it
only when a valid static default exists. Dynamic DSL declarations use
`afsmMachine(initialPhase = ...)`, and the concise ViewModel host overload
accepts only `AfsmDefaultMachine`.

Rationale:

- Checkout previously used `CheckoutData(productId = 0)` only to satisfy graph
  topology even though no product `0` is a valid Android navigation default.
- Documentation could not prevent accidental `afsmHost(machine)` use, while a
  subtype-restricted overload makes the mistake fail at compile time.
- The graph needs an initial phase, not invented durable data.
- Static Draft/Auth/ProductEditor flows retain their existing concise default
  path and body syntax.

Consequences:

- Checkout and future navigation/deep-link/restoration features expose base
  `AfsmMachine` and must pass a runtime state to `afsmHost`.
- Static features expose `AfsmDefaultMachine` and may use `machine.initialState`.
- API dumps, public docs, tests, graph fixtures, and external consumers change
  together before release; no compatibility shim is required.
- The type split remains pre-release and may change after fresh-user evidence.

## [2026-07-10] Measure first-use comprehension without coaching

Decision: Use a fixed Checkout machine/graph/tests participant task and a
separate facilitator-only rubric before claiming human readability evidence.

Rationale:

- Repository tests and AI reviews can prove coherence but cannot measure how a
  new Android developer reads the product.
- Showing the answer key, defining DSL terms, or changing the rubric after a
  result would turn the session into guided documentation review.
- Timing, misconceptions, artifact hops, and hesitation provide more actionable
  redesign evidence than a general preference question alone.

Consequences:

- The current 20-minute and 9/11 gates are explicitly assumptions until real
  data validates or replaces them; the rubric grew when restoration safety
  became part of Checkout.
- AI-assisted and unaided sessions are recorded separately.
- Raw answers and interventions are preserved before Wiki conclusions change.
- A comprehension pass is necessary but does not replace a production-like
  feature pilot.

## [2026-07-10] Restore Checkout with stable and unknown-payment keys

Decision: Keep restoration feature-owned and persist only Checkout product id,
completed order id, and pending payment request id. Restore an unresolved
payment to `PaymentStatusUnknown(requestId)` without automatic commands or
retry actions.

Rationale:

- Restoring `PaymentInProgress` would resurrect a command-shaped phase without
  its coroutine or a known backend result.
- Restoring every interrupted flow as `Idle` would hide payment uncertainty and
  make duplicate work easier.
- Full state serialization would persist repository-derived product data and
  couple Android storage to every machine field.
- A visible conservative phase is safer and more readable than claiming the
  local runtime can resolve remote payment status.

Consequences:

- Durable completion survives recreation without replaying its effect.
- Fresh `Idle` alone dispatches the startup event.
- `PaymentStatusUnknown` appears as a restoration-only graph state with no
  ordinary incoming edge.
- Production payment apps still need idempotency and status lookup.
- A public generic restoration helper remains unjustified until another real
  flow repeats enough of this feature-owned shape.

## [2026-07-11] Make diagnostics types-only by default

Decision: Replace raw top-level `AfsmDiagnostic` values with a safe envelope of
code, decision category, fixed message, simple type names, and Afsm-owned
metadata. Raw state/event/command/reason/throwable values are available only
through nullable `diagnostic.values` after explicit `IncludeValues` config.

Rationale:

- Auth commands contain email/password values and Draft commands contain user
  text, so raw diagnostic getters make accidental logging unsafe.
- `AfsmLogger.None` does not protect teams once they intentionally configure a
  Record policy and real logger.
- Type/category context remains useful for locating a flow boundary without
  serializing domain values.
- A named opt-in preserves local debugging flexibility while making the privacy
  decision visible in host configuration.

Consequences:

- Old raw top-level getters and the public diagnostic constructor are removed
  before release; no compatibility alias is retained.
- Invalid reasons and exception messages are hidden under the default policy.
- Enum instances intentionally collapse to their enum type name.
- A future custom safe-attribute mapper requires real pilot evidence.

## [2026-07-11] Prototype phase-owned command invocation

Decision: Prototype a bounded `onEnter { invoke(key, label) { command } }`
contract whose runtime job is automatically cancelled when the machine exits
the owning phase. Keep ordinary commands sequential and do not add a global
concurrent command policy.

Rationale:

- The documented `onExit` cancel-command pattern cannot interrupt the active
  command because the cancel waits in the same sequential queue.
- ViewModel-owned job registries duplicate lifecycle, failure, and result
  delivery policy in every feature.
- Global concurrency weakens ordering and bounded-pressure guarantees for
  unrelated short commands.
- A keyed phase-owned invocation makes lifetime visible in the machine and can
  be tested without Android dependencies.

Consequences:

- This is a breaking pre-release prototype, not an API freeze.
- `AfsmTransition` needs separate ordinary-command and invocation output.
- ProductEditor image upload is the first realistic proof.
- Request ids and idempotency remain necessary for remote/non-cooperative work.
- Full actor, hierarchy, restart, and service semantics remain deferred.

## [2026-07-11] Inject the ProductEditor upload boundary

Decision: Replace the hardcoded ViewModel upload delay/token with a
feature-owned `ProductImageUploader` suspend interface. Keep invocation
lifetime in `AfsmHost`, map normal uploader failures to typed machine events,
and rethrow cancellation.

Rationale:

- Cancelling `delay(250)` is weaker evidence than cancelling an injected
  repository/use-case boundary.
- A controllable fake removes timing races and can prove start/cancel/failure
  behavior directly.
- ProductRepository owns persisted product records and should not absorb image
  transfer concerns.
- A generic Afsm upload/service API would overreach the current evidence.

Consequences:

- ProductEditorRoute explicitly supplies the sample mock uploader.
- The mock uses a demo-only visibility delay; it is not runtime policy.
- Machine topology and public Afsm APIs remain unchanged.
- Real remote/SDK cancellation remains pilot evidence, not a repository claim.

## [2026-07-11] Pre-register production-like pilot evidence

Decision: Compare one isolated complex feature against its real pre-Afsm
baseline using fixed requirements, pre-registered success and stop criteria,
the same reviewer questions and safety scenarios, and a measured rollback
drill. Repository fixtures and AI reviews cannot populate the result.

Rationale:

- A sample can prove behavior but cannot establish adoption benefit, authoring
  friction, reviewer comprehension, or rollback feasibility for a real team.
- Post-hoc success criteria and intentionally weak comparators would make a
  pilot appear favorable without testing the product goal.
- Safety and Android integration need scenario evidence, not line-count claims.
- A failed or stopped pilot is valuable evidence when its baseline and reasons
  are preserved.

Consequences:

- A target, owner, reviewer, rollback owner, baseline commit, thresholds, and
  evidence permission must exist before implementation is counted as a pilot.
- The result is classified as keep, revise, reject-for-feature, or invalid-pilot
  only after immutable raw evidence exists.
- One successful pilot does not freeze the API; repeated external evidence is
  still needed for broad confidence.
