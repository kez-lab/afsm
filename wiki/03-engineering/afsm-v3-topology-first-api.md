---
title: Superseded Afsm v3 Phased State API
updated: 2026-05-09
---

# Superseded Afsm v3 Phased State API

This page is preserved for history and path continuity.

The canonical current v3 direction is now:

- [[afsm-v3-executable-dsl|Afsm v3 Executable DSL]]

## Superseded Direction

The previous v3 direction was:

```text
State = Phase + Context
+ transitionTo(Phase)
+ hidden PhaseEntryPolicy for context/command/effect entry rules
+ graph generation from Phase transitions
```

The real ProductEditor spike proved that separating `Phase` and `Context` is valuable, but `when + PhaseEntryPolicy` is not the best public authoring model.

## What We Learned

Useful ideas to keep:

- finite flow state should be separate from carried data,
- meaningful flow states such as `SavingDraft` and `DraftSaved` must stay visible as phases,
- actual data such as `ProductDraft` belongs in context,
- transition actions should remain outputs executed by the host,
- Android `ViewModel` should remain the lifecycle adapter.

Problems with this direction:

- graph generation depends on source-code inference,
- current phase/event scope is not structurally declared,
- `PhaseEntryPolicy` hides behavior that users expect near the state,
- context update, guard, command emission, and transition rules are split across files,
- users must follow conventions precisely or graph extraction and runtime behavior can drift.

## Current Replacement

Afsm v3 should use a scoped executable statechart DSL:

```kotlin
afsmMachine<ProductEditorPhase, ProductEditorContext, ProductEditorEvent, ProductEditorAction, ProductEditorEffect> {
    state(ProductEditorPhase.EditingDraft) {
        on<ProductEditorEvent.SubmitClicked> {
            guard({ context.draft.isValidForSubmission() }) {
                assign { copy(draft = draft.normalized(), errorMessage = null) }
                transitionTo(ProductEditorPhase.ImageUploadInProgress)
            }
            otherwise {
                assign { copy(errorMessage = draft.validationMessage()) }
            }
        }
    }

    state(ProductEditorPhase.ImageUploadInProgress) {
        onEnter {
            action(ProductEditorAction.StartImageUpload(context.draft))
        }
    }
}
```

This makes the machine definition both executable and graphable.

See [[afsm-v3-executable-dsl|Afsm v3 Executable DSL]] for the current plan.
