package com.slowatcoding.finio.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import com.slowatcoding.finio.core.util.MAX_NAME_LENGTH
import com.slowatcoding.finio.core.util.cleanText
import com.slowatcoding.finio.core.util.getErrorMessage
import com.slowatcoding.finio.core.util.isValidEmail
import com.slowatcoding.finio.core.util.jsTrim
import com.slowatcoding.finio.core.util.stripLeading
import com.slowatcoding.finio.di.appContainer
import com.slowatcoding.finio.ui.components.ButtonSize
import com.slowatcoding.finio.ui.components.FinioButton
import com.slowatcoding.finio.ui.components.FinioCheckbox
import com.slowatcoding.finio.ui.components.toast
import com.slowatcoding.finio.ui.icons.LucideIcons
import com.slowatcoding.finio.ui.navigation.FinioNavigator
import com.slowatcoding.finio.ui.navigation.Routes
import com.slowatcoding.finio.ui.theme.FinioTheme
import com.slowatcoding.finio.ui.theme.FinioType
import kotlinx.coroutines.launch

/** Port of web/src/pages/auth/Register.tsx — `/register`. */
@Composable
fun RegisterScreen(nav: FinioNavigator) {
    val container = appContainer()
    val scope = rememberCoroutineScope()
    val colors = FinioTheme.colors

    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    var agreed by rememberSaveable { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }

    val mismatch = confirmPassword.isNotEmpty() && confirmPassword != password

    fun handleSubmit() {
        if (loading) return
        if (jsTrim(name).isEmpty() || email.isEmpty() || password.isEmpty()) {
            toast.error("Please fill in all fields")
            return
        }
        if (!isValidEmail(email)) {
            toast.error("Enter a valid email")
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
        if (!agreed) {
            toast.error("Please confirm your age and agree to the Terms of Service and Privacy Policy")
            return
        }
        loading = true
        scope.launch {
            try {
                val trimmedEmail = jsTrim(email)
                container.api.register(cleanText(name, MAX_NAME_LENGTH), trimmedEmail, password)
                toast.success("Account created! Check your email for the OTP.")
                nav.navigate(Routes.VerifyOtp(trimmedEmail))
            } catch (e: Exception) {
                toast.error(getErrorMessage(e, "Registration failed"))
            } finally {
                loading = false
            }
        }
    }

    AuthShell(
        nav = nav,
        heading = "Create your account",
        footer = { AuthFooterLink("Already have an account?", "Sign in") { nav.navigate(Routes.Login) } },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            AuthField(
                name,
                { name = stripLeading(it).take(MAX_NAME_LENGTH) },
                LucideIcons.User,
                "Name",
                capitalization = KeyboardCapitalization.Words,
            )
            AuthField(email, { email = it }, LucideIcons.Mail, "Email", keyboardType = KeyboardType.Email)
            PasswordAuthField(
                password,
                { password = it },
                "Password (min 8 characters)",
                show = showPassword,
                onToggleShow = { showPassword = !showPassword },
            )
            Column {
                PasswordAuthField(
                    confirmPassword,
                    { confirmPassword = it },
                    "Confirm password",
                    show = showPassword,
                    onToggleShow = null,
                    imeAction = ImeAction.Done,
                    isError = mismatch,
                )
                if (mismatch) FieldError("Passwords do not match")
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                // The checkbox brings its own 8dp hit-area padding; pull it back into line.
                FinioCheckbox(checked = agreed, onCheckedChange = { agreed = it }, modifier = Modifier.offset(x = (-8).dp, y = (-6).dp))
                val link = TextLinkStyles(SpanStyle(color = colors.primary))
                Text(
                    buildAnnotatedString {
                        append("I confirm I am at least 16 years old and agree to the ")
                        withLink(LinkAnnotation.Clickable("terms", link) { nav.navigate(Routes.Terms) }) { append("Terms of Service") }
                        append(" and ")
                        withLink(LinkAnnotation.Clickable("privacy", link) { nav.navigate(Routes.Privacy) }) { append("Privacy Policy") }
                    },
                    Modifier.weight(1f).offset(x = (-16).dp),
                    style = FinioType.body,
                    color = colors.mutedForeground,
                )
            }

            FinioButton(
                if (loading) "Creating account…" else "Sign up",
                onClick = ::handleSubmit,
                enabled = !loading && agreed,
                size = ButtonSize.Lg,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
