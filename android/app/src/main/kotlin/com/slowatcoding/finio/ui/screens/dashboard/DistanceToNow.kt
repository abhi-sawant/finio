package com.slowatcoding.finio.ui.screens.dashboard

import java.time.Instant
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToLong

/**
 * date-fns `formatDistanceToNow(date, { addSuffix: true })` (en-US) — only the Dashboard's
 * "backed up 3 hours ago" uses it, so it lives here rather than in core.
 */
fun formatDistanceToNow(date: Instant, now: Instant = Instant.now()): String {
    val diffMs = now.toEpochMilli() - date.toEpochMilli()
    val past = diffMs >= 0
    val seconds = abs(diffMs) / 1000.0
    val minutes = (seconds / 60).roundToLong()

    val phrase = when {
        minutes < 2 -> if (seconds < 30) "less than a minute" else "1 minute"
        minutes < 45 -> "$minutes minutes"
        minutes < 90 -> "about 1 hour"
        minutes < 1440 -> "about ${(minutes / 60.0).roundToLong()} hours"
        minutes < 2520 -> "1 day"
        minutes < 43200 -> "${(minutes / 1440.0).roundToLong()} days"
        minutes < 86400 -> {
            val months = (minutes / 43200.0).roundToLong()
            "about $months month${if (months == 1L) "" else "s"}"
        }
        else -> {
            val months = monthsBetween(date, now)
            if (months < 12) {
                val n = maxOf(1, months)
                "$n month${if (n == 1) "" else "s"}"
            } else {
                val monthsSinceStartOfYear = months % 12
                val years = floor(months / 12.0).toInt()
                when {
                    monthsSinceStartOfYear < 3 -> "about $years year${if (years == 1) "" else "s"}"
                    monthsSinceStartOfYear < 9 -> "over $years year${if (years == 1) "" else "s"}"
                    else -> "almost ${years + 1} years"
                }
            }
        }
    }
    return if (past) "$phrase ago" else "in $phrase"
}

private fun monthsBetween(a: Instant, b: Instant): Int {
    val zone = java.time.ZoneId.systemDefault()
    val (from, to) = if (a.isBefore(b)) a to b else b to a
    return java.time.temporal.ChronoUnit.MONTHS.between(from.atZone(zone), to.atZone(zone)).toInt()
}
