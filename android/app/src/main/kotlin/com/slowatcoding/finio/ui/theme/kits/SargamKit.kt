package com.slowatcoding.finio.ui.theme.kits

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
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
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Sargam — the audio cassette and its J-card (web/src/themes/sargam.css). Cool card stock with the
 * five-stripe run in light, a brown-black tape deck with amber keys in dark. Transport keys sit on a
 * 4dp edge and drop 2dp when pressed. Michroma sets titles, legends and the hero figure, Lexend
 * everything else. Signature: the 12-bar level meter and the tape window under the hero.
 *
 * Michroma has no rupee sign and a Compose FontFamily cannot fall back per glyph, so ₹ inside
 * Michroma text resolves to the system font; only the hero figure splits the sign out into Lexend.
 */
object SargamKit : ThemeKit {
    override val visual = VisualTheme.Sargam
    override fun palette(dark: Boolean): ThemePalette = if (dark) DarkPalette else LightPalette
    override val type: FinioTypeScale get() = Scale
    override val shapes: FinioShapeScale get() = Shapes
    override fun coinInk(dark: Boolean) = c(if (dark) 0x24130A else 0x2A150A)
    override val coinShape: Shape get() = androidx.compose.foundation.shape.CircleShape

    @Composable
    override fun coinDecoration(shape: Shape): Modifier {
        val ink = SargamInk.of(FinioTheme.colors.isDark)
        val coinInk = coinInk(FinioTheme.colors.isDark)
        return Modifier
            .border(1.5.dp, ink.actD, shape)
            .drawBehind {
                drawCircle(coinInk.copy(alpha = 0.5f), radius = size.minDimension / 2f - 6.dp.toPx() - 0.75.dp.toPx(), style = Stroke(1.5.dp.toPx()))
            }
    }

    // ── progress: the 12-bar level meter, cut through track and fill alike ──
    override val progressTrackHeight: Dp get() = 14.dp

    @Composable
    override fun progressDecoration(): Modifier {
        val ink = SargamInk.of(FinioTheme.colors.isDark)
        val gap = FinioTheme.colors.muted
        return Modifier.drawWithContent {
            drawRect(ink.off)
            drawContent()
            val seg = size.width / 12f
            val g = 3.dp.toPx()
            for (k in 0 until 12) drawRect(gap, Offset((k + 1) * seg - g, 0f), Size(g, size.height))
        }
    }

    // ── hero: the J-card label, the stripe run turning its top-right corner ──
    @Composable
    override fun heroContainer(shape: Shape): Modifier {
        val ink = SargamInk.of(FinioTheme.colors.isDark)
        val card = FinioTheme.colors.card
        val wide = LocalConfiguration.current.screenWidthDp >= 1024
        val sw = (if (wide) 7 else 5).dp
        val ri = (if (wide) 18 else 14).dp
        return Modifier
            .clip(shape)
            .background(card)
            .drawBehind {
                val w = size.width
                val s = sw.toPx()
                val r = ri.toPx()
                val band = s * 5f
                val stripes = ink.stripes
                // top band: five blocks left to right (sg-run-h)
                val topW = w - band - r
                for (i in 0 until 5) drawRect(stripes[i], Offset(topW * i / 5f, 0f), Size(topW / 5f + 0.5f, band))
                // right band: vertical stripes, tangerine on the outer edge (sg-run-hr)
                val rightH = size.height - band - r
                for (i in 0 until 5) drawRect(stripes[4 - i], Offset(w - band + s * i, band + r), Size(s, rightH))
                // the corner: concentric quarter rings, teal inside to tangerine outside
                val cx = w - band - r
                val cy = band + r
                for (i in 0 until 5) {
                    val rad = r + s * (i + 0.5f)
                    drawArc(
                        stripes[4 - i], 270f, 90f, false,
                        Offset(cx - rad, cy - rad), Size(rad * 2, rad * 2), style = Stroke(s),
                    )
                }
                // the thin inner frame
                val inset = 3.dp.toPx()
                drawRoundRect(
                    ink.rule, Offset(inset, inset), Size(w - 2 * inset, size.height - 2 * inset),
                    CornerRadius(8.dp.toPx()), style = Stroke(1.dp.toPx()),
                )
            }
            .border(1.5.dp, ink.line, shape)
    }

