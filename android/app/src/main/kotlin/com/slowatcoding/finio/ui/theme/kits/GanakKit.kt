package com.slowatcoding.finio.ui.theme.kits

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
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
import com.slowatcoding.finio.ui.theme.CssLinearGradient
import com.slowatcoding.finio.ui.theme.CssRadialGradient
import com.slowatcoding.finio.ui.theme.CssShadow
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
import com.slowatcoding.finio.ui.theme.cssInsetShadow
import com.slowatcoding.finio.ui.theme.cssShadow
import com.slowatcoding.finio.ui.theme.rememberReducedMotion
import com.slowatcoding.finio.ui.theme.rgba
import com.slowatcoding.finio.ui.theme.stock
import kotlin.math.tan

/*
 * Ganak — the desk calculator (web/src/themes/ganak.css). Putty plastic in light, the black
 * model with a glowing VFD in dark; recessed moulded panels, rubber keys with a bottom lip, an
 * orange "=" key, Archivo throughout. The hero figure is drawn as seven-segment digits in an LCD
 * window ([GanakKit.HeroFigure]); budget tracks are LCD bar graphs.
 */

private fun c(rgb: Long) = Color(0xFF000000 or rgb)
private fun v(a: Long, b: Long): Brush = CssLinearGradient(180f, listOf(Stop(0f, c(a)), Stop(1f, c(b))))

// ───────────────────────────── type ─────────────────────────────

private fun archivo(weight: Int, width: Float) = Font(
    resId = R.font.ganak_archivo,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight), FontVariation.width(width)),
)

/** Archivo at its normal width — everything people read or type. */
private val Archivo = FontFamily(archivo(400, 100f), archivo(500, 100f), archivo(600, 100f), archivo(700, 100f), archivo(800, 100f))

/** Archivo stretched to 112% — titles, figures and key legends ("bold and wide"). */
private val ArchivoWide = FontFamily(archivo(500, 112f), archivo(600, 112f), archivo(700, 112f), archivo(800, 112f))

private val GanakTypeScale = FinioTypeScale(
    ArchivoWide, ArchivoWide, Archivo,
    displayWeight = 700, displayTracking = -0.01f, pageTitleWeight = 700, screenTitleWeight = 700,
    headlineWeight = 700, moneyWeight = 700, dialogTitleWeight = 700, titleWeight = 700,
)

// Panels 14dp; keys (buttons, icon buttons, tracks) 10dp rounded rectangles, never pills.
private val GanakShapeScale = FinioShapeScale(base = 14.dp, full = RoundedCornerShape(10.dp))

// ───────────────────────────── materials ─────────────────────────────

/** The calculator's own materials, per mode (web `--g-*`). */
internal class GanakMaterials(dark: Boolean) {
    val body2 = if (dark) c(0x1D1E22) else c(0xDDD6C5)
    val edge = if (dark) rgba(255, 255, 255, 0.06f) else c(0xFBF8F1)
    val shade = if (dark) rgba(0, 0, 0, 0.55f) else rgba(74, 62, 38, 0.28f)
    val keyTop = if (dark) c(0x34363C) else c(0xF6F2E8)
    val keyBottom = if (dark) c(0x28292E) else c(0xE2DCCD)
    val keyLip = if (dark) c(0x0A0A0C) else c(0xB3AA95)
    val keyHi = if (dark) rgba(255, 255, 255, 0.09f) else rgba(255, 255, 255, 0.75f)
    val orangeLip = if (dark) c(0x7A2D08) else c(0x8F300A)
    val lcd = if (dark) c(0x07110E) else c(0xB9C3A3)
    val lcdTop = if (dark) c(0x0A1512) else c(0xAAB593)
    val seg = if (dark) c(0x5EF2C4) else c(0x1F2A1C)
    val ghost = if (dark) rgba(94, 242, 196, 0.07f) else rgba(31, 42, 28, 0.075f)
    val bezel = if (dark) c(0x050506) else c(0x3A362E)
    val led = if (dark) c(0xFF7A3D) else c(0xE8622A)
    val ledOff = if (dark) c(0x3A3B40) else c(0xC3BBA9)
    val key: Brush = CssLinearGradient(180f, listOf(Stop(0f, keyTop), Stop(1f, keyBottom)))
    fun keyShadow(lip: Dp = 3.dp) = listOf(
        CssShadow(y = 1.dp, color = keyHi, inset = true),
        CssShadow(y = lip, color = keyLip),
        CssShadow(y = lip + 1.dp, blur = 7.dp, color = shade),
    )
}

