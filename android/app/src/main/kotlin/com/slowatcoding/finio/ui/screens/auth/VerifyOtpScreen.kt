package com.slowatcoding.finio.ui.screens.auth

import androidx.compose.runtime.Composable
import com.slowatcoding.finio.ui.common.PlaceholderScreen
import com.slowatcoding.finio.ui.navigation.FinioNavigator

/**
 * PLACEHOLDER — port of web/src/pages/auth/VerifyOtp.tsx — route `/verify-otp`.
 *
 * Replace the body; keep the signature (the NavHost calls it).
 * @param email the address the OTP was sent to (web `location.state.email`); blank → `nav.replace(Routes.Register)`.
 */
@Composable
fun VerifyOtpScreen(nav: FinioNavigator, email: String) {
    PlaceholderScreen("Verify email", nav)
}
