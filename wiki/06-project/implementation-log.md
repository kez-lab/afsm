---
title: Implementation Log
updated: 2026-05-10
---

# Implementation Log

## [2026-05-03] afsm-core minimal Kotlin skeleton

Change:

- Added Gradle root project and wrapper.
- Added `afsm-core` Kotlin/JVM module.
- Implemented v2 core public types in package `afsm.core`.
- Added compile-check source for signup and login flows.

Verification:

```bash
./gradlew :afsm-core:compileTestKotlin --no-daemon
```

Result:

```text
BUILD SUCCESSFUL
```

Conclusion:

- `AfsmTransition<S, C, F>` compiles cleanly with screen-local typealiases.
- `AfsmNoEffect` compiles cleanly as a sealed no-effect marker.
- Runtime, ViewModel, and test helper modules are not implemented yet.

## [2026-05-09] afsm-runtime minimal dispatch loop

Change:

- Added `afsm-runtime` Kotlin/JVM module.
- Added coroutine runtime dependency.
- Implemented `AfsmHost`, `AfsmCommandHandler`, `AfsmConfig`, `AfsmEffectDelivery`, invalid transition policy, diagnostics, and logger.
- Added tests for FIFO dispatch, non-reentrant command-dispatched events, state/effect/command order, `Stayed`, `Ignored`, and `Invalid` behavior.

Verification:

```bash
./gradlew :afsm-runtime:test --no-daemon
./gradlew test --no-daemon
./gradlew test --warning-mode all --no-daemon
```

Result:

```text
BUILD SUCCESSFUL
```

Conclusion:

- `afsm-runtime` is viable as a low-coupling coroutine module.
- Android ViewModel integration should be a thin wrapper over `AfsmHost`, not part of runtime.

## [2026-05-09] afsm-viewmodel thin helper

Change:

- Added `afsm-viewmodel` Android library module.
- Added `ViewModel.afsmHost(...)`.
- Wired `AfsmHost` to AndroidX `viewModelScope`.
- Added ViewModel usage tests that model the developer-facing adapter code.
- Updated Gradle wrapper to `8.11.1` and added Android Gradle Plugin `8.10.1` for the Android library module.
- Enabled AndroidX through `gradle.properties`.

Verification:

```bash
./gradlew :afsm-viewmodel:testDebugUnitTest --no-daemon
./gradlew test --no-daemon
./gradlew test --warning-mode all --no-daemon
```

Result:

```text
BUILD SUCCESSFUL
```

Conclusion:

- The ViewModel integration can stay as a thin extension function.
- The natural ViewModel usage shape is `private val host = afsmHost(...)`, `val state = host.state`, `val effects = host.effects`, and `fun onEvent(event) = host.dispatch(event)`.

## [2026-05-09] sample-shop reference app

Change:

- Added `:sample-shop` Android application module.
- Added Compose Material 3, Navigation Compose, Lifecycle Compose, Room, and KSP dependencies.
- Added Room entities/DAOs/database for users, products, favorites, reviews, and orders.
- Added repository layer and manual `ShopAppContainer`.
- Added Afsm-backed auth flow.
- Added Afsm-backed checkout flow with mock payment failure and retry.
- Added ordinary ViewModel + Flow screens for catalog, product registration, product detail, likes, review registration, and review list.
- Added state machine tests for auth and checkout.
- Added `docs/sample-shop-afsm-guide.md`.
- Increased Gradle JVM heap for app module builds.

Verification:

```bash
./gradlew :sample-shop:testDebugUnitTest :sample-shop:assembleDebug --warning-mode all --no-daemon
./gradlew test :sample-shop:assembleDebug --warning-mode all --no-daemon
```

Result:

```text
BUILD SUCCESSFUL
```

Conclusion:

- `ViewModel.afsmHost(...)` remains readable in real Android ViewModels.
- Afsm is useful for auth and checkout retry flows.
- Ordinary ViewModel + Flow remains preferable for simple Room-backed data screens.

## [2026-05-09] sample-shop sealed FSM rewrite and Android smoke verification

Change:

- Rewrote auth from flat `AuthState` to sealed phases: `Editing`, `Submitting`, and `Authenticated`.
- Added `AuthForm` so text inputs are context data updated by self-transitions.
- Replaced product registration's ordinary ViewModel with an Afsm-backed state machine.
- Added product registration phases for draft saving, mock image upload, review rejection, resubmission, approval, publishing, and completion.
- Added `ProductEditorStateMachineTest`.
- Updated sample documentation and wiki pages.
- Added raw Android CLI verification evidence under `raw/verification/2026-05-09-sample-shop-fsm-smoke/`.

Verification:

```bash
./gradlew :sample-shop:testDebugUnitTest --no-daemon
./gradlew test :sample-shop:assembleDebug --warning-mode all --no-daemon
android run --device=emulator-5556 --apks="/Users/kwak-euijin/Documents/New project 2/sample-shop/build/outputs/apk/debug/sample-shop-debug.apk" --activity=.MainActivity
android layout --device=emulator-5556 --pretty --output=...
```

Result:

```text
BUILD SUCCESSFUL
Android CLI smoke journey PASSED
```

Conclusion:

- The reference app now better separates self-transitions from phase transitions.
- Product registration is the clearest current demonstration of Afsm value.

## [2026-05-09] v3 topology-first API design note

Change:

- Added `wiki/03-engineering/afsm-v3-topology-first-api.md`.
- Compared current v2 ProductEditor reducer implementation with a possible `transition<From, Event, To>` topology-first API.
- Documented graph generation implications and prototype plan.

Verification:

```bash
git diff --check
```

Conclusion:

- v2 should remain the low-level reducer-style engine.
- v3 should be explored as an optional graph-friendly authoring layer before any implementation commitment.

## [2026-05-09] v3 terminology and transition action design note

Change:

- Added `wiki/03-engineering/afsm-v3-terminology-transition-actions.md`.
- Clarified that `Command` is a transition action/output, not another event.
- Defined naming policy for `State`, `Event`, transition action, and `Effect`.
- Documented ProductEditor rename candidates such as `ImageUploadInProgress` plus `StartImageUpload`.

Verification:

```bash
git diff --check
```

Conclusion:

- Afsm can keep normal state-machine semantics while emitting host-executed actions during transitions.
- The next ProductEditor refactor should prove whether clearer names reduce the need for a DSL before graph generation work continues.

## [2026-05-09] ProductEditor transition action naming cleanup

Change:

- Renamed ProductEditor phase states from `UploadingImages`, `SubmittingForReview`, and `Publishing` to `ImageUploadInProgress`, `ReviewSubmissionInProgress`, and `PublishInProgress`.
- Renamed ProductEditor commands from `UploadImages`, `SubmitForReview`, and `PublishProduct` to `StartImageUpload`, `StartReviewSubmission`, and `StartProductPublish`.
- Updated ProductEditor tests and sample documentation.
- Added Android CLI regression evidence under `raw/verification/2026-05-09-product-editor-transition-action-rename-smoke/`.

Verification:

```bash
./gradlew :sample-shop:testDebugUnitTest --no-daemon
./gradlew test :sample-shop:assembleDebug --warning-mode all --no-daemon
android run --device=emulator-5556 --apks="/Users/kwak-euijin/Documents/New project 2/sample-shop/build/outputs/apk/debug/sample-shop-debug.apk" --activity=.MainActivity
android layout --device=emulator-5556 --pretty --output=...
android screen capture --output=...
```

