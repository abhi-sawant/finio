package com.slowatcoding.finio.ui.screens.analytics

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.slowatcoding.finio.core.calc.InsightAction
import com.slowatcoding.finio.core.calc.InsightInput
import com.slowatcoding.finio.core.calc.InsightKind
import com.slowatcoding.finio.core.calc.InsightSeverity
import com.slowatcoding.finio.core.calc.buildInsights
import com.slowatcoding.finio.core.format.formatCurrency
import com.slowatcoding.finio.core.format.formatShortDate
import com.slowatcoding.finio.core.js.nowInstant
import com.slowatcoding.finio.core.model.TransactionType
import com.slowatcoding.finio.core.period.PeriodType
import com.slowatcoding.finio.core.period.normalizeMonthStartDay
import com.slowatcoding.finio.core.period.periodLabel
import com.slowatcoding.finio.core.period.periodRange
import com.slowatcoding.finio.core.store.NewRecurring
import com.slowatcoding.finio.ui.charts.CardTitle
import com.slowatcoding.finio.ui.charts.ChartCard
import com.slowatcoding.finio.ui.common.collectFinanceState
import com.slowatcoding.finio.ui.common.financeStore
import com.slowatcoding.finio.ui.common.rememberDerived
import com.slowatcoding.finio.ui.common.undoToast
import com.slowatcoding.finio.ui.components.ButtonSize
import com.slowatcoding.finio.ui.components.ButtonVariant
import com.slowatcoding.finio.ui.components.FinioButton
import com.slowatcoding.finio.ui.components.FinioDivider
import com.slowatcoding.finio.ui.icons.LucideIcons
import com.slowatcoding.finio.ui.navigation.FinioNavigator
import com.slowatcoding.finio.ui.theme.FinioShapes
import com.slowatcoding.finio.ui.theme.FinioTheme
import com.slowatcoding.finio.ui.theme.FinioType

private fun kindIcon(kind: InsightKind) = when (kind) {
    InsightKind.CategorySpike -> LucideIcons.TrendingUp
    InsightKind.CategoryDrop -> LucideIcons.ThumbsUp
    InsightKind.Subscription -> LucideIcons.Repeat
    InsightKind.BudgetOver, InsightKind.BudgetPace -> LucideIcons.AlertTriangle
    InsightKind.SavingsRate, InsightKind.CategoryShare -> LucideIcons.Lightbulb
    InsightKind.NegativeBalance -> LucideIcons.CircleAlert
}

/**
 * Port of InsightsFeed.tsx — what stands out this financial month. A subscription candidate can be
 * turned into a recurring rule (starting at its next expected charge, with Undo) or waved away;
 * dismissals last for this visit only. Navigate actions go through [FinioNavigator.navigateToPath].
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InsightsFeed(nav: FinioNavigator, modifier: Modifier = Modifier) {
    val store = financeStore()
    val state by collectFinanceState()
    val colors = FinioTheme.colors
    val hideAmounts = state.settings.hideAmounts
    val monthStartDay = normalizeMonthStartDay(state.settings.monthStartDay)
    var dismissed by rememberSaveable { mutableStateOf(listOf<String>()) }

    val insights = rememberDerived(
        state.transactions, state.categories, state.labels, state.budgets, state.recurring, state.accounts, monthStartDay, hideAmounts,
    ) {
        buildInsights(
            InsightInput(
                transactions = state.transactions,
                categories = state.categories,
                labels = state.labels,
                budgets = state.budgets,
                recurring = state.recurring,
                accounts = state.accounts,
                monthStartDay = monthStartDay,
            ),
        ) { value -> formatCurrency(value, compact = true, hidden = hideAmounts, precise = false) }
    } ?: return

    val visible = insights.filter { it.id !in dismissed }
    if (visible.isEmpty()) return

    ChartCard(title = null, modifier = modifier) {
        Row(
            Modifier.fillMaxWidth().padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(LucideIcons.Lightbulb, null, Modifier.size(15.dp), tint = colors.mutedForeground)
            CardTitle("Insights", Modifier.weight(1f))
            Text(
                periodLabel(periodRange(PeriodType.Monthly, nowInstant(), monthStartDay), monthStartDay),
                style = FinioType.label,
                color = colors.mutedForeground,
            )
        }

        visible.forEachIndexed { index, insight ->
            if (index > 0) FinioDivider()
            val tone = when (insight.severity) {
                InsightSeverity.Warn -> colors.warning
                InsightSeverity.Info -> colors.primary
                InsightSeverity.Good -> colors.positive
            }
            Row(
                Modifier.fillMaxWidth().padding(top = if (index == 0) 0.dp else 12.dp, bottom = if (index == visible.lastIndex) 0.dp else 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(kindIcon(insight.kind), null, Modifier.padding(top = 2.dp).size(16.dp), tint = tone)
                Column(Modifier.weight(1f)) {
                    Text(insight.title, style = FinioType.bodyMedium, color = colors.foreground)
                    Text(insight.detail, Modifier.padding(top = 2.dp), style = FinioType.caption, color = colors.mutedForeground)

                    when (val action = insight.action) {
                        is InsightAction.CreateRecurring -> FlowRow(
                            Modifier.padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            FinioButton(
                                "Create recurring rule",
                                onClick = {
                                    val c = action.candidate
                                    // Starts at the next expected charge, so nothing already in the ledger is regenerated.
                                    val ruleId = store.addRecurring(
                                        NewRecurring(
                                            type = TransactionType.Expense,
                                            amount = c.amount,
                                            accountId = c.accountId,
                                            categoryId = c.categoryId,
                                            note = c.note,
                                            labels = c.labels,
                                            frequency = c.frequency,
                                            startDate = c.nextDate,
                                        ),
                                    )
                                    dismissed = dismissed + insight.id
                                    undoToast("Recurring rule created — next on ${formatShortDate(c.nextDate)}") {
                                        store.deleteRecurring(ruleId)
                                        dismissed = dismissed - insight.id
                                    }
                                },
                                size = ButtonSize.Sm,
                            )
                            FinioButton(
                                "Not a subscription",
                                onClick = { dismissed = dismissed + insight.id },
                                size = ButtonSize.Sm,
                                variant = ButtonVariant.Ghost,
                            )
                        }
                        is InsightAction.Navigate -> Row(
                            Modifier
                                .padding(top = 6.dp)
                                .clip(FinioShapes.chip)
                                .clickable(role = Role.Button) { nav.navigateToPath(action.to) },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(action.label, style = FinioType.label, color = colors.primary)
                            Icon(LucideIcons.ChevronRight, null, Modifier.size(12.dp), tint = colors.primary)
                        }
                        null -> Unit
                    }
                }
            }
        }
    }
}
