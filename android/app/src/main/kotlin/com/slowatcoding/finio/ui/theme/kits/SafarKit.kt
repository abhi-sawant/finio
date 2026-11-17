package com.slowatcoding.finio.ui.theme.kits

import androidx.compose.animation.core.Animatable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slowatcoding.finio.R
import com.slowatcoding.finio.ui.mudra.Denomination
import com.slowatcoding.finio.ui.mudra.NoteStyle
import com.slowatcoding.finio.ui.navigation.FinioTab
import com.slowatcoding.finio.ui.shell.ThemedTab
import com.slowatcoding.finio.ui.theme.CssShadow
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
import kotlin.math.cos
import kotlin.math.roundToInt

/**
 * Safar — the departures board (web/src/themes/safar.css). Light is the chrome-yellow platform
 * sign over a concrete field with a coach-blue shell; dark is the black board with amber, green
 * and red status colours. 4dp boards, hairline frames, Overpass for names and Overpass Mono for
 * every figure. The signature is the split-flap hero figure ([HeroFigure]: one tile per character,
 * flipping in once) with a "Day n of N" clock and a 20-segment period strip ([HeroFooter]).
 */
object SafarKit : ThemeKit {
    override val visual = VisualTheme.Safar
    override fun palette(dark: Boolean): ThemePalette = if (dark) Dark else Light
    override val type: FinioTypeScale get() = Scale
    override val shapes: FinioShapeScale get() = Shapes

    // The FAB is the board's lamp: blue by day, lit yellow at night, ringed like a bezel.
    override fun coinInk(dark: Boolean) = if (dark) c(0x111213) else c(0xFFFFFF)

    @Composable
    override fun coinDecoration(shape: Shape): Modifier =
        Modifier.border(2.dp, if (FinioTheme.colors.isDark) c(0x3A3F47) else c(0x111213), shape)

    // ── progress: 20 segments, cut into the track and the fill alike ──
    override val progressTrackHeight: Dp get() = 12.dp

    @Composable
    override fun progressDecoration(): Modifier {
        val colors = FinioTheme.colors
        val cover = colors.card
        val track = if (colors.isDark) rgba(242, 236, 220, 0.14f) else rgba(17, 18, 19, 0.17f)
        return Modifier.drawWithContent {
            val cell = size.width / SEGMENTS
            val gap = 2.dp.toPx()
            // The muted track behind is covered, then the cells are laid down in the board's own tint.
            drawRect(cover)
            for (i in 0 until SEGMENTS) {
                drawRect(track, Offset(i * cell, 0f), Size((cell - gap).coerceAtLeast(0f), size.height))
            }
            drawContent()
            for (i in 0 until SEGMENTS) {
                drawRect(cover, Offset((i + 1) * cell - gap, 0f), Size(gap, size.height))
            }
        }
    }

    // ── hero: the departures board with rivets in the corners ──
    @Composable
    override fun heroContainer(shape: Shape): Modifier {
        val colors = FinioTheme.colors
        val dark = colors.isDark
        val shape4 = RoundedCornerShape(4.dp)
        val fill = if (!dark) c(0xF6C400) else if (colors.isAmoled) c(0x07090B) else c(0x15181C)
        val frame = if (dark) c(0x2C3037) else c(0x111213)
        val rivet = if (dark) rgba(242, 236, 220, 0.3f) else rgba(17, 18, 19, 0.55f)
        return Modifier
            .clip(shape4)
            .background(fill)
            .border(if (dark) 1.dp else 1.5.dp, frame, shape4)
            .drawBehind {
                val o = 7.dp.toPx()
                val r = 1.5.dp.toPx()
                drawCircle(rivet, r, Offset(o, o))
                drawCircle(rivet, r, Offset(size.width - o, o))
                drawCircle(rivet, r, Offset(o, size.height - o))
                drawCircle(rivet, r, Offset(size.width - o, size.height - o))
            }
    }

    override fun heroPadding(wide: Boolean) = PaddingValues(18.dp, 16.dp, 18.dp, 16.dp)
    override val replacesHeroBar: Boolean get() = true

