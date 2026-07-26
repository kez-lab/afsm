# Product Editor Walkthrough

Product Editor is the last sample in the reading path. Read it after Checkout,
when dynamic initial state, retries, stale-result handling, restoration, and
verb-named ViewModel boundaries are already familiar. It demonstrates a longer
flow and phase-owned cancellable work.

Do not use Product Editor as the first template for a new Afsm feature. Start
from Draft, Auth, or Checkout unless the feature really has multiple business
milestones and phase-owned work.

Source:

- generated graph: `sample-shop/build/generated/afsm/mmd/ProductEditorStateMachine.mmd`
  after `./gradlew :sample-shop:generateAfsmMmd`
- `sample-shop/src/main/kotlin/afsm/sample/shop/feature/editor/ProductEditorFlow.kt`
- `sample-shop/src/main/kotlin/afsm/sample/shop/feature/editor/ProductEditorStateMachine.kt`
- `sample-shop/src/main/kotlin/afsm/sample/shop/feature/editor/ProductEditorViewModel.kt`
- `sample-shop/src/main/kotlin/afsm/sample/shop/feature/editor/ProductEditorScreen.kt`
- `sample-shop/src/test/kotlin/afsm/sample/shop/feature/editor/ProductEditorStateMachineTest.kt`
- `sample-shop/src/test/kotlin/afsm/sample/shop/feature/editor/ProductEditorViewModelTest.kt`

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

## From Checkout to Product Editor

Keep these Checkout lessons:

- read the generated graph for topology, then machine source for exact rules,
  then tests for payload and ignored/invalid details,
- expose ViewModel verbs to Compose instead of machine events,
- convert command results into typed events at the Android boundary,
- keep UI-only navigation callbacks outside the machine.

Product Editor adds only the advanced parts:

- several business milestones: save draft, upload images, review, publish,
- a phase-owned image upload that must be cancelled when the upload phase exits,
- render state that hides phase payload details from the UI,
- topology tests that assert `invoke StartImageUpload` entry work and upload
  cancellation on phase exit.

What not to copy into a first feature:

- do not model every button as an event just because Product Editor has many
  workflow actions,
- do not add `invoke` for ordinary repository calls that can finish
  sequentially,
- do not route `Done` through the machine when completion is already durable
  state and the button only closes the UI surface.

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

Use `invoke` only when all of these are true:

- the work starts because the machine entered a phase,
- the work belongs to that phase and should stop when the phase exits,
- the host work can be cancelled cooperatively without inventing a fake domain
  failure.

Use ordinary `command` for sequential work that is not phase-cancelled. In this
sample, draft save, review submission, and publish are commands; only the local
image upload is an invocation.

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

Compose does not construct `ProductEditorEvent`. It calls ViewModel verbs and
renders `ProductEditorRenderState`, so internal phase payloads such as
`ReviewSubmissionInProgress(uploadToken)` stay out of UI code.

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
- topology and phase-owned invocation assertions,
- ViewModel cancellation and failure-boundary tests for the injected uploader.