// ───────────────────────────── palettes ─────────────────────────────

private fun colors(
    dark: Boolean, background: Long, foreground: Long, card: Long, popover: Long, primary: Long, primaryFg: Long,
    secondary: Long, muted: Long, mutedFg: Long, accent: Long, destructive: Long, destructiveFg: Long,
    positive: Long, warning: Long, border: Long, input: Color, charts: List<Long>, glassStrong: Long,
    shadowTint: Color, scrim: Color,
) = FinioColors(
    isDark = dark,
    background = c(background), foreground = c(foreground),
    card = c(card), cardForeground = c(foreground),
    popover = c(popover), popoverForeground = c(foreground),
    primary = c(primary), primaryForeground = c(primaryFg),
    secondary = c(secondary), secondaryForeground = c(foreground),
    muted = c(muted), mutedForeground = c(mutedFg),
    accent = c(accent), accentForeground = c(foreground),
    destructive = c(destructive), destructiveForeground = c(destructiveFg),
    positive = c(positive), warning = c(warning),
    border = c(border), input = input, ring = if (dark) c(0xFF7A3D) else c(0xE8622A),
    chart1 = c(charts[0]), chart2 = c(charts[1]), chart3 = c(charts[2]), chart4 = c(charts[3]), chart5 = c(charts[4]),
    sidebar = c(glassStrong), sidebarForeground = c(foreground),
    sidebarPrimary = c(primary), sidebarPrimaryForeground = c(primaryFg),
    sidebarAccent = c(card), sidebarAccentForeground = c(foreground),
    sidebarBorder = c(border), sidebarRing = c(primary),
    warningBand = c(card), warningBandForeground = c(foreground), warningBandAccent = c(destructive),
    engraving = Color.Transparent,
    glass = c(card), glassStrong = c(glassStrong), glassBorder = c(border), glassHighlight = Color.Transparent,
    shadowTint = shadowTint, scrim = scrim,
)

private fun keyNotes(ink: Long, vararg tints: Pair<Denomination, Pair<Long, Long>>) =
    tints.associate { (d, bg) -> d to NoteStyle(c(bg.first), c(bg.second), c(ink)) }

private val NotesLight = keyNotes(
    0x24211B,
    Denomination.Rs100 to (0xF3EFE4L to 0xE0DACBL), Denomination.Rs500 to (0xE9EEDCL to 0xD6DCC3L),
    Denomination.Rs2000 to (0xF6E4D8L to 0xE8CDBBL), Denomination.Rs200 to (0xF5ECD2L to 0xE6D9B4L),
    Denomination.Rs10 to (0xEFE5D8L to 0xDDCFBCL), Denomination.Rs50 to (0xE4ECEAL to 0xCFDAD7L),
    Denomination.Rs20 to (0xEDF0D8L to 0xDADFBDL),
)
private val NotesDark = keyNotes(
    0xEBE7DC,
    Denomination.Rs100 to (0x34363CL to 0x28292EL), Denomination.Rs500 to (0x2F3630L to 0x252B26L),
    Denomination.Rs2000 to (0x3D302BL to 0x2F2521L), Denomination.Rs200 to (0x3A3529L to 0x2D2920L),
    Denomination.Rs10 to (0x3A322CL to 0x2C2622L), Denomination.Rs50 to (0x2C3538L to 0x22292CL),
    Denomination.Rs20 to (0x33362AL to 0x282A21L),
)

