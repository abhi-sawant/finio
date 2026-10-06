package com.slowatcoding.finio.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.slowatcoding.finio.core.util.getErrorMessage
import com.slowatcoding.finio.di.appContainer
import com.slowatcoding.finio.ui.components.ButtonSize
import com.slowatcoding.finio.ui.components.ButtonVariant
import com.slowatcoding.finio.ui.components.FinioButton
import com.slowatcoding.finio.ui.components.toast
import com.slowatcoding.finio.ui.navigation.FinioNavigator
import com.slowatcoding.finio.ui.navigation.FinioTab
import com.slowatcoding.finio.ui.navigation.Routes
import com.slowatcoding.finio.ui.theme.FinioTheme
import com.slowatcoding.finio.ui.theme.FinioType
import kotlinx.coroutines.launch

/** Port of web/src/pages/auth/VerifyOtp.tsx — `/verify-otp`, with the address the OTP went to. */
@Composable
fun VerifyOtpScreen(nav: FinioNavigator, email: String) {
    if (email.isBlank()) {
        LaunchedEffect(Unit) { nav.replace(Routes.Register) }
        return
    }
    val container = appContainer()
    val scope = rememberCoroutineScope()
    val colors = FinioTheme.colors

    var otp by rememberSaveable { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var resending by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    fun handleSubmit() {
        if (loading) return
        if (otp.length != OTP_LENGTH) {
            toast.error("Please enter the full 6-digit OTP")
            return
        }
        loading = true
        scope.launch {
            try {
                val result = container.api.verifyOtp(email, otp)
                container.auth.setAuth(result.token, result.user.toStoreUser())
                toast.success("Email verified successfully!")
                nav.openTab(FinioTab.Home)
            } catch (e: Exception) {
                toast.error(getErrorMessage(e, "Verification failed"))
            } finally {
                loading = false
            }
        }
    }

    fun handleResend() {
        resending = true
        scope.launch {
            try {
                container.api.resendOtp(email)
                toast.success("New OTP sent to your email")
            } catch (e: Exception) {
                toast.error(getErrorMessage(e, "Failed to resend OTP"))
            } finally {
                resending = false
            }
        }
    }

    AuthShell(
        nav = nav,
        heading = "Verify your email",
        description = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Enter the 6-digit code sent to", style = FinioType.body, color = colors.mutedForeground, textAlign = TextAlign.Center)
                Text(email, style = FinioType.bodyMedium, color = colors.foreground, textAlign = TextAlign.Center)
            }
        },
        footer = {
            FinioButton(
                if (resending) "Sending…" else "Didn't receive the code? Resend",
                onClick = ::handleResend,
                enabled = !resending,
                variant = ButtonVariant.Link,
                size = ButtonSize.Sm,
            )
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
            OtpInput(otp, { otp = it }, Modifier.focusRequester(focus), onDone = ::handleSubmit)
            FinioButton(
                if (loading) "Verifying…" else "Verify",
                onClick = ::handleSubmit,
                enabled = !loading,
                size = ButtonSize.Lg,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
