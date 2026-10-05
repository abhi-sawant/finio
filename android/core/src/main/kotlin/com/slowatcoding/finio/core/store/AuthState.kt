package com.slowatcoding.finio.core.store

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable

// Port of web/src/store/useAuthStore.ts (`finio-auth`): the cloud account's JWT, profile and the
// last successful cloud backup. Optional — the app works signed out. Never part of a backup.

/** `AuthUser` in web/src/services/api.ts. */
@Serializable
data class AuthUser(val id: Int, val name: String, val email: String)

@Serializable
data class PersistedAuth(
    val token: String? = null,
    val user: AuthUser? = null,
    val lastBackupAt: String? = null,
)

data class AuthUiState(
    val token: String? = null,
    val user: AuthUser? = null,
    val lastBackupAt: String? = null,
    /** True once storage has been read (the web's `loadAuth` on rehydrate). */
    val isLoaded: Boolean = false,
) {
    val isSignedIn: Boolean get() = !token.isNullOrEmpty()
    fun persisted() = PersistedAuth(token, user, lastBackupAt)
}

class AuthState(private val persist: (PersistedAuth) -> Unit = {}) {
    private val lock = Any()
    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()
    val current: AuthUiState get() = _state.value

    private fun update(f: (AuthUiState) -> AuthUiState) = synchronized(lock) {
        val before = _state.value
        val after = f(before)
        _state.value = after
        if (after.persisted() != before.persisted()) persist(after.persisted())
    }

    /** Load what was persisted, then mark loaded. */
    fun hydrate(persisted: PersistedAuth?) {
        synchronized(lock) {
            val p = persisted ?: PersistedAuth()
            _state.value = AuthUiState(p.token, p.user, p.lastBackupAt)
        }
        loadAuth()
    }

    fun loadAuth() = update { it.copy(isLoaded = true) }

    fun setAuth(token: String, user: AuthUser) = update { it.copy(token = token, user = user) }

    fun clearAuth() = update { it.copy(token = null, user = null, lastBackupAt = null) }

    fun setLastBackupAt(date: String) = update { it.copy(lastBackupAt = date) }
}
