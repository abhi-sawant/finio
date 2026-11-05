package com.slowatcoding.finio.ui.mudra

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale as drawScale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slowatcoding.finio.ui.theme.BaghScene
import com.slowatcoding.finio.ui.theme.FinioTheme
import com.slowatcoding.finio.ui.theme.FinioType
import com.slowatcoding.finio.ui.theme.VisualTheme
import com.slowatcoding.finio.ui.theme.rememberReducedMotion
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/*
 * Pieces that belong to one visual theme each (the web's ui/theme-extras.tsx). Call sites place
 * them unconditionally and each draws nothing unless its own theme is active.
 */

private val Ink = Color(0xFF1B1230)

/** Gulal: a price-tag burst stamped on the hero — how much of the budget is used. */
@Composable
fun BurstBadge(text: String, modifier: Modifier = Modifier) {
    if (FinioTheme.visual != VisualTheme.Gulal) return
    val reduced = rememberReducedMotion()
    val transition = rememberInfiniteTransition(label = "burst")
    val wobble by transition.animateFloat(
        initialValue = 12f, targetValue = 4f,
        animationSpec = infiniteRepeatable(tween(1600), RepeatMode.Reverse), label = "burst-rot",
    )
    val pulse by transition.animateFloat(
        initialValue = 1f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(1600), RepeatMode.Reverse), label = "burst-scale",
    )
    Box(
        modifier
            .size(76.dp)
            .rotate(if (reduced) 12f else wobble)
            .scale(if (reduced) 1f else pulse)
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val path = Path()
            for (i in 0 until 28) {
                val a = PI * i / 14
                val r = (if (i % 2 == 1) 31f else 38f) / 80f * size.minDimension
                val x = size.width / 2 + (cos(a) * r).toFloat()
                val y = size.height / 2 + (sin(a) * r).toFloat()
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            drawPath(path, Color(0xFFFFE14D))
            drawPath(path, Ink, style = Stroke(2.5.dp.toPx(), join = androidx.compose.ui.graphics.StrokeJoin.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text, style = FinioType.money.copy(fontSize = 20.sp, lineHeight = 20.sp), color = Ink, maxLines = 1)
            Text("used", style = FinioType.caption.copy(fontSize = 11.sp, lineHeight = 11.sp, fontWeight = FontWeight.Bold), color = Ink)
        }
    }
}

/**
 * Shunya: the period as a hairline of day ticks with today marked, and spend drawn on the same
 * axis. Spend short of the "today" mark reads as under pace without a word of copy.
 */
@Composable
fun MonthLine(elapsed: Float, spentPercent: Float, modifier: Modifier = Modifier) {
    if (FinioTheme.visual != VisualTheme.Shunya) return
    val colors = FinioTheme.colors
    val e = elapsed.coerceIn(0f, 1f)
    val under = spentPercent <= e * 100f
    Column(modifier.fillMaxWidth().clearAndSetSemantics { }) {
        BoxWithConstraints(Modifier.fillMaxWidth().height(46.dp)) {
            Text(
                "Today",
                Modifier.offset(x = maxWidth * e + 6.dp, y = 0.dp),
                style = FinioType.caption,
                color = colors.primary,
            )
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width
                val top = 22.dp.toPx()
                val tickBase = top + 8.dp.toPx()
                for (i in 0..30) {
                    val x = w * i / 30f
                    val key = i == 0 || i == 14 || i == 30
                    val h = (if (key) 8 else 4).dp.toPx()
                    drawLine(
                        colors.mutedForeground.copy(alpha = if (key) 1f else 0.6f),
                        Offset(x, tickBase - h), Offset(x, tickBase), 1.dp.toPx(),
                    )
                }
                val lineY = tickBase + 8.dp.toPx()
                drawLine(colors.border, Offset(0f, lineY), Offset(w, lineY), 1.dp.toPx())
                drawLine(
                    colors.foreground, Offset(0f, lineY), Offset(w * (spentPercent / 100f).coerceIn(0f, 1f), lineY),
                    3.dp.toPx(),
                )
                val nx = w * e
                drawLine(colors.primary, Offset(nx, 0f), Offset(nx, lineY), 1.dp.toPx())
                drawCircle(colors.primary, 3.5.dp.toPx(), Offset(nx, lineY))
            }
        }
        Text(
            if (under) "Under pace" else "Ahead of pace",
            Modifier.fillMaxWidth().padding(top = 4.dp),
            style = FinioType.caption,
            color = if (under) colors.positive else colors.warning,
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
        )
    }
}

