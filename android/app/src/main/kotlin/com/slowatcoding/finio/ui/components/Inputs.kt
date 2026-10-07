package com.slowatcoding.finio.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.slowatcoding.finio.ui.icons.LucideIcons
import com.slowatcoding.finio.ui.theme.CssShadow
import com.slowatcoding.finio.ui.theme.FinioColors
import com.slowatcoding.finio.ui.theme.FinioShapes
import com.slowatcoding.finio.ui.theme.FinioTheme
import com.slowatcoding.finio.ui.theme.FinioType
import com.slowatcoding.finio.ui.theme.cssShadow
import com.slowatcoding.finio.ui.theme.mix

/**
 * The field outline every input-shaped control shares (input.tsx / select trigger / date
 * trigger): a 1dp `--input` stroke that turns `--ring` on focus with a 3dp ring at 50% outside it;
 * invalid fields are magenta with a 20% (dark 40%) ring at all times.
 */
@Composable
internal fun Modifier.fieldChrome(focused: Boolean, isError: Boolean, enabled: Boolean): Modifier {
    val colors = FinioTheme.colors
    val shape = FinioShapes.sm
    val border by animateColorAsState(fieldBorder(colors, focused, isError), tween(150), label = "field")
    val ring = when {
        isError -> colors.destructive.mix(if (colors.isDark) 0.4f else 0.2f)
        focused -> colors.ring.mix(0.5f)
        else -> Color.Transparent
    }
    return this
        .cssShadow(shape, listOf(CssShadow(spread = 3.dp, color = ring)))
        .alpha(if (enabled) 1f else 0.5f)
        .clip(shape)
        .background(if (enabled) colors.card else colors.muted)
        .border(1.dp, border, shape)
}

private fun fieldBorder(colors: FinioColors, focused: Boolean, isError: Boolean) = when {
    isError -> if (colors.isDark) colors.destructive.mix(0.5f) else colors.destructive
    focused -> colors.ring
    else -> colors.input
}

/**
 * The text input (input.tsx; textarea.tsx when [minLines] > 1): opaque card fill, 13.2dp radius,
 * 40dp tall, 12dp × 4dp padding, 16sp text (the web's mobile size), muted placeholder, lavender
 * caret.
 */
@Composable
fun FinioTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    isError: Boolean = false,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    interactionSource: MutableInteractionSource? = null,
) {
    val colors = FinioTheme.colors
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val multiline = !singleLine || minLines > 1
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = if (multiline) 64.dp else 40.dp)
            .fieldChrome(focused, isError, enabled),
        enabled = enabled,
        readOnly = readOnly,
        textStyle = FinioType.input.copy(color = colors.foreground),
        cursorBrush = SolidColor(colors.primary),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine && minLines <= 1,
        minLines = minLines,
        maxLines = maxLines,
        visualTransformation = visualTransformation,
        interactionSource = source,
        decorationBox = { inner ->
            Row(
                Modifier.padding(horizontal = 12.dp, vertical = if (multiline) 8.dp else 4.dp),
                verticalAlignment = if (multiline) Alignment.Top else Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                leading?.invoke()
                Box(Modifier.weight(1f), contentAlignment = if (multiline) Alignment.TopStart else Alignment.CenterStart) {
                    if (value.isEmpty() && placeholder != null) {
                        Text(placeholder, style = FinioType.input, color = colors.mutedForeground, maxLines = if (multiline) Int.MAX_VALUE else 1)
                    }
                    inner()
                }
                trailing?.invoke()
            }
        },
    )
}

/**
 * The input-shaped trigger the select and date pickers share: a field outline with [text] (muted
 * when it is the placeholder), an optional leading icon and a trailing chevron.
 */
@Composable
internal fun FieldTrigger(
    text: String,
    isPlaceholder: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    open: Boolean = false,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    showChevron: Boolean = true,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingText: String? = null,
) {
    val colors = FinioTheme.colors
    // A trigger opens a popup/sheet/dialog; drop text-field focus first, or the window hands it
    // back to the last field when the popup closes and the keyboard pops up unasked.
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    Row(
        modifier
            .fillMaxWidth()
            .height(40.dp)
            .fieldChrome(focused = open, isError = isError, enabled = enabled)
            .clickable(enabled = enabled, role = Role.DropdownList) {
                focusManager.clearFocus(force = true)
                onClick()
            }
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (leadingIcon != null) {
            Icon(leadingIcon, null, Modifier.size(16.dp), tint = if (isPlaceholder) colors.mutedForeground else colors.foreground)
            Spacer(Modifier.width(2.dp))
        }
        leadingContent?.invoke()
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text,
                Modifier.weight(1f, fill = false),
                style = FinioType.input,
                color = if (isPlaceholder) colors.mutedForeground else colors.foreground,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
            if (trailingText != null) {
                Text(trailingText, style = FinioType.caption, color = colors.mutedForeground, maxLines = 1, softWrap = false)
            }
        }
        if (showChevron) Icon(LucideIcons.ChevronDown, null, Modifier.size(16.dp), tint = colors.mutedForeground)
    }
}

