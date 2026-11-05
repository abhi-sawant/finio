package com.slowatcoding.finio.ui.theme.kits

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.slowatcoding.finio.ui.navigation.FinioTab
import com.slowatcoding.finio.ui.shell.tabClickable
import com.slowatcoding.finio.ui.theme.FinioShapeScale
import com.slowatcoding.finio.ui.theme.FinioTheme
import com.slowatcoding.finio.ui.theme.FinioTypeScale
import com.slowatcoding.finio.ui.theme.ThemePalette
import com.slowatcoding.finio.ui.theme.VisualTheme
import com.slowatcoding.finio.ui.theme.rememberReducedMotion
import java.util.Calendar
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * Parchi — the thermal receipt (web/src/themes/parchi.css). Martian Mono on its width axis, square
 * everything, one receipt red; the hero is a torn slip that feeds out of a printer slot once per
 * process, prints its total double-height and ends in a barcode and a stamp line. The tab bar is a
 * perforated paper strip with the current tab printed inverse and the red print key docked in it.
 */
object ParchiKit : ThemeKit {
    override val visual = VisualTheme.Parchi
    override fun palette(dark: Boolean): ThemePalette = if (dark) ParchiDark else ParchiLight

    override val type: FinioTypeScale = FinioTypeScale(
        heading = ParchiWide, money = ParchiWide, sansFamily = ParchiMono,
        displayWeight = 800, pageTitleWeight = 700, screenTitleWeight = 700, headlineWeight = 700,
        moneyWeight = 700, dialogTitleWeight = 700, titleWeight = 700,
    )
    override val shapes: FinioShapeScale = FinioShapeScale(base = 0.dp, full = RectangleShape)

    // ── the red print key ──
    override fun coinInk(dark: Boolean) = ParchiInks.of(dark).onRed
    override val coinShape: Shape = RectangleShape

    /** An inner paper ring: the key's printed bezel (web: a counter ring and a red outline). */
    @Composable
    override fun coinDecoration(shape: Shape): Modifier {
        val ink = ParchiInks.of(FinioTheme.colors.isDark)
        return Modifier.drawBehind {
            val inset = 4.dp.toPx()
            val w = 1.5.dp.toPx()
            drawRect(
                ink.onRed.copy(alpha = 0.7f),
                topLeft = Offset(inset, inset),
                size = Size(size.width - inset * 2, size.height - inset * 2),
                style = Stroke(w),
            )
        }
    }

    /** Docked into the strip: centred on the tab bar's ~68dp height. */
    override val fabBottom: Dp = 8.dp

    // ── progress: a dot-matrix meter ──
    override val progressTrackHeight: Dp = 10.dp

    @Composable
    override fun progressDecoration(): Modifier {
        val ink = ParchiInks.of(FinioTheme.colors.isDark)
        return Modifier.drawBehind {
            drawRect(ink.paper)
            val on = 4.dp.toPx()
            val period = 6.dp.toPx()
            var x = 0f
            while (x < size.width) {
                drawRect(ink.faint, Offset(x, 0f), Size(minOf(on, size.width - x), size.height))
                x += period
            }
        }
    }

    // ── the hero: a torn slip feeding out of the slot ──
    @Composable
    override fun heroContainer(shape: Shape): Modifier {
        val ink = ParchiInks.of(FinioTheme.colors.isDark)
        val reduced = rememberReducedMotion()
        val feed = remember { Animatable(if (printed || reduced) 1f else 0f) }
        LaunchedEffect(Unit) {
            if (!printed) {
                printed = true
                if (!reduced) feed.animateTo(1f, tween(1200, delayMillis = 150, easing = Steps12))
            }
        }
        val torn = TornSlipShape()
        return Modifier
            // The printer's mouth, drawn over the slip as it emerges.
            .drawWithContent {
                drawContent()
                drawRect(ink.slot, Offset(-6.dp.toPx(), 0f), Size(size.width + 12.dp.toPx(), 6.dp.toPx()))
            }
            .padding(top = 4.dp)
            .clipToBounds()
            .graphicsLayer { translationY = -(1f - feed.value) * size.height }
            .clip(torn)
            .background(ink.paper)
            .drawBehind {
                // Faint thermal banding.
                val step = 4.dp.toPx()
                var x = 3.dp.toPx()
                while (x < size.width) {
                    drawRect(ink.band, Offset(x, 0f), Size(1.dp.toPx(), size.height))
                    x += step
                }
            }
    }

