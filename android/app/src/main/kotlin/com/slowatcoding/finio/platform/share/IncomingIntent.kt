package com.slowatcoding.finio.platform.share

import android.content.Intent

/**
 * Android adapter for [parseLaunch]. Call from `MainActivity.onCreate` (only when
 * `savedInstanceState == null`, so a rotation doesn't replay it) and from `onNewIntent`
 * (singleTask: shortcuts, shares and notification clicks into a running task land there).
 */
object IncomingIntent {
    fun parse(intent: Intent?): LaunchTarget? {
        if (intent == null) return null
        return parseLaunch(
            action = intent.action,
            data = intent.dataString,
            mimeType = intent.type,
            subject = intent.getCharSequenceExtra(Intent.EXTRA_SUBJECT)?.toString(),
            text = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString(),
        )
    }
}
