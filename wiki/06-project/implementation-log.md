---
title: Implementation Log
updated: 2026-05-09
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
- Added ProductEditor pseudo implementation showing `state`, `on`, `guard`, `assign`, `onEnter`, `action`, `effect`, and `transitionTo`.
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
- Added a minimal executable DSL builder/interpreter with `afsmMachine`, `initial`, `state`, `on`, `onEnter`, `guard`, `otherwise`, `assign`, `transitionTo`, `action`, and `effect`.
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

- Added `AfsmTopology`, `AfsmTopologyState`, `AfsmTopologyTransition`, and `AfsmTopology.toMermaidStateDiagram()`.
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
