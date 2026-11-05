package com.slowatcoding.finio.ui.theme.kits

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.slowatcoding.finio.R
import com.slowatcoding.finio.ui.mudra.Denomination
import com.slowatcoding.finio.ui.mudra.NoteStyle
import com.slowatcoding.finio.ui.theme.CssRepeatingLinearGradient
import com.slowatcoding.finio.ui.theme.CssShadow
import com.slowatcoding.finio.ui.theme.FinioBrushes
import com.slowatcoding.finio.ui.theme.FinioColors
import com.slowatcoding.finio.ui.theme.FinioShadows
import com.slowatcoding.finio.ui.theme.FinioSpec
import com.slowatcoding.finio.ui.theme.Stop
import com.slowatcoding.finio.ui.theme.ThemePalette
import com.slowatcoding.finio.ui.theme.VisualTheme
import com.slowatcoding.finio.ui.theme.stock

/*
 * Parchi's palette and faces — web/src/themes/parchi.css. Light is warm thermal paper on a darker
 * counter with one receipt red; dark is the carbon copy (blue-black paper, pale blue print).
 */

private fun c(rgb: Long) = Color(0xFF000000 or rgb)
private fun solid(rgb: Long): Brush = SolidColor(c(rgb))
private fun rgba(r: Int, g: Int, b: Int, a: Float) = Color(r, g, b, (a * 255f + 0.5f).toInt())

/** The receipt's own inks, beyond the shared tokens (web `--parchi-*`). */
internal class ParchiInks(
    val counter: Color,
    val paper: Color,
    val ink: Color,
    val rule: Color,
    val faint: Color,
    val red: Color,
    val onRed: Color,
    val slot: Color,
    val band: Color,
) {
    companion object {
        val Light = ParchiInks(
            counter = c(0xE3DDCF), paper = c(0xF7F4EC), ink = c(0x23211C), rule = c(0xA59D8A), faint = c(0xDDD6C6),
            red = c(0xC8242B), onRed = c(0xFBF7EF), slot = c(0x2B2925), band = rgba(90, 70, 30, 0.035f),
        )
        val Dark = ParchiInks(
            counter = c(0x0B0D13), paper = c(0x181C27), ink = c(0xDFE6F5), rule = c(0x4A5470), faint = c(0x2A3142),
            red = c(0xFF7A6E), onRed = c(0x130C0E), slot = Color.Black, band = rgba(150, 180, 255, 0.03f),
        )
        fun of(dark: Boolean) = if (dark) Dark else Light
    }
}

// ── Martian Mono, riding its width axis: 75% condensed for line items, 112.5% wide for totals ──

private fun mono(weight: Int, width: Float) = Font(
    resId = R.font.parchi_martian_mono,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight), FontVariation.width(width)),
)

/** Body width (87.5%) — everything people read or type. */
internal val ParchiMono = FontFamily(mono(400, 87.5f), mono(500, 87.5f), mono(600, 87.5f), mono(700, 87.5f), mono(800, 87.5f))

/** Wide (112.5%) — titles and totals. */
internal val ParchiWide = FontFamily(mono(500, 112.5f), mono(600, 112.5f), mono(700, 112.5f), mono(800, 112.5f))

/** Condensed (75%) — line-item labels, the stamp, tab labels. */
internal val ParchiCondensed = FontFamily(mono(400, 75f), mono(500, 75f), mono(600, 75f), mono(700, 75f), mono(800, 75f))

// ── palette ──

/** Every denomination prints on the same paper: a receipt is monochrome. */
private fun paperNotes(paper: Long, ink: Long) =
    Denomination.entries.associateWith { NoteStyle(c(paper), c(paper), c(ink)) }

/** A dot-matrix fill: [ink] 4dp on, 2dp off. */
private fun dots(ink: Long): (Float) -> Brush = { density ->
    CssRepeatingLinearGradient(
        angleDegrees = 90f,
        periodPx = 6f * density,
        stops = listOf(Stop(0f, c(ink)), Stop(4f / 6f, c(ink)), Stop(4f / 6f, Color.Transparent), Stop(1f, Color.Transparent)),
    )
}

