package com.slowatcoding.finio.ui.screens.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.slowatcoding.finio.ui.components.ButtonVariant
import com.slowatcoding.finio.ui.components.ButtonSize
import com.slowatcoding.finio.ui.components.FinioButton
import com.slowatcoding.finio.ui.components.FinioPopover
import com.slowatcoding.finio.ui.icons.LucideIcons
import com.slowatcoding.finio.ui.theme.CssShadow
import com.slowatcoding.finio.ui.theme.FinioShapes
import com.slowatcoding.finio.ui.theme.FinioTheme
import com.slowatcoding.finio.ui.theme.FinioType
import com.slowatcoding.finio.ui.theme.cssInsetShadow
import com.slowatcoding.finio.ui.theme.cssShadow

/** `Button size="sm" className="h-7 px-2 text-xs"` — the horizon/range/period toggles on cards. */
@Composable
internal fun MiniToggle(label: String, selected: Boolean, onClick: () -> Unit) {
    FinioButton(
        onClick = onClick,
        variant = if (selected) ButtonVariant.Default else ButtonVariant.Ghost,
        size = ButtonSize.Sm,
        contentPadding = PaddingValues(horizontal = 8.dp),
    ) {
        Text(label, style = FinioType.label, maxLines = 1)
    }
}

/** The `‹ label ›` month stepper on the calendar cards: 28dp round muted buttons, 30% when disabled. */
@Composable
internal fun MonthStepper(
    label: String,
    minLabelWidth: Dp,
    canPrev: Boolean,
    canNext: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    buttonSize: Dp = 28.dp,
    iconSize: Dp = 15.dp,
    labelStyle: androidx.compose.ui.text.TextStyle = FinioType.label,
    prevDescription: String = "Previous month",
    nextDescription: String = "Next month",
) {
    val colors = FinioTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        StepButton(LucideIcons.ChevronLeft, prevDescription, canPrev, onPrev, buttonSize, iconSize)
        Text(label, Modifier.widthIn(min = minLabelWidth), style = labelStyle, color = colors.foreground, textAlign = TextAlign.Center)
        StepButton(LucideIcons.ChevronRight, nextDescription, canNext, onNext, buttonSize, iconSize)
    }
}

@Composable
private fun StepButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
    size: Dp,
    iconSize: Dp,
) {
    val colors = FinioTheme.colors
    Box(
        Modifier
            .size(size)
            .alpha(if (enabled) 1f else 0.3f)
            .clip(FinioShapes.full)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, Modifier.size(iconSize), tint = colors.mutedForeground)
    }
}

/** Monday-first weekday initials (`WEEKDAYS`). */
internal val WeekdayInitials = listOf("M", "T", "W", "T", "F", "S", "S")

/**
 * The 7-column day grid both calendar cards use (`mx-auto max-w-md`, `grid-cols-7 gap-1`): a row
 * of weekday initials, then one row per week. [cell] draws a day into its square box.
 */
@Composable
internal fun <T> WeekGrid(weeks: List<List<T>>, key: (T) -> String, cell: @Composable BoxScope.(T) -> Unit) {
    val colors = FinioTheme.colors
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 448.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                WeekdayInitials.forEach {
                    Text(it, Modifier.weight(1f), style = FinioType.label, color = colors.mutedForeground, textAlign = TextAlign.Center)
                }
            }
            weeks.forEach { week ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    week.forEach { day ->
                        androidx.compose.runtime.key(key(day)) {
                            Box(Modifier.weight(1f).aspectRatio(1f), contentAlignment = Alignment.Center) { cell(day) }
                        }
                    }
                }
            }
        }
    }
}

/**
 * One calendar day square (`rounded-md text-xs font-medium`, a 2dp primary ring for today). The
 * web shows a hover `title`; here a tap opens the same sentence in a popover.
 */
@Composable
internal fun DaySquare(
    text: String,
    background: Color,
    textColor: Color,
    isToday: Boolean,
    title: String?,
    modifier: Modifier = Modifier,
    badge: (@Composable BoxScope.() -> Unit)? = null,
) {
    val colors = FinioTheme.colors
    var open by remember { mutableStateOf(false) }
    val shape = FinioShapes.md
    Box(
        modifier
            .matchParentSizeSafe()
            .clip(shape)
            .background(background)
            .then(if (isToday) Modifier.border(2.dp, colors.primary, shape) else Modifier)
            .then(
                if (title != null) {
                    Modifier
                        .semantics { contentDescription = title }
                        .clickable(role = Role.Button) { open = true }
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = FinioType.label, color = textColor)
        badge?.invoke(this)
        if (open && title != null) {
            FinioPopover(onDismissRequest = { open = false }) {
                Text(title, Modifier.widthIn(max = 260.dp), style = FinioType.caption, color = colors.popoverForeground)
            }
        }
    }
}

private fun Modifier.matchParentSizeSafe(): Modifier = this.then(Modifier.fillMaxWidth().aspectRatio(1f))

/** `bg-muted/40 rounded-sm` — the small stat tiles on the forecast and comparison cards. */
@Composable
internal fun MutedTile(modifier: Modifier = Modifier, padding: Dp = 10.dp, content: @Composable () -> Unit) {
    val colors = FinioTheme.colors
    Box(modifier.clip(FinioShapes.sm).background(colors.muted.copy(alpha = colors.muted.alpha * 0.4f)).padding(padding)) { content() }
}

/**
 * `card-elevated bg-grad-surface rounded-md p-4` — the period summary: the glass card's hairline,
 * highlight and shadow over the surface gradient.
 */
@Composable
internal fun SurfaceCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val colors = FinioTheme.colors
    val shape = FinioShapes.md
    val highlight = remember(colors.glassHighlight) { listOf(CssShadow(y = 1.dp, color = colors.glassHighlight, inset = true)) }
    Box(
        modifier
            .fillMaxWidth()
            .cssShadow(shape, FinioTheme.shadows.card)
            .clip(shape)
            .background(FinioTheme.brushes.gradSurface)
            .cssInsetShadow(shape, highlight)
            .border(1.dp, colors.glassBorder, shape)
            .padding(16.dp),
    ) { content() }
}
