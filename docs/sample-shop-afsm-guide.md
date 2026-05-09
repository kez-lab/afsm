# Sample Shop Afsm Guide

This sample app validates Afsm against a small but realistic Android shopping app.

The point is not to force every screen into a finite state machine. The app uses Afsm only where flow correctness matters, and keeps ordinary data screens on standard ViewModel + Flow.

## Module

- Gradle module: `:sample-shop`
- Package: `afsm.sample.shop`
- App entry point: `sample-shop/src/main/kotlin/afsm/sample/shop/MainActivity.kt`
- Manual DI: `sample-shop/src/main/kotlin/afsm/sample/shop/app/ShopAppContainer.kt`
- Database: `sample-shop/src/main/kotlin/afsm/sample/shop/core/database/ShopDatabase.kt`

## Dependencies

The sample intentionally stays close to the Android/Kotlin stack:

- Afsm modules: `afsm-core`, `afsm-runtime`, `afsm-viewmodel`
- Compose Material 3
- Navigation Compose
- AndroidX Lifecycle Compose
- Room with KSP
- Kotlin coroutines

No external DI, networking, MVI framework, or reducer framework is used.

## Architecture Rule

Use Afsm for screens where state transitions are the behavior:

- signup/login submission
- payment loading, failure, retry, and completion

Use ordinary ViewModel state for screens where the behavior is mostly data observation:

- product list
- product detail
- likes
- product registration
- review registration
- review list

This split is intentional. A usable Android FSM library must make complex flows clearer without making simple screens ceremonial.

## Auth Flow

Files:

- `feature/auth/AuthContract.kt`
- `feature/auth/AuthStateMachine.kt`
- `feature/auth/AuthViewModel.kt`
- `feature/auth/AuthScreen.kt`

Flow:

```text
TextField/Button interaction
-> AuthEvent
-> AuthStateMachine.transition(state, event)
-> AuthState + AuthCommand/AuthEffect
-> AuthViewModel command handler
-> AuthRepository
-> AuthEvent.AuthSucceeded/AuthFailed
-> AuthState/AuthEffect.OpenCatalog
-> AuthRoute navigation callback
```

Contract:

- `AuthState` keeps visible form state, loading, and validation errors.
- `AuthEvent` models user input and command results.
- `AuthCommand` models async work the ViewModel must execute.
- `AuthEffect.OpenCatalog` models one-shot navigation after successful auth.

Key usage shape:

```kotlin
private val host = afsmHost(
    initialState = AuthState(),
    stateMachine = AuthStateMachine(),
    commandHandler = AfsmCommandHandler { command, dispatch ->
        // Execute repository work, then dispatch result events.
    },
)

val state = host.state
val effects = host.effects

fun onEvent(event: AuthEvent) {
    host.dispatch(event)
}
```

## Checkout Flow

Files:

- `feature/checkout/CheckoutContract.kt`
- `feature/checkout/CheckoutStateMachine.kt`
- `feature/checkout/CheckoutViewModel.kt`
- `feature/checkout/CheckoutScreen.kt`

Flow:

```text
CheckoutRoute enters with productId
-> CheckoutEvent.ScreenEntered
-> CheckoutCommand.LoadProduct
-> ProductRepository.findProduct(productId)
-> CheckoutEvent.ProductLoaded/ProductUnavailable
-> PayClicked
-> CheckoutCommand.SubmitPayment
-> PaymentRepository.submitPayment(...)
-> PaymentSucceeded/PaymentFailed
-> PaymentCompleted effect or retryable error state
```

Policy:

- Product loading and payment commands are serialized by `AfsmHost`.
- Duplicate enter/pay events are ignored while work is already running.
- First mock payment attempt fails for higher-priced products, so retry can be exercised.
- Payment completion is an effect because navigation is a UI-side one-shot action.

## Testing Policy

State machine tests are plain JVM tests:

- `AuthStateMachineTest`
- `CheckoutStateMachineTest`

These tests are executable specs. If a test fails, treat it as a product behavior signal first. Do not weaken tests just to make implementation pass.

Current verification:

```bash
./gradlew :sample-shop:testDebugUnitTest :sample-shop:assembleDebug --warning-mode all --no-daemon
```

## Early API Feedback

The current sample suggests:

- `AfsmTransition<S, C, F>` is verbose in raw signatures but acceptable with screen-local typealiases such as `AuthTransition`.
- `ViewModel.afsmHost(...)` reads naturally in real Android ViewModels.
- `Command` is easier to explain than making transition functions suspend.
- `Effect` should stay rare and focused on UI-side one-shot work.
- Simple CRUD/data screens should not be forced into Afsm.

Open follow-up:

- Add a `collectEffectsWithLifecycle` helper or documentation snippet.
- Add a concise README showing the `AuthViewModel` usage shape.
- Add instrumentation smoke tests once an emulator target is available.
