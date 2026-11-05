package com.slowatcoding.finio.ui.theme.kits

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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.slowatcoding.finio.R
import com.slowatcoding.finio.ui.mudra.Denomination
import com.slowatcoding.finio.ui.mudra.NoteStyle
import com.slowatcoding.finio.ui.navigation.FinioTab
import com.slowatcoding.finio.ui.shell.tabClickable
import com.slowatcoding.finio.ui.theme.CssLinearGradient
import com.slowatcoding.finio.ui.theme.CssRepeatingLinearGradient
import com.slowatcoding.finio.ui.theme.CssShadow
import com.slowatcoding.finio.ui.theme.Figtree
import com.slowatcoding.finio.ui.theme.FinioBrushes
import com.slowatcoding.finio.ui.theme.FinioColors
import com.slowatcoding.finio.ui.theme.FinioShadows
import com.slowatcoding.finio.ui.theme.FinioShapeScale
import com.slowatcoding.finio.ui.theme.FinioSpec
import com.slowatcoding.finio.ui.theme.FinioTheme
import com.slowatcoding.finio.ui.theme.FinioType
import com.slowatcoding.finio.ui.theme.FinioTypeScale
import com.slowatcoding.finio.ui.theme.Stop
import com.slowatcoding.finio.ui.theme.ThemePalette
import com.slowatcoding.finio.ui.theme.VisualTheme
import com.slowatcoding.finio.ui.theme.rgba
import com.slowatcoding.finio.ui.theme.stock

// ───────────────────────────── fonts ─────────────────────────────

/** Limelight — a single-weight deco display face: titles and the hero figure. No ₹ glyph. */
internal val Limelight = FontFamily(Font(R.font.mahal_limelight, FontWeight.Normal))

