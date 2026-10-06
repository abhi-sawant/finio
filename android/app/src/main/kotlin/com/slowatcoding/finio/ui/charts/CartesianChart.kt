package com.slowatcoding.finio.ui.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.slowatcoding.finio.ui.theme.FinioTheme
import com.slowatcoding.finio.ui.theme.mix
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * How categories sit on the x axis: `Band` (Bar/Composed charts — each value owns a slot and is
 * drawn at its centre) or `Point` (Line/Area charts — first and last values sit on the edges).
 */
enum class XScale { Band, Point }

/** Recharts' XAxis `interval`. */
sealed interface XTicks {
    /** A number: show every (n+1)th tick from the first. */
    data class Every(val interval: Int) : XTicks
    data class PreserveStartEnd(val minTickGap: Dp = 5.dp) : XTicks
    /** Recharts' default. */
    data class PreserveEnd(val minTickGap: Dp = 5.dp) : XTicks
}

/** The tooltip cursor: a muted band behind the slot (bars) or a thin vertical line (lines). */
enum class ChartCursor { Band, Line }

/** Where things land in the plot, handed to the series drawing code. */
@Immutable
class ChartGeometry(
    val plot: Rect,
    val count: Int,
    val scale: XScale,
    private val domainMin: Double,
    private val domainMax: Double,
) {
    /** Band width (Band scale) or the gap between points (Point scale). */
    val step: Float = when {
        scale == XScale.Band -> plot.width / max(count, 1)
        count > 1 -> plot.width / (count - 1)
        else -> 0f
    }

    fun x(index: Int): Float = when {
        scale == XScale.Band -> plot.left + step * (index + 0.5f)
        count > 1 -> plot.left + step * index
        else -> plot.center.x
    }

    fun y(value: Double): Float {
        val span = domainMax - domainMin
        if (span == 0.0) return plot.bottom
        return (plot.bottom - (value - domainMin) / span * plot.height).toFloat()
    }

    fun indexAt(px: Float): Int {
        if (count <= 0) return -1
        val i = when (scale) {
            XScale.Band -> floor((px - plot.left) / step).toInt()
            XScale.Point -> if (count == 1) 0 else ((px - plot.left) / step).roundToInt()
        }
        return i.coerceIn(0, count - 1)
    }
}

private val YAxisWidth = 56.dp
private val XAxisHeight = 30.dp
private val MarginTop = 8.dp
private val MarginRight = 8.dp

/**
 * A Recharts-style cartesian chart drawn on a Canvas: a 56dp money Y axis (compact en-IN ticks,
 * honouring hide-amounts), x labels beneath, the dashed horizontal grid, and a tap/drag tooltip
 * with its cursor. Series are drawn by [drawSeries] (behind the active dots, above the grid).
 *
 * @param domainValues every value the series plot — sets the `[0, auto]` nice Y domain.
 * @param activeDots per index, the (value, colour) points that get Recharts' active dot.
 */
