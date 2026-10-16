package com.slowatcoding.finio.platform.notify

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext

/**
 * Compose handle for the POST_NOTIFICATIONS prompt — the analogue of the web's
 * `requestNotificationPermission()`, which must run straight from a user gesture. Call
 * [request] from a click handler; [onResult] receives whether notifications can now be shown.
 */
class NotificationPermissionRequester internal constructor(
    private val context: Context,
    private val launch: () -> Unit,
    private val onAlreadyResolved: (Boolean) -> Unit,
) {
    fun request() {
        if (NotificationPermission.needsRuntimeRequest(context)) launch()
        else onAlreadyResolved(NotificationPermission.isGranted(context))
    }

    /** Whether reminders can be shown right now (permission + not blocked). */
    val isGranted: Boolean get() = NotificationPermission.isGranted(context)
}

@Composable
fun rememberNotificationPermissionRequester(onResult: (granted: Boolean) -> Unit): NotificationPermissionRequester {
    val context = LocalContext.current
    val callback = rememberUpdatedState(onResult)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        callback.value(NotificationPermission.isGranted(context))
    }
    return remember(context, launcher) {
        NotificationPermissionRequester(
            context = context,
            launch = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            },
            onAlreadyResolved = { callback.value(it) },
        )
    }
}

/**
 * Once the user has denied twice the OS stops showing the prompt (the web's "denied" state);
 * this opens Finio's notification settings page so they can turn it on by hand.
 */
fun appNotificationSettingsIntent(context: Context): Intent =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData(android.net.Uri.fromParts("package", context.packageName, null))
    }
