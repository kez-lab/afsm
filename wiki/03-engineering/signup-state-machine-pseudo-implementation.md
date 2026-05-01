---
title: Signup StateMachine Pseudo Implementation
updated: 2026-05-01
---

# Signup StateMachine Pseudo Implementation

## Purpose

This document stress-tests whether `AfsmTransition<S, C, F>` is too verbose when applied to the first reference flow: signup + identity verification + retry.

The code is intentionally close to real Kotlin, but it is still pseudo-implementation. Its goal is API ergonomics validation before implementation approval.

Related:

- [[afsm-public-api-draft|Afsm Public API Draft]]
- [[reference-flow-signup-identity-retry|Reference Flow - Signup Identity Retry]]

## Local Typealiases

The generic public API is verbose at declaration sites. The first mitigation is simple feature-local typealiases.

```kotlin
private typealias SignupTransition =
    AfsmTransition<SignupState, SignupCommand, SignupEffect>

private typealias SignupMachine =
    AfsmStateMachine<SignupState, SignupEvent, SignupCommand, SignupEffect>
```

With these aliases, the feature implementation reads acceptably:

```kotlin
class SignupStateMachine(
    private val inputValidator: SignupInputValidator,
    private val retryPolicy: IdentityRetryPolicy,
) : SignupMachine {

    override fun transition(
        state: SignupState,
        event: SignupEvent,
    ): SignupTransition {
        return when (state) {
            is SignupState.Editing -> reduceEditing(state, event)
            is SignupState.SubmittingAccount -> reduceSubmittingAccount(state, event)
            is SignupState.ReadyForIdentityVerification -> reduceReadyForIdentity(state, event)
            is SignupState.RequestingIdentityVerification -> reduceRequestingIdentity(state, event)
            is SignupState.AwaitingIdentityResult -> reduceAwaitingIdentityResult(state, event)
            is SignupState.IdentityVerificationFailed -> reduceIdentityFailed(state, event)
            is SignupState.CompletingSignup -> reduceCompletingSignup(state, event)
            is SignupState.Completed -> reduceCompleted(state, event)
        }
    }
}
```

## Domain Types

The state machine should depend on small domain value types, not Android types.

```kotlin
data class SignupInput(
    val name: String = "",
    val email: String = "",
    val password: String = "",
)

data class FieldErrors(
    val name: String? = null,
    val email: String? = null,
    val password: String? = null,
) {
    val hasErrors: Boolean
        get() = name != null || email != null || password != null
}

@JvmInline value class SignupSessionId(val value: String)
@JvmInline value class VerificationRequestId(val value: String)
@JvmInline value class VerificationLaunchToken(val value: String)
@JvmInline value class AccountId(val value: String)

sealed interface SignupFailureReason {
    data object Network : SignupFailureReason
    data object EmailAlreadyExists : SignupFailureReason
    data object Unknown : SignupFailureReason
}

sealed interface IdentityFailureReason {
    data object Network : IdentityFailureReason
    data object Timeout : IdentityFailureReason
    data object UserCancelled : IdentityFailureReason
    data object ProviderRejected : IdentityFailureReason
    data object RetryLimitReached : IdentityFailureReason
    data object Unknown : IdentityFailureReason
}

sealed interface ExternalIdentityResult {
    data object Success : ExternalIdentityResult
    data object Cancelled : ExternalIdentityResult
    data class Failure(val reason: IdentityFailureReason) : ExternalIdentityResult
}
```

## State, Event, Command, Effect

This mirrors the reference flow document with one refinement: messages are kept out of `SignupEffect` for the initial pseudo-implementation. The only effect is launching identity verification.

