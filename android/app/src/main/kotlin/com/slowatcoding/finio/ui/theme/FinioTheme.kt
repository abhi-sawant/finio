package com.slowatcoding.finio.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.slowatcoding.finio.core.model.Theme

/** Resolves the Settings theme value (incl. "system") to dark or light. */
@Composable
fun Theme.isDark(): Boolean = when (this) {
    Theme.Dark -> true
    Theme.Light -> false
    Theme.System -> isSystemInDarkTheme()
}

/**
 * The Mudra theme. Provides [FinioColors], [FinioBrushes] and [FinioShadows] through
 * composition locals (read them via [FinioTheme.colors] etc.) and maps the palette onto a
 * Material 3 [ColorScheme], so any stock M3 component that slips in still wears the world.
 */
@Composable
fun FinioTheme(theme: Theme = Theme.System, amoled: Boolean = false, content: @Composable () -> Unit) {
    FinioTheme(dark = theme.isDark(), amoled = amoled, content = content)
}

/** [amoled] only takes effect while [dark] is true, like `.dark.amoled` in the stylesheet. */
@Composable
fun FinioTheme(dark: Boolean, amoled: Boolean = false, content: @Composable () -> Unit) {
    val black = dark && amoled
    val colors = if (black) AmoledFinioColors else if (dark) DarkFinioColors else LightFinioColors
    val brushes = if (black) AmoledFinioBrushes else if (dark) DarkFinioBrushes else LightFinioBrushes
    val shadows = if (black) AmoledFinioShadows else if (dark) DarkFinioShadows else LightFinioShadows
    val scheme = remember(colors) { colors.toColorScheme() }
    // `::selection { background: color-mix(in srgb, var(--primary) 28%, transparent) }`.
    val selection = remember(colors) { TextSelectionColors(colors.primary, colors.primary.mix(0.28f)) }

    MaterialTheme(colorScheme = scheme, typography = MaterialTypography, shapes = MaterialShapes) {
        CompositionLocalProvider(
            LocalFinioColors provides colors,
            LocalFinioBrushes provides brushes,
            LocalFinioShadows provides shadows,
            LocalTextSelectionColors provides selection,
            LocalContentColor provides colors.foreground,
            content = content,
        )
    }
}

object FinioTheme {
    val colors: FinioColors
        @Composable @ReadOnlyComposable get() = LocalFinioColors.current
    val brushes: FinioBrushes
        @Composable @ReadOnlyComposable get() = LocalFinioBrushes.current
    val shadows: FinioShadows
        @Composable @ReadOnlyComposable get() = LocalFinioShadows.current
    val type: FinioType get() = FinioType
}

private fun FinioColors.toColorScheme(): ColorScheme {
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = primary,
        onPrimary = primaryForeground,
        primaryContainer = accent,
        onPrimaryContainer = accentForeground,
        inversePrimary = primary,
        secondary = secondaryForeground,
        onSecondary = secondary,
        secondaryContainer = secondary,
        onSecondaryContainer = secondaryForeground,
        tertiary = positive,
        onTertiary = if (isDark) Color(0xFF0B2A20) else Color.White,
        tertiaryContainer = accent,
        onTertiaryContainer = accentForeground,
        background = background,
        onBackground = foreground,
        surface = card,
        onSurface = foreground,
        surfaceVariant = muted,
        onSurfaceVariant = mutedForeground,
        surfaceTint = Color.Transparent,
        inverseSurface = foreground,
        inverseOnSurface = background,
        error = destructive,
        onError = destructiveForeground,
        errorContainer = warningBand,
        onErrorContainer = warningBandForeground,
        outline = input,
        outlineVariant = border,
        scrim = scrim,
        surfaceBright = card,
        surfaceDim = background,
        surfaceContainerLowest = card,
        surfaceContainerLow = card,
        surfaceContainer = popover,
        surfaceContainerHigh = popover,
        surfaceContainerHighest = muted,
    )
}

private val MaterialTypography = Typography(
    displayLarge = FinioType.displayMoney,
    displayMedium = FinioType.displayMoney.copy(fontSize = FinioType.displayMoney.fontSize * 0.82f),
    displaySmall = FinioType.headline,
    headlineLarge = FinioType.pageTitle,
    headlineMedium = FinioType.headline,
    headlineSmall = FinioType.screenTitle,
    titleLarge = FinioType.screenTitle,
    titleMedium = FinioType.title,
    titleSmall = FinioType.bodyMedium,
    bodyLarge = FinioType.input,
    bodyMedium = FinioType.body,
    bodySmall = FinioType.caption,
    labelLarge = FinioType.bodyMedium,
    labelMedium = FinioType.label,
    labelSmall = FinioType.micro,
)

private val MaterialShapes = Shapes(
    extraSmall = FinioShapes.chip,
    small = FinioShapes.sm,
    medium = FinioShapes.md,
    large = FinioShapes.lg,
    extraLarge = FinioShapes.xl,
)
