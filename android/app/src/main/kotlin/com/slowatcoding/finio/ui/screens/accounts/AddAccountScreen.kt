package com.slowatcoding.finio.ui.screens.accounts

import androidx.compose.runtime.Composable
import com.slowatcoding.finio.ui.common.PlaceholderScreen
import com.slowatcoding.finio.ui.navigation.FinioNavigator

/**
 * PLACEHOLDER — port of web/src/pages/AddAccount.tsx — routes `/add-account` and `/edit-account/:id`.
 *
 * Replace the body; keep the signature (the NavHost calls it).
 * @param accountId non-null on `/edit-account/:id` (EditGuard-ed by the shell).
 */
@Composable
fun AddAccountScreen(nav: FinioNavigator, accountId: String?) {
    PlaceholderScreen("Add account", nav)
}
