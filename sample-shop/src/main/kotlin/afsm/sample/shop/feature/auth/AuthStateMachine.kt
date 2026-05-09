package afsm.sample.shop.feature.auth

import afsm.core.Afsm
import afsm.core.AfsmStateMachine

class AuthStateMachine : AfsmStateMachine<AuthState, AuthEvent, AuthCommand, AuthEffect> {
    override fun transition(
        state: AuthState,
        event: AuthEvent,
    ): AuthTransition {
        return when (state) {
            is AuthState.Editing -> reduceEditing(state, event)
            is AuthState.Submitting -> reduceSubmitting(state, event)
            is AuthState.Authenticated -> reduceAuthenticated(state, event)
        }
    }

    private fun reduceEditing(
        state: AuthState.Editing,
        event: AuthEvent,
    ): AuthTransition {
        return when (event) {
            is AuthEvent.ModeChanged -> Afsm.transitionTo(
                state = state.copy(
                    mode = event.mode,
                    errorMessage = null,
                ),
            )

            is AuthEvent.NameChanged -> Afsm.transitionTo(
                state.copy(
                    form = state.form.copy(name = event.value),
                    errorMessage = null,
                ),
            )

            is AuthEvent.EmailChanged -> Afsm.transitionTo(
                state.copy(
                    form = state.form.copy(email = event.value),
                    errorMessage = null,
                ),
            )

            is AuthEvent.PasswordChanged -> Afsm.transitionTo(
                state.copy(
                    form = state.form.copy(password = event.value),
                    errorMessage = null,
                ),
            )

            AuthEvent.SubmitClicked -> submit(state)

            is AuthEvent.AuthSucceeded,
            is AuthEvent.AuthFailed -> Afsm.invalid(
                state,
                reason = "Auth result arrived while no request is pending.",
            )
        }
    }

    private fun reduceSubmitting(
        state: AuthState.Submitting,
        event: AuthEvent,
    ): AuthTransition {
        return when (event) {
            is AuthEvent.AuthSucceeded -> Afsm.transitionTo(
                state = AuthState.Authenticated(event.session),
                effects = listOf(AuthEffect.OpenCatalog),
            )

            is AuthEvent.AuthFailed -> Afsm.transitionTo(
                state = AuthState.Editing(
                    mode = state.mode,
                    form = state.form,
                    errorMessage = event.message,
                ),
            )

            AuthEvent.SubmitClicked -> Afsm.ignore(
                state,
                reason = "Duplicate submit while auth command is running.",
            )

            is AuthEvent.ModeChanged,
            is AuthEvent.NameChanged,
            is AuthEvent.EmailChanged,
            is AuthEvent.PasswordChanged -> Afsm.ignore(
                state,
                reason = "Form edits are ignored while auth command is running.",
            )
        }
    }

    private fun reduceAuthenticated(
        state: AuthState.Authenticated,
        event: AuthEvent,
    ): AuthTransition {
        return when (event) {
            is AuthEvent.AuthSucceeded,
            is AuthEvent.AuthFailed,
            AuthEvent.SubmitClicked,
            is AuthEvent.ModeChanged,
            is AuthEvent.NameChanged,
            is AuthEvent.EmailChanged,
            is AuthEvent.PasswordChanged -> Afsm.ignore(
                state,
                reason = "Auth flow already completed.",
            )
        }
    }

    private fun submit(state: AuthState.Editing): AuthTransition {
        val email = state.form.email.trim()
        val password = state.form.password
        val name = state.form.name.trim()
        val normalizedForm = state.form.copy(
            name = name,
            email = email,
        )

        if (email.isBlank()) {
            return Afsm.stay(state.copy(form = normalizedForm, errorMessage = "Email is required."))
        }

        if (password.length < 6) {
            return Afsm.stay(
                state.copy(
                    form = normalizedForm,
                    errorMessage = "Password must be at least 6 characters.",
                ),
            )
        }

        return when (state.mode) {
            AuthMode.Login -> Afsm.transitionTo(
                state = AuthState.Submitting(
                    mode = state.mode,
                    form = normalizedForm,
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
                    return Afsm.stay(
                        state.copy(
                            form = normalizedForm,
                            errorMessage = "Name is required.",
                        ),
                    )
                }

                Afsm.transitionTo(
                    state = AuthState.Submitting(
                        mode = state.mode,
                        form = normalizedForm,
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
