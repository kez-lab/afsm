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

`initial(...)` on an `AfsmDefaultMachine` and host-supplied state for a dynamic
`AfsmMachine` do not run `onEnter`. `initialPhase` only marks graph topology; it
does not create runtime data. This is intentional.

If initial state construction ran `onEnter`, restoring a state such as
`PaymentInProgress` could accidentally start another non-idempotent payment.

Prefer one of these patterns:

- Use a UI event such as `ScreenStarted` or `RetryClicked` to intentionally
  start work.
- Restore to a stable phase, then let the user retry.
- Store a request id and query status before deciding whether to retry.
- Make the command idempotent on the server side if automatic retry is required.

### Checkout Reference Policy

The sample Checkout flow persists three small keys:

- navigation `productId`,
- durable `completedOrderId`,
- `pendingPaymentRequestId` while the payment repository call is unresolved.

Restoration uses `completed > pending > fresh` priority. Completion restores a
durable `Completed(orderId)` phase without replaying its effect. A pending key
restores `PaymentStatusUnknown(requestId)`, which offers no automatic retry or
payment action. A fresh route restores `Idle` and intentionally dispatches
`ScreenEntered` to load the product.

This does not claim that a local request id can determine a remote payment
outcome. Production payment integrations still need backend idempotency and a
status query before resolving `PaymentStatusUnknown`.

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
effect { CheckoutEffect.NavigateToReceipt(orderId) }
```

if that is the only place the completed order is represented.

Good:

```kotlin
effect { CheckoutEffect.NavigateToReceipt(orderId) }
transitionTo(CheckoutPhase.Completed(orderId))
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

If a command handler does throw unexpectedly, the runtime does not create a
domain failure event for the feature. `AfsmCommandFailurePolicy.Throw` fails
the processing coroutine by default; `AfsmCommandFailurePolicy.Record` writes
an `AfsmDiagnostic` through the configured logger and lets later host work
continue. Use that policy for defensive logging, not for normal repository or
validation failures.

Diagnostics are `TypesOnly` by default: the logger receives a safe code,
decision category, fixed message, type names, and Afsm-owned metadata, not raw
domain values or exception details. `IncludeValues` is an explicit privacy-risk
opt-in and requires an application-owned redaction boundary.

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

## 7. Use Invocation for Phase-Owned Local Cancellation

Ordinary commands are sequential. A cancel command emitted from `onExit` cannot
interrupt the command ahead of it, so that pattern is not a valid cancellation
contract.

For a cooperative upload, timer, or polling loop owned by one phase, use:

```kotlin
val Upload = AfsmInvocationKey("editor/upload")

phase(EditorPhase.Uploading) {
    onEnter {
        invoke(Upload, label = "StartUpload") {
            EditorCommand.StartUpload(data.draft)
        }
    }

    on<EditorEvent.CancelUploadClicked> {
        transitionTo(EditorPhase.Editing)
    }
}
```

The runtime starts a tracked child job and cancels it on every phase exit or
host closure. `CancellationException` is not a command failure, and a cancelled
invocation cannot dispatch through its Afsm callback.

Cancellation is requested before target-phase invocation starts, but Afsm does
not wait for non-cancellable cleanup or a remote operation to finish before
publishing the next state.

This is local cooperative cancellation only. Include request ids so stale
results are ignored, use backend idempotency where required, and preserve an
application/SDK cancellation contract when work can outlive the coroutine.

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
    navigationProductId: Long,
    savedStateHandle: SavedStateHandle,
    paymentRepository: PaymentRepository,
) : ViewModel() {
    private val initialState = checkoutStateFromSavedState(
        savedStateHandle = savedStateHandle,
        navigationProductId = navigationProductId,
    )

    private val host = afsmHost(
        machine = checkoutStateMachine,
        initialState = initialState,
        commandHandler = { command: CheckoutCommand, dispatch ->
            when (command) {
                is CheckoutCommand.SubmitPayment -> {
                    savedStateHandle[CheckoutPendingPaymentRequestIdKey] =
                        command.requestId
                    paymentRepository.submit(command.payload).fold(
                        onSuccess = { receipt ->
                            savedStateHandle[CheckoutCompletedOrderIdKey] =
                                receipt.orderId
                            savedStateHandle.remove<Long>(
                                CheckoutPendingPaymentRequestIdKey,
                            )
                            dispatch(
                                CheckoutEvent.PaymentSucceeded(
                                    requestId = command.requestId,
                                    receipt = receipt,
                                ),
                            )
                        },
                        onFailure = { error ->
                            savedStateHandle.remove<Long>(
                                CheckoutPendingPaymentRequestIdKey,
                            )
                            dispatch(
                                CheckoutEvent.PaymentFailed(
                                    requestId = command.requestId,
                                    message = error.message ?: "Payment failed.",
                                ),
                            )
                        },
                    )
                }
            }
        },
    )

    val state: StateFlow<CheckoutState> = host.state
    val effects: Flow<CheckoutEffect> = host.effects

    init {
        if (initialState.phase == CheckoutPhase.Idle) {
            host.dispatch(CheckoutEvent.ScreenEntered)
        }
    }

    fun onEvent(event: CheckoutEvent) {
        host.dispatch(event)
    }
}
```

The ViewModel owns Android lifecycle integration. The machine owns flow rules.
The command handler owns side-effectful work and feature-owned persistence keys.
The Afsm core does not serialize machine state.
