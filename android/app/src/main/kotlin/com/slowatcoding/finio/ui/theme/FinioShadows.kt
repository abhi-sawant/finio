package com.slowatcoding.finio.ui.theme

import android.graphics.BlurMaskFilter
import android.graphics.Paint
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * One CSS `box-shadow` layer: `[inset] <x> <y> <blur> <spread> <color>`.
 *
 * Compose's `Modifier.shadow` is an elevation shadow — neutral, ambient-lit, no spread, no
 * offset — so Mudra's lavender-tinted, negatively-spread drops are drawn by hand (see
 * [cssShadow]).
 */
@Immutable
data class CssShadow(
    val x: Dp = 0.dp,
    val y: Dp = 0.dp,
    val blur: Dp = 0.dp,
    val spread: Dp = 0.dp,
    val color: Color,
    val inset: Boolean = false,
)

/** The shadow vocabulary of index.css / design.md "Elevation & Depth", per mode. */
@Immutable
data class FinioShadows(
    /** `--shadow-card` — glass cards, number-pad display, alert band. */
    val card: List<CssShadow>,
    /** `--shadow-float` — the hero note, dialogs, popovers, toasts, selected rings. */
    val float: List<CssShadow>,
    /** `shadow-glow-primary` — under gradient-filled buttons only (includes the inset). */
    val glowPrimary: List<CssShadow>,
    val glowSuccess: List<CssShadow>,
    val glowDanger: List<CssShadow>,
    /** `bg-coin`'s shadow (inset top light + lavender drop). */
    val coin: List<CssShadow>,
    /** Note tiles. */
    val tile: List<CssShadow>,
    /** The mobile tab bar — casts upward onto content. */
    val tabBar: List<CssShadow>,
    /** `.note-chip`: inner 1px white ring + a small drop. */
    val noteChip: List<CssShadow>,
    // Stock Tailwind scale, re-tinted from --shadow-tint.
    val xs: List<CssShadow>,
    val sm: List<CssShadow>,
    val md: List<CssShadow>,
    val lg: List<CssShadow>,
    val xl: List<CssShadow>,
)

private val whiteInset25 = CssShadow(y = 1.dp, color = rgba(255, 255, 255, 0.25f), inset = true)

private fun stock(tint: Color) = listOf(
    listOf(CssShadow(y = 1.dp, blur = 2.dp, color = tint.copy(alpha = 0.08f))),
    listOf(
        CssShadow(y = 2.dp, blur = 6.dp, spread = (-1).dp, color = tint.copy(alpha = 0.14f)),
        CssShadow(y = 1.dp, blur = 2.dp, spread = (-1).dp, color = tint.copy(alpha = 0.1f)),
    ),
    listOf(
        CssShadow(y = 6.dp, blur = 14.dp, spread = (-4).dp, color = tint.copy(alpha = 0.2f)),
        CssShadow(y = 2.dp, blur = 4.dp, spread = (-2).dp, color = tint.copy(alpha = 0.12f)),
    ),
    listOf(
        CssShadow(y = 14.dp, blur = 28.dp, spread = (-10).dp, color = tint.copy(alpha = 0.28f)),
        CssShadow(y = 4.dp, blur = 8.dp, spread = (-4).dp, color = tint.copy(alpha = 0.12f)),
    ),
    listOf(
        CssShadow(y = 22.dp, blur = 44.dp, spread = (-16).dp, color = tint.copy(alpha = 0.34f)),
        CssShadow(y = 8.dp, blur = 12.dp, spread = (-6).dp, color = tint.copy(alpha = 0.14f)),
    ),
)

