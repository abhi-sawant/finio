package com.slowatcoding.finio.ui.theme.kits

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
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
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * Sector — Chandigarh modernism (web/src/themes/sector.css). Raw concrete ground, flush zero-radius
 * enamel blocks, 6dp joints, no shadows and no outlines. The hero is one big yellow block with a
 * poster-scale Darker Grotesque figure; accounts are enamel bays (savings cobalt, card red,
 * deposits green, cash graphite). The signature is the month as a row of fins under the hero
 * ([HeroFooter]): one per day, days gone solid, today tall, days ahead open, louvres opening once.
 *
 * Fonts: Darker Grotesque and Hanken Grotesk have no rupee sign. Compose cannot fall back per glyph
 * inside one FontFamily, so only the hero figure splits the rupee out into Public Sans (as the web
 * does); every other rupee sign resolves through the system font.
 *
 * Dark-mode hero: like the web, the yellow block stays yellow in dark mode and the text tokens are
 * re-pointed to ink inside it (ThemedNoteCard does that for Sector, as it does for Gulal's sticker).
 */
object SectorKit : ThemeKit {
    override val visual = VisualTheme.Sector
    override fun palette(dark: Boolean): ThemePalette = if (dark) Dark else Light
    override val type: FinioTypeScale get() = Scale
    override val shapes: FinioShapeScale get() = Shapes
    override fun coinInk(dark: Boolean) = c(0x141413)
    override val coinShape: Shape get() = RectangleShape

    /** The FAB is a yellow square: the coin brush is yellow in both modes with graphite ink. */
    override val progressTrackHeight: Dp get() = 10.dp

    /** A hollow bay: a 1.5dp ink outline under the fill. */
    @Composable
    override fun progressDecoration(): Modifier {
        val ink = FinioTheme.colors.foreground
        return Modifier.drawWithContent {
            val w = 1.5.dp.toPx()
            drawRect(ink, Offset(w / 2, w / 2), Size(size.width - w, size.height - w), style = Stroke(w))
            drawContent()
        }
    }

    // ── hero: the big yellow block ──
    @Composable
    override fun heroContainer(shape: Shape): Modifier {
        val dark = FinioTheme.colors.isDark
        return if (dark && !DarkHeroYellow) Modifier.background(c(0x27272A), RectangleShape)
        else Modifier.background(yellow(dark), RectangleShape)
    }

    override fun heroPadding(wide: Boolean) =
        if (wide) PaddingValues(24.dp, 20.dp, 24.dp, 24.dp) else PaddingValues(14.dp, 12.dp, 14.dp, 14.dp)

    @Composable
    override fun HeroFigure(text: String, style: TextStyle, color: Color, whole: Boolean, modifier: Modifier): Boolean {
        val colors = FinioTheme.colors
        val reduced = rememberReducedMotion()
        val reveal = remember { Animatable(if (reduced) 1f else 0f) }
        LaunchedEffect(Unit) { if (!reduced) reveal.animateTo(1f, tween(700, delayMillis = 100, easing = Louvre)) }
        val wide = LocalConfiguration.current.screenWidthDp >= 1024
        val screenW = LocalConfiguration.current.screenWidthDp
        // clamp(4rem, 28vw, 6.5rem) on a phone, 11rem on desktop.
        val target = if (wide) 176f else (screenW * 0.28f).coerceIn(64f, 104f)
        // Darker Grotesque digits run about 0.5em wide: shrink a long figure rather than clip it.
        val ink = if (colors.isDark && !DarkHeroYellow && color == colors.foreground) yellow(true) else color
        val annotated = remember(text) {
            buildAnnotatedString {
                for (ch in text) {
                    if (ch == '₹') withStyle(SpanStyle(fontFamily = PublicSans, fontWeight = FontWeight(900))) { append(ch) } else append(ch)
                }
            }
        }
        BoxWithConstraints(modifier) {
            val fit = maxWidth.value / (text.length.coerceAtLeast(1) * 0.5f)
            val size = minOf(target, fit).coerceAtLeast(40f)
            Text(
                annotated,
                Modifier
                    .padding(top = 6.dp, bottom = (size * 0.24f).dp)
                    .graphicsLayer {
                        transformOrigin = TransformOrigin(0f, 0.5f)
                        scaleX = reveal.value
                    },
                style = style.copy(
                    fontFamily = DarkerGrotesque,
                    fontWeight = FontWeight(900),
                    fontSize = size.sp,
                    lineHeight = (size * 0.78f).sp,
                    letterSpacing = (-0.02).em,
                ),
                color = ink,
                maxLines = 1,
                softWrap = false,
            )
        }
        return true
    }

    @Composable
    override fun HeroFooter(elapsed: Float, spentPercent: Float, isOver: Boolean, daysLeft: Int) {
        val colors = FinioTheme.colors
        val ink = if (colors.isDark && !DarkHeroYellow) yellow(true) else c(0x141413)
        val period = LocalHeroPeriod.current
        val total = period?.totalDays?.coerceAtLeast(1) ?: periodDays(elapsed, daysLeft)
        val day = period?.dayOfPeriod?.coerceIn(1, total) ?: (total - daysLeft + 1).coerceIn(1, total)
        val reduced = rememberReducedMotion()
        val finsTotal = if (total > MaxFins) 850f else 150f + 28f * (total - 1) + 550f
        val clock = remember(total) { Animatable(if (reduced) finsTotal else 0f) }
        LaunchedEffect(total) { if (!reduced) clock.animateTo(finsTotal, tween(finsTotal.toInt(), easing = LinearEasing)) }
        Column(Modifier.padding(top = 12.dp).clearAndSetSemantics { }) {
            if (total > MaxFins) {
                val p = Louvre.transform(((clock.value - 150f) / 700f).coerceIn(0f, 1f))
                Canvas(Modifier.fillMaxWidth().height(24.dp)) {
                    val w = 1.5.dp.toPx()
                    drawRect(ink, Offset(w / 2, w / 2), Size(size.width - w, size.height - w), style = Stroke(w))
                    drawRect(ink, Offset.Zero, Size(size.width * day / total * p, size.height))
                }
            } else {
                Canvas(Modifier.fillMaxWidth().height(if (LocalConfiguration.current.screenWidthDp >= 1024) 60.dp else 42.dp)) {
                    val gap = 3.dp.toPx()
                    val fw = (size.width - gap * (total - 1)) / total
                    val line = 1.5.dp.toPx()
                    for (k in 0 until total) {
                        val p = Louvre.transform(((clock.value - (150f + 28f * k)) / 550f).coerceIn(0f, 1f))
                        if (p <= 0f) continue
                        val today = k + 1 == day
                        val h = if (today) size.height else size.height * 0.64f
                        val top = size.height - h
                        val x = k * (fw + gap)
                        val wPx = fw * p
                        if (k + 1 > day) {
                            if (wPx > line) drawRect(ink, Offset(x + line / 2, top + line / 2), Size(wPx - line, h - line), style = Stroke(line))
                        } else {
                            drawRect(ink, Offset(x, top), Size(wPx, h))
                        }
                    }
                }
            }
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val start = (maxWidth * ((day - 1f) / total)).coerceAtMost((maxWidth - 92.dp).coerceAtLeast(0.dp))
                Text(
                    "Day $day of $total",
                    Modifier.padding(start = start, top = 3.dp),
                    style = FinioTheme.type.caption.copy(fontWeight = FontWeight(700)),
                    color = ink,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
    }

    // ── chrome: flush enamel keys, the current one graphite ──
    @Composable
    override fun TabBar(active: FinioTab?, onSelect: (FinioTab) -> Unit, modifier: Modifier) {
        val colors = FinioTheme.colors
        val panel = if (colors.isDark) c(0x27272A) else c(0xE5E2DA)
        Row(
            modifier
                .fillMaxWidth()
                .background(colors.background)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 3.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FinioTab.entries.forEach { tab ->
                val selected = tab == active
                val tint = if (selected) colors.background else colors.mutedForeground
                val icon = remember(tab) { tab.icon(1.8f) }
                Column(
                    Modifier
                        .weight(1f)
                        .padding(horizontal = 3.dp)
                        .background(if (selected) colors.foreground else panel, RectangleShape)
                        .tabClickable(selected) { onSelect(tab) }
                        .padding(top = 8.dp, bottom = 7.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = tint)
                    Text(tab.label, style = TabStyle, color = tint, maxLines = 1)
                }
            }
        }
    }
}

/** Flip to true once ThemedNoteCard re-points foreground/muted/primary/destructive to ink for Sector. */
private const val DarkHeroYellow = true

/** Beyond this many days a fin per day is too fine to read, so the period becomes a plain bar. */
private const val MaxFins = 62

/**
 * Fallback only (when `LocalHeroPeriod` is null, e.g. previews): the exact period length is read from
 * `LocalHeroPeriod`. The kit hook gives only (elapsed, daysLeft), not the period length. daysLeft counts today, so
 * today is day `total - daysLeft + 1` and `elapsed * total` falls in `[total - daysLeft, total - daysLeft + 1)`,
 * i.e. `total` lies in `((L-1)/(1-e), L/(1-e)]`. Where that window holds several integers (the last days
 * of a period) pick the calendar length nearest the usual ones.
 */
private fun periodDays(elapsed: Float, daysLeft: Int): Int {
    val l = daysLeft.coerceAtLeast(1)
    val e = elapsed.coerceIn(0f, 0.9995f)
    val hi = floor(l / (1f - e) + 1e-3f).toInt()
    val lo = floor((l - 1) / (1f - e) + 1e-3f).toInt() + 1
    if (hi < lo) return (l + (e * l / (1f - e)).roundToInt()).coerceAtLeast(l)
    if (lo == hi) return hi
    val common = listOf(7, 28, 29, 30, 31, 365, 366).filter { it in lo..hi }
    return common.minByOrNull { kotlin.math.abs(it - 30) } ?: hi
}

// ───────────────────────────── type & shape ─────────────────────────────

private fun variable(res: Int, weight: Int) = Font(
    resId = res,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

private val DarkerGrotesque = FontFamily(
    variable(R.font.sector_darker_grotesque, 400), variable(R.font.sector_darker_grotesque, 500),
    variable(R.font.sector_darker_grotesque, 600), variable(R.font.sector_darker_grotesque, 700),
    variable(R.font.sector_darker_grotesque, 800), variable(R.font.sector_darker_grotesque, 900),
)
private val HankenGrotesk = FontFamily(
    variable(R.font.sector_hanken_grotesk, 400), variable(R.font.sector_hanken_grotesk, 500),
    variable(R.font.sector_hanken_grotesk, 600), variable(R.font.sector_hanken_grotesk, 700),
    variable(R.font.sector_hanken_grotesk, 800),
)
/** Another theme's resource: the ₹-capable face the web falls back to. */
private val PublicSans = FontFamily(variable(R.font.daak_public_sans, 800), variable(R.font.daak_public_sans, 900))

private val Scale = FinioTypeScale(
    DarkerGrotesque, DarkerGrotesque, HankenGrotesk,
    displayWeight = 900, displayTracking = -0.02f, pageTitleWeight = 900, screenTitleWeight = 800,
    headlineWeight = 800, moneyWeight = 800, dialogTitleWeight = 900, titleWeight = 700,
)
private val Shapes = FinioShapeScale(base = 0.dp, full = RectangleShape)

private val TabStyle = TextStyle(fontFamily = HankenGrotesk, fontWeight = FontWeight(700), fontSize = 10.sp)

/** `cubic-bezier(.16,1,.3,1)` — the louvre opening. */
private val Louvre = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

// ───────────────────────────── palettes ─────────────────────────────

private fun c(rgb: Long) = Color(0xFF000000 or rgb)
private fun solid(rgb: Long): Brush = SolidColor(c(rgb))
private fun yellow(dark: Boolean) = if (dark) c(0xEAB700) else c(0xF5C400)

private fun colorsOf(
    dark: Boolean,
    background: Long, foreground: Long, card: Long, popover: Long,
    primary: Long, primaryFg: Long, secondary: Long, muted: Long, mutedFg: Long,
    accent: Long, accentFg: Long, destructive: Long, destructiveFg: Long,
    positive: Long, warning: Long, border: Long, input: Long, ring: Long,
    charts: List<Long>, warningBand: Long, shadowTint: Color, scrim: Color,
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
    border = c(border), input = c(input), ring = c(ring),
    chart1 = c(charts[0]), chart2 = c(charts[1]), chart3 = c(charts[2]), chart4 = c(charts[3]), chart5 = c(charts[4]),
    sidebar = c(background), sidebarForeground = c(foreground),
    sidebarPrimary = c(if (dark) 0x2450C0 else 0x1846B0), sidebarPrimaryForeground = c(0xFFFFFF),
    sidebarAccent = c(card), sidebarAccentForeground = c(foreground),
    sidebarBorder = c(border), sidebarRing = c(ring),
    warningBand = c(warningBand), warningBandForeground = c(0xFFFFFF), warningBandAccent = c(0xFFFFFF),
    engraving = Color.Transparent,
    glass = Color.Transparent, glassStrong = c(background), glassBorder = Color.Transparent, glassHighlight = Color.Transparent,
    shadowTint = shadowTint, scrim = scrim,
)

/** No shadows anywhere: flush, unlit. */
private val NoShadows = FinioShadows(
    card = emptyList(), float = emptyList(), glowPrimary = emptyList(), glowSuccess = emptyList(), glowDanger = emptyList(),
    coin = emptyList(), tile = emptyList(), tabBar = emptyList(), noteChip = emptyList(),
    xs = emptyList(), sm = emptyList(), md = emptyList(), lg = emptyList(), xl = emptyList(),
)

private fun bay(fill: Long, ink: Long) = NoteStyle(c(fill), c(fill), c(ink))

/** Savings cobalt, card red, deposits green, cash graphite, anything else pale cobalt. */
private fun bays(pale: Long, paleInk: Long, cobalt: Long, red: Long, green: Long, graphite: Long, graphiteInk: Long) =
    Denomination.entries.associateWith { d ->
        when (d) {
            Denomination.Rs500 -> bay(cobalt, 0xFFFFFF)
            Denomination.Rs2000 -> bay(red, 0xFFFFFF)
            Denomination.Rs200 -> bay(green, 0xFFFFFF)
            Denomination.Rs10 -> bay(graphite, graphiteInk)
            else -> bay(pale, paleInk)
        }
    }

private val Light = ThemePalette(
    colors = colorsOf(
        false, 0xC8C4BA, 0x141413, 0xE5E2DA, 0xE5E2DA, 0x153D9C, 0xFFFFFF, 0xD5D1C7, 0xD5D1C7, 0x48453E,
        0xD5D1C7, 0x141413, 0x9A110D, 0xFFFFFF, 0x14632F, 0xA5330A, 0xB3AFA4, 0x8A867B, 0x1846B0,
        charts = listOf(0x1846B0, 0xC01C18, 0x166B34, 0xF5C400, 0x48453E), warningBand = 0xC01C18,
        shadowTint = Color(20, 20, 19), scrim = rgba(20, 20, 19, 0.5f),
    ),
    brushes = FinioBrushes(
        gradPrimary = solid(0x1846B0), gradPrimarySoft = solid(0xD5D1C7),
        gradSuccess = solid(0x166B34), gradSuccessSoft = solid(0xCFDCC9),
        gradDanger = solid(0xA8120E), gradDangerSoft = solid(0xE6C4BD),
        gradInfo = solid(0x1846B0), gradWarning = solid(0xA5330A), gradSurface = solid(0xE5E2DA),
        paper = SolidColor(Color.Transparent), coin = solid(0xF5C400), register = { _ -> solid(0x141413) }, thread = solid(0x141413),
    ),
    shadows = NoShadows,
    spec = FinioSpec(VisualTheme.Sector, 0.dp, bays(0x8FB0EA, 0x0A1A40, 0x1846B0, 0xC01C18, 0x166B34, 0x141413, 0xF1EEE7)),
)

private val Dark = ThemePalette(
    colors = colorsOf(
        true, 0x161617, 0xF1EEE7, 0x27272A, 0x2D2D31, 0x8AA9E6, 0x0A1A40, 0x333337, 0x333337, 0xB7B3AA,
        0x333337, 0xF1EEE7, 0xFF7B70, 0x1A0604, 0x62D08A, 0xF0A35A, 0x3E3E43, 0x6A6A70, 0x8AA9E6,
        charts = listOf(0x7C9FE0, 0xFF7B70, 0x62D08A, 0xEAB700, 0xB7B3AA), warningBand = 0xC42A24,
        shadowTint = Color(0, 0, 0), scrim = rgba(0, 0, 0, 0.65f),
    ),
    brushes = FinioBrushes(
        gradPrimary = solid(0x2450C0), gradPrimarySoft = solid(0x333337),
        gradSuccess = solid(0x1F7D42), gradSuccessSoft = solid(0x1C3326),
        gradDanger = solid(0xB3241E), gradDangerSoft = solid(0x3B1B19),
        gradInfo = solid(0x2450C0), gradWarning = solid(0xA5330A), gradSurface = solid(0x27272A),
        paper = SolidColor(Color.Transparent), coin = solid(0xEAB700), register = { _ -> solid(0xF1EEE7) }, thread = solid(0xF1EEE7),
    ),
    shadows = NoShadows,
    spec = FinioSpec(VisualTheme.Sector, 0.dp, bays(0x7C9FE0, 0x0A1A40, 0x2450C0, 0xC42A24, 0x1F7D42, 0xF1EEE7, 0x141413)),
)
