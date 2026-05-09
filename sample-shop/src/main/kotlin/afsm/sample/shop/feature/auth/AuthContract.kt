package afsm.sample.shop.feature.auth

import afsm.core.AfsmTransition
import afsm.sample.shop.core.model.UserSession

enum class AuthMode {
    Login,
    Register,
}

data class AuthState(
    val mode: AuthMode = AuthMode.Login,
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

sealed interface AuthEvent {
    data object LoginModeSelected : AuthEvent

    data object RegisterModeSelected : AuthEvent

    data class NameChanged(val value: String) : AuthEvent

    data class EmailChanged(val value: String) : AuthEvent

    data class PasswordChanged(val value: String) : AuthEvent

    data object SubmitClicked : AuthEvent

    data class AuthSucceeded(val session: UserSession) : AuthEvent

    data class AuthFailed(val message: String) : AuthEvent
}

sealed interface AuthCommand {
    data class Login(
        val email: String,
        val password: String,
    ) : AuthCommand

    data class Register(
        val name: String,
        val email: String,
        val password: String,
    ) : AuthCommand
}

sealed interface AuthEffect {
    data object OpenCatalog : AuthEffect
}

typealias AuthTransition = AfsmTransition<AuthState, AuthCommand, AuthEffect>
