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
