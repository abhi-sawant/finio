package com.slowatcoding.finio.ui.theme.kits

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.slowatcoding.finio.R
import com.slowatcoding.finio.ui.mudra.Denomination
import com.slowatcoding.finio.ui.mudra.NoteStyle
import com.slowatcoding.finio.ui.navigation.FinioTab
import com.slowatcoding.finio.ui.shell.tabClickable
import com.slowatcoding.finio.ui.theme.CssRepeatingLinearGradient
import com.slowatcoding.finio.ui.theme.FinioBrushes
import com.slowatcoding.finio.ui.theme.FinioColors
import com.slowatcoding.finio.ui.theme.FinioShadows
import com.slowatcoding.finio.ui.theme.FinioShapeScale
import com.slowatcoding.finio.ui.theme.FinioSpec
import com.slowatcoding.finio.ui.theme.FinioTheme
import com.slowatcoding.finio.ui.theme.FinioTypeScale
import com.slowatcoding.finio.ui.theme.Stop
import com.slowatcoding.finio.ui.theme.ThemePalette
import com.slowatcoding.finio.ui.theme.VisualTheme
import com.slowatcoding.finio.ui.theme.rememberReducedMotion
import com.slowatcoding.finio.ui.theme.rgba
import com.slowatcoding.finio.ui.theme.stock
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Takhti — the writing board (web/src/themes/takhti.css). A green-black chalkboard by night, a
 * whiteboard by day; no filled cards, structure is hand-ruled lines. The web roughens decorative
 * strokes with an SVG turbulence filter; here they are jittered paths with a fixed seed (never
 * random per frame), and text and figures are never touched. Kalam writes (titles, the hero
 * figure), Nunito Sans counts (every list figure). Signature: the double scribbled underline under
 * the hero figure that draws itself, and the period tallied in gates of five ([HeroFooter]).
 */
object TakhtiKit : ThemeKit {
    override val visual = VisualTheme.Takhti
    override fun palette(dark: Boolean): ThemePalette = if (dark) Dark else Light
    override val type: FinioTypeScale get() = Scale
    override val shapes: FinioShapeScale get() = Shapes
    override fun coinInk(dark: Boolean) = c(if (dark) 0x1D2B25 else 0xF4F7F9)
    override val coinShape: Shape get() = RoundedCornerShape(52, 48, 50, 50)
    override val fabBottom: Dp get() = 96.dp

    // ── progress: a ruled box struck in diagonal hatch ──
    override val progressTrackHeight: Dp get() = 24.dp

