package com.slowatcoding.finio.ui.screens.loans

import androidx.compose.runtime.Composable
import com.slowatcoding.finio.ui.common.PlaceholderScreen
import com.slowatcoding.finio.ui.navigation.FinioNavigator

/**
 * PLACEHOLDER — port of web/src/pages/AddLoan.tsx — routes `/add-loan` and `/edit-loan/:id`.
 *
 * Replace the body; keep the signature (the NavHost calls it).
 * @param loanId non-null on `/edit-loan/:id` (EditGuard-ed by the shell).
 */
@Composable
fun AddLoanScreen(nav: FinioNavigator, loanId: String?) {
    PlaceholderScreen("Add loan", nav)
}
