package com.slowatcoding.finio.ui.theme.kits

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
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
import kotlin.math.roundToInt

/**
 * Akhbaar — the morning broadsheet (web/src/themes/akhbaar.css). Newsprint and black ink with one
 * masthead red; dark is a hand-tuned night edition. No cards and no radii — structure is rules and
 * gutters. Oswald sets headlines and the banner figure, Newsreader everything people read. The
 * signature is the halftone pace chart under the hero ([HeroFooter]); the figure is inked in with a
 * left-to-right wipe once ([HeroFigure]).
 */
object AkhbaarKit : ThemeKit {
    override val visual = VisualTheme.Akhbaar
    override fun palette(dark: Boolean): ThemePalette = if (dark) Dark else Light
    override val type: FinioTypeScale get() = Scale
    override val shapes: FinioShapeScale get() = Shapes
    override fun coinInk(dark: Boolean) = c(if (dark) 0x17181B else 0xF2EFE6)
    override val coinShape get() = RectangleShape

    override val progressTrackHeight: Dp get() = 12.dp

    /** The track is a faint dot screen on paper with an ink baseline (the fill draws over it). */
    @Composable
    override fun progressDecoration(): Modifier {
        val colors = FinioTheme.colors
        val ink = colors.foreground
        val bg = colors.background
        val track = remember(ink) { DotScreen(ink.copy(alpha = 0.16f), 0.6f) }
        return Modifier.drawWithContent {
            drawRect(bg)
            drawRect(track.brush(density))
            drawContent()
            drawLine(ink, Offset(0f, size.height - 0.5.dp.toPx()), Offset(size.width, size.height - 0.5.dp.toPx()), 1.dp.toPx())
        }
    }

    // ── hero: the lead story ──
    override fun heroPadding(wide: Boolean) = PaddingValues(0.dp, 12.dp, 0.dp, 8.dp)
    override val replacesHeroBar: Boolean get() = true

    @Composable
    override fun HeroFigure(text: String, style: TextStyle, color: Color, whole: Boolean, modifier: Modifier): Boolean {
        val reduced = rememberReducedMotion()
        val reveal = remember { Animatable(if (reduced) 1f else 0f) }
        LaunchedEffect(Unit) { if (!reduced) reveal.animateTo(1f, tween(950, delayMillis = 120, easing = InkEase)) }
        val sized = style.copy(fontSize = 76.sp, lineHeight = 70.sp, fontWeight = FontWeight(600), letterSpacing = (-0.01).em)
        Text(
            text,
            modifier.drawWithContent {
                clipRect(right = size.width * reveal.value) { this@drawWithContent.drawContent() }
            },
            style = sized,
            color = color,
            maxLines = 1,
            softWrap = false,
        )
        return true
    }