private fun jost(weight: Int) = Font(
    resId = R.font.mahal_jost,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

/** Jost — every row, label and keypad digit (web `'Jost Variable'`). */
internal val Jost = FontFamily(jost(300), jost(400), jost(500), jost(600), jost(700))

// ───────────────────────────── palette ─────────────────────────────

private fun c(rgb: Long) = Color(0xFF000000 or rgb)
private fun solid(rgb: Long): Brush = SolidColor(c(rgb))
private fun grad(a: Long, b: Long): Brush = CssLinearGradient(135f, listOf(Stop(0f, c(a)), Stop(1f, c(b))))

/** The deco kit's own colours (web `--mahal-*`), per mode. */
internal object MahalInk {
    fun gold(dark: Boolean) = if (dark) c(0xD6B46A) else c(0x9A7A35)
    /** Gold that is set as text: 4.5:1 on the ivory. */
    fun goldText(dark: Boolean) = if (dark) c(0xD6B46A) else c(0x7A5A1C)
    fun gline(dark: Boolean) = if (dark) rgba(214, 180, 106, 0.42f) else rgba(154, 122, 53, 0.45f)
    fun hair(dark: Boolean) = if (dark) rgba(214, 180, 106, 0.16f) else rgba(154, 122, 53, 0.22f)
    fun unlit(dark: Boolean) = if (dark) rgba(214, 180, 106, 0.2f) else rgba(154, 122, 53, 0.24f)
    fun button(dark: Boolean) = if (dark) c(0xD6B46A) else c(0x0F3A3F)
    fun buttonInk(dark: Boolean) = if (dark) c(0x0C1F22) else c(0xF6EEDD)
}

private fun notes(vararg tints: Triple<Denomination, Pair<Long, Long>, Long>) =
    tints.associate { (d, bg, ink) -> d to NoteStyle(c(bg.first), c(bg.second), c(ink)) }

private val NotesLight = notes(
    Triple(Denomination.Rs100, 0xE3EEEAL to 0xD6E6E1L, 0x0F3A3FL),
    Triple(Denomination.Rs500, 0xEAF0DCL to 0xDDE7CBL, 0x1F4A2AL),
    Triple(Denomination.Rs2000, 0xF6E0D6L to 0xEFD0C2L, 0x6E1F10L),
    Triple(Denomination.Rs200, 0xF6EBCDL to 0xEFDFB4L, 0x5C4314L),
    Triple(Denomination.Rs10, 0xEFE3D2L to 0xE6D5BDL, 0x4D3217L),
    Triple(Denomination.Rs50, 0xDCEBEEL to 0xCDE2E6L, 0x0D4450L),
    Triple(Denomination.Rs20, 0xE9EED5L to 0xDDE5BFL, 0x3D4A0EL),
)
private val NotesDark = notes(
    Triple(Denomination.Rs100, 0x16393DL to 0x123134L, 0xD4EFEAL),
    Triple(Denomination.Rs500, 0x1C3A2AL to 0x163122L, 0xDCECD2L),
    Triple(Denomination.Rs2000, 0x43231BL to 0x371C16L, 0xFFD9CDL),
    Triple(Denomination.Rs200, 0x3D3218L to 0x322913L, 0xF6E2AEL),
    Triple(Denomination.Rs10, 0x3A2C1FL to 0x2E2318L, 0xF2DCC2L),
    Triple(Denomination.Rs50, 0x133A44L to 0x0F3038L, 0xC8EEF5L),
    Triple(Denomination.Rs20, 0x313A17L to 0x283012L, 0xE8F0B8L),
)

/** The register (budget fill): a row of gilt tiles, 8dp lit and a 2dp gap (web progressbar mask). */
private fun tiles(ink: Long): (Float) -> Brush = { density ->
    CssRepeatingLinearGradient(
        angleDegrees = 90f,
        periodPx = 10f * density,
        stops = listOf(Stop(0f, c(ink)), Stop(0.8f, c(ink)), Stop(0.8f, Color.Transparent), Stop(1f, Color.Transparent)),
    )
}

private fun colors(dark: Boolean): FinioColors {
    val bg = if (dark) c(0x0C1F22) else c(0xF4ECDC)
    val fg = if (dark) c(0xF3EAD6) else c(0x0F3A3F)
    val card = if (dark) c(0x112A2E) else c(0xFBF6EA)
    val popover = if (dark) c(0x143236) else c(0xFBF6EA)
    val primary = if (dark) c(0xD6B46A) else c(0x0F4A50)
    val primaryFg = if (dark) c(0x0C1F22) else c(0xFBF6EA)
    val muted = if (dark) c(0x173539) else c(0xECE1C8)
    val mutedFg = if (dark) c(0xB5B09B) else c(0x4D6562)
    val accent = if (dark) c(0x1F4044) else c(0xEADBB5)
    val accentFg = if (dark) c(0xF3EAD6) else c(0x5C4314)
    val border = if (dark) rgba(214, 180, 106, 0.2f) else rgba(154, 122, 53, 0.28f)
    val gline = MahalInk.gline(dark)
    val charts: List<Long> = if (dark) listOf(0xD6B46AL, 0x6FD3A3L, 0xFF8266L, 0x7FC4CFL, 0xC9A6E8L) else listOf(0x0F4A50L, 0x9A7A35L, 0xB3321BL, 0x1B7550L, 0x5B7FA6L)
    return FinioColors(
        isDark = dark,
        background = bg, foreground = fg,
        card = card, cardForeground = fg,
        popover = popover, popoverForeground = fg,
        primary = primary, primaryForeground = primaryFg,
        secondary = muted, secondaryForeground = fg,
        muted = muted, mutedForeground = mutedFg,
        accent = accent, accentForeground = accentFg,
        destructive = if (dark) c(0xFF8266) else c(0xB3321B),
        destructiveForeground = if (dark) c(0x2A0C05) else Color.White,
        positive = if (dark) c(0x6FD3A3) else c(0x1B7550),
        warning = if (dark) c(0xF2B84F) else c(0x8A5600),
        border = border,
        input = if (dark) rgba(214, 180, 106, 0.45f) else rgba(154, 122, 53, 0.55f),
        ring = MahalInk.gold(dark),
        chart1 = c(charts[0]), chart2 = c(charts[1]), chart3 = c(charts[2]), chart4 = c(charts[3]), chart5 = c(charts[4]),
        sidebar = card, sidebarForeground = fg,
        sidebarPrimary = primary, sidebarPrimaryForeground = primaryFg,
        sidebarAccent = accent, sidebarAccentForeground = accentFg,
        sidebarBorder = gline, sidebarRing = MahalInk.gold(dark),
        warningBand = if (dark) c(0x3A1A12) else c(0xF6DFD3),
        warningBandForeground = if (dark) c(0xFFE1D8) else c(0x4A160B),
        warningBandAccent = if (dark) c(0xFF8266) else c(0xB3321B),
        engraving = Color.Transparent,
        glass = card, glassStrong = card, glassBorder = gline, glassHighlight = Color.Transparent,
        shadowTint = if (dark) Color(0, 0, 0) else Color(15, 58, 63),
        scrim = if (dark) rgba(2, 8, 9, 0.65f) else rgba(12, 31, 34, 0.45f),
    )
}

private fun brushes(dark: Boolean) = FinioBrushes(
    // Call sites put white text on gradPrimary: peacock in light, a deep bronze in dark.
    gradPrimary = if (dark) grad(0x8A6A28, 0x6B5018) else grad(0x17575D, 0x0F3A3F),
    gradPrimarySoft = if (dark) solid(0x1F4044) else solid(0xEFE4C8),
    gradSuccess = grad(0x1F8A5E, 0x1B7550),
    gradSuccessSoft = if (dark) solid(0x123A2C) else solid(0xDCEBDC),
    gradDanger = grad(0xC43D22, 0xA52C16),
    gradDangerSoft = if (dark) solid(0x3A1A12) else solid(0xF6DCD3),
    gradInfo = grad(0x2F7A86, 0x1F5F69),
    gradWarning = grad(0xB07A1A, 0x8A5600),
    gradSurface = if (dark) solid(0x112A2E) else solid(0xFBF6EA),
    paper = SolidColor(Color.Transparent),
    coin = SolidColor(MahalInk.button(dark)),
    register = if (dark) tiles(0xD6B46A) else tiles(0x9A7A35),
    thread = if (dark) solid(0xD6B46A) else solid(0x9A7A35),
)

private fun shadows(dark: Boolean): FinioShadows {
    val tint = if (dark) Color(0, 0, 0) else Color(15, 58, 63)
    val s = stock(tint)
    val float = listOf(CssShadow(y = 24.dp, blur = 48.dp, spread = (-22).dp, color = if (dark) rgba(0, 0, 0, 0.8f) else rgba(15, 58, 63, 0.45f)))
    return FinioShadows(
        card = emptyList(), float = float, glowPrimary = emptyList(), glowSuccess = emptyList(), glowDanger = emptyList(),
        coin = emptyList(), tile = emptyList(), tabBar = emptyList(), noteChip = emptyList(),
        xs = s[0], sm = s[1], md = s[2], lg = s[3], xl = s[4],
    )
}

private val PaletteLight = ThemePalette(colors(false), brushes(false), shadows(false), FinioSpec(VisualTheme.Mahal, 1.dp, NotesLight))
private val PaletteDark = ThemePalette(colors(true), brushes(true), shadows(true), FinioSpec(VisualTheme.Mahal, 1.dp, NotesDark))

// ───────────────────────────── shapes ─────────────────────────────

/** Elongated, chamfered ends (web `--mahal-chamfer`): buttons, pills and progress tracks. */
private object ChamferShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val cut = minOf(with(density) { 10.dp.toPx() }, size.height / 2f, size.width / 2f)
        val p = Path().apply {
            moveTo(cut, 0f)
            lineTo(size.width - cut, 0f)
            lineTo(size.width, size.height / 2f)
            lineTo(size.width - cut, size.height)
            lineTo(cut, size.height)
            lineTo(0f, size.height / 2f)
            close()
        }
        return Outline.Generic(p)
    }
}

