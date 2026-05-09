package afsm.sample.shop.feature.auth

import afsm.core.AfsmGraph
import afsm.core.AfsmGraphSource
import afsm.core.AfsmMachine
import afsm.core.AfsmSnapshot
import afsm.core.AfsmStateMachine
import afsm.core.AfsmTopology
import afsm.core.afsmMachine
import afsm.sample.shop.core.model.UserSession

@AfsmGraph(
    id = "Auth",
    fileName = "AuthStateMachine.mmd",
)
class AuthStateMachine : AfsmStateMachine<AuthState, AuthEvent, AuthCommand, AuthEffect>,
    AfsmGraphSource {
    private val machine: AfsmMachine<
        AuthPhase,
        AuthContext,
        AuthEvent,
        AuthCommand,
        AuthEffect,
        > = authMachine()

    override val topology: AfsmTopology
        get() = machine.topology

    override fun transition(
        state: AuthState,
        event: AuthEvent,
    ): AuthTransition {
        val transition = machine.transition(
            snapshot = state.toSnapshot(),
            event = event,
        )

        return AuthTransition(
            state = transition.state.toAuthState(),
            commands = transition.commands,
            effects = transition.effects,
            decision = transition.decision,
        )
    }
}

private fun authMachine(): AfsmMachine<AuthPhase, AuthContext, AuthEvent, AuthCommand, AuthEffect> {
    return afsmMachine {
        initial(
            phase = AuthPhase.Editing,
            context = AuthContext(),
        )

        state(AuthPhase.Editing) {
            on<AuthEvent.ModeChanged> {
                stay {
                    updateContext {
                        copy(
                            mode = event.mode,
                            errorMessage = null,
                        )
                    }
                }
            }

            on<AuthEvent.NameChanged> {
                stay {
                    updateContext {
                        copy(
                            form = form.copy(name = event.value),
                            errorMessage = null,
                        )
                    }
                }
            }

            on<AuthEvent.EmailChanged> {
                stay {
                    updateContext {
                        copy(
                            form = form.copy(email = event.value),
                            errorMessage = null,
                        )
                    }
                }
            }

            on<AuthEvent.PasswordChanged> {
                stay {
                    updateContext {
                        copy(
                            form = form.copy(password = event.value),
                            errorMessage = null,
                        )
                    }
                }
            }

            on<AuthEvent.SubmitClicked> {
                transitionTo(
                    phase = AuthPhase.Submitting,
                    guard = { context.canSubmitLogin() },
                ) {
                    val normalized = context.normalized()
                    updateContext {
                        normalized.copy(
                            errorMessage = null,
                            session = null,
                        )
                    }
                    action(
                        AuthCommand.Login(
                            email = normalized.form.email,
                            password = normalized.form.password,
                        ),
                    )
                }

                transitionTo(
                    phase = AuthPhase.Submitting,
                    guard = { context.canSubmitRegister() },
                ) {
                    val normalized = context.normalized()
                    updateContext {
                        normalized.copy(
                            errorMessage = null,
                            session = null,
                        )
                    }
                    action(
                        AuthCommand.Register(
                            name = normalized.form.name,
                            email = normalized.form.email,
                            password = normalized.form.password,
                        ),
                    )
                }

                otherwise {
                    updateContext {
                        val normalized = normalized()
                        normalized.copy(errorMessage = normalized.submitError())
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
                transitionTo(AuthPhase.Authenticated) {
                    updateContext {
                        copy(
                            session = event.session,
                            errorMessage = null,
                        )
                    }
                    effect(AuthEffect.OpenCatalog)
                }
            }

            on<AuthEvent.AuthFailed> {
                transitionTo(AuthPhase.Editing) {
                    updateContext {
                        copy(errorMessage = event.message)
                    }
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

        state(AuthPhase.Authenticated) {
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

private sealed interface AuthPhase {
    data object Editing : AuthPhase
    data object Submitting : AuthPhase
    data object Authenticated : AuthPhase
}

private data class AuthContext(
    val mode: AuthMode = AuthMode.Login,
    val form: AuthForm = AuthForm(),
    val errorMessage: String? = null,
    val session: UserSession? = null,
)

private fun AuthState.toSnapshot(): AfsmSnapshot<AuthPhase, AuthContext> {
    return when (this) {
        is AuthState.Editing -> AfsmSnapshot(
            phase = AuthPhase.Editing,
            context = AuthContext(
                mode = mode,
                form = form,
                errorMessage = errorMessage,
            ),
        )

        is AuthState.Submitting -> AfsmSnapshot(
            phase = AuthPhase.Submitting,
            context = AuthContext(
                mode = mode,
                form = form,
            ),
        )

        is AuthState.Authenticated -> AfsmSnapshot(
            phase = AuthPhase.Authenticated,
            context = AuthContext(session = session),
        )
    }
}

private fun AfsmSnapshot<AuthPhase, AuthContext>.toAuthState(): AuthState {
    return when (phase) {
        AuthPhase.Editing -> AuthState.Editing(
            mode = context.mode,
            form = context.form,
            errorMessage = context.errorMessage,
        )

        AuthPhase.Submitting -> AuthState.Submitting(
            mode = context.mode,
            form = context.form,
        )

        AuthPhase.Authenticated -> AuthState.Authenticated(
            session = requireNotNull(context.session) {
                "Authenticated phase requires a session in AuthContext."
            },
        )
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