/** Bagh: the sun rides an arc across the hero; where it sits is how far through the month you are. */
@Composable
fun SunArc(elapsed: Float, modifier: Modifier = Modifier) {
    if (FinioTheme.visual != VisualTheme.Bagh) return
    val colors = FinioTheme.colors
    val t = elapsed.coerceIn(0f, 1f)
    Canvas(modifier.fillMaxWidth().aspectRatio(342f / 92f).clearAndSetSemantics { }) {
        val s = size.width / 342f
        // Quadratic bezier from (10,86) via (171,-36) to (332,86).
        val x = ((1 - t) * (1 - t) * 10 + 2 * (1 - t) * t * 171 + t * t * 332) * s
        val y = ((1 - t) * (1 - t) * 86 + 2 * (1 - t) * t * -36 + t * t * 86) * s
        val arc = Path().apply {
            moveTo(10 * s, 86 * s)
            quadraticTo(171 * s, -36 * s, 332 * s, 86 * s)
        }
        drawPath(
            arc, colors.foreground.copy(alpha = 0.28f),
            style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f * s, 6f * s))),
        )
        drawCircle(BaghScene.sun.copy(alpha = 0.28f), 22 * s, Offset(x, y))
        drawCircle(BaghScene.sun, 11.5f * s, Offset(x, y))
    }
}

private val leafA = PathParser().parsePathString("M24 29c-8 0-11-6-10-10 7-1 10 3 10 10z").toPath()
private val leafB = PathParser().parsePathString("M24 25c7 0 10-5 9-9-6-1-9 3-9 9z").toPath()
private val leafC = PathParser().parsePathString("M24 34c-9 0-12-6-11-10 8-1 11 3 11 10z").toPath()
private val leafD = PathParser().parsePathString("M24 28c8 0 11-5 10-9-7-1-10 3-10 9z").toPath()
private val leafE = PathParser().parsePathString("M24 20c-6 0-8-5-7-8 6-1 8 3 7 8z").toPath()
private val leafF = PathParser().parsePathString("M24 18c6 0 8-4 7-7-5-1-8 2-7 7z").toPath()

/** Bagh: a goal drawn as a plant — seed, sprout, sapling, tree as it fills. */
@Composable
fun GoalPlant(percent: Float, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    if (FinioTheme.visual != VisualTheme.Bagh) return
    val dark = FinioTheme.colors.isDark
    val soil = if (dark) Color(0xFF5B4330) else Color(0xFF8A6446)
    val seed = if (dark) Color(0xFFFFC83D) else Color(0xFFF4B21B)
    val stem = if (dark) Color(0xFF5FAE5A) else Color(0xFF3F7D3A)
    val leaf = if (dark) Color(0xFF7FCF7A) else Color(0xFF4F9A4A)
    val leaf2 = if (dark) Color(0xFF9BE08F) else Color(0xFF78B45F)
    val stage = if (percent >= 100f) 3 else if (percent >= 50f) 2 else if (percent > 0f) 1 else 0
    Canvas(modifier.size(size).clearAndSetSemantics { }) {
        drawScale(this.size.width / 48f, Offset.Zero) { plant(stage, soil, seed, stem, leaf, leaf2) }
    }
}

