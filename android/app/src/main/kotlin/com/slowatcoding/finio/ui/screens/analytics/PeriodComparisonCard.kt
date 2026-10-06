package com.slowatcoding.finio.ui.screens.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.slowatcoding.finio.core.calc.PeriodSummary
import com.slowatcoding.finio.core.calc.buildPeriodComparison
import com.slowatcoding.finio.core.calc.categoryMovements
import com.slowatcoding.finio.core.format.formatPercentChange
import com.slowatcoding.finio.core.js.jsRound
import com.slowatcoding.finio.core.period.PERIOD_LABELS
import com.slowatcoding.finio.core.period.PERIOD_TYPES
import com.slowatcoding.finio.core.period.PeriodType
import com.slowatcoding.finio.core.period.normalizeMonthStartDay
import com.slowatcoding.finio.ui.charts.CardTitle
import com.slowatcoding.finio.ui.charts.ChartCard
import com.slowatcoding.finio.ui.common.collectFinanceState
import com.slowatcoding.finio.ui.common.rememberDerived
import com.slowatcoding.finio.ui.common.rememberMoneyFormatter
import com.slowatcoding.finio.ui.components.CategoryIcon
import com.slowatcoding.finio.ui.components.parseHexColor
import com.slowatcoding.finio.ui.icons.LucideIcons
import com.slowatcoding.finio.ui.theme.FinioTheme
import com.slowatcoding.finio.ui.theme.FinioType
import kotlin.math.abs

/** Column headings per period type — "last week" reads better than repeating the date range. */
private val ColumnLabels = mapOf(
    PeriodType.Weekly to listOf("This week", "Last week", "Same week last year"),
    PeriodType.Monthly to listOf("This month", "Last month", "Same month last year"),
    PeriodType.Yearly to listOf("This year", "Last year", ""),
)

private fun ratio(current: Double, previous: Double): Double? = if (previous == 0.0) null else (current - previous) / previous

/** For spending, up is bad ([invert]); for income and net, up is good. */
@Composable
private fun ChangeBadge(ratio: Double?, invert: Boolean = false) {
    val colors = FinioTheme.colors
    if (ratio == null) {
        Text("—", style = FinioType.caption, color = colors.mutedForeground)
        return
    }
    val pct = jsRound(ratio * 100).toInt()
    if (pct == 0) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            Icon(LucideIcons.Minus, null, Modifier.size(9.dp), tint = colors.mutedForeground)
            Text("flat", style = FinioType.caption, color = colors.mutedForeground)
        }
        return
    }
    val good = if (invert) pct < 0 else pct > 0
    val tone = if (good) colors.positive else colors.destructive
    val text = formatPercentChange(ratio).trimStart('+', '-')
    Row(
        Modifier.semantics(mergeDescendants = true) { contentDescription = "${if (pct > 0) "up" else "down"} $text" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Icon(if (pct > 0) LucideIcons.ArrowUp else LucideIcons.ArrowDown, null, Modifier.size(9.dp), tint = tone)
        Text(text, style = FinioType.label, color = tone)
    }
}

/**
 * Port of PeriodComparison.tsx — this week/month/year against the last one (and the same one a
 * year ago when there is history), with how the period in progress is pacing and the categories
 * that moved most.
 */
@Composable
fun PeriodComparisonCard(modifier: Modifier = Modifier) {
    val state by collectFinanceState()
    val money = rememberMoneyFormatter()
    val colors = FinioTheme.colors
    val transactions = state.transactions
    val categories = state.categories
    val monthStartDay = normalizeMonthStartDay(state.settings.monthStartDay)
    var type by rememberSaveable { mutableStateOf(PeriodType.Monthly) }

    val result = rememberDerived(transactions, type, monthStartDay) {
        val c = buildPeriodComparison(transactions, type = type, monthStartDay = monthStartDay)
        c to categoryMovements(c.current, c.previous, 5)
    } ?: return
    val (comparison, movers) = result
    if (comparison.current.transactionCount == 0 && comparison.previous.transactionCount == 0) return

    val (currentLabel, previousLabel, lastYearLabel) = ColumnLabels.getValue(type)
    val columns = buildList<Pair<String, PeriodSummary>> {
        add(currentLabel to comparison.current)
        add(previousLabel to comparison.previous)
        val ly = comparison.lastYear
        if (ly != null && ly.transactionCount > 0) add(lastYearLabel to ly)
    }
    val compact: (Double) -> String = { money(it, compact = true) }
    val categoryFor = remember(categories) { categories.associateBy { it.id } }
    val semibold = FinioType.caption.copy(fontWeight = FontWeight.SemiBold)

    ChartCard(title = null, modifier = modifier) {
        Row(
            Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CardTitle("Compare periods")
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                PERIOD_TYPES.forEach { option -> MiniToggle(PERIOD_LABELS.getValue(option), type == option) { type = option } }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            columns.forEachIndexed { index, (heading, summary) ->
                MutedTile(Modifier.weight(1f), padding = 12.dp) {
                    Column {
                        Text(heading, style = FinioType.label, color = colors.mutedForeground, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            summary.label,
                            Modifier.padding(top = 2.dp),
                            style = FinioType.caption,
                            color = colors.mutedForeground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Figure("Income", compact(summary.income), colors.positive)
                            Figure("Expenses", compact(summary.expenses), colors.destructive)
                            Figure("Net", compact(summary.net), if (summary.net >= 0) colors.positive else colors.destructive)
                        }
                        // How the period in progress compares against this one, not the reverse.
                        if (index > 0) {
                            Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Spend now:", style = FinioType.caption, color = colors.mutedForeground)
                                ChangeBadge(ratio(comparison.current.expenses, summary.expenses), invert = true)
                            }
                        }
                    }
                }
            }
        }

        if (comparison.current.isPartial) {
            Text(
                "$currentLabel is still in progress — on pace for ${compact(comparison.current.projectedExpenses)} of spending.",
                Modifier.padding(top = 8.dp),
                style = FinioType.caption,
                color = colors.mutedForeground,
            )
        }

        if (movers.isNotEmpty()) {
            Column(Modifier.padding(top = 16.dp)) {
                Text(
                    "Biggest movers vs ${previousLabel.lowercase()}",
                    Modifier.padding(bottom = 8.dp),
                    style = FinioType.label,
                    color = colors.mutedForeground,
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    movers.forEach { mover ->
                        val category = categoryFor[mover.categoryId]
                        val isUp = mover.change > 0
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            CategoryIcon(
                                category?.icon ?: "circle-ellipsis",
                                size = 14.dp,
                                tint = category?.color?.let { parseHexColor(it, colors.mutedForeground) } ?: colors.mutedForeground,
                            )
                            Text(
                                category?.name ?: "Uncategorized",
                                Modifier.weight(1f),
                                style = FinioType.label,
                                color = colors.foreground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                (if (isUp) "+" else "−") + compact(abs(mover.change)),
                                style = semibold,
                                color = if (isUp) colors.destructive else colors.positive,
                            )
                            Box(Modifier.width(48.dp), contentAlignment = Alignment.CenterEnd) {
                                ChangeBadge(mover.percentChange, invert = true)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Figure(label: String, value: String, tone: androidx.compose.ui.graphics.Color) {
    Column {
        Text(label, style = FinioType.caption, color = FinioTheme.colors.mutedForeground)
        Text(value, style = FinioType.caption.copy(fontWeight = FontWeight.SemiBold), color = tone)
    }
}
