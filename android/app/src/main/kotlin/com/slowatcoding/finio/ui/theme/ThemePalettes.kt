package com.slowatcoding.finio.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.slowatcoding.finio.ui.mudra.Denomination
import com.slowatcoding.finio.ui.mudra.NoteStyle

/**
 * Per-theme values that are not colours, brushes or shadows: metrics components read, plus the
 * denomination tints. [visual] is the one thing a component may branch on for a signature piece.
 */
@Immutable
data class FinioSpec(
    val visual: VisualTheme = VisualTheme.Mudra,
    /** The hairline (or outline) around cards, tiles and outlined buttons. */
    val borderWidth: Dp = 1.dp,
    /** Account note tints for this theme; null means Mudra's own denomination colours. */
    val notes: Map<Denomination, NoteStyle>? = null,
)

val LocalFinioSpec = staticCompositionLocalOf { FinioSpec() }

/** Everything [FinioTheme] provides for one (theme, mode). */
@Immutable
class ThemePalette(val colors: FinioColors, val brushes: FinioBrushes, val shadows: FinioShadows, val spec: FinioSpec)

fun finioPalette(visual: VisualTheme, dark: Boolean, amoled: Boolean): ThemePalette {
    val black = dark && amoled
    com.slowatcoding.finio.ui.theme.kits.kitFor(visual)?.let { kit ->
        val p = kit.palette(dark)
        return if (black) p.amoled() else p
    }
    return when (visual) {
        VisualTheme.Mudra -> ThemePalette(
            colors = if (black) AmoledFinioColors else if (dark) DarkFinioColors else LightFinioColors,
            brushes = if (black) AmoledFinioBrushes else if (dark) DarkFinioBrushes else LightFinioBrushes,
            shadows = if (black) AmoledFinioShadows else if (dark) DarkFinioShadows else LightFinioShadows,
            spec = FinioSpec(),
        )
        else -> {
            val p = when (visual) {
                VisualTheme.Khata -> if (dark) KhataDark else KhataLight
                VisualTheme.Gulal -> if (dark) GulalDark else GulalLight
                VisualTheme.Shunya -> if (dark) ShunyaDark else ShunyaLight
                else -> if (dark) BaghDark else BaghLight
            }
            if (black) p.amoled() else p
        }
    }
}

/** `html.dark.amoled[data-visual-theme]`: a true-black field on any theme. */
private fun ThemePalette.amoled() = ThemePalette(
    colors.copy(isAmoled = true, background = Color.Black),
    brushes.copy(paper = SolidColor(Color.Transparent)),
    shadows,
    spec,
)

// ───────────────────────────── helpers ─────────────────────────────

private fun c(rgb: Long) = Color(0xFF000000 or rgb)
private fun solid(rgb: Long): Brush = SolidColor(c(rgb))
private fun grad(a: Long, b: Long): Brush = CssLinearGradient(135f, listOf(Stop(0f, c(a)), Stop(1f, c(b))))

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

private fun shadowsOf(
    card: List<CssShadow>, float: List<CssShadow>, glow: List<CssShadow>, coin: List<CssShadow>,
    tile: List<CssShadow>, tabBar: List<CssShadow>, tint: Color,
) = stock(tint).let { s ->
    FinioShadows(
        card = card, float = float, glowPrimary = glow, glowSuccess = glow, glowDanger = glow,
        coin = coin, tile = tile, tabBar = tabBar, noteChip = emptyList(),
        xs = s[0], sm = s[1], md = s[2], lg = s[3], xl = s[4],
    )
}

private fun brushesOf(
    primary: Brush, primarySoft: Brush, success: Brush, successSoft: Brush, danger: Brush, dangerSoft: Brush,
    info: Brush, warning: Brush, surface: Brush, coin: Brush, register: (Float) -> Brush, thread: Brush,
) = FinioBrushes(
    gradPrimary = primary, gradPrimarySoft = primarySoft, gradSuccess = success, gradSuccessSoft = successSoft,
    gradDanger = danger, gradDangerSoft = dangerSoft, gradInfo = info, gradWarning = warning,
    gradSurface = surface, paper = SolidColor(Color.Transparent), coin = coin, register = register, thread = thread,
)

