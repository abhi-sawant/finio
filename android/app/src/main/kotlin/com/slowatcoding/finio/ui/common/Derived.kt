package com.slowatcoding.finio.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Heavy derived data (analytics, forecasts, budget statuses over thousands of rows) computed on
 * Dispatchers.Default whenever [keys] change — the `useMemo` of a large page, off the main thread.
 *
 * Returns null until the first result lands, then keeps showing the previous result while a
 * recompute runs (no flicker back to a loading state):
 *
 *   val statuses = rememberDerived(state.budgets, state.transactions, monthStartDay) {
 *       computeBudgetStatuses(state.budgets, state.transactions, BudgetPeriodOptions(monthStartDay))
 *   }
 *   if (statuses == null) PageLoader() else …
 *
 * [compute] must be pure — read only what is in [keys]. For cheap work use `remember(keys) { … }`.
 */
@Composable
fun <T : Any> rememberDerived(vararg keys: Any?, compute: () -> T): T? {
    val value by produceState<T?>(initialValue = null, keys = keys) {
        value = withContext(Dispatchers.Default) { compute() }
    }
    return value
}
