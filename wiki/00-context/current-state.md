---
title: Current State
updated: 2026-05-19
---

# Current State

This project is preparing to build an Android-specific FSM library.

The current direction is:

- Treat the final deliverable as a reusable Android library, not only an app-local convention.
- Keep Android `ViewModel`.
- Make complex screen flows explicit with plain Kotlin finite state machines.
- Use the `ViewModel` as the Android lifecycle and UI integration layer, not as the place where all transition rules live.
- Store durable architecture reasoning in this LLM Wiki so future Codex sessions can continue without rediscovering the same context.
- Use official Android UI layer, ViewModel, Compose state, coroutine, Flow testing, and saved state guidance as constraints for the FSM design.
- A minimal Kotlin/JVM `afsm-core` project now exists and compiles with the v2 core API shape.
- `AfsmNoEffect` and `AfsmTransition<S, C, F>` were validated through compile-time signup/login reference usage.
- A minimal coroutine-based `afsm-runtime` module now exists.
- `AfsmHost` serializes non-suspending `dispatch(event)` calls through a bounded FIFO event queue, exposes `StateFlow<S>` state, exposes best-effort `Flow<F>` effects, and executes commands sequentially on a separate command processor so suspended commands do not block later event reduction.
- Project-scoped AI engineering guardrails now require spec-first/TDD-oriented work and prohibit weakening tests merely to make implementation pass.
- A thin AndroidX `afsm-viewmodel` module now exists with `ViewModel.afsmHost(...)`, wiring `AfsmHost` to `viewModelScope`; graphable machines can use `afsmHost(machine = StateMachine, ...)`, while navigation-argument screens can use `afsmHost(machine = StateMachine, initialState = ...)`.
- A thin `afsm-compose` module now exists with `CollectAfsmEffects(...)` for lifecycle-aware one-shot effect collection in Compose routes.
- A `:sample-shop` Android app module now exists to validate Afsm in a realistic Compose + Room shopping app.
- The sample app uses Afsm for auth, product registration review/publish, and checkout retry flows, while keeping product list/detail/likes/reviews on ordinary ViewModel + Flow to avoid unnecessary FSM ceremony.
- Public sample documentation now lives in `docs/sample-shop-afsm-guide.md`.
- Auth now uses the executable DSL directly with `typealias AuthState = AfsmState<AuthPhase, AuthContext>`.
- Product registration is now the stronger FSM reference flow: draft editing, mock image upload, review rejection, resubmission, approval, publishing, and close effect.
- Android CLI smoke verification passed for signup and product registration, with layout/screenshot evidence under `raw/verification/2026-05-09-sample-shop-fsm-smoke/`.
- The canonical v3 API direction is now a scoped executable machine DSL: `state`, `on`, `transitionTo`, `stay`, `otherwise`, `updateContext`, `onEnter`, `onExit`, `command`, and `effect` in one machine definition.
- Afsm DSL public KDoc now explains phase/context/event/command/effect type parameters, runtime parameters, topology-only metadata parameters, guard behavior, branch ordering, and entry/exit execution order.
- `afsm-core` now distinguishes `AfsmReducer<S, E, C, F>` as the low-level host-facing contract, `AfsmMachine<S, E, C, F>` as the graphable feature-boundary machine, and `AfsmPhaseMachine<P, X, E, C, F>` as the DSL-built phase/context machine.
- The pre-release `AfsmGraphReducer` name was removed from the public API before release docs; new graphable code should use `AfsmMachine<State, Event, Command, Effect>`.
- Deprecated pre-release aliases and the temporary `AfsmMachineAdapter` base were removed from the public source surface. New graphable code uses `AfsmReducer`, `AfsmMachine`, `afsmMachine`, and `AfsmState`.
- The executable DSL spike passes ProductEditor-like core tests for phase transitions, context updates, `onExit -> transition -> onEnter` ordering, typed payload phases, guard fallback, DSL build validation, and UI-side effect emission.
- The executable DSL now exposes `AfsmPhaseMachine.topology` plus `AfsmTopology.toMmd()`; event branches are declared with graphable `transitionTo(...)`, `transitionTo<PayloadPhase>(phase = { ... })`, `stay(...)`, and `otherwise(...)`.
- `AfsmState<P, Context>` is now the standard phase/context state value. `AfsmPhaseMachine` directly implements `AfsmMachine<AfsmState<P, Context>, ...>`.
- Root `README.md` and `docs/afsm-public-api.md` now document only the current public API names.
- Maven local publishing now works for `afsm-core`, `afsm-runtime`, `afsm-viewmodel`, `afsm-compose`, `afsm-graph-ksp`, and the `io.github.afsm.graph` Gradle plugin using `io.github.afsm:*:0.1.0-SNAPSHOT` pre-release coordinates.
- `consumer-smoke` now exists as a separate Android Gradle build that consumes those Maven Local coordinates, compiles a ViewModel-hosted Afsm machine, applies the graph Gradle plugin, runs the KSP graph processor, and generates `.mmd` output without project-module dependencies.
- `docs/release-readiness.md` now defines the local release gate, remaining product decisions, engineering gates, and the known Kotlin Gradle plugin POM deprecation warning.
- Kotlin explicit API mode is enabled for `afsm-core`, `afsm-runtime`, `afsm-viewmodel`, `afsm-compose`, and `afsm-graph-ksp` so public declarations must be intentional.
- Binary API validation is enabled with API dumps for the five Afsm library modules; `sample-shop` is excluded because it is a sample app, not published API.
- `CHANGELOG.md` now contains the initial `0.1.0 - Unreleased` release notes and documents pre-release alias removal.
- `CONTRIBUTING.md` now captures the project development flow, test-integrity rules, public API change policy, and local release gate.
- `scripts/verify-release-local.sh` now runs the full local release gate, including tests, sample graph generation, `apiCheck`, Maven Local publication, and external consumer smoke.
- Maven Local generated POMs now have documented metadata audit status: packaging, internal dependency coordinates, names/descriptions are present; URL, license, SCM, and developer metadata remain product-owned decisions.
- The graph-generation slice now exists: annotated `StateMachine` classes implement `AfsmGraphSource`, `afsm-graph-ksp` generates `AfsmGeneratedGraphRegistry`, the `io.github.afsm.graph` Gradle plugin generates a JUnit4-compatible export test, and `generateAfsmMmd` writes one `.mmd` per registry entry without running the app's whole unit-test suite.
- Graph tooling verification now includes KSP functional tests for registry generation and invalid annotation diagnostics, plus Gradle TestKit functional tests for Android app/library plugin usage, KSP-missing failure messaging, ordinary unit-test separation, and no-registry `generateAfsmMmd` failure messaging.
- `AuthStateMachine`, `CheckoutStateMachine`, and `ProductEditorStateMachine` are annotated graph sources; `generateAfsmMmd` writes `AuthStateMachine.mmd`, `CheckoutStateMachine.mmd`, and `ProductEditorStateMachine.mmd`.
- The phased-state helper spike has been removed from `afsm-core`; it remains only as superseded design history.
- Afsm terminology now consistently treats `Command` as host-executed work emitted by the machine and executed by the host, not as another input event; v3 naming should distinguish phase states like `ImageUploadInProgress` from commands like `StartImageUpload`.
- ProductEditor now uses transition-action naming in code: `ImageUploadInProgress` with `StartImageUpload`, `ReviewSubmissionInProgress` with `StartReviewSubmission`, and `PublishInProgress` with `StartProductPublish`.
- ProductEditor now maps `ProductEditorState` to `ProductEditorRenderState` before Compose rendering, so the most complex sample no longer branches on internal `ProductEditorPhase` values in the UI layer; published render state hides draft fields and shows only completion-focused UI.
- Android CLI regression smoke verification passed after the ProductEditor naming cleanup, with evidence under `raw/verification/2026-05-09-product-editor-transition-action-rename-smoke/`.
- ProductEditor was briefly refactored to the phased-state helper as an intermediate spike: state was split into `ProductEditorPhase + ProductEditorContext`, `ProductDraft` lived in context, and reducers called `transitionTo(ProductEditorPhase.X)`.
- The failed intermediate idea of hiding `SavingDraft`/`DraftSaved` as context flags was rejected; meaningful flow states must remain phases so the state diagram stays visible.
- ProductEditor has now been migrated from the phased helper to the executable DSL while keeping `State = Phase + Context`; `ProductEditorStateMachine.topology` exposes `.mmd` graph metadata from the real sample implementation.
- ProductEditor submit/resubmit transitions now stay inline inside each event branch; only context transformations are helperized so the FSM flow remains readable in the machine body.
- ProductEditor validation failure is modeled as `otherwise` staying in the current phase with a context error, not as a second `transitionTo` competing with the success transition.
- Android CLI smoke verification passed after the ProductEditor executable DSL migration, with layout/screenshot evidence under `raw/verification/2026-05-09-product-editor-executable-dsl-smoke/`.
- ProductEditor now uses `typealias ProductEditorState = AfsmState<ProductEditorPhase, ProductEditorContext>` and delegates `ProductEditorStateMachine` directly to the DSL machine, removing the previous phase/context adapter mapping.
- Kotlin `typealias` cannot share a same-named factory with the aliased constructor, so ProductEditor uses a lowercase `productEditorState()` factory for default initial state construction.
- A shared `AfsmStateFactory` API was spiked and rejected for now; Kotlin singleton phase inference requires explicit `<Phase, Context>` arguments, so the small feature-local factory function remains clearer.
- Auth, ProductEditor, and consumer smoke now expose graphable feature state machines as singleton `object`s through `AfsmMachine<State, Event, Command, Effect>` aliases, avoiding repeated five-parameter `AfsmPhaseMachine<Phase, Context, Event, Command, Effect>` aliases at feature boundaries.
- A five-perspective public API usability review concluded that Afsm should be presented as an Android executable extended statechart DSL for complex flows, with `afsmMachine { ... }` as the primary onboarding path and `AfsmReducer`/graph metadata as advanced reference concepts.
- Runtime hardening now makes invalid transitions throw by default, adds `tryDispatch(event)`, bounds the default event and command queues to 64 items, and keeps command execution sequential without blocking event reduction.
- A reference architecture review compared Afsm against XState, SCXML, Tinder StateMachine, KStateMachine, Redux, Elm, Square Workflow, and Android guidance. The first hardening pass is now implemented: naming uses `AfsmReducer`/`AfsmMachine`, DSL output terminology is `command`, `onExit` exists, DSL build validation exists, topology metadata is richer, `AfsmHost` has a configurable command failure policy, and command cancellation remains explicit in feature commands/events.
- A ten-agent Android developer POC review concluded that the current executable DSL direction should continue, but Afsm must be positioned as a complex transaction/flow screen toolkit rather than a general ViewModel replacement.
- The first public usability hardening pass is now implemented: README is minimal-first, `afsm-compose` provides `CollectAfsmEffects`, `AfsmConfig.commandQueueCapacity` exists, `afsmHost(machine, initialState)` exists, Checkout demonstrates request-id stale result handling, and MMD output has initial nodes, entry/exit metadata notes, labels, and `AfsmMmdOptions.Flow`/`Full`.
- The remaining graph tooling release concerns are multi-variant/multi-module graph generation policy and eventual graph API/module-boundary decisions before broad external adoption.
- A 2026-05-14 ten-agent follow-up plus CTO review approved Afsm for internal beta only and blocked broad OSS/stable release until API/ABI, runtime pressure, restoration/effect policy, graph tooling, and OSS release identity are hardened.
- The first follow-up hardening loop is complete: internal DSL helper functions no longer appear in the public API dump, `AfsmTransition` is factory-based, `AfsmDslMarker` exists, graphable `machine + initialState` is separated from custom `reducer + initialState`, Checkout guards completed payment, `.mmd` output paths are validated, and `docs/modeling-rules.md` documents modeling choices.
- Runtime pressure hardening now fails fast with `AfsmCommandQueueOverflowException` when accepted commands exceed the bounded command queue, instead of suspending the event processor indefinitely. Tests also lock down that default effects are not replayed to late collectors.
- `docs/restoration-effect-command-policy.md` now documents the Android-facing rules for restoreable state, `onEnter`, effect durability, state-plus-acknowledgement UI work, command result events, request ids, explicit cancellation, and queue pressure.
- Checkout is now a graphable phase/context `AfsmMachine` sample instead of a custom reducer escape hatch. It demonstrates dynamic initial state from navigation `productId`, product loading, payment retry, request-id stale result handling, durable completion state, optional navigation effect, render-state mapping, and generated `CheckoutStateMachine.mmd`.
- Public example onboarding is now organized as a ladder: README minimal Draft, Auth, Checkout, ProductEditor, plus ordinary non-Afsm data screens as anti-examples. Public docs live in `docs/examples.md`, `docs/auth-walkthrough.md`, `docs/checkout-walkthrough.md`, and `docs/product-editor-walkthrough.md`.
- The project is now pushed to the private GitHub repository `kez-lab/afsm`. README has GitHub-facing status badges, a quickstart, and internal-beta positioning. `.github/workflows/ci.yml` runs the same local release gate on push, pull request, and manual dispatch.
- A 2026-05-19 six-agent usability loop simplified first-use onboarding, added terminal-state `state(phase)` convenience, moved Auth to a render-state UI boundary, made Checkout primary UI actions explicit, documented ProductEditor transition execution order, added `docs/graph-generation.md`, and clarified the internal beta adoption contract.
- The first graph Gradle plugin slice and its KSP/Gradle functional verification pass are implemented. The remaining graph-tooling concerns are plugin/processor version synchronization, multi-variant/multi-module aggregation, and eventual graph API/module-boundary decisions before broad external adoption.

