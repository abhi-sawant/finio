package com.slowatcoding.finio.ui.theme.kits

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.slowatcoding.finio.R
import com.slowatcoding.finio.ui.mudra.Denomination
import com.slowatcoding.finio.ui.mudra.NoteStyle
import com.slowatcoding.finio.ui.navigation.FinioTab
import com.slowatcoding.finio.ui.shell.tabClickable
import com.slowatcoding.finio.ui.theme.CssRadialGradient
import com.slowatcoding.finio.ui.theme.CssShadow
import com.slowatcoding.finio.ui.theme.FinioBrushes
import com.slowatcoding.finio.ui.theme.FinioColors
import com.slowatcoding.finio.ui.theme.FinioShadows
import com.slowatcoding.finio.ui.theme.FinioShapeScale
import com.slowatcoding.finio.ui.theme.FinioSpec
import com.slowatcoding.finio.ui.theme.FinioTheme
import com.slowatcoding.finio.ui.theme.FinioTypeScale
import com.slowatcoding.finio.ui.theme.Stop
import com.slowatcoding.finio.ui.theme.ThemePalette
import com.slowatcoding.finio.ui.theme.VisualTheme
import com.slowatcoding.finio.ui.theme.cssShadow
import com.slowatcoding.finio.ui.theme.rememberReducedMotion
import com.slowatcoding.finio.ui.theme.rgba
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * Maidan — the cricket ground (web/src/themes/maidan.css). Mown-stripe outfield green on the tab bar,
 * pavilion-white cards on cream, black number plates for the hero figure and the chosen tab, one
 * ball-red accent (a pink ball under the floodlights in dark). 4dp corners, no blur.
 *
 * Differences from the web, forced by the shared files: the hero ink is the page's ink (the shared
 * card cannot re-ink for a dark board), so in light mode the hero is a white scorecard in a green
 * frame instead of a green board; page headers stay plain (no grass hook). Barlow has no rupee
 * sign and Compose cannot fall back per glyph inside one FontFamily, so ₹ in Barlow text comes from
 * the system font; the hero figure and money (Big Shoulders) do carry it.
 */
object MaidanKit : ThemeKit {
    override val visual = VisualTheme.Maidan
    override fun palette(dark: Boolean): ThemePalette = if (dark) Dark else Light
    override val type: FinioTypeScale get() = Scale
    override val shapes: FinioShapeScale get() = Shapes
    override fun coinInk(dark: Boolean) = if (dark) c(0x26030F) else c(0xFFFFFF)
    override val coinShape: Shape get() = CircleShape

    /** The stitched ball: a seam curve and its dashed stitches on the left of the coin. */
    @Composable
    override fun coinDecoration(shape: Shape): Modifier {
        val dark = FinioTheme.colors.isDark
        val seam = if (dark) c(0xB0246B) else c(0x7D0F16)
        val stitch = if (dark) c(0x460022).copy(alpha = 0.7f) else c(0xFFDED6).copy(alpha = 0.85f)
        return Modifier.drawBehind {
            val s = size.width / 56f
            val main = Path().apply {
                moveTo(17.5f * s, 3.5f * s); cubicTo(9f * s, 17f * s, 9f * s, 39f * s, 17.5f * s, 52.5f * s)
            }
            drawPath(main, seam, style = Stroke(2f * s))
            val dash = PathEffect.dashPathEffect(floatArrayOf(1.6f * s, 3.4f * s))
            for (p in listOf(
                Path().apply { moveTo(13.6f * s, 6.5f * s); cubicTo(6f * s, 19f * s, 6f * s, 37f * s, 13.6f * s, 49.5f * s) },
                Path().apply { moveTo(21.6f * s, 2.6f * s); cubicTo(13f * s, 17f * s, 13f * s, 39f * s, 21.6f * s, 53.4f * s) },
            )) drawPath(p, stitch, style = Stroke(1.4f * s, cap = StrokeCap.Round, pathEffect = dash))
        }
    }

    override val progressTrackHeight: Dp get() = 9.dp

    // ── hero: the scoreboard ──
    @Composable
    override fun heroContainer(shape: Shape): Modifier {
        val dark = FinioTheme.colors.isDark
        val card = RoundedCornerShape(3.dp)
        val fill = if (dark) c(0x050A07) else FinioTheme.colors.card
        val frame = if (dark) c(0xE9E3CD) else c(0x12381F)
        return Modifier
            .cssShadow(card, FinioTheme.shadows.float)
            .background(fill, card)
            .border(3.dp, frame, card)
    }

