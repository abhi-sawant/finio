package com.slowatcoding.finio.ui.theme.kits

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.slowatcoding.finio.R
import com.slowatcoding.finio.ui.mudra.Denomination
import com.slowatcoding.finio.ui.mudra.NoteStyle
import com.slowatcoding.finio.ui.navigation.FinioTab
import com.slowatcoding.finio.ui.shell.tabClickable
import com.slowatcoding.finio.ui.theme.FinioBrushes
import com.slowatcoding.finio.ui.theme.FinioColors
import com.slowatcoding.finio.ui.theme.FinioShadows
import com.slowatcoding.finio.ui.theme.FinioShapeScale
import com.slowatcoding.finio.ui.theme.FinioSpec
import com.slowatcoding.finio.ui.theme.FinioTheme
import com.slowatcoding.finio.ui.theme.FinioTypeScale
import com.slowatcoding.finio.ui.theme.ThemePalette
import com.slowatcoding.finio.ui.theme.VisualTheme
import com.slowatcoding.finio.ui.theme.rememberReducedMotion
import com.slowatcoding.finio.ui.theme.rgba
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Kolam — the threshold drawing (web/src/themes/kolam.css). Stone-sand ground under a red-oxide
 * band in light, a drenched oxide floor with rice-white line and lamp-yellow in dark. One line
 * weight draws everything: dot rows replace rules and progress tracks, outlines are pills, keys
 * and tabs are circles. The signature is the month as a kolam ([HeroFooter]): one dot per day on
 * a snake, one woven line looped around the days gone, today ringed in lamp-yellow.
 */
object KolamKit : ThemeKit {
    override val visual = VisualTheme.Kolam
    override fun palette(dark: Boolean): ThemePalette = if (dark) Dark else Light
    override val type: FinioTypeScale get() = Scale
    override val shapes: FinioShapeScale get() = Shapes
    override fun coinInk(dark: Boolean) = c(if (dark) 0x651E14 else 0xF8F0E4)

    /** The FAB sits in a gap ring and a 1.5dp outline 5dp out (web: box-shadow + outline-offset). */
    @Composable
    override fun coinDecoration(shape: Shape): Modifier {
        val colors = FinioTheme.colors
        val bg = colors.background
        val outline = if (colors.isDark) c(0xF8F0E4) else colors.foreground
        return Modifier.drawBehind {
            val r = size.minDimension / 2f
            drawCircle(bg, radius = r + 2.dp.toPx(), style = Stroke(width = 4.dp.toPx()))
            drawCircle(outline, radius = r + 5.75.dp.toPx(), style = Stroke(width = 1.5.dp.toPx()))
        }
    }

    override val progressTrackHeight: Dp get() = 12.dp

