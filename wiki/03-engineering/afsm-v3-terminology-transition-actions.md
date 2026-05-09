---
title: Afsm v3 Terminology and Transition Actions
updated: 2026-05-09
---

# Afsm v3 Terminology and Transition Actions

This document clarifies the `State` / `Event` / `Command` / `Effect` model after the ProductEditor discussion.

The main clarification is:

```text
Command is not another user event.
Command is a transition action emitted by the state machine and executed by the host.
```

The current implemented API calls this output `Command`. For v3 public API design, the clearer user-facing concept may be `Action` or `TransitionAction`.

## Mental Model

A state machine edge should be read as:

```text
CurrentState -- Event / TransitionAction --> NextState
```

Example:

```text
EditingDraft -- SubmitClicked / StartImageUpload --> ImageUploadInProgress
```

This says:

- the machine was in `EditingDraft`,
- `SubmitClicked` happened,
- the machine moved to `ImageUploadInProgress`,
- the transition requested `StartImageUpload`.

This is still a normal state machine model. Transition actions are common in UML state machines and Mealy-style machines. Afsm uses them because Android work such as use case calls, Room writes, timers, and network requests cannot be executed inside a pure transition function without coupling the state machine to runtime concerns.

## Directionality

| Term | Direction | Meaning | Example |
|---|---|---|---|
| `State` | machine holds | Current business phase and durable context | `ImageUploadInProgress(draft)` |
| `Event` | into machine | Something that happened | `SubmitClicked`, `ImageUploadSucceeded` |
| `TransitionAction` | out of machine to host | Work the host should start because of this transition | `StartImageUpload(draft)` |
| `Effect` | out of machine to UI | One-shot UI-side behavior | `CloseEditor`, `NavigateToCatalog` |

The key separation:

```text
Event = happened
TransitionAction = please do
Effect = please show/navigate/launch
```

## Phase, Context, And Entry Policy

The current v3 direction keeps the same distinction for Android screen state:

```text
State = Phase + Context
```

Where:

- `Phase` is the finite state-machine node used for diagrams.
- `Context` is the durable data carried across phases.
- scoped DSL blocks such as `state`, `on`, `guard`, `assign`, `onEnter`, and `action` own transition behavior.

Example edge:

```text
EditingDraft -- SubmitClicked / StartImageUpload --> ImageUploadInProgress
```

In the current v3 DSL direction, the machine definition should say:

```kotlin
state(ProductEditorPhase.EditingDraft) {
    on<ProductEditorEvent.SubmitClicked> {
        guard({ context.draft.isValidForSubmission() }) {
            assign { copy(draft = draft.normalized(), errorMessage = null) }
            transitionTo(ProductEditorPhase.ImageUploadInProgress)
        }
    }
}
```

The target phase can declare visible entry actions:

```kotlin
state(ProductEditorPhase.ImageUploadInProgress) {
    onEnter {
        action(ProductEditorAction.StartImageUpload(context.draft))
    }
}
```

This keeps graph-oriented flow visible without hiding behavior in a separate phase entry policy.

Canonical v3 page: [[afsm-v3-executable-dsl|Afsm v3 Executable DSL]].

## Naming Policy

Afsm should make the three axes visually distinct.

### State Names

State names should describe the business phase, not the work function.

Prefer:

- `EditingDraft`
- `ImageUploadInProgress`
- `ReviewSubmissionInProgress`
- `Rejected`
- `Approved`
- `PublishInProgress`
- `Published`

Avoid ambiguous verb-like state names when the matching action has the same meaning:

- `UploadingImages`
- `SubmittingForReview`
- `Publishing`

Those names are not wrong, but they make the state and action feel duplicated.

### Event Names

Event names should describe something that happened.

Prefer:

- `SubmitClicked`
- `ImageUploadSucceeded`
- `ImageUploadFailed`
- `ReviewApproved`
- `ReviewRejected`
- `PublishClicked`
- `PublishSucceeded`
- `PublishFailed`

UI events can be interaction-shaped. Async result events should usually be past-tense result-shaped.

### Transition Action Names

Transition action names should describe work that the host should start.

Prefer:

- `StartImageUpload`
- `StartReviewSubmission`
- `StartProductPublish`
- `PersistDraft`
- `StartPaymentAuthorization`
- `CancelPaymentAuthorization`

Avoid names that read like either state names or result events:

- `UploadImages`
- `SubmitForReview`
- `PublishProduct`

The verb `Start` is useful when the action begins async work and the machine expects a later result event.

### Effect Names

Effect names should describe one-shot UI behavior.

Prefer:

- `CloseEditor`
- `NavigateToCatalog`
- `ShowValidationMessage`
- `LaunchIdentityVerification`