/** A repeating tick pattern across the fill: [inkWidth] on, the rest of [period] off (in dp). */
private fun ticks(ink: Long, period: Float, inkWidth: Float): (Float) -> Brush = { density ->
    CssRepeatingLinearGradient(
        angleDegrees = 90f,
        periodPx = period * density,
        stops = listOf(
            Stop(0f, c(ink)), Stop(inkWidth / period, c(ink)),
            Stop(inkWidth / period, Color.Transparent), Stop(1f, Color.Transparent),
        ),
    )
}

private fun hatch(a: Long, b: Long): (Float) -> Brush = { density ->
    CssRepeatingLinearGradient(
        angleDegrees = 45f,
        periodPx = 18f * density,
        stops = listOf(Stop(0f, c(a)), Stop(0.5f, c(a)), Stop(0.5f, c(b)), Stop(1f, c(b))),
    )
}

private fun hard(color: Color, d: Float) = listOf(CssShadow(x = d.dp, y = d.dp, color = color))

private fun notes(vararg tints: Triple<Denomination, Pair<Long, Long>, Long>) =
    tints.associate { (d, bg, ink) -> d to NoteStyle(c(bg.first), c(bg.second), c(ink)) }

// ───────────────────────────── Khata ─────────────────────────────

private val KhataNotesLight = notes(
    Triple(Denomination.Rs100, 0xD4E0F2L to 0xC3D3ECL, 0x14234AL),
    Triple(Denomination.Rs500, 0xDBE8CCL to 0xCBDCB8L, 0x1F4A2AL),
    Triple(Denomination.Rs2000, 0xF3D9D5L to 0xECC3BDL, 0x8A1D16L),
    Triple(Denomination.Rs200, 0xF3E6BDL to 0xEAD69AL, 0x5C4300L),
    Triple(Denomination.Rs10, 0xEBDCC7L to 0xDFC8AAL, 0x4D3217L),
    Triple(Denomination.Rs50, 0xD2E8ECL to 0xBDDDE3L, 0x0D4F60L),
    Triple(Denomination.Rs20, 0xE2ECC2L to 0xD3E2A4L, 0x485A0CL),
)
private val KhataNotesDark = notes(
    Triple(Denomination.Rs100, 0x263A63L to 0x1D2D4FL, 0xD6E2FFL),
    Triple(Denomination.Rs500, 0x2A4332L to 0x213627L, 0xDCECD2L),
    Triple(Denomination.Rs2000, 0x4A2622L to 0x3A1C19L, 0xFFD8D2L),
    Triple(Denomination.Rs200, 0x4A3D12L to 0x3A300DL, 0xFFE9A8L),
    Triple(Denomination.Rs10, 0x3D2E1EL to 0x2F2317L, 0xF2DCC2L),
    Triple(Denomination.Rs50, 0x1C424AL to 0x15333AL, 0xC8EEF5L),
    Triple(Denomination.Rs20, 0x3A4818L to 0x2E3A12L, 0xE8F5B8L),
)

private val KhataLight = ThemePalette(
    colors = colorsOf(
        false, 0xE3ECD9, 0x14234A, 0xEEF4E6, 0xF3F8EC, 0x14234A, 0xF3F7EC, 0xD5E2CA, 0xD5E2CA, 0x46556E,
        0xCDDBC1, 0x14234A, 0xB8322A, 0xFFFFFF, 0x1B6A42, 0x8A5A00,
        border = c(0xB4C9AB), input = rgba(20, 35, 74, 0.45f),
        charts = listOf(0x14234A, 0xB8322A, 0x8A5A00, 0x1B6A42, 0x3D6AA8),
        warningBand = 0xF6E0DC, warningBandFg = 0x5A1410, warningBandAccent = 0xB8322A,
        glass = c(0xEEF4E6), glassStrong = c(0xEEF4E6), glassBorder = rgba(20, 35, 74, 0.5f),
        shadowTint = Color(20, 35, 74), scrim = rgba(14, 24, 52, 0.35f),
    ),
    brushes = brushesOf(
        solid(0x14234A), solid(0xD5E2CA), solid(0x1B6A42), solid(0xD2E8D6), solid(0xB8322A), solid(0xF2D6D2),
        solid(0x3D6AA8), solid(0x8A5A00), solid(0xEEF4E6), solid(0xB8322A), ticks(0x14234A, 7f, 2f), solid(0x14234A),
    ),
    shadows = shadowsOf(
        card = emptyList(), float = listOf(CssShadow(y = 14.dp, blur = 28.dp, spread = (-14).dp, color = rgba(20, 35, 74, 0.5f))),
        glow = emptyList(), coin = listOf(CssShadow(y = 8.dp, blur = 16.dp, spread = (-8).dp, color = rgba(10, 20, 50, 0.55f))),
        tile = emptyList(), tabBar = emptyList(), tint = Color(20, 35, 74),
    ),
    spec = FinioSpec(VisualTheme.Khata, 1.dp, KhataNotesLight),
)

