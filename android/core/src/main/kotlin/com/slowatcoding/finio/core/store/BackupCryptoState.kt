package com.slowatcoding.finio.core.store

import com.slowatcoding.finio.core.model.BackupCryptoConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import javax.crypto.SecretKey

// Port of web/src/store/useBackupCryptoStore.ts (`finio-backup-crypto`). Key material must never
// sit inside the payload it protects, so this lives apart from the finance state, and the derived
// session key is held in memory only — [PersistedBackupCrypto] carries the config and nothing else.

/** What is persisted — the web `partialize` allow-list. */
@Serializable
data class PersistedBackupCrypto(val config: BackupCryptoConfig? = null)

data class BackupCryptoUiState(
    /** Null when backup encryption has never been set up, or has been disabled. */
    val config: BackupCryptoConfig? = null,
    /** Cached for this process so automatic backups can run without a prompt. Never persisted. */
    val sessionKey: SecretKey? = null,
    /** The salt [sessionKey] was derived from, so a rotated passphrase reads as "locked". */
    val sessionKeySalt: String? = null,
)

class BackupCryptoState(private val persist: (PersistedBackupCrypto) -> Unit = {}) {
    private val lock = Any()
    private val _state = MutableStateFlow(BackupCryptoUiState())
    val state: StateFlow<BackupCryptoUiState> = _state.asStateFlow()
    val current: BackupCryptoUiState get() = _state.value

    private fun update(f: (BackupCryptoUiState) -> BackupCryptoUiState) = synchronized(lock) {
        val before = _state.value
        val after = f(before)
        _state.value = after
        if (after.config != before.config) persist(PersistedBackupCrypto(after.config))
    }

    /** Load the persisted config. Every session starts with no cached key. */
    fun hydrate(persisted: PersistedBackupCrypto?) = synchronized(lock) {
        _state.value = BackupCryptoUiState(config = persisted?.config)
    }

    fun setConfig(config: BackupCryptoConfig) = update { it.copy(config = config) }

    fun clearConfig() = update { BackupCryptoUiState() }

    fun setSessionKey(key: SecretKey?, salt: String?) = update { it.copy(sessionKey = key, sessionKeySalt = salt) }

    /** The cached key, if it was derived from [salt] — `getCachedKey` in web/src/services/backup.ts. */
    fun cachedKey(salt: String): SecretKey? = current.let { if (it.sessionKey != null && it.sessionKeySalt == salt) it.sessionKey else null }

    /** Encryption is on but this session holds no usable key ("Cloud backup locked"). */
    val isLocked: Boolean
        get() = current.config?.let { it.enabled && cachedKey(it.salt) == null } ?: false
}
