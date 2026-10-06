package com.slowatcoding.finio.ui.screens.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.slowatcoding.finio.core.calc.topMerchants
import com.slowatcoding.finio.core.format.shouldCompactGroup
import com.slowatcoding.finio.core.model.Transaction
import com.slowatcoding.finio.ui.charts.CardTitle
import com.slowatcoding.finio.ui.charts.ChartCard
import com.slowatcoding.finio.ui.common.rememberDerived
import com.slowatcoding.finio.ui.common.rememberMoneyFormatter
import com.slowatcoding.finio.ui.navigation.FinioNavigator
import com.slowatcoding.finio.ui.navigation.Routes
import com.slowatcoding.finio.ui.theme.FinioShapes
import com.slowatcoding.finio.ui.theme.FinioTheme
import com.slowatcoding.finio.ui.theme.FinioType

private const val TOP_N = 5

/**
 * Port of TopMerchants.tsx — the five biggest expense merchants in the period, each with a
 * lavender-gradient bar relative to the first, and "See all" into Merchants.
 */
@Composable
fun TopMerchants(transactions: List<Transaction>, nav: FinioNavigator, modifier: Modifier = Modifier) {
    val money = rememberMoneyFormatter()
    val colors = FinioTheme.colors
    val merchants = rememberDerived(transactions) { topMerchants(transactions, TOP_N) } ?: return
    if (merchants.isEmpty()) return
    val compact = shouldCompactGroup(merchants.map { it.totalAmount })
    val max = merchants.first().totalAmount

    ChartCard(title = null, modifier = modifier) {
        Row(
            Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CardTitle("Top merchants")
            Text(
                "See all",
                Modifier.clip(FinioShapes.chip).clickable(role = Role.Button) { nav.navigate(Routes.Merchants) },
                style = FinioType.label,
                color = colors.primary,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            merchants.forEach { m ->
                Column {
                    Row(
                        Modifier.fillMaxWidth().padding(bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            m.displayName,
                            Modifier.weight(1f, fill = false),
                            style = FinioType.label,
                            color = colors.foreground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            money(m.totalAmount, compact = true, forceCompact = compact),
                            Modifier.padding(start = 8.dp),
                            style = FinioType.caption,
                            color = colors.mutedForeground,
                        )
                    }
                    Box(Modifier.fillMaxWidth().height(6.dp).clip(FinioShapes.full).background(colors.muted)) {
                        val f = if (max > 0) (m.totalAmount / max).toFloat().coerceIn(0f, 1f) else 0f
                        Box(Modifier.fillMaxHeight().fillMaxWidth(f).clip(FinioShapes.full).background(FinioTheme.brushes.gradPrimary))
                    }
                }
            }
        }
    }
}
