package com.slowatcoding.finio.ui.mudra

import android.graphics.BlurMaskFilter
import android.graphics.Paint
import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.slowatcoding.finio.ui.theme.CssConicGradient
import com.slowatcoding.finio.ui.theme.CssShadow
import com.slowatcoding.finio.ui.theme.EaseOutExpo
import com.slowatcoding.finio.ui.theme.FinioShapes
import com.slowatcoding.finio.ui.theme.FinioTheme
import com.slowatcoding.finio.ui.theme.FinioType
import com.slowatcoding.finio.ui.theme.cssInsetShadow
import com.slowatcoding.finio.ui.theme.cssShadow
import com.slowatcoding.finio.ui.theme.hsl
import com.slowatcoding.finio.ui.theme.mix
import com.slowatcoding.finio.ui.theme.rememberReducedMotion
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.roundToInt
import kotlin.math.sin

private const val MICROPRINT = "FINIO · KEPT ON THIS DEVICE · NOT A BANK · YOUR DATA STAYS YOURS · "

private class Fibre(
    val x: Float, val y: Float, val cx: Float, val cy: Float, val x2: Float, val y2: Float, val ink: Color,
)

/**
 * Security fibres, scattered once with the web's fixed seed (Park–Miller, seed 7) so they sit in
 * exactly the same places as on the PWA, in a 400×200 box stretched over the note. Right third
 * only, so no fibre crosses a figure.
 */
private val FIBRES: List<Fibre> = run {
    val inks = listOf(Color(0xFF7FF3FF), Color(0xFFFF8FD0), Color(0xFFFFF27A))
    var seed = 7L
    fun rand(): Double {
        seed = (seed * 16807) % 2147483647
        return (seed - 1) / 2147483646.0
    }
    fun r1(v: Double) = (round(v * 10) / 10).toFloat()
    List(26) { i ->
        val x = 272 + rand() * 124
        val y = rand() * 200
        val a = rand() * PI * 2
        val len = 5 + rand() * 9
        val bend = (rand() - 0.5) * 8
        val x2 = x + cos(a) * len
        val y2 = y + sin(a) * len
        val cx = (x + x2) / 2 - sin(a) * bend
        val cy = (y + y2) / 2 + cos(a) * bend
        Fibre(r1(x), r1(y), r1(cx), r1(cy), r1(x2), r1(y2), inks[i % inks.size])
    }
}

private val SheenColors = listOf(
    Color(120, 255, 210, (0.35f * 255).roundToInt()),
    Color(120, 160, 255, (0.3f * 255).roundToInt()),
    Color(240, 140, 255, (0.3f * 255).roundToInt()),
    Color(255, 230, 140, (0.3f * 255).roundToInt()),
    Color(120, 255, 210, (0.35f * 255).roundToInt()),
)

/**
 * The Mudra hero surface — a banknote (web/src/components/ui/note-card.tsx + `.note-card*`).
 *
 * `grad-surface` fill, glass hairline + inset highlight, Float shadow, 25.3dp radius; a lavender
 * guilloche rosette top-right; a 9dp windowed security thread whose hue (150° → 200° → 255°)
 * shifts by up to 140° as a pointer crosses the note, which also tilts up to ±2°/±3°
 * (perspective 900, 0.5s ease-out-expo); a thin-film conic sheen; the privacy microprint along
 * the bottom edge; and UV fibres that only fluoresce in dark mode.
 *
 * Unlike the web (where touch never tilts), a finger dragging across the note shifts the thread
 * and tilts it too, springing flat on release — events are observed, never consumed, so the
 * page still scrolls. Reduced motion removes the tilt.
 *
 * Content keeps clear of the thread with 80dp end padding (none from 1024dp, where it is capped
 * at 512dp wide instead). Lay out the inside freely.
 */
