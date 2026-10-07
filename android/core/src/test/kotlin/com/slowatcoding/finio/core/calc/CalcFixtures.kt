package com.slowatcoding.finio.core.calc

import com.slowatcoding.finio.core.golden.Golden
import com.slowatcoding.finio.core.golden.s
import com.slowatcoding.finio.core.js.iso
import com.slowatcoding.finio.core.model.Account
import com.slowatcoding.finio.core.model.Budget
import com.slowatcoding.finio.core.model.Category
import com.slowatcoding.finio.core.model.Label
import com.slowatcoding.finio.core.model.NetWorthSnapshot
import com.slowatcoding.finio.core.model.RecurringTransaction
import com.slowatcoding.finio.core.model.Transaction
import com.slowatcoding.finio.core.period.PeriodRange
import com.slowatcoding.finio.core.period.PeriodType
import com.slowatcoding.finio.core.period.json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.Instant

// Shared by the calc golden tests. Each calc fixture emits the shared ledger once as a `ledger`
// case; other cases point into it with "@ledger.<field>" or "@tx:<id>,<id>" (see
// web/src/spec/fixtures/calculations.fixture.ts).

class FixtureLedger(module: String) {
    val root: JsonObject = Golden.cases(module).first { it.fn == "ledger" }.out.jsonObject
    private val txById: Map<String, JsonElement> =
        root.getValue("transactions").jsonArray.associateBy { it.jsonObject.getValue("id").s }

    val accounts: List<Account> = Golden.decode(root.getValue("accounts"))
    val transactions: List<Transaction> = Golden.decode(root.getValue("transactions"))
    val categories: List<Category> = Golden.decode(root.getValue("categories"))
    val labels: List<Label> = Golden.decode(root.getValue("labels"))
    val budgets: List<Budget> = Golden.decode(root.getValue("budgets"))
    val recurring: List<RecurringTransaction> = Golden.decode(root.getValue("recurring"))
    val snapshots: List<NetWorthSnapshot> = Golden.decode(root.getValue("snapshots"))

    /** Expand a "@ledger." / "@tx:" reference; anything else is returned as-is. */
    fun resolve(e: JsonElement): JsonElement {
        val p = e as? JsonPrimitive ?: return e
        if (!p.isString) return e
        val v = p.content
        return when {
            v.startsWith("@ledger.") -> root.getValue(v.removePrefix("@ledger."))
            v.startsWith("@tx:") -> JsonArray(v.removePrefix("@tx:").split(',').filter { it.isNotEmpty() }.map { txById.getValue(it) })
            else -> e
        }
    }

    fun arg(c: Golden.Case, i: Int): JsonElement = resolve(c.arg(i))
    inline fun <reified T> decodeArg(c: Golden.Case, i: Int): T = Golden.decode(arg(c, i))
}

fun ids(rows: List<Transaction>): JsonElement = JsonArray(rows.map { JsonPrimitive(it.id) })

val JsonElement.instantArg: Instant get() = iso(jsonPrimitive.content)

fun JsonElement.rangeArg(): PeriodRange = jsonObject.let {
    PeriodRange(
        type = Golden.decode<PeriodType>(it.getValue("type")),
        start = it.getValue("start").instantArg,
        end = it.getValue("end").instantArg,
    )
}

fun JsonElement.optionsArg(): BudgetPeriodOptions = (this as? JsonObject)?.let { o ->
    BudgetPeriodOptions(
        monthStartDay = (o["monthStartDay"] as? JsonPrimitive)?.content?.toDouble()?.toInt(),
        now = (o["now"] as? JsonPrimitive)?.let { iso(it.content) },
    )
} ?: BudgetPeriodOptions()

fun nullable(v: JsonElement?): JsonElement = v ?: JsonNull

fun BudgetPeriodResult.json(): JsonElement = buildJsonObject {
    put("range", range.json())
    put("spent", JsonPrimitive(spent))
    put("limit", JsonPrimitive(limit))
    put("isOver", JsonPrimitive(isOver))
}

fun BudgetStatus.json(): JsonElement = buildJsonObject {
    put("budget", JsonPrimitive(budget.id))
    put("range", range.json())
    put("spent", JsonPrimitive(spent))
    put("carryover", JsonPrimitive(carryover))
    put("limit", JsonPrimitive(limit))
    put("remaining", JsonPrimitive(remaining))
    put("percent", JsonPrimitive(percent))
    put("isOver", JsonPrimitive(isOver))
}
