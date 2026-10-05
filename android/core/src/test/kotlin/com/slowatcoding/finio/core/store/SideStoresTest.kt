package com.slowatcoding.finio.core.store

import com.slowatcoding.finio.core.crypto.deriveEncryptionKey
import com.slowatcoding.finio.core.data.defaultSettings
import com.slowatcoding.finio.core.model.AppLockConfig
import com.slowatcoding.finio.core.model.BackupCryptoConfig
import com.slowatcoding.finio.core.model.FinioJson
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Ports of web/src/store/useAppLockStore.test.ts and useBackupCryptoStore.test.ts, plus the auth
 * store (which has no web suite).
 */
class SideStoresTest {
    private val now = java.time.Instant.parse("2026-06-15T12:00:00.000Z").toEpochMilli()

    private fun lockConfig(webauthn: String? = null, autoLockMinutes: Int = 1) = AppLockConfig(
        enabled = true, salt = "AAAAAAAAAAAAAAAAAAAAAA", hash = "BBBBBBBBBBBBBBBBBBBBBB", iterations = 1000,
        pinLength = 4, autoLockMinutes = autoLockMinutes, webauthnCredentialId = webauthn, createdAt = "2026-06-01T00:00:00.000Z",
    )

    private val written = mutableListOf<PersistedAppLock>()
    private var lockSideEffects = 0
    private val lock = AppLockState(persist = { written += it }, onLock = { lockSideEffects++ }).also {
        it.hydrate(null, backgroundedAt = null, now = now)
    }

    // ---- lock and unlock ---------------------------------------------------------------------

    @Test fun lockFlipsTheTransientFlag() {
        lock.lock()
        assertTrue(lock.current.isLocked)
        lock.unlock()
        assertFalse(lock.current.isLocked)
    }

    @Test fun lockRunsThePlatformSideEffects() {
        lock.lock()
        assertEquals(1, lockSideEffects)
    }

    @Test fun unlockClearsFailuresAndCooldown() {
        repeat(6) { lock.registerFailure(now) }
        lock.unlock()
        assertEquals(0, lock.current.failedAttempts)
        assertNull(lock.current.lockedOutUntil)
    }

    @Test fun firstFailuresCountWithoutACooldown() {
        lock.registerFailure(now)
        lock.registerFailure(now)
        assertEquals(2, lock.current.failedAttempts)
        assertNull(lock.current.lockedOutUntil)
    }

    @Test fun fifthFailureStartsTheCooldown() {
        repeat(5) { lock.registerFailure(now) }
        assertEquals(5, lock.current.failedAttempts)
        assertEquals(now + 15_000, lock.current.lockedOutUntil)
    }

    @Test fun expireLockoutKeepsTheCount() {
        repeat(5) { lock.registerFailure(now) }
        lock.expireLockout()
        assertNull(lock.current.lockedOutUntil)
        assertEquals(5, lock.current.failedAttempts)
    }

    @Test fun persistsTheConfigButNeverTheTransientFlags() {
        lock.setConfig(lockConfig())
        lock.lock()
        val json = FinioJson.encodeToJsonElement(written.last()).jsonObject
        assertEquals(true, written.last().config?.enabled)
        assertFalse("isLocked" in json)
        assertFalse("isReady" in json)
    }

    @Test fun keepsTheFailureCountAcrossAReload() {
        lock.registerFailure(now)
        assertEquals(1, written.last().failedAttempts)
        val reloaded = AppLockState().apply { hydrate(written.last(), backgroundedAt = null, now = now) }
        assertEquals(1, reloaded.current.failedAttempts)
    }

    @Test fun clearConfigDropsPinBiometricAndCooldown() {
        lock.setConfig(lockConfig(webauthn = "cred-1"))
        lock.registerFailure(now)
        lock.clearConfig()
        assertNull(lock.current.config)
        assertEquals(0, lock.current.failedAttempts)
        assertNull(lock.current.lockedOutUntil)
        assertFalse(lock.current.isLocked)
    }

    @Test fun updatesTheAutoLockDelayInPlace() {
        lock.setConfig(lockConfig())
        lock.setAutoLockMinutes(15)
        assertEquals(15, lock.current.config?.autoLockMinutes)
    }

    @Test fun forgetsTheBiometricFlagWithoutTouchingThePin() {
        lock.setConfig(lockConfig(webauthn = "cred-1"))
        lock.setWebauthnCredentialId(null)
        assertNull(lock.current.config?.webauthnCredentialId)
        assertEquals(lockConfig().hash, lock.current.config?.hash)
        lock.setBiometricEnabled(true)
        assertTrue(lock.isBiometricEnabled)
    }

    @Test fun configMutatorsDoNothingWithoutAConfig() {
        lock.setAutoLockMinutes(15)
        assertNull(lock.current.config)
    }

    @Test fun lockFieldsStayOutOfSettings() {
        val keys = FinioJson.encodeToJsonElement(defaultSettings).jsonObject.keys
        assertTrue(keys.none { Regex("pin|lock", RegexOption.IGNORE_CASE).containsMatchIn(it) })
    }

