package com.slowatcoding.finio.ui.theme.kits

import android.content.res.Resources
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import androidx.compose.ui.graphics.Canvas as GCanvas

/**
 * Ajrakh — block-printed cloth (web/src/themes/ajrakh.css). A madder-red ground printed with a
 * diamond jaal, panels on it, and a printed double line round every panel; dark is the indigo vat
 * with resist-white ink. 2dp radii. Rozha One sets titles and the figure, Hind everything else.
 * The signature is the stamped row under the hero ([HeroFooter]) — twenty eight-point blocks,
 * each 5% of the budget — and every progress bar is printed through the same block mask.
 *
 * Palette note: the web re-points `--foreground` per surface (cream on the ground, ink on cotton
 * panels). Android has one `foreground`, and the palette test wants it readable on both the page
 * and the cards, so panels are the indigo dye here (cream ink on both grounds and panels); the
 * cotton account tiles (`spec.notes`) and the hero's cloth keep their own ink.
 */
object AjrakhKit : ThemeKit {
    override val visual = VisualTheme.Ajrakh
    override fun palette(dark: Boolean): ThemePalette = if (dark) Dark else Light
    override val type: FinioTypeScale get() = Scale
    override val shapes: FinioShapeScale get() = Shapes
    override fun coinInk(dark: Boolean) = c(0xFDF3E0)
    override val coinShape: Shape get() = CircleShape

    /** The add button is a dyed disc with a printed ring just inside its edge. */
    @Composable
    override fun coinDecoration(shape: Shape): Modifier = Modifier.drawWithContent {
        drawContent()
        val w = 1.dp.toPx()
        drawCircle(Cream75, radius = size.minDimension / 2f - 5.dp.toPx(), style = Stroke(w))
        drawCircle(Color(0xFF241A17), radius = size.minDimension / 2f - w / 2f, style = Stroke(w))
    }

    // ── progress: stamped blocks ──
    override val progressTrackHeight: Dp get() = 14.dp

    /** The track is the resist outline of the blocks; whatever fills it is printed through the solid block mask. */
    @Composable
    override fun progressDecoration(): Modifier {
        val bg = FinioTheme.colors.card
        return Modifier.drawWithContent {
            drawRect(bg)
            drawRect(OutTile)
            drawIntoCanvas { canvas ->
                canvas.saveLayer(Rect(0f, 0f, size.width, size.height), Paint())
                this@drawWithContent.drawContent()
                drawRect(FillTile, blendMode = BlendMode.DstIn)
                canvas.restore()
            }
        }
    }

    // ── hero: a dyed cloth with the medallion print, a panel on it, a lozenge selvedge below ──
    @Composable
    override fun heroContainer(shape: Shape): Modifier {
        val colors = FinioTheme.colors
        val dark = colors.isDark
        val dye = c(if (dark) 0xB5382A else 0x1B2A5C)
        val line = c(if (dark) 0x4354A8 else 0x241A17)
        val mis = c(if (dark) 0x0C1238 else 0xE2A419)
        val med = if (dark) MedDark else MedLight
        val band = if (dark) BandDark else BandLight
        val panel = colors.card
        val ruled = Cream26
        return Modifier
            .drawBehind { drawRect(mis, Offset(2.dp.toPx(), 2.dp.toPx()), size) }
            .background(dye)
            .drawBehind {
                drawRect(med)
                translate(0f, size.height - 10.dp.toPx()) { drawRect(band, size = Size(size.width, 10.dp.toPx())) }
                drawRect(line, style = Stroke(1.dp.toPx()))
            }
            .padding(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 22.dp)
            .background(panel)
            .drawBehind {
                drawRect(line, style = Stroke(1.dp.toPx()))
                val inset = 4.5.dp.toPx()
                drawRect(ruled, Offset(inset, inset), Size(size.width - 2 * inset, size.height - 2 * inset), style = Stroke(1.dp.toPx()))
            }
    }

    override fun heroPadding(wide: Boolean): PaddingValues = PaddingValues(16.dp, 14.dp, 16.dp, 12.dp)
    override val replacesHeroBar: Boolean get() = true

