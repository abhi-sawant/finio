package com.slowatcoding.finio.core.calc

import com.slowatcoding.finio.core.js.addDays
import com.slowatcoding.finio.core.js.differenceInCalendarDays
import com.slowatcoding.finio.core.js.endOfWeek
import com.slowatcoding.finio.core.js.format
import com.slowatcoding.finio.core.js.isSameDay
import com.slowatcoding.finio.core.js.nowInstant
import com.slowatcoding.finio.core.js.parseIso
import com.slowatcoding.finio.core.js.startOfDay
import com.slowatcoding.finio.core.js.startOfWeek
import com.slowatcoding.finio.core.model.Account
import com.slowatcoding.finio.core.model.RecurringTransaction
import com.slowatcoding.finio.core.model.Transaction
import com.slowatcoding.finio.core.model.TransactionType
import com.slowatcoding.finio.core.money.roundMoney
import com.slowatcoding.finio.core.period.PeriodRange
import com.slowatcoding.finio.core.period.WEEK_STARTS_ON
import com.slowatcoding.finio.core.store.futureOccurrences
import java.time.Instant
import kotlin.math.max
import kotlin.math.min

// Port of web/src/utils/forecast.ts — cash-flow forecast.
//
// Projects *liquid* cash — open, non-credit, non-deposit accounts — forward from recurring rules
// (known amounts on known dates) and the recent per-category spending average (everything else).
// Card spending only appears when the card payment transfer does, so nothing is double-counted.

/** How far ahead the projection runs by default. */
const val DEFAULT_FORECAST_DAYS = 90

/** How much history the everyday-spend average is drawn from. */
const val DEFAULT_LOOKBACK_DAYS = 90

private fun forecastOrder(diff: Double): Int = if (diff.isNaN() || diff == 0.0) 0 else if (diff > 0) 1 else -1

/** Open accounts that hold spendable cash (insertion-ordered, like the JS Set). */
fun liquidAccountIds(accounts: List<Account>): Set<String> =
    activeAccounts(accounts).filter { isLiquidAccount(it) }.mapTo(LinkedHashSet()) { it.id }

/** Sum of the liquid accounts' current balances — where the projection starts from. */
fun liquidBalance(accounts: List<Account>): Double =
    roundMoney(activeAccounts(accounts).filter { isLiquidAccount(it) }.fold(0.0) { sum, a -> sum + a.balance })

/**
 * Signed effect on total liquid cash. A transfer between two liquid accounts nets to zero; one
 * that crosses the boundary (paying a card off, funding a deposit) doesn't.
 */
fun liquidDelta(
    type: TransactionType,
    amount: Double,
    accountId: String,
    toAccountId: String?,
    liquid: Set<String>,
): Double {
    val fromLiquid = accountId in liquid
    if (type == TransactionType.Expense) return if (fromLiquid) -amount else 0.0
    if (type == TransactionType.Income) return if (fromLiquid) amount else 0.0

    val toLiquid = if (!toAccountId.isNullOrEmpty()) toAccountId in liquid else false
    if (fromLiquid == toLiquid) return 0.0
    return if (fromLiquid) -amount else amount
}

fun liquidDelta(rule: RecurringTransaction, liquid: Set<String>): Double =
    liquidDelta(rule.type, rule.amount, rule.accountId, rule.toAccountId, liquid)

fun liquidDelta(t: Transaction, liquid: Set<String>): Double =
    liquidDelta(t.type, t.amount, t.accountId, t.toAccountId, liquid)

data class ScheduledFlow(
    val ruleId: String,
    val note: String,
    val categoryId: String,
    val date: Instant,
    val type: TransactionType,
    val amount: Double,
    /** Signed effect on liquid cash — negative for money leaving. */
    val delta: Double,
)

data class CategoryAverage(
    val categoryId: String,
    val dailyAverage: Double,
    val monthlyAverage: Double,
    /** This category's share of the estimated everyday spend, 0–1. */
    val share: Double,
)

data class ForecastPoint(
    val date: Instant,
    /** `yyyy-MM-dd` — stable key for chart rows. */
    val key: String,
    /** Projected liquid balance at the end of this day. */
    val balance: Double,
    val scheduledIn: Double,
    val scheduledOut: Double,
    /** Everyday spend estimated from category averages. */
    val estimatedOut: Double,
)

data class ForecastTotals(val scheduledIn: Double, val scheduledOut: Double, val estimatedOut: Double)

data class ForecastLow(val date: Instant, val balance: Double)

