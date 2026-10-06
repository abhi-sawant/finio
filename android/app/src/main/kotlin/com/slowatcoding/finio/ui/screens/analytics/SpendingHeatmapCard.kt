package com.slowatcoding.finio.ui.screens.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.slowatcoding.finio.core.calc.buildSpendingCalendar
import com.slowatcoding.finio.core.js.format
import com.slowatcoding.finio.core.js.nowInstant
import com.slowatcoding.finio.core.period.PeriodType
import com.slowatcoding.finio.core.period.normalizeMonthStartDay
import com.slowatcoding.finio.core.period.periodRange
import com.slowatcoding.finio.core.period.shiftPeriod
import com.slowatcoding.finio.ui.charts.CardTitle
import com.slowatcoding.finio.ui.charts.ChartCard
import com.slowatcoding.finio.ui.charts.ChartDataTable
import com.slowatcoding.finio.ui.charts.ChartTableRow
import com.slowatcoding.finio.ui.common.collectFinanceState
import com.slowatcoding.finio.ui.common.rememberDerived
import com.slowatcoding.finio.ui.common.rememberMoneyFormatter
import com.slowatcoding.finio.ui.theme.FinioTheme
import com.slowatcoding.finio.ui.theme.FinioType
import com.slowatcoding.finio.ui.theme.mix

/** Past a bit over half strength the fill is dark enough that white reads better than ink. */
private const val LIGHT_TEXT_INTENSITY = 0.55

/**
 * Port of SpendingHeatmap.tsx — daily expense totals of one financial month as a Monday-first
 * grid, each day primary mixed by its intensity; steps back through history. Tap a day for its
 * total.
 */
@Composable
fun SpendingHeatmapCard(modifier: Modifier = Modifier) {
    val state by collectFinanceState()
    val money = rememberMoneyFormatter()
    val colors = FinioTheme.colors
    val transactions = state.transactions
    val monthStartDay = normalizeMonthStartDay(state.settings.monthStartDay)
    var offset by rememberSaveable { mutableIntStateOf(0) }

    val calendar = rememberDerived(transactions, offset, monthStartDay) {
        val current = periodRange(PeriodType.Monthly, nowInstant(), monthStartDay)
        buildSpendingCalendar(transactions, if (offset == 0) current else shiftPeriod(current, offset))
    } ?: return
    val compact: (Double) -> String = { money(it, compact = true) }
    val spendingDays = remember(calendar) { calendar.weeks.flatten().filter { it.inRange && it.total > 0 } }

    ChartCard(title = null, modifier = modifier) {
        Row(
            Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CardTitle("Spending calendar")
            MonthStepper(
                label = calendar.label,
                minLabelWidth = 112.dp,
                canPrev = true,
                canNext = offset < 0,
                onPrev = { offset -= 1 },
                onNext = { offset = minOf(0, offset + 1) },
            )
        }

        WeekGrid(calendar.weeks, key = { it.key }) { day ->
            if (!day.inRange) return@WeekGrid
            val date = format(day.date, "d MMM")
            val title = date +
                (if (day.isFuture) ", upcoming" else ", ${if (day.total > 0) compact(day.total) else "nothing"} spent") +
                (if (day.transactionCount > 0) " across ${day.transactionCount} transaction${if (day.transactionCount == 1) "" else "s"}" else "") +
                (if (day.isToday) ", today" else "")
            val (bg, fg) = when {
                day.isFuture -> colors.muted.mix(0.3f) to colors.mutedForeground.mix(0.4f)
                day.total == 0.0 -> colors.muted.mix(0.5f) to colors.mutedForeground
                else -> colors.primary.mix(day.intensity.toFloat()) to
                    (if (day.intensity >= LIGHT_TEXT_INTENSITY) Color.White else colors.foreground)
            }
            DaySquare(format(day.date, "d"), bg, fg, day.isToday, title)
        }

        Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Stat(Modifier.weight(1f), "Total") { Text(compact(calendar.total), style = statValue, color = colors.foreground) }
            Stat(Modifier.weight(1f), "Avg / spend day") {
                Text(compact(calendar.averagePerActiveDay), style = statValue, color = colors.foreground)
            }
            Stat(Modifier.weight(1f), "Busiest") {
                val busiest = calendar.busiest
                Text(
                    if (busiest == null) {
                        buildAnnotatedString { append("—") }
                    } else {
                        buildAnnotatedString {
                            append(format(busiest.date, "d MMM"))
                            append(" ")
                            withStyle(SpanStyle(color = colors.mutedForeground, fontWeight = FontWeight.Normal)) { append(compact(busiest.total)) }
                        }
                    },
                    style = statValue,
                    color = colors.foreground,
                    textAlign = TextAlign.Center,
                )
            }
        }

        if (calendar.daysWithSpend == 0) {
            Text(
                "No spending recorded in this period.",
                Modifier.fillMaxWidth().padding(top = 12.dp),
                style = FinioType.caption,
                color = colors.mutedForeground,
                textAlign = TextAlign.Center,
            )
        }

        ChartDataTable(
            caption = "Days with spending in this period",
            columns = listOf("Day", "Spent", "Transactions"),
            rows = spendingDays.map { ChartTableRow(it.key, listOf(format(it.date, "d MMM"), compact(it.total), it.transactionCount.toString())) },
        )
    }
}

private val statValue = FinioType.caption.copy(fontWeight = FontWeight.SemiBold)

@Composable
private fun Stat(modifier: Modifier, label: String, value: @Composable () -> Unit) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = FinioType.label, color = FinioTheme.colors.mutedForeground, textAlign = TextAlign.Center)
        value()
    }
}
