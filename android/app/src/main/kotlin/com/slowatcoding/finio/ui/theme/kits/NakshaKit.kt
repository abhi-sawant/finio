package com.slowatcoding.finio.ui.theme.kits

import android.content.res.Resources
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.CircleShape
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
import com.slowatcoding.finio.ui.theme.rgba
import com.slowatcoding.finio.ui.theme.stock
import kotlin.math.roundToInt

/**
 * Naksha — the architect's drawing sheet (web/src/themes/naksha.css). Cyanotype Prussian blue in
 * the dark, white tracing paper with blue ink in the light. Nothing is filled and nothing is
 * round: panels are 1dp drawn rectangles, a selection is a 45-degree hatch, a warning is red
 * pencil. Azeret Mono letters figures, Sofia Sans sets names. The signature is the dimensioned
 * budget bar ([HeroFooter] → [NakshaHero]) which replaces the shared hero bar.
 *
 * Approximations: the drafting grid behind every screen and the corner crop marks inside panels
 * are not drawn (they need shared surfaces); Azeret/Sofia have no rupee sign, and a Compose
 * FontFamily cannot fall back per glyph to a named face, so the rupee sign falls to the system font.
 */
object NakshaKit : ThemeKit {
    override val visual = VisualTheme.Naksha
    override fun palette(dark: Boolean): ThemePalette = if (dark) Dark else Light
    override val type: FinioTypeScale get() = Scale
    override val shapes: FinioShapeScale get() = Shapes
    override fun coinInk(dark: Boolean) = c(if (dark) 0x12397A else 0xFFFFFF)
    override val coinShape: Shape get() = CircleShape

    /** A drafted circle ringed like a stamp: a 1dp line 3dp outside the coin. */
    @Composable
    override fun coinDecoration(shape: Shape): Modifier {
        val colors = FinioTheme.colors
        val ln = if (colors.isDark) Color(0xFFDBE8FB) else Color(0xFF1B4AA5)
        return Modifier.drawWithContent {
            drawContent()
            drawCircle(ln, radius = size.minDimension / 2f + 3.5.dp.toPx(), style = Stroke(1.dp.toPx()))
        }
    }

    override val progressTrackHeight: Dp get() = 14.dp

    /** An outlined track; the fill (the palette's hatch brushes) draws inside it. */
    @Composable
    override fun progressDecoration(): Modifier {
        val ln = if (FinioTheme.colors.isDark) Color(0xFFDBE8FB) else Color(0xFF1B4AA5)
        return Modifier.drawWithContent {
            drawContent()
            val w = 1.dp.toPx()
            drawRect(ln, Offset(w / 2f, w / 2f), Size(size.width - w, size.height - w), style = Stroke(w))
        }
    }

    // ── hero: the main view of the sheet — no surface, the dimensioned bar beneath ──
    override fun heroPadding(wide: Boolean) = PaddingValues(0.dp, 4.dp, 0.dp, 0.dp)
    override val replacesHeroBar: Boolean get() = true

    @Composable
    override fun HeroFooter(elapsed: Float, spentPercent: Float, isOver: Boolean, daysLeft: Int) =
        NakshaHero.Footer(elapsed, spentPercent, isOver, daysLeft)

