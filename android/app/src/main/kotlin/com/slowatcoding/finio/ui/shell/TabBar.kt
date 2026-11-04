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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.slowatcoding.finio.ui.components.chromeGlass
import com.slowatcoding.finio.ui.navigation.FinioTab
import com.slowatcoding.finio.ui.theme.FinioShapes
import com.slowatcoding.finio.ui.theme.FinioTheme
import com.slowatcoding.finio.ui.theme.FinioType
import com.slowatcoding.finio.ui.theme.VisualTheme
import com.slowatcoding.finio.ui.theme.cssShadow

/**
 * The mobile bottom nav (Layout.tsx `<nav>`), dressed by the active visual theme the way
 * the web theme stylesheets restyles `[data-slot='tab-bar']`:
 *
 * - **Mudra** — strong glass, a glass-border hairline, the lavender lift shadow; icon, a 16×4dp
 *   gradient pill (invisible when inactive) and a 12sp label, primary when active.
 * - **Khata** — a full-width ink bar; the active tab is a red index tab.
 * - **Gulal** — a floating ink pill; the active tab is a yellow chip.
 * - **Shunya** — text only, a hairline above, a dot under the current page.
 * - **Bagh** — a floating sheet with leaf corners; the active tab is a soft pebble.
 */
@Composable
fun FinioTabBar(active: FinioTab?, onSelect: (FinioTab) -> Unit, modifier: Modifier = Modifier) {
    when (FinioTheme.visual) {
        VisualTheme.Mudra -> MudraBar(active, onSelect, modifier)
        VisualTheme.Khata -> KhataBar(active, onSelect, modifier)
        VisualTheme.Gulal -> GulalBar(active, onSelect, modifier)
        VisualTheme.Shunya -> ShunyaBar(active, onSelect, modifier)
        VisualTheme.Bagh -> BaghBar(active, onSelect, modifier)
    }
}

// ─────────────────────────────── Mudra ───────────────────────────────

@Composable
private fun MudraBar(active: FinioTab?, onSelect: (FinioTab) -> Unit, modifier: Modifier) {
    val colors = FinioTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            // cssShadow before background, or the fill paints over the shadow.
            .cssShadow(RectangleShape, FinioTheme.shadows.tabBar)
            .background(colors.chromeGlass)
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
            .tabClickable(selected, onClick)
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

@Composable
private fun Modifier.tabClickable(selected: Boolean, onClick: () -> Unit): Modifier = this
    .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        role = Role.Tab,
        onClick = onClick,
    )
    .semantics { this.selected = selected }

// ─────────────────────────────── shared ───────────────────────────────

/** One tab cell for the themed bars: [icon] optional, a [label], coloured by [tint]. */
@Composable
private fun ThemedTab(
    tab: FinioTab,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
    tint: Color,
    shape: Shape,
    background: Color = Color.Transparent,
    showIcon: Boolean = true,
    showLabel: Boolean = true,
    horizontal: Boolean = false,
) {
    val icon = remember(tab, selected) { tab.icon(if (selected) 2.4f else 2f) }
    val content: @Composable () -> Unit = {
        if (showIcon) Icon(icon, contentDescription = if (showLabel) null else tab.label, modifier = Modifier.size(20.dp), tint = tint)
        if (showLabel) Text(tab.label, style = FinioType.label, color = tint, maxLines = 1)
    }
    val m = modifier.clip(shape).background(background, shape).tabClickable(selected, onClick)
    if (horizontal) {
        Row(m.padding(horizontal = 14.dp, vertical = 10.dp), Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally), Alignment.CenterVertically) { content() }
    } else {
        Column(m.padding(horizontal = 4.dp, vertical = 6.dp), Arrangement.spacedBy(3.dp), Alignment.CenterHorizontally) { content() }
    }
}

// ─────────────────────────────── Khata ───────────────────────────────

