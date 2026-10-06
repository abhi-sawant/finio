package com.slowatcoding.finio.ui.screens.settings

import com.slowatcoding.finio.core.crypto.createVerifier
import com.slowatcoding.finio.core.crypto.decodeEnvelope
import com.slowatcoding.finio.core.crypto.decryptJson
import com.slowatcoding.finio.core.crypto.deriveEncryptionKey
import com.slowatcoding.finio.core.importing.validateBackup
import com.slowatcoding.finio.core.js.nowInstant
import com.slowatcoding.finio.core.js.toIso
import com.slowatcoding.finio.core.model.BackupCryptoConfig
import com.slowatcoding.finio.core.model.ImportMode
import com.slowatcoding.finio.di.AppContainer
import com.slowatcoding.finio.platform.api.BackupListEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonElement
import javax.crypto.SecretKey

/*
 * The restore half of web/src/services/backup.ts (`decodeBackupResponse`, `restoreLatestBackup`,
 * `restoreBackupByDate`, `listCloudBackups`, `deleteCloudBackup`, `PassphraseRequiredError`) —
 * the Settings-only flows the platform layer leaves to the app. Upload lives in
 * platform `AutoBackup.uploadBackup`.
 */

/**
 * Thrown when a cloud restore hits an encrypted backup and no usable key is cached — the
 * Backup section catches this specifically to prompt for the passphrase and retry.
 */
class PassphraseRequiredError : Exception("This backup is encrypted. Enter your passphrase to restore it.")

object CloudRestore {

    private fun AppContainer.requireToken(): String =
        auth.current.token?.takeIf { it.isNotEmpty() } ?: throw IllegalStateException("Not signed in")

    /**
     * Decrypts an encrypted envelope (legacy plaintext passes straight through). A key derived
     * from [passphrase] that doesn't match the cached session key is adopted as the session key,
     * and local config is seeded from the envelope's salt/iterations when it doesn't match —
     * which is what makes a restore on a brand-new device self-healing.
     */
    suspend fun decodeBackupResponse(container: AppContainer, raw: JsonElement, passphrase: String?): JsonElement {
        val envelope = decodeEnvelope(raw) ?: return raw
        val crypto = container.backupCrypto

        val cached = crypto.cachedKey(envelope.salt)
        val key: SecretKey = cached ?: run {
            if (passphrase.isNullOrEmpty()) throw PassphraseRequiredError()
            withContext(Dispatchers.Default) { deriveEncryptionKey(passphrase, envelope.salt, envelope.iterations) }
        }

        val plaintext = try {
            withContext(Dispatchers.Default) { decryptJson(key, envelope.iv, envelope.ciphertext) }
        } catch (_: Exception) {
            throw IllegalStateException("Incorrect passphrase")
        }

        if (cached == null) {
            crypto.setSessionKey(key, envelope.salt)
            val config = crypto.current.config
            if (config == null || config.salt != envelope.salt) {
                val verifier = withContext(Dispatchers.Default) { createVerifier(key) }
                crypto.setConfig(
                    BackupCryptoConfig(
                        enabled = true,
                        salt = envelope.salt,
                        iterations = envelope.iterations,
                        verifierIv = verifier.iv,
                        verifierCiphertext = verifier.ciphertext,
                        createdAt = nowInstant().toIso(),
                    ),
                )
            }
        }
        return plaintext
    }

    /** Validates (cloud payloads get the same checks as a picked file) and replaces local data. */
    private suspend fun importDecoded(container: AppContainer, decoded: JsonElement) {
        val validated = withContext(Dispatchers.Default) { validateBackup(decoded) }
        container.financeStore.importData(validated.data, ImportMode.Replace)
        // The pending reminder schedule points at the budgets and rules that were just replaced.
        container.refreshReminders()
    }

    suspend fun restoreLatestBackup(container: AppContainer, passphrase: String? = null) {
        val token = container.requireToken()
        val res = container.api.getLatestBackup(token)
        importDecoded(container, decodeBackupResponse(container, res, passphrase))
    }

    suspend fun restoreBackupByDate(container: AppContainer, date: String, passphrase: String? = null) {
        val token = container.requireToken()
        val res = container.api.getBackup(token, date)
        importDecoded(container, decodeBackupResponse(container, res, passphrase))
    }

    suspend fun listCloudBackups(container: AppContainer): List<BackupListEntry> =
        container.api.listBackups(container.requireToken()).backups

    suspend fun deleteCloudBackup(container: AppContainer, date: String) {
        container.api.deleteBackup(container.requireToken(), date)
    }
}
