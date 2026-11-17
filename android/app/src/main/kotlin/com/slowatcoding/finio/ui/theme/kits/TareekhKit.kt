package com.slowatcoding.finio.ui.theme.kits

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * Tareekh — the household wall calendar (web/src/themes/tareekh.css). White coated paper, black
 * ink, calendar red and postal blue; ruled rectangular modules, zero radii, no shadows. Anek Latin
 * is worked through its width axis: wide ExtraBold for date numerals and the headline figure,
 * condensed caps for labels. The signature is the budget period drawn as a Sunday-first calendar
 * ([HeroFooter]) with today ringed in red — the ring is drawn once.
 */
object TareekhKit : ThemeKit {
    override val visual = VisualTheme.Tareekh
    override fun palette(dark: Boolean): ThemePalette = if (dark) Dark else Light
    override val type: FinioTypeScale get() = Scale
    override val shapes: FinioShapeScale get() = Shapes

    // ── the add button: a red slab with a darker top edge, like a stamped key ──
    override fun coinInk(dark: Boolean) = Inks.of(dark).onRed
    override val coinShape: Shape get() = RectangleShape

    @Composable
    override fun coinDecoration(shape: Shape): Modifier {
        val ink = Inks.of(FinioTheme.colors.isDark)
        return Modifier.drawBehind {
            drawRect(ink.redDeep, size = Size(size.width, 7.dp.toPx()))
            drawRect(ink.line, style = Stroke(1.dp.toPx()))
        }
    }

    override val progressTrackHeight: Dp get() = 11.dp

    /** A tinted, ruled track; the fill is cut into 20 day slots by paper-coloured gaps. */
    @Composable
    override fun progressDecoration(): Modifier {
        val colors = FinioTheme.colors
        val ink = Inks.of(colors.isDark)
        val bg = colors.background
        return Modifier.drawWithContent {
            drawRect(ink.tint)
            drawContent()
            val slots = 20
            val gap = 1.dp.toPx()
            for (i in 1 until slots) {
                val x = size.width * i / slots - gap
                drawRect(bg.copy(alpha = 0.75f), Offset(x, 0f), Size(gap, size.height))
            }
            val w = 1.dp.toPx()
            drawRect(ink.line, Offset(w / 2, w / 2), Size(size.width - w, size.height - w), style = Stroke(w))
        }
    }

    // ── hero: a ruled module; the calendar runs rule to rule ──
    @Composable
    override fun heroContainer(shape: Shape): Modifier {
        val ink = Inks.of(FinioTheme.colors.isDark)
        return Modifier.drawWithContent {
            drawContent()
            val w = 1.dp.toPx()
            drawRect(ink.line, Offset(w / 2, w / 2), Size(size.width - w, size.height - w), style = Stroke(w))
        }
    }

    override fun heroPadding(wide: Boolean) = PaddingValues(HeroGutter, 10.dp, HeroGutter, 0.dp)

    @Composable
    override fun HeroFigure(text: String, style: TextStyle, color: Color, whole: Boolean, modifier: Modifier): Boolean {
        Text(
            text,
            modifier,
            style = style.copy(
                fontFamily = WideFamily,
                fontSize = 60.sp,
                lineHeight = 57.sp,
                fontWeight = FontWeight(800),
                letterSpacing = (-0.02).em,
                fontFeatureSettings = "tnum",
            ),
            color = color,
            maxLines = 1,
            softWrap = false,
        )
        return true
    }

