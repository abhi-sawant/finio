package com.slowatcoding.finio.platform.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.slowatcoding.finio.MainActivity
import com.slowatcoding.finio.R
import com.slowatcoding.finio.core.notify.NotificationKind
import com.slowatcoding.finio.core.notify.ScheduledNotification
import com.slowatcoding.finio.core.notify.selectDueNotifications
import com.slowatcoding.finio.platform.share.DeepLinks

/*
 * Port of web/src/services/notificationRunner.ts — "show whatever is due". Called by the app on
 * start/resume and by the WorkManager workers, exactly as the web calls it from the page and
 * the service worker.
 */

/** One channel per reminder kind, so each can be silenced on its own in system settings. */
object NotificationChannels {
    const val BILLS = "bills"
    const val BUDGETS = "budgets"
    const val CREDIT = "credit"
    const val DAILY = "daily"

    fun idFor(kind: NotificationKind): String = when (kind) {
        NotificationKind.Bill -> BILLS
        NotificationKind.Budget -> BUDGETS
        NotificationKind.Credit -> CREDIT
        NotificationKind.Daily -> DAILY
    }

    /** Idempotent — safe to call on every start and before every show. */
    fun ensure(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        fun channel(id: String, name: Int, desc: Int, importance: Int) =
            NotificationChannel(id, context.getString(name), importance).apply {
                description = context.getString(desc)
            }
        manager.createNotificationChannels(
            listOf(
                channel(BILLS, R.string.notify_channel_bills, R.string.notify_channel_bills_desc, NotificationManager.IMPORTANCE_DEFAULT),
                channel(BUDGETS, R.string.notify_channel_budgets, R.string.notify_channel_budgets_desc, NotificationManager.IMPORTANCE_DEFAULT),
                channel(CREDIT, R.string.notify_channel_credit, R.string.notify_channel_credit_desc, NotificationManager.IMPORTANCE_DEFAULT),
                channel(DAILY, R.string.notify_channel_daily, R.string.notify_channel_daily_desc, NotificationManager.IMPORTANCE_LOW),
            ),
        )
    }
}

object NotificationRunner {

    /**
     * Show every due, unclaimed reminder (at most MAX_NOTIFICATIONS_PER_RUN). Returns how many
     * were shown. Blocking file IO — call off the main thread (workers already are).
     *
     * Does nothing (and claims nothing) without the POST_NOTIFICATIONS permission: a reminder
     * claimed but never displayed would be lost for good.
     */
    fun runDue(context: Context, now: Long = System.currentTimeMillis()): Int {
        if (!NotificationPermission.isGranted(context)) return 0
        val store = NotificationStore.forContext(context)
        val due = selectDueNotifications(store.readSchedule(), store.readFiredIds(), now)
        if (due.isEmpty()) return 0
        NotificationChannels.ensure(context)

        var shown = 0
        for (entry in due) {
            // Claim before showing, never after (see NotificationStore).
            if (!store.claimFired(entry.id, now)) continue
            if (show(context, entry)) shown += 1
        }
        return shown
    }

    /** Post one reminder. Tag = entry id, so a re-show replaces rather than stacks. */
    fun show(context: Context, entry: ScheduledNotification): Boolean {
        val notification = NotificationCompat.Builder(context, NotificationChannels.idFor(entry.kind))
            .setSmallIcon(R.drawable.ic_stat_finio)
            .setColor(ACCENT)
            .setContentTitle(entry.title)
            .setContentText(entry.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(entry.body))
            .setContentIntent(contentIntent(context, entry.url, entry.id.hashCode()))
            .setAutoCancel(true)
            .setWhen(entry.fireAt)
            .setShowWhen(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .build()
        return try {
            NotificationManagerCompat.from(context).notify(entry.id, NOTIFICATION_ID, notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }

    /**
     * Settings → "Send a test notification". Bypasses the schedule and ledger; uses the daily
     * channel and a fixed tag so repeated taps replace one notification.
     */
    fun showTestNotification(context: Context): Boolean {
        if (!NotificationPermission.isGranted(context)) return false
        NotificationChannels.ensure(context)
        val entry = ScheduledNotification(
            id = "test:finio:now",
            kind = NotificationKind.Daily,
            fireAt = System.currentTimeMillis(),
            expiresAt = System.currentTimeMillis() + 60_000,
            title = context.getString(R.string.notify_test_title),
            body = context.getString(R.string.notify_test_body),
            url = "/settings",
        )
        return show(context, entry)
    }

    /** Remove every Finio reminder from the shade (reminders turned off / data reset). */
    fun cancelAllShown(context: Context) = NotificationManagerCompat.from(context).cancelAll()

    /**
     * Click → `finio://open?path=<url>` delivered explicitly to MainActivity (singleTask, so an
     * open task gets it via onNewIntent). Explicit + immutable: no other app can retarget it.
     */
    fun contentIntent(context: Context, path: String, requestCode: Int): PendingIntent {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(DeepLinks.forPath(path)))
            .setClass(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    /** Every Finio reminder shares one numeric id; the per-entry tag makes them distinct. */
    private const val NOTIFICATION_ID = 1

    /** `--primary` light (#4b36c7) tints the small icon / accent. */
    private const val ACCENT = 0xFF4B36C7.toInt()
}

/** POST_NOTIFICATIONS + the app-level "notifications blocked" switch. */
object NotificationPermission {
    /** Granted at runtime (API 33+) or implicitly (≤32), and not blocked in system settings. */
    fun isGranted(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return false
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    /** Whether the runtime permission must be requested (API 33+ only). */
    fun needsRuntimeRequest(context: Context): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED

    /** `Settings.notificationsEnabled` AND the OS will actually show them. */
    fun canNotify(context: Context, settingsEnabled: Boolean): Boolean =
        settingsEnabled && isGranted(context)
}
