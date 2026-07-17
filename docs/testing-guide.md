# Testing Guide

Treat tests as executable flow specification.

## Pure transition tests

The core equation is:

```text
current State + Event -> next State + Commands + command invocations + Decision
```

```kotlin
val result = checkoutMachine.transition(
    state = checkoutState(
        productId = product.id,
        phase = CheckoutPhase.ProductReady,
        data = CheckoutData(productId = product.id, product = product),
    ),
    event = CheckoutEvent.PayClicked,
)

result
    .assertTransitioned()
    .assertPhase(CheckoutPhase.PaymentInProgress(requestId = 1))
    .assertCommands(
        CheckoutCommand.SubmitPayment(requestId = 1, product = product),
    )
```

Available helpers include:

- `assertTransitioned()`
- `assertHandled(reason?)`
- `assertIgnored(reason?)`
- `assertInvalid(reason?)`
- `assertState(...)`, `assertPhase(...)`, `assertData(...)`
- `assertCommands(...)`, `assertNoCommands()`
- `assertCommandInvocations(...)`, `assertNoCommandInvocations()`
- `assertNoOutputs()` for no command work

## Minimum edge-case matrix

For a non-trivial flow, test:

1. primary success path,
2. validation failure without phase change,
3. repository failure,
4. retry,
5. duplicate user event while work is running,
6. stale async result,
7. event invalid in the current phase,
8. restored state that must not restart unsafe work.

## ViewModel wiring tests

Machine tests prove flow rules. ViewModel tests prove that commands call the
right Android dependency and return the right result event.

Call the public feature methods, not internal event construction:

```kotlin
viewModel.updateTitle("Plan")
viewModel.save()
mainDispatcher.scheduler.advanceUntilIdle()

assertEquals(listOf("Plan"), repository.savedTitles)
assertEquals(DraftPhase.Saved, viewModel.state.value.phase)
```

Do not duplicate every machine branch in ViewModel tests.

## Runtime contract tests

Runtime tests cover serialized FIFO event processing, state publication before
command execution, command ordering, queue overflow, command failure policy,
host closure, and phase-owned invocation cancellation.

## Graph tests

Assert important topology entries and generate `.mmd` files in verification.
The graph deliberately omits some no-op decisions, so it cannot replace
transition tests.

## TDD order

1. State the intended behavior in the canonical doc or spec.
2. Add or adjust a focused test.
3. Observe the expected failure when practical.
4. Implement the smallest production change.
5. Run the focused test.
6. Run the broader module and release gates.

Never weaken a failing test merely to make the build green. Classify it as an
implementation bug, harness issue, stale spec, or intentional requirement
change first.
