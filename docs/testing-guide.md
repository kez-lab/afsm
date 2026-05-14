# Afsm Testing Guide

Afsm state machines should be tested as plain Kotlin behavior specs.

The machine is deterministic:

```text
current state + event -> next state + commands + effects + decision
```

That shape is the main reason to keep transition rules outside Android `ViewModel`.

## First Five Tests

### Valid transition

```kotlin
@Test
fun `SubmitClicked enters Submitting when form is valid`() {
    val result = machine.transition(
        state = editingState(validForm),
        event = SignupEvent.SubmitClicked,
    )

    assertEquals(SignupPhase.Submitting, result.state.phase)
    assertEquals(AfsmDecision.Transitioned, result.decision)
}
```

### Invalid transition

```kotlin
@Test
fun `SubmitSucceeded before submit is invalid`() {
    val result = machine.transition(
        state = editingState(),
        event = SignupEvent.SubmitSucceeded,
    )

    assertIs<AfsmDecision.Invalid>(result.decision)
}
```

### Command emission

```kotlin
@Test
fun `entering Submitting emits submit command`() {
    val result = machine.transition(
        state = editingState(validForm),
        event = SignupEvent.SubmitClicked,
    )

    assertEquals(
        listOf(SignupCommand.Submit(validForm)),
        result.commands,
    )
}
```

### Effect emission

```kotlin
@Test
fun `completed signup emits navigation effect`() {
    val result = machine.transition(
        state = submittingState(),
        event = SignupEvent.SubmitSucceeded,
    )

    assertEquals(
        listOf(SignupEffect.OpenHome),
        result.effects,
    )
}
```

### Stale command result

Long-running commands can finish after the user has moved on. Prefer a
request/correlation id in both the command and the result event.

```kotlin
@Test
fun `stale payment failure is ignored`() {
    val result = machine.transition(
        state = checkoutState(
            productId = 7,
            phase = CheckoutPhase.PaymentInProgress(requestId = 2),
            context = CheckoutContext(
                productId = 7,
                nextPaymentRequestId = 2,
            ),
        ),
        event = CheckoutEvent.PaymentFailed(
            requestId = 1,
            message = "late failure",
        ),
    )

    assertIs<AfsmDecision.Ignored>(result.decision)
    assertEquals(CheckoutPhase.PaymentInProgress(requestId = 2), result.state.phase)
}
```

Use `Invalid` for programmer errors and impossible flow results. Use `Ignored`
for expected late/stale results that can happen in real asynchronous systems.

## ViewModel Tests

ViewModel tests should verify wiring, not duplicate every transition test.

Good ViewModel tests cover:

- the hosted machine receives events,
- command handlers call repositories/use cases,
- command results dispatch follow-up events,
- state is exposed as `StateFlow`,
- effects can be collected by the UI layer.

Use `runTest`, a test main dispatcher, and `Dispatchers.setMain/resetMain`
around `viewModelScope` code.

## Do Not Weaken Spec Tests

If a transition test fails, treat it as a behavior regression first.

Only change the test when the product behavior has intentionally changed. In
that case, update the relevant wiki/spec/docs before changing the assertion.
