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
