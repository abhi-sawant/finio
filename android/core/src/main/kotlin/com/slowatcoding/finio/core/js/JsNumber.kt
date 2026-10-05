package com.slowatcoding.finio.core.js

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs
import kotlin.math.floor

/** JS `Math.round`: half-way rounds toward +∞ (so -2.5 → -2), unlike Kotlin's roundToLong. */
fun jsRound(x: Double): Double {
    if (!x.isFinite()) return x
    val f = floor(x)
    val r = if (x - f >= 0.5) f + 1.0 else f
    return if (r == 0.0) 0.0 else r
}

/** `Math.trunc`. */
fun jsTrunc(x: Double): Double = if (x < 0) -floor(-x) else floor(x)

/** `String(n)` / template-literal formatting of a JS number: 15000 → "15000", 0.1 → "0.1". */
fun jsNumberToString(x: Double): String {
    if (x.isNaN()) return "NaN"
    if (x.isInfinite()) return if (x > 0) "Infinity" else "-Infinity"
    if (x == 0.0) return "0"
    if (x == floor(x) && abs(x) < 1e21) return BigDecimal(x).toPlainString()
    val plain = BigDecimal(x.toString()).stripTrailingZeros().toPlainString()
    return plain
}

/** `n.toFixed(digits)` — JS rounds the exact binary value half-up, as BigDecimal does here. */
fun jsToFixed(x: Double, digits: Int): String =
    BigDecimal(x).setScale(digits, RoundingMode.HALF_UP).toPlainString().let {
        if (it.startsWith("-") && BigDecimal(it).signum() == 0) it.substring(1) else it
    }
