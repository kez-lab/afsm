# Afsm Testing Guide

Afsm state machines should be tested as plain Kotlin behavior specs.

The machine is deterministic:

```text
current state + event -> next state + commands + effects + decision
```

That shape is the main reason to keep transition rules outside Android `ViewModel`.

Use `afsm-test` when you want transition tests to read like the machine's
behavior instead of repeated list and decision assertions:

```kotlin
dependencies {
    testImplementation("io.github.afsm:afsm-test:0.1.0-SNAPSHOT")
}
```

Import the helpers you use from `afsm.test`, for example
`assertTransitioned`, `assertPhase`, `assertCommands`, `assertIgnored`, and
`assertInvalid`.

## First Five Tests

### Valid transition

```kotlin
@Test
fun `SubmitClicked enters Submitting when form is valid`() {
    val result = machine.transition(
        state = editingState(validForm),
        event = SignupEvent.SubmitClicked,
    )

    result
        .assertTransitioned()
        .assertPhase(SignupPhase.Submitting)
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

    result.assertInvalid()
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

    result.assertCommands(SignupCommand.Submit(validForm))
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

    result.assertEffects(SignupEffect.OpenHome)
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
            data = CheckoutData(
                productId = 7,
                nextPaymentRequestId = 2,
            ),
        ),
        event = CheckoutEvent.PaymentFailed(
            requestId = 1,
            message = "late failure",
        ),
    )

    result
        .assertIgnored()
        .assertPhase(CheckoutPhase.PaymentInProgress(requestId = 2))
}
```

Use `Invalid` for programmer errors and impossible flow results. Use `Ignored`
for expected late/stale results that can happen in real asynchronous systems.

The helpers are ordinary Kotlin test assertions. They return the same
`AfsmTransition`, so tests can chain the decision, phase, state, command, and
effect expectations that matter for a scenario:

```kotlin
machine.transition(editingState(validForm), SignupEvent.SubmitClicked)
    .assertTransitioned()
    .assertPhase(SignupPhase.Submitting)
    .assertCommands(SignupCommand.Submit(validForm))
    .assertNoEffects()
```

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
