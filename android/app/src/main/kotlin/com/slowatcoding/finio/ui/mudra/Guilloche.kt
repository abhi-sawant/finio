package com.slowatcoding.finio.ui.mudra

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.round
import kotlin.math.sin

/**
 * Procedural guilloche rosette — the engraved lathe-work every banknote carries. A line-for-line
 * port of web/src/components/ui/guilloche.tsx: a 240-unit viewBox, [rings] closed curves of
 * radius 46 + 7k modulated by `amp·sin(petals·t + 0.6k)`, sampled every 3°, rounded to 0.1 like
 * the SVG path text, stroked at 0.55 units with opacity 0.9 − 0.08k. Purely decorative.
 *
 * Like the SVG (`preserveAspectRatio` meet), the rosette is drawn square, centred in the bounds.
 */
@Composable
fun Guilloche(
    color: Color,
    modifier: Modifier = Modifier,
    petals: Int = 18,
    rings: Int = 7,
) {
    val paths = remember(petals, rings) { guillochePaths(petals, rings) }
    Canvas(modifier) { drawGuilloche(paths, color) }
}

internal fun guillochePaths(petals: Int, rings: Int): List<Path> = List(rings) { k ->
    val radius = 46.0 + k * 7
    val amp = 9.0 + (k % 3) * 3
    Path().apply {
        var i = 0
        while (i <= 360) {
            val t = i * PI / 180
            val r = radius + amp * sin(petals * t + k * 0.6)
            val x = round1(120 + r * cos(t))
            val y = round1(120 + r * sin(t))
            if (i == 0) moveTo(x, y) else lineTo(x, y)
            i += 3
        }
        close()
    }
}

/** `Number.toFixed(1)`, as the web serialises its path points. */
private fun round1(v: Double): Float = (round(v * 10) / 10).toFloat()

/**
 * Draws [paths] (in a [viewBox]-unit square) scaled into the square at [topLeft] of side
 * [boxSize] px — how the web positions its absolutely-placed rosettes (`top`/`right`/`width`).
 */
internal fun DrawScope.drawGuilloche(
    paths: List<Path>,
    color: Color,
    topLeft: Offset = Offset((size.width - min(size.width, size.height)) / 2f, (size.height - min(size.width, size.height)) / 2f),
    boxSize: Float = min(size.width, size.height),
    viewBox: Float = 240f,
    strokeWidth: Float = 0.55f,
) {
    val s = boxSize / viewBox
    translate(topLeft.x, topLeft.y) {
        scale(s, s, pivot = Offset.Zero) {
            paths.forEachIndexed { i, path ->
                drawPath(
                    path,
                    color = color,
                    alpha = (0.9f - i * 0.08f).coerceIn(0f, 1f),
                    style = Stroke(width = strokeWidth),
                )
            }
        }
    }
}

/** The page rosette (public/guilloche.svg), parsed once. */
internal val pageRosettePaths: List<Path> by lazy {
    PageRosetteData.paths.map { PathParser().parsePathString(it).toPath() }
}
