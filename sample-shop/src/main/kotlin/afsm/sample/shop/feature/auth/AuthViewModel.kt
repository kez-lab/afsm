package afsm.sample.shop.feature.auth

import afsm.sample.shop.core.data.AuthRepository
import afsm.sample.shop.core.data.SessionRepository
import afsm.sample.shop.app.shopAfsmConfig
import afsm.viewmodel.afsmHost
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val sessionRepository: SessionRepository,
) : ViewModel() {
    private val host = afsmHost(
        machine = authStateMachine,
        config = shopAfsmConfig(),
        commandHandler = { command: AuthCommand, send ->
            when (command) {
                is AuthCommand.Login -> {
                    authRepository.login(
                        email = command.email,
                        password = command.password,
                    ).fold(
                        onSuccess = { session ->
                            sessionRepository.setSession(session)
                            send(AuthEvent.AuthSucceeded(session))
                        },
                        onFailure = { error ->
                            send(AuthEvent.AuthFailed(error.message ?: "Login failed."))
                        },
                    )
                }

                is AuthCommand.Register -> {
                    authRepository.register(
                        name = command.name,
                        email = command.email,
                        password = command.password,
                    ).fold(
                        onSuccess = { session ->
                            sessionRepository.setSession(session)
                            send(AuthEvent.AuthSucceeded(session))
                        },
                        onFailure = { error ->
                            send(AuthEvent.AuthFailed(error.message ?: "Registration failed."))
                        },
                    )
                }
            }
        },
    )

    val state: StateFlow<AuthState> = host.state

    fun selectMode(mode: AuthMode) = send(AuthEvent.ModeChanged(mode))

    fun updateName(value: String) = send(AuthEvent.NameChanged(value))

    fun updateEmail(value: String) = send(AuthEvent.EmailChanged(value))

    fun updatePassword(value: String) = send(AuthEvent.PasswordChanged(value))

    fun submit() = send(AuthEvent.SubmitClicked)

    private fun send(event: AuthEvent) = host.send(event)
}
