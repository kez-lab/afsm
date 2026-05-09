package afsm.sample.shop.feature.auth

import afsm.core.Afsm
import afsm.core.AfsmStateMachine

class AuthStateMachine : AfsmStateMachine<AuthState, AuthEvent, AuthCommand, AuthEffect> {
    override fun transition(
        state: AuthState,
        event: AuthEvent,
    ): AuthTransition {
        return when (event) {
            AuthEvent.LoginModeSelected -> {
                if (state.isLoading) {
                    Afsm.ignore(state, reason = "Cannot switch auth mode while submitting.")
                } else {
                    Afsm.transitionTo(
                        state = state.copy(
                            mode = AuthMode.Login,
                            errorMessage = null,
                        ),
                    )
                }
            }

            AuthEvent.RegisterModeSelected -> {
                if (state.isLoading) {
                    Afsm.ignore(state, reason = "Cannot switch auth mode while submitting.")
                } else {
                    Afsm.transitionTo(
                        state = state.copy(
                            mode = AuthMode.Register,
                            errorMessage = null,
                        ),
                    )
                }
            }

            is AuthEvent.NameChanged -> {
                if (state.isLoading) {
                    Afsm.ignore(state, reason = "Cannot edit name while submitting.")
                } else {
                    Afsm.transitionTo(state.copy(name = event.value, errorMessage = null))
                }
            }

            is AuthEvent.EmailChanged -> {
                if (state.isLoading) {
                    Afsm.ignore(state, reason = "Cannot edit email while submitting.")
                } else {
                    Afsm.transitionTo(state.copy(email = event.value, errorMessage = null))
                }
            }

            is AuthEvent.PasswordChanged -> {
                if (state.isLoading) {
                    Afsm.ignore(state, reason = "Cannot edit password while submitting.")
                } else {
                    Afsm.transitionTo(state.copy(password = event.value, errorMessage = null))
                }
            }

            AuthEvent.SubmitClicked -> submit(state)

            is AuthEvent.AuthSucceeded -> {
                if (!state.isLoading) {
                    Afsm.invalid(state, reason = "Auth success arrived without a pending request.")
                } else {
                    Afsm.transitionTo(
                        state = state.copy(isLoading = false, errorMessage = null),
                        effects = listOf(AuthEffect.OpenCatalog),
                    )
                }
            }

            is AuthEvent.AuthFailed -> {
                if (!state.isLoading) {
                    Afsm.invalid(state, reason = "Auth failure arrived without a pending request.")
                } else {
                    Afsm.transitionTo(
                        state = state.copy(
                            isLoading = false,
                            errorMessage = event.message,
                        ),
                    )
                }
            }
        }
    }

    private fun submit(state: AuthState): AuthTransition {
        if (state.isLoading) {
            return Afsm.ignore(state, reason = "Duplicate submit while auth command is running.")
        }

        val email = state.email.trim()
        val password = state.password
        val name = state.name.trim()

        if (email.isBlank()) {
            return Afsm.stay(state.copy(errorMessage = "Email is required."))
        }

        if (password.length < 6) {
            return Afsm.stay(state.copy(errorMessage = "Password must be at least 6 characters."))
        }

        return when (state.mode) {
            AuthMode.Login -> Afsm.transitionTo(
                state = state.copy(
                    email = email,
                    isLoading = true,
                    errorMessage = null,
                ),
                commands = listOf(
                    AuthCommand.Login(
                        email = email,
                        password = password,
                    ),
                ),
            )

            AuthMode.Register -> {
                if (name.isBlank()) {
                    return Afsm.stay(state.copy(errorMessage = "Name is required."))
                }

                Afsm.transitionTo(
                    state = state.copy(
                        name = name,
                        email = email,
                        isLoading = true,
                        errorMessage = null,
                    ),
                    commands = listOf(
                        AuthCommand.Register(
                            name = name,
                            email = email,
                            password = password,
                        ),
                    ),
                )
            }
        }
    }
}
