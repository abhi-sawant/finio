package com.slowatcoding.finio.ui.screens.auth

import androidx.compose.runtime.Composable
import com.slowatcoding.finio.ui.common.PlaceholderScreen
import com.slowatcoding.finio.ui.navigation.FinioNavigator

/**
 * PLACEHOLDER — port of web/src/pages/auth/ResetPassword.tsx — route `/reset-password`.
 *
 * Replace the body; keep the signature (the NavHost calls it).
 * @param email web `location.state.email`; null/blank → `nav.replace(Routes.ForgotPassword)`.
 */
@Composable
fun ResetPasswordScreen(nav: FinioNavigator, email: String?) {
    PlaceholderScreen("Reset password", nav)
}
