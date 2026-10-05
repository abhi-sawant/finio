package com.slowatcoding.finio.core.store

import com.slowatcoding.finio.core.applock.nextLockoutUntil
import com.slowatcoding.finio.core.applock.remainingLockoutMs
import com.slowatcoding.finio.core.applock.shouldLockOnResume
import com.slowatcoding.finio.core.crypto.PIN_HASH_ITERATIONS
import com.slowatcoding.finio.core.crypto.derivePinHash
import com.slowatcoding.finio.core.crypto.generateSalt
import com.slowatcoding.finio.core.crypto.pinRecord
import com.slowatcoding.finio.core.crypto.verifyPin
import com.slowatcoding.finio.core.js.toIso
import com.slowatcoding.finio.core.model.AppLockConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import java.time.Instant

// Port of web/src/store/useAppLockStore.ts (`finio-lock`). Deliberately separate from the finance
// store: the finance state is what gets exported and uploaded, and a PIN hash has no business in
// a portable payload. Only [PersistedAppLock] is ever written — the transient `isLocked` /
// `isReady` flags are decided fresh on every cold start (the web's `partialize`).

/** What is persisted — the web `partialize` allow-list. `lockedOutUntil` is epoch ms. */
@Serializable
data class PersistedAppLock(
    val config: AppLockConfig? = null,
    val failedAttempts: Int = 0,
    val lockedOutUntil: Long? = null,
)

data class AppLockUiState(
    /** Null when the lock has never been set up, or has been disabled. */
    val config: AppLockConfig? = null,
    val failedAttempts: Int = 0,
    val lockedOutUntil: Long? = null,
    val isLocked: Boolean = false,
    val isReady: Boolean = false,
) {
    fun persisted() = PersistedAppLock(config, failedAttempts, lockedOutUntil)
}

/** Value Android stores in `webauthnCredentialId` to mean "biometric unlock is on". */
const val ANDROID_BIOMETRIC_CREDENTIAL = "android-biometric"

enum class PinCheck { Unlocked, Wrong, LockedOut, NotConfigured }

/**
 * @param persist called with the persisted slice whenever it changes.
 * @param onLock platform side effects of locking — the web dismisses toasts and clears the
 *   backgrounded-at stamp so a restart can't begin inside a stale grace window.
 */
class AppLockState(
    private val persist: (PersistedAppLock) -> Unit = {},
    private val onLock: () -> Unit = {},
) {
    private val lock = Any()
    private val _state = MutableStateFlow(AppLockUiState())
    val state: StateFlow<AppLockUiState> = _state.asStateFlow()
    val current: AppLockUiState get() = _state.value

    private fun update(f: (AppLockUiState) -> AppLockUiState) = synchronized(lock) {
        val before = _state.value
        val after = f(before)
        _state.value = after
        if (after.persisted() != before.persisted()) persist(after.persisted())
    }

    /**
     * Cold start (the web's `onRehydrateStorage`): load what was persisted and decide the lock
     * before the first frame. Locked when the lock is enabled and [shouldLockOnResume] says so —
     * which fails closed when [backgroundedAt] is missing.
     */
    fun hydrate(persisted: PersistedAppLock?, backgroundedAt: Long?, now: Long) {
        val p = persisted ?: PersistedAppLock()
        synchronized(lock) {
            _state.value = AppLockUiState(p.config, p.failedAttempts, p.lockedOutUntil, isLocked = false, isReady = false)
        }
        val config = p.config
        val locked = config?.enabled == true && shouldLockOnResume(backgroundedAt, config.autoLockMinutes, now)
        setReady(true)
        if (locked) lock()
    }

    fun setConfig(config: AppLockConfig) = update { it.copy(config = config, failedAttempts = 0, lockedOutUntil = null) }

    fun clearConfig() = update { it.copy(config = null, failedAttempts = 0, lockedOutUntil = null, isLocked = false) }

    fun setAutoLockMinutes(minutes: Int) = update { it.copy(config = it.config?.copy(autoLockMinutes = minutes)) }

    fun setWebauthnCredentialId(id: String?) = update { it.copy(config = it.config?.copy(webauthnCredentialId = id)) }

    /** Android's biometric toggle, stored in the web's credential-id slot. */
    fun setBiometricEnabled(enabled: Boolean) = setWebauthnCredentialId(if (enabled) ANDROID_BIOMETRIC_CREDENTIAL else null)

    val isBiometricEnabled: Boolean get() = !current.config?.webauthnCredentialId.isNullOrEmpty()

    fun lock() {
        onLock()
        update { it.copy(isLocked = true) }
    }

    fun unlock() = update { it.copy(isLocked = false, failedAttempts = 0, lockedOutUntil = null) }

    fun registerFailure(now: Long) = update {
        val failedAttempts = it.failedAttempts + 1
        it.copy(failedAttempts = failedAttempts, lockedOutUntil = nextLockoutUntil(failedAttempts, now))
    }

    /** Cooldown served. Clears the deadline but keeps the count, so the ladder keeps escalating. */
    fun expireLockout() = update { it.copy(lockedOutUntil = null) }

    fun setReady(ready: Boolean) = update { it.copy(isReady = ready) }

    // ---- Conveniences over the web's LockScreen / AppLockSection flows ---------------------

    /** Hash a new PIN (PBKDF2, slow — call off the main thread) and enable the lock with it. */
    fun setPin(pin: String, autoLockMinutes: Int, now: Instant, salt: String = generateSalt()) {
        val hash = derivePinHash(pin, salt, PIN_HASH_ITERATIONS)
        setConfig(
            AppLockConfig(
                enabled = true,
                salt = salt,
                hash = hash,
                iterations = PIN_HASH_ITERATIONS,
                pinLength = pin.length,
                autoLockMinutes = autoLockMinutes,
                webauthnCredentialId = current.config?.webauthnCredentialId,
                createdAt = now.toIso(),
            ),
        )
    }

    /** Check a PIN as the lock screen does: refused during a cooldown, unlocks or counts a failure. */
    fun checkPin(pin: String, now: Long): PinCheck {
        val config = current.config ?: return PinCheck.NotConfigured
        if (remainingLockoutMs(current.lockedOutUntil, now) > 0) return PinCheck.LockedOut
        return if (verifyPin(pin, config.pinRecord())) {
            unlock()
            PinCheck.Unlocked
        } else {
            registerFailure(now)
            PinCheck.Wrong
        }
    }

    /** Turning the lock off forgets the PIN, the biometric flag and any cooldown. */
    fun disable() = clearConfig()
}
