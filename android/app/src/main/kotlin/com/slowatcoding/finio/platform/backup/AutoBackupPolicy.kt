package com.slowatcoding.finio.platform.backup

import com.slowatcoding.finio.core.js.parseJsDate

/*
 * The pure decisions from web/src/services/backup.ts, separated so they are unit-tested.
 */

/** `autoLocalBackupIfNeeded`: enabled → has data → not already done today. */
fun shouldRunLocalBackup(
    enabled: Boolean,
    hasData: Boolean,
    lastLocalBackupAt: String?,
    today: String,
): Boolean = enabled && hasData && lastLocalBackupAt != today

/**
 * `autoBackupIfNeeded`, in the web's order: signed in → not E2EE-locked → has data → never
 * backed up, or ≥24h since [lastBackupAt]. An unparseable [lastBackupAt] is `NaN` hours on the
 * web, which fails `>= 24` — so it never uploads; mirrored faithfully.
 */
fun shouldRunCloudBackup(
    hasToken: Boolean,
    encryptionLocked: Boolean,
    hasData: Boolean,
    lastBackupAt: String?,
    now: Long,
): Boolean {
    if (!hasToken) return false
    if (encryptionLocked) return false
    if (!hasData) return false
    if (lastBackupAt == null) return true
    val last = parseJsDate(lastBackupAt) ?: return false
    val hoursSinceLast = (now - last.toEpochMilli()) / (1000.0 * 60 * 60)
    return hoursSinceLast >= 24
}
