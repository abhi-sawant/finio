package com.slowatcoding.finio.core.period

import com.slowatcoding.finio.core.js.addMonths
import com.slowatcoding.finio.core.js.addWeeks
import com.slowatcoding.finio.core.js.addYears
import com.slowatcoding.finio.core.js.dayOfMonth
import com.slowatcoding.finio.core.js.format
import com.slowatcoding.finio.core.js.fullYear
import com.slowatcoding.finio.core.js.jsTrunc
import com.slowatcoding.finio.core.js.jsRound
import com.slowatcoding.finio.core.js.localDate
import com.slowatcoding.finio.core.js.month0
import com.slowatcoding.finio.core.js.startOfDay
import com.slowatcoding.finio.core.js.startOfWeek
import com.slowatcoding.finio.core.model.BudgetPeriod
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import java.time.Instant
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

// Port of web/src/utils/period.ts. A "month" is a *financial* month: it starts on
// `monthStartDay` rather than on the 1st. With the default start day of 1 every range below is
// the plain calendar period.

@Serializable
enum class PeriodType {
    @SerialName("weekly") Weekly,
    @SerialName("monthly") Monthly,
    @SerialName("yearly") Yearly,
}

fun BudgetPeriod.toPeriodType(): PeriodType = when (this) {
    BudgetPeriod.Weekly -> PeriodType.Weekly
    BudgetPeriod.Monthly -> PeriodType.Monthly
    BudgetPeriod.Yearly -> PeriodType.Yearly
}

/** Weeks run Monday–Sunday. */
const val WEEK_STARTS_ON = 1

const val MIN_MONTH_START_DAY = 1
/** 29–31 don't exist in every month, so a cycle can never be anchored past the 28th. */
const val MAX_MONTH_START_DAY = 28
const val DEFAULT_MONTH_START_DAY = 1

val PERIOD_LABELS: Map<PeriodType, String> = linkedMapOf(
    PeriodType.Weekly to "Weekly",
    PeriodType.Monthly to "Monthly",
    PeriodType.Yearly to "Yearly",
)

val PERIOD_TYPES: List<PeriodType> = listOf(PeriodType.Weekly, PeriodType.Monthly, PeriodType.Yearly)

data class PeriodRange(
    val type: PeriodType,
    /** Inclusive start, at local midnight. */
    val start: Instant,
    /** Inclusive end — the last millisecond before the next period begins. */
    val end: Instant,
)

/**
 * Coerce anything persisted or imported into a usable start day. Like the TS `unknown`
 * parameter, only a finite *number* counts — a numeric string such as "25" falls back to 1.
 * Accepts Kotlin numbers and numeric (non-string) JSON primitives.
 */
fun normalizeMonthStartDay(value: Any?): Int {
    val n: Double = when (value) {
        is Number -> value.toDouble()
        is JsonPrimitive -> if (value.isString) return DEFAULT_MONTH_START_DAY else value.doubleOrNull ?: return DEFAULT_MONTH_START_DAY
        else -> return DEFAULT_MONTH_START_DAY
    }
    if (!n.isFinite()) return DEFAULT_MONTH_START_DAY
    val day = jsTrunc(n)
    if (day < MIN_MONTH_START_DAY) return MIN_MONTH_START_DAY
    if (day > MAX_MONTH_START_DAY) return MAX_MONTH_START_DAY
    return day.toInt()
}

/** Start of the financial month containing [date]. */
fun monthPeriodStart(date: Instant, monthStartDay: Int = DEFAULT_MONTH_START_DAY): Instant {
    val day = normalizeMonthStartDay(monthStartDay)
    val anchor = startOfDay(localDate(date.fullYear, date.month0, day))
    return if (date.dayOfMonth >= day) anchor else addMonths(anchor, -1)
}

