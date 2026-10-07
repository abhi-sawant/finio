package com.slowatcoding.finio.ui.theme

import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/** `cubic-bezier(0.16, 1, 0.3, 1)` — Mudra's ease-out-expo (note tilt, tile press). */
val EaseOutExpo = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

/** Tailwind's default `transition` / `ease-in-out` curve, `cubic-bezier(0.4, 0, 0.2, 1)`. */
val TailwindEase = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)

/**
 * `prefers-reduced-motion: reduce` — Android's "Remove animations" (animator duration scale 0).
 * The note tilt and the PIN shake both stop under it.
 */
@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}