Result:

```text
BUILD SUCCESSFUL
Android CLI smoke journey PASSED
```

Conclusion:

- The naming policy improves code readability without changing ProductEditor behavior.
- The plain Kotlin `when (state)` implementation remains understandable after the terminology cleanup.

## [2026-05-09] v3 topology-first API correction

Change:

- Revised `wiki/03-engineering/afsm-v3-topology-first-api.md` after CEO feedback that the pseudo API still looked like v2 result assembly.
- Replaced the recommended v3 shape from `transition<From, Event, To> { goTo(state, commands, effects) }` to a `from<FromState> { on<Event>().to<ToState>() }` topology companion.
- Kept runtime behavior in plain Kotlin typed receiver functions.

Verification:

```bash
git diff --check
```

Conclusion:

- This intermediate from-state-scoped direction was later superseded by the typed-handler convention, and then by the phased-state profile.
- Graph generation remains a topology metadata concern, while transition execution can remain readable Kotlin code.

## [2026-05-09] v3 typed-handler API correction

Change:

- Revised `wiki/03-engineering/afsm-v3-topology-first-api.md` again after CEO feedback that the previous `from/on/to` correction still introduced a DSL-like authoring surface.
- Documented the preferred v3 direction as plain Kotlin `when` plus concrete State/Event handler signatures.
- Documented that `FromState` and `Event` should come from handler parameters, while `ToState` should come from the `transitionTo` state argument or optional `transitionTo<ToState>`.

Verification:

```bash
git diff --check
```

Conclusion:

- v3 should not require a DSL as the main authoring style.
- This typed-handler direction was later superseded by the phased-state profile, where reducers call `transitionTo(Phase)` and entry policy hides context assembly.

## [2026-05-09] v3 canonical synthesis cleanup

Change:

- Rewrote `wiki/03-engineering/afsm-v3-topology-first-api.md` into a canonical current-direction page titled `Afsm v3 Typed Handler API`.
- Updated `wiki/03-engineering/afsm-v3-terminology-transition-actions.md` to point at the canonical v3 direction.
- Consolidated the accepted direction around plain Kotlin `when`, concrete State/Event handlers, and `transitionTo` next-state extraction.
- Moved rejected `transition<From, Event, To>`, `from/on/to`, and `goTo` ideas into a short `Superseded Ideas` section.
- Added canonical synthesis rules to `AGENTS.md` and `wiki/07-llm/wiki-maintenance-guide.md`.

Verification:

```bash
git diff --check
```

Conclusion:

- Future agents should read the v3 page as the current answer, not reconstruct the answer from previous corrections.
- Design corrections should update canonical synthesis pages directly before appending supporting history.
- The `Afsm v3 Typed Handler API` content was later superseded by `Afsm v3 Phased State API`, and then by `Afsm v3 Executable DSL`.

## [2026-05-09] afsm-core phased-state API spike

Change:

- Added `AfsmPhasedState`, `AfsmPhaseEntry`, `AfsmPhaseEntryPolicy`, and `AfsmPhasedTransitionScope` to `afsm-core`.
- Added `Afsm.phased(state, event, entryPolicy)` as the scope factory.
- Added scope functions for `transitionTo(Phase)`, `updateContext`, `stay`, `ignore`, and `invalid`.
- Added `AfsmPhasedCompileCheckTest` with a ProductEditor-like flow to validate compile ergonomics and behavior.
- Added `kotlin("test-junit5")` to `afsm-core` test dependencies so core API spikes can have executable tests instead of compile-only checks.

Verification:

```bash
./gradlew :afsm-core:compileTestKotlin
./gradlew :afsm-core:test
./gradlew :afsm-core:check :afsm-runtime:test
```

Conclusion:

- `transitionTo(Phase)` works when reducer code runs inside `AfsmPhasedTransitionScope`.
- `PhaseEntryPolicy` can hide context assembly and emit commands from the next context.
- `updateContext { ... }` requires a dedicated single-lambda overload for Kotlin trailing-lambda ergonomics.
- The next proof should apply the phased profile to the real ProductEditor sample before treating the API as stable.

## [2026-05-09] AfsmPhasedStateMachine helper spike

Change:

- Added `AfsmPhasedStateMachine<S, P, X, E, C, F>` to `afsm-core`.
- The helper implements `AfsmStateMachine` and creates `AfsmPhasedTransitionScope` internally.
- Refactored the ProductEditor-like compile check so feature code no longer calls `Afsm.phased(state, event, entryPolicy)` directly.
- Added a focused test that verifies the helper still exposes normal `AfsmStateMachine` behavior while hiding scope creation.

Verification:

```bash
./gradlew :afsm-core:compileTestKotlin
./gradlew :afsm-core:check :afsm-runtime:test
```

Conclusion:

- The desired shape is feasible: reducer code can call `transitionTo(Phase)` directly inside the helper-managed scope.
- Raw type verbosity still exists at the class declaration, so feature-local typealiases or future type reduction remain important.
- The helper introduces inheritance; this should be tested against a real ProductEditor refactor before deciding it is the public recommendation.

## [2026-05-09] ProductEditor phased-state helper spike

Change:

- Refactored real `sample-shop` ProductEditor from sealed state classes carrying `ProductDraft` into `ProductEditorState = ProductEditorPhase + ProductEditorContext`.
- Kept meaningful flow states as phases, including `SavingDraft` and `DraftSaved`.
- Moved `ProductDraft` and validation error data into `ProductEditorContext`.
- Updated reducers to call `transitionTo(ProductEditorPhase.X)` instead of assembling full state objects.
- Added `ProductEditorPhaseEntryPolicy` to update context and emit save/upload/review/publish commands on phase entry.
- Updated ProductEditor UI and tests to consume `state.phase` plus `state.context`.

Verification:

```bash
./gradlew :sample-shop:testDebugUnitTest --tests 'afsm.sample.shop.feature.editor.ProductEditorStateMachineTest'
```

Result:

```text
BUILD SUCCESSFUL
```

Conclusion:

- The helper can make ProductEditor reducer code read more like a state diagram.
- The failed intermediate `saveStatus` context flag shape was rejected because it hid flow states and degraded state-machine readability.
- The remaining API issue is how much `phase/context` boilerplate should be exposed to Android UI code and public documentation.

## [2026-05-09] v3 executable DSL planning

Change:

- Added `wiki/03-engineering/afsm-v3-executable-dsl.md` as the new canonical v3 direction.
- Marked the previous phased-state v3 page as superseded history.
- Updated terminology, current state, open questions, and decision log to reflect the shift from `when + PhaseEntryPolicy` to a scoped executable DSL.
- Added ProductEditor pseudo implementation showing `state`, `on`, `guard`, `updateContext`, `onEnter`, `action`, `effect`, and `transitionTo`.
- Added an implementation plan for a Kotlin compile spike, interpreter spike, graph exporter, ProductEditor migration, and public API naming decision.

Verification:

```bash
git diff --check
```

Conclusion:

- Afsm v3 should make the machine definition itself executable and graphable.
- The next engineering step is a small `afsm-core` or isolated test spike that validates Kotlin DSL ergonomics before changing sample-shop runtime code.

## [2026-05-09] Afsm executable DSL core spike

