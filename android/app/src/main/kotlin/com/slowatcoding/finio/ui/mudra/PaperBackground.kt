package com.slowatcoding.finio.ui.mudra

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.dp
import com.slowatcoding.finio.ui.theme.FinioTheme

/**
 * The note paper every screen is printed on: `body { background: var(--background) var(--paper)
 * fixed }` plus the engraved rosette of `body::before` (560px, 200px above and 240px past the
 * top-right corner, in `--engraving`).
 *
 * It is *fixed*: put scrolling content inside it, never scroll the paper itself.
 */
@Composable
fun PaperBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit = {}) {
    val colors = FinioTheme.colors
    val paper = FinioTheme.brushes.paper
    Box(
        modifier
            .fillMaxSize()
            .background(colors.background)
            .background(paper)
            .drawBehind {
                val box = 560.dp.toPx()
                val left = size.width + 240.dp.toPx() - box
                val top = -200.dp.toPx()
                val s = box / PageRosetteData.VIEWBOX
                translate(left, top) {
                    scale(s, s, pivot = androidx.compose.ui.geometry.Offset.Zero) {
                        pageRosettePaths.forEachIndexed { i, path ->
                            drawPath(
                                path,
                                color = colors.engraving,
                                alpha = PageRosetteData.opacities[i],
                                style = Stroke(PageRosetteData.STROKE_WIDTH),
                            )
                        }
                    }
                }
            },
        content = content,
    )
}
