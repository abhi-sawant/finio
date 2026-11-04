package com.slowatcoding.finio.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
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

// ── Faces for the alternative visual themes (web: the web theme stylesheets). Variable fonts; the weight axis
// is driven by FontVariation, any other axis (opsz, wdth) stays at its default. ──
val SourceSerif4 = FontFamily(
    variable(R.font.source_serif_4, 400), variable(R.font.source_serif_4, 500),
    variable(R.font.source_serif_4, 600), variable(R.font.source_serif_4, 700),
)
val SourceSans3 = FontFamily(
    variable(R.font.source_sans_3, 400), variable(R.font.source_sans_3, 500),
    variable(R.font.source_sans_3, 600), variable(R.font.source_sans_3, 700),
)
val BricolageGrotesque = FontFamily(
    variable(R.font.bricolage_grotesque, 400), variable(R.font.bricolage_grotesque, 500),
    variable(R.font.bricolage_grotesque, 600), variable(R.font.bricolage_grotesque, 700),
    variable(R.font.bricolage_grotesque, 800),
)
val Geist = FontFamily(
    variable(R.font.geist, 200), variable(R.font.geist, 300), variable(R.font.geist, 400),
    variable(R.font.geist, 500), variable(R.font.geist, 600),
)
val Gabarito = FontFamily(
    variable(R.font.gabarito, 400), variable(R.font.gabarito, 500),
    variable(R.font.gabarito, 600), variable(R.font.gabarito, 700),
)
val Figtree = FontFamily(
    variable(R.font.figtree, 400), variable(R.font.figtree, 500),
    variable(R.font.figtree, 600), variable(R.font.figtree, 700),
)

/** `font-variant-numeric: tabular-nums` is set on <body>, so every body style carries it. */
private const val TNUM = "tnum"

/** CSS line boxes centre the glyphs in the line height and don't trim; match that. */
private val CssLineHeight = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None,
)

/**
 * The type scale for one visual theme (design.md "Typography"), sized like Tailwind's (`text-xs`
 * 12/16 … `text-3xl` 30/36; 1rem = 16sp). The sizes and line heights are shared by every theme;
 * a theme chooses its faces and weights — the `--ff-*` variables of the web theme stylesheets.
 */
@Immutable
class FinioTypeScale(
    heading: FontFamily,
    money: FontFamily,
    private val sansFamily: FontFamily,
    displayWeight: Int = 600, displayTracking: Float = -0.02f,
    pageTitleWeight: Int = 700, screenTitleWeight: Int = 600, headlineWeight: Int = 600,
    moneyWeight: Int = 600, dialogTitleWeight: Int = 500,
    /** Added to every reading style's weight (Gulal sets body at 500). */
    private val sansBoost: Int = 0,
    /** Weight of section headings and other `title` text. */
    titleWeight: Int = 600,
) {
    private fun sans(size: TextUnit, lineHeight: TextUnit, weight: Int = 400) = TextStyle(
        fontFamily = sansFamily,
        fontSize = size,
        lineHeight = lineHeight,
        fontWeight = FontWeight((weight + sansBoost).coerceAtMost(800)),
        fontFeatureSettings = TNUM,
        lineHeightStyle = CssLineHeight,
    )

    private fun face(family: FontFamily, size: TextUnit, lineHeight: TextUnit, weight: Int, tracking: Float) = TextStyle(
        fontFamily = family,
        fontSize = size,
        lineHeight = lineHeight,
        fontWeight = FontWeight(weight),
        letterSpacing = tracking.em,
        fontFeatureSettings = TNUM,
        lineHeightStyle = CssLineHeight,
    )

    val displayMoney = face(money, 44.sp, 46.2.sp, displayWeight, displayTracking)
    val pageTitle = face(heading, 24.sp, 32.sp, pageTitleWeight, -0.025f)
    val screenTitle = face(heading, 16.sp, 24.sp, screenTitleWeight, -0.02f)
    val headline = face(heading, 24.sp, 32.sp, headlineWeight, -0.02f)
    val money = face(money, 18.sp, 28.sp, moneyWeight, -0.02f)
    val dialogTitle = face(heading, 16.sp, 16.sp, dialogTitleWeight, -0.02f)

    val title = sans(16.sp, 24.sp, titleWeight)
    val input = sans(16.sp, 24.sp, 400)
    val body = sans(14.sp, 20.sp, 400)
    val bodyMedium = sans(14.sp, 20.sp, 500)
    val rowValue = sans(14.sp, 20.sp, 600)
    val label = sans(12.sp, 16.sp, 500)
    val caption = sans(12.sp, 16.sp, 400)
    val micro = sans(10.sp, 14.sp, 500)
    val toast = sans(13.sp, 19.5.sp, 500)
}

