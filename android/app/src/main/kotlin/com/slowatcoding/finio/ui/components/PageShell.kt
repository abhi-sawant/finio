package com.slowatcoding.finio.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.layout.layout
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.slowatcoding.finio.ui.mudra.PaperBackground
import com.slowatcoding.finio.ui.theme.FinioTheme
import com.slowatcoding.finio.ui.theme.FinioType

/** Tailwind `max-w-5xl` — the one centred content column. */
val ContentMaxWidth = 1024.dp

/** True from Tailwind's `lg` breakpoint (1024dp), where Mudra switches to its desktop metrics. */
@Composable
fun isWideLayout(): Boolean = LocalConfiguration.current.screenWidthDp >= 1024

/** The header frosts over once content has scrolled more than 4px beneath it. */
@Composable
fun ScrollState.headerScrolled(): Boolean {
    val threshold = with(LocalDensity.current) { 4.dp.toPx() }
    val scrolled by remember(this, threshold) { derivedStateOf { value > threshold } }
    return scrolled
}

@Composable
fun LazyListState.headerScrolled(): Boolean {
    val threshold = with(LocalDensity.current) { 4.dp.roundToPx() }
    val scrolled by remember(this, threshold) {
        derivedStateOf { firstVisibleItemIndex > 0 || firstVisibleItemScrollOffset > threshold }
    }
    return scrolled
}

/**
 * The page header (header.tsx): transparent on the paper at rest, glass-chrome with a white
 * hairline once [scrolled]. Draws under the status bar (edge to edge) and pads its row below it;
 * the row is centred at [ContentMaxWidth], 12dp/32dp sides, 12dp tall padding, space-between.
 *
 * Android has no backdrop blur behind arbitrary content, so the scrolled header is translucency
 * only (`--glass-strong`).
 */
