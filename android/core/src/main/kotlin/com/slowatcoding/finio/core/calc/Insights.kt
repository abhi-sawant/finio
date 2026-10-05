package com.slowatcoding.finio.core.calc

import com.slowatcoding.finio.core.js.addDays
import com.slowatcoding.finio.core.js.addMonths
import com.slowatcoding.finio.core.js.addWeeks
import com.slowatcoding.finio.core.js.addYears
import com.slowatcoding.finio.core.js.format
import com.slowatcoding.finio.core.js.iso
import com.slowatcoding.finio.core.js.jsRound
import com.slowatcoding.finio.core.js.nowInstant
import com.slowatcoding.finio.core.js.parseIso
import com.slowatcoding.finio.core.js.toIso
import com.slowatcoding.finio.core.model.Account
import com.slowatcoding.finio.core.model.AccountType
import com.slowatcoding.finio.core.model.Budget
import com.slowatcoding.finio.core.model.Category
import com.slowatcoding.finio.core.model.Label
import com.slowatcoding.finio.core.model.RecurrenceFrequency
import com.slowatcoding.finio.core.model.RecurringTransaction
import com.slowatcoding.finio.core.model.Transaction
import com.slowatcoding.finio.core.model.TransactionType
import com.slowatcoding.finio.core.money.roundMoney
import com.slowatcoding.finio.core.period.DEFAULT_MONTH_START_DAY
import com.slowatcoding.finio.core.period.PeriodRange
import com.slowatcoding.finio.core.period.PeriodType
import com.slowatcoding.finio.core.period.daysElapsedInPeriod
import com.slowatcoding.finio.core.period.daysInPeriod
import com.slowatcoding.finio.core.period.periodRange
import com.slowatcoding.finio.core.period.shiftPeriod
import java.time.Instant
import kotlin.math.abs
import kotlin.math.max

// Port of web/src/utils/insights.ts — the insights feed ("Food is 40% above your 3-month
// average", "three ₹499 charges from Spotify, want a recurring rule?"). Everything is derived on
// the fly from the ledger; money never appears in copy except through the caller's
// `formatAmount`, so the hide-amounts toggle is honoured.

enum class InsightKind(val wire: String) {
    CategorySpike("category-spike"),
    CategoryDrop("category-drop"),
    Subscription("subscription"),
    BudgetOver("budget-over"),
    BudgetPace("budget-pace"),
    SavingsRate("savings-rate"),
    CategoryShare("category-share"),
    NegativeBalance("negative-balance"),
}

enum class InsightSeverity(val wire: String) { Warn("warn"), Info("info"), Good("good") }

sealed class InsightAction {
    data class CreateRecurring(val candidate: SubscriptionCandidate) : InsightAction()
    data class Navigate(val to: String, val label: String) : InsightAction()
}

data class Insight(
    /** Stable across rebuilds for the same underlying fact. */
    val id: String,
    val kind: InsightKind,
    val severity: InsightSeverity,
    val title: String,
    val detail: String,
    val action: InsightAction? = null,
)

data class SubscriptionCandidate(
    /** The normalized note the group was matched on. */
    val key: String,
    /** The most recent raw note, i.e. what the user actually typed. */
    val note: String,
    val amount: Double,
    val frequency: RecurrenceFrequency,
    val occurrences: Int,
    val accountId: String,
    val categoryId: String,
    val labels: List<String>,
    /** ISO date of the most recent charge. */
    val lastDate: String,
    /** ISO date of the next expected charge, always in the future (never backfills). */
    val nextDate: String,
)

/** Charges must repeat at least this many times before they look like a subscription. */
private const val MIN_SUBSCRIPTION_OCCURRENCES = 3

/** How far back subscription detection looks. */
private const val SUBSCRIPTION_LOOKBACK_DAYS = 400

/** Amounts count as "the same charge" within this fraction of each other. */
private const val AMOUNT_TOLERANCE = 0.05

/** Absolute slack for small amounts, where 5% is less than a rupee or two. */
private const val AMOUNT_TOLERANCE_FLOOR = 2.0

private data class CadenceWindow(val frequency: RecurrenceFrequency, val min: Int, val max: Int)