    @Composable
    private fun bandDp(): Dp = (if (LocalConfiguration.current.screenWidthDp >= 1024) 35 else 25).dp

    override fun heroPadding(wide: Boolean): PaddingValues {
        val band = if (wide) 35 else 25
        return PaddingValues(18.dp, (band + 12).dp, (band + 14).dp, 14.dp)
    }

    @Composable
    override fun HeroFigure(text: String, style: TextStyle, color: Color, whole: Boolean, modifier: Modifier): Boolean {
        val wide = LocalConfiguration.current.screenWidthDp >= 1024
        // The shared style steps 44 → 36 → 28sp with length; Michroma is wide, so the whole ladder drops.
        val ratio = style.fontSize.value / 44f
        val size = ((if (wide) 52f else 38f) * ratio).sp
        val annotated = buildAnnotatedString {
            text.forEach { ch ->
                if (ch == '₹') {
                    pushStyle(SpanStyle(fontFamily = Lexend, fontWeight = FontWeight(500)))
                    append(ch)
                    pop()
                } else append(ch)
            }
        }
        Text(
            annotated,
            modifier.padding(top = 4.dp),
            style = style.copy(fontSize = size, lineHeight = size * 1.05f, fontWeight = FontWeight.Normal, letterSpacing = (-0.01).em),
            color = color,
            maxLines = 1,
            softWrap = false,
        )
        return true
    }

    override val replacesHeroBar: Boolean get() = true

