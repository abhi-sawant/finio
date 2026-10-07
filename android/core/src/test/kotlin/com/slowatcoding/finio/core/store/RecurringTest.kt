package com.slowatcoding.finio.core.store

import com.slowatcoding.finio.core.js.iso
import com.slowatcoding.finio.core.js.toIso
import com.slowatcoding.finio.core.model.RecurrenceFrequency
import com.slowatcoding.finio.core.model.RecurringTransaction
import com.slowatcoding.finio.core.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// The parts of web/src/store/recurring.test.ts that are about the *API*, not values the golden
// grid pins (occurrence identity, infinite allowance, pass-to-pass behaviour).
class RecurringTest {
    private val now = iso("2026-06-15T12:00:00.000Z")
    private fun rule(id: String, freq: RecurrenceFrequency = RecurrenceFrequency.Monthly, start: String = "2026-01-10T00:00:00.000Z", max: Int? = null) =
        RecurringTransaction(id = id, type = TransactionType.Expense, amount = 100.0, accountId = "acc-1", categoryId = "cat-1",
            note = "", frequency = freq, startDate = start, maxOccurrences = max, createdAt = "2026-01-01T00:00:00.000Z")

    @Test fun unlimitedRuleHasInfiniteAllowance() {
        assertTrue(remainingOccurrences(rule("r")).isInfinite())
        assertEquals(0.0, remainingOccurrences(rule("r", max = 2).copy(occurrenceCount = 5)), 0.0)
    }

    @Test fun occurrencesCarryTheOriginalRule() {
        val r = rule("r")
        val plan = planRecurring(listOf(r), listOf("acc-1"), now)
        assertEquals(6, plan.occurrences.size)
        assertTrue(plan.occurrences.all { it.rule === r })
        assertEquals("2026-06-10T00:00:00.000Z", plan.rules[0].lastRunDate)
        assertEquals(6, plan.rules[0].occurrenceCount)
    }

    @Test fun cappedRuleFinishesOnLaterPasses() {
        var rules = listOf(rule("daily", RecurrenceFrequency.Daily, "2025-01-01T00:00:00.000Z"))
        val first = planRecurring(rules, listOf("acc-1"), now)
        assertEquals(MAX_OCCURRENCES_PER_RULE, first.occurrences.size)
        assertEquals(listOf("daily"), first.cappedRuleIds)
        rules = first.rules
        val second = planRecurring(rules, listOf("acc-1"), now)
        assertEquals(listOf<String>(), second.cappedRuleIds)
        assertEquals(now.toIso().substring(0, 10), second.rules[0].lastRunDate!!.substring(0, 10))
    }

    @Test fun startFromTodayLeavesNothingToBackfill() {
        val r = rule("r")
        val skipped = r.copy(lastRunDate = lastOccurrenceOnOrBefore(r, now)!!.toIso())
        assertEquals(0, previewBackfill(skipped, listOf("acc-1"), now).count)
        assertEquals(600.0, previewBackfill(r, listOf("acc-1"), now).total, 0.0)
    }
}