/** Day gaps that read as a given cadence. Fortnightly and quarterly have no rule frequency. */
private val CADENCE_WINDOWS = listOf(
    CadenceWindow(RecurrenceFrequency.Weekly, 6, 8),
    CadenceWindow(RecurrenceFrequency.Monthly, 26, 35),
    CadenceWindow(RecurrenceFrequency.Yearly, 350, 380),
)

/**
 * A note reduced to its recognisable core: lowercased, digits and punctuation stripped, so
 * "UPI/Spotify/9921" and "Spotify 449" land in the same bucket. ASCII letters only survive —
 * every non-ASCII letter (é, देवनागरी, emoji) becomes a space, exactly as the web's `[^a-z\s]`.
 * `\s` is the JS whitespace set, not Java's ASCII one.
 */
fun normalizeNote(note: String): String {
    // \d+ → ' ', [^a-z\s] → ' ', \s+ → ' ', trim — fused into one pass: every character that
    // isn't a-z ends up as whitespace, and whitespace runs collapse to one space.
    val lower = note.lowercase()
    val sb = StringBuilder(lower.length)
    var pendingSpace = false
    for (c in lower) {
        val keep = c in 'a'..'z'
        if (keep) {
            if (pendingSpace && sb.isNotEmpty()) sb.append(' ')
            pendingSpace = false
            sb.append(c)
        } else {
            pendingSpace = true
        }
    }
    return sb.toString()
}

/** True when two amounts are "the same charge": within 5%, or a couple of rupees for small ones. */
fun amountsMatch(a: Double, b: Double): Boolean =
    abs(a - b) <= max(max(a, b) * AMOUNT_TOLERANCE, AMOUNT_TOLERANCE_FLOOR)

private fun advanceByFrequency(date: Instant, frequency: RecurrenceFrequency): Instant = when (frequency) {
    RecurrenceFrequency.Daily -> addDays(date, 1)
    RecurrenceFrequency.Weekly -> addWeeks(date, 1)
    RecurrenceFrequency.Monthly -> addMonths(date, 1)
    RecurrenceFrequency.Yearly -> addYears(date, 1)
}

private fun median(values: List<Double>): Double {
    val sorted = values.sorted()
    val mid = sorted.size / 2
    return if (sorted.size % 2 == 0) (sorted[mid - 1] + sorted[mid]) / 2 else sorted[mid]
}

private fun annualWeight(f: RecurrenceFrequency): Int = when (f) {
    RecurrenceFrequency.Daily -> 365
    RecurrenceFrequency.Weekly -> 52
    RecurrenceFrequency.Monthly -> 12
    RecurrenceFrequency.Yearly -> 1
}

/**
 * Expenses that repeat on a regular cadence for a near-identical amount and aren't already
 * covered by a recurring rule. Only plain, noted, non-split expenses qualify. As on the web,
 * *any* `splits` array — even an empty one — disqualifies a row, while an empty `recurringId`
 * does not.
 */
fun detectSubscriptions(
    transactions: List<Transaction>,
    recurring: List<RecurringTransaction>,
    now: Instant = nowInstant(),
): List<SubscriptionCandidate> {
    val from = addDays(now, -SUBSCRIPTION_LOOKBACK_DAYS)
    val covered = recurring.map { normalizeNote(it.note) }.filter { it != "" }.toSet()

    val groups = LinkedHashMap<String, MutableList<Transaction>>()
    for (t in transactions) {
        if (t.type != TransactionType.Expense || !t.recurringId.isNullOrEmpty() || t.splits != null) continue
        val key = normalizeNote(t.note)
        if (key == "" || key in covered) continue
        val date = parseIso(t.date) ?: continue
        if (date.isBefore(from) || date.isAfter(now)) continue
        groups.getOrPut(key) { mutableListOf() }.add(t)
    }

    val candidates = mutableListOf<SubscriptionCandidate>()

    for ((key, rows) in groups) {
        if (rows.size < MIN_SUBSCRIPTION_OCCURRENCES) continue

        // `localeCompare` on ISO strings; plain ordinal order agrees for same-format dates.
        val sorted = rows.sortedWith { a, b -> a.date.compareTo(b.date) }
        val amounts = sorted.map { it.amount }
        val typical = median(amounts)
        val slack = max(typical * AMOUNT_TOLERANCE, AMOUNT_TOLERANCE_FLOOR)
        if (amounts.any { abs(it - typical) > slack }) continue

        val dates = sorted.map { iso(it.date) }
        val gaps = (1 until dates.size).map { i -> (dates[i].toEpochMilli() - dates[i - 1].toEpochMilli()) / 86_400_000.0 }
        val cadence = CADENCE_WINDOWS.find { w -> gaps.all { it >= w.min && it <= w.max } } ?: continue

        val latest = sorted.last()
        var next = advanceByFrequency(iso(latest.date), cadence.frequency)
        // Never hand back a start date in the past: a rule created from it would backfill.
        while (!next.isAfter(now)) next = advanceByFrequency(next, cadence.frequency)

        candidates += SubscriptionCandidate(
            key = key,
            note = latest.note,
            amount = roundMoney(typical),
            frequency = cadence.frequency,
            occurrences = sorted.size,
            accountId = latest.accountId,
            categoryId = latest.categoryId,
            labels = latest.labels.toList(),
            lastDate = latest.date,
            nextDate = next.toIso(),
        )
    }

    // Biggest annualised spend first — that is the one worth automating.
    return candidates.sortedWith { a, b ->
        jsCompare(b.amount * annualWeight(b.frequency) - a.amount * annualWeight(a.frequency))
    }
}

