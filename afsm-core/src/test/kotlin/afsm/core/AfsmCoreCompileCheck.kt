package afsm.core

private sealed interface SignupState {
    data class Editing(
        val email: String = "",
        val password: String = "",
    ) : SignupState

    data class VerifyingIdentity(
        val email: String,
        val password: String,
        val attempt: Int,
    ) : SignupState

    data class Failed(
        val email: String,
        val password: String,
        val message: String,
    ) : SignupState

    data object Completed : SignupState
}

private sealed interface SignupEvent {
    data class EmailChanged(val value: String) : SignupEvent
    data class PasswordChanged(val value: String) : SignupEvent
    data object SubmitRequested : SignupEvent
    data object VerificationSucceeded : SignupEvent
    data class VerificationFailed(val message: String) : SignupEvent
}

private sealed interface SignupCommand {
    data class RequestIdentityVerification(
        val email: String,
        val password: String,
        val attempt: Int,
    ) : SignupCommand
}

private sealed interface SignupEffect {
    data object NavigateToHome : SignupEffect
}

private typealias SignupTransition =
    AfsmTransition<SignupState, SignupCommand, SignupEffect>

private typealias SignupMachine =
    AfsmReducer<SignupState, SignupEvent, SignupCommand, SignupEffect>

private class SignupStateMachine : SignupMachine {
    override fun transition(
        state: SignupState,
        event: SignupEvent,
    ): SignupTransition {
        return when (state) {
            is SignupState.Editing -> handleEditing(state, event)
            is SignupState.VerifyingIdentity -> handleVerifyingIdentity(state, event)
            is SignupState.Failed -> handleFailed(state, event)
            SignupState.Completed -> Afsm.ignore(state, reason = "terminal state")
        }
    }

    private fun handleEditing(
        state: SignupState.Editing,
        event: SignupEvent,
    ): SignupTransition {
        return when (event) {
            is SignupEvent.EmailChanged -> Afsm.transitionTo(
                state = state.copy(email = event.value),
            )

            is SignupEvent.PasswordChanged -> Afsm.transitionTo(
                state = state.copy(password = event.value),
            )

            SignupEvent.SubmitRequested -> Afsm.transitionTo(
                state = SignupState.VerifyingIdentity(
                    email = state.email,
                    password = state.password,
                    attempt = 1,
                ),
                commands = listOf(
                    SignupCommand.RequestIdentityVerification(
                        email = state.email,
                        password = state.password,
                        attempt = 1,
                    ),
                ),
            )

            SignupEvent.VerificationSucceeded,
            is SignupEvent.VerificationFailed -> Afsm.invalid(
                state = state,
                reason = "verification result before submit",
            )
        }
    }

    private fun handleVerifyingIdentity(
        state: SignupState.VerifyingIdentity,
        event: SignupEvent,
    ): SignupTransition {
        return when (event) {
            SignupEvent.VerificationSucceeded -> Afsm.transitionTo(
                state = SignupState.Completed,
                effects = listOf(SignupEffect.NavigateToHome),
            )

            is SignupEvent.VerificationFailed -> Afsm.transitionTo(
                state = SignupState.Failed(
                    email = state.email,
                    password = state.password,
                    message = event.message,
                ),
            )

            SignupEvent.SubmitRequested -> Afsm.ignore(
                state = state,
                reason = "verification already in progress",
            )

            is SignupEvent.EmailChanged,
            is SignupEvent.PasswordChanged -> Afsm.stay(
                state = state,
                reason = "input locked while verifying",
            )
        }
    }

    private fun handleFailed(
        state: SignupState.Failed,
        event: SignupEvent,
    ): SignupTransition {
        return when (event) {
            SignupEvent.SubmitRequested -> Afsm.transitionTo(
                state = SignupState.VerifyingIdentity(
                    email = state.email,
                    password = state.password,
                    attempt = 2,
                ),
                commands = listOf(
                    SignupCommand.RequestIdentityVerification(
                        email = state.email,
                        password = state.password,
                        attempt = 2,
                    ),
                ),
            )

            is SignupEvent.EmailChanged -> Afsm.transitionTo(
                state = SignupState.Editing(
                    email = event.value,
                    password = state.password,
                ),
            )

            is SignupEvent.PasswordChanged -> Afsm.transitionTo(
                state = SignupState.Editing(
                    email = state.email,
                    password = event.value,
                ),
            )

            SignupEvent.VerificationSucceeded,
            is SignupEvent.VerificationFailed -> Afsm.ignore(
                state = state,
                reason = "stale verification result",
            )
        }
    }
}

private sealed interface LoginState {
    data object Editing : LoginState
    data object Submitting : LoginState
    data object Submitted : LoginState
}

private sealed interface LoginEvent {
    data object SubmitRequested : LoginEvent
    data object SubmitSucceeded : LoginEvent
    data object CancelRequested : LoginEvent
}

private sealed interface LoginCommand {
    data object ExecuteLogin : LoginCommand
    data object CancelLogin : LoginCommand
}

private typealias LoginTransition =
    AfsmTransition<LoginState, LoginCommand, AfsmNoEffect>

private typealias LoginMachine =
    AfsmReducer<LoginState, LoginEvent, LoginCommand, AfsmNoEffect>

private class LoginStateMachine : LoginMachine {
    override fun transition(
        state: LoginState,
        event: LoginEvent,
    ): LoginTransition {
        return when (state) {
            LoginState.Editing -> when (event) {
                LoginEvent.SubmitRequested -> Afsm.transitionTo(
                    state = LoginState.Submitting,
                    commands = listOf(LoginCommand.ExecuteLogin),
                )

                LoginEvent.CancelRequested -> Afsm.stay(
                    state = state,
                    reason = "nothing to cancel",
                )

                LoginEvent.SubmitSucceeded -> Afsm.invalid(
                    state = state,
                    reason = "success before submit",
                )
            }

            LoginState.Submitting -> when (event) {
                LoginEvent.SubmitSucceeded -> Afsm.transitionTo(
                    state = LoginState.Submitted,
                )

                LoginEvent.CancelRequested -> Afsm.stay(
                    state = state,
                    commands = listOf(LoginCommand.CancelLogin),
                    reason = "cancel accepted with cleanup",
                )

                LoginEvent.SubmitRequested -> Afsm.ignore(
                    state = state,
                    reason = "duplicate submit",
                )
            }

            LoginState.Submitted -> Afsm.ignore(
                state = state,
                reason = "terminal state",
            )
        }
    }
}

@Suppress("unused")
private fun compileAfsmNoEffectUsage() {
    val machine: LoginMachine = LoginStateMachine()
    val transition: LoginTransition = machine.transition(
        state = LoginState.Editing,
        event = LoginEvent.SubmitRequested,
    )

    val noEffects: List<AfsmNoEffect> = transition.effects
    check(noEffects.isEmpty())
}

@Suppress("unused")
private fun compileAfsmTransitionWithEffectsUsage() {
    val machine: SignupMachine = SignupStateMachine()
    val transition: SignupTransition = machine.transition(
        state = SignupState.VerifyingIdentity(
            email = "user@example.com",
            password = "password",
            attempt = 1,
        ),
        event = SignupEvent.VerificationSucceeded,
    )

    check(transition.effects == listOf(SignupEffect.NavigateToHome))
}
