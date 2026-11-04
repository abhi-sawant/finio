package com.slowatcoding.finio.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The radius scale, all derived from `--radius` (Mudra 22px; web/src/index.css and each theme file):
 * `sm` ×0.6, `md` ×0.8, `lg` ×1, `xl` ×1.4, `note` ×1.15.
 */
@Immutable
class FinioShapeScale(
    val base: Dp,
    /** Buttons, icon buttons, nav items, coin, progress tracks. Mudra: a circle. */
    val full: Shape = CircleShape,
    /** Bagh's leaf corners: three big radii and one tight one (the tight one is [leafTight]). */
    val leafTight: Dp? = null,
) {
    private fun shape(r: Dp): CornerBasedShape =
        if (leafTight == null) RoundedCornerShape(r) else RoundedCornerShape(r, r, r, minOf(leafTight, r))

    val sm: CornerBasedShape = RoundedCornerShape(base * 0.6f)
    val md: CornerBasedShape = shape(base * 0.8f)
    val lg: CornerBasedShape = shape(base)
    val xl: CornerBasedShape = shape(base * 1.4f)
    val note: CornerBasedShape = shape(base * 1.15f)
    val chip: CornerBasedShape = RoundedCornerShape(5.dp)

    val radiusSm: Dp = base * 0.6f
    val radiusMd: Dp = base * 0.8f
    val radiusLg: Dp = base
}

val MudraShapeScale = FinioShapeScale(22.dp)

fun shapeScaleFor(visual: VisualTheme): FinioShapeScale = when (visual) {
    VisualTheme.Mudra -> MudraShapeScale
    VisualTheme.Khata -> KhataShapeScale
    VisualTheme.Gulal -> GulalShapeScale
    VisualTheme.Shunya -> ShunyaShapeScale
    VisualTheme.Bagh -> BaghShapeScale
}

// Khata: nothing is round — pills and circles become 3dp squares, cards 2dp.
private val KhataShapeScale = FinioShapeScale(base = 4.dp, full = RoundedCornerShape(3.dp))
private val GulalShapeScale = FinioShapeScale(base = 24.dp)
private val ShunyaShapeScale = FinioShapeScale(base = 14.dp)
private val BaghShapeScale = FinioShapeScale(base = 24.dp, leafTight = 10.dp)

val LocalFinioShapes = staticCompositionLocalOf { MudraShapeScale }

/** Static radii kept for the few callers that build their own corner shapes. */
object FinioRadius {
    val sm: Dp @Composable @ReadOnlyComposable get() = LocalFinioShapes.current.radiusSm
    val md: Dp @Composable @ReadOnlyComposable get() = LocalFinioShapes.current.radiusMd
    val lg: Dp @Composable @ReadOnlyComposable get() = LocalFinioShapes.current.radiusLg
}

/** The shapes of the active visual theme: `FinioShapes.md` etc. (composable getters). */
object FinioShapes {
    val sm: Shape @Composable @ReadOnlyComposable get() = LocalFinioShapes.current.sm
    val md: Shape @Composable @ReadOnlyComposable get() = LocalFinioShapes.current.md
    val lg: Shape @Composable @ReadOnlyComposable get() = LocalFinioShapes.current.lg
    val xl: Shape @Composable @ReadOnlyComposable get() = LocalFinioShapes.current.xl
    val note: Shape @Composable @ReadOnlyComposable get() = LocalFinioShapes.current.note
    val chip: Shape @Composable @ReadOnlyComposable get() = LocalFinioShapes.current.chip
    /** Buttons, icon buttons, nav items, tab indicator, coin, progress tracks. */
    val full: Shape @Composable @ReadOnlyComposable get() = LocalFinioShapes.current.full
}
