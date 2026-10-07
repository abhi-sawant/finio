package com.slowatcoding.finio.ui.screens.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.scale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.slowatcoding.finio.core.data.COLOR_PALETTE
import com.slowatcoding.finio.ui.components.CategoryIcon
import com.slowatcoding.finio.ui.components.CategoryIconNames
import com.slowatcoding.finio.ui.components.parseHexColor
import com.slowatcoding.finio.ui.theme.FinioShapes
import com.slowatcoding.finio.ui.theme.FinioTheme
import com.slowatcoding.finio.ui.theme.FinioType

/**
 * The colour swatch row every picker offers (`COLOR_PALETTE`): 28dp discs, 8dp apart, wrapping;
 * the selected one wears `ring-2 ring-primary ring-offset-2` and grows to 110%.
 * Shared by Manage Categories and Manage Labels.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorPalettePicker(selected: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    val colors = FinioTheme.colors
    FlowRow(
        modifier.fillMaxWidth().padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        COLOR_PALETTE.forEach { c ->
            val active = c.equals(selected, ignoreCase = true)
            Box(
                Modifier
                    .size(28.dp)
                    .scale(if (active) 1.1f else 1f)
                    .then(
                        if (active) {
                            // ring-2 ring-primary ring-offset-2: a 2dp ring, 2dp clear of the disc.
                            Modifier.drawBehind {
                                drawCircle(colors.primary, radius = size.minDimension / 2 + 3.dp.toPx(), style = Stroke(2.dp.toPx()))
                            }
                        } else {
                            Modifier
                        },
                    )
                    .clip(FinioShapes.full)
                    .background(parseHexColor(c))
                    .semantics {
                        contentDescription = "Color $c"
                        this.selected = active
                    }
                    .clickable(role = Role.Button) { onSelect(c) },
            )
        }
    }
}

/**
 * The category icon picker (ManageCategories' `grid h-50 grid-cols-6 gap-2 overflow-auto`):
 * six columns of 36dp pill buttons in a 200dp scrolling box; the chosen one is primary-bordered
 * on primary at 10%.
 */
@Composable
fun CategoryIconPicker(selected: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    val colors = FinioTheme.colors
    Column(
        modifier.fillMaxWidth().height(200.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CategoryIconNames.chunked(6).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { name ->
                    val active = name == selected
                    Box(
                        Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(FinioShapes.full)
                            .background(if (active) colors.primary.copy(alpha = 0.1f) else colors.card)
                            .border(1.dp, if (active) colors.primary else colors.border, FinioShapes.full)
                            .semantics {
                                contentDescription = "Icon $name"
                                this.selected = active
                            }
                            .clickable(role = Role.Button) { onSelect(name) },
                        contentAlignment = Alignment.Center,
                    ) {
                        CategoryIcon(name, size = 16.dp, tint = colors.foreground)
                    }
                }
                repeat(6 - row.size) { Box(Modifier.weight(1f)) }
            }
        }
    }
}

/** An inline validation line inside a dialog (toasts draw beneath dialog windows on Android). */
@Composable
fun DialogError(text: String?) {
    if (text != null) Text(text, style = FinioType.caption, color = FinioTheme.colors.destructive)
}
