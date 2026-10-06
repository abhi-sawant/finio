package com.slowatcoding.finio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.slowatcoding.finio.ui.icons.LucideIcons
import com.slowatcoding.finio.ui.theme.FinioShapes
import com.slowatcoding.finio.ui.theme.FinioTheme
import com.slowatcoding.finio.ui.theme.FinioType
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.RoundedCornerShape
import com.slowatcoding.finio.ui.theme.FinioRadius

/**
 * One choice in a [FinioSelect]. [leading] is drawn before the label in both the list and the
 * trigger. The closed trigger shows [selectedLabel] (defaults to [label]) — the web's
 * `<SelectValue>` children, for when the field reads differently from the list row — followed by
 * [selectedTrailing] as muted inline text ("HDFC Checking · Bank account").
 */
@Immutable
data class SelectOption<T>(
    val value: T,
    val label: String,
    val description: String? = null,
    val enabled: Boolean = true,
    val leading: (@Composable () -> Unit)? = null,
    val selectedLabel: String? = null,
    val selectedTrailing: String? = null,
)

/**
 * The select (select.tsx): an input-shaped trigger (40dp, `--input` stroke, chevron) that opens
 * the options as a modal bottom sheet styled like the web popover — opaque `--popover`, rows with
 * a check on the selected one and the `--accent` tint while pressed.
 *
 * Compromise: the web anchors a popup list to the trigger; a phone-sized sheet is the native
 * equivalent and keeps long lists (accounts, categories) thumb-reachable. Rows are 44dp tall
 * rather than the popover's 28dp.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> FinioSelect(
    value: T?,
    options: List<SelectOption<T>>,
    onValueChange: (T) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Select",
    title: String? = null,
    enabled: Boolean = true,
    isError: Boolean = false,
) {
    var open by remember { mutableStateOf(false) }
    val selected = options.firstOrNull { it.value == value }
    FieldTrigger(
        text = selected?.let { it.selectedLabel ?: it.label } ?: placeholder,
        isPlaceholder = selected == null,
        trailingText = selected?.selectedTrailing,
        onClick = { open = true },
        modifier = modifier,
        enabled = enabled,
        isError = isError,
        open = open,
        leadingContent = selected?.leading,
    )
    if (open) {
        SelectSheet(
            title = title,
            options = options,
            selectedValue = value,
            onDismiss = { open = false },
            onSelect = {
                onValueChange(it)
                open = false
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> SelectSheet(
    title: String?,
    options: List<SelectOption<T>>,
    selectedValue: T?,
    onDismiss: () -> Unit,
    onSelect: (T) -> Unit,
) {
    val colors = FinioTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = options.size <= 8)
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = (options.indexOfFirst { it.value == selectedValue } - 2).coerceAtLeast(0),
    )
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = FinioRadius.lg, topEnd = FinioRadius.lg),
        containerColor = colors.popover,
        contentColor = colors.popoverForeground,
        tonalElevation = 0.dp,
        scrimColor = colors.scrim,
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 8.dp, bottom = 4.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(FinioShapes.full)
                    .background(colors.border),
            )
        },
    ) {
        Column(Modifier.windowInsetsPadding(WindowInsets.navigationBars)) {
            if (title != null) {
                // SelectLabel: `text-muted-foreground px-1.5 py-1 text-xs`.
                Text(
                    title,
                    Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    style = FinioType.caption,
                    color = colors.mutedForeground,
                )
            }
            LazyColumn(state = listState, modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)) {
                itemsIndexed(options) { _, option ->
                    val isSelected = option.value == selectedValue
                    SelectRow(option, isSelected) {
                        scope.launch { sheetState.hide() }.invokeOnCompletion { onSelect(option.value) }
                    }
                }
            }
        }
    }
}

@Composable
private fun <T> SelectRow(option: SelectOption<T>, selected: Boolean, onClick: () -> Unit) {
    val colors = FinioTheme.colors
    val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Row(
        Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 44.dp)
            .alpha(if (option.enabled) 1f else 0.5f)
            .clip(FinioShapes.md)
            .background(if (pressed) colors.accent else Color.Transparent)
            .semantics { this.selected = selected }
            .clickable(interaction, null, enabled = option.enabled, role = Role.Button, onClick = onClick)
            .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        option.leading?.invoke()
        Column(Modifier.weight(1f)) {
            Text(
                option.label,
                style = FinioType.body,
                color = if (pressed) colors.accentForeground else colors.popoverForeground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (option.description != null) {
                Text(option.description, style = FinioType.caption, color = colors.mutedForeground, maxLines = 2)
            }
        }
        if (selected) {
            Icon(LucideIcons.Check, contentDescription = "Selected", Modifier.size(16.dp), tint = if (pressed) colors.accentForeground else colors.popoverForeground)
        } else {
            Box(Modifier.width(16.dp).height(16.dp))
        }
    }
}
