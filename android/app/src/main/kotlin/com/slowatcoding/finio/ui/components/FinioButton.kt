package com.slowatcoding.finio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slowatcoding.finio.ui.theme.CssShadow
import com.slowatcoding.finio.ui.theme.FinioColors
import com.slowatcoding.finio.ui.theme.FinioShadows
import com.slowatcoding.finio.ui.theme.FinioShapes
import com.slowatcoding.finio.ui.theme.FinioTheme
import com.slowatcoding.finio.ui.theme.FinioType
import com.slowatcoding.finio.ui.theme.cssInsetShadow
import com.slowatcoding.finio.ui.theme.cssShadow
import com.slowatcoding.finio.ui.theme.mix

/** button-variants.ts `variant`. */
enum class ButtonVariant {
    /** The ₹100-lavender gradient with white text and the primary glow — every filled action. */
    Default,
    /** Strong glass with a white glass hairline; pressed fills muted. */
    Outline,
    Secondary,
    Ghost,
    /** Magenta at 10% (dark 20%) with magenta text — a tint, never a solid slab. */
    Destructive,
    Link,
}

/** button-variants.ts `size`: heights 24 / 28 / 36 / 44, square icon sizes 24 / 28 / 32 / 36. */
enum class ButtonSize(
    internal val height: Dp,
    internal val horizontalPadding: Dp,
    internal val gap: Dp,
    internal val iconSize: Dp,
    internal val textSize: Float,
    internal val square: Boolean = false,
) {
    Default(36.dp, 16.dp, 6.dp, 16.dp, 14f),
    Xs(24.dp, 10.dp, 4.dp, 12.dp, 12f),
    Sm(28.dp, 12.dp, 4.dp, 14.dp, 12.8f),
    Lg(44.dp, 20.dp, 6.dp, 16.dp, 15.2f),
    Icon(32.dp, 0.dp, 0.dp, 16.dp, 14f, square = true),
    IconXs(24.dp, 0.dp, 0.dp, 12.dp, 12f, square = true),
    IconSm(28.dp, 0.dp, 0.dp, 16.dp, 14f, square = true),
    IconLg(36.dp, 0.dp, 0.dp, 16.dp, 14f, square = true),
}

internal class ButtonPaint(val fill: Brush?, val content: Color, val border: Color?, val shadows: List<CssShadow>)

internal fun buttonPaint(
    variant: ButtonVariant,
    pressed: Boolean,
    colors: FinioColors,
    gradPrimary: Brush,
    shadows: FinioShadows,
): ButtonPaint = when (variant) {
    ButtonVariant.Default -> ButtonPaint(gradPrimary, Color.White, null, shadows.glowPrimary)
    ButtonVariant.Outline -> ButtonPaint(
        SolidColor(if (pressed) colors.muted else colors.glassStrong),
        colors.foreground,
        colors.glassBorder,
        emptyList(),
    )
    ButtonVariant.Secondary -> ButtonPaint(
        SolidColor(if (pressed) colors.secondary.mix(0.8f) else colors.secondary),
        colors.secondaryForeground,
        null,
        emptyList(),
    )
    ButtonVariant.Ghost -> ButtonPaint(
        if (pressed) SolidColor(if (colors.isDark) colors.muted.mix(0.5f) else colors.muted) else null,
        colors.foreground,
        null,
        emptyList(),
    )
    ButtonVariant.Destructive -> {
        val rest = if (colors.isDark) 0.2f else 0.1f
        val down = if (colors.isDark) 0.3f else 0.2f
        ButtonPaint(SolidColor(colors.destructive.mix(if (pressed) down else rest)), colors.destructive, null, emptyList())
    }
    ButtonVariant.Link -> ButtonPaint(null, colors.primary, null, emptyList())
}

/**
 * The Mudra button (button.tsx + button-variants.ts): pill-shaped, 14sp 500 Geist, lit from
 * above. Pressing nudges it down 1dp (`active:translate-y-px`) and disabled is 50% opacity.
 * Content is a Row; use [FinioButton] with `text` for the common case.
 */
@Composable
fun FinioButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.Default,
    size: ButtonSize = ButtonSize.Default,
    enabled: Boolean = true,
    contentPadding: PaddingValues? = null,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val colors = FinioTheme.colors
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val paint = buttonPaint(variant, pressed, colors, FinioTheme.brushes.gradPrimary, FinioTheme.shadows)
    val shape = FinioShapes.full
    val textStyle = FinioType.bodyMedium.copy(
        fontSize = size.textSize.sp,
        lineHeight = (size.textSize * 1.43f).sp,
        textDecoration = if (variant == ButtonVariant.Link && pressed) TextDecoration.Underline else null,
    )

    Row(
        modifier
            .graphicsLayer { translationY = if (pressed && enabled) density * 1f else 0f }
            .alpha(if (enabled) 1f else 0.5f)
            .then(if (size.square) Modifier.size(size.height) else Modifier.height(size.height).defaultMinSize(minWidth = size.height))
            .cssShadow(shape, paint.shadows)
            .clip(shape)
            .then(if (paint.fill != null) Modifier.background(paint.fill) else Modifier)
            .cssInsetShadow(shape, paint.shadows)
            .border(1.dp, paint.border ?: Color.Transparent, shape)
            .clickable(interactionSource = source, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(contentPadding ?: PaddingValues(horizontal = size.horizontalPadding)),
        horizontalArrangement = Arrangement.spacedBy(size.gap, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(
            LocalContentColor provides paint.content,
            LocalButtonIconSize provides size.iconSize,
            androidx.compose.material3.LocalTextStyle provides textStyle,
        ) {
            content()
        }
    }
}

/** A text button with optional leading/trailing lucide icons (`data-icon=inline-start/end`). */
@Composable
fun FinioButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.Default,
    size: ButtonSize = ButtonSize.Default,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
) {
    // `has-data-[icon=inline-start]:pl-3` — an icon side pads 4dp less (12 instead of 16).
    val trim = when (size) {
        ButtonSize.Xs, ButtonSize.Sm -> size.horizontalPadding - 6.dp
        else -> size.horizontalPadding - 4.dp
    }
    FinioButton(
        onClick = onClick,
        modifier = modifier,
        variant = variant,
        size = size,
        enabled = enabled,
        contentPadding = PaddingValues(
            start = if (leadingIcon != null) trim else size.horizontalPadding,
            end = if (trailingIcon != null) trim else size.horizontalPadding,
        ),
    ) {
        if (leadingIcon != null) ButtonIcon(leadingIcon)
        Text(text, maxLines = 1)
        if (trailingIcon != null) ButtonIcon(trailingIcon)
    }
}

/** An icon-only button (`size="icon"` and friends). */
@Composable
fun FinioIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.Ghost,
    size: ButtonSize = ButtonSize.Icon,
    enabled: Boolean = true,
    tint: Color = Color.Unspecified,
) {
    FinioButton(onClick, modifier, variant, size, enabled, contentPadding = PaddingValues(0.dp)) {
        ButtonIcon(icon, contentDescription, tint)
    }
}

internal val LocalButtonIconSize = androidx.compose.runtime.staticCompositionLocalOf { 16.dp }

/** An icon at the enclosing button's icon size (`[&_svg]:size-4`). */
@Composable
fun ButtonIcon(icon: ImageVector, contentDescription: String? = null, tint: Color = Color.Unspecified) {
    Icon(
        icon,
        contentDescription,
        Modifier.size(LocalButtonIconSize.current),
        tint = if (tint == Color.Unspecified) LocalContentColor.current else tint,
    )
}