private val KhataDark = ThemePalette(
    colors = colorsOf(
        true, 0x0F1B17, 0xE4EEDE, 0x15241F, 0x1A2C26, 0xA9BDF5, 0x0F1B17, 0x1D3029, 0x1D3029, 0x9DB2A4,
        0x24392F, 0xE4EEDE, 0xFF7B6C, 0x1A0E0C, 0x6FD79D, 0xF0C15A,
        border = c(0x2A4036), input = rgba(228, 238, 222, 0.35f),
        charts = listOf(0xA9BDF5, 0xFF7B6C, 0xF0C15A, 0x6FD79D, 0x7FB0E8),
        warningBand = 0x3A1B17, warningBandFg = 0xFFE2DC, warningBandAccent = 0xFF7B6C,
        glass = c(0x15241F), glassStrong = c(0x15241F), glassBorder = rgba(228, 238, 222, 0.4f),
        shadowTint = Color(0, 0, 0), scrim = rgba(3, 8, 6, 0.6f),
    ),
    brushes = brushesOf(
        solid(0x34508F), solid(0x1D3029), solid(0x1E7A4C), solid(0x173A2B), solid(0xB8322A), solid(0x3A1B17),
        solid(0x3D6AA8), solid(0x8A5A00), solid(0x15241F), solid(0xFF7B6C), ticks(0xE4EEDE, 7f, 2f), solid(0xE4EEDE),
    ),
    shadows = shadowsOf(
        card = emptyList(), float = listOf(CssShadow(y = 14.dp, blur = 28.dp, spread = (-14).dp, color = rgba(0, 0, 0, 0.8f))),
        glow = emptyList(), coin = emptyList(), tile = emptyList(), tabBar = emptyList(), tint = Color(0, 0, 0),
    ),
    spec = FinioSpec(VisualTheme.Khata, 1.dp, KhataNotesDark),
)

// ───────────────────────────── Gulal ─────────────────────────────

private val GulalNotes = notes(
    Triple(Denomination.Rs100, 0xBDA9FFL to 0xAD97FBL, 0x1B1230L),
    Triple(Denomination.Rs500, 0x72E8C2L to 0x5DDCB2L, 0x1B1230L),
    Triple(Denomination.Rs2000, 0xFFAB5CL to 0xFF9A3DL, 0x1B1230L),
    Triple(Denomination.Rs200, 0xFFD23FL to 0xFFC21AL, 0x1B1230L),
    Triple(Denomination.Rs10, 0xFF9FCDL to 0xFF8AC2L, 0x1B1230L),
    Triple(Denomination.Rs50, 0x82DCFFL to 0x6FD0FFL, 0x1B1230L),
    Triple(Denomination.Rs20, 0xCBF47FL to 0xB9EA5EL, 0x1B1230L),
)

private val GulalBlue = grad(0x5568FF, 0x3A4BF0)

