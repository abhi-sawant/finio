package com.slowatcoding.finio.ui.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import com.slowatcoding.finio.ui.icons.LucideIcons

// Port of web/src/components/layout/navItems.ts.

/** The mobile bottom tabs (web `navTabs` minus the desktop-only Settings, plus Tools). */
enum class FinioTab(val route: FinioRoute, val label: String, val iconName: String) {
    Home(Routes.Dashboard, "Home", "layout-dashboard"),
    Accounts(Routes.Accounts, "Accounts", "wallet"),
    Transactions(Routes.Transactions, "Txns", "arrow-left-right"),
    Analytics(Routes.Analytics, "Analytics", "bar-chart-3"),
    Tools(Routes.Tools, "Tools", "wrench"),
    ;

    /** The tab's lucide icon at [strokeWidth] (2 at rest, 2.4 active). */
    fun icon(strokeWidth: Float = 2f): ImageVector = LucideIcons.byKebab(iconName, strokeWidth)!!
}

/** A secondary destination — the Tools page list (web `moreNavItems`). */
data class MoreNavItem(val route: FinioRoute, val icon: ImageVector, val label: String, val description: String)

val MoreNavItems: List<MoreNavItem> = listOf(
    MoreNavItem(Routes.Budgets, LucideIcons.Target, "Budgets", "Spending limits by category or label"),
    MoreNavItem(Routes.Recurring, LucideIcons.Repeat, "Recurring", "Bills, subscriptions and income that repeat"),
    MoreNavItem(Routes.Goals, LucideIcons.PiggyBank, "Goals", "Savings targets and contributions"),
    MoreNavItem(Routes.Debts, LucideIcons.HandCoins, "Debts", "Money you've lent or owe"),
    MoreNavItem(Routes.Loans, LucideIcons.Landmark, "Loans", "EMIs, schedule and prepayments"),
    MoreNavItem(Routes.Merchants, LucideIcons.Store, "Merchants", "Where your money goes, grouped by name"),
    MoreNavItem(Routes.YearInReview, LucideIcons.PartyPopper, "Year in review", "Your financial year, looked back on"),
)

/** Routes rendered inside the web `<Layout>`: the tab bar shows on these. */
fun NavDestination.isLayoutRoute(): Boolean =
    hasRoute(Routes.Dashboard::class) || hasRoute(Routes.Accounts::class) ||
        hasRoute(Routes.Transactions::class) || hasRoute(Routes.Analytics::class) ||
        hasRoute(Routes.Tools::class) || hasRoute(Routes.Settings::class) ||
        hasRoute(Routes.SettingsCategory::class)

/** Layout routes where the coin FAB would cover the page's own primary action (web `hideFab`). */
fun NavDestination.hidesFab(): Boolean =
    hasRoute(Routes.Accounts::class) || hasRoute(Routes.Tools::class) ||
        hasRoute(Routes.Settings::class) || hasRoute(Routes.SettingsCategory::class)

/** The tab a destination highlights (web `isTabActive`): Home only on `/`; Settings highlights none. */
fun NavDestination.activeTab(): FinioTab? = when {
    hasRoute(Routes.Dashboard::class) -> FinioTab.Home
    hasRoute(Routes.Accounts::class) -> FinioTab.Accounts
    hasRoute(Routes.Transactions::class) -> FinioTab.Transactions
    hasRoute(Routes.Analytics::class) -> FinioTab.Analytics
    hasRoute(Routes.Tools::class) -> FinioTab.Tools
    else -> null
}
