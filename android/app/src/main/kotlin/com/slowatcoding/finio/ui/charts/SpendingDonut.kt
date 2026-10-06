package com.slowatcoding.finio.ui.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.slowatcoding.finio.core.calc.transactionCategoryAmounts
import com.slowatcoding.finio.core.model.Transaction
import com.slowatcoding.finio.core.model.TransactionType
import com.slowatcoding.finio.ui.common.collectFinanceState
import com.slowatcoding.finio.ui.common.rememberDerived
import com.slowatcoding.finio.ui.common.rememberMoneyFormatter
import com.slowatcoding.finio.ui.components.parseHexColor
import com.slowatcoding.finio.ui.theme.FinioShapes
import com.slowatcoding.finio.ui.theme.FinioTheme
import com.slowatcoding.finio.ui.theme.FinioType
import kotlin.math.atan2
import kotlin.math.roundToInt
import kotlin.math.sqrt

@Immutable
private data class Slice(val name: String, val value: Double, val color: String?)

/**
 * Port of SpendingDonut.tsx — expenses by category (splits count per portion), the eight biggest,
 * as a 66/112 ring with 2° padding and card-coloured 2dp separators in a 288dp square, the total
 * in the hole and a legend below. Tap a slice for its tooltip.
 */
@Composable
fun SpendingDonut(transactions: List<Transaction>, modifier: Modifier = Modifier) {
    val state by collectFinanceState()
    val money = rememberMoneyFormatter()
    val colors = FinioTheme.colors
    val categories = state.categories

    val data = rememberDerived(transactions, categories) {
        val catMap = categories.associateBy { it.id }
        val byCategory = LinkedHashMap<String, Double>()
        for (tx in transactions) {
            if (tx.type != TransactionType.Expense) continue
            for (part in transactionCategoryAmounts(tx)) {
                byCategory[part.categoryId] = (byCategory[part.categoryId] ?: 0.0) + part.amount
            }
        }
        byCategory.entries
            .map { (id, amount) -> catMap[id].let { Slice(it?.name ?: "Other", amount, it?.color) } }
            .sortedByDescending { it.value }
            .take(8)
    } ?: return
    if (data.isEmpty()) return

    val total = data.sumOf { it.value }
    val sliceColors = data.map { it.color?.let { c -> parseHexColor(c, colors.mutedForeground) } ?: colors.mutedForeground }

    ChartCard("Spending by category", modifier) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(
                Modifier.size(288.dp).semantics {
                    contentDescription = "Expenses split across ${data.size} categor${if (data.size == 1) "y" else "ies"}, " +
                        "${money(total, compact = true)} in total. Every category is listed below."
                },
            ) {
                Donut(data, sliceColors) { money(it) }
                Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Total", style = FinioType.caption, color = colors.mutedForeground)
                    Text(money(total, compact = true), style = FinioType.body.copy(fontWeight = FontWeight.Bold), color = colors.foreground)
                }
            }
            Column(Modifier.width(288.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                data.forEachIndexed { i, item ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(Modifier.size(10.dp).clip(FinioShapes.full).background(sliceColors[i]))
                            Text(
                                item.name,
                                Modifier.widthIn(max = 100.dp),
                                style = FinioType.caption,
                                color = colors.mutedForeground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Text(
                            money(item.value, compact = true),
                            Modifier.padding(start = 8.dp),
                            style = FinioType.label,
                            color = colors.foreground,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Donut(data: List<Slice>, sliceColors: List<Color>, format: (Double) -> String) {
    val colors = FinioTheme.colors
    var active by remember(data) { mutableStateOf<Pair<Int, Offset>?>(null) }
    val total = data.sumOf { it.value }
    val pad = 2f
    // Recharts: 0° at 3 o'clock, counter-clockwise; full circle → every slice gives up 2°.
    val nonZero = data.count { it.value != 0.0 }
    val real = 360f - pad * nonZero
    val angles = remember(data) {
        var start = 0f
        data.map { d ->
            val sweep = if (total > 0) (d.value / total * real).toFloat() else 0f
            val a = start to start + sweep
            start += sweep + if (d.value != 0.0) pad else 0f
            a
        }
    }

    Layout(
        content = {
            Canvas(
                Modifier.fillMaxSize().pointerInput(data) {
                    detectTapGestures { pos ->
                        val c = Offset(size.width / 2f, size.height / 2f)
                        val d = pos - c
                        val r = sqrt(d.x * d.x + d.y * d.y)
                        val hit = if (r < 66.dp.toPx() || r > 112.dp.toPx()) {
                            -1
                        } else {
                            var deg = Math.toDegrees(atan2(-d.y, d.x).toDouble()).toFloat()
                            if (deg < 0) deg += 360f
                            angles.indexOfFirst { (a0, a1) -> deg in a0..a1 }
                        }
                        active = if (hit < 0 || active?.first == hit) null else hit to pos
                    }
                },
            ) {
                val c = center
                val outer = 112.dp.toPx()
                val inner = 66.dp.toPx()
                val outerRect = Rect(c, outer)
                val innerRect = Rect(c, inner)
                angles.forEachIndexed { i, (a0, a1) ->
                    val sweep = a1 - a0
                    if (sweep <= 0f) return@forEachIndexed
                    val path = Path().apply {
                        arcTo(outerRect, -a0, -sweep, true)
                        arcTo(innerRect, -a1, sweep, false)
                        close()
                    }
                    drawPath(path, sliceColors[i])
                    drawPath(path, colors.card, style = Stroke(2.dp.toPx()))
                }
            }
            active?.let { (i, _) ->
                ChartTooltip(TooltipData(label = null, rows = listOf(TooltipRow(data[i].name, format(data[i].value)))))
            }
        },
    ) { measurables, constraints ->
        val canvas = measurables[0].measure(Constraints.fixed(constraints.maxWidth, constraints.maxHeight))
        val tip = measurables.getOrNull(1)?.measure(Constraints(maxWidth = constraints.maxWidth))
        layout(constraints.maxWidth, constraints.maxHeight) {
            canvas.place(0, 0)
            val at = active?.second
            if (tip != null && at != null) {
                val gap = 10.dp.roundToPx()
                val x = at.x.roundToInt() + gap
                val y = at.y.roundToInt() + gap
                tip.place(
                    if (x + tip.width <= constraints.maxWidth) x else (at.x.roundToInt() - gap - tip.width).coerceAtLeast(0),
                    if (y + tip.height <= constraints.maxHeight) y else (at.y.roundToInt() - gap - tip.height).coerceAtLeast(0),
                )
            }
        }
    }
}
