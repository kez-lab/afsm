---
title: Afsm v3 Executable DSL
updated: 2026-05-09
---

# Afsm v3 Executable DSL

This page is the canonical current direction for Afsm v3.

Afsm v3 should move from `when`-based helper APIs to a scoped executable statechart DSL.

The DSL must be the single source of truth for:

- runtime behavior,
- state diagram generation,
- transition tests,
- documentation examples.

Do not build a separate graph-only DSL beside a reducer implementation. That duplicates behavior and creates synchronization risk.

## Decision

Use a scoped executable DSL as the recommended v3 authoring model for complex Android FSM screens.

Keep the Android architecture boundary:

```text
Compose/View
-> ViewModel
-> AfsmHost
-> AfsmMachine DSL interpreter
-> StateFlow<UiState>
```

The `ViewModel` remains the Android lifecycle and business state holder adapter. The state machine remains plain Kotlin and Android-free.

## Reference Constraints

Afsm v3 is constrained by Android architecture and statechart practice.

Android:

- UI state should be produced by a state holder, commonly a `ViewModel` for screen-level business state.
- Events are transient inputs; state is durable output consumed by UI.
- UI-originated business events should be handled by the ViewModel/state holder.
- ViewModel-originated UI actions should usually become UI state; one-shot effects are exceptional.
- UI behavior logic such as navigation, snackbar display, focus, scroll, and animations stays in UI or UI-scoped state holders unless business logic requires otherwise.

References:

