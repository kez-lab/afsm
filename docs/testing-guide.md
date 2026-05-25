# Afsm Testing Guide

Afsm state machines should be tested as plain Kotlin behavior specs.
For the first copy-pasteable Draft tests, start with
[getting-started.md](getting-started.md).

The machine is deterministic:

```text
current state + event -> next state + commands + effects + decision
```

That shape is the main reason to keep transition rules outside Android `ViewModel`.

Add the optional test helper artifact when transition tests start repeating raw
`result.state.phase`, `result.commands`, `result.effects`, and
`result.decision` assertions:

```kotlin
testImplementation("io.github.afsm:afsm-test:0.1.0-SNAPSHOT")
```

The helpers return the same transition, so assertions can be chained:

```kotlin
import afsm.test.assertCommands
import afsm.test.assertPhase
import afsm.test.assertTransitioned

result
    .assertTransitioned()
    .assertPhase(Phase.Saving)
    .assertCommands(Command.Save("Plan"))
```

## First Six Tests

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

### Command failure result

Expected domain failures should return to the machine as typed events from the
command handler. Test the resulting state transition like any other event.

```kotlin
@Test
fun `save failure returns to Editing with message`() {
    val result = DraftStateMachine.transition(
        state = DraftState(
            phase = DraftPhase.Saving,
            data = DraftData(title = "Plan"),
        ),
        event = DraftEvent.DraftSaveFailed("Network unavailable"),
    )

    result
        .assertTransitioned()
        .assertPhase(DraftPhase.Editing)
        .assertData(
            DraftData(
                title = "Plan",
                errorMessage = "Network unavailable",
            ),
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

```kotlin
testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
```

```kotlin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Test

class DraftViewModelTest {
    @Test
    fun saveClickedCallsRepositoryAndPublishesSavedState() = runTest {
        val mainDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(mainDispatcher)
        try {
            val repository = RecordingDraftRepository(Result.success(Unit))
            val viewModel = DraftViewModel(repository)

            viewModel.onEvent(DraftEvent.TitleChanged("Plan"))
            viewModel.onEvent(DraftEvent.SaveClicked)
            mainDispatcher.scheduler.advanceUntilIdle()

            assertEquals(listOf("Plan"), repository.savedTitles)
            assertEquals(DraftPhase.Saved, viewModel.state.value.phase)
        } finally {
            Dispatchers.resetMain()
        }
    }

    private class RecordingDraftRepository(
        private val result: Result<Unit>,
    ) : DraftRepository {
        val savedTitles = mutableListOf<String>()

        override suspend fun save(title: String): Result<Unit> {
            savedTitles += title
            return result
        }
    }
}
```

Keep the ViewModel test narrow. It should prove the host bridge and command
handler wiring, while the machine tests continue to own every transition branch.

## Do Not Weaken Spec Tests

If a transition test fails, treat it as a behavior regression first.

Only change the test when the product behavior has intentionally changed. In
that case, update the relevant wiki/spec/docs before changing the assertion.