private val MahalShapes = FinioShapeScale(base = 2.dp, full = ChamferShape)

private val MahalType = FinioTypeScale(
    Limelight, Jost, Jost,
    displayWeight = 500, displayTracking = 0f,
    pageTitleWeight = 400, screenTitleWeight = 400, headlineWeight = 400,
    moneyWeight = 500, dialogTitleWeight = 400, titleWeight = 500,
)

// ───────────────────────────── the kit ─────────────────────────────

/**
 * Mahal — Bombay Art Deco after dark (web/src/themes/mahal.css). Midnight peacock and antique
 * gold; Limelight titles and hero figure over Jost; chamfered buttons; a frieze tab bar with a
 * gilt ziggurat over the current tab; a porthole add button; and an arched proscenium hero whose
 * arch holds the sunburst gauge ([HeroScene], MahalHero.kt).
 */
object MahalKit : ThemeKit {
    override val visual = VisualTheme.Mahal
    override fun palette(dark: Boolean): ThemePalette = if (dark) PaletteDark else PaletteLight
    override val type: FinioTypeScale get() = MahalType
    override val shapes: FinioShapeScale get() = MahalShapes

    override fun coinInk(dark: Boolean) = MahalInk.buttonInk(dark)
    override val coinShape: Shape get() = CircleShape

