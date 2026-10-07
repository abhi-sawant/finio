package com.slowatcoding.finio.ui.screens.yearinreview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slowatcoding.finio.core.calc.YearInReviewInput
import com.slowatcoding.finio.core.calc.buildYearInReview
import com.slowatcoding.finio.core.format.formatPercentChange
import com.slowatcoding.finio.core.format.formatShortDate
import com.slowatcoding.finio.core.format.shouldCompactGroup
import com.slowatcoding.finio.core.js.jsRound
import com.slowatcoding.finio.core.js.nowInstant
import com.slowatcoding.finio.core.js.parseJsDate
import com.slowatcoding.finio.core.period.PeriodType
import com.slowatcoding.finio.core.period.normalizeMonthStartDay
import com.slowatcoding.finio.core.period.periodRange
import com.slowatcoding.finio.core.period.shiftPeriod
import com.slowatcoding.finio.ui.charts.CardTitle
import com.slowatcoding.finio.ui.charts.ChartCard
import com.slowatcoding.finio.ui.charts.ChartDataTable
import com.slowatcoding.finio.ui.charts.ChartTableRow
import com.slowatcoding.finio.ui.common.BackButton
import com.slowatcoding.finio.ui.common.HideAmountsToggle
import com.slowatcoding.finio.ui.common.SectionLoader
import com.slowatcoding.finio.ui.common.collectFinanceState
import com.slowatcoding.finio.ui.common.rememberDerived
import com.slowatcoding.finio.ui.common.rememberMoneyFormatter
import com.slowatcoding.finio.ui.components.FinioDivider
import com.slowatcoding.finio.ui.components.FinioScreen
import com.slowatcoding.finio.ui.components.ScreenTitle
import com.slowatcoding.finio.ui.icons.LucideIcons
import com.slowatcoding.finio.ui.navigation.FinioNavigator
import com.slowatcoding.finio.ui.screens.analytics.MonthStepper
import com.slowatcoding.finio.ui.screens.analytics.SurfaceCard
import com.slowatcoding.finio.ui.theme.FinioRadius
import com.slowatcoding.finio.ui.theme.FinioTheme
import com.slowatcoding.finio.ui.theme.FinioType
import com.slowatcoding.finio.ui.theme.mix
import kotlin.math.abs
import kotlin.math.max

private fun ratio(current: Double, previous: Double): Double? = if (previous == 0.0) null else (current - previous) / previous

/** For spending, up is bad ([invert]); for income and net, up is good. */
@Composable
private fun ChangeBadge(value: Double?, modifier: Modifier = Modifier, invert: Boolean = false) {
    val colors = FinioTheme.colors
    Box(modifier, contentAlignment = Alignment.Center) {
        when {
            value == null -> Text("new this year", style = FinioType.caption, color = colors.mutedForeground, textAlign = TextAlign.Center)
            jsRound(value * 100) == 0.0 -> Text("flat vs last year", style = FinioType.caption, color = colors.mutedForeground, textAlign = TextAlign.Center)
            else -> {
                val good = if (invert) value < 0 else value > 0
                val tone = if (good) colors.positive else colors.destructive
                val text = "${formatPercentChange(value).trimStart('+', '-')} vs last year"
                Row(
                    Modifier.semantics(mergeDescendants = true) { contentDescription = "${if (value > 0) "up" else "down"} $text" },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Icon(if (value > 0) LucideIcons.ArrowUp else LucideIcons.ArrowDown, null, Modifier.size(11.dp), tint = tone)
                    Text(text, style = FinioType.label, color = tone)
                }
            }
        }
    }
}

/**
 * Port of web/src/pages/YearInReview.tsx — route `/year-in-review`. One financial year at a
 * time (back to the year holding the oldest transaction): income/expenses/net against last year,
 * net worth across the year, spending by month, top categories, movers and the biggest expense.
 */
