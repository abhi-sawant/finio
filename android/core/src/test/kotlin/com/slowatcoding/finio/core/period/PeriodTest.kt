package com.slowatcoding.finio.core.period

import com.slowatcoding.finio.core.js.localDate
import com.slowatcoding.finio.core.model.BudgetPeriod
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test

class PeriodTest {
    @Test fun budgetPeriodMapsOneToOne() {
        assertEquals(listOf(PeriodType.Weekly, PeriodType.Monthly, PeriodType.Yearly), BudgetPeriod.entries.map { it.toPeriodType() })
    }

    @Test fun normalizeAcceptsKotlinAndJsonNumbersOnly() {
        assertEquals(25, normalizeMonthStartDay(25))
        assertEquals(25, normalizeMonthStartDay(25.7))
        assertEquals(28, normalizeMonthStartDay(31L))
        assertEquals(1, normalizeMonthStartDay(-2f))
        assertEquals(25, normalizeMonthStartDay(JsonPrimitive(25)))
        assertEquals(1, normalizeMonthStartDay(JsonPrimitive("25")))
        assertEquals(1, normalizeMonthStartDay(JsonNull))
        assertEquals(1, normalizeMonthStartDay(null))
        assertEquals(1, normalizeMonthStartDay(Double.POSITIVE_INFINITY))
    }

    @Test fun salaryCycleLabel() {
        val r = periodRange(PeriodType.Monthly, localDate(2026, 6, 27, 12), 25)
        assertEquals("25 Jul – 24 Aug 2026", periodLabel(r, 25))
        assertEquals(31, daysInPeriod(r))
    }

    @Test fun differenceInMonthsQuirks() {
        // date-fns: 31 Jan → 1 Sep 2014 is 7 full months.
        assertEquals(7, differenceInMonths(localDate(2014, 8, 1), localDate(2014, 0, 31)))
        // Last day of a short month counts the month as full.
        assertEquals(1, differenceInMonths(localDate(2026, 1, 28), localDate(2026, 0, 31)))
        // …but not in reverse: date-fns is asymmetric here (the TS returns 0 too).
        assertEquals(0, differenceInMonths(localDate(2026, 0, 31), localDate(2026, 1, 28)))
        assertEquals(0, differenceInMonths(localDate(2026, 1, 27), localDate(2026, 0, 31)))
    }
}
