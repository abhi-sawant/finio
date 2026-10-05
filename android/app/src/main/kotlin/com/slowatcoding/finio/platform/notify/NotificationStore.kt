package com.slowatcoding.finio.platform.notify

import android.content.Context
import com.slowatcoding.finio.core.notify.ScheduledNotification
import com.slowatcoding.finio.platform.storage.JsonFileStore
import com.slowatcoding.finio.platform.storage.StoreFiles
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import java.io.File

/*
 * Port of web/src/services/notificationDb.ts — the reminder schedule and the "already fired"
 * ledger. On the web these live in IndexedDB because the service worker can't reach
 * localStorage; on Android the app and the WorkManager worker share a process and a filesDir,
 * so two atomic JSON files do the same job.
 *
 * The ledger is the dedupe contract's other half: [claimFired] is check-and-insert under one
 * process-wide lock plus a durable write, the Android equivalent of IDB `add()` rejecting a
 * duplicate key. Claim before showing, never after — a crash between the two costs a missed
 * reminder rather than a duplicated one.
 */

@Serializable
data class FiredRecord(val id: String, val firedAt: Long)

/** How long a fired-reminder record is kept before it is pruned (web: FIRED_RETENTION_MS). */
const val FIRED_RETENTION_MS: Long = 90L * 24 * 60 * 60 * 1000

class NotificationStore(dir: File) {
    private val scheduleFile = JsonFileStore(dir, StoreFiles.NOTIFY_SCHEDULE)
    private val firedFile = JsonFileStore(dir, StoreFiles.NOTIFY_FIRED)

    fun readSchedule(): List<ScheduledNotification> = synchronized(LOCK) {
        scheduleFile.read(SCHEDULE_SERIALIZER) ?: emptyList()
    }

    /** Whole-list replace in one atomic file write — a reader never sees half a schedule. */
    fun writeSchedule(entries: List<ScheduledNotification>) = synchronized(LOCK) {
        scheduleFile.write(SCHEDULE_SERIALIZER, entries)
    }

    fun readFired(): List<FiredRecord> = synchronized(LOCK) {
        firedFile.read(FIRED_SERIALIZER) ?: emptyList()
    }

    fun readFiredIds(): Set<String> = readFired().mapTo(HashSet()) { it.id }

    /**
     * Claim the right to show [id], returning false if it was already claimed. The record is
     * durable before this returns true.
     */
    fun claimFired(id: String, firedAt: Long): Boolean = synchronized(LOCK) {
        val fired = firedFile.read(FIRED_SERIALIZER) ?: emptyList()
        if (fired.any { it.id == id }) return false
        firedFile.write(FIRED_SERIALIZER, fired + FiredRecord(id, firedAt))
        true
    }

    /**
     * Drop ledger entries fired before [before]. By age only, never by "no longer in the
     * schedule": a fired reminder leaves the schedule as soon as it expires, so pruning on
     * absence would let every one of them fire again.
     */
    fun pruneFired(before: Long) = synchronized(LOCK) {
        val fired = firedFile.read(FIRED_SERIALIZER) ?: return
        val kept = fired.filter { it.firedAt >= before }
        if (kept.size != fired.size) firedFile.write(FIRED_SERIALIZER, kept)
    }

    /** Wipe both — reminders turned off, or the user reset their data. */
    fun clear() = synchronized(LOCK) {
        scheduleFile.delete()
        firedFile.delete()
    }

    companion object {
        /** Process-wide: the app and the worker may hold different instances over the same files. */
        private val LOCK = Any()
        private val SCHEDULE_SERIALIZER = ListSerializer(ScheduledNotification.serializer())
        private val FIRED_SERIALIZER = ListSerializer(FiredRecord.serializer())

        fun forContext(context: Context) = NotificationStore(context.applicationContext.filesDir)
    }
}

/**
 * When the one-shot worker should next wake: the earliest `fireAt` still in the future among
 * entries that haven't fired or expired. Null when nothing is pending. Entries already due are
 * the current run's job, not the next wake-up's.
 */
fun nextWakeAt(schedule: List<ScheduledNotification>, firedIds: Set<String>, now: Long): Long? =
    schedule
        .asSequence()
        .filter { it.fireAt > now && it.fireAt < it.expiresAt && it.id !in firedIds }
        .minOfOrNull { it.fireAt }
