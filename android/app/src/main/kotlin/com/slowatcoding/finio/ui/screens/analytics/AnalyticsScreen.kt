package com.slowatcoding.finio.ui.screens.analytics

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slowatcoding.finio.core.calc.getTotalExpenses
import com.slowatcoding.finio.core.calc.getTotalIncome
import com.slowatcoding.finio.core.format.shouldCompactGroup
import com.slowatcoding.finio.core.js.endOfDay
import com.slowatcoding.finio.core.js.localDate
import com.slowatcoding.finio.core.js.nowInstant
import com.slowatcoding.finio.core.js.parseIso
import com.slowatcoding.finio.core.js.startOfDay
import com.slowatcoding.finio.core.js.subMonths
import com.slowatcoding.finio.core.model.Transaction
import com.slowatcoding.finio.core.period.monthPeriodStart
import com.slowatcoding.finio.core.period.normalizeMonthStartDay
import com.slowatcoding.finio.core.period.yearPeriodStart
import com.slowatcoding.finio.core.store.isRulePaused
import com.slowatcoding.finio.ui.charts.BalanceTrend
import com.slowatcoding.finio.ui.charts.IncomeExpenseBar
import com.slowatcoding.finio.ui.charts.LabelSpendingBar
import com.slowatcoding.finio.ui.charts.SpendingDonut
import com.slowatcoding.finio.ui.common.HideAmountsToggle
import com.slowatcoding.finio.ui.common.SectionLoader
import com.slowatcoding.finio.ui.common.collectFinanceState
import com.slowatcoding.finio.ui.common.rememberDerived
import com.slowatcoding.finio.ui.common.rememberMoneyFormatter
import com.slowatcoding.finio.ui.components.ButtonSize
import com.slowatcoding.finio.ui.components.ButtonVariant
import com.slowatcoding.finio.ui.components.FinioButton
import com.slowatcoding.finio.ui.components.FinioCard
import com.slowatcoding.finio.ui.components.FinioDivider
import com.slowatcoding.finio.ui.components.FinioPopover
import com.slowatcoding.finio.ui.components.FinioScreen
import com.slowatcoding.finio.ui.components.PageTitle
import com.slowatcoding.finio.ui.components.formatShortDate
import com.slowatcoding.finio.ui.icons.LucideIcons
import com.slowatcoding.finio.ui.navigation.FinioNavigator
import com.slowatcoding.finio.ui.navigation.Routes
import com.slowatcoding.finio.ui.theme.FinioTheme
import com.slowatcoding.finio.ui.theme.FinioType
import java.time.Instant
import java.time.LocalDate

private enum class Filter { All, Month, ThreeMonths, SixMonths, Year, Custom }

private val FilterChips = listOf(
    Filter.All to "All",
    Filter.Month to "This month",
    Filter.ThreeMonths to "Last 3 months",
    Filter.SixMonths to "Last 6 months",
    Filter.Year to "This year",
)

@Immutable
private class AnalyticsView(
    val from: Instant,
    val to: Instant,
    val filtered: List<Transaction>,
    val income: Double,
    val expenses: Double,
)

private fun LocalDate.toInstant(): Instant = localDate(year, monthValue - 1, dayOfMonth)

/**
 * Port of web/src/pages/Analytics.tsx — route `/analytics` (tab). Period chips and a custom range
 * calendar filter the summary and the first group of charts; insights and the forecast, calendar,
 * net-worth, comparison and heatmap cards carry their own windows. Links to Budgets and Recurring.
 */
