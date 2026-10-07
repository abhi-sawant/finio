package com.slowatcoding.finio.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.slowatcoding.finio.ui.icons.LucideIcons
import com.slowatcoding.finio.ui.theme.CssShadow
import com.slowatcoding.finio.ui.theme.FinioShapes
import com.slowatcoding.finio.ui.theme.FinioTheme
import com.slowatcoding.finio.ui.theme.FinioType
import com.slowatcoding.finio.ui.theme.cssInsetShadow
import com.slowatcoding.finio.ui.theme.cssShadow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.abs

enum class ToastType { Success, Error, Warning, Info, Message }

/** A toast's one action — the Undo pill. */
@Immutable
data class ToastAction(val label: String, val onClick: () -> Unit)

@Immutable
data class ToastData(
    val id: Long,
    val type: ToastType,
    val message: String,
    val description: String? = null,
    val action: ToastAction? = null,
    val durationMillis: Long = ToastController.DefaultDuration,
)

/**
 * Sonner's API, in-process: `toast.success("Saved")`, `toast.error(…)`, `toast.info(…)`, with an
 * optional description and action. Safe to call from anywhere on the main thread (ViewModels
 * included); [FinioToastHost] renders the queue.
 */
@Stable
class ToastController {
    internal val toasts = mutableStateListOf<ToastData>()
    private val ids = AtomicLong(0)

    fun show(
        type: ToastType,
        message: String,
        description: String? = null,
        action: ToastAction? = null,
        durationMillis: Long = DefaultDuration,
    ): Long {
        val id = ids.incrementAndGet()
        toasts.add(ToastData(id, type, message, description, action, durationMillis))
        return id
    }

    fun success(message: String, description: String? = null, action: ToastAction? = null) =
        show(ToastType.Success, message, description, action)

    fun error(message: String, description: String? = null, action: ToastAction? = null) =
        show(ToastType.Error, message, description, action)

    fun warning(message: String, description: String? = null, action: ToastAction? = null) =
        show(ToastType.Warning, message, description, action)

    fun info(message: String, description: String? = null, action: ToastAction? = null) =
        show(ToastType.Info, message, description, action)

    fun message(message: String, description: String? = null, action: ToastAction? = null) =
        show(ToastType.Message, message, description, action)

    fun dismiss(id: Long) {
        toasts.removeAll { it.id == id }
    }

    fun dismissAll() = toasts.clear()

    companion object {
        /** Sonner's default `duration`. */
        const val DefaultDuration = 4000L
        /** Sonner's default `visibleToasts`. */
        const val VisibleToasts = 3
    }
}

/** The app-wide toaster, like Sonner's module-level `toast`. */
val toast = ToastController()

/**
 * The Toaster (App.tsx `<Toaster position="top-center" closeButton>` + the index.css overrides):
 * top-centre, 12dp below the status bar, 356dp wide (16dp clear of the edges on phones). Each toast
 * is strong glass with a white hairline, the Float shadow and an inset highlight; the type shows
 * only in the icon colour (success positive, error destructive, warning amber, info lavender), the
 * action is a 28dp lavender-gradient pill, and a small close button sits on the top-left corner.
 *
 * Sonner's collapsed stack: the newest toast in front, up to two older ones peeking 14dp below
 * it at 95%/90% scale. Swipe a toast up or sideways to dismiss it.
 *
 * Compromise: no backdrop blur (translucency only). Place it last, above dialogs: the host draws
 * in the activity window, so toasts shown while a Dialog is open render beneath that dialog's
 * window.
 */
