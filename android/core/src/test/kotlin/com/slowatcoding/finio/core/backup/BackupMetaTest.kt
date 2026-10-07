package com.slowatcoding.finio.core.backup

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.junit.Assert.assertEquals
import org.junit.Test

class BackupMetaTest {
    @Test fun roundTripsAStampedFile() {
        val stamped = withBackupMeta(buildJsonObject { put("accounts", JsonArray(emptyList())) })
        val meta = readBackupMeta(stamped)
        assertEquals(BACKUP_SCHEMA_VERSION, meta.version)
        assertEquals((stamped["exportedAt"] as JsonPrimitive).content, meta.exportedAt)
        assertEquals(listOf("version", "exportedAt", "accounts"), stamped.keys.toList())
    }

    @Test fun nonObjectsYieldNothing() {
        assertEquals(BackupMeta(), readBackupMeta(null))
        assertEquals(BackupMeta(), readBackupMeta(JsonNull))
        assertEquals(BackupMeta(), readBackupMeta(JsonArray(listOf(JsonPrimitive(1)))))
    }

    @Test fun knownGapLegacyDateStrings() {
        // V8 accepts these through its legacy parser; the port deliberately does not (see jsDateParses).
        assertEquals(null, readBackupMeta(buildJsonObject { put("exportedAt", JsonPrimitive("Oct 5 2026")) }).exportedAt)
    }
}