private val GulalLight = ThemePalette(
    colors = colorsOf(
        false, 0xFFE14D, 0x1B1230, 0xFFFFFF, 0xFFFFFF, 0x4457FF, 0xFFFFFF, 0xFFF3A6, 0xFFF3A6, 0x5A4780,
        0xFFB8DC, 0x1B1230, 0xD6154F, 0xFFFFFF, 0x05714A, 0x8A4B00,
        border = rgba(27, 18, 48, 0.3f), input = c(0x1B1230),
        charts = listOf(0x4457FF, 0xFF5FA8, 0xFF9A3D, 0x1FB386, 0x8B5CFF),
        warningBand = 0x1B1230, warningBandFg = 0xFFE14D, warningBandAccent = 0xFF9A3D,
        glass = c(0xFFFFFF), glassStrong = c(0xFFFFFF), glassBorder = c(0x1B1230),
        shadowTint = Color(27, 18, 48), scrim = rgba(27, 18, 48, 0.45f),
    ),
    brushes = brushesOf(
        GulalBlue, grad(0xE3E6FF, 0xD3D8FF), grad(0x17B982, 0x079468), grad(0xC9F7E3, 0xB3F0D4),
        grad(0xF0306B, 0xD6154F), grad(0xFFD3DF, 0xFFBDD0), grad(0x5AB6FF, 0x3A9BE8), grad(0xFFAA4D, 0xF08A1C),
        solid(0xFFFFFF), GulalBlue, hatch(0x4457FF, 0x6B7AFF), hatch(0xFF5FA8, 0xFF8CC2)(3f),
    ),
    shadows = shadowsOf(
        card = hard(c(0x1B1230), 4f), float = hard(c(0x1B1230), 6f), glow = hard(c(0x1B1230), 3f),
        coin = hard(c(0x1B1230), 4f), tile = hard(c(0x1B1230), 4f), tabBar = hard(c(0x1B1230), 4f), tint = Color(27, 18, 48),
    ),
    spec = FinioSpec(VisualTheme.Gulal, 2.5.dp, GulalNotes),
)

private val GulalDark = ThemePalette(
    colors = colorsOf(
        true, 0x1C1233, 0xFFF3D6, 0x2B1E4B, 0x2B1E4B, 0x9AA6FF, 0x1C1233, 0x3A2A63, 0x33255A, 0xC8B9EC,
        0x4B3480, 0xFFF3D6, 0xFF7A8A, 0x2B0A14, 0x5BE0A8, 0xFFC83D,
        border = rgba(255, 243, 214, 0.25f), input = rgba(255, 243, 214, 0.6f),
        charts = listOf(0x9AA6FF, 0xFF7FBB, 0xFFC83D, 0x5BE0A8, 0x6FD0FF),
        warningBand = 0xFFF3D6, warningBandFg = 0x1C1233, warningBandAccent = 0xD6154F,
        glass = c(0x2B1E4B), glassStrong = c(0x2B1E4B), glassBorder = c(0xFFF3D6),
        shadowTint = Color(0, 0, 0), scrim = rgba(8, 4, 20, 0.62f),
    ),
    brushes = brushesOf(
        GulalBlue, grad(0x3A2F78, 0x2F2565), grad(0x17B982, 0x079468), grad(0x17463A, 0x11382E),
        grad(0xF0306B, 0xD6154F), grad(0x532244, 0x431A37), grad(0x5AB6FF, 0x3A9BE8), grad(0xFFAA4D, 0xF08A1C),
        solid(0x2B1E4B), GulalBlue, hatch(0x4457FF, 0x6B7AFF), hatch(0xFF5FA8, 0xFF8CC2)(3f),
    ),
    shadows = shadowsOf(
        card = hard(c(0xFF5FA8), 4f), float = hard(c(0xFF5FA8), 6f), glow = hard(c(0xFF5FA8), 3f),
        coin = hard(c(0xFF5FA8), 4f), tile = hard(c(0xFF5FA8), 4f), tabBar = hard(c(0xFF5FA8), 4f), tint = Color(0, 0, 0),
    ),
    spec = FinioSpec(VisualTheme.Gulal, 2.5.dp, GulalNotes),
)

// ───────────────────────────── Shunya ─────────────────────────────

private fun shunyaNotes(ink: Long, a: Long, b: Long, cc: Long, d: Long, e: Long, f: Long, g: Long) = notes(
    Triple(Denomination.Rs100, a to a, ink), Triple(Denomination.Rs500, b to b, ink),
    Triple(Denomination.Rs2000, cc to cc, ink), Triple(Denomination.Rs200, d to d, ink),
    Triple(Denomination.Rs10, e to e, ink), Triple(Denomination.Rs50, f to f, ink), Triple(Denomination.Rs20, g to g, ink),
)

