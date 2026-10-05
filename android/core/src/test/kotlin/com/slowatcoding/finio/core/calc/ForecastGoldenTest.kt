package com.slowatcoding.finio.core.calc

import com.slowatcoding.finio.core.golden.Golden
import com.slowatcoding.finio.core.golden.d
import com.slowatcoding.finio.core.golden.s
import com.slowatcoding.finio.core.model.Account
import com.slowatcoding.finio.core.model.RecurringTransaction
import com.slowatcoding.finio.core.model.Transaction
import com.slowatcoding.finio.core.model.TransactionType
import com.slowatcoding.finio.core.period.json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.Test

class ForecastGoldenTest {
    private val ledger = FixtureLedger("forecast")

    private fun JsonObject.intOrNull(k: String): Int? = (this[k] as? JsonPrimitive)?.content?.toDouble()?.toInt()
    private inline fun <reified T> JsonObject.field(k: String): T = Golden.decode(ledger.resolve(getValue(k)))

    private fun ScheduledFlow.json() = buildJsonObject {
        put("ruleId", JsonPrimitive(ruleId))
        put("note", JsonPrimitive(note))
        put("categoryId", JsonPrimitive(categoryId))
        put("date", date.json())
        put("type", JsonPrimitive(type.wire))
        put("amount", JsonPrimitive(amount))
        put("delta", JsonPrimitive(delta))
    }

    private fun decodeFlow(e: JsonElement): ScheduledFlow = e.jsonObject.let { o ->
        ScheduledFlow(
            ruleId = o.getValue("ruleId").s,
            note = o.getValue("note").s,
            categoryId = o.getValue("categoryId").s,
            date = o.getValue("date").instantArg,
            type = Golden.decode<TransactionType>(o.getValue("type")),
            amount = o.getValue("amount").d,
            delta = o.getValue("delta").d,
        )
    }

    private fun CategoryAverage.json() = buildJsonObject {
        put("categoryId", JsonPrimitive(categoryId))
        put("dailyAverage", JsonPrimitive(dailyAverage))
        put("monthlyAverage", JsonPrimitive(monthlyAverage))
        put("share", JsonPrimitive(share))
    }

    private fun CategoryDailyAverages.json() = buildJsonObject {
        put("averages", JsonArray(averages.map { it.json() }))
        put("dailyEstimate", JsonPrimitive(dailyEstimate))
        put("lookbackDays", JsonPrimitive(lookbackDays))
    }

    private fun ForecastPoint.json() = buildJsonObject {
        put("date", date.json())
        put("key", JsonPrimitive(key))
        put("balance", JsonPrimitive(balance))
        put("scheduledIn", JsonPrimitive(scheduledIn))
        put("scheduledOut", JsonPrimitive(scheduledOut))
        put("estimatedOut", JsonPrimitive(estimatedOut))
    }

    private fun CashFlowForecast.json() = buildJsonObject {
        put("startBalance", JsonPrimitive(startBalance))
        put("endBalance", JsonPrimitive(endBalance))
        put("points", JsonArray(points.map { it.json() }))
        put("scheduled", JsonArray(scheduled.map { it.json() }))
        put("categoryAverages", JsonArray(categoryAverages.map { it.json() }))
        put("dailyEstimate", JsonPrimitive(dailyEstimate))
        put("lookbackDays", JsonPrimitive(lookbackDays))
        put("totals", buildJsonObject {
            put("scheduledIn", JsonPrimitive(totals.scheduledIn))
            put("scheduledOut", JsonPrimitive(totals.scheduledOut))
            put("estimatedOut", JsonPrimitive(totals.estimatedOut))
        })
        put("low", low?.let { l -> buildJsonObject { put("date", l.date.json()); put("balance", JsonPrimitive(l.balance)) } } ?: JsonNull)
        put("shortfallDate", shortfallDate.json())
        put("isEmpty", JsonPrimitive(isEmpty))
    }

    @Test
    fun matchesTypeScript() = Golden.verify("forecast") { c ->
        when (c.fn) {
            "ledger" -> c.out
            "constants" -> buildJsonObject {
                put("DEFAULT_FORECAST_DAYS", JsonPrimitive(DEFAULT_FORECAST_DAYS))
                put("DEFAULT_LOOKBACK_DAYS", JsonPrimitive(DEFAULT_LOOKBACK_DAYS))
            }
            "liquidAccountIds" -> JsonArray(liquidAccountIds(ledger.decodeArg(c, 0)).map(::JsonPrimitive))
            "liquidBalance" -> JsonPrimitive(liquidBalance(ledger.decodeArg(c, 0)))
            "liquidDelta" -> {
                val o = c.arg(0).jsonObject
                JsonPrimitive(
                    liquidDelta(
                        Golden.decode<TransactionType>(o.getValue("type")),
                        o.getValue("amount").d,
                        o.getValue("accountId").s,
                        o["toAccountId"]?.s,
                        liquidAccountIds(ledger.decodeArg<List<Account>>(c, 1)),
                    ),
                )
            }
            "categoryDailyAverages" -> {
                val o = c.arg(2).jsonObject
                categoryDailyAverages(
                    ledger.decodeArg<List<Transaction>>(c, 0),
                    ledger.decodeArg<List<Account>>(c, 1),
                    now = o["now"]?.instantArg,
                    lookbackDays = o.intOrNull("lookbackDays"),
                    recurring = if ("recurring" in o) o.field<List<RecurringTransaction>>("recurring") else null,
                ).json()
            }
            "buildCashFlowForecast" -> {
                val o = c.arg(0).jsonObject
                buildCashFlowForecast(
                    ForecastInput(
                        accounts = o.field("accounts"),
                        transactions = o.field("transactions"),
                        recurring = o.field("recurring"),
                        now = o["now"]?.instantArg,
                        days = o.intOrNull("days"),
                        lookbackDays = o.intOrNull("lookbackDays"),
                    ),
                ).json()
            }
            "buildCashFlowCalendarMonth" -> {
                val r = buildCashFlowCalendarMonth(
                    c.arg(0).jsonArray.map(::decodeFlow),
                    c.arg(1).rangeArg(),
                    c.arg(2).instantArg,
                )
                buildJsonObject {
                    put("weeks", JsonArray(r.weeks.map { w ->
                        JsonArray(w.map { d ->
                            buildJsonObject {
                                put("date", d.date.json())
                                put("key", JsonPrimitive(d.key))
                                put("netFlow", JsonPrimitive(d.netFlow))
                                put("flows", JsonArray(d.flows.map { it.json() }))
                                put("inRange", JsonPrimitive(d.inRange))
                                put("isToday", JsonPrimitive(d.isToday))
                            }
                        })
                    }))
                }
            }
            else -> null
        }
    }
}
