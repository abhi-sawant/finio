package com.slowatcoding.finio.core.backup

import com.slowatcoding.finio.core.js.nowInstant
import com.slowatcoding.finio.core.js.toIso
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import java.time.Instant
import kotlin.math.floor

// Port of web/src/utils/backupMeta.ts — provenance stamped onto local backup files (manual
// export and the auto-backup folder). Cloud uploads are left alone.

/** Bump when a backup's shape changes in a way an older app would misread. */
const val BACKUP_SCHEMA_VERSION = 1

data class BackupMeta(
    val version: Int? = null,
    /** ISO timestamp of when the file was written. */
    val exportedAt: String? = null,
)

/**
 * `{ version, exportedAt, ...payload }` — the stamp comes first, and a payload key of the same
 * name wins while keeping that first position (JS object-spread semantics).
 */
fun withBackupMeta(payload: JsonObject, now: Instant = nowInstant()): JsonObject {
    val out = LinkedHashMap<String, JsonElement>()
    out["version"] = JsonPrimitive(BACKUP_SCHEMA_VERSION)
    out["exportedAt"] = JsonPrimitive(now.toIso())
    out.putAll(payload)
    return JsonObject(out)
}

/**
 * Reads the stamp back out of an untrusted parsed file. Each field is independent: a legacy file
 * yields an empty meta, and a malformed value is dropped rather than trusted.
 *
 * `exportedAt` is accepted when V8's `Date.parse` would accept it — see [jsDateParses].
 */
fun readBackupMeta(raw: JsonElement?): BackupMeta {
    if (raw !is JsonObject) return BackupMeta()
    var version: Int? = null
    var exportedAt: String? = null
    (raw["version"] as? JsonPrimitive)?.takeIf { !it.isString }?.doubleOrNull?.let { v ->
        if (v.isFinite() && floor(v) == v && v >= 1) version = v.toInt()
    }
    (raw["exportedAt"] as? JsonPrimitive)?.takeIf { it.isString }?.content?.let { s ->
        if (jsDateParses(s)) exportedAt = s
    }
    return BackupMeta(version, exportedAt)
}

private val ISO_LIKE = Regex(
    """^([+-]\d{6}|\d{4})(?:-(\d{2})(?:-(\d{2}))?)?(?:[Tt ](\d{2}):(\d{2})(?::(\d{2})(?:\.\d+)?)?([Zz]|[+-](\d{2}):?(\d{2}))?)?$""",
)

/**
 * `!Number.isNaN(Date.parse(s))` for the ISO family V8 accepts — which is laxer than java.time:
 * any day 1–31 in any month ("2026-02-31" is 3 Mar), year-only and year-month forms, a space or
 * lower-case `t` separator, `24:00`. V8's legacy free-form fallback ("Oct 5 2026", even "12") is
 * not reproduced; backup files only ever carry `toISOString()` output.
 */
internal fun jsDateParses(raw: String): Boolean {
    val m = ISO_LIKE.matchEntire(raw.trim()) ?: return false
    val g = m.groupValues
    fun num(i: Int) = g[i].takeIf { it.isNotEmpty() }?.toInt()
    num(2)?.let { if (it !in 1..12) return false }
    num(3)?.let { if (it !in 1..31) return false }
    val hour = num(4); val minute = num(5); val second = num(6) ?: 0
    if (hour != null && minute != null) {
        if (hour > 24 || minute > 59 || second > 59) return false
        if (hour == 24 && (minute != 0 || second != 0)) return false
    }
    num(8)?.let { if (it > 23) return false }
    num(9)?.let { if (it > 59) return false }
    return true
}