private val coinShadow = listOf(
    // `inset 0 1px 1px` — drawn unblurred; at 1px the difference is invisible.
    CssShadow(y = 1.dp, color = rgba(255, 255, 255, 0.6f), inset = true),
    CssShadow(y = 10.dp, blur = 24.dp, spread = (-6).dp, color = rgba(108, 87, 214, 0.7f)),
)
private val tileShadow = listOf(CssShadow(y = 14.dp, blur = 28.dp, spread = (-20).dp, color = rgba(40, 26, 110, 0.5f)))
private val tabBarShadow = listOf(CssShadow(y = (-12).dp, blur = 30.dp, spread = (-22).dp, color = rgba(40, 26, 110, 0.45f)))
private val noteChipShadow = listOf(
    CssShadow(spread = 1.dp, color = rgba(255, 255, 255, 0.55f), inset = true),
    CssShadow(y = 3.dp, blur = 8.dp, spread = (-4).dp, color = rgba(40, 26, 110, 0.45f)),
)

val LightFinioShadows = stock(LightFinioColors.shadowTint).let { s ->
    FinioShadows(
        card = listOf(CssShadow(y = 18.dp, blur = 36.dp, spread = (-26).dp, color = rgba(60, 40, 140, 0.42f))),
        float = listOf(CssShadow(y = 28.dp, blur = 56.dp, spread = (-26).dp, color = rgba(40, 26, 110, 0.5f))),
        glowPrimary = listOf(whiteInset25, CssShadow(y = 10.dp, blur = 22.dp, spread = (-10).dp, color = rgba(75, 54, 199, 0.65f))),
        glowSuccess = listOf(whiteInset25, CssShadow(y = 10.dp, blur = 22.dp, spread = (-10).dp, color = rgba(11, 122, 85, 0.5f))),
        glowDanger = listOf(whiteInset25, CssShadow(y = 10.dp, blur = 22.dp, spread = (-10).dp, color = rgba(176, 18, 95, 0.55f))),
        coin = coinShadow,
        tile = tileShadow,
        tabBar = tabBarShadow,
        noteChip = noteChipShadow,
        xs = s[0], sm = s[1], md = s[2], lg = s[3], xl = s[4],
    )
}

val DarkFinioShadows = stock(DarkFinioColors.shadowTint).let { s ->
    FinioShadows(
        card = listOf(CssShadow(y = 20.dp, blur = 40.dp, spread = (-26).dp, color = rgba(0, 0, 0, 0.7f))),
        float = listOf(CssShadow(y = 28.dp, blur = 56.dp, spread = (-24).dp, color = rgba(0, 0, 0, 0.75f))),
        glowPrimary = listOf(whiteInset25, CssShadow(y = 10.dp, blur = 24.dp, spread = (-8).dp, color = rgba(108, 87, 214, 0.7f))),
        glowSuccess = listOf(whiteInset25, CssShadow(y = 10.dp, blur = 24.dp, spread = (-8).dp, color = rgba(23, 167, 119, 0.45f))),
        glowDanger = listOf(whiteInset25, CssShadow(y = 10.dp, blur = 24.dp, spread = (-8).dp, color = rgba(224, 65, 143, 0.5f))),
        coin = coinShadow,
        tile = tileShadow,
        tabBar = tabBarShadow,
        noteChip = noteChipShadow,
        xs = s[0], sm = s[1], md = s[2], lg = s[3], xl = s[4],
    )
}

val LocalFinioShadows = staticCompositionLocalOf { LightFinioShadows }

/**
 * Draws CSS `box-shadow` layers for an element of [shape].
 *
 * Put it **before** the element's background in the modifier chain: outer layers paint behind
 * the background and — like CSS — are clipped out of the element's own box, so a translucent
 * glass card never shows its own shadow through itself. Inset layers paint *after* the
 * background (on top of it, under the content), so they need their own [cssInsetShadow] placed
 * after the background; [cssShadow] ignores them.
 *
 * Blur is a Gaussian with σ = blur / 2, as CSS specifies, via [BlurMaskFilter]. Spread grows
 * (or, negative, shrinks) the shape and its corner radii.
 */
