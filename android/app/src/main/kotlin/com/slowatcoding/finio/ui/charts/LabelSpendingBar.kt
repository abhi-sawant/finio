package com.slowatcoding.finio.ui.charts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.slowatcoding.finio.core.model.Transaction
import com.slowatcoding.finio.core.model.TransactionType
import com.slowatcoding.finio.ui.common.collectFinanceState
import com.slowatcoding.finio.ui.common.rememberDerived
import com.slowatcoding.finio.ui.common.rememberMoneyFormatter
import com.slowatcoding.finio.ui.components.parseHexColor
import com.slowatcoding.finio.ui.theme.FinioShapes
import com.slowatcoding.finio.ui.theme.FinioTheme
import com.slowatcoding.finio.ui.theme.FinioType

@Immutable
private data class LabelAmount(val name: String, val amount: Double, val color: String?)

/**
 * Port of LabelSpendingBar.tsx — the six labels with the most expense, each a muted name, a
 * compact amount and an 8dp track filled in the label's colour relative to the biggest.
 */
@Composable
fun LabelSpendingBar(transactions: List<Transaction>, modifier: Modifier = Modifier) {
    val state by collectFinanceState()
    val money = rememberMoneyFormatter()
    val colors = FinioTheme.colors
    val labels = state.labels

    val data = rememberDerived(transactions, labels) {
        val byLabel = LinkedHashMap<String, Double>()
        for (tx in transactions) {
            if (tx.type != TransactionType.Expense) continue
            for (id in tx.labels) byLabel[id] = (byLabel[id] ?: 0.0) + tx.amount
        }
        byLabel.entries
            .map { (id, amount) -> labels.firstOrNull { it.id == id }.let { LabelAmount(it?.name ?: "Unknown", amount, it?.color) } }
            .sortedByDescending { it.amount }
            .take(6)
    } ?: return
    if (data.isEmpty()) return

    ChartCard("Spending by label", modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val max = data.first().amount
            data.forEach { item ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(item.name, style = FinioType.caption, color = colors.mutedForeground)
                        Text(money(item.amount, compact = true), style = FinioType.label, color = colors.foreground)
                    }
                    Box(Modifier.fillMaxWidth().height(8.dp).clip(FinioShapes.full).background(colors.muted)) {
                        val fraction = if (max > 0) (item.amount / max).toFloat().coerceIn(0f, 1f) else 0f
                        Box(
                            Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(fraction)
                                .clip(FinioShapes.full)
                                .background(item.color?.let { parseHexColor(it, colors.mutedForeground) } ?: colors.mutedForeground),
                        )
                    }
                }
            }
        }
    }
}
