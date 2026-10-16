package com.slowatcoding.finio.ui.screens.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.slowatcoding.finio.core.data.MISC_CATEGORY_ID
import com.slowatcoding.finio.core.js.nowInstant
import com.slowatcoding.finio.core.js.toIso
import com.slowatcoding.finio.core.model.Account
import com.slowatcoding.finio.core.model.AccountType
import com.slowatcoding.finio.core.model.TransactionType
import com.slowatcoding.finio.core.store.NewTransaction
import com.slowatcoding.finio.core.store.reconciliationAdjustment
import com.slowatcoding.finio.core.util.MAX_NOTE_LENGTH
import com.slowatcoding.finio.core.util.cleanText
import com.slowatcoding.finio.core.util.stripLeading
import com.slowatcoding.finio.ui.common.financeStore
import com.slowatcoding.finio.ui.common.rememberMoneyFormatter
import com.slowatcoding.finio.ui.common.undoToast
import com.slowatcoding.finio.ui.components.ButtonSize
import com.slowatcoding.finio.ui.components.ButtonVariant
import com.slowatcoding.finio.ui.components.FinioButton
import com.slowatcoding.finio.ui.components.FinioDialog
import com.slowatcoding.finio.ui.components.FinioTextField
import com.slowatcoding.finio.ui.components.NumberPad
import com.slowatcoding.finio.ui.theme.FinioTheme
import com.slowatcoding.finio.ui.theme.FinioType
import kotlin.math.abs

/**
 * Port of components/accounts/ReconcileAccountDialog.tsx — statement balance in, adjustment
 * transaction out. Per-account and destructive by design (it posts a real Miscellaneous
 * transaction for the gap) — unlike Settings → Reconcile Balances, which only recomputes caches.
 *
 * Render it only while open; closing resets it (the web's `reset()` on close).
 */
@Composable
fun ReconcileAccountDialog(account: Account, onDismissRequest: () -> Unit) {
    val store = financeStore()
    val money = rememberMoneyFormatter()
    val colors = FinioTheme.colors
    val isCredit = account.type == AccountType.Credit

    var statementInput by rememberSaveable { mutableStateOf("0") }
    var note by rememberSaveable { mutableStateOf("") }
    var showResult by rememberSaveable { mutableStateOf(false) }

    // Credit accounts store balance as a negative "amount owed"; the statement shows a positive due.
    val parsedStatement = jsParseFloat(statementInput) ?: 0.0
    val effectiveStatement = if (isCredit) -parsedStatement else parsedStatement
    val adjustment = reconciliationAdjustment(account.balance, effectiveStatement)

    val handleConfirm = {
        val type = adjustment.type
        if (type != null) {
            val transactionId = store.addTransaction(
                NewTransaction(
                    type = type,
                    amount = adjustment.amount,
                    accountId = account.id,
                    categoryId = MISC_CATEGORY_ID,
                    date = nowInstant().toIso(),
                    note = cleanText(note, MAX_NOTE_LENGTH).ifEmpty { "Balance adjustment for ${account.name}" },
                    labels = emptyList(),
                ),
            )
            undoToast("Adjustment posted to \"${account.name}\"") { store.deleteTransaction(transactionId) }
            onDismissRequest()
        }
    }

    FinioDialog(onDismissRequest = onDismissRequest, title = "Reconcile \"${account.name}\"") {
        if (!showResult) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Enter the balance from your ${if (isCredit) "card statement" else "bank statement"} and we'll show you the difference against what Finio has on record (${money(if (isCredit) abs(account.balance) else account.balance)}).",
                    style = FinioType.caption,
                    color = colors.mutedForeground,
                )
                Text(if (isCredit) "Statement due" else "Statement balance", style = FinioType.label, color = colors.mutedForeground)
                NumberPad(statementInput, { statementInput = it })
                FinioButton("Compare", onClick = { showResult = true }, modifier = Modifier.fillMaxWidth(), size = ButtonSize.Lg)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (adjustment.type == null) {
                    Text("You're all set — Finio already matches your statement.", style = FinioType.body, color = colors.foreground)
                } else {
                    val income = adjustment.type == TransactionType.Income
                    Text(
                        buildAnnotatedString {
                            append("Off by ")
                            withStyle(SpanStyle(color = if (income) colors.positive else colors.destructive)) {
                                append(if (income) "+" else "−")
                                append(money(adjustment.amount))
                            }
                            append(". We'll add a ")
                            append(if (income) "balance-up income" else "balance-down expense")
                            append(" adjustment, categorized as Miscellaneous, so you can re-categorize it later.")
                        },
                        style = FinioType.body,
                        color = colors.foreground,
                    )
                    FinioTextField(
                        note,
                        { note = stripLeading(it).take(MAX_NOTE_LENGTH) },
                        placeholder = "Note (optional)",
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (adjustment.type != null) {
                        FinioButton("Add adjustment", onClick = handleConfirm, modifier = Modifier.weight(1f))
                    }
                    FinioButton(
                        if (adjustment.type == null) "Close" else "Back",
                        onClick = { if (adjustment.type == null) onDismissRequest() else showResult = false },
                        variant = ButtonVariant.Secondary,
                    )
                }
            }
        }
    }
}
