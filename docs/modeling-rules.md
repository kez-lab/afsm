# Afsm Modeling Rules

Use this page before adding a new Afsm screen. It answers the questions that
usually decide whether a machine will feel useful or ceremonial.

After choosing the model shape, use
[restoration-effect-command-policy.md](restoration-effect-command-policy.md) for
Android restoration, effect, command failure, and queue pressure rules.

## Adoption Rule

Use Afsm when a screen has meaningful business phases:

- a multi-step transaction,
- async commands with success/failure results,
- retry or cancellation,
- invalid transitions that should be visible,
- state diagrams that help code review.

Do not use Afsm for ordinary data screens where `ViewModel + StateFlow` is
already direct: catalog lists, detail display, likes, review lists, and simple
loading/content/error views.

Before adding Afsm, answer these checks:

- Can a reviewer draw at least three meaningful business phases?
- Can some events be valid in one phase and invalid or ignored in another?
- Does the screen start async work whose result may arrive later?
- Does retry, cancellation, restoration, or stale result handling matter?
- Would a generated state diagram make code review easier?

If most answers are no, start with ordinary `ViewModel + StateFlow`.

## Phase vs Data

| Put it in | Use for | Example |
|---|---|---|
| `Phase` | finite node in the flow diagram | `EditingDraft`, `ImageUploadInProgress`, `Published` |
| `Data` | durable data carried across phases | form fields, selected ids, retry count, validation message |
| payload phase | data required only while that phase exists | `ReviewSubmissionInProgress(uploadToken)` |
| UI local state | rendering mechanics not part of business flow | focus, scroll, sheet animation, snackbar host |

If removing a value would change which events are valid, it is probably phase
or payload phase data. If it is merely data the current phase renders, it is
probably data.

Payload phase data should be minimal. Good payloads identify a specific phase
instance, for example `PaymentInProgress(requestId)`,
`ReviewSubmissionInProgress(uploadToken)`, or `Completed(orderId)`. Do not put
ordinary form fields, loaded product records, validation messages, or retry
counts in every phase constructor; keep them in `Data`.

## DSL Machine vs Reducer

Prefer `afsmMachine { ... }` for graphable complex flows. This gives you:

- executable transition rules,
- generated topology,
- `.mmd` diagrams,
- a consistent Android-facing `AfsmState<Phase, Data>` snapshot.

Use `AfsmReducer` directly only when the state shape is intentionally custom
or the screen is not graph-worthy. A direct reducer should be treated as an
escape hatch, not the primary onboarding style.

## Command Placement

| Emit command from | Use when |
|---|---|
| `onEnter` | work starts because a phase was entered |
| `case(...)` | work belongs to one specific event branch |
| `onExit` | cleanup/cancel work belongs to leaving a phase |

Example: entering `ImageUploadInProgress` can emit `StartImageUpload` from
`onEnter`. Clicking login can emit `Login` from the transition when the screen
does not need a distinct `SubmittingLogin` entry action.

Long-running commands should carry a request or correlation id when stale
results are possible.

Do not emit large bursts of tiny commands from a single transition. Afsm keeps
the command queue bounded and throws `AfsmCommandQueueOverflowException` if it
fills, so prefer one coarse command that owns its internal fan-out.

## State vs Effect

Effects are best-effort one-shot UI outputs. They are acceptable for disposable
UI behavior, but they are not durable state.

| Model as | Use for |
|---|---|
| state | completed checkout, logged-in/authenticated, persisted result id |
| effect | close editor, fire-and-forget snackbar, optional navigation callback |
| state plus acknowledgement | navigation or snackbar that must survive lifecycle gaps |

If losing the output across process death or a stopped lifecycle would break
the product flow, do not model it as effect-only.

## No-Transition Cases vs Ignore vs Invalid

| API | Meaning |
|---|---|
| `transitionTo` | accepted phase change |
| `case { updateData(...) }` | accepted event with no phase change |
| `updateData { ... }` | shorthand for data-only event handling |
| `ignore` | expected no-op event, such as duplicate submit while already submitting |
| omitted handler | invalid by default because the event is not valid in that phase |
| `invalid(reason)` | explicit invalid branch when a clearer diagnostic is worth writing |

You do not need to enumerate every impossible event. Omitted handlers are
invalid by default. Add `ignore` only when the event is expected and harmless.
Low-level reducers may still return `AfsmTransition.handled(...)`, but graphable
DSL examples should model no-transition handling by omitting `transitionTo(...)`
from the accepted case.

Use `case(label, condition = ...)` like a graphable `if` branch. The label
should describe the business condition, not the Kotlin expression. Prefer
`label = "valid draft"` with `condition = { data.canSubmitDraft() }` over
`label = "draft.form.validationError() == null"`.

## First Reading Order

1. `README.md` minimal machine.
2. [examples.md](examples.md) to choose the right sample.
3. `sample-shop` Auth for the smallest real screen.
4. [checkout-walkthrough.md](checkout-walkthrough.md) for loading, retry, stale results, and durable completion.
5. Generated `ProductEditorStateMachine.mmd` for the full graph.
6. `ProductEditorStateMachine.kt` for the implementation.

## Naming Rules

- Phase names should describe the current business condition:
  `ImageUploadInProgress`, not `UploadImages`.
- Command names should describe host work:
  `StartImageUpload`, not `ImageUploadInProgress`.
- Event names should describe what happened:
  prefer `DraftSaveCompleted` over reusing the phase name `DraftSaved`.
- Keep low-level `AfsmReducer` out of feature code unless you intentionally need
  a custom state shape. Graphable feature code should expose
  `AfsmMachine<State, Event, Command, Effect>`.
