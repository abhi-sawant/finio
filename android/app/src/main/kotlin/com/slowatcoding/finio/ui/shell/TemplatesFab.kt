package com.slowatcoding.finio.ui.shell

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.slowatcoding.finio.core.js.nowInstant
import com.slowatcoding.finio.core.js.toIso
import com.slowatcoding.finio.core.store.NewTransaction
import com.slowatcoding.finio.ui.common.collectFinanceState
import com.slowatcoding.finio.ui.common.financeStore
import com.slowatcoding.finio.ui.common.rememberMoneyFormatter
import com.slowatcoding.finio.ui.common.undoToast
import com.slowatcoding.finio.ui.components.FinioPopover
import com.slowatcoding.finio.ui.icons.LucideIcons
import com.slowatcoding.finio.ui.mudra.CoinFab
import com.slowatcoding.finio.ui.theme.FinioShapes
import com.slowatcoding.finio.ui.theme.FinioTheme
import com.slowatcoding.finio.ui.theme.FinioType

/**
 * The coin FAB (Layout.tsx): tap → Add Transaction; long-press → a "Templates" popover listing
 * the saved templates, one tap adding today's copy with an Undo toast. Positioned by the shell
 * (16dp from the right, 88dp above the navigation-bar inset).
 */
@Composable
fun TemplatesFab(onAdd: () -> Unit, modifier: Modifier = Modifier) {
    val store = financeStore()
    val state by collectFinanceState()
    val money = rememberMoneyFormatter()
    val haptics = LocalHapticFeedback.current
    var open by remember { mutableStateOf(false) }
    val colors = FinioTheme.colors

    Box(modifier) {
        CoinFab(
            onClick = onAdd,
            onLongClick = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                open = true
            },
        )
        if (open) {
            FinioPopover(onDismissRequest = { open = false }, contentPadding = PaddingValues(10.dp)) {
                Column(Modifier.width(236.dp)) {
                    Text(
                        "Templates",
                        Modifier.padding(start = 4.dp, end = 4.dp, bottom = 4.dp),
                        style = FinioType.label,
                        color = colors.mutedForeground,
                    )
                    if (state.templates.isEmpty()) {
                        Text(
                            "No saved templates yet. Long-press a transaction and choose \"Save as template\".",
                            Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                            style = FinioType.caption,
                            color = colors.mutedForeground,
                        )
                    } else {
                        Column(
                            Modifier.heightIn(max = 288.dp).verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            state.templates.forEach { t ->
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(FinioShapes.md)
                                        .clickable {
                                            val newId = store.addTransaction(
                                                NewTransaction(
                                                    type = t.type,
                                                    amount = t.amount,
                                                    accountId = t.accountId,
                                                    toAccountId = t.toAccountId,
                                                    categoryId = t.categoryId,
                                                    date = nowInstant().toIso(),
                                                    note = t.note,
                                                    labels = t.labels,
                                                    splits = t.splits,
                                                ),
                                            )
                                            open = false
                                            undoToast("Added \"${t.name}\"") { store.deleteTransaction(newId) }
                                        }
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(LucideIcons.Repeat, null, Modifier.size(13.dp), tint = colors.mutedForeground)
                                    Text(
                                        t.name,
                                        Modifier.weight(1f),
                                        style = FinioType.body,
                                        color = colors.popoverForeground,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(money(t.amount, compact = true), style = FinioType.caption, color = colors.mutedForeground)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
