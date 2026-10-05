package com.slowatcoding.finio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.slowatcoding.finio.ui.theme.CssShadow
import com.slowatcoding.finio.ui.theme.FinioShapes
import com.slowatcoding.finio.ui.theme.FinioTheme
import com.slowatcoding.finio.ui.theme.FinioType
import com.slowatcoding.finio.ui.theme.cssInsetShadow
import com.slowatcoding.finio.ui.theme.cssShadow

/**
 * The glass card (`card-elevated rounded-md`): white at 60% (dark 6%) over the paper, a 1dp white
 * glass hairline, an inset top highlight and the lavender Card shadow, 16dp padding.
 *
 * A homogeneous list is **one** card holding plain rows split by [FinioDivider] — pass
 * `contentPadding = PaddingValues(horizontal = 12.dp)` (rows pad their own 12dp vertically).
 *
 * Compromise: Android can't blur what is behind an arbitrary view (`backdrop-filter`), so the
 * frost is translucency only.
 */
@Composable
fun FinioCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    shape: Shape = FinioShapes.md,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = FinioTheme.colors
    Column(
        modifier
            .glassSurface(shape, colors.glass, FinioTheme.shadows.card)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(contentPadding),
        content = content,
    )
}

/**
 * The shared glass recipe: [shadows] (outer) → clip → [fill] → inset highlight → glass hairline.
 */
@Composable
fun Modifier.glassSurface(shape: Shape, fill: Color, shadows: List<CssShadow>): Modifier {
    val colors = FinioTheme.colors
    val highlight = remember(colors.glassHighlight) { listOf(CssShadow(y = 1.dp, color = colors.glassHighlight, inset = true)) }
    return this
        .cssShadow(shape, shadows)
        .clip(shape)
        .background(fill, shape)
        .cssInsetShadow(shape, highlight)
        .border(1.dp, colors.glassBorder, shape)
}

/** `divide-y` / `border-t` — the 1dp ink-at-10% hairline between plain rows. */
@Composable
fun FinioDivider(modifier: Modifier = Modifier, color: Color = FinioTheme.colors.border) {
    Box(modifier.fillMaxWidth().height(1.dp).background(color))
}

/**
 * The one surfaced alert per screen: the magenta band (`bg-warning-band`) with band ink, glass
 * hairline, Card shadow, 22dp radius, 16dp padding. [icon] is drawn in the band accent.
 */
@Composable
fun AlertBand(
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = FinioTheme.colors
    val highlight = remember(colors.glassHighlight) { listOf(CssShadow(y = 1.dp, color = colors.glassHighlight, inset = true)) }
    val shape = FinioShapes.lg
    CompositionLocalProvider(LocalContentColor provides colors.warningBandForeground) {
        Row(
            modifier
                .fillMaxWidth()
                .cssShadow(shape, FinioTheme.shadows.card)
                .clip(shape)
                .background(colors.warningBand)
                .cssInsetShadow(shape, highlight)
                .border(1.dp, colors.glassBorder, shape)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (icon != null) Icon(icon, null, Modifier.size(18.dp), tint = colors.warningBandAccent)
            Column(Modifier.weight(1f), content = content)
        }
    }
}

/**
 * A section heading row (`h2 text-base font-semibold` + an optional lavender "See all"), with the
 * `mb-3` gap built in.
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier.fillMaxWidth().padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, Modifier.semantics { heading() }, style = FinioType.title, color = FinioTheme.colors.foreground)
        if (actionLabel != null && onAction != null) {
            Text(
                actionLabel,
                Modifier
                    .clip(FinioShapes.sm)
                    .clickable(onClick = onAction)
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                style = FinioType.label,
                color = FinioTheme.colors.primary,
            )
        }
    }
}

/**
 * The empty-list block (Transactions' "No transactions yet"): centred, 48dp vertical padding,
 * a 500-weight title, a muted 14sp explanation capped at 320dp, and an optional action.
 */
@Composable
fun EmptyState(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    icon: ImageVector? = null,
    action: (@Composable () -> Unit)? = null,
) {
    val colors = FinioTheme.colors
    Column(
        modifier.fillMaxWidth().padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (icon != null) Icon(icon, null, Modifier.size(20.dp), tint = colors.mutedForeground)
        Text(title, style = FinioType.input.copy(fontWeight = FontWeight.Medium), color = colors.foreground, textAlign = TextAlign.Center)
        if (description != null) {
            Text(
                description,
                Modifier.widthIn(max = 320.dp),
                style = FinioType.body,
                color = colors.mutedForeground,
                textAlign = TextAlign.Center,
            )
        }
        action?.invoke()
    }
}

/**
 * A filter pill (Transactions' type filter, Manage Categories' tabs): 12dp × 6dp, 12sp 500,
 * full round; selected is the lavender gradient with its glow and white text, otherwise muted.
 */
@Composable
fun FinioChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
) {
    FinioButton(
        onClick = onClick,
        modifier = modifier,
        variant = if (selected) ButtonVariant.Default else ButtonVariant.Secondary,
        size = ButtonSize.Sm,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
    ) {
        val color = if (selected) Color.White else FinioTheme.colors.mutedForeground
        if (leadingIcon != null) Icon(leadingIcon, null, Modifier.size(12.dp), tint = color)
        Text(text, style = FinioType.label, color = color, maxLines = 1)
    }
}

/**
 * A transaction's label chip: muted pill, 6dp colour dot, 12sp, capped at 160dp
 * (TransactionItem.tsx).
 */
@Composable
fun LabelChip(name: String, color: Color, modifier: Modifier = Modifier) {
    val colors = FinioTheme.colors
    Row(
        modifier
            .widthIn(max = 160.dp)
            .clip(FinioShapes.full)
            .background(colors.muted)
            .padding(horizontal = 6.dp, vertical = 1.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(6.dp).clip(FinioShapes.full).background(color))
        Text(name, style = FinioType.caption, color = colors.foreground, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
    }
}

/** A field caption above a control: `text-muted-foreground mb-1.5 block text-xs font-medium`. */
@Composable
fun FieldLabel(text: String, modifier: Modifier = Modifier, bottomGap: Dp = 6.dp) {
    Text(text, modifier.padding(bottom = bottomGap), style = FinioType.label, color = FinioTheme.colors.mutedForeground)
}

