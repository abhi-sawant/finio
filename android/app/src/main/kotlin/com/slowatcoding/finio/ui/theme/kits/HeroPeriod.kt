package com.slowatcoding.finio.ui.theme.kits

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Where today sits in the budget period behind the Dashboard hero — the web's `HeroInfo.totalDays`
 * and `dayOfPeriod`. The `ThemeKit` hero hooks only receive the elapsed fraction and the days left,
 * which cannot tell a 28-day month from a 31-day one near its end; a kit that draws "Day n of N"
 * or one mark per day reads this instead. Null outside the hero, so keep a fallback.
 */
data class HeroPeriod(val totalDays: Int, val dayOfPeriod: Int)

val LocalHeroPeriod = staticCompositionLocalOf<HeroPeriod?> { null }
