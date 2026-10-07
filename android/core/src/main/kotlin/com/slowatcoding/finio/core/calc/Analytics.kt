package com.slowatcoding.finio.core.calc

import com.slowatcoding.finio.core.format.localDayKey
import com.slowatcoding.finio.core.js.addDays
import com.slowatcoding.finio.core.js.endOfWeek
import com.slowatcoding.finio.core.js.format
import com.slowatcoding.finio.core.js.isSameDay
import com.slowatcoding.finio.core.js.nowInstant
import com.slowatcoding.finio.core.js.startOfDay
import com.slowatcoding.finio.core.js.startOfWeek
import com.slowatcoding.finio.core.model.Account
import com.slowatcoding.finio.core.model.Transaction
import com.slowatcoding.finio.core.model.TransactionType
import com.slowatcoding.finio.core.money.roundMoney
import com.slowatcoding.finio.core.period.DEFAULT_MONTH_START_DAY
import com.slowatcoding.finio.core.period.PeriodRange
import com.slowatcoding.finio.core.period.PeriodType
import com.slowatcoding.finio.core.period.WEEK_STARTS_ON
import com.slowatcoding.finio.core.period.daysElapsedInPeriod
import com.slowatcoding.finio.core.period.daysInPeriod
import com.slowatcoding.finio.core.period.periodLabel
import com.slowatcoding.finio.core.period.periodRange
import com.slowatcoding.finio.core.period.shiftPeriod
import java.time.Instant
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

// Port of web/src/utils/analytics.ts — period-over-period comparison, the daily spending
// calendar and Year in Review. All pure: a transaction list and a window in, plain data out.

/** JS `(a, b) => x` comparator result for a numeric difference (NaN and ±0 compare equal). */
private fun diffOrder(diff: Double): Int = if (diff.isNaN() || diff == 0.0) 0 else if (diff > 0) 1 else -1

data class CategoryTotal(val categoryId: String, val amount: Double)

data class PeriodSummary(
    val range: PeriodRange,
    val label: String,
    val income: Double,
    val expenses: Double,
    val net: Double,
    val transactionCount: Int,
    /** Expense totals per category, biggest first. Split expenses count per split entry. */
    val categoryTotals: List<CategoryTotal>,
    /** True while the period is still running, so its totals are only partial. */
    val isPartial: Boolean,
    /** Expenses scaled up by how much of the period has elapsed; `expenses` once finished. */
    val projectedExpenses: Double,
)

/** Totals for one window. [now] decides whether the window counts as still running. */
fun summarizePeriod(
    transactions: List<Transaction>,
    range: PeriodRange,
    label: String? = null,
    now: Instant? = null,
    monthStartDay: Int? = null,
): PeriodSummary {
    val at = now ?: nowInstant()
    val msd = monthStartDay ?: DEFAULT_MONTH_START_DAY
    val rows = transactionsInPeriod(transactions, range)

    val income = roundMoney(getTotalIncome(rows))
    val expenses = roundMoney(getTotalExpenses(rows))

    val byCategory = LinkedHashMap<String, Double>()
    for (t in rows) {
        if (t.type != TransactionType.Expense) continue
        for (s in transactionCategoryAmounts(t)) {
            byCategory[s.categoryId] = (byCategory[s.categoryId] ?: 0.0) + s.amount
        }
    }

    val nowMs = at.toEpochMilli()
    val isPartial = nowMs < range.end.toEpochMilli() && nowMs >= range.start.toEpochMilli()
    val elapsed = daysElapsedInPeriod(range, at)
    val total = daysInPeriod(range)

    return PeriodSummary(
        range = range,
        label = label ?: periodLabel(range, msd),
        income = income,
        expenses = expenses,
        net = roundMoney(income - expenses),
        transactionCount = rows.size,
        categoryTotals = byCategory.map { (id, amount) -> CategoryTotal(id, roundMoney(amount)) }
            .sortedWith { a, b -> diffOrder(b.amount - a.amount) },
        isPartial = isPartial,
        projectedExpenses = if (isPartial && elapsed > 0) roundMoney((expenses / elapsed) * total) else expenses,
    )
}

data class PeriodComparison(
    val current: PeriodSummary,
    /** The period immediately before `current`. */
    val previous: PeriodSummary,
    /** The same period one year earlier. Null for a yearly comparison. */
    val lastYear: PeriodSummary?,
)

/** How many periods back "the same period last year" sits, per period type. */
private fun periodsPerYear(type: PeriodType): Int? = when (type) {
    PeriodType.Weekly -> 52
    PeriodType.Monthly -> 12
    PeriodType.Yearly -> null
}

