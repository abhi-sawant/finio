package com.slowatcoding.finio.core.calc

import com.slowatcoding.finio.core.golden.Golden
import com.slowatcoding.finio.core.golden.b
import com.slowatcoding.finio.core.golden.d
import com.slowatcoding.finio.core.golden.i
import com.slowatcoding.finio.core.golden.s
import com.slowatcoding.finio.core.model.Account
import com.slowatcoding.finio.core.model.Transaction
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
import org.junit.Test

class AnalyticsGoldenTest {
    private val ledger = FixtureLedger("analytics")

    private fun JsonObject.intOrNull(k: String): Int? = (this[k] as? JsonPrimitive)?.content?.toDouble()?.toInt()
    private inline fun <reified T> JsonObject.field(k: String): T = Golden.decode(ledger.resolve(getValue(k)))

    private fun CategoryTotal.json() = buildJsonObject {
        put("categoryId", JsonPrimitive(categoryId)); put("amount", JsonPrimitive(amount))
    }

    private fun PeriodSummary.json(): JsonElement = buildJsonObject {
        put("range", range.json())
        put("label", JsonPrimitive(label))
        put("income", JsonPrimitive(income))
        put("expenses", JsonPrimitive(expenses))
        put("net", JsonPrimitive(net))
        put("transactionCount", JsonPrimitive(transactionCount))
        put("categoryTotals", JsonArray(categoryTotals.map { it.json() }))
        put("isPartial", JsonPrimitive(isPartial))
        put("projectedExpenses", JsonPrimitive(projectedExpenses))
    }

    private fun decodeSummary(e: JsonElement): PeriodSummary = e.jsonObject.let { o ->
        PeriodSummary(
            range = o.getValue("range").rangeArg(),
            label = o.getValue("label").s,
            income = o.getValue("income").d,
            expenses = o.getValue("expenses").d,
            net = o.getValue("net").d,
            transactionCount = o.getValue("transactionCount").i,
            categoryTotals = o.getValue("categoryTotals").jsonArray.map {
                CategoryTotal(it.jsonObject.getValue("categoryId").s, it.jsonObject.getValue("amount").d)
            },
            isPartial = o.getValue("isPartial").b,
            projectedExpenses = o.getValue("projectedExpenses").d,
        )
    }

    private fun CategoryMovement.json() = buildJsonObject {
        put("categoryId", JsonPrimitive(categoryId))
        put("current", JsonPrimitive(current))
        put("previous", JsonPrimitive(previous))
        put("change", JsonPrimitive(change))
        put("percentChange", percentChange?.let(::JsonPrimitive) ?: JsonNull)
    }

    private fun CalendarDay.json() = buildJsonObject {
        put("date", date.json())
        put("key", JsonPrimitive(key))
        put("total", JsonPrimitive(total))
        put("transactionCount", JsonPrimitive(transactionCount))
        put("intensity", JsonPrimitive(intensity))
        put("inRange", JsonPrimitive(inRange))
        put("isFuture", JsonPrimitive(isFuture))
        put("isToday", JsonPrimitive(isToday))
    }

    private fun MonthTotal.json() = buildJsonObject {
        put("key", JsonPrimitive(key)); put("label", JsonPrimitive(label))
        put("income", JsonPrimitive(income)); put("expenses", JsonPrimitive(expenses))
    }

    @Test
    fun matchesTypeScript() = Golden.verify("analytics") { c ->
        when (c.fn) {
            "ledger" -> c.out
            "summarizePeriod" -> {
                val o = c.arg(2).jsonObject
                summarizePeriod(
                    ledger.decodeArg<List<Transaction>>(c, 0),
                    c.arg(1).rangeArg(),
                    label = o["label"]?.s,
                    now = o["now"]?.instantArg,
                    monthStartDay = o.intOrNull("monthStartDay"),
                ).json()
            }
            "buildPeriodComparison" -> {
                val o = c.arg(1).jsonObject
                val r = buildPeriodComparison(
                    ledger.decodeArg<List<Transaction>>(c, 0),
                    type = o["type"]?.let { Golden.decode<PeriodType>(it) },
                    now = o["now"]?.instantArg,
                    monthStartDay = o.intOrNull("monthStartDay"),
                )
                buildJsonObject {
                    put("current", r.current.json())
                    put("previous", r.previous.json())
                    put("lastYear", r.lastYear?.json() ?: JsonNull)
                }
            }
            "categoryMovements" -> JsonArray(
                categoryMovements(decodeSummary(c.arg(0)), decodeSummary(c.arg(1)), c.arg(2).i).map { it.json() },
            )
            "buildSpendingCalendar" -> {
                val r = buildSpendingCalendar(ledger.decodeArg(c, 0), c.arg(1).rangeArg(), c.arg(2).instantArg)
                buildJsonObject {
                    put("range", r.range.json())
                    put("label", JsonPrimitive(r.label))
                    put("weeks", JsonArray(r.weeks.map { w -> JsonArray(w.map { it.json() }) }))
                    put("total", JsonPrimitive(r.total))
                    put("max", JsonPrimitive(r.max))
                    put("daysWithSpend", JsonPrimitive(r.daysWithSpend))
                    put("averagePerActiveDay", JsonPrimitive(r.averagePerActiveDay))
                    put("busiest", r.busiest?.json() ?: JsonNull)
                }
            }
            "buildYearInReview" -> {
                val o = c.arg(0).jsonObject
                val r = buildYearInReview(
                    YearInReviewInput(
                        transactions = o.field<List<Transaction>>("transactions"),
                        accounts = o.field<List<Account>>("accounts"),
                        now = o["now"]?.instantArg,
                        monthStartDay = o.intOrNull("monthStartDay"),
                        yearOffset = o.intOrNull("yearOffset"),
                    ),
                )
                buildJsonObject {
                    put("range", r.range.json())
                    put("label", JsonPrimitive(r.label))
                    put("current", r.current.json())
                    put("previous", r.previous.json())
                    put("topCategories", JsonArray(r.topCategories.map { it.json() }))
                    put("movers", JsonArray(r.movers.map { it.json() }))
                    put("monthlyBreakdown", JsonArray(r.monthlyBreakdown.map { it.json() }))
                    put("busiestMonth", r.busiestMonth?.json() ?: JsonNull)
                    put("netWorthStart", JsonPrimitive(r.netWorthStart))
                    put("netWorthEnd", JsonPrimitive(r.netWorthEnd))
                    put("netWorthChange", JsonPrimitive(r.netWorthChange))
                    put("biggestExpense", r.biggestExpense?.let { JsonPrimitive(it.id) } ?: JsonNull)
                }
            }
            else -> null
        }
    }
}
