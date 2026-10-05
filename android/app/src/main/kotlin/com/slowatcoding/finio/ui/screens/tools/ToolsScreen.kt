package com.slowatcoding.finio.ui.screens.tools

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.slowatcoding.finio.debugGallery
import com.slowatcoding.finio.ui.components.FinioCard
import com.slowatcoding.finio.ui.components.FinioDivider
import com.slowatcoding.finio.ui.components.FinioScreen
import com.slowatcoding.finio.ui.components.PageTitle
import com.slowatcoding.finio.ui.icons.LucideIcons
import com.slowatcoding.finio.ui.navigation.FinioNavigator
import com.slowatcoding.finio.ui.navigation.MoreNavItem
import com.slowatcoding.finio.ui.navigation.MoreNavItems
import com.slowatcoding.finio.ui.navigation.Routes
import com.slowatcoding.finio.ui.theme.FinioTheme
import com.slowatcoding.finio.ui.theme.FinioType
import com.slowatcoding.finio.ui.theme.mix

/**
 * Port of web/src/pages/Tools.tsx — the mobile home of the secondary destinations (the desktop
 * Sidebar's "Tools" group): one glass card of rows, each an icon, a label, a one-line description
 * and a chevron.
 *
 * Debug builds: long-press the "Tools" title to open the Mudra component gallery.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ToolsScreen(nav: FinioNavigator) {
    FinioScreen(header = {
        PageTitle(
            "Tools",
            Modifier.then(
                if (debugGallery != null) {
                    Modifier.combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onLongClick = { nav.navigate(Routes.DebugGallery) },
                        onClick = {},
                    )
                } else Modifier,
            ),
        )
    }) {
        FinioCard(contentPadding = PaddingValues(0.dp)) {
            MoreNavItems.forEachIndexed { index, item ->
                if (index > 0) FinioDivider()
                ToolRow(item, onClick = { nav.navigate(item.route) })
            }
        }
    }
}

@Composable
private fun ToolRow(item: MoreNavItem, onClick: () -> Unit) {
    val colors = FinioTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Row(
        Modifier
            .fillMaxWidth()
            // `hover:bg-muted/50` — the touch equivalent is a pressed tint.
            .background(if (pressed) colors.muted.mix(0.5f) else Color.Transparent)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(item.icon, null, Modifier.size(18.dp), tint = colors.mutedForeground)
        Column(Modifier.weight(1f)) {
            Text(item.label, style = FinioType.bodyMedium, color = colors.foreground)
            Text(
                item.description,
                style = FinioType.caption,
                color = colors.mutedForeground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(LucideIcons.ChevronRight, null, Modifier.size(16.dp), tint = colors.mutedForeground)
    }
}
