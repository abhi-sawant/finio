package com.slowatcoding.finio.core.util

import com.slowatcoding.finio.core.js.jsRound

// Port of web/src/utils/chartTable.ts — helpers for the data-table fallback under every chart.

data class SampledRows<T>(val rows: List<T>, val sampled: Boolean)

/**
 * Evenly-spaced sample of a long series, always keeping the first and last points. Returns the
 * input untouched when it already fits, and reports whether it thinned anything.
 */
fun <T> sampleForTable(items: List<T>, max: Int = 24): SampledRows<T> {
    if (max < 2) return SampledRows(items.take(maxOf(max, 0)), items.size > max)
    if (items.size <= max) return SampledRows(items, false)

    val step = (items.size - 1).toDouble() / (max - 1)
    val rows = ArrayList<T>(max)
    for (i in 0 until max) rows += items[jsRound(i * step).toInt()]
    return SampledRows(rows, true)
}