Change:

- Added `AfsmMachine<P, X, E, A, F>` and `AfsmSnapshot<P, X>` to `afsm-core`.
- Added a minimal executable DSL builder/interpreter with `afsmMachine`, `initial`, `state`, `on`, `onEnter`, `guard`, `otherwise`, `updateContext`, `transitionTo`, `action`, and `effect`.
- Added a ProductEditor-like DSL test proving phase transitions, context assignment, entry actions, typed payload phase access, guard fallback, and effect-only stayed transitions.

Verification:

```bash
./gradlew :afsm-core:compileTestKotlin --no-daemon
./gradlew :afsm-core:test --no-daemon
```

Conclusion:

- The executable DSL direction is viable in Kotlin without source-scanning or a separate graph-only definition.
- The current spike is not public API-final because graph metadata/export, exit actions, duplicate handler validation, and ProductEditor migration remain unresolved.

## [2026-05-09] Afsm executable DSL topology spike

Change:

- Added `AfsmTopology`, `AfsmTopologyState`, `AfsmTopologyTransition`, and `AfsmTopology.toMmd()`.
- Added `AfsmMachine.topology`.
- Refined the executable DSL so event branches are graphable at build time through `transitionTo(...)`, `transitionTo<PayloadPhase>(phase = { ... })`, `stay(...)`, and `otherwise(...)`.
- Updated the ProductEditor-like DSL test to verify Mermaid/topology export without executing sample events.

Verification:

```bash
./gradlew :afsm-core:compileTestKotlin --no-daemon
./gradlew :afsm-core:test --no-daemon
```

Conclusion:

- The executable DSL can now be both runtime behavior and graph source without source scanning or sample-state fixtures.
- Topology currently records state/event edges only; action labels, guard labels, entry rendering, and duplicate declaration diagnostics remain future work.

## [2026-05-09] ProductEditor executable DSL migration

Change:

- Migrated `sample-shop` ProductEditor from `AfsmPhasedStateMachine` and `ProductEditorPhaseEntryPolicy` to the executable DSL.
- Kept the Android-facing state shape as `ProductEditorState = ProductEditorPhase + ProductEditorContext`.
- Wrapped the DSL `AfsmMachine<ProductEditorPhase, ProductEditorContext, ...>` inside `ProductEditorStateMachine` so the existing `AfsmHost` integration still receives `AfsmStateMachine<ProductEditorState, ...>`.
- Added ProductEditor topology assertions and updated sample/wiki documentation.

Verification:

```bash
./gradlew :sample-shop:compileDebugKotlin :sample-shop:testDebugUnitTest --tests 'afsm.sample.shop.feature.editor.ProductEditorStateMachineTest' --no-daemon
./gradlew :sample-shop:testDebugUnitTest --tests 'afsm.sample.shop.feature.editor.ProductEditorStateMachineTest' --no-daemon
```

Conclusion:

- The executable DSL is now validated in a real Android sample flow, not only an isolated core test.
- The next verification gap is Android CLI smoke testing the product registration journey after the migration.

## [2026-05-09] ProductEditor executable DSL Android smoke

Change:

- Ran Android CLI smoke verification against the ProductEditor executable DSL migration.
- Captured layout JSON and annotated screenshots under `raw/verification/2026-05-09-product-editor-executable-dsl-smoke/`.
- Added a QA report linking the evidence.

Verification:

```bash
./gradlew :sample-shop:assembleDebug --no-daemon
android run --device=emulator-5556 --apks=sample-shop/build/outputs/apk/debug/sample-shop-debug.apk --activity=.MainActivity
android layout --device=emulator-5556 --pretty --output=...
android screen capture --annotate --output=...
```

Conclusion:

- The migrated ProductEditor flow works on device through register, draft entry, first rejection, resubmission, approval, publish, and return to catalog.

## [2026-05-09] Afsm DSL API cleanup and `.mmd` generation

Change:

- Removed the superseded phased helper surface from `afsm-core`, including `AfsmPhasedStateMachine` and `Afsm.phased(...)`.
- Renamed the context mutation DSL from `assign` to `updateContext`.
- Renamed the event branch receiver from `AfsmEventBuilder` to `AfsmEventBranchScope` and the transition receiver from `AfsmEventScope` to `AfsmTransitionScope`.
- Replaced `AfsmTopology.toMermaidStateDiagram()` with `AfsmTopology.toMmd()`.
- Added `:sample-shop:generateAfsmMmd`, which generates `sample-shop/build/generated/afsm/mmd/ProductEditorStateMachine.mmd` from the real ProductEditor machine topology.

Verification:

```bash
./gradlew :afsm-core:test --no-daemon
./gradlew :sample-shop:testDebugUnitTest --tests 'afsm.sample.shop.feature.editor.ProductEditorStateMachineTest' --tests 'afsm.sample.shop.feature.editor.ProductEditorMmdExportTest' --no-daemon
./gradlew :sample-shop:generateAfsmMmd --no-daemon
```

Conclusion:

- The current public-ish v3 surface is smaller and closer to the user's requested model: executable DSL first, automatic `.mmd` artifact generation, and no phased helper inheritance API.

## [2026-05-09] KSP `.mmd` generation design

Change:

- Added a design for KSP-based automatic `.mmd` generation across multiple Afsm state machines.
- Chose `@AfsmGraph` on `StateMachine` classes as the MVP registration model.
- Added `AfsmGraphSource` as the topology contract for graphable state-machine classes.
- Chose generated registry plus compiled topology execution instead of KSP static DSL parsing.
- Documented MVP registry generation and future Gradle plugin path.

Verification:

- Design-only change; no code verification required.

Conclusion:

- The next implementation spike should add `AfsmGraph`/`AfsmGraphSource`, add `afsm-graph-ksp`, generate a registry from annotated state-machine classes, and replace ProductEditor-only sample generation with registry-driven generation.

## [2026-05-09] KSP `.mmd` generation implementation slice

Change:

- Added graph API types to `afsm-core`: `AfsmGraph`, `AfsmGraphSource`, `AfsmGraphEntry`, `AfsmGraphRegistry`, and `AfsmMmdWriter`.
- Added `afsm-graph-ksp` with `AfsmGraphProcessorProvider` and `AfsmGraphProcessor`.
- The processor discovers `@AfsmGraph` state-machine classes, validates `AfsmStateMachine`/`AfsmGraphSource` conformance and no-required-arg construction, then generates `afsm.generated.AfsmGeneratedGraphRegistry`.
- Annotated `ProductEditorStateMachine` and changed sample `.mmd` export to use the generated registry instead of directly constructing ProductEditor.

Verification:

```bash
./gradlew :afsm-core:test :afsm-graph-ksp:test --no-daemon
./gradlew :sample-shop:generateAfsmMmd --no-daemon
```

Conclusion:

- The first KSP loop is working end to end. The remaining proof is to add a second graphable state machine without duplicating topology by hand.

## [2026-05-09] Executable DSL ignore and invalid branches

Change:

- Added `ignore(reason = ...)` and `invalid(reason = ...)` branches to `AfsmEventBranchScope`.
- These branches preserve `AfsmDecision.Ignored` and `AfsmDecision.Invalid` without adding topology edges.
- Added core DSL tests proving the decision behavior and no-edge topology behavior.

Verification:

```bash
./gradlew :afsm-core:test --no-daemon
```

