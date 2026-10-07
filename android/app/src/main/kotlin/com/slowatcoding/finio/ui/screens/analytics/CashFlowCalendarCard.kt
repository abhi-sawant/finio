package com.slowatcoding.finio.ui.screens.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.slowatcoding.finio.core.calc.ForecastInput
import com.slowatcoding.finio.core.calc.buildCashFlowCalendarMonth
import com.slowatcoding.finio.core.calc.buildCashFlowForecast
import com.slowatcoding.finio.core.calc.getCreditCardDueInfo
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
import com.slowatcoding.finio.ui.icons.LucideIcons
import com.slowatcoding.finio.ui.theme.FinioTheme
import com.slowatcoding.finio.ui.theme.FinioType
import com.slowatcoding.finio.ui.theme.mix

/** How far forward month navigation may go — comfortably inside the forecast horizon. */
private const val MAX_MONTHS_AHEAD = 2

/** A little past MAX_MONTHS_AHEAD of calendar months, so the last visible month is never cut off. */
private const val FORECAST_DAYS = 100

/**
 * Port of CashFlowCalendar.tsx — the scheduled cash flow of one financial month (this one and up
 * to two ahead) as a Monday-first grid: green for money in, magenta for money out, a card glyph on
 * credit-card due dates. Tap a day for its details.
 */
@Composable
fun CashFlowCalendarCard(modifier: Modifier = Modifier) {
    val state by collectFinanceState()
    val money = rememberMoneyFormatter()
    val colors = FinioTheme.colors
    val accounts = state.accounts
    val transactions = state.transactions
    val recurring = state.recurring
    val monthStartDay = normalizeMonthStartDay(state.settings.monthStartDay)
    var offset by rememberSaveable { mutableIntStateOf(0) }

    val forecast = rememberDerived(accounts, transactions, recurring) {
        buildCashFlowForecast(ForecastInput(accounts = accounts, transactions = transactions, recurring = recurring, days = FORECAST_DAYS))
    } ?: return
    if (forecast.isEmpty) return

    val monthRange = remember(offset, monthStartDay) {
        val current = periodRange(PeriodType.Monthly, nowInstant(), monthStartDay)
        if (offset == 0) current else shiftPeriod(current, offset)
    }
    val calendar = remember(forecast, monthRange) { buildCashFlowCalendarMonth(forecast.scheduled, monthRange) }
    val dueDatesByDay = remember(accounts) {
        val map = LinkedHashMap<String, List<String>>()
        for (account in accounts) {
            val due = getCreditCardDueInfo(account) ?: continue
            val key = format(due.dueDate, "yyyy-MM-dd")
            map[key] = (map[key] ?: emptyList()) + account.name
        }
        map
    }
    val compact: (Double) -> String = { money(it, compact = true) }
    val daysWithFlows = remember(calendar) { calendar.weeks.flatten().filter { it.inRange && it.flows.isNotEmpty() } }

    ChartCard(title = null, modifier = modifier) {
        Row(
            Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CardTitle("Cash-flow calendar")
            MonthStepper(
                label = format(monthRange.start, "MMM yyyy"),
                minLabelWidth = 80.dp,
                canPrev = offset > 0,
                canNext = offset < MAX_MONTHS_AHEAD,
                onPrev = { offset = maxOf(0, offset - 1) },
                onNext = { offset = minOf(MAX_MONTHS_AHEAD, offset + 1) },
            )
        }

        WeekGrid(calendar.weeks, key = { it.key }) { day ->
            if (!day.inRange) return@WeekGrid
            val due = dueDatesByDay[day.key]
            val title = listOfNotNull(
                format(day.date, "d MMM"),
                if (day.netFlow != 0.0) {
                    "${if (day.netFlow > 0) "+" else ""}${compact(day.netFlow)} (${day.flows.joinToString(", ") { it.note.ifEmpty { "Untitled" } }})"
                } else {
                    "nothing scheduled"
                },
                due?.let { "${it.joinToString(", ")} payment due" },
            ).joinToString(" — ") + if (day.isToday) ", today" else ""
            val (bg, fg) = when {
                day.netFlow == 0.0 -> colors.muted.mix(0.5f) to colors.mutedForeground
                day.netFlow > 0 -> colors.positive.mix(0.1f) to colors.positive
                else -> colors.destructive.mix(0.1f) to colors.destructive
            }
            DaySquare(
                text = format(day.date, "d"),
                background = bg,
                textColor = fg,
                isToday = day.isToday,
                title = title,
                badge = if (due != null) {
                    {
                        Icon(
                            LucideIcons.CreditCard,
                            null,
                            Modifier.align(Alignment.BottomEnd).padding(2.dp).size(9.dp),
                            tint = colors.mutedForeground,
                        )
                    }
                } else {
                    null
                },
            )
        }

        Row(
            Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LegendSwatch(colors.positive, "Money in")
            LegendSwatch(colors.destructive, "Money out")
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(LucideIcons.CreditCard, null, Modifier.size(10.dp), tint = colors.mutedForeground)
                Text("Card due", style = FinioType.caption, color = colors.mutedForeground)
            }
        }

        if (daysWithFlows.isEmpty()) {
            Text(
                "Nothing scheduled to land this month.",
                Modifier.fillMaxWidth().padding(top = 12.dp),
                style = FinioType.caption,
                color = colors.mutedForeground,
                textAlign = TextAlign.Center,
            )
        }

        ChartDataTable(
            caption = "Scheduled cash flow this month",
            columns = listOf("Day", "Net", "Details"),
            rows = daysWithFlows.map { day ->
                ChartTableRow(
                    day.key,
                    listOf(format(day.date, "d MMM"), compact(day.netFlow), day.flows.joinToString(", ") { it.note.ifEmpty { "Untitled" } }),
                )
            },
        )
    }
}

@Composable
private fun LegendSwatch(color: Color, label: String) {
    val shape = RoundedCornerShape(3.dp)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(10.dp).background(color.mix(0.15f), shape).border(1.dp, color.mix(0.5f), shape))
        Text(label, style = FinioType.caption, color = FinioTheme.colors.mutedForeground)
    }
}
