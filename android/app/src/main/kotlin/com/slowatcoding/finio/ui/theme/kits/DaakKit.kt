package com.slowatcoding.finio.ui.theme.kits

import android.graphics.Paint as NativePaint
import android.graphics.Path as NativePath
import android.graphics.RectF
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import com.slowatcoding.finio.R
import com.slowatcoding.finio.ui.mudra.Denomination
import com.slowatcoding.finio.ui.mudra.NoteStyle
import com.slowatcoding.finio.ui.navigation.FinioTab
import com.slowatcoding.finio.ui.shell.tabClickable
import com.slowatcoding.finio.ui.theme.CssShadow
import com.slowatcoding.finio.ui.theme.FinioBrushes
import com.slowatcoding.finio.ui.theme.FinioColors
import com.slowatcoding.finio.ui.theme.FinioShadows
import com.slowatcoding.finio.ui.theme.FinioShapeScale
import com.slowatcoding.finio.ui.theme.FinioSpec
import com.slowatcoding.finio.ui.theme.FinioTheme
import com.slowatcoding.finio.ui.theme.FinioTypeScale
import com.slowatcoding.finio.ui.theme.ThemePalette
import com.slowatcoding.finio.ui.theme.VisualTheme
import com.slowatcoding.finio.ui.theme.rememberReducedMotion
import com.slowatcoding.finio.ui.theme.rgba
import com.slowatcoding.finio.ui.theme.stock
import java.util.Calendar

/**
 * Daak — India Post stationery (web/src/themes/daak.css). The pale-blue inland letter is the
 * ground (ink-navy sorting office in dark), pillar-box red is the action, panels carry the
 * letter's printed double rule, fields and progress tracks are ruled. Young Serif sets titles and
 * figures, Public Sans every row. The signature is the double-ring date postmark struck once in
 * the hero's corner ([HeroScene]); the add button is a pillar box and the tab bar a strip of
 * paper with a perforated top edge.
 *
 * Known gaps (no shared hook): account tiles/chips are not perforated stamps, "Near limit" /
 * "Over budget" are not rubber stamps, section heads have no dashed fold line, fields are not
 * reduced to a ruled line.
 */
object DaakKit : ThemeKit {
    override val visual = VisualTheme.Daak
    override fun palette(dark: Boolean): ThemePalette = if (dark) Dark else Light
    override val type: FinioTypeScale get() = Scale
    override val shapes: FinioShapeScale get() = Shapes
    override fun coinInk(dark: Boolean) = c(0xFFF8F0)

    /** The FAB is a pillar box: a domed top, a square foot. */
    override val coinShape: Shape get() = PillarShape

    @androidx.compose.runtime.Composable
    override fun coinDecoration(shape: Shape): Modifier {
        val base = FinioTheme.colors.let { if (it.isDark) c(0x4A0F0B) else c(0x2A1210) }
        return Modifier.drawBehind {
            // inset 0 -6px 0 base — the box's dark foot.
            val foot = 6.dp.toPx()
            drawRect(base, Offset(0f, size.height - foot), Size(size.width, foot))
            // The posting slot.
            val y = 15.dp.toPx()
            drawLine(Color.Black.copy(alpha = 0.22f), Offset(5.dp.toPx(), y), Offset(size.width - 5.dp.toPx(), y), 2.dp.toPx())
        }
    }

    override val progressTrackHeight: Dp get() = 12.dp

    /** A ruled bar quartered like a form scale: paper, ink border, ticks at each quarter. */
    @Composable
    override fun progressDecoration(): Modifier {
        val colors = FinioTheme.colors
        val paper = colors.popover
        val ink = colors.foreground
        return Modifier.drawWithContent {
            drawRect(paper)
            drawContent()
            val tick = ink.copy(alpha = 0.55f)
            for (i in 1..3) {
                val x = size.width * i / 4f
                drawLine(tick, Offset(x, 0f), Offset(x, size.height), 1.dp.toPx())
            }
            val w = 1.dp.toPx()
            drawRect(ink, Offset(w / 2, w / 2), Size(size.width - w, size.height - w), style = Stroke(w))
        }
    }