Conclusion:

- The DSL can now migrate existing reducers that distinguish ignored events from invalid transitions without polluting the state diagram.

## [2026-05-09] Auth executable DSL graph migration

Change:

- Migrated `AuthStateMachine` from a `when` reducer to the executable DSL.
- Kept `AuthState` as the Android-facing sealed state while using internal `AuthPhase + AuthContext` for graphable DSL execution.
- Annotated `AuthStateMachine` with `@AfsmGraph`.
- Verified generated registry discovery for both `AuthStateMachine` and `ProductEditorStateMachine`.
- `generateAfsmMmd` now writes both `AuthStateMachine.mmd` and `ProductEditorStateMachine.mmd`.

Verification:

```bash
./gradlew :afsm-core:test :sample-shop:testDebugUnitTest --tests 'afsm.sample.shop.feature.auth.AuthStateMachineTest' --tests 'afsm.sample.shop.feature.editor.ProductEditorMmdExportTest' :sample-shop:generateAfsmMmd --no-daemon
```

Conclusion:

- The KSP graph pipeline now proves multiple real state-machine classes without hand-maintained topology duplicates.

## [2026-05-09] Afsm statechart naming and adapter cleanup

Change:

- Renamed the current executable DSL concept from `AfsmMachine` to `AfsmStateChart`.
- Renamed `AfsmSnapshot` to `AfsmChartState` so the phase/context pair reads as state, not as a persistence snapshot.
- Removed the old `AfsmMachine`, `afsmMachine`, and `AfsmSnapshot` names from the current spike API so IDE completion only exposes the statechart terminology.
- Added `AfsmStateChartMachine`, an adapter base that maps one Android-facing state to `AfsmChartState<Phase, Context>` and forwards topology automatically.
- Updated Auth and ProductEditor to use `AfsmStateChartMachine`, removing repeated `topology` forwarding and reducing exposed generic lists through feature-local chart typealiases.
- Clarified `ignore(...)` as an intentional handled no-op, not a replacement for omitted invalid transitions.

Verification:

```bash
./gradlew :afsm-core:test :afsm-graph-ksp:test :sample-shop:testDebugUnitTest --tests 'afsm.sample.shop.feature.auth.AuthStateMachineTest' --tests 'afsm.sample.shop.feature.editor.ProductEditorStateMachineTest' --tests 'afsm.sample.shop.feature.editor.ProductEditorMmdExportTest' :sample-shop:generateAfsmMmd --no-daemon
```

Conclusion:

- The current API now separates host-facing `AfsmStateMachine` from DSL-built `AfsmStateChart`, while keeping Android usage centered on one screen state.

## [2026-05-10] AfsmState phase/context model validation

Change:

- Added `AfsmState<P, X>` as the standard public phase/context state data class in `afsm-core`.
- Deprecated `AfsmChartState<P, X>` as a compatibility alias to `AfsmState<P, X>`.
- Changed `AfsmStateChart` to implement both `AfsmStateMachine<AfsmState<P, X>, E, A, F>` and `AfsmGraphSource`.
- Updated the executable DSL interpreter to use `AfsmState` directly.
- Migrated ProductEditor to `typealias ProductEditorState = AfsmState<ProductEditorPhase, ProductEditorContext>`.
- Replaced the ProductEditor chart adapter mapping with direct chart delegation in `ProductEditorStateMachine`.
- Updated Auth and core DSL tests away from deprecated `AfsmChartState` references.

Verification:

```bash
./gradlew :sample-shop:compileDebugKotlin --no-daemon
./gradlew :afsm-core:test :sample-shop:testDebugUnitTest :sample-shop:generateAfsmMmd --no-daemon
```

Conclusion:

- The standard `AfsmState<Phase, Context>` model compiles and reduces ProductEditor boilerplate without breaking runtime behavior or `.mmd` graph generation.
- Kotlin typealiases cannot have same-named default factories, so feature samples should use lower-case factories such as `productEditorState()` for default state construction.
- Custom sealed Android-facing states remain possible through `AfsmStateChartMachine`; ProductEditor now demonstrates the simpler direct state path.

## [2026-05-10] Afsm API hardening loop

Change:

- Added `AfsmReducer<S, E, C, F>` and changed `AfsmHost` / `ViewModel.afsmHost(...)` to use `reducer`.
- Added `AfsmMachine<P, X, E, C, F>` as the DSL-built executable machine name.
- Kept deprecated compatibility aliases for `AfsmStateMachine`, `AfsmStateChart`, `afsmStateChart`, and `AfsmStateChartMachine`.
- Renamed DSL output calls from `action(...)` to `command(...)` in core tests and sample state machines.
- Added flat `onExit` support with deterministic `onExit -> transition block -> onEnter` ordering.
- Added `AfsmDefinitionException` and build-time DSL validation for missing initial state declarations, duplicate state/event declarations, and undeclared transition targets.
- Expanded `AfsmTopologyTransition` with guard, command, effect, transition kind, and fallback metadata.
- Added `AfsmCommandFailurePolicy` plus runtime diagnostics for command-handler failures; cancellation exceptions are rethrown.
- Kept command cancellation explicit in feature commands/events; the runtime does not cancel commands automatically when later events arrive.
- Updated KSP graph validation to require `AfsmReducer` plus `AfsmGraphSource`.
- Migrated Auth, ProductEditor, Checkout, runtime tests, and ViewModel tests to the new names.

Verification:

```bash
./gradlew :afsm-core:test :afsm-runtime:test :afsm-viewmodel:testDebugUnitTest --stacktrace
./gradlew :sample-shop:compileDebugKotlin :sample-shop:testDebugUnitTest :sample-shop:generateAfsmMmd --stacktrace
```

Conclusion:

- The hardened API compiles through core/runtime/ViewModel modules and real sample-shop usage.
- Generated graphs still come from the compiled machine topology and are written to `sample-shop/build/generated/afsm/mmd/`.

## [2026-05-11] Public API alias removal and README

Change:

- Removed pre-release compatibility aliases: `AfsmStateMachine`, `AfsmStateChart`, `afsmStateChart`, `AfsmStateChartMachine`, and `AfsmChartState`.
- Added root `README.md` with module overview, reducer example, machine DSL example, ViewModel integration, graph generation, runtime policies, and verification commands.
- Added `docs/afsm-public-api.md` as the current API reference.
- Updated canonical wiki pages away from the removed names where they describe current architecture.

Verification:

```bash
./gradlew :afsm-core:test :afsm-runtime:test :afsm-viewmodel:testDebugUnitTest :sample-shop:compileDebugKotlin :sample-shop:testDebugUnitTest :sample-shop:generateAfsmMmd --stacktrace
```

Conclusion:

- New public-facing docs now teach the final pre-release vocabulary and the codebase compiles without the removed aliases.

## [2026-05-11] Maven local publication setup

Change:

- Added common pre-release `group = "io.github.afsm"` and `version = "0.1.0-SNAPSHOT"`.
- Added `maven-publish` publications for `afsm-core`, `afsm-runtime`, `afsm-graph-ksp`, and `afsm-viewmodel`.
- JVM modules publish jars, sources jars, javadoc jars, POMs, and Gradle module metadata.
- `afsm-viewmodel` publishes the Android `release` AAR with sources.
- Updated public docs with local Maven coordinates.

