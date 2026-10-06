package com.slowatcoding.finio.ui.screens.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.slowatcoding.finio.core.calc.ForecastInput
import com.slowatcoding.finio.core.calc.buildCashFlowForecast
import com.slowatcoding.finio.core.format.formatDayMonth
import com.slowatcoding.finio.core.format.formatShortDate
import com.slowatcoding.finio.core.format.shouldCompactGroup
import com.slowatcoding.finio.core.util.sampleForTable
import com.slowatcoding.finio.ui.charts.CardTitle
import com.slowatcoding.finio.ui.charts.CartesianChart
import com.slowatcoding.finio.ui.charts.ChartCard
import com.slowatcoding.finio.ui.charts.ChartCursor
import com.slowatcoding.finio.ui.charts.ChartDataTable
import com.slowatcoding.finio.ui.charts.ChartTableRow
import com.slowatcoding.finio.ui.charts.TooltipData
import com.slowatcoding.finio.ui.charts.TooltipRow
import com.slowatcoding.finio.ui.charts.XScale
import com.slowatcoding.finio.ui.charts.XTicks
import com.slowatcoding.finio.ui.charts.monotonePath
import com.slowatcoding.finio.ui.common.collectFinanceState
import com.slowatcoding.finio.ui.common.rememberDerived
import com.slowatcoding.finio.ui.common.rememberMoneyFormatter
import com.slowatcoding.finio.ui.icons.LucideIcons
import com.slowatcoding.finio.ui.theme.FinioShapes
import com.slowatcoding.finio.ui.theme.FinioTheme
import com.slowatcoding.finio.ui.theme.FinioType
import com.slowatcoding.finio.ui.theme.mix
import kotlin.math.abs

private val Horizons = listOf(30 to "30d", 60 to "60d", 90 to "90d")

/**
 * Port of CashFlowForecast.tsx — liquid cash projected over 30/60/90 days: a monotone area with a
 * dashed zero line, today / end / lowest tiles, a shortfall warning, the next four scheduled
 * flows and the everyday-spend estimate.
 */
