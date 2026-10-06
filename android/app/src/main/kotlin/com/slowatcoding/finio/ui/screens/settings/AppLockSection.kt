package com.slowatcoding.finio.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.slowatcoding.finio.core.applock.AUTO_LOCK_OPTIONS
import com.slowatcoding.finio.core.applock.DEFAULT_AUTO_LOCK_MINUTES
import com.slowatcoding.finio.core.applock.autoLockLabel
import com.slowatcoding.finio.core.crypto.PIN_HASH_ITERATIONS
import com.slowatcoding.finio.core.crypto.PIN_LENGTH_OPTIONS
import com.slowatcoding.finio.core.crypto.derivePinHash
import com.slowatcoding.finio.core.crypto.generateSalt
import com.slowatcoding.finio.core.crypto.isPinCryptoSupported
import com.slowatcoding.finio.core.crypto.pinRecord
import com.slowatcoding.finio.core.crypto.verifyPin
import com.slowatcoding.finio.core.js.nowInstant
import com.slowatcoding.finio.core.js.toIso
import com.slowatcoding.finio.core.model.AppLockConfig
import com.slowatcoding.finio.di.appContainer
import com.slowatcoding.finio.platform.lock.BiometricUnlock
import com.slowatcoding.finio.platform.lock.findActivity
import com.slowatcoding.finio.ui.components.ButtonSize
import com.slowatcoding.finio.ui.components.FinioButton
import com.slowatcoding.finio.ui.components.FinioCard
import com.slowatcoding.finio.ui.components.FinioDialog
import com.slowatcoding.finio.ui.components.FinioDivider
import com.slowatcoding.finio.ui.components.PadSurface
import com.slowatcoding.finio.ui.components.PinDots
import com.slowatcoding.finio.ui.components.PinPad
import com.slowatcoding.finio.ui.components.SwitchField
import com.slowatcoding.finio.ui.components.toast
import com.slowatcoding.finio.ui.icons.LucideIcons
import com.slowatcoding.finio.ui.theme.FinioTheme
import com.slowatcoding.finio.ui.theme.FinioType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class LockDialogKind { Set, Change, Disable }
private enum class PinPhase { Length, Current, Enter, Confirm }

/**
 * Port of web/src/components/settings/AppLockSection.tsx — the app lock switch (set / change /
 * disable PIN in one phase-machine dialog), auto-lock delay, biometric unlock and "Lock now".
 * The copy stays honest: this is a screen gate, not encryption.
 */