Verification:

```bash
./gradlew publishToMavenLocal --stacktrace
```

Conclusion:

- Local Maven publication succeeds for all library modules; remote publication still needs license, SCM, signing, and final coordinate decisions.

## [2026-05-11] Maven Local consumer smoke

Change:

- Added `consumer-smoke`, a separate Android Gradle build that resolves Afsm only from Maven Local.
- Added a small consumer ViewModel using `ViewModel.afsmHost(...)`, an `AfsmMachine`, a command handler, and `@AfsmGraph` KSP generation.
- Added `scripts/verify-consumer-smoke.sh` to publish local artifacts and compile the consumer build.
- Updated public docs with the consumer smoke release gate.

Verification:

```bash
./scripts/verify-consumer-smoke.sh
```

Conclusion:

- Afsm's current Maven Local artifacts are consumable by an external Android library build, including the Android ViewModel AAR and KSP graph processor.

## [2026-05-11] Release readiness warning triage

Change:

- Investigated `publishToMavenLocal --warning-mode all`.
- Added `docs/release-readiness.md` with the current release gate, remaining product decisions, engineering gates, and known warning policy.

Verification:

```bash
./gradlew publishToMavenLocal --warning-mode all
./gradlew :afsm-runtime:generatePomFileForMavenPublication --warning-mode all --stacktrace
```

Conclusion:

- The remaining Gradle deprecation warning is emitted from Kotlin Gradle plugin POM rewriting for a project dependency, not from direct Afsm build-script usage.
- Current policy is to track it as a Kotlin/Gradle compatibility item and re-check during plugin upgrades rather than weakening publication metadata.

## [2026-05-11] Kotlin explicit API gate

Change:

- Enabled Kotlin `explicitApi()` for `afsm-core`, `afsm-runtime`, `afsm-viewmodel`, and `afsm-graph-ksp`.
- Updated release readiness documentation to mark explicit API mode as part of the current local release gate.

Verification:

```bash
./gradlew :afsm-core:compileKotlin :afsm-runtime:compileKotlin :afsm-graph-ksp:compileKotlin :afsm-viewmodel:compileDebugKotlin --stacktrace
```

Conclusion:

- Existing Afsm public declarations already satisfy explicit API requirements.
- Binary API validation remains the next stronger public API stability gate.

## [2026-05-11] Binary API validation gate

Change:

- Added JetBrains binary compatibility validator `0.18.1` to the root build.
- Excluded `sample-shop` from API validation.
- Generated API dumps for `afsm-core`, `afsm-runtime`, `afsm-viewmodel`, and `afsm-graph-ksp`.
- Removed unnecessary ABI exposure for `AfsmEventBranchScope.addDecisionBranch` and `afsmLabelForValue`.
- Updated release docs to include `apiCheck`.

Verification:

```bash
./gradlew apiDump --stacktrace
./gradlew apiCheck --stacktrace
```

Conclusion:

- Public ABI is now tracked before the first public release.
- Remaining `@PublishedApi internal` DSL helpers are intentional ABI support for inline/reified DSL entry points.

## [2026-05-11] Pre-release changelog

Change:

- Added `CHANGELOG.md` with an initial `0.1.0 - Unreleased` entry.
- Linked the changelog from `README.md`.
- Removed the changelog item from the remaining engineering gates in `docs/release-readiness.md`.

Verification:

```bash
git diff --check
```

Conclusion:

- Public release notes now have a durable place before the first published artifact.

## [2026-05-11] Contribution guardrails

Change:

- Added `CONTRIBUTING.md` with engineering principles, development flow, test integrity policy, public API change policy, verification commands, and documentation rules.
- Linked contribution guidance from `README.md`.
- Added contribution guide alignment to release readiness.

Verification:

```bash
git diff --check
```

Conclusion:

- The project now has a public rule set that matches the user's TDD/spec-first expectations and release gate.

## [2026-05-11] One-command local release verification

Change:

- Added `scripts/verify-release-local.sh` as the canonical local release gate.
- Updated README, release readiness docs, and contribution docs to point to the one-command gate.

Verification:

```bash
./scripts/verify-release-local.sh
```

Conclusion:

- Release-facing verification is now harder to run partially by accident.

## [2026-05-11] Maven publication metadata audit

Change:

- Inspected generated Maven Local POMs for `afsm-core`, `afsm-runtime`, `afsm-viewmodel`, and `afsm-graph-ksp`.
- Documented current metadata status in `docs/release-readiness.md`.

Verification:

```bash
sed -n '1,220p' ~/.m2/repository/io/github/afsm/afsm-core/0.1.0-SNAPSHOT/afsm-core-0.1.0-SNAPSHOT.pom
sed -n '1,260p' ~/.m2/repository/io/github/afsm/afsm-runtime/0.1.0-SNAPSHOT/afsm-runtime-0.1.0-SNAPSHOT.pom
sed -n '1,260p' ~/.m2/repository/io/github/afsm/afsm-viewmodel/0.1.0-SNAPSHOT/afsm-viewmodel-0.1.0-SNAPSHOT.pom
sed -n '1,220p' ~/.m2/repository/io/github/afsm/afsm-graph-ksp/0.1.0-SNAPSHOT/afsm-graph-ksp-0.1.0-SNAPSHOT.pom
```

Conclusion:

- POM packaging and internal dependency coordinates are correct for local consumption.
- URL, license, SCM, and developer metadata are intentionally blocked on product ownership decisions.

## [2026-05-11] ProductEditor submit transition readability cleanup

Change:

- Removed the `submitDraft(...)` event-branch helper from `ProductEditorStateMachine`.
- Inlined submit/resubmit `transitionTo(...)` branches in `EditingDraft`, `DraftSaved`, and `Rejected`.
- Kept only context transformation helperization through `normalizeDraftForSubmit()`.
- Updated sample guide and sample wiki guidance to keep graph-relevant transitions visible in event branches.

Verification:

```bash
./gradlew :sample-shop:testDebugUnitTest :sample-shop:generateAfsmMmd --stacktrace
```

Conclusion:

- ProductEditor remains behaviorally equivalent while making phase movement easier to read directly from the DSL machine body.

## [2026-05-11] ProductEditor guarded transition cleanup

Change:

- Changed invalid `SubmitClicked` handling from `DraftSaved` to use `otherwise` instead of a second guarded `transitionTo`.
- Kept valid submit as the single phase-changing branch to `ImageUploadInProgress`.
- Display validation errors while in `DraftSaved` so the stayed invalid branch is visible to the user.
- Added unit coverage for invalid submit from a saved draft.
- Updated sample guide and wiki guidance to avoid competing `transitionTo` branches for validation failure.

Verification:

```bash
./gradlew :sample-shop:testDebugUnitTest :sample-shop:generateAfsmMmd --stacktrace
```

Conclusion:

- The sample now better communicates the intended Afsm style: success transitions move phases; validation failure is a handled stayed branch.

## [2026-05-11] Afsm DSL KDoc expansion

Change:

- Expanded KDoc for the public Afsm executable DSL in `AfsmMachineDsl.kt`.
- Documented type parameter roles for phase, context, event, command, and effect.
- Documented runtime parameters such as `phase`, `guard`, `block`, `updateContext`, `command`, and `effect`.
- Documented topology-only metadata parameters such as `guardLabel`, `commandLabels`, and `effectLabels`.
- Clarified branch ordering, `otherwise`, `ignore`, `invalid`, and `onExit -> transition block -> onEnter` execution order.

