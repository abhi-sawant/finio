package com.slowatcoding.finio.ui.screens.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.slowatcoding.finio.ui.common.HideAmountsToggle
import com.slowatcoding.finio.ui.common.PlaceholderScreen
import com.slowatcoding.finio.ui.components.HeaderIconButton
import com.slowatcoding.finio.ui.icons.LucideIcons
import com.slowatcoding.finio.ui.navigation.FinioNavigator
import com.slowatcoding.finio.ui.navigation.Routes

/**
 * PLACEHOLDER — port of web/src/pages/Dashboard.tsx — route `/` (tab). Header: greeting, HideAmountsToggle, Settings button (mobile reaches Settings from here).
 *
 * Replace the body; keep the signature (the NavHost calls it).
 */
@Composable
fun DashboardScreen(nav: FinioNavigator) {
    PlaceholderScreen("Home", nav, isTab = true, actions = {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HideAmountsToggle()
            HeaderIconButton(LucideIcons.Settings, "Settings", onClick = { nav.navigate(Routes.Settings) })
        }
    })
}
