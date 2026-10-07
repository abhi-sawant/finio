package com.slowatcoding.finio.core.calc

import com.slowatcoding.finio.core.js.iso
import com.slowatcoding.finio.core.js.isSameDay
import com.slowatcoding.finio.core.model.Account
import com.slowatcoding.finio.core.model.AccountType
import com.slowatcoding.finio.core.model.RecurrenceFrequency
import com.slowatcoding.finio.core.model.RecurringTransaction
import com.slowatcoding.finio.core.model.Transaction
import com.slowatcoding.finio.core.model.TransactionType
import com.slowatcoding.finio.core.period.PeriodType
import com.slowatcoding.finio.core.period.periodRange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Behavioural checks from analytics.test.ts / forecast.test.ts, readable on their own; the golden
// fixtures (AnalyticsGoldenTest, ForecastGoldenTest) are the parity proof.
class AnalyticsForecastTest {
    private val now = iso("2026-06-15T12:00:00.000Z")

    private fun tx(type: TransactionType, amount: Double, date: String, categoryId: String = "cat-food", accountId: String = "checking") =
        Transaction(
            id = "$type-$amount-$date", type = type, amount = amount, accountId = accountId,
            categoryId = categoryId, date = date, note = "", createdAt = date,
        )

    private fun checking(balance: Double) = Account(
        "checking", "checking", AccountType.Checking, "#000", "landmark", balance, 0.0, "2026-01-01T00:00:00.000Z",
    )

    private fun rule(id: String, amount: Double, type: TransactionType = TransactionType.Expense) = RecurringTransaction(
        id = id, type = type, amount = amount, accountId = "checking", categoryId = "cat-bills", note = "Rent",
        frequency = RecurrenceFrequency.Monthly, startDate = "2026-06-20T00:00:00.000Z", createdAt = "2026-01-01T00:00:00.000Z",
    )

    @Test
    fun partialPeriodIsPacedToAFullOne() {
        val range = periodRange(PeriodType.Monthly, now)
        val rows = listOf(tx(TransactionType.Expense, 1500.0, "2026-06-01T00:00:00.000Z"))
        val partial = summarizePeriod(rows, range, now = now)
        assertTrue(partial.isPartial)
        assertEquals(3000.0, partial.projectedExpenses, 0.0)
        val finished = summarizePeriod(rows, range, now = iso("2026-08-01T00:00:00.000Z"))
        assertEquals(1500.0, finished.projectedExpenses, 0.0)
    }

    @Test
    fun movementsRankBySwingAndANewCategoryHasNoPercent() {
        val range = periodRange(PeriodType.Monthly, now)
        val current = summarizePeriod(
            listOf(
                tx(TransactionType.Expense, 1000.0, "2026-06-05T00:00:00.000Z", "food"),
                tx(TransactionType.Expense, 300.0, "2026-06-06T00:00:00.000Z", "new"),
            ),
            range, now = now,
        )
        val previous = summarizePeriod(
            listOf(tx(TransactionType.Expense, 400.0, "2026-05-05T00:00:00.000Z", "food")),
            periodRange(PeriodType.Monthly, iso("2026-05-15T12:00:00.000Z")), now = now,
        )
        val movers = categoryMovements(current, previous)
        assertEquals(listOf("food", "new"), movers.map { it.categoryId })
        assertEquals(1.5, movers[0].percentChange!!, 1e-12)
        assertNull(movers[1].percentChange)
    }

    @Test
    fun emptyYearHasNoBusiestMonth() {
        val review = buildYearInReview(YearInReviewInput(emptyList(), listOf(checking(5000.0)), now = now))
        assertNull(review.busiestMonth)
        assertNull(review.biggestExpense)
        assertEquals(12, review.monthlyBreakdown.size)
    }

    @Test
    fun forecastDrawsDownAndReportsShortfall() {
        val forecast = buildCashFlowForecast(
            ForecastInput(
                accounts = listOf(checking(1000.0)),
                transactions = listOf(tx(TransactionType.Expense, 100.0, "2026-06-15T00:00:00.000Z")),
                recurring = emptyList(),
                now = now,
                days = 30,
            ),
        )
        assertTrue(isSameDay(forecast.shortfallDate!!, iso("2026-06-26T00:00:00.000Z")))
        assertEquals(-2000.0, forecast.endBalance, 0.0)
        assertEquals(31, forecast.points.size)
    }

    @Test
    fun cardPaymentLeavesCashButInternalTransferNets() {
        val liquid = setOf("checking", "savings")
        assertEquals(0.0, liquidDelta(TransactionType.Transfer, 500.0, "checking", "savings", liquid), 0.0)
        assertEquals(-500.0, liquidDelta(TransactionType.Transfer, 500.0, "checking", "card", liquid), 0.0)
        assertEquals(0.0, liquidDelta(TransactionType.Expense, 100.0, "card", null, liquid), 0.0)
    }

    @Test
    fun calendarNetsSameDayFlows() {
        val forecast = buildCashFlowForecast(
            ForecastInput(
                accounts = listOf(checking(10000.0)),
                transactions = emptyList(),
                recurring = listOf(rule("rent", 2000.0), rule("salary", 50000.0, TransactionType.Income)),
                now = now,
                days = 30,
            ),
        )
        val day20 = buildCashFlowCalendarMonth(forecast.scheduled, periodRange(PeriodType.Monthly, now), now)
            .weeks.flatten().first { it.key == "2026-06-20" }
        assertEquals(48000.0, day20.netFlow, 0.0)
        assertEquals(2, day20.flows.size)
    }
}