    @Composable
    override fun HeroFooter(elapsed: Float, spentPercent: Float, isOver: Boolean, daysLeft: Int) {
        val ink = SargamInk.of(FinioTheme.colors.isDark)
        val colors = FinioTheme.colors
        val reduced = rememberReducedMotion()

        // Exact period from LocalHeroPeriod; the estimate from elapsed fraction and days left is the fallback.
        val period = LocalHeroPeriod.current
        val left = daysLeft.coerceAtLeast(0)
        val e = elapsed.coerceIn(0f, 1f)
        val total = period?.totalDays?.coerceAtLeast(1) ?: when {
            left == 0 -> if (e > 0f) 30 else 1
            e >= 0.999f -> left + 1
            else -> (left / (1f - e)).roundToInt().coerceAtLeast(left + 1)
        }
        val exactDay = period?.dayOfPeriod?.coerceIn(1, total)
        val gone = if (exactDay != null) (exactDay - 1).coerceIn(0, total) else (total - left).coerceIn(0, total)
        val day = exactDay ?: if (left == 0) total else (gone + 1).coerceAtMost(total)
        val rl = packRadius(left.toFloat() / total)
        val rr = packRadius(gone.toFloat() / total)
        val pct = spentPercent.coerceAtLeast(0f).roundToInt()
        val lit = ((pct.coerceAtMost(100) * 12) / 100f).roundToInt().coerceIn(0, 12)

        val spin = remember { Animatable(if (reduced) 1f else 0f) }
        LaunchedEffect(Unit) { if (!reduced) spin.animateTo(1f, tween(1400, easing = ReelEase)) }
        val angle = -120f * (1f - spin.value)

        Column(Modifier.padding(top = 8.dp).widthIn(max = 480.dp).clearAndSetSemantics { }) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.weight(1f).height(16.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    for (k in 0 until 12) {
                        val on = k < lit
                        val tint = when {
                            !on -> ink.off
                            k < 8 -> ink.stripes[4]
                            k < 10 -> ink.stripes[1]
                            else -> ink.stripes[2]
                        }
                        Box(Modifier.weight(1f).height(16.dp).clip(RoundedCornerShape(2.5.dp)).background(tint))
                    }
                }
                Text("$pct% used", style = Body.copy(fontWeight = FontWeight(600)), color = colors.foreground, maxLines = 1)
            }
            Canvas(Modifier.padding(top = 10.dp).fillMaxWidth().aspectRatio(300f / 96f)) {
                val s = size.width / 300f
                scale(s, s, Offset.Zero) { drawTape(ink, rl, rr, angle) }
            }
            Row(Modifier.fillMaxWidth().padding(top = 5.dp), verticalAlignment = Alignment.Bottom) {
                Text(
                    "$left day${if (left == 1) "" else "s"} left", Modifier.weight(1f),
                    style = Body.copy(fontSize = 13.sp), color = colors.mutedForeground, maxLines = 1,
                )
                Text(
                    "Day $day of $total",
                    style = Body.copy(fontSize = 13.sp, fontWeight = FontWeight(600)), color = colors.foreground, maxLines = 1,
                )
                Text(
                    "$gone day${if (gone == 1) "" else "s"} gone", Modifier.weight(1f),
                    style = Body.copy(fontSize = 13.sp), color = colors.mutedForeground, maxLines = 1, textAlign = TextAlign.End,
                )
            }
        }
    }

    // ── chrome: a recessed deck of piano keys; the current page is pressed and lit ──
    @Composable
    override fun TabBar(active: FinioTab?, onSelect: (FinioTab) -> Unit, modifier: Modifier) {
        val colors = FinioTheme.colors
        val ink = SargamInk.of(colors.isDark)
        Row(
            modifier
                .fillMaxWidth()
                .background(colors.glassStrong)
                .drawBehind {
                    drawRect(Color.Black.copy(alpha = 0.07f), Offset.Zero, Size(size.width, 4.dp.toPx()))
                    drawLine(ink.line, Offset(0f, 0.75.dp.toPx()), Offset(size.width, 0.75.dp.toPx()), 1.5.dp.toPx())
                }
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.Top,
        ) {
            FinioTab.entries.forEach { tab ->
                val selected = tab == active
                val tint = if (selected) ink.onAct else colors.mutedForeground
                val icon = remember(tab) { tab.icon(1.75f) }
                val edge = if (selected) 2.dp else 4.dp
                Column(
                    Modifier
                        .weight(1f)
                        .offset(y = if (selected) 2.dp else 0.dp)
                        .drawBehind {
                            val r = 8.dp.toPx()
                            drawRoundRect(if (selected) ink.actD else ink.edge, Offset.Zero, size, CornerRadius(r))
                            val e = edge.toPx()
                            drawRoundRect(
                                if (selected) ink.act else ink.key, Offset(1.dp.toPx(), 1.dp.toPx()),
                                Size(size.width - 2.dp.toPx(), size.height - 1.dp.toPx() - e), CornerRadius(r - 1.dp.toPx()),
                            )
                            if (!selected) drawLine(ink.hi, Offset(r, 1.5.dp.toPx()), Offset(size.width - r, 1.5.dp.toPx()), 1.dp.toPx())
                        }
                        .tabClickable(selected) { onSelect(tab) }
                        .padding(start = 2.dp, end = 2.dp, top = 7.dp, bottom = 4.dp + edge),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = tint)
                    Text(
                        tab.label,
                        style = Body.copy(fontSize = 10.sp, fontWeight = FontWeight(500), letterSpacing = (-0.01).em),
                        color = tint, maxLines = 1,
                    )
                }
            }
        }
    }
}

// ───────────────────────────── tape window ─────────────────────────────

private const val CY = 44f
private const val HUB = 13f
private const val RMAX = 37f
private const val LX = 76f
private const val RX = 224f

/** A pack's area is proportional to the days it stands for (sargam.tsx `packRadius`). */
private fun packRadius(f: Float): Float = sqrt(HUB * HUB + f.coerceIn(0f, 1f) * (RMAX * RMAX - HUB * HUB))

/** Tangent point from (px, py) to a reel pack, the left- or right-most of the two. */
private fun tangent(px: Float, py: Float, cx: Float, r: Float, first: Boolean): Offset {
    val d = hypot(px - cx, py - CY)
    val a = atan2(py - CY, px - cx)
    val b = acos((r / d).coerceAtMost(1f))
    val pts = listOf(a + b, a - b).map { Offset(cx + r * cos(it), CY + r * sin(it)) }.sortedBy { it.x }
    return if (first) pts[0] else pts[1]
}

