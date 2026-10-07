package com.slowatcoding.finio.core.js

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale

/*
 * JS `Date` + date-fns semantics on top of java.time, so ported code can be read line-for-line
 * against web/src. A JS Date is an instant; every "local" operation (startOfDay, getDate,
 * addMonths…) happens in the device's zone — exactly like the browser. Tests pin the zone to
 * Asia/Kolkata (see core/build.gradle.kts), the same as web/vitest.config.ts.
 *
 * Two different string parsers exist on the web and they disagree on date-only strings:
 *  - `new Date("2026-10-05")` → UTC midnight          → [parseJsDate]
 *  - date-fns `parseISO("2026-10-05")` → local midnight → [parseIso]
 * Port each call site with the one it actually uses.
 */

/** The zone every local-time operation uses. Overridable for tests. */
@Volatile
var finioZone: ZoneId = ZoneId.systemDefault()

private val ISO_OUT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ROOT).withZone(ZoneOffset.UTC)

private val DATE_ONLY = Regex("""^(\d{4})-(\d{2})-(\d{2})$""")
private val LOCAL_DATETIME = Regex("""^(\d{4})-(\d{2})-(\d{2})[T ](\d{2}):(\d{2})(?::(\d{2})(?:\.(\d{1,9}))?)?$""")

/** `Date.prototype.toISOString()` — always `yyyy-MM-ddTHH:mm:ss.sssZ`. */
fun Instant.toIso(): String = ISO_OUT.format(truncatedTo(ChronoUnit.MILLIS))

