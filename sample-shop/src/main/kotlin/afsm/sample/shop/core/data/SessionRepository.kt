package afsm.sample.shop.core.data

import afsm.sample.shop.core.model.UserSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SessionRepository {
    private val _session = MutableStateFlow<UserSession?>(null)

    val session: StateFlow<UserSession?> = _session.asStateFlow()

    fun setSession(session: UserSession) {
        _session.value = session
    }

    fun clearSession() {
        _session.value = null
    }

    fun currentSession(): UserSession? = _session.value
}
