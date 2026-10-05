package com.slowatcoding.finio.ui.screens.legal

import androidx.compose.runtime.Composable
import com.slowatcoding.finio.ui.common.PlaceholderScreen
import com.slowatcoding.finio.ui.navigation.FinioNavigator

/**
 * PLACEHOLDER — port of web/src/pages/legal/PrivacyPolicy.tsx — route `/privacy`. Back is plain `nav.back()` (the Dashboard is always underneath, so useLegalBack's fallback isn't needed).
 *
 * Replace the body; keep the signature (the NavHost calls it).
 */
@Composable
fun PrivacyScreen(nav: FinioNavigator) {
    PlaceholderScreen("Privacy policy", nav)
}
