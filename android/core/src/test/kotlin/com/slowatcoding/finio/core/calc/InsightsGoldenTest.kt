package com.slowatcoding.finio.core.calc

import com.slowatcoding.finio.core.format.formatCurrency
import com.slowatcoding.finio.core.golden.Golden
import com.slowatcoding.finio.core.golden.d
import com.slowatcoding.finio.core.golden.s
import com.slowatcoding.finio.core.model.Account
import com.slowatcoding.finio.core.model.Budget
import com.slowatcoding.finio.core.model.Category
import com.slowatcoding.finio.core.model.Label
import com.slowatcoding.finio.core.model.RecurringTransaction
import com.slowatcoding.finio.core.model.Transaction
import com.slowatcoding.finio.core.model.wireName
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.Test

class InsightsGoldenTest {
    private val ledger = FixtureLedger("insights")

    private val formatters: Map<String, (Double) -> String> = mapOf(
        "currency" to { v -> formatCurrency(v) },
        "hidden" to { v -> formatCurrency(v, hidden = true) },
        "plain" to { v -> "Rs${jsNumberString(v)}" },
    )

    private fun SubscriptionCandidate.json(): JsonElement = buildJsonObject {
        put("key", JsonPrimitive(key))
        put("note", JsonPrimitive(note))
        put("amount", JsonPrimitive(amount))
        put("frequency", JsonPrimitive(wireName(frequency)))
        put("occurrences", JsonPrimitive(occurrences))
        put("accountId", JsonPrimitive(accountId))
        put("categoryId", JsonPrimitive(categoryId))
        put("labels", JsonArray(labels.map(::JsonPrimitive)))
        put("lastDate", JsonPrimitive(lastDate))
        put("nextDate", JsonPrimitive(nextDate))
    }

    private fun Insight.json(): JsonElement = buildJsonObject {
        put("id", JsonPrimitive(id))
        put("kind", JsonPrimitive(kind.wire))
        put("severity", JsonPrimitive(severity.wire))
        put("title", JsonPrimitive(title))
        put("detail", JsonPrimitive(detail))
        when (val a = action) {
            is InsightAction.CreateRecurring -> put("action", buildJsonObject {
                put("type", JsonPrimitive("create-recurring")); put("candidate", a.candidate.json())
            })
            is InsightAction.Navigate -> put("action", buildJsonObject {
                put("type", JsonPrimitive("navigate")); put("to", JsonPrimitive(a.to)); put("label", JsonPrimitive(a.label))
            })
            null -> Unit
        }
    }

    private inline fun <reified T> JsonObject.field(k: String): T = Golden.decode(ledger.resolve(getValue(k)))

    @Test
    fun matchesTypeScript() = Golden.verify("insights") { c ->
        when (c.fn) {
            "ledger" -> c.out
            "normalizeNote" -> JsonPrimitive(normalizeNote(c.arg(0).s))
            "amountsMatch" -> JsonPrimitive(amountsMatch(c.arg(0).d, c.arg(1).d))
            "detectSubscriptions" -> JsonArray(
                detectSubscriptions(
                    ledger.decodeArg<List<Transaction>>(c, 0),
                    ledger.decodeArg<List<RecurringTransaction>>(c, 1),
                    c.arg(2).instantArg,
                ).map { it.json() },
            )
            "buildInsights" -> c.arg(0).jsonObject.let { o ->
                val input = InsightInput(
                    transactions = o.field<List<Transaction>>("transactions"),
                    categories = o.field<List<Category>>("categories"),
                    labels = o.field<List<Label>>("labels"),
                    budgets = o.field<List<Budget>>("budgets"),
                    recurring = o.field<List<RecurringTransaction>>("recurring"),
                    accounts = if ("accounts" in o) o.field<List<Account>>("accounts") else null,
                    now = o["now"]?.instantArg,
                    monthStartDay = (o["monthStartDay"] as? JsonPrimitive)?.content?.toDouble()?.toInt(),
                    limit = (o["limit"] as? JsonPrimitive)?.content?.toDouble()?.toInt(),
                )
                JsonArray(buildInsights(input, formatters.getValue(c.arg(1).s)).map { it.json() })
            }
            else -> null
        }
    }
}
