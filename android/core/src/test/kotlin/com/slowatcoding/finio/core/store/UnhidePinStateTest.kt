package com.slowatcoding.finio.core.store

import com.slowatcoding.finio.core.data.defaultSettings
import com.slowatcoding.finio.core.model.AppLockConfig
import com.slowatcoding.finio.core.model.FinioJson
import com.slowatcoding.finio.core.model.ImportMode
import com.slowatcoding.finio.core.model.ImportPayload
import com.slowatcoding.finio.core.model.UNHIDE_SOURCE_APPLOCK
import com.slowatcoding.finio.core.model.UNHIDE_SOURCE_OWN
import com.slowatcoding.finio.core.model.UnhidePinConfig
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/** Port of web/src/store/useUnhidePinStore.test.ts. */
class UnhidePinStateTest {
    private val now = Instant.parse("2026-06-15T12:00:00.000Z")
    private val nowMs = now.toEpochMilli()

    private fun own(autoHide: Boolean = true) = UnhidePinConfig(
        source = UNHIDE_SOURCE_OWN, salt = "AAAAAAAAAAAAAAAAAAAAAA", hash = "BBBBBBBBBBBBBBBBBBBBBB",
        iterations = 1000, pinLength = 4, autoHideOnExit = autoHide, createdAt = "2026-06-01T00:00:00.000Z",
    )

    private fun linked() = own().copy(source = UNHIDE_SOURCE_APPLOCK, salt = "", hash = "", iterations = 0)

    private fun lock(enabled: Boolean = true) = AppLockConfig(
        enabled = enabled, salt = "AAAAAAAAAAAAAAAAAAAAAA", hash = "BBBBBBBBBBBBBBBBBBBBBB", iterations = 1000,
        pinLength = 4, autoLockMinutes = 1, createdAt = "2026-06-01T00:00:00.000Z",
    )

    private val written = mutableListOf<PersistedUnhidePin>()
    private val state = UnhidePinState { written += it }

    @Test fun setConfigClearsFailures() {
        state.registerFailure(nowMs)
        state.setConfig(own())
        assertEquals(0, state.current.failedAttempts)
        assertNull(state.current.lockedOutUntil)
        assertEquals(UNHIDE_SOURCE_OWN, state.current.config?.source)
    }

    @Test fun cooldownStartsAfterTheFreeAttempts() {
        repeat(4) { state.registerFailure(nowMs) }
        assertNull(state.current.lockedOutUntil)
        state.registerFailure(nowMs)
        assertNotNull(state.current.lockedOutUntil)
        assertTrue(state.isLockedOut(nowMs))
        state.expireLockout()
        assertEquals(5, state.current.failedAttempts)
        assertFalse(state.isLockedOut(nowMs))
    }

    @Test fun autoHideChangesInPlaceAndIgnoresNoConfig() {
        state.setAutoHideOnExit(false)
        assertNull(state.current.config)
        state.setConfig(own())
        state.setAutoHideOnExit(false)
        assertFalse(state.current.config!!.autoHideOnExit)
    }

    @Test fun biometricFlagTogglesInPlace() {
        state.setBiometricEnabled(true)
        assertNull(state.current.config)
        state.setConfig(own())
        assertFalse(state.isBiometricEnabled)
        state.setBiometricEnabled(true)
        assertTrue(state.isBiometricEnabled)
        state.setBiometricEnabled(false)
        assertNull(state.current.config!!.biometricCredentialId)
        state.setOwnPin("1234", autoHideOnExit = true, now = now, biometric = true)
        assertTrue(state.isBiometricEnabled)
    }

    @Test fun persistsAndHydratesAcrossARestart() {
        state.setConfig(own())
        val reloaded = UnhidePinState().apply { hydrate(written.last()) }
        assertEquals(own(), reloaded.current.config)
    }

    @Test fun clearConfigForgetsEverything() {
        state.setConfig(own())
        state.registerFailure(nowMs)
        state.clearConfig()
        assertNull(state.current.config)
        assertEquals(0, state.current.failedAttempts)
    }

    @Test fun protectedFollowsTheAppLockForLinkedConfigs() {
        assertFalse(isUnhideProtected(null, lock()))
        assertTrue(isUnhideProtected(own(), null))
        assertTrue(isUnhideProtected(linked(), lock()))
        assertFalse(isUnhideProtected(linked(), lock(enabled = false)))
        assertFalse(isUnhideProtected(linked(), null))
    }

    @Test fun verifiesOwnAndLinkedPins() {
        state.setOwnPin("1234", autoHideOnExit = true, now = now)
        val mine = state.current.config!!
        assertTrue(verifyUnhidePin("1234", mine, null))
        assertFalse(verifyUnhidePin("9999", mine, null))

        val lockState = AppLockState().apply { setPin("5678", 5, now) }
        val appLock = lockState.current.config!!
        val link = UnhidePinState().apply { linkToAppLock(appLock, true, now) }.current.config!!
        assertTrue(verifyUnhidePin("5678", link, appLock))
        assertFalse(verifyUnhidePin("1234", link, appLock))
        assertFalse(verifyUnhidePin("5678", link, appLock.copy(enabled = false)))
        assertFalse(verifyUnhidePin("5678", link, null))
    }

    @Test fun keepsPinMaterialOutOfSettings() {
        val keys = FinioJson.encodeToJsonElement(defaultSettings).jsonObject.keys
        assertTrue(keys.none { Regex("pin|lock|unhide", RegexOption.IGNORE_CASE).containsMatchIn(it) })
    }

    @Test fun importCannotRevealAmountsWhilePinned() {
        val store = FinanceStore()
        store.updateSettings { it.copy(hideAmounts = true) }
        store.keepAmountsHidden = { true }
        store.importData(ImportPayload(settings = defaultSettings.copy(hideAmounts = false)), ImportMode.Replace)
        assertTrue(store.current.settings.hideAmounts)
    }
}
