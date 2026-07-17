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

private typealias SignupTransition =
    AfsmTransition<SignupState, SignupCommand>

private typealias SignupMachine =
    AfsmReducer<SignupState, SignupEvent, SignupCommand>

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
            is SignupEvent.EmailChanged -> Afsm.transitioned(
                state = state.copy(email = event.value),
            )

            is SignupEvent.PasswordChanged -> Afsm.transitioned(
                state = state.copy(password = event.value),
            )

            SignupEvent.SubmitRequested -> Afsm.transitioned(
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
            SignupEvent.VerificationSucceeded -> Afsm.transitioned(
                state = SignupState.Completed,
            )

            is SignupEvent.VerificationFailed -> Afsm.transitioned(
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
            is SignupEvent.PasswordChanged -> AfsmTransition.handled(
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
            SignupEvent.SubmitRequested -> Afsm.transitioned(
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

            is SignupEvent.EmailChanged -> Afsm.transitioned(
                state = SignupState.Editing(
                    email = event.value,
                    password = state.password,
                ),
            )

            is SignupEvent.PasswordChanged -> Afsm.transitioned(
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
    AfsmTransition<LoginState, LoginCommand>

private typealias LoginMachine =
    AfsmReducer<LoginState, LoginEvent, LoginCommand>

private class LoginStateMachine : LoginMachine {
    override fun transition(
        state: LoginState,
        event: LoginEvent,
    ): LoginTransition {
        return when (state) {
            LoginState.Editing -> when (event) {
                LoginEvent.SubmitRequested -> Afsm.transitioned(
                    state = LoginState.Submitting,
                    commands = listOf(LoginCommand.ExecuteLogin),
                )

                LoginEvent.CancelRequested -> AfsmTransition.handled(
                    state = state,
                    reason = "nothing to cancel",
                )

                LoginEvent.SubmitSucceeded -> Afsm.invalid(
                    state = state,
                    reason = "success before submit",
                )
            }

            LoginState.Submitting -> when (event) {
                LoginEvent.SubmitSucceeded -> Afsm.transitioned(
                    state = LoginState.Submitted,
                )

                LoginEvent.CancelRequested -> AfsmTransition.handled(
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

private sealed interface ToggleState {
    data object Off : ToggleState
    data object On : ToggleState
}

private sealed interface ToggleEvent {
    data object ToggleRequested : ToggleEvent
}

private typealias ToggleTransition =
    AfsmTransition<ToggleState, AfsmNoCommand>

private typealias ToggleMachine =
    AfsmReducer<ToggleState, ToggleEvent, AfsmNoCommand>

private object ToggleStateMachine : ToggleMachine {
    override fun transition(
        state: ToggleState,
        event: ToggleEvent,
    ): ToggleTransition {
        return when (event) {
            ToggleEvent.ToggleRequested -> when (state) {
                ToggleState.Off -> Afsm.transitioned(ToggleState.On)
                ToggleState.On -> Afsm.transitioned(ToggleState.Off)
            }
        }
    }
}

@Suppress("unused")
private fun compileEffectFreeTransitionUsage() {
    val machine: LoginMachine = LoginStateMachine()
    val transition: LoginTransition = machine.transition(
        state = LoginState.Editing,
        event = LoginEvent.SubmitRequested,
    )

    check(transition.state == LoginState.Submitting)
}

@Suppress("unused")
private fun compileAfsmNoCommandUsage() {
    val transition: ToggleTransition = ToggleStateMachine.transition(
        state = ToggleState.Off,
        event = ToggleEvent.ToggleRequested,
    )

    val noCommands: List<AfsmNoCommand> = transition.commands
    check(noCommands.isEmpty())
}

@Suppress("unused")
private fun compileDurableCompletionUsage() {
    val machine: SignupMachine = SignupStateMachine()
    val transition: SignupTransition = machine.transition(
        state = SignupState.VerifyingIdentity(
            email = "user@example.com",
            password = "password",
            attempt = 1,
        ),
        event = SignupEvent.VerificationSucceeded,
    )

    check(transition.state == SignupState.Completed)
}