    // ── hero: the letter-card ──
    @Composable
    override fun heroContainer(shape: Shape): Modifier {
        val colors = FinioTheme.colors
        val card = colors.card
        val print = if (colors.isDark) c(0x8FB4E0) else c(0x235089)
        val line = colors.border
        return Modifier
            .clip(shape)
            .background(card)
            .drawWithContent {
                drawContent()
                // box-shadow: inset 0 0 0 3px card, inset 0 0 0 4px line — the printed double rule.
                val a = 0.5.dp.toPx()
                drawRect(print, Offset(a, a), Size(size.width - 2 * a, size.height - 2 * a), style = Stroke(1.dp.toPx()))
                val b = 3.5.dp.toPx()
                drawRect(line, Offset(b, b), Size(size.width - 2 * b, size.height - 2 * b), style = Stroke(1.dp.toPx()))
            }
    }

    override fun heroPadding(wide: Boolean) = if (wide) PaddingValues(30.dp, 26.dp) else PaddingValues(18.dp)

    @Composable
    override fun HeroFigure(text: String, style: TextStyle, color: Color, whole: Boolean, modifier: Modifier): Boolean {
        val sized = style.copy(fontFamily = YoungSerif, fontWeight = FontWeight.Normal, letterSpacing = (-0.01).em)
        // Young Serif has no rupee sign; Public Sans supplies it, as on the web.
        val annotated = remember(text) {
            buildAnnotatedString {
                for (ch in text) {
                    if (ch == '₹') withStyle(SpanStyle(fontFamily = PublicSans, fontWeight = FontWeight.Normal)) { append(ch) } else append(ch)
                }
            }
        }
        Text(annotated, modifier, style = sized, color = color, maxLines = 1, softWrap = false)
        return true
    }

    /**
     * The postmark sits in the scene slot, which the card lays out above the label, so it takes its
     * own band at the corner and can never sit under the figure or text.
     */
    @Composable
    override fun HeroScene(elapsed: Float, spentPercent: Float, isOver: Boolean) {
        val wide = LocalConfiguration.current.screenWidthDp >= 840
        val reduced = rememberReducedMotion()
        val strike = remember { Animatable(if (reduced) 1f else 0f) }
        LaunchedEffect(Unit) { if (!reduced) strike.animateTo(1f, tween(350, delayMillis = 300, easing = StrikeEase)) }
        val ink = if (FinioTheme.colors.isDark) c(0xBCD7F2) else c(0x1C3566)
        val context = LocalContext.current
        val typeface = remember { ResourcesCompat.getFont(context, R.font.daak_courier_prime_bold) }
        val period = LocalHeroPeriod.current
        val stamp = remember(period) { Postmark.today(period) }
        val ringSize = if (wide) 152.dp else 96.dp
        Row(
            Modifier.fillMaxWidth().padding(bottom = 4.dp).clearAndSetSemantics { },
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (wide) {
                Canvas(Modifier.size(72.dp, 88.dp).offset(x = 8.dp)) { drawWaves(ink.copy(alpha = 0.85f)) }
            }
            Canvas(
                Modifier
                    .size(ringSize)
                    .graphicsLayer {
                        val t = strike.value
                        scaleX = 1.25f + (1f - 1.25f) * t
                        scaleY = scaleX
                        rotationZ = -14f + (-9f + 14f) * t
                        alpha = 0.88f * t
                    },
            ) { drawRing(ink, typeface, stamp) }
        }
    }

