package com.slowatcoding.finio.platform.lock

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import java.io.File
import com.slowatcoding.finio.platform.storage.JsonFileStore
import com.slowatcoding.finio.platform.storage.StoreFiles
import android.content.Context

/**
 * Whole-app foreground/background, the Android counterpart of the web's
 * `visibilitychange`/`pagehide` listeners in `useAutoLock`. Uses [ProcessLifecycleOwner], so a
 * rotation or an in-app activity switch is not a "background"; ON_STOP is dispatched ~700ms
 * after the last activity stops.
 *
 * Callbacks only — the app layer decides what to do (stamp [BackgroundedAtStore], lock
 * immediately when the delay is 0, run core `shouldLockOnResume` on start, flush debounced
 * writers, kick auto-backups, refresh the reminder schedule).
 *
 * Install once, from `Application.onCreate` (main thread).
 */
class AppLifecycleWatcher(
    private val onBackground: () -> Unit,
    private val onForeground: () -> Unit,
) : DefaultLifecycleObserver {

    private var installed = false

    fun install() {
        if (installed) return
        installed = true
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    fun uninstall() {
        if (!installed) return
        installed = false
        ProcessLifecycleOwner.get().lifecycle.removeObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) = onForeground()

    override fun onStop(owner: LifecycleOwner) = onBackground()
}

/**
 * `finio-lock-bg` — when the app was last backgrounded. A file rather than memory for the same
 * reason the web uses localStorage over sessionStorage: Android kills backgrounded processes,
 * and a lost timestamp fails closed (PIN on every cold start despite a grace window).
 *
 * Writes are synchronous and tiny: they happen in ON_STOP, right before the process may die.
 */
class BackgroundedAtStore(dir: File) {
    private val store = JsonFileStore(dir, StoreFiles.LOCK_BACKGROUNDED_AT)

    fun write(at: Long) {
        try {
            store.writeText(at.toString())
        } catch (_: Exception) {
            // Storage full — the missing timestamp fails closed, which is the safe side.
        }
    }

    fun read(): Long? = store.readText { it.trim().toLongOrNull() != null }?.trim()?.toLongOrNull()

    fun clear() = store.delete()

    companion object {
        fun forContext(context: Context) = BackgroundedAtStore(context.applicationContext.filesDir)
    }
}