private fun palette(
    dark: Boolean,
    background: Long, foreground: Long, card: Long, muted: Long, mutedFg: Long, accent: Long,
    primary: Long, primaryFg: Long, positive: Long, warning: Long, border: Long, input: Long,
    warningBand: Long, gradPrimary: Long, softPrimary: Long, successSoft: Long, dangerSoft: Long, info: Long,
    shadowTint: Color, shade: Color, scrim: Color,
): ThemePalette {
    val colors = FinioColors(
        isDark = dark,
        background = c(background), foreground = c(foreground),
        card = c(card), cardForeground = c(foreground),
        popover = c(card), popoverForeground = c(foreground),
        primary = c(primary), primaryForeground = c(primaryFg),
        secondary = c(muted), secondaryForeground = c(foreground),
        muted = c(muted), mutedForeground = c(mutedFg),
        accent = c(accent), accentForeground = c(foreground),
        destructive = c(primary), destructiveForeground = c(primaryFg),
        positive = c(positive), warning = c(warning),
        border = c(border), input = c(input), ring = c(primary),
        chart1 = c(foreground), chart2 = c(primary), chart3 = c(positive), chart4 = c(warning), chart5 = c(mutedFg),
        sidebar = c(card), sidebarForeground = c(foreground),
        sidebarPrimary = c(primary), sidebarPrimaryForeground = c(primaryFg),
        sidebarAccent = c(foreground), sidebarAccentForeground = c(card),
        sidebarBorder = c(border), sidebarRing = c(primary),
        warningBand = c(warningBand), warningBandForeground = c(foreground), warningBandAccent = c(primary),
        engraving = Color.Transparent,
        glass = c(card), glassStrong = c(card), glassBorder = c(input), glassHighlight = Color.Transparent,
        shadowTint = shadowTint, scrim = scrim,
    )
    val drop = listOf(CssShadow(y = 1.5.dp, blur = 1.dp, color = shade))
    val s = stock(shadowTint)
    return ThemePalette(
        colors = colors,
        brushes = FinioBrushes(
            gradPrimary = solid(gradPrimary), gradPrimarySoft = solid(softPrimary),
            gradSuccess = solid(0x2E6A3D), gradSuccessSoft = solid(successSoft),
            gradDanger = solid(gradPrimary), gradDangerSoft = solid(dangerSoft),
            gradInfo = solid(info), gradWarning = solid(0x855506),
            gradSurface = solid(card), paper = SolidColor(Color.Transparent),
            coin = solid(primary), register = dots(foreground), thread = solid(foreground),
        ),
        shadows = FinioShadows(
            card = drop, float = drop, glowPrimary = emptyList(), glowSuccess = emptyList(), glowDanger = emptyList(),
            coin = emptyList(), tile = drop, tabBar = emptyList(), noteChip = emptyList(),
            xs = s[0], sm = s[1], md = s[2], lg = s[3], xl = s[4],
        ),
        // Slips need no outline — paper on the counter is the edge; outlined controls keep the ink.
        spec = FinioSpec(VisualTheme.Parchi, 0.dp, paperNotes(card, foreground)),
    )
}

internal val ParchiLight = palette(
    dark = false,
    background = 0xE3DDCF, foreground = 0x23211C, card = 0xF7F4EC, muted = 0xEFEADD, mutedFg = 0x635D4F, accent = 0xEFEADD,
    primary = 0xC8242B, primaryFg = 0xFBF7EF, positive = 0x2E6A3D, warning = 0x855506, border = 0xDDD6C6, input = 0x23211C,
    warningBand = 0xF6E4DC, gradPrimary = 0xC8242B, softPrimary = 0xEFEADD, successSoft = 0xE3EDDD, dangerSoft = 0xF6E4DC,
    info = 0x23211C, shadowTint = Color(60, 45, 20), shade = rgba(60, 45, 20, 0.22f), scrim = rgba(35, 33, 28, 0.4f),
)

// Filled actions keep white text (call sites hardcode it), so the dark gradPrimary stays a deep red.
internal val ParchiDark = palette(
    dark = true,
    background = 0x0B0D13, foreground = 0xDFE6F5, card = 0x181C27, muted = 0x141822, mutedFg = 0x909BB4, accent = 0x232938,
    primary = 0xFF7A6E, primaryFg = 0x130C0E, positive = 0x7FCF9A, warning = 0xE8B45A, border = 0x2A3142, input = 0x8A94AD,
    warningBand = 0x2A1719, gradPrimary = 0xB8262E, softPrimary = 0x141822, successSoft = 0x14261D, dangerSoft = 0x2A1719,
    info = 0x2C3550, shadowTint = Color(0, 0, 0), shade = rgba(0, 0, 0, 0.6f), scrim = rgba(0, 0, 0, 0.62f),
)