private fun DrawScope.drawTape(ink: SargamInk, rl: Float, rr: Float, hubAngle: Float) {
    drawRoundRect(ink.win, Offset(0.75f, 0.75f), Size(298.5f, 94.5f), CornerRadius(12f))
    drawRoundRect(ink.winl, Offset(0.75f, 0.75f), Size(298.5f, 94.5f), CornerRadius(12f), style = Stroke(1.5f))
    val a = tangent(13f, 80f, LX, rl, true)
    val b = tangent(287f, 80f, RX, rr, false)
    val path = Path().apply {
        moveTo(a.x, a.y)
        lineTo(13f, 80f)
        quadraticTo(13f, 89f, 22f, 89f)
        lineTo(278f, 89f)
        quadraticTo(287f, 89f, 287f, 80f)
        lineTo(b.x, b.y)
    }
    drawPath(path, ink.groove, style = Stroke(1.5f, join = StrokeJoin.Round))
    drawCircle(ink.hub, 3f, Offset(13f, 82f))
    drawCircle(ink.hub, 3f, Offset(287f, 82f))
    drawReel(ink, LX, rl, hubAngle)
    drawReel(ink, RX, rr, hubAngle)
}

private fun DrawScope.drawReel(ink: SargamInk, cx: Float, r: Float, hubAngle: Float) {
    translate(cx, CY) {
        drawCircle(
            ink.full, RMAX, Offset.Zero,
            style = Stroke(1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(1.5f, 3.5f))),
        )
        drawCircle(ink.pack, r, Offset.Zero)
        drawCircle(ink.groove.copy(alpha = 0.7f), (r + HUB) / 2f, Offset.Zero, style = Stroke(0.75f))
        drawCircle(ink.groove.copy(alpha = 0.7f), r - 2.5f, Offset.Zero, style = Stroke(0.75f))
        rotate(hubAngle, Offset.Zero) {
            drawCircle(ink.hub, HUB, Offset.Zero)
            drawCircle(ink.win, 9.2f, Offset.Zero)
            for (k in 0 until 6) {
                rotate(k * 60f, Offset.Zero) {
                    drawRoundRect(ink.hub, Offset(-1.5f, -9.6f), Size(3f, 4.4f), CornerRadius(0.6f))
                }
            }
        }
    }
}

/** `cubic-bezier(.16,1,.3,1)` — the hubs turn a third and settle. */
private val ReelEase = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

// ───────────────────────────── inks ─────────────────────────────

private fun c(rgb: Long) = Color(0xFF000000 or rgb)

/** The theme's own tokens that are not part of [FinioColors] (`--sg-*`). */
private class SargamInk(
    val stripes: List<Color>,
    val line: Color, val rule: Color, val key: Color, val edge: Color, val hi: Color,
    val act: Color, val actD: Color, val onAct: Color, val off: Color,
    val win: Color, val winl: Color, val pack: Color, val groove: Color, val hub: Color, val full: Color,
) {
    companion object {
        private val Light = SargamInk(
            stripes = listOf(c(0xF26B1D), c(0xE3AA1E), c(0xD0214F), c(0x5C2A3D), c(0x188078)),
            line = c(0x3A2315), rule = c(0xCDC7BC), key = c(0xFBFAF6), edge = c(0xB3AA9D), hi = Color.White.copy(alpha = 0.95f),
            act = c(0xF26B1D), actD = c(0xB04608), onAct = c(0x2A150A), off = c(0xD2CEC5),
            win = c(0x2A1A12), winl = c(0x3A2315), pack = c(0x70493A), groove = c(0x8D6450), hub = c(0xF1ECE3),
            full = rgba(241, 236, 227, 0.34f),
        )
        private val Dark = SargamInk(
            stripes = listOf(c(0xFF8A3A), c(0xF5C443), c(0xF24A74), c(0x93506C), c(0x31B7A8)),
            line = c(0x5E493C), rule = c(0x3F3027), key = c(0x35271F), edge = c(0x090605), hi = rgba(255, 232, 200, 0.12f),
            act = c(0xFF8A3A), actD = c(0x9C450B), onAct = c(0x24130A), off = c(0x3E2F26),
            win = c(0x0C0806), winl = c(0x5E493C), pack = c(0x7D5643), groove = c(0x9C7059), hub = c(0xF4EADC),
            full = rgba(255, 184, 77, 0.5f),
        )
        fun of(dark: Boolean) = if (dark) Dark else Light
    }
}

// ───────────────────────────── type & shape ─────────────────────────────

