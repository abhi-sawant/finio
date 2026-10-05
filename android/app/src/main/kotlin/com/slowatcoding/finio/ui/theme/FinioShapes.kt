package com.slowatcoding.finio.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * The radius scale from index.css, all derived from `--radius: 1.375rem` (22px):
 * `sm` ×0.6, `md` ×0.8, `lg` ×1, `xl` ×1.4, `2xl` ×1.8, `3xl` ×2.2, `4xl` ×2.6.
 */
object FinioRadius {
    val base = 22.dp
    /** Inputs, select triggers, number/PIN keys, category tiles. */
    val sm = 13.2.dp
    /** Cards, list containers, note tiles, popovers, the calendar cell. */
    val md = 17.6.dp
    /** Dialogs, alert bands. */
    val lg = 22.dp
    val xl = 30.8.dp
    val xxl = 39.6.dp
    val xxxl = 48.4.dp
    val xxxxl = 57.2.dp
    /** The hero NoteCard: `--radius * 1.15`. */
    val note = 25.3.dp
    /** The account note chip — the one sharp shape. */
    val chip = 5.dp
}

object FinioShapes {
    val sm = RoundedCornerShape(FinioRadius.sm)
    val md = RoundedCornerShape(FinioRadius.md)
    val lg = RoundedCornerShape(FinioRadius.lg)
    val xl = RoundedCornerShape(FinioRadius.xl)
    val note = RoundedCornerShape(FinioRadius.note)
    val chip = RoundedCornerShape(FinioRadius.chip)
    /** Buttons, icon buttons, nav items, tab indicator, coin, progress tracks. */
    val full = CircleShape
}
