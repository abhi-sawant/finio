package com.slowatcoding.finio.ui.mudra

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.slowatcoding.finio.ui.theme.FinioShapes
import com.slowatcoding.finio.ui.theme.FinioTheme
import com.slowatcoding.finio.ui.theme.TailwindEase

/**
 * Paints [brush] through a repeating window mask — the CSS
 * `mask: repeating-linear-gradient(180deg, #000 0 <on>, transparent <on> <on+off>)` every Mudra
 * thread uses. [vertical] windows run top → bottom (the note threads); horizontal ones run
 * left → right (goal progress). The brush is sized to the whole strip, so a gradient runs
 * continuously through the gaps.
 */
internal fun DrawScope.drawWindows(
    brush: Brush,
    topLeft: Offset,
    size: Size,
    on: Float,
    off: Float,
    vertical: Boolean,
    alpha: Float = 1f,
) {
    val length = if (vertical) size.height else size.width
    var p = 0f
    while (p < length) {
        val seg = minOf(on, length - p)
        if (vertical) {
            drawRect(brush, Offset(topLeft.x, topLeft.y + p), Size(size.width, seg), alpha)
        } else {
            drawRect(brush, Offset(topLeft.x + p, topLeft.y), Size(seg, size.height), alpha)
        }
        p += on + off
    }
}

/** Mirrors `budgetHealth()` in web/src/utils/calculations.ts. */
enum class BudgetHealth { Ok, Near, Over }

/** `BUDGET_NEAR_LIMIT_PERCENT`. */
const val BudgetNearLimitPercent = 85f

fun budgetHealth(isOver: Boolean, percent: Float): BudgetHealth = when {
    isOver -> BudgetHealth.Over
    percent >= BudgetNearLimitPercent -> BudgetHealth.Near
    else -> BudgetHealth.Ok
}

/**
 * Goals and other toward-a-target bars: the colour-shift thread (`thread-fill`) masked into 10px
 * windows with 3px gaps, inside an 8px round muted track. [percent] is 0..100 (clamped for the
 * fill only).
 */
@Composable
fun ThreadProgressBar(percent: Float, modifier: Modifier = Modifier, valueText: String? = null) {
    val colors = FinioTheme.colors
    val thread = FinioTheme.brushes.thread
    val fill by animateFloatAsState(percent.coerceIn(0f, 100f) / 100f, tween(150, easing = TailwindEase), label = "thread")
    Box(
        modifier
            .progressSemantics(percent, valueText)
            .fillMaxWidth()
            .height(8.dp)
            .clip(FinioShapes.full)
            .background(colors.muted)
            .drawBehind {
                val w = size.width * fill
                if (w > 0f) {
                    // The gradient spans the *fill* (the CSS sets it on the fill element).
                    drawWindows(
                        brush = thread,
                        topLeft = Offset.Zero,
                        size = Size(w, size.height),
                        on = 10.dp.toPx(),
                        off = 3.dp.toPx(),
                        vertical = false,
                    )
                }
            },
    )
}

/**
 * The budget bar (BudgetHealthBadge.tsx `BudgetProgressBar`): an 8px muted track whose round fill
 * is magenta when over, amber when near the limit, and [okFill] otherwise — usually the scope's
 * own colour, or the [RegisterBar] hatch for the overall budget.
 */
@Composable
fun BudgetProgressBar(
    percent: Float,
    isOver: Boolean,
    okFill: Brush,
    modifier: Modifier = Modifier,
    valueText: String? = null,
) {
    val colors = FinioTheme.colors
    val health = budgetHealth(isOver, percent)
    val fill by animateFloatAsState(percent.coerceIn(0f, 100f) / 100f, tween(150, easing = TailwindEase), label = "budget")
    Box(
        modifier
            .progressSemantics(percent, valueText)
            .fillMaxWidth()
            .height(8.dp)
            .clip(FinioShapes.full)
            .background(colors.muted),
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(fill)
                .clip(FinioShapes.full)
                .background(
                    when (health) {
                        BudgetHealth.Over -> SolidColor(colors.destructive)
                        BudgetHealth.Near -> SolidColor(colors.warning)
                        BudgetHealth.Ok -> okFill
                    },
                ),
        )
    }
}

/** [BudgetProgressBar] in the overall budget's engraved `--register` hatch. */
@Composable
fun RegisterBar(percent: Float, isOver: Boolean, modifier: Modifier = Modifier, valueText: String? = null) {
    val density = LocalDensity.current.density
    BudgetProgressBar(percent, isOver, FinioTheme.brushes.register(density), modifier, valueText)
}

/** [BudgetProgressBar] with a flat colour fill (a category's colour). */
@Composable
fun BudgetProgressBar(percent: Float, isOver: Boolean, okColor: Color, modifier: Modifier = Modifier, valueText: String? = null) =
    BudgetProgressBar(percent, isOver, SolidColor(okColor), modifier, valueText)

private fun Modifier.progressSemantics(percent: Float, valueText: String?) = semantics {
    progressBarRangeInfo = ProgressBarRangeInfo(percent.coerceIn(0f, 100f), 0f..100f)
    if (valueText != null) stateDescription = valueText
}
