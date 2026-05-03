---
title: Afsm Core Compile Validation
updated: 2026-05-03
---

# Afsm Core Compile Validation

## Summary

The first `afsm-core` Kotlin/JVM skeleton compiles with the v2 core API shape.

Validated types:

- `AfsmStateMachine<S, E, C, F>`
- `AfsmTransition<S, C, F>`
- `AfsmDecision.Transitioned`
- `AfsmDecision.Stayed`
- `AfsmDecision.Ignored`
- `AfsmDecision.Invalid`
- `AfsmNoEffect`
- `Afsm.transitionTo(...)`
- `Afsm.stay(...)`
- `Afsm.ignore(...)`
- `Afsm.invalid(...)`

## Project Shape

`afsm-core` is a plain Kotlin/JVM module.

Core dependency policy:

- allowed: Kotlin stdlib through the Kotlin JVM plugin
- not used: Android SDK, AndroidX, coroutines, Compose, serialization, DI, KSP

Files:

- `settings.gradle.kts`
- `build.gradle.kts`
- `afsm-core/build.gradle.kts`
- `afsm-core/src/main/kotlin/afsm/core/`
- `afsm-core/src/test/kotlin/afsm/core/AfsmCoreCompileCheck.kt`

## Compile Check

Command:

```bash
./gradlew :afsm-core:compileTestKotlin --no-daemon
```

Result:

```text
BUILD SUCCESSFUL
```

## What Was Verified

`AfsmTransition<S, C, F>` is usable when hidden behind feature-local typealiases:

```kotlin
private typealias SignupTransition =
    AfsmTransition<SignupState, SignupCommand, SignupEffect>
```

`AfsmNoEffect` works as the no-effect marker for flows that cannot emit effects:

```kotlin
private typealias LoginTransition =
    AfsmTransition<LoginState, LoginCommand, AfsmNoEffect>
```

Builder type inference works in both effectful and no-effect flows:

- `Afsm.transitionTo(...)` with commands
- `Afsm.transitionTo(...)` with effects
- `Afsm.stay(...)` with no outputs
- `Afsm.stay(...)` with cleanup commands
- `Afsm.ignore(...)`
- `Afsm.invalid(...)`

## Conclusion

The v2 core API is viable enough to proceed to the next implementation layer.

The main ergonomic finding still holds: `AfsmTransition<S, C, F>` should be documented with screen-local typealiases as the standard usage pattern.

## Follow-Up

Next recommended task: design and implement the minimal `afsm-runtime` dispatch loop that consumes `AfsmTransition` and verifies serialized event processing.
