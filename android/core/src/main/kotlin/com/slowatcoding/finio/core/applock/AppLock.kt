package com.slowatcoding.finio.core.applock

import kotlin.math.ceil

// Port of web/src/utils/appLock.ts. Pure decision logic for the app lock: when to re-lock on
// resume, and how long to make someone wait after repeated wrong PINs. Everything takes `now`
// (epoch ms) explicitly. Times are Long epoch ms, so the TS `!Number.isFinite` guards have no
// Kotlin counterpart — a non-finite timestamp is unrepresentable here.

/** Auto-lock delays offered in Settings, in minutes. 0 means the moment the app is hidden. */
val AUTO_LOCK_OPTIONS: List<Int> = listOf(0, 1, 5, 15, 60)

const val DEFAULT_AUTO_LOCK_MINUTES = 5

/** Wrong PINs tolerated before any cooldown starts. */
const val FREE_ATTEMPTS = 4

/** Cooldown ladder in ms, applied from the 5th failure onward; the last value is the cap. */
private val PENALTY_LADDER_MS = longArrayOf(15_000, 30_000, 60_000, 120_000, 300_000)

/**
 * Whether returning to a backgrounded app should demand the PIN again. Fails closed in every
 * ambiguous case: no timestamp (iOS often skips `pagehide`; Android may kill the process), a
 * clock that moved backwards, or a zero delay.
 */
fun shouldLockOnResume(backgroundedAt: Long?, autoLockMinutes: Int, now: Long): Boolean {
    if (backgroundedAt == null) return true
    if (backgroundedAt > now) return true
    if (autoLockMinutes <= 0) return true
    return now - backgroundedAt >= autoLockMinutes * 60_000L
}

/** Cooldown in ms earned by [failedAttempts] consecutive wrong PINs. 0 below the threshold. */
fun penaltyForAttempts(failedAttempts: Int): Long {
    if (failedAttempts <= FREE_ATTEMPTS) return 0
    val index = minOf(failedAttempts - FREE_ATTEMPTS - 1, PENALTY_LADDER_MS.size - 1)
    return PENALTY_LADDER_MS[index]
}

/** Epoch ms the pad becomes usable again, or null when no cooldown has been earned. */
fun nextLockoutUntil(failedAttempts: Int, now: Long): Long? {
    val penalty = penaltyForAttempts(failedAttempts)
    return if (penalty == 0L) null else now + penalty
}

fun remainingLockoutMs(lockedOutUntil: Long?, now: Long): Long {
    if (lockedOutUntil == null) return 0
    return maxOf(0, lockedOutUntil - now)
}

/** `m:ss`, rounded up so the countdown never shows 0:00 while the pad is still disabled. */
fun formatLockoutCountdown(ms: Long): String {
    val totalSeconds = ceil(ms / 1000.0).toLong()
    val minutes = Math.floorDiv(totalSeconds, 60L)
    val seconds = totalSeconds % 60 // JS `%` truncates, like Kotlin's
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

fun autoLockLabel(minutes: Int): String {
    if (minutes <= 0) return "Immediately"
    if (minutes == 60) return "After 1 hour"
    return "After $minutes minute${if (minutes == 1) "" else "s"}"
}