/** Mudra: Familjen Grotesk (the banknote numeral) for titles and money, Instrument Sans to read. */
val MudraTypeScale = FinioTypeScale(FamiljenGrotesk, FamiljenGrotesk, InstrumentSans)

fun typeScaleFor(visual: VisualTheme): FinioTypeScale = when (visual) {
    VisualTheme.Mudra -> MudraTypeScale
    VisualTheme.Khata -> khataScale
    VisualTheme.Gulal -> gulalScale
    VisualTheme.Shunya -> shunyaScale
    VisualTheme.Bagh -> baghScale
}

private val khataScale = FinioTypeScale(
    SourceSerif4, SourceSerif4, SourceSans3,
    displayWeight = 700, pageTitleWeight = 700, screenTitleWeight = 700, headlineWeight = 700, moneyWeight = 600,
)
private val gulalScale = FinioTypeScale(
    BricolageGrotesque, BricolageGrotesque, BricolageGrotesque,
    displayWeight = 800, displayTracking = -0.04f, pageTitleWeight = 800, screenTitleWeight = 800,
    headlineWeight = 800, moneyWeight = 800, dialogTitleWeight = 700, sansBoost = 100, titleWeight = 700,
)
private val shunyaScale = FinioTypeScale(
    Geist, Geist, Geist,
    displayWeight = 200, displayTracking = -0.05f, pageTitleWeight = 300, screenTitleWeight = 400,
    headlineWeight = 300, moneyWeight = 400, dialogTitleWeight = 400, titleWeight = 500,
)
private val baghScale = FinioTypeScale(
    Gabarito, Gabarito, Figtree,
    displayWeight = 700, displayTracking = -0.035f, pageTitleWeight = 700, screenTitleWeight = 700,
    headlineWeight = 700, moneyWeight = 700, dialogTitleWeight = 600,
)

val LocalFinioType = staticCompositionLocalOf { MudraTypeScale }

/**
 * The type scale of the active visual theme, read like the old static object: `FinioType.body`.
 * Every member is a composable getter, so it can only be read during composition (a `remember {}`
 * or default argument is fine; a plain callback is not).
 */
object FinioType {
    val displayMoney: TextStyle @Composable @ReadOnlyComposable get() = LocalFinioType.current.displayMoney
    val pageTitle: TextStyle @Composable @ReadOnlyComposable get() = LocalFinioType.current.pageTitle
    val screenTitle: TextStyle @Composable @ReadOnlyComposable get() = LocalFinioType.current.screenTitle
    val headline: TextStyle @Composable @ReadOnlyComposable get() = LocalFinioType.current.headline
    val money: TextStyle @Composable @ReadOnlyComposable get() = LocalFinioType.current.money
    val dialogTitle: TextStyle @Composable @ReadOnlyComposable get() = LocalFinioType.current.dialogTitle
    val title: TextStyle @Composable @ReadOnlyComposable get() = LocalFinioType.current.title
    val input: TextStyle @Composable @ReadOnlyComposable get() = LocalFinioType.current.input
    val body: TextStyle @Composable @ReadOnlyComposable get() = LocalFinioType.current.body
    val bodyMedium: TextStyle @Composable @ReadOnlyComposable get() = LocalFinioType.current.bodyMedium
    val rowValue: TextStyle @Composable @ReadOnlyComposable get() = LocalFinioType.current.rowValue
    val label: TextStyle @Composable @ReadOnlyComposable get() = LocalFinioType.current.label
    val caption: TextStyle @Composable @ReadOnlyComposable get() = LocalFinioType.current.caption
    val micro: TextStyle @Composable @ReadOnlyComposable get() = LocalFinioType.current.micro
    val toast: TextStyle @Composable @ReadOnlyComposable get() = LocalFinioType.current.toast
}
