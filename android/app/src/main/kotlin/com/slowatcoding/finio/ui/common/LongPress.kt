package com.slowatcoding.finio.ui.common

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.slowatcoding.finio.ui.components.FinioDivider
import com.slowatcoding.finio.ui.components.FinioPopover
import com.slowatcoding.finio.ui.theme.FinioShapes
import com.slowatcoding.finio.ui.theme.FinioTheme
import com.slowatcoding.finio.ui.theme.FinioType
import com.slowatcoding.finio.ui.theme.mix

/**
 * Port of `useLongPress`: tap → [onClick], hold (~500ms, the platform long-press timeout) →
 * [onLongClick] with a haptic tick. Compose already swallows the click that the same press would
 * otherwise generate, so there is no `firedRef` dance. No ripple — the web rows have none; pass a
 * pressed tint yourself if the row needs one.
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.longPressable(
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    enabled: Boolean = true,
    onLongClickLabel: String? = null,
): Modifier = composed {
    val haptics = LocalHapticFeedback.current
    combinedClickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        enabled = enabled,
        role = Role.Button,
        onLongClickLabel = onLongClickLabel,
        onLongClick = onLongClick?.let { cb ->
            {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                cb()
            }
        },
        onClick = onClick,
    )
}

/** One entry of an [ActionMenu]. [separatorBefore] draws the `DropdownMenuSeparator` above it. */
data class MenuAction(
    val label: String,
    val icon: ImageVector? = null,
    val destructive: Boolean = false,
    val separatorBefore: Boolean = false,
    val enabled: Boolean = true,
    val onClick: () -> Unit,
)

/**
 * The long-press row menu (dropdown-menu.tsx content): popover fill, 4dp padding, 14sp items with
 * 16dp icons, destructive items in magenta. Call it from inside the anchor's layout (it anchors
 * to its parent, below it, flipping above near the bottom edge). Picking an item dismisses first.
 *
 *   Box {
 *       Row(Modifier.longPressable(onClick = open, onLongClick = { menu = true })) { … }
 *       if (menu) ActionMenu(onDismissRequest = { menu = false }, actions = listOf(
 *           MenuAction("Select", LucideIcons.SquareCheckBig) { … },
 *           MenuAction("Delete", LucideIcons.Trash2, destructive = true, separatorBefore = true) { … },
 *       ))
 *   }
 */
@Composable
fun ActionMenu(
    onDismissRequest: () -> Unit,
    actions: List<MenuAction>,
    modifier: Modifier = Modifier,
    minWidth: Dp = 128.dp,
) {
    val colors = FinioTheme.colors
    FinioPopover(onDismissRequest = onDismissRequest, modifier = modifier, contentPadding = PaddingValues(4.dp)) {
        Column(Modifier.widthIn(min = minWidth).width(androidx.compose.foundation.layout.IntrinsicSize.Max)) {
            actions.forEach { action ->
                if (action.separatorBefore) FinioDivider(Modifier.padding(vertical = 4.dp))
                val tint = if (action.destructive) colors.destructive else colors.popoverForeground
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(FinioShapes.sm)
                        .clickable(enabled = action.enabled, role = Role.Button) {
                            onDismissRequest()
                            action.onClick()
                        }
                        .background(androidx.compose.ui.graphics.Color.Transparent)
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (action.icon != null) Icon(action.icon, null, Modifier.size(16.dp), tint = if (action.enabled) tint else tint.mix(0.5f))
                    Text(action.label, style = FinioType.body, color = if (action.enabled) tint else tint.mix(0.5f), maxLines = 1)
                }
            }
        }
    }
}