@Composable
fun CartesianChart(
    count: Int,
    xLabels: List<String>,
    domainValues: List<Double>,
    scale: XScale,
    xTicks: XTicks,
    hidden: Boolean,
    cursor: ChartCursor,
    tooltip: (Int) -> TooltipData,
    modifier: Modifier = Modifier,
    activeDots: ((Int) -> List<Pair<Double, Color>>)? = null,
    drawSeries: DrawScope.(ChartGeometry) -> Unit,
) {
    val colors = FinioTheme.colors
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val tickStyle = AxisTickStyle.copy(color = colors.mutedForeground)
    val ticks = remember(domainValues) { niceTicks(domainValues) }
    val tickLabels = remember(ticks, hidden) { ticks.map { formatAxisMoney(it, hidden) } }
    var active by remember(count) { mutableStateOf<Int?>(null) }
    var geometry by remember { mutableStateOf<ChartGeometry?>(null) }

    val gridColor = colors.border
    val cursorBand = colors.muted.mix(0.5f)
    val cursorLine = colors.mutedForeground.mix(0.4f)
    val dotRing = colors.card

    Layout(
        modifier = modifier,
        content = {
            Canvas(
                Modifier
                    .fillMaxSize()
                    .pointerInput(count) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val g = geometry ?: return@awaitEachGesture
                            if (g.plot.contains(down.position) || down.position.x in g.plot.left..g.plot.right) {
                                val index = g.indexAt(down.position.x)
                                val up = waitForUpOrCancellation()
                                if (up != null) active = if (active == index) null else index
                            }
                        }
                    }
                    .pointerInput(count) {
                        detectHorizontalDragGestures { change, _ ->
                            val g = geometry ?: return@detectHorizontalDragGestures
                            active = g.indexAt(change.position.x)
                        }
                    },
            ) {
                val plot = Rect(
                    left = YAxisWidth.toPx(),
                    top = MarginTop.toPx(),
                    right = size.width - MarginRight.toPx(),
                    bottom = size.height - XAxisHeight.toPx(),
                )
                val g = ChartGeometry(plot, count, scale, ticks.first(), ticks.last())
                if (geometry?.plot != plot || geometry?.count != count) geometry = g

                // Grid: dashed horizontal lines at each Y tick (GRID_PROPS).
                val dash = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx()))
                ticks.forEach { t ->
                    val y = g.y(t)
                    drawLine(gridColor, Offset(plot.left, y), Offset(plot.right, y), 1.dp.toPx(), pathEffect = dash)
                }

                // Y tick labels: right-aligned 10dp left of the plot (tickSize 6 + tickMargin 4).
                ticks.forEachIndexed { i, t ->
                    val layout = measurer.measure(tickLabels[i], tickStyle)
                    val y = g.y(t)
                    drawText(layout, topLeft = Offset(plot.left - 10.dp.toPx() - layout.size.width, y - layout.size.height / 2f))
                }

                // X tick labels.
                if (count > 0) {
                    val layouts = xLabels.map { measurer.measure(it, tickStyle) }
                    val shown = visibleXTicks(g, layouts, xTicks, density.density)
                    shown.forEach { (i, cx) ->
                        val l = layouts[i]
                        val baseline = plot.bottom + 8.dp.toPx() + 7.1f * density.fontScale * density.density
                        drawText(l, topLeft = Offset(cx - l.size.width / 2f, baseline - l.firstBaseline))
                    }
                }

                // Cursor.
                val a = active
                if (a != null && a in 0 until count) {
                    val x = g.x(a)
                    when (cursor) {
                        ChartCursor.Band -> drawRect(cursorBand, Offset(x - g.step / 2f, plot.top), Size(g.step, plot.height))
                        ChartCursor.Line -> drawLine(cursorLine, Offset(x, plot.top), Offset(x, plot.bottom), 1.dp.toPx())
                    }
                }

                drawSeries(g)

                if (a != null && a in 0 until count && activeDots != null) {
                    activeDots(a).forEach { (v, c) ->
                        val center = Offset(g.x(a), g.y(v))
                        drawCircle(c, 4.dp.toPx(), center)
                        drawCircle(dotRing, 4.dp.toPx(), center, style = Stroke(2.dp.toPx()))
                    }
                }
            }
            val a = active
            if (a != null && a in 0 until count) ChartTooltip(tooltip(a))
        },
    ) { measurables, constraints ->
        val canvas = measurables[0].measure(Constraints.fixed(constraints.maxWidth, constraints.maxHeight))
        val tip = measurables.getOrNull(1)?.measure(Constraints(maxWidth = constraints.maxWidth))
        layout(constraints.maxWidth, constraints.maxHeight) {
            canvas.place(0, 0)
            if (tip != null) {
                val g = geometry
                val a = active
                val gap = 10.dp.roundToPx()
                val x = if (g != null && a != null) g.x(a).roundToInt() else 0
                val right = x + gap
                val tx = if (right + tip.width <= constraints.maxWidth) right else max(0, x - gap - tip.width)
                tip.place(tx, MarginTop.roundToPx())
            }
        }
    }
}

/** Recharts' tick-visibility pass (getTicks): which x labels to draw, and where (index → centre). */
private fun visibleXTicks(
    g: ChartGeometry,
    layouts: List<TextLayoutResult>,
    mode: XTicks,
    density: Float,
): List<Pair<Int, Float>> {
    val n = min(layouts.size, g.count)
    if (n == 0) return emptyList()
    val sizes = (0 until n).map { layouts[it].size.width.toFloat() }
    val coords = (0 until n).map { g.x(it) }
    fun visible(c: Float, size: Float, start: Float, end: Float) = c - size / 2 - start >= 0 && c + size / 2 - end <= 0
    val start0 = g.plot.left
    val end0 = g.plot.right
    return when (mode) {
        is XTicks.Every -> (0 until n).filter { it % (mode.interval + 1) == 0 }.map { it to coords[it] }
        is XTicks.PreserveEnd -> {
            val gap = mode.minTickGap.value * density
            var end = end0
            val out = mutableListOf<Pair<Int, Float>>()
            for (i in n - 1 downTo 0) {
                var c = coords[i]
                if (i == n - 1) {
                    val over = c + sizes[i] / 2 - end
                    if (over > 0) c -= over
                }
                if (visible(c, sizes[i], start0, end)) {
                    end = c - (sizes[i] / 2 + gap)
                    out += i to c
                }
            }
            out
        }
        is XTicks.PreserveStartEnd -> {
            val gap = mode.minTickGap.value * density
            var start = start0
            var end = end0
            val out = mutableListOf<Pair<Int, Float>>()
            var tail = coords[n - 1]
            val tailOver = tail + sizes[n - 1] / 2 - end
            if (tailOver > 0) tail -= tailOver
            val tailShown = visible(tail, sizes[n - 1], start, end)
            if (tailShown) end = tail - (sizes[n - 1] / 2 + gap)
            for (i in 0 until n - 1) {
                var c = coords[i]
                if (i == 0) {
                    val under = c - sizes[i] / 2 - start
                    if (under < 0) c -= under
                }
                if (visible(c, sizes[i], start, end)) {
                    start = c + sizes[i] / 2 + gap
                    out += i to c
                }
            }
            if (tailShown) out += (n - 1) to tail
            out
        }
    }
}