    @Composable
    override fun progressDecoration(): Modifier {
        val colors = FinioTheme.colors
        val line = lineColor(colors)
        val bg = colors.background
        return Modifier
            .drawWithCache {
                val d = density
                val hatch = hatchPath(size.width, size.height, d)
                val box = roughRect(size.width, size.height, inset = 1.5f * d, amp = 0.7f * d, seed = 3, step = 22f * d)
                onDrawWithContent {
                    // The track is the board itself: hide the shared muted background.
                    drawRect(bg)
                    drawIntoCanvas { it.saveLayer(Rect(Offset.Zero, size), Paint()) }
                    drawContent()
                    drawPath(hatch, Color.Black, blendMode = BlendMode.DstIn)
                    drawIntoCanvas { it.restore() }
                    drawPath(box, line, style = Stroke(1.7f * d, cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
            }
            .padding(4.5.dp)
    }

    // ── hero: the working shown ──
    override fun heroPadding(wide: Boolean) = PaddingValues(0.dp, 4.dp, 0.dp, 0.dp)

    @Composable
    override fun HeroFigure(text: String, style: TextStyle, color: Color, whole: Boolean, modifier: Modifier): Boolean {
        val reduced = rememberReducedMotion()
        val draw = remember { Animatable(if (reduced) 1f else 0f) }
        LaunchedEffect(Unit) { if (!reduced) draw.animateTo(1f, tween(500, delayMillis = 250, easing = ScribbleEase)) }
        val hi = FinioTheme.colors.primary
        val sized = style.copy(
            fontFamily = Kalam, fontWeight = FontWeight.W700, fontSize = 64.sp, lineHeight = 66.sp,
            letterSpacing = (-0.01).em,
        )
        Column(modifier.width(IntrinsicSize.Max)) {
            Text(text, style = sized, color = color, maxLines = 1, softWrap = false)
            Canvas(Modifier.fillMaxWidth().padding(top = 2.dp).height(14.dp).clearAndSetSemantics { }) {
                val sx = size.width / 300f
                val sy = size.height / 14f
                fun p(x: Float, y: Float) = Offset(x * sx, y * sy)
                val first = Path().apply {
                    moveTo(p(3f, 4f).x, p(3f, 4f).y)
                    cubicTo(p(90f, 1f).x, p(90f, 1f).y, p(210f, 8f).x, p(210f, 8f).y, p(297f, 3.5f).x, p(297f, 3.5f).y)
                }
                val second = Path().apply {
                    moveTo(p(297f, 3.5f).x, p(297f, 3.5f).y)
                    cubicTo(p(210f, 9f).x, p(210f, 9f).y, p(90f, 11f).x, p(90f, 11f).y, p(7f, 11f).x, p(7f, 11f).y)
                }
                val t = draw.value
                val stroke = Stroke(2.2.dp.toPx(), cap = StrokeCap.Round)
                drawPartial(first, (t * 2f).coerceIn(0f, 1f), hi, stroke)
                drawPartial(second, (t * 2f - 1f).coerceIn(0f, 1f), hi, stroke)
            }
        }
        return true
    }

    /**
     * What Android's hero does not hand a kit, so the web's written-out sum ("limit budget − spent
     * spent = remaining left", "remaining ÷ N days") is not drawn: the shared text lines already
     * state the remaining amount and the days to go.
     */
    @Composable
    override fun HeroFooter(elapsed: Float, spentPercent: Float, isOver: Boolean, daysLeft: Int) {
        val colors = FinioTheme.colors
        val reduced = rememberReducedMotion()
        val ring = remember { Animatable(if (reduced) 1f else 0f) }
        LaunchedEffect(Unit) { if (!reduced) ring.animateTo(1f, tween(450, delayMillis = 750, easing = ScribbleEase)) }

        // Exact period from LocalHeroPeriod; the estimate from the share elapsed and days left is the fallback.
        val period = LocalHeroPeriod.current
        val e = elapsed.coerceIn(0f, 0.999f)
        val left = max(daysLeft, 0)
        val total = period?.totalDays?.coerceAtLeast(1) ?: max(1, if (left == 0) 1 else (left / (1f - e)).roundToInt())
        val today = period?.dayOfPeriod?.coerceIn(1, total) ?: (total - left + 1).coerceIn(1, total)
        val long = total > TallyMaxDays

        Column(Modifier.padding(top = 12.dp).clearAndSetSemantics { }) {
            if (long) HatchedPeriod(elapsed.coerceIn(0f, 1f), colors) else Tally(total, today, ring.value, colors)
            Row(Modifier.padding(top = 4.dp), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)) {
                Text("Day $today of $total", style = CaptionStyle.copy(fontWeight = FontWeight.W700), color = colors.primary, maxLines = 1)
                Text("$left day${if (left == 1) "" else "s"} to go", style = CaptionStyle, color = colors.mutedForeground, maxLines = 1)
                Text("${spentPercent.roundToInt()}% spent", style = CaptionStyle, color = colors.mutedForeground, maxLines = 1)
            }
        }
    }

    // ── chrome: a ledge along the top, the current tab ringed in the highlight ──
    @Composable
    override fun TabBar(active: FinioTab?, onSelect: (FinioTab) -> Unit, modifier: Modifier) {
        val colors = FinioTheme.colors
        val dark = colors.isDark
        val hi = colors.primary
        val bar = when {
            dark && colors.background.red < 0.02f -> Color.Black
            dark -> c(0x15201B)
            else -> c(0xE6EAEE)
        }
        val ledge = if (dark) {
            listOf(c(0x9A6C40), c(0x6F4C2B), c(0x432D19))
        } else {
            listOf(c(0xFBFCFD), c(0xCDD2D8), c(0x989FA8))
        }
        val ledgeShadow = if (dark) rgba(0, 0, 0, 0.45f) else rgba(40, 50, 70, 0.22f)
        Column(modifier.fillMaxWidth()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .drawBehind {
                        drawRect(
                            Brush.verticalGradient(
                                0f to ledge[0], 0.45f to ledge[1], 1f to ledge[2],
                            ),
                        )
                        // two bits of chalk resting on the ledge
                        val y = -4.dp.toPx()
                        drawRect(c(0xF2EFE6), Offset(24.dp.toPx(), y), Size(19.dp.toPx(), 5.dp.toPx()))
                        drawRect(c(0xF7D75E), Offset(49.dp.toPx(), y), Size(12.dp.toPx(), 5.dp.toPx()))
                    },
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(bar)
                    .drawBehind {
                        drawRect(
                            Brush.verticalGradient(listOf(ledgeShadow, Color.Transparent), 0f, 5.dp.toPx()),
                            size = Size(size.width, 5.dp.toPx()),
                        )
                    }
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FinioTab.entries.forEach { tab ->
                    val selected = tab == active
                    val tint = if (selected) hi else colors.mutedForeground
                    val icon = remember(tab) { tab.icon(1.9f) }
                    Column(
                        Modifier.weight(1f).tabClickable(selected) { onSelect(tab) }.padding(vertical = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            Modifier
                                .size(46.dp, 33.dp)
                                .drawBehind {
                                    if (selected) {
                                        drawPath(
                                            ringPath(size.width / 2f, size.height / 2f, size.width * 0.46f, size.height * 0.46f),
                                            hi,
                                            style = Stroke(2.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                                        )
                                    }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = tint)
                        }
                        Text(
                            tab.label,
                            style = TabStyle.copy(fontWeight = if (selected) FontWeight.W700 else FontWeight.W400),
                            color = tint,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

// ───────────────────────────── drawing helpers ─────────────────────────────

private const val TallyMaxDays = 62
private const val GateW = 44f

/** `cubic-bezier(.2,.7,.2,1)` — the scribble draws itself. */
private val ScribbleEase = CubicBezierEasing(0.2f, 0.7f, 0.2f, 1f)

/** The tk-ink / tk-faint pair of the web: black marker or cream chalk, and the same at low alpha. */
private fun lineColor(colors: FinioColors): Color = colors.foreground.copy(alpha = if (colors.isDark) 0.7f else 0.72f)

/** Deterministic noise in -1..1 (no randomness per frame). */
private fun noise(i: Int, seed: Int): Float {
    val s = sin((i * 12.9898 + seed * 78.233)) * 43758.5453
    return ((s - floor(s)) * 2.0 - 1.0).toFloat()
}

/** A closed rectangle whose outline wanders a little: points every [step] px, smoothed. */
private fun roughRect(w: Float, h: Float, inset: Float, amp: Float, seed: Int, step: Float): Path {
    val l = inset
    val t = inset
    val r = w - inset
    val b = h - inset
    val pts = ArrayList<Offset>()
    var i = 0
    fun edge(x0: Float, y0: Float, x1: Float, y1: Float) {
        val len = kotlin.math.hypot(x1 - x0, y1 - y0)
        val n = max(1, (len / step).roundToInt())
        for (k in 0 until n) {
            val f = k / n.toFloat()
            pts.add(Offset(x0 + (x1 - x0) * f + noise(i, seed) * amp, y0 + (y1 - y0) * f + noise(i + 101, seed) * amp))
            i++
        }
    }
    edge(l, t, r, t); edge(r, t, r, b); edge(r, b, l, b); edge(l, b, l, t)
    val path = Path()
    val first = Offset((pts[0].x + pts[pts.size - 1].x) / 2f, (pts[0].y + pts[pts.size - 1].y) / 2f)
    path.moveTo(first.x, first.y)
    for (k in pts.indices) {
        val p = pts[k]
        val nx = pts[(k + 1) % pts.size]
        path.quadraticTo(p.x, p.y, (p.x + nx.x) / 2f, (p.y + nx.y) / 2f)
    }
    path.close()
    return path
}

/** `repeating-linear-gradient(-56deg, ink 0 2.1px, transparent 2.1px 5.6px)` as parallelograms. */
private fun hatchPath(w: Float, h: Float, density: Float, period: Float = 6.75f, width: Float = 2.53f): Path {
    val slope = 0.674f
    val p = period * density
    val sw = width * density
    val shift = slope * h
    val path = Path()
    var x = -shift - p
    while (x < w + p) {
        path.moveTo(x, h)
        path.lineTo(x + sw, h)
        path.lineTo(x + sw + shift, 0f)
        path.lineTo(x + shift, 0f)
        path.close()
        x += p
    }
    return path
}

/** A hand-drawn ring around a centre that overshoots where it starts (web `ringPath`). */
private fun ringPath(cx: Float, cy: Float, rx: Float, ry: Float): Path {
    fun px(x: Float) = cx + x * rx
    fun py(y: Float) = cy + y * ry
    return Path().apply {
        moveTo(px(-0.55f), py(-0.85f))
        cubicTo(px(0.1f), py(-1.15f), px(1f), py(-0.8f), px(1f), py(0f))
        cubicTo(px(1f), py(0.8f), px(0.3f), py(1.05f), px(-0.3f), py(0.95f))
        cubicTo(px(-0.9f), py(0.85f), px(-1.05f), py(0.2f), px(-0.9f), py(-0.35f))
        cubicTo(px(-0.75f), py(-0.85f), px(-0.2f), py(-1.08f), px(0.5f), py(-0.92f))
    }
}

/** Draws the first [fraction] of [path]'s length (`stroke-dashoffset` draw-on). */
private fun DrawScope.drawPartial(path: Path, fraction: Float, color: Color, stroke: Stroke) {
    if (fraction <= 0f) return
    if (fraction >= 1f) {
        drawPath(path, color, style = stroke)
        return
    }
    val m = PathMeasure()
    m.setPath(path, false)
    val seg = Path()
    m.getSegment(0f, m.length * fraction, seg, true)
    drawPath(seg, color, style = stroke)
}

private val Jit = floatArrayOf(0f, 0.9f, -0.7f, 0.5f, -1f, 0.4f, 0.8f, -0.5f)
private fun jit(n: Int) = Jit[((n % 8) + 8) % 8]

/** One stroke per day, struck through in gates of five; days gone inked, today ringed, the rest faint. */
@Composable
private fun Tally(total: Int, today: Int, ring: Float, colors: FinioColors) {
    val gates = ceil(total / 5f).toInt()
    val vbW = gates * GateW + 4f
    val ink = colors.foreground
    val faint = ink.copy(alpha = if (colors.isDark) 0.28f else 0.2f)
    val hi = colors.primary
    Canvas(
        Modifier
            .widthIn(max = vbW.dp)
            .fillMaxWidth()
            .aspectRatio(vbW / 46f),
    ) {
        val s = size.width / vbW
        fun line(x0: Float, y0: Float, x1: Float, y1: Float, day: Int) {
            val color = when {
                day < today -> ink
                day == today -> hi
                else -> faint
            }
            val w = (if (day == today) 3f else 2.5f) * s
            drawLine(color, Offset(x0 * s, y0 * s), Offset(x1 * s, y1 * s), w, StrokeCap.Round)
        }
        for (g in 0 until gates) {
            val gx = 12f + g * GateW
            val inGate = min(5, total - g * 5)
            for (k in 0 until min(inGate, 4)) {
                val day = g * 5 + k + 1
                val x = gx + k * 8f
                line(x + jit(day), 9f + jit(day + 2), x - jit(day + 3), 37f + jit(day + 5), day)
            }
            if (inGate == 5) {
                val day = g * 5 + 5
                line(gx - 6f, 31f + jit(g), gx + 30f, 14f - jit(g + 1), day)
            }
        }
        val tg = (today - 1) / 5
        val ringX = 12f + tg * GateW + 12f
        val p = ringPath(ringX, 23f, 25f, 21f)
        // scale the unit-less viewBox path into pixels
        val scaled = Path().apply { addPath(p); transform(androidx.compose.ui.graphics.Matrix().apply { scale(s, s) }) }
        drawPartial(scaled, ring, hi, Stroke(2.2f * s, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/** For a long period: one hatched bar like a budget bar, today marked by an upright stroke. */
@Composable
private fun HatchedPeriod(elapsed: Float, colors: FinioColors) {
    val ink = colors.foreground
    val hi = colors.primary
    val line = lineColor(colors)
    Canvas(Modifier.fillMaxWidth().height(24.dp)) {
        val d = density
        val box = roughRect(size.width, size.height, 1.5f * d, 0.7f * d, 5, 22f * d)
        drawPath(box, line, style = Stroke(1.7f * d, cap = StrokeCap.Round, join = StrokeJoin.Round))
        val pad = 4.5f * d
        val inner = size.width - 2 * pad
        val goneW = inner * elapsed
        if (goneW > 0f) {
            clipRect(pad, pad, pad + goneW, size.height - pad) {
                drawPath(hatchPath(size.width, size.height, d), ink)
            }
        }
        val x = pad + goneW
        drawLine(hi, Offset(x, -1.dp.toPx()), Offset(x, size.height + 1.dp.toPx()), 3.dp.toPx(), StrokeCap.Round)
    }
}

// ───────────────────────────── type & shape ─────────────────────────────

private fun variable(res: Int, weight: Int) = Font(
    resId = res,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

/** Kalam writes: titles, labels, the hero figure, the keypad. Static faces, with ₹. */
private val Kalam = FontFamily(
    Font(R.font.takhti_kalam_regular, FontWeight.W400),
    Font(R.font.takhti_kalam_bold, FontWeight.W700),
)

/** Nunito Sans counts: every list and table figure. Variable; only wght is driven. */
private val Nunito = FontFamily(
    variable(R.font.takhti_nunito_sans, 400), variable(R.font.takhti_nunito_sans, 500),
    variable(R.font.takhti_nunito_sans, 600), variable(R.font.takhti_nunito_sans, 700),
)

private val Scale = FinioTypeScale(
    Kalam, Nunito, Nunito,
    displayWeight = 700, displayTracking = -0.01f, pageTitleWeight = 700, screenTitleWeight = 700,
    headlineWeight = 700, moneyWeight = 700, dialogTitleWeight = 700, titleWeight = 700,
)
private val Shapes = FinioShapeScale(base = 6.dp, full = RoundedCornerShape(6.dp, 9.dp, 7.dp, 10.dp))

private val CaptionStyle = TextStyle(fontFamily = Kalam, fontWeight = FontWeight.W400, fontSize = 14.sp, lineHeight = 18.sp)
private val TabStyle = TextStyle(fontFamily = Kalam, fontSize = 12.sp, lineHeight = 16.sp)

// ───────────────────────────── palettes ─────────────────────────────

private fun c(rgb: Long) = Color(0xFF000000 or rgb)
private fun solid(rgb: Long): Brush = SolidColor(c(rgb))

/** The hatch as a brush for goal bars (no density at hand, so a typical 2.75x phone). */
private fun hatch(ink: Long, density: Float): Brush = CssRepeatingLinearGradient(
    angleDegrees = 304f,
    periodPx = 5.6f * density,
    stops = listOf(Stop(0f, c(ink)), Stop(2.1f / 5.6f, c(ink)), Stop(2.1f / 5.6f, Color.Transparent), Stop(1f, Color.Transparent)),
)

private fun colorsOf(
    dark: Boolean,
    background: Long, foreground: Long, popover: Long,
    primary: Long, primaryFg: Long, secondary: Long, mutedFg: Long,
    accent: Long, destructive: Long, destructiveFg: Long,
    positive: Long, warning: Long, border: Long, input: Long,
    charts: List<Long>,
    warningBand: Long, warningBandFg: Long, warningBandAccent: Long,
    shadowTint: Color, scrim: Color,
) = FinioColors(
    isDark = dark,
    background = c(background), foreground = c(foreground),
    card = c(background), cardForeground = c(foreground),
    popover = c(popover), popoverForeground = c(foreground),
    primary = c(primary), primaryForeground = c(primaryFg),
    secondary = c(secondary), secondaryForeground = c(foreground),
    muted = c(secondary), mutedForeground = c(mutedFg),
    accent = c(accent), accentForeground = c(foreground),
    destructive = c(destructive), destructiveForeground = c(destructiveFg),
    positive = c(positive), warning = c(warning),
    border = c(border), input = c(input), ring = c(primary),
    chart1 = c(charts[0]), chart2 = c(charts[1]), chart3 = c(charts[2]), chart4 = c(charts[3]), chart5 = c(charts[4]),
    sidebar = c(background), sidebarForeground = c(foreground),
    sidebarPrimary = c(primary), sidebarPrimaryForeground = c(primaryFg),
    sidebarAccent = c(accent), sidebarAccentForeground = c(foreground),
    sidebarBorder = c(border), sidebarRing = c(primary),
    warningBand = c(warningBand), warningBandForeground = c(warningBandFg), warningBandAccent = c(warningBandAccent),
    engraving = Color.Transparent,
    glass = Color.Transparent, glassStrong = c(background), glassBorder = c(border), glassHighlight = Color.Transparent,
    shadowTint = shadowTint, scrim = scrim,
)

/** No shadows anywhere: it is a board, nothing floats. */
private fun noShadows(tint: Color) = stock(tint).let { s ->
    FinioShadows(
        card = emptyList(), float = emptyList(), glowPrimary = emptyList(), glowSuccess = emptyList(), glowDanger = emptyList(),
        coin = emptyList(), tile = emptyList(), tabBar = emptyList(), noteChip = emptyList(),
        xs = s[0], sm = s[1], md = s[2], lg = s[3], xl = s[4],
    )
}

/** Accounts are written, not printed: board-coloured, marker ink, and what is owed (cards) in the owe colour. */
private fun takhtiNotes(board: Long, ink: Long, owe: Long) = Denomination.entries.associateWith { d ->
    NoteStyle(c(board), c(board), c(if (d == Denomination.Rs2000) owe else ink))
}

private val Light = ThemePalette(
    colors = colorsOf(
        false, 0xF4F7F9, 0x15171C, 0xFBFCFD,
        0x1650CF, 0xFFFFFF, 0xE6EAEE, 0x515865,
        0xE6EAEE, 0xCF222E, 0xFFFFFF,
        0x0F7A43, 0x8A5600, 0xC5CCD4, 0x6B7482,
        charts = listOf(0x1650CF, 0xCF222E, 0x0F7A43, 0x8A5600, 0x515865),
        warningBand = 0xFBE9EA, warningBandFg = 0x5A0C12, warningBandAccent = 0xCF222E,
        shadowTint = Color(21, 23, 28), scrim = rgba(21, 23, 28, 0.35f),
    ),
    brushes = FinioBrushes(
        gradPrimary = solid(0x1650CF), gradPrimarySoft = solid(0xE6EAEE),
        gradSuccess = solid(0x0F7A43), gradSuccessSoft = solid(0xE2F1E8),
        gradDanger = solid(0xCF222E), gradDangerSoft = solid(0xFBE9EA),
        gradInfo = solid(0x1650CF), gradWarning = solid(0x8A5600), gradSurface = solid(0xF4F7F9),
        paper = SolidColor(Color.Transparent), coin = solid(0x15171C),
        // The progress decoration cuts the hatch itself, so the register stays a flat ink.
        register = { solid(0x15171C) }, thread = hatch(0x15171C, 2.75f),
    ),
    shadows = noShadows(Color(21, 23, 28)),
    spec = FinioSpec(VisualTheme.Takhti, 1.7.dp, takhtiNotes(0xF4F7F9, 0x15171C, 0xCF222E)),
)

private val Dark = ThemePalette(
    colors = colorsOf(
        true, 0x1D2B25, 0xF2EFE6, 0x24352D,
        0xF7D75E, 0x1A2620, 0x26372F, 0xAEBCB2,
        0x2A3D34, 0xFF93A3, 0x1A2620,
        0xA5E59A, 0xF0B45A, 0x3E5148, 0x8A9D92,
        charts = listOf(0xF7D75E, 0xFF93A3, 0x93D0F5, 0xA5E59A, 0xF2EFE6),
        warningBand = 0x3A2328, warningBandFg = 0xFFE3E8, warningBandAccent = 0xFF93A3,
        shadowTint = Color(0, 0, 0), scrim = rgba(0, 0, 0, 0.6f),
    ),
    brushes = FinioBrushes(
        // Chalk-yellow can't carry white text, so filled actions are chalk-blue (white on it is ~5.9:1).
        gradPrimary = solid(0x2C62B8), gradPrimarySoft = solid(0x26372F),
        gradSuccess = solid(0x1F7A4A), gradSuccessSoft = solid(0x1D3A2B),
        gradDanger = solid(0xB3243A), gradDangerSoft = solid(0x3A2328),
        gradInfo = solid(0x2C62B8), gradWarning = solid(0x8A5600), gradSurface = solid(0x1D2B25),
        paper = SolidColor(Color.Transparent), coin = solid(0xF2EFE6),
        register = { solid(0xF2EFE6) }, thread = hatch(0xF2EFE6, 2.75f),
    ),
    shadows = noShadows(Color(0, 0, 0)),
    spec = FinioSpec(VisualTheme.Takhti, 1.7.dp, takhtiNotes(0x1D2B25, 0xF2EFE6, 0xFF93A3)),
)
