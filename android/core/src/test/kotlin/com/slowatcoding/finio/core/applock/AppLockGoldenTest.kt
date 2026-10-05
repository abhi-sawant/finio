package com.slowatcoding.finio.core.applock

import com.slowatcoding.finio.core.golden.Golden
import com.slowatcoding.finio.core.golden.d
import com.slowatcoding.finio.core.golden.dOrNull
import com.slowatcoding.finio.core.golden.i
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.add
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLockGoldenTest {
    @Test
    fun matchesTypeScript() = Golden.verify("appLock") { c ->
        when (c.fn) {
            "constants" -> buildJsonObject {
                putJsonArray("AUTO_LOCK_OPTIONS") { AUTO_LOCK_OPTIONS.forEach { add(it) } }
                put("DEFAULT_AUTO_LOCK_MINUTES", DEFAULT_AUTO_LOCK_MINUTES)
                put("FREE_ATTEMPTS", FREE_ATTEMPTS)
            }
            "shouldLockOnResume" -> c.arg(0).jsonObject.let {
                JsonPrimitive(shouldLockOnResume(it.getValue("backgroundedAt").dOrNull?.toLong(), it.getValue("autoLockMinutes").i, it.getValue("now").d.toLong()))
            }
            "penaltyForAttempts" -> JsonPrimitive(penaltyForAttempts(c.arg(0).i))
            "nextLockoutUntil" -> nextLockoutUntil(c.arg(0).i, c.arg(1).d.toLong())?.let(::JsonPrimitive) ?: JsonNull
            "remainingLockoutMs" -> JsonPrimitive(remainingLockoutMs(c.arg(0).dOrNull?.toLong(), c.arg(1).d.toLong()))
            "formatLockoutCountdown" -> JsonPrimitive(formatLockoutCountdown(c.arg(0).d.toLong()))
            "autoLockLabel" -> JsonPrimitive(autoLockLabel(c.arg(0).i))
            else -> null
        }
    }

    @Test
    fun defaultDelayIsAnOfferedOption() {
        assertTrue(DEFAULT_AUTO_LOCK_MINUTES > 0)
        assertTrue(DEFAULT_AUTO_LOCK_MINUTES in AUTO_LOCK_OPTIONS)
    }

    @Test
    fun penaltyNeverDecreases() {
        var previous = 0L
        for (i in 0..50) {
            val p = penaltyForAttempts(i)
            assertTrue(p >= previous)
            previous = p
        }
        assertEquals(300_000L, penaltyForAttempts(Int.MAX_VALUE))
    }
}
