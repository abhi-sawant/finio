package com.slowatcoding.finio.ui.charts

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.slowatcoding.finio.core.calc.getNetWorth
import com.slowatcoding.finio.core.format.localDayKey
import com.slowatcoding.finio.core.js.format
import com.slowatcoding.finio.core.js.nowInstant
import com.slowatcoding.finio.core.js.parseIso
import com.slowatcoding.finio.core.js.subDays
import com.slowatcoding.finio.core.model.TransactionType
import com.slowatcoding.finio.core.period.PeriodType
import com.slowatcoding.finio.core.period.addPeriods
import com.slowatcoding.finio.core.period.monthPeriodStart
import com.slowatcoding.finio.core.period.normalizeMonthStartDay
import com.slowatcoding.finio.core.util.sampleForTable
import com.slowatcoding.finio.ui.common.collectFinanceState
import com.slowatcoding.finio.ui.common.rememberDerived
import com.slowatcoding.finio.ui.common.rememberMoneyFormatter
import com.slowatcoding.finio.ui.theme.FinioTheme
import java.time.Instant

@Immutable
private data class BalancePoint(val dateKey: String, val date: String, val balance: Double)

/** date-fns `differenceInDays`: whole days between two instants, truncated toward zero. */
internal fun differenceInDays(later: Instant, earlier: Instant): Int =
    ((later.toEpochMilli() - earlier.toEpochMilli()) / 86_400_000L).toInt()

/**
 * Port of BalanceTrend.tsx — net worth walked back day by day from today's balances; daily points
 * up to 90 days, otherwise sampled at the start of each financial month (plus the `to` endpoint).
 * A 3dp monotone primary line in a 176dp chart, tap/drag for the balance on a day.
 */
@Composable
fun BalanceTrend(from: Instant, to: Instant, modifier: Modifier = Modifier) {
    val state by collectFinanceState()
    val money = rememberMoneyFormatter()
    val colors = FinioTheme.colors
    val transactions = state.transactions
    val accounts = state.accounts
    val monthStartDay = normalizeMonthStartDay(state.settings.monthStartDay)

    val data = rememberDerived(transactions, accounts, from, to, monthStartDay) {
        buildBalanceTrend(getNetWorth(accounts), from, to, monthStartDay, transactionDeltas(transactions))
    } ?: return

    if (accounts.isEmpty()) return
    val numDays = differenceInDays(to, from)
    val xTicks = when {
        numDays <= 31 -> XTicks.Every(4)
        numDays <= 60 -> XTicks.Every(9)
        else -> XTicks.PreserveStartEnd()
    }
    val compact: (Double) -> String = { money(it, compact = true) }
    val first = data.firstOrNull()
    val last = data.lastOrNull()
    val table = sampleForTable(data)
    val line = colors.primary

    ChartCard("Balance trend", modifier) {
        val chartModifier = Modifier
            .fillMaxWidth()
            .height(176.dp)
            .semantics {
                contentDescription = if (first != null && last != null) {
                    "Balance from ${first.date} to ${last.date}, ${compact(first.balance)} to ${compact(last.balance)}."
                } else {
                    "Balance over time."
                }
            }
        if (data.size < 2) {
            EmptyChart(chartModifier, "Not enough history in this range to draw a trend.")
        } else {
            CartesianChart(
                count = data.size,
                xLabels = data.map { it.date },
                domainValues = data.map { it.balance },
                scale = XScale.Point,
                xTicks = xTicks,
                hidden = money.hidden,
                cursor = ChartCursor.Line,
                tooltip = { i -> TooltipData(data[i].date, listOf(TooltipRow("Balance", money(data[i].balance)))) },
                activeDots = { i -> listOf(data[i].balance to line) },
                modifier = chartModifier,
            ) { g ->
                val path = monotonePath(data.mapIndexed { i, p -> Offset(g.x(i), g.y(p.balance)) })
                drawPath(path, line, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
        ChartDataTable(
            caption = "Balance over time",
            columns = listOf("Date", "Balance"),
            note = if (table.sampled) "sampled to ${table.rows.size} of ${data.size} points" else null,
            rows = table.rows.map { ChartTableRow(it.dateKey, listOf(it.date, compact(it.balance))) },
        )
    }
}

private fun transactionDeltas(transactions: List<com.slowatcoding.finio.core.model.Transaction>): Map<String, Double> {
    val dayDelta = HashMap<String, Double>()
    for (t in transactions) {
        if (t.type == TransactionType.Transfer) continue
        if (parseIso(t.date) == null) continue
        val key = localDayKey(t.date)
        val delta = if (t.type == TransactionType.Income) t.amount else -t.amount
        dayDelta[key] = (dayDelta[key] ?: 0.0) + delta
    }
    return dayDelta
}

private fun buildBalanceTrend(
    currentBalance: Double,
    from: Instant,
    to: Instant,
    monthStartDay: Int,
    dayDelta: Map<String, Double>,
): List<BalancePoint> {
    val today = nowInstant()
    val numDays = differenceInDays(to, from)
    val daysFromToday = differenceInDays(today, from)

    if (numDays > 90) {
        var balance = currentBalance
        val allDaily = ArrayDeque<Pair<String, Double>>()
        for (i in 0..daysFromToday) {
            val dayKey = format(subDays(today, i), "yyyy-MM-dd")
            allDaily.addFirst(dayKey to balance)
            balance -= dayDelta[dayKey] ?: 0.0
        }
        val daily = allDaily.toList()
        val monthly = mutableListOf<BalancePoint>()
        val fromKey = format(from, "yyyy-MM-dd")
        var cursor = monthPeriodStart(from, monthStartDay)
        while (!cursor.isAfter(to)) {
            val cursorKey = format(cursor, "yyyy-MM-dd")
            val key = if (cursorKey < fromKey) fromKey else cursorKey
            daily.firstOrNull { it.first >= key }?.let { (dk, b) -> monthly += BalancePoint(dk, format(cursor, "MMM yy"), b) }
            cursor = addPeriods(PeriodType.Monthly, cursor, 1)
        }
        val lastKey = format(to, "yyyy-MM-dd")
        daily.lastOrNull { it.first <= lastKey }?.let { (dk, b) ->
            val endLabel = format(to, "MMM yy")
            val entry = BalancePoint(dk, endLabel, b)
            if (monthly.lastOrNull()?.date == endLabel) monthly[monthly.size - 1] = entry else monthly += entry
        }
        return monthly
    }

    var balance = currentBalance
    val points = ArrayDeque<BalancePoint>()
    for (i in 0..daysFromToday) {
        val day = subDays(today, i)
        val dayKey = format(day, "yyyy-MM-dd")
        points.addFirst(BalancePoint(dayKey, format(day, "d MMM"), balance))
        balance -= dayDelta[dayKey] ?: 0.0
    }
    val fromKey = format(from, "yyyy-MM-dd")
    val toKey = format(to, "yyyy-MM-dd")
    return points.filter { it.dateKey in fromKey..toKey }
}
