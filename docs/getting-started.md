# Afsm Getting Started

This is the first document to read when adding Afsm to an Android screen.

Afsm is for complex `ViewModel` flows. If the screen is only loading a list,
showing a detail page, toggling a like, or submitting a one-step form, ordinary
`ViewModel + StateFlow` is usually clearer.

## The 10-Minute Model

Use these words in this order:

| Term | Meaning |
|---|---|
| `Phase` | The node in the state diagram |
| `Data` | Durable screen data carried across phases |
| `State` | The Android-facing snapshot: `AfsmState<Phase, Data>` |
| `Event` | User input or command result |
| `Command` | Work the ViewModel host must execute |
| `Effect` | Optional UI one-shot output |

`Data` is not `android.content.Context`. It is normal immutable screen data,
such as form fields, loaded product, request id, or validation message.

## Build A New Machine

1. Write the phase list first.

```kotlin
sealed interface CheckoutPhase {
    data object Idle : CheckoutPhase
    data object ProductLoading : CheckoutPhase
    data object ProductReady : CheckoutPhase
    data class PaymentInProgress(val requestId: Long) : CheckoutPhase
    data object PaymentFailed : CheckoutPhase
    data class Completed(val orderId: Long) : CheckoutPhase
}
```

2. Put durable screen data in `Data`.

```kotlin
data class CheckoutData(
    val productId: Long,
    val product: Product? = null,
    val nextPaymentRequestId: Long = 0,
    val errorMessage: String? = null,
)

typealias CheckoutState = AfsmState<CheckoutPhase, CheckoutData>
```

3. Model user input and async results as events.

```kotlin
sealed interface CheckoutEvent {
    data object ScreenEntered : CheckoutEvent
    data class ProductLoaded(val product: Product) : CheckoutEvent
    data object PayClicked : CheckoutEvent
    data class PaymentSucceeded(val requestId: Long, val receipt: OrderReceipt) : CheckoutEvent
    data class PaymentFailed(val requestId: Long, val message: String) : CheckoutEvent
}
```

4. Model repository or timer work as commands.

```kotlin
sealed interface CheckoutCommand {
    data class LoadProduct(val productId: Long) : CheckoutCommand
    data class SubmitPayment(val requestId: Long, val product: Product) : CheckoutCommand
}
```

5. Write the machine in phase order.

```kotlin
private fun checkoutMachine(): CheckoutMachine = afsmMachine {
    initial(
        phase = CheckoutPhase.Idle,
        data = CheckoutData(productId = 0),
    )

    phase(CheckoutPhase.Idle) {
        on<CheckoutEvent.ScreenEntered> {
            transitionTo(CheckoutPhase.ProductLoading)
        }
    }

    phase(CheckoutPhase.ProductLoading) {
        onEnter {
            command(label = "LoadProduct") {
                CheckoutCommand.LoadProduct(data.productId)
            }
        }

        on<CheckoutEvent.ProductLoaded> {
            case {
                updateData { data, event ->
                    data.copy(product = event.product, errorMessage = null)
                }
                transitionTo(CheckoutPhase.ProductReady)
            }
        }
    }
}
```

Read an event block top to bottom:

```text
on<Event>
-> first matching case
-> updateData / command / effect
-> optional transitionTo
-> target onEnter if phase changed
```

If a case does not call `transitionTo(...)`, it handles the event without a
phase change. Use that for form text changes and validation errors.

## Host From ViewModel

The machine never calls repositories directly. The ViewModel host executes
commands and dispatches result events back into the machine.

```kotlin
private val host = afsmHost(
    machine = CheckoutStateMachine,
    initialState = checkoutState(productId = productId),
    commandHandler = AfsmCommandHandler { command, dispatch ->
        when (command) {
            is CheckoutCommand.LoadProduct -> {
                val product = repository.find(command.productId)
                dispatch(CheckoutEvent.ProductLoaded(product))
            }
            is CheckoutCommand.SubmitPayment -> {
                val receipt = paymentRepository.pay(command.product)
                dispatch(CheckoutEvent.PaymentSucceeded(command.requestId, receipt))
            }
        }
    },
)
```

Expose `host.state` as `StateFlow<State>`, expose `host.effects` only when the
feature has one-shot UI effects, and forward UI input to `host.dispatch(event)`.

## What To Read Next

1. [modeling-rules.md](modeling-rules.md) for when to use Afsm.
2. [auth-walkthrough.md](auth-walkthrough.md) for a small form.
3. [checkout-walkthrough.md](checkout-walkthrough.md) for retry and stale results.
4. [product-editor-walkthrough.md](product-editor-walkthrough.md) for a large transaction flow.
5. [graph-generation.md](graph-generation.md) only after the machine is useful.
