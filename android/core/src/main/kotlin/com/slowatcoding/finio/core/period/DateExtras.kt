package com.slowatcoding.finio.core.period

import com.slowatcoding.finio.core.js.differenceInCalendarMonths
import com.slowatcoding.finio.core.js.endOfDay
import com.slowatcoding.finio.core.js.endOfMonth
import com.slowatcoding.finio.core.js.local
import com.slowatcoding.finio.core.js.localDate
import java.time.Instant
import kotlin.math.abs

// date-fns helpers missing from core/js/JsDate.kt, ported from date-fns 4.3.0 source.

/** date-fns `compareAsc`: -1, 0 or 1. */
fun compareAsc(a: Instant, b: Instant): Int = a.toEpochMilli().compareTo(b.toEpochMilli()).coerceIn(-1, 1)

/** date-fns `isLastDayOfMonth`. */
fun isLastDayOfMonth(d: Instant): Boolean = endOfDay(d) == endOfMonth(d)

/** JS `date.setDate(day)` on a copy — local time, with overflow normalisation. */
private fun Instant.withJsDate(day: Int): Instant {
    val l = local()
    return localDate(l.year, l.monthValue - 1, day, l.hour, l.minute, l.second, l.nano / 1_000_000)
}

/** JS `date.setMonth(month0)` on a copy — keeps the day, so Mar 31 → "Feb 31" → Mar 3. */
private fun Instant.withJsMonth(month0: Int): Instant {
    val l = local()
    return localDate(l.year, month0, l.dayOfMonth, l.hour, l.minute, l.second, l.nano / 1_000_000)
}

/**
 * date-fns `differenceInMonths(later, earlier)` — full months between the dates, including its
 * JS `setMonth` overflow quirks (the Feb > 27 → setDate(30) trick and the last-day-of-month rule).
 */
fun differenceInMonths(laterDate: Instant, earlierDate: Instant): Int {
    var working = laterDate
    val sign = compareAsc(working, earlierDate)
    val difference = abs(differenceInCalendarMonths(working, earlierDate))
    if (difference < 1) return 0

    if (working.local().monthValue - 1 == 1 && working.local().dayOfMonth > 27) working = working.withJsDate(30)
    working = working.withJsMonth(working.local().monthValue - 1 - sign * difference)

    var isLastMonthNotFull = compareAsc(working, earlierDate) == -sign
    if (isLastDayOfMonth(laterDate) && difference == 1 && compareAsc(laterDate, earlierDate) == 1) {
        isLastMonthNotFull = false
    }
    return sign * (difference - if (isLastMonthNotFull) 1 else 0)
}
