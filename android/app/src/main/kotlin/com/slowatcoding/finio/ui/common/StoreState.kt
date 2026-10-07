package com.slowatcoding.finio.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.slowatcoding.finio.core.model.FinanceState
import com.slowatcoding.finio.core.store.FinanceStore
import com.slowatcoding.finio.di.appContainer

/*
 * How a screen reads data — the Zustand selector hooks' counterpart. There are no ViewModels:
 *
 *   val store = financeStore()
 *   val state by collectFinanceState()
 *   val accounts = state.accounts
 *   …
 *   onClick = { store.deleteBudget(id) }
 *
 * Store actions are synchronous, atomic and main-thread safe. Anything heavier than a filter or
 * a sum goes through [rememberDerived] so it runs on Dispatchers.Default.
 */

/** The process's finance store (actions + `current` snapshot). */
@Composable
fun financeStore(): FinanceStore = appContainer().financeStore

/** The whole finance state as Compose state, lifecycle-aware. */
@Composable
fun collectFinanceState(): State<FinanceState> = appContainer().financeStore.state.collectAsStateWithLifecycle()
