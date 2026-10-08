package com.slowatcoding.finio.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
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
 * The latest result per call site is also remembered for the process, so coming back to a screen
 * whose inputs haven't changed paints with data on the first frame instead of flashing a loader.
 *
 * [compute] must be pure — read only what is in [keys]. For cheap work use `remember(keys) { … }`.
 */
@Composable
fun <T : Any> rememberDerived(vararg keys: Any?, compute: () -> T): T? {
    val site = compute.javaClass
    val keyList = keys.asList()
    @Suppress("UNCHECKED_CAST")
    val cached = remember(site, keyList) { DerivedCache.get(site, keyList) as T? }
    val value by produceState(initialValue = cached, keys = keys) {
        if (cached != null) return@produceState
        val result = withContext(Dispatchers.Default) { compute() }
        DerivedCache.put(site, keyList, result)
        value = result
    }
    return value
}

/** One entry per [rememberDerived] call site: the last keys and the result computed for them. */
private object DerivedCache {
    private class Entry(val keys: List<Any?>, val value: Any)

    private val entries = HashMap<Class<*>, Entry>()

    @Synchronized
    fun get(site: Class<*>, keys: List<Any?>): Any? =
        entries[site]?.takeIf { sameKeys(it.keys, keys) }?.value

    @Synchronized
    fun put(site: Class<*>, keys: List<Any?>, value: Any) {
        entries[site] = Entry(keys, value)
    }

    // Reference equality first: the store hands out the same immutable lists until they change, so
    // this stays O(keys) instead of walking thousands of transactions.
    private fun sameKeys(a: List<Any?>, b: List<Any?>) =
        a.size == b.size && a.indices.all { a[it] === b[it] || a[it] == b[it] }
}