private fun variable(res: Int, weight: Int) = Font(
    resId = res,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

private val Michroma = FontFamily(Font(R.font.sargam_michroma, FontWeight.Normal))
private val Lexend = FontFamily(
    variable(R.font.sargam_lexend, 400), variable(R.font.sargam_lexend, 500),
    variable(R.font.sargam_lexend, 600), variable(R.font.sargam_lexend, 700),
)

private val Scale = FinioTypeScale(
    Michroma, Michroma, Lexend,
    displayWeight = 400, displayTracking = -0.01f, pageTitleWeight = 400, screenTitleWeight = 400,
    headlineWeight = 400, moneyWeight = 400, dialogTitleWeight = 400, titleWeight = 500,
)
private val Shapes = FinioShapeScale(base = 11.dp, full = RoundedCornerShape(9.dp))

private val Body = TextStyle(fontFamily = Lexend, fontWeight = FontWeight(400), fontSize = 13.sp, fontFeatureSettings = "tnum")

// ───────────────────────────── palettes ─────────────────────────────

private fun solid(rgb: Long): Brush = SolidColor(c(rgb))

private fun colorsOf(
    dark: Boolean,
    background: Long, foreground: Long, card: Long, popover: Long,
    primary: Long, primaryFg: Long, secondary: Long, mutedFg: Long,
    accent: Long, destructive: Long, destructiveFg: Long,
    positive: Long, warning: Long, border: Long, input: Long, ring: Long,
    charts: List<Long>,
    warningBand: Long, warningBandFg: Long, warningBandAccent: Long,
    glassStrong: Long, glassBorder: Long, shadowTint: Color, scrim: Color,
) = FinioColors(
    isDark = dark,
    background = c(background), foreground = c(foreground),
    card = c(card), cardForeground = c(foreground),
    popover = c(popover), popoverForeground = c(foreground),
    primary = c(primary), primaryForeground = c(primaryFg),
    secondary = c(secondary), secondaryForeground = c(foreground),
    muted = c(secondary), mutedForeground = c(mutedFg),
    accent = c(accent), accentForeground = c(foreground),
    destructive = c(destructive), destructiveForeground = c(destructiveFg),
    positive = c(positive), warning = c(warning),
    border = c(border), input = c(input), ring = c(ring),
    chart1 = c(charts[0]), chart2 = c(charts[1]), chart3 = c(charts[2]), chart4 = c(charts[3]), chart5 = c(charts[4]),
    sidebar = c(card), sidebarForeground = c(foreground),
    sidebarPrimary = c(primary), sidebarPrimaryForeground = c(primaryFg),
    sidebarAccent = c(accent), sidebarAccentForeground = c(foreground),
    sidebarBorder = c(glassBorder), sidebarRing = c(ring),
    warningBand = c(warningBand), warningBandForeground = c(warningBandFg), warningBandAccent = c(warningBandAccent),
    engraving = Color.Transparent,
    glass = c(card), glassStrong = c(glassStrong), glassBorder = c(glassBorder), glassHighlight = Color.Transparent,
    shadowTint = shadowTint, scrim = scrim,
)

/** Flat: the only "shadow" is the key edge, which the pieces draw themselves; the coin keeps its 3dp drop. */
private fun shadowsOf(tint: Color, coinDrop: Color) = stock(tint).let { s ->
    FinioShadows(
        card = emptyList(), float = emptyList(), glowPrimary = emptyList(), glowSuccess = emptyList(), glowDanger = emptyList(),
        coin = listOf(CssShadow(y = 3.dp, color = coinDrop)), tile = emptyList(), tabBar = emptyList(), noteChip = emptyList(),
        xs = s[0], sm = s[1], md = s[2], lg = s[3], xl = s[4],
    )
}

/** Account tiles: the cassette's label colours as flat fills. */
private fun sargamNotes(tints: List<Triple<Denomination, Long, Long>>) =
    tints.associate { (d, bg, ink) -> d to NoteStyle(c(bg), c(bg), c(ink)) }

private val LightPalette = ThemePalette(
    colors = colorsOf(
        false, 0xECEBE6, 0x3A2315, 0xF8F7F3, 0xFBFAF6, 0xB04608, 0xFFFFFF, 0xE4E1D9, 0x6A564A,
        0xF6D9C4, 0xB51643, 0xFFFFFF, 0x0D6E63, 0xE3AA1E, 0xCDC7BC, 0xA89F92, 0xF26B1D,
        charts = listOf(0xF26B1D, 0x188078, 0xD0214F, 0xE3AA1E, 0x5C2A3D),
        warningBand = 0xFBE9C4, warningBandFg = 0x4A2F00, warningBandAccent = 0xE3AA1E,
        glassStrong = 0xDCD9D1, glassBorder = 0x3A2315,
        shadowTint = Color(58, 35, 21), scrim = rgba(42, 21, 10, 0.45f),
    ),
    brushes = FinioBrushes(
        gradPrimary = solid(0xB04608), gradPrimarySoft = solid(0xF6D9C4),
        gradSuccess = solid(0x0D6E63), gradSuccessSoft = solid(0xD3EBE6),
        gradDanger = solid(0xB51643), gradDangerSoft = solid(0xF6D5DE),
        gradInfo = solid(0x5C2A3D), gradWarning = solid(0x8A5F00), gradSurface = solid(0xF8F7F3),
        paper = SolidColor(Color.Transparent), coin = solid(0xF26B1D),
        register = { _ -> SolidColor(c(0x188078)) }, thread = solid(0x188078),
    ),
    shadows = shadowsOf(Color(58, 35, 21), c(0xB04608)),
    spec = FinioSpec(
        VisualTheme.Sargam, 1.dp,
        sargamNotes(
            listOf(
                Triple(Denomination.Rs100, 0xF9DCC8, 0x5A2208), Triple(Denomination.Rs500, 0xE6E2D8, 0x3A2315),
                Triple(Denomination.Rs2000, 0xF3C6D2, 0x7D0E2D), Triple(Denomination.Rs200, 0xF6E2A8, 0x4A3300),
                Triple(Denomination.Rs10, 0xC9E6E1, 0x0A463F), Triple(Denomination.Rs50, 0xE6D3DA, 0x4A2132),
                Triple(Denomination.Rs20, 0xD8ECE6, 0x0A463F),
            ),
        ),
    ),
)

private val DarkPalette = ThemePalette(
    colors = colorsOf(
        true, 0x1A1310, 0xF4EADC, 0x241A15, 0x2B1F19, 0xFF8A3A, 0x24130A, 0x2F231C, 0xBBA898,
        0x3A281D, 0xFF7896, 0x24130A, 0x56D3C0, 0xF5C443, 0x3F3027, 0x6E5848, 0xFFB84D,
        charts = listOf(0xFF8A3A, 0x31B7A8, 0xF24A74, 0xF5C443, 0xC488A3),
        warningBand = 0x3A2A10, warningBandFg = 0xFFE9B8, warningBandAccent = 0xF5C443,
        glassStrong = 0x110C09, glassBorder = 0x5E493C,
        shadowTint = Color(0, 0, 0), scrim = rgba(0, 0, 0, 0.65f),
    ),
    brushes = FinioBrushes(
        gradPrimary = solid(0x9C450B), gradPrimarySoft = solid(0x3A281D),
        gradSuccess = solid(0x0D6E63), gradSuccessSoft = solid(0x143832),
        gradDanger = solid(0xA8153F), gradDangerSoft = solid(0x41202A),
        gradInfo = solid(0x7A3A58), gradWarning = solid(0x8A5F00), gradSurface = solid(0x241A15),
        paper = SolidColor(Color.Transparent), coin = solid(0xFF8A3A),
        register = { _ -> SolidColor(c(0x31B7A8)) }, thread = solid(0x31B7A8),
    ),
    shadows = shadowsOf(Color(0, 0, 0), c(0x9C450B)),
    spec = FinioSpec(
        VisualTheme.Sargam, 1.dp,
        sargamNotes(
            listOf(
                Triple(Denomination.Rs100, 0x4A2C1A, 0xFFC9A3), Triple(Denomination.Rs500, 0x35291F, 0xEADFCE),
                Triple(Denomination.Rs2000, 0x4A2230, 0xFFB0C4), Triple(Denomination.Rs200, 0x463818, 0xF8DD8A),
                Triple(Denomination.Rs10, 0x173B36, 0x9FE6DA), Triple(Denomination.Rs50, 0x3A2430, 0xE8C3D4),
                Triple(Denomination.Rs20, 0x183A33, 0xA9EBDD),
            ),
        ),
    ),
)
