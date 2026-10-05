package com.slowatcoding.finio.ui.screens.accounts

import androidx.compose.runtime.Composable
import com.slowatcoding.finio.ui.common.HideAmountsToggle
import com.slowatcoding.finio.ui.common.PlaceholderScreen
import com.slowatcoding.finio.ui.navigation.FinioNavigator

/**
 * PLACEHOLDER — port of web/src/pages/Accounts.tsx — route `/accounts` (tab; the coin FAB is hidden here, the page has its own add button).
 *
 * Replace the body; keep the signature (the NavHost calls it).
 */
@Composable
fun AccountsScreen(nav: FinioNavigator) {
    PlaceholderScreen("Accounts", nav, isTab = true, actions = { HideAmountsToggle() })
}
