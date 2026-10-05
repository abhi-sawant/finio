package com.slowatcoding.finio.ui.shell

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import com.slowatcoding.finio.di.appContainer
import com.slowatcoding.finio.platform.share.LaunchTarget
import com.slowatcoding.finio.ui.navigation.FinioNavigator
import com.slowatcoding.finio.ui.navigation.Routes
import com.slowatcoding.finio.ui.navigation.activeTab
import com.slowatcoding.finio.ui.navigation.hidesFab
import com.slowatcoding.finio.ui.navigation.isLayoutRoute
import com.slowatcoding.finio.ui.navigation.routeForPath

/**
 * The app once every gate has lifted — `<Routes>` plus Layout.tsx's chrome. One NavHost holds
 * every route; on layout routes (the tabs, Settings, Settings category) the glass tab bar floats
 * over the bottom, and on those that don't hide it the coin FAB floats above the bar.
 * Full-screen routes get neither. Screens pad their own bottoms (FinioMain's 160dp), so content
 * scrolls under the bar as on the web.
 *
 * The bar and the coin also step aside while the keyboard is up (an Android nicety — on the web
 * the browser keyboard simply covers them).
 *
 * Also consumes a parked [LaunchTarget] (share sheet, shortcut, notification) — this composable
 * only exists once the gates have lifted, which is exactly when the web's URL would start to
 * render.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppShell(navController: NavHostController, graph: NavGraph, navigator: FinioNavigator) {
    val container = appContainer()
    val shell = LocalShellState.current
    val entry by navController.currentBackStackEntryAsState()
    val destination = entry?.destination
    val imeVisible = WindowInsets.isImeVisible

    Box(Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            graph = graph,
            modifier = Modifier.fillMaxSize(),
            // The web swaps pages instantly; so do we.
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None },
        )

        if (destination != null && destination.isLayoutRoute() && !imeVisible) {
            if (!destination.hidesFab() && !shell.isFabSuppressed) {
                TemplatesFab(
                    onAdd = { navigator.navigate(Routes.AddTransaction()) },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(end = 16.dp, bottom = 88.dp),
                )
            }
            FinioTabBar(
                active = destination.activeTab(),
                onSelect = navigator::openTab,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }

    // Deep links wait here until the gates are down, then go exactly where they pointed.
    val pending by container.pendingLaunch.collectAsStateWithLifecycle()
    LaunchedEffect(pending, entry != null) {
        val target = pending ?: return@LaunchedEffect
        if (entry == null) return@LaunchedEffect // the graph isn't attached yet
        when (target) {
            is LaunchTarget.AddTransaction -> navigator.navigate(Routes.AddTransaction.from(target.draft))
            is LaunchTarget.Route -> navigator.navigate(routeForPath(target.path))
        }
        container.consumeLaunch(target)
    }
}
