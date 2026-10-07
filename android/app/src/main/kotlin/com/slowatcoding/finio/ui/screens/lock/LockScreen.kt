package com.slowatcoding.finio.ui.screens.lock

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.slowatcoding.finio.core.applock.FREE_ATTEMPTS
import com.slowatcoding.finio.core.applock.formatLockoutCountdown
import com.slowatcoding.finio.core.applock.remainingLockoutMs
import com.slowatcoding.finio.core.store.PinCheck
import com.slowatcoding.finio.di.appContainer
import com.slowatcoding.finio.platform.lock.BiometricUnlock
import com.slowatcoding.finio.platform.lock.findActivity
import com.slowatcoding.finio.ui.components.ButtonSize
import com.slowatcoding.finio.ui.components.ButtonVariant
import com.slowatcoding.finio.ui.components.FinioButton
import com.slowatcoding.finio.ui.components.FinioDialog
import com.slowatcoding.finio.ui.components.PadKey
import com.slowatcoding.finio.ui.components.PinDots
import com.slowatcoding.finio.ui.components.PinPad
import com.slowatcoding.finio.ui.icons.LucideIcons
import com.slowatcoding.finio.ui.theme.FinioShapes
import com.slowatcoding.finio.ui.theme.FinioTheme
import com.slowatcoding.finio.ui.theme.FinioType
import com.slowatcoding.finio.ui.theme.cssShadow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Port of web/src/components/applock/LockScreen.tsx — rendered by the shell *instead of* the app
 * while it is locked. It never navigates; the back stack (and any parked launch target) is intact
 * when it lifts. System back sends the task to the background rather than past the gate.
 *
 * No name greeting on purpose: it is the one piece of personal data that would otherwise be
 * readable without unlocking.
 */