    // ── chrome: a strip of paper with a perforated top edge; the current page in red with a dot ──
    @Composable
    override fun TabBar(active: FinioTab?, onSelect: (FinioTab) -> Unit, modifier: Modifier) {
        val colors = FinioTheme.colors
        val red = colors.primary
        Row(
            modifier
                .fillMaxWidth()
                .clip(PerforatedTop)
                .background(colors.popover)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(top = 4.dp)
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FinioTab.entries.forEach { tab ->
                val selected = tab == active
                val tint = if (selected) red else colors.mutedForeground
                val icon = remember(tab) { tab.icon(1.6f) }
                Column(
                    Modifier
                        .weight(1f)
                        .drawBehind { if (selected) drawCircle(red, 2.5.dp.toPx(), Offset(size.width / 2f, 2.5.dp.toPx())) }
                        .tabClickable(selected) { onSelect(tab) }
                        .padding(top = 10.dp, bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp), tint = tint)
                    Text(
                        tab.label,
                        style = TabStyle,
                        color = tint,
                        fontWeight = FontWeight(if (selected) 700 else 500),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

// ───────────────────────────── the postmark ─────────────────────────────

private object Postmark {
    private val months = listOf("JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC")
    private val days = listOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT")

    class Stamp(val date: String, val weekday: String, val progress: String)

    /**
     * "DAY n OF N" uses the exact budget period from `LocalHeroPeriod` (as the web does); only when
     * that is null (previews) does it fall back to counting the calendar month.
     */
    fun today(period: HeroPeriod?): Stamp {
        val cal = Calendar.getInstance()
        val d = cal.get(Calendar.DAY_OF_MONTH)
        return Stamp(
            "$d ${months[cal.get(Calendar.MONTH)]} ${cal.get(Calendar.YEAR)}",
            days[cal.get(Calendar.DAY_OF_WEEK) - 1],
            if (period != null) "DAY ${period.dayOfPeriod} OF ${period.totalDays}"
            else "DAY $d OF ${cal.getActualMaximum(Calendar.DAY_OF_MONTH)}",
        )
    }
}

private const val RingText = "FINIO · KEPT ONLY ON THIS DEVICE"

/** The 140×140 SVG ring, drawn with the same coordinates and scaled to the canvas. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRing(ink: Color, typeface: android.graphics.Typeface?, stamp: Postmark.Stamp) {
    val s = size.width / 140f
    val centre = Offset(70f * s, 70f * s)
    drawCircle(ink, 66f * s, centre, style = Stroke(2.2f * s))
    drawCircle(ink, 61.5f * s, centre, style = Stroke(1f * s))
    drawCircle(ink, 37f * s, centre, style = Stroke(1.4f * s))
    drawCircle(ink, 2f * s, Offset(70f * s, 114.5f * s))
    drawLine(ink, Offset(35f * s, 59f * s), Offset(105f * s, 59f * s), 1.5f * s)
    drawLine(ink, Offset(35f * s, 81f * s), Offset(105f * s, 81f * s), 1.5f * s)

    val paint = NativePaint(NativePaint.ANTI_ALIAS_FLAG).apply {
        color = ink.toArgb()
        this.typeface = typeface
        style = NativePaint.Style.FILL
    }
    drawIntoCanvas { canvas ->
        val nc = canvas.nativeCanvas
        nc.save()
        nc.scale(s, s)
        // Ring text: r46 circle from the bottom, clockwise, spread to 262 units (SVG textLength).
        val path = NativePath().apply {
            arcTo(RectF(24f, 24f, 116f, 116f), 90f, 359.9f)
        }
        paint.textSize = 9.6f
        paint.textAlign = NativePaint.Align.LEFT
        val natural = paint.measureText(RingText)
        paint.letterSpacing = ((262f - natural) / RingText.length / 9.6f).coerceAtLeast(0f)
        nc.drawTextOnPath(RingText, path, 9f, 0f, paint)
        paint.letterSpacing = 0f
        paint.textAlign = NativePaint.Align.CENTER
        paint.textSize = 10.6f
        nc.drawText(stamp.date, 70f, 74.2f, paint)
        paint.textSize = 7.6f
        nc.drawText(stamp.weekday, 70f, 52.5f, paint)
        nc.drawText(stamp.progress, 70f, 93.5f, paint)
        nc.restore()
    }
}

/** Five cancellation lines: `M2 y q7-6 14 0 t14 0 14 0 14 0` in a 60×80 box. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawWaves(ink: Color) {
    val s = size.width / 60f
    for (y in listOf(12f, 26f, 40f, 54f, 68f)) {
        val p = Path().apply {
            moveTo(2f * s, y * s)
            relativeQuadraticTo(7f * s, -6f * s, 14f * s, 0f)
            relativeQuadraticTo(7f * s, 6f * s, 14f * s, 0f)
            relativeQuadraticTo(7f * s, -6f * s, 14f * s, 0f)
            relativeQuadraticTo(7f * s, 6f * s, 14f * s, 0f)
        }
        drawPath(p, ink, style = Stroke(1.7f * s, cap = StrokeCap.Round))
    }
}

/** `cubic-bezier(.16,.84,.3,1)` — the strike. */
private val StrikeEase = CubicBezierEasing(0.16f, 0.84f, 0.3f, 1f)

// ───────────────────────────── shapes ─────────────────────────────

/** A pillar box: domed top (26dp), 2dp foot. */
private val PillarShape = RoundedCornerShape(26.dp, 26.dp, 2.dp, 2.dp)

/**
 * A strip of paper whose top edge is punched: holes of 3dp radius every 8dp, centred on a line 4dp
 * below the top (the web's `radial-gradient` mask).
 */
private val PerforatedTop: Shape = object : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val inset = with(density) { 4.dp.toPx() }
        val pitch = with(density) { 8.dp.toPx() }
        val r = with(density) { 3.dp.toPx() }
        val body = Path().apply { addRect(Rect(0f, inset, size.width, size.height)) }
        val holes = Path()
        var x = 0f
        while (x <= size.width + pitch) {
            holes.addOval(Rect(x - r, inset - r, x + r, inset + r))
            x += pitch
        }
        return Outline.Generic(Path.combine(PathOperation.Difference, body, holes))
    }
}

// ───────────────────────────── type ─────────────────────────────

private fun variable(res: Int, weight: Int) = Font(
    resId = res,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

/** Static, single weight, no ₹ — titles, money and the hero figure. */
private val YoungSerif = FontFamily(Font(R.font.daak_young_serif, FontWeight.Normal))

private val PublicSans = FontFamily(
    variable(R.font.daak_public_sans, 400), variable(R.font.daak_public_sans, 500),
    variable(R.font.daak_public_sans, 600), variable(R.font.daak_public_sans, 700),
)

private val Scale = FinioTypeScale(
    YoungSerif, YoungSerif, PublicSans,
    displayWeight = 400, displayTracking = -0.01f, pageTitleWeight = 400, screenTitleWeight = 400,
    headlineWeight = 400, moneyWeight = 400, dialogTitleWeight = 400, titleWeight = 600,
)
private val Shapes = FinioShapeScale(base = 2.dp, full = RoundedCornerShape(2.dp))

private val TabStyle = TextStyle(fontFamily = PublicSans, fontSize = 10.5.sp)

// ───────────────────────────── palettes ─────────────────────────────

private fun c(rgb: Long) = Color(0xFF000000 or rgb)
private fun solid(rgb: Long): Brush = SolidColor(c(rgb))

private fun colorsOf(
    dark: Boolean,
    background: Long, foreground: Long, card: Long, popover: Long,
    primary: Long, primaryFg: Long, secondary: Long, muted: Long, mutedFg: Long,
    accent: Long, accentFg: Long, destructive: Long, destructiveFg: Long,
    positive: Long, warning: Long, ring: Long, border: Long, input: Long,
    charts: List<Long>,
    sidebar: Long, sidebarPrimary: Long, sidebarAccent: Long,
    warningBand: Long, warningBandFg: Long, warningBandAccent: Long,
    glassStrong: Long, shadowTint: Color, scrim: Color,
) = FinioColors(
    isDark = dark,
    background = c(background), foreground = c(foreground),
    card = c(card), cardForeground = c(foreground),
    popover = c(popover), popoverForeground = c(foreground),
    primary = c(primary), primaryForeground = c(primaryFg),
    secondary = c(secondary), secondaryForeground = c(foreground),
    muted = c(muted), mutedForeground = c(mutedFg),
    accent = c(accent), accentForeground = c(accentFg),
    destructive = c(destructive), destructiveForeground = c(destructiveFg),
    positive = c(positive), warning = c(warning),
    border = c(border), input = c(input), ring = c(ring),
    chart1 = c(charts[0]), chart2 = c(charts[1]), chart3 = c(charts[2]), chart4 = c(charts[3]), chart5 = c(charts[4]),
    sidebar = c(sidebar), sidebarForeground = c(foreground),
    sidebarPrimary = c(sidebarPrimary), sidebarPrimaryForeground = c(primaryFg),
    sidebarAccent = c(sidebarAccent), sidebarAccentForeground = c(foreground),
    sidebarBorder = c(input), sidebarRing = c(ring),
    warningBand = c(warningBand), warningBandForeground = c(warningBandFg), warningBandAccent = c(warningBandAccent),
    engraving = Color.Transparent,
    glass = Color.Transparent, glassStrong = c(glassStrong), glassBorder = c(border), glassHighlight = Color.Transparent,
    shadowTint = shadowTint, scrim = scrim,
)

/** Flat stationery: no card, float, glow or tile shadows. */
private fun shadowsOf(tint: Color) = stock(tint).let { s ->
    FinioShadows(
        card = emptyList(), float = emptyList(), glowPrimary = emptyList(), glowSuccess = emptyList(), glowDanger = emptyList(),
        coin = emptyList(), tile = emptyList(), tabBar = emptyList(), noteChip = emptyList(),
        xs = s[0], sm = s[1], md = s[2], lg = s[3], xl = s[4],
    )
}

/** Definitive stamps: one ink per denomination, cream lettering. */
private fun stamps(ink: Long, rs10: Long, rs20: Long, rs50: Long, rs100: Long, rs200: Long, rs500: Long, rs2000: Long) =
    Denomination.entries.associateWith { d ->
        val bg = when (d) {
            Denomination.Rs10 -> rs10
            Denomination.Rs20 -> rs20
            Denomination.Rs50 -> rs50
            Denomination.Rs100 -> rs100
            Denomination.Rs200 -> rs200
            Denomination.Rs500 -> rs500
            Denomination.Rs2000 -> rs2000
        }
        NoteStyle(c(bg), c(bg), c(ink))
    }

private val Light = ThemePalette(
    colors = colorsOf(
        false, 0xC5DCEF, 0x13213D, 0xDBEAF7, 0xFBFAF5, 0x9C1410, 0xFFF8F0, 0xB4D0E8, 0xB4D0E8, 0x35486A,
        0xA9CAE6, 0x13213D, 0x9D1230, 0xFFF8F0, 0x3D5412, 0x6D4300, ring = 0xC4201B, border = 0x6F97C0, input = 0x235089,
        charts = listOf(0x235089, 0xC4201B, 0x55671F, 0x8D590B, 0x663A58),
        sidebar = 0xA9CAE6, sidebarPrimary = 0xC4201B, sidebarAccent = 0xC5DCEF,
        warningBand = 0xFBFAF5, warningBandFg = 0x13213D, warningBandAccent = 0x9C1410,
        glassStrong = 0xFBFAF5, shadowTint = Color(19, 33, 61), scrim = rgba(19, 33, 61, 0.45f),
    ),
    brushes = FinioBrushes(
        gradPrimary = solid(0xC4201B), gradPrimarySoft = solid(0xB4D0E8),
        gradSuccess = solid(0x3D5412), gradSuccessSoft = solid(0xD9E3C4),
        gradDanger = solid(0x9D1230), gradDangerSoft = solid(0xF0D3D6),
        gradInfo = solid(0x235089), gradWarning = solid(0x6D4300), gradSurface = solid(0xDBEAF7),
        paper = SolidColor(Color.Transparent),
        // The FAB is pillar-box red (the web paints [data-slot=fab] with --dk-fill, not --coin).
        coin = solid(0xC4201B), register = { _ -> solid(0x235089) }, thread = solid(0x235089),
    ),
    shadows = shadowsOf(Color(19, 33, 61)),
    spec = FinioSpec(
        VisualTheme.Daak, 1.dp,
        stamps(0xFBFAF5, rs10 = 0x2C5A86, rs20 = 0x55671F, rs50 = 0x2D6A66, rs100 = 0x663A58, rs200 = 0x8D590B, rs500 = 0x55671F, rs2000 = 0xA3213A),
    ),
)

private val Dark = ThemePalette(
    colors = colorsOf(
        true, 0x0D1730, 0xD3E4F5, 0x13213F, 0x1B2B50, 0xFF8479, 0x1C0705, 0x1B2B50, 0x1B2B50, 0x9AB0D0,
        0x223660, 0xD3E4F5, 0xFF8A96, 0x1C0705, 0xA9C46A, 0xE3B65E, ring = 0xF0473C, border = 0x3F5C8C, input = 0x8FB4E0,
        charts = listOf(0x8FB4E0, 0xF0473C, 0xA9C46A, 0xE3B65E, 0xC995B8),
        sidebar = 0x091124, sidebarPrimary = 0xF0473C, sidebarAccent = 0x13213F,
        warningBand = 0x1B2B50, warningBandFg = 0xD3E4F5, warningBandAccent = 0xFF8479,
        glassStrong = 0x1B2B50, shadowTint = Color(0, 0, 0), scrim = rgba(0, 0, 0, 0.6f),
    ),
    brushes = FinioBrushes(
        gradPrimary = solid(0xB8231C), gradPrimarySoft = solid(0x1B2B50),
        gradSuccess = solid(0x4A5A1B), gradSuccessSoft = solid(0x1D2C22),
        gradDanger = solid(0xA01B32), gradDangerSoft = solid(0x3A1A2C),
        gradInfo = solid(0x254C72), gradWarning = solid(0x7A4D09), gradSurface = solid(0x13213F),
        paper = SolidColor(Color.Transparent),
        coin = solid(0xB8231C), register = { _ -> solid(0x8FB4E0) }, thread = solid(0x8FB4E0),
    ),
    shadows = shadowsOf(Color(0, 0, 0)),
    spec = FinioSpec(
        VisualTheme.Daak, 1.dp,
        stamps(0xF4F2EA, rs10 = 0x254C72, rs20 = 0x4A5A1B, rs50 = 0x265A57, rs100 = 0x57314B, rs200 = 0x7A4D09, rs500 = 0x4A5A1B, rs2000 = 0x8F1D33),
    ),
)
