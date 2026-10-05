package com.slowatcoding.finio.ui.shell

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.slowatcoding.finio.ui.navigation.FinioTab
import com.slowatcoding.finio.ui.theme.FinioShapes
import com.slowatcoding.finio.ui.theme.FinioTheme
import com.slowatcoding.finio.ui.theme.FinioType
import com.slowatcoding.finio.ui.theme.cssShadow

/**
 * The mobile bottom nav (Layout.tsx `<nav>`): `glass-chrome` (strong glass — no backdrop blur on
 * Android), a glass-border top hairline, the lavender lift shadow above it, 8dp sides and top,
 * and the navigation-bar inset below (`pb-safe`).
 *
 * Each tab: 20dp lucide icon (stroke 2, 2.4 when active), a 16×4dp gradient pill (invisible when
 * inactive), a 12sp/500 label; primary when active, muted otherwise, colour easing over 150ms.
 */
@Composable
fun FinioTabBar(active: FinioTab?, onSelect: (FinioTab) -> Unit, modifier: Modifier = Modifier) {
    val colors = FinioTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            // cssShadow before background, or the fill paints over the shadow.
            .cssShadow(RectangleShape, FinioTheme.shadows.tabBar)
            .background(colors.glassStrong)
            .drawBehind { drawLine(colors.glassBorder, Offset(0f, 0.5.dp.toPx()), Offset(size.width, 0.5.dp.toPx()), 1.dp.toPx()) }
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(start = 8.dp, end = 8.dp, top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FinioTab.entries.forEach { tab ->
            TabItem(tab, selected = tab == active, onClick = { onSelect(tab) }, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun TabItem(tab: FinioTab, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val colors = FinioTheme.colors
    val tint by animateColorAsState(if (selected) colors.primary else colors.mutedForeground, tween(150), label = "tab")
    val icon = remember(tab, selected) { tab.icon(if (selected) 2.4f else 2f) }
    Column(
        modifier
            .clip(FinioShapes.sm)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            )
            .semantics { this.selected = selected }
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = tint)
        Box(
            Modifier
                .width(16.dp)
                .height(4.dp)
                .alpha(if (selected) 1f else 0f)
                .clip(FinioShapes.full)
                .background(FinioTheme.brushes.gradPrimary),
        )
        Text(tab.label, style = FinioType.label, color = tint, maxLines = 1)
    }
}