private fun shadows(dark: Boolean): FinioShadows {
    val m = GanakMaterials(dark)
    val tint = if (dark) Color(0, 0, 0) else Color(74, 62, 38)
    val s = stock(tint)
    val orange = listOf(
        CssShadow(y = 1.5.dp, color = rgba(255, 255, 255, 0.3f), inset = true),
        CssShadow(y = 3.dp, color = m.orangeLip),
        CssShadow(y = 5.dp, blur = 9.dp, color = m.shade),
    )
    return FinioShadows(
        card = emptyList(),
        float = listOf(CssShadow(y = 18.dp, blur = 36.dp, spread = (-14).dp, color = if (dark) rgba(0, 0, 0, 0.85f) else rgba(74, 62, 38, 0.55f))),
        glowPrimary = orange, glowSuccess = emptyList(), glowDanger = emptyList(),
        coin = listOf(
            CssShadow(y = 2.dp, color = rgba(255, 255, 255, 0.3f), inset = true),
            CssShadow(y = 4.dp, color = m.orangeLip),
            CssShadow(y = 7.dp, blur = 12.dp, color = m.shade),
        ),
        tile = m.keyShadow(),
        tabBar = emptyList(),
        noteChip = listOf(CssShadow(y = 1.5.dp, color = m.keyLip)),
        xs = s[0], sm = s[1], md = s[2], lg = s[3], xl = s[4],
    )
}

private fun brushes(dark: Boolean): FinioBrushes {
    val m = GanakMaterials(dark)
    return FinioBrushes(
        // White text sits on gradPrimary at every call site, so it stays a deep orange in dark too.
        gradPrimary = if (dark) v(0xC24A12, 0xA83C0C) else v(0xD2531A, 0xB8440E),
        gradPrimarySoft = SolidColor(if (dark) c(0x28292E) else c(0xD4CCB9)),
        gradSuccess = if (dark) v(0x2F7A3E, 0x24632F) else v(0x357A2F, 0x2A6125),
        gradSuccessSoft = SolidColor(if (dark) c(0x132419) else c(0xD3DCC0)),
        gradDanger = if (dark) v(0xB8302A, 0x9A2420) else v(0xB8302A, 0xA3221B),
        gradDangerSoft = SolidColor(if (dark) c(0x2A1513) else c(0xEAD0C4)),
        gradInfo = if (dark) v(0x5B4F45, 0x463C34) else v(0x5A554B, 0x45413A),
        gradWarning = v(0x9A6A10, 0x7A4F00),
        gradSurface = SolidColor(if (dark) c(0x0F1012) else c(0xDCD5C4)),
        paper = SolidColor(Color.Transparent),
        coin = CssRadialGradient(
            0.5f, 0.3f,
            if (dark) listOf(Stop(0f, c(0xFF7A3D)), Stop(1f, c(0xEF6326))) else listOf(Stop(0f, c(0xE8622A)), Stop(1f, c(0xC9480F))),
        ),
        register = { _ -> SolidColor(m.seg) },
        thread = SolidColor(m.seg),
    )
}

private val GanakLight = ThemePalette(
    colors = colors(
        false, 0xE7E1D3, 0x24211B, 0xDCD5C4, 0xEEE9DD, 0xA83C0C, 0xFFFFFF,
        0xD4CCB9, 0xDDD6C5, 0x5F5747, 0xD4CCB9, 0xA3221B, 0xFFFFFF,
        0x2A6125, 0x7A4F00, 0xCBC2AD, rgba(74, 62, 38, 0.32f),
        listOf(0xC9480F, 0x24211B, 0x2A6125, 0x5A7A4A, 0x7A4F00), 0xDDD6C5,
        Color(74, 62, 38), rgba(36, 33, 27, 0.4f),
    ),
    brushes = brushes(false),
    shadows = shadows(false),
    spec = FinioSpec(VisualTheme.Ganak, 0.dp, NotesLight),
)