Use effects sparingly. If the behavior can be durable state, prefer state.

## ProductEditor Rename

The previous ProductEditor sample worked, but its state and command names were too close.

| Previous state | Current state | Previous command | Current transition action |
|---|---|---|---|
| `UploadingImages` | `ImageUploadInProgress` | `UploadImages` | `StartImageUpload` |
| `SubmittingForReview` | `ReviewSubmissionInProgress` | `SubmitForReview` | `StartReviewSubmission` |
| `Publishing` | `PublishInProgress` | `PublishProduct` | `StartProductPublish` |

The revised graph reads better:

```text
EditingDraft -- SubmitClicked / StartImageUpload --> ImageUploadInProgress
ImageUploadInProgress -- ImageUploadSucceeded / StartReviewSubmission --> ReviewSubmissionInProgress
ReviewSubmissionInProgress -- ReviewRejected --> Rejected
Rejected -- ResubmitClicked / StartImageUpload --> ImageUploadInProgress
Approved -- PublishClicked / StartProductPublish --> PublishInProgress
```

This separates:

- the phase the UI can render,
- the event that entered the machine,
- the work the ViewModel host must run.

This rename has been applied to the sample-shop ProductEditor reference flow and verified with JVM tests plus Android CLI smoke testing.

The next v3 naming decision is whether the public API should keep `Command` for compatibility or switch the DSL-facing term to `Action` / `TransitionAction`.

## Why Not Put Commands Into State?

An alternative is:

```kotlin
data class ImageUploadInProgress(
    val draft: ProductDraft,
    val shouldStartUpload: Boolean = true,
)
```

This looks simpler because the ViewModel can observe state and start work from the state.

The problems:

- Durable state now contains one-shot execution intent.
- Process restoration can accidentally restart work.
- Recomposition or repeated collection can duplicate work unless the ViewModel tracks local "already started" flags.
- Tests must inspect state flags instead of transition outputs.
- The state name and flag together say the same thing twice.

`ImageUploadInProgress` should mean "the flow is in the upload phase." `StartImageUpload` should mean "this transition requested the upload operation now."

## Why Not Merge Transition Actions Into Events?

Another alternative is to remove commands/actions and emit events instead:

```text
EditingDraft -- SubmitClicked --> ImageUploadInProgress + Event.StartImageUpload
```

This loses directionality.

`SubmitClicked`, `ImageUploadSucceeded`, and `ReviewRejected` are inputs to the machine. `StartImageUpload` is output from the machine. They have opposite ownership:

- events are produced by UI or by completed host work,
- transition actions are produced by the machine and consumed by the host.

If they share one sealed type, the API still needs subcategories or conventions to prevent output events from being dispatched back into the machine by mistake.

## API Naming Implication

The current v2 generic type is:

```kotlin
AfsmTransition<S, C, F>
```

Where `C` means command.

That compiles and works, but public documentation must explain that `C` is a transition action channel.

Before public release, we should evaluate whether the user-facing API should rename this concept:

```text
Command -> Action
Command -> TransitionAction
commands -> actions
AfsmCommandHandler -> AfsmActionHandler
```

Recommendation:

- Keep the implemented v2 code stable until the current sample app validates behavior.
- Use the term "transition action" in documentation immediately.
- Run one naming-only spike before public API freeze to compare `Command` vs `Action` in real sample code.

## Executable DSL Direction

Earlier guidance preferred plain Kotlin `when` and then the phased-state helper before introducing a DSL.

Current recommendation has changed:

- Use the v2 `AfsmTransition<S, C, F>` runtime model as the lower-level engine.
- Make the public v3 authoring model a scoped executable DSL.
- Keep `state`, `on`, `guard`, `assign`, `onEnter`, `action`, `effect`, and `transitionTo` in one machine definition.
- Generate graphs from that machine definition, not from source-code inference over `when` branches.

Canonical v3 direction: [[afsm-v3-executable-dsl|Afsm v3 Executable DSL]].

## Graph Generation Implication

A graph renderer needs to know:

```text
FromPhase
Event
ToPhase
optional transition actions
optional effects
```

In unconstrained v2 reducer code, this topology is still hidden in executable Kotlin.

Better names make graphs understandable, but they do not make graphs automatically extractable. Current v3 direction is to make extraction possible through:

- structural DSL scopes for phase and event,
- explicit `transitionTo(...)` calls inside event handlers,
- explicit `action(...)` and `effect(...)` outputs,
- machine-definition metadata, without requiring KSP or source scanning for the first proof.

The naming cleanup should remain because any generated graph will only be useful if phase, event, and transition action names are already understandable.
