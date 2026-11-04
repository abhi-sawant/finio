package com.slowatcoding.finio.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.slowatcoding.finio.di.LocalAppContainer
import com.slowatcoding.finio.ui.icons.LucideIcons
import com.slowatcoding.finio.ui.theme.FinioShapes
import com.slowatcoding.finio.ui.theme.FinioTheme
import com.slowatcoding.finio.ui.theme.FinioType
import com.slowatcoding.finio.ui.theme.VisualTheme

/**
 * The visual-theme chooser (web: `VisualThemePicker.tsx`): one radio per world with its light and
 * dark palette and a line on its personality. A theme changes shape, type, density and motion as
 * well as colour, so a name and a swatch alone would undersell it. Light/dark is the separate
 * "Mode" row above. The choice is a per-device preference (`VisualThemeStore`), not a setting.
 */
@Composable
fun VisualThemePicker(modifier: Modifier = Modifier) {
    val container = LocalAppContainer.current
    val current by container.visualTheme.theme.collectAsStateWithLifecycle()
    val colors = FinioTheme.colors
    Column(modifier.fillMaxWidth().padding(16.dp)) {
        Text("Style", style = FinioType.bodyMedium, color = colors.foreground)
        Text(
            "How Finio looks and feels — shape, type, density, not just colour",
            Modifier.padding(bottom = 12.dp),
            style = FinioType.caption,
            color = colors.mutedForeground,
        )
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            VisualTheme.entries.forEach { theme ->
                ThemeOption(theme, selected = theme == current, onClick = { container.visualTheme.set(theme) })
            }
        }
    }
}

@Composable
private fun ThemeOption(theme: VisualTheme, selected: Boolean, onClick: () -> Unit) {
    val colors = FinioTheme.colors
    val shape = FinioShapes.md
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) colors.accent else Color.Transparent)
            .border(1.dp, if (selected) colors.primary else colors.border, shape)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(theme.label, style = FinioType.title, color = colors.foreground)
            if (selected) Icon(LucideIcons.byKebab("check", strokeWidth = 2.4f)!!, "Selected", Modifier.size(15.dp), tint = colors.primary)
        }
        Text(theme.tagline, style = FinioType.caption, color = colors.mutedForeground)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Strip(theme.swatch, "${theme.label} light palette", Modifier.weight(1f))
            Strip(theme.swatchDark, "${theme.label} dark palette", Modifier.weight(1f))
        }
    }
}

@Composable
private fun Strip(swatch: List<Color>, label: String, modifier: Modifier) {
    val shape = RoundedCornerShape(5.dp)
    Row(
        modifier
            .height(20.dp)
            .clip(shape)
            .border(1.dp, FinioTheme.colors.border, shape)
            .semantics { contentDescription = label },
    ) {
        swatch.forEach { Box(Modifier.weight(1f).fillMaxWidth().height(20.dp).background(it)) }
    }
}