/** Start of the financial year containing [date] — anchored to the same day-of-month in January. */
fun yearPeriodStart(date: Instant, monthStartDay: Int = DEFAULT_MONTH_START_DAY): Instant {
    val day = normalizeMonthStartDay(monthStartDay)
    val anchor = startOfDay(localDate(date.fullYear, 0, day))
    return if (date.toEpochMilli() >= anchor.toEpochMilli()) anchor else addYears(anchor, -1)
}

fun periodStart(type: PeriodType, date: Instant, monthStartDay: Int = DEFAULT_MONTH_START_DAY): Instant =
    when (type) {
        PeriodType.Weekly -> startOfWeek(date, WEEK_STARTS_ON)
        PeriodType.Monthly -> monthPeriodStart(date, monthStartDay)
        PeriodType.Yearly -> yearPeriodStart(date, monthStartDay)
    }

/** Advance a canonical period start by whole periods. Safe for any delta, including negative. */
fun addPeriods(type: PeriodType, start: Instant, delta: Int): Instant = when (type) {
    PeriodType.Weekly -> addWeeks(start, delta)
    PeriodType.Monthly -> addMonths(start, delta)
    PeriodType.Yearly -> addYears(start, delta)
}

private fun rangeFromStart(type: PeriodType, start: Instant): PeriodRange =
    PeriodRange(type, start, Instant.ofEpochMilli(addPeriods(type, start, 1).toEpochMilli() - 1))

/** The period of [type] that contains [date]. */
fun periodRange(type: PeriodType, date: Instant, monthStartDay: Int = DEFAULT_MONTH_START_DAY): PeriodRange =
    rangeFromStart(type, periodStart(type, date, monthStartDay))

/** The period [delta] whole periods away from [range] (negative = earlier). */
fun shiftPeriod(range: PeriodRange, delta: Int): PeriodRange =
    rangeFromStart(range.type, addPeriods(range.type, range.start, delta))

fun isWithinPeriod(date: Instant, range: PeriodRange): Boolean {
    val time = date.toEpochMilli()
    return time >= range.start.toEpochMilli() && time <= range.end.toEpochMilli()
}

/** Whole days the period spans. */
fun daysInPeriod(range: PeriodRange): Int =
    max(1.0, jsRound((range.end.toEpochMilli() + 1 - range.start.toEpochMilli()) / 86_400_000.0)).toInt()

/** How many of the period's days have started as of [now]. */
fun daysElapsedInPeriod(range: PeriodRange, now: Instant): Int {
    if (now.toEpochMilli() < range.start.toEpochMilli()) return 0
    val elapsed = floor((now.toEpochMilli() - range.start.toEpochMilli()) / 86_400_000.0).toInt() + 1
    return min(elapsed, daysInPeriod(range))
}

/** Human label for a range: "July 2026", "25 Jun – 24 Jul 2026", "20 – 26 Jul". */
fun periodLabel(range: PeriodRange, monthStartDay: Int = DEFAULT_MONTH_START_DAY): String {
    val day = normalizeMonthStartDay(monthStartDay)
    return when (range.type) {
        PeriodType.Weekly -> "${format(range.start, "d MMM")} – ${format(range.end, "d MMM")}"
        PeriodType.Monthly -> if (day == MIN_MONTH_START_DAY) format(range.start, "MMMM yyyy")
        else "${format(range.start, "d MMM")} – ${format(range.end, "d MMM yyyy")}"
        PeriodType.Yearly -> if (day == MIN_MONTH_START_DAY) format(range.start, "yyyy")
        else "${format(range.start, "d MMM yyyy")} – ${format(range.end, "d MMM yyyy")}"
    }
}

/** Compact label for history rows, where the range is already implied by its neighbours. */
fun periodShortLabel(range: PeriodRange): String = when (range.type) {
    PeriodType.Weekly -> format(range.start, "d MMM")
    PeriodType.Monthly -> format(range.start, "MMM")
    PeriodType.Yearly -> format(range.start, "yyyy")
}
