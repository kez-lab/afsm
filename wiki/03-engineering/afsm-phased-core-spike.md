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
- `afsm-core/src/main/kotlin/afsm/core/AfsmPhasedStateMachine.kt`
- `afsm-core/src/main/kotlin/afsm/core/Afsm.kt`

Added concepts:

```kotlin
interface AfsmPhasedState<S : Any, P : Any, X : Any>

data class AfsmPhaseEntry<X : Any, C : Any, F : Any>

fun interface AfsmPhaseEntryPolicy<P : Any, X : Any, E : Any, C : Any, F : Any>

class AfsmPhasedTransitionScope<S, P, X, E, C, F>

abstract class AfsmPhasedStateMachine<S, P, X, E, C, F>

Afsm.phased(state, event, entryPolicy)
```

The preferred reducer shape is now the `AfsmPhasedStateMachine` helper:

```kotlin
class ProductEditorStateMachine : AfsmPhasedStateMachine<
    ProductEditorState,
    ProductEditorPhase,
    ProductEditorContext,
    ProductEditorEvent,
    ProductEditorCommand,
    ProductEditorEffect,
>(
    entryPolicy = ProductEditorPhaseEntryPolicy(),
) {
    override fun ProductEditorTransitionScope.reduce(
        event: ProductEditorEvent,
    ): ProductEditorTransition {
        return when (state.phase) {
            ProductEditorPhase.EditingDraft -> reduceEditingDraft(event)
            ProductEditorPhase.ImageUploadInProgress -> reduceImageUploadInProgress(event)
            is ProductEditorPhase.ReviewSubmissionInProgress -> ignore("Review submission is running.")
        }
    }
}
```

`Afsm.phased(state, event, entryPolicy)` remains available as a low-level helper, but feature reducers should not need to call it directly in the normal path.

Before the base/helper, the reducer had to expose setup code:

```kotlin
Afsm.phased(state, event, entryPolicy).run {
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
- `AfsmPhasedStateMachine` hides scope creation so feature reducers can call `transitionTo(Phase)` directly.

## ProductEditor Sample Spike

The real `sample-shop` ProductEditor has now been refactored onto the phased-state helper.

Current sample shape:

```kotlin
data class ProductEditorState(
    override val phase: ProductEditorPhase,
    override val context: ProductEditorContext,
) : AfsmPhasedState<ProductEditorState, ProductEditorPhase, ProductEditorContext>
```

Reducer code is intentionally phase-first:

```kotlin
ProductEditorEvent.SaveDraftClicked -> transitionTo(ProductEditorPhase.SavingDraft)
ProductEditorEvent.SubmitClicked -> transitionTo(ProductEditorPhase.ImageUploadInProgress)
ProductEditorEvent.PublishClicked -> transitionTo(ProductEditorPhase.PublishInProgress)
```

The important correction from the first failed spike is that durable flow states remain phases. `SavingDraft` and `DraftSaved` are phases, not context flags. Context is reserved for actual screen data such as `ProductDraft` and validation errors.

`ProductEditorPhaseEntryPolicy` hides:

- draft form updates for editable phase re-entry,
- validation error placement,
- draft normalization before upload,
- review attempt increment,
- command creation for save/upload/review/publish phases.

## Verification

Commands run:

```bash
./gradlew :afsm-core:compileTestKotlin
./gradlew :afsm-core:test
./gradlew :afsm-core:check :afsm-runtime:test
./gradlew :sample-shop:testDebugUnitTest --tests 'afsm.sample.shop.feature.editor.ProductEditorStateMachineTest'
```

Result: all passed.

## Findings

- The core idea compiles cleanly without Android, AndroidX, coroutines, reflection, or code generation.
- `transitionTo(Phase)` is achievable if reducers run inside an `AfsmPhasedTransitionScope`.
- `AfsmPhasedStateMachine` makes the desired user-facing shape more plausible by creating the scope internally.
- `PhaseEntryPolicy` successfully hides context assembly while keeping transition actions in `AfsmTransition`.
- `updateContext { ... }` needs a single-lambda overload because Kotlin trailing lambda syntax otherwise binds poorly when multiple defaulted parameters exist.
- Feature-local typealiases are still important; the raw `AfsmPhasedTransitionScope<S, P, X, E, C, F>` type is too verbose for repeated use.
- For user-facing samples, prefer `transitionTo(Phase)` plus entry policy over direct `updateContext(..., commands = ...)`.
- Do not demote meaningful flow states into context fields merely to reduce the number of phase values.

## Remaining Concerns

- Final module placement is not settled: this spike lives in `afsm-core`, but `afsm-phased` remains an option before public release.
- `PhaseEntryPolicy` can emit effects in the spike; this should be revisited before API freeze.
- The base/helper reduces setup noise, but it introduces inheritance into the authoring model. This should be validated against Android developer expectations before API freeze.
- Graph extraction has not been implemented yet.
- The sample still exposes `ProductEditorState.phase` and `ProductEditorState.context` to UI code; a future helper may make state consumption less mechanical.

## Current Verdict

The phased-state profile is viable enough to continue, but only if the authoring style preserves explicit flow phases.

The next proof should focus on API naming and documentation so Android developers understand that phases describe the state diagram, while context stores data carried across phases.
