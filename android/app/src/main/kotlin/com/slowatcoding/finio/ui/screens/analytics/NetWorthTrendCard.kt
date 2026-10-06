package com.slowatcoding.finio.ui.screens.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.slowatcoding.finio.core.calc.NetWorthSeriesInput
import com.slowatcoding.finio.core.calc.NetWorthSource
import com.slowatcoding.finio.core.calc.buildNetWorthSeries
import com.slowatcoding.finio.core.format.formatPercentChange
import com.slowatcoding.finio.core.period.normalizeMonthStartDay
import com.slowatcoding.finio.ui.charts.CardTitle
import com.slowatcoding.finio.ui.charts.CartesianChart
import com.slowatcoding.finio.ui.charts.ChartCard
import com.slowatcoding.finio.ui.charts.ChartCursor
import com.slowatcoding.finio.ui.charts.ChartDataTable
import com.slowatcoding.finio.ui.charts.ChartTableRow
import com.slowatcoding.finio.ui.charts.EmptyChart
import com.slowatcoding.finio.ui.charts.TooltipData
import com.slowatcoding.finio.ui.charts.TooltipRow
import com.slowatcoding.finio.ui.charts.XScale
import com.slowatcoding.finio.ui.charts.XTicks
import com.slowatcoding.finio.ui.charts.barSlots
import com.slowatcoding.finio.ui.charts.drawBar
import com.slowatcoding.finio.ui.charts.monotonePath
import com.slowatcoding.finio.ui.common.collectFinanceState
import com.slowatcoding.finio.ui.common.rememberDerived
import com.slowatcoding.finio.ui.common.rememberMoneyFormatter
import com.slowatcoding.finio.ui.icons.LucideIcons
import com.slowatcoding.finio.ui.theme.FinioTheme
import com.slowatcoding.finio.ui.theme.FinioType
import kotlin.math.abs

private val Ranges = listOf(6 to "6m", 12 to "12m", 24 to "24m")

/**
 * Port of NetWorthTrend.tsx — net worth by financial month over 6/12/24 months: assets as bars up,
 * liabilities as bars down, net worth as the monotone line through them; the headline figure with
 * its change, the data table and the snapshot note.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NetWorthTrendCard(modifier: Modifier = Modifier) {
    val state by collectFinanceState()
    val money = rememberMoneyFormatter()
    val colors = FinioTheme.colors
    val accounts = state.accounts
    val transactions = state.transactions
    val snapshots = state.netWorthSnapshots
    val monthStartDay = normalizeMonthStartDay(state.settings.monthStartDay)
    var months by rememberSaveable { mutableIntStateOf(12) }

    val series = rememberDerived(accounts, transactions, snapshots, months, monthStartDay) {
        buildNetWorthSeries(
            NetWorthSeriesInput(accounts = accounts, transactions = transactions, snapshots = snapshots, months = months, monthStartDay = monthStartDay),
        )
    } ?: return
    if (accounts.isEmpty()) return

    val compact: (Double) -> String = { money(it, compact = true) }
    val latest = series.lastOrNull()
    val earliest = series.firstOrNull()
    val change = if (latest != null && earliest != null) latest.netWorth - earliest.netWorth else 0.0
    val changeRatio = if (earliest != null && earliest.netWorth != 0.0) change / abs(earliest.netWorth) else null
    val snapshotCount = series.count { it.source == NetWorthSource.Snapshot }
    val assetsColor = colors.primary
    val liabilitiesColor = colors.destructive

    ChartCard(title = null, modifier = modifier) {
        Row(
            Modifier.fillMaxWidth().padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CardTitle("Net worth over time")
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Ranges.forEach { (m, label) -> MiniToggle(label, months == m) { months = m } }
            }
        }

        FlowRow(
            Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            Text(if (latest != null) compact(latest.netWorth) else "—", style = FinioType.money, color = colors.foreground)
            if (change != 0.0) {
                val tone = if (change > 0) colors.positive else colors.destructive
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    Icon(if (change > 0) LucideIcons.TrendingUp else LucideIcons.TrendingDown, null, Modifier.size(12.dp), tint = tone)
                    Text(
                        compact(abs(change)) + (changeRatio?.let { " (${formatPercentChange(it)})" } ?: ""),
                        style = FinioType.label,
                        color = tone,
                    )
                }
            }
            Text("over $months months", style = FinioType.caption, color = colors.mutedForeground)
        }

        val chartModifier = Modifier
            .fillMaxWidth()
            .height(176.dp)
            .semantics { contentDescription = "Net worth by month${latest?.let { ", currently ${compact(it.netWorth)}" } ?: ""}." }
        if (series.size < 2) {
            EmptyChart(chartModifier, "Net worth needs at least two months of history to chart.")
        } else {
            CartesianChart(
                count = series.size,
                xLabels = series.map { it.shortLabel },
                domainValues = series.flatMap { listOf(it.assets, -it.liabilities, it.netWorth) },
                scale = XScale.Band,
                xTicks = XTicks.PreserveStartEnd(24.dp),
                hidden = money.hidden,
                cursor = ChartCursor.Band,
                tooltip = { i ->
                    val p = series[i]
                    TooltipData(
                        p.shortLabel,
                        listOf(
                            TooltipRow("Assets", money(abs(p.assets))),
                            TooltipRow("Liabilities", money(abs(p.liabilities))),
                            TooltipRow("Net worth", money(abs(p.netWorth))),
                        ),
                    )
                },
                activeDots = { i -> listOf(series[i].netWorth to assetsColor) },
                modifier = chartModifier,
            ) { g ->
                // Assets up, liabilities down, net worth as the line that sums them.
                val slots = barSlots(g, 2)
                val zero = g.y(0.0)
                val r = 4.dp.toPx()
                series.forEachIndexed { i, p ->
                    val left = g.x(i) - g.step / 2f
                    drawBar(assetsColor, left + slots[0].first, slots[0].second, g.y(p.assets), zero, r, alpha = 0.55f)
                    val liability = if (p.liabilities == 0.0) 0.0 else -p.liabilities
                    drawBar(liabilitiesColor, left + slots[1].first, slots[1].second, g.y(liability), zero, r, alpha = 0.55f)
                }
                val line = monotonePath(series.mapIndexed { i, p -> Offset(g.x(i), g.y(p.netWorth)) })
                drawPath(line, assetsColor, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }

        ChartDataTable(
            caption = "Net worth, assets and liabilities by month",
            columns = listOf("Month", "Net worth", "Assets", "Liabilities"),
            rows = series.map { ChartTableRow(it.key, listOf(it.shortLabel, compact(it.netWorth), compact(it.assets), compact(it.liabilities))) },
        )

        Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(LucideIcons.Camera, null, Modifier.padding(top = 2.dp).size(11.dp), tint = colors.mutedForeground)
            Text(
                (
                    if (snapshotCount > 0) {
                        "$snapshotCount of these months ${if (snapshotCount == 1) "is a" else "are"} saved snapshot${if (snapshotCount == 1) "" else "s"}" +
                            " — editing old transactions won't rewrite them. "
                    } else {
                        ""
                    }
                    ) + "Months without a snapshot are reconstructed from your current history.",
                style = FinioType.caption,
                color = colors.mutedForeground,
            )
        }
    }
}