private val GanakDark = ThemePalette(
    colors = colors(
        true, 0x16171A, 0xEBE7DC, 0x0F1012, 0x222327, 0xFF9461, 0x1A0D05,
        0x28292E, 0x1D1E22, 0xA7A193, 0x2A2B30, 0xFF6B5E, 0x1A0705,
        0x71DC8F, 0xF2B84B, 0x2A2B30, rgba(255, 255, 255, 0.16f),
        listOf(0xFF7A3D, 0x5EF2C4, 0x71DC8F, 0xEBE7DC, 0xF2B84B), 0x1D1E22,
        Color(0, 0, 0), rgba(0, 0, 0, 0.65f),
    ),
    brushes = brushes(true),
    shadows = shadows(true),
    spec = FinioSpec(VisualTheme.Ganak, 0.dp, NotesDark),
)

// ───────────────────────────── seven-segment digits ─────────────────────────────

// Segment geometry in viewBox units, ported from the prototype's segDigits(): a 48×88 digit,
// 10-unit strokes, a 64-unit advance, slanted 7°.
private const val SW = 48f
private const val SH = 88f
private const val ST = 10f
private const val SG = 1.6f
private const val ADV = 64f
private const val SL = 12f
private val SKEW = tan(Math.toRadians(7.0)).toFloat()

private fun hz(x0: Float, x1: Float, y: Float) = listOf(
    x0 to y, x0 + ST / 2 to y - ST / 2, x1 - ST / 2 to y - ST / 2, x1 to y, x1 - ST / 2 to y + ST / 2, x0 + ST / 2 to y + ST / 2,
)
private fun vt(x: Float, y0: Float, y1: Float) = listOf(
    x to y0, x + ST / 2 to y0 + ST / 2, x + ST / 2 to y1 - ST / 2, x to y1, x - ST / 2 to y1 - ST / 2, x - ST / 2 to y0 + ST / 2,
)

private val SEGMENTS: Map<Char, List<Pair<Float, Float>>> = run {
    val l = ST / 2
    val r = SW - ST / 2
    val m = SH / 2
    mapOf(
        'a' to hz(l + SG, r - SG, ST / 2),
        'b' to vt(r, ST / 2 + SG, m - SG),
        'c' to vt(r, m + SG, SH - ST / 2 - SG),
        'd' to hz(l + SG, r - SG, SH - ST / 2),
        'e' to vt(l, m + SG, SH - ST / 2 - SG),
        'f' to vt(l, ST / 2 + SG, m - SG),
        'g' to hz(l + SG, r - SG, m),
    )
}
private val DOT = (SW + 6f).let { px -> listOf(px to SH - 8f, px + 8f to SH - 8f, px + 8f to SH, px to SH) }
private val COMMA_TAIL = (SW + 6f).let { px -> listOf(px + 5f to SH, px + 8f to SH, px + 3f to SH + 9f, px to SH + 9f) }
private val DIGITS = listOf("abcdef", "bc", "abdeg", "abcdg", "bcfg", "acdfg", "acdefg", "abc", "abcdefg", "abcdfg")

private class Cell(val on: String, var sep: Char? = null)

/** Same parsing as the web's GanakDisplay: masks (•) and minus signs are dashes; ₹ is set in type. */
private fun cells(text: String, min: Int = 6): List<Cell> {
    val out = mutableListOf<Cell>()
    for (ch in text) {
        when {
            ch in '0'..'9' -> out += Cell(DIGITS[ch - '0'])
            ch == '•' || ch == '-' || ch == '−' -> out += Cell("g")
            (ch == ',' || ch == '.') && out.isNotEmpty() -> out.last().sep = ch
        }
    }
    while (out.size < min) out.add(0, Cell(""))
    return out
}

/** Power-on test (88,88,888 for a beat) — once per process, like the web's once per session. */
private var poweredOn = false

// ───────────────────────────── the kit ─────────────────────────────

object GanakKit : ThemeKit {
    override val visual = VisualTheme.Ganak
    override fun palette(dark: Boolean): ThemePalette = if (dark) GanakDark else GanakLight
    override val type: FinioTypeScale get() = GanakTypeScale
    override val shapes: FinioShapeScale get() = GanakShapeScale
    override fun coinInk(dark: Boolean) = if (dark) c(0x1A0D05) else Color.White
    override val coinShape: Shape get() = CircleShape
    override val fabBottom: Dp get() = 96.dp

