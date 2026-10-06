package com.slowatcoding.finio.ui.charts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.slowatcoding.finio.core.js.format
import com.slowatcoding.finio.core.js.iso
import com.slowatcoding.finio.core.model.Transaction
import com.slowatcoding.finio.core.model.TransactionType
import com.slowatcoding.finio.core.period.monthPeriodStart
import com.slowatcoding.finio.core.period.normalizeMonthStartDay
import com.slowatcoding.finio.ui.common.collectFinanceState
import com.slowatcoding.finio.ui.common.rememberDerived
import com.slowatcoding.finio.ui.common.rememberMoneyFormatter
import com.slowatcoding.finio.ui.theme.FinioShapes
import com.slowatcoding.finio.ui.theme.FinioTheme
import com.slowatcoding.finio.ui.theme.FinioType

@Immutable
private data class MonthBar(val key: String, val month: String, val income: Double, val expenses: Double)

/**
 * Port of IncomeExpenseBar.tsx — income (green) beside expenses (magenta) per financial month,
 * grouped bars with 6dp top corners in a 192dp chart, a legend and the data table.
 */
@Composable
fun IncomeExpenseBar(transactions: List<Transaction>, modifier: Modifier = Modifier) {
    val state by collectFinanceState()
    val money = rememberMoneyFormatter()
    val colors = FinioTheme.colors
    val monthStartDay = normalizeMonthStartDay(state.settings.monthStartDay)

    val data = rememberDerived(transactions, monthStartDay) {
        val map = LinkedHashMap<String, DoubleArray>()
        for (t in transactions) {
            if (t.type != TransactionType.Income && t.type != TransactionType.Expense) continue
            val start = monthPeriodStart(iso(t.date), monthStartDay)
            val key = format(start, "yyyy-MM-dd")
            val entry = map.getOrPut(key) { DoubleArray(2) }
            if (t.type == TransactionType.Income) entry[0] += t.amount else entry[1] += t.amount
        }
        map.entries.sortedBy { it.key }.map { (key, v) ->
            MonthBar(key, format(iso(key), "MMM yy"), v[0], v[1])
        }
    } ?: return

    if (data.none { it.income > 0 || it.expenses > 0 }) return
    val compact: (Double) -> String = { money(it, compact = true) }
    val totalIncome = data.sumOf { it.income }
    val totalExpenses = data.sumOf { it.expenses }
    val income = colors.positive
    val expense = colors.destructive

    ChartCard("Income vs expenses", modifier) {
        CartesianChart(
            count = data.size,
            xLabels = data.map { it.month },
            domainValues = data.flatMap { listOf(it.income, it.expenses) },
            scale = XScale.Band,
            xTicks = XTicks.PreserveEnd(),
            hidden = money.hidden,
            cursor = ChartCursor.Band,
            tooltip = { i ->
                TooltipData(
                    label = data[i].month,
                    rows = listOf(TooltipRow("Income", money(data[i].income)), TooltipRow("Expenses", money(data[i].expenses))),
                    separator = ": ",
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(192.dp)
                .semantics {
                    contentDescription = "Income against expenses across ${data.size} month${if (data.size == 1) "" else "s"}: " +
                        "${compact(totalIncome)} earned, ${compact(totalExpenses)} spent in total."
                },
        ) { g ->
            val slots = barSlots(g, 2)
            val zero = g.y(0.0)
            val r = 6.dp.toPx()
            data.forEachIndexed { i, d ->
                val left = g.x(i) - g.step / 2f
                drawBar(income, left + slots[0].first, slots[0].second, g.y(d.income), zero, r)
                drawBar(expense, left + slots[1].first, slots[1].second, g.y(d.expenses), zero, r)
            }
        }
        ChartLegend(listOf("Income" to income, "Expenses" to expense), Modifier.padding(top = 8.dp))
        ChartDataTable(
            caption = "Income and expenses by month",
            columns = listOf("Month", "Income", "Expenses"),
            rows = data.map { ChartTableRow(it.key, listOf(it.month, compact(it.income), compact(it.expenses))) },
        )
    }
}

/** A centred row of `dot + muted label` legend entries, 16dp apart. */
@Composable
fun ChartLegend(entries: List<Pair<String, Color>>, modifier: Modifier = Modifier) {
    val colors = FinioTheme.colors
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)) {
        entries.forEach { (label, color) ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(10.dp).clip(FinioShapes.full).background(color))
                Text(label, style = FinioType.caption, color = colors.mutedForeground)
            }
        }
    }
}
