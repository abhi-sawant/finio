package com.slowatcoding.finio.ui.theme.kits

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.slowatcoding.finio.ui.navigation.FinioTab
import com.slowatcoding.finio.ui.theme.FinioShapeScale
import com.slowatcoding.finio.ui.theme.FinioTypeScale
import com.slowatcoding.finio.ui.theme.ThemePalette
import com.slowatcoding.finio.ui.theme.VisualTheme

/**
 * Everything one of the round-two themes (Parchi, Mahal, Ganak, Akhbaar, Khel) brings, in one
 * place. The round-one themes are threaded through the shared files with `when (visual)`; from
 * round two on a theme is a single [ThemeKit] in this package and the shared files ask
 * [kitFor] — so adding a theme never means editing ten `when`s. Mirrors `web/src/themes/<id>.css`.
 *
 * Every member has the Mudra-ish default, so a kit only overrides what its world changes.
 */
interface ThemeKit {
    val visual: VisualTheme

    /** Colours, brushes, shadows and spec for one mode (AMOLED is applied on top by the caller). */
    fun palette(dark: Boolean): ThemePalette

    val type: FinioTypeScale
    val shapes: FinioShapeScale

    // ── the add button (Coin.kt) ──
    fun coinInk(dark: Boolean): Color
    /** The coin's outline; null means [FinioShapeScale.full]. */
    val coinShape: Shape? get() = null
    /** Extra decoration on the coin after its fill (an outline, a bezel…). */
    @Composable fun coinDecoration(shape: Shape): Modifier = Modifier
    /** How far above the navigation bar the FAB floats (AppShell). */
    val fabBottom: Dp get() = 88.dp

    // ── progress ──
    val progressTrackHeight: Dp get() = 8.dp
    /** Decoration around a progress track (Threads.kt), e.g. an outline or bezel. */
    @Composable fun progressDecoration(): Modifier = Modifier

    // ── the hero (ThemedNoteCard / Dashboard) ──
    /** The hero surface, applied before the padding. [shape] is FinioShapes.note. */
    @Composable fun heroContainer(shape: Shape): Modifier = Modifier
    fun heroPadding(wide: Boolean): PaddingValues =
        if (wide) PaddingValues(32.dp, 28.dp, 32.dp, 36.dp) else PaddingValues(22.dp, 22.dp, 22.dp, 28.dp)
    val heroFigureScale: Float get() = 1f

    /**
     * Draws the hero figure itself. Return false to let the shared [com.slowatcoding.finio.ui.mudra.HeroFigure]
     * draw it (scaled by [heroFigureScale]).
     */
    @Composable
    fun HeroFigure(text: String, style: TextStyle, color: Color, whole: Boolean, modifier: Modifier): Boolean = false

    /** Drawn in the hero's scene slot, above the label (Mahal's sunburst). */
    @Composable fun HeroScene(elapsed: Float, spentPercent: Float, isOver: Boolean) {}

    /** Drawn in the hero under the budget bar and its caption (Parchi's barcode, Akhbaar's halftone pace). */
    @Composable fun HeroFooter(elapsed: Float, spentPercent: Float, isOver: Boolean, daysLeft: Int) {}

    /** True when [HeroFooter] stands in for the shared budget bar. */
    val replacesHeroBar: Boolean get() = false

    // ── chrome ──
    @Composable fun TabBar(active: FinioTab?, onSelect: (FinioTab) -> Unit, modifier: Modifier)
}

/** The kit for a round-two theme, or null for Mudra and the round-one themes. */
fun kitFor(visual: VisualTheme): ThemeKit? = when (visual) {
    VisualTheme.Parchi -> ParchiKit
    VisualTheme.Mahal -> MahalKit
    VisualTheme.Ganak -> GanakKit
    VisualTheme.Akhbaar -> AkhbaarKit
    VisualTheme.Khel -> KhelKit
    else -> null
}
