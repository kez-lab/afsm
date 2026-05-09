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

## ProductEditor Rename Candidate

The current ProductEditor sample works, but its state and command names are too close.

| Current state | Candidate state | Current command | Candidate transition action |
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

## Plain Kotlin Before DSL

The CEO feedback is that a DSL-like API can feel suspicious if it is not needed.

Current recommendation:

- Do not force a DSL yet.
- Keep the v2 plain Kotlin `when (state)` reducer API as the implemented engine.
- Improve terminology and sample naming first.
- Revisit graph generation after the state/action vocabulary is clear.

The next v3 design should prove whether graph generation can be supported without making normal state machine code feel unfamiliar.

## Graph Generation Implication

A graph renderer needs to know:

```text
FromState
Event
ToState
optional transition actions
optional effects
```

In v2 reducer code, this topology is still hidden in executable Kotlin.

Better names make graphs understandable, but they do not make graphs automatically extractable. Automatic graph extraction still needs one of these:

- declarative transition registration,
- explicit edge metadata beside the reducer,
- or a code generation/static analysis strategy.

The naming cleanup should happen first because any generated graph will only be useful if state, event, and action names are already understandable.
