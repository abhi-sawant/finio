package com.slowatcoding.finio.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.slowatcoding.finio.core.calc.activeAccounts
import com.slowatcoding.finio.core.deposit.isDepositAccount
import com.slowatcoding.finio.core.model.AccountType
import com.slowatcoding.finio.core.store.balanceTx
import com.slowatcoding.finio.core.store.sumTransactionDeltas
import com.slowatcoding.finio.ui.common.collectFinanceState
import com.slowatcoding.finio.ui.common.financeStore
import com.slowatcoding.finio.ui.common.rememberMoneyFormatter
import com.slowatcoding.finio.ui.components.ButtonVariant
import com.slowatcoding.finio.ui.components.FinioButton
import com.slowatcoding.finio.ui.components.FinioDialog
import com.slowatcoding.finio.ui.components.FinioTextField
import com.slowatcoding.finio.ui.theme.FinioTheme
import com.slowatcoding.finio.ui.theme.FinioType
import kotlin.math.abs

/**
 * Port of web/src/components/settings/SwitchToFullDialog.tsx. Leaving transactions-only mode: ask
 * what each account really holds today, so its opening balance can be set to `current − Σ
 * transactions` and the books come out straight. Deposits are left out — they are funded by
 * transfers, so their balance is already right.
 */
@Composable
fun SwitchToFullDialog(onDismissRequest: () -> Unit) {
    val store = financeStore()
    val state by collectFinanceState()
    val money = rememberMoneyFormatter()
    val colors = FinioTheme.colors

    val rows = remember(state.accounts, state.transactions) {
        val deltas = sumTransactionDeltas(state.transactions.map { it.balanceTx() })
        activeAccounts(state.accounts)
            .filter { !isDepositAccount(it) }
            .map { it to (deltas[it.id] ?: 0.0) }
    }
    var values by remember { mutableStateOf(emptyMap<String, String>()) }

    fun confirm() {
        val balances = HashMap<String, Double>()
        for ((account, _) in rows) {
            val parsed = values[account.id]?.toDoubleOrNull()?.takeIf { it.isFinite() } ?: continue
            // A card's figure is what is owed, stored as a negative balance.
            balances[account.id] = if (account.type == AccountType.Credit) -abs(parsed) else parsed
        }
        store.switchToFullMode(balances)
        onDismissRequest()
    }

    FinioDialog(
        onDismissRequest = onDismissRequest,
        title = "Set your current balances",
        description = "Finio wasn't tracking balances, so it needs a starting point. Enter what each account holds right now and the opening balance is worked out from your transactions. Leave one blank to keep it as it is.",
    ) {
        Column(
            Modifier.fillMaxWidth().heightIn(max = 360.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            for ((account, logged) in rows) {
                val isCredit = account.type == AccountType.Credit
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "${account.name} · ${if (isCredit) "amount owed" else "balance now"}",
                        style = FinioType.bodyMedium,
                        color = colors.foreground,
                    )
                    FinioTextField(
                        value = values[account.id] ?: "",
                        onValueChange = { input ->
                            values = values + (account.id to input.filter { it.isDigit() || it == '.' || it == '-' })
                        },
                        placeholder = "0",
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    )
                    Text(
                        "Transactions logged so far net ${if (logged < 0) "−" else "+"}${money(abs(logged))}",
                        style = FinioType.caption,
                        color = colors.mutedForeground,
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FinioButton("Switch to full tracking", onClick = ::confirm, modifier = Modifier.weight(1f))
            FinioButton("Cancel", onClick = onDismissRequest, variant = ButtonVariant.Secondary)
        }
    }
}