@Composable
fun AppLockSection() {
    val container = appContainer()
    val appLock = container.appLock
    val lockState by appLock.state.collectAsStateWithLifecycle()
    val auth by container.auth.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val scope = rememberCoroutineScope()
    val colors = FinioTheme.colors

    val lockConfig = lockState.config
    val lockEnabled = lockConfig?.enabled == true
    val token = auth.token?.takeIf { it.isNotEmpty() }

    val biometricAvailable = remember(context) { BiometricUnlock.isAvailable(context) }
    var lockDialog by remember { mutableStateOf<LockDialogKind?>(null) }
    var showAutoLockPicker by remember { mutableStateOf(false) }
    var pinPhase by remember { mutableStateOf(PinPhase.Length) }
    var pinLength by remember { mutableIntStateOf(4) }
    var pinEntry by remember { mutableStateOf("") }
    var firstPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }
    var pinBusy by remember { mutableStateOf(false) }

    val handleExport = rememberBackupExport()

    fun closeLockDialog() {
        lockDialog = null
        pinPhase = PinPhase.Length
        pinEntry = ""
        firstPin = ""
        pinError = null
        pinBusy = false
    }

    fun openLockDialog(which: LockDialogKind) {
        pinEntry = ""
        firstPin = ""
        pinError = null
        pinLength = lockConfig?.pinLength ?: 4
        pinPhase = if (which == LockDialogKind.Set) PinPhase.Length else PinPhase.Current
        lockDialog = which
    }

    fun savePin(pin: String) {
        pinBusy = true
        val wasChanging = lockDialog == LockDialogKind.Change
        val previous = appLock.current.config
        scope.launch {
            val salt = generateSalt()
            val hash = withContext(Dispatchers.Default) { derivePinHash(pin, salt, PIN_HASH_ITERATIONS) }
            appLock.setConfig(
                AppLockConfig(
                    enabled = true,
                    salt = salt,
                    hash = hash,
                    iterations = PIN_HASH_ITERATIONS,
                    pinLength = pin.length,
                    autoLockMinutes = previous?.autoLockMinutes ?: DEFAULT_AUTO_LOCK_MINUTES,
                    webauthnCredentialId = previous?.webauthnCredentialId,
                    createdAt = previous?.createdAt ?: nowInstant().toIso(),
                ),
            )
            pinBusy = false
            closeLockDialog()
            toast.success(if (wasChanging) "PIN changed" else "App lock is on")
        }
    }

    fun handlePinComplete(value: String) {
        if (pinBusy) return
        pinError = null
        when (pinPhase) {
            PinPhase.Current -> {
                val config = lockConfig ?: return
                pinBusy = true
                scope.launch {
                    val ok = withContext(Dispatchers.Default) { verifyPin(value, config.pinRecord()) }
                    pinBusy = false
                    pinEntry = ""
                    if (!ok) {
                        // No backoff ladder here — the app is already unlocked, so this guards a
                        // settings change rather than the lock itself.
                        pinError = "Incorrect PIN"
                        return@launch
                    }
                    if (lockDialog == LockDialogKind.Disable) {
                        appLock.clearConfig()
                        container.backgroundedAt.clear()
                        closeLockDialog()
                        toast.success("App lock is off")
                        return@launch
                    }
                    pinPhase = PinPhase.Enter
                }
            }
            PinPhase.Enter -> {
                firstPin = value
                pinEntry = ""
                pinPhase = PinPhase.Confirm
            }
            PinPhase.Confirm -> {
                if (value != firstPin) {
                    // Back to entry, keeping the chosen length.
                    pinEntry = ""
                    pinPhase = PinPhase.Enter
                    pinError = "Those didn't match. Try again."
                    return
                }
                savePin(value)
            }
            PinPhase.Length -> Unit
        }
    }

    fun handleToggleBiometric(next: Boolean) {
        if (!next) {
            appLock.setBiometricEnabled(false)
            toast.success("Biometric unlock off")
            return
        }
        // The web registers a passkey here; Android confirms the enrolled biometric once instead.
        val host = activity as? FragmentActivity
        if (host == null) {
            toast.error("Could not set up biometric unlock")
            return
        }
        scope.launch {
            if (BiometricUnlock.authenticate(host)) {
                appLock.setBiometricEnabled(true)
                toast.success("Biometric unlock is on")
            } else {
                toast.error("Could not set up biometric unlock")
            }
        }
    }

    FinioCard(contentPadding = PaddingValues(0.dp)) {
        if (!isPinCryptoSupported()) {
            SettingsValueRow(LucideIcons.Lock, "App lock", subtitle = "Needs a secure connection (https or localhost).") {}
        } else {
            SwitchField(
                title = "App lock",
                description = "Ask for a PIN before opening Finio. Data on this device is not encrypted.",
                checked = lockEnabled,
                // Both directions need input, so the switch opens a dialog rather than writing state.
                onCheckedChange = { next -> openLockDialog(if (next) LockDialogKind.Set else LockDialogKind.Disable) },
                icon = { Icon(LucideIcons.Lock, null, Modifier.size(18.dp), tint = colors.mutedForeground) },
                modifier = Modifier.padding(16.dp),
            )
            if (lockEnabled) {
                FinioDivider()
                SettingsRow(LucideIcons.KeyRound, "Change PIN", chevron = true, onClick = { openLockDialog(LockDialogKind.Change) })
                FinioDivider()
                val minutes = lockConfig?.autoLockMinutes ?: 0
                SettingsValueRow(LucideIcons.Timer, "Auto-lock", subtitle = "${autoLockLabel(minutes)} in the background") {
                    PillButton(
                        if (minutes == 0) "Now" else "${minutes}m",
                        onClick = { showAutoLockPicker = true },
                        contentDescription = "Change auto-lock delay",
                    )
                }
                if (biometricAvailable) {
                    FinioDivider()
                    SwitchField(
                        title = "Unlock with biometrics",
                        description = "Use your fingerprint or face instead of typing your PIN. The PIN always still works.",
                        checked = !lockConfig?.webauthnCredentialId.isNullOrEmpty(),
                        onCheckedChange = ::handleToggleBiometric,
                        icon = { Icon(LucideIcons.Fingerprint, null, Modifier.size(18.dp), tint = colors.mutedForeground) },
                        modifier = Modifier.padding(16.dp),
                    )
                }
                FinioDivider()
                SettingsRow(LucideIcons.LockKeyhole, "Lock now", onClick = { appLock.lock() })
            }
        }
    }

    if (showAutoLockPicker) {
        FinioDialog(
            onDismissRequest = { showAutoLockPicker = false },
            title = "Auto-lock",
            description = "How long Finio can sit in the background before it asks for your PIN again.",
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                AUTO_LOCK_OPTIONS.forEach { minutes ->
                    ChoicePill(
                        autoLockLabel(minutes),
                        selected = minutes == lockConfig?.autoLockMinutes,
                        onClick = {
                            appLock.setAutoLockMinutes(minutes)
                            showAutoLockPicker = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        verticalPadding = 10.dp,
                        horizontalPadding = 16.dp,
                        alignStart = true,
                    )
                }
            }
        }
    }

    val dialog = lockDialog
    if (dialog != null) {
        SecretDialogShell(
            onDismissRequest = ::closeLockDialog,
            title = when (dialog) {
                LockDialogKind.Disable -> "Turn off app lock"
                LockDialogKind.Change -> "Change PIN"
                LockDialogKind.Set -> "Set a PIN"
            },
            description = when {
                dialog == LockDialogKind.Disable ->
                    "Enter your current PIN to remove the lock. This also forgets any biometric unlock."
                pinPhase == PinPhase.Current -> "Enter your current PIN first."
                pinPhase == PinPhase.Confirm -> "Enter it once more to confirm."
                pinPhase == PinPhase.Enter -> "Choose a PIN you will remember — it cannot be recovered."
                else -> "Finio will ask for this PIN when you open it. This is a screen lock, not encryption: " +
                    "your data stays stored unencrypted on this device."
            },
        ) {
            if (pinPhase == PinPhase.Length) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    ChoiceGrid(PIN_LENGTH_OPTIONS, columns = 2, gap = 8.dp) { length, mod ->
                        ChoicePill("$length digits", selected = pinLength == length, onClick = { pinLength = length }, modifier = mod, verticalPadding = 12.dp)
                    }
                    if (token == null) {
                        Text(
                            buildAnnotatedString {
                                append("A forgotten PIN can’t be recovered. ")
                                withLink(
                                    LinkAnnotation.Clickable(
                                        "export",
                                        TextLinkStyles(SpanStyle(color = colors.primary, textDecoration = TextDecoration.Underline)),
                                    ) { handleExport() },
                                ) { append("Export a backup first?") }
                            },
                            style = FinioType.caption,
                            color = colors.mutedForeground,
                        )
                    }
                    FinioButton("Continue", onClick = { pinPhase = PinPhase.Enter }, size = ButtonSize.Lg, modifier = Modifier.fillMaxWidth())
                }
            } else {
                val total = if (pinPhase == PinPhase.Current) lockConfig?.pinLength ?: 4 else pinLength
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    PinDots(filled = pinEntry.length, total = total, error = pinError != null, modifier = Modifier.fillMaxWidth())
                    Box(Modifier.fillMaxWidth().defaultMinSize(minHeight = 20.dp), contentAlignment = Alignment.Center) {
                        SecretDialogError(pinError)
                    }
                    PinPad(
                        value = pinEntry,
                        onValueChange = {
                            if (pinError != null) pinError = null
                            pinEntry = it
                        },
                        maxLength = total,
                        onComplete = ::handlePinComplete,
                        enabled = !pinBusy,
                        surface = PadSurface.Card,
                    )
                }
            }
        }
    }
}