Verification:

```bash
./gradlew :afsm-core:test apiCheck --stacktrace
```

Conclusion:

- The DSL source now explains the public API directly at the call site without changing binary API.

## [2026-05-11] AfsmMachineAdapter removal

Change:

- Removed `AfsmMachineAdapter` from `afsm-core`.
- Migrated Auth from a custom sealed `AuthState` plus adapter mapping to `typealias AuthState = AfsmState<AuthPhase, AuthContext>`.
- Updated Auth ViewModel initial state, Auth transition tests, public docs, changelog, and wiki guidance.
- Moved the authenticated `UserSession` into the `Authenticated` phase payload and cleared Auth form context on success so password input is not retained.

Verification:

```bash
./gradlew :sample-shop:testDebugUnitTest --warning-mode all --no-daemon
./gradlew :afsm-core:apiDump --no-daemon
```

Conclusion:

- Auth and ProductEditor now both demonstrate the direct `AfsmState<Phase, Context>` path, removing the last sample need for an adapter base.

## [2026-05-11] AfsmState factory spike

Change:

- Spiked a core `AfsmStateFactory` / `afsmStateFactory(...)` API locally against Auth and ProductEditor.
- Verified that the callable factory shape can preserve `authState()` / `productEditorState()` call sites.
- Rejected the API before keeping it because singleton phase type inference required explicit `<Phase, Context>` arguments and the public API cost outweighed the small local boilerplate reduction.

Verification:

```bash
./gradlew :afsm-core:test :sample-shop:testDebugUnitTest --warning-mode all --no-daemon
```

Conclusion:

- Continue using small feature-local factory functions for default `AfsmState<Phase, Context>` construction.

## [2026-05-11] AfsmGraphReducer feature-boundary cleanup

Change:

- Added `AfsmGraphReducer<S, E, C, F>` to `afsm-core`.
- Made `AfsmMachine<P, X, E, C, F>` extend `AfsmGraphReducer<AfsmState<P, X>, E, C, F>`.
- Refactored Auth/ProductEditor state machines from no-arg classes to singleton objects.
- Changed Auth/ProductEditor machine aliases from five-parameter `AfsmMachine<Phase, Context, Event, Command, Effect>` aliases to four-parameter `AfsmGraphReducer<State, Event, Command, Effect>` aliases.
- Updated ViewModels to use `StateMachine.initialState` and the singleton reducer object.
- Updated consumer smoke to use `AfsmGraphReducer`.
- Kept KSP validation on the underlying `AfsmReducer + AfsmGraphSource` supertypes because KSP does not reliably expose `AfsmGraphReducer` through typealias declarations.

Verification:

```bash
./gradlew :afsm-core:test :sample-shop:testDebugUnitTest :sample-shop:generateAfsmMmd --warning-mode all --no-daemon
./gradlew :afsm-core:test :sample-shop:testDebugUnitTest :sample-shop:generateAfsmMmd apiCheck --warning-mode all --no-daemon
./gradlew :sample-shop:assembleDebug --warning-mode all --no-daemon
./scripts/verify-release-local.sh
git diff --check
```

Conclusion:

- The feature-boundary code now reads in terms of screen state instead of repeating the internal phase/context split, while preserving graph generation and runtime behavior.
- The full local release gate still passes after the API cleanup, including Maven Local publication and the external `consumer-smoke` compile/KSP check.

## [2026-05-11] Public API usability hardening pass

Change:

- Recorded the five-perspective public API usability review in the meetings wiki.
- Added `ViewModel.afsmHost(machine = ...)` for graphable machines.
- Updated Auth, ProductEditor, and consumer smoke ViewModels to use the machine overload.
- Changed `AfsmHost` command handling so commands remain sequential but no longer block event reduction.
- Added `AfsmHost.tryDispatch(event)`.
- Bounded the default event queue with `AfsmConfig.eventQueueCapacity = 64`.
- Changed the default invalid transition policy to `Throw`.
- Rewrote README onboarding around the `afsmMachine { ... }` happy path.

Verification:

```bash
./gradlew --stop && ./gradlew :afsm-runtime:clean :afsm-runtime:test --no-daemon
./gradlew :afsm-viewmodel:compileDebugKotlin :sample-shop:compileDebugKotlin --no-daemon
./gradlew :afsm-runtime:apiDump :afsm-viewmodel:apiDump --no-daemon
./gradlew :afsm-core:test :afsm-runtime:test :afsm-viewmodel:testDebugUnitTest :sample-shop:testDebugUnitTest :sample-shop:compileDebugKotlin --warning-mode all --no-daemon
./gradlew :sample-shop:generateAfsmMmd apiCheck :sample-shop:assembleDebug --warning-mode all --no-daemon
./scripts/verify-release-local.sh
```

Conclusion:

- The standard ViewModel setup is shorter and easier to read.
- Runtime semantics are safer for Android UI responsiveness.
- The full local release gate passes after the docs/API updates, including API validation, sample APK assembly, Maven Local publication, and external consumer smoke.

## [2026-05-11] Public API usability hardening pass v2

Change:

- Added `afsm-compose` with `CollectAfsmEffects(...)` and migrated sample-shop routes away from repeated lifecycle effect collection code.
- Removed the pre-release `AfsmGraphReducer` public name.
- Introduced `AfsmMachine<S, E, C, F>` as the graphable feature-boundary API and `AfsmPhaseMachine<P, X, E, C, F>` as the DSL-built phase/context API.
- Added `ViewModel.afsmHost(machine = ..., initialState = ...)` for dynamic initial state while keeping graph metadata.
- Added `AfsmConfig.commandQueueCapacity` and validation for invalid queue capacities.
- Updated Checkout to use payment request ids and ignore stale command results.
- Enriched topology/MMD output with initial state, entry/exit command/effect labels, and `AfsmMmdOptions.Flow` / `Full`.
- Rewrote public README onboarding and added `docs/testing-guide.md`.
- Updated API dumps and release/consumer documentation.

Verification:

```bash
./gradlew :afsm-core:test :afsm-runtime:test :afsm-viewmodel:testDebugUnitTest :afsm-compose:compileDebugKotlin :sample-shop:compileDebugKotlin :sample-shop:testDebugUnitTest --warning-mode all --no-daemon
./gradlew apiDump --warning-mode all --no-daemon
./scripts/verify-release-local.sh --warning-mode all
```

Conclusion:

- The public API now has a simpler first-contact vocabulary: `AfsmReducer`, `AfsmMachine`, `AfsmPhaseMachine`, `AfsmState`, `AfsmTransition`, `Command`, and optional `Effect`.
- The full local release gate passes after the usability hardening pass, including Maven Local publication and the external consumer smoke compile/KSP check.

## [2026-05-14] Afsm adoption hardening loop

Change:

- Ran a second 10-agent Android developer review and CTO synthesis.
- Added `AfsmDslMarker`.
- Refactored `afsm-core` DSL internals so `addState`, `addBranch`, `addEventDefinition`, and `afsmLabelForClass` no longer appear in the public API dump.
- Changed `AfsmTransition` from public data-class construction to factory-based construction.
- Split `ViewModel.afsmHost(machine = ..., initialState = ...)` from the custom `reducer = ..., initialState = ...` escape hatch.
- Guarded completed Checkout against duplicate pay/retry events and rendered completion state.
- Hardened graph `.mmd` output paths in both `AfsmMmdWriter` and the KSP processor.
- Added `docs/modeling-rules.md` and linked it from README/sample docs.

