---
title: Afsm ViewModel Integration
updated: 2026-07-17
---

# Afsm ViewModel Integration

## Responsibility Split

Plain Kotlin machine:

- transition validity,
- phase/data changes,
- command values,
- duplicate/stale/invalid policy.

Android `ViewModel`:

- `viewModelScope`,
- `StateFlow`,
- repositories, databases, SDKs, and timers,
- command execution and result-event mapping,
- `SavedStateHandle` conversion,
- verb-named UI methods.

Compose/UI:

- lifecycle-aware state collection,
- rendering,
- navigation and UI-only callbacks,
- focus, scroll, animation, sheet, and snackbar host state.

## Host Overloads

Static default:

```kotlin
val host = afsmHost(
    machine = authStateMachine,
    commandHandler = ...,
)
```

Runtime/restored initial state:

```kotlin
val host = afsmHost(
    machine = checkoutStateMachine,
    initialState = checkoutStateFromSavedState(...),
    commandHandler = ...,
)
```

The type split prevents a dynamic machine from pretending placeholder runtime
data is a usable default.

## Intended UI Shape

```kotlin
class CheckoutViewModel(...) : ViewModel() {
    private val host = afsmHost(...)

    val state: StateFlow<CheckoutState> = host.state

    fun pay() = host.dispatch(CheckoutEvent.PayClicked)
    fun retry() = host.dispatch(CheckoutEvent.RetryClicked)
}
```

Compose calls `pay()` and `retry()`, not `onEvent(CheckoutEvent)`. Event types
remain the internal language between ViewModel and machine.

Async command results remain typed events because that boundary needs explicit
correlation and serialized reduction.

## Completion Policy

- Authentication completion: `Authenticated(session)` state.
- Checkout completion: `Completed(orderId)` state.
- ProductEditor Done: direct route callback.

Routes may use `LaunchedEffect` on durable completion state to invoke navigation
callbacks. Repeated handling that is unsafe must be modeled explicitly in
feature state.

## Verification

ViewModel tests call the public verbs, advance the test dispatcher, assert
repository calls and resulting state, and avoid duplicating the full pure
machine test matrix.
