package com.slowatcoding.finio.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Every semantic colour token from web/src/index.css (`:root` = light, `.dark` = dark), one field
 * per CSS custom property. Components read these — never raw hex — exactly as the web components
 * use `bg-card`, `text-muted-foreground`, `var(--glass)` and friends. Gradients live in
 * [FinioBrushes], shadows in FinioShadows.
 */
@Immutable
data class FinioColors(
    val isDark: Boolean,
    /** True-black dark mode (`.dark.amoled`); always paired with `isDark = true`. */
    val isAmoled: Boolean = false,
    val background: Color,
    val foreground: Color,
    val card: Color,
    val cardForeground: Color,
    val popover: Color,
    val popoverForeground: Color,
    val primary: Color,
    val primaryForeground: Color,
    val secondary: Color,
    val secondaryForeground: Color,
    val muted: Color,
    val mutedForeground: Color,
    val accent: Color,
    val accentForeground: Color,
    val destructive: Color,
    val destructiveForeground: Color,
    val positive: Color,
    val warning: Color,
    /** Hairlines: ink at 10%. */
    val border: Color,
    /** Input strokes: ink at 16%. */
    val input: Color,
    val ring: Color,
    val chart1: Color,
    val chart2: Color,
    val chart3: Color,
    val chart4: Color,
    val chart5: Color,
    val sidebar: Color,
    val sidebarForeground: Color,
    val sidebarPrimary: Color,
    val sidebarPrimaryForeground: Color,
    val sidebarAccent: Color,
    val sidebarAccentForeground: Color,
    val sidebarBorder: Color,
    val sidebarRing: Color,
    /** The one collapsed-alert band — ₹2000 magenta as a soft tint. */
    val warningBand: Color,
    val warningBandForeground: Color,
    val warningBandAccent: Color,
    /** The page rosette's ink. */
    val engraving: Color,
    /** Frosted glass: card fill. */
    val glass: Color,
    /** Frosted glass: chrome, outline buttons, toasts, pad keys. */
    val glassStrong: Color,
    val glassBorder: Color,
    val glassHighlight: Color,
    /** `--shadow-tint`: lavender in light, black in dark — the stock shadow scale's colour. */
    val shadowTint: Color,
    /** Modal scrim — always dims, never lightens. */
    val scrim: Color,
) {
    val chart: List<Color> get() = listOf(chart1, chart2, chart3, chart4, chart5)
}

val LightFinioColors = FinioColors(
    isDark = false,
    background = Color(0xFFF1EEFB),
    foreground = Color(0xFF1D1747),
    card = Color(0xFFFDFCFF),
    cardForeground = Color(0xFF1D1747),
    popover = Color(0xFFFDFCFF),
    popoverForeground = Color(0xFF1D1747),
    primary = Color(0xFF4B36C7),
    primaryForeground = Color(0xFFFFFFFF),
    secondary = Color(0xFFEBE7F8),
    secondaryForeground = Color(0xFF1D1747),
    muted = Color(0xFFEBE7F8),
    mutedForeground = Color(0xFF5B5689),
    accent = Color(0xFFE9E4FF),
    accentForeground = Color(0xFF3B2A96),
    destructive = Color(0xFFB0125F),
    destructiveForeground = Color(0xFFFFFFFF),
    positive = Color(0xFF0B7A55),
    warning = Color(0xFF9A6A00),
    border = rgba(29, 23, 71, 0.1f),
    input = rgba(29, 23, 71, 0.16f),
    ring = Color(0xFF4B36C7),
    chart1 = Color(0xFF4B36C7),
    chart2 = Color(0xFFC2185B),
    chart3 = Color(0xFFC48A12),
    chart4 = Color(0xFF0F8F6A),
    chart5 = Color(0xFF2F7FD1),
    sidebar = rgba(255, 255, 255, 0.5f),
    sidebarForeground = Color(0xFF1D1747),
    sidebarPrimary = Color(0xFF4B36C7),
    sidebarPrimaryForeground = Color(0xFFFFFFFF),
    sidebarAccent = Color(0xFFE9E4FF),
    sidebarAccentForeground = Color(0xFF3B2A96),
    sidebarBorder = rgba(255, 255, 255, 0.85f),
    sidebarRing = Color(0xFF4B36C7),
    warningBand = Color(0xFFFFE1EF),
    warningBandForeground = Color(0xFF5C0D38),
    warningBandAccent = Color(0xFFB0125F),
    engraving = rgba(90, 70, 190, 0.075f),
    glass = rgba(255, 255, 255, 0.6f),
    glassStrong = rgba(255, 255, 255, 0.78f),
    glassBorder = rgba(255, 255, 255, 0.9f),
    glassHighlight = rgba(255, 255, 255, 0.95f),
    shadowTint = Color(60, 40, 140),
    scrim = rgba(29, 23, 71, 0.24f),
)