    /** One flap tile per character; tile size comes from the container so long figures shrink. */
    @Composable
    override fun HeroFigure(text: String, style: TextStyle, color: Color, whole: Boolean, modifier: Modifier): Boolean {
        val dark = FinioTheme.colors.isDark
        val t1 = if (dark) c(0x2B2F36) else c(0x24272C)
        val t2 = if (dark) c(0x1F2227) else c(0x17191D)
        val chars = remember(text) { text.toList() }
        val n = chars.size.coerceAtLeast(1)
        val reduced = rememberReducedMotion()
        val total = (n - 1) * 90f + 470f + 300f
        val clock = remember(text) { Animatable(if (reduced) total else 0f) }
        LaunchedEffect(text, reduced) {
            if (!reduced) {
                clock.snapTo(0f)
                clock.animateTo(total, tween((total).toInt(), easing = LinearEasing))
            } else clock.snapTo(total)
        }
        val density = LocalDensity.current
        BoxWithConstraints(modifier.fillMaxWidth().semantics { contentDescription = text }) {
            val gap = 4.dp
            val tw = minOf(48.dp, (maxWidth - gap * (n - 1)) / n).coerceAtLeast(6.dp)
            val th = tw * 1.42f
            Row(Modifier.clearAndSetSemantics { }, horizontalArrangement = Arrangement.spacedBy(gap)) {
                chars.forEachIndexed { i, ch ->
                    val punct = ch == ',' || ch == '.'
                    val rupee = ch == '₹'
                    val w = if (punct) tw * 0.5f else tw
                    val fs = with(density) { (tw * 0.98f * (if (rupee) 0.8f else 1f)).toSp() }
                    Box(
                        Modifier
                            .size(w, th)
                            .clip(RoundedCornerShape(3.dp))
                            .drawBehind {
                                drawRect(t1, Offset.Zero, Size(size.width, size.height / 2))
                                drawRect(t2, Offset(0f, size.height / 2), Size(size.width, size.height / 2))
                                drawRect(Color.White.copy(alpha = 0.08f), Offset.Zero, Size(size.width, 1.dp.toPx()))
                            }
                            .drawWithContent {
                                drawContent()
                                val h2 = size.height / 2
                                val t = clock.value
                                val top = ((t - (i * 90f + 150f)) / 340f).coerceIn(0f, 1f)
                                val bottom = ((t - (i * 90f + 470f)) / 300f).coerceIn(0f, 1f)
                                if (top < 1f) {
                                    val hh = h2 * cos(top * Math.PI.toFloat() / 2f)
                                    drawRect(t1, Offset(0f, h2 - hh), Size(size.width, hh))
                                }
                                if (bottom < 1f) {
                                    val hh = h2 * cos(bottom * Math.PI.toFloat() / 2f)
                                    drawRect(t2, Offset(0f, h2), Size(size.width, hh))
                                }
                                drawRect(Color.Black.copy(alpha = 0.6f), Offset(0f, h2 - 0.5.dp.toPx()), Size(size.width, 1.dp.toPx()))
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            ch.toString(),
                            Modifier.padding(top = tw * 0.08f),
                            style = TextStyle(
                                fontFamily = OverpassMono,
                                fontWeight = FontWeight(700),
                                fontSize = fs,
                                lineHeight = fs,
                                fontFeatureSettings = "tnum",
                                textAlign = TextAlign.Center,
                                platformStyle = PlatformTextStyle(includeFontPadding = false),
                            ),
                            color = c(0xF4EFE0),
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                }
            }
        }
        return true
    }

    /** "Day n of N" platform clock, then the 20-segment strip with a tick for today. */
    @Composable
    override fun HeroFooter(elapsed: Float, spentPercent: Float, isOver: Boolean, daysLeft: Int) {
        val colors = FinioTheme.colors
        val dark = colors.isDark
        val spent = spentPercent.coerceIn(0f, 100f)
        val lit = ((spent / 100f) * SEGMENTS).roundToInt()
        val today = elapsed.coerceIn(0f, 1f)
        // Exact values come from LocalHeroPeriod; the estimate from days left and elapsed share is the fallback.
        val period = LocalHeroPeriod.current
        val total = period?.totalDays?.coerceAtLeast(1)
            ?: if (today < 0.999f) (daysLeft / (1f - today)).roundToInt().coerceAtLeast(daysLeft) else daysLeft
        val day = period?.dayOfPeriod?.coerceIn(1, total.coerceAtLeast(1)) ?: (total - daysLeft + 1).coerceIn(1, total.coerceAtLeast(1))
        val ink = if (dark) c(0xF2ECDC) else c(0x111213)
        val trackC = if (dark) rgba(242, 236, 220, 0.15f) else rgba(17, 18, 19, 0.2f)
        val fill = when {
            isOver -> if (dark) c(0xFF6A5C) else c(0xB3261E)
            spent >= 85f -> if (dark) c(0xFFB224) else c(0xF2A900)
            else -> ink
        }
        val clockInk = if (dark) c(0xFFB224) else c(0xF6C400)
        Column(Modifier.padding(top = 10.dp).clearAndSetSemantics { }) {
            Row(
                Modifier
                    .height(26.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (dark) c(0x1F2227) else c(0x17191D))
                    .padding(horizontal = 9.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Canvas(Modifier.size(13.dp)) {
                    val sw = 13.dp.toPx() / 24f * 2.2f
                    val s = size.width / 24f
                    drawCircle(clockInk, 9f * s, center, style = Stroke(sw))
                    drawLine(clockInk, Offset(12f * s, 7f * s), Offset(12f * s, 12f * s), sw)
                    drawLine(clockInk, Offset(12f * s, 12f * s), Offset(15f * s, 14f * s), sw)
                }
                Text(
                    "Day $day of $total",
                    style = TextStyle(fontFamily = OverpassMono, fontWeight = FontWeight(600), fontSize = 13.sp, fontFeatureSettings = "tnum"),
                    color = clockInk,
                    maxLines = 1,
                )
            }
            Canvas(Modifier.padding(top = 12.dp, bottom = 4.dp).fillMaxWidth().height(12.dp).widthIn(max = 512.dp)) {
                val gap = 2.dp.toPx()
                val cell = (size.width - gap * (SEGMENTS - 1)) / SEGMENTS
                for (i in 0 until SEGMENTS) {
                    drawRect(if (i < lit) fill else trackC, Offset(i * (cell + gap), 0f), Size(cell, size.height))
                }
                val x = size.width * today
                drawRect(ink, Offset(x - 1.dp.toPx(), -4.dp.toPx()), Size(2.dp.toPx(), size.height + 8.dp.toPx()))
            }
        }
    }

    // ── chrome: the blue shell strip, the active tab a lit yellow tile ──
    @Composable
    override fun TabBar(active: FinioTab?, onSelect: (FinioTab) -> Unit, modifier: Modifier) {
        val dark = FinioTheme.colors.isDark
        val blue = if (dark) c(0x17396A) else c(0x1C4A8A)
        val blueD = if (dark) c(0x102B52) else c(0x143A70)
        val mut = if (dark) c(0xB3C6E2) else c(0xC4D4EC)
        val yellow = if (dark) c(0xF8C900) else c(0xF6C400)
        Row(
            modifier
                .fillMaxWidth()
                .background(blue)
                .drawBehind { drawRect(blueD, Offset.Zero, Size(size.width, 2.dp.toPx())) }
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(start = 6.dp, end = 6.dp, top = 8.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FinioTab.entries.forEach { tab ->
                val selected = tab == active
                ThemedTab(
                    tab, selected, { onSelect(tab) }, Modifier.weight(1f),
                    tint = if (selected) c(0x111213) else mut,
                    shape = RoundedCornerShape(3.dp),
                    background = if (selected) yellow else Color.Transparent,
                )
            }
        }
    }
}

private const val SEGMENTS = 20

// ───────────────────────────── type & shape ─────────────────────────────

private fun variable(res: Int, weight: Int) = Font(
    resId = res,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

private val Overpass = FontFamily(
    variable(R.font.safar_overpass, 400), variable(R.font.safar_overpass, 500),
    variable(R.font.safar_overpass, 600), variable(R.font.safar_overpass, 700),
    variable(R.font.safar_overpass, 800), variable(R.font.safar_overpass, 900),
)
private val OverpassMono = FontFamily(
    variable(R.font.safar_overpass_mono, 400), variable(R.font.safar_overpass_mono, 500),
    variable(R.font.safar_overpass_mono, 600), variable(R.font.safar_overpass_mono, 700),
)

private val Scale = FinioTypeScale(
    Overpass, OverpassMono, Overpass,
    displayWeight = 700, displayTracking = -0.01f, pageTitleWeight = 900, screenTitleWeight = 800,
    headlineWeight = 800, moneyWeight = 600, dialogTitleWeight = 800, titleWeight = 800,
)

// base 5dp gives md = 4dp (the board radius), sm = 3dp.
private val Shapes = FinioShapeScale(base = 5.dp, full = RoundedCornerShape(4.dp))

// ───────────────────────────── palettes ─────────────────────────────

private fun c(rgb: Long) = Color(0xFF000000 or rgb)
private fun solid(rgb: Long): Brush = SolidColor(c(rgb))

private fun colorsOf(
    dark: Boolean,
    background: Long, foreground: Long, card: Long, popover: Long,
    primary: Long, primaryFg: Long, secondary: Long, muted: Long, mutedFg: Long,
    accent: Long, accentFg: Long, destructive: Long, destructiveFg: Long,
    positive: Long, warning: Long, border: Long, input: Long,
    charts: List<Long>,
    sidebar: Long, sidebarFg: Long, sidebarPrimary: Long, sidebarPrimaryFg: Long, sidebarAccent: Long, sidebarBorder: Long,
    warningBand: Long, warningBandFg: Long, warningBandAccent: Long,
    glass: Long, glassStrong: Long, glassBorder: Long, shadowTint: Color, scrim: Color,
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
    sidebar = c(sidebar), sidebarForeground = c(sidebarFg),
    sidebarPrimary = c(sidebarPrimary), sidebarPrimaryForeground = c(sidebarPrimaryFg),
    sidebarAccent = c(sidebarAccent), sidebarAccentForeground = c(sidebarFg),
    sidebarBorder = c(sidebarBorder), sidebarRing = c(sidebarPrimary),
    warningBand = c(warningBand), warningBandForeground = c(warningBandFg), warningBandAccent = c(warningBandAccent),
    engraving = Color.Transparent,
    glass = c(glass), glassStrong = c(glassStrong), glassBorder = c(glassBorder), glassHighlight = Color.Transparent,
    shadowTint = shadowTint, scrim = scrim,
)

/** Boards cast no soft shadows; only overlays get a hard 4dp offset. */
private fun shadowsOf(tint: Color, float: List<CssShadow>) = com.slowatcoding.finio.ui.theme.stock(tint).let { s ->
    FinioShadows(
        card = emptyList(), float = float, glowPrimary = emptyList(), glowSuccess = emptyList(), glowDanger = emptyList(),
        coin = emptyList(), tile = emptyList(), tabBar = emptyList(), noteChip = emptyList(),
        xs = s[0], sm = s[1], md = s[2], lg = s[3], xl = s[4],
    )
}

/** Account tiles are plain boards; what is owed (cards) is in red. */
private fun safarNotes(board: Long, ink: Long, red: Long) = Denomination.entries.associateWith { d ->
    NoteStyle(c(board), c(board), c(if (d == Denomination.Rs2000) red else ink))
}

private val Light = ThemePalette(
    colors = colorsOf(
        false, 0xD9D8D1, 0x111213, 0xEFEEE8, 0xF6F5F0, 0x1C4A8A, 0xFFFFFF, 0xE3E2DB, 0xE3E2DB, 0x55554E,
        0xE3E2DB, 0x111213, 0xB3261E, 0xFFFFFF, 0x0B6B3A, 0x8A5A00, 0xBDBCB3, 0x8A897F,
        charts = listOf(0x1C4A8A, 0xB87400, 0x0B6B3A, 0xB3261E, 0x6B5A00),
        sidebar = 0x1C4A8A, sidebarFg = 0xEEF3FA, sidebarPrimary = 0xF6C400, sidebarPrimaryFg = 0x111213,
        sidebarAccent = 0x143A70, sidebarBorder = 0x143A70,
        warningBand = 0x15171A, warningBandFg = 0xF4EFE0, warningBandAccent = 0xF2A900,
        glass = 0xEFEEE8, glassStrong = 0xD9D8D1, glassBorder = 0x111213,
        shadowTint = Color(17, 18, 19), scrim = rgba(17, 18, 19, 0.45f),
    ),
    brushes = FinioBrushes(
        gradPrimary = solid(0x1C4A8A), gradPrimarySoft = solid(0xE3E2DB),
        gradSuccess = solid(0x0B6B3A), gradSuccessSoft = solid(0xD3E6D9),
        gradDanger = solid(0xB3261E), gradDangerSoft = solid(0xF0D4D0),
        gradInfo = solid(0x1C4A8A), gradWarning = solid(0x8A5A00), gradSurface = solid(0xEFEEE8),
        paper = SolidColor(Color.Transparent), coin = solid(0x1C4A8A), register = { _ -> solid(0x0B6B3A) }, thread = solid(0x0B6B3A),
    ),
    shadows = shadowsOf(Color(17, 18, 19), listOf(CssShadow(x = 4.dp, y = 4.dp, color = rgba(17, 18, 19, 0.2f)))),
    spec = FinioSpec(VisualTheme.Safar, 1.5.dp, safarNotes(0xEFEEE8, 0x111213, 0xB3261E)),
)

private val Dark = ThemePalette(
    colors = colorsOf(
        true, 0x0C0E11, 0xF2ECDC, 0x15181C, 0x1B1E23, 0xF8C900, 0x111213, 0x1B1E23, 0x1B1E23, 0xA39D8F,
        0x22262C, 0xF2ECDC, 0xFF6A5C, 0x111213, 0x5BD08A, 0xFFB224, 0x2C3037, 0x4A4F58,
        charts = listOf(0xF8C900, 0x5BD08A, 0xFFB224, 0xFF6A5C, 0x8FB2E8),
        sidebar = 0x17396A, sidebarFg = 0xE6EEFA, sidebarPrimary = 0xF8C900, sidebarPrimaryFg = 0x111213,
        sidebarAccent = 0x102B52, sidebarBorder = 0x102B52,
        warningBand = 0x2A1F09, warningBandFg = 0xF2ECDC, warningBandAccent = 0xFFB224,
        glass = 0x15181C, glassStrong = 0x0C0E11, glassBorder = 0x2C3037,
        shadowTint = Color(0, 0, 0), scrim = rgba(0, 0, 0, 0.65f),
    ),
    brushes = FinioBrushes(
        gradPrimary = solid(0x2457A6), gradPrimarySoft = solid(0x1B1E23),
        gradSuccess = solid(0x0F6A3B), gradSuccessSoft = solid(0x12281B),
        gradDanger = solid(0xB3261E), gradDangerSoft = solid(0x2E1512),
        gradInfo = solid(0x2457A6), gradWarning = solid(0x8A5A00), gradSurface = solid(0x15181C),
        paper = SolidColor(Color.Transparent), coin = solid(0xF8C900), register = { _ -> solid(0x5BD08A) }, thread = solid(0x5BD08A),
    ),
    shadows = shadowsOf(Color(0, 0, 0), listOf(CssShadow(x = 4.dp, y = 4.dp, color = rgba(242, 236, 220, 0.2f)))),
    spec = FinioSpec(VisualTheme.Safar, 1.dp, safarNotes(0x15181C, 0xF2ECDC, 0xFF6A5C)),
)
