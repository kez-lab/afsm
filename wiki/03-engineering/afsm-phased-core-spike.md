---
title: Afsm Phased Core Spike
updated: 2026-05-09
---

# Afsm Phased Core Spike

This spike validates the current v3 phased-state idea in real Kotlin code inside `afsm-core`.

It is not a public API freeze.

## Implemented Surface

Files:

- `afsm-core/src/main/kotlin/afsm/core/AfsmPhasedState.kt`
- `afsm-core/src/main/kotlin/afsm/core/AfsmPhaseEntry.kt`
- `afsm-core/src/main/kotlin/afsm/core/AfsmPhaseEntryPolicy.kt`
- `afsm-core/src/main/kotlin/afsm/core/AfsmPhasedTransitionScope.kt`
- `afsm-core/src/main/kotlin/afsm/core/Afsm.kt`

Added concepts:

```kotlin
interface AfsmPhasedState<S : Any, P : Any, X : Any>

data class AfsmPhaseEntry<X : Any, C : Any, F : Any>

fun interface AfsmPhaseEntryPolicy<P : Any, X : Any, E : Any, C : Any, F : Any>

class AfsmPhasedTransitionScope<S, P, X, E, C, F>

Afsm.phased(state, event, entryPolicy)
```

The intended reducer shape is:

```kotlin
return Afsm.phased(
    state = state,
    event = event,
    entryPolicy = entryPolicy,
).run {
    when (state.phase) {
        ProductEditorPhase.EditingDraft -> reduceEditingDraft(event)
        ProductEditorPhase.ImageUploadInProgress -> reduceImageUploadInProgress(event)
        is ProductEditorPhase.ReviewSubmissionInProgress -> ignore("Review submission is running.")
    }
}
```

Inside the scope, feature code can call:

```kotlin
transitionTo(ProductEditorPhase.ImageUploadInProgress)
```

The phase entry policy then owns context normalization plus command/effect output.

## ProductEditor Compile Check

The test file `afsm-core/src/test/kotlin/afsm/core/AfsmPhasedCompileCheckTest.kt` models a ProductEditor-like flow.

It validates:

- `updateContext { ... }` keeps the current phase, updates context, and emits no outputs.
- `updateContext(update = ..., commands = ...)` keeps the phase while emitting a secondary command such as draft save.
- `transitionTo(Phase)` applies `AfsmPhaseEntryPolicy`.
- Entry policy can use the target phase payload, triggering event, and current context.
- Commands are emitted from the next context, not stale pre-entry context.

## Verification

Commands run:

```bash
./gradlew :afsm-core:compileTestKotlin
./gradlew :afsm-core:test
./gradlew :afsm-core:check :afsm-runtime:test
```

Result: all passed.

## Findings

- The core idea compiles cleanly without Android, AndroidX, coroutines, reflection, or code generation.
- `transitionTo(Phase)` is achievable if reducers run inside an `AfsmPhasedTransitionScope`.
- `PhaseEntryPolicy` successfully hides context assembly while keeping transition actions in `AfsmTransition`.
- `updateContext { ... }` needs a single-lambda overload because Kotlin trailing lambda syntax otherwise binds poorly when multiple defaulted parameters exist.
- Feature-local typealiases are still important; the raw `AfsmPhasedTransitionScope<S, P, X, E, C, F>` type is too verbose for repeated use.

## Remaining Concerns

- Final module placement is not settled: this spike lives in `afsm-core`, but `afsm-phased` remains an option before public release.
- `PhaseEntryPolicy` can emit effects in the spike; this should be revisited before API freeze.
- The reducer still needs explicit `Afsm.phased(state, event, entryPolicy).run { ... }` setup. A base class or helper may improve ergonomics, but could make the API feel more framework-like.
- Graph extraction has not been implemented yet.

## Current Verdict

The phased-state profile is viable enough to continue.

Next proof should apply it to the real ProductEditor sample and compare readability against the current sealed-state implementation.
