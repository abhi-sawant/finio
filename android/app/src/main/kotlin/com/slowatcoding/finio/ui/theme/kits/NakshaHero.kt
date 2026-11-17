package com.slowatcoding.finio.ui.theme.kits

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slowatcoding.finio.ui.theme.FinioTheme
import com.slowatcoding.finio.ui.theme.rememberReducedMotion
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Naksha's hero piece (web: ui/extras/naksha.tsx) — the budget drawn to scale with dimension lines.
 * Everything below is in dp units on a sheet as wide as the hero; [draw] converts at the end.
 */
private const val BAR_TOP = 52f
private const val BAR_H = 18f
private const val DIM_Y = 40f
private const val X0 = 1f

private val DrawEase = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

private enum class Align { Start, Middle, End }

private class Label(val text: String, var at: Float, var align: Align, val tone: Int = 0) {
    var left = 0f
    var width = 0f
    var row = 0
}

/** Drop each label on the first row where it clears the ones already placed (web `place`). */
private fun place(labels: List<Label>, widths: Map<String, Float>, sheet: Float, rows: Int): List<Label> {
    val placed = ArrayList<Label>()
    for (l in labels) {
        val w = widths.getValue(l.text)
        val raw = when (l.align) {
            Align.Start -> l.at
            Align.End -> l.at - w
            Align.Middle -> l.at - w / 2f
        }
        l.width = w
        l.left = raw.coerceIn(0f, max(sheet - w, 0f))
        var row = 0
        while (row < rows - 1 && placed.any { it.row == row && l.left < it.left + it.width + 8f && it.left < l.left + w + 8f }) row++
        l.row = row
        placed.add(l)
    }
    return placed
}

private fun plural(n: Int, unit: String) = "$n $unit" + if (n == 1) "" else "s"