@Composable
private fun KhataBar(active: FinioTab?, onSelect: (FinioTab) -> Unit, modifier: Modifier) {
    val dark = FinioTheme.colors.isDark
    val bar = if (dark) Color(0xFF09110E) else Color(0xFF14234A)
    val text = Color(0xFFE8F0E0)
    val red = if (dark) Color(0xFFFF7B6C) else Color(0xFFB8322A)
    val redInk = if (dark) Color(0xFF1A0E0C) else Color.White
    Row(
        modifier.fillMaxWidth().background(bar).windowInsetsPadding(WindowInsets.navigationBars),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FinioTab.entries.forEach { tab ->
            val selected = tab == active
            ThemedTab(
                tab, selected, { onSelect(tab) }, Modifier.weight(1f),
                tint = if (selected) redInk else text.copy(alpha = 0.82f),
                shape = RectangleShape,
                background = if (selected) red else Color.Transparent,
            )
        }
    }
}

// ─────────────────────────────── Gulal ───────────────────────────────

@Composable
private fun GulalBar(active: FinioTab?, onSelect: (FinioTab) -> Unit, modifier: Modifier) {
    val colors = FinioTheme.colors
    val ink = colors.foreground // ink in light, cream in dark — the bar inverts with the page
    val shape = CircleShape
    Row(
        modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(start = 16.dp, end = 16.dp, bottom = 14.dp)
            .fillMaxWidth()
            .cssShadow(shape, FinioTheme.shadows.tabBar)
            .background(ink, shape)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FinioTab.entries.forEach { tab ->
            val selected = tab == active
            ThemedTab(
                tab, selected, { onSelect(tab) }, Modifier,
                tint = if (selected) Color(0xFF1B1230) else colors.background,
                shape = shape,
                background = if (selected) Color(0xFFFFE14D) else Color.Transparent,
                showLabel = selected,
                horizontal = true,
            )
        }
    }
}

// ─────────────────────────────── Shunya ───────────────────────────────

@Composable
private fun ShunyaBar(active: FinioTab?, onSelect: (FinioTab) -> Unit, modifier: Modifier) {
    val colors = FinioTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            .background(colors.background)
            .drawBehind { drawLine(colors.border, Offset(0f, 0.5.dp.toPx()), Offset(size.width, 0.5.dp.toPx()), 1.dp.toPx()) }
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(start = 4.dp, end = 4.dp, top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FinioTab.entries.forEach { tab ->
            val selected = tab == active
            Column(
                Modifier.weight(1f).tabClickable(selected) { onSelect(tab) }.padding(top = 10.dp, bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    tab.label,
                    style = FinioType.label.copy(fontSize = androidx.compose.ui.unit.TextUnit(13f, androidx.compose.ui.unit.TextUnitType.Sp)),
                    color = if (selected) colors.foreground else colors.mutedForeground,
                    maxLines = 1,
                )
                Box(Modifier.size(4.dp).clip(CircleShape).background(if (selected) colors.primary else Color.Transparent))
            }
        }
    }
}

// ─────────────────────────────── Bagh ───────────────────────────────

@Composable
private fun BaghBar(active: FinioTab?, onSelect: (FinioTab) -> Unit, modifier: Modifier) {
    val colors = FinioTheme.colors
    val shape = RoundedCornerShape(32.dp, 32.dp, 32.dp, 12.dp)
    Row(
        modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(start = 14.dp, end = 14.dp, bottom = 14.dp)
            .fillMaxWidth()
            .cssShadow(shape, FinioTheme.shadows.tabBar)
            .background(colors.card, shape)
            .padding(6.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FinioTab.entries.forEach { tab ->
            val selected = tab == active
            ThemedTab(
                tab, selected, { onSelect(tab) }, Modifier.weight(1f),
                tint = if (selected) colors.accentForeground else colors.mutedForeground,
                shape = RoundedCornerShape(22.dp),
                background = if (selected) colors.accent else Color.Transparent,
            )
        }
    }
}