private fun DrawScope.plant(stage: Int, soil: Color, seed: Color, stem: Color, leaf: Color, leaf2: Color) {
    drawOval(soil, Offset(9f, 37.5f), Size(30f, 9f))
    fun stemLine(to: Float, w: Float) = drawLine(stem, Offset(24f, 40f), Offset(24f, to), w, StrokeCap.Round)
    when (stage) {
        0 -> {
            drawOval(seed, Offset(20.4f, 36.6f), Size(7.2f, 4.8f))
            drawLine(stem, Offset(24f, 36.4f), Offset(24f, 34.8f), 1.6f, StrokeCap.Round)
        }
        1 -> { stemLine(25f, 3f); drawPath(leafA, leaf); drawPath(leafB, leaf2) }
        2 -> {
            stemLine(18f, 3f)
            drawPath(leafC, leaf); drawPath(leafD, leaf2); drawPath(leafE, leaf); drawPath(leafF, leaf2)
        }
        else -> {
            drawLine(stem, Offset(24f, 41f), Offset(24f, 24f), 4f, StrokeCap.Round)
            drawCircle(leaf, 11f, Offset(24f, 18f))
            drawCircle(leaf2, 7f, Offset(16f, 22f))
            drawCircle(leaf2, 7f, Offset(32f, 22f))
            drawCircle(seed, 2f, Offset(20f, 14f))
            drawCircle(seed, 2f, Offset(29f, 19f))
        }
    }
}

/**
 * The hero figure, set the way the active theme sets it: Khata puts it on a ledger rule with a
 * paise column behind a red divider; Shunya and Gulal run it much larger; the rest use [style] as is.
 * [whole] says the figure is whole rupees (so ".00" can be shown without inventing anything).
 */
@Composable
fun HeroFigure(
    text: String,
    style: TextStyle,
    color: Color,
    whole: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = FinioTheme.colors
    val visual = FinioTheme.visual
    val kit = com.slowatcoding.finio.ui.theme.kits.kitFor(visual)
    if (kit != null && kit.HeroFigure(text, style, color, whole, modifier)) return
    val scale = when (visual) {
        VisualTheme.Shunya -> 1.75f
        VisualTheme.Gulal -> 1.6f
        VisualTheme.Bagh -> 1.3f
        else -> kit?.heroFigureScale ?: 1f
    }
    val sized = if (scale == 1f) style else style.copy(fontSize = style.fontSize * scale, lineHeight = style.lineHeight * scale)
    if (visual == VisualTheme.Khata) {
        val ink = colors.foreground
        Row(
            modifier
                .drawBehind {
                    drawLine(ink, Offset(0f, 0.5.dp.toPx()), Offset(size.width, 0.5.dp.toPx()), 1.dp.toPx())
                    drawLine(ink, Offset(0f, size.height - 3.5.dp.toPx()), Offset(size.width, size.height - 3.5.dp.toPx()), 1.dp.toPx())
                    drawLine(ink, Offset(0f, size.height - 0.5.dp.toPx()), Offset(size.width, size.height - 0.5.dp.toPx()), 1.dp.toPx())
                }
                .padding(top = 2.dp, bottom = 4.dp, end = 6.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(text, style = sized, color = color, maxLines = 1, softWrap = false)
            if (whole) {
                val paiseStyle = style.copy(fontSize = style.fontSize * 0.42f, lineHeight = style.fontSize * 0.5f, fontWeight = FontWeight.Normal)
                Text(
                    "00",
                    Modifier
                        .padding(start = 6.dp, bottom = 6.dp)
                        .drawBehind { drawLine(Color(0xFFB8322A).let { if (colors.isDark) Color(0xFFFF7B6C) else it }, Offset(0f, 0f), Offset(0f, size.height), 1.5.dp.toPx()) }
                        .padding(start = 6.dp),
                    style = paiseStyle,
                    color = colors.mutedForeground,
                    maxLines = 1,
                )
            }
        }
    } else {
        Text(text, modifier, style = sized, color = color, maxLines = 1, softWrap = false)
    }
}

/** A progress track's height in this theme (Mudra 8dp). */
@Composable
fun progressTrackHeight(): Dp = when (FinioTheme.visual) {
    VisualTheme.Gulal -> 18.dp
    VisualTheme.Khata, VisualTheme.Bagh -> 14.dp
    VisualTheme.Shunya -> 3.dp
    VisualTheme.Mudra -> 8.dp
    else -> com.slowatcoding.finio.ui.theme.kits.kitFor(FinioTheme.visual)?.progressTrackHeight ?: 8.dp
}
