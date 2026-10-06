package com.slowatcoding.finio.ui.charts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slowatcoding.finio.core.format.formatCurrency
import com.slowatcoding.finio.core.format.intlFormat
import com.slowatcoding.finio.ui.components.FinioCard
import com.slowatcoding.finio.ui.theme.FinioTheme
import com.slowatcoding.finio.ui.theme.FinioType
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min

// Port of web/src/components/charts/chartTheme.ts — the shared look of every chart: muted 10sp
// ticks, no tick marks or axis lines, a dashed horizontal grid, and a popover-surface tooltip.

/** `AXIS_TICK`: muted, 10px. */
internal val AxisTickStyle: TextStyle = FinioType.caption.copy(fontSize = 10.sp, lineHeight = 12.sp)

/**
 * One money-axis tick format for every chart: always compact (₹90K, ₹1.35L, ₹2L) with up to two
 * decimals, honouring "hide amounts" — `Intl.NumberFormat('en-IN', { style: 'currency',
 * notation: 'compact', maximumFractionDigits: 2 })`.
 */
fun formatAxisMoney(value: Double, hidden: Boolean = false): String =
    if (hidden) formatCurrency(value, compact = true, hidden = true)
    else intlFormat(value, minFrac = 0, maxFrac = 2, compact = true, currency = true)

/** One tooltip row: Recharts' `name<separator>value`. */
@Immutable
data class TooltipRow(val name: String, val value: String)

/** What a tooltip shows: an optional heading (the x value) and its rows. */
@Immutable
data class TooltipData(val label: String?, val rows: List<TooltipRow>, val separator: String = " : ")

/**
 * The tooltip box (`TOOLTIP_CONTENT_STYLE`): popover fill, ink text, 1dp border, 12dp radius,
 * 12sp, no shadow; the heading muted with a 2dp gap, rows 4dp apart (Recharts' item padding).
 */
@Composable
fun ChartTooltip(data: TooltipData, modifier: Modifier = Modifier) {
    val colors = FinioTheme.colors
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier
            .widthIn(max = 240.dp)
            .background(colors.popover, shape)
            .border(1.dp, colors.border, shape)
            .padding(10.dp),
    ) {
        val style = FinioType.caption.copy(fontSize = 12.sp, lineHeight = 16.sp)
        if (data.label != null) {
            Text(data.label, Modifier.padding(bottom = 2.dp), style = style, color = colors.mutedForeground)
        }
        data.rows.forEach { row ->
            Text(
                "${row.name}${data.separator}${row.value}",
                Modifier.padding(vertical = 4.dp),
                style = style,
                color = colors.popoverForeground,
            )
        }
    }
}

/**
 * The card every chart sits in (`card-elevated rounded-md p-4` + `h3 mb-3 text-sm font-semibold`).
 */
@Composable
fun ChartCard(
    title: String?,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    FinioCard(modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
        if (title != null) CardTitle(title, Modifier.padding(bottom = 12.dp))
        content()
    }
}

/** `h3 text-sm font-semibold`. */
@Composable
fun CardTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier.semantics { heading() }, style = FinioType.rowValue, color = FinioTheme.colors.foreground)
}

// ── Recharts' "nice" Y ticks (recharts-scale getNiceTickValues) ─────────────────────────────

private val MC = MathContext.DECIMAL64

private fun digitCount(value: Double): Int = if (value == 0.0) 1 else floor(log10(abs(value))).toInt() + 1

private fun formatStep(roughStep: BigDecimal, correction: Int): BigDecimal {
    if (roughStep.signum() <= 0) return BigDecimal.ZERO
    val digits = digitCount(roughStep.toDouble())
    val digitValue = BigDecimal.TEN.pow(digits, MC).let { if (digits < 0) BigDecimal.ONE.divide(BigDecimal.TEN.pow(-digits), MC) else it }
    val ratio = roughStep.divide(digitValue, MC)
    val scale = if (digits != 1) BigDecimal("0.05") else BigDecimal("0.1")
    val amended = BigDecimal(ceil(ratio.divide(scale, MC).toDouble())).add(BigDecimal(correction)).multiply(scale, MC)
    return amended.multiply(digitValue, MC)
}

private data class Step(val step: BigDecimal, val tickMin: BigDecimal, val tickMax: BigDecimal)

private fun calculateStep(lo: Double, hi: Double, count: Int, correction: Int = 0): Step {
    if (!((hi - lo) / (count - 1)).isFinite()) return Step(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)
    val step = formatStep(BigDecimal(hi).subtract(BigDecimal(lo)).divide(BigDecimal(count - 1), MC), correction)
    if (step.signum() == 0) return Step(step, BigDecimal(lo), BigDecimal(hi))
    val middle = if (lo <= 0 && hi >= 0) {
        BigDecimal.ZERO
    } else {
        val m = BigDecimal(lo).add(BigDecimal(hi)).divide(BigDecimal(2), MC)
        m.subtract(m.remainder(step, MC))
    }
    var below = ceil(middle.subtract(BigDecimal(lo)).divide(step, MC).toDouble()).toInt()
    var up = ceil(BigDecimal(hi).subtract(middle).divide(step, MC).toDouble()).toInt()
    val scaleCount = below + up + 1
    if (scaleCount > count) return calculateStep(lo, hi, count, correction + 1)
    if (scaleCount < count) {
        if (hi > 0) up += count - scaleCount else below += count - scaleCount
    }
    return Step(step, middle.subtract(BigDecimal(below).multiply(step)), middle.add(BigDecimal(up).multiply(step)))
}

/**
 * Y-axis ticks for Recharts' default numeric domain `[0, 'auto']` with 5 ticks: the domain always
 * includes zero, is widened to the data, then snapped to "nice" steps.
 */
fun niceTicks(values: List<Double>, tickCount: Int = 5): List<Double> {
    val finite = values.filter { it.isFinite() }
    val lo = min(0.0, finite.minOrNull() ?: 0.0)
    val hi = max(0.0, finite.maxOrNull() ?: 0.0)
    if (lo == hi) {
        // getTickOfSingleValue for 0: 0,1,2,3,4 shifted so the middle index holds floor((n-1)/2).
        val middleIndex = (tickCount - 1) / 2
        val middle = if (lo == 0.0) middleIndex.toDouble() else floor(lo)
        return (0 until tickCount).map { middle + (it - middleIndex) }
    }
    val s = calculateStep(lo, hi, max(tickCount, 2))
    if (s.step.signum() == 0) return listOf(lo, hi)
    val out = mutableListOf<Double>()
    var v = s.tickMin
    val limit = s.tickMax.add(s.step.multiply(BigDecimal("0.1")))
    while (v < limit && out.size < 50) {
        out += v.setScale(10, RoundingMode.HALF_UP).toDouble()
        v = v.add(s.step)
    }
    return out
}
