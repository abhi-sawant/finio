package com.slowatcoding.finio.ui.screens.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.slowatcoding.finio.core.model.Settings
import com.slowatcoding.finio.core.notify.MAX_NOTIFY_LEAD_DAYS
import com.slowatcoding.finio.di.appContainer
import com.slowatcoding.finio.platform.notify.NotificationPermission
import com.slowatcoding.finio.platform.notify.NotificationRunner
import com.slowatcoding.finio.platform.notify.appNotificationSettingsIntent
import com.slowatcoding.finio.platform.notify.rememberNotificationPermissionRequester
import com.slowatcoding.finio.ui.common.collectFinanceState
import com.slowatcoding.finio.ui.common.financeStore
import com.slowatcoding.finio.ui.components.FinioCard
import com.slowatcoding.finio.ui.components.FinioDialog
import com.slowatcoding.finio.ui.components.FinioDivider
import com.slowatcoding.finio.ui.components.SwitchField
import com.slowatcoding.finio.ui.components.ToastAction
import com.slowatcoding.finio.ui.components.toast
import com.slowatcoding.finio.ui.icons.LucideIcons
import com.slowatcoding.finio.ui.theme.FinioTheme
import kotlinx.coroutines.launch

private val leadDayOptions = (0..MAX_NOTIFY_LEAD_DAYS).toList()

/**
 * Port of web/src/components/settings/NotificationsSection.tsx — the reminders master switch
 * (the one place notification permission is requested), per-kind switches, lead time and a
 * test reminder. Delivery is WorkManager-backed, so reminders always arrive with Finio closed.
 */
@Composable
fun NotificationsSection() {
    val container = appContainer()
    val store = financeStore()
    val state by collectFinanceState()
    val settings = state.settings
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val colors = FinioTheme.colors

    // Permission lives in the OS, not the store — the user can revoke it behind the app's back,
    // so it is re-read every time the screen resumes.
    var granted by remember { mutableStateOf(NotificationPermission.isGranted(context)) }
    // The last request came back denied (the web's `permission === 'denied'`).
    var denied by remember { mutableStateOf(false) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        granted = NotificationPermission.isGranted(context)
        if (granted) denied = false
    }
    // Permission held but notifications switched off for Finio in system settings.
    val blocked = denied || (!granted && !NotificationPermission.needsRuntimeRequest(context))
    val remindersOn = settings.notificationsEnabled && granted

    var showLeadDaysPicker by remember { mutableStateOf(false) }

    fun updateAndRefresh(update: (Settings) -> Settings) {
        store.updateSettings(update)
        scope.launch { container.refreshReminders() }
    }

    val requester = rememberNotificationPermissionRequester { result ->
        granted = result
        if (!result) {
            denied = true
            toast.error(
                "Notifications are blocked for Finio. Turn them on in your device settings.",
                action = ToastAction("Settings") { context.startActivity(appNotificationSettingsIntent(context)) },
            )
            return@rememberNotificationPermissionRequester
        }
        denied = false
        store.updateSettings { it.copy(notificationsEnabled = true) }
        scope.launch { container.refreshReminders() }
        toast.success("Reminders are on")
    }

    fun handleToggleReminders(next: Boolean) {
        if (!next) {
            store.updateSettings { it.copy(notificationsEnabled = false) }
            scope.launch { container.disableReminders() }
            return
        }
        requester.request()
    }

    fun handleTestNotification() {
        if (NotificationRunner.showTestNotification(context)) {
            // The OS notification may land somewhere easy to miss, so confirm in-app that it was sent.
            toast.success("Test reminder sent", "If it didn't appear, check your device's notification settings.")
        } else {
            toast.error("Could not show a notification")
        }
    }

    @Composable
    fun Kind(icon: ImageVector, title: String, description: String, checked: Boolean, update: (Settings, Boolean) -> Settings) {
        FinioDivider()
        SwitchField(
            title = title,
            description = description,
            checked = checked,
            onCheckedChange = { next -> updateAndRefresh { update(it, next) } },
            icon = { Icon(icon, null, Modifier.size(18.dp), tint = colors.mutedForeground) },
            modifier = Modifier.padding(16.dp),
        )
    }

    FinioCard(contentPadding = PaddingValues(0.dp)) {
        SwitchField(
            title = "Reminders",
            description = if (blocked) "Blocked in your device settings for Finio"
            else "Bill, budget and card alerts, even when Finio is closed",
            checked = remindersOn,
            // Unlike the web, a blocked switch stays tappable: Android may still re-prompt, and
            // otherwise the toast offers a way to the system settings.
            onCheckedChange = ::handleToggleReminders,
            icon = { Icon(LucideIcons.Bell, null, Modifier.size(18.dp), tint = colors.mutedForeground) },
            modifier = Modifier.padding(16.dp),
        )

        if (remindersOn) {
            Kind(LucideIcons.Repeat, "Upcoming bills", "Recurring transactions coming due", settings.notifyBills) { s, v -> s.copy(notifyBills = v) }
            Kind(LucideIcons.Target, "Budget alerts", "When a budget passes 85% or goes over", settings.notifyBudgets) { s, v -> s.copy(notifyBudgets = v) }
            Kind(LucideIcons.CreditCard, "Credit card dues", "Before a statement payment is due", settings.notifyCreditDue) { s, v -> s.copy(notifyCreditDue = v) }
            Kind(LucideIcons.NotebookPen, "Daily reminder", "An evening nudge to log today's transactions", settings.notifyDailyLog) { s, v -> s.copy(notifyDailyLog = v) }

            FinioDivider()
            val days = settings.notifyLeadDays
            SettingsValueRow(
                LucideIcons.CalendarClock,
                "Remind me",
                subtitle = if (days == 0) "On the due day" else "$days day${if (days == 1) "" else "s"} before the due date",
            ) {
                PillButton(
                    if (days == 0) "Same day" else "${days}d",
                    onClick = { showLeadDaysPicker = true },
                    contentDescription = "Change how early to remind",
                )
            }

            // A reminder may be days out, so without this there is no way to confirm the pipeline works.
            FinioDivider()
            SettingsRow(LucideIcons.BellRing, "Send a test reminder", onClick = ::handleTestNotification)
        }
    }

    if (showLeadDaysPicker) {
        FinioDialog(
            onDismissRequest = { showLeadDaysPicker = false },
            title = "Remind me",
            description = "How many days before a bill or card payment is due to send the reminder.",
        ) {
            ChoiceGrid(leadDayOptions, columns = 4) { days, mod ->
                ChoicePill(
                    if (days == 0) "Same" else "${days}d",
                    selected = days == settings.notifyLeadDays,
                    onClick = {
                        updateAndRefresh { it.copy(notifyLeadDays = days) }
                        showLeadDaysPicker = false
                    },
                    modifier = mod,
                )
            }
        }
    }
}