data class CashFlowForecast(
    val startBalance: Double,
    val endBalance: Double,
    /** One point per day, starting with today at the current balance. */
    val points: List<ForecastPoint>,
    /** Every projected recurring occurrence in the window, chronologically. */
    val scheduled: List<ScheduledFlow>,
    /** Per-category everyday spend, biggest first. */
    val categoryAverages: List<CategoryAverage>,
    /** Estimated everyday (non-recurring) spend per day. */
    val dailyEstimate: Double,
    /** How many days of history the average was drawn from. */
    val lookbackDays: Int,
    val totals: ForecastTotals,
    /** The lowest point of the projection. */
    val low: ForecastLow?,
    /** First day the projection goes negative, if it ever does. */
    val shortfallDate: Instant?,
    /** True when there is nothing to project from — no liquid accounts, no rules, no history. */
    val isEmpty: Boolean,
)

data class ForecastInput(
    val accounts: List<Account>,
    val transactions: List<Transaction>,
    val recurring: List<RecurringTransaction>,
    val now: Instant? = null,
    val days: Int? = null,
    val lookbackDays: Int? = null,
)

data class CategoryDailyAverages(
    val averages: List<CategoryAverage>,
    val dailyEstimate: Double,
    val lookbackDays: Int,
)

/**
 * Average everyday spend per category, per day. Recurring-generated rows are left out (they are
 * projected from the rules themselves), and so are untagged expenses that look like an active
 * expense rule's charge (same account, category, note and amount).
 */
fun categoryDailyAverages(
    transactions: List<Transaction>,
    accounts: List<Account>,
    now: Instant? = null,
    lookbackDays: Int? = null,
    recurring: List<RecurringTransaction>? = null,
): CategoryDailyAverages {
    val at = now ?: nowInstant()
    val window = max(1, lookbackDays ?: DEFAULT_LOOKBACK_DAYS)
    val liquid = liquidAccountIds(accounts)
    val from = startOfDay(addDays(at, -(window - 1)))

    val rules = (recurring ?: emptyList())
        .filter { it.pausedAt.isNullOrEmpty() && it.type == TransactionType.Expense }
        .map { it to normalizeNote(it.note) }
    fun matchesRule(t: Transaction): Boolean {
        val key = normalizeNote(t.note)
        return rules.any { (rule, ruleKey) ->
            rule.accountId == t.accountId &&
                rule.categoryId == t.categoryId &&
                ruleKey == key &&
                amountsMatch(rule.amount, t.amount)
        }
    }

    val byCategory = LinkedHashMap<String, Double>()
    var earliest: Instant? = null

    for (t in transactions) {
        if (t.type != TransactionType.Expense || !t.recurringId.isNullOrEmpty()) continue
        if (t.accountId !in liquid) continue
        if (matchesRule(t)) continue
        val date = parseIso(t.date) ?: continue
        if (date.isBefore(from) || date.isAfter(at)) continue

        if (earliest == null || date.isBefore(earliest)) earliest = date
        for (s in transactionCategoryAmounts(t)) {
            byCategory[s.categoryId] = (byCategory[s.categoryId] ?: 0.0) + s.amount
        }
    }

    // Divide by the history that actually exists, not the whole window.
    val observedDays = earliest?.let { differenceInCalendarDays(at, it) + 1 } ?: 0
    val days = max(1, min(window, observedDays))

    val total = byCategory.values.fold(0.0) { sum, amount -> sum + amount }
    val dailyEstimate = roundMoney(total / days)

    val averages = byCategory.map { (categoryId, amount) ->
        CategoryAverage(
            categoryId = categoryId,
            dailyAverage = roundMoney(amount / days),
            monthlyAverage = roundMoney((amount / days) * 30),
            share = if (total > 0) amount / total else 0.0,
        )
    }
        .filter { it.dailyAverage > 0 }
        .sortedWith { a, b -> forecastOrder(b.dailyAverage - a.dailyAverage) }

    return CategoryDailyAverages(averages, dailyEstimate, days)
}