/** A JS comparator's numeric result as a Kotlin one (NaN counts as equal). */
private fun jsCompare(diff: Double): Int = if (diff.isNaN() || diff == 0.0) 0 else if (diff > 0) 1 else -1

data class InsightInput(
    val transactions: List<Transaction>,
    val categories: List<Category>,
    val labels: List<Label>,
    val budgets: List<Budget>,
    val recurring: List<RecurringTransaction>,
    /** Optional — only needed for the negative-balance check. */
    val accounts: List<Account>? = null,
    val now: Instant? = null,
    val monthStartDay: Int? = null,
    /** Maximum insights returned. */
    val limit: Int? = null,
)

/** Months of completed history the "vs your average" comparisons are drawn from. */
private const val BASELINE_MONTHS = 3

/** A baseline needs at least this many of those months to have been in use. */
private const val MIN_BASELINE_MONTHS = 2

/** Below this, a percentage swing is arithmetic noise rather than a change in behaviour. */
private const val MIN_NOTABLE_AMOUNT = 500.0

/** How far a category must move against its baseline to be worth saying out loud. */
private const val NOTABLE_CHANGE = 0.25

/** Past this, "N% above" reads as a bug — say it as a multiplier instead ("3× your average"). */
private const val SPIKE_MULTIPLIER_CUTOFF = 2.0

private fun jsInt(x: Double): String = jsNumberString(x)

private fun formatSpikeTitle(categoryName: String, change: Double, months: Int): String {
    if (change > SPIKE_MULTIPLIER_CUTOFF) {
        val multiplier = jsRound((1 + change) * 10) / 10
        return "$categoryName is ${jsNumberString(multiplier)}× your $months-month average"
    }
    return "$categoryName is ${jsInt(jsRound(change * 100))}% above your $months-month average"
}

/** Pace-based insights need at least this many elapsed days, or the projection is noise. */
private const val MIN_PACE_DAYS = 7

private fun severityOrder(s: InsightSeverity): Int = when (s) {
    InsightSeverity.Warn -> 0
    InsightSeverity.Info -> 1
    InsightSeverity.Good -> 2
}

private fun categorySpend(rows: List<Transaction>): LinkedHashMap<String, Double> {
    val totals = LinkedHashMap<String, Double>()
    for (t in rows) {
        if (t.type != TransactionType.Expense) continue
        for ((categoryId, amount) in transactionCategoryAmounts(t)) {
            totals[categoryId] = (totals[categoryId] ?: 0.0) + amount
        }
    }
    return totals
}

/** Expenses scaled to the whole period by how much of it has elapsed. */
private fun paceToFullPeriod(amount: Double, range: PeriodRange, now: Instant): Double {
    val elapsed = daysElapsedInPeriod(range, now)
    if (elapsed <= 0) return amount
    return roundMoney((amount / elapsed) * daysInPeriod(range))
}

private data class Movement(val categoryId: String, val projected: Double, val baseline: Double, val change: Double)

/**
 * Everything worth telling the user about this month, most urgent first. [formatAmount] renders
 * money inside insight copy — pass one that honours the hide-amounts setting.
 */
