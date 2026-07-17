# Product Editor Walkthrough

Product Editor demonstrates a longer flow and phase-owned cancellable work.

Source:

- `feature/editor/ProductEditorFlow.kt`
- `feature/editor/ProductEditorStateMachine.kt`
- `feature/editor/ProductEditorViewModel.kt`
- `feature/editor/ProductEditorScreen.kt`
- `ProductEditorStateMachineTest.kt`

## Flow

```text
EditingDraft -> SavingDraft -> DraftSaved
     |
     +-> ImageUploadInProgress -> ReviewSubmissionInProgress
                                      |             |
                                   Rejected      Approved
                                      |             |
                                      +--retry      +-> PublishInProgress -> Published
```

The generated graph is the recommended starting point because the source keeps
each rule inside its valid phase.

## Phase-owned invocation

Image upload begins when entering `ImageUploadInProgress` and is cancelled when
leaving it:

```kotlin
phase(ProductEditorPhase.ImageUploadInProgress) {
    onEnter {
        invoke(
            key = productEditorImageUploadInvocationKey,
            label = "StartImageUpload",
        ) {
            ProductEditorCommand.StartImageUpload(data.draft)
        }
    }

    on<ProductEditorEvent.CancelUploadClicked> {
        transitionTo(ProductEditorPhase.EditingDraft)
    }
}
```

Use `invoke` when work is owned by the active phase and must be cancelled on
exit. Use ordinary `command` for sequential work that is not phase-cancelled.

## Validation and retries

Submit and resubmit use named `case` branches because valid and invalid drafts
are graph-relevant alternatives. Editing events update data directly without
unnecessary `case` syntax.

## Android boundary

The ViewModel exposes feature verbs:

```kotlin
fun updateTitle(value: String)
fun updateDescription(value: String)
fun updatePrice(value: String)
fun saveDraft()
fun continueEditing()
fun submitForReview()
fun resubmitForReview()
fun publish()
fun cancelUpload()
```

Compose does not construct `ProductEditorEvent`.

## Why Done bypasses the machine

`Published(productId, title)` is already the durable business result. Pressing
Done only closes the current UI surface, so `ProductEditorScreen` calls the
route's `onDone` callback directly. Sending `DoneClicked` through the machine to
produce another UI output would add vocabulary without adding a business rule.

## Tests to read

- draft save and continuation,
- validation failure,
- upload success, failure, and cancellation,
- review rejection and resubmission,
- publish success and failure,
- render-state coverage for every phase,
- topology and phase-owned invocation assertions.
