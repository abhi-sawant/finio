package com.slowatcoding.finio.core.importing

import com.slowatcoding.finio.core.golden.Golden
import com.slowatcoding.finio.core.golden.b
import com.slowatcoding.finio.core.golden.i
import com.slowatcoding.finio.core.js.parseJsDate
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.junit.Test

/** JSON in the shape the TS `ValidatedBackup` serializes to. */
internal fun encodeValidated(v: ValidatedBackup): JsonElement = buildJsonObject {
    val d = v.data
    put("data", buildJsonObject {
        d.accounts?.let { list ->
            put("accounts", JsonArray(list.map { a ->
                val o = Golden.encode(a).jsonObject
                // The TS keeps openingBalance absent; Kotlin carries a placeholder + the id set.
                if (a.id in d.accountsMissingOpeningBalance) JsonObject(o - "openingBalance") else o
            }))
        }
        d.transactions?.let { put("transactions", Golden.encode(it)) }
        d.categories?.let { put("categories", Golden.encode(it)) }
        d.labels?.let { put("labels", Golden.encode(it)) }
        d.budgets?.let { put("budgets", Golden.encode(it)) }
        d.recurring?.let { put("recurring", Golden.encode(it)) }
        d.templates?.let { put("templates", Golden.encode(it)) }
        d.rules?.let { put("rules", Golden.encode(it)) }
        d.goals?.let { put("goals", Golden.encode(it)) }
        d.goalContributions?.let { put("goalContributions", Golden.encode(it)) }
        d.people?.let { put("people", Golden.encode(it)) }
        d.debtEntries?.let { put("debtEntries", Golden.encode(it)) }
        d.netWorthSnapshots?.let { put("netWorthSnapshots", Golden.encode(it)) }
        d.loans?.let { put("loans", Golden.encode(it)) }
        d.loanPrepayments?.let { put("loanPrepayments", Golden.encode(it)) }
        d.settings?.let { put("settings", Golden.encode(it)) }
    })
    put("report", encodeReport(v.report))
    put("meta", buildJsonObject {
        v.meta.version?.let { put("version", it) }
        v.meta.exportedAt?.let { put("exportedAt", it) }
    })
}

internal fun encodeReport(r: ImportReport): JsonElement = buildJsonObject {
    put("counts", buildJsonObject {
        r.counts.forEach { (e, c) ->
            put(e.key, buildJsonObject {
                put("present", c.present); put("total", c.total); put("accepted", c.accepted); put("rejected", c.rejected)
            })
        }
    })
    put("hasSettings", r.hasSettings)
    put("issues", JsonArray(r.issues.map(::JsonPrimitive)))
    put("warnings", JsonArray(r.warnings.map(::JsonPrimitive)))
}

private fun decodeReport(e: JsonElement): ImportReport {
    val o = e.jsonObject
    val counts = o.getValue("counts").jsonObject
    return ImportReport(
        counts = IMPORT_ENTITIES.associateWith { entity ->
            val c = counts.getValue(entity.key).jsonObject
            EntityReport(c.getValue("present").b, c.getValue("total").i, c.getValue("accepted").i, c.getValue("rejected").i)
        },
        hasSettings = o.getValue("hasSettings").b,
        issues = o.getValue("issues").jsonArray.map { it.jsonPrimitive.content },
        warnings = o.getValue("warnings").jsonArray.map { it.jsonPrimitive.content },
    )
}

class ImportValidationGoldenTest {
    @Test
    fun matchesTypeScript() = Golden.verify("importValidation") { c ->
        when (c.fn) {
            "IMPORT_ENTITIES" -> JsonArray(IMPORT_ENTITIES.map { JsonPrimitive(it.key) })
            "ENTITY_LABELS" -> buildJsonObject { ENTITY_LABELS.forEach { (k, v) -> put(k.key, v) } }
            "validateBackup" -> {
                val now = parseJsDate(c.arg(1).jsonPrimitive.content)!!
                try {
                    encodeValidated(validateBackup(c.arg(0), now))
                } catch (e: InvalidBackupException) {
                    buildJsonObject { put("throws", e.message) }
                }
            }
            "hasImportableData" -> JsonPrimitive(hasImportableData(decodeReport(c.arg(0))))
            else -> null
        }
    }
}
