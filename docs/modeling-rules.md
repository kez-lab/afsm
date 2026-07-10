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

## State vs Render State

Expose `StateFlow<State>` from the ViewModel so the Android integration remains
honest and testable. A Compose route may pass that state directly to a small
screen at first.

Add a feature-owned render state when UI code would otherwise:

- branch on several internal phases,
- infer button labels or enabled states from business phases,
- hide or reshape fields for terminal phases,
- duplicate the same `phase + data` interpretation in multiple composables.

Keep the mapping local:

```kotlin
val state by viewModel.state.collectAsStateWithLifecycle()

CheckoutScreen(
    state = state.toRenderState(),
    onPayClick = { viewModel.onEvent(CheckoutEvent.PayClicked) },
)
```

Do not add render state merely to wrap every `Data` property. The boundary is
useful when it keeps Compose rendering ordinary while the machine graph remains
precise.

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
| `onEnter { command(...) }` | short sequential work starts because a phase was entered |
| `case(...)` | short sequential work belongs to one specific event branch |
| `onExit { command(...) }` | short sequential cleanup belongs to leaving a phase |
| `onEnter { invoke(...) }` | long-running cooperative work is owned by the phase and must cancel on exit |

Example: entering `ImageUploadInProgress` invokes `StartImageUpload` from
`onEnter`; leaving the phase cancels it automatically. Clicking login can emit
an ordinary `Login` command from the transition when the screen does not need a
distinct `SubmittingLogin` entry action.

Do not emit a cancel command from `onExit` to interrupt an ordinary command: it
waits behind the active sequential command. Use `invoke` for local cooperative
cancellation. Long-running work should still carry a request or correlation id
when remote or non-cooperative stale results are possible.

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
In pure machine tests, assert important impossible events with `assertInvalid()`.

Runtime diagnostics intentionally expose type/category context rather than raw
state, event, command, reason, or throwable values by default. Do not put
sensitive data into `invalid(reason)` expecting it to reach production logs;
use typed domain state and application-owned safe telemetry when detailed
business context is required.

At runtime, the host applies `AfsmInvalidTransitionPolicy`; the default policy
throws so flow bugs are visible while developing.
Low-level reducers may still return `AfsmTransition.handled(...)`, but graphable
DSL examples should model no-transition handling by omitting `transitionTo(...)`
from the accepted case.

Use `case(label, condition = ...)` like a graphable `if` branch. The label
should describe the business condition, not the Kotlin expression. Prefer
`label = "valid draft"` with `condition = { data.canSubmitDraft() }` over
`label = "draft.form.validationError() == null"`.

When one event needs multiple actions, put them in the same `case { ... }`.
Top-level shorthand calls are complete alternatives, not a list of actions to
merge. For example, use `case { updateData(...); transitionTo(Phase.X) }` when
the event both changes data and changes phase.

## First Reading Order

1. [getting-started.md](getting-started.md) for the minimum Draft path:
   machine, JVM tests, ViewModel host, and one ViewModel wiring test.
2. [testing-guide.md](testing-guide.md) before expanding transition or
   ViewModel coverage.
3. [examples.md](examples.md) to choose the right sample.
4. [auth-walkthrough.md](auth-walkthrough.md) for the smallest real screen.
5. [checkout-walkthrough.md](checkout-walkthrough.md) for loading, retry, stale results, and durable completion.
6. Generated `ProductEditorStateMachine.mmd` for the full graph.
7. `ProductEditorStateMachine.kt` for the implementation.

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
