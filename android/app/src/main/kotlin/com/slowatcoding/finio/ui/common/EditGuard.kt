package com.slowatcoding.finio.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.slowatcoding.finio.ui.navigation.FinioNavigator
import com.slowatcoding.finio.ui.navigation.FinioRoute

/**
 * Port of App.tsx's `EditGuard`: an edit route whose id no longer exists replaces itself with
 * [fallback] (the list) instead of rendering a blank "add" form.
 *
 * The check is made ONCE, when the destination is first composed — an entity deleted from inside
 * its own edit screen must not trigger a redirect that fights that screen's own `nav.back()`.
 * The app shell already wraps EditTransaction / EditAccount / EditLoan / LoanSchedule in it.
 */
@Composable
fun EditGuard(
    exists: () -> Boolean,
    fallback: FinioRoute,
    nav: FinioNavigator,
    content: @Composable () -> Unit,
) {
    val found = remember { exists() }
    if (found) {
        content()
    } else {
        LaunchedEffect(Unit) { nav.replace(fallback) }
    }
}
