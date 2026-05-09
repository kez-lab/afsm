package afsm.sample.shop.feature.auth

import afsm.core.AfsmTransition
import afsm.sample.shop.core.model.UserSession

enum class AuthMode {
    Login,
    Register,
}

data class AuthForm(
    val name: String = "",
    val email: String = "",
    val password: String = "",
)

sealed interface AuthState {
    data class Editing(
        val mode: AuthMode = AuthMode.Login,
        val form: AuthForm = AuthForm(),
        val errorMessage: String? = null,
    ) : AuthState

    data class Submitting(
        val mode: AuthMode,
        val form: AuthForm,
    ) : AuthState

    data class Authenticated(
        val session: UserSession,
    ) : AuthState
}

data class AuthRenderState(
    val mode: AuthMode,
    val form: AuthForm,
    val isLoading: Boolean,
    val errorMessage: String? = null,
)

sealed interface AuthEvent {
    data class ModeChanged(val mode: AuthMode) : AuthEvent

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

fun AuthState.toRenderState(): AuthRenderState {
    return when (this) {
        is AuthState.Editing -> AuthRenderState(
            mode = mode,
            form = form,
            isLoading = false,
            errorMessage = errorMessage,
        )

        is AuthState.Submitting -> AuthRenderState(
            mode = mode,
            form = form,
            isLoading = true,
        )

        is AuthState.Authenticated -> AuthRenderState(
            mode = AuthMode.Login,
            form = AuthForm(),
            isLoading = false,
        )
    }
}
