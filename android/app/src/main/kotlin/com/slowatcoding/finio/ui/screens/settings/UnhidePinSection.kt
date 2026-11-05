package com.slowatcoding.finio.ui.screens.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.fragment.app.FragmentActivity
import com.slowatcoding.finio.platform.lock.BiometricUnlock
import com.slowatcoding.finio.platform.lock.findActivity
import com.slowatcoding.finio.ui.components.toast
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.slowatcoding.finio.core.crypto.isPinCryptoSupported
import com.slowatcoding.finio.core.model.UNHIDE_SOURCE_APPLOCK
import com.slowatcoding.finio.core.store.isUnhideProtected
import com.slowatcoding.finio.di.UnhideDialogKind
import com.slowatcoding.finio.di.UnhideDialogRequest
import com.slowatcoding.finio.di.UnhideIntent
import com.slowatcoding.finio.di.appContainer
import com.slowatcoding.finio.ui.components.FinioCard
import com.slowatcoding.finio.ui.components.FinioDivider
import com.slowatcoding.finio.ui.components.SwitchField
import com.slowatcoding.finio.ui.icons.LucideIcons
import com.slowatcoding.finio.ui.theme.FinioTheme
import com.slowatcoding.finio.ui.theme.FinioType

/** Port of web/src/components/settings/UnhidePinSection.tsx — Settings → Security. */
@Composable
fun UnhidePinSection() {
    if (!isPinCryptoSupported()) return
    val container = appContainer()
    val state by container.unhidePin.state.collectAsStateWithLifecycle()
    val lock by container.appLock.state.collectAsStateWithLifecycle()
    val colors = FinioTheme.colors
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() as? FragmentActivity }
    val biometricAvailable = remember(context) { activity != null && BiometricUnlock.isAvailable(context) }
    val scope = rememberCoroutineScope()

    fun handleToggleBiometric(next: Boolean) {
        if (!next) {
            container.unhidePin.setBiometricEnabled(false)
            toast.success("Biometric unhide off")
            return
        }
        val host = activity ?: return
        scope.launch {
            if (BiometricUnlock.authenticate(host, title = "Turn on biometrics")) {
                container.unhidePin.setBiometricEnabled(true)
                toast.success("Biometric unhide is on")
            } else {
                toast.error("Could not set up biometrics")
            }
        }
    }
    val config = state.config
    val protectedNow = isUnhideProtected(config, lock.config)

    fun open(kind: UnhideDialogKind, intent: UnhideIntent) {
        container.unhideDialog.value = UnhideDialogRequest(kind, intent)
    }

    FinioCard(contentPadding = PaddingValues(0.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text("Amount privacy", style = FinioType.body, color = colors.foreground)
            Text(
                when {
                    !protectedNow || config == null -> "Set a PIN so hidden amounts cannot be revealed with one tap."
                    config.source == UNHIDE_SOURCE_APPLOCK -> "Showing hidden amounts asks for your app lock PIN."
                    else -> "Showing hidden amounts asks for a PIN."
                },
                style = FinioType.caption,
                color = colors.mutedForeground,
            )
        }
        FinioDivider()
        if (protectedNow && config != null) {
            if (biometricAvailable) {
                SwitchField(
                    title = "Unlock with biometrics",
                    description = "Use your fingerprint or face first when showing amounts. The PIN always still works.",
                    checked = state.config?.biometricCredentialId?.isNotEmpty() == true,
                    onCheckedChange = ::handleToggleBiometric,
                    icon = { Icon(LucideIcons.Fingerprint, null, Modifier.size(18.dp), tint = colors.mutedForeground) },
                    modifier = Modifier.padding(16.dp),
                )
                FinioDivider()
            }
            SwitchField(
                title = "Hide amounts on exit",
                description = "Automatically hide amounts when you leave the app.",
                checked = config.autoHideOnExit,
                onCheckedChange = container.unhidePin::setAutoHideOnExit,
                icon = { Icon(LucideIcons.EyeOff, null, Modifier.size(18.dp), tint = colors.mutedForeground) },
                modifier = Modifier.padding(16.dp),
            )
            FinioDivider()
            SettingsRow(LucideIcons.KeyRound, "Change unhide PIN", chevron = true, onClick = { open(UnhideDialogKind.Verify, UnhideIntent.Change) })
            FinioDivider()
            SettingsRow(LucideIcons.Trash2, "Remove unhide PIN", tone = RowTone.Destructive, onClick = { open(UnhideDialogKind.Verify, UnhideIntent.Remove) })
        } else {
            SettingsRow(LucideIcons.KeyRound, "Set unhide PIN", chevron = true, onClick = { open(UnhideDialogKind.Setup, UnhideIntent.Change) })
        }
    }
}