```kotlin
sealed interface SignupState {
    data class Editing(
        val input: SignupInput = SignupInput(),
        val fieldErrors: FieldErrors = FieldErrors(),
    ) : SignupState

    data class SubmittingAccount(
        val input: SignupInput,
    ) : SignupState

    data class ReadyForIdentityVerification(
        val sessionId: SignupSessionId,
        val input: SignupInput,
        val retryCount: Int = 0,
    ) : SignupState

    data class RequestingIdentityVerification(
        val sessionId: SignupSessionId,
        val input: SignupInput,
        val retryCount: Int,
    ) : SignupState

    data class AwaitingIdentityResult(
        val sessionId: SignupSessionId,
        val verificationRequestId: VerificationRequestId,
        val input: SignupInput,
        val retryCount: Int,
    ) : SignupState

    data class IdentityVerificationFailed(
        val sessionId: SignupSessionId,
        val input: SignupInput,
        val reason: IdentityFailureReason,
        val retryCount: Int,
        val canRetry: Boolean,
    ) : SignupState

    data class CompletingSignup(
        val sessionId: SignupSessionId,
        val input: SignupInput,
    ) : SignupState

    data class Completed(
        val accountId: AccountId,
    ) : SignupState
}

sealed interface SignupEvent {
    data class NameChanged(val value: String) : SignupEvent
    data class EmailChanged(val value: String) : SignupEvent
    data class PasswordChanged(val value: String) : SignupEvent
    data object SubmitSignupRequested : SignupEvent

    data class SignupSessionCreated(val sessionId: SignupSessionId) : SignupEvent
    data class SignupSessionCreationFailed(val reason: SignupFailureReason) : SignupEvent

    data object StartIdentityVerificationRequested : SignupEvent
    data class IdentityVerificationRequestCreated(
        val verificationRequestId: VerificationRequestId,
        val launchToken: VerificationLaunchToken,
    ) : SignupEvent
    data class IdentityVerificationRequestFailed(val reason: IdentityFailureReason) : SignupEvent

    data class ExternalIdentityResultReceived(val result: ExternalIdentityResult) : SignupEvent
    data object IdentityPollingTick : SignupEvent
    data object IdentityVerificationSucceeded : SignupEvent
    data class IdentityVerificationFailed(val reason: IdentityFailureReason) : SignupEvent

    data object RetryIdentityVerificationRequested : SignupEvent
    data object EditInputRequested : SignupEvent
    data object CancelRequested : SignupEvent

    data class SignupCompleted(val accountId: AccountId) : SignupEvent
    data class SignupCompletionFailed(val reason: SignupFailureReason) : SignupEvent
}

sealed interface SignupCommand {
    data class CreateSignupSession(val input: SignupInput) : SignupCommand
    data class RequestIdentityVerification(val sessionId: SignupSessionId) : SignupCommand
    data class CheckIdentityVerificationStatus(
        val sessionId: SignupSessionId,
        val verificationRequestId: VerificationRequestId,
    ) : SignupCommand
    data class CompleteSignup(val sessionId: SignupSessionId) : SignupCommand
    data class CancelSignupSession(val sessionId: SignupSessionId) : SignupCommand
}

sealed interface SignupEffect {
    data class LaunchIdentityVerification(
        val launchToken: VerificationLaunchToken,
    ) : SignupEffect
}
```

## Reducer Implementation

### Editing