    /** The porthole's inner ring, four dp inside the rim. */
    @Composable
    override fun coinDecoration(shape: Shape): Modifier {
        val ink = MahalInk.buttonInk(FinioTheme.colors.isDark)
        return Modifier.drawWithContent {
            drawContent()
            val inset = 4.5.dp.toPx()
            drawCircle(ink, radius = size.minDimension / 2f - inset, style = Stroke(1.dp.toPx()))
        }
    }

    override val progressTrackHeight: Dp get() = 7.dp

    @Composable
    override fun heroContainer(shape: Shape): Modifier = mahalProscenium()

    override fun heroPadding(wide: Boolean): PaddingValues =
        if (wide) PaddingValues(HeroSideWide, HeroTop, HeroSideWide, 32.dp) else PaddingValues(HeroSide, HeroTop, HeroSide, 26.dp)

    @Composable
    override fun HeroFigure(text: String, style: TextStyle, color: Color, whole: Boolean, modifier: Modifier): Boolean {
        val sized = style.copy(
            fontFamily = Limelight,
            fontWeight = FontWeight.Normal,
            fontSize = style.fontSize * 1.35f,
            lineHeight = style.lineHeight * 1.35f,
            letterSpacing = 0.01.em,
        )
        // Limelight has no rupee sign; Figtree supplies it, as on the web.
        val annotated = remember(text) {
            buildAnnotatedString {
                for (ch in text) {
                    if (ch == '₹') withStyle(SpanStyle(fontFamily = Figtree, fontWeight = FontWeight.Normal)) { append(ch) } else append(ch)
                }
            }
        }
        Text(annotated, modifier, style = sized, color = color, maxLines = 1, softWrap = false)
        return true
    }

    @Composable
    override fun HeroScene(elapsed: Float, spentPercent: Float, isOver: Boolean) = MahalSunburst(elapsed, spentPercent, isOver)

    @Composable
    override fun TabBar(active: FinioTab?, onSelect: (FinioTab) -> Unit, modifier: Modifier) {
        val colors = FinioTheme.colors
        val dark = colors.isDark
        val gold = MahalInk.gold(dark)
        val goldText = MahalInk.goldText(dark)
        Row(
            modifier
                .fillMaxWidth()
                .drawBehind {
                    drawRect(colors.card)
                    // A 3px double rule: the frieze's top edge.
                    val w = 1.dp.toPx()
                    drawLine(gold, Offset(0f, w / 2f), Offset(size.width, w / 2f), w)
                    drawLine(gold, Offset(0f, w * 2.5f), Offset(size.width, w * 2.5f), w)
                }
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(start = 6.dp, end = 6.dp, top = 10.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FinioTab.entries.forEach { tab ->
                val selected = tab == active
                val icon = remember(tab) { tab.icon(1.75f) }
                Column(
                    Modifier
                        .weight(1f)
                        .drawBehind { if (selected) drawZiggurat(gold, Offset(size.width / 2f, -10.dp.toPx())) }
                        .tabClickable(selected) { onSelect(tab) }
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = if (selected) goldText else colors.mutedForeground)
                    Text(
                        tab.label.uppercase(),
                        style = FinioType.micro.copy(letterSpacing = 0.08.em, fontSize = 10.sp),
                        color = if (selected) colors.foreground else colors.mutedForeground,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

internal val HeroTop = 14.dp
internal val HeroSide = 22.dp
internal val HeroSideWide = 32.dp

/** The stepped ziggurat marker, 40 × 9dp, its base centred on [base]. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawZiggurat(color: Color, base: Offset) {
    val w = 40.dp.toPx()
    val h = 9.dp.toPx()
    val l = base.x - w / 2f
    val t = base.y - h
    fun x(f: Float) = l + w * f
    fun y(f: Float) = t + h * f
    val p = Path().apply {
        moveTo(x(0f), y(1f)); lineTo(x(0f), y(0.7f)); lineTo(x(0.18f), y(0.7f)); lineTo(x(0.18f), y(0.38f))
        lineTo(x(0.34f), y(0.38f)); lineTo(x(0.34f), y(0f)); lineTo(x(0.66f), y(0f)); lineTo(x(0.66f), y(0.38f))
        lineTo(x(0.82f), y(0.38f)); lineTo(x(0.82f), y(0.7f)); lineTo(x(1f), y(0.7f)); lineTo(x(1f), y(1f)); close()
    }
    drawPath(p, color)
}