    // ── LCD bar graph tracks ──
    override val progressTrackHeight: Dp get() = 14.dp

    @Composable
    override fun progressDecoration(): Modifier {
        val m = GanakMaterials(FinioTheme.colors.isDark)
        return Modifier.drawWithContent {
            val r = CornerRadius(4.dp.toPx())
            drawRoundRect(m.lcd, cornerRadius = r)
            val inX = 4.dp.toPx()
            val inY = 3.dp.toPx()
            val on = 7.dp.toPx()
            val off = 2.5.dp.toPx()
            val h = size.height - inY * 2
            var x = inX
            while (x < size.width - inX) {
                drawRect(m.ghost, Offset(x, inY), Size(minOf(on, size.width - inX - x), h))
                x += on + off
            }
            clipRect(inX, inY, size.width - inX, size.height - inY) { this@drawWithContent.drawContent() }
            // Cut the fill into segments with the window's own colour.
            x = inX + on
            while (x < size.width - inX) {
                drawRect(m.lcd, Offset(x, inY), Size(off, h))
                x += on + off
            }
            drawRoundRect(m.bezel, cornerRadius = r, style = Stroke(2.dp.toPx()))
        }
    }

    // ── hero: a recessed panel holding the LCD window ──
    @Composable
    override fun heroContainer(shape: Shape): Modifier {
        val colors = FinioTheme.colors
        val m = GanakMaterials(colors.isDark)
        val panel = RoundedCornerShape(16.dp)
        return Modifier
            .clip(panel)
            .background(colors.card)
            .cssInsetShadow(panel, listOf(CssShadow(y = 2.dp, blur = 3.dp, color = m.shade, inset = true)))
            .drawBehind { drawLine(m.edge, Offset(16.dp.toPx(), size.height - 0.5.dp.toPx()), Offset(size.width - 16.dp.toPx(), size.height - 0.5.dp.toPx()), 1.dp.toPx()) }
    }

    override fun heroPadding(wide: Boolean): PaddingValues =
        if (wide) PaddingValues(24.dp, 20.dp, 24.dp, 22.dp) else PaddingValues(14.dp, 14.dp, 14.dp, 16.dp)