```kotlin
private fun reduceEditing(
    state: SignupState.Editing,
    event: SignupEvent,
): SignupTransition {
    return when (event) {
        is SignupEvent.NameChanged -> {
            Afsm.transitionTo(
                state = state.copy(
                    input = state.input.copy(name = event.value),
                    fieldErrors = state.fieldErrors.copy(name = null),
                ),
            )
        }

        is SignupEvent.EmailChanged -> {
            Afsm.transitionTo(
                state = state.copy(
                    input = state.input.copy(email = event.value),
                    fieldErrors = state.fieldErrors.copy(email = null),
                ),
            )
        }

        is SignupEvent.PasswordChanged -> {
            Afsm.transitionTo(
                state = state.copy(
                    input = state.input.copy(password = event.value),
                    fieldErrors = state.fieldErrors.copy(password = null),
                ),
            )
        }

        SignupEvent.SubmitSignupRequested -> {
            val errors = inputValidator.validate(state.input)
            if (errors.hasErrors) {
                Afsm.transitionTo(
                    state = state.copy(fieldErrors = errors),
                )
            } else {
                Afsm.transitionTo(
                    state = SignupState.SubmittingAccount(input = state.input),
                    commands = listOf(SignupCommand.CreateSignupSession(state.input)),
                )
            }
        }

        SignupEvent.CancelRequested -> {
            Afsm.ignore(state, "Cancel in Editing is handled by UI navigation")
        }

        else -> Afsm.invalid(
            state = state,
            reason = "Received ${event::class.simpleName} while editing signup input",
        )
    }
}
```

### Submitting Account

```kotlin
private fun reduceSubmittingAccount(
    state: SignupState.SubmittingAccount,
    event: SignupEvent,
): SignupTransition {
    return when (event) {
        is SignupEvent.SignupSessionCreated -> {
            Afsm.transitionTo(
                state = SignupState.ReadyForIdentityVerification(
                    sessionId = event.sessionId,
                    input = state.input,
                ),
            )
        }

        is SignupEvent.SignupSessionCreationFailed -> {
            Afsm.transitionTo(
                state = SignupState.Editing(
                    input = state.input,
                    fieldErrors = event.reason.toFieldErrors(),
                ),
            )
        }

        SignupEvent.SubmitSignupRequested -> {
            Afsm.ignore(state, "Signup submission already in progress")
        }

        SignupEvent.CancelRequested -> {
            Afsm.transitionTo(
                state = SignupState.Editing(input = state.input),
            )
        }

        else -> Afsm.invalid(
            state = state,
            reason = "Received ${event::class.simpleName} while creating signup session",
        )
    }
}
```

### Ready For Identity Verification

```kotlin
private fun reduceReadyForIdentity(
    state: SignupState.ReadyForIdentityVerification,
    event: SignupEvent,
): SignupTransition {
    return when (event) {
        SignupEvent.StartIdentityVerificationRequested -> {
            Afsm.transitionTo(
                state = SignupState.RequestingIdentityVerification(
                    sessionId = state.sessionId,
                    input = state.input,
                    retryCount = state.retryCount,
                ),
                commands = listOf(
                    SignupCommand.RequestIdentityVerification(state.sessionId),
                ),
            )
        }

        SignupEvent.EditInputRequested -> {
            Afsm.transitionTo(
                state = SignupState.Editing(input = state.input),
                commands = listOf(SignupCommand.CancelSignupSession(state.sessionId)),
            )
        }

        SignupEvent.CancelRequested -> {
            Afsm.transitionTo(
                state = state,
                commands = listOf(SignupCommand.CancelSignupSession(state.sessionId)),
                decision = AfsmDecision.Ignored("UI owns actual exit navigation"),
            )
        }

        else -> Afsm.invalid(
            state = state,
            reason = "Received ${event::class.simpleName} before identity verification starts",
        )
    }
}
```

This exposes the first API issue: the current `Afsm.transitionTo` draft does not include a `decision` parameter. Either `transitionTo` needs a `decision` override, or cancellation should use `Afsm.ignore(..., commands = ...)`, which also does not exist in the draft. This suggests the builder API needs adjustment.

### Requesting Identity Verification

