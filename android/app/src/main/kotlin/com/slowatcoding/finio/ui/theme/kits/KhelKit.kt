package com.slowatcoding.finio.ui.theme.kits

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
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
import com.slowatcoding.finio.ui.theme.stock
import java.util.Calendar
import kotlin.math.roundToInt

/**
 * Khel — the 8-bit handheld (web/src/themes/khel.css). A four-shade pea-green LCD by day, a CRT
 * night palette after dark. Nothing is round; panels are RPG text boxes (a stepped outer line with
 * notched corners and an inner line). Pixelify Sans for words, VT323 for the hero figure and HUD
 * figures. Signature: the overall budget as a 20-segment HP bar with a pixel heart (red near the
 * limit, blinking from 90%), a pixel coin for add that idles with a 4-frame spin.
 */
object KhelKit : ThemeKit {
    override val visual = VisualTheme.Khel

    override fun palette(dark: Boolean): ThemePalette = if (dark) KhelDark else KhelLight

    override val type: FinioTypeScale get() = KhelTypeScale
    override val shapes: FinioShapeScale get() = KhelShapeScale

    // ── the coin ──
    override fun coinInk(dark: Boolean) = if (dark) c(0x5A3A00) else c(0x0F380F)
    override val coinShape: Shape get() = PixelCoinShape
    override val fabBottom get() = 96.dp

    /** Draws the pixel disc, shine and ring itself (the coin brush is clear) so the spin turns all of it. */
    @Composable
    override fun coinDecoration(shape: Shape): Modifier {
        val dark = FinioTheme.colors.isDark
        val disc = if (dark) c(0xFFCF3F) else c(0x9BBC0F)
        val ring = coinInk(dark)
        val shine = if (dark) c(0xFFF6C8) else c(0xE0EFBE)
        val spin = rememberSpin()
        return Modifier
            .graphicsLayer {
                // The sidebar's small brand coin stays still; only the FAB idles.
                scaleX = if (size.width >= 48.dp.toPx()) spin.value else 1f
            }
            .drawBehind { drawCoin(disc, ring, shine) }
    }

    // ── progress: 16dp tracks cut into 20 segments, a 2dp pixel outline ──
    override val progressTrackHeight get() = 16.dp

    @Composable
    override fun progressDecoration(): Modifier {
        val line = lineColor()
        val gap = FinioTheme.colors.card
        return Modifier
            .drawWithContent {
                drawContent()
                val px = 2.dp.toPx()
                val inner = size.width - px * 2
                for (i in 1 until 20) {
                    val x = px + inner * i / 20f - px / 2
                    drawRect(gap, Offset(x, px), Size(px, size.height - px * 2))
                }
                drawPixelOutline(line, px)
            }
            .padding(2.dp)
    }

    // ── the hero: a text box ──
    @Composable
    override fun heroContainer(shape: Shape): Modifier {
        val colors = FinioTheme.colors
        val line = lineColor()
        val line2 = line2Color()
        return Modifier.drawBehind {
            val b = 3.dp.toPx()
            drawRect(colors.card, Offset(b, b), Size(size.width - b * 2, size.height - b * 2))
            drawPixelOutline(line, b)
            val i = 6.dp.toPx()
            val w = 2.dp.toPx()
            drawRect(line2, Offset(i, i), Size(size.width - i * 2, w))
            drawRect(line2, Offset(i, size.height - i - w), Size(size.width - i * 2, w))
            drawRect(line2, Offset(i, i), Size(w, size.height - i * 2))
            drawRect(line2, Offset(size.width - i - w, i), Size(w, size.height - i * 2))
        }
    }

    override fun heroPadding(wide: Boolean): PaddingValues =
        if (wide) PaddingValues(32.dp, 26.dp, 32.dp, 26.dp) else PaddingValues(24.dp, 22.dp, 24.dp, 22.dp)

    /** VT323 at a size where its 0 can never be read as a 6. */
    @Composable
    override fun HeroFigure(text: String, style: TextStyle, color: Color, whole: Boolean, modifier: Modifier): Boolean {
        val size = style.fontSize * 1.55f
        Text(
            text,
            modifier,
            style = style.copy(fontFamily = VT323, fontWeight = FontWeight.Normal, fontSize = size, lineHeight = size, letterSpacing = 0.em),
            color = color,
            maxLines = 1,
            softWrap = false,
        )
        return true
    }

    override val replacesHeroBar: Boolean get() = true