/** switch.tsx sizes: `sm` is inline-in-a-row (20×36), `md` the settings-row size (24×44). */
enum class SwitchSize { Sm, Md }

/**
 * The bare toggle (switch.tsx): lavender gradient + glow when on, `--input` grey when off, a
 * card-coloured thumb with `shadow-md` sliding 16/20dp. Give it a [contentDescription] when it
 * stands alone; [SwitchField] names it for you.
 */
@Composable
fun FinioSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    size: SwitchSize = SwitchSize.Md,
    enabled: Boolean = true,
    contentDescription: String? = null,
) {
    val colors = FinioTheme.colors
    val brushes = FinioTheme.brushes
    val shadows = FinioTheme.shadows
    val (w, h, thumb, travel) = if (size == SwitchSize.Sm) listOf(36.dp, 20.dp, 16.dp, 16.dp) else listOf(44.dp, 24.dp, 20.dp, 20.dp)
    val offset by animateDpAsState(if (checked) travel else 0.dp, tween(150), label = "thumb")
    Box(
        modifier
            .semantics { if (contentDescription != null) this.contentDescription = contentDescription }
            .alpha(if (enabled) 1f else 0.5f)
            .size(w, h)
            .cssShadow(FinioShapes.full, if (checked) shadows.glowPrimary else emptyList())
            .clip(FinioShapes.full)
            .then(if (checked) Modifier.background(brushes.gradPrimary) else Modifier.background(colors.input))
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(2.dp),
    ) {
        Box(
            Modifier
                .offset(x = offset)
                .size(thumb)
                .cssShadow(FinioShapes.full, shadows.md)
                .clip(FinioShapes.full)
                .background(colors.card),
        )
    }
}

/**
 * A labelled settings row (switch.tsx `SwitchField`): optional icon, a 14sp 500 title and a
 * 12sp muted description on the left, the switch on the right. [interactiveRow] makes the whole
 * row toggle it.
 */
@Composable
fun SwitchField(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    icon: (@Composable () -> Unit)? = null,
    size: SwitchSize = SwitchSize.Md,
    enabled: Boolean = true,
    interactiveRow: Boolean = false,
) {
    val colors = FinioTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            .then(
                if (interactiveRow) {
                    Modifier.toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
                } else {
                    Modifier
                },
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        icon?.invoke()
        Column(Modifier.weight(1f)) {
            Text(title, style = FinioType.bodyMedium, color = colors.foreground)
            if (description != null) Text(description, style = FinioType.caption, color = colors.mutedForeground)
        }
        FinioSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            size = size,
            enabled = enabled,
            contentDescription = title,
        )
    }
}

/**
 * checkbox.tsx: a 16dp box with a 4dp radius and `--input` border; checked fills `--primary` with
 * a 14dp check in `--primary-foreground`. Pass `onCheckedChange = null` for a display-only box
 * inside a row that toggles itself (the transaction selection checkbox).
 */
@Composable
fun FinioCheckbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = FinioTheme.colors
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
    Box(
        modifier
            .then(
                if (onCheckedChange != null) {
                    Modifier.toggleable(value = checked, enabled = enabled, role = Role.Checkbox, onValueChange = onCheckedChange)
                        .padding(8.dp) // `after:-inset-x-3 after:-inset-y-2` — a larger hit area
                } else {
                    Modifier.semantics {
                        toggleableState = ToggleableState(checked)
                        stateDescription = if (checked) "Selected" else "Not selected"
                    }
                },
            )
            .alpha(if (enabled) 1f else 0.5f)
            .size(16.dp)
            .clip(shape)
            .background(
                when {
                    checked -> colors.primary
                    colors.isDark -> colors.input.mix(0.3f)
                    else -> Color.Transparent
                },
            )
            .border(1.dp, if (checked) colors.primary else colors.input, shape),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) Icon(LucideIcons.Check, null, Modifier.size(14.dp), tint = colors.primaryForeground)
    }
}