@Composable
fun NoteCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val colors = FinioTheme.colors
    val brushes = FinioTheme.brushes
    val shadows = FinioTheme.shadows
    val density = LocalDensity.current
    val wide = LocalConfiguration.current.screenWidthDp >= 1024
    val reducedMotion = rememberReducedMotion()
    val scope = rememberCoroutineScope()

    var shift by remember { mutableFloatStateOf(0f) }
    val rx = remember { Animatable(0f) }
    val ry = remember { Animatable(0f) }
    val tilt = tween<Float>(500, easing = EaseOutExpo)

    val shape = FinioShapes.note
    val highlight = remember(colors.glassHighlight) { listOf(CssShadow(y = 1.dp, color = colors.glassHighlight, inset = true)) }

    Box(
        modifier
            .pointerInput(reducedMotion) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull() ?: continue
                        when (event.type) {
                            PointerEventType.Move, PointerEventType.Press, PointerEventType.Enter -> {
                                if (size.width == 0 || size.height == 0) continue
                                val x = (change.position.x / size.width).coerceIn(0f, 1f)
                                val y = (change.position.y / size.height).coerceIn(0f, 1f)
                                shift = (x * 140f).roundToInt().toFloat()
                                if (!reducedMotion) {
                                    scope.launch { rx.animateTo((0.5f - y) * 4f, tilt) }
                                    scope.launch { ry.animateTo((x - 0.5f) * 6f, tilt) }
                                }
                            }
                            PointerEventType.Release, PointerEventType.Exit -> {
                                scope.launch { rx.animateTo(0f, tilt) }
                                scope.launch { ry.animateTo(0f, tilt) }
                            }
                        }
                    }
                }
            }
            .graphicsLayer {
                rotationX = rx.value
                rotationY = ry.value
                cameraDistance = 900f * density.density
            }
            .cssShadow(shape, shadows.float)
            .clip(shape)
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .background(brushes.gradSurface)
            .cssInsetShadow(shape, highlight)
            .border(1.dp, colors.glassBorder, shape),
    ) {
        // Fibres, rosette and thread — the engraving under everything.
        Spacer(
            Modifier
                .matchParentSize()
                .drawWithCache {
                    val sx = size.width / 400f
                    val sy = size.height / 200f
                    val fibrePaths = FIBRES.map { f ->
                        Path().apply {
                            moveTo(f.x * sx, f.y * sy)
                            quadraticTo(f.cx * sx, f.cy * sy, f.x2 * sx, f.y2 * sy)
                        } to f.ink
                    }
                    val fibreWidth = 1.2.dp.toPx()
                    val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        style = Paint.Style.STROKE
                        strokeWidth = fibreWidth
                        strokeCap = Paint.Cap.ROUND
                        color = colors.foreground.copy(alpha = 0.85f).toArgb()
                        maskFilter = BlurMaskFilter(((1.dp.toPx()) - 0.5f).coerceAtLeast(0.5f) / 0.57735f, BlurMaskFilter.Blur.NORMAL)
                    }
                    val rosetteBox = (if (wide) 380.dp else 250.dp).toPx()
                    val rosetteTopLeft = if (wide) {
                        Offset(size.width - 40.dp.toPx() - rosetteBox, -90.dp.toPx())
                    } else {
                        Offset(size.width + 60.dp.toPx() - rosetteBox, -50.dp.toPx())
                    }
                    val rosette = guillochePaths(18, 7)
                    val threadWidth = 9.dp.toPx()
                    val threadLeft = size.width - (if (wide) 30.dp else 56.dp).toPx() - threadWidth
                    onDrawBehind {
                        if (colors.isDark) {
                            // `.dark .note-card-fibres { opacity: .85; filter: drop-shadow(0 0 2px currentColor) }`
                            drawIntoCanvas { c ->
                                for ((p, _) in fibrePaths) c.nativeCanvas.drawPath(p.asAndroidPath(), glow)
                            }
                            for ((p, ink) in fibrePaths) {
                                drawPath(p, ink, alpha = 0.85f, style = Stroke(fibreWidth, cap = StrokeCap.Round))
                            }
                        }
                        drawGuilloche(rosette, colors.primary.mix(0.26f), rosetteTopLeft, rosetteBox)
                        val threadBrush = Brush.verticalGradient(
                            listOf(
                                hsl(150f + shift, 0.70f, 0.42f),
                                hsl(200f + shift, 0.78f, 0.50f),
                                hsl(255f + shift, 0.70f, 0.58f),
                            ),
                            startY = 0f,
                            endY = size.height,
                        )
                        drawWindows(
                            threadBrush,
                            Offset(threadLeft, 0f),
                            Size(threadWidth, size.height),
                            on = 16.dp.toPx(),
                            off = 10.dp.toPx(),
                            vertical = true,
                            alpha = 0.85f,
                        )
                    }
                },
        )

        Microprint(
            color = colors.foreground.mix(0.36f),
            fontSize = with(density) { 6.dp.toSp() },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 10.dp),
        )

        // Thin-film sheen, turning with the pointer: soft-light in daylight, screen at 30% in UV.
        Spacer(
            Modifier
                .matchParentSize()
                .drawBehind {
                    val sheen = CssConicGradient(200f + shift, 0.7f, 0.3f, SheenColors)
                    when {
                        colors.isDark -> drawRect(sheen, alpha = 0.3f, blendMode = BlendMode.Screen)
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ->
                            drawRect(sheen, blendMode = BlendMode.Softlight)
                        // No soft-light before API 29: a faint plain wash is the closest look.
                        else -> drawRect(sheen, alpha = 0.18f)
                    }
                },
        )

        CompositionLocalProvider(LocalContentColor provides colors.foreground) {
            val padding = if (wide) PaddingValues(32.dp, 28.dp, 32.dp, 40.dp) else PaddingValues(22.dp, 22.dp, 22.dp, 34.dp)
            Column(
                Modifier
                    .padding(padding)
                    .then(if (wide) Modifier.widthIn(max = 512.dp) else Modifier.padding(end = 80.dp)),
                content = content,
            )
        }
    }
}

@Composable
private fun Microprint(color: Color, fontSize: TextUnit, modifier: Modifier) {
    Text(
        text = MICROPRINT.repeat(6),
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics { },
        color = color,
        style = FinioType.caption.copy(
            fontSize = fontSize,
            lineHeight = fontSize * 1.5f,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.2.em,
        ),
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
    )
}

/**
 * Size step for a NoteCard's headline figure (note-figure.ts): a crore-scale amount must shrink
 * rather than run under the thread or get clipped at the card edge.
 */
fun noteFigureSize(text: String): TextUnit = when {
    text.length <= 8 -> 44.sp
    text.length <= 11 -> 36.sp
    else -> 28.sp
}

/** [FinioType.displayMoney] at the [noteFigureSize] step for [text] (line-height 1.05). */
fun noteFigureStyle(text: String) = noteFigureSize(text).let { size ->
    FinioType.displayMoney.copy(fontSize = size, lineHeight = size * 1.05f)
}