    override fun heroPadding(wide: Boolean) =
        if (wide) PaddingValues(24.dp, 20.dp, 24.dp, 22.dp) else PaddingValues(16.dp, 14.dp, 16.dp, 16.dp)

    /** The figure on one black number plate per digit, stepping down as it grows. */
    @Composable
    override fun HeroFigure(text: String, style: TextStyle, color: Color, whole: Boolean, modifier: Modifier): Boolean {
        val colors = FinioTheme.colors
        val dark = colors.isDark
        val wide = LocalConfiguration.current.screenWidthDp >= 1024
        val len = text.codePointCount(0, text.length)
        val fs = when {
            len <= 6 -> if (wide) 84f else 56f
            len <= 8 -> if (wide) 64f else 44f
            else -> if (wide) 48f else 34f
        }
        val plate = if (dark) c(0x000000) else c(0x111311)
        val plateBd = if (dark) c(0x30493A) else c(0x111311)
        val plateHi = if (dark) rgba(255, 240, 200, 0.3f) else rgba(255, 255, 255, 0.26f)
        val onPlate = if (dark) c(0xFFF1CF) else c(0xFBFAF3)
        val notch = if (dark) c(0x050A07) else c(0x12381F)
        val digitInk = if (color == colors.destructive) c(0xFF7D6E) else onPlate
        val density = LocalDensity.current
        fun em(v: Float): Dp = with(density) { (fs * v).sp.toDp() }
        val digitStyle = TextStyle(
            fontFamily = BigShoulders, fontWeight = FontWeight(900), fontSize = fs.sp, lineHeight = fs.sp,
            fontFeatureSettings = "tnum",
            lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
        )
        Row(
            modifier.clearAndSetSemantics { contentDescription = text },
            horizontalArrangement = Arrangement.spacedBy(em(0.07f)),
            verticalAlignment = Alignment.Bottom,
        ) {
            text.codePoints().toArray().forEach { cp ->
                val ch = String(Character.toChars(cp))
                when {
                    ch[0].isDigit() || ch == "•" -> Box(
                        Modifier
                            .width(em(0.72f)).height(em(1.15f))
                            .background(plate, RoundedCornerShape(2.dp))
                            .border(1.dp, plateBd, RoundedCornerShape(2.dp))
                            .drawBehind {
                                drawLine(plateHi, Offset(1.dp.toPx(), 1.dp.toPx()), Offset(size.width - 1.dp.toPx(), 1.dp.toPx()), 1.dp.toPx())
                                val w = 0.14f * size.width / 0.72f
                                drawRect(notch, Offset((size.width - w) / 2f, 0f), androidx.compose.ui.geometry.Size(w, 0.07f * size.width / 0.72f))
                            },
                        contentAlignment = Alignment.Center,
                    ) { Text(ch, style = digitStyle, color = digitInk, maxLines = 1, softWrap = false) }
                    ch == "₹" -> Text(
                        ch,
                        Modifier.padding(end = em(0.04f), bottom = em(0.16f)),
                        style = digitStyle.copy(fontSize = (fs * 0.55f).sp, lineHeight = (fs * 0.55f).sp, fontWeight = FontWeight(800)),
                        color = colors.foreground, maxLines = 1, softWrap = false,
                    )
                    else -> Box(Modifier.width(em(0.14f)).padding(bottom = em(0.06f)), contentAlignment = Alignment.BottomCenter) {
                        Text(
                            ch, Modifier.wrapContentWidth(unbounded = true),
                            style = digitStyle.copy(fontWeight = FontWeight(900)), color = colors.foreground, maxLines = 1, softWrap = false,
                        )
                    }
                }
            }
        }
        return true
    }

    override val replacesHeroBar: Boolean get() = true