val DarkFinioColors = FinioColors(
    isDark = true,
    background = Color(0xFF161236),
    foreground = Color(0xFFEEEAFF),
    card = Color(0xFF221D4D),
    cardForeground = Color(0xFFEEEAFF),
    popover = Color(0xFF221D4D),
    popoverForeground = Color(0xFFEEEAFF),
    primary = Color(0xFFB9ADFF),
    primaryForeground = Color(0xFF15103D),
    secondary = Color(0xFF2B2559),
    secondaryForeground = Color(0xFFEEEAFF),
    muted = Color(0xFF2B2559),
    mutedForeground = Color(0xFFAAA4D8),
    accent = Color(0xFF302874),
    accentForeground = Color(0xFFDDD6FF),
    destructive = Color(0xFFFF7AB8),
    destructiveForeground = Color(0xFF3A0820),
    positive = Color(0xFF5FE0AE),
    warning = Color(0xFFF2C55C),
    border = rgba(238, 234, 255, 0.1f),
    input = rgba(238, 234, 255, 0.16f),
    ring = Color(0xFFB9ADFF),
    chart1 = Color(0xFFA495FF),
    chart2 = Color(0xFFFF6FAE),
    chart3 = Color(0xFFF2C55C),
    chart4 = Color(0xFF4FD6A2),
    chart5 = Color(0xFF6FB4FF),
    sidebar = rgba(255, 255, 255, 0.04f),
    sidebarForeground = Color(0xFFEEEAFF),
    sidebarPrimary = Color(0xFFB9ADFF),
    sidebarPrimaryForeground = Color(0xFF15103D),
    sidebarAccent = Color(0xFF302874),
    sidebarAccentForeground = Color(0xFFDDD6FF),
    sidebarBorder = rgba(255, 255, 255, 0.1f),
    sidebarRing = Color(0xFFB9ADFF),
    warningBand = Color(0xFF4A1534),
    warningBandForeground = Color(0xFFFFE1EE),
    warningBandAccent = Color(0xFFFF8CC2),
    engraving = rgba(170, 150, 255, 0.09f),
    glass = rgba(255, 255, 255, 0.06f),
    glassStrong = rgba(30, 24, 70, 0.72f),
    glassBorder = rgba(255, 255, 255, 0.11f),
    glassHighlight = rgba(255, 255, 255, 0.12f),
    shadowTint = Color(0, 0, 0),
    scrim = rgba(6, 4, 24, 0.6f),
)

/**
 * `.dark.amoled` in index.css: the dark palette on a true-black field, so an AMOLED panel can
 * switch those pixels off. Surfaces lift off black by a hairline and a faint lavender wash instead
 * of by shadow (a black shadow is invisible on black).
 */
val AmoledFinioColors = DarkFinioColors.copy(
    isAmoled = true,
    background = Color(0xFF000000),
    card = Color(0xFF0B0A14),
    cardForeground = Color(0xFFEEEAFF),
    popover = Color(0xFF0E0C1B),
    secondary = Color(0xFF15122A),
    muted = Color(0xFF15122A),
    mutedForeground = Color(0xFFA49FD2),
    accent = Color(0xFF1E1951),
    border = rgba(238, 234, 255, 0.12f),
    input = rgba(238, 234, 255, 0.2f),
    sidebar = rgba(0, 0, 0, 0f),
    sidebarBorder = rgba(238, 234, 255, 0.12f),
    sidebarAccent = Color(0xFF1E1951),
    warningBand = Color(0xFF2A0C1E),
    // 0.05 in the stylesheet, then `body::before { opacity: .8 }`.
    engraving = rgba(170, 150, 255, 0.04f),
    glass = rgba(185, 173, 255, 0.045f),
    glassStrong = rgba(0, 0, 0, 0.86f),
    glassBorder = rgba(185, 173, 255, 0.15f),
    glassHighlight = rgba(185, 173, 255, 0.08f),
    scrim = rgba(0, 0, 0, 0.72f),
)

val LocalFinioColors = staticCompositionLocalOf { LightFinioColors }

/** CSS `rgb(r g b / a)`. */
internal fun rgba(r: Int, g: Int, b: Int, a: Float): Color = Color(r, g, b, (a * 255f + 0.5f).toInt())

/** CSS `color-mix(in srgb, <this> p%, transparent)` — the same colour at [fraction] of its alpha. */
fun Color.mix(fraction: Float): Color = copy(alpha = alpha * fraction)

/** CSS `hsl(h s% l%)`. */
fun hsl(hue: Float, saturation: Float, lightness: Float, alpha: Float = 1f): Color =
    Color.hsl(((hue % 360f) + 360f) % 360f, saturation, lightness, alpha)
