package com.slowatcoding.finio.ui.navigation

import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController

/**
 * The only navigation API screens use — they never touch the NavController. The web equivalents:
 *
 * | web                                   | here                          |
 * | ------------------------------------- | ----------------------------- |
 * | `navigate('/budgets')`                | `navigate(Routes.Budgets)`    |
 * | `navigate(-1)`                        | `back()`                      |
 * | tab bar `navigate(tab.path)`          | `openTab(FinioTab.Accounts)`  |
 * | `<Navigate to="/x" replace />`        | `replace(Routes.X)`           |
 * | `navigate(action.to)` (a path string) | `navigateToPath("/budgets")`  |
 *
 * The Dashboard is always the bottom of the stack, so `back()` from any pushed screen lands
 * somewhere — deep links and launch targets are pushed on top of it.
 */
@Stable
class FinioNavigator(private val navController: NavHostController) {

    /** Push [route] (a no-op if it is already on top). */
    fun navigate(route: FinioRoute) {
        if (route.isTabRoute()) {
            openTab(FinioTab.entries.first { it.route == route })
            return
        }
        navController.navigate(route) { launchSingleTop = true }
    }

    /** Pop the current screen. Returns false (and does nothing) on the root Dashboard. */
    fun back(): Boolean {
        if (navController.previousBackStackEntry == null) return false
        return navController.popBackStack()
    }

    /**
     * Switch tabs: back to the Dashboard root, then the tab on top. Tabs open fresh (no saved
     * per-tab state), like the web's `navigate(tab.path)`; back from any tab returns Home.
     */
    fun openTab(tab: FinioTab) {
        navController.navigate(tab.route) {
            popUpTo(Routes.Dashboard) { inclusive = false }
            launchSingleTop = true
        }
    }

    /**
     * Replace the current screen with [route] (`<Navigate replace>`): pop this one, then show
     * [route] unless it is what is now on top (e.g. a stale edit id falling back to its list).
     */
    fun replace(route: FinioRoute) {
        if (navController.previousBackStackEntry != null) navController.popBackStack()
        val top = navController.currentBackStackEntry?.destination
        // An object route already on top is the same screen; a data-class route (with an id)
        // is always navigated to, since its arguments may differ.
        if (top != null && route.isObject() && top.hasRoute(route::class)) return
        navigate(route)
    }

    /** Navigate to a web-style in-app path, e.g. an insight's `action.to` or a notification URL. */
    fun navigateToPath(path: String) = navigate(routeForPath(path))

    private fun FinioRoute.isTabRoute() = FinioTab.entries.any { it.route == this }

    /** A Kotlin `object` route (no arguments) — without needing kotlin-reflect. */
    private fun FinioRoute.isObject() = runCatching { javaClass.getField("INSTANCE") }.isSuccess
}

/** The navigator, for components deep inside a screen. Screens receive it as a parameter. */
val LocalFinioNavigator = staticCompositionLocalOf<FinioNavigator> {
    error("LocalFinioNavigator used outside the app shell")
}