@Composable
fun CashFlowForecastCard(modifier: Modifier = Modifier) {
    val state by collectFinanceState()
    val money = rememberMoneyFormatter()
    val colors = FinioTheme.colors
    var days by rememberSaveable { mutableIntStateOf(90) }
    val accounts = state.accounts
    val transactions = state.transactions
    val recurring = state.recurring
    val categories = state.categories

    val forecast = rememberDerived(accounts, transactions, recurring, days) {
        buildCashFlowForecast(ForecastInput(accounts = accounts, transactions = transactions, recurring = recurring, days = days))
    } ?: return
    if (forecast.isEmpty) return

    val compact: (Double) -> String = { money(it, compact = true) }
    fun categoryName(id: String) = categories.firstOrNull { it.id == id }?.name ?: "Uncategorized"
    val upcoming = forecast.scheduled.take(4)
    val tilesCompact = shouldCompactGroup(listOf(forecast.startBalance, forecast.endBalance, forecast.low?.balance ?: 0.0))
    val tile: (Double) -> String = { money(it, compact = true, precise = false, forceCompact = tilesCompact) }
    val projected = sampleForTable(forecast.points)
    val labels = forecast.points.map { formatDayMonth(it.date) }
    val primary = colors.primary
    val zeroLine = colors.mutedForeground

    ChartCard(title = null, modifier = modifier) {
        Row(
            Modifier.fillMaxWidth().padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CardTitle("Cash-flow forecast")
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Horizons.forEach { (d, label) -> MiniToggle(label, days == d) { days = d } }
            }
        }
        Text(
            "Liquid cash projected from your recurring rules plus your last ${forecast.lookbackDays} days " +
                "of everyday spending. Credit cards are excluded until the payment leaves an account.",
            Modifier.padding(bottom = 12.dp),
            style = FinioType.caption,
            color = colors.mutedForeground,
        )

        CartesianChart(
            count = forecast.points.size,
            xLabels = labels,
            domainValues = forecast.points.map { it.balance },
            scale = XScale.Point,
            xTicks = XTicks.PreserveStartEnd(32.dp),
            hidden = money.hidden,
            cursor = ChartCursor.Line,
            tooltip = { i ->
                TooltipData(labels[i], listOf(TooltipRow("Projected balance", money(forecast.points[i].balance, precise = false))))
            },
            activeDots = { i -> listOf(forecast.points[i].balance to primary) },
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .semantics {
                    contentDescription = "Projected balance over the next $days days, from " +
                        "${compact(forecast.startBalance)} today to ${compact(forecast.endBalance)}."
                },
        ) { g ->
            // Zero is the line that matters — everything below it is an overdraft.
            val zero = g.y(0.0)
            drawLine(
                zeroLine,
                Offset(g.plot.left, zero),
                Offset(g.plot.right, zero),
                1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx())),
            )
            val pts = forecast.points.mapIndexed { i, p -> Offset(g.x(i), g.y(p.balance)) }
            if (pts.isNotEmpty()) {
                val line = monotonePath(pts)
                val area = Path().apply {
                    addPath(line)
                    lineTo(pts.last().x, zero)
                    lineTo(pts.first().x, zero)
                    close()
                }
                drawPath(area, primary.mix(0.12f))
                drawPath(line, primary, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }

        ChartDataTable(
            caption = "Projected liquid balance by day",
            columns = listOf("Date", "Projected balance"),
            note = if (projected.sampled) "sampled to ${projected.rows.size} of ${forecast.points.size} days" else null,
            rows = projected.rows.map { ChartTableRow(it.key, listOf(formatDayMonth(it.date), compact(it.balance))) },
        )

        Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile(Modifier.weight(1f), "Today", LucideIcons.Wallet, tile(forecast.startBalance), null, false)
            StatTile(Modifier.weight(1f), "In $days days", null, tile(forecast.endBalance), null, forecast.endBalance < 0)
            val low = forecast.low
            StatTile(
                Modifier.weight(1f),
                "Lowest",
                LucideIcons.TrendingDown,
                if (low != null) tile(low.balance) else "—",
                low?.let { formatDayMonth(it.date) },
                low != null && low.balance < 0,
            )
        }

        forecast.shortfallDate?.let { date ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .clip(FinioShapes.sm)
                    .background(colors.destructive.mix(0.1f))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(LucideIcons.AlertTriangle, null, Modifier.padding(top = 2.dp).size(13.dp), tint = colors.destructive)
                Text(
                    buildAnnotatedString {
                        append("At this rate your liquid balance runs out around ")
                        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(formatShortDate(date)) }
                        append(".")
                    },
                    style = FinioType.caption,
                    color = colors.destructive,
                )
            }
        }

        if (upcoming.isNotEmpty()) {
            Column(Modifier.padding(top = 16.dp)) {
                Text(
                    "Scheduled next — ${compact(forecast.totals.scheduledOut)} out, ${compact(forecast.totals.scheduledIn)} in over $days days",
                    Modifier.padding(bottom = 8.dp),
                    style = FinioType.label,
                    color = colors.mutedForeground,
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    upcoming.forEach { flow ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(formatDayMonth(flow.date), Modifier.width(56.dp), style = FinioType.caption, color = colors.mutedForeground)
                            Text(
                                flow.note.ifEmpty { categoryName(flow.categoryId) },
                                Modifier.weight(1f),
                                style = FinioType.caption,
                                color = colors.foreground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                (if (flow.delta > 0) "+" else "−") + compact(abs(flow.delta)),
                                style = FinioType.caption.copy(fontWeight = FontWeight.SemiBold),
                                color = if (flow.delta > 0) colors.positive else colors.destructive,
                            )
                        }
                    }
                }
            }
        }

        if (forecast.dailyEstimate > 0) {
            val leaders = forecast.categoryAverages.take(3).joinToString(", ") { categoryName(it.categoryId) }
            Text(
                "Everyday spend estimated at ${compact(forecast.dailyEstimate)} a day" +
                    (if (forecast.categoryAverages.isNotEmpty()) ", led by $leaders" else "") + ".",
                Modifier.padding(top = 12.dp),
                style = FinioType.caption,
                color = colors.mutedForeground,
            )
        }
    }
}

@Composable
private fun StatTile(
    modifier: Modifier,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    value: String,
    sub: String?,
    negative: Boolean,
) {
    val colors = FinioTheme.colors
    MutedTile(modifier) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (icon != null) Icon(icon, null, Modifier.size(10.dp), tint = colors.mutedForeground)
                Text(label, style = FinioType.label, color = colors.mutedForeground, maxLines = 1)
            }
            Text(
                value,
                Modifier.padding(top = 2.dp),
                style = FinioType.caption.copy(fontWeight = FontWeight.SemiBold),
                color = if (negative) colors.destructive else colors.foreground,
            )
            if (sub != null) Text(sub, style = FinioType.caption, color = colors.mutedForeground)
        }
    }
}
