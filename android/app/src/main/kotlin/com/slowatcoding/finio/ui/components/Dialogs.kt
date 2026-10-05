package com.slowatcoding.finio.ui.components

import android.os.Build
import android.view.WindowManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.slowatcoding.finio.ui.icons.LucideIcons
import com.slowatcoding.finio.ui.theme.FinioShapes
import com.slowatcoding.finio.ui.theme.FinioTheme
import com.slowatcoding.finio.ui.theme.FinioType
import com.slowatcoding.finio.ui.theme.cssShadow
import com.slowatcoding.finio.ui.theme.mix
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.math.max

/** Tailwind's `sm` breakpoint — dialogs lay their header and footer out in a row from here. */
@Composable
private fun isSmUp(): Boolean = LocalConfiguration.current.screenWidthDp >= 640

/**
 * The modal window every Mudra dialog sits in: full screen, edge to edge, the platform dim
 * replaced by the `--scrim` token (indigo 24% / near-black 60% — it always dims) and, on API 31+,
 * a 4dp blur behind (`backdrop-blur-xs`). The panel fades and zooms in from 95% over 100ms.
 */
@Composable
internal fun ModalWindow(
    onDismissRequest: () -> Unit,
    dismissOnScrimClick: Boolean = true,
    panel: @Composable (Modifier) -> Unit,
) {
    val colors = FinioTheme.colors
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        val blur = with(LocalDensity.current) { 4.dp.roundToPx() }
        SideEffect {
            window?.setDimAmount(0f)
            if (window != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                window.attributes = window.attributes.apply { blurBehindRadius = blur }
            }
        }
        val enter = remember { Animatable(0f) }
        LaunchedEffect(Unit) { enter.animateTo(1f, tween(100)) }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = enter.value }
                    .background(colors.scrim)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = dismissOnScrimClick,
                        onClick = onDismissRequest,
                    ),
            )
            Box(
                Modifier
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .imePadding()
                    .padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                panel(
                    Modifier.graphicsLayer {
                        alpha = enter.value
                        val s = 0.95f + 0.05f * enter.value
                        scaleX = s
                        scaleY = s
                    },
                )
            }
        }
    }
}

/**
 * The opaque dialog panel (dialog.tsx `DialogContent`): `--popover` fill, 1dp `--border` ring,
 * 22dp radius, Float shadow, 16dp padding, children 16dp apart, scrolling when taller than the
 * screen.
 */
@Composable
private fun DialogPanel(
    modifier: Modifier,
    maxWidth: Dp,
    title: String?,
    footer: (@Composable () -> Unit)?,
    closeButton: (() -> Unit)?,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = FinioTheme.colors
    val shape = FinioShapes.lg
    CompositionLocalProvider(LocalContentColor provides colors.popoverForeground) {
        Box(
            modifier
                .widthIn(max = maxWidth)
                .fillMaxWidth()
                .semantics { if (title != null) paneTitle = title }
                .cssShadow(shape, FinioTheme.shadows.float)
                .clip(shape)
                .background(colors.popover)
                .border(1.dp, colors.border, shape),
        ) {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    content = content,
                )
                if (footer != null) DialogFooter(footer)
            }
            if (closeButton != null) {
                FinioIconButton(
                    icon = LucideIcons.X,
                    contentDescription = "Close",
                    onClick = closeButton,
                    modifier = Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 8.dp),
                    size = ButtonSize.IconSm,
                )
            }
        }
    }
}

/**
 * The dialog footer strip (`DialogFooter`): muted at 50%, a hairline on top, 16dp padding, bled to
 * the panel edges. Buttons stack full-width in reverse order on phones (the first one you declare
 * — usually Cancel — ends up at the bottom) and sit in a right-aligned row from 640dp.
 */
@Composable
private fun DialogFooter(content: @Composable () -> Unit) {
    val colors = FinioTheme.colors
    val row = isSmUp()
    Column {
        FinioDivider()
        Box(Modifier.fillMaxWidth().background(colors.muted.mix(0.5f)).padding(16.dp)) {
            Layout(content = content) { measurables, constraints ->
                val gap = 8.dp.roundToPx()
                if (row) {
                    val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0)) }
                    val height = placeables.maxOfOrNull { it.height } ?: 0
                    layout(constraints.maxWidth, height) {
                        var x = constraints.maxWidth - placeables.sumOf { it.width } - gap * (placeables.size - 1).coerceAtLeast(0)
                        placeables.forEach {
                            it.place(x, (height - it.height) / 2)
                            x += it.width + gap
                        }
                    }
                } else {
                    val full = constraints.copy(minWidth = constraints.maxWidth)
                    val placeables = measurables.map { it.measure(full) }
                    val height = placeables.sumOf { it.height } + gap * max(0, placeables.size - 1)
                    layout(constraints.maxWidth, height) {
                        var y = 0
                        placeables.asReversed().forEach {
                            it.place(0, y)
                            y += it.height + gap
                        }
                    }
                }
            }
        }
    }
}

