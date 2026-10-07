package com.slowatcoding.finio.ui.shell

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Chrome state a screen may need to influence. Today: hiding the coin FAB while the Transactions
 * screen is in bulk-selection mode (the web's `<body data-tx-selecting>` + `in-data-[tx-selecting]:hidden`).
 */
@Stable
class ShellState {
    /** A counter rather than a flag, so two suppressors can never un-hide each other. */
    private var fabSuppressors by mutableIntStateOf(0)

    val isFabSuppressed: Boolean get() = fabSuppressors > 0

    internal fun suppressFab() {
        fabSuppressors++
    }

    internal fun releaseFab() {
        fabSuppressors = (fabSuppressors - 1).coerceAtLeast(0)
    }
}

val LocalShellState = staticCompositionLocalOf<ShellState> { error("LocalShellState used outside the app shell") }

/**
 * Hide the coin FAB while [active] (and automatically when the caller leaves composition):
 *
 *   SuppressFab(active = selectionMode)
 */
@Composable
fun SuppressFab(active: Boolean) {
    val shell = LocalShellState.current
    DisposableEffect(active) {
        if (active) shell.suppressFab()
        onDispose { if (active) shell.releaseFab() }
    }
}
