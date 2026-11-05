package com.slowatcoding.finio.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.slowatcoding.finio.core.applock.formatLockoutCountdown
import com.slowatcoding.finio.core.applock.remainingLockoutMs
import com.slowatcoding.finio.core.crypto.PIN_LENGTH_OPTIONS
import com.slowatcoding.finio.core.crypto.isPinCryptoSupported
import com.slowatcoding.finio.core.crypto.pinRecord
import com.slowatcoding.finio.core.crypto.verifyPin
import com.slowatcoding.finio.core.js.nowInstant
import com.slowatcoding.finio.core.store.unhidePinLength
import com.slowatcoding.finio.core.store.verifyUnhidePin
import com.slowatcoding.finio.di.UnhideDialogKind
import com.slowatcoding.finio.di.UnhideDialogRequest
import com.slowatcoding.finio.di.UnhideIntent
import com.slowatcoding.finio.di.appContainer
import com.slowatcoding.finio.platform.lock.BiometricUnlock
import com.slowatcoding.finio.platform.lock.findActivity
import com.slowatcoding.finio.ui.components.PadSurface
import com.slowatcoding.finio.ui.components.PinDots
import com.slowatcoding.finio.ui.components.PinPad
import com.slowatcoding.finio.ui.components.SwitchField
import com.slowatcoding.finio.ui.components.toast
import com.slowatcoding.finio.ui.icons.LucideIcons
import com.slowatcoding.finio.ui.theme.FinioTheme
import com.slowatcoding.finio.ui.theme.FinioType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Port of web/src/components/UnhidePinDialogs.tsx. Hosted once from `FinioRoot` (inside the
 * unlocked branch); the eye button only sets `container.unhideDialog`.
 */
@Composable
fun UnhidePinDialogs() {
    val container = appContainer()
    val request by container.unhideDialog.collectAsStateWithLifecycle()
    when (request?.kind) {
        UnhideDialogKind.Setup -> UnhideSetupDialog(request!!.intent)
        UnhideDialogKind.Verify -> UnhideVerifyDialog(request!!.intent)
        null -> Unit
    }
}

/** Verify the PIN, then reveal amounts (or continue the Settings change / remove flow). */
@Composable
private fun UnhideVerifyDialog(intent: UnhideIntent) {
    val container = appContainer()
    val unhide = container.unhidePin
    val state by unhide.state.collectAsStateWithLifecycle()
    val appLockConfig by container.appLock.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val config = state.config ?: return

    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() as? FragmentActivity }
    val biometricReady = !config.biometricCredentialId.isNullOrEmpty() && activity != null &&
        remember(context) { BiometricUnlock.isAvailable(context) }

    // Biometrics go first when enrolled: the system prompt appears straight away and its
    // "Use PIN" button (or any failure) falls through to the keypad below.
    var tryingBiometric by remember { mutableStateOf(biometricReady) }
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }

    val remaining = remainingLockoutMs(state.lockedOutUntil, now)
    val lockedOut = remaining > 0
    LaunchedEffect(state.lockedOutUntil) {
        while (state.lockedOutUntil != null) {
            now = System.currentTimeMillis()
            if (remainingLockoutMs(state.lockedOutUntil, now) <= 0) unhide.expireLockout()
            delay(250)
        }
    }

    val length = unhidePinLength(config, appLockConfig.config)
    fun close() { container.unhideDialog.value = null }

    /** Shared ending for a verified PIN or biometric. */
    fun onVerified() {
        unhide.resetFailures()
        when (intent) {
            UnhideIntent.Change -> container.unhideDialog.value = UnhideDialogRequest(UnhideDialogKind.Setup, UnhideIntent.Change)
            UnhideIntent.Remove -> {
                unhide.clearConfig()
                close()
                toast.success("Unhide PIN removed")
            }
            UnhideIntent.Toggle -> {
                container.setAmountsHidden(false)
                close()
                toast.success("Amounts visible")
            }
        }
    }

    suspend fun runBiometric() {
        val host = activity ?: run { tryingBiometric = false; return }
        tryingBiometric = true
        val ok = BiometricUnlock.authenticate(
            host,
            title = if (intent == UnhideIntent.Toggle) "Show amounts" else "Confirm it’s you",
        )
        if (ok) onVerified() else tryingBiometric = false
    }

    LaunchedEffect(Unit) { if (biometricReady) runBiometric() }

    // The system prompt is the UI while it is up; nothing of ours is drawn behind it.
    if (tryingBiometric) return

    fun handleComplete(value: String) {
        if (busy || lockedOut) return
        busy = true
        scope.launch {
            val ok = withContext(Dispatchers.Default) { verifyUnhidePin(value, config, container.appLock.current.config) }
            busy = false
            pin = ""
            if (!ok) {
                unhide.registerFailure(System.currentTimeMillis())
                now = System.currentTimeMillis()
                error = "Incorrect PIN"
                return@launch
            }
            onVerified()
        }
    }

    SecretDialogShell(
        onDismissRequest = ::close,
        title = if (intent == UnhideIntent.Toggle) "Show amounts" else "Enter your PIN",
        description = if (intent == UnhideIntent.Toggle) "Enter your PIN to reveal the amounts." else "Enter your current unhide PIN to continue.",
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            PinDots(filled = pin.length, total = length, error = error != null, modifier = Modifier.fillMaxWidth())
            Box(Modifier.fillMaxWidth().defaultMinSize(minHeight = 20.dp), contentAlignment = Alignment.Center) {
                SecretDialogError(if (lockedOut) "Too many attempts. Try again in ${formatLockoutCountdown(remaining)}." else error)
            }
            PinPad(
                value = pin,
                onValueChange = {
                    if (error != null) error = null
                    pin = it
                },
                maxLength = length,
                onComplete = ::handleComplete,
                enabled = !busy && !lockedOut,
                surface = PadSurface.Card,
            )
            if (biometricReady) {
                LinkText("Use biometrics") { scope.launch { runBiometric() } }
            }
        }
    }
}