    // ── chrome: a ruled strip of cells, the current sheet hatched ──
    @Composable
    override fun TabBar(active: FinioTab?, onSelect: (FinioTab) -> Unit, modifier: Modifier) {
        val colors = FinioTheme.colors
        val dark = colors.isDark
        val ln = if (dark) Color(0xFFDBE8FB) else Color(0xFF1B4AA5)
        val ln2 = ln.copy(alpha = if (dark) 0.4f else 0.34f)
        val hatch = remember(dark) { HatchBrush(if (dark) rgba(255, 233, 138, 0.55f) else rgba(27, 74, 165, 0.3f), 1f) }
        val selBg = if (dark) Color.Transparent else c(0xFFF1A0)
        val selInk = if (dark) c(0xFFE98A) else colors.foreground
        Row(
            modifier
                .fillMaxWidth()
                .background(colors.background)
                .drawBehind { drawLine(ln, Offset(0f, 0.5.dp.toPx()), Offset(size.width, 0.5.dp.toPx()), 1.dp.toPx()) }
                .windowInsetsPadding(WindowInsets.navigationBars),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FinioTab.entries.forEachIndexed { index, tab ->
                val selected = tab == active
                val tint = if (selected) selInk else colors.mutedForeground
                val icon = remember(tab) { tab.icon(1.5f) }
                Column(
                    Modifier
                        .weight(1f)
                        .drawBehind {
                            if (selected) {
                                drawRect(selBg)
                                drawRect(hatch)
                            }
                            if (index > 0) drawLine(ln2, Offset(0.5.dp.toPx(), 0f), Offset(0.5.dp.toPx(), size.height), 1.dp.toPx())
                        }
                        .tabClickable(selected) { onSelect(tab) }
                        .padding(top = 10.dp, bottom = 8.dp),
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

internal object NakshaFonts {
    /** Azeret Mono; no rupee sign, so that glyph resolves through the system fallback. */
    val Mono = FontFamily(
        variable(R.font.naksha_azeret_mono, 400), variable(R.font.naksha_azeret_mono, 500),
        variable(R.font.naksha_azeret_mono, 600), variable(R.font.naksha_azeret_mono, 700),
    )
    val Sofia = FontFamily(
        variable(R.font.naksha_sofia_sans, 400), variable(R.font.naksha_sofia_sans, 500),
        variable(R.font.naksha_sofia_sans, 600), variable(R.font.naksha_sofia_sans, 700),
    )
}

private val Scale = FinioTypeScale(
    NakshaFonts.Sofia, NakshaFonts.Mono, NakshaFonts.Sofia,
    displayWeight = 600, displayTracking = -0.04f, pageTitleWeight = 600, screenTitleWeight = 600,
    headlineWeight = 600, moneyWeight = 600, dialogTitleWeight = 600, titleWeight = 600,
)
private val Shapes = FinioShapeScale(base = 0.dp, full = RectangleShape)

private val TabStyle = TextStyle(fontFamily = NakshaFonts.Mono, fontWeight = FontWeight(500), fontSize = 9.sp, letterSpacing = 0.05.em)

// ───────────────────────────── hatch ─────────────────────────────

/**
 * `repeating-linear-gradient(45deg, ink 0 1px, transparent 1px 5px)`: one diagonal per square tile
 * of 5*sqrt(2) dp gives stripes 5dp apart. Brushes are drawn at the system density so a palette
 * (built once, without a density) can hold them.
 */
internal class HatchBrush(private val color: Color, private val strokeDp: Float) : ShaderBrush() {
    private var cache: Pair<Float, Shader>? = null

    override fun createShader(size: Size): Shader {
        val density = Resources.getSystem().displayMetrics.density
        cache?.let { (d, s) -> if (d == density) return s }
        val px = (7.07f * density).roundToInt().coerceAtLeast(4)
        val bmp = ImageBitmap(px, px)
        androidx.compose.ui.graphics.Canvas(bmp).drawLine(
            Offset(-1f, -1f), Offset(px + 1f, px + 1f),
            Paint().apply {
                this.color = this@HatchBrush.color
                style = PaintingStyle.Stroke
                strokeWidth = strokeDp * density
                isAntiAlias = true
            },
        )
        val shader = ImageShader(bmp, TileMode.Repeated, TileMode.Repeated)
        cache = density to shader
        return shader
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
    positive: Long, warning: Long, border: Color, input: Long,
    charts: List<Long>,
    warningBand: Long, warningBandFg: Long, warningBandAccent: Long,
    glassStrong: Long, shadowTint: Color, scrim: Color,
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
    border = border, input = c(input), ring = c(primary),
    chart1 = c(charts[0]), chart2 = c(charts[1]), chart3 = c(charts[2]), chart4 = c(charts[3]), chart5 = c(charts[4]),
    sidebar = c(card), sidebarForeground = c(foreground),
    sidebarPrimary = c(primary), sidebarPrimaryForeground = c(primaryFg),
    sidebarAccent = c(accent), sidebarAccentForeground = c(accentFg),
    sidebarBorder = c(input), sidebarRing = c(primary),
    warningBand = c(warningBand), warningBandForeground = c(warningBandFg), warningBandAccent = c(warningBandAccent),
    engraving = Color.Transparent,
    glass = Color.Transparent, glassStrong = c(glassStrong), glassBorder = border, glassHighlight = Color.Transparent,
    shadowTint = shadowTint, scrim = scrim,
)

private fun flatShadows(tint: Color) = stock(tint).let { s ->
    FinioShadows(
        card = emptyList(), float = emptyList(), glowPrimary = emptyList(), glowSuccess = emptyList(), glowDanger = emptyList(),
        coin = emptyList(), tile = emptyList(), tabBar = emptyList(), noteChip = emptyList(),
        xs = s[0], sm = s[1], md = s[2], lg = s[3], xl = s[4],
    )
}

/** No tint on any account tile: the sheet shows through; what is owed (cards) is red pencil. */
private fun nakshaNotes(paper: Long, ink: Long, red: Long) = Denomination.entries.associateWith { d ->
    NoteStyle(c(paper), c(paper), c(if (d == Denomination.Rs2000) red else ink))
}

private val Light = run {
    val hatch = HatchBrush(rgba(27, 74, 165, 1f), 1f)
    ThemePalette(
        colors = colorsOf(
            false, 0xF6F9FC, 0x0D2C6B, 0xF6F9FC, 0xFBFDFF, 0x1647A6, 0xFFFFFF, 0xE8F0F9, 0xE8F0F9, 0x4A5565,
            0xFFF1A0, 0x0D2C6B, 0xC4271B, 0xFFFFFF, 0x0A6E48, 0x8A5600,
            border = rgba(27, 74, 165, 0.34f), input = 0x1B4AA5,
            charts = listOf(0x1647A6, 0xC4271B, 0x0A6E48, 0x8A5600, 0x5B6B85),
            warningBand = 0xFBECEB, warningBandFg = 0x6B150E, warningBandAccent = 0xC4271B,
            glassStrong = 0xF6F9FC, shadowTint = Color(13, 44, 107), scrim = rgba(13, 44, 107, 0.4f),
        ),
        brushes = FinioBrushes(
            gradPrimary = solid(0x1647A6), gradPrimarySoft = solid(0xE8F0F9),
            gradSuccess = solid(0x0A6E48), gradSuccessSoft = solid(0xE2F3EC),
            gradDanger = solid(0xC4271B), gradDangerSoft = solid(0xFBECEB),
            gradInfo = solid(0x1647A6), gradWarning = solid(0x8A5600), gradSurface = solid(0xF6F9FC),
            paper = SolidColor(Color.Transparent), coin = solid(0x1647A6), register = { _ -> hatch }, thread = hatch,
        ),
        shadows = flatShadows(Color(13, 44, 107)),
        spec = FinioSpec(VisualTheme.Naksha, 1.dp, nakshaNotes(0xF6F9FC, 0x0D2C6B, 0xC4271B)),
    )
}

private val Dark = run {
    val hatch = HatchBrush(rgba(255, 233, 138, 0.85f), 1f)
    ThemePalette(
        colors = colorsOf(
            true, 0x12397A, 0xF4F8FF, 0x12397A, 0x0F3270, 0xFFE98A, 0x0D2C6B, 0x194A96, 0x194A96, 0xC3D7F5,
            0x194A96, 0xF4F8FF, 0xFFA08F, 0x0D2C6B, 0xA9F2D2, 0xFFD36E,
            border = rgba(219, 232, 251, 0.4f), input = 0xDBE8FB,
            charts = listOf(0xF4F8FF, 0xFFE98A, 0xA9F2D2, 0xFFA08F, 0x9FBBE6),
            warningBand = 0x4A2A5E, warningBandFg = 0xFFE4DE, warningBandAccent = 0xFFA08F,
            glassStrong = 0x12397A, shadowTint = Color(0, 0, 0), scrim = rgba(5, 20, 52, 0.65f),
        ),
        brushes = FinioBrushes(
            // The filled action keeps a deep blue so white text reads; the drawn outline lifts it off the sheet.
            gradPrimary = solid(0x1A50B8), gradPrimarySoft = solid(0x194A96),
            gradSuccess = solid(0x0D6B4A), gradSuccessSoft = solid(0x0F4A5C),
            gradDanger = solid(0xB3281C), gradDangerSoft = solid(0x4A2A5E),
            gradInfo = solid(0x1A50B8), gradWarning = solid(0x8A5600), gradSurface = solid(0x12397A),
            paper = SolidColor(Color.Transparent), coin = solid(0xF4F8FF), register = { _ -> hatch }, thread = hatch,
        ),
        shadows = flatShadows(Color(0, 0, 0)),
        spec = FinioSpec(VisualTheme.Naksha, 1.dp, nakshaNotes(0x12397A, 0xF4F8FF, 0xFFA08F)),
    )
}
