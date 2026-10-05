package com.slowatcoding.finio.platform.lock

import android.app.Activity
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import android.content.ContextWrapper

/**
 * FLAG_SECURE: blanks the recents thumbnail and blocks screenshots/screen recording. Android's
 * answer to the web flipping the lock "immediately" on hide so the task-switcher snapshot catches
 * the lock screen — here the snapshot is simply never taken. Tie it to "app lock enabled".
 */
fun Activity.setSecureWindow(enabled: Boolean) {
    if (enabled) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
    else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
}

/** Compose: keep FLAG_SECURE in sync with [enabled] for the hosting activity. */
@Composable
fun SecureWindowEffect(enabled: Boolean) {
    val activity = LocalContext.current.findActivity() ?: return
    DisposableEffect(activity, enabled) {
        activity.setSecureWindow(enabled)
        onDispose { }
    }
}

internal tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
