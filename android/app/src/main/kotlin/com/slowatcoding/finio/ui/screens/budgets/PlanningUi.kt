package com.slowatcoding.finio.ui.screens.budgets

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slowatcoding.finio.core.data.COLOR_PALETTE
import com.slowatcoding.finio.ui.components.CategoryIcon
import com.slowatcoding.finio.ui.components.FinioButton
import com.slowatcoding.finio.ui.components.FinioDivider
import com.slowatcoding.finio.ui.components.parseHexColor
import com.slowatcoding.finio.ui.icons.LucideIcons
import com.slowatcoding.finio.ui.theme.FinioShapes
import com.slowatcoding.finio.ui.theme.FinioTheme
import com.slowatcoding.finio.ui.theme.FinioType
import com.slowatcoding.finio.ui.theme.cssShadow
import com.slowatcoding.finio.ui.theme.mix

/*
 * Small pieces the planning pages (Budgets, Recurring, Goals, Debts, Loans) share on the web by
 * copy-paste: the 11px meta line, the 28dp ghost icon actions on a card, the "History" disclosure,
 * the tinted secondary buttons, the icon / colour pickers and the centred empty block.
 */

/** `text-[11px]` — the card meta lines. */
val Micro: TextStyle get() = FinioType.caption.copy(fontSize = 11.sp, lineHeight = 16.sp)

/** `text-[10px]`. */
val Tiny: TextStyle get() = FinioType.caption.copy(fontSize = 10.sp, lineHeight = 14.sp)

/**
 * The radio pill row inside the inline forms (`bg-muted grid grid-cols-N gap-1 rounded-full p-1`,
 * cells `rounded-full py-2 text-xs font-medium`): the selected cell is the lavender gradient with
 * its glow and white text.
 */
@Composable
fun <T> FormPills(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = FinioTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            .clip(FinioShapes.full)
            .background(colors.muted)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { (value, label) ->
            val active = value == selected
            val text by animateColorAsState(if (active) Color.White else colors.mutedForeground, tween(150), label = "pill")
            Box(
                Modifier
                    .weight(1f)
                    .cssShadow(FinioShapes.full, if (active) FinioTheme.shadows.glowPrimary else emptyList())
                    .clip(FinioShapes.full)
                    .then(if (active) Modifier.background(FinioTheme.brushes.gradPrimary) else Modifier)
                    .semantics { this.selected = active }
                    .clickable(role = Role.RadioButton) { onSelect(value) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, style = FinioType.label, color = text, maxLines = 1)
            }
        }
    }
}

/** A labelled form field (`Label text-xs font-medium muted mb-1.5` above its control). */
@Composable
fun FormField(label: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth()) {
        Text(label, Modifier.padding(bottom = 6.dp), style = FinioType.label, color = FinioTheme.colors.mutedForeground)
        content()
    }
}

/** The 28dp ghost icon buttons in a card's top-right (`h-7 w-7`, 13px glyph). */
@Composable
fun CardIconAction(
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    onClick: () -> Unit,
    size: Dp = 28.dp,
    iconSize: Dp = 13.dp,
) {
    val colors = FinioTheme.colors
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    Box(
        Modifier
            .size(size)
            .clip(FinioShapes.full)
            .background(if (pressed) colors.muted else Color.Transparent)
            .clickable(interactionSource = source, indication = null, role = Role.Button, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, Modifier.size(iconSize), tint = tint)
    }
}

/** The "History" / "Details" disclosure (`mt-2 text-[11px] font-medium muted`, a turning chevron). */
@Composable
fun DisclosureToggle(
    label: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = LucideIcons.History,
) {
    val muted = FinioTheme.colors.mutedForeground
    val turn by animateFloatAsState(if (expanded) 180f else 0f, tween(150), label = "chevron")
    Row(
        modifier
            .padding(top = 8.dp)
            .clip(FinioShapes.sm)
            .clickable(role = Role.Button, onClick = onToggle)
            .semantics { stateDescription = if (expanded) "Expanded" else "Collapsed" },
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) Icon(leadingIcon, null, Modifier.size(12.dp), tint = muted)
        Text(label, style = Micro.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium), color = muted)
        Icon(LucideIcons.ChevronDown, null, Modifier.size(12.dp).rotate(turn), tint = muted)
    }
}

/** The expanded history panel (`border-t mt-2 pt-2 space-y-1.5`). */
@Composable
fun DisclosurePanel(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        FinioDivider()
        Spacer(Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp), content = content)
    }
}

/**
 * A `variant="secondary"` button re-tinted by the call site (`bg-positive/10 text-positive`,
 * `bg-accent text-accent-foreground`): 36dp, fully round, 12sp text with a 13dp leading glyph.
 */
