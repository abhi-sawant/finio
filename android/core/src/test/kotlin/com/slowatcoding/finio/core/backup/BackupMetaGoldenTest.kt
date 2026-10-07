package com.slowatcoding.finio.core.backup

import com.slowatcoding.finio.core.golden.Golden
import com.slowatcoding.finio.core.period.instant
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.Test

class BackupMetaGoldenTest {
    @Test
    fun matchesTypeScript() = Golden.verify("backupMeta") { c ->
        when (c.fn) {
            "BACKUP_SCHEMA_VERSION" -> JsonPrimitive(BACKUP_SCHEMA_VERSION)
            // Out is Object.entries(...) so key order is asserted too.
            "withBackupMeta" -> JsonArray(
                withBackupMeta(c.arg(0).jsonObject, c.arg(1).instant).entries.map { (k, v) -> JsonArray(listOf(JsonPrimitive(k), v)) },
            )
            "readBackupMeta" -> readBackupMeta(c.arg(0)).let { m ->
                buildJsonObject {
                    m.version?.let { put("version", JsonPrimitive(it)) }
                    m.exportedAt?.let { put("exportedAt", JsonPrimitive(it)) }
                }
            }
            else -> null
        }
    }
}
