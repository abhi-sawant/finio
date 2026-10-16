package com.slowatcoding.finio.core.format

import com.slowatcoding.finio.core.js.format
import com.slowatcoding.finio.core.js.isToday
import com.slowatcoding.finio.core.js.isYesterday
import com.slowatcoding.finio.core.js.iso
import com.slowatcoding.finio.core.js.jsNumberToString
import com.slowatcoding.finio.core.js.jsRound
import com.slowatcoding.finio.core.js.jsToFixed
import com.slowatcoding.finio.core.js.local
import com.slowatcoding.finio.core.js.nowInstant
import com.slowatcoding.finio.core.util.isJsWhitespace
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.time.Instant
import kotlin.math.abs

// Port of web/src/utils/formatters.ts. Finio is INR-only, and every number goes through a
// hand-rolled copy of `Intl.NumberFormat('en-IN', …)` ([intlFormat]) rather than the JVM's ICU,
// whose data (and therefore output) differs between Android releases.

/** Only shorten amounts at or above this magnitude. */
private const val COMPACT_THRESHOLD = 100_000.0

/** The bare currency symbol Intl renders for INR in en-IN. */
const val CURRENCY_PREFIX = "₹"

/**
 * Format a rupee amount. `compact` alone is gated per-value by the 1-lakh threshold;
 * [forceCompact] skips the gate (pair it with [shouldCompactGroup]). [precise] = false rounds to
 * whole rupees. Amounts with real paise are padded to two decimals (₹450.50).
 */
fun formatCurrency(
    amount: Double,
    compact: Boolean = false,
    hidden: Boolean = false,
    precise: Boolean = true,
    forceCompact: Boolean = false,
): String {
    if (hidden) return "${if (amount < 0) "-" else ""}$CURRENCY_PREFIX••••"
    val useCompact = forceCompact || (compact && abs(amount) >= COMPACT_THRESHOLD)
    val hasPaise = !useCompact && precise && jsRound(abs(amount) * 100) % 100 != 0.0
    return intlFormat(
        amount,
        minFrac = if (hasPaise) 2 else 0,
        maxFrac = if (useCompact) 1 else if (precise) 2 else 0,
        compact = useCompact,
        currency = true,
    )
}

/** Whether a *group* of related amounts should render compact together. */
fun shouldCompactGroup(amounts: List<Double>): Boolean = amounts.any { abs(it) >= COMPACT_THRESHOLD }

/** "Today", "Yesterday", or "Mon, 5 Oct". [now] defaults to the clock (the TS reads `new Date()`). */
fun formatDate(dateStr: String, now: Instant = nowInstant()): String {
    val date = iso(dateStr)
    if (isToday(date, now)) return "Today"
    if (isYesterday(date, now)) return "Yesterday"
    return format(date, "EEE, d MMM")
}

/** "5 October 2026" — dialog titles and long-form copy. */
fun formatFullDate(dateStr: String): String = format(iso(dateStr), "d MMMM yyyy")

/** "5 Oct 2026" — the canonical date format everywhere a year is shown. */
fun formatShortDate(date: Instant): String = format(date, "d MMM yyyy")
fun formatShortDate(date: String): String = formatShortDate(iso(date))

/** "5 Oct" — compact day + month where the year is obvious from context. */
fun formatDayMonth(date: Instant): String = format(date, "d MMM")
fun formatDayMonth(date: String): String = formatDayMonth(iso(date))

fun formatTime(dateStr: String): String = format(iso(dateStr), "h:mm a")

/** The user's local calendar day of an ISO instant, as `yyyy-MM-dd`. */
fun localDayKey(iso: String): String = format(iso(iso), "yyyy-MM-dd")

/** Today's local calendar day as `yyyy-MM-dd`. */
fun todayKey(now: Instant = nowInstant()): String = format(now, "yyyy-MM-dd")

/** The value an `<input type="datetime-local">` expects, in local time: `yyyy-MM-ddTHH:mm`. */
fun toLocalDateTimeInputValue(d: Instant): String {
    val l = d.local()
    fun pad(n: Int) = n.toString().padStart(2, '0')
    return "${l.year}-${pad(l.monthValue)}-${pad(l.dayOfMonth)}T${pad(l.hour)}:${pad(l.minute)}"
}
fun toLocalDateTimeInputValue(iso: String): String = toLocalDateTimeInputValue(iso(iso))

/**
 * Format a raw numeric input string using the Indian number system, keeping the decimal part
 * intact: "122999" → "1,22,999", "122999.5" → "1,22,999.5". Mirrors `raw.split('.')` (anything
 * after a second dot is dropped) and `parseInt` (leading junk-free prefix, NaN → "0").
 */
fun formatInputAmount(raw: String): String {
    if (raw.isEmpty()) return "0"
    val parts = raw.split('.')
    val intPart = parts[0]
    val decPart = parts.getOrNull(1)
    val intNum = jsParseInt(intPart.ifEmpty { "0" })
    val formatted = if (intNum.isNaN()) "0" else intlFormat(intNum, 0, 3, compact = false, currency = false)
    return if (decPart != null) "$formatted.$decPart" else formatted
}

/** 1 → "1st", 22 → "22nd". Used for month start days. */
fun formatOrdinal(value: Int): String {
    val teens = value % 100
    if (teens in 11..13) return "${value}th"
    return when (value % 10) {
        1 -> "${value}st"
        2 -> "${value}nd"
        3 -> "${value}rd"
        else -> "${value}th"
    }
}

