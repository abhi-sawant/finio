package com.slowatcoding.finio.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.slowatcoding.finio.ui.icons.LucideIcons
import com.slowatcoding.finio.ui.theme.FinioShapes
import com.slowatcoding.finio.ui.theme.FinioTheme
import com.slowatcoding.finio.ui.theme.FinioType
import com.slowatcoding.finio.ui.theme.cssShadow
import com.slowatcoding.finio.ui.theme.mix

/*
 * The handful of row and button shapes every Settings section repeats on the web — written once
 * here so the sections read like their TSX.
 */

/** Which icon colour a row uses: muted (default), destructive (Sign out, Reset) or warning (locked). */
enum class RowTone { Muted, Destructive, Warning }

/**
 * A tappable settings row (`hover:bg-muted/50 flex w-full items-center gap-3 p-4`): an 18dp icon,
 * a 14sp 500 title with an optional 12sp muted subtitle, and an optional chevron. Pressed state
 * is the `bg-muted/50` tint; [enabled] = false is the web's `disabled:opacity-60`.
 */
@Composable
fun SettingsRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    chevron: Boolean = false,
    tone: RowTone = RowTone.Muted,
    enabled: Boolean = true,
    truncateSubtitle: Boolean = false,
) {
    val colors = FinioTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val iconTint = when (tone) {
        RowTone.Muted -> colors.mutedForeground
        RowTone.Destructive -> colors.destructive
        RowTone.Warning -> colors.warning
    }
    Row(
        modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.6f)
            .background(if (pressed) colors.muted.mix(0.5f) else Color.Transparent)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, Modifier.size(18.dp), tint = iconTint)
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = FinioType.bodyMedium,
                color = if (tone == RowTone.Destructive) colors.destructive else colors.foreground,
                maxLines = if (truncateSubtitle) 1 else Int.MAX_VALUE,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = FinioType.caption,
                    color = colors.mutedForeground,
                    maxLines = if (truncateSubtitle) 1 else Int.MAX_VALUE,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (chevron) Icon(LucideIcons.ChevronRight, null, Modifier.size(16.dp), tint = colors.mutedForeground)
    }
}

/**
 * A static row with a trailing control (`flex items-center justify-between gap-3 p-4`): icon,
 * title + truncated subtitle, then [trailing] (a [PillButton], a select…).
 */
@Composable
fun SettingsValueRow(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: @Composable () -> Unit,
) {
    val colors = FinioTheme.colors
    Row(
        modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, Modifier.size(18.dp), tint = colors.mutedForeground)
        Column(Modifier.weight(1f)) {
            Text(title, style = FinioType.bodyMedium, color = colors.foreground, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) {
                Text(subtitle, style = FinioType.caption, color = colors.mutedForeground, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        trailing()
    }
}

/** `bg-muted hover:bg-muted/70 rounded-full px-3 py-1.5 text-sm font-medium` (text-xs with [small]). */
@Composable
fun PillButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, small: Boolean = false, contentDescription: String? = null) {
    val colors = FinioTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(
        modifier
            .clip(FinioShapes.full)
            .background(if (pressed) colors.muted.mix(0.7f) else colors.muted)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClickLabel = contentDescription, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = if (small) FinioType.label else FinioType.bodyMedium, color = colors.foreground, maxLines = 1)
    }
}

/**
 * One choice in the pickers (month start, auto-lock, lead days, PIN length): a full-round pill,
 * the lavender gradient + glow + white text when [selected], muted otherwise.
 */
@Composable
fun ChoicePill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    verticalPadding: Dp = 8.dp,
    horizontalPadding: Dp = 0.dp,
    alignStart: Boolean = false,
) {
    val colors = FinioTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(
        modifier
            .cssShadow(FinioShapes.full, if (selected) FinioTheme.shadows.glowPrimary else emptyList())
            .clip(FinioShapes.full)
            .then(
                if (selected) Modifier.background(FinioTheme.brushes.gradPrimary)
                else Modifier.background(if (pressed) colors.muted.mix(0.7f) else colors.muted),
            )
            .semantics { this.selected = selected }
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        contentAlignment = if (alignStart) Alignment.CenterStart else Alignment.Center,
    ) {
        Text(text, style = FinioType.bodyMedium, color = if (selected) Color.White else colors.foreground, maxLines = 1)
    }
}

/** `grid grid-cols-N gap-1.5` of [items]; the last row is padded so cells stay one width. */
@Composable
fun <T> ChoiceGrid(
    items: List<T>,
    columns: Int,
    modifier: Modifier = Modifier,
    gap: Dp = 6.dp,
    cell: @Composable (T, Modifier) -> Unit,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(gap)) {
        items.chunked(columns).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                row.forEach { cell(it, Modifier.weight(1f)) }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** `role="alert"` error row shared by the PIN and passphrase dialogs (SecretDialogError). */
@Composable
fun SecretDialogError(message: String?, modifier: Modifier = Modifier) {
    if (message == null) return
    val colors = FinioTheme.colors
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(LucideIcons.AlertTriangle, null, Modifier.size(13.dp), tint = colors.destructive)
        Text(message, style = FinioType.label, color = colors.destructive)
    }
}
