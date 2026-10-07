package com.slowatcoding.finio.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.slowatcoding.finio.core.util.getErrorMessage
import com.slowatcoding.finio.di.appContainer
import com.slowatcoding.finio.platform.api.UpdateProfileRequest
import com.slowatcoding.finio.ui.components.ButtonSize
import com.slowatcoding.finio.ui.components.ButtonVariant
import com.slowatcoding.finio.ui.components.FinioButton
import com.slowatcoding.finio.ui.components.FinioCard
import com.slowatcoding.finio.ui.components.FinioDialog
import com.slowatcoding.finio.ui.components.FinioDivider
import com.slowatcoding.finio.ui.components.FinioTextField
import com.slowatcoding.finio.ui.components.LocalConfirm
import com.slowatcoding.finio.ui.components.toast
import com.slowatcoding.finio.ui.icons.LucideIcons
import com.slowatcoding.finio.ui.navigation.FinioNavigator
import com.slowatcoding.finio.ui.navigation.Routes
import com.slowatcoding.finio.ui.screens.auth.toStoreUser
import com.slowatcoding.finio.ui.theme.FinioTheme
import com.slowatcoding.finio.ui.theme.FinioType
import kotlinx.coroutines.launch

/**
 * Port of web/src/components/settings/CloudAccountSection.tsx — the signed-in profile with
 * change password / sign out / delete cloud account, or a "Sign in" row when signed out.
 */
@Composable
fun CloudAccountSection(nav: FinioNavigator) {
    val container = appContainer()
    val authState = container.auth
    val auth by authState.state.collectAsStateWithLifecycle()
    val confirm = LocalConfirm.current
    val scope = rememberCoroutineScope()
    val colors = FinioTheme.colors

    var showChangePassword by remember { mutableStateOf(false) }
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var changingPassword by remember { mutableStateOf(false) }

    var showDeleteAccount by remember { mutableStateOf(false) }
    var deleteAccountPassword by remember { mutableStateOf("") }
    var deletingAccount by remember { mutableStateOf(false) }

    val token = auth.token?.takeIf { it.isNotEmpty() }
    val user = auth.user

    fun closeChangePassword() {
        showChangePassword = false
        currentPassword = ""
        newPassword = ""
        confirmPassword = ""
    }

    fun handleChangePassword() {
        if (newPassword.length < 8) {
            toast.error("New password must be at least 8 characters")
            return
        }
        if (newPassword != confirmPassword) {
            toast.error("New passwords do not match")
            return
        }
        val t = token ?: return
        changingPassword = true
        scope.launch {
            try {
                val res = container.api.updateProfile(
                    t,
                    UpdateProfileRequest(currentPassword = currentPassword, newPassword = newPassword),
                )
                authState.setAuth(res.token, res.user.toStoreUser())
                toast.success("Password changed")
                closeChangePassword()
            } catch (e: Exception) {
                toast.error(getErrorMessage(e, "Failed to change password"))
            } finally {
                changingPassword = false
            }
        }
    }

    fun handleDeleteCloudAccount() {
        val t = token ?: return
        if (deleteAccountPassword.isEmpty()) return
        deletingAccount = true
        scope.launch {
            try {
                container.api.deleteAccount(t, deleteAccountPassword)
                authState.clearAuth()
                showDeleteAccount = false
                deleteAccountPassword = ""
                toast.success("Cloud account and backups permanently deleted")
            } catch (e: Exception) {
                toast.error(getErrorMessage(e, "Failed to delete account"))
            } finally {
                deletingAccount = false
            }
        }
    }

    fun handleLogout() {
        scope.launch {
            val confirmed = confirm.confirm(
                title = "Sign out?",
                description = "Your finance data stays on this device — only cloud backup is disconnected.",
                confirmLabel = "Sign out",
            )
            if (confirmed) {
                authState.clearAuth()
                toast.success("Signed out")
            }
        }
    }

    FinioCard(contentPadding = PaddingValues(0.dp)) {
        if (token != null && user != null) {
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(LucideIcons.User, null, Modifier.size(18.dp), tint = colors.mutedForeground)
                Column(Modifier.weight(1f)) {
                    Text(user.name, style = FinioType.bodyMedium, color = colors.foreground, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(user.email, style = FinioType.caption, color = colors.mutedForeground, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            FinioDivider()
            SettingsRow(LucideIcons.KeyRound, "Change password", onClick = { showChangePassword = true })
            FinioDivider()
            SettingsRow(LucideIcons.LogOut, "Sign out", onClick = ::handleLogout, tone = RowTone.Destructive)
            FinioDivider()
            SettingsRow(LucideIcons.UserX, "Delete cloud account", onClick = { showDeleteAccount = true }, tone = RowTone.Destructive)
        } else {
            SettingsRow(
                LucideIcons.LogIn,
                "Sign in",
                subtitle = "Back up and restore your data",
                chevron = true,
                onClick = { nav.navigate(Routes.Login) },
            )
        }
    }

    if (showChangePassword) {
        FinioDialog(
            onDismissRequest = ::closeChangePassword,
            title = "Change password",
            description = "Sign in on other devices again after this.",
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PasswordField("Current password", currentPassword, { currentPassword = it })
                PasswordField("New password", newPassword, { newPassword = it }, placeholder = "Min 8 characters")
                PasswordField("Confirm new password", confirmPassword, { confirmPassword = it })
                FinioButton(
                    if (changingPassword) "Changing…" else "Change password",
                    onClick = ::handleChangePassword,
                    enabled = !changingPassword && currentPassword.isNotEmpty() && newPassword.isNotEmpty(),
                    size = ButtonSize.Lg,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    if (showDeleteAccount) {
        FinioDialog(
            onDismissRequest = {
                showDeleteAccount = false
                deleteAccountPassword = ""
            },
            title = "Delete cloud account?",
            description = "Your account and every backup on the server will be permanently deleted. " +
                "This cannot be undone. Finance data on this device is not affected.",
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PasswordField("Confirm password", deleteAccountPassword, { deleteAccountPassword = it })
                FinioButton(
                    if (deletingAccount) "Deleting…" else "Permanently delete account",
                    onClick = ::handleDeleteCloudAccount,
                    enabled = !deletingAccount && deleteAccountPassword.isNotEmpty(),
                    variant = ButtonVariant.Destructive,
                    size = ButtonSize.Lg,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** A labelled password input (`Label` + `Input type="password"`). */
@Composable
internal fun PasswordField(label: String, value: String, onValueChange: (String) -> Unit, placeholder: String? = null) {
    Column {
        // `Label className="mb-1.5 block text-xs font-medium"` — foreground, not muted.
        Text(label, Modifier.padding(bottom = 6.dp), style = FinioType.label, color = FinioTheme.colors.foreground)
        FinioTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = placeholder,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false),
        )
    }
}
