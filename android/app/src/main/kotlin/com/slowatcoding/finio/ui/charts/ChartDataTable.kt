package com.slowatcoding.finio.ui.charts

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slowatcoding.finio.ui.components.FinioDivider
import com.slowatcoding.finio.ui.icons.LucideIcons
import com.slowatcoding.finio.ui.theme.FinioShapes
import com.slowatcoding.finio.ui.theme.FinioTheme
import com.slowatcoding.finio.ui.theme.FinioType
import com.slowatcoding.finio.ui.theme.mix

/** One table row: the first cell is the row header (the month, date…), the rest are values. */
@Immutable
data class ChartTableRow(val key: String, val cells: List<String>)

/**
 * Port of ChartDataTable.tsx — the numbers behind a chart, as a real table inside a "View data
 * table" disclosure (Table2 + chevron, 11sp muted). The table is 11sp, capped at 224dp tall and
 * scrolls, with a pinned header row; the first column is muted, the rest right-aligned and
 * medium. Columns size to their widest cell like an auto-layout HTML table.
 */
@Composable
fun ChartDataTable(
    caption: String,
    columns: List<String>,
    rows: List<ChartTableRow>,
    modifier: Modifier = Modifier,
    note: String? = null,
) {
    if (rows.isEmpty()) return
    val colors = FinioTheme.colors
    var open by rememberSaveable(caption) { mutableStateOf(false) }
    val rotation by animateFloatAsState(if (open) 180f else 0f, label = "chevron")
    val small = FinioType.label.copy(fontSize = 11.sp, lineHeight = 16.sp)

    Column(modifier.fillMaxWidth().padding(top = 12.dp)) {
        Row(
            Modifier
                .clip(FinioShapes.chip)
                .semantics { stateDescription = if (open) "Expanded" else "Collapsed" }
                .clickable(role = Role.Button) { open = !open },
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(LucideIcons.Table2, null, Modifier.size(12.dp), tint = colors.mutedForeground)
            Text("View data table", style = small, color = colors.mutedForeground)
            Icon(LucideIcons.ChevronDown, null, Modifier.size(12.dp).rotate(rotation), tint = colors.mutedForeground)
        }
        if (open) {
            val desc = if (note != null) "$caption — $note" else caption
            DataTable(columns, rows, Modifier.padding(top = 8.dp).semantics { contentDescription = desc })
        }
    }
}

@Composable
private fun DataTable(columns: List<String>, rows: List<ChartTableRow>, modifier: Modifier) {
    val colors = FinioTheme.colors
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val header = FinioType.label.copy(fontSize = 11.sp, lineHeight = 16.sp)
    val rowHead = FinioType.caption.copy(fontSize = 11.sp, lineHeight = 16.sp)
    val value = header
    val cellPad = 8.dp

    // Natural width of each column (widest cell + its padding), like table-layout: auto.
    val natural = remember(columns, rows) {
        columns.indices.map { c ->
            val cells = listOf(columns[c] to header) + rows.mapNotNull { r -> r.cells.getOrNull(c)?.let { it to if (c == 0) rowHead else value } }
            val px = cells.maxOf { (text, style) -> measurer.measure(text, style.copy(fontWeight = FontWeight.Medium)).size.width }
            with(density) { px.toDp() } + cellPad
        }
    }

    BoxWithConstraints(modifier.fillMaxWidth()) {
        val others = natural.drop(1).fold(0.dp) { a, b -> a + b }
        val first = maxOf(natural.first(), maxWidth - others)
        val widths = listOf(first) + natural.drop(1)
        val total = widths.fold(0.dp) { a, b -> a + b }
        val hScroll = rememberScrollState()
        Box(Modifier.fillMaxWidth().horizontalScroll(hScroll, enabled = total > maxWidth)) {
            Column(Modifier.width(total)) {
                Row(Modifier.background(colors.card).padding(vertical = 4.dp)) {
                    columns.forEachIndexed { i, col ->
                        Text(
                            col,
                            Modifier.width(widths[i]).padding(start = if (i == 0) 0.dp else cellPad),
                            style = header,
                            color = colors.mutedForeground,
                            textAlign = if (i == 0) TextAlign.Start else TextAlign.End,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                }
                Column(Modifier.heightIn(max = 224.dp - 24.dp).verticalScroll(rememberScrollState())) {
                    rows.forEach { row ->
                        FinioDivider(color = colors.border.mix(0.6f))
                        Row(Modifier.padding(vertical = 4.dp)) {
                            row.cells.forEachIndexed { i, cell ->
                                Text(
                                    cell,
                                    Modifier
                                        .width(widths.getOrElse(i) { 0.dp })
                                        .padding(start = if (i == 0) 0.dp else cellPad, end = if (i == 0) cellPad else 0.dp),
                                    style = if (i == 0) rowHead else value,
                                    color = if (i == 0) colors.mutedForeground else colors.foreground,
                                    textAlign = if (i == 0) TextAlign.Start else TextAlign.End,
                                    maxLines = 1,
                                    softWrap = false,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Port of EmptyChart.tsx — the placeholder for a chart that can't draw yet: a muted line-chart
 * glyph over one 12sp line, centred in the chart's own height.
 */
@Composable
fun EmptyChart(
    modifier: Modifier = Modifier,
    message: String = "Not enough data yet — check back after a few more entries.",
) {
    val colors = FinioTheme.colors
    Column(
        modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(LucideIcons.LineChart, null, Modifier.size(20.dp), tint = colors.mutedForeground)
        Text(
            message,
            Modifier.width(224.dp),
            style = FinioType.caption,
            color = colors.mutedForeground,
            textAlign = TextAlign.Center,
        )
    }
}
