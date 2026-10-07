package com.slowatcoding.finio.platform.files

/*
 * Pure half of web/src/services/backupFolder.ts `writeBackupAndRotate`: which files in the backup
 * folder are Finio auto-backups, and which of them fall outside the newest [keep].
 */

/** Same pattern as the web: only `finio-backup-YYYY-MM-DD.json` is ever rotated. */
val BACKUP_FILENAME_PATTERN = Regex("""^finio-backup-(\d{4}-\d{2}-\d{2})\.json$""")

/** Web: MAX_LOCAL_BACKUPS. */
const val MAX_LOCAL_BACKUPS = 10

/** `finio-backup-<day>.json` for a `yyyy-MM-dd` day key (core `todayKey()`). */
fun backupFileName(dayKey: String): String = "finio-backup-$dayKey.json"

/**
 * Names to delete: matching files sorted newest-first by their date, everything past [keep].
 * Anything not matching the pattern (the user's own files, `… (1).json` duplicates) is never
 * touched.
 */
fun selectStaleBackups(names: Iterable<String>, keep: Int = MAX_LOCAL_BACKUPS): List<String> =
    names
        .mapNotNull { name -> BACKUP_FILENAME_PATTERN.matchEntire(name)?.let { name to it.groupValues[1] } }
        .sortedByDescending { it.second }
        .drop(keep.coerceAtLeast(0))
        .map { it.first }
