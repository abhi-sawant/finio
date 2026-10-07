package com.slowatcoding.finio.ui.common

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import com.slowatcoding.finio.ui.components.EmptyState
import com.slowatcoding.finio.ui.components.FinioScreen
import com.slowatcoding.finio.ui.components.HeaderIconButton
import com.slowatcoding.finio.ui.components.HeaderIconSpacer
import com.slowatcoding.finio.ui.components.PageTitle
import com.slowatcoding.finio.ui.components.ScreenTitle
import com.slowatcoding.finio.ui.icons.LucideIcons
import com.slowatcoding.finio.ui.navigation.FinioNavigator

/*
 * The two header shapes every web page uses, so screens don't re-derive them:
 *
 *  Tab pages (Dashboard, Accounts, Transactions, Analytics, Tools, Settings):
 *      FinioScreen(header = { PageTitle("Accounts"); Row { HideAmountsToggle(); … } }) { … }
 *
 *  Sub-pages (everything else):
 *      FinioScreen(header = {
 *          BackButton(nav)
 *          ScreenTitle("Budgets")
 *          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { HideAmountsToggle(); HeaderIconButton(…) }
 *      }) { … }
 *    or with no trailing action, HeaderIconSpacer() in its place so the title stays centred.
 */

/** The header's back button (`<HeaderIconButton onClick={() => navigate(-1)} aria-label="Back">`). */
@Composable
fun BackButton(nav: FinioNavigator, onBack: (() -> Unit)? = null) {
    HeaderIconButton(LucideIcons.ArrowLeft, "Back", onClick = { onBack?.invoke() ?: nav.back() })
}

/**
 * A not-yet-built screen: the right header shape plus a muted note. Screen ports replace the
 * whole body of their `…Screen` composable — nothing else depends on this.
 */
@Composable
fun PlaceholderScreen(
    title: String,
    nav: FinioNavigator,
    isTab: Boolean = false,
    actions: @Composable RowScope.() -> Unit = { HeaderIconSpacer() },
) {
    FinioScreen(header = {
        if (isTab) {
            PageTitle(title)
            actions()
        } else {
            BackButton(nav)
            ScreenTitle(title)
            actions()
        }
    }) {
        EmptyState(title = "$title is coming", description = "This screen hasn't been ported to Android yet.")
    }
}