```kotlin
private fun reduceRequestingIdentity(
    state: SignupState.RequestingIdentityVerification,
    event: SignupEvent,
): SignupTransition {
    return when (event) {
        is SignupEvent.IdentityVerificationRequestCreated -> {
            Afsm.transitionTo(
                state = SignupState.AwaitingIdentityResult(
                    sessionId = state.sessionId,
                    verificationRequestId = event.verificationRequestId,
                    input = state.input,
                    retryCount = state.retryCount,
                ),
                effects = listOf(
                    SignupEffect.LaunchIdentityVerification(event.launchToken),
                ),
            )
        }

        is SignupEvent.IdentityVerificationRequestFailed -> {
            val canRetry = retryPolicy.canRetry(
                reason = event.reason,
                retryCount = state.retryCount,
            )
            Afsm.transitionTo(
                state = SignupState.IdentityVerificationFailed(
                    sessionId = state.sessionId,
                    input = state.input,
                    reason = event.reason,
                    retryCount = state.retryCount,
                    canRetry = canRetry,
                ),
            )
        }

        SignupEvent.RetryIdentityVerificationRequested -> {
            Afsm.ignore(state, "Identity verification request already in progress")
        }

        SignupEvent.CancelRequested -> {
            Afsm.transitionTo(
                state = SignupState.ReadyForIdentityVerification(
                    sessionId = state.sessionId,
                    input = state.input,
                    retryCount = state.retryCount,
                ),
            )
        }

        else -> Afsm.invalid(
            state = state,
            reason = "Received ${event::class.simpleName} while requesting identity verification",
        )
    }
}
```

### Awaiting Identity Result

```kotlin
private fun reduceAwaitingIdentityResult(
    state: SignupState.AwaitingIdentityResult,
    event: SignupEvent,
): SignupTransition {
    return when (event) {
        is SignupEvent.ExternalIdentityResultReceived -> {
            when (event.result) {
                ExternalIdentityResult.Success -> {
                    Afsm.transitionTo(
                        state = SignupState.CompletingSignup(
                            sessionId = state.sessionId,
                            input = state.input,
                        ),
                        commands = listOf(SignupCommand.CompleteSignup(state.sessionId)),
                    )
                }

                ExternalIdentityResult.Cancelled -> {
                    identityFailure(
                        state = state,
                        reason = IdentityFailureReason.UserCancelled,
                    )
                }

                is ExternalIdentityResult.Failure -> {
                    identityFailure(
                        state = state,
                        reason = event.result.reason,
                    )
                }
            }
        }

        SignupEvent.IdentityPollingTick -> {
            Afsm.transitionTo(
                state = state,
                commands = listOf(
                    SignupCommand.CheckIdentityVerificationStatus(
                        sessionId = state.sessionId,
                        verificationRequestId = state.verificationRequestId,
                    ),
                ),
            )
        }

        SignupEvent.IdentityVerificationSucceeded -> {
            Afsm.transitionTo(
                state = SignupState.CompletingSignup(
                    sessionId = state.sessionId,
                    input = state.input,
                ),
                commands = listOf(SignupCommand.CompleteSignup(state.sessionId)),
            )
        }

        is SignupEvent.IdentityVerificationFailed -> {
            identityFailure(
                state = state,
                reason = event.reason,
            )
        }

        SignupEvent.CancelRequested -> {
            Afsm.transitionTo(
                state = SignupState.ReadyForIdentityVerification(
                    sessionId = state.sessionId,
                    input = state.input,
                    retryCount = state.retryCount,
                ),
                commands = listOf(SignupCommand.CancelSignupSession(state.sessionId)),
            )
        }

        else -> Afsm.ignore(
            state = state,
            reason = "Ignored ${event::class.simpleName} while awaiting identity result",
        )
    }
}
```

### Identity Failed

