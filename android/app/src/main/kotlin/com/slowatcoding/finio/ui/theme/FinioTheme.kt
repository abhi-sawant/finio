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
 * The Finio theme. Provides [FinioColors], [FinioBrushes], [FinioShadows], the [FinioSpec], the type
 * scale and the shape scale of the chosen [visual] world (Mudra by default) through composition
 * locals (read them via [FinioTheme.colors] etc.) and maps the palette onto a Material 3
 * [ColorScheme], so any stock M3 component that slips in still wears the world.
 */
@Composable
fun FinioTheme(
    theme: Theme = Theme.System,
    amoled: Boolean = false,
    visual: VisualTheme = VisualTheme.Default,
    content: @Composable () -> Unit,
) {
    FinioTheme(dark = theme.isDark(), amoled = amoled, visual = visual, content = content)
}

/** [amoled] only takes effect while [dark] is true, like `.dark.amoled` in the stylesheet. */
@Composable
fun FinioTheme(
    dark: Boolean,
    amoled: Boolean = false,
    visual: VisualTheme = VisualTheme.Default,
    content: @Composable () -> Unit,
) {
    val palette = remember(visual, dark, amoled) { finioPalette(visual, dark, amoled) }
    val colors = palette.colors
    val typeScale = remember(visual) { typeScaleFor(visual) }
    val shapeScale = remember(visual) { shapeScaleFor(visual) }
    val scheme = remember(colors) { colors.toColorScheme() }
    val materialTypography = remember(typeScale) { typeScale.toMaterialTypography() }
    val materialShapes = remember(shapeScale) {
        Shapes(
            extraSmall = shapeScale.chip, small = shapeScale.sm, medium = shapeScale.md,
            large = shapeScale.lg, extraLarge = shapeScale.xl,
        )
    }
    // `::selection { background: color-mix(in srgb, var(--primary) 28%, transparent) }`.
    val selection = remember(colors) { TextSelectionColors(colors.primary, colors.primary.mix(0.28f)) }

    MaterialTheme(colorScheme = scheme, typography = materialTypography, shapes = materialShapes) {
        CompositionLocalProvider(
            LocalFinioColors provides colors,
            LocalFinioBrushes provides palette.brushes,
            LocalFinioShadows provides palette.shadows,
            LocalFinioSpec provides palette.spec,
            LocalFinioType provides typeScale,
            LocalFinioShapes provides shapeScale,
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
    val spec: FinioSpec
        @Composable @ReadOnlyComposable get() = LocalFinioSpec.current
    /** The active visual world — branch on this only for a theme's signature pieces. */
    val visual: VisualTheme
        @Composable @ReadOnlyComposable get() = LocalFinioSpec.current.visual
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

private fun FinioTypeScale.toMaterialTypography() = Typography(
    displayLarge = displayMoney,
    displayMedium = displayMoney.copy(fontSize = displayMoney.fontSize * 0.82f),
    displaySmall = headline,
    headlineLarge = pageTitle,
    headlineMedium = headline,
    headlineSmall = screenTitle,
    titleLarge = screenTitle,
    titleMedium = title,
    titleSmall = bodyMedium,
    bodyLarge = input,
    bodyMedium = body,
    bodySmall = caption,
    labelLarge = bodyMedium,
    labelMedium = label,
    labelSmall = micro,
)