/** `DialogHeader` / `AlertDialogHeader`: Unbounded title over a muted 14sp description, 8dp apart. */
@Composable
private fun DialogHeading(title: String?, description: String?, centered: Boolean, gap: Dp) {
    if (title == null && description == null) return
    val colors = FinioTheme.colors
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(gap),
    ) {
        if (title != null) {
            Text(title, style = FinioType.dialogTitle, color = colors.popoverForeground, textAlign = if (centered) TextAlign.Center else TextAlign.Start)
        }
        if (description != null) {
            Text(description, style = FinioType.body, color = colors.mutedForeground, textAlign = if (centered) TextAlign.Center else TextAlign.Start)
        }
    }
}

/**
 * A dialog (dialog.tsx): centred, at most 384dp wide (and 16dp clear of the screen edges), with an
 * optional title/description header, a ghost close button top-right, the body, and an optional
 * [footer] of buttons (see the footer layout rules on phones vs. wider screens).
 */
@Composable
fun FinioDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    description: String? = null,
    showCloseButton: Boolean = true,
    dismissOnScrimClick: Boolean = true,
    footer: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    ModalWindow(onDismissRequest, dismissOnScrimClick) { anim ->
        DialogPanel(
            modifier = modifier.then(anim),
            maxWidth = 384.dp,
            title = title,
            footer = footer,
            closeButton = if (showCloseButton) onDismissRequest else null,
        ) {
            DialogHeading(title, description, centered = false, gap = 8.dp)
            content()
        }
    }
}

/**
 * An alert dialog (alert-dialog.tsx): 320dp wide on phones (384dp from 640dp), header centred on
 * phones and left-aligned wider, no close button, a Cancel + action footer. The action is
 * destructive-tinted unless [destructive] is false.
 */
@Composable
fun FinioAlertDialog(
    title: String,
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    confirmLabel: String = "Confirm",
    cancelLabel: String = "Cancel",
    destructive: Boolean = true,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    val sm = isSmUp()
    ModalWindow(onDismissRequest) { anim ->
        DialogPanel(
            modifier = modifier.then(anim),
            maxWidth = if (sm) 384.dp else 320.dp,
            title = title,
            footer = {
                FinioButton(cancelLabel, onClick = onDismissRequest, variant = ButtonVariant.Outline)
                FinioButton(
                    confirmLabel,
                    onClick = onConfirm,
                    variant = if (destructive) ButtonVariant.Destructive else ButtonVariant.Default,
                )
            },
            closeButton = null,
        ) {
            DialogHeading(title, description, centered = !sm, gap = 6.dp)
            content()
        }
    }
}

// ───────────── confirm(): the promise-based native confirm() replacement (confirm.tsx) ─────────────

/** use-confirm.ts `ConfirmOptions`. */
data class ConfirmOptions(
    val title: String,
    val description: String? = null,
    val confirmLabel: String = "Confirm",
    val cancelLabel: String = "Cancel",
    /** Styles the confirming button as destructive. Defaults to true — nearly every caller deletes. */
    val destructive: Boolean = true,
)

private class ConfirmRequest(val options: ConfirmOptions, private val continuation: CancellableContinuation<Boolean>) {
    fun settle(value: Boolean) {
        if (continuation.isActive) continuation.resume(value)
    }
}

/**
 * One shared confirmation dialog for the whole app. `confirm()` suspends until the user answers
 * and returns true only for the confirming action; dismissing (back, scrim) is a cancel, and a
 * second request while one is pending resolves the first as false rather than stranding it.
 */
@Stable
class ConfirmState {
    private var request by mutableStateOf<ConfirmRequest?>(null)

    suspend fun confirm(options: ConfirmOptions): Boolean {
        request?.settle(false)
        return suspendCancellableCoroutine { cont ->
            val next = ConfirmRequest(options, cont)
            request = next
            cont.invokeOnCancellation { if (request === next) request = null }
        }
    }

    suspend fun confirm(
        title: String,
        description: String? = null,
        confirmLabel: String = "Confirm",
        cancelLabel: String = "Cancel",
        destructive: Boolean = true,
    ): Boolean = confirm(ConfirmOptions(title, description, confirmLabel, cancelLabel, destructive))

    @Composable
    internal fun Render() {
        val current = request ?: return
        val o = current.options
        val settle = { value: Boolean ->
            request = null
            current.settle(value)
        }
        FinioAlertDialog(
            title = o.title,
            description = o.description,
            confirmLabel = o.confirmLabel,
            cancelLabel = o.cancelLabel,
            destructive = o.destructive,
            onConfirm = { settle(true) },
            onDismissRequest = { settle(false) },
        )
    }
}

val LocalConfirm = staticCompositionLocalOf<ConfirmState> {
    error("LocalConfirm used outside a ConfirmHost")
}

/** Provides [LocalConfirm] to [content] and renders the shared confirmation dialog. */
@Composable
fun ConfirmHost(state: ConfirmState = remember { ConfirmState() }, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalConfirm provides state) {
        content()
        state.Render()
    }
}

