# Modeling Rules

## Use Afsm selectively

Prefer Afsm for multi-step, high-branching flows where validity depends on the
current phase. Prefer ordinary Android state production for simple screens.

## State is Phase plus Data

- `Phase` answers “which business step is active?”
- `Data` carries values that survive across phases.
- Keep Android objects out of both.
- Use typed phase payloads only when the value exists because that phase exists,
  such as `PaymentInProgress(requestId)`.

Avoid duplicating the same product data in many phase constructors.

## Event is input

Events represent user intent or results returned by command execution. Event
names describe what happened, not which UI widget produced it.

Machine events are an internal flow language. Android UI should usually call
verb-named ViewModel functions such as `pay()`, `retry()`, or
`updateEmail(value)`.

## Command is external work

Use a command for repository, database, timer, network, or SDK work that cannot
run in a pure reducer. Execute it in `ViewModel`, then dispatch a result event.

Prefer entry commands when entering a phase means work must begin:

```kotlin
phase(CheckoutPhase.ProductLoading) {
    onEnter {
        command("LoadProduct") {
            CheckoutCommand.LoadProduct(data.productId)
        }
    }
}
```

This connects work to a named business phase without making state observation
itself execute the work.

## UI actions do not need a fourth output type

- Persist business completion in state.
- Let routes react to durable completion state when navigation is required.
- Keep UI-originated UI-only actions as direct callbacks.
- Model an acknowledgement in state only when repeated handling would be unsafe.

This avoids a best-effort one-shot channel whose behavior becomes ambiguous
during collector gaps or recreation.

## Use case only for real conditional branches

Write unconditional behavior directly:

```kotlin
on<CheckoutEvent.ProductLoaded> {
    updateData { data, event -> data.copy(product = event.product) }
    transitionTo(CheckoutPhase.ProductReady)
}
```

Use `case` when one event has multiple named conditions that reviewers should
also see on the graph:

```kotlin
on<CheckoutEvent.PaymentSucceeded> {
    case("matching request", condition = { phase.requestId == event.requestId }) {
        transitionTo<CheckoutPhase.Completed> {
            CheckoutPhase.Completed(event.receipt.orderId)
        }
    }
    ignore(
        reason = "Stale payment result.",
        condition = { phase.requestId != event.requestId },
    )
}
```

## Decision meanings

| Decision | Meaning |
|---|---|
| `Transitioned` | The accepted rule changed phase |
| `Handled` | The accepted rule stayed in the same phase and may update data or emit command work |
| `Ignored` | The event is recognized and intentionally does nothing |
| `Invalid` | No valid rule exists for the current phase, or a rule explicitly rejects the event |

Use `Ignored` for expected duplicates and stale results. Use `Invalid` for
programming mistakes or impossible timing.

## Model stale async results explicitly

Put a request id in the active phase or state. Accept only a matching result and
ignore older results. Test both paths.

## Separate business and UI state

Machine state: payment progress, retry eligibility, selected product, completed
order id, validation that changes the flow.

UI state: focus, scroll position, animation progress, sheet expansion,
`SnackbarHostState`.

## Readability contract

- Graph: complete topology and named conditions.
- Machine: exact local data and work rules.
- Tests: executable edge-case proof.

Phase-scoped lambdas improve locality but reduce whole-file scanability. The
generated graph is the deliberate compensating artifact, not optional artwork.
