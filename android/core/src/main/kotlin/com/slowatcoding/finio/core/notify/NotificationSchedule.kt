package com.slowatcoding.finio.core.notify

import com.slowatcoding.finio.core.calc.BudgetHealth
import com.slowatcoding.finio.core.calc.BudgetPeriodOptions
import com.slowatcoding.finio.core.calc.activeAccounts
import com.slowatcoding.finio.core.calc.budgetHealth
import com.slowatcoding.finio.core.calc.computeBudgetStatuses
import com.slowatcoding.finio.core.calc.getCreditCardDueInfo
import com.slowatcoding.finio.core.format.formatCurrency
import com.slowatcoding.finio.core.js.addDays
import com.slowatcoding.finio.core.js.format
import com.slowatcoding.finio.core.js.jsRound
import com.slowatcoding.finio.core.js.parseJsDate
import com.slowatcoding.finio.core.js.setHours
import com.slowatcoding.finio.core.js.startOfDay
import com.slowatcoding.finio.core.js.subDays
import com.slowatcoding.finio.core.js.toIso
import com.slowatcoding.finio.core.model.Account
import com.slowatcoding.finio.core.model.AccountType
import com.slowatcoding.finio.core.model.Budget
import com.slowatcoding.finio.core.model.Category
import com.slowatcoding.finio.core.model.Label
import com.slowatcoding.finio.core.model.RecurringTransaction
import com.slowatcoding.finio.core.model.Transaction
import com.slowatcoding.finio.core.store.futureOccurrences
import com.slowatcoding.finio.core.util.jsTrim
import java.time.Instant
import kotlin.math.max

// Port of web/src/utils/notificationSchedule.ts — turns the user's data into a flat list of dated
// reminders.
//
// Pure and `now`-taking, because the schedule is rebuilt from scratch on every app open and the
// ids must come out identical each time (and identical to the web's — spec/backup-format.md §6).
// That stability *is* the dedupe contract: nothing in an id may derive from "when we rebuilt".

data class NotificationScheduleInput(
    val recurring: List<RecurringTransaction>,
    val budgets: List<Budget>,
    val transactions: List<Transaction>,
    val accounts: List<Account>,
    val categories: List<Category>,
    val labels: List<Label>,
    val monthStartDay: Int,
    val prefs: NotificationPrefs,
)

/** `yyyy-MM-dd` in local time — the occurrence's identity, independent of the lead time. */
private fun dayKey(date: Instant): String = format(date, "yyyy-MM-dd")

/**
 * When to actually show a reminder for something due on [dueDate]: NOTIFY_HOUR on the lead day,
 * clamped forward to [now] so a missed lead window is late rather than lost. Clamping here (and
 * never in the id) is what lets the lead time change without re-firing anything.
 */
private fun leadFireAt(dueDate: Instant, leadDays: Int, now: Instant): Long {
    val target = setHours(startOfDay(subDays(dueDate, leadDays)), NOTIFY_HOUR)
    return max(now.toEpochMilli(), target.toEpochMilli())
}

private fun relativeDueLabel(dueDate: Instant, now: Instant): String {
    val days = jsRound(
        (startOfDay(dueDate).toEpochMilli() - startOfDay(now).toEpochMilli()) / (24.0 * 60 * 60 * 1000),
    ).toLong()
    if (days <= 0) return "today"
    if (days == 1L) return "tomorrow"
    return "in $days days"
}

private fun money(amount: Double, prefs: NotificationPrefs): String =
    formatCurrency(amount, compact = false, hidden = prefs.hideAmounts)

private fun buildBillEntries(input: NotificationScheduleInput, now: Instant): List<ScheduledNotification> {
    val prefs = input.prefs
    if (!prefs.notifyBills) return emptyList()

    val horizonEnd = addDays(now, NOTIFICATION_HORIZON_DAYS)
    val entries = mutableListOf<ScheduledNotification>()

    for (rule in input.recurring) {
        // `from = now`: this runs after processRecurring(), so anything due at or before now is
        // already a real transaction. futureOccurrences honours pause / endDate / maxOccurrences.
        for (occurrence in futureOccurrences(rule, now, horizonEnd)) {
            val categoryName = input.categories.firstOrNull { it.id == rule.categoryId }?.name ?: "Recurring"
            val name = jsTrim(rule.note).ifEmpty { categoryName }

            entries += ScheduledNotification(
                id = "bill:${rule.id}:${dayKey(occurrence)}",
                kind = NotificationKind.Bill,
                fireAt = leadFireAt(occurrence, prefs.notifyLeadDays, now),
                expiresAt = startOfDay(addDays(occurrence, 1)).toEpochMilli(),
                title = "Bill due ${relativeDueLabel(occurrence, now)}",
                body = "$name · ${money(rule.amount, prefs)}",
                url = "/recurring",
            )
        }
    }
    return entries
}