/** `new Date(s)`: date-only is UTC midnight, a bare date-time is local, an offset is honoured. */
fun parseJsDate(s: String): Instant? {
    val t = s.trim()
    DATE_ONLY.matchEntire(t)?.let { m ->
        val (y, mo, d) = m.destructured
        return runCatching { LocalDate.of(y.toInt(), mo.toInt(), d.toInt()).atStartOfDay(ZoneOffset.UTC).toInstant() }.getOrNull()
    }
    LOCAL_DATETIME.matchEntire(t)?.let { return parseLocalDateTime(it) }
    return runCatching { ZonedDateTime.parse(t, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant() }
        .recoverCatching { Instant.parse(t) }
        .getOrNull()
}

/** date-fns `parseISO(s)`: date-only and bare date-times are local; an offset is honoured. */
fun parseIso(s: String): Instant? {
    val t = s.trim()
    DATE_ONLY.matchEntire(t)?.let { m ->
        val (y, mo, d) = m.destructured
        return runCatching { LocalDate.of(y.toInt(), mo.toInt(), d.toInt()).atStartOfDay(finioZone).toInstant() }.getOrNull()
    }
    LOCAL_DATETIME.matchEntire(t)?.let { return parseLocalDateTime(it) }
    return runCatching { ZonedDateTime.parse(t, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant() }
        .recoverCatching { Instant.parse(t) }
        .getOrNull()
}

private fun parseLocalDateTime(m: MatchResult): Instant? {
    val g = m.groupValues
    val nanos = g[7].takeIf { it.isNotEmpty() }?.padEnd(9, '0')?.toInt() ?: 0
    return runCatching {
        LocalDateTime.of(g[1].toInt(), g[2].toInt(), g[3].toInt(), g[4].toInt(), g[5].toInt(), g[6].ifEmpty { "0" }.toInt(), nanos)
            .atZone(finioZone).toInstant()
    }.getOrNull()
}

/** Parse, or throw — for values that the store itself wrote. */
fun iso(s: String): Instant = parseIso(s) ?: error("Invalid date: $s")

/** `new Date(y, m0, d, h, min, s, ms)` — local time, with JS-style overflow normalisation. */
fun localDate(year: Int, month0: Int, day: Int = 1, hour: Int = 0, minute: Int = 0, second: Int = 0, ms: Int = 0): Instant =
    LocalDateTime.of(year, 1, 1, 0, 0)
        .plusMonths(month0.toLong())
        .plusDays((day - 1).toLong())
        .plusHours(hour.toLong())
        .plusMinutes(minute.toLong())
        .plusSeconds(second.toLong())
        .plus(ms.toLong(), ChronoUnit.MILLIS)
        .atZone(finioZone)
        .toInstant()

fun Instant.local(): ZonedDateTime = atZone(finioZone)
fun Instant.localDate(): LocalDate = atZone(finioZone).toLocalDate()

val Instant.fullYear: Int get() = local().year
/** 0-based, like `getMonth()`. */
val Instant.month0: Int get() = local().monthValue - 1
/** Day of month, like `getDate()`. */
val Instant.dayOfMonth: Int get() = local().dayOfMonth
/** 0 = Sunday … 6 = Saturday, like `getDay()`. */
val Instant.jsDay: Int get() = local().dayOfWeek.value % 7
val Instant.hours: Int get() = local().hour
val Instant.epochMs: Long get() = toEpochMilli()

fun msToInstant(ms: Long): Instant = Instant.ofEpochMilli(ms)
fun nowInstant(): Instant = Instant.ofEpochMilli(System.currentTimeMillis())

private fun ZonedDateTime.back(): Instant = toInstant()
private fun LocalDateTime.back(): Instant = atZone(finioZone).toInstant()

// ---- date-fns ----------------------------------------------------------------------------

fun startOfDay(d: Instant): Instant = d.localDate().atStartOfDay(finioZone).toInstant()
fun endOfDay(d: Instant): Instant = startOfDay(addDays(d, 1)).minusMillis(1)
fun addDays(d: Instant, n: Int): Instant = d.local().toLocalDateTime().plusDays(n.toLong()).back()
fun subDays(d: Instant, n: Int): Instant = addDays(d, -n)
fun addWeeks(d: Instant, n: Int): Instant = addDays(d, n * 7)
/** date-fns addMonths: same local time, day clamped to the target month's length. */
fun addMonths(d: Instant, n: Int): Instant = d.local().toLocalDateTime().plusMonths(n.toLong()).back()
fun subMonths(d: Instant, n: Int): Instant = addMonths(d, -n)
fun addYears(d: Instant, n: Int): Instant = addMonths(d, n * 12)
fun addHours(d: Instant, n: Int): Instant = d.plusSeconds(n * 3600L)
fun addMilliseconds(d: Instant, n: Long): Instant = d.plusMillis(n)

/** weekStartsOn: 0 = Sunday, 1 = Monday (Finio always uses 1). */
fun startOfWeek(d: Instant, weekStartsOn: Int = 1): Instant {
    val first = if (weekStartsOn == 0) DayOfWeek.SUNDAY else DayOfWeek.MONDAY
    return d.localDate().with(TemporalAdjusters.previousOrSame(first)).atStartOfDay(finioZone).toInstant()
}

fun endOfWeek(d: Instant, weekStartsOn: Int = 1): Instant = addWeeks(startOfWeek(d, weekStartsOn), 1).minusMillis(1)

fun startOfMonth(d: Instant): Instant = d.localDate().withDayOfMonth(1).atStartOfDay(finioZone).toInstant()
fun endOfMonth(d: Instant): Instant = addMonths(startOfMonth(d), 1).minusMillis(1)

fun setHours(d: Instant, h: Int): Instant = d.local().withHour(h).back()

/** date-fns setHours/minutes/seconds/ms in one go. */
fun atLocalTime(d: Instant, hour: Int, minute: Int = 0, second: Int = 0, ms: Int = 0): Instant =
    d.local().with(LocalTime.of(hour, minute, second, ms * 1_000_000)).back()

fun differenceInCalendarDays(a: Instant, b: Instant): Int =
    ChronoUnit.DAYS.between(b.localDate(), a.localDate()).toInt()

fun differenceInCalendarMonths(a: Instant, b: Instant): Int {
    val la = a.localDate(); val lb = b.localDate()
    return (la.year - lb.year) * 12 + (la.monthValue - lb.monthValue)
}

fun isSameDay(a: Instant, b: Instant): Boolean = a.localDate() == b.localDate()
fun isToday(d: Instant, now: Instant = nowInstant()): Boolean = isSameDay(d, now)
fun isYesterday(d: Instant, now: Instant = nowInstant()): Boolean = isSameDay(d, subDays(now, 1))
fun isAfter(a: Instant, b: Instant): Boolean = a.isAfter(b)
fun isBefore(a: Instant, b: Instant): Boolean = a.isBefore(b)
fun getDaysInMonth(d: Instant): Int = d.localDate().lengthOfMonth()

private val formatterCache = java.util.concurrent.ConcurrentHashMap<String, DateTimeFormatter>()

/**
 * date-fns `format` for the token subset Finio uses (d dd M MM MMM MMMM yy yyyy EEE EEEE H HH
 * h hh m mm s ss a, and 'quoted' literals). Locale is en-US like date-fns' default — except
 * `a`, which date-fns renders as "AM"/"PM" too.
 */
fun format(d: Instant, pattern: String): String {
    val f = formatterCache.getOrPut(pattern) {
        val translated = pattern.replace("yyyy", "uuuu").replace("yy", "uu")
        DateTimeFormatter.ofPattern(translated, Locale.US)
    }
    return f.format(d.local())
}