/** Format a byte count as a human-readable size, e.g. 2_400 → "2.3 KB". */
fun formatFileSize(bytes: Double): String {
    if (bytes < 1024) return "${jsNumberToString(bytes)} B"
    val units = listOf("KB", "MB", "GB")
    var value = bytes / 1024
    var unitIndex = 0
    while (value >= 1024 && unitIndex < units.size - 1) {
        value /= 1024
        unitIndex++
    }
    return "${jsToFixed(value, 1)} ${units[unitIndex]}"
}
fun formatFileSize(bytes: Long): String = formatFileSize(bytes.toDouble())

/** Format a 0..1 ratio as a +/- signed percentage. */
fun formatPercentChange(ratio: Double): String {
    val pct = jsRound(ratio * 100)
    val sign = if (pct > 0) "+" else ""
    return "$sign${jsNumberToString(pct)}%"
}

// ---- Intl.NumberFormat('en-IN') -----------------------------------------------------------

/** JS `parseInt(s, 10)`: optional leading whitespace and sign, then the longest digit prefix. */
internal fun jsParseInt(s: String): Double {
    var i = 0
    while (i < s.length && isJsWhitespace(s[i])) i++
    var sign = 1.0
    if (i < s.length && (s[i] == '+' || s[i] == '-')) {
        if (s[i] == '-') sign = -1.0
        i++
    }
    val start = i
    while (i < s.length && s[i] in '0'..'9') i++
    if (i == start) return Double.NaN
    return sign * s.substring(start, i).toDouble()
}

/**
 * The shortest decimal that round-trips to [x] (what ICU's DecimalQuantity uses for a double,
 * and what JS prints). Avoids `Double.toString`, which is not always shortest before JDK 19.
 */
internal fun shortestDecimal(x: Double): BigDecimal {
    if (x == 0.0) return BigDecimal.ZERO
    val exact = BigDecimal(x)
    for (p in 1..17) {
        val candidate = exact.round(MathContext(p, RoundingMode.HALF_EVEN))
        if (candidate.toDouble() == x) return candidate
    }
    return exact
}

/** ICU magnitude: the power of ten of the leading digit. */
private fun BigDecimal.magnitude(): Int = precision() - scale() - 1

/** en-IN short compact patterns: power of ten to divide by, and the suffix. */
private fun compactPattern(magnitude: Int): Pair<Int, String> = when {
    magnitude < 3 -> 0 to ""
    magnitude < 5 -> 3 to "K"
    magnitude < 7 -> 5 to "L"
    magnitude < 10 -> 7 to "Cr"
    magnitude < 12 -> 10 to "KCr"
    else -> 12 to "LCr"
}

/** Indian grouping (3, then 2s). ICU only groups when the number has `2 + minGrouping` digits. */
private fun groupIndian(digits: String, minGrouping: Int): String {
    if (digits.length < 3 + minGrouping) return digits
    val head = digits.substring(0, digits.length - 3)
    val tail = digits.substring(digits.length - 3)
    val sb = StringBuilder()
    val firstLen = head.length % 2
    if (firstLen > 0) sb.append(head, 0, firstLen)
    var i = firstLen
    while (i < head.length) {
        if (sb.isNotEmpty()) sb.append(',')
        sb.append(head, i, i + 2)
        i += 2
    }
    return "$sb,$tail"
}

/**
 * `new Intl.NumberFormat('en-IN', { style: currency ? 'currency' : 'decimal', currency: 'INR',
 * notation: compact ? 'compact' : 'standard', minimumFractionDigits, maximumFractionDigits })
 * .format(x)`, reproduced exactly: half-expand rounding of the shortest decimal, a sign on any
 * negative value including -0 and values that round to zero, en-IN compact suffixes
 * (K/L/Cr/KCr/LCr) with ICU's re-rounding when a value rounds up into the next magnitude, and
 * compact notation's minimum-grouping-digits of 2.
 */
fun intlFormat(x: Double, minFrac: Int, maxFrac: Int, compact: Boolean, currency: Boolean): String {
    val prefix = if (currency) CURRENCY_PREFIX else ""
    if (x.isNaN()) return "${prefix}NaN"
    val negative = x < 0 || (x == 0.0 && 1.0 / x < 0)
    val sign = if (negative) "-" else ""
    if (x.isInfinite()) return "$sign$prefix∞"

    val value = shortestDecimal(abs(x))
    fun round(v: BigDecimal) = v.setScale(maxFrac, RoundingMode.HALF_UP)

    var suffix = ""
    val rounded: BigDecimal
    if (compact && value.signum() != 0) {
        val magnitude = value.magnitude()
        val (power, sfx) = compactPattern(magnitude)
        var r = round(value.movePointLeft(power))
        suffix = sfx
        if (r.signum() != 0 && r.stripTrailingZeros().magnitude() != magnitude - power) {
            val (power2, sfx2) = compactPattern(magnitude + 1)
            if (power2 != power) {
                r = round(value.movePointLeft(power2))
                suffix = sfx2
            }
        }
        rounded = r
    } else {
        rounded = round(value)
    }

    val plain = rounded.toPlainString()
    val dot = plain.indexOf('.')
    val intDigits = if (dot < 0) plain else plain.substring(0, dot)
    var frac = if (dot < 0) "" else plain.substring(dot + 1)
    while (frac.length > minFrac && frac.endsWith('0')) frac = frac.dropLast(1)
    while (frac.length < minFrac) frac += "0"

    val grouped = groupIndian(intDigits, if (compact) 2 else 1)
    return buildString {
        append(sign).append(prefix).append(grouped)
        if (frac.isNotEmpty()) append('.').append(frac)
        append(suffix)
    }
}
