package com.slowatcoding.finio.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.slowatcoding.finio.core.util.getErrorMessage
import com.slowatcoding.finio.core.util.isValidEmail
import com.slowatcoding.finio.core.util.jsTrim
import com.slowatcoding.finio.di.appContainer
import com.slowatcoding.finio.ui.components.ButtonSize
import com.slowatcoding.finio.ui.components.FinioButton
import com.slowatcoding.finio.ui.components.toast
import com.slowatcoding.finio.ui.icons.LucideIcons
import com.slowatcoding.finio.ui.navigation.FinioNavigator
import com.slowatcoding.finio.ui.navigation.Routes
import com.slowatcoding.finio.ui.theme.FinioTheme
import com.slowatcoding.finio.ui.theme.FinioType
import kotlinx.coroutines.launch

/** Port of web/src/pages/auth/ForgotPassword.tsx — `/forgot-password`. */
@Composable
fun ForgotPasswordScreen(nav: FinioNavigator) {
    val container = appContainer()
    val scope = rememberCoroutineScope()
    var email by rememberSaveable { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    fun handleSubmit() {
        if (loading) return
        if (email.isEmpty()) {
            toast.error("Please enter your email")
            return
        }
        if (!isValidEmail(email)) {
            toast.error("Enter a valid email")
            return
        }
        loading = true
        scope.launch {
            try {
                val trimmed = jsTrim(email)
                container.api.forgotPassword(trimmed)
                toast.success("If an account exists, an OTP has been sent.")
                nav.navigate(Routes.ResetPassword(trimmed))
            } catch (e: Exception) {
                toast.error(getErrorMessage(e, "Something went wrong"))
            } finally {
                loading = false
            }
        }
    }

    AuthShell(
        nav = nav,
        heading = "Forgot password",
        description = {
            Text(
                "Enter your email and we'll send you an OTP to reset your password.",
                style = FinioType.body,
                color = FinioTheme.colors.mutedForeground,
                textAlign = TextAlign.Center,
            )
        },
        footer = { AuthFooterLink("Remember your password?", "Sign in") { nav.navigate(Routes.Login) } },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            AuthField(
                email,
                { email = it },
                LucideIcons.Mail,
                "Email",
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Send,
                onImeAction = ::handleSubmit,
            )
            FinioButton(
                if (loading) "Sending…" else "Send OTP",
                onClick = ::handleSubmit,
                enabled = !loading,
                size = ButtonSize.Lg,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
