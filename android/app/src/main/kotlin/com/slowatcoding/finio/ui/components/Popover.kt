package com.slowatcoding.finio.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.slowatcoding.finio.ui.theme.FinioShapes
import com.slowatcoding.finio.ui.theme.FinioTheme
import com.slowatcoding.finio.ui.theme.cssShadow

/**
 * Below the anchor, start-aligned, [gapPx] apart — flipping above when it would run off the
 * bottom, and clamped inside the window (Base UI's `side="bottom" align="start"` with collision
 * avoidance).
 */
private class AnchoredBelow(private val gapPx: Int, private val marginPx: Int) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        // The popup content carries an 8dp transparent inset (shadow room) on its sides.
        val x = (if (layoutDirection == LayoutDirection.Ltr) anchorBounds.left - marginPx else anchorBounds.right - popupContentSize.width + marginPx)
            .coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0))
        val below = anchorBounds.bottom + gapPx
        val y = if (below + popupContentSize.height <= windowSize.height) {
            below
        } else {
            (anchorBounds.top - gapPx - popupContentSize.height + 3 * marginPx).coerceAtLeast(0)
        }
        return IntOffset(x, y)
    }
}

/**
 * An anchored popover (popover.tsx `PopoverContent`): opaque `--popover`, 1dp `--border` ring,
 * `md` radius, Float shadow, fading and zooming in from 95% over 100ms. Call it from inside the
 * trigger's layout so it anchors to it; tapping outside dismisses.
 */
@Composable
fun FinioPopover(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(10.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = FinioTheme.colors
    val density = LocalDensity.current
    val provider = remember(density) { AnchoredBelow(with(density) { 4.dp.roundToPx() }, with(density) { 8.dp.roundToPx() }) }
    val enter = remember { Animatable(0f) }
    LaunchedEffect(Unit) { enter.animateTo(1f, tween(100)) }
    Popup(popupPositionProvider = provider, onDismissRequest = onDismissRequest, properties = PopupProperties(focusable = true)) {
        val shape = FinioShapes.md
        CompositionLocalProvider(LocalContentColor provides colors.popoverForeground) {
            Box(
                modifier
                    // Room for the Float shadow, which a popup window would otherwise crop.
                    .padding(start = 8.dp, end = 8.dp, bottom = 24.dp)
                    .graphicsLayer {
                        alpha = enter.value
                        val s = 0.95f + 0.05f * enter.value
                        scaleX = s
                        scaleY = s
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0f)
                    }
                    .cssShadow(shape, FinioTheme.shadows.float)
                    .clip(shape)
                    .background(colors.popover)
                    .border(1.dp, colors.border, shape)
                    .padding(contentPadding),
                content = content,
            )
        }
    }
}
