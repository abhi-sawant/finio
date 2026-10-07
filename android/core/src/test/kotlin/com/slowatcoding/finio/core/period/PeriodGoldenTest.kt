package com.slowatcoding.finio.core.period

import com.slowatcoding.finio.core.golden.Golden
import com.slowatcoding.finio.core.golden.i
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.Test

class PeriodGoldenTest {
    private fun type(e: JsonElement): PeriodType = Golden.decode(e)
    private fun range(e: JsonElement): PeriodRange = e.jsonObject.let {
        PeriodRange(type(it.getValue("type")), it.getValue("start").instant, it.getValue("end").instant)
    }

    @Test
    fun matchesTypeScript() = Golden.verify("period") { c ->
        when (c.fn) {
            "PERIOD_TYPES" -> JsonArray(PERIOD_TYPES.map { Golden.encode(it) })
            "PERIOD_LABELS" -> buildJsonObject { PERIOD_LABELS.forEach { (k, v) -> put(Golden.encode(k).let { (it as JsonPrimitive).content }, JsonPrimitive(v)) } }
            "normalizeMonthStartDay" -> JsonPrimitive(normalizeMonthStartDay(c.arg(0)))
            "monthPeriodStart" -> monthPeriodStart(c.arg(0).instant, c.arg(1).i).json()
            "yearPeriodStart" -> yearPeriodStart(c.arg(0).instant, c.arg(1).i).json()
            "periodStart" -> periodStart(type(c.arg(0)), c.arg(1).instant, c.arg(2).i).json()
            "periodRange" -> periodRange(type(c.arg(0)), c.arg(1).instant, c.arg(2).i).json()
            "addPeriods" -> addPeriods(type(c.arg(0)), c.arg(1).instant, c.arg(2).i).json()
            "shiftPeriod" -> shiftPeriod(range(c.arg(0)), c.arg(1).i).json()
            "daysInPeriod" -> JsonPrimitive(daysInPeriod(range(c.arg(0))))
            "isWithinPeriod" -> JsonPrimitive(isWithinPeriod(c.arg(0).instant, range(c.arg(1))))
            "daysElapsedInPeriod" -> JsonPrimitive(daysElapsedInPeriod(range(c.arg(0)), c.arg(1).instant))
            "periodLabel" -> JsonPrimitive(periodLabel(range(c.arg(0)), c.arg(1).i))
            "periodShortLabel" -> JsonPrimitive(periodShortLabel(range(c.arg(0))))
            else -> null
        }
    }
}