    /** The HP bar (heart + 20 segments) and the "Day n/N" HUD tag. */
    @Composable
    override fun HeroFooter(elapsed: Float, spentPercent: Float, isOver: Boolean, daysLeft: Int) {
        val colors = FinioTheme.colors
        val danger = isOver || spentPercent >= 85f
        val blinking = isOver || spentPercent >= 90f
        val blink = rememberBlink(blinking)
        val line = lineColor()
        val fill = if (danger) colors.destructive else if (colors.isDark) c(0x7BE36F) else c(0x0F380F)
        val empty = emptyColor()
        val card = colors.card
        Row(
            Modifier.padding(top = 12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Canvas(Modifier.size(16.dp).clearAndSetSemantics { }) {
                if (blink.value) drawPixels(HEART, if (danger) colors.destructive else colors.foreground)
            }
            Canvas(
                Modifier
                    .weight(1f)
                    .height(20.dp)
                    .semantics { progressBarRangeInfo = ProgressBarRangeInfo(spentPercent.coerceIn(0f, 100f), 0f..100f) },
            ) {
                val px = 2.dp.toPx()
                drawRect(card)
                drawPixelOutline(line, px)
                val inset = px * 2
                val w = size.width - inset * 2
                val h = size.height - inset * 2
                val segW = w / 20f
                val on = (spentPercent.coerceIn(0f, 100f) / 5f).roundToInt()
                for (i in 0 until 20) {
                    val x = inset + segW * i
                    val segColor = if (i < on) fill else empty
                    drawRect(segColor, Offset(x, inset), Size(segW - px, h))
                    if (danger && i >= on) {
                        // the danger hatch on the empty segments
                        var s = -h
                        while (s < segW) {
                            drawLine(colors.destructive, Offset(x + s, inset + h), Offset(x + s + h, inset), px * 0.8f)
                            s += px * 2.5f
                        }
                    }
                }
            }
        }
        val hud = remember(elapsed, daysLeft) { dayOfPeriod(elapsed, daysLeft) }
        Row(Modifier.padding(top = 8.dp).fillMaxWidth().clearAndSetSemantics { }, verticalAlignment = Alignment.CenterVertically) {
            if (hud != null) {
                Text(
                    "DAY ${hud.first}/${hud.second}",
                    Modifier.background(colors.foreground).padding(horizontal = 7.dp, vertical = 4.dp),
                    style = HudStyle,
                    color = colors.background,
                )
            }
            Spacer(Modifier.weight(1f))
            Text("${spentPercent.roundToInt()}% used", style = HudFigureStyle, color = if (danger) colors.destructive else colors.foreground)
        }
    }

    // ── chrome: a handheld's button strip ──
    @Composable
    override fun TabBar(active: FinioTab?, onSelect: (FinioTab) -> Unit, modifier: Modifier) {
        val colors = FinioTheme.colors
        val line = lineColor()
        val line2 = line2Color()
        Row(
            modifier
                .fillMaxWidth()
                .background(colors.card)
                .drawBehind {
                    drawRect(line2, Offset.Zero, Size(size.width, 2.dp.toPx()))
                    drawRect(line, Offset(0f, 5.dp.toPx()), Size(size.width, 3.dp.toPx()))
                }
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(start = 4.dp, end = 4.dp, top = 14.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FinioTab.entries.forEach { tab ->
                val selected = tab == active
                ThemedTab(
                    tab, selected, { onSelect(tab) }, Modifier.weight(1f),
                    tint = if (selected) colors.background else colors.mutedForeground,
                    shape = RectangleShape,
                    background = if (selected) colors.foreground else Color.Transparent,
                )
            }
        }
    }
}

// ───────────────────────────── drawing ─────────────────────────────

@Composable
private fun lineColor() = if (FinioTheme.colors.isDark) c(0xF6EDD3) else c(0x0F380F)

@Composable
private fun line2Color() = if (FinioTheme.colors.isDark) c(0x4F6BD8) else c(0x306230)

@Composable
private fun emptyColor() = if (FinioTheme.colors.isDark) c(0x2C2760) else c(0x9BBC0F)

/** A stepped outline [w] thick with notched corners — the four sides, never the corner squares. */
private fun DrawScope.drawPixelOutline(color: Color, w: Float) {
    drawRect(color, Offset(w, 0f), Size(size.width - w * 2, w))
    drawRect(color, Offset(w, size.height - w), Size(size.width - w * 2, w))
    drawRect(color, Offset(0f, w), Size(w, size.height - w * 2))
    drawRect(color, Offset(size.width - w, w), Size(w, size.height - w * 2))
}

/** Rows of '#' drawn as square pixels filling the canvas. */
private fun DrawScope.drawPixels(rows: List<String>, color: Color) {
    val p = size.width / rows[0].length
    rows.forEachIndexed { y, row ->
        row.forEachIndexed { x, ch -> if (ch == '#') drawRect(color, Offset(x * p, y * p), Size(p, p)) }
    }
}

private fun DrawScope.drawCoin(disc: Color, ring: Color, shine: Color) {
    drawPixels(COIN_DISC, disc)
    drawPixels(COIN_SHINE, shine)
    drawPixels(COIN_RING, ring)
}

private val HEART = listOf(
    "........", ".##.##..", "#######.", "#######.", ".#####..", "..###...", "...#....", "........",
)
private val COIN_DISC = listOf(
    "....######....", "..##########..", ".############.", ".############.",
    "##############", "##############", "##############", "##############", "##############", "##############",
    ".############.", ".############.", "..##########..", "....######....",
)
private val COIN_RING = listOf(
    "....######....", "..##......##..", ".#..........#.", ".#..........#.",
    "#............#", "#............#", "#............#", "#............#", "#............#", "#............#",
    ".#..........#.", ".#..........#.", "..##......##..", "....######....",
)
private val COIN_SHINE = listOf(
    "..............", "..............", "...##.........", "..#...........", "..#...........", "..............",
    "..............", "..............", "..............", "..............", "..............", "..............",
    "..............", "..............",
)

/** The coin's stepped silhouette on its 14-pixel grid. */
private val PixelCoinShape = GenericShape { size, _ ->
    val p = size.width / 14f
    val spans = COIN_DISC.map { row -> row.indexOf('#') to row.lastIndexOf('#') + 1 }
    moveTo(spans[0].first * p, 0f)
    spans.forEachIndexed { y, (l, _) ->
        lineTo(l * p, y * p)
        lineTo(l * p, (y + 1) * p)
    }
    for (y in spans.indices.reversed()) {
        val r = spans[y].second
        lineTo(r * p, (y + 1) * p)
        lineTo(r * p, y * p)
    }
    close()
}

// ───────────────────────────── motion ─────────────────────────────

/** The coin's 4-frame idle spin (web `khel-spin`: a quick turn every 6s, steps). Still under reduced motion. */
@Composable
private fun rememberSpin(): State<Float> {
    if (rememberReducedMotion()) return remember { mutableFloatStateOf(1f) }
    val t by rememberInfiniteTransition(label = "khel-spin").animateFloat(
        0f, 1f, infiniteRepeatable(tween(6000, easing = LinearEasing)), label = "t",
    )
    return remember {
        object : State<Float> {
            override val value: Float
                get() = when {
                    t < 0.86f -> 1f
                    t < 0.89f -> 0.64f
                    t < 0.92f -> 0.14f
                    t < 0.95f -> 0.64f
                    else -> 1f
                }
        }
    }
}

/** On for 70% of each 0.8s beat (web `khel-blink`); always on when not [active] or under reduced motion. */
@Composable
private fun rememberBlink(active: Boolean): State<Boolean> {
    val reduced = rememberReducedMotion()
    if (!active || reduced) return remember { object : State<Boolean> { override val value = true } }
    val t by rememberInfiniteTransition(label = "khel-blink").animateFloat(
        0f, 1f, infiniteRepeatable(tween(800, easing = LinearEasing)), label = "t",
    )
    return remember { object : State<Boolean> { override val value: Boolean get() = t < 0.7f } }
}

/**
 * Day n of N in the budget period, from the hero's own `elapsed` and `daysLeft` (the period runs
 * start-of-day to end-of-day, so `elapsed × N = N − daysLeft + today's fraction`). Null when it
 * doesn't resolve to a whole, in-range day.
 */
private fun dayOfPeriod(elapsed: Float, daysLeft: Int): Pair<Int, Int>? {
    if (elapsed <= 0f || elapsed >= 1f || daysLeft < 1) return null
    val now = Calendar.getInstance()
    val fraction = (now.get(Calendar.HOUR_OF_DAY) * 3600 + now.get(Calendar.MINUTE) * 60 + now.get(Calendar.SECOND)) / 86_400f
    val total = ((daysLeft - fraction) / (1f - elapsed)).roundToInt()
    val day = total - daysLeft + 1
    return if (total in 1..366 && day in 1..total) day to total else null
}

// ───────────────────────────── type & shape ─────────────────────────────

private fun pixelify(weight: Int) = Font(
    resId = R.font.khel_pixelify_sans,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

/** Pixelify Sans — titles, labels, tabs, rows. */
private val PixelifySans = FontFamily(pixelify(400), pixelify(500), pixelify(600), pixelify(700))

/** VT323 — the figure face (only ever at ≥20sp; below that its 0 reads like a 6). */
private val VT323 = FontFamily(Font(R.font.khel_vt323, FontWeight.Normal))

/** Silkscreen — tiny uppercase HUD tags only. */
private val Silkscreen = FontFamily(Font(R.font.khel_silkscreen, FontWeight.Normal))

private val HudStyle = TextStyle(fontFamily = Silkscreen, fontSize = 11.sp, lineHeight = 11.sp)
private val HudFigureStyle = TextStyle(fontFamily = VT323, fontSize = 22.sp, lineHeight = 22.sp)

/**
 * Money stays Pixelify Sans here: the shared scale sets `money` at 18sp, too small for VT323. The
 * hero figure and the HUD are VT323 (see [KhelKit.HeroFigure]).
 */
private val KhelTypeScale = FinioTypeScale(
    PixelifySans, PixelifySans, PixelifySans,
    displayWeight = 600, displayTracking = 0f, pageTitleWeight = 700, screenTitleWeight = 700,
    headlineWeight = 700, moneyWeight = 600, dialogTitleWeight = 700, titleWeight = 700,
)

/** Nothing is round. */
private val KhelShapeScale = FinioShapeScale(base = 0.dp, full = RectangleShape)

// ───────────────────────────── palettes ─────────────────────────────

private fun c(rgb: Long) = Color(0xFF000000 or rgb)
private fun solid(rgb: Long) = SolidColor(c(rgb))

private fun colorsOf(
    dark: Boolean,
    background: Long, foreground: Long, card: Long, popover: Long,
    primary: Long, primaryFg: Long, secondary: Long, muted: Long, mutedFg: Long,
    accent: Long, accentFg: Long, destructive: Long, destructiveFg: Long,
    positive: Long, warning: Long, border: Color, input: Color,
    charts: List<Long>,
    warningBand: Long, warningBandFg: Long, warningBandAccent: Long,
    glass: Color, glassStrong: Color, glassBorder: Color, shadowTint: Color, scrim: Color,
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
    border = border, input = input, ring = input,
    chart1 = c(charts[0]), chart2 = c(charts[1]), chart3 = c(charts[2]), chart4 = c(charts[3]), chart5 = c(charts[4]),
    sidebar = c(card), sidebarForeground = c(foreground),
    sidebarPrimary = c(primary), sidebarPrimaryForeground = c(primaryFg),
    sidebarAccent = c(accent), sidebarAccentForeground = c(accentFg),
    sidebarBorder = c(foreground), sidebarRing = input,
    warningBand = c(warningBand), warningBandForeground = c(warningBandFg), warningBandAccent = c(warningBandAccent),
    engraving = Color.Transparent,
    glass = glass, glassStrong = glassStrong, glassBorder = glassBorder, glassHighlight = Color.Transparent,
    shadowTint = shadowTint, scrim = scrim,
)

private fun shadowsOf(float: List<CssShadow>, tint: Color) = stock(tint).let { s ->
    FinioShadows(
        card = emptyList(), float = float, glowPrimary = emptyList(), glowSuccess = emptyList(), glowDanger = emptyList(),
        coin = emptyList(), tile = emptyList(), tabBar = emptyList(), noteChip = emptyList(),
        xs = s[0], sm = s[1], md = s[2], lg = s[3], xl = s[4],
    )
}

private fun brushesOf(
    primary: Long, primarySoft: Long, success: Long, successSoft: Long, danger: Long, dangerSoft: Long,
    info: Long, warning: Long, surface: Long, fill: Long,
) = FinioBrushes(
    gradPrimary = solid(primary), gradPrimarySoft = solid(primarySoft), gradSuccess = solid(success),
    gradSuccessSoft = solid(successSoft), gradDanger = solid(danger), gradDangerSoft = solid(dangerSoft),
    gradInfo = solid(info), gradWarning = solid(warning), gradSurface = solid(surface),
    paper = SolidColor(Color.Transparent),
    // The coin draws its own pixels (KhelKit.coinDecoration) so the spin turns the whole disc.
    coin = SolidColor(Color.Transparent),
    register = { _ -> solid(fill) }, thread = solid(fill),
)

private fun notes(vararg tints: Triple<Denomination, Long, Long>) =
    tints.associate { (d, bg, ink) -> d to NoteStyle(c(bg), c(bg), c(ink)) }

private val KhelLight = ThemePalette(
    colors = colorsOf(
        false, 0xE0EFBE, 0x0F380F, 0xE0EFBE, 0xE0EFBE, 0x0F380F, 0xE0EFBE, 0xCFE39A, 0xCFE39A, 0x306230,
        0x9BBC0F, 0x0F380F, 0xA01641, 0xFFFFFF, 0x306230, 0x5C5A00,
        border = c(0x9BBC0F), input = c(0x306230),
        charts = listOf(0x0F380F, 0x8BAC0F, 0xA01641, 0x4F6BD8, 0xB07A00),
        warningBand = 0x0F380F, warningBandFg = 0xE0EFBE, warningBandAccent = 0x9BBC0F,
        glass = c(0xE0EFBE), glassStrong = c(0xE0EFBE), glassBorder = c(0x0F380F),
        shadowTint = Color(15, 56, 15), scrim = rgba(15, 56, 15, 0.45f),
    ),
    brushes = brushesOf(0x0F380F, 0xCFE39A, 0x306230, 0xCFE39A, 0xA01641, 0xECC6CC, 0x306230, 0x7A6A00, 0xE0EFBE, 0x0F380F),
    shadows = shadowsOf(listOf(CssShadow(y = 6.dp, color = rgba(15, 56, 15, 0.35f))), Color(15, 56, 15)),
    spec = FinioSpec(
        VisualTheme.Khel, 2.dp,
        notes(
            Triple(Denomination.Rs100, 0xCFE39AL, 0x0F380FL), Triple(Denomination.Rs500, 0xBCD77AL, 0x0F380FL),
            Triple(Denomination.Rs2000, 0xECC6CCL, 0x6B0F2CL), Triple(Denomination.Rs200, 0xE2E6A0L, 0x3D3A00L),
            Triple(Denomination.Rs10, 0xD6E8A8L, 0x0F380FL), Triple(Denomination.Rs50, 0xC4DFA0L, 0x0F380FL),
            Triple(Denomination.Rs20, 0xB5D06AL, 0x0F380FL),
        ),
    ),
)

private val KhelDark = ThemePalette(
    colors = colorsOf(
        true, 0x0D0B1E, 0xF6EDD3, 0x18143A, 0x18143A, 0xFFCF3F, 0x0D0B1E, 0x2C2760, 0x2C2760, 0xBDB5E2,
        0x3A4FB8, 0xF6EDD3, 0xFF5C72, 0x0D0B1E, 0x7BE36F, 0xFFCF3F,
        border = c(0x3A3478), input = c(0x4F6BD8),
        charts = listOf(0x7BE36F, 0xFFCF3F, 0xFF5C72, 0x6F8CFF, 0xC58BFF),
        warningBand = 0x2A1430, warningBandFg = 0xF6EDD3, warningBandAccent = 0xFF5C72,
        glass = c(0x18143A), glassStrong = c(0x18143A), glassBorder = c(0xF6EDD3),
        shadowTint = Color(0, 0, 0), scrim = rgba(5, 4, 14, 0.7f),
    ),
    brushes = brushesOf(0x3A4FB8, 0x2C2760, 0x2B6E27, 0x1C3A2A, 0xB02A44, 0x3A1630, 0x3A4FB8, 0xA8780A, 0x18143A, 0x7BE36F),
    shadows = shadowsOf(listOf(CssShadow(y = 6.dp, color = rgba(0, 0, 0, 0.6f))), Color(0, 0, 0)),
    spec = FinioSpec(
        VisualTheme.Khel, 2.dp,
        notes(
            Triple(Denomination.Rs100, 0x2C2760L, 0xF6EDD3L), Triple(Denomination.Rs500, 0x1C3A2AL, 0xC4F7B8L),
            Triple(Denomination.Rs2000, 0x3A1630L, 0xFFC8D0L), Triple(Denomination.Rs200, 0x3A3010L, 0xFFE49AL),
            Triple(Denomination.Rs10, 0x2A2350L, 0xE8DCFFL), Triple(Denomination.Rs50, 0x16304AL, 0xC4E2FFL),
            Triple(Denomination.Rs20, 0x263A16L, 0xDCF5A8L),
        ),
    ),
)