// ── d3 curveMonotoneX ──────────────────────────────────────────────────────────────────────

/** d3-shape's `curveMonotoneX` through [points] (Recharts `type="monotone"`), as a Path. */
fun monotonePath(points: List<Offset>): Path {
    val path = Path()
    if (points.isEmpty()) return path
    var x0 = Float.NaN
    var y0 = Float.NaN
    var x1 = Float.NaN
    var y1 = Float.NaN
    var t0 = Float.NaN
    var state = 0

    fun sgn(v: Float) = if (v < 0) -1f else 1f
    fun slope3(x2: Float, y2: Float): Float {
        val h0 = x1 - x0
        val h1 = x2 - x1
        val s0 = (y1 - y0) / (if (h0 != 0f) h0 else if (h1 < 0) -0f else 0f)
        val s1 = (y2 - y1) / (if (h1 != 0f) h1 else if (h0 < 0) -0f else 0f)
        val p = (s0 * h1 + s1 * h0) / (h0 + h1)
        val r = (sgn(s0) + sgn(s1)) * min(min(abs(s0), abs(s1)), 0.5f * abs(p))
        return if (r.isNaN()) 0f else r
    }
    fun slope2(t: Float): Float {
        val h = x1 - x0
        return if (h != 0f) (3 * (y1 - y0) / h - t) / 2 else t
    }
    fun bezier(ta: Float, tb: Float) {
        val dx = (x1 - x0) / 3
        path.cubicTo(x0 + dx, y0 + dx * ta, x1 - dx, y1 - dx * tb, x1, y1)
    }

    for (pt in points) {
        val x = pt.x
        val y = pt.y
        var t1 = Float.NaN
        if (x == x1 && y == y1) continue
        when (state) {
            0 -> { state = 1; path.moveTo(x, y) }
            1 -> state = 2
            2 -> { state = 3; t1 = slope3(x, y); bezier(slope2(t1), t1) }
            else -> { t1 = slope3(x, y); bezier(t0, t1) }
        }
        x0 = x1; x1 = x
        y0 = y1; y1 = y
        t0 = t1
    }
    when (state) {
        2 -> path.lineTo(x1, y1)
        3 -> bezier(t0, slope2(t0))
    }
    return path
}

/** A bar with only its top (or bottom, for a negative value) corners rounded. */
fun DrawScope.drawBar(color: Color, left: Float, width: Float, yValue: Float, yZero: Float, radius: Float, alpha: Float = 1f) {
    if (width <= 0f) return
    val top = min(yValue, yZero)
    val bottom = max(yValue, yZero)
    val h = bottom - top
    if (h <= 0f) return
    val r = min(radius, min(width / 2, h))
    val cr = CornerRadius(r, r)
    val rr = if (yValue <= yZero) {
        RoundRect(left, top, left + width, bottom, topLeftCornerRadius = cr, topRightCornerRadius = cr)
    } else {
        RoundRect(left, top, left + width, bottom, bottomLeftCornerRadius = cr, bottomRightCornerRadius = cr)
    }
    drawPath(Path().apply { addRoundRect(rr) }, color, alpha = alpha)
}

/** Recharts' grouped-bar slot maths: `barCategoryGap` 10%, `barGap` 4. Returns (offset, size). */
fun DrawScope.barSlots(g: ChartGeometry, series: Int, barGap: Dp = 4.dp): List<Pair<Float, Float>> {
    val band = g.step
    val offset = band * 0.1f
    val gap = barGap.toPx()
    val size = max(0f, (band - 2 * offset - (series - 1) * gap) / series)
    return (0 until series).map { s -> (offset + s * (size + gap)) to size }
}
