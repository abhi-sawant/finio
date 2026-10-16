package com.slowatcoding.finio.platform.lock

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.slowatcoding.finio.R
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/*
 * Port of web/src/services/appLockBiometric.ts — a *convenience* unlock layered on the PIN, never
 * a replacement for it. The web uses a WebAuthn platform authenticator with user verification;
 * the closest Android equivalent is a Class 3 (BIOMETRIC_STRONG) prompt. Device credential is
 * deliberately NOT allowed: the device PIN is not the Finio PIN, and the fallback is our own pad
 * (the negative button, "Use PIN").
 *
 * There is no credential id to store on Android — `AppLockConfig.webauthnCredentialId` can hold a
 * marker such as "android-biometric" when the user enables it.
 */
object BiometricUnlock {

    /** Strong biometrics are present and enrolled. */
    fun isAvailable(context: Context): Boolean =
        BiometricManager.from(context).canAuthenticate(BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS

    /** Hardware exists but nothing is enrolled — Settings can say "set up a fingerprint first". */
    fun needsEnrollment(context: Context): Boolean =
        BiometricManager.from(context).canAuthenticate(BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED

    /**
     * Show the prompt; true on success, false on cancel / "Use PIN" / lockout / error. Must be
     * called with the activity at least STARTED. Requires a [FragmentActivity] — androidx
     * BiometricPrompt hosts itself in a fragment.
     *
     * A single failed match does not end the prompt (the system lets the user retry); only a
     * terminal error resolves false.
     */
    suspend fun authenticate(
        activity: FragmentActivity,
        title: String = activity.getString(R.string.biometric_unlock_title),
        subtitle: String? = activity.getString(R.string.biometric_unlock_subtitle),
        negativeButton: String = activity.getString(R.string.biometric_unlock_negative),
    ): Boolean = suspendCancellableCoroutine { cont ->
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    if (cont.isActive) cont.resume(true)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (cont.isActive) cont.resume(false)
                }
            },
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .apply { if (subtitle != null) setSubtitle(subtitle) }
            .setNegativeButtonText(negativeButton)
            .setAllowedAuthenticators(BIOMETRIC_STRONG)
            .setConfirmationRequired(false)
            .build()
        cont.invokeOnCancellation { runCatching { prompt.cancelAuthentication() } }
        prompt.authenticate(info)
    }
}
