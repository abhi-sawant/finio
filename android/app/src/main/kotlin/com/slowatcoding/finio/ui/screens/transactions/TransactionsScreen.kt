package com.slowatcoding.finio.ui.screens.transactions

import androidx.compose.runtime.Composable
import com.slowatcoding.finio.ui.common.HideAmountsToggle
import com.slowatcoding.finio.ui.common.PlaceholderScreen
import com.slowatcoding.finio.ui.navigation.FinioNavigator

/**
 * PLACEHOLDER — port of web/src/pages/Transactions.tsx — route `/transactions` (tab). Bulk-selection mode must call `SuppressFab(active = selecting)` (ui/shell) to hide the coin FAB.
 *
 * Replace the body; keep the signature (the NavHost calls it).
 */
@Composable
fun TransactionsScreen(nav: FinioNavigator) {
    PlaceholderScreen("Transactions", nav, isTab = true, actions = { HideAmountsToggle() })
}