    @Composable
    override fun HeroFooter(elapsed: Float, spentPercent: Float, isOver: Boolean, daysLeft: Int) {
        val period = LocalHeroPeriod.current
        val (day, total) = (if (period != null) period.dayOfPeriod to period.totalDays else dayOfPeriod(elapsed, daysLeft)) ?: return
        val colors = FinioTheme.colors
        val ink = Inks.of(colors.isDark)
        val reduced = rememberReducedMotion()
        Column(
            Modifier
                .padding(top = 12.dp)
                .bleed(HeroGutter)
                .clearAndSetSemantics { },
        ) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(ink.line))
            if (total <= MaxGridDays) CalendarGrid(day, total, ink, reduced) else WeekStrip(day, total, ink)
            // the key
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(22.dp)
                    .background(ink.tint)
                    .drawBehind { drawRect(ink.rule, size = Size(size.width, 1.dp.toPx())) }
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.width(10.dp).height(1.dp).background(colors.mutedForeground))
                Text("PAST", Modifier.padding(start = 4.dp), style = KeyStyle, color = colors.mutedForeground, maxLines = 1)
                Box(Modifier.padding(start = 8.dp).width(10.dp).height(4.dp).background(ink.red))
                Text("TODAY", Modifier.padding(start = 4.dp), style = KeyStyle, color = colors.mutedForeground, maxLines = 1)
                Text(
                    "DAY $day OF $total",
                    Modifier.weight(1f),
                    style = KeyStyle,
                    color = colors.foreground,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                )
            }
        }
    }

    // ── chrome: ruled cells of condensed caps, the current page filled red ──
    @Composable
    override fun TabBar(active: FinioTab?, onSelect: (FinioTab) -> Unit, modifier: Modifier) {
        val colors = FinioTheme.colors
        val ink = Inks.of(colors.isDark)
        Row(
            modifier
                .fillMaxWidth()
                .background(colors.background)
                .drawBehind { drawRect(ink.line, size = Size(size.width, 2.dp.toPx())) }
                .windowInsetsPadding(WindowInsets.navigationBars),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FinioTab.entries.forEachIndexed { index, tab ->
                val selected = tab == active
                val tint = if (selected) ink.onRed else colors.foreground
                val icon = remember(tab) { tab.icon(1.75f) }
                Column(
                    Modifier
                        .weight(1f)
                        .background(if (selected) ink.red else Color.Transparent)
                        .drawBehind {
                            if (index > 0) drawRect(ink.line, Offset(0f, 2.dp.toPx()), Size(1.dp.toPx(), size.height - 2.dp.toPx()))
                        }
                        .tabClickable(selected) { onSelect(tab) }
                        .padding(top = 12.dp, bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = tint)
                    Text(tab.label.uppercase(), style = TabStyle, color = tint, maxLines = 1)
                }
            }
        }
    }
}

private val HeroGutter = 10.dp

/** Past this many days a grid of cells stops being a calendar, so the piece becomes a week strip. */
private const val MaxGridDays = 42

/** Lets a child run [by] past the parent's horizontal padding on both sides (the grid runs rule to rule). */
private fun Modifier.bleed(by: Dp) = layout { measurable, constraints ->
    val extra = by.roundToPx() * 2
    val placeable = measurable.measure(
        constraints.copy(
            minWidth = if (constraints.hasBoundedWidth) constraints.maxWidth + extra else constraints.minWidth,
            maxWidth = if (constraints.hasBoundedWidth) constraints.maxWidth + extra else constraints.maxWidth,
        ),
    )
    layout(placeable.width - extra, placeable.height) { placeable.place(-extra / 2, 0) }
}

// ───────────────────────────── the calendar ─────────────────────────────

private val Weekdays = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
private val Months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

/** `cubic-bezier(.2,.7,.2,1)` — the ring stroke. */
private val RingEase = CubicBezierEasing(0.2f, 0.7f, 0.2f, 1f)

/**
 * Fallback when `LocalHeroPeriod` is null (the exact values are used otherwise): day n of N in the budget period, from the hero's own `elapsed` and `daysLeft` (the period runs
 * start-of-day to end-of-day, so `elapsed × N = N − daysLeft + today's fraction`) — the same
 * derivation as Khel's HUD. Null when it doesn't resolve to a whole, in-range day.
 */