    @Composable
    override fun HeroFigure(text: String, style: TextStyle, color: Color, whole: Boolean, modifier: Modifier): Boolean {
        val m = GanakMaterials(FinioTheme.colors.isDark)
        val reduced = rememberReducedMotion()
        val boot = remember { Animatable(if (poweredOn || reduced) 1f else 0f) }
        LaunchedEffect(Unit) {
            poweredOn = true
            if (boot.value < 1f) boot.animateTo(1f, tween(900, easing = LinearEasing))
        }
        val cs = remember(text) { cells(text) }
        val lcdShape = RoundedCornerShape(8.dp)
        Column(
            modifier
                .padding(top = 4.dp, bottom = 8.dp)
                .fillMaxWidth()
                .clearAndSetSemantics { contentDescription = text }
                .drawBehind {
                    // Bezel and its edge highlight, outside the window.
                    drawRoundRect(m.edge, Offset(-4.dp.toPx(), -4.dp.toPx()), Size(size.width + 8.dp.toPx(), size.height + 8.dp.toPx()), CornerRadius(12.dp.toPx()))
                    drawRoundRect(m.bezel, Offset(-3.dp.toPx(), -3.dp.toPx()), Size(size.width + 6.dp.toPx(), size.height + 6.dp.toPx()), CornerRadius(11.dp.toPx()))
                }
                .clip(lcdShape)
                .background(CssLinearGradient(180f, listOf(Stop(0f, m.lcdTop), Stop(0.22f, m.lcd))))
                .cssInsetShadow(lcdShape, listOf(CssShadow(y = 2.dp, blur = 4.dp, color = rgba(0, 0, 0, if (FinioTheme.colors.isDark) 0.8f else 0.35f), inset = true)))
                .padding(start = 12.dp, end = 12.dp, top = 7.dp, bottom = 10.dp),
        ) {
            val ann = FinioType.micro.copy(fontFamily = ArchivoWide, fontSize = 9.sp, lineHeight = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.08.em)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("M", style = ann, color = m.seg)
                Text("MONTH", style = ann, color = m.seg)
                Spacer(Modifier.weight(1f))
                Text("E", style = ann, color = m.ghost)
            }
            BoxWithConstraints(Modifier.fillMaxWidth().padding(top = 7.dp)) {
                val vbW = SL + cs.size * ADV
                val vbH = SH + 10f
                val rupee = 24.dp
                val h = minOf(58.dp, (maxWidth - rupee) * (vbH / vbW))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Text(
                        "₹",
                        Modifier.width(rupee).padding(top = 2.dp),
                        style = TextStyle(fontFamily = Archivo, fontSize = 22.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
                        color = m.seg,
                    )
                    Canvas(Modifier.width(h * (vbW / vbH)).height(h)) {
                        val k = size.height / vbH
                        val t = boot.value
                        val allLit = t in 0.12f..0.52f
                        val path = Path()
                        fun poly(pts: List<Pair<Float, Float>>, ox: Float, lit: Boolean) {
                            path.reset()
                            pts.forEachIndexed { i, (x, y) ->
                                val px = (ox + x - SKEW * y) * k + SKEW * SH * k
                                val py = y * k
                                if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                            }
                            path.close()
                            val showLit = if (t < 0.12f) false else lit || allLit
                            drawPath(path, if (showLit) m.seg else m.ghost)
                        }
                        cs.forEachIndexed { i, cell ->
                            val ox = SL + i * ADV - SKEW * SH
                            for ((key, pts) in SEGMENTS) poly(pts, ox, cell.on.contains(key))
                            poly(DOT, ox, cell.sep != null)
                            if (cell.sep == ',') poly(COMMA_TAIL, ox, true)
                        }
                    }
                }
            }
        }
        return true
    }

    // ── chrome: a row of rubber function keys, the active one pressed with its LED lit ──
    @Composable
    override fun TabBar(active: FinioTab?, onSelect: (FinioTab) -> Unit, modifier: Modifier) {
        val colors = FinioTheme.colors
        val m = GanakMaterials(colors.isDark)
        val keyShape = RoundedCornerShape(9.dp)
        Row(
            modifier
                .fillMaxWidth()
                .background(m.body2)
                .drawBehind {
                    drawLine(colors.border, Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx())
                    drawLine(m.edge, Offset(0f, 1.dp.toPx()), Offset(size.width, 1.dp.toPx()), 1.dp.toPx())
                }
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(start = 10.dp, end = 10.dp, top = 9.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FinioTab.entries.forEach { tab ->
                val selected = tab == active
                val icon = remember(tab) { tab.icon(2f) }
                val tint = if (selected) colors.foreground else colors.mutedForeground
                Box(
                    Modifier
                        .weight(1f)
                        .offset(y = if (selected) 1.5.dp else 0.dp)
                        .cssShadow(keyShape, if (selected) listOf(CssShadow(y = 1.dp, color = m.keyLip)) else m.keyShadow(2.5.dp))
                        .clip(keyShape)
                        .background(m.key)
                        .cssInsetShadow(keyShape, listOf(CssShadow(y = 1.dp, color = m.keyHi, inset = true)))
                        .tabClickable(selected) { onSelect(tab) },
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(top = 7.dp, bottom = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Icon(icon, contentDescription = null, modifier = Modifier.size(19.dp), tint = tint)
                        Text(
                            tab.label,
                            style = FinioType.micro.copy(fontFamily = ArchivoWide, fontWeight = FontWeight.Bold),
                            color = tint,
                            maxLines = 1,
                        )
                    }
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 5.dp, end = 6.dp)
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(if (selected) m.led else m.ledOff),
                    )
                }
            }
        }
    }
}