private val ShunyaLight = ThemePalette(
    colors = colorsOf(
        false, 0xFCFCFA, 0x0F0F10, 0xFCFCFA, 0xFFFFFF, 0x2740FF, 0xFFFFFF, 0xF1F1ED, 0xF1F1ED, 0x6D6D68,
        0xF1F1ED, 0x0F0F10, 0xC7361F, 0xFFFFFF, 0x12805A, 0x9A6200,
        border = c(0xE9E9E3), input = c(0xCFCFC8),
        charts = listOf(0x0F0F10, 0x2740FF, 0x12805A, 0xC7361F, 0x9A6200),
        warningBand = 0xF7ECE9, warningBandFg = 0x5A1A10, warningBandAccent = 0xC7361F,
        glass = Color.Transparent, glassStrong = c(0xFCFCFA), glassBorder = c(0xE9E9E3),
        shadowTint = Color(15, 15, 16), scrim = rgba(15, 15, 16, 0.3f),
    ),
    brushes = brushesOf(
        solid(0x0F0F10), solid(0xF1F1ED), solid(0x12805A), solid(0xE4F3EC), solid(0xC7361F), solid(0xF9E6E2),
        solid(0x2740FF), solid(0x9A6200), solid(0xFCFCFA), solid(0x0F0F10), { _ -> solid(0x0F0F10) }, solid(0x0F0F10),
    ),
    shadows = shadowsOf(
        card = emptyList(), float = listOf(CssShadow(y = 16.dp, blur = 40.dp, spread = (-20).dp, color = rgba(15, 15, 16, 0.35f))),
        glow = emptyList(), coin = emptyList(), tile = emptyList(), tabBar = emptyList(), tint = Color(15, 15, 16),
    ),
    spec = FinioSpec(
        VisualTheme.Shunya, 1.dp,
        shunyaNotes(0x0F0F10, 0xF2F3FB, 0xF1F5EE, 0xFBF0ED, 0xFAF5E5, 0xF6F0E8, 0xEEF6F8, 0xF4F8E8),
    ),
)

private val ShunyaDark = ThemePalette(
    colors = colorsOf(
        true, 0x0A0A0B, 0xF3F3EF, 0x0A0A0B, 0x141416, 0x8F9DFF, 0x0A0A0B, 0x18181A, 0x18181A, 0x9A9A94,
        0x1C1C1F, 0xF3F3EF, 0xFF8A76, 0x1A0A07, 0x5FD3A0, 0xF0BD5A,
        border = c(0x232325), input = c(0x3A3A3D),
        charts = listOf(0xF3F3EF, 0x8F9DFF, 0x5FD3A0, 0xFF8A76, 0xF0BD5A),
        warningBand = 0x25120F, warningBandFg = 0xFFE3DD, warningBandAccent = 0xFF8A76,
        glass = Color.Transparent, glassStrong = c(0x0A0A0B), glassBorder = c(0x232325),
        shadowTint = Color(0, 0, 0), scrim = rgba(0, 0, 0, 0.6f),
    ),
    brushes = brushesOf(
        solid(0x3346E0), solid(0x18181A), solid(0x12805A), solid(0x10261D), solid(0xC7361F), solid(0x2A1410),
        solid(0x2740FF), solid(0x9A6200), solid(0x0A0A0B), solid(0xF3F3EF), { _ -> solid(0xF3F3EF) }, solid(0xF3F3EF),
    ),
    shadows = shadowsOf(
        card = emptyList(), float = listOf(CssShadow(y = 16.dp, blur = 40.dp, spread = (-20).dp, color = rgba(0, 0, 0, 0.8f))),
        glow = emptyList(), coin = emptyList(), tile = emptyList(), tabBar = emptyList(), tint = Color(0, 0, 0),
    ),
    spec = FinioSpec(
        VisualTheme.Shunya, 1.dp,
        shunyaNotes(0xF3F3EF, 0x15161F, 0x141A14, 0x1F1413, 0x1E1A10, 0x1C1712, 0x111A1C, 0x171B0F),
    ),
)

// ───────────────────────────── Bagh ─────────────────────────────