## Core Architecture Position

The project should combine:

- MVVM's lifecycle fit and state ownership through `ViewModel`.
- UDF's one-way event/state flow.
- FSM's explicit state transition model.
- Plain Kotlin unit tests for transition behavior.

Primary engineering page: [[../03-engineering/android-fsm-architecture|Android FSM Architecture]].

Official Android constraints: [[../03-engineering/android-official-guidance|Android Official Guidance]].

Product strategy: [[../01-product/android-fsm-library-strategy|Android FSM Library Strategy]].

Delivery plan: [[../03-engineering/library-delivery-plan|Library Delivery Plan]].

First reference flow: [[../03-engineering/reference-flow-signup-identity-retry|Reference Flow - Signup Identity Retry]].

Public API draft: [[../03-engineering/afsm-public-api-draft|Afsm Public API Draft]].

Implementation-candidate API draft: [[../03-engineering/afsm-public-api-draft-v2|Afsm Public API Draft v2]].

v3 executable DSL direction: [[../03-engineering/afsm-v3-executable-dsl|Afsm v3 Executable DSL]].

KSP `.mmd` generation: [[../03-engineering/afsm-ksp-mmd-generation|Afsm KSP MMD Generation]].

Reference architecture review: [[../03-engineering/afsm-reference-architecture-review|Afsm Reference Architecture Review]].