/** This period vs the last one vs the same one a year ago. */
fun buildPeriodComparison(
    transactions: List<Transaction>,
    type: PeriodType? = null,
    now: Instant? = null,
    monthStartDay: Int? = null,
): PeriodComparison {
    val t = type ?: PeriodType.Monthly
    val at = now ?: nowInstant()
    val msd = monthStartDay ?: DEFAULT_MONTH_START_DAY
    val current = periodRange(t, at, msd)
    fun summarize(range: PeriodRange) = summarizePeriod(transactions, range, now = at, monthStartDay = msd)

    val yearOffset = periodsPerYear(t)
    return PeriodComparison(
        current = summarize(current),
        previous = summarize(shiftPeriod(current, -1)),
        lastYear = yearOffset?.let { summarize(shiftPeriod(current, -it)) },
    )
}

data class CategoryMovement(
    val categoryId: String,
    val current: Double,
    val previous: Double,
    /** Signed: positive means more was spent this period. */
    val change: Double,
    /** Null when there is nothing to divide by — the category is new this period. */
    val percentChange: Double?,
)

/**
 * The categories that moved most between two periods, biggest absolute swing first. Categories
 * present in only one of the two periods count as a move from (or to) zero.
 */
fun categoryMovements(current: PeriodSummary, previous: PeriodSummary, limit: Int = 5): List<CategoryMovement> {
    val previousById = LinkedHashMap<String, Double>()
    for (c in previous.categoryTotals) previousById[c.categoryId] = c.amount
    val ids = LinkedHashSet<String>()
    current.categoryTotals.forEach { ids += it.categoryId }
    ids += previousById.keys

    return ids.map { categoryId ->
        val currentAmount = current.categoryTotals.firstOrNull { it.categoryId == categoryId }?.amount ?: 0.0
        val previousAmount = previousById[categoryId] ?: 0.0
        CategoryMovement(
            categoryId = categoryId,
            current = currentAmount,
            previous = previousAmount,
            change = roundMoney(currentAmount - previousAmount),
            percentChange = if (previousAmount > 0) (currentAmount - previousAmount) / previousAmount else null,
        )
    }
        .filter { it.change != 0.0 }
        .sortedWith { a, b -> diffOrder(abs(b.change) - abs(a.change)) }
        .let { it.take(if (limit >= 0) limit else max(0, it.size + limit)) } // JS slice(0, limit)
}

data class CalendarDay(
    val date: Instant,
    /** `yyyy-MM-dd`, the same key the rest of the app slices dates on. */
    val key: String,
    /** Total expense recorded on this day. */
    val total: Double,
    val transactionCount: Int,
    /** 0–1 shading weight relative to the heaviest day in the grid (square-rooted). */
    val intensity: Double,
    /** False for the leading/trailing days that only exist to square off the week rows. */
    val inRange: Boolean,
    val isFuture: Boolean,
    val isToday: Boolean,
)

data class SpendingCalendar(
    val range: PeriodRange,
    val label: String,
    /** Week rows, each exactly 7 days, Monday first — matching `WEEK_STARTS_ON`. */
    val weeks: List<List<CalendarDay>>,
    val total: Double,
    /** The heaviest single day, which is what `intensity` is scaled against. */
    val max: Double,
    val daysWithSpend: Int,
    /** Mean spend across days that had any. */
    val averagePerActiveDay: Double,
    val busiest: CalendarDay?,
)

private class DayEntry(var total: Double, var count: Int)

/**
 * A month grid of daily expense totals. Days outside the period are padded in (and flagged
 * `inRange: false`) so every row is a full week. Note: the label uses the default month start
 * day, exactly as the web does.
 */
fun buildSpendingCalendar(transactions: List<Transaction>, range: PeriodRange, now: Instant = nowInstant()): SpendingCalendar {
    val byDay = LinkedHashMap<String, DayEntry>()
    for (t in transactionsInPeriod(transactions, range)) {
        if (t.type != TransactionType.Expense) continue
        val key = localDayKey(t.date)
        val entry = byDay.getOrPut(key) { DayEntry(0.0, 0) }
        entry.total = roundMoney(entry.total + t.amount)
        entry.count += 1
    }

    val maxTotal = byDay.values.fold(0.0) { m, e -> max(m, e.total) }
    val today = startOfDay(now)
    val gridStart = startOfWeek(range.start, WEEK_STARTS_ON)
    val gridEnd = endOfWeek(range.end, WEEK_STARTS_ON)

    val weeks = mutableListOf<List<CalendarDay>>()
    var week = mutableListOf<CalendarDay>()

    var day = gridStart
    while (!day.isAfter(gridEnd)) {
        val key = format(day, "yyyy-MM-dd")
        val inRange = !day.isBefore(range.start) && !day.isAfter(range.end)
        val entry = if (inRange) byDay[key] else null
        val total = entry?.total ?: 0.0

        week += CalendarDay(
            date = day,
            key = key,
            total = total,
            transactionCount = entry?.count ?: 0,
            intensity = if (maxTotal > 0 && total > 0) 0.2 + 0.8 * sqrt(total / maxTotal) else 0.0,
            inRange = inRange,
            isFuture = day.isAfter(today),
            isToday = isSameDay(day, today),
        )
        if (week.size == 7) {
            weeks += week
            week = mutableListOf()
        }
        day = addDays(day, 1)
    }
    if (week.isNotEmpty()) weeks += week

    val active = byDay.values.filter { it.total > 0 }
    val total = active.fold(0.0) { sum, e -> roundMoney(sum + e.total) }
    val busiest = weeks.flatten()
        .filter { it.inRange && it.total > 0 }
        .sortedWith { a, b -> diffOrder(b.total - a.total) }
        .firstOrNull()

    return SpendingCalendar(
        range = range,
        label = periodLabel(range),
        weeks = weeks,
        total = total,
        max = maxTotal,
        daysWithSpend = active.size,
        averagePerActiveDay = if (active.isNotEmpty()) roundMoney(total / active.size) else 0.0,
        busiest = busiest,
    )
}

