package com.slowatcoding.finio.platform.backup

import android.content.Context
import com.slowatcoding.finio.core.backup.withBackupMeta
import com.slowatcoding.finio.core.crypto.BackupEnvelope
import com.slowatcoding.finio.core.crypto.encryptJson
import com.slowatcoding.finio.core.crypto.packEnvelope
import com.slowatcoding.finio.core.format.todayKey
import com.slowatcoding.finio.core.js.toIso
import com.slowatcoding.finio.platform.api.FinioApi
import com.slowatcoding.finio.platform.files.BackupFolder
import com.slowatcoding.finio.platform.files.MAX_LOCAL_BACKUPS
import com.slowatcoding.finio.platform.files.backupFileName
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean

/*
 * Port of the automatic halves of web/src/services/backup.ts (`autoLocalBackupIfNeeded`,
 * `uploadBackup`, `autoBackupIfNeeded`). Called by the workers and by the app on start/resume.
 * Restore/decrypt (`decodeBackupResponse`) stays with the app layer's Settings flows.
 */
object AutoBackup {

    /** Pretty JSON with 2-space indent — the web's `JSON.stringify(data, null, 2)`. */
    @OptIn(ExperimentalSerializationApi::class)
    val PrettyJson: Json = Json {
        prettyPrint = true
        prettyPrintIndent = "  "
    }

    /** Envelope encoding must keep `v`/`enc`/`kdf` defaults — they mark it as encrypted. */
    private val EnvelopeJson = Json { encodeDefaults = true }

    private val backupInProgress = AtomicBoolean(false)

    enum class LocalOutcome { Written, Skipped, NoFolder, Failed }

    /**
     * Daily local backup into the user's chosen SAF folder. Unlike the web there is no download
     * fallback: with no folder (or a revoked grant) it skips silently — never prompts — and does
     * NOT stamp `lastLocalBackupAt`, so it runs as soon as a folder is granted.
     */
    suspend fun autoLocalBackupIfNeeded(
        context: Context,
        source: BackupDataSource,
        folder: BackupFolder = BackupFolder(context),
        today: String = todayKey(),
    ): LocalOutcome {
        if (!shouldRunLocalBackup(
                enabled = source.autoLocalBackupEnabled(),
                hasData = source.hasBackupWorthyData(),
                lastLocalBackupAt = source.lastLocalBackupAt(),
                today = today,
            )
        ) return LocalOutcome.Skipped
        if (!folder.isGranted()) return LocalOutcome.NoFolder
        return try {
            val data = withBackupMeta(source.payload())
            folder.writeBackupAndRotate(
                backupFileName(today),
                PrettyJson.encodeToString(JsonObject.serializer(), data),
                MAX_LOCAL_BACKUPS,
            )
            source.setLastLocalBackupAt(today)
            LocalOutcome.Written
        } catch (_: Exception) {
            LocalOutcome.Failed // best-effort, like the web's silent catch
        }
    }

    /**
     * `uploadBackup()` — also the manual "Back up now". Throws when signed out or E2EE-locked,
     * with the web's messages; network/API errors propagate (IOException / ApiException).
     * Returns the ISO timestamp stamped into `lastBackupAt`.
     */
    suspend fun uploadBackup(
        source: BackupDataSource,
        session: CloudBackupSession,
        api: FinioApi,
        now: Instant = Instant.now(),
    ): String {
        val token = session.token() ?: throw IllegalStateException("Not signed in")
        val payload = source.payload()
        val body: String = when (val enc = session.encryption()) {
            CloudEncryption.Off -> Json.encodeToString(JsonObject.serializer(), payload)
            CloudEncryption.Locked ->
                throw IllegalStateException("Cloud backup is locked — enter your passphrase to continue.")
            is CloudEncryption.Ready -> {
                val sealed = encryptJson(enc.key, payload)
                val envelope = packEnvelope(enc.salt, enc.iterations, sealed.iv, sealed.ciphertext)
                EnvelopeJson.encodeToString(BackupEnvelope.serializer(), envelope)
            }
        }
        api.uploadBackup(token, body)
        val stamp = now.toIso()
        session.setLastBackupAt(stamp)
        return stamp
    }

    /**
     * `autoBackupIfNeeded()`: at most once per 24h, never prompting. Returns true if it
     * uploaded. Errors from the upload itself propagate (the worker treats them as best-effort).
     */
    suspend fun autoBackupIfNeeded(
        source: BackupDataSource,
        session: CloudBackupSession,
        api: FinioApi,
        now: Long = System.currentTimeMillis(),
    ): Boolean {
        if (!backupInProgress.compareAndSet(false, true)) return false
        try {
            val token = session.token()
            val run = shouldRunCloudBackup(
                hasToken = token != null,
                encryptionLocked = token != null && session.encryption() is CloudEncryption.Locked,
                hasData = token != null && source.hasBackupWorthyData(),
                lastBackupAt = session.lastBackupAt(),
                now = now,
            )
            if (!run) return false
            uploadBackup(source, session, api, Instant.ofEpochMilli(now))
            return true
        } finally {
            backupInProgress.set(false)
        }
    }
}
