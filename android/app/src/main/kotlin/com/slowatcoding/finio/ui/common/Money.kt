package com.slowatcoding.finio.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.slowatcoding.finio.core.format.formatCurrency

/**
 * `formatCurrency` with `hidden` already bound to `settings.hideAmounts` — the web passes
 * `hideAmounts` by hand at every call site; here a screen grabs one formatter:
 *
 *   val money = rememberMoneyFormatter()
 *   Text(money(balance))                 // ₹1,23,456.50 or ₹••••
 *   Text(money(total, compact = true))   // ₹1.2L above the lakh threshold
 *
 * Never format money with anything else (`formatCurrency` pads paise, groups en-IN, compacts).
 */
@Immutable
class MoneyFormatter(val hidden: Boolean) {
    operator fun invoke(
        amount: Double,
        compact: Boolean = false,
        precise: Boolean = true,
        forceCompact: Boolean = false,
    ): String = formatCurrency(amount, compact = compact, hidden = hidden, precise = precise, forceCompact = forceCompact)
}

@Composable
fun rememberMoneyFormatter(): MoneyFormatter {
    val state by collectFinanceState()
    val hidden = state.settings.hideAmounts
    return remember(hidden) { MoneyFormatter(hidden) }
}