@Composable
fun YearInReviewScreen(nav: FinioNavigator) {
    val state by collectFinanceState()
    val money = rememberMoneyFormatter()
    val colors = FinioTheme.colors
    val transactions = state.transactions
    val accounts = state.accounts
    val categories = state.categories
    val monthStartDay = normalizeMonthStartDay(state.settings.monthStartDay)
    var yearOffset by rememberSaveable { mutableIntStateOf(0) }

    val review = rememberDerived(transactions, accounts, monthStartDay, yearOffset) {
        buildYearInReview(YearInReviewInput(transactions = transactions, accounts = accounts, monthStartDay = monthStartDay, yearOffset = yearOffset))
    }
    // How far back the year navigation may go: the financial year holding the oldest transaction.
    val minYearOffset = rememberDerived(transactions, monthStartDay) {
        val earliest = transactions.minOfOrNull { it.date }?.let(::parseJsDate)
        if (earliest == null) {
            0
        } else {
            var range = periodRange(PeriodType.Yearly, nowInstant(), monthStartDay)
            var offset = 0
            // Bounded — a corrupt far-past date shouldn't spin this loop.
            while (range.start.isAfter(earliest) && offset > -100) {
                range = shiftPeriod(range, -1)
                offset -= 1
            }
            offset
        }
    } ?: 0
    val categoryFor = remember(categories) { categories.associateBy { it.id } }

    FinioScreen(header = {
        BackButton(nav)
        ScreenTitle("Year in review")
        HideAmountsToggle()
    }) {
        if (review == null) {
            SectionLoader()
            return@FinioScreen
        }
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            MonthStepper(
                label = review.label,
                minLabelWidth = 80.dp,
                canPrev = yearOffset > minYearOffset,
                canNext = yearOffset < 0,
                onPrev = { yearOffset = max(minYearOffset, yearOffset - 1) },
                onNext = { yearOffset = minOf(0, yearOffset + 1) },
                buttonSize = 32.dp,
                iconSize = 16.dp,
                labelStyle = FinioType.title.copy(fontSize = 18.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold),
                prevDescription = "Previous year",
                nextDescription = "Next year",
            )
        }

        val isEmpty = review.current.transactionCount == 0 && review.previous.transactionCount == 0
        if (isEmpty) {
            Text(
                if (transactions.isNotEmpty()) "No activity in ${review.label}" else "Add some transactions to see a year in review.",
                Modifier.fillMaxWidth().padding(vertical = 48.dp),
                style = FinioType.body,
                color = colors.mutedForeground,
                textAlign = TextAlign.Center,
            )
            return@FinioScreen
        }

        val compact: (Double) -> String = { money(it, compact = true) }
        val cur = review.current
        val heroCompact = shouldCompactGroup(listOf(cur.income, cur.expenses, cur.net))
        val heroStyle = FinioType.money.copy(fontSize = 16.sp, lineHeight = 24.sp)

        SurfaceCard {
            Column {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    HeroFigure(Modifier.weight(1f), "Income", money(cur.income, compact = true, forceCompact = heroCompact), colors.positive, heroStyle)
                    HeroFigure(Modifier.weight(1f), "Expenses", money(cur.expenses, compact = true, forceCompact = heroCompact), colors.foreground, heroStyle)
                    HeroFigure(
                        Modifier.weight(1f),
                        "Net",
                        money(cur.net, compact = true, forceCompact = heroCompact),
                        if (cur.net >= 0) colors.positive else colors.destructive,
                        heroStyle,
                    )
                }
                FinioDivider(Modifier.padding(top = 12.dp))
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ChangeBadge(ratio(cur.income, review.previous.income), Modifier.weight(1f))
                    ChangeBadge(ratio(cur.expenses, review.previous.expenses), Modifier.weight(1f), invert = true)
                    ChangeBadge(ratio(cur.net, review.previous.net), Modifier.weight(1f))
                }
            }
        }

        ChartCard("Net worth") {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Start of year", style = FinioType.label, color = colors.mutedForeground)
                    Text(compact(review.netWorthStart), style = FinioType.rowValue, color = colors.foreground)
                }
                Icon(LucideIcons.ArrowRight, null, Modifier.size(16.dp), tint = colors.mutedForeground)
                Column(horizontalAlignment = Alignment.End) {
                    Text(if (yearOffset == 0) "Now" else "End of year", style = FinioType.label, color = colors.mutedForeground)
                    Text(compact(review.netWorthEnd), style = FinioType.rowValue, color = colors.foreground)
                }
            }
            Text(
                (if (review.netWorthChange >= 0) "+" else "") + compact(review.netWorthChange) + " this year",
                Modifier.fillMaxWidth().padding(top = 8.dp),
                style = FinioType.label,
                color = if (review.netWorthChange >= 0) colors.positive else colors.destructive,
                textAlign = TextAlign.Center,
            )
        }

        ChartCard("Spending by month") {
            val busiest = review.busiestMonth
            val busiestExpenses = busiest?.expenses ?: 0.0
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .semantics {
                        contentDescription = "Spending by month in ${review.label}" +
                            (busiest?.let { ", highest in ${it.label} at ${compact(it.expenses)}" } ?: "") +
                            ". The figures are in the data table below."
                    },
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                review.monthlyBreakdown.forEach { month ->
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        val h = if (busiestExpenses > 0) max(4.0, month.expenses / busiestExpenses * 72) else 4.0
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(h.dp)
                                .background(
                                    if (month.key == busiest?.key) colors.primary else colors.primary.mix(0.25f),
                                    RoundedCornerShape(topStart = FinioRadius.sm, topEnd = FinioRadius.sm),
                                ),
                        )
                        Text(month.label, style = FinioType.caption, color = colors.mutedForeground, maxLines = 1, softWrap = false)
                    }
                }
            }
            if (busiest != null) {
                Row(
                    Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(LucideIcons.Trophy, null, Modifier.size(12.dp), tint = colors.warning)
                    Text("Biggest spend: ${busiest.label} · ${compact(busiest.expenses)}", style = FinioType.caption, color = colors.mutedForeground)
                }
            }
            ChartDataTable(
                caption = "Spending by month, ${review.label}",
                columns = listOf("Month", "Spent"),
                rows = review.monthlyBreakdown.map { ChartTableRow(it.key, listOf(it.label, compact(it.expenses))) },
            )
        }

        if (review.topCategories.isNotEmpty()) {
            ChartCard("Top categories") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    review.topCategories.forEach { c ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                categoryFor[c.categoryId]?.name ?: "Uncategorized",
                                Modifier.weight(1f),
                                style = FinioType.label,
                                color = colors.foreground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(compact(c.amount), style = FinioType.caption.copy(fontWeight = FontWeight.SemiBold), color = colors.foreground)
                        }
                    }
                }
            }
        }

        if (review.movers.isNotEmpty()) {
            ChartCard("Biggest movers vs last year") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    review.movers.forEach { mover ->
                        val isUp = mover.change > 0
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                categoryFor[mover.categoryId]?.name ?: "Uncategorized",
                                Modifier.weight(1f),
                                style = FinioType.label,
                                color = colors.foreground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                (if (isUp) "+" else "−") + compact(abs(mover.change)),
                                style = FinioType.caption.copy(fontWeight = FontWeight.SemiBold),
                                color = if (isUp) colors.destructive else colors.positive,
                            )
                        }
                    }
                }
            }
        }

        review.biggestExpense?.let { tx ->
            ChartCard(title = null) {
                CardTitle("Biggest single expense", Modifier.padding(bottom = 8.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            tx.note.ifEmpty { categoryFor[tx.categoryId]?.name ?: "" }.ifEmpty { "Expense" },
                            style = FinioType.bodyMedium,
                            color = colors.foreground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(formatShortDate(tx.date), style = FinioType.caption, color = colors.mutedForeground)
                    }
                    Text(compact(tx.amount), style = FinioType.rowValue, color = colors.destructive)
                }
            }
        }

        Text(
            "${cur.transactionCount} transaction${if (cur.transactionCount == 1) "" else "s"} this year",
            Modifier.fillMaxWidth().padding(bottom = 8.dp),
            style = FinioType.caption,
            color = colors.mutedForeground,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun HeroFigure(modifier: Modifier, label: String, value: String, tone: Color, style: androidx.compose.ui.text.TextStyle) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = FinioType.label, color = FinioTheme.colors.mutedForeground)
        Text(value, style = style, color = tone, maxLines = 1)
    }
}