@Composable
fun FinioToastHost(modifier: Modifier = Modifier, controller: ToastController = toast) {
    val visible = controller.toasts.takeLast(ToastController.VisibleToasts).asReversed()
    var frontHeight by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    Box(
        modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(top = 12.dp, start = 16.dp, end = 16.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        visible.forEachIndexed { index, data ->
            key(data.id) {
                ToastItem(
                    data = data,
                    index = index,
                    frontHeightPx = frontHeight,
                    onFrontMeasured = { if (index == 0) frontHeight = it },
                    onDismiss = { controller.dismiss(data.id) },
                    modifier = Modifier
                        .zIndex((ToastController.VisibleToasts - index).toFloat())
                        .widthIn(max = 356.dp)
                        .fillMaxWidth()
                        .then(
                            if (index > 0 && frontHeight > 0) Modifier.height(with(density) { frontHeight.toDp() }) else Modifier,
                        ),
                )
            }
        }
    }
}

@Composable
private fun ToastItem(
    data: ToastData,
    index: Int,
    frontHeightPx: Int,
    onFrontMeasured: (Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier,
) {
    val colors = FinioTheme.colors
    val scope = rememberCoroutineScope()
    val enter = remember { Animatable(0f) }
    val dragX = remember { Animatable(0f) }
    val dragY = remember { Animatable(0f) }
    val lift = remember { Animatable(0f) }

    LaunchedEffect(Unit) { enter.animateTo(1f, tween(400)) }
    LaunchedEffect(index) { lift.animateTo(index.toFloat(), tween(400)) }
    LaunchedEffect(data.id) {
        delay(data.durationMillis)
        enter.animateTo(0f, tween(200))
        onDismiss()
    }

    val front = index == 0
    val shape = FinioShapes.md
    val highlight = remember(colors.glassHighlight) { listOf(CssShadow(y = 1.dp, color = colors.glassHighlight, inset = true)) }

    Box(
        modifier
            .graphicsLayer {
                val stackOffset = lift.value * 14.dp.toPx()
                translationY = stackOffset + dragY.value - (1f - enter.value) * (frontHeightPx.takeIf { it > 0 } ?: 64.dp.roundToPx())
                translationX = dragX.value
                val s = 1f - 0.05f * lift.value
                scaleX = s
                scaleY = s
                alpha = enter.value * (1f - (abs(dragX.value) / size.width.coerceAtLeast(1f)).coerceIn(0f, 1f))
            }
            .pointerInput(data.id) {
                detectDragGestures(
                    onDragEnd = {
                        if (abs(dragX.value) > 45.dp.toPx() || dragY.value < -45.dp.toPx()) {
                            onDismiss()
                        } else {
                            scope.launch { dragX.animateTo(0f) }
                            scope.launch { dragY.animateTo(0f) }
                        }
                    },
                    onDragCancel = {
                        scope.launch { dragX.animateTo(0f) }
                        scope.launch { dragY.animateTo(0f) }
                    },
                ) { change, amount ->
                    change.consume()
                    scope.launch {
                        dragX.snapTo(dragX.value + amount.x)
                        // Top toasts only swipe upward; downward drags resist.
                        dragY.snapTo((dragY.value + amount.y).coerceAtMost(0f) + if (amount.y > 0) amount.y * 0.1f else 0f)
                    }
                }
            }
            .onSizeChanged { if (front) onFrontMeasured(it.height) },
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .then(if (front) Modifier else Modifier.fillMaxHeight())
                .cssShadow(shape, FinioTheme.shadows.float)
                .clip(shape)
                .background(colors.glassStrong)
                .cssInsetShadow(shape, highlight)
                .border(1.dp, colors.glassBorder, shape)
                .semantics { liveRegion = LiveRegionMode.Polite }
                .padding(16.dp)
                .graphicsLayer { alpha = if (front) 1f else 0f },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val icon = toastIcon(data.type)
            if (icon != null) {
                Icon(
                    icon.first,
                    contentDescription = null,
                    modifier = Modifier.offset(x = (-3).dp).size(20.dp),
                    tint = icon.second,
                )
                Spacer(Modifier.width(4.dp + 6.dp - 3.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(data.message, style = FinioType.toast, color = colors.foreground)
                if (data.description != null) {
                    Text(
                        data.description,
                        style = FinioType.toast.copy(fontWeight = FontWeight.Normal, lineHeight = 18.2.sp),
                        color = colors.mutedForeground,
                    )
                }
            }
            if (data.action != null) {
                Spacer(Modifier.width(8.dp))
                ToastActionPill(data.action) {
                    data.action.onClick()
                    onDismiss()
                }
            }
        }
        // `closeButton`: a 20dp popover-coloured disc straddling the top-left corner.
        if (front) {
            Box(
                Modifier
                    .offset(x = (-7).dp, y = (-7).dp)
                    .size(20.dp)
                    .clip(FinioShapes.full)
                    .background(colors.popover)
                    .border(1.dp, colors.border, FinioShapes.full)
                    .clickable(onClickLabel = "Close toast", onClick = onDismiss),
                contentAlignment = Alignment.Center,
            ) {
                Icon(LucideIcons.X, contentDescription = "Close", Modifier.size(12.dp), tint = colors.mutedForeground)
            }
        }
    }
}

@Composable
private fun toastIcon(type: ToastType): Pair<ImageVector, Color>? {
    val colors = FinioTheme.colors
    return when (type) {
        ToastType.Success -> LucideIcons.CircleCheck to colors.positive
        ToastType.Error -> LucideIcons.CircleX to colors.destructive
        ToastType.Warning -> LucideIcons.TriangleAlert to colors.warning
        ToastType.Info -> LucideIcons.Info to colors.primary
        ToastType.Message -> null
    }
}

/** The Undo/action button: 28dp tall, 12dp sides, full round, lavender gradient, white 600 text. */
@Composable
private fun ToastActionPill(action: ToastAction, onClick: () -> Unit) {
    Box(
        Modifier
            .height(28.dp)
            .clip(FinioShapes.full)
            .background(FinioTheme.brushes.gradPrimary)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(action.label, style = FinioType.label.copy(fontWeight = FontWeight.SemiBold), color = Color.White, maxLines = 1)
    }
}