internal object NakshaHero {
    @Composable
    fun Footer(elapsed: Float, spentPercent: Float, isOver: Boolean, daysLeft: Int) {
        val colors = FinioTheme.colors
        val dark = colors.isDark
        val ln = if (dark) Color(0xFFDBE8FB) else Color(0xFF1B4AA5)
        val ln2 = ln.copy(alpha = if (dark) 0.4f else 0.34f)
        val red = if (dark) Color(0xFFFFA08F) else Color(0xFFC4271B)
        val reduced = rememberReducedMotion()
        val clock = remember { Animatable(if (reduced) 2f else 0f) }
        LaunchedEffect(Unit) { if (!reduced) clock.animateTo(1.2f, tween(1200, easing = LinearEasing)) }

        val measurer = rememberTextMeasurer()
        val density = LocalDensity.current.density
        val labelStyle = remember { TextStyle(fontFamily = NakshaFonts.Mono, fontWeight = FontWeight(500), fontSize = 13.sp, fontFeatureSettings = "tnum") }

        val pct = max(spentPercent, 0f)
        val spentInt = pct.roundToInt()
        val leftInt = (100 - spentInt).coerceAtLeast(0)
        val dayText = if (daysLeft <= 0) "Last day" else plural(daysLeft, "day") + " left"

        BoxWithConstraints(
            Modifier.fillMaxWidth().padding(top = 14.dp).drawBehind {
                drawLine(ln2, Offset(0f, 0.5f * density), Offset(size.width, 0.5f * density), 1f * density)
            }.padding(top = 8.dp).clearAndSetSemantics { },
        ) {
            val sheet = min(maxWidth.value, 400f)
            val x1 = sheet - 1f
            val sp = x1 - X0
            val scale = if (isOver) max(pct, 100.1f) else 100f
            val xLimit = if (isOver) X0 + sp * 100f / scale else x1
            val xSpent = if (isOver) x1 else X0 + sp * pct.coerceIn(0f, 100f) / 100f
            val xDatum = X0 + (xLimit - X0) * elapsed.coerceIn(0f, 1f)
            val split = if (isOver) xLimit else xSpent

            val top = if (isOver) {
                listOf(
                    Label("Limit 100%", (X0 + xLimit) / 2f, Align.Middle),
                    Label("+${spentInt - 100}% over", (xLimit + x1) / 2f, Align.Middle, 2),
                )
            } else {
                listOf(
                    Label("$spentInt% spent", (X0 + xSpent) / 2f, Align.Middle),
                    Label("$leftInt% left", (xSpent + x1) / 2f, Align.Middle),
                )
            }
            val bottom = listOf(
                Label("0", X0, Align.Start, 1),
                Label(if (isOver) "$spentInt%" else "100%", x1, Align.End, 1),
                Label(dayText, xDatum, Align.Middle, 1),
            )
            val texts = (top + bottom).map { it.text }.distinct()
            val widths = texts.associateWith { measurer.measure(it, labelStyle).size.width / density }
            // A span too short for its label hands the label to the nearest end.
            if (!isOver) {
                val (spent, left) = top
                if (xSpent - X0 < widths.getValue(spent.text) + 14f) { spent.align = Align.Start; spent.at = X0 }
                if (x1 - xSpent < widths.getValue(left.text) + 14f) { left.align = Align.End; left.at = x1 }
            }
            val topPlaced = place(top, widths, sheet, 2)
            val botPlaced = place(bottom, widths, sheet, 2)
            val topUsed = topPlaced.maxOf { it.row }
            val botUsed = botPlaced.maxOf { it.row }
            val yOff = if (topUsed > 0) 16f else 0f
            val height = yOff + BAR_TOP + BAR_H + 34f + botUsed * 16f
            val hatch = remember(ln, dark) { HatchBrush(ln.copy(alpha = 0.85f), 1.1f) }
            val hatchRed = remember(red) { HatchBrush(red, 1.1f) }

            Canvas(Modifier.widthIn(max = 400.dp).fillMaxWidth().height(height.dp)) {
                val d = density
                fun px(v: Float) = v * d
                translate(0f, px(yOff)) {
                    val bt = BAR_TOP
                    val bb = BAR_TOP + BAR_H
                    // extension lines
                    for (x in listOf(X0, split, x1)) drawLine(ln2, Offset(px(x), px(DIM_Y - 8)), Offset(px(x), px(bt)), px(1f))
                    // dimension spans: line draws in, arrowheads fade in after it
                    fun dim(a: Float, b: Float, delay: Float, tone: Color) {
                        if (b - a < 16f) return
                        val t = DrawEase.transform(((clock.value - delay) / 0.55f).coerceIn(0f, 1f))
                        val alpha = ((clock.value - delay - 0.4f) / 0.2f).coerceIn(0f, 1f)
                        val y = px(DIM_Y)
                        if (t > 0f) drawLine(tone, Offset(px(a), y), Offset(px(a + (b - a) * t), y), px(1f))
                        if (alpha > 0f) {
                            val head = Path().apply {
                                moveTo(px(a), y); lineTo(px(a + 7f), y - px(2.6f)); lineTo(px(a + 7f), y + px(2.6f)); close()
                                moveTo(px(b), y); lineTo(px(b - 7f), y - px(2.6f)); lineTo(px(b - 7f), y + px(2.6f)); close()
                            }
                            drawPath(head, tone.copy(alpha = alpha))
                        }
                    }
                    dim(X0, split, 0.05f, ln)
                    dim(split, x1, 0.2f, if (isOver) red else ln)
                    // labels
                    fun text(l: Label, baseline: Float) {
                        val color = when (l.tone) { 2 -> red; 1 -> colors.mutedForeground; else -> colors.foreground }
                        val r = measurer.measure(l.text, labelStyle)
                        drawText(r, color, Offset(px(l.left), px(baseline) - r.firstBaseline))
                    }
                    topPlaced.forEach { text(it, if (it.row == 0) DIM_Y - 7f else DIM_Y - 23f) }
                    // the bar, hatched where it is spent
                    val fillBrush = if (isOver) hatchRed else hatch
                    drawRect(fillBrush, Offset(px(X0), px(bt)), Size(px(max(xSpent - X0, 0f)), px(BAR_H)))
                    if (isOver) drawRect(hatchRed, Offset(px(xLimit), px(bt)), Size(px(x1 - xLimit), px(BAR_H)))
                    drawRect(ln, Offset(px(X0), px(bt)), Size(px(x1 - X0), px(BAR_H)), style = Stroke(px(1.5f)))
                    drawLine(if (isOver) red else ln, Offset(px(split), px(bt)), Offset(px(split), px(bb)), px(1.5f))
                    for (f in listOf(0.25f, 0.5f, 0.75f)) {
                        val x = px(X0 + (x1 - X0) * f)
                        drawLine(ln, Offset(x, px(bb)), Offset(x, px(bb + 4f)), px(1f))
                    }
                    // datum: today — dash-dot centre line and a triangle
                    drawLine(
                        red, Offset(px(xDatum), px(DIM_Y + 6f)), Offset(px(xDatum), px(bb + 9f)), px(1.2f),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(px(9f), px(3f), px(2f), px(3f))),
                    )
                    drawPath(
                        Path().apply {
                            moveTo(px(xDatum - 4f), px(bb + 13f)); lineTo(px(xDatum + 4f), px(bb + 13f)); lineTo(px(xDatum), px(bb + 7f)); close()
                        },
                        red,
                    )
                    botPlaced.forEach { text(it, if (it.row == 0) bb + 26f else bb + 42f) }
                }
            }
        }
    }
}