    @Test fun coldStartLocksWhenEnabledAndTheGraceWindowIsOver() {
        val p = PersistedAppLock(lockConfig(autoLockMinutes = 1))
        val fresh = AppLockState().apply { hydrate(p, backgroundedAt = null, now = now) }
        assertTrue(fresh.current.isReady)
        assertTrue(fresh.current.isLocked)
        val withinGrace = AppLockState().apply { hydrate(p, backgroundedAt = now - 30_000, now = now) }
        assertFalse(withinGrace.current.isLocked)
        val disabled = AppLockState().apply { hydrate(PersistedAppLock(lockConfig().copy(enabled = false)), null, now) }
        assertFalse(disabled.current.isLocked)
    }

    @Test fun checkPinUnlocksCountsFailuresAndHonoursTheCooldown() {
        val s = AppLockState()
        s.setPin("1234", autoLockMinutes = 5, now = java.time.Instant.ofEpochMilli(now), salt = "AAAAAAAAAAAAAAAAAAAAAA")
        s.lock()
        assertEquals(PinCheck.Wrong, s.checkPin("0000", now))
        assertEquals(1, s.current.failedAttempts)
        assertEquals(PinCheck.Unlocked, s.checkPin("1234", now))
        assertFalse(s.current.isLocked)
        repeat(5) { s.registerFailure(now) }
        assertEquals(PinCheck.LockedOut, s.checkPin("1234", now + 1000))
        assertEquals(PinCheck.NotConfigured, AppLockState().checkPin("1234", now))
    }

    // ---- backup crypto -----------------------------------------------------------------------

    private fun cryptoConfig() = BackupCryptoConfig(
        enabled = true, salt = "AAAAAAAAAAAAAAAAAAAAAA", iterations = 1000, verifierIv = "BBBBBBBBBBBB",
        verifierCiphertext = "CCCCCCCCCCCC", createdAt = "2026-06-01T00:00:00.000Z",
    )

    private val cryptoWritten = mutableListOf<PersistedBackupCrypto>()
    private val crypto = BackupCryptoState { cryptoWritten += it }
    private val key by lazy { deriveEncryptionKey("passphrase", "AAAAAAAAAAAAAAAAAAAAAA", 1000) }

    @Test fun storesACryptoConfig() {
        crypto.setConfig(cryptoConfig())
        assertEquals(true, crypto.current.config?.enabled)
    }

    @Test fun clearConfigDropsTheConfigAndSessionKey() {
        crypto.setConfig(cryptoConfig())
        crypto.setSessionKey(key, cryptoConfig().salt)
        crypto.clearConfig()
        assertNull(crypto.current.config)
        assertNull(crypto.current.sessionKey)
        assertNull(crypto.current.sessionKeySalt)
    }

    @Test fun cachesAKeyWithItsSalt() {
        crypto.setSessionKey(key, "some-salt")
        assertSame(key, crypto.current.sessionKey)
        assertEquals("some-salt", crypto.current.sessionKeySalt)
        crypto.setSessionKey(null, null)
        assertNull(crypto.current.sessionKey)
        assertNull(crypto.current.sessionKeySalt)
    }

    @Test fun persistsTheConfigButNeverTheSessionKey() {
        crypto.setConfig(cryptoConfig())
        crypto.setSessionKey(key, cryptoConfig().salt)
        val json = FinioJson.encodeToJsonElement(cryptoWritten.last()).jsonObject
        assertEquals(setOf("config"), json.keys)
        assertEquals(1, cryptoWritten.size)
    }

    @Test fun everySessionStartsWithoutACachedKey() {
        val reloaded = BackupCryptoState().apply { hydrate(PersistedBackupCrypto(cryptoConfig())) }
        assertNull(reloaded.current.sessionKey)
        assertTrue(reloaded.isLocked)
        reloaded.setSessionKey(key, cryptoConfig().salt)
        assertFalse(reloaded.isLocked)
        assertSame(key, reloaded.cachedKey(cryptoConfig().salt))
        assertNull(reloaded.cachedKey("rotated-salt"))
    }

    @Test fun cryptoFieldsStayOutOfSettings() {
        val keys = FinioJson.encodeToJsonElement(defaultSettings).jsonObject.keys
        assertTrue(keys.none { Regex("salt|cipher|verifier|passphrase", RegexOption.IGNORE_CASE).containsMatchIn(it) })
    }

    // ---- auth --------------------------------------------------------------------------------

    @Test fun authLifecycle() {
        val writes = mutableListOf<PersistedAuth>()
        val auth = AuthState { writes += it }
        auth.hydrate(null)
        assertTrue(auth.current.isLoaded)
        assertFalse(auth.current.isSignedIn)
        auth.setAuth("jwt", AuthUser(1, "Asha", "asha@example.com"))
        auth.setLastBackupAt("2026-06-15T12:00:00.000Z")
        assertTrue(auth.current.isSignedIn)
        assertEquals(PersistedAuth("jwt", AuthUser(1, "Asha", "asha@example.com"), "2026-06-15T12:00:00.000Z"), writes.last())
        auth.clearAuth()
        assertEquals(PersistedAuth(), writes.last())
        val reloaded = AuthState().apply { hydrate(PersistedAuth("t", null, "x")) }
        assertEquals("t", reloaded.current.token)
        assertTrue(reloaded.current.isLoaded)
    }
}
