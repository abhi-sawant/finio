package com.slowatcoding.finio.core.importing

import com.slowatcoding.finio.core.golden.Golden
import com.slowatcoding.finio.core.js.parseJsDate
import com.slowatcoding.finio.core.model.FinioJson
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** Kotlin-side behaviour the golden fixtures can't express (representation, JVM regex quirks). */
class ImportValidationTest {
    private val now = parseJsDate("2026-10-05T09:00:00.000Z")!!

    @Test
    fun throwsTheTsMessage() {
        for (raw in listOf(null, JsonNull, JsonPrimitive(42), buildJsonArray { }, buildJsonObject { put("foo", 1) })) {
            try {
                validateBackup(raw, now); fail("expected a throw for $raw")
            } catch (e: InvalidBackupException) {
                assertEquals("Not a Finio backup file", e.message)
            }
        }
    }

    @Test
    fun missingOpeningBalanceIsReportedAsNullToTheStore() {
        val raw = FinioJson.parseToJsonElement(
            """{"accounts":[{"id":"a","name":"A","type":"cash","balance":10},{"id":"b","name":"B","type":"cash","balance":5,"openingBalance":3}]}""",
        )
        val v = validateBackup(raw, now)
        assertEquals(setOf("a"), v.data.accountsMissingOpeningBalance)
        val imported = v.data.importedAccounts()!!
        assertNull(imported[0].openingBalance)
        assertEquals(3.0, imported[1].openingBalance!!, 0.0)
    }

    @Test
    fun periodKeyWithTrailingNewlineIsRejectedLikeJs() {
        // A Java `$` matches before a final line terminator; JS's doesn't.
        val raw = FinioJson.parseToJsonElement(
            """{"netWorthSnapshots":[{"id":"s","periodKey":"2026-05\n","date":"2026-05-31T00:00:00.000Z","assets":1,"liabilities":0}]}""",
        )
        val v = validateBackup(raw, now)
        assertEquals(0, v.report.counts.getValue(ImportEntity.NetWorthSnapshots).accepted)
    }

    @Test
    fun numericStringsAndBooleansAreNotNumbers() {
        val raw = FinioJson.parseToJsonElement(
            """{"budgets":[{"id":"b1","categoryId":"","amount":"100"},{"id":"b2","categoryId":"","amount":true},{"id":"b3","categoryId":"","amount":1e400}]}""",
        )
        val v = validateBackup(raw, now)
        assertEquals(listOf("Budgets #1: amount is not a number", "Budgets #2: amount is not a number", "Budgets #3: amount is not a number"), v.report.issues)
        assertFalse(hasImportableData(v.report))
    }

    @Test
    fun sampleBackupFixtureImportsCleanly() {
        val file = Golden.cases("sampleBackup").single().out
        val v = validateBackup(file, now)
        assertTrue(v.report.issues.isEmpty())
        assertTrue(v.report.warnings.isEmpty())
        assertEquals(54, v.data.transactions!!.size)
        assertTrue(v.data.accountsMissingOpeningBalance.isEmpty())
        assertEquals(1, v.meta.version)
    }
}