private enum class SetupStep { First, Second, AppLock }

/**
 * First-time setup: a PIN, its confirmation and the auto-hide switch on one screen. Finishing
 * saves the PIN and then performs the toggle the user originally asked for (`Toggle` intent).
 */
@Composable
private fun UnhideSetupDialog(intent: UnhideIntent) {
    val container = appContainer()
    val unhide = container.unhidePin
    val appLockState by container.appLock.state.collectAsStateWithLifecycle()
    val appLock = appLockState.config?.takeIf { it.enabled }
    val scope = rememberCoroutineScope()
    val colors = FinioTheme.colors

    var length by remember { mutableStateOf(4) }
    var first by remember { mutableStateOf("") }
    var second by remember { mutableStateOf("") }
    var appLockPin by remember { mutableStateOf("") }
    var step by remember { mutableStateOf(SetupStep.First) }
    var autoHide by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() as? FragmentActivity }
    val biometricAvailable = remember(context) { activity != null && BiometricUnlock.isAvailable(context) }
    var useBiometric by remember { mutableStateOf(true) }

    @Composable
    fun Options() {
        Column {
            if (biometricAvailable) {
                SwitchField(
                    title = "Unlock with biometrics",
                    description = "Use your fingerprint or face first. The PIN is always one tap away.",
                    checked = useBiometric,
                    onCheckedChange = { useBiometric = it },
                    icon = { Icon(LucideIcons.Fingerprint, null, Modifier.size(18.dp), tint = colors.mutedForeground) },
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
            SwitchField(
                title = "Hide amounts on exit",
                description = "Automatically hide amounts when you leave the app.",
                checked = autoHide,
                onCheckedChange = { autoHide = it },
                icon = { Icon(LucideIcons.EyeOff, null, Modifier.size(18.dp), tint = colors.mutedForeground) },
                modifier = Modifier.padding(vertical = 4.dp),
            )
        }
    }

    fun close() { container.unhideDialog.value = null }

    /** Android has no credential to register; confirming the enrolled biometric once is the equivalent. */
    suspend fun enrollBiometric(): Boolean {
        val host = activity ?: return false
        if (!biometricAvailable || !useBiometric) return false
        val ok = BiometricUnlock.authenticate(host, title = "Turn on biometrics")
        if (!ok) toast.error("Could not set up biometrics — you can turn them on later in Settings")
        return ok
    }

    fun finish(message: String) {
        if (intent == UnhideIntent.Toggle) {
            container.setAmountsHidden(!container.financeStore.current.settings.hideAmounts)
        }
        close()
        toast.success(message)
    }

    fun handleSecond(value: String) {
        if (busy) return
        if (value != first) {
            first = ""
            second = ""
            step = SetupStep.First
            error = "Those didn’t match. Try again."
            return
        }
        busy = true
        scope.launch {
            val biometric = enrollBiometric()
            withContext(Dispatchers.Default) { unhide.setOwnPin(value, autoHide, nowInstant(), biometric = biometric) }
            busy = false
            finish("Unhide PIN set")
        }
    }

    fun handleAppLockPin(value: String) {
        val lock = appLock ?: return
        if (busy) return
        busy = true
        scope.launch {
            val ok = withContext(Dispatchers.Default) { verifyPin(value, lock.pinRecord()) }
            busy = false
            appLockPin = ""
            if (!ok) {
                error = "Incorrect app lock PIN"
                return@launch
            }
            val biometric = enrollBiometric()
            unhide.linkToAppLock(lock, autoHide, nowInstant(), biometric)
            finish("Using your app lock PIN to show amounts")
        }
    }

    SecretDialogShell(
        onDismissRequest = ::close,
        title = if (step == SetupStep.AppLock) "Use app lock PIN" else "Set a PIN to show amounts",
        description = if (step == SetupStep.AppLock) {
            "Enter your app lock PIN to confirm."
        } else {
            "Hiding amounts is always one tap. Showing them again will ask for this PIN. " +
                "It is a screen privacy gate, not encryption."
        },
    ) {
        if (!isPinCryptoSupported()) {
            Text("Needs a secure connection (https or localhost).", style = FinioType.caption, color = colors.mutedForeground)
        } else if (step == SetupStep.AppLock) {
            val total = appLock?.pinLength ?: 4
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                PinDots(filled = appLockPin.length, total = total, error = error != null, modifier = Modifier.fillMaxWidth())
                Box(Modifier.fillMaxWidth().defaultMinSize(minHeight = 20.dp), contentAlignment = Alignment.Center) { SecretDialogError(error) }
                PinPad(
                    value = appLockPin,
                    onValueChange = {
                        if (error != null) error = null
                        appLockPin = it
                    },
                    maxLength = total,
                    onComplete = ::handleAppLockPin,
                    enabled = !busy,
                    surface = PadSurface.Card,
                )
                Options()
                LinkText("Choose a separate PIN instead") {
                    error = null
                    step = SetupStep.First
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                val locked = first.isNotEmpty() || step == SetupStep.Second
                ChoiceGrid(PIN_LENGTH_OPTIONS, columns = 2, gap = 8.dp) { l, mod ->
                    ChoicePill("$l digits", selected = length == l, onClick = { if (!locked) length = l }, modifier = mod, verticalPadding = 8.dp)
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        if (step == SetupStep.First) "Enter PIN" else "Confirm PIN",
                        style = FinioType.caption,
                        color = colors.foreground,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    PinDots(filled = first.length, total = length, error = error != null, modifier = Modifier.fillMaxWidth())
                    PinDots(filled = second.length, total = length, error = error != null, modifier = Modifier.fillMaxWidth())
                }
                Box(Modifier.fillMaxWidth().defaultMinSize(minHeight = 20.dp), contentAlignment = Alignment.Center) { SecretDialogError(error) }
                PinPad(
                    value = if (step == SetupStep.First) first else second,
                    onValueChange = {
                        if (error != null) error = null
                        if (step == SetupStep.First) first = it else second = it
                    },
                    maxLength = length,
                    onComplete = { v ->
                        if (step == SetupStep.First) {
                            first = v
                            step = SetupStep.Second
                        } else {
                            handleSecond(v)
                        }
                    },
                    enabled = !busy,
                    surface = PadSurface.Card,
                )
                Options()
                if (appLock != null) {
                    LinkText("Use my app lock PIN instead") {
                        error = null
                        first = ""
                        second = ""
                        step = SetupStep.AppLock
                    }
                }
            }
        }
    }
}

@Composable
private fun LinkText(text: String, onClick: () -> Unit) {
    val colors = FinioTheme.colors
    Box(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 4.dp), contentAlignment = Alignment.Center) {
        Text(text, style = FinioType.caption, color = colors.primary, textDecoration = TextDecoration.Underline)
    }
}
