package com.slowatcoding.finio.platform.storage

import android.content.Context
import com.slowatcoding.finio.ui.theme.VisualTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Which visual world the app wears (Mudra, Khata, Gulal, Shunya, Bagh), in its own SharedPreferences
 * file — the counterpart of the web's `finio-visual-theme` localStorage key.
 *
 * Deliberately **not** in `FinanceState.settings`: settings are exported into every backup, so a
 * field there would change the backup contract (spec, web validator, Android validator), and a
 * look is a per-device preference — a phone and a laptop restoring the same backup are free to wear
 * different themes. Read synchronously at construction so the first frame is already themed.
 */
class VisualThemeStore(context: Context) {
    private val prefs = context.getSharedPreferences("finio-visual-theme", Context.MODE_PRIVATE)
    private val _theme = MutableStateFlow(VisualTheme.fromId(prefs.getString(KEY, null)))

    val theme: StateFlow<VisualTheme> = _theme.asStateFlow()

    fun set(theme: VisualTheme) {
        prefs.edit().putString(KEY, theme.id).apply()
        _theme.value = theme
    }

    private companion object {
        const val KEY = "visualTheme"
    }
}
