package afsm.sample.shop.feature.auth

import afsm.core.AfsmState
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

typealias AuthState = AfsmState<AuthPhase, AuthData>

fun authState(
    phase: AuthPhase = AuthPhase.Editing,
    data: AuthData = AuthData(),
): AuthState {
    return AfsmState(
        phase = phase,
        data = data,
    )
}

sealed interface AuthPhase {
    data object Editing : AuthPhase

    data object Submitting : AuthPhase

    data class Authenticated(
        val session: UserSession,
    ) : AuthPhase
}

data class AuthRenderState(
    val mode: AuthMode,
    val form: AuthForm,
    val isLoading: Boolean,
    val isAuthenticated: Boolean = false,
    val authenticatedEmail: String? = null,
    val errorMessage: String? = null,
)

data class AuthData(
    val mode: AuthMode = AuthMode.Login,
    val form: AuthForm = AuthForm(),
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

fun AuthState.toRenderState(): AuthRenderState {
    return when (val currentPhase = phase) {
        AuthPhase.Editing -> AuthRenderState(
            mode = data.mode,
            form = data.form,
            isLoading = false,
            errorMessage = data.errorMessage,
        )

        AuthPhase.Submitting -> AuthRenderState(
            mode = data.mode,
            form = data.form,
            isLoading = true,
        )

        is AuthPhase.Authenticated -> AuthRenderState(
            mode = AuthMode.Login,
            form = AuthForm(),
            isLoading = false,
            isAuthenticated = true,
            authenticatedEmail = currentPhase.session.email,
        )
    }
}
