package com.slowatcoding.finio.core.store

import com.slowatcoding.finio.core.golden.Golden
import com.slowatcoding.finio.core.golden.i
import com.slowatcoding.finio.core.golden.s
import com.slowatcoding.finio.core.model.RecurrenceFrequency
import com.slowatcoding.finio.core.model.RecurringTransaction
import com.slowatcoding.finio.core.period.instant
import com.slowatcoding.finio.core.period.json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.Test

class RecurringGoldenTest {
    private fun rule(e: JsonElement): RecurringTransaction = Golden.decode(e)
    private fun ids(e: JsonElement) = e.jsonArray.map { it.s }

    private fun plan(p: RecurringPlan): JsonElement = buildJsonObject {
        put("occurrences", JsonArray(p.occurrences.map { o ->
            buildJsonObject { put("ruleId", JsonPrimitive(o.rule.id)); put("date", o.date.json()) }
        }))
        put("rules", Golden.encode(p.rules))
        put("cappedRuleIds", JsonArray(p.cappedRuleIds.map(::JsonPrimitive)))
    }

    @Test
    fun matchesTypeScript() = Golden.verify("recurring") { c ->
        when (c.fn) {
            "MAX_OCCURRENCES_PER_RULE" -> JsonPrimitive(MAX_OCCURRENCES_PER_RULE)
            "nextOccurrence" -> nextOccurrence(c.arg(0).instant, Golden.decode<RecurrenceFrequency>(c.arg(1))).json()
            "isRulePaused" -> JsonPrimitive(isRulePaused(rule(c.arg(0))))
            "remainingOccurrences" -> remainingOccurrences(rule(c.arg(0))).let { if (it.isInfinite()) JsonNull else JsonPrimitive(it) }
            "nextDueDate" -> nextDueDate(rule(c.arg(0))).json()
            "isRuleFinished" -> JsonPrimitive(isRuleFinished(rule(c.arg(0))))
            "lastOccurrenceOnOrBefore" -> c.arg(0).jsonObject.let { o ->
                if (o.containsKey("id")) lastOccurrenceOnOrBefore(rule(o), c.arg(1).instant)
                else lastOccurrenceOnOrBefore(o.getValue("startDate").s, Golden.decode(o.getValue("frequency")), c.arg(1).instant)
            }.json()
            "previewBackfill" -> previewBackfill(rule(c.arg(0)), ids(c.arg(1)), c.arg(2).instant).let {
                buildJsonObject {
                    put("count", JsonPrimitive(it.count)); put("total", JsonPrimitive(it.total))
                    put("firstDate", it.firstDate.json()); put("lastDate", it.lastDate.json())
                    put("capped", JsonPrimitive(it.capped))
                }
            }
            "planRecurring" -> plan(planRecurring(c.arg(0).jsonArray.map(::rule), ids(c.arg(1)), c.arg(2).instant))
            "futureOccurrences" -> {
                val r = rule(c.arg(0))
                val dates = if (c.args.size > 3) futureOccurrences(r, c.arg(1).instant, c.arg(2).instant, c.arg(3).i)
                else futureOccurrences(r, c.arg(1).instant, c.arg(2).instant)
                JsonArray(dates.map { it.json() })
            }
            else -> null
        }
    }
}