private val BaghNotesLight = notes(
    Triple(Denomination.Rs100, 0xD4ECF0L to 0xC3E0E6L, 0x16301FL),
    Triple(Denomination.Rs500, 0xDCEDC4L to 0xCCE2ACL, 0x16301FL),
    Triple(Denomination.Rs2000, 0xF8D8C6L to 0xF1C4ADL, 0x3A1F12L),
    Triple(Denomination.Rs200, 0xF9E7B0L to 0xF3D88AL, 0x4D3A00L),
    Triple(Denomination.Rs10, 0xEFDFCAL to 0xE3CDB0L, 0x4A2F15L),
    Triple(Denomination.Rs50, 0xD0E8E8L to 0xBDDEDEL, 0x0D4F50L),
    Triple(Denomination.Rs20, 0xE2F0B8L to 0xD3E69AL, 0x3D5008L),
)
private val BaghNotesDark = notes(
    Triple(Denomination.Rs100, 0x1F4048L to 0x19353CL, 0xE4F2F4L),
    Triple(Denomination.Rs500, 0x27472DL to 0x203B25L, 0xE6F3DDL),
    Triple(Denomination.Rs2000, 0x4C2F23L to 0x3D251BL, 0xFBE6DAL),
    Triple(Denomination.Rs200, 0x4A3D12L to 0x3A300DL, 0xFFE9A8L),
    Triple(Denomination.Rs10, 0x3D2E1EL to 0x2F2317L, 0xF2DCC2L),
    Triple(Denomination.Rs50, 0x1C424AL to 0x15333AL, 0xC8EEF5L),
    Triple(Denomination.Rs20, 0x3A4818L to 0x2E3A12L, 0xE8F5B8L),
)

private val GoldCoin = grad(0xFFC83D, 0xF4A000)

private val BaghLight = ThemePalette(
    colors = colorsOf(
        false, 0xE6EFDC, 0x16301F, 0xF8FAF1, 0xF8FAF1, 0x3A7A36, 0xFFFFFF, 0xD4E8BD, 0xDBE8CF, 0x4B6652,
        0xD4E8BD, 0x2A5A2D, 0xC4552F, 0xFFFFFF, 0x16683A, 0x8A5A00,
        border = c(0xD3DFC4), input = rgba(22, 48, 31, 0.28f),
        charts = listOf(0x3A7A36, 0xF4B21B, 0xC4552F, 0x6BB5C9, 0xB58AD9),
        warningBand = 0xF5D9CC, warningBandFg = 0x4A1D0D, warningBandAccent = 0xC4552F,
        glass = c(0xF8FAF1), glassStrong = c(0xF8FAF1), glassBorder = Color.Transparent,
        shadowTint = Color(35, 75, 40), scrim = rgba(22, 48, 31, 0.35f),
    ),
    brushes = brushesOf(
        grad(0x4A9244, 0x2F6B3A), grad(0xE3F0D3, 0xD4E8BD), grad(0x2F9A5C, 0x16683A), grad(0xDBF0E2, 0xC8E8D4),
        grad(0xD9693F, 0xB4451F), grad(0xFBE3D8, 0xF5CFBD), grad(0x7CC3D6, 0x4F9FB5), grad(0xF4B21B, 0xE08A1A),
        CssLinearGradient(180f, listOf(Stop(0f, c(0xCDE8EE)), Stop(1f, c(0xEAF3D4)))), GoldCoin,
        { _ -> CssLinearGradient(90f, listOf(Stop(0f, c(0x7FB069)), Stop(1f, c(0x4F9A4A)))) },
        CssLinearGradient(90f, listOf(Stop(0f, c(0x7FB069)), Stop(1f, c(0x3F8A45)))),
    ),
    shadows = shadowsOf(
        card = listOf(CssShadow(y = 14.dp, blur = 26.dp, spread = (-16).dp, color = rgba(35, 75, 40, 0.45f))),
        float = listOf(CssShadow(y = 24.dp, blur = 44.dp, spread = (-18).dp, color = rgba(25, 60, 30, 0.5f))),
        glow = listOf(CssShadow(y = 12.dp, blur = 20.dp, spread = (-12).dp, color = rgba(40, 100, 45, 0.6f))),
        coin = listOf(
            CssShadow(y = 4.dp, color = rgba(150, 95, 0, 0.45f)),
            CssShadow(y = 16.dp, blur = 24.dp, spread = (-10).dp, color = rgba(190, 120, 0, 0.6f)),
        ),
        tile = listOf(CssShadow(y = 14.dp, blur = 26.dp, spread = (-16).dp, color = rgba(35, 75, 40, 0.45f))),
        tabBar = listOf(CssShadow(y = 20.dp, blur = 34.dp, spread = (-14).dp, color = rgba(25, 60, 30, 0.5f))),
        tint = Color(35, 75, 40),
    ),
    spec = FinioSpec(VisualTheme.Bagh, 0.dp, BaghNotesLight),
)

