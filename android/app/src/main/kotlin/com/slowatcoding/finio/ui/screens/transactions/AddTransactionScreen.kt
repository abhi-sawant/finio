package com.slowatcoding.finio.ui.screens.transactions

import androidx.compose.runtime.Composable
import com.slowatcoding.finio.core.share.SharedTransactionDraft
import com.slowatcoding.finio.ui.common.PlaceholderScreen
import com.slowatcoding.finio.ui.navigation.FinioNavigator

/**
 * PLACEHOLDER — port of web/src/pages/AddTransaction.tsx — routes `/add-transaction`, `/share-target` and `/edit-transaction/:id` (same page, like the web).
 *
 * Replace the body; keep the signature (the NavHost calls it).
 * @param transactionId non-null on `/edit-transaction/:id` (already EditGuard-ed by the shell: it exists when this first composes).
 * @param draft share-sheet / shortcut draft (`/share-target`, `/add-transaction?type=`); seeds a blank form only, never an edit. Null for a plain add.
 */
@Composable
fun AddTransactionScreen(nav: FinioNavigator, transactionId: String?, draft: SharedTransactionDraft?) {
    PlaceholderScreen("Add transaction", nav)
}
