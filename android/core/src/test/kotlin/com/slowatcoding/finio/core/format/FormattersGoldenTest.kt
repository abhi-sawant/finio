package com.slowatcoding.finio.core.format

import com.slowatcoding.finio.core.golden.Golden
import com.slowatcoding.finio.core.golden.b
import com.slowatcoding.finio.core.golden.d
import com.slowatcoding.finio.core.golden.i
import com.slowatcoding.finio.core.golden.s
import com.slowatcoding.finio.core.js.iso
import com.slowatcoding.finio.core.js.msToInstant
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test

class FormattersGoldenTest {
    /** A clock far from every fixture date, so formatDate never says Today/Yesterday — like the TS run. */
    private val farNow = msToInstant(4_102_444_800_000) // 2100-01-01

    @Test
    fun matchesTypeScript() = Golden.verify("formatters") { c ->
        when (c.fn) {
            "formatCurrency" -> {
                val opts = c.arg(3).jsonObject
                JsonPrimitive(
                    formatCurrency(
                        c.arg(0).d, c.arg(1).b, c.arg(2).b,
                        precise = opts["precise"]?.jsonPrimitive?.booleanOrNull ?: true,
                        forceCompact = opts["forceCompact"]?.jsonPrimitive?.booleanOrNull ?: false,
                    ),
                )
            }
            "shouldCompactGroup" -> JsonPrimitive(shouldCompactGroup(c.arg(0).jsonArray.map { it.d }))
            "formatFullDate" -> JsonPrimitive(formatFullDate(c.arg(0).s))
            // Date args arrive as ISO strings; the "date" cases also exercise the Instant overloads.
            "formatShortDate" -> JsonPrimitive(if (c.name == "date") formatShortDate(iso(c.arg(0).s)) else formatShortDate(c.arg(0).s))
            "formatDayMonth" -> JsonPrimitive(if (c.name == "date") formatDayMonth(iso(c.arg(0).s)) else formatDayMonth(c.arg(0).s))
            "toLocalDateTimeInputValue" -> JsonPrimitive(
                if (c.name == "date") toLocalDateTimeInputValue(iso(c.arg(0).s)) else toLocalDateTimeInputValue(c.arg(0).s),
            )
            "formatTime" -> JsonPrimitive(formatTime(c.arg(0).s))
            "localDayKey" -> JsonPrimitive(localDayKey(c.arg(0).s))
            "formatDate" -> JsonPrimitive(formatDate(c.arg(0).s, farNow))
            "todayKey" -> JsonPrimitive(todayKey(iso(c.arg(0).s)))
            "formatInputAmount" -> JsonPrimitive(formatInputAmount(c.arg(0).s))
            "formatOrdinal" -> JsonPrimitive(formatOrdinal(c.arg(0).i))
            "formatFileSize" -> JsonPrimitive(formatFileSize(c.arg(0).d))
            "formatPercentChange" -> JsonPrimitive(formatPercentChange(c.arg(0).d))
            else -> null
        }
    }
}