```kotlin
private fun reduceIdentityFailed(
    state: SignupState.IdentityVerificationFailed,
    event: SignupEvent,
): SignupTransition {
    return when (event) {
        SignupEvent.RetryIdentityVerificationRequested -> {
            if (!state.canRetry) {
                Afsm.invalid(
                    state = state,
                    reason = "Retry requested after retry limit reached",
                )
            } else {
                val nextRetryCount = state.retryCount + 1
                Afsm.transitionTo(
                    state = SignupState.RequestingIdentityVerification(
                        sessionId = state.sessionId,
                        input = state.input,
                        retryCount = nextRetryCount,
                    ),
                    commands = listOf(
                        SignupCommand.RequestIdentityVerification(state.sessionId),
                    ),
                )
            }
        }

        SignupEvent.EditInputRequested -> {
            Afsm.transitionTo(
                state = SignupState.Editing(input = state.input),
                commands = listOf(SignupCommand.CancelSignupSession(state.sessionId)),
            )
        }

        SignupEvent.CancelRequested -> {
            Afsm.transitionTo(
                state = state,
                commands = listOf(SignupCommand.CancelSignupSession(state.sessionId)),
            )
        }

        else -> Afsm.ignore(
            state = state,
            reason = "Ignored ${event::class.simpleName} after identity failure",
        )
    }
}
```

### Completing Signup

```kotlin
private fun reduceCompletingSignup(
    state: SignupState.CompletingSignup,
    event: SignupEvent,
): SignupTransition {
    return when (event) {
        is SignupEvent.SignupCompleted -> {
            Afsm.transitionTo(
                state = SignupState.Completed(accountId = event.accountId),
            )
        }

        is SignupEvent.SignupCompletionFailed -> {
            Afsm.transitionTo(
                state = SignupState.ReadyForIdentityVerification(
                    sessionId = state.sessionId,
                    input = state.input,
                ),
            )
        }

        SignupEvent.CancelRequested -> {
            Afsm.ignore(state, "Cannot cancel while final signup completion is in progress")
        }

        else -> Afsm.ignore(
            state = state,
            reason = "Ignored ${event::class.simpleName} while completing signup",
        )
    }
}
```

### Completed

```kotlin
private fun reduceCompleted(
    state: SignupState.Completed,
    event: SignupEvent,
): SignupTransition {
    return when (event) {
        SignupEvent.CancelRequested -> {
            Afsm.ignore(state, "Already completed")
        }

        else -> Afsm.ignore(
            state = state,
            reason = "Ignored ${event::class.simpleName} after completion",
        )
    }
}
```

## Helper Functions

```kotlin
private fun identityFailure(
    state: SignupState.AwaitingIdentityResult,
    reason: IdentityFailureReason,
): SignupTransition {
    val canRetry = retryPolicy.canRetry(
        reason = reason,
        retryCount = state.retryCount,
    )

    return Afsm.transitionTo(
        state = SignupState.IdentityVerificationFailed(
            sessionId = state.sessionId,
            input = state.input,
            reason = reason,
            retryCount = state.retryCount,
            canRetry = canRetry,
        ),
    )
}

private fun SignupFailureReason.toFieldErrors(): FieldErrors {
    return when (this) {
        SignupFailureReason.EmailAlreadyExists -> FieldErrors(
            email = "Email is already registered",
        )
        SignupFailureReason.Network,
        SignupFailureReason.Unknown -> FieldErrors()
    }
}
```

## API Ergonomics Findings

### Finding 1: Raw generic type is too verbose at feature boundary

This is noisy:

```kotlin
AfsmTransition<SignupState, SignupCommand, SignupEffect>
AfsmStateMachine<SignupState, SignupEvent, SignupCommand, SignupEffect>
```

But local typealiases make the feature implementation readable:

```kotlin
private typealias SignupTransition =
    AfsmTransition<SignupState, SignupCommand, SignupEffect>

private typealias SignupMachine =
    AfsmStateMachine<SignupState, SignupEvent, SignupCommand, SignupEffect>
```

Conclusion: generic verbosity is acceptable if documentation strongly recommends feature-local typealiases.

### Finding 2: Builder API needs command/effect support on ignored/invalid decisions

The pseudo-implementation exposes a real gap. Sometimes the state does not meaningfully change, but the transition should still emit a command.

Example:

