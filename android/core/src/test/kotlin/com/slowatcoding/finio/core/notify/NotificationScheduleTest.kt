package com.slowatcoding.finio.core.notify

import com.slowatcoding.finio.core.js.hours
import com.slowatcoding.finio.core.js.localDate
import com.slowatcoding.finio.core.js.msToInstant
import com.slowatcoding.finio.core.js.toIso
import com.slowatcoding.finio.core.model.Account
import com.slowatcoding.finio.core.model.AccountType
import com.slowatcoding.finio.core.model.Budget
import com.slowatcoding.finio.core.model.BudgetPeriod
import com.slowatcoding.finio.core.model.RecurringTransaction
import com.slowatcoding.finio.core.model.RecurrenceFrequency
import com.slowatcoding.finio.core.model.Transaction
import com.slowatcoding.finio.core.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// Behavioural checks from notificationSchedule.test.ts that the golden fixtures only imply —
// above all the dedupe contract: ids must not move with the rebuild time.
class NotificationScheduleTest {
    private val now = localDate(2026, 5, 15, 12)
    private fun isoFromNow(days: Int, h: Int = 9) = localDate(2026, 5, 15 + days, h).toIso()

    private val prefs = NotificationPrefs(
        notificationsEnabled = true, notifyBills = true, notifyBudgets = true, notifyCreditDue = true,
        notifyLeadDays = 2, notifyDailyLog = true, hideAmounts = false,
    )

    private fun rule(id: String, startDate: String) = RecurringTransaction(
        id = id, type = TransactionType.Expense, amount = 500.0, accountId = "acc-1", categoryId = "cat-bills",
        note = "Broadband", frequency = RecurrenceFrequency.Monthly, startDate = startDate,
        createdAt = "2026-01-01T00:00:00.000Z",
    )

    private fun input(monthStartDay: Int = 1) = NotificationScheduleInput(
        recurring = listOf(rule("r1", isoFromNow(5))),
        budgets = listOf(Budget("b1", "cat-food", null, 100.0, BudgetPeriod.Monthly, false, "2026-01-01T00:00:00.000Z")),
        transactions = listOf(
            Transaction(
                id = "t1", type = TransactionType.Expense, amount = 900.0, accountId = "acc-1",
                categoryId = "cat-food", date = isoFromNow(-1), note = "", createdAt = isoFromNow(-1),
            ),
        ),
        accounts = listOf(
            Account(
                "acc-card", "HDFC Card", AccountType.Credit, "#000", "credit-card", -5000.0, 0.0,
                "2026-01-01T00:00:00.000Z", creditLimit = 50000.0, statementCloseDay = 20, paymentDueDays = 15,
            ),
        ),
        categories = emptyList(),
        labels = emptyList(),
        monthStartDay = monthStartDay,
        prefs = prefs,
    )

    @Test
    fun idsAreStableAcrossRebuildsOnTheSameDay() {
        val first = buildNotificationSchedule(input(), now).map { it.id }.sorted()
        val later = buildNotificationSchedule(input(), localDate(2026, 5, 15, 18, 30)).map { it.id }.sorted()
        assertTrue(first.isNotEmpty())
        assertEquals(first, later)
    }

    @Test
    fun idsUseTheWireFormat() {
        val ids = buildNotificationSchedule(input(), now).map { it.id }.toSet()
        assertTrue("bill:r1:2026-06-20" in ids)
        assertTrue("budget:b1:${localDate(2026, 5, 1).toIso()}:over" in ids)
        assertTrue(ids.any { it.startsWith("credit:acc-card:") })
        assertTrue("daily:log:2026-06-15" in ids)
    }

    @Test
    fun billFiresAtNotifyHourOnTheLeadDay() {
        val bill = buildNotificationSchedule(input(), now).first { it.kind == NotificationKind.Bill }
        assertEquals(NOTIFY_HOUR, msToInstant(bill.fireAt).hours)
        assertEquals(localDate(2026, 5, 18, NOTIFY_HOUR).toEpochMilli(), bill.fireAt)
    }

    @Test
    fun budgetIdFollowsMonthStartDay() {
        val a = buildNotificationSchedule(input(1), now).first { it.kind == NotificationKind.Budget }.id
        val b = buildNotificationSchedule(input(25), now).first { it.kind == NotificationKind.Budget }.id
        assertNotEquals(a, b)
    }

    @Test
    fun masterSwitchSilencesEverything() {
        val off = input().copy(prefs = prefs.copy(notificationsEnabled = false))
        assertEquals(emptyList<ScheduledNotification>(), buildNotificationSchedule(off, now))
    }
}