- [Android State holders and UI state](https://developer.android.com/topic/architecture/ui-layer/stateholders)
- [Android UI State production](https://developer.android.com/topic/architecture/ui-layer/state-production)
- [Android UI events](https://developer.android.com/topic/architecture/ui-layer/events)

Statechart references:

- [XState transitions](https://stately.ai/docs/transitions)
- [XState actions](https://stately.ai/docs/actions)
- [XState guards](https://stately.ai/docs/guards)
- [W3C SCXML](https://www.w3.org/TR/scxml/)
- [Square Workflow](https://square.github.io/workflow/)

## Why DSL

The `when + transitionTo(Phase) + PhaseEntryPolicy` spike proved a useful concept but exposed product-level problems:

- Graph generation depends on source-code inference.
- The current phase/event scope is not structurally declared.
- Entry policy hides behavior that users expect to see near the state.
- Context update, guard, command emission, and transition are still split across files.
- Users must follow conventions precisely or graph extraction and runtime behavior drift apart.

A scoped executable DSL makes the structure explicit:

```text
state scope
-> event handler
-> guard
-> assign context
-> emit transition action
-> transition target
```

This matches standard statechart vocabulary while keeping Android execution in `ViewModel`/`AfsmHost`.

## Core Concepts

| Concept | Meaning | Android Mapping |
|---|---|---|
| `State` | Full UI state exposed to Android | `StateFlow<S>` |
| `Phase` | Finite statechart node | Renderable business phase |
| `Context` | Extended state carried across phases | Form data, ids, retry count, validation error |
| `Event` | Something that happened | User input or command result |
| `Guard` | Boolean decision before transition | Validation, retry allowance, auth requirement |
| `Assign` | Context update | Immutable state data update |
| `Action` | Host-executed work emitted by transition or entry | Repository/use case call, timer, local DB write |
| `Effect` | UI-side one-shot output | Close screen, launch permission, optional navigation signal |
| `Entry` | Work when entering a phase | Start async command, clear error |
| `Exit` | Work when leaving a phase | Cancel timer, clear transient context |

Current implemented APIs use the word `Command`. For v3, the user-facing name should likely be `Action` or `TransitionAction`. This page uses `Action` for readability, but the final naming decision remains open.

## Proposed Authoring Shape

Target developer experience:

```kotlin
val ProductEditorMachine = afsmMachine<
    ProductEditorPhase,
    ProductEditorContext,
    ProductEditorEvent,
    ProductEditorAction,
    ProductEditorEffect,
> {
    initial(
        phase = ProductEditorPhase.EditingDraft,
        context = ProductEditorContext(),
    )

    state(ProductEditorPhase.EditingDraft) {
        on<ProductEditorEvent.TitleChanged> {
            assign {
                copy(
                    draft = draft.withTitle(event.value),
                    errorMessage = null,
                )
            }
        }

        on<ProductEditorEvent.SaveDraftClicked> {
            transitionTo(ProductEditorPhase.SavingDraft)
        }

        on<ProductEditorEvent.SubmitClicked> {
            if (context.draft.isValidForSubmission()) {
                transitionTo(ProductEditorPhase.ImageUploadInProgress)
            } else {
                assign {
                    copy(errorMessage = draft.validationMessage())
                }
            }
        }
    }

    state(ProductEditorPhase.SavingDraft) {
        onEnter {
            action(ProductEditorAction.SaveDraft(context.draft))
        }

        on<ProductEditorEvent.DraftSaved> {
            transitionTo(ProductEditorPhase.DraftSaved)
        }
    }
}
```

Important properties:

- `state(Phase)` creates a structural state scope.
- `on<Event>` creates a structural edge scope.
- `onEnter` and `onExit` are state-local and visible.
- `assign` mutates context immutably.
- `action` emits host-executed work.
- `effect` emits UI-side one-shot output.
- `transitionTo` changes phase.
- The same definition is executable and graphable.

## ProductEditor Pseudo Implementation

Types:

```kotlin
sealed interface ProductEditorPhase {
    data object EditingDraft : ProductEditorPhase
    data object SavingDraft : ProductEditorPhase
    data object DraftSaved : ProductEditorPhase
    data object ImageUploadInProgress : ProductEditorPhase

    data class ReviewSubmissionInProgress(
        val uploadToken: String,
    ) : ProductEditorPhase

    data class Rejected(
        val reason: String,
    ) : ProductEditorPhase

    data object Approved : ProductEditorPhase
    data object PublishInProgress : ProductEditorPhase

    data class Published(
        val productId: Long,
        val title: String,
    ) : ProductEditorPhase
}

data class ProductEditorContext(
    val draft: ProductDraft = ProductDraft(),
    val errorMessage: String? = null,
)
```

Machine:

```kotlin
val ProductEditorMachine = afsmMachine<
    ProductEditorPhase,
    ProductEditorContext,
    ProductEditorEvent,
    ProductEditorAction,
    ProductEditorEffect,
> {
    initial(ProductEditorPhase.EditingDraft, ProductEditorContext())

    state(ProductEditorPhase.EditingDraft) {
        on<ProductEditorEvent.TitleChanged> {
            assign { copy(draft = draft.withTitle(event.value), errorMessage = null) }
        }

        on<ProductEditorEvent.DescriptionChanged> {
            assign { copy(draft = draft.withDescription(event.value), errorMessage = null) }
        }

        on<ProductEditorEvent.PriceChanged> {
            assign { copy(draft = draft.withPriceText(event.value), errorMessage = null) }
        }

        on<ProductEditorEvent.SaveDraftClicked> {
            transitionTo(ProductEditorPhase.SavingDraft)
        }

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

    state(ProductEditorPhase.SavingDraft) {
        onEnter {
            action(ProductEditorAction.SaveDraft(context.draft))
        }

        on<ProductEditorEvent.DraftSaved> {
            transitionTo(ProductEditorPhase.DraftSaved)
        }
    }

    state(ProductEditorPhase.DraftSaved) {
        on<ProductEditorEvent.ContinueEditingClicked> {
            transitionTo(ProductEditorPhase.EditingDraft)
        }

        on<ProductEditorEvent.SubmitClicked> {
            guard({ context.draft.isValidForSubmission() }) {
                assign { copy(draft = draft.normalized(), errorMessage = null) }
                transitionTo(ProductEditorPhase.ImageUploadInProgress)
            }

            otherwise {
                assign { copy(errorMessage = draft.validationMessage()) }
                transitionTo(ProductEditorPhase.EditingDraft)
            }
        }

        on<ProductEditorEvent.TitleChanged> {
            assign { copy(draft = draft.withTitle(event.value), errorMessage = null) }
            transitionTo(ProductEditorPhase.EditingDraft)
        }
    }

    state(ProductEditorPhase.ImageUploadInProgress) {
        onEnter {
            action(ProductEditorAction.StartImageUpload(context.draft))
        }

        on<ProductEditorEvent.ImageUploadSucceeded> {
            assign {
                copy(
                    draft = draft.copy(reviewAttempt = draft.reviewAttempt + 1),
                    errorMessage = null,
                )
            }

            transitionTo(
                ProductEditorPhase.ReviewSubmissionInProgress(
                    uploadToken = event.uploadToken,
                ),
            )
        }

        on<ProductEditorEvent.ImageUploadFailed> {
            assign { copy(errorMessage = event.message) }
            transitionTo(ProductEditorPhase.EditingDraft)
        }
    }

    state<ProductEditorPhase.ReviewSubmissionInProgress> {
        onEnter {
            action(
                ProductEditorAction.StartReviewSubmission(
                    draft = context.draft,
                    uploadToken = phase.uploadToken,
                ),
            )
        }

        on<ProductEditorEvent.ReviewApproved> {
            transitionTo(ProductEditorPhase.Approved)
        }

        on<ProductEditorEvent.ReviewRejected> {
            transitionTo(ProductEditorPhase.Rejected(reason = event.reason))
        }
    }

    state<ProductEditorPhase.Rejected> {
        on<ProductEditorEvent.TitleChanged> {
            assign { copy(draft = draft.withTitle(event.value), errorMessage = null) }
        }

        on<ProductEditorEvent.DescriptionChanged> {
            assign { copy(draft = draft.withDescription(event.value), errorMessage = null) }
        }

        on<ProductEditorEvent.PriceChanged> {
            assign { copy(draft = draft.withPriceText(event.value), errorMessage = null) }
        }

        on<ProductEditorEvent.ContinueEditingClicked> {
            transitionTo(ProductEditorPhase.EditingDraft)
        }

        on<ProductEditorEvent.ResubmitClicked> {
            guard({ context.draft.isValidForSubmission() }) {
                assign { copy(draft = draft.normalized(), errorMessage = null) }
                transitionTo(ProductEditorPhase.ImageUploadInProgress)
            }

            otherwise {
                assign { copy(errorMessage = draft.validationMessage()) }
            }
        }
    }

    state(ProductEditorPhase.Approved) {
        on<ProductEditorEvent.ContinueEditingClicked> {
            transitionTo(ProductEditorPhase.EditingDraft)
        }

        on<ProductEditorEvent.PublishClicked> {
            transitionTo(ProductEditorPhase.PublishInProgress)
        }
    }

    state(ProductEditorPhase.PublishInProgress) {
        onEnter {
            action(ProductEditorAction.StartProductPublish(context.draft))
        }

        on<ProductEditorEvent.PublishSucceeded> {
            transitionTo(
                ProductEditorPhase.Published(
                    productId = event.productId,
                    title = context.draft.form.title.trim(),
                ),
            )
        }

        on<ProductEditorEvent.PublishFailed> {
            assign { copy(errorMessage = event.message) }
            transitionTo(ProductEditorPhase.Approved)
        }
    }

    state<ProductEditorPhase.Published> {
        on<ProductEditorEvent.DoneClicked> {
            effect(ProductEditorEffect.CloseEditor)
        }
    }
}
```

This started as pseudo-code. The first `afsm-core` spike now validates the core shape in executable Kotlin test code for `initial`, `state(phase)`, `state<PayloadPhase>`, `on<Event>`, `guard`, `otherwise`, `assign`, `onEnter`, `action`, `effect`, and `transitionTo`.

## Graph Output

The machine definition can produce Mermaid without KSP:

```mermaid
stateDiagram-v2
  EditingDraft --> SavingDraft: SaveDraftClicked / SaveDraft
  SavingDraft --> DraftSaved: DraftSaved
  DraftSaved --> EditingDraft: ContinueEditingClicked
  DraftSaved --> ImageUploadInProgress: SubmitClicked / StartImageUpload
  EditingDraft --> ImageUploadInProgress: SubmitClicked / StartImageUpload
  ImageUploadInProgress --> ReviewSubmissionInProgress: ImageUploadSucceeded / StartReviewSubmission
  ImageUploadInProgress --> EditingDraft: ImageUploadFailed
  ReviewSubmissionInProgress --> Approved: ReviewApproved
  ReviewSubmissionInProgress --> Rejected: ReviewRejected
  Rejected --> EditingDraft: ContinueEditingClicked
  Rejected --> ImageUploadInProgress: ResubmitClicked / StartImageUpload
  Approved --> EditingDraft: ContinueEditingClicked
  Approved --> PublishInProgress: PublishClicked / StartProductPublish
  PublishInProgress --> Published: PublishSucceeded
  PublishInProgress --> Approved: PublishFailed
```

Context-only `assign` operations can be omitted from the main graph or rendered as self-update annotations.

## Runtime Semantics

The v3 DSL should compile into an `AfsmMachine` definition.

Execution contract:

```kotlin
interface AfsmMachine<P : Any, X : Any, E : Any, A : Any, F : Any> {
    val initialSnapshot: AfsmSnapshot<P, X>

    fun transition(
        snapshot: AfsmSnapshot<P, X>,
        event: E,
    ): AfsmTransition<AfsmSnapshot<P, X>, A, F>
}

data class AfsmSnapshot<P : Any, X : Any>(
    val phase: P,
    val context: X,
)
```

`AfsmHost` can stay conceptually the same:

```text
dispatch(event)
-> serialize event
-> machine.transition(snapshot, event)
-> update StateFlow
-> execute actions sequentially
-> dispatch result events
-> emit effects
```

## Android ViewModel Shape

ViewModel usage should stay small:

```kotlin
class ProductEditorViewModel(
    private val productRepository: ProductRepository,
) : ViewModel() {
    private val host = afsmHost(
        machine = ProductEditorMachine,
        actionHandler = { action, dispatch ->
            when (action) {
                is ProductEditorAction.SaveDraft -> {
                    productRepository.saveDraft(action.draft)
                    dispatch(ProductEditorEvent.DraftSaved)
                }

                is ProductEditorAction.StartImageUpload -> {
                    val token = productRepository.uploadImages(action.draft)
                    dispatch(ProductEditorEvent.ImageUploadSucceeded(token))
                }
            }
        },
    )

    val state = host.state
    val effects = host.effects

    fun onEvent(event: ProductEditorEvent) {
        host.dispatch(event)
    }
}
```

The UI should still receive immutable state and callbacks. Do not pass the `ViewModel` deep into composables.

## API Design Rules

1. The DSL must be executable. No graph-only DSL.
2. The DSL must be Android-free.
3. `ViewModel` integration must remain an adapter, not a required base class.
4. The machine definition must expose enough topology metadata for graph generation.
5. Guards must be visible where branch decisions happen.
6. Entry actions must be state-local and testable.
7. Context updates must use explicit `assign`.
8. Async work must be emitted as actions, not launched from the DSL itself.
9. Effects should be rare and reserved for UI-side one-shot behavior.
10. Simple screens should keep ordinary ViewModel state instead of adopting Afsm ceremony.

## Implementation Plan

### Step 1: API Compile Spike

Add a new isolated core test file or small internal package that validates the DSL shape in Kotlin without changing sample-shop yet.

Target surface:

```kotlin
afsmMachine<P, X, E, A, F> { ... }
initial(phase, context)
state(phase) { ... }
state<PSubtype> { ... }
on<EventSubtype> { ... }
onEnter { ... }
guard(predicate) { ... }
otherwise { ... }
assign { ... }
transitionTo(phase)
action(action)
effect(effect)
```

Success criteria:

- ProductEditor pseudo-flow compiles in test code.
- Event subtype access works without unsafe casts in user code.
- Phase subtype access works for payload phases like `ReviewSubmissionInProgress`.
- Builder syntax is readable enough for Android developers.

Result on 2026-05-09:

- Added `AfsmMachine<P, X, E, A, F>` and `AfsmSnapshot<P, X>` to `afsm-core`.
- Added a minimal executable DSL in `afsm-core`: `afsmMachine`, `initial`, `state`, `on`, `onEnter`, `guard`, `otherwise`, `assign`, `transitionTo`, `action`, and `effect`.
- Added `AfsmExecutableDslCompileCheckTest` with a ProductEditor-like flow.
- Verified that event subtype access, typed payload phase access, guard fallback, entry action emission, and effect-only stayed transitions work in compiled Kotlin tests.
- Current limitation: the machine is executable but does not yet expose graph/topology metadata for Mermaid generation.

### Step 2: Interpreter Spike

Implement enough interpreter behavior to execute one event:

- find current state definition,
- find matching event handler,
- evaluate guards in declaration order,
- apply `assign` operations in order,
- apply one transition target,
- run exit/transition/entry outputs in deterministic order,
- return `AfsmTransition<AfsmSnapshot<P, X>, A, F>`.

Current spike status:

- Implemented current state lookup, event handler lookup, ordered `assign`, single `transitionTo`, target `onEnter`, command/action collection, effect collection, and `Stayed` versus `Transitioned` decisions.
- `onExit`, transition-level metadata, duplicate handler validation, and topology export remain unimplemented.

### Step 3: Graph Exporter

Add a plain Kotlin Mermaid exporter over the machine definition.

Success criteria:

- ProductEditor graph is generated from the machine object.
- No source scanning is required.
- `action(...)` labels can appear on edges or entry nodes.

### Step 4: ProductEditor Migration

Port ProductEditor from `when + PhaseEntryPolicy` to the executable DSL.

Success criteria:

- Existing ProductEditor state machine tests remain behaviorally equivalent.
- Android sample still builds.
- The DSL file reads more like the Mermaid state diagram than the current reducer file.

### Step 5: Public API Decision

Decide naming before public release:

- `Command` vs `Action` vs `TransitionAction`.
- `effect` delivery policy.
- whether `AfsmSnapshot` is public or hidden behind feature state mapping.
- whether DSL lives in `afsm-core` or an `afsm-dsl` module.

## Superseded Direction

The previous v3 direction was:

```text
State = Phase + Context
+ transitionTo(Phase)
+ hidden PhaseEntryPolicy
```

It remains useful as implementation background, but it is no longer the preferred public authoring model.

Reason:

- `PhaseEntryPolicy` hides too much from the user.
- `when` reducers require convention-heavy graph extraction.
- state/event topology is implicit in code structure rather than represented as data.

The phased helper may still be useful internally or as a lower-level API, but the public v3 recommendation should be the executable DSL.
