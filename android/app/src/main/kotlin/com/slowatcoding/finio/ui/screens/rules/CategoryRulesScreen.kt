package com.slowatcoding.finio.ui.screens.rules

import androidx.compose.runtime.Composable
import com.slowatcoding.finio.core.model.RuleScope
import com.slowatcoding.finio.ui.common.PlaceholderScreen
import com.slowatcoding.finio.ui.navigation.FinioNavigator

/**
 * PLACEHOLDER — port of web/src/pages/CategoryRules.tsx — route `/category-rules`.
 *
 * Replace the body; keep the signature (the NavHost calls it).
 * @param prefillPattern Merchants' "Create a rule" prefill (web `location.state.pattern`); opens the form when set.
 * @param prefillScope the prefill's scope (web `location.state.scope`).
 */
@Composable
fun CategoryRulesScreen(nav: FinioNavigator, prefillPattern: String?, prefillScope: RuleScope?) {
    PlaceholderScreen("Category rules", nav)
}
