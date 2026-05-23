# Restoration, Effect, and Command Policy

Afsm keeps the runtime small on purpose. It does not pretend every UI action,
process recreation case, and background operation can be made durable by the
state machine runtime alone.

This policy is the default for Android apps using Afsm.

## 1. Restore Durable State, Not Work

Persist or reconstruct state that describes the user's business flow:

- current phase,
- form data,
- selected ids,
- request or order ids,
- validation errors,
- completed result ids.

Do not restore an in-memory coroutine, network call, database transaction, or
collector. After process death, reconstruct the minimum state and decide whether
the work should be retried, queried from storage, or shown as recoverable.

Good restored state:

```kotlin
AfsmState(
    phase = CheckoutPhase.Completed(orderId = orderId),
    data = CheckoutData(cartId = cartId),
)
```

Risky restored state:

```kotlin
AfsmState(
    phase = CheckoutPhase.PaymentInProgress(requestId = oldRequestId),
    data = CheckoutData(...),
)
```

`PaymentInProgress` may be valid while the ViewModel is alive, but after process
death the app should usually reload the order/payment status or return to a
recoverable phase such as `PaymentReview`.

## 2. Treat `onEnter` as Runtime Entry, Not Restoration

`initial(...)` does not run `onEnter`. This is intentional.

If initial state construction ran `onEnter`, restoring a state such as
`PaymentInProgress` could accidentally start another non-idempotent payment.

Prefer one of these patterns:

- Use a UI event such as `ScreenStarted` or `RetryClicked` to intentionally
  start work.
- Restore to a stable phase, then let the user retry.
- Store a request id and query status before deciding whether to retry.
- Make the command idempotent on the server side if automatic retry is required.

## 3. Effects Are Best-Effort

Afsm effects are one-shot outputs exposed as `Flow<F>`.

Default effect delivery has no replay. A collector that starts late does not
receive old effects. This prevents accidental duplicate navigation, dialogs, or
snackbars after recreation.

Use effects for disposable UI behavior:

- close editor,
- optional navigation callback,
- fire-and-forget snackbar,
- haptic feedback.

Do not use effect-only output for required product progress.

Bad:

```kotlin
effect(CheckoutEffect.NavigateToReceipt(orderId))
```

if that is the only place the completed order is represented.

Good:

```kotlin
case {
    transitionTo(CheckoutPhase.Completed(orderId))
    effect(CheckoutEffect.NavigateToReceipt(orderId))
}
```

The state is durable. The effect is only a convenience for the currently active
UI.

## 4. Use State Plus Acknowledgement for Durable UI Work

If a snackbar, dialog, or navigation must survive a lifecycle gap, model it as
state and clear it with an acknowledgement event.

```kotlin
data class FormData(
    val pendingMessage: String? = null,
)

on<FormEvent.SubmitFailed> {
    updateData {
        copy(pendingMessage = "Submit failed")
    }
}

on<FormEvent.PendingMessageShown> {
    updateData {
        copy(pendingMessage = null)
    }
}
```

The UI observes `pendingMessage`, shows the snackbar, then dispatches
`PendingMessageShown`.

## 5. Commands Are Host Work, Not State

Commands are emitted by accepted transitions and executed by `AfsmHost` through
`AfsmCommandHandler`.

Use commands for:

- network calls,
- database writes,
- file upload,
- payment SDK calls,
- timers or polling owned by the host layer.

Commands should return typed events:

```kotlin
command(CheckoutCommand.SubmitPayment(requestId, payload))

// command handler
submitPayment(command).fold(
    onSuccess = { dispatch(CheckoutEvent.PaymentSucceeded(command.requestId, it.orderId)) },
    onFailure = { dispatch(CheckoutEvent.PaymentFailed(command.requestId, it.message)) },
)
```

Domain failure should become a domain event. It should not be thrown as an
exception from the command handler.

## 6. Use Request IDs for Stale Results

Long-running commands can finish after the user retries, cancels, or leaves the
phase. Include a request or correlation id in the command and result event.

```kotlin
data class PaymentInProgress(val requestId: String)

data class SubmitPayment(val requestId: String, val payload: PaymentPayload)

data class PaymentSucceeded(val requestId: String, val orderId: String)
```

Then ignore stale result events when their `requestId` no longer matches the
active phase.

## 7. Cancellation Is Explicit in v1

Afsm does not automatically cancel commands when the phase changes.

For v1, model cancellation explicitly:

- emit a cancel command from `onExit`,
- include request ids so stale results are ignored,
- make command handlers cooperative with coroutine cancellation,
- rethrow `CancellationException`.

Future versions may add invoked-service semantics, but the current runtime keeps
this policy explicit.

## 8. Command Queue Pressure Is a Modeling Signal

Afsm uses bounded event and command queues.

- `tryDispatch(event)` returns `false` if the external event queue is full.
- `dispatch(event)` throws if the external event queue rejects the event.
- Command result events use the same bounded event queue. If a command result
  event cannot be queued, the host throws `AfsmEventQueueOverflowException`
  instead of suspending the sequential command processor.
- If the host is already closed, command result events are dropped and logged.
  This is a normal Android lifecycle path when a `ViewModel` is cleared while
  command work is finishing.
- Accepted commands use a bounded queue and throw
  `AfsmCommandQueueOverflowException` if the queue is full.

If this happens, prefer:

- one coarse command instead of many tiny commands,
- smaller command bursts,
- increasing `commandQueueCapacity` only after confirming the burst is expected.

Do not treat command queue overflow as normal domain failure.

## 9. Recommended Android ViewModel Shape

```kotlin
class CheckoutViewModel(
    savedStateHandle: SavedStateHandle,
    paymentRepository: PaymentRepository,
) : ViewModel() {
    private val initialState = restoreCheckoutState(savedStateHandle)

    private val host = afsmHost(
        machine = CheckoutStateMachine,
        initialState = initialState,
        commandHandler = AfsmCommandHandler { command, dispatch ->
            when (command) {
                is CheckoutCommand.SubmitPayment -> {
                    val result = paymentRepository.submit(command.payload)
                    dispatch(result.toCheckoutEvent(command.requestId))
                }
            }
        },
    )

    val state: StateFlow<CheckoutState> = host.state
    val effects: Flow<CheckoutEffect> = host.effects

    fun onEvent(event: CheckoutEvent) {
        host.dispatch(event)
    }
}
```

The ViewModel owns Android lifecycle integration. The machine owns flow rules.
The command handler owns side-effectful work.
