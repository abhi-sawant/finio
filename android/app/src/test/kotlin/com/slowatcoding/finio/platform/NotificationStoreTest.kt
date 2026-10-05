package com.slowatcoding.finio.platform

import com.slowatcoding.finio.core.notify.NotificationKind
import com.slowatcoding.finio.core.notify.ScheduledNotification
import com.slowatcoding.finio.platform.notify.FIRED_RETENTION_MS
import com.slowatcoding.finio.platform.notify.FiredRecord
import com.slowatcoding.finio.platform.notify.NotificationStore
import com.slowatcoding.finio.platform.notify.nextWakeAt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

class NotificationStoreTest {
    @get:Rule val tmp = TemporaryFolder()

    private fun entry(id: String, fireAt: Long, expiresAt: Long = fireAt + 1000) =
        ScheduledNotification(id, NotificationKind.Bill, fireAt, expiresAt, "t", "b", "/recurring")

    @Test fun claimIsOnceOnlyAndDurable() {
        val store = NotificationStore(tmp.root)
        assertTrue(store.claimFired("bill:r1:2026-10-05", 100))
        assertFalse(store.claimFired("bill:r1:2026-10-05", 200))
        // A fresh instance (the worker) sees the same ledger.
        val other = NotificationStore(tmp.root)
        assertFalse(other.claimFired("bill:r1:2026-10-05", 300))
        assertEquals(listOf(FiredRecord("bill:r1:2026-10-05", 100)), other.readFired())
    }

    @Test fun concurrentClaimsHaveExactlyOneWinner() {
        val pool = Executors.newFixedThreadPool(8)
        val wins = AtomicInteger()
        val tasks = (0 until 16).map {
            pool.submit { if (NotificationStore(tmp.root).claimFired("daily:log:2026-10-05", 1)) wins.incrementAndGet() }
        }
        tasks.forEach { it.get() }
        pool.shutdown()
        assertEquals(1, wins.get())
    }

    @Test fun pruneIsByAgeOnly() {
        val store = NotificationStore(tmp.root)
        val now = 200L * 24 * 60 * 60 * 1000
        store.claimFired("old", now - FIRED_RETENTION_MS - 1)
        store.claimFired("edge", now - FIRED_RETENTION_MS)
        store.claimFired("new", now)
        store.writeSchedule(emptyList()) // absence from the schedule must not matter
        store.pruneFired(now - FIRED_RETENTION_MS)
        assertEquals(setOf("edge", "new"), store.readFiredIds())
    }

    @Test fun scheduleRoundTripsAndClearWipesBoth() {
        val store = NotificationStore(tmp.root)
        val schedule = listOf(entry("a", 10), entry("b", 20))
        store.writeSchedule(schedule)
        store.claimFired("a", 10)
        assertEquals(schedule, NotificationStore(tmp.root).readSchedule())
        store.clear()
        assertEquals(emptyList<ScheduledNotification>(), store.readSchedule())
        assertEquals(emptySet<String>(), store.readFiredIds())
    }

    @Test fun nextWakeSkipsDueFiredAndExpired() {
        val now = 1_000L
        val schedule = listOf(
            entry("due", 900, 5_000),
            entry("fired", 1_500, 5_000),
            entry("broken", 1_600, 1_600),
            entry("later", 3_000, 9_000),
            entry("soon", 2_000, 9_000),
        )
        assertEquals(2_000L, nextWakeAt(schedule, setOf("fired"), now))
        assertNull(nextWakeAt(listOf(entry("due", 900, 5_000)), emptySet(), now))
    }
}