    /** The track is a row of small dots; the fill is masked to a row of larger dots (9dp pitch). */
    @Composable
    override fun progressDecoration(): Modifier {
        val dot = kolDot(FinioTheme.colors.isDark)
        return Modifier.drawWithContent {
            val pitch = 9.dp.toPx()
            val cy = size.height / 2f
            val count = (size.width / pitch).toInt() + 1
            val small = 1.3.dp.toPx()
            for (i in 0 until count) drawCircle(dot, small, Offset(pitch / 2f + i * pitch, cy))
            val mask = Path().apply {
                fillType = PathFillType.EvenOdd
                addRect(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height))
                for (i in 0 until count) {
                    addOval(
                        androidx.compose.ui.geometry.Rect(
                            Offset(pitch / 2f + i * pitch, cy), 2.6.dp.toPx(),
                        ),
                    )
                }
            }
            drawIntoCanvas { canvas ->
                canvas.saveLayer(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height), Paint())
                drawContent()
                canvas.drawPath(mask, Paint().apply { blendMode = BlendMode.DstOut; color = Color.Black })
                canvas.restore()
            }
        }
    }

    // ── hero: no card, a light figure and the month kolam ──
    override fun heroPadding(wide: Boolean) = PaddingValues(0.dp, 8.dp, 0.dp, 4.dp)
    override val replacesHeroBar: Boolean get() = true

    @Composable
    override fun HeroFigure(text: String, style: TextStyle, color: Color, whole: Boolean, modifier: Modifier): Boolean {
        Text(
            text,
            modifier,
            style = style.copy(fontSize = 60.sp, lineHeight = 60.sp, fontWeight = FontWeight(300), letterSpacing = (-0.025).em),
            color = color,
            maxLines = 1,
            softWrap = false,
        )
        return true
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    override fun HeroFooter(elapsed: Float, spentPercent: Float, isOver: Boolean, daysLeft: Int) {
        val colors = FinioTheme.colors
        val dark = colors.isDark
        val period = LocalHeroPeriod.current
        val (day, total) = remember(elapsed, daysLeft, period) {
            if (period != null) {
                val t = period.totalDays.coerceAtLeast(1)
                period.dayOfPeriod.coerceIn(1, t) to t
            } else {
                periodDay(elapsed, daysLeft)
            }
        }
        val d = remember(total, day) { draw(total, day) }
        val reduced = rememberReducedMotion()
        val progress = remember(d) { Animatable(if (reduced) 1f else 0f) }
        val ring = remember(d) { Animatable(if (reduced) 1f else 0f) }
        LaunchedEffect(d, reduced) {
            if (!reduced) {
                progress.snapTo(0f); ring.snapTo(0f)
                progress.animateTo(1f, tween(1200, easing = DrawEase))
            }
        }
        LaunchedEffect(d, reduced) {
            if (!reduced) ring.animateTo(1f, tween(300, delayMillis = 1100, easing = DrawEase))
        }
        val line = if (dark) c(0xF8F0E4) else c(0x8F2D1C)
        val lamp = lampColor(dark)
        val bare = if (dark) c(0xD9A999) else c(0x7D6C5E)
        val ink = colors.foreground
        val dayLabel = "Day $day of $total"

        @Composable
        fun KolamCanvas(modifier: Modifier) {
            Canvas(modifier) {
                val s = size.width / d.w
                scale(s, s, pivot = Offset.Zero) {
                    drawKolam(d, progress.value, ring.value, line, lamp, bare, ink)
                }
            }
        }

        @Composable
        fun Key(rowMode: Boolean) {
            val keyText = TextStyle(fontFamily = Kumbh, fontSize = 13.sp, fontWeight = FontWeight(400), fontFeatureSettings = "tnum")
            val items: @Composable () -> Unit = {
                Text(
                    dayLabel,
                    style = TextStyle(fontFamily = Kumbh, fontSize = 16.sp, fontWeight = FontWeight(600), fontFeatureSettings = "tnum"),
                    color = colors.foreground, maxLines = 1, softWrap = false,
                )
                KeyItem("Gone", KeyKind.Gone, keyText, line, lamp, bare, ink, colors.mutedForeground)
                KeyItem("Today", KeyKind.Today, keyText, line, lamp, bare, ink, colors.mutedForeground)
                KeyItem("To come", KeyKind.Come, keyText, line, lamp, bare, ink, colors.mutedForeground)
            }
            if (rowMode) {
                FlowRow(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    itemVerticalAlignment = Alignment.CenterVertically,
                ) { items() }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.8.dp)) { items() }
            }
        }

        // aria-hidden on the web: the figure and budget copy already say it.
        Column(Modifier.padding(top = 8.dp).clearAndSetSemantics { }) {
            if (d.row) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    KolamCanvas(Modifier.fillMaxWidth().aspectRatio(d.w / d.h))
                    Key(true)
                }
            } else {
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val gridW = min(216f, maxWidth.value * 0.58f).dp
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        KolamCanvas(Modifier.width(gridW).aspectRatio(d.w / d.h))
                        Key(false)
                    }
                }
            }
        }
    }

    // ── chrome: dot-row top edge, circle icons, the current page ringed ──
    @Composable
    override fun TabBar(active: FinioTab?, onSelect: (FinioTab) -> Unit, modifier: Modifier) {
        val colors = FinioTheme.colors
        val dark = colors.isDark
        val dot = kolDot(dark)
        val sel = if (dark) c(0xF4C24F) else c(0x8F2D1C)
        Row(
            modifier
                .fillMaxWidth()
                .background(colors.background)
                .drawBehind {
                    val pitch = 10.dp.toPx()
                    val n = (size.width / pitch).toInt() + 1
                    for (i in 0 until n) drawCircle(dot, 1.1.dp.toPx(), Offset(pitch / 2f + i * pitch, 1.5.dp.toPx()))
                }
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FinioTab.entries.forEach { tab ->
                val selected = tab == active
                val icon = remember(tab) { tab.icon(1.6f) }
                Column(
                    Modifier
                        .weight(1f)
                        .tabClickable(selected) { onSelect(tab) }
                        .padding(top = 10.dp, bottom = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Icon(
                        icon, contentDescription = null,
                        modifier = Modifier
                            .size(20.dp + 6.4.dp * 2 + 3.5.dp)
                            .drawBehind {
                                val w = 1.75.dp.toPx()
                                drawCircle(
                                    if (selected) sel else Color.Transparent,
                                    radius = size.minDimension / 2f - w / 2f,
                                    style = Stroke(width = w),
                                )
                            }
                            .padding(1.75.dp + 6.4.dp),
                        tint = if (selected) colors.foreground else colors.mutedForeground,
                    )
                    Text(
                        tab.label,
                        style = TabStyle.copy(fontWeight = FontWeight(if (selected) 700 else 500)),
                        color = if (selected) colors.foreground else colors.mutedForeground,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

// ───────────────────────────── the month kolam ─────────────────────────────

private class Pt(val x: Float, val y: Float) {
    operator fun plus(o: Pt) = Pt(x + o.x, y + o.y)
    operator fun minus(o: Pt) = Pt(x - o.x, y - o.y)
    operator fun times(k: Float) = Pt(x * k, y * k)
}

/**
 * Port of `weave()` in web/src/components/ui/extras/kolam.tsx: one closed line woven around a
 * route of dots (alternate sides going out, the opposite coming back, crossing midway), as
 * Catmull-Rom curves. Starts beside dot [from] when given, else beside the last dot.
 */
private fun weave(pts: List<Pt>, a: Float, from: Int = -1): Path {
    val path = Path()
    val n = pts.size
    if (n == 0) return path
    val t = pts.indices.map { i ->
        val p = pts[max(i - 1, 0)]
        val q = pts[min(i + 1, n - 1)]
        val dx = q.x - p.x
        val dy = q.y - p.y
        val l = hypot(dx, dy)
        if (l != 0f) Pt(dx / l, dy / l) else Pt(1f, 0f)
    }
    fun dir(i: Int, j: Int): Pt {
        val l = hypot(pts[j].x - pts[i].x, pts[j].y - pts[i].y).let { if (it == 0f) 1f else it }
        return Pt((pts[j].x - pts[i].x) / l, (pts[j].y - pts[i].y) / l)
    }
    fun off(i: Int, sg: Float): List<Pt> {
        val nrm = Pt(-t[i].y * sg, t[i].x * sg)
        val c = pts[i] + nrm * a
        if (i == 0 || i == n - 1) return listOf(c)
        val d1 = dir(i - 1, i)
        val d2 = dir(i, i + 1)
        if (abs(d1.x * d2.x + d1.y * d2.y) > 0.5f || nrm.x * (d2.x - d1.x) + nrm.y * (d2.y - d1.y) > 0f) return listOf(c)
        return listOf(pts[i] - d2 * a, c, pts[i] + d1 * a)
    }
    fun mid(i: Int) = Pt((pts[i].x + pts[i + 1].x) / 2f, (pts[i].y + pts[i + 1].y) / 2f)

    var p = ArrayList<Pt>()
    p.add(pts[n - 1] + t[n - 1] * a)
    for (i in n - 1 downTo 0) {
        p.addAll(off(i, if (i % 2 == 1) 1f else -1f).reversed())
        if (i > 0) p.add(mid(i - 1))
    }
    p.add(pts[0] - t[0] * a)
    for (i in 0 until n) {
        p.addAll(off(i, if (i % 2 == 1) -1f else 1f))
        if (i < n - 1) p.add(mid(i))
    }
    if (from in 0 until n) {
        val j = p.indexOfFirst { hypot(it.x - pts[from].x, it.y - pts[from].y) < a * 1.01f }
        if (j > 0) p = ArrayList(p.drop(j) + p.take(j))
    }
    val m = p.size
    fun g(i: Int) = p[((i % m) + m) % m]
    path.moveTo(p[0].x, p[0].y)
    for (i in 0 until m) {
        val p0 = g(i - 1); val p1 = g(i); val p2 = g(i + 1); val p3 = g(i + 2)
        path.cubicTo(
            p1.x + (p2.x - p0.x) / 6f, p1.y + (p2.y - p0.y) / 6f,
            p2.x - (p3.x - p1.x) / 6f, p2.y - (p3.y - p1.y) / 6f,
            p2.x, p2.y,
        )
    }
    path.close()
    return path
}

private const val COLS = 7
private const val S = 30f
private const val MAX_GRID_DAYS = 42

/** Everything in viewBox units (like the web's SVG). */
private class Drawing(
    val row: Boolean, val w: Float, val h: Float, val dots: List<Pt>, val path: Path?, val gone: Int,
    val todayIdx: Int, val ringR: Float, val dotR: Float, val stroke: Float,
)

/** Pure function of the day counts (web `draw()`). */
private fun draw(total: Int, day: Int): Drawing {
    val n = max(1, total)
    val gone = min(n, max(0, day))
    if (n > MAX_GRID_DAYS) {
        val cnt = 20
        val p = 16f
        val k = ((gone.toFloat() / n) * cnt).roundToInt()
        val dots = List(cnt) { i -> Pt(p / 2f + i * p, 10f) }
        return Drawing(
            true, cnt * p, 20f, dots, if (k > 0) weave(dots.take(k), 5.6f) else null, k,
            min(max(k - 1, 0), cnt - 1), 6f, 1.8f, 1.6f,
        )
    }
    val cols = min(n, COLS)
    val rows = (n + cols - 1) / cols
    val m = (S * 0.6f).roundToInt().toFloat()
    val dots = List(n) { i ->
        val r = i / cols
        val c = i % cols
        Pt(m + (if (r % 2 == 1) cols - 1 - c else c) * S, m + r * S)
    }
    val w = 2 * m + (cols - 1) * S
    val h = 2 * m + (rows - 1) * S
    val a = S * 0.31f
    return Drawing(
        false, w, h, dots, if (gone > 0) weave(dots.take(gone), a, gone - 1) else null, gone,
        min(max(gone - 1, 0), n - 1), a * 0.56f, min(3.4f, S * 0.075f), 1.75f,
    )
}

private fun DrawScope.drawKolam(
    d: Drawing, progress: Float, ring: Float, line: Color, lamp: Color, bare: Color, ink: Color,
) {
    d.path?.let { full ->
        val measure = PathMeasure().apply { setPath(full, false) }
        val seg = Path()
        measure.getSegment(0f, measure.length * progress.coerceIn(0f, 1f), seg, true)
        drawPath(seg, line, style = Stroke(width = d.stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
    d.dots.forEachIndexed { i, p ->
        if (i != d.todayIdx) drawCircle(if (i < d.gone) ink else bare, d.dotR, Offset(p.x, p.y))
    }
    val t = d.dots[d.todayIdx]
    if (ring > 0f) {
        drawCircle(
            lamp.copy(alpha = lamp.alpha * ring), d.ringR * (0.3f + 0.7f * ring), Offset(t.x, t.y),
            style = Stroke(width = d.stroke),
        )
    }
    drawCircle(lamp, d.dotR * 1.25f, Offset(t.x, t.y))
}

private enum class KeyKind { Gone, Today, Come }

@Composable
private fun KeyItem(
    label: String, kind: KeyKind, style: TextStyle,
    line: Color, lamp: Color, bare: Color, ink: Color, text: Color,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.4.dp), verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(14.dp)) {
            val ctr = Offset(size.width / 2f, size.height / 2f)
            val w = 1.5.dp.toPx()
            when (kind) {
                KeyKind.Gone -> {
                    drawCircle(line, size.minDimension / 2f - w / 2f, ctr, style = Stroke(width = w))
                    drawCircle(ink, 2.dp.toPx(), ctr)
                }
                KeyKind.Today -> {
                    drawCircle(lamp, size.minDimension / 2f - w / 2f, ctr, style = Stroke(width = w))
                    drawCircle(lamp, 2.2.dp.toPx(), ctr)
                }
                KeyKind.Come -> drawCircle(bare, 2.dp.toPx(), ctr)
            }
        }
        Text(label, style = style, color = text, maxLines = 1, softWrap = false)
    }
}

/**
 * Fallback when `LocalHeroPeriod` is null: day n of N from the hero's own `elapsed` and `daysLeft` (the period runs start-of-day to
 * end-of-day, so `elapsed × N = N − daysLeft + today's fraction`); clamped when it doesn't resolve.
 */
private fun periodDay(elapsed: Float, daysLeft: Int): Pair<Int, Int> {
    val now = Calendar.getInstance()
    val fraction = (now.get(Calendar.HOUR_OF_DAY) * 3600 + now.get(Calendar.MINUTE) * 60 + now.get(Calendar.SECOND)) / 86_400f
    val total = if (elapsed in 0f..0.9999f && daysLeft >= 1) {
        ((daysLeft - fraction) / (1f - elapsed)).roundToInt().coerceIn(1, 366)
    } else {
        max(daysLeft, 1)
    }
    val day = (total - daysLeft + 1).coerceIn(1, total)
    return day to total
}

private val DrawEase = CubicBezierEasing(0.2f, 0.7f, 0.2f, 1f)

// ───────────────────────────── type & shape ─────────────────────────────

private fun variable(res: Int, weight: Int) = Font(
    resId = res,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

/**
 * Kumbh Sans has no rupee sign (the web stacks Instrument Sans next); Compose can't fall back per
 * glyph inside one FontFamily, so the sign resolves through the system font.
 */
private val Kumbh = FontFamily(
    variable(R.font.kolam_kumbh_sans, 300), variable(R.font.kolam_kumbh_sans, 400),
    variable(R.font.kolam_kumbh_sans, 500), variable(R.font.kolam_kumbh_sans, 600),
    variable(R.font.kolam_kumbh_sans, 700),
)

private val Scale = FinioTypeScale(
    Kumbh, Kumbh, Kumbh,
    displayWeight = 300, displayTracking = -0.025f, pageTitleWeight = 600, screenTitleWeight = 600,
    headlineWeight = 600, moneyWeight = 600, dialogTitleWeight = 600, titleWeight = 600,
)
private val Shapes = FinioShapeScale(base = 16.dp)

private val TabStyle = TextStyle(fontFamily = Kumbh, fontSize = 10.5.sp)

private fun lampColor(dark: Boolean) = if (dark) c(0xF4C24F) else c(0xA05F00)
private fun kolDot(dark: Boolean) = if (dark) rgba(248, 240, 228, 0.5f) else rgba(42, 27, 21, 0.46f)

// ───────────────────────────── palettes ─────────────────────────────

private fun c(rgb: Long) = Color(0xFF000000 or rgb)
private fun solid(rgb: Long): Brush = SolidColor(c(rgb))

private fun colorsOf(
    dark: Boolean,
    background: Long, foreground: Long, popover: Long,
    primary: Long, primaryFg: Long, secondary: Long, mutedFg: Long,
    destructive: Long, destructiveFg: Long,
    positive: Long, warning: Long, border: Color, input: Color,
    charts: List<Long>,
    warningBandAccent: Long,
    glassBorder: Color, scrim: Color, sidebar: Long, sidebarPrimary: Long, sidebarPrimaryFg: Long, ring: Long,
) = FinioColors(
    isDark = dark,
    background = c(background), foreground = c(foreground),
    card = c(background), cardForeground = c(foreground),
    popover = c(popover), popoverForeground = c(foreground),
    primary = c(primary), primaryForeground = c(primaryFg),
    secondary = c(secondary), secondaryForeground = c(foreground),
    muted = c(secondary), mutedForeground = c(mutedFg),
    accent = c(secondary), accentForeground = c(foreground),
    destructive = c(destructive), destructiveForeground = c(destructiveFg),
    positive = c(positive), warning = c(warning),
    border = border, input = input, ring = c(ring),
    chart1 = c(charts[0]), chart2 = c(charts[1]), chart3 = c(charts[2]), chart4 = c(charts[3]), chart5 = c(charts[4]),
    sidebar = c(sidebar), sidebarForeground = c(0xF8F0E4),
    sidebarPrimary = c(sidebarPrimary), sidebarPrimaryForeground = c(sidebarPrimaryFg),
    sidebarAccent = Color.Transparent, sidebarAccentForeground = c(0xF8F0E4),
    sidebarBorder = Color.Transparent, sidebarRing = c(ring),
    // The alert is an outlined pill on the ground: its fill is the ground itself.
    warningBand = c(background), warningBandForeground = c(foreground), warningBandAccent = c(warningBandAccent),
    engraving = Color.Transparent,
    glass = Color.Transparent, glassStrong = c(background), glassBorder = glassBorder, glassHighlight = Color.Transparent,
    shadowTint = if (dark) Color(0, 0, 0) else Color(42, 27, 21), scrim = scrim,
)

private fun shadowsOf(tint: Color) = com.slowatcoding.finio.ui.theme.stock(tint).let { s ->
    FinioShadows(
        card = emptyList(), float = emptyList(), glowPrimary = emptyList(), glowSuccess = emptyList(), glowDanger = emptyList(),
        coin = emptyList(), tile = emptyList(), tabBar = emptyList(), noteChip = emptyList(),
        xs = s[0], sm = s[1], md = s[2], lg = s[3], xl = s[4],
    )
}

/** Accounts are drawn, not tinted: no fill, an ink line. */
private fun kolamNotes(ground: Long, ink: Long) = Denomination.entries.associateWith {
    NoteStyle(c(ground), c(ground), c(ink))
}

private val Light = ThemePalette(
    colors = colorsOf(
        false, 0xE3DDD1, 0x2A1B15, 0xECE6DA, 0x8F2D1C, 0xF8F0E4, 0xD9D2C4, 0x5D4E41,
        0x2A1B15, 0xF8F0E4, 0x1C6436, 0x8A4F00,
        border = rgba(42, 27, 21, 0.3f), input = rgba(42, 27, 21, 0.62f),
        charts = listOf(0x8F2D1C, 0x2A1B15, 0x1C6436, 0xA05F00, 0x7D6C5E),
        warningBandAccent = 0x2A1B15,
        glassBorder = rgba(42, 27, 21, 0.55f), scrim = rgba(42, 27, 21, 0.45f),
        sidebar = 0x8F2D1C, sidebarPrimary = 0xF8F0E4, sidebarPrimaryFg = 0x8F2D1C, ring = 0x8F2D1C,
    ),
    brushes = FinioBrushes(
        gradPrimary = solid(0x8F2D1C), gradPrimarySoft = solid(0xD9D2C4),
        gradSuccess = solid(0x1C6436), gradSuccessSoft = solid(0xD6E3D4),
        gradDanger = solid(0x2A1B15), gradDangerSoft = solid(0xD9D2C4),
        gradInfo = solid(0x8F2D1C), gradWarning = solid(0x8A4F00), gradSurface = solid(0xE3DDD1),
        paper = SolidColor(Color.Transparent), coin = solid(0x8F2D1C),
        register = { _ -> solid(0x2A1B15) }, thread = solid(0x8F2D1C),
    ),
    shadows = shadowsOf(Color(42, 27, 21)),
    spec = FinioSpec(VisualTheme.Kolam, 1.5.dp, kolamNotes(0xE3DDD1, 0x2A1B15)),
)

private val Dark = ThemePalette(
    colors = colorsOf(
        true, 0x651E14, 0xF8F0E4, 0x571910, 0xF4C24F, 0x571910, 0x7A2A1D, 0xE9CABD,
        0xFFF3DD, 0x571910, 0xAEE8C4, 0xF4C24F,
        border = rgba(248, 240, 228, 0.32f), input = rgba(248, 240, 228, 0.65f),
        charts = listOf(0xF4C24F, 0xF8F0E4, 0xAEE8C4, 0xE9CABD, 0xD9A999),
        warningBandAccent = 0xF4C24F,
        glassBorder = rgba(248, 240, 228, 0.55f), scrim = rgba(20, 4, 2, 0.6f),
        sidebar = 0x4A130C, sidebarPrimary = 0xF4C24F, sidebarPrimaryFg = 0x571910, ring = 0xF4C24F,
    ),
    brushes = FinioBrushes(
        gradPrimary = solid(0x4A130C), gradPrimarySoft = solid(0x7A2A1D),
        gradSuccess = solid(0x1C6436), gradSuccessSoft = solid(0x5A2A1C),
        gradDanger = solid(0x2B0A05), gradDangerSoft = solid(0x7A2A1D),
        gradInfo = solid(0x4A130C), gradWarning = solid(0x8A4F00), gradSurface = solid(0x651E14),
        paper = SolidColor(Color.Transparent), coin = solid(0xF8F0E4),
        register = { _ -> solid(0xF8F0E4) }, thread = solid(0xF8F0E4),
    ),
    shadows = shadowsOf(Color(0, 0, 0)),
    spec = FinioSpec(VisualTheme.Kolam, 1.5.dp, kolamNotes(0x651E14, 0xF8F0E4)),
)
