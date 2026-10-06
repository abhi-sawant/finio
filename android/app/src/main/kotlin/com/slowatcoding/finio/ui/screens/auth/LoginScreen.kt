package com.slowatcoding.finio.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.slowatcoding.finio.core.util.getErrorMessage
import com.slowatcoding.finio.core.util.isValidEmail
import com.slowatcoding.finio.core.util.jsTrim
import com.slowatcoding.finio.di.appContainer
import com.slowatcoding.finio.ui.components.ButtonSize
import com.slowatcoding.finio.ui.components.ButtonVariant
import com.slowatcoding.finio.ui.components.FinioButton
import com.slowatcoding.finio.ui.components.toast
import com.slowatcoding.finio.ui.icons.LucideIcons
import com.slowatcoding.finio.ui.navigation.FinioNavigator
import com.slowatcoding.finio.ui.navigation.FinioTab
import com.slowatcoding.finio.ui.navigation.Routes
import kotlinx.coroutines.launch

/** Port of web/src/pages/auth/Login.tsx — `/login`. */
@Composable
fun LoginScreen(nav: FinioNavigator) {
    val container = appContainer()
    val scope = rememberCoroutineScope()

    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }

    fun handleSubmit() {
        if (loading) return
        if (email.isEmpty() || password.isEmpty()) {
            toast.error("Please fill in all fields")
            return
        }
        if (!isValidEmail(email)) {
            toast.error("Enter a valid email")
            return
        }
        loading = true
        scope.launch {
            try {
                val result = container.api.login(jsTrim(email), password)
                container.auth.setAuth(result.token, result.user.toStoreUser())
                toast.success("Logged in successfully")
                nav.openTab(FinioTab.Home)
            } catch (e: Exception) {
                toast.error(getErrorMessage(e, "Login failed"))
            } finally {
                loading = false
            }
        }
    }

    AuthShell(
        nav = nav,
        heading = "Sign in to your account",
        footer = { AuthFooterLink("Don't have an account?", "Sign up") { nav.navigate(Routes.Register) } },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            AuthField(email, { email = it }, LucideIcons.Mail, "Email", keyboardType = KeyboardType.Email)
            PasswordAuthField(
                password,
                { password = it },
                "Password",
                show = showPassword,
                onToggleShow = { showPassword = !showPassword },
                imeAction = ImeAction.Go,
                onImeAction = ::handleSubmit,
            )
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                FinioButton(
                    "Forgot password?",
                    onClick = { nav.navigate(Routes.ForgotPassword) },
                    variant = ButtonVariant.Link,
                    size = ButtonSize.Sm,
                )
            }
            FinioButton(
                if (loading) "Signing in…" else "Sign in",
                onClick = ::handleSubmit,
                enabled = !loading,
                size = ButtonSize.Lg,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
