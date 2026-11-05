package com.slowatcoding.finio.core.store

import com.slowatcoding.finio.core.applock.nextLockoutUntil
import com.slowatcoding.finio.core.applock.remainingLockoutMs
import com.slowatcoding.finio.core.crypto.PIN_HASH_ITERATIONS
import com.slowatcoding.finio.core.crypto.PinRecord
import com.slowatcoding.finio.core.crypto.derivePinHash
import com.slowatcoding.finio.core.crypto.generateSalt
import com.slowatcoding.finio.core.crypto.pinRecord
import com.slowatcoding.finio.core.crypto.verifyPin
import com.slowatcoding.finio.core.js.toIso
import com.slowatcoding.finio.core.model.AppLockConfig
import com.slowatcoding.finio.core.model.UNHIDE_BIOMETRIC_MARKER
import com.slowatcoding.finio.core.model.UNHIDE_SOURCE_APPLOCK
import com.slowatcoding.finio.core.model.UNHIDE_SOURCE_OWN
import com.slowatcoding.finio.core.model.UnhidePinConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import java.time.Instant

// Port of web/src/store/useUnhidePinStore.ts (`finio-unhide-pin`) + utils/unhidePin.ts. Separate
// from the finance store for the same reason as the app lock: never exported, never uploaded.

@Serializable
data class PersistedUnhidePin(
    val config: UnhidePinConfig? = null,
    val failedAttempts: Int = 0,
    val lockedOutUntil: Long? = null,
)

data class UnhidePinUiState(
    val config: UnhidePinConfig? = null,
    val failedAttempts: Int = 0,
    val lockedOutUntil: Long? = null,
) {
    fun persisted() = PersistedUnhidePin(config, failedAttempts, lockedOutUntil)
}

/**
 * Whether unhiding is PIN-protected. An `applock`-sourced config is only as alive as the app lock
 * it points at; once that is off there is no PIN to ask for, so setup is offered again.
 */
fun isUnhideProtected(config: UnhidePinConfig?, appLock: AppLockConfig?): Boolean = when {
    config == null -> false
    config.source == UNHIDE_SOURCE_APPLOCK -> appLock?.enabled == true
    else -> config.hash.isNotEmpty()
}

/** Digits the unhide prompt collects. */
fun unhidePinLength(config: UnhidePinConfig, appLock: AppLockConfig?): Int =
    if (config.source == UNHIDE_SOURCE_APPLOCK) appLock?.pinLength ?: config.pinLength else config.pinLength

/** Blocking (PBKDF2). Never throws; false when the config points at an app lock that is off. */
fun verifyUnhidePin(pin: String, config: UnhidePinConfig, appLock: AppLockConfig?): Boolean {
    val record = if (config.source == UNHIDE_SOURCE_APPLOCK) {
        if (appLock?.enabled != true) return false
        appLock.pinRecord()
    } else {
        PinRecord(config.salt, config.hash, config.iterations)
    }
    return verifyPin(pin, record)
}

class UnhidePinState(private val persist: (PersistedUnhidePin) -> Unit = {}) {
    private val lock = Any()
    private val _state = MutableStateFlow(UnhidePinUiState())
    val state: StateFlow<UnhidePinUiState> = _state.asStateFlow()
    val current: UnhidePinUiState get() = _state.value

    private fun update(f: (UnhidePinUiState) -> UnhidePinUiState) = synchronized(lock) {
        val before = _state.value
        val after = f(before)
        _state.value = after
        if (after.persisted() != before.persisted()) persist(after.persisted())
    }

    fun hydrate(persisted: PersistedUnhidePin?) {
        val p = persisted ?: PersistedUnhidePin()
        synchronized(lock) { _state.value = UnhidePinUiState(p.config, p.failedAttempts, p.lockedOutUntil) }
    }

    fun setConfig(config: UnhidePinConfig) = update { it.copy(config = config, failedAttempts = 0, lockedOutUntil = null) }

    fun clearConfig() = update { it.copy(config = null, failedAttempts = 0, lockedOutUntil = null) }

    fun setAutoHideOnExit(enabled: Boolean) = update { it.copy(config = it.config?.copy(autoHideOnExit = enabled)) }

    fun setBiometricEnabled(enabled: Boolean) =
        update { it.copy(config = it.config?.copy(biometricCredentialId = if (enabled) UNHIDE_BIOMETRIC_MARKER else null)) }

    val isBiometricEnabled: Boolean get() = !current.config?.biometricCredentialId.isNullOrEmpty()

    fun registerFailure(now: Long) = update {
        val failedAttempts = it.failedAttempts + 1
        it.copy(failedAttempts = failedAttempts, lockedOutUntil = nextLockoutUntil(failedAttempts, now))
    }

    fun expireLockout() = update { it.copy(lockedOutUntil = null) }

    fun resetFailures() = update { it.copy(failedAttempts = 0, lockedOutUntil = null) }

    fun isLockedOut(now: Long): Boolean = remainingLockoutMs(current.lockedOutUntil, now) > 0

    /** Hash a PIN of the user's choosing (slow — call off the main thread) and store it. */
    fun setOwnPin(pin: String, autoHideOnExit: Boolean, now: Instant, salt: String = generateSalt(), biometric: Boolean = false) {
        setConfig(
            UnhidePinConfig(
                source = UNHIDE_SOURCE_OWN,
                salt = salt,
                hash = derivePinHash(pin, salt, PIN_HASH_ITERATIONS),
                iterations = PIN_HASH_ITERATIONS,
                pinLength = pin.length,
                autoHideOnExit = autoHideOnExit,
                biometricCredentialId = if (biometric) UNHIDE_BIOMETRIC_MARKER else null,
                createdAt = now.toIso(),
            ),
        )
    }

    /** Point the unhide PIN at the app lock's PIN. The caller has already verified it. */
    fun linkToAppLock(appLock: AppLockConfig, autoHideOnExit: Boolean, now: Instant, biometric: Boolean = false) {
        setConfig(
            UnhidePinConfig(
                source = UNHIDE_SOURCE_APPLOCK,
                salt = "",
                hash = "",
                iterations = 0,
                pinLength = appLock.pinLength,
                autoHideOnExit = autoHideOnExit,
                biometricCredentialId = if (biometric) UNHIDE_BIOMETRIC_MARKER else null,
                createdAt = now.toIso(),
            ),
        )
    }
}
