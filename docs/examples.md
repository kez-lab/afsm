# Examples

## Choosing an example

| Example | Use it to learn | Do not infer |
|---|---|---|
| Draft quickstart | minimal machine, command handler, verb-named ViewModel API | every form needs an FSM |
| Auth | validation branches and durable completion | Afsm requires app-wide MVI |
| Checkout | dynamic initial state, retry, stale-result safety, restoration | every navigation callback belongs in the machine |
| Product Editor | long flow and phase-owned cancellable work | every UI action needs an Event |

## Reading order

For a feature, read:

1. generated `.mmd` for topology,
2. `*StateMachine.kt` for exact rules,
3. `*StateMachineTest.kt` for edge-case proof,
4. `*ViewModel.kt` for Android work execution,
5. `*Screen.kt` for the UI boundary.

The role file is named `*Flow.kt`, not `*Contract.kt`, to describe product flow
without implying an MVI contract layer.

## UI boundary examples

Auth UI calls `selectMode`, `updateEmail`, and `submit`. Checkout calls `pay` and
`retry`. Product Editor calls editing and workflow verbs. Machine event types do
not leak into Compose screens.

## Graph role

The samples intentionally keep rules phase-local. Generated graphs restore the
whole-flow overview. Tests cover ignored, invalid, duplicate, stale, and payload
details that a diagram cannot express completely.

Start with [Getting started](getting-started.md), then choose the closest
walkthrough: [Auth](auth-walkthrough.md), [Checkout](checkout-walkthrough.md), or
[Product Editor](product-editor-walkthrough.md).
