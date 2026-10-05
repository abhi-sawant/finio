package com.slowatcoding.finio.platform.backup

import javax.crypto.SecretKey
import kotlinx.serialization.json.JsonObject

/*
 * The seams between the platform backup workers and the app's real stores. The app layer
 * implements these over core FinanceStore / AuthState / BackupCryptoState and exposes them by
 * making its `Application` implement [AutoBackupHost] — WorkManager instantiates workers itself,
 * so that is the one place a worker can reach the live stores from.
 */

/** Read side of the finance store, as `backup.ts` uses `useFinanceStore.getState()`. */
interface BackupDataSource {
    /**
     * False when accounts, transactions, budgets, recurring, goals and people are ALL empty —
     * the web's "nothing worth backing up" guard (a fresh install must never overwrite a real
     * cloud backup with an empty one).
     */
    suspend fun hasBackupWorthyData(): Boolean

    /** `collectBackupPayload()` — the 16 backup keys, WITHOUT `withBackupMeta`. */
    suspend fun payload(): JsonObject

    /** `settings.autoLocalBackup`. */
    suspend fun autoLocalBackupEnabled(): Boolean

    /** `lastLocalBackupAt` — a `yyyy-MM-dd` day key, or null. */
    suspend fun lastLocalBackupAt(): String?

    suspend fun setLastLocalBackupAt(dayKey: String)
}

/** The auth + backup-crypto slice `uploadBackup`/`autoBackupIfNeeded` read. */
interface CloudBackupSession {
    /** JWT, or null when signed out. */
    suspend fun token(): String?

    /** ISO timestamp of the last successful upload (`useAuthStore.lastBackupAt`). */
    suspend fun lastBackupAt(): String?

    suspend fun setLastBackupAt(iso: String)

    /**
     * E2EE state. [CloudEncryption.Ready] only when a session key is cached in memory AND was
     * derived for the configured salt (web `hasUsableSessionKey`).
     */
    suspend fun encryption(): CloudEncryption
}

sealed interface CloudEncryption {
    /** Encryption off — upload the plaintext payload. */
    data object Off : CloudEncryption

    /** Encryption on but no usable session key — background uploads skip silently. */
    data object Locked : CloudEncryption

    /** Encryption on and unlocked for this session. */
    data class Ready(val key: SecretKey, val salt: String, val iterations: Int) : CloudEncryption
}

/** Implemented by the app's `Application`. Return null while the stores aren't ready. */
interface AutoBackupHost {
    fun backupDataSource(): BackupDataSource?
    fun cloudBackupSession(): CloudBackupSession?
}