```kotlin
CancelRequested from ReadyForIdentityVerification
-> same state or UI-owned exit
-> command = CancelSignupSession
-> decision = Ignored or Transitioned?
```

The current draft has:

```kotlin
Afsm.ignore(state, reason)
Afsm.invalid(state, reason)
```

but no way to include commands/effects with those decisions.

Recommendation:

```kotlin
public fun <S : Any, C : Any, F : Any> stay(
    state: S,
    commands: List<C> = emptyList(),
    effects: List<F> = emptyList(),
    reason: String? = null,
): AfsmTransition<S, C, F>
```

and extend `ignore`/`invalid` with optional commands/effects only if there is a concrete need.

### Finding 3: Effect in core is useful for this reference flow

`LaunchIdentityVerification` is exactly the kind of output that is not a command and should not be durable state. Keeping `effects` in `AfsmTransition` keeps the transition table honest:

```kotlin
IdentityVerificationRequestCreated
-> AwaitingIdentityResult
-> effect LaunchIdentityVerification
```

If effects were moved out of core, the ViewModel integration would need a second mapping layer that hides part of the flow.

Conclusion: keep `effects` in core for MVP, but provide a `NoEffect` convention for flows without effects.

### Finding 4: Need a NoEffect marker

For flows without effects, `AfsmTransition<S, C, Nothing>` is technically possible but awkward for users.

Recommendation:

```kotlin
public sealed interface AfsmNoEffect
```

or:

```kotlin
public data object AfsmNoEffect
```

Prefer sealed interface if no value should ever be emitted. Prefer data object if APIs need a concrete type. This needs implementation validation.

### Finding 5: Decision semantics need sharper naming

`Ignored` is overloaded. It can mean:

- harmless duplicate event,
- late async result,
- UI-owned action,
- no state change but command emitted.

Recommendation:

- `Transitioned`: state/commands/effects accepted.
- `Stayed`: event accepted, state intentionally unchanged.
- `Ignored`: event irrelevant and no outputs should run.
- `Invalid`: programmer or flow error.

This makes tests clearer.

### Finding 6: Ordinary Kotlin reducers are readable enough

The implementation does not need a DSL yet. `when (state) { ... when (event) { ... } }` is verbose, but the verbosity is domain clarity rather than framework noise.

Recommendation: do not build a DSL in MVP.

## Proposed API Adjustments Before Implementation

Revise the public API draft:

```kotlin
public sealed interface AfsmDecision {
    public data object Transitioned : AfsmDecision
    public data class Stayed(val reason: String? = null) : AfsmDecision
    public data class Ignored(val reason: String? = null) : AfsmDecision
    public data class Invalid(val reason: String? = null) : AfsmDecision
}
```

```kotlin
public object Afsm {
    public fun <S : Any, C : Any, F : Any> transitionTo(
        state: S,
        commands: List<C> = emptyList(),
        effects: List<F> = emptyList(),
    ): AfsmTransition<S, C, F>

    public fun <S : Any, C : Any, F : Any> stay(
        state: S,
        commands: List<C> = emptyList(),
        effects: List<F> = emptyList(),
        reason: String? = null,
    ): AfsmTransition<S, C, F>

    public fun <S : Any, C : Any, F : Any> ignore(
        state: S,
        reason: String? = null,
    ): AfsmTransition<S, C, F>

    public fun <S : Any, C : Any, F : Any> invalid(
        state: S,
        reason: String? = null,
    ): AfsmTransition<S, C, F>
}
```

Keep `ignore` and `invalid` output-free initially. Use `stay` when outputs are intentional.

## Current Conclusion

`AfsmTransition<S, C, F>` is verbose but not disqualifying.

The practical readability hinges on three conventions:

- feature-local typealiases,
- concise builder functions,
- no DSL until repeated pain appears.

The bigger issue is not generic length; it is decision semantics. The API should add `Stayed` and `Afsm.stay(...)` before implementation.
