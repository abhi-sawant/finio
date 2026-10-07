package com.slowatcoding.finio

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import com.slowatcoding.finio.platform.share.IncomingIntent
import com.slowatcoding.finio.ui.shell.FinioRoot

/**
 * The single activity. A [FragmentActivity] (not a plain ComponentActivity) because androidx
 * BiometricPrompt hosts itself in a fragment.
 *
 * Every way in — the share sheet, a launcher shortcut, a notification click — is parsed here into
 * a LaunchTarget and parked on the container; the shell navigates to it once the hydration, lock
 * and onboarding gates have lifted (they never navigate themselves, exactly like App.tsx).
 */
class MainActivity : FragmentActivity() {

    private val container get() = (application as FinioApp).container

    override fun onCreate(savedInstanceState: Bundle?) {
        // Edge to edge: the note paper runs under both system bars, as the PWA's does in
        // standalone mode; headers and the tab bar pad themselves by the insets. FinioRoot
        // re-applies it whenever the theme flips, to keep the bar icons legible.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // Only a fresh launch: a recreated activity (rotation, process restore) must not replay it.
        if (savedInstanceState == null) IncomingIntent.parse(intent)?.let(container::offerLaunch)
        setContent { FinioRoot(container) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        IncomingIntent.parse(intent)?.let(container::offerLaunch)
    }
}
