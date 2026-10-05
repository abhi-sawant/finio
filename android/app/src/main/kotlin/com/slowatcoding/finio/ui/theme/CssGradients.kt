package com.slowatcoding.finio.ui.theme

import android.graphics.Matrix
import android.graphics.SweepGradient
import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.RadialGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.toArgb
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin

/** One colour stop: CSS `<color> <position%>` with [position] in 0..1. */
@Immutable
data class Stop(val position: Float, val color: Color)

/**
 * CSS `linear-gradient(<angle>deg, …)` with the CSS geometry: 0deg points up, angles turn
 * clockwise, and the gradient line is sized so the 0% and 100% stops touch the box's corners
 * (`|w·sinθ| + |h·cosθ|`). Compose's own `Brush.linearGradient` runs corner to corner instead,
 * which only matches CSS for a square at 135deg — so every Mudra gradient goes through this.
 */
@Immutable
class CssLinearGradient(
    private val angleDegrees: Float,
    private val stops: List<Stop>,
) : ShaderBrush() {
    override fun createShader(size: Size): Shader {
        val rad = Math.toRadians(angleDegrees.toDouble())
        val dx = sin(rad).toFloat()
        val dy = -cos(rad).toFloat()
        val half = (abs(size.width * dx) + abs(size.height * dy)) / 2f
        val c = Offset(size.width / 2f, size.height / 2f)
        return LinearGradientShader(
            from = Offset(c.x - dx * half, c.y - dy * half),
            to = Offset(c.x + dx * half, c.y + dy * half),
            colors = stops.map { it.color },
            colorStops = stops.map { it.position },
        )
    }

    override fun equals(other: Any?) =
        other is CssLinearGradient && other.angleDegrees == angleDegrees && other.stops == stops

    override fun hashCode() = 31 * angleDegrees.hashCode() + stops.hashCode()
}

/**
 * CSS `repeating-linear-gradient(<angle>deg, …)` whose stops span [periodPx] along the gradient
 * line — the budget "register" hatch. Hard stops are expressed as two stops at one position.
 */
@Immutable
class CssRepeatingLinearGradient(
    private val angleDegrees: Float,
    private val periodPx: Float,
    private val stops: List<Stop>,
) : ShaderBrush() {
    override fun createShader(size: Size): Shader {
        val rad = Math.toRadians(angleDegrees.toDouble())
        val dx = sin(rad).toFloat()
        val dy = -cos(rad).toFloat()
        val half = (abs(size.width * dx) + abs(size.height * dy)) / 2f
        val start = Offset(size.width / 2f - dx * half, size.height / 2f - dy * half)
        return LinearGradientShader(
            from = start,
            to = Offset(start.x + dx * periodPx, start.y + dy * periodPx),
            colors = stops.map { it.color },
            colorStops = stops.map { it.position },
            tileMode = TileMode.Repeated,
        )
    }

    override fun equals(other: Any?) =
        other is CssRepeatingLinearGradient && other.angleDegrees == angleDegrees &&
            other.periodPx == periodPx && other.stops == stops

    override fun hashCode() = (31 * angleDegrees.hashCode() + periodPx.hashCode()) * 31 + stops.hashCode()
}

/**
 * CSS `radial-gradient(circle at <x%> <y%>, …)` with the default `farthest-corner` extent — the
 * coin and the app-icon tile.
 */
@Immutable
class CssRadialGradient(
    private val centerX: Float,
    private val centerY: Float,
    private val stops: List<Stop>,
) : ShaderBrush() {
    override fun createShader(size: Size): Shader {
        val cx = size.width * centerX
        val cy = size.height * centerY
        val radius = max(
            max(hypot(cx, cy), hypot(size.width - cx, cy)),
            max(hypot(cx, size.height - cy), hypot(size.width - cx, size.height - cy)),
        ).coerceAtLeast(0.01f)
        return RadialGradientShader(
            center = Offset(cx, cy),
            radius = radius,
            colors = stops.map { it.color },
            colorStops = stops.map { it.position },
        )
    }

    override fun equals(other: Any?) =
        other is CssRadialGradient && other.centerX == centerX && other.centerY == centerY &&
            other.stops == stops

    override fun hashCode() = (31 * centerX.hashCode() + centerY.hashCode()) * 31 + stops.hashCode()
}

/**
 * CSS `conic-gradient(from <angle>deg at <x%> <y%>, …)`. CSS starts at 12 o'clock, Android's
 * sweep at 3 o'clock, hence the −90° in the local matrix. Stops are spread evenly, as in CSS when
 * none carry positions.
 */
@Immutable
class CssConicGradient(
    private val fromDegrees: Float,
    private val centerX: Float,
    private val centerY: Float,
    private val colors: List<Color>,
) : ShaderBrush() {
    override fun createShader(size: Size): Shader {
        val cx = size.width * centerX
        val cy = size.height * centerY
        return SweepGradient(cx, cy, colors.map { it.toArgb() }.toIntArray(), null).apply {
            setLocalMatrix(Matrix().apply { setRotate(fromDegrees - 90f, cx, cy) })
        }
    }

    override fun equals(other: Any?) =
        other is CssConicGradient && other.fromDegrees == fromDegrees &&
            other.centerX == centerX && other.centerY == centerY && other.colors == colors

    override fun hashCode() =
        ((31 * fromDegrees.hashCode() + centerX.hashCode()) * 31 + centerY.hashCode()) * 31 + colors.hashCode()
}

/** `linear-gradient(<angle>deg, a 0%, b 100%)` shorthand. */
fun cssLinear(angle: Float, vararg stops: Stop) = CssLinearGradient(angle, stops.toList())

/** Evenly spaced stops, like CSS stops written without positions. */
fun cssLinear(angle: Float, colors: List<Color>) = CssLinearGradient(
    angle,
    colors.mapIndexed { i, c -> Stop(if (colors.size == 1) 0f else i / (colors.size - 1f), c) },
)
