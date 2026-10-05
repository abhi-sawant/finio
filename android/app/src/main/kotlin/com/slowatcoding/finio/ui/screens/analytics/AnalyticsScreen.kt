package com.slowatcoding.finio.ui.screens.analytics

import androidx.compose.runtime.Composable
import com.slowatcoding.finio.ui.common.HideAmountsToggle
import com.slowatcoding.finio.ui.common.PlaceholderScreen
import com.slowatcoding.finio.ui.navigation.FinioNavigator

/**
 * PLACEHOLDER — port of web/src/pages/Analytics.tsx — route `/analytics` (tab).
 *
 * Replace the body; keep the signature (the NavHost calls it).
 */
@Composable
fun AnalyticsScreen(nav: FinioNavigator) {
    PlaceholderScreen("Analytics", nav, isTab = true, actions = { HideAmountsToggle() })
}