fun buildInsights(input: InsightInput, formatAmount: (Double) -> String): List<Insight> {
    val now = input.now ?: nowInstant()
    val monthStartDay = input.monthStartDay ?: DEFAULT_MONTH_START_DAY
    val money = formatAmount
    val limit = input.limit ?: 6

    val range = periodRange(PeriodType.Monthly, now, monthStartDay)
    val periodKey = format(range.start, "yyyy-MM")
    val rows = transactionsInPeriod(input.transactions, range)
    val insights = mutableListOf<Insight>()
    val elapsed = daysElapsedInPeriod(range, now)
    val hasPace = elapsed >= MIN_PACE_DAYS

    fun categoryName(id: String) = input.categories.find { it.id == id }?.name ?: "Uncategorized"

    // ── Impossible balances ── (any non-credit type — fd/rd included — going negative)
    val negativeAccounts = activeAccounts(input.accounts ?: emptyList())
        .filter { it.type != AccountType.Credit && it.balance < 0 }
        .sortedWith { a, b -> jsCompare(a.balance - b.balance) }

    for (account in negativeAccounts.take(2)) {
        insights += Insight(
            id = "negative-balance:${account.id}",
            kind = InsightKind.NegativeBalance,
            severity = InsightSeverity.Warn,
            title = "${account.name} is negative",
            detail = "${money(abs(account.balance))} in the red — check for a missing transaction or a data-entry error.",
            action = InsightAction.Navigate("/edit-account/${account.id}", "Review account"),
        )
    }

    // ── Categories against their own recent average ──
    val priorMonths = (0 until BASELINE_MONTHS)
        .map { i -> transactionsInPeriod(input.transactions, shiftPeriod(range, -(i + 1))) }
        .filter { it.isNotEmpty() }

    if (hasPace && priorMonths.size >= MIN_BASELINE_MONTHS) {
        val baselines = LinkedHashMap<String, Double>()
        for (month in priorMonths) {
            for ((categoryId, amount) in categorySpend(month)) {
                baselines[categoryId] = (baselines[categoryId] ?: 0.0) + amount
            }
        }

        val current = categorySpend(rows)
        val movements = mutableListOf<Movement>()
        for ((categoryId, baselineTotal) in baselines) {
            val baseline = roundMoney(baselineTotal / priorMonths.size)
            if (baseline < MIN_NOTABLE_AMOUNT) continue
            // Pace-adjusted, or a three-day-old month always reads as a collapse in spending.
            val projected = paceToFullPeriod(current[categoryId] ?: 0.0, range, now)
            val change = (projected - baseline) / baseline
            if (abs(change) < NOTABLE_CHANGE) continue
            movements += Movement(categoryId, projected, baseline, change)
        }

        val sortedMoves = movements.sortedWith { a, b -> jsCompare(abs(b.change) - abs(a.change)) }

        for (move in sortedMoves.filter { it.change > 0 }.take(2)) {
            insights += Insight(
                id = "spike:${move.categoryId}:$periodKey",
                kind = InsightKind.CategorySpike,
                severity = InsightSeverity.Warn,
                title = formatSpikeTitle(categoryName(move.categoryId), move.change, priorMonths.size),
                detail = "On pace for ${money(move.projected)} this month, against ${money(move.baseline)} on average.",
            )
        }

        val biggestDrop = sortedMoves.firstOrNull { it.change < 0 }
        if (biggestDrop != null) {
            insights += Insight(
                id = "drop:${biggestDrop.categoryId}:$periodKey",
                kind = InsightKind.CategoryDrop,
                severity = InsightSeverity.Good,
                title = "${categoryName(biggestDrop.categoryId)} is down ${jsInt(jsRound(abs(biggestDrop.change) * 100))}% on your average",
                detail = "On pace for ${money(biggestDrop.projected)} this month, against ${money(biggestDrop.baseline)} on average.",
            )
        }
    }

    // ── Budgets ──
    fun budgetName(budget: Budget): String {
        val labelId = budget.labelId
        if (!labelId.isNullOrEmpty()) return input.labels.find { it.id == labelId }?.name ?: "Unknown label"
        return if (budget.categoryId == "") "Overall spending" else categoryName(budget.categoryId)
    }

    val statuses = computeBudgetStatuses(input.budgets, input.transactions, BudgetPeriodOptions(monthStartDay, now))

    val overBudget = statuses.filter { it.isOver }.sortedWith { a, b -> jsCompare(b.percent - a.percent) }
    for (status in overBudget.take(2)) {
        insights += Insight(
            id = "budget-over:${budgetScopeKey(status.budget)}:$periodKey",
            kind = InsightKind.BudgetOver,
            severity = InsightSeverity.Warn,
            title = "${budgetName(status.budget)} is over budget",
            detail = "${money(status.spent)} spent against a ${money(status.limit)} limit.",
            action = InsightAction.Navigate("/budgets", "Review budgets"),
        )
    }

    val onPace = (if (hasPace) statuses else emptyList())
        .filter { !it.isOver && paceToFullPeriod(it.spent, it.range, now) > it.limit && it.limit > 0 }
        .sortedWith { a, b -> jsCompare(b.percent - a.percent) }
    for (status in onPace.take(1)) {
        insights += Insight(
            id = "budget-pace:${budgetScopeKey(status.budget)}:$periodKey",
            kind = InsightKind.BudgetPace,
            severity = InsightSeverity.Warn,
            title = "${budgetName(status.budget)} is on pace to go over",
            detail = "At this rate you'll spend ${money(paceToFullPeriod(status.spent, status.range, now))} against a ${money(status.limit)} limit.",
            action = InsightAction.Navigate("/budgets", "Review budgets"),
        )
    }

    // ── Subscription detection ──
    for (candidate in detectSubscriptions(input.transactions, input.recurring, now).take(2)) {
        val cadence = when (candidate.frequency) {
            RecurrenceFrequency.Monthly -> "monthly"
            RecurrenceFrequency.Weekly -> "weekly"
            else -> "yearly"
        }
        insights += Insight(
            id = "subscription:${candidate.key}",
            kind = InsightKind.Subscription,
            severity = InsightSeverity.Info,
            title = "${candidate.occurrences} ${money(candidate.amount)} charges from \"${candidate.note}\"",
            detail = "Looks like a $cadence subscription. Track it as a recurring rule so it shows up in upcoming bills and the forecast.",
            action = InsightAction.CreateRecurring(candidate),
        )
    }

    // ── Savings rate ──
    val income = getTotalIncome(rows)
    val expenses = getTotalExpenses(rows)
    if (income > 0) {
        val savingsRate = (income - expenses) / income
        if (savingsRate < 0) {
            insights += Insight(
                id = "savings-negative:$periodKey",
                kind = InsightKind.SavingsRate,
                severity = InsightSeverity.Warn,
                title = "You've spent ${money(roundMoney(expenses - income))} more than you earned this month",
                detail = "${money(expenses)} out against ${money(income)} in.",
            )
        } else if (savingsRate >= 0.2) {
            insights += Insight(
                id = "savings-good:$periodKey",
                kind = InsightKind.SavingsRate,
                severity = InsightSeverity.Good,
                title = "You're saving ${jsInt(jsRound(savingsRate * 100))}% of your income this month",
                detail = "${money(roundMoney(income - expenses))} kept from ${money(income)} earned.",
            )
        }
    }

    // ── Concentration ──
    val expenseRowCount = rows.count { it.type == TransactionType.Expense }
    if (hasPace && expenseRowCount >= 3 && expenses >= MIN_NOTABLE_AMOUNT * 2) {
        val top = categorySpend(rows).entries.sortedWith { a, b -> jsCompare(b.value - a.value) }.firstOrNull()
        if (top != null && top.value / expenses >= 0.4) {
            insights += Insight(
                id = "share:${top.key}:$periodKey",
                kind = InsightKind.CategoryShare,
                severity = InsightSeverity.Info,
                title = "${categoryName(top.key)} is ${jsInt(jsRound((top.value / expenses) * 100))}% of this month's spending",
                detail = "${money(roundMoney(top.value))} of ${money(expenses)} total.",
            )
        }
    }

    val sorted = insights.sortedBy { severityOrder(it.severity) }
    // JS `slice(0, limit)`: a negative limit counts from the end.
    return if (limit >= 0) sorted.take(limit) else sorted.take(max(0, sorted.size + limit))
}