@Composable
fun LockScreen() {
    val container = appContainer()
    val appLock = container.appLock
    val lockState by appLock.state.collectAsStateWithLifecycle()
    val auth by container.auth.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val scope = rememberCoroutineScope()
    val colors = FinioTheme.colors

    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var checking by remember { mutableStateOf(false) }
    var showForgot by remember { mutableStateOf(false) }
    /** Display only. The pad's disabled state comes from the store, never from this. */
    var cooldownMs by remember { mutableLongStateOf(0L) }

    val config = lockState.config
    val pinLength = config?.pinLength ?: 4
    val biometricAvailable = !config?.webauthnCredentialId.isNullOrEmpty() && BiometricUnlock.isAvailable(context)
    // Derived from the store, so there is no window where the pad is live but should not be.
    val inCooldown = lockState.lockedOutUntil != null

    BackHandler { activity?.moveTaskToBack(true) }

    // Live countdown; when the deadline passes it clears the store's lockout, re-enabling the pad.
    LaunchedEffect(lockState.lockedOutUntil) {
        val until = lockState.lockedOutUntil ?: return@LaunchedEffect
        while (true) {
            val remaining = remainingLockoutMs(until, System.currentTimeMillis())
            cooldownMs = remaining
            if (remaining <= 0) {
                // The "Incorrect PIN" that earned this cooldown is stale once it is served.
                error = null
                appLock.expireLockout()
                break
            }
            delay(250)
        }
    }

    fun succeed() {
        // Drop the stale backgrounded-at stamp so the next resume is measured from now.
        container.backgroundedAt.clear()
    }

    fun handleComplete(candidate: String) {
        if (config == null || checking || inCooldown) return
        checking = true
        scope.launch {
            // PBKDF2 — slow by design; keep it off the main thread.
            val result = withContext(Dispatchers.Default) { appLock.checkPin(candidate, System.currentTimeMillis()) }
            checking = false
            when (result) {
                PinCheck.Unlocked -> succeed()
                PinCheck.Wrong, PinCheck.LockedOut -> {
                    cooldownMs = remainingLockoutMs(appLock.current.lockedOutUntil, System.currentTimeMillis())
                    pin = ""
                    error = "Incorrect PIN"
                }
                PinCheck.NotConfigured -> Unit
            }
        }
    }

    fun handleBiometric() {
        val host = activity as? FragmentActivity ?: return
        if (config?.webauthnCredentialId.isNullOrEmpty() || checking || inCooldown) return
        checking = true
        scope.launch {
            val ok = BiometricUnlock.authenticate(host)
            checking = false
            // A cancelled or failed biometric is ordinary — say so gently and leave the pad working.
            if (ok) {
                succeed()
                appLock.unlock()
            } else {
                error = "Use your PIN instead"
            }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).widthIn(max = 384.dp).fillMaxWidth().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(
                    Modifier
                        .size(56.dp)
                        .cssShadow(FinioShapes.full, FinioTheme.shadows.glowPrimary)
                        .clip(FinioShapes.full)
                        .background(FinioTheme.brushes.gradPrimary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(LucideIcons.Lock, null, Modifier.size(24.dp), tint = Color.White)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Finio is locked",
                        Modifier.semantics { heading() },
                        style = FinioType.pageTitle,
                        color = colors.foreground,
                        textAlign = TextAlign.Center,
                    )
                    Text("Enter your PIN to continue.", style = FinioType.body, color = colors.mutedForeground, textAlign = TextAlign.Center)
                }
            }

            PinDots(filled = pin.length, total = pinLength, error = error != null, modifier = Modifier.fillMaxWidth())

            Box(
                Modifier.fillMaxWidth().heightIn(min = 40.dp).semantics { liveRegion = LiveRegionMode.Polite },
                contentAlignment = Alignment.Center,
            ) {
                when {
                    inCooldown -> StatusLine("Too many attempts · try again in ${formatLockoutCountdown(cooldownMs)}")
                    error != null -> {
                        val failed = lockState.failedAttempts
                        val left = if (failed in 1..FREE_ATTEMPTS) " · ${FREE_ATTEMPTS + 1 - failed} left" else ""
                        StatusLine("$error$left")
                    }
                    checking -> Text("Checking…", style = FinioType.body, color = colors.mutedForeground)
                }
            }

            PinPad(
                value = pin,
                onValueChange = {
                    if (error != null) error = null
                    pin = it
                },
                maxLength = pinLength,
                onComplete = ::handleComplete,
                enabled = !checking && !inCooldown,
                leadingAction = if (biometricAvailable) {
                    {
                        PadKey(
                            onClick = ::handleBiometric,
                            enabled = !checking && !inCooldown,
                            contentDescription = "Unlock with biometrics",
                        ) {
                            Icon(LucideIcons.Fingerprint, null, Modifier.size(22.dp), tint = colors.mutedForeground)
                        }
                    }
                } else null,
            )

            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                FinioButton("Forgot PIN?", onClick = { showForgot = true }, variant = ButtonVariant.Ghost, size = ButtonSize.Sm)
            }
        }
    }

    if (showForgot) {
        FinioDialog(
            onDismissRequest = { showForgot = false },
            title = "Forgot your PIN?",
            description = "Only a hash of your PIN is stored, so it cannot be recovered or reset from here.",
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    buildAnnotatedString {
                        append("The lock is a screen gate, not encryption — your data is still stored on this device. ")
                        append("The way back in is to clear Finio’s storage, which removes the lock ")
                        withStyle(SpanStyle(color = colors.foreground, fontWeight = FontWeight.SemiBold)) {
                            append("and every transaction on this device")
                        }
                        append(".")
                    },
                    style = FinioType.body,
                    color = colors.mutedForeground,
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Bullet("Settings → Apps → Finio → Storage → Clear storage")
                    Bullet("Or uninstall and reinstall Finio")
                }
                Text(
                    if (auth.isSignedIn) {
                        "Your cloud backup is unaffected — sign in afterwards and restore from it."
                    } else {
                        "If you have an exported backup file, you can restore from it afterwards."
                    },
                    style = FinioType.body,
                    color = colors.mutedForeground,
                )
            }
        }
    }
}

@Composable
private fun StatusLine(text: String) {
    val colors = FinioTheme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(LucideIcons.AlertTriangle, null, Modifier.size(15.dp), tint = colors.destructive)
        Text(text, style = FinioType.bodyMedium, color = colors.destructive)
    }
}

@Composable
private fun Bullet(text: String) {
    val colors = FinioTheme.colors
    Row(Modifier.padding(start = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("•", style = FinioType.body, color = colors.mutedForeground)
        Text(text, style = FinioType.body, color = colors.mutedForeground)
    }
}
