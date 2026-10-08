package com.slowatcoding.finio.ui.shell

import android.graphics.Color as AndroidColor
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.slowatcoding.finio.di.AppContainer
import com.slowatcoding.finio.di.LocalAppContainer
import com.slowatcoding.finio.platform.lock.SecureWindowEffect
import com.slowatcoding.finio.platform.lock.findActivity
import com.slowatcoding.finio.ui.common.PageLoader
import com.slowatcoding.finio.ui.components.ConfirmHost
import com.slowatcoding.finio.ui.components.FinioToastHost
import com.slowatcoding.finio.ui.components.UpdateDialog
import com.slowatcoding.finio.ui.mudra.PaperBackground
import com.slowatcoding.finio.ui.navigation.FinioNavigator
import com.slowatcoding.finio.ui.navigation.LocalFinioNavigator
import com.slowatcoding.finio.ui.navigation.buildFinioNavGraph
import com.slowatcoding.finio.ui.screens.lock.LockScreen
import com.slowatcoding.finio.ui.screens.onboarding.OnboardingScreen
import com.slowatcoding.finio.ui.theme.FinioTheme
import com.slowatcoding.finio.ui.theme.isDark

/**
 * The root of the composition — App.tsx's `App` + `AppRoutes`.
 *
 * Gates, in the web's order, each rendered INSTEAD of the app and none of them navigating:
 * hydration (page loader) → app lock → onboarding → the [AppShell]. Because nothing on that path
 * navigates, a launch target parked on the container and the restored back stack are both intact
 * when the gates lift. Lock before onboarding: a locked app must never show a wizard a stranger
 * could complete.
 *
 * The NavController, its graph and the screens' saveable state live HERE, above the gates, so a
 * lock → unlock round trip returns to the same screen with its state (the NavHost itself leaves
 * composition while locked, which also tears down any dialog a screen had open).
 *
 * Around everything: the theme (following `settings.theme`), system-bar icon contrast, FLAG_SECURE
 * while the lock is enabled, the paper background, the shared confirm dialog and the toaster.
 */
@Composable
fun FinioRoot(container: AppContainer) {
    val finance by container.financeStore.state.collectAsStateWithLifecycle()
    val hydrated by container.financeStore.isHydrated.collectAsStateWithLifecycle()
    val lock by container.appLock.state.collectAsStateWithLifecycle()

    val navController = rememberNavController()
    val navigator = remember(navController) { FinioNavigator(navController) }
    val graph = remember(navController) {
        buildFinioNavGraph(navController, navigator) { container.financeStore.current }
    }
    val screenState = rememberSaveableStateHolder()
    val shellState = remember { ShellState() }

    CompositionLocalProvider(
        LocalAppContainer provides container,
        LocalFinioNavigator provides navigator,
        LocalShellState provides shellState,
    ) {
        FinioTheme(finance.settings.theme, amoled = finance.settings.amoledDark) {
            SystemBarsEffect(dark = finance.settings.theme.isDark())
            SecureWindowEffect(enabled = lock.config?.enabled == true)
            ConfirmHost {
                PaperBackground {
                    when {
                        !hydrated || !lock.isReady -> PageLoader()
                        lock.isLocked -> LockScreen()
                        finance.settings.onboardedAt == null -> OnboardingScreen()
                        else -> screenState.SaveableStateProvider(SHELL_STATE_KEY) {
                            AppShell(navController, graph, navigator)
                            // Inside this branch so it can never draw over the lock screen.
                            val update by container.availableUpdate.collectAsStateWithLifecycle()
                            update?.let { release ->
                                UpdateDialog(
                                    release = release,
                                    onNotNow = container::dismissUpdate,
                                    onSkip = { container.skipUpdate(release) },
                                )
                            }
                        }
                    }
                    // Last, so toasts draw above the page (never give a wrapper a z-index).
                    FinioToastHost()
                }
            }
        }
    }
}

private const val SHELL_STATE_KEY = "finio-shell"

/** Light/dark status- and navigation-bar icons to match the theme; both bars stay transparent. */
@Composable
private fun SystemBarsEffect(dark: Boolean) {
    val context = LocalContext.current
    LaunchedEffect(dark, context) {
        val activity = context.findActivity() as? ComponentActivity ?: return@LaunchedEffect
        val style = if (dark) {
            SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        } else {
            SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
        }
        activity.enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }
}