@Composable
fun FinioHeader(
    scrolled: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val colors = FinioTheme.colors
    val fill by animateColorAsState(if (scrolled) colors.chromeGlass else Color.Transparent, tween(200), label = "header")
    val hairline by animateColorAsState(if (scrolled) colors.glassBorder else Color.Transparent, tween(200), label = "hairline")
    val wide = isWideLayout()
    Box(
        modifier
            .fillMaxWidth()
            .background(fill)
            .drawBehind {
                val y = size.height - 0.5.dp.toPx()
                drawLine(hairline, Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
            }
            .windowInsetsPadding(WindowInsets.statusBars),
        contentAlignment = Alignment.TopCenter,
    ) {
        CompositionLocalProvider(LocalContentColor provides colors.foreground) {
            Row(
                Modifier
                    .widthIn(max = ContentMaxWidth)
                    .fillMaxWidth()
                    .padding(horizontal = if (wide) 32.dp else 12.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                content = content,
            )
        }
    }
}

/** A tab page's `h1` (`text-2xl font-bold tracking-tight`, FamiljenGrotesk). */
@Composable
fun PageTitle(text: String, modifier: Modifier = Modifier, style: TextStyle = FinioType.pageTitle) {
    Text(text, modifier, style = style, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

/** A sub-page `h1` between a back button and an action (`text-base font-semibold`, FamiljenGrotesk). */
@Composable
fun ScreenTitle(text: String, modifier: Modifier = Modifier) = PageTitle(text, modifier, FinioType.screenTitle)

/**
 * The page body (main.tsx): centred at [ContentMaxWidth], 12dp sides (32dp wide), 8dp top,
 * 160dp bottom on mobile so content clears the tab bar and coin (32dp wide), children stacked
 * 16dp apart (24dp wide).
 */
@Composable
fun FinioMain(
    modifier: Modifier = Modifier,
    bottomPadding: Dp? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val wide = isWideLayout()
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier
                .widthIn(max = ContentMaxWidth)
                .fillMaxWidth()
                .padding(mainPadding(wide, bottomPadding)),
            verticalArrangement = Arrangement.spacedBy(if (wide) 24.dp else 16.dp),
            content = content,
        )
    }
}

/** [FinioMain]'s padding, for a LazyColumn's `contentPadding`. */
fun mainPadding(wide: Boolean, bottom: Dp? = null) = PaddingValues(
    start = if (wide) 32.dp else 12.dp,
    end = if (wide) 32.dp else 12.dp,
    top = 8.dp,
    bottom = bottom ?: if (wide) 32.dp else 160.dp,
)

/**
 * Shrinks the scroll viewport by what floats over it — the header at the top, a sticky footer at
 * the bottom — before deferring to [base], so bring-into-view (focus, keyboard) lands a child in
 * the visible band between them rather than under either.
 */
@OptIn(ExperimentalFoundationApi::class)
private class UnobscuredBringIntoViewSpec(
    private val base: BringIntoViewSpec,
    private val obscured: () -> Pair<Float, Float>,
) : BringIntoViewSpec {
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
        val (topPx, bottomPx) = obscured()
        val top = topPx.coerceIn(0f, containerSize)
        val bottom = bottomPx.coerceIn(0f, containerSize - top)
        return base.calculateScrollDistance(offset - top, size, containerSize - top - bottom)
    }
}

/**
 * A whole page: the sticky [FinioHeader] floating over a scrolling [FinioMain], wired so the
 * header frosts as soon as content scrolls under it. Put it inside a PaperBackground.
 * [bottomObscured] is the height of anything the caller floats over the bottom of the page (a
 * sticky submit bar), so a focused field is never brought into view behind it.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FinioScreen(
    header: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    bottomPadding: Dp? = null,
    bottomObscured: Dp = 0.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    var headerHeight by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val innerSpec = LocalBringIntoViewSpec.current
    val bottomPx by rememberUpdatedState(with(density) { bottomObscured.toPx() })
    val headerSpec = remember(innerSpec) {
        UnobscuredBringIntoViewSpec(innerSpec) { headerHeight.toFloat() to bottomPx }
    }
    // The header is measured *before* the body in this same pass and its height lands in
    // [headerHeight] before the body's spacer is measured. A Box + onSizeChanged would learn the
    // height a frame late, so every page opened with its body jammed under the header and then
    // visibly jumped down.
    // The page paints its own (identical) paper: while a navigation swaps, the outgoing page is
    // still composed for a frame and would otherwise show through this transparent one.
    PaperBackground(modifier) {
    Layout(
        modifier = Modifier.fillMaxSize(),
        content = {
            ScreenBody(headerSpec, innerSpec, scrollState, bottomPadding, content) { headerHeight }
            FinioHeader(
                scrolled = scrollState.headerScrolled(),
                content = header,
            )
        },
    ) { measurables, constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val header = measurables[1].measure(loose)
        headerHeight = header.height
        val body = measurables[0].measure(constraints)
        layout(constraints.maxWidth, constraints.maxHeight) {
            body.place(0, 0)
            header.place(0, 0)
        }
    }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ScreenBody(
    headerSpec: BringIntoViewSpec,
    innerSpec: BringIntoViewSpec,
    scrollState: ScrollState,
    bottomPadding: Dp?,
    content: @Composable ColumnScope.() -> Unit,
    headerHeight: () -> Int,
) {
    Box(Modifier.fillMaxSize()) {
        // The header floats over the scroll viewport, so a focused field brought "into view" at
        // the viewport top would sit under it: the page's own scroller treats the band beneath
        // the header as off-screen. Nested scrollers get the default spec back.
        CompositionLocalProvider(LocalBringIntoViewSpec provides headerSpec) {
            // imePadding: edge to edge, adjustResize no longer shrinks the window, so the scroll
            // viewport must end at the keyboard or a focused field is "in view" behind it.
            Column(Modifier.fillMaxSize().imePadding().verticalScroll(scrollState)) {
                CompositionLocalProvider(LocalBringIntoViewSpec provides innerSpec) {
                    Spacer(
                        Modifier.layout { measurable, constraints ->
                            val placeable = measurable.measure(constraints.copy(minHeight = headerHeight(), maxHeight = headerHeight()))
                            layout(placeable.width, placeable.height) {}
                        },
                    )
                    FinioMain(bottomPadding = bottomPadding, content = content)
                }
            }
        }
    }
}

/**
 * The one icon button every page header uses (header-icon-button.tsx): a 40dp outline circle
 * with an 18dp icon. [tone] only changes the icon colour; a [pressed] toggle (active filters,
 * hidden amounts) fills with the lavender gradient and a white icon.
 */
@Composable
fun HeaderIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tone: HeaderIconTone = HeaderIconTone.Neutral,
    pressed: Boolean = false,
    enabled: Boolean = true,
) {
    val colors = FinioTheme.colors
    FinioButton(
        onClick = onClick,
        modifier = modifier.size(40.dp),
        variant = if (pressed) ButtonVariant.Default else ButtonVariant.Outline,
        size = ButtonSize.Icon,
        enabled = enabled,
        contentPadding = PaddingValues(0.dp),
    ) {
        androidx.compose.material3.Icon(
            icon,
            contentDescription,
            Modifier.size(18.dp),
            tint = when {
                pressed -> Color.White
                tone == HeaderIconTone.Primary -> colors.primary
                tone == HeaderIconTone.Destructive -> colors.destructive
                else -> colors.foreground
            },
        )
    }
}

enum class HeaderIconTone { Neutral, Primary, Destructive }

/** Keeps a title centred on pages with a back button but no trailing action. */
@Composable
fun HeaderIconSpacer() = Spacer(Modifier.size(40.dp))