Verification:

```bash
./gradlew :afsm-core:test :afsm-runtime:test :afsm-viewmodel:testDebugUnitTest :sample-shop:testDebugUnitTest --warning-mode all --no-daemon
./gradlew :sample-shop:testDebugUnitTest :sample-shop:compileDebugKotlin --warning-mode all --no-daemon
./gradlew :afsm-core:test :afsm-graph-ksp:test :sample-shop:generateAfsmMmd apiCheck --warning-mode all --no-daemon
./scripts/verify-release-local.sh --warning-mode all
```

Conclusion:

- The P0 public ABI leak is fixed and the local release gate still passes.
- Afsm is more credible for internal beta, but runtime pressure tests, restoration/effect policy, graph compile-testing, and Checkout graphability remain before public OSS/stable release.

## [2026-05-14] Runtime pressure and effect lifecycle hardening

Change:

- Added `AfsmCommandQueueOverflowException`.
- Changed accepted command enqueueing from suspending `send` to fail-fast `trySend`.
- Documented command queue overflow behavior in README, public API docs, modeling rules, and runtime wiki.
- Added runtime tests for command queue overflow and default no-replay effect delivery.

Verification:

```bash
./gradlew :afsm-runtime:test --warning-mode all --no-daemon
./gradlew :afsm-runtime:apiDump --warning-mode all --no-daemon
./gradlew apiCheck --warning-mode all --no-daemon
```

Conclusion:

- Afsm now surfaces command pressure as an explicit runtime error rather than risking an event-loop stall.
- Superseded follow-up: command-result event overflow was later resolved by
  `AfsmEventQueueOverflowException` in the 2026-05-20 hardening pass.

## [2026-05-14] Restoration, effect, and command policy guide

Change:

- Added `docs/restoration-effect-command-policy.md`.
- Documented restoreable state versus runtime work, `onEnter` restoration safety, best-effort effects, state-plus-acknowledgement UI work, command result events, request ids, explicit cancellation, and queue pressure.
- Linked the guide from README, public API docs, modeling rules, and sample-shop guide.

Verification:

```bash
./scripts/verify-release-local.sh --warning-mode all
```

Conclusion:

- The pre-release docs now give Android developers a concrete policy for the lifecycle and command questions that were blocking broad adoption confidence.

## [2026-05-14] Graphable Checkout and curated examples

Change:

- Converted Checkout from a custom `AfsmReducer` escape hatch to a graphable `AfsmMachine`.
- Split Checkout into `CheckoutPhase + CheckoutContext` with `CheckoutState = AfsmState<CheckoutPhase, CheckoutContext>`.
- Added `CheckoutRenderState` and `CheckoutState.toRenderState()` so Compose rendering does not need to know every internal phase.
- Annotated `CheckoutStateMachine` with `@AfsmGraph`, producing `CheckoutStateMachine.mmd`.
- Added tests for Checkout topology and updated payment retry/stale-result tests for the phase/context model.
- Added `docs/examples.md` and `docs/checkout-walkthrough.md`.
- Updated README, modeling rules, sample guide, testing guide, and wiki pages to make Auth -> Checkout -> ProductEditor the public example ladder.

Verification:

```bash
./gradlew :sample-shop:testDebugUnitTest :sample-shop:compileDebugKotlin :sample-shop:generateAfsmMmd --warning-mode all --no-daemon
```

Conclusion:

- Checkout is now the mid-size adoption example for dynamic initial state, loading, retry, stale command results, durable completion, optional effects, and generated state diagrams.

## [2026-05-14] Dedicated walkthroughs for all primary examples

Change:

- Added `docs/auth-walkthrough.md`.
- Added `docs/product-editor-walkthrough.md`.
- Updated `docs/examples.md`, README, and sample guide so Auth, Checkout, and ProductEditor each have a dedicated public walkthrough.

Verification:

```bash
./scripts/verify-release-local.sh --warning-mode all
```

Conclusion:

- The primary examples are now curated as a complete onboarding set rather than being buried inside the sample app guide.

## [2026-05-16] GitHub-facing README and CI

Change:

- Added `.github/workflows/ci.yml`.
- Added README status badges, internal-beta status, quickstart commands, and clearer GitHub first-screen positioning.
- Updated `docs/release-readiness.md` with the private GitHub repo, CI workflow, and badge visibility note.
- Updated `CONTRIBUTING.md` and `CHANGELOG.md`.
- Updated verification scripts so additional Gradle arguments such as `--warning-mode all` are forwarded to all underlying Gradle invocations.

Verification:

```bash
./scripts/verify-release-local.sh --warning-mode all
```

Conclusion:

- The GitHub README now communicates current maturity and the fastest validation path.
- CI and local release verification now share the same command.

## [2026-05-19] Six-agent usability hardening loop

Change:

- Ran six Android-developer usability reviews, implemented the high-signal
  feedback, then ran six post-change reviews.
- Added `state(phase)` and `state<PayloadPhase>()` no-block DSL convenience.
- Updated README first-use onboarding and minimal Draft naming.
- Added `docs/graph-generation.md`.
- Moved Auth screen rendering to `AuthRenderState` and explicit authenticated
  render state.
- Added Checkout `primaryAction` render state and kept payment-in-progress
  button visible.
- Clarified ProductEditor transition execution order and beta adoption policy.
- Updated `CHANGELOG.md`, public API docs, sample guide, example catalog, and
  wiki meeting notes.

Verification:

```bash
./gradlew :afsm-core:test :sample-shop:testDebugUnitTest --warning-mode all --no-daemon
./gradlew :afsm-core:apiDump --warning-mode all --no-daemon
./gradlew apiCheck --warning-mode all --no-daemon
./gradlew :sample-shop:generateAfsmMmd :sample-shop:assembleDebug --warning-mode all --no-daemon
```

Conclusion:

- The first-use path is lighter, sample UI boundaries are more consistent, and
  graph setup is better documented. The next major usability target is a
  first-class graph-generation Gradle plugin.

## [2026-05-19] Graph plugin and ProductEditor render-state loop

Change:

- Added the `afsm-graph-gradle-plugin` included build and Gradle plugin
  artifact with plugin id `io.github.afsm.graph`.
- The graph plugin generates a JUnit4-compatible `AfsmGeneratedMmdExportTest`,
  reuses the selected Android unit-test variant classpath, wires
  `afsm.mmd.outputDir`, and registers `generateAfsmMmd` as a dedicated graph
  export `Test` task.
- Removed the hand-written `ProductEditorMmdExportTest`; sample graph
  generation is now plugin-driven.
- Updated `consumer-smoke` so an external Maven Local Android build applies the
  published graph plugin and runs `:app:generateAfsmMmd`.
- Added `ProductEditorRenderState` plus primary/secondary UI actions so
  `ProductEditorScreen` no longer branches on internal `ProductEditorPhase`.
- Added sample graph registry coverage so Auth, Checkout, and ProductEditor
  graph registration cannot silently regress.
- Hid draft fields in ProductEditor published render state and added render
  mapping tests for rejected, processing, and published UI states.