Example catalog: [[../03-engineering/afsm-example-catalog|Afsm Example Catalog]].

10-agent CTO review: [[../08-meetings/2026-05-14-afsm-10-agent-cto-review|Afsm 10-Agent CTO Review]].

Phased core spike: [[../03-engineering/afsm-phased-core-spike|Afsm Phased Core Spike]].

Transition action terminology: [[../03-engineering/afsm-v3-terminology-transition-actions|Afsm v3 Terminology and Transition Actions]].

Core compile validation: [[../03-engineering/afsm-core-compile-validation|Afsm Core Compile Validation]].

Runtime dispatch validation: [[../03-engineering/afsm-runtime-dispatch-loop|Afsm Runtime Dispatch Loop]].

AI engineering guardrails: [[../07-llm/ai-engineering-guardrails|AI Engineering Guardrails]].

ViewModel integration validation: [[../03-engineering/afsm-viewmodel-integration|Afsm ViewModel Integration]].

Sample app validation: [[../03-engineering/sample-shop-reference-app|Sample Shop Reference App]].

## Current Source Material

- Raw discussion: [Android ViewModel FSM Discussion](../../raw/conversations/2026-05-01-android-viewmodel-fsm-discussion.md)
- Raw pattern note: [LLM Wiki Pattern](../../raw/sources/2026-05-01-llm-wiki-pattern.md)
- Official Android docs research: [Android Official Docs Research](../../raw/sources/2026-05-01-android-official-docs-fsm-research.md)
