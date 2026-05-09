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
