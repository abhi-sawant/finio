package com.slowatcoding.finio.ui.screens.settings

import androidx.compose.runtime.Composable
import com.slowatcoding.finio.ui.common.PlaceholderScreen
import com.slowatcoding.finio.ui.navigation.FinioNavigator

/**
 * PLACEHOLDER — port of web/src/pages/SettingsCategory.tsx — route `/settings/:category` (layout route: tab bar, no FAB).
 *
 * Replace the body; keep the signature (the NavHost calls it).
 * @param category account | backup | profile | security | notifications | organise. An unknown one should `nav.replace(Routes.Settings)` (web `<Navigate to="/settings" replace />`).
 */
@Composable
fun SettingsCategoryScreen(nav: FinioNavigator, category: String) {
    PlaceholderScreen("Settings", nav)
}
