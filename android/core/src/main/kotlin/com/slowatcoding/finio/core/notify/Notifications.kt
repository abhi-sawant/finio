package com.slowatcoding.finio.core.notify

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Port of web/src/utils/notifications.ts — shared reminder types and the due-selection rule.

/** Periodic sync / WorkManager unique-work tag. */
const val NOTIFICATION_SYNC_TAG = "finio-notifications"

/** How far ahead the schedule looks. */
const val NOTIFICATION_HORIZON_DAYS = 45

/** Upper bound on `Settings.notifyLeadDays`. */
const val MAX_NOTIFY_LEAD_DAYS = 7

/** Local hour of day reminders land at. */
const val NOTIFY_HOUR = 9

/** Local hour of day the daily transaction-logging nudge lands at. */
const val DAILY_LOG_HOUR = 21

/** Never show more than this in one pass; the rest expire quietly. */
const val MAX_NOTIFICATIONS_PER_RUN = 3

@Serializable
enum class NotificationKind {
    @SerialName("bill") Bill,
    @SerialName("budget") Budget,
    @SerialName("credit") Credit,
    @SerialName("daily") Daily,
}

@Serializable
data class ScheduledNotification(
    /**
     * `${kind}:${subjectId}:${occurrenceKey}` — the dedupe contract. Derivable from the trigger
     * alone (never from fireAt), so a rebuilt schedule never re-sends a fired reminder.
     */
    val id: String,
    val kind: NotificationKind,
    /** Epoch ms from which this is eligible to fire. */
    val fireAt: Long,
    /** Epoch ms after which it is stale and must never fire. */
    val expiresAt: Long,
    val title: String,
    val body: String,
    /** In-app path to open when the notification is clicked. */
    val url: String,
)

/** The slice of `Settings` the scheduler cares about, plus the amount-masking flag. */
@Serializable
data class NotificationPrefs(
    val notificationsEnabled: Boolean,
    val notifyBills: Boolean,
    val notifyBudgets: Boolean,
    val notifyCreditDue: Boolean,
    val notifyLeadDays: Int,
    val notifyDailyLog: Boolean,
    /** Mirrors `Settings.hideAmounts`. */
    val hideAmounts: Boolean,
)

/** Which entries are due right now and have not already been shown (stable sort by fireAt). */
fun selectDueNotifications(
    schedule: List<ScheduledNotification>,
    firedIds: Set<String>,
    now: Long,
): List<ScheduledNotification> =
    schedule
        .filter { it.fireAt <= now && now < it.expiresAt && it.id !in firedIds }
        .sortedBy { it.fireAt }
        .take(MAX_NOTIFICATIONS_PER_RUN)
