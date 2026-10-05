package com.slowatcoding.finio.ui.screens.settings

import androidx.compose.runtime.Composable
import com.slowatcoding.finio.ui.common.PlaceholderScreen
import com.slowatcoding.finio.ui.navigation.FinioNavigator

/**
 * PLACEHOLDER — port of web/src/pages/Settings.tsx — route `/settings` (layout route: tab bar shown, no FAB, no tab highlighted). Category rows navigate to `Routes.SettingsCategory("account"|"backup"|"profile"|"security"|"notifications"|"organise")`; the footer links to `Routes.Privacy` / `Routes.Terms`.
 *
 * Replace the body; keep the signature (the NavHost calls it).
 */
@Composable
fun SettingsScreen(nav: FinioNavigator) {
    PlaceholderScreen("Settings", nav, isTab = true)
}
