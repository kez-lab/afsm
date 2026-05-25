package afsm.sample.shop.feature.auth

import afsm.sample.shop.core.data.AuthRepository
import afsm.sample.shop.core.data.SessionRepository
import afsm.viewmodel.afsmHost
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val sessionRepository: SessionRepository,
) : ViewModel() {
    private val host = afsmHost(
        machine = AuthStateMachine,
        commandHandler = { command: AuthCommand, dispatch ->
            when (command) {
                is AuthCommand.Login -> {
                    authRepository.login(
                        email = command.email,
                        password = command.password,
                    ).fold(
                        onSuccess = { session ->
                            sessionRepository.setSession(session)
                            dispatch(AuthEvent.AuthSucceeded(session))
                        },
                        onFailure = { error ->
                            dispatch(AuthEvent.AuthFailed(error.message ?: "Login failed."))
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
                            dispatch(AuthEvent.AuthSucceeded(session))
                        },
                        onFailure = { error ->
                            dispatch(AuthEvent.AuthFailed(error.message ?: "Registration failed."))
                        },
                    )
                }
            }
        },
    )

    val state: StateFlow<AuthState> = host.state
    val effects: Flow<AuthEffect> = host.effects

    fun onEvent(event: AuthEvent) {
        host.dispatch(event)
    }
}