- Updated release/docs/wiki guidance to make the graph plugin the preferred
  graph-generation path.

Verification:

```bash
./gradlew -p afsm-graph-gradle-plugin clean test --warning-mode all --no-daemon
./gradlew :sample-shop:compileDebugKotlin :sample-shop:testDebugUnitTest --warning-mode all --no-daemon
./gradlew :sample-shop:generateAfsmMmd --warning-mode all --no-daemon
./scripts/verify-consumer-smoke.sh --warning-mode all --no-daemon
./scripts/verify-release-local.sh --warning-mode all --no-daemon
```

Conclusion:

- The most visible graph-generation boilerplate is now library-owned instead of
  app-owned.
- `generateAfsmMmd` no longer runs the whole app unit-test suite and does not
  force JUnit Platform on existing Android tests.
- The next graph hardening target is processor compile-testing and plugin
  functional tests.

## [2026-05-19] Graph tooling verification hardening

Change:

- Ran a fourth six-agent Android developer review round focused on graph
  plugin/KSP trustworthiness.
- Added `afsm-graph-ksp` functional tests that run a real Kotlin/KSP fixture
  and verify generated registry output plus invalid annotation diagnostics.
- Added `afsm-graph-gradle-plugin` TestKit functional tests for Android
  app/library modules, missing KSP plugin messaging, normal unit-test
  separation, and no-registry `generateAfsmMmd` messaging.
- Removed direct AGP DSL type references from the graph Gradle plugin to avoid
  plugin classloader fragility.
- Made the generated graph export test reflection-based, excluded it from
  ordinary Android unit-test execution, and kept graph generation behind the
  dedicated `generateAfsmMmd` task.
- Updated consumer smoke to assert the generated `.mmd` file exists, starts
  with `stateDiagram-v2`, and contains representative transition lines.

Verification:

```bash
./gradlew :afsm-graph-ksp:test --warning-mode all --no-daemon
./gradlew -p afsm-graph-gradle-plugin test --warning-mode all --no-daemon
./gradlew :sample-shop:testDebugUnitTest :sample-shop:generateAfsmMmd --warning-mode all --no-daemon
```

Conclusion:

- Graph generation now has executable coverage for both processor contracts and
  Gradle plugin UX.
- The next graph-tooling work should focus on multi-variant/multi-module policy
  rather than adding more public API.

## [2026-05-20] Version alignment and command-result pressure hardening

Change:

- Ran a fifth six-agent Android developer usability review focused on external
  setup, public API complexity, graph plugin version behavior, runtime pressure,
  and sample adoption boundaries.
- Moved the shared pre-release version to `gradle.properties` as `afsmVersion`.
- Updated the graph Gradle plugin so its default `afsm-graph-ksp` processor
  coordinate is generated from the shared Afsm version.
- Updated `consumer-smoke` and `verify-consumer-smoke.sh` so the separate build
  verifies the root `afsmVersion` instead of hardcoded stale Maven Local
  coordinates.
- Added fail-fast command-result event overflow handling through
  `AfsmEventQueueOverflowException`, while dropping and logging closed-host
  command results as lifecycle completion.
- Added the `docs/examples.md` adoption decision table to make Auth a syntax
  tutorial and Checkout/ProductEditor the stronger Afsm proof cases.
- Added README Maven Local pilot setup snippets based on `consumer-smoke`.
- Post-review fixes forced `consumer-smoke` to clean and refresh dependencies,
  corrected app-consumer README plugin snippets, corrected the graph setup
  block label, and changed closed-host command result handling to logged drops
  instead of overflow errors.

Verification:

```bash
./gradlew :afsm-runtime:test --tests afsm.runtime.AfsmHostTest.'command result event overflow fails fast instead of suspending command processing' --warning-mode all --no-daemon
./gradlew :afsm-runtime:test --tests "afsm.runtime.AfsmHostTest.command result event after host close is dropped as lifecycle completion" --warning-mode all --no-daemon
./gradlew :afsm-runtime:apiDump --warning-mode all --no-daemon
./gradlew -p afsm-graph-gradle-plugin test --warning-mode all --no-daemon
./gradlew :afsm-runtime:test --warning-mode all --no-daemon
./scripts/verify-consumer-smoke.sh --warning-mode all --no-daemon
./scripts/verify-release-local.sh --warning-mode all --no-daemon
```

Conclusion:

- The current hardening pass closes two concrete false-positive/stall risks:
  stale local artifact verification and suspended command-result dispatch.
  The full local release gate passes with the existing documented Gradle
  deprecation warning from Kotlin POM rewriting.

## [2026-05-21] Case-oriented DSL usability pass

Change:

- Added named `case(label, condition = ...) { ... }` event branches to the
  executable DSL.
- Simplified DSL-level `transitionTo(...)` so it only declares phase changes.
- Removed DSL-level `stay(...)` and `otherwise(...)` from the public source
  surface.
- Kept context mutation under `updateContext(...)`, including an overload for
  `updateContext { context, event -> ... }` when event payload is needed.
- Migrated Auth, Checkout, ProductEditor, README, public API docs, walkthroughs,
  and `consumer-smoke` to the case-oriented style.
- Updated API dumps and canonical wiki/decision pages.

Verification:

```bash
./gradlew :afsm-core:test :sample-shop:testDebugUnitTest :sample-shop:generateAfsmMmd --no-daemon
./gradlew :afsm-core:apiCheck --no-daemon
./scripts/verify-consumer-smoke.sh --warning-mode all --no-daemon
./scripts/verify-release-local.sh --warning-mode all --no-daemon
```

Conclusion:

- The graphable DSL no longer requires Android developers to learn `stay` or
  `otherwise` for normal usage.
- `transitionTo` now reads as phase change only, while named cases carry
  conditions, context updates, commands, effects, and graph labels.

## [2026-05-21] Public condition terminology cleanup

Change:

- Renamed `AfsmTopologyTransition.guardLabel` to `conditionLabel`.
- Renamed optional `ignore`/`invalid` branch parameters from `guard` to
  `condition`.
- Updated sample expectations and canonical docs so DSL source, topology
  metadata, and `.mmd` labels use one term.

Verification:

```bash
./gradlew :afsm-core:compileKotlin :afsm-core:compileTestKotlin :sample-shop:compileDebugKotlin :sample-shop:compileDebugUnitTestKotlin --no-daemon
./gradlew :afsm-core:apiDump --no-daemon
./gradlew :afsm-core:apiCheck :afsm-core:test :sample-shop:testDebugUnitTest :sample-shop:generateAfsmMmd --no-daemon
./scripts/verify-release-local.sh --warning-mode all --no-daemon
```

Conclusion:

- The current public surface no longer asks Android users to learn both
  `condition` and `guard` for the same idea.

## [2026-05-21] Explicit validation branch conditions

Change:

- Updated ProductEditor and Auth state-machine examples so invalid form/draft
  branches declare explicit `condition = ...` predicates.
- Updated public walkthroughs and wiki pages to describe no-transition
  validation handling as an explicit condition branch, not a fallback branch.

Verification:

```bash
./gradlew :afsm-core:test :sample-shop:testDebugUnitTest :sample-shop:generateAfsmMmd --no-daemon
```

Conclusion:

- The DSL examples now avoid recreating `otherwise` through an unconditional
  final case in validation flows.
