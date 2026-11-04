package com.slowatcoding.finio.ui.mudra

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.slowatcoding.finio.ui.theme.BaghScene
import com.slowatcoding.finio.ui.theme.CssShadow
import com.slowatcoding.finio.ui.theme.FinioShapes
import com.slowatcoding.finio.ui.theme.FinioTheme
import com.slowatcoding.finio.ui.theme.LocalFinioColors
import com.slowatcoding.finio.ui.theme.VisualTheme
import com.slowatcoding.finio.ui.theme.cssShadow

private val Ink = Color(0xFF1B1230)

private val hillBack = PathParser().parsePathString("M0 22C50 4 110 8 170 24S290 36 342 14V70H0z").toPath()
private val hillFront = PathParser().parsePathString("M0 40C60 22 130 28 190 40S300 50 342 34V70H0z").toPath()

/**
 * The hero surface for every theme but Mudra (web: `.note-card` in the theme stylesheets). Each world keeps
 * the same job — the one figure per money screen — and paints it its own way:
 *
 * - **Khata** — a ledger sheet: paper fill, an ink outline and a double rule at the foot.
 * - **Gulal** — a pink sticker, hard shadow, a hair of tilt; its inside stays dark ink in both
 *   modes, and [badge] (the "% used" burst) is stamped on its corner.
 * - **Shunya** — nothing: no fill, no outline, just the figure and whatever [scene] draws.
 * - **Bagh** — sky, the [scene] (sun on an arc) and two hills along the bottom edge.
 */
@Composable
internal fun ThemedNoteCard(
    visual: VisualTheme,
    modifier: Modifier,
    badge: (@Composable () -> Unit)?,
    scene: (@Composable () -> Unit)?,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = FinioTheme.colors
    val shadows = FinioTheme.shadows
    val brushes = FinioTheme.brushes
    val shape = FinioShapes.note
    val wide = LocalConfiguration.current.screenWidthDp >= 1024
    val dark = colors.isDark

    val container = when (visual) {
        VisualTheme.Khata -> Modifier
            .clip(shape)
            .background(colors.card)
            .border(1.dp, colors.foreground.copy(alpha = 0.55f), shape)
            .drawBehind {
                val y = size.height - 3.dp.toPx()
                drawLine(colors.foreground, androidx.compose.ui.geometry.Offset(0f, y), androidx.compose.ui.geometry.Offset(size.width, y), 1.dp.toPx())
            }
        VisualTheme.Gulal -> Modifier
            .rotate(-0.6f)
            .cssShadow(shape, listOf(CssShadow(x = 6.dp, y = 6.dp, color = if (dark) Color(0xFFFFE14D) else Ink)))
            .clip(shape)
            .background(Color(0xFFFF5FA8))
            .border(2.5.dp, Ink, shape)
        VisualTheme.Bagh -> Modifier
            .cssShadow(shape, shadows.card)
            .clip(shape)
            .background(brushes.gradSurface)
            .drawBehind {
                val h = 70.dp.toPx()
                val sx = size.width / 342f
                val sy = h / 70f
                for ((path, color) in listOf(hillBack to BaghScene.hillBack(dark), hillFront to BaghScene.hillFront(dark))) {
                    translate(0f, size.height - h) {
                        scale(sx, sy, androidx.compose.ui.geometry.Offset.Zero) { drawPath(path, color) }
                    }
                }
            }
        else -> Modifier
    }

    val padding = when (visual) {
        VisualTheme.Shunya -> PaddingValues(0.dp, 12.dp, 0.dp, 8.dp)
        VisualTheme.Bagh -> PaddingValues(22.dp, 12.dp, 22.dp, 92.dp)
        else -> if (wide) PaddingValues(32.dp, 28.dp, 32.dp, 36.dp) else PaddingValues(22.dp, 22.dp, 22.dp, 28.dp)
    }

    val inner: @Composable () -> Unit = {
        Box(modifier.fillMaxWidth().then(container)) {
            Column(Modifier.padding(padding)) {
                if (scene != null) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Box(Modifier.widthIn(max = 340.dp)) { scene() }
                    }
                }
                Column(if (wide) Modifier.widthIn(max = 512.dp) else Modifier, content = content)
            }
            if (badge != null) {
                Box(Modifier.align(Alignment.TopEnd).offset(x = (-14).dp, y = (-18).dp)) { badge() }
            }
        }
    }

    if (visual == VisualTheme.Gulal) {
        // The sticker's inside is always dark ink on pink, whatever the page mode.
        CompositionLocalProvider(
            LocalFinioColors provides colors.copy(
                foreground = Ink, mutedForeground = Ink, primary = Ink,
                destructive = Color(0xFF8A0A32), card = Color.White,
            ),
            LocalContentColor provides Ink,
        ) { inner() }
    } else {
        CompositionLocalProvider(LocalContentColor provides colors.foreground) { inner() }
    }
}
