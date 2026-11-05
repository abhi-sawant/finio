package com.slowatcoding.finio.ui.theme.kits

import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.slowatcoding.finio.R
import com.slowatcoding.finio.ui.theme.EaseOutExpo
import com.slowatcoding.finio.ui.theme.FinioTheme
import com.slowatcoding.finio.ui.theme.rememberReducedMotion
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI

// The prototype's proscenium lives in a 350-unit-wide box (design-prototypes/themes/src/mahal.mjs).
// The scene canvas is that box, [SCENE_UNITS] tall; the shoulders step out to the card's sides at
// y = 176, so the hero's text starts below them at full width.
private const val BOX = 350f
private const val SCENE_UNITS = 176f
private const val CX = 175f
private const val CY = 140f
private const val R0 = 54f
private const val R1 = 92f
private const val R2 = 84f
private const val RAYS = 25
private const val MAX_SCENE_DP = 340f

/** True while a sunburst is on screen, so the container knows to draw the arch above it. */
private val sceneShown = mutableStateOf(false)

/**
 * The hero surface (web `.note-card` under Mahal): with a sunburst, an arched proscenium on
 * stepped shoulders — gold outline, a keyline six units inside — whose arch sits exactly around
 * the scene canvas; without one, a stepped-notch panel with an inset keyline.
 */
@Composable
internal fun mahalProscenium(): Modifier {
    val colors = FinioTheme.colors
    val dark = colors.isDark
    val gold = MahalInk.gold(dark)
    val gline = MahalInk.gline(dark)
    val hair = MahalInk.hair(dark)
    val wide = LocalConfiguration.current.screenWidthDp >= 1024
    val side = if (wide) HeroSideWide else HeroSide
    return Modifier.drawBehind {
        val e = 0.6.dp.toPx()
        if (sceneShown.value) {
            val w = minOf(size.width - 2 * side.toPx(), MAX_SCENE_DP.dp.toPx())
            val s = w / BOX
            val ox = (size.width - w) / 2f
            val oy = HeroTop.toPx()
            val outer = proscenium(size.width, size.height, ox, oy, s, d = 0f, edge = e)
            drawPath(outer, colors.card)
            drawPath(outer, gold, style = Stroke(1.25.dp.toPx()))
            drawPath(proscenium(size.width, size.height, ox, oy, s, d = 6f, edge = 6.dp.toPx()), gline, style = Stroke(1.dp.toPx()))
        } else {
            val n = 7.dp.toPx()
            val panel = notch(size.width, size.height, n)
            drawPath(panel, colors.card)
            drawPath(panel, gline, style = Stroke(1.dp.toPx()))
            val k = 9.dp.toPx()
            drawRect(hair, Offset(k, k), androidx.compose.ui.geometry.Size(size.width - 2 * k, size.height - 2 * k), style = Stroke(1.dp.toPx()))
        }
    }
}

/** The proscenium outline, [d] units inside the outer one; card edges [edge] px in. */
private fun proscenium(w: Float, h: Float, ox: Float, oy: Float, s: Float, d: Float, edge: Float): Path {
    fun x(u: Float) = ox + u * s
    fun y(u: Float) = oy + u * s
    val ledge = y(176f + d)
    return Path().apply {
        moveTo(edge, h - edge)
        lineTo(edge, ledge)
        lineTo(x(12f + d), ledge); lineTo(x(12f + d), y(164f + d))
        lineTo(x(26f + d), y(164f + d)); lineTo(x(26f + d), y(152f + d))
        lineTo(x(47f + d), y(152f + d)); lineTo(x(47f + d), y(CY))
        val r = (128f - d) * s
        arcTo(Rect(Offset(x(CX), y(CY)), r), 180f, 180f, false)
        lineTo(x(303f - d), y(152f + d))
        lineTo(x(324f - d), y(152f + d)); lineTo(x(324f - d), y(164f + d))
        lineTo(x(338f - d), y(164f + d)); lineTo(x(338f - d), ledge)
        lineTo(w - edge, ledge)
        lineTo(w - edge, h - edge)
        close()
    }
}

/** A rectangle with stepped (square-notched) corners of [n] px — web `--mahal-notch`. */
private fun notch(w: Float, h: Float, n: Float) = Path().apply {
    val i = 0.5f
    moveTo(n, i); lineTo(w - n, i); lineTo(w - n, n); lineTo(w - i, n)
    lineTo(w - i, h - n); lineTo(w - n, h - n); lineTo(w - n, h - i); lineTo(n, h - i)
    lineTo(n, h - n); lineTo(i, h - n); lineTo(i, n); lineTo(n, n); close()
}

/**
 * The sunburst gauge: a half-fan of gilt rays, lit left to right for the share of the budget
 * spent, a needle at today's place in the period, "Now showing · <month>" lettered around the
 * arch. Vermilion once the budget is over or 90% gone. Fans open once (reduced motion: no).
 */