    @Composable
    override fun HeroFooter(elapsed: Float, spentPercent: Float, isOver: Boolean, daysLeft: Int) {
        val colors = FinioTheme.colors
        val gone = (elapsed.coerceIn(0f, 1f) * 100f)
        val reduced = rememberReducedMotion()
        val fill = remember { Animatable(if (reduced) 1f else 0f) }
        // steps(14): the dots print in 14 strokes rather than a smooth slide.
        LaunchedEffect(Unit) { if (!reduced) fill.animateTo(1f, tween(900, delayMillis = 750)) }
        val stepped = (kotlin.math.ceil(fill.value * 14f) / 14f).coerceIn(0f, 1f)
        val red = colors.primary
        val dark = remember(colors.foreground) { DotScreen(colors.foreground, 1.4f) }
        val light = remember(colors.foreground) { DotScreen(colors.foreground.copy(alpha = 0.42f), 0.95f) }
        val track = remember(colors.foreground) { DotScreen(colors.foreground.copy(alpha = 0.16f), 0.6f) }
        Column(Modifier.padding(top = 10.dp).widthIn(max = 512.dp).clearAndSetSemantics { }) {
            listOf(Triple("Budget used", spentPercent, dark), Triple("Month gone", gone, light)).forEach { (label, value, screen) ->
                Row(Modifier.fillMaxWidth().padding(bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(label.uppercase(), Modifier.width(88.dp), style = LabelStyle, color = colors.foreground, maxLines = 1)
                    Canvas(Modifier.weight(1f).height(14.dp)) {
                        drawRect(track.brush(density))
                        val w = size.width * (value.coerceIn(0f, 100f) / 100f) * stepped
                        if (w > 0f) drawRect(screen.brush(density), size = size.copy(width = w))
                        drawLine(colors.foreground, Offset(0f, size.height - 0.5.dp.toPx()), Offset(size.width, size.height - 0.5.dp.toPx()), 1.dp.toPx())
                        val x = size.width * gone / 100f
                        drawLine(red, Offset(x, -4.dp.toPx()), Offset(x, size.height + 4.dp.toPx()), 2.dp.toPx())
                    }
                    Text(
                        "${value.roundToInt()}%",
                        Modifier.width(48.dp),
                        style = FigureStyle,
                        color = colors.foreground,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                    )
                }
            }
            Text(
                if (spentPercent <= gone) "Spending runs behind the calendar." else "Spending runs ahead of the calendar.",
                Modifier.padding(top = 2.dp),
                style = Scale.caption.copy(fontSize = 13.sp, fontStyle = FontStyle.Italic),
                color = colors.mutedForeground,
            )
        }
    }

    // ── chrome: a ruled strip of caps, the current page under a red bar ──
    @Composable
    override fun TabBar(active: FinioTab?, onSelect: (FinioTab) -> Unit, modifier: Modifier) {
        val colors = FinioTheme.colors
        val ink = colors.foreground
        val red = colors.primary
        Row(
            modifier
                .fillMaxWidth()
                .background(colors.background)
                .drawBehind {
                    drawLine(ink, Offset(0f, 0.5.dp.toPx()), Offset(size.width, 0.5.dp.toPx()), 1.dp.toPx())
                    drawLine(ink, Offset(0f, 3.5.dp.toPx()), Offset(size.width, 3.5.dp.toPx()), 1.dp.toPx())
                }
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FinioTab.entries.forEach { tab ->
                val selected = tab == active
                val tint = if (selected) red else colors.mutedForeground
                val icon = remember(tab) { tab.icon(1.6f) }
                Column(
                    Modifier
                        .weight(1f)
                        .drawBehind {
                            if (selected) {
                                val inset = size.width * 0.14f
                                drawRect(red, Offset(inset, 0f), size.copy(width = size.width - 2 * inset, height = 4.dp.toPx()))
                            }
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

// ───────────────────────────── type & shape ─────────────────────────────

private fun variable(res: Int, weight: Int) = Font(
    resId = res,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

private val Oswald = FontFamily(
    variable(R.font.akhbaar_oswald, 400), variable(R.font.akhbaar_oswald, 500),
    variable(R.font.akhbaar_oswald, 600), variable(R.font.akhbaar_oswald, 700),
)
private val Newsreader = FontFamily(
    variable(R.font.akhbaar_newsreader, 400), variable(R.font.akhbaar_newsreader, 500),
    variable(R.font.akhbaar_newsreader, 600), variable(R.font.akhbaar_newsreader, 700),
)

private val Scale = FinioTypeScale(
    Oswald, Oswald, Newsreader,
    displayWeight = 600, displayTracking = -0.01f, pageTitleWeight = 600, screenTitleWeight = 600,
    headlineWeight = 600, moneyWeight = 600, dialogTitleWeight = 600, titleWeight = 600,
)
private val Shapes = FinioShapeScale(base = 0.dp, full = RectangleShape)

private val LabelStyle = TextStyle(fontFamily = Oswald, fontWeight = FontWeight(500), fontSize = 11.5.sp, letterSpacing = 0.06.em)
private val FigureStyle = TextStyle(fontFamily = Oswald, fontWeight = FontWeight(600), fontSize = 17.sp, fontFeatureSettings = "tnum")
private val TabStyle = TextStyle(fontFamily = Oswald, fontWeight = FontWeight(500), fontSize = 10.sp, letterSpacing = 0.07.em)

/** `cubic-bezier(.2,.75,.3,1)` — the ink wipe. */
private val InkEase = CubicBezierEasing(0.2f, 0.75f, 0.3f, 1f)

/**
 * A halftone screen: one dot of [radiusDp] centred in every 4dp cell (CSS `radial-gradient(circle,
 * ink r, transparent) 0 0 / 4px 4px`). The tile is rasterised lazily per density.
 */
private class DotScreen(private val color: Color, private val radiusDp: Float) {
    private var cached: Pair<Float, Brush>? = null
    fun brush(density: Float): Brush {
        cached?.let { (d, b) -> if (d == density) return b }
        val px = (4f * density).roundToInt().coerceAtLeast(2)
        val bmp = ImageBitmap(px, px)
        androidx.compose.ui.graphics.Canvas(bmp).drawCircle(
            Offset(px / 2f, px / 2f),
            radiusDp * density,
            Paint().apply { this.color = this@DotScreen.color; isAntiAlias = true },
        )
        val b = ShaderBrush(ImageShader(bmp, TileMode.Repeated, TileMode.Repeated))
        cached = density to b
        return b
    }
}

// ───────────────────────────── palettes ─────────────────────────────

private fun c(rgb: Long) = Color(0xFF000000 or rgb)
private fun solid(rgb: Long): Brush = SolidColor(c(rgb))

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
    border = border, input = input, ring = c(primary),
    chart1 = c(charts[0]), chart2 = c(charts[1]), chart3 = c(charts[2]), chart4 = c(charts[3]), chart5 = c(charts[4]),
    sidebar = c(card), sidebarForeground = c(foreground),
    sidebarPrimary = c(primary), sidebarPrimaryForeground = c(primaryFg),
    sidebarAccent = c(accent), sidebarAccentForeground = c(accentFg),
    sidebarBorder = border, sidebarRing = c(primary),
    warningBand = c(warningBand), warningBandForeground = c(warningBandFg), warningBandAccent = c(warningBandAccent),
    engraving = Color.Transparent,
    glass = glass, glassStrong = glassStrong, glassBorder = glassBorder, glassHighlight = Color.Transparent,
    shadowTint = shadowTint, scrim = scrim,
)

private fun shadowsOf(tint: Color, float: List<CssShadow>) = stock(tint).let { s ->
    FinioShadows(
        card = emptyList(), float = float, glowPrimary = emptyList(), glowSuccess = emptyList(), glowDanger = emptyList(),
        coin = emptyList(), tile = emptyList(), tabBar = emptyList(), noteChip = emptyList(),
        xs = s[0], sm = s[1], md = s[2], lg = s[3], xl = s[4],
    )
}

/** Accounts are set like the markets table: paper, black ink, and what is owed (cards) in red. */
private fun akhbaarNotes(paper: Long, ink: Long, red: Long) = Denomination.entries.associateWith { d ->
    NoteStyle(c(paper), c(paper), c(if (d == Denomination.Rs2000) red else ink))
}

/** The overall budget prints through a dot screen. */
private fun halftone(ink: Long): (Float) -> Brush {
    val screen = DotScreen(c(ink), 1.4f)
    return { density -> screen.brush(density) }
}

private val Light = ThemePalette(
    colors = colorsOf(
        false, 0xF2EFE6, 0x151515, 0xF2EFE6, 0xF8F6EF, 0xC4161C, 0xF7F4EC, 0xE7E3D7, 0xE7E3D7, 0x57534A,
        0xE7E3D7, 0x151515, 0xC4161C, 0xF7F4EC, 0x1E6A3B, 0x8A5600,
        border = c(0xCBC5B5), input = c(0x8F8A7C),
        charts = listOf(0x151515, 0xC4161C, 0x1E6A3B, 0x8A5600, 0x6B675D),
        warningBand = 0xF6E4DC, warningBandFg = 0x4A0D0F, warningBandAccent = 0xC4161C,
        glass = Color.Transparent, glassStrong = c(0xF2EFE6), glassBorder = c(0xCBC5B5),
        shadowTint = Color(21, 21, 21), scrim = rgba(21, 21, 21, 0.35f),
    ),
    brushes = FinioBrushes(
        gradPrimary = solid(0x151515), gradPrimarySoft = solid(0xE7E3D7),
        gradSuccess = solid(0x1E6A3B), gradSuccessSoft = solid(0xE1ECDF),
        gradDanger = solid(0xC4161C), gradDangerSoft = solid(0xF6E4DC),
        gradInfo = solid(0x151515), gradWarning = solid(0x8A5600), gradSurface = solid(0xF2EFE6),
        paper = SolidColor(Color.Transparent), coin = solid(0x151515), register = halftone(0x151515), thread = solid(0x151515),
    ),
    shadows = shadowsOf(Color(21, 21, 21), listOf(CssShadow(x = 4.dp, y = 4.dp, color = rgba(21, 21, 21, 0.18f)))),
    spec = FinioSpec(VisualTheme.Akhbaar, 1.dp, akhbaarNotes(0xF2EFE6, 0x151515, 0xC4161C)),
)

private val Dark = ThemePalette(
    colors = colorsOf(
        true, 0x17181B, 0xECE8DC, 0x17181B, 0x202125, 0xFF6259, 0x17181B, 0x24252A, 0x24252A, 0xA8A397,
        0x26272C, 0xECE8DC, 0xFF6259, 0x17181B, 0x6FCF8F, 0xE8B04A,
        border = c(0x3A3B41), input = c(0x64656C),
        charts = listOf(0xECE8DC, 0xFF6259, 0x6FCF8F, 0xE8B04A, 0xA8A397),
        warningBand = 0x2C1918, warningBandFg = 0xFFE1DC, warningBandAccent = 0xFF6259,
        glass = Color.Transparent, glassStrong = c(0x17181B), glassBorder = c(0x3A3B41),
        shadowTint = Color(0, 0, 0), scrim = rgba(0, 0, 0, 0.6f),
    ),
    brushes = FinioBrushes(
        // Cream ink can't carry white text, so the night edition's filled action is the masthead red.
        gradPrimary = solid(0xB3141A), gradPrimarySoft = solid(0x24252A),
        gradSuccess = solid(0x1E6A3B), gradSuccessSoft = solid(0x16261C),
        gradDanger = solid(0xB3141A), gradDangerSoft = solid(0x2C1918),
        gradInfo = solid(0xB3141A), gradWarning = solid(0x8A5600), gradSurface = solid(0x17181B),
        paper = SolidColor(Color.Transparent), coin = solid(0xECE8DC), register = halftone(0xECE8DC), thread = solid(0xECE8DC),
    ),
    shadows = shadowsOf(Color(0, 0, 0), listOf(CssShadow(x = 4.dp, y = 4.dp, color = rgba(236, 232, 220, 0.14f)))),
    spec = FinioSpec(VisualTheme.Akhbaar, 1.dp, akhbaarNotes(0x17181B, 0xECE8DC, 0xFF6259)),
)