    /** The par chart: dashed par line, the run to the ball at today, a caption. Draws once; the ball bounces in. */
    @Composable
    override fun HeroFooter(elapsed: Float, spentPercent: Float, isOver: Boolean, daysLeft: Int) {
        val colors = FinioTheme.colors
        val dark = colors.isDark
        val wide = LocalConfiguration.current.screenWidthDp >= 1024
        val reduced = rememberReducedMotion()
        val e = elapsed.coerceIn(0f, 1f)
        val spent = spentPercent.coerceAtLeast(0f)
        val par = e * 100f
        val gap = (spent - par).roundToInt()
        val period = LocalHeroPeriod.current
        val (day, total) = remember(e, daysLeft, period) {
            if (period != null) {
                val t = period.totalDays.coerceAtLeast(1)
                period.dayOfPeriod.coerceIn(1, t) to t
            } else {
                dayOf(e, daysLeft)
            }
        }
        val at = if (day != null) " at day $day" else ""
        val caption = when {
            isOver -> "Over budget: ${(spent - 100f).roundToInt()}% past the limit$at"
            gap == 0 -> "On par$at"
            gap < 0 -> "${abs(gap)}% of the budget under par$at"
            else -> "$gap% of the budget over par$at"
        }
        val chalk = if (dark) c(0xFFF1CF) else colors.foreground
        val parInk = if (dark) c(0x9CC4A8) else c(0x2A7436)
        val ballInk = colors.primary
        val ballHi = if (dark) c(0xFFA3CF) else c(0xE2474E)
        val textInk = if (isOver) colors.destructive else colors.foreground

        val run = remember { Animatable(if (reduced) 1f else 0f) }
        val land = remember { Animatable(if (reduced) 1f else 0f) }
        LaunchedEffect(Unit) {
            if (!reduced) {
                run.animateTo(1f, tween(1200, delayMillis = 150, easing = RunEase))
                land.animateTo(1f, tween(550, easing = LinearEasing))
            }
        }
        val plotH = if (wide) 110.dp else 76.dp
        val ballR = if (wide) 9.dp else 7.dp

        Column(Modifier.padding(top = 20.dp).widthIn(max = 512.dp).clearAndSetSemantics { }) {
            Canvas(Modifier.fillMaxWidth().height(plotH)) {
                val w = size.width
                val h = size.height
                val y0 = h * 0.92f
                val yFull = h * 0.08f
                fun yOf(pct: Float) = y0 - (pct.coerceIn(0f, 108f) / 100f) * (y0 - yFull)
                drawLine(chalk.copy(alpha = 0.5f), Offset(0f, y0), Offset(w, y0), 1.5.dp.toPx())
                if (total != null) {
                    for (i in 1..total) {
                        val x = w * i / total
                        drawLine(chalk.copy(alpha = 0.5f), Offset(x, y0), Offset(x, y0 + (if (i % 5 == 0) 6 else 3).dp.toPx()), 1.dp.toPx())
                    }
                }
                drawLine(
                    parInk, Offset(0f, y0), Offset(w, yFull), 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx())),
                )
                val bx = w * e
                val by = yOf(spent)
                val rx = bx * run.value
                val ry = y0 + (by - y0) * run.value
                drawLine(chalk, Offset(0f, y0), Offset(rx, ry), 3.5.dp.toPx(), StrokeCap.Round)
                if (land.value > 0f) {
                    val t = land.value
                    val lift = when {
                        t < 0.5f -> -30f * (1f - t / 0.5f)
                        t < 0.72f -> -6f * ((t - 0.5f) / 0.22f)
                        else -> -6f * (1f - (t - 0.72f) / 0.28f)
                    }.dp.toPx()
                    val a = (t / 0.5f).coerceAtMost(1f)
                    val ctr = Offset(bx, by + lift)
                    val r = ballR.toPx()
                    drawCircle(Color.Black.copy(alpha = 0.3f * a), r + 2.dp.toPx(), ctr + Offset(0f, 3.dp.toPx()))
                    drawCircle(chalk.copy(alpha = a), r + 2.dp.toPx(), ctr)
                    drawCircle(
                        Brush.radialGradient(
                            listOf(ballHi.copy(alpha = a), ballInk.copy(alpha = a)),
                            center = ctr + Offset(-0.3f * r, -0.4f * r), radius = r * 1.5f,
                        ),
                        r, ctr,
                    )
                }
            }
            BoxWithConstraints(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                val label = TextStyle(fontFamily = BarlowSc, fontWeight = FontWeight(500), fontSize = 13.sp, lineHeight = 15.sp)
                val mid = if (day != null) "Day $day, today" else "Today"
                val midW = 110.dp
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(if (day != null) "Day 1" else "Start", style = label, color = colors.mutedForeground)
                    Text(if (total != null) "Day $total" else "End", style = label, color = colors.mutedForeground)
                }
                Text(
                    mid,
                    Modifier.width(midW).offset(x = (maxWidth * e - midW / 2).coerceIn(0.dp, (maxWidth - midW).coerceAtLeast(0.dp))),
                    style = label.copy(fontWeight = FontWeight(600)), color = colors.foreground,
                    textAlign = TextAlign.Center, maxLines = 1,
                )
            }
            Row(Modifier.padding(top = 5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Box(
                    Modifier.padding(start = 2.dp).size(9.dp)
                        .border(1.5.dp, chalk, CircleShape).padding(1.5.dp).clip(CircleShape).background(ballInk),
                )
                Text(
                    caption,
                    style = TextStyle(fontFamily = BarlowSc, fontWeight = FontWeight(600), fontSize = 14.5.sp, lineHeight = 17.sp),
                    color = textInk,
                )
            }
        }
    }

    // ── chrome: the outfield, the chosen tab a black plate with the ball's mark on top ──
    @Composable
    override fun TabBar(active: FinioTab?, onSelect: (FinioTab) -> Unit, modifier: Modifier) {
        val dark = FinioTheme.colors.isDark
        val g1 = if (dark) c(0x0E2618) else c(0x2A7436)
        val g2 = if (dark) c(0x0B1F14) else c(0x256B31)
        val chalk = if (dark) c(0xFFF1CF) else c(0xF6F8EF)
        val onG = if (dark) c(0xF3F0E0) else c(0xF6F8EF)
        val plate = if (dark) c(0x000000) else c(0x111311)
        val plateBd = if (dark) c(0x30493A) else c(0x111311)
        val onPlate = if (dark) c(0xFFF1CF) else c(0xFBFAF3)
        val ball = if (dark) c(0xFF5AA8) else c(0xB81D24)
        Row(
            modifier
                .fillMaxWidth()
                .drawBehind {
                    drawRect(g1)
                    val band = 65.dp.toPx()
                    var x = band
                    while (x < size.width) {
                        drawRect(g2, Offset(x, 0f), androidx.compose.ui.geometry.Size(band, size.height))
                        x += band * 2
                    }
                    if (dark) {
                        drawRect(
                            Brush.radialGradient(
                                listOf(rgba(190, 255, 205, 0.16f), Color.Transparent),
                                center = Offset(size.width / 2f, -0.12f * size.height), radius = size.width * 0.62f,
                            ),
                        )
                    }
                    // the chalked touchline: 6 on, 2 darker
                    val edge = chalk.copy(alpha = 0.7f)
                    var cx = 0f
                    while (cx < size.width) {
                        drawRect(chalk, Offset(cx, 0f), androidx.compose.ui.geometry.Size(6.dp.toPx(), 4.dp.toPx()))
                        drawRect(edge, Offset(cx + 6.dp.toPx(), 0f), androidx.compose.ui.geometry.Size(2.dp.toPx(), 4.dp.toPx()))
                        cx += 8.dp.toPx()
                    }
                }
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(start = 6.dp, end = 6.dp, top = 8.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val tabShape = RoundedCornerShape(3.dp)
            FinioTab.entries.forEach { tab ->
                val selected = tab == active
                val ink = if (selected) onPlate else onG
                val icon = remember(tab) { tab.icon(if (selected) 2f else 1.7f) }
                Column(
                    Modifier
                        .weight(1f)
                        .clip(tabShape)
                        .then(
                            if (selected) Modifier
                                .background(plate, tabShape)
                                .border(1.dp, plateBd, tabShape)
                                .drawBehind { drawRect(ball, size = androidx.compose.ui.geometry.Size(size.width, 3.dp.toPx())) }
                            else Modifier,
                        )
                        .tabClickable(selected) { onSelect(tab) }
                        .padding(top = 8.dp, bottom = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = ink)
                    Text(tab.label, style = TabStyle, color = ink, maxLines = 1)
                }
            }
        }
    }
}