@Composable
fun TintButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fill: Color = FinioTheme.colors.secondary,
    pressedFill: Color = fill.mix(0.8f),
    content: Color = FinioTheme.colors.secondaryForeground,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    Row(
        modifier
            .height(36.dp)
            .clip(FinioShapes.full)
            .background(if (pressed && enabled) pressedFill else fill)
            .clickable(interactionSource = source, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val c = if (enabled) content else content.mix(0.5f)
        if (icon != null) {
            Icon(icon, null, Modifier.size(13.dp), tint = c)
            Spacer(Modifier.size(4.dp))
        }
        Text(text, style = FinioType.label, color = c, maxLines = 1)
    }
}

/** The `bg-positive/10 text-positive` "Add funds" / "They owe me" button. */
@Composable
fun PositiveTintButton(text: String, icon: ImageVector?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = FinioTheme.colors
    TintButton(text, onClick, modifier, fill = colors.positive.mix(0.1f), pressedFill = colors.positive.mix(0.15f), content = colors.positive, icon = icon)
}

/** The `bg-accent text-accent-foreground` "Settle up" / "Add prepayment" button. */
@Composable
fun AccentTintButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val colors = FinioTheme.colors
    TintButton(text, onClick, modifier, fill = colors.accent, pressedFill = colors.accent.mix(0.8f), content = colors.accentForeground, enabled = enabled)
}

/** The 6-column round icon picker (Goals, Debts): `h-9 rounded-full border`, primary when picked. */
@Composable
fun IconChoiceGrid(names: List<String>, selected: String, onSelect: (String) -> Unit) {
    val colors = FinioTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        names.chunked(6).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { name ->
                    val active = name == selected
                    Box(
                        Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(FinioShapes.full)
                            .background(if (active) colors.primary.mix(0.1f) else colors.card)
                            .border(1.dp, if (active) colors.primary else colors.border, FinioShapes.full)
                            .semantics { this.selected = active; contentDescription = "Icon $name" }
                            .clickable(role = Role.Button) { onSelect(name) },
                        contentAlignment = Alignment.Center,
                    ) {
                        CategoryIcon(name, size = 16.dp, tint = colors.foreground)
                    }
                }
                repeat(6 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** The `COLOR_PALETTE` swatches (`flex-wrap gap-3`, 28dp discs, ring-2 ring-offset-2 + scale 110%). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorSwatches(selected: String, onSelect: (String) -> Unit) {
    val colors = FinioTheme.colors
    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        COLOR_PALETTE.forEach { hex ->
            val active = hex == selected
            val scale by animateFloatAsState(if (active) 1.1f else 1f, tween(150), label = "swatch")
            Box(
                Modifier
                    .size(28.dp)
                    .scale(scale)
                    // ring-2 ring-offset-2: a 2dp primary ring drawn 2dp outside the disc.
                    .drawBehind {
                        if (active) {
                            val stroke = 2.dp.toPx()
                            drawCircle(colors.primary, radius = size.minDimension / 2 + 2.dp.toPx() + stroke / 2, style = Stroke(stroke))
                        }
                    }
                    .clip(FinioShapes.full)
                    .background(parseHexColor(hex))
                    .semantics { this.selected = active; contentDescription = "Color $hex" }
                    .clickable(role = Role.Button) { onSelect(hex) },
            )
        }
    }
}

/** The pages' empty block (`py-12 text-center`: muted 28px glyph, muted line, round button). */
@Composable
fun PlanningEmpty(
    icon: ImageVector,
    message: String,
    actionLabel: String?,
    onAction: () -> Unit,
    iconTint: Color = FinioTheme.colors.mutedForeground,
    title: String? = null,
) {
    val colors = FinioTheme.colors
    Column(
        Modifier.fillMaxWidth().padding(vertical = if (title != null) 40.dp else 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, null, Modifier.size(28.dp), tint = iconTint)
        Spacer(Modifier.height(12.dp))
        if (title != null) {
            Text(title, style = FinioType.input.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium), color = colors.foreground)
            Spacer(Modifier.height(4.dp))
            Text(message, style = FinioType.body, color = colors.mutedForeground, textAlign = TextAlign.Center)
        } else {
            Text(message, style = FinioType.input, color = colors.mutedForeground, textAlign = TextAlign.Center)
        }
        if (actionLabel != null) {
            Spacer(Modifier.height(16.dp))
            FinioButton(
                onClick = onAction,
                modifier = Modifier.height(40.dp),
                contentPadding = PaddingValues(horizontal = 20.dp),
            ) { Text(actionLabel, maxLines = 1) }
        }
    }
}

/** The Save / Cancel row under every inline form. */
@Composable
fun FormActions(
    saveLabel: String,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    cancelVariant: com.slowatcoding.finio.ui.components.ButtonVariant = com.slowatcoding.finio.ui.components.ButtonVariant.Outline,
    saveEnabled: Boolean = true,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FinioButton(saveLabel, onSave, Modifier.weight(1f), enabled = saveEnabled)
        FinioButton("Cancel", onCancel, variant = cancelVariant)
    }
}