@Composable
internal fun MahalSunburst(elapsed: Float, spentPercent: Float, isOver: Boolean) {
    DisposableEffect(Unit) {
        sceneShown.value = true
        onDispose { sceneShown.value = false }
    }
    val colors = FinioTheme.colors
    val dark = colors.isDark
    val gold = MahalInk.gold(dark)
    val goldText = MahalInk.goldText(dark)
    val unlit = MahalInk.unlit(dark)
    val gline = MahalInk.gline(dark)
    val hair = MahalInk.hair(dark)
    val needleInk = colors.foreground
    val hot = isOver || spentPercent >= 90f
    val lit = if (hot) colors.destructive else gold
    val pct = spentPercent.coerceIn(0f, 100f).takeIf { !it.isNaN() } ?: 0f
    val pace = elapsed.coerceIn(0f, 1f).takeIf { !it.isNaN() } ?: 0f

    val reduced = rememberReducedMotion()
    val total = 1750f
    val clock = remember { Animatable(if (reduced) total else 0f) }
    LaunchedEffect(reduced) {
        if (!reduced) clock.animateTo(total, tween(total.toInt(), easing = LinearEasing))
    }

    val context = LocalContext.current
    val jost = remember(context) { context.resources.getFont(R.font.mahal_jost) }
    val limelight = remember(context) { context.resources.getFont(R.font.mahal_limelight) }
    val marquee = remember { "NOW SHOWING · " + SimpleDateFormat("MMMM yyyy", Locale.ENGLISH).format(Date()).uppercase() }
    val hub = "${Math.round(spentPercent.coerceIn(0f, 999f))}%"

    BoxWithConstraints {
        val w = min(maxWidth, MAX_SCENE_DP.dp)
        Canvas(Modifier.width(w).height(w * (SCENE_UNITS / BOX))) {
            val s = size.width / BOX
            val c = Offset(CX * s, CY * s)
            val t = clock.value

            // the hairline arch and the base rule
            drawArc(hair, 180f, 180f, false, Offset((CX - 98f) * s, (CY - 98f) * s), androidx.compose.ui.geometry.Size(196f * s, 196f * s), style = Stroke(1.dp.toPx()))
            drawLine(gline, Offset(53f * s, 140.5f * s), Offset(297f * s, 140.5f * s), 1.dp.toPx())

            // rays
            for (i in 0 until RAYS) {
                val a = i * 180f / (RAYS - 1)
                val on = pct > 0f && a <= pct * 1.8f + 0.01f
                val local = EaseOutExpo.transform(((t - i * 22f) / 1100f).coerceIn(0f, 1f))
                val r = if (i % 2 == 1) R2 else R1
                rotate(a * local, pivot = c) {
                    drawLine(
                        (if (on) lit else unlit).copy(alpha = (if (on) lit else unlit).alpha * local),
                        Offset(c.x - R0 * s, c.y), Offset(c.x - r * s, c.y),
                        1.6f * s, StrokeCap.Round,
                    )
                }
            }

            // the pace needle
            val nl = EaseOutExpo.transform(((t - 450f) / 1300f).coerceIn(0f, 1f))
            rotate(pace * 180f * nl, pivot = c) {
                val ink = needleInk.copy(alpha = nl)
                val tip = c.x - (R1 + 7f) * s
                drawLine(ink, Offset(c.x - (R0 - 4f) * s, c.y), Offset(tip, c.y), 1.25.dp.toPx(), StrokeCap.Round)
                drawPath(
                    Path().apply {
                        moveTo(tip, c.y); lineTo(tip + 3.5f * s, c.y - 2.6f * s); lineTo(tip + 7f * s, c.y); lineTo(tip + 3.5f * s, c.y + 2.6f * s); close()
                    },
                    ink,
                )
            }

            drawLettering(marquee, hub, s, c, goldText, needleInk, jost, limelight)
        }
    }
}

private fun DrawScope.drawLettering(
    marquee: String, hub: String, s: Float, c: Offset, goldText: Color, ink: Color,
    jost: android.graphics.Typeface, limelight: android.graphics.Typeface,
) {
    val canvas = drawContext.canvas.nativeCanvas
    val marq = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = jost
        fontVariationSettings = "'wght' 500"
        textSize = 10.5f * s
        letterSpacing = 0.24f
        color = goldText.toArgb()
        textAlign = Paint.Align.CENTER
    }
    val r = 110f * s
    val arc = android.graphics.Path().apply { addArc(RectF(c.x - r, c.y - r, c.x + r, c.y + r), 180f, 180f) }
    canvas.drawTextOnPath(marquee, arc, (PI * r / 2.0).toFloat(), 0f, marq)

    val hubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = limelight
        textSize = 27f * s
        color = ink.toArgb()
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText(hub, c.x, 131f * s, hubPaint)
}