private fun buildBudgetEntries(input: NotificationScheduleInput, now: Instant): List<ScheduledNotification> {
    val prefs = input.prefs
    if (!prefs.notifyBudgets) return emptyList()

    val statuses = computeBudgetStatuses(
        input.budgets,
        input.transactions,
        BudgetPeriodOptions(monthStartDay = input.monthStartDay, now = now),
    )
    val entries = mutableListOf<ScheduledNotification>()

    for (status in statuses) {
        val health = budgetHealth(status)
        if (health == BudgetHealth.Ok) continue

        val budget = status.budget
        val labelId = budget.labelId
        val scopeName = when {
            !labelId.isNullOrEmpty() -> input.labels.firstOrNull { it.id == labelId }?.name ?: "Budget"
            budget.categoryId.isNotEmpty() ->
                input.categories.firstOrNull { it.id == budget.categoryId }?.name ?: "Budget"
            else -> "Overall"
        }

        entries += ScheduledNotification(
            // Keyed by the period start (re-arms when the period rolls, follows monthStartDay)
            // and by severity (near → over is a second reminder, each still sent once).
            id = "budget:${budget.id}:${status.range.start.toIso()}:${health.wire}",
            kind = NotificationKind.Budget,
            fireAt = now.toEpochMilli(),
            expiresAt = status.range.end.toEpochMilli() + 1,
            title = if (health == BudgetHealth.Over) "$scopeName is over budget" else "$scopeName is near its limit",
            body = "${money(status.spent, prefs)} of ${money(status.limit, prefs)} spent",
            url = "/budgets",
        )
    }
    return entries
}

private fun buildCreditEntries(input: NotificationScheduleInput, now: Instant): List<ScheduledNotification> {
    val prefs = input.prefs
    if (!prefs.notifyCreditDue) return emptyList()

    val entries = mutableListOf<ScheduledNotification>()
    for (account in activeAccounts(input.accounts).filter { it.type == AccountType.Credit }) {
        // Null unless the card has a statement cycle configured and something outstanding.
        val dueInfo = getCreditCardDueInfo(account, now) ?: continue

        entries += ScheduledNotification(
            id = "credit:${account.id}:${dayKey(dueInfo.dueDate)}",
            kind = NotificationKind.Credit,
            fireAt = leadFireAt(dueInfo.dueDate, prefs.notifyLeadDays, now),
            // A day longer than a bill: an already-overdue card is still worth surfacing.
            expiresAt = startOfDay(addDays(dueInfo.dueDate, 2)).toEpochMilli(),
            title = if (dueInfo.isOverdue) "${account.name} payment is overdue"
            else "${account.name} payment due ${relativeDueLabel(dueInfo.dueDate, now)}",
            body = "${money(dueInfo.outstanding, prefs)} outstanding · min ${money(dueInfo.minimumDue, prefs)}",
            url = "/accounts",
        )
    }
    return entries
}

/** A once-daily evening nudge to log the day's transactions, skipped once one is logged today. */
private fun buildDailyLogEntries(input: NotificationScheduleInput, now: Instant): List<ScheduledNotification> {
    val prefs = input.prefs
    if (!prefs.notifyDailyLog) return emptyList()

    val today = dayKey(now)
    // `format(new Date(t.createdAt))` throws a RangeError on an unparseable createdAt in the web;
    // mirrored here so both clients fail the same way.
    val alreadyLogged = input.transactions.any { t ->
        val created = parseJsDate(t.createdAt) ?: throw IllegalArgumentException("Invalid time value")
        dayKey(created) == today
    }
    if (alreadyLogged) return emptyList()

    return listOf(
        ScheduledNotification(
            id = "daily:log:$today",
            kind = NotificationKind.Daily,
            fireAt = max(now.toEpochMilli(), setHours(startOfDay(now), DAILY_LOG_HOUR).toEpochMilli()),
            expiresAt = startOfDay(addDays(now, 1)).toEpochMilli(),
            title = "Log today's transactions",
            body = "A quick add keeps your balances accurate.",
            url = "/add-transaction",
        ),
    )
}

fun buildNotificationSchedule(input: NotificationScheduleInput, now: Instant): List<ScheduledNotification> {
    if (!input.prefs.notificationsEnabled) return emptyList()
    return buildBillEntries(input, now) +
        buildBudgetEntries(input, now) +
        buildCreditEntries(input, now) +
        buildDailyLogEntries(input, now)
}