@Composable
fun AnalyticsScreen(nav: FinioNavigator) {
    val state by collectFinanceState()
    val money = rememberMoneyFormatter()
    val colors = FinioTheme.colors
    val transactions = state.transactions
    val monthStartDay = normalizeMonthStartDay(state.settings.monthStartDay)

    var filter by rememberSaveable { mutableStateOf(Filter.Month) }
    var fromDay by rememberSaveable { mutableStateOf<Long?>(null) }
    var toDay by rememberSaveable { mutableStateOf<Long?>(null) }
    val range = fromDay?.let { DayRange(LocalDate.ofEpochDay(it), toDay?.let(LocalDate::ofEpochDay)) }

    val view = rememberDerived(filter, range, transactions, monthStartDay) {
        val now = nowInstant()
        fun monthStart(d: Instant) = monthPeriodStart(d, monthStartDay)
        val (from, to) = when {
            filter == Filter.Month -> monthStart(now) to now
            filter == Filter.ThreeMonths -> monthStart(subMonths(now, 2)) to now
            filter == Filter.SixMonths -> monthStart(subMonths(now, 5)) to now
            filter == Filter.Year -> yearPeriodStart(now, monthStartDay) to now
            filter == Filter.Custom && range != null ->
                startOfDay(range.from.toInstant()) to endOfDay((range.to ?: range.from).toInstant())
            else -> {
                val earliest = transactions.minOfOrNull { it.date }?.let(::parseIso)
                (earliest?.let(::startOfDay) ?: monthStart(now)) to now
            }
        }
        val filtered = if (filter == Filter.All || (filter == Filter.Custom && range == null)) {
            transactions
        } else {
            transactions.filter { t -> parseIso(t.date)?.let { !it.isBefore(from) && !it.isAfter(to) } ?: false }
        }
        AnalyticsView(from, to, filtered, getTotalIncome(filtered), getTotalExpenses(filtered))
    }
    val activeRecurring = remember(state.recurring) { state.recurring.count { !isRulePaused(it) } }

    fun changeFilter(f: Filter) {
        filter = f
        if (f != Filter.Custom) {
            fromDay = null
            toDay = null
        }
    }

    FinioScreen(header = {
        PageTitle("Analytics")
        HideAmountsToggle()
    }) {
        if (transactions.isEmpty()) {
            Text(
                "Add transactions to see analytics",
                Modifier.fillMaxWidth().padding(vertical = 48.dp),
                style = FinioType.body,
                color = colors.mutedForeground,
                textAlign = TextAlign.Center,
            )
        } else {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilterChips.forEach { (value, label) ->
                    FinioButton(
                        label,
                        onClick = { changeFilter(value) },
                        size = ButtonSize.Sm,
                        variant = if (filter == value) ButtonVariant.Default else ButtonVariant.Outline,
                    )
                }
                CustomRangeButton(
                    range = range,
                    active = filter == Filter.Custom,
                    onSelect = { r ->
                        fromDay = r.from.toEpochDay()
                        toDay = r.to?.toEpochDay()
                        filter = Filter.Custom
                    },
                )
            }

            if (view == null) {
                SectionLoader()
            } else {
                val net = view.income - view.expenses
                val summaryCompact = shouldCompactGroup(listOf(view.income, view.expenses, net))
                SurfaceCard {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SummaryFigure(Modifier.weight(1f), "Income", money(view.income, compact = true, forceCompact = summaryCompact), colors.positive)
                        SummaryFigure(Modifier.weight(1f), "Expenses", money(view.expenses, compact = true, forceCompact = summaryCompact), colors.foreground)
                        SummaryFigure(
                            Modifier.weight(1f),
                            "Net",
                            money(net, compact = true, forceCompact = summaryCompact),
                            if (net >= 0) colors.positive else colors.destructive,
                        )
                    }
                }

                // Insights — always about the current month, so it sits outside the filter.
                InsightsFeed(nav)

                if (view.filtered.isEmpty()) {
                    FinioCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 40.dp)) {
                        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(LucideIcons.CalendarX, null, Modifier.padding(bottom = 12.dp).size(28.dp), tint = colors.mutedForeground)
                            Text("Nothing in this period", style = FinioType.bodyMedium, color = colors.foreground)
                            Text(
                                "No transactions fall in the selected range — try a wider one.",
                                Modifier.padding(top = 4.dp),
                                style = FinioType.caption,
                                color = colors.mutedForeground,
                                textAlign = TextAlign.Center,
                            )
                            Box(Modifier.padding(top = 16.dp)) {
                                FinioButton(
                                    onClick = { changeFilter(Filter.All) },
                                    variant = ButtonVariant.Outline,
                                    contentPadding = PaddingValues(horizontal = 20.dp),
                                ) { Text("Show all time") }
                            }
                        }
                    }
                } else {
                    SpendingDonut(view.filtered)
                    IncomeExpenseBar(view.filtered)
                    BalanceTrend(view.from, view.to)
                    LabelSpendingBar(view.filtered)
                    TopMerchants(view.filtered, nav)
                }

                // These carry their own window, anchored to "now" rather than to the chips above.
                CashFlowForecastCard()
                CashFlowCalendarCard()
                NetWorthTrendCard()
                PeriodComparisonCard()
                SpendingHeatmapCard()
            }
        }

        FinioCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
            ToolLink(
                LucideIcons.Target,
                "Budgets",
                if (state.budgets.isEmpty()) "Set monthly limits" else "${state.budgets.size} active",
            ) { nav.navigate(Routes.Budgets) }
            FinioDivider()
            ToolLink(
                LucideIcons.Repeat,
                "Recurring transactions",
                if (activeRecurring == 0) "Automate repeating items" else "$activeRecurring active",
            ) { nav.navigate(Routes.Recurring) }
        }
    }
}

@Composable
private fun SummaryFigure(modifier: Modifier, label: String, value: String, tone: Color) {
    Column(modifier) {
        Text(label, style = FinioType.label, color = FinioTheme.colors.mutedForeground)
        Text(value, style = FinioType.money.copy(fontSize = 16.sp, lineHeight = 24.sp), color = tone, maxLines = 1)
    }
}

@Composable
private fun CustomRangeButton(range: DayRange?, active: Boolean, onSelect: (DayRange) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val label = when {
        range == null -> "Pick a date"
        range.to != null -> "${formatShortDate(range.from)} - ${formatShortDate(range.to)}"
        else -> formatShortDate(range.from)
    }
    Box {
        FinioButton(
            onClick = { open = true },
            variant = if (active) ButtonVariant.Default else ButtonVariant.Outline,
            contentPadding = PaddingValues(horizontal = 10.dp),
        ) {
            Icon(LucideIcons.Calendar, null, Modifier.size(16.dp))
            Text(label, style = FinioType.body, maxLines = 1)
        }
        if (open) {
            FinioPopover(onDismissRequest = { open = false }, contentPadding = PaddingValues(0.dp)) {
                RangeCalendar(selected = range, onSelect = onSelect)
            }
        }
    }
}

@Composable
private fun ToolLink(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    val colors = FinioTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, null, Modifier.size(18.dp), tint = colors.mutedForeground)
            Column {
                Text(title, style = FinioType.bodyMedium, color = colors.foreground)
                Text(subtitle, style = FinioType.caption, color = colors.mutedForeground)
            }
        }
        Icon(LucideIcons.ChevronRight, null, Modifier.size(16.dp), tint = colors.mutedForeground)
    }
}
