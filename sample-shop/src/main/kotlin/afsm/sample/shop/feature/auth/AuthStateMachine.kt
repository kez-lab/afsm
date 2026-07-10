package afsm.sample.shop.feature.auth

import afsm.core.AfsmGraph
import afsm.core.AfsmDefaultMachine
import afsm.core.afsmMachine

@AfsmGraph(
    id = "Auth",
    fileName = "AuthStateMachine.mmd",
)
internal val authStateMachine:
    AfsmDefaultMachine<AuthState, AuthEvent, AuthCommand, AuthEffect> =
    afsmMachine {
        initial(
            phase = AuthPhase.Editing,
            data = AuthData(),
        )

        phase(AuthPhase.Editing) {
            on<AuthEvent.ModeChanged> {
                updateData { data, event ->
                    data.copy(
                        mode = event.mode,
                        errorMessage = null,
                    )
                }
            }

            on<AuthEvent.NameChanged> {
                updateData { data, event ->
                    data.copy(
                        form = data.form.copy(name = event.value),
                        errorMessage = null,
                    )
                }
            }

            on<AuthEvent.EmailChanged> {
                updateData { data, event ->
                    data.copy(
                        form = data.form.copy(email = event.value),
                        errorMessage = null,
                    )
                }
            }

            on<AuthEvent.PasswordChanged> {
                updateData { data, event ->
                    data.copy(
                        form = data.form.copy(password = event.value),
                        errorMessage = null,
                    )
                }
            }

            on<AuthEvent.SubmitClicked> {
                case(
                    label = "login form",
                    condition = { data.canSubmitLoginRequest() },
                ) {
                    updateData {
                        val normalized = normalized()
                        normalized.copy(
                            errorMessage = null,
                        )
                    }
                    command(label = "Login") {
                        AuthCommand.Login(
                            email = data.form.email,
                            password = data.form.password,
                        )
                    }
                    transitionTo(AuthPhase.Submitting)
                }

                case(
                    label = "register form",
                    condition = { data.canSubmitRegistrationRequest() },
                ) {
                    updateData {
                        val normalized = normalized()
                        normalized.copy(
                            errorMessage = null,
                        )
                    }
                    command(label = "Register") {
                        AuthCommand.Register(
                            name = data.form.name,
                            email = data.form.email,
                            password = data.form.password,
                        )
                    }
                    transitionTo(AuthPhase.Submitting)
                }

                case(
                    label = "invalid form",
                    condition = { data.hasSubmitError() },
                ) {
                    updateData {
                        val normalized = normalized()
                        copy(
                            form = normalized.form,
                            errorMessage = normalized.submitError(),
                        )
                    }
                }
            }

            on<AuthEvent.AuthSucceeded> {
                invalid(reason = "Auth result arrived while no request is pending.")
            }

            on<AuthEvent.AuthFailed> {
                invalid(reason = "Auth result arrived while no request is pending.")
            }
        }

        phase(AuthPhase.Submitting) {
            on<AuthEvent.AuthSucceeded> {
                case {
                    updateData {
                        AuthData()
                    }
                    effect(label = "OpenCatalog") { AuthEffect.OpenCatalog }
                    transitionTo<AuthPhase.Authenticated> {
                        AuthPhase.Authenticated(
                            session = event.session,
                        )
                    }
                }
            }

            on<AuthEvent.AuthFailed> {
                case {
                    updateData { data, event ->
                        data.copy(errorMessage = event.message)
                    }
                    transitionTo(AuthPhase.Editing)
                }
            }

            on<AuthEvent.SubmitClicked> {
                ignore(reason = "Duplicate submit while auth command is running.")
            }
        }

        phase<AuthPhase.Authenticated>()
}

private fun AuthData.normalized(): AuthData {
    return copy(
        form = form.copy(
            name = form.name.trim(),
            email = form.email.trim(),
        ),
    )
}

private fun AuthData.canSubmitLoginRequest(): Boolean {
    return mode == AuthMode.Login && submitError() == null
}

private fun AuthData.canSubmitRegistrationRequest(): Boolean {
    return mode == AuthMode.Register && submitError() == null
}

private fun AuthData.hasSubmitError(): Boolean {
    return submitError() != null
}

private fun AuthData.submitError(): String? {
    val normalized = normalized()
    return when {
        normalized.form.email.isBlank() -> "Email is required."
        normalized.form.password.length < 6 -> "Password must be at least 6 characters."
        normalized.mode == AuthMode.Register && normalized.form.name.isBlank() -> "Name is required."
        else -> null
    }
}