private val BaghDark = ThemePalette(
    colors = colorsOf(
        true, 0x0E1A12, 0xE9F3E0, 0x172A1D, 0x1B3022, 0x7FCF7A, 0x0E1A12, 0x1F3A27, 0x1F3A27, 0xA3BBA8,
        0x27472D, 0xE9F3E0, 0xFF8E68, 0x2A0F06, 0x7FE0A3, 0xFFC83D,
        border = rgba(233, 243, 224, 0.1f), input = rgba(233, 243, 224, 0.25f),
        charts = listOf(0x7FCF7A, 0xFFC83D, 0xFF8E68, 0x7CC3D6, 0xCFA6EE),
        warningBand = 0x3A1F14, warningBandFg = 0xFFE2D4, warningBandAccent = 0xFF8E68,
        glass = c(0x172A1D), glassStrong = c(0x172A1D), glassBorder = Color.Transparent,
        shadowTint = Color(0, 0, 0), scrim = rgba(2, 8, 4, 0.62f),
    ),
    brushes = brushesOf(
        grad(0x3F8A45, 0x2A6A34), grad(0x27472D, 0x1F3A27), grad(0x2F9A5C, 0x16683A), grad(0x1C3D2A, 0x153021),
        grad(0xD9693F, 0xB4451F), grad(0x4A2418, 0x3A1B12), grad(0x7CC3D6, 0x4F9FB5), grad(0xF4B21B, 0xE08A1A),
        CssLinearGradient(180f, listOf(Stop(0f, c(0x12303A)), Stop(1f, c(0x1A3523)))), GoldCoin,
        { _ -> CssLinearGradient(90f, listOf(Stop(0f, c(0x5FAE5A)), Stop(1f, c(0x9BE08F)))) },
        CssLinearGradient(90f, listOf(Stop(0f, c(0x5FAE5A)), Stop(1f, c(0x9BE08F)))),
    ),
    shadows = shadowsOf(
        card = listOf(CssShadow(y = 14.dp, blur = 26.dp, spread = (-16).dp, color = rgba(0, 0, 0, 0.7f))),
        float = listOf(CssShadow(y = 24.dp, blur = 44.dp, spread = (-18).dp, color = rgba(0, 0, 0, 0.8f))),
        glow = listOf(CssShadow(y = 12.dp, blur = 20.dp, spread = (-12).dp, color = rgba(20, 60, 25, 0.7f))),
        coin = listOf(
            CssShadow(y = 4.dp, color = rgba(150, 95, 0, 0.45f)),
            CssShadow(y = 16.dp, blur = 24.dp, spread = (-10).dp, color = rgba(190, 120, 0, 0.45f)),
        ),
        tile = listOf(CssShadow(y = 14.dp, blur = 26.dp, spread = (-16).dp, color = rgba(0, 0, 0, 0.7f))),
        tabBar = listOf(CssShadow(y = 20.dp, blur = 34.dp, spread = (-14).dp, color = rgba(0, 0, 0, 0.7f))),
        tint = Color(0, 0, 0),
    ),
    spec = FinioSpec(VisualTheme.Bagh, 0.dp, BaghNotesDark),
)

/** The Bagh hero's sky/hill colours (web `--bagh-sky-*`, `--bagh-hill-*`). */
object BaghScene {
    fun skyTop(dark: Boolean) = if (dark) c(0x12303A) else c(0xCDE8EE)
    fun skyBottom(dark: Boolean) = if (dark) c(0x1A3523) else c(0xEAF3D4)
    fun hillBack(dark: Boolean) = if (dark) c(0x2C6038) else c(0x7FB069)
    fun hillFront(dark: Boolean) = if (dark) c(0x17391F) else c(0x2F6B3A)
    val sun = c(0xF4B21B)
}
