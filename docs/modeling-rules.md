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

## Phase vs Context

| Put it in | Use for | Example |
|---|---|---|
| `Phase` | finite node in the flow diagram | `EditingDraft`, `ImageUploadInProgress`, `Published` |
| `Context` | durable data carried across phases | form fields, selected ids, retry count, validation message |
| payload phase | data required only while that phase exists | `ReviewSubmissionInProgress(uploadToken)` |
| UI local state | rendering mechanics not part of business flow | focus, scroll, sheet animation, snackbar host |

If removing a value would change which events are valid, it is probably phase
or payload phase data. If it is merely data the current phase renders, it is
probably context.

## DSL Machine vs Reducer

Prefer `afsmMachine { ... }` for graphable complex flows. This gives you:

- executable transition rules,
- generated topology,
- `.mmd` diagrams,
- a consistent Android-facing `AfsmState<Phase, Context>` snapshot.

Use `AfsmReducer` directly only when the state shape is intentionally custom
or the screen is not graph-worthy. A direct reducer should be treated as an
escape hatch, not the primary onboarding style.

## Command Placement

| Emit command from | Use when |
|---|---|
| `onEnter` | work starts because a phase was entered |
| transition block | work belongs to one specific edge |
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

## stay vs ignore vs invalid

| API | Meaning |
|---|---|
| `transitionTo` | accepted phase change |
| `stay` | accepted event with no phase change |
| `otherwise` | fallback handled branch after guards fail |
| `ignore` | expected no-op event, such as duplicate submit while already submitting |
| `invalid` | event is a flow bug in this phase |

You do not need to enumerate every impossible event. Omitted handlers are
invalid by default. Add `ignore` only when the event is expected and harmless.

## First Reading Order

1. `README.md` minimal machine.
2. `sample-shop` Auth for the smallest real screen.
3. Generated `ProductEditorStateMachine.mmd` for the full graph.
4. `ProductEditorStateMachine.kt` for the implementation.
5. Checkout tests for stale command result handling.

## Naming Rules

- Phase names should describe the current business condition:
  `ImageUploadInProgress`, not `UploadImages`.
- Command names should describe host work:
  `StartImageUpload`, not `ImageUploadInProgress`.
- Event names should describe what happened:
  prefer `DraftSaveCompleted` over reusing the phase name `DraftSaved`.
- Keep `AfsmPhaseMachine` out of feature code unless writing library-level
  reference docs. Feature code should expose `AfsmMachine<State, Event,
  Command, Effect>`.