    override fun heroPadding(wide: Boolean): PaddingValues =
        if (wide) PaddingValues(24.dp, 26.dp, 24.dp, 20.dp) else PaddingValues(16.dp, 20.dp, 16.dp, 16.dp)

    /** The total prints double-height (scaleY 1.65), wide and heavy, like a thermal head. */
    @Composable
    override fun HeroFigure(text: String, style: TextStyle, color: Color, whole: Boolean, modifier: Modifier): Boolean {
        val printed = style.copy(fontFamily = ParchiWide, fontWeight = FontWeight(800), letterSpacing = (-0.02).em, lineHeight = style.fontSize)
        Text(
            text,
            modifier
                .layout { measurable, constraints ->
                    val p = measurable.measure(constraints)
                    val h = (p.height * 1.65f).roundToInt()
                    layout(p.width, h) { p.placeRelative(0, 0) }
                }
                .graphicsLayer {
                    scaleY = 1.65f
                    transformOrigin = TransformOrigin(0f, 0f)
                },
            style = printed,
            color = color,
            maxLines = 1,
            softWrap = false,
        )
        return true
    }

    /** The receipt's foot: a dashed rule, a deterministic barcode and the printed stamp. */
    @Composable
    override fun HeroFooter(elapsed: Float, spentPercent: Float, isOver: Boolean, daysLeft: Int) {
        val colors = FinioTheme.colors
        val ink = ParchiInks.of(colors.isDark)
        val stamp = remember(elapsed, daysLeft) { Stamp.of(elapsed, daysLeft) }
        val bars = remember(stamp.seed) { barcode(stamp.seed) }
        Column(Modifier.fillMaxWidth().padding(top = 10.dp).clearAndSetSemantics { }) {
            DashedRule(ink.rule)
            Row(
                Modifier.fillMaxWidth().padding(top = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Canvas(Modifier.width(150.dp).height(30.dp)) {
                    val unit = size.width / bars.second
                    bars.first.forEach { (x, w) -> drawRect(ink.ink, Offset(x * unit, 0f), Size(w * unit, size.height)) }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(stamp.date, style = condensed(11, 700), color = colors.foreground)
                    Text(stamp.line, style = condensed(10, 500), color = colors.mutedForeground)
                }
            }
        }
    }

    // ── chrome: a perforated paper strip ──
    @Composable
    override fun TabBar(active: FinioTab?, onSelect: (FinioTab) -> Unit, modifier: Modifier) {
        val colors = FinioTheme.colors
        val ink = ParchiInks.of(colors.isDark)
        Row(
            modifier
                .fillMaxWidth()
                .background(ink.paper)
                .drawBehind {
                    val w = 1.5.dp.toPx()
                    drawLine(
                        ink.rule, Offset(0f, w / 2), Offset(size.width, w / 2), w,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx())),
                    )
                    // The perforation: a row of punched holes showing the counter.
                    val r = 1.8.dp.toPx()
                    val y = 7.dp.toPx()
                    var x = 5.dp.toPx()
                    while (x < size.width) {
                        drawCircle(ink.counter, r, Offset(x, y))
                        x += 10.dp.toPx()
                    }
                }
                .windowInsetsPadding(WindowInsets.navigationBars)
                // Room on the right for the docked print key.
                .padding(start = 6.dp, end = 80.dp, top = 14.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FinioTab.entries.forEach { tab ->
                val selected = tab == active
                val tint = if (selected) ink.paper else colors.mutedForeground
                val icon = remember(tab, selected) { tab.icon(if (selected) 2.25f else 1.75f) }
                Column(
                    Modifier
                        .weight(1f)
                        .background(if (selected) ink.ink else Color.Transparent)
                        .tabClickable(selected) { onSelect(tab) }
                        .padding(horizontal = 2.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = tint)
                    Text(
                        tab.label.uppercase(),
                        style = condensed(10, if (selected) 700 else 500).copy(letterSpacing = 0.04.em),
                        color = tint,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/** Set once the hero has fed out — the slip prints once per process, not on every visit. */
private var printed = false

/** CSS `steps(12, jump-end)`. */
private val Steps12 = Easing { x -> floor(x * 12f) / 12f }

private fun condensed(size: Int, weight: Int) = TextStyle(
    fontFamily = ParchiCondensed,
    fontSize = size.sp,
    lineHeight = (size * 1.35f).sp,
    fontWeight = FontWeight(weight),
    letterSpacing = 0.05.em,
    fontFeatureSettings = "tnum",
)

@Composable
private fun DashedRule(color: Color) {
    Canvas(Modifier.fillMaxWidth().height(1.5.dp)) {
        drawLine(
            color, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), size.height,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx())),
        )
    }
}

/** A slip torn along a sawtooth at top and bottom: 10dp teeth, 5dp deep (web: the conic masks). */
private class TornSlipShape(private val period: Dp = 10.dp) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val p = with(density) { period.toPx() }
        val d = p / 2f
        val path = Path().apply {
            moveTo(0f, d)
            var x = 0f
            while (x < size.width) {
                lineTo(minOf(x + d, size.width), 0f)
                lineTo(minOf(x + p, size.width), d)
                x += p
            }
            lineTo(size.width, size.height - d)
            x = size.width
            while (x > 0f) {
                lineTo(maxOf(x - d, 0f), size.height)
                lineTo(maxOf(x - p, 0f), size.height - d)
                x -= p
            }
            close()
        }
        return Outline.Generic(path)
    }
}

/** Bar/space widths derived from a fixed string, guard bars at both ends — never random. */
private fun barcode(seed: String): Pair<List<Pair<Float, Float>>, Float> {
    val seq = mutableListOf(1, 1, 1, 1)
    for (ch in seed) {
        val c = ch.code
        seq += listOf((c % 3) + 1, ((c shr 2) % 2) + 1, ((c shr 3) % 3) + 1, ((c shr 1) % 2) + 1)
    }
    seq += listOf(1, 1, 1)
    var x = 0f
    val bars = mutableListOf<Pair<Float, Float>>()
    seq.forEachIndexed { i, w ->
        if (i % 2 == 0) bars += x to w.toFloat()
        x += w
    }
    return bars to x
}

/**
 * The printed stamp: today's date and which day of the budget period it is. Dashboard passes the
 * elapsed fraction (by the clock) and the calendar days left including today, so
 * `elapsed · total = (day − 1) + fractionOfToday` and `day = total − daysLeft + 1` (web: ParchiSlip).
 */
private class Stamp(val date: String, val line: String, val seed: String) {
    companion object {
        private val MONTHS = listOf("JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC")

        fun of(elapsed: Float, daysLeft: Int): Stamp {
            val now = Calendar.getInstance()
            val dayOfMonth = now.get(Calendar.DAY_OF_MONTH)
            val month = now.get(Calendar.MONTH)
            val year = now.get(Calendar.YEAR)
            val secondsToday = now.get(Calendar.HOUR_OF_DAY) * 3600 + now.get(Calendar.MINUTE) * 60 + now.get(Calendar.SECOND)
            val f = secondsToday / 86_400.0
            val rest = 1.0 - elapsed
            val total = if (rest > 1e-6) ((daysLeft - f) / rest).roundToInt() else -1
            val day = if (total >= daysLeft) total - daysLeft + 1 else null
            val line = if (day != null) "DAY $day OF $total" else "$daysLeft DAY${if (daysLeft == 1) "" else "S"} TO GO"
            val seed = String.format(java.util.Locale.ROOT, "FINIO*%02d%02d%02d*%d", dayOfMonth, month + 1, year % 100, day ?: daysLeft)
            return Stamp("$dayOfMonth ${MONTHS[month]} $year", line, seed)
        }
    }
}