data class MonthTotal(
    /** `yyyy-MM` of the financial month's start. */
    val key: String,
    val label: String,
    val income: Double,
    val expenses: Double,
)

data class YearInReview(
    val range: PeriodRange,
    val label: String,
    val current: PeriodSummary,
    val previous: PeriodSummary,
    /** This year's biggest expense categories, most first. */
    val topCategories: List<CategoryTotal>,
    /** Categories that moved most against last year. */
    val movers: List<CategoryMovement>,
    /** One entry per financial month in the year, in order. */
    val monthlyBreakdown: List<MonthTotal>,
    /** The month with the most spending, or null for a year with none at all. */
    val busiestMonth: MonthTotal?,
    val netWorthStart: Double,
    val netWorthEnd: Double,
    val netWorthChange: Double,
    /** The single biggest expense of the year, or null if there wasn't one. */
    val biggestExpense: Transaction?,
)

data class YearInReviewInput(
    val transactions: List<Transaction>,
    val accounts: List<Account>,
    val now: Instant? = null,
    val monthStartDay: Int? = null,
    /** 0 = the financial year in progress, negative = that many years back. */
    val yearOffset: Int? = null,
)

/** A one-screen annual summary, built entirely from period math and net-worth reconstruction. */
fun buildYearInReview(input: YearInReviewInput): YearInReview {
    val now = input.now ?: nowInstant()
    val monthStartDay = input.monthStartDay ?: DEFAULT_MONTH_START_DAY
    val yearOffset = input.yearOffset ?: 0

    val thisYear = periodRange(PeriodType.Yearly, now, monthStartDay)
    val range = if (yearOffset == 0) thisYear else shiftPeriod(thisYear, yearOffset)
    val previousRange = shiftPeriod(range, -1)

    val current = summarizePeriod(input.transactions, range, now = now, monthStartDay = monthStartDay)
    val previous = summarizePeriod(input.transactions, previousRange, now = now, monthStartDay = monthStartDay)
    val movers = categoryMovements(current, previous, 5)

    val firstMonth = periodRange(PeriodType.Monthly, range.start, monthStartDay)
    val monthlyBreakdown = (0 until 12).map { i ->
        val monthRange = shiftPeriod(firstMonth, i)
        val summary = summarizePeriod(input.transactions, monthRange, now = now, monthStartDay = monthStartDay)
        MonthTotal(
            key = format(monthRange.start, "yyyy-MM"),
            label = format(monthRange.start, "MMM"),
            income = summary.income,
            expenses = summary.expenses,
        )
    }

    // Zero everywhere shouldn't crown January "busiest" — that's just an empty year.
    val busiestMonth = monthlyBreakdown.fold<MonthTotal, MonthTotal?>(null) { best, month ->
        if (month.expenses > 0 && (best == null || month.expenses > best.expenses)) month else best
    }

    // Net worth "at the start of the year" is the instant before its first day; a year still in
    // progress reads its end value as of now.
    val asOfEnd = if (range.end.toEpochMilli() < now.toEpochMilli()) range.end else now
    val netWorthStart = netWorthAt(input.accounts, input.transactions, range.start.minusMillis(1)).netWorth
    val netWorthEnd = netWorthAt(input.accounts, input.transactions, asOfEnd).netWorth

    val biggestExpense = transactionsInPeriod(input.transactions, range)
        .filter { it.type == TransactionType.Expense }
        .sortedWith { a, b -> diffOrder(b.amount - a.amount) }
        .firstOrNull()

    return YearInReview(
        range = range,
        label = periodLabel(range, monthStartDay),
        current = current,
        previous = previous,
        topCategories = current.categoryTotals.take(5),
        movers = movers,
        monthlyBreakdown = monthlyBreakdown,
        busiestMonth = busiestMonth,
        netWorthStart = netWorthStart,
        netWorthEnd = netWorthEnd,
        netWorthChange = roundMoney(netWorthEnd - netWorthStart),
        biggestExpense = biggestExpense,
    )
}
