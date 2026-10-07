package com.slowatcoding.finio.platform.notify

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.slowatcoding.finio.core.notify.NOTIFICATION_SYNC_TAG
import com.slowatcoding.finio.core.notify.ScheduledNotification
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/*
 * The Android side of web/src/services/notifications.ts `refreshNotifications` + the service
 * worker's periodic sync. Two wake-ups, both landing in [NotificationRunner.runDue]:
 *
 *  - a 12h periodic worker (the web's periodicSync floor — a safety net), and
 *  - a one-shot worker at the next pending `fireAt` (replaced on every publish), which gives
 *    on-time reminders the web can't promise.
 *
 * The schedule itself is built by core (`buildNotificationSchedule`) in the app layer, from the
 * finance store, and handed to [publish] on every app start/resume and after relevant edits.
 */
object NotificationScheduler {
    /** Unique name of the one-shot "next reminder" work. */
    const val NEXT_WORK_NAME = "$NOTIFICATION_SYNC_TAG-next"

    private const val PERIODIC_INTERVAL_HOURS = 12L

    /**
     * Persist [schedule] (whole-list replace), prune the fired ledger past 90 days, show anything
     * due now, and (re)arm both workers. Returns how many reminders were shown.
     */
    suspend fun publish(
        context: Context,
        schedule: List<ScheduledNotification>,
        now: Long = System.currentTimeMillis(),
    ): Int = withContext(Dispatchers.IO) {
        val app = context.applicationContext
        val store = NotificationStore.forContext(app)
        store.writeSchedule(schedule)
        store.pruneFired(now - FIRED_RETENTION_MS)
        val shown = NotificationRunner.runDue(app, now)
        ensurePeriodic(app)
        scheduleNext(app, now)
        shown
    }

    /**
     * Reminders turned off (or data reset): wipe schedule + ledger and cancel both workers — the
     * web's `clearNotificationData()` plus unregistering periodic sync.
     */
    suspend fun disable(context: Context) = withContext(Dispatchers.IO) {
        val app = context.applicationContext
        NotificationStore.forContext(app).clear()
        val wm = WorkManager.getInstance(app)
        wm.cancelUniqueWork(NOTIFICATION_SYNC_TAG)
        wm.cancelUniqueWork(NEXT_WORK_NAME)
    }

    /** The 12h periodic safety net. KEEP: re-enqueueing must not reset its clock. */
    fun ensurePeriodic(context: Context) {
        val request = PeriodicWorkRequestBuilder<NotificationWorker>(PERIODIC_INTERVAL_HOURS, TimeUnit.HOURS)
            .addTag(NOTIFICATION_SYNC_TAG)
            .build()
        WorkManager.getInstance(context.applicationContext)
            .enqueueUniquePeriodicWork(NOTIFICATION_SYNC_TAG, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    /** Arm (REPLACE) the one-shot for the next pending fireAt, or cancel it if none. */
    fun scheduleNext(context: Context, now: Long = System.currentTimeMillis()) {
        val app = context.applicationContext
        val store = NotificationStore.forContext(app)
        val wm = WorkManager.getInstance(app)
        val next = nextWakeAt(store.readSchedule(), store.readFiredIds(), now)
        if (next == null) {
            wm.cancelUniqueWork(NEXT_WORK_NAME)
            return
        }
        val request = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setInitialDelay((next - now).coerceAtLeast(0), TimeUnit.MILLISECONDS)
            .addTag(NOTIFICATION_SYNC_TAG)
            .build()
        wm.enqueueUniqueWork(NEXT_WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }
}

/** Both wake-ups: show what's due, then arm the next one-shot. Never touches the finance data. */
class NotificationWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        runCatching { NotificationRunner.runDue(applicationContext, now) }
        // Don't REPLACE ourselves while running as the one-shot: enqueueing the next one-shot
        // under the same unique name cancels this run — harmless, since runDue already finished.
        runCatching { NotificationScheduler.scheduleNext(applicationContext, now) }
        Result.success()
    }
}