/** Project liquid cash forward day by day. */
fun buildCashFlowForecast(input: ForecastInput): CashFlowForecast {
    val now = input.now ?: nowInstant()
    val days = max(1, input.days ?: DEFAULT_FORECAST_DAYS)
    val today = startOfDay(now)
    val horizon = addDays(today, days)

    val liquid = liquidAccountIds(input.accounts)
    val startBalance = liquidBalance(input.accounts)
    val avg = categoryDailyAverages(
        input.transactions,
        input.accounts,
        now = now,
        lookbackDays = input.lookbackDays,
        recurring = input.recurring,
    )
    val dailyEstimate = avg.dailyEstimate

    val scheduled = input.recurring
        .flatMap { rule ->
            futureOccurrences(rule, now, horizon).map { date ->
                ScheduledFlow(
                    ruleId = rule.id,
                    note = rule.note,
                    categoryId = rule.categoryId,
                    date = date,
                    type = rule.type,
                    amount = rule.amount,
                    delta = roundMoney(liquidDelta(rule, liquid)),
                )
            }
        }
        // A rule with no effect on liquid cash would just be noise.
        .filter { it.delta != 0.0 }
        .sortedWith { a, b -> a.date.toEpochMilli().compareTo(b.date.toEpochMilli()) }

    val scheduledByDay = LinkedHashMap<String, MutableList<ScheduledFlow>>()
    for (flow in scheduled) scheduledByDay.getOrPut(format(flow.date, "yyyy-MM-dd")) { mutableListOf() } += flow

    val points = mutableListOf(
        ForecastPoint(today, format(today, "yyyy-MM-dd"), startBalance, 0.0, 0.0, 0.0),
    )

    var balance = startBalance
    var totalIn = 0.0
    var totalOut = 0.0
    var totalEstimated = 0.0
    var low: ForecastLow? = null
    var shortfallDate: Instant? = null

    for (offset in 1..days) {
        val date = addDays(today, offset)
        val key = format(date, "yyyy-MM-dd")

        var scheduledIn = 0.0
        var scheduledOut = 0.0
        for (flow in scheduledByDay[key].orEmpty()) {
            if (flow.delta > 0) scheduledIn = roundMoney(scheduledIn + flow.delta)
            else scheduledOut = roundMoney(scheduledOut - flow.delta)
        }

        balance = roundMoney(balance + scheduledIn - scheduledOut - dailyEstimate)
        totalIn = roundMoney(totalIn + scheduledIn)
        totalOut = roundMoney(totalOut + scheduledOut)
        totalEstimated = roundMoney(totalEstimated + dailyEstimate)

        points += ForecastPoint(date, key, balance, scheduledIn, scheduledOut, dailyEstimate)

        if (low == null || balance < low.balance) low = ForecastLow(date, balance)
        if (shortfallDate == null && balance < 0) shortfallDate = date
    }

    return CashFlowForecast(
        startBalance = startBalance,
        endBalance = balance,
        points = points,
        scheduled = scheduled,
        categoryAverages = avg.averages,
        dailyEstimate = dailyEstimate,
        lookbackDays = avg.lookbackDays,
        totals = ForecastTotals(totalIn, totalOut, totalEstimated),
        low = low,
        shortfallDate = shortfallDate,
        isEmpty = liquid.isEmpty() || (scheduled.isEmpty() && dailyEstimate == 0.0),
    )
}

data class CashFlowCalendarDay(
    val date: Instant,
    /** `yyyy-MM-dd` — stable key for grid cells. */
    val key: String,
    /** Net scheduled effect on liquid cash this day — positive is money in. */
    val netFlow: Double,
    /** The individual scheduled occurrences landing on this day, chronological. */
    val flows: List<ScheduledFlow>,
    /** False for the leading/trailing days that only exist to square off the week rows. */
    val inRange: Boolean,
    val isToday: Boolean,
)

data class CashFlowCalendarMonth(
    /** Week rows, each exactly 7 days, Monday first. */
    val weeks: List<List<CashFlowCalendarDay>>,
)

/**
 * A month grid of `scheduled` cash flow — the forward-looking counterpart to
 * `buildSpendingCalendar`. Days outside [monthRange] are padded in and flagged `inRange: false`.
 */
fun buildCashFlowCalendarMonth(
    scheduled: List<ScheduledFlow>,
    monthRange: PeriodRange,
    now: Instant = nowInstant(),
): CashFlowCalendarMonth {
    val byDay = LinkedHashMap<String, MutableList<ScheduledFlow>>()
    for (flow in scheduled) byDay.getOrPut(format(flow.date, "yyyy-MM-dd")) { mutableListOf() } += flow

    val today = startOfDay(now)
    val gridStart = startOfWeek(monthRange.start, WEEK_STARTS_ON)
    val gridEnd = endOfWeek(monthRange.end, WEEK_STARTS_ON)

    val weeks = mutableListOf<List<CashFlowCalendarDay>>()
    var week = mutableListOf<CashFlowCalendarDay>()

    var day = gridStart
    while (!day.isAfter(gridEnd)) {
        val key = format(day, "yyyy-MM-dd")
        val inRange = !day.isBefore(monthRange.start) && !day.isAfter(monthRange.end)
        val flows: List<ScheduledFlow> = if (inRange) byDay[key].orEmpty() else emptyList()

        week += CashFlowCalendarDay(
            date = day,
            key = key,
            netFlow = roundMoney(flows.fold(0.0) { sum, f -> sum + f.delta }),
            flows = flows,
            inRange = inRange,
            isToday = isSameDay(day, today),
        )
        if (week.size == 7) {
            weeks += week
            week = mutableListOf()
        }
        day = addDays(day, 1)
    }
    if (week.isNotEmpty()) weeks += week

    return CashFlowCalendarMonth(weeks)
}