// ───────────────────────────── type & shape ─────────────────────────────

/** Fallback when `LocalHeroPeriod` is null: day number of today and the period's length, recovered from the fraction elapsed and days left. */
private fun dayOf(elapsed: Float, daysLeft: Int): Pair<Int?, Int?> {
    if (daysLeft < 1) return null to null
    for (d in 1..400) {
        val total = daysLeft + d - 1
        if (floor(elapsed * total).toInt() + 1 == d) return d to total
    }
    return null to null
}

private fun variable(res: Int, weight: Int) = Font(
    resId = res,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

private val BigShoulders = FontFamily(
    variable(R.font.maidan_big_shoulders, 500), variable(R.font.maidan_big_shoulders, 600),
    variable(R.font.maidan_big_shoulders, 700), variable(R.font.maidan_big_shoulders, 800),
    variable(R.font.maidan_big_shoulders, 900),
)
private val Barlow = FontFamily(
    Font(R.font.maidan_barlow_regular, FontWeight.W400), Font(R.font.maidan_barlow_medium, FontWeight.W500),
    Font(R.font.maidan_barlow_semibold, FontWeight.W600), Font(R.font.maidan_barlow_bold, FontWeight.W700),
)
private val BarlowSc = FontFamily(
    Font(R.font.maidan_barlow_sc_medium, FontWeight.W500), Font(R.font.maidan_barlow_sc_semibold, FontWeight.W600),
    Font(R.font.maidan_barlow_sc_bold, FontWeight.W700),
)

private val Scale = FinioTypeScale(
    BigShoulders, BigShoulders, Barlow,
    displayWeight = 800, displayTracking = 0.01f, pageTitleWeight = 800, screenTitleWeight = 800,
    headlineWeight = 800, moneyWeight = 800, dialogTitleWeight = 800, titleWeight = 600,
)
private val Shapes = FinioShapeScale(base = 4.dp, full = RoundedCornerShape(4.dp))

private val TabStyle = TextStyle(fontFamily = BarlowSc, fontWeight = FontWeight(600), fontSize = 11.sp, letterSpacing = 0.03.em)

/** `cubic-bezier(.22,.8,.3,1)` — the run to the ball. */
private val RunEase = CubicBezierEasing(0.22f, 0.8f, 0.3f, 1f)

// ───────────────────────────── palettes ─────────────────────────────

private fun c(rgb: Long) = Color(0xFF000000 or rgb)
private fun solid(rgb: Long): Brush = SolidColor(c(rgb))

private fun colorsOf(
    dark: Boolean,
    background: Long, foreground: Long, card: Long, popover: Long,
    primary: Long, primaryFg: Long, secondary: Long, muted: Long, mutedFg: Long,
    accent: Long, accentFg: Long, destructive: Long, destructiveFg: Long,
    positive: Long, warning: Long, border: Long, input: Long,
    charts: List<Long>, sidebar: Long,
    warningBand: Long, warningBandFg: Long, warningBandAccent: Long,
    glass: Long, glassStrong: Long, shadowTint: Color, scrim: Color,
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
    border = c(border), input = c(input), ring = c(primary),
    chart1 = c(charts[0]), chart2 = c(charts[1]), chart3 = c(charts[2]), chart4 = c(charts[3]), chart5 = c(charts[4]),
    sidebar = c(sidebar), sidebarForeground = c(if (dark) 0xF3F0E0 else 0xF6F8EF),
    sidebarPrimary = c(primary), sidebarPrimaryForeground = c(primaryFg),
    sidebarAccent = c(if (dark) 0x000000 else 0x111311), sidebarAccentForeground = c(if (dark) 0xFFF1CF else 0xFBFAF3),
    sidebarBorder = c(if (dark) 0xF3F0E0 else 0xF6F8EF), sidebarRing = c(primary),
    warningBand = c(warningBand), warningBandForeground = c(warningBandFg), warningBandAccent = c(warningBandAccent),
    engraving = Color.Transparent,
    glass = c(glass), glassStrong = c(glassStrong), glassBorder = c(border), glassHighlight = Color.Transparent,
    shadowTint = shadowTint, scrim = scrim,
)

private fun shadowsOf(tint: Color) = FinioShadows(
    card = emptyList(),
    float = listOf(CssShadow(y = 8.dp, blur = 16.dp, spread = (-8).dp, color = rgba(0, 0, 0, 0.55f))),
    glowPrimary = emptyList(), glowSuccess = emptyList(), glowDanger = emptyList(),
    coin = emptyList(), tile = emptyList(), tabBar = emptyList(), noteChip = emptyList(),
    xs = emptyList(), sm = emptyList(), md = emptyList(), lg = emptyList(), xl = emptyList(),
).also { tint.hashCode() }

/** Accounts are scorecard cells: one paper tint, black ink; the card owes in red. */
private fun notes(paper: Long, ink: Long, red: Long) = Denomination.entries.associateWith { d ->
    NoteStyle(c(paper), c(paper), c(if (d == Denomination.Rs2000) red else ink))
}

private fun ball(hi: Long, mid: Long, seam: Long): Brush = CssRadialGradient(
    0.34f, 0.26f,
    listOf(Stop(0f, c(hi)), Stop(0.43f, c(mid)), Stop(1f, c(seam))),
)

private val Light = ThemePalette(
    colors = colorsOf(
        false, 0xF6F4EA, 0x141A16, 0xFCFBF5, 0xFCFBF5, 0xB81D24, 0xFFFFFF, 0xE9E4D0, 0xE9E4D0, 0x566058,
        0xE9E4D0, 0x141A16, 0xB81D24, 0xFFFFFF, 0x1C7230, 0x855600, 0xD3D0BF, 0x8A8873,
        charts = listOf(0x12381F, 0xB81D24, 0x2A7436, 0x855600, 0x566058), sidebar = 0x163F24,
        warningBand = 0xFBE8D6, warningBandFg = 0x3D2400, warningBandAccent = 0x855600,
        glass = 0xFCFBF5, glassStrong = 0xF6F4EA,
        shadowTint = Color(20, 26, 22), scrim = rgba(10, 20, 14, 0.5f),
    ),
    brushes = FinioBrushes(
        gradPrimary = solid(0xB81D24), gradPrimarySoft = solid(0xE9E4D0),
        gradSuccess = solid(0x1C7230), gradSuccessSoft = solid(0xDCEBD9),
        gradDanger = solid(0xB81D24), gradDangerSoft = solid(0xF6DEDC),
        gradInfo = solid(0x12381F), gradWarning = solid(0x855600), gradSurface = solid(0xFCFBF5),
        paper = SolidColor(Color.Transparent), coin = ball(0xE2474E, 0xB81D24, 0x7D0F16),
        register = { SolidColor(c(0x1C7230)) }, thread = solid(0xB81D24),
    ),
    shadows = shadowsOf(Color(20, 26, 22)),
    spec = FinioSpec(VisualTheme.Maidan, 1.dp, notes(0xFCFBF5, 0x141A16, 0xB81D24)),
)

private val Dark = ThemePalette(
    colors = colorsOf(
        true, 0x09140E, 0xECEADB, 0x0F2017, 0x12261A, 0xFF5AA8, 0x26030F, 0x12261A, 0x12261A, 0x9BAD9F,
        0x173022, 0xECEADB, 0xFF7D6E, 0x2A0603, 0x5FE07F, 0xF0B445, 0x24392C, 0x4A6553,
        charts = listOf(0x5FE07F, 0xFF5AA8, 0xFFF1CF, 0xF0B445, 0x9BAD9F), sidebar = 0x06100A,
        warningBand = 0x2C1F0E, warningBandFg = 0xFFE9C8, warningBandAccent = 0xF0B445,
        glass = 0x0F2017, glassStrong = 0x06100A,
        shadowTint = Color(0, 0, 0), scrim = rgba(0, 0, 0, 0.65f),
    ),
    brushes = FinioBrushes(
        gradPrimary = solid(0xA81F62), gradPrimarySoft = solid(0x12261A),
        gradSuccess = solid(0x1C7230), gradSuccessSoft = solid(0x12301D),
        gradDanger = solid(0xB3261E), gradDangerSoft = solid(0x2B1412),
        gradInfo = solid(0x173022), gradWarning = solid(0x855600), gradSurface = solid(0x0F2017),
        paper = SolidColor(Color.Transparent), coin = ball(0xFFA3CF, 0xFF5AA8, 0xB0246B),
        register = { SolidColor(c(0x5FE07F)) }, thread = solid(0xFF5AA8),
    ),
    shadows = shadowsOf(Color(0, 0, 0)),
    spec = FinioSpec(VisualTheme.Maidan, 1.dp, notes(0x0F2017, 0xECEADB, 0xFF7D6E)),
)