fun Modifier.cssShadow(shape: Shape, shadows: List<CssShadow>): Modifier {
    val outer = shadows.filter { !it.inset }
    if (outer.isEmpty()) return this
    return drawWithCache {
        val outline = shape.createOutline(size, layoutDirection, this)
        val clip = outline.toPath()
        val layers = outer.map { s ->
            val spread = s.spread.toPx()
            val path = outline.spread(spread, size)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = s.color.toArgb()
                val sigma = s.blur.toPx() / 2f
                if (sigma > 0.5f) maskFilter = BlurMaskFilter((sigma - 0.5f) / 0.57735f, BlurMaskFilter.Blur.NORMAL)
            }
            Triple(path.asAndroidPath(), paint, Offset(s.x.toPx(), s.y.toPx()))
        }
        val clipPath = clip.asAndroidPath()
        onDrawBehind {
            drawIntoCanvas { canvas ->
                val native = canvas.nativeCanvas
                native.save()
                native.clipOutPath(clipPath)
                for ((path, paint, offset) in layers) {
                    native.save()
                    native.translate(offset.x, offset.y)
                    native.drawPath(path, paint)
                    native.restore()
                }
                native.restore()
            }
        }
    }
}

/**
 * The inset layers of a CSS `box-shadow` (the glass top highlight `inset 0 1px 0`, the note chip's
 * 1px inner ring). Place it **after** the background. Only unblurred insets are supported —
 * Mudra uses no other kind.
 */
fun Modifier.cssInsetShadow(shape: Shape, shadows: List<CssShadow>): Modifier {
    val inner = shadows.filter { it.inset }
    if (inner.isEmpty()) return this
    return drawWithCache {
        val outline = shape.createOutline(size, layoutDirection, this)
        val base = outline.toPath()
        val rings = inner.map { s ->
            // The inset shadow's "hole" is the shape shifted by the offset and shrunk by spread;
            // what is painted is the shape minus that hole.
            val hole = outline.spread(-s.spread.toPx(), size).apply {
                translate(Offset(s.x.toPx(), s.y.toPx()))
            }
            Path.combine(PathOperation.Difference, base, hole) to s.color
        }
        onDrawBehind {
            for ((path, color) in rings) drawPath(path, color)
        }
    }
}

internal fun Outline.toPath(): Path = when (this) {
    is Outline.Generic -> path
    is Outline.Rectangle -> Path().apply { addRect(rect) }
    is Outline.Rounded -> Path().apply { addRoundRect(roundRect) }
}

/** The outline grown by [spread] px on every side, with radii grown to match (CSS spread). */
private fun Outline.spread(spread: Float, size: Size): Path {
    if (spread == 0f) return toPath()
    return when (this) {
        is Outline.Rectangle -> Path().apply {
            addRect(rect.inflate(spread).takeIf { it.width > 0 && it.height > 0 } ?: rect.copy(right = rect.left, bottom = rect.top))
        }
        is Outline.Rounded -> Path().apply {
            val r = roundRect
            val w = (r.width + 2 * spread).coerceAtLeast(0f)
            val h = (r.height + 2 * spread).coerceAtLeast(0f)
            fun grow(c: CornerRadius) = CornerRadius((c.x + spread).coerceAtLeast(0f), (c.y + spread).coerceAtLeast(0f))
            addRoundRect(
                RoundRect(
                    left = r.left - spread,
                    top = r.top - spread,
                    right = r.left - spread + w,
                    bottom = r.top - spread + h,
                    topLeftCornerRadius = grow(r.topLeftCornerRadius),
                    topRightCornerRadius = grow(r.topRightCornerRadius),
                    bottomRightCornerRadius = grow(r.bottomRightCornerRadius),
                    bottomLeftCornerRadius = grow(r.bottomLeftCornerRadius),
                ),
            )
        }
        is Outline.Generic -> Path().apply {
            // Arbitrary paths: approximate spread by scaling about the centre.
            addPath(path)
            if (size.width > 0 && size.height > 0) {
                val m = androidx.compose.ui.graphics.Matrix()
                val sx = (size.width + 2 * spread) / size.width
                val sy = (size.height + 2 * spread) / size.height
                m.translate(size.width / 2, size.height / 2)
                m.scale(sx, sy)
                m.translate(-size.width / 2, -size.height / 2)
                transform(m)
            }
        }
    }
}
