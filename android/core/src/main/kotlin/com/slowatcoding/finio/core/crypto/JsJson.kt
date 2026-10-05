package com.slowatcoding.finio.core.crypto

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

// Byte-exact `JSON.stringify` for a JsonElement. The encrypted backup is AES-GCM over the UTF-8
// of `JSON.stringify(payload)`, so for the ciphertext to be reproducible across platforms (and
// for golden tests to prove it) Kotlin must serialize exactly as V8 does: no whitespace, V8's
// string escaping (lowercase \u00xx, lone surrogates escaped), JS Number::toString for every
// number (4200.0 → "4200", 1e21 → "1e+21", 1e-7 → "1e-7"), and JS own-key order — array-index
// keys ("0", "12") first in ascending numeric order, then the rest in insertion order.

/** `JSON.stringify(value)` — compact form. */
fun jsonStringify(value: JsonElement): String = StringBuilder().also { write(it, value) }.toString()

private fun write(sb: StringBuilder, v: JsonElement) {
    when (v) {
        is JsonNull -> sb.append("null")
        is JsonPrimitive -> when {
            v.isString -> quote(sb, v.content)
            v.content == "true" || v.content == "false" -> sb.append(v.content)
            else -> {
                val d = v.content.toDouble()
                sb.append(if (d.isFinite()) jsNumberString(d) else "null")
            }
        }
        is JsonArray -> {
            sb.append('[')
            v.forEachIndexed { i, e -> if (i > 0) sb.append(','); write(sb, e) }
            sb.append(']')
        }
        is JsonObject -> {
            sb.append('{')
            var first = true
            for (key in jsOwnKeyOrder(v.keys)) {
                if (!first) sb.append(',')
                first = false
                quote(sb, key)
                sb.append(':')
                write(sb, v.getValue(key))
            }
            sb.append('}')
        }
    }
}

/** An "array index" key: canonical numeric string for an integer in [0, 2³² − 2]. */
private fun isArrayIndex(key: String): Boolean {
    if (key.isEmpty() || key.length > 10 || key.any { it !in '0'..'9' }) return false
    if (key.length > 1 && key[0] == '0') return false
    return key.toLong() <= 4294967294L
}

internal fun jsOwnKeyOrder(keys: Collection<String>): List<String> {
    val (index, rest) = keys.partition(::isArrayIndex)
    return index.sortedBy { it.toLong() } + rest
}

/** QuoteJSONString (ES2019 well-formed stringify). */
private fun quote(sb: StringBuilder, s: String) {
    sb.append('"')
    var i = 0
    while (i < s.length) {
        val c = s[i]
        when {
            c == '"' -> sb.append("\\\"")
            c == '\\' -> sb.append("\\\\")
            c == '\b' -> sb.append("\\b")
            c == '\u000C' -> sb.append("\\f")
            c == '\n' -> sb.append("\\n")
            c == '\r' -> sb.append("\\r")
            c == '\t' -> sb.append("\\t")
            c < ' ' -> sb.append("\\u").append(String.format(java.util.Locale.ROOT, "%04x", c.code))
            c.isHighSurrogate() && i + 1 < s.length && s[i + 1].isLowSurrogate() -> {
                sb.append(c).append(s[i + 1]); i++
            }
            c.isSurrogate() -> sb.append("\\u").append(String.format(java.util.Locale.ROOT, "%04x", c.code))
            else -> sb.append(c)
        }
        i++
    }
    sb.append('"')
}

/**
 * JS Number::toString(10): the shortest decimal that round-trips (closest one on a tie), laid
 * out per ES2024 6.1.6.1.20. Independent of the JDK's Double.toString, which before JDK 19 was
 * not always shortest.
 */
fun jsNumberString(x: Double): String {
    if (x.isNaN()) return "NaN"
    if (x == 0.0) return "0"
    if (x.isInfinite()) return if (x > 0) "Infinity" else "-Infinity"
    if (x < 0) return "-" + jsNumberString(-x)

    val exact = BigDecimal(x)
    var best: BigDecimal = exact
    for (p in 1..17) {
        val r = exact.round(MathContext(p, RoundingMode.HALF_EVEN))
        if (r.toDouble() == x) { best = r; break }
    }
    val stripped = best.stripTrailingZeros()
    val digits = stripped.unscaledValue().toString()
    val k = digits.length
    val n = k - stripped.scale() // x = 0.d1d2…dk × 10^n

    return when {
        n in k..21 -> digits + "0".repeat(n - k)
        n in 1..21 -> digits.substring(0, n) + "." + digits.substring(n)
        n in -5..0 -> "0." + "0".repeat(-n) + digits
        else -> {
            val e = n - 1
            val exp = (if (e < 0) "-" else "+") + kotlin.math.abs(e)
            if (k == 1) "${digits}e$exp" else "${digits[0]}.${digits.substring(1)}e$exp"
        }
    }
}