    /** Rozha One with a vermilion off-register second impression (web: text-shadow 1px 1px 0). */
    @Composable
    override fun HeroFigure(text: String, style: TextStyle, color: Color, whole: Boolean, modifier: Modifier): Boolean {
        val px = with(LocalDensity.current) { 1.dp.toPx() }
        Text(
            text,
            modifier,
            style = style.copy(
                fontSize = 50.sp,
                lineHeight = 52.sp,
                fontWeight = FontWeight.W400,
                letterSpacing = 0.005.sp,
                shadow = Shadow(StampMis, Offset(px, px), 0f),
            ),
            color = color,
            maxLines = 1,
            softWrap = false,
        )
        return true
    }

    /** The stamped row: 20 blocks, inked = spent, outline = left, turmeric marker at today. */
    @Composable
    override fun HeroFooter(elapsed: Float, spentPercent: Float, isOver: Boolean, daysLeft: Int) {
        val colors = FinioTheme.colors
        val reduced = rememberReducedMotion()
        val press = remember { Animatable(if (reduced) 1f else 0f) }
        LaunchedEffect(Unit) { if (!reduced) press.animateTo(1f, tween(PressTotalMs, easing = LinearEasing)) }
        val spent = spentPercent.coerceAtLeast(0f)
        val hot = isOver || spent >= 85f
        val today = elapsed.coerceIn(0f, 1f)
        val period = LocalHeroPeriod.current
        val (total, day) = remember(elapsed, daysLeft, period) {
            if (period != null) {
                val t = period.totalDays.coerceAtLeast(1)
                t to period.dayOfPeriod.coerceIn(1, t)
            } else {
                periodDays(elapsed, daysLeft)
            }
        }
        Column(Modifier.padding(top = 12.dp).widthIn(max = 512.dp).clearAndSetSemantics { }) {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val gap = 2.dp
                val s = (maxWidth - gap * (Blocks - 1)) / Blocks
                Canvas(Modifier.fillMaxWidth().height(s + 18.dp)) {
                    val sp = s.toPx()
                    val k = sp / 16f
                    val gp = gap.toPx()
                    val y0 = 9.dp.toPx()
                    val fill = starPath(8f * k, 8f * k, 7.7f * k, 5.5f * k, 1.8f * k)
                    val outline = starPath(8f * k, 8f * k, 7.1f * k, 5.1f * k, 0f)
                    val shadow = 1.dp.toPx()
                    for (i in 0 until Blocks) {
                        val f = ((spent - i * 5f) / 5f).coerceIn(0f, 1f)
                        val x = i * (sp + gp)
                        translate(x, y0) {
                            if (f < 1f) drawPath(outline, Resist, style = Stroke(1.1f * k))
                            if (f > 0f) {
                                val t = ((press.value * PressTotalMs - (i * 40f + 250f)) / 300f).coerceIn(0f, 1f)
                                val e = 1f - (1f - t) * (1f - t) * (1f - t)
                                val isHot = hot && i >= 16
                                val ink = if (isHot) Hot else Stamp
                                val mis = if (isHot) HotMis else StampMis
                                scale(1.15f - 0.15f * e, Offset(sp / 2f, sp / 2f)) {
                                    clipRect(0f, -2f * shadow, sp * f + shadow, sp + 2f * shadow) {
                                        translate(shadow, shadow) { drawPath(fill, mis, alpha = e) }
                                    }
                                    clipRect(0f, 0f, sp * f, sp) { drawPath(fill, ink, alpha = e) }
                                }
                            }
                        }
                    }
                    // today
                    val row = size.width
                    val mx = row * today
                    val mw = 3.dp.toPx()
                    drawRect(Turmeric, Offset(mx - mw / 2f, y0 - 5.dp.toPx()), Size(mw, sp + 10.dp.toPx()))
                    drawRect(MarkLine, Offset(mx - mw / 2f, y0 - 5.dp.toPx()), Size(mw, sp + 10.dp.toPx()), style = Stroke(1.dp.toPx()))
                    val d = 7.dp.toPx()
                    val cy = y0 - 5.dp.toPx() - 0.5.dp.toPx()
                    rotate(45f, Offset(mx, cy)) {
                        drawRect(Turmeric, Offset(mx - d / 2f, cy - d / 2f), Size(d, d))
                        drawRect(MarkLine, Offset(mx - d / 2f, cy - d / 2f), Size(d, d), style = Stroke(1.dp.toPx()))
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Each block is 5% of the limit", style = CaptionStyle, color = colors.mutedForeground)
                Text("Day $day of $total", style = CaptionStyle, color = colors.mutedForeground)
            }
        }
    }

    // ── chrome: a cloth strip under a lozenge selvedge; the current page is a dyed patch ──
    @Composable
    override fun TabBar(active: FinioTab?, onSelect: (FinioTab) -> Unit, modifier: Modifier) {
        val colors = FinioTheme.colors
        val dark = colors.isDark
        val band = if (dark) BandDark else BandLight
        val line = c(if (dark) 0x4354A8 else 0x241A17)
        val mis = c(if (dark) 0x0C1238 else 0xE2A419)
        val dye = c(0xB5382A)
        val onDye = c(0xFDF3E0)
        Row(
            modifier
                .fillMaxWidth()
                .background(colors.card)
                .drawBehind {
                    val h = 10.dp.toPx()
                    drawRect(band, size = Size(size.width, h))
                    drawLine(line, Offset(0f, 0.5.dp.toPx()), Offset(size.width, 0.5.dp.toPx()), 1.dp.toPx())
                    drawLine(line, Offset(0f, h + 0.5.dp.toPx()), Offset(size.width, h + 0.5.dp.toPx()), 1.dp.toPx())
                }
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(start = 6.dp, end = 6.dp, top = 16.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FinioTab.entries.forEach { tab ->
                val selected = tab == active
                val tint = if (selected) onDye else colors.mutedForeground
                val icon = remember(tab) { tab.icon(1.7f) }
                Column(
                    Modifier
                        .weight(1f)
                        .then(
                            if (selected) {
                                Modifier
                                    .drawBehind { drawRect(mis, Offset(1.dp.toPx(), 1.dp.toPx()), size) }
                                    .background(dye, RoundedCornerShape(2.dp))
                                    .drawBehind {
                                        val i = 2.5.dp.toPx()
                                        drawRect(Cream50, Offset(i, i), Size(size.width - 2 * i, size.height - 2 * i), style = Stroke(1.dp.toPx()))
                                    }
                            } else Modifier,
                        )
                        .tabClickable(selected) { onSelect(tab) }
                        .padding(horizontal = 2.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = tint)
                    Text(tab.label, style = TabStyle, color = tint, maxLines = 1)
                }
            }
        }
    }
}

// ───────────────────────────── type & shape ─────────────────────────────

private val Rozha = FontFamily(Font(R.font.ajrakh_rozha_one, FontWeight.W400))
private val Hind = FontFamily(
    Font(R.font.ajrakh_hind_regular, FontWeight.W400),
    Font(R.font.ajrakh_hind_medium, FontWeight.W500),
    Font(R.font.ajrakh_hind_semibold, FontWeight.W600),
    Font(R.font.ajrakh_hind_bold, FontWeight.W700),
)

// Rozha One is single-weight: every heading/figure role is 400 so nothing is synthesised bold.
private val Scale = FinioTypeScale(
    Rozha, Rozha, Hind,
    displayWeight = 400, displayTracking = 0.005f, pageTitleWeight = 400, screenTitleWeight = 400,
    headlineWeight = 400, moneyWeight = 400, dialogTitleWeight = 400, titleWeight = 600,
)
private val Shapes = FinioShapeScale(base = 2.dp, full = RoundedCornerShape(2.dp))

private val TabStyle = TextStyle(fontFamily = Hind, fontWeight = FontWeight.W600, fontSize = 11.sp)
private val CaptionStyle = TextStyle(fontFamily = Hind, fontWeight = FontWeight.W400, fontSize = 13.sp, fontFeatureSettings = "tnum")

private const val Blocks = 20
private const val PressTotalMs = 19 * 40 + 250 + 300

/** Fallback when `LocalHeroPeriod` is null: whole days in the period and today's 1-based day, recovered from what the hero is given. */
private fun periodDays(elapsed: Float, daysLeft: Int): Pair<Int, Int> {
    val total = listOf(7, 28, 29, 30, 31, 365, 366)
        .filter { it >= daysLeft }
        .minByOrNull { abs((it - daysLeft + 0.5f) / it - elapsed) } ?: daysLeft
    return total to (total - daysLeft + 1).coerceIn(1, total)
}

// ───────────────────────────── printed tiles ─────────────────────────────

private fun c(rgb: Long) = Color(0xFF000000 or rgb)

private val Cream75 = Color(0xFFFBF5E6).copy(alpha = 0.75f)
private val Cream50 = Color(0xFFF8F0DF).copy(alpha = 0.5f)
private val Cream26 = Color(0xFFF1E9D6).copy(alpha = 0.26f)

// The stamped row sits on an indigo panel in both modes, so it carries the vat's ink.
private val Stamp = Color(0xFFF1E9D6)
private val StampMis = Color(0xFFE5553E)
private val Hot = Color(0xFFE5553E)
private val HotMis = Color(0xFFF1E9D6)
private val Resist = Color(0xFFF1E9D6).copy(alpha = 0.36f)
private val Turmeric = Color(0xFFE2A419)
private val MarkLine = Color(0xFF0C1238)

/** An eight-point block star: 16 vertices alternating [outer]/[inner] (+ a diamond hole when [hole] > 0). */
private fun starPath(cx: Float, cy: Float, outer: Float, inner: Float, hole: Float): Path = Path().apply {
    fillType = PathFillType.EvenOdd
    for (i in 0 until 16) {
        val a = Math.toRadians(-90.0 + i * 22.5)
        val r = if (i % 2 == 0) outer else inner
        val x = cx + (r * cos(a)).toFloat()
        val y = cy + (r * sin(a)).toFloat()
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
    if (hole > 0f) {
        moveTo(cx, cy - hole); lineTo(cx + hole, cy); lineTo(cx, cy + hole); lineTo(cx - hole, cy); close()
    }
}

private fun fillPaint(color: Color) = Paint().apply { this.color = color; isAntiAlias = true }
private fun strokePaint(color: Color, width: Float) = Paint().apply {
    this.color = color; isAntiAlias = true; style = PaintingStyle.Stroke; strokeWidth = width
}

private fun GCanvas.diamond(cx: Float, cy: Float, hx: Float, hy: Float, paint: Paint) {
    val p = Path().apply { moveTo(cx, cy - hy); lineTo(cx + hx, cy); lineTo(cx, cy + hy); lineTo(cx - hx, cy); close() }
    drawPath(p, paint)
}

/**
 * A repeating raster tile. [wDp]x[hDp] is the tile in dp; [draw] paints it with a px-per-dp scale.
 * The brush is built lazily against the device density (a Brush has no density of its own) and
 * tiles from the origin of whatever it is drawn into.
 */
private class TileBrush(private val wDp: Float, private val hDp: Float, private val draw: GCanvas.(Float) -> Unit) : ShaderBrush() {
    private var cached: Pair<Float, Shader>? = null
    override fun createShader(size: Size): Shader {
        val d = Resources.getSystem().displayMetrics.density
        cached?.let { (cd, s) -> if (cd == d) return s }
        val bmp = ImageBitmap((wDp * d).roundToInt().coerceAtLeast(2), (hDp * d).roundToInt().coerceAtLeast(2))
        GCanvas(bmp).draw(d)
        val shader = ImageShader(bmp, TileMode.Repeated, TileMode.Repeated)
        cached = d to shader
        return shader
    }
}

/** The ground print: a diamond jaal, 18dp. */
private fun jaal(line: Color, dot: Color) = TileBrush(18f, 18f) { d ->
    val s = d
    val p = Path().apply { moveTo(9 * s, 1.5f * s); lineTo(16.5f * s, 9 * s); lineTo(9 * s, 16.5f * s); lineTo(1.5f * s, 9 * s); close() }
    drawPath(p, strokePaint(line, 1.3f * s))
    diamond(9 * s, 9 * s, 2.6f * s, 2.6f * s, fillPaint(line))
    val dp = fillPaint(dot)
    for ((x, y) in listOf(0f to 0f, 18f to 0f, 0f to 18f, 18f to 18f)) drawCircle(androidx.compose.ui.geometry.Offset(x * s, y * s), 1.3f * s, dp)
}

/** The medallion print (a 24-unit motif drawn into a 16dp tile): circle, star, centre dot. */
private fun medallion(ring: Color, star: Color, dot: Color) = TileBrush(16f, 16f) { d ->
    val k = d * 16f / 24f
    drawCircle(androidx.compose.ui.geometry.Offset(12 * k, 12 * k), 10 * k, strokePaint(ring, 1.3f * k))
    drawPath(starPath(12 * k, 12 * k, 7.6f * k, 5.2f * k, 0f), fillPaint(star))
    drawCircle(androidx.compose.ui.geometry.Offset(12 * k, 12 * k), 1.7f * k, fillPaint(dot))
    val rp = fillPaint(ring)
    for ((x, y) in listOf(0f to 0f, 24f to 0f, 0f to 24f, 24f to 24f)) drawCircle(androidx.compose.ui.geometry.Offset(x * k, y * k), 1.7f * k, rp)
}

/** The lozenge border band: 20x10dp, ruled top and bottom with a diamond between. */
private fun band(cloth: Color, rule: Color, lozenge: Color, dot: Color) = TileBrush(20f, 10f) { d ->
    drawRect(androidx.compose.ui.geometry.Rect(0f, 0f, 20 * d, 10 * d), fillPaint(cloth))
    val rp = strokePaint(rule, d)
    drawLine(androidx.compose.ui.geometry.Offset(0f, 0.75f * d), androidx.compose.ui.geometry.Offset(20 * d, 0.75f * d), rp)
    drawLine(androidx.compose.ui.geometry.Offset(0f, 9.25f * d), androidx.compose.ui.geometry.Offset(20 * d, 9.25f * d), rp)
    diamond(10 * d, 5 * d, 5.5f * d, 2.4f * d, fillPaint(lozenge))
    val dp = fillPaint(dot)
    drawCircle(androidx.compose.ui.geometry.Offset(0f, 5 * d), 1.2f * d, dp)
    drawCircle(androidx.compose.ui.geometry.Offset(20 * d, 5 * d), 1.2f * d, dp)
}

private val JaalLight = jaal(c(0x7D1E15), Color(0xFFF8F0DF).copy(alpha = 0.5f))
private val JaalDark = jaal(c(0x1F2C76), Color(0xFFF1E9D6).copy(alpha = 0.34f))
private val MedLight = medallion(Color(0xFFF7F0E1).copy(alpha = 0.8f), c(0xCF4A34), c(0x1B2A5C))
private val MedDark = medallion(Color(0xFFF1E9D6).copy(alpha = 0.8f), c(0x121A4E), c(0xF0B82E))
private val BandLight = band(c(0xF7F0E1), c(0x241A17), c(0x1B2A5C), c(0x9A2B1F))
private val BandDark = band(c(0x0C1238), c(0xF1E9D6), c(0xF1E9D6), c(0xF0B82E))

/** The progress mask: solid block stars (a diamond hole each) on a 14dp pitch; the outline tile is the track. */
private val FillTile = TileBrush(14f, 14f) { d ->
    val k = d * 14f / 16f
    drawPath(starPath(8 * k, 8 * k, 7.7f * k, 5.5f * k, 1.8f * k), fillPaint(Color.White))
}
private val OutTile = TileBrush(14f, 14f) { d ->
    val k = d * 14f / 16f
    drawPath(starPath(8 * k, 8 * k, 7.1f * k, 5.1f * k, 0f), strokePaint(Resist, 1.1f * k))
}

// ───────────────────────────── palettes ─────────────────────────────

private fun solid(rgb: Long): Brush = SolidColor(c(rgb))

private fun colorsOf(
    dark: Boolean,
    background: Long, foreground: Long, card: Long, popover: Long,
    primary: Long, primaryFg: Long, secondary: Long, mutedFg: Long,
    accent: Long, destructive: Long, destructiveFg: Long,
    positive: Long, warning: Long, border: Color, input: Color, ring: Long,
    charts: List<Long>,
    sidebar: Long, sidebarFg: Long, sidebarPrimary: Long, sidebarPrimaryFg: Long,
    warningBand: Long, warningBandFg: Long, warningBandAccent: Long,
    glassBorder: Color, shadowTint: Color, scrim: Color,
) = FinioColors(
    isDark = dark,
    background = c(background), foreground = c(foreground),
    card = c(card), cardForeground = c(foreground),
    popover = c(popover), popoverForeground = c(foreground),
    primary = c(primary), primaryForeground = c(primaryFg),
    secondary = c(secondaryOf(card)), secondaryForeground = c(foreground),
    muted = c(secondaryOf(card)), mutedForeground = c(mutedFg),
    accent = c(accent), accentForeground = c(foreground),
    destructive = c(destructive), destructiveForeground = c(destructiveFg),
    positive = c(positive), warning = c(warning),
    border = border, input = input, ring = c(ring),
    chart1 = c(charts[0]), chart2 = c(charts[1]), chart3 = c(charts[2]), chart4 = c(charts[3]), chart5 = c(charts[4]),
    sidebar = c(sidebar), sidebarForeground = c(sidebarFg),
    sidebarPrimary = c(sidebarPrimary), sidebarPrimaryForeground = c(sidebarPrimaryFg),
    sidebarAccent = c(0xF0B82E), sidebarAccentForeground = c(0x1A1206),
    sidebarBorder = c(0xF1E9D6), sidebarRing = c(0xF0B82E),
    warningBand = c(warningBand), warningBandForeground = c(warningBandFg), warningBandAccent = c(warningBandAccent),
    engraving = Color.Transparent,
    glass = c(card), glassStrong = c(card), glassBorder = glassBorder, glassHighlight = Color.Transparent,
    shadowTint = shadowTint, scrim = scrim,
)

/** The muted/secondary fill: the panel one step lighter. */
private fun secondaryOf(card: Long) = if (card == 0x22337AL) 0x2E418FL else 0x26337AL

private fun shadowsOf(tint: Color, mis: Color) = stock(tint).let { s ->
    val offset = listOf(CssShadow(x = 2.dp, y = 2.dp, color = mis))
    FinioShadows(
        card = offset, float = offset, glowPrimary = emptyList(), glowSuccess = emptyList(), glowDanger = emptyList(),
        coin = emptyList(), tile = emptyList(), tabBar = emptyList(), noteChip = emptyList(),
        xs = s[0], sm = s[1], md = s[2], lg = s[3], xl = s[4],
    )
}

private fun note(from: Long, ink: Long) = NoteStyle(c(from), c(from), c(ink))

/** Cotton tiles in light, vat-dyed tiles in dark — tinted by denomination, each with its own ink. */
private fun ajrakhNotes(dark: Boolean) = Denomination.entries.associateWith { d ->
    if (dark) when (d) {
        Denomination.Rs10 -> note(0x1F2A63, 0xF1E9D6)
        Denomination.Rs20 -> note(0x1D3A47, 0xBFE8B0)
        Denomination.Rs50 -> note(0x26337A, 0xF1E9D6)
        Denomination.Rs100 -> note(0x1C2868, 0xF1E9D6)
        Denomination.Rs200 -> note(0x3D3318, 0xFFE9B0)
        Denomination.Rs500 -> note(0x22307A, 0xF1E9D6)
        Denomination.Rs2000 -> note(0x4A2036, 0xFFB3A0)
    } else when (d) {
        Denomination.Rs10 -> note(0xE7E1CF, 0x1B2A5C)
        Denomination.Rs20 -> note(0xE3EAD3, 0x1D4A1C)
        Denomination.Rs50 -> note(0xDFE3EF, 0x1B2A5C)
        Denomination.Rs100 -> note(0xF7F0E1, 0x1B2A5C)
        Denomination.Rs200 -> note(0xF6E6B4, 0x3A2600)
        Denomination.Rs500 -> note(0xEFE3C8, 0x1B2A5C)
        Denomination.Rs2000 -> note(0xF5DCD0, 0x7D1E15)
    }
}

private val Light = ThemePalette(
    colors = colorsOf(
        false, 0x94281C, 0xFBF5E6, 0x22337A, 0x2A3C8A,
        0xFBF5E6, 0x1B2A5C, 0, 0xF3D3C5,
        0x2E418F, 0xFF9279, 0x1A0D0A,
        0x93D47C, 0xF0B82E,
        border = Color(0xFFFBF5E6).copy(alpha = 0.22f), input = Color(0xFFFBF5E6).copy(alpha = 0.5f), ring = 0xF0B82E,
        charts = listOf(0xFBF5E6, 0xE5553E, 0xF0B82E, 0x93D47C, 0x8B9BE0),
        sidebar = 0x1B2A5C, sidebarFg = 0xF8F0DF, sidebarPrimary = 0xE2A419, sidebarPrimaryFg = 0x241A17,
        warningBand = 0xFBE3A8, warningBandFg = 0x3A2600, warningBandAccent = 0x7A5200,
        glassBorder = Color(0xFFFBF5E6).copy(alpha = 0.45f), shadowTint = Color(36, 26, 23), scrim = rgba(36, 26, 23, 0.5f),
    ),
    brushes = FinioBrushes(
        gradPrimary = solid(0xB5382A), gradPrimarySoft = solid(0x2E418F),
        gradSuccess = solid(0x2C6A2B), gradSuccessSoft = solid(0x1B3A3A),
        gradDanger = solid(0xB5382A), gradDangerSoft = solid(0x4A2036),
        gradInfo = solid(0xB5382A), gradWarning = solid(0x8A5A00), gradSurface = solid(0x22337A),
        paper = JaalLight, coin = solid(0xB5382A), register = { _ -> solid(0xF1E9D6) }, thread = solid(0xF1E9D6),
    ),
    shadows = shadowsOf(Color(36, 26, 23), c(0xE2A419)),
    spec = FinioSpec(VisualTheme.Ajrakh, 1.dp, ajrakhNotes(false)),
)

private val Dark = ThemePalette(
    colors = colorsOf(
        true, 0x121A4E, 0xF1E9D6, 0x1C2868, 0x222F78,
        0xF1E9D6, 0x121A4E, 0, 0xB7BDDD,
        0x2A3990, 0xFF9279, 0x1A0D0A,
        0x93D47C, 0xF0B82E,
        border = Color(0xFFF1E9D6).copy(alpha = 0.15f), input = Color(0xFFF1E9D6).copy(alpha = 0.5f), ring = 0xF0B82E,
        charts = listOf(0xF1E9D6, 0xE5553E, 0xF0B82E, 0x93D47C, 0x8B9BE0),
        sidebar = 0xB5382A, sidebarFg = 0xFDF3E0, sidebarPrimary = 0xF0B82E, sidebarPrimaryFg = 0x1A1206,
        warningBand = 0x3D2F12, warningBandFg = 0xFFE9B0, warningBandAccent = 0xF0B82E,
        glassBorder = c(0x4354A8), shadowTint = Color(0, 0, 0), scrim = rgba(5, 8, 30, 0.65f),
    ),
    brushes = FinioBrushes(
        gradPrimary = solid(0xB5382A), gradPrimarySoft = solid(0x26337A),
        gradSuccess = solid(0x2C6A2B), gradSuccessSoft = solid(0x1B3A3A),
        gradDanger = solid(0xB5382A), gradDangerSoft = solid(0x4A2036),
        gradInfo = solid(0xB5382A), gradWarning = solid(0x8A5A00), gradSurface = solid(0x1C2868),
        paper = JaalDark, coin = solid(0xB5382A), register = { _ -> solid(0xF1E9D6) }, thread = solid(0xF1E9D6),
    ),
    shadows = shadowsOf(Color(0, 0, 0), c(0x0C1238)),
    spec = FinioSpec(VisualTheme.Ajrakh, 1.dp, ajrakhNotes(true)),
)