private fun dayOfPeriod(elapsed: Float, daysLeft: Int): Pair<Int, Int>? {
    if (elapsed >= 1f || daysLeft < 1) return null
    val now = Calendar.getInstance()
    val fraction = (now.get(Calendar.HOUR_OF_DAY) * 3600 + now.get(Calendar.MINUTE) * 60 + now.get(Calendar.SECOND)) / 86_400f
    if (elapsed <= 0f) return 1 to daysLeft
    val total = ((daysLeft - fraction) / (1f - elapsed)).roundToInt()
    val day = total - daysLeft + 1
    return if (total in 1..366 && day in 1..total) day to total else null
}

@Composable
private fun CalendarGrid(day: Int, total: Int, ink: TkInk, reduced: Boolean) {
    val colors = FinioTheme.colors
    // The period's first day: today minus (day − 1) calendar days.
    val start = remember(day) { Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -(day - 1)) } }
    val lead = start.get(Calendar.DAY_OF_WEEK) - 1 // Sunday-first
    // Cells: lead blanks, then the days.
    val cellCount = lead + total
    val rows = (cellCount + 6) / 7
    Row(Modifier.fillMaxWidth().height(17.dp).background(ink.tint).drawBehind {
        drawRect(ink.line, Offset(0f, size.height - 1.dp.toPx()), Size(size.width, 1.dp.toPx()))
    }) {
        Weekdays.forEachIndexed { i, w ->
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .drawBehind { if (i > 0) drawRect(ink.rule, size = Size(1.dp.toPx(), size.height)) }
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(w.uppercase(), style = WeekdayStyle, color = if (i == 0) ink.redText else colors.foreground, maxLines = 1)
            }
        }
    }
    repeat(rows) { r ->
        Row(Modifier.fillMaxWidth().height(44.dp)) {
            repeat(7) { col ->
                val index = r * 7 + col
                val n = index - lead + 1
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .drawBehind {
                            val w = 1.dp.toPx()
                            if (col > 0) drawRect(ink.rule, size = Size(w, size.height))
                            if (r > 0) drawRect(ink.rule, size = Size(size.width, w))
                        },
                ) {
                    if (n < 1 || n > total) {
                        Box(Modifier.fillMaxSize().background(ink.tint.copy(alpha = 0.6f)))
                    } else {
                        DayCell(n, day, start, col == 0, ink, reduced)
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(n: Int, today: Int, start: Calendar, sunday: Boolean, ink: TkInk, reduced: Boolean) {
    val colors = FinioTheme.colors
    val date = remember(n, start) { (start.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, n - 1) } }
    val dom = date.get(Calendar.DAY_OF_MONTH)
    val isToday = n == today
    val isPast = n < today
    val numeral = when {
        sunday -> ink.redText
        isPast -> colors.mutedForeground
        else -> colors.foreground
    }
    Box(Modifier.fillMaxSize().background(if (isToday) ink.redTint else Color.Transparent).padding(start = 4.dp, top = 4.dp, end = 3.dp, bottom = 4.dp)) {
        Column {
            Box(if (isToday) RingModifier(ink.red, reduced) else Modifier) {
                Text(dom.toString(), style = DateStyle, color = numeral, maxLines = 1)
            }
            if (isToday) {
                Text("TODAY", Modifier.padding(top = 2.dp), style = TodayStyle, color = ink.redText, maxLines = 1, overflow = TextOverflow.Clip)
            }
        }
        if (n == 1 || dom == 1) {
            Text(
                Months[date.get(Calendar.MONTH)].uppercase(),
                Modifier.align(Alignment.TopEnd),
                style = MonthStyle,
                color = colors.mutedForeground,
                maxLines = 1,
            )
        }
        if (isPast) {
            // the "nothing more to do here" tick
            Box(Modifier.align(Alignment.BottomStart).padding(bottom = 1.dp).width(9.dp).height(1.dp).background(colors.mutedForeground.copy(alpha = 0.7f)))
        }
    }
}

/** The hand-drawn red loop round today; strokes on once, then stays. */
@Composable
private fun RingModifier(red: Color, reduced: Boolean): Modifier {
    val progress = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(Unit) { if (!reduced) progress.animateTo(1f, tween(600, delayMillis = 700, easing = RingEase)) }
    val path = remember { RingPath() }
    return Modifier.drawBehind {
        val padX = 15.dp.toPx()
        val padY = 11.dp.toPx()
        val w = size.width + padX
        val h = size.height + padY
        val left = (size.width - w) / 2f
        val top = (size.height - h) / 2f - 0.04f * h
        val scaled = Path().apply {
            addPath(path)
            transform(androidx.compose.ui.graphics.Matrix().apply { scale(w / 44f, h / 30f) })
            translate(Offset(left, top))
        }
        val measure = PathMeasure().apply { setPath(scaled, false) }
        val seg = Path()
        measure.getSegment(0f, measure.length * progress.value, seg, true)
        drawPath(seg, red, style = Stroke(1.75.dp.toPx(), cap = StrokeCap.Round))
    }
}

/** SVG `M31 4.5 C22 1 6 3 3.5 13.5 c-2 9.5 12 14.5 24 13 C38 25 43 17 40 10 37.5 4.5 29 2.5 20 3.5` in a 44×30 box. */
private fun RingPath() = Path().apply {
    moveTo(31f, 4.5f)
    cubicTo(22f, 1f, 6f, 3f, 3.5f, 13.5f)
    cubicTo(1.5f, 23f, 15.5f, 28f, 27.5f, 26.5f)
    cubicTo(38f, 25f, 43f, 17f, 40f, 10f)
    cubicTo(37.5f, 4.5f, 29f, 2.5f, 20f, 3.5f)
}

/** Long periods (a year) become a strip: one tick per week, today's week in red. */
@Composable
private fun WeekStrip(day: Int, total: Int, ink: TkInk) {
    val colors = FinioTheme.colors
    val weeks = ceil(total / 7f).toInt().coerceAtLeast(1)
    val now = ((day - 1) / 7).coerceAtMost(weeks - 1)
    Row(Modifier.fillMaxWidth().height(20.dp).padding(horizontal = HeroGutter, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(1.dp)) {
        repeat(weeks) { w ->
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .background(if (w < now) colors.mutedForeground else if (w == now) ink.red else ink.rule),
            )
        }
    }
}

// ───────────────────────────── type & shape ─────────────────────────────

private fun anek(weight: Int, width: Float) = Font(
    resId = R.font.tareekh_anek_latin,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight), FontVariation.width(width)),
)

private fun anekFamily(width: Float) = FontFamily(
    anek(400, width), anek(500, width), anek(600, width), anek(700, width), anek(800, width),
)

/** Wide (108%) — date numerals, the headline figure, titles. */
private val WideFamily = anekFamily(108f)

/** Condensed (80%) — caps labels, tabs, the key. */
private val CondensedFamily = anekFamily(80f)

/** Reading text sits at 90% (the web's body `font-stretch`). */
private val ReadingFamily = anekFamily(90f)

private val Scale = FinioTypeScale(
    WideFamily, WideFamily, ReadingFamily,
    displayWeight = 800, displayTracking = -0.02f, pageTitleWeight = 800, screenTitleWeight = 700,
    headlineWeight = 800, moneyWeight = 800, dialogTitleWeight = 800, titleWeight = 700,
)
private val Shapes = FinioShapeScale(base = 0.dp, full = RectangleShape)

private val TabStyle = TextStyle(fontFamily = CondensedFamily, fontWeight = FontWeight(600), fontSize = 11.sp, letterSpacing = 0.04.em)
private val KeyStyle = TextStyle(fontFamily = CondensedFamily, fontWeight = FontWeight(600), fontSize = 11.sp, letterSpacing = 0.05.em, fontFeatureSettings = "tnum")
private val WeekdayStyle = TextStyle(fontFamily = CondensedFamily, fontWeight = FontWeight(700), fontSize = 11.sp, letterSpacing = 0.08.em)
private val MonthStyle = TextStyle(fontFamily = CondensedFamily, fontWeight = FontWeight(700), fontSize = 10.sp, letterSpacing = 0.05.em)
private val TodayStyle = TextStyle(fontFamily = CondensedFamily, fontWeight = FontWeight(700), fontSize = 11.sp, letterSpacing = 0.06.em)
private val DateStyle = TextStyle(
    fontFamily = WideFamily, fontWeight = FontWeight(800), fontSize = 15.sp, lineHeight = 15.sp, fontFeatureSettings = "tnum",
)

// ───────────────────────────── palettes ─────────────────────────────

private fun c(rgb: Long) = Color(0xFF000000 or rgb)
private fun solid(rgb: Long): Brush = SolidColor(c(rgb))

/** The theme's own inks (web `--tk-*`), for the pieces drawn here. */
private class TkInk(
    val red: Color, val redDeep: Color, val redText: Color, val onRed: Color, val redTint: Color,
    val line: Color, val rule: Color, val tint: Color,
)

private object Inks {
    private val light = TkInk(
        red = c(0xD51F2A), redDeep = c(0xA8131D), redText = c(0xA8131D), onRed = c(0xFFFFFF), redTint = c(0xFDECEE),
        line = c(0x0C0E12), rule = c(0xC3CAD5), tint = c(0xEEF1F5),
    )
    private val dark = TkInk(
        red = c(0xFF4A4D), redDeep = c(0xC9282F), redText = c(0xFF4A4D), onRed = c(0x0A0F1F), redTint = c(0x2A1422),
        line = c(0x56628A), rule = c(0x27314F), tint = c(0x111931),
    )
    fun of(dark: Boolean) = if (dark) this.dark else light
}

private fun colorsOf(
    dark: Boolean,
    background: Long, foreground: Long, card: Long, popover: Long,
    primary: Long, primaryFg: Long, secondary: Long, muted: Long, mutedFg: Long,
    accent: Long, accentFg: Long, destructive: Long, destructiveFg: Long,
    positive: Long, warning: Long, border: Color, input: Color, ring: Long,
    charts: List<Long>,
    warningBand: Long, warningBandFg: Long, warningBandAccent: Long,
    glassStrong: Color, glassBorder: Color, shadowTint: Color, scrim: Color,
) = FinioColors(
    isDark = dark,
    background = c(background), foreground = c(foreground),
    card = c(card), cardForeground = c(foreground),
    popover = c(popover), popoverForeground = c(foreground),
    primary = c(primary), primaryForeground = c(primaryFg),
    secondary = c(secondary), secondaryForeground = c(foreground),
    muted = c(muted), mutedForeground = c(mutedFg),
    accent = c(accent), accentForeground = c(accentFg),
    destructive = c(destructive), destructiveForeground = c(destructiveFg),
    positive = c(positive), warning = c(warning),
    border = border, input = input, ring = c(ring),
    chart1 = c(charts[0]), chart2 = c(charts[1]), chart3 = c(charts[2]), chart4 = c(charts[3]), chart5 = c(charts[4]),
    sidebar = c(card), sidebarForeground = c(foreground),
    sidebarPrimary = c(primary), sidebarPrimaryForeground = c(primaryFg),
    sidebarAccent = c(accent), sidebarAccentForeground = c(accentFg),
    sidebarBorder = border, sidebarRing = c(ring),
    warningBand = c(warningBand), warningBandForeground = c(warningBandFg), warningBandAccent = c(warningBandAccent),
    engraving = Color.Transparent,
    glass = Color.Transparent, glassStrong = glassStrong, glassBorder = glassBorder, glassHighlight = Color.Transparent,
    shadowTint = shadowTint, scrim = scrim,
)

/** No shadows anywhere — overlays get a hard offset in the web, which the stock scale cannot express. */
private fun flatShadows() = FinioShadows(
    card = emptyList(), float = emptyList(), glowPrimary = emptyList(), glowSuccess = emptyList(), glowDanger = emptyList(),
    coin = emptyList(), tile = emptyList(), tabBar = emptyList(), noteChip = emptyList(),
    xs = emptyList(), sm = emptyList(), md = emptyList(), lg = emptyList(), xl = emptyList(),
)

/** Accounts are ruled rows in black ink; what is owed runs in red, the 200 note in postal blue. */
private fun tareekhNotes(ink: Long, red: Long, blue: Long) = Denomination.entries.associateWith { d ->
    NoteStyle(
        Color.Transparent, Color.Transparent,
        c(
            when (d) {
                Denomination.Rs2000 -> red
                Denomination.Rs200 -> blue
                else -> ink
            },
        ),
    )
}

private val Light = ThemePalette(
    colors = colorsOf(
        false, 0xFCFDFE, 0x0C0E12, 0xFCFDFE, 0xFFFFFF, 0xD51F2A, 0xFFFFFF, 0xEEF1F5, 0xEEF1F5, 0x4A5362,
        0xEEF1F5, 0x0C0E12, 0xD51F2A, 0xFFFFFF, 0x1746B3, 0x8A4B00,
        border = c(0xC3CAD5), input = c(0x6B7484), ring = 0x1746B3,
        charts = listOf(0x0C0E12, 0xD51F2A, 0x1746B3, 0xB45309, 0x6B7484),
        warningBand = 0xFDECEE, warningBandFg = 0x4A0A10, warningBandAccent = 0xD51F2A,
        glassStrong = c(0xFCFDFE), glassBorder = c(0xC3CAD5),
        shadowTint = Color(12, 14, 18), scrim = rgba(12, 14, 18, 0.4f),
    ),
    brushes = FinioBrushes(
        gradPrimary = solid(0xD51F2A), gradPrimarySoft = solid(0xEEF1F5),
        gradSuccess = solid(0x1746B3), gradSuccessSoft = solid(0xE6ECFA),
        gradDanger = solid(0xD51F2A), gradDangerSoft = solid(0xFDECEE),
        gradInfo = solid(0x1746B3), gradWarning = solid(0x8A4B00), gradSurface = solid(0xFCFDFE),
        paper = SolidColor(Color.Transparent), coin = solid(0xD51F2A),
        register = { _ -> solid(0x0C0E12) }, thread = solid(0xD51F2A),
    ),
    shadows = flatShadows(),
    spec = FinioSpec(VisualTheme.Tareekh, 1.dp, tareekhNotes(0x0C0E12, 0xD51F2A, 0x1746B3)),
)

private val Dark = ThemePalette(
    colors = colorsOf(
        true, 0x0A0F1F, 0xEDEFE8, 0x0A0F1F, 0x111931, 0xFF4A4D, 0x0A0F1F, 0x111931, 0x111931, 0x9CA7C2,
        0x172041, 0xEDEFE8, 0xFF4A4D, 0x0A0F1F, 0x86ACFF, 0xF0B252,
        border = c(0x27314F), input = c(0x56628A), ring = 0x86ACFF,
        charts = listOf(0xEDEFE8, 0xFF4A4D, 0x86ACFF, 0xF0B252, 0x9CA7C2),
        warningBand = 0x2A1422, warningBandFg = 0xFFE1E1, warningBandAccent = 0xFF4A4D,
        glassStrong = c(0x0A0F1F), glassBorder = c(0x27314F),
        shadowTint = Color(0, 0, 0), scrim = rgba(0, 0, 0, 0.6f),
    ),
    brushes = FinioBrushes(
        // Deep enough for the white text call sites hardcode (5.5:1 on #C9282F).
        gradPrimary = solid(0xC9282F), gradPrimarySoft = solid(0x111931),
        gradSuccess = solid(0x2956C4), gradSuccessSoft = solid(0x111D3D),
        gradDanger = solid(0xC9282F), gradDangerSoft = solid(0x2A1422),
        gradInfo = solid(0x2956C4), gradWarning = solid(0x8A4B00), gradSurface = solid(0x0A0F1F),
        paper = SolidColor(Color.Transparent), coin = solid(0xFF4A4D),
        register = { _ -> solid(0xEDEFE8) }, thread = solid(0xFF4A4D),
    ),
    shadows = flatShadows(),
    spec = FinioSpec(VisualTheme.Tareekh, 1.dp, tareekhNotes(0xEDEFE8, 0xFF4A4D, 0x86ACFF)),
)
