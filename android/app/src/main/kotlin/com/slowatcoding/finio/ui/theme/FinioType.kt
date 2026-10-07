package com.slowatcoding.finio.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.slowatcoding.finio.R

private fun variable(res: Int, weight: Int) = Font(
    resId = res,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

/** Instrument Sans — everything people read or type. */
val InstrumentSans = FontFamily(
    variable(R.font.instrument_sans, 400),
    variable(R.font.instrument_sans, 500),
    variable(R.font.instrument_sans, 600),
    variable(R.font.instrument_sans, 700),
)

/** Familjen Grotesk — the banknote numeral: page titles, dialog titles, headline money. */
val FamiljenGrotesk = FontFamily(
    variable(R.font.familjen_grotesk, 400),
    variable(R.font.familjen_grotesk, 500),
    variable(R.font.familjen_grotesk, 600),
    variable(R.font.familjen_grotesk, 700),
)

/** `font-variant-numeric: tabular-nums` is set on <body>, so every Instrument Sans style carries it. */
private const val TNUM = "tnum"

/** CSS line boxes centre the glyphs in the line height and don't trim; match that. */
private val CssLineHeight = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None,
)

private fun instrumentSans(size: TextUnit, lineHeight: TextUnit, weight: Int = 400) = TextStyle(
    fontFamily = InstrumentSans,
    fontSize = size,
    lineHeight = lineHeight,
    fontWeight = FontWeight(weight),
    fontFeatureSettings = TNUM,
    lineHeightStyle = CssLineHeight,
)

private fun familjen(size: TextUnit, lineHeight: TextUnit, weight: Int, tracking: TextUnit) = TextStyle(
    fontFamily = FamiljenGrotesk,
    fontSize = size,
    lineHeight = lineHeight,
    fontWeight = FontWeight(weight),
    letterSpacing = tracking,
    fontFeatureSettings = TNUM,
    lineHeightStyle = CssLineHeight,
)

/**
 * The Mudra type scale (design.md "Typography"), sized like Tailwind's (`text-xs` 12/16,
 * `text-sm` 14/20, `text-base` 16/24, `text-lg` 18/28, `text-xl` 20/28, `text-2xl` 24/32,
 * `text-3xl` 30/36). 1rem = 16sp.
 */
@Immutable
object FinioType {
    // ── Familjen Grotesk ──
    /** The hero figure on the banknote: 600, 2.75rem, line-height 1.05, −0.03em. Shrink with noteFigureSize. */
    val displayMoney = familjen(44.sp, 46.2.sp, 600, (-0.02).em)
    /** Tab-page `h1` ("Transactions", "Settings"): `text-2xl font-bold tracking-tight`. */
    val pageTitle = familjen(24.sp, 32.sp, 700, (-0.025).em)
    /** Sub-page `h1` in a back-button header: `text-base font-semibold`, −0.02em. */
    val screenTitle = familjen(16.sp, 24.sp, 600, (-0.02).em)
    /** Headline (h1 base): 600, 1.5rem, −0.02em. */
    val headline = familjen(24.sp, 32.sp, 600, (-0.02).em)
    /** `.font-money` at note-tile size (1.125rem). Use `.copy(fontSize = …)` for other steps. */
    val money = familjen(18.sp, 28.sp, 600, (-0.02).em)
    /** `DialogTitle`: `font-heading text-base leading-none font-medium`. */
    val dialogTitle = familjen(16.sp, 16.sp, 500, (-0.02).em)

    // ── Instrument Sans ──
    /** Section headings ("Where it sits", "Latest"): 600, 1rem. */
    val title = instrumentSans(16.sp, 24.sp, 600)
    /** Inputs and select triggers on mobile (`text-base`). */
    val input = instrumentSans(16.sp, 24.sp, 400)
    /** Row names and sentences: 400, 0.875rem. */
    val body = instrumentSans(14.sp, 20.sp, 400)
    /** Row names that carry weight (`text-sm font-medium`), buttons. */
    val bodyMedium = instrumentSans(14.sp, 20.sp, 500)
    /** Row values: 600 Instrument Sans — never Familjen Grotesk. */
    val rowValue = instrumentSans(14.sp, 20.sp, 600)
    /** Labels: 500, 0.75rem, sentence case, muted. */
    val label = instrumentSans(12.sp, 16.sp, 500)
    /** Captions / secondary row text (`text-xs`). */
    val caption = instrumentSans(12.sp, 16.sp, 400)
    /** The budget badge and other 10px micro text. */
    val micro = instrumentSans(10.sp, 14.sp, 500)
    /** Toasts (Sonner's 13px). */
    val toast = instrumentSans(13.sp, 19.5.sp, 500)
}
