package com.slowatcoding.finio.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.slowatcoding.finio.core.util.getErrorMessage
import com.slowatcoding.finio.di.appContainer
import com.slowatcoding.finio.ui.components.ButtonSize
import com.slowatcoding.finio.ui.components.FinioButton
import com.slowatcoding.finio.ui.components.toast
import com.slowatcoding.finio.ui.navigation.FinioNavigator
import com.slowatcoding.finio.ui.navigation.Routes
import com.slowatcoding.finio.ui.theme.FinioTheme
import com.slowatcoding.finio.ui.theme.FinioType
import kotlinx.coroutines.launch

/** Port of web/src/pages/auth/ResetPassword.tsx — `/reset-password`, with the email from ForgotPassword. */
@Composable
fun ResetPasswordScreen(nav: FinioNavigator, email: String?) {
    if (email.isNullOrBlank()) {
        LaunchedEffect(Unit) { nav.replace(Routes.ForgotPassword) }
        return
    }
    val container = appContainer()
    val scope = rememberCoroutineScope()
    val colors = FinioTheme.colors

    var otp by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    val mismatch = confirmPassword.isNotEmpty() && confirmPassword != password

    fun handleSubmit() {
        if (loading) return
        if (otp.length != OTP_LENGTH) {
            toast.error("Please enter the full 6-digit OTP")
            return
        }
        if (password.length < 8) {
            toast.error("Password must be at least 8 characters")
            return
        }
        if (password != confirmPassword) {
            toast.error("Passwords do not match")
            return
        }
        loading = true
        scope.launch {
            try {
                container.api.resetPassword(email, otp, password)
                toast.success("Password reset successfully! Please sign in.")
                nav.replace(Routes.Login)
            } catch (e: Exception) {
                toast.error(getErrorMessage(e, "Reset failed"))
            } finally {
                loading = false
            }
        }
    }

    AuthShell(
        nav = nav,
        heading = "Reset password",
        description = {
            Text(
                buildAnnotatedString {
                    append("Enter the OTP sent to ")
                    withStyle(SpanStyle(color = colors.foreground, fontWeight = FontWeight.Medium)) { append(email) }
                    append(" and your new password.")
                },
                style = FinioType.body,
                color = colors.mutedForeground,
                textAlign = TextAlign.Center,
            )
        },
        footer = { AuthFooterLink("Remember your password?", "Sign in") { nav.navigate(Routes.Login) } },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Column {
                FormLabel("OTP code")
                OtpInput(otp, { otp = it })
            }
            Column {
                FormLabel("New password")
                PasswordAuthField(
                    password,
                    { password = it },
                    "Min 8 characters",
                    show = showPassword,
                    onToggleShow = { showPassword = !showPassword },
                )
            }
            Column {
                FormLabel("Confirm new password")
                PasswordAuthField(
                    confirmPassword,
                    { confirmPassword = it },
                    null,
                    show = showPassword,
                    onToggleShow = null,
                    imeAction = ImeAction.Done,
                    onImeAction = ::handleSubmit,
                    isError = mismatch,
                )
                if (mismatch) FieldError("Passwords do not match")
            }
            FinioButton(
                if (loading) "Resetting…" else "Reset password",
                onClick = ::handleSubmit,
                enabled = !loading,
                size = ButtonSize.Lg,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** `Label className="text-foreground mb-2 block text-sm font-medium"`. */
@Composable
private fun FormLabel(text: String) {
    Text(text, Modifier.padding(bottom = 8.dp), style = FinioType.bodyMedium, color = FinioTheme.colors.foreground)
}
