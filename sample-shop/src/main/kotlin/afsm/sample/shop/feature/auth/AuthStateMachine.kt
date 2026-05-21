package afsm.sample.shop.feature.auth

import afsm.core.AfsmGraph
import afsm.core.AfsmMachine
import afsm.core.afsmMachine

private typealias AuthMachine = AfsmMachine<AuthState, AuthEvent, AuthCommand, AuthEffect>

@AfsmGraph(
    id = "Auth",
    fileName = "AuthStateMachine.mmd",
)
internal object AuthStateMachine : AuthMachine by authMachine()

private fun authMachine(): AuthMachine {
    return afsmMachine {
        initial(
            phase = AuthPhase.Editing,
            context = AuthContext(),
        )

        state(AuthPhase.Editing) {
            on<AuthEvent.ModeChanged> {
                updateContext { context, event ->
                    context.copy(
                        mode = event.mode,
                        errorMessage = null,
                    )
                }
            }

            on<AuthEvent.NameChanged> {
                updateContext { context, event ->
                    context.copy(
                        form = context.form.copy(name = event.value),
                        errorMessage = null,
                    )
                }
            }

            on<AuthEvent.EmailChanged> {
                updateContext { context, event ->
                    context.copy(
                        form = context.form.copy(email = event.value),
                        errorMessage = null,
                    )
                }
            }

            on<AuthEvent.PasswordChanged> {
                updateContext { context, event ->
                    context.copy(
                        form = context.form.copy(password = event.value),
                        errorMessage = null,
                    )
                }
            }

            on<AuthEvent.SubmitClicked> {
                case(
                    label = "login form",
                    condition = { context.canSubmitLogin() },
                ) {
                    updateContext {
                        val normalized = normalized()
                        normalized.copy(
                            errorMessage = null,
                        )
                    }
                    command(label = "Login") {
                        val normalized = context.normalized()
                        AuthCommand.Login(
                            email = normalized.form.email,
                            password = normalized.form.password,
                        )
                    }
                    transitionTo(AuthPhase.Submitting)
                }

                case(
                    label = "register form",
                    condition = { context.canSubmitRegister() },
                ) {
                    updateContext {
                        val normalized = normalized()
                        normalized.copy(
                            errorMessage = null,
                        )
                    }
                    command(label = "Register") {
                        val normalized = context.normalized()
                        AuthCommand.Register(
                            name = normalized.form.name,
                            email = normalized.form.email,
                            password = normalized.form.password,
                        )
                    }
                    transitionTo(AuthPhase.Submitting)
                }

                case(label = "invalid form") {
                    updateContext {
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

        state(AuthPhase.Submitting) {
            on<AuthEvent.AuthSucceeded> {
                case {
                    updateContext {
                        AuthContext()
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
                    updateContext { context, event ->
                        context.copy(errorMessage = event.message)
                    }
                    transitionTo(AuthPhase.Editing)
                }
            }

            on<AuthEvent.SubmitClicked> {
                ignore(reason = "Duplicate submit while auth command is running.")
            }

            on<AuthEvent.ModeChanged> {
                ignore(reason = "Form edits are ignored while auth command is running.")
            }

            on<AuthEvent.NameChanged> {
                ignore(reason = "Form edits are ignored while auth command is running.")
            }

            on<AuthEvent.EmailChanged> {
                ignore(reason = "Form edits are ignored while auth command is running.")
            }

            on<AuthEvent.PasswordChanged> {
                ignore(reason = "Form edits are ignored while auth command is running.")
            }
        }

        state<AuthPhase.Authenticated> {
            on<AuthEvent.AuthSucceeded> {
                ignore(reason = "Auth flow already completed.")
            }

            on<AuthEvent.AuthFailed> {
                ignore(reason = "Auth flow already completed.")
            }

            on<AuthEvent.SubmitClicked> {
                ignore(reason = "Auth flow already completed.")
            }

            on<AuthEvent.ModeChanged> {
                ignore(reason = "Auth flow already completed.")
            }

            on<AuthEvent.NameChanged> {
                ignore(reason = "Auth flow already completed.")
            }

            on<AuthEvent.EmailChanged> {
                ignore(reason = "Auth flow already completed.")
            }

            on<AuthEvent.PasswordChanged> {
                ignore(reason = "Auth flow already completed.")
            }
        }
    }
}

private fun AuthContext.normalized(): AuthContext {
    return copy(
        form = form.copy(
            name = form.name.trim(),
            email = form.email.trim(),
        ),
    )
}

private fun AuthContext.canSubmitLogin(): Boolean {
    return mode == AuthMode.Login && submitError() == null
}

private fun AuthContext.canSubmitRegister(): Boolean {
    return mode == AuthMode.Register && submitError() == null
}

private fun AuthContext.submitError(): String? {
    val normalized = normalized()
    return when {
        normalized.form.email.isBlank() -> "Email is required."
        normalized.form.password.length < 6 -> "Password must be at least 6 characters."
        normalized.mode == AuthMode.Register && normalized.form.name.isBlank() -> "Name is required."
        else -> null
    }
}
