package com.slowatcoding.finio.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slowatcoding.finio.ui.icons.LucideIcons
import com.slowatcoding.finio.ui.theme.CssShadow
import com.slowatcoding.finio.ui.theme.FinioShapes
import com.slowatcoding.finio.ui.theme.FinioTheme
import com.slowatcoding.finio.ui.theme.FinioType
import com.slowatcoding.finio.ui.theme.TailwindEase
import com.slowatcoding.finio.ui.theme.cssInsetShadow
import com.slowatcoding.finio.ui.theme.cssShadow
import com.slowatcoding.finio.ui.theme.mix
import com.slowatcoding.finio.ui.theme.rememberReducedMotion
import java.math.BigInteger

/**
 * `formatInputAmount` (formatters.ts): groups the integer part the Indian way (1,22,999) and
 * keeps the decimal part exactly as typed. Empty is "0".
 */
fun formatInputAmount(raw: String): String {
    if (raw.isEmpty()) return "0"
    val dot = raw.indexOf('.')
    val intPart = if (dot >= 0) raw.substring(0, dot) else raw
    val digits = intPart.ifEmpty { "0" }.let { s -> s.toBigIntegerOrNull()?.toString() ?: "0" }
    val grouped = groupIndian(digits)
    return if (dot >= 0) "$grouped.${raw.substring(dot + 1)}" else grouped
}

private fun String.toBigIntegerOrNull(): BigInteger? =
    if (isNotEmpty() && all { it.isDigit() }) BigInteger(this) else null

private fun groupIndian(digits: String): String {
    if (digits.length <= 3) return digits
    val last3 = digits.takeLast(3)
    val rest = digits.dropLast(3)
    val head = rest.reversed().chunked(2).joinToString(",").reversed()
    return "$head,$last3"
}

/**
 * The amount keypad rule (number-pad.tsx `handlePress`): backspace drops a char; '.' only once
 * ("" → "0."); at most 2 decimal digits; at most 10 integer digits; a leading "0" is replaced.
 */
fun numberPadPress(value: String, key: Char): String {
    when (key) {
        '⌫' -> return value.dropLast(1)
        '.' -> return if ('.' in value) value else if (value.isEmpty()) "0." else "$value."
    }
    val dot = value.indexOf('.')
    val intPart = if (dot >= 0) value.substring(0, dot) else value
    val dec = if (dot >= 0) value.substring(dot + 1) else null
    if (dec != null && dec.length >= 2) return value
    if (intPart.length >= 10 && dec == null) return value
    return if (value.isEmpty() || value == "0") key.toString() else value + key
}

private val NumberPadKeys = listOf('7', '8', '9', '4', '5', '6', '1', '2', '3', '.', '0', '⌫')

/**
 * The amount entry (number-pad.tsx): a `grad-surface` display in the money face (30sp, 24sp past
 * 10 characters, muted when empty) above a 3×4 grid of 56dp strong-glass keys that scale to 95%
 * on press. Hardware digits, '.'/',' and Backspace work while the pad has focus.
 */
@Composable
fun NumberPad(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val colors = FinioTheme.colors
    val shadows = FinioTheme.shadows
    val current by rememberUpdatedState(value)
    val press = { key: Char -> onValueChange(numberPadPress(current, key)) }
    val display = formatInputAmount(value)

    Column(
        modifier.onKeyEvent { e ->
            if (e.type != KeyEventType.KeyDown) return@onKeyEvent false
            val c = e.utf16CodePoint.toChar()
            when {
                c in '0'..'9' -> press(c).let { true }
                c == '.' || c == ',' -> press('.').let { true }
                e.key == Key.Backspace -> press('⌫').let { true }
                else -> false
            }
        },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val shape = FinioShapes.md
        Box(
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 64.dp)
                .cssShadow(shape, shadows.card)
                .clip(shape)
                .background(FinioTheme.brushes.gradSurface)
                .border(1.dp, colors.glassBorder, shape)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            val big = display.length <= 10
            Text(
                display,
                style = FinioType.money.copy(fontSize = if (big) 30.sp else 24.sp, lineHeight = if (big) 36.sp else 32.sp),
                color = if (value.isEmpty()) colors.mutedForeground else colors.foreground,
                maxLines = 1,
                softWrap = false,
            )
        }
        KeyGrid(NumberPadKeys) { key ->
            PadKey(
                onClick = { press(key) },
                surface = PadSurface.Background,
                numberPadStyle = true,
                contentDescription = when (key) {
                    '⌫' -> "Delete last digit"
                    '.' -> "Decimal point"
                    else -> null
                },
            ) {
                when (key) {
                    '⌫' -> Icon(LucideIcons.Delete, null, Modifier.size(20.dp), tint = colors.mutedForeground)
                    '.' -> Text("·", style = FinioType.body.copy(fontSize = 24.sp, lineHeight = 24.sp), color = colors.mutedForeground)
                    else -> Text(key.toString(), style = PadDigitStyle)
                }
            }
        }
    }
}

/** CSS `ease-in-out`. */
private val CssEaseInOut = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)

private val PadDigitStyle = FinioType.rowValue.copy(fontSize = 20.sp, lineHeight = 28.sp)

@Composable
private fun <K> KeyGrid(keys: List<K>, key: @Composable BoxScope.(K) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        keys.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { k -> Box(Modifier.weight(1f)) { key(k) } }
            }
        }
    }
}

/** What a pad sits on (PinPad `surface`): glass keys read on the paper, solid ones on a card/dialog. */
enum class PadSurface { Background, Card }

/**
 * One 56dp pad key (`pinKeyClass`): `md` radius, strong glass with a white hairline and inset
 * highlight on the paper — or solid `--secondary` with a hairline on a card — muted while
 * pressed, scaling to 95%, 40% opacity when disabled. Exposed so a PinPad `leadingAction`
 * (the biometric key) matches the pad.
 */
@Composable
fun PadKey(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    surface: PadSurface = PadSurface.Background,
    enabled: Boolean = true,
    contentDescription: String? = null,
    numberPadStyle: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = FinioTheme.colors
    val shadows = FinioTheme.shadows
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.95f else 1f, tween(150, easing = TailwindEase), label = "key")
    val fill by animateColorAsState(
        when {
            pressed -> colors.muted
            surface == PadSurface.Card -> colors.secondary
            else -> colors.glassStrong
        },
        tween(150),
        label = "keyFill",
    )
    val shape = FinioShapes.md
    // NumberPad keys: `border-border … shadow-sm`; PIN keys on the paper: glass hairline + inset highlight.
    val border = when {
        surface == PadSurface.Card || numberPadStyle -> colors.border
        else -> colors.glassBorder
    }
    val outer = if (numberPadStyle) shadows.sm else emptyList()
    val inset = if (!numberPadStyle && surface == PadSurface.Background) {
        listOf(CssShadow(y = 1.dp, color = colors.glassHighlight, inset = true))
    } else {
        emptyList()
    }
    CompositionLocalProvider(
        LocalContentColor provides if (surface == PadSurface.Card) colors.secondaryForeground else colors.foreground,
    ) {
        Box(
            modifier
                .fillMaxWidth()
                .height(56.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .alpha(if (enabled) 1f else 0.4f)
                .cssShadow(shape, outer)
                .clip(shape)
                .background(fill)
                .cssInsetShadow(shape, inset)
                .border(1.dp, border, shape)
                .then(if (contentDescription != null) Modifier.semantics { this.contentDescription = contentDescription } else Modifier)
                .clickable(interaction, null, enabled = enabled, onClick = onClick),
            contentAlignment = Alignment.Center,
            content = content,
        )
    }
}

/**
 * The PIN dots (PinPad.tsx `PinDots`): 14dp, 12dp apart; filled is a solid foreground disc at
 * 110%, empty a 2dp ring at 50% muted — they differ by shape, not only colour. [error] turns the
 * rings magenta and shakes the row for 320ms (none under reduced motion).
 */
@Composable
fun PinDots(filled: Int, total: Int, modifier: Modifier = Modifier, error: Boolean = false) {
    val colors = FinioTheme.colors
    val reducedMotion = rememberReducedMotion()
    val shake = remember { Animatable(0f) }
    LaunchedEffect(error) {
        if (error && !reducedMotion) {
            shake.snapTo(0f)
            shake.animateTo(
                0f,
                keyframes {
                    durationMillis = 320
                    // `ease-in-out` applies to every keyframe interval, as in CSS.
                    0f at 0 using CssEaseInOut
                    -6f at 64 using CssEaseInOut
                    6f at 128 using CssEaseInOut
                    -6f at 192 using CssEaseInOut
                    6f at 256 using CssEaseInOut
                    0f at 320
                },
            )
        }
    }
    Row(
        modifier
            .graphicsLayer { translationX = shake.value * density }
            .clearAndSetSemantics { },
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
    ) {
        repeat(total) { i ->
            val isFilled = i < filled
            val scale by animateFloatAsState(if (isFilled) 1.1f else 1f, tween(150, easing = TailwindEase), label = "dot")
            Box(
                Modifier
                    .size(14.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .clip(FinioShapes.full)
                    .then(
                        if (isFilled) {
                            Modifier.background(colors.foreground)
                        } else {
                            Modifier.border(2.dp, if (error) colors.destructive else colors.mutedForeground.mix(0.5f), FinioShapes.full)
                        },
                    ),
            )
        }
    }
}

/**
 * The PIN keypad (PinPad.tsx) — deliberately separate from [NumberPad]: digits only, a fixed
 * [maxLength], [onComplete] fired once when it fills, and a [leadingAction] slot bottom-left
 * (the biometric key — build it with [PadKey]).
 */
@Composable
fun PinPad(
    value: String,
    onValueChange: (String) -> Unit,
    maxLength: Int,
    modifier: Modifier = Modifier,
    onComplete: ((String) -> Unit)? = null,
    enabled: Boolean = true,
    surface: PadSurface = PadSurface.Background,
    leadingAction: (@Composable () -> Unit)? = null,
) {
    val colors = FinioTheme.colors
    val current by rememberUpdatedState(value)
    val complete by rememberUpdatedState(onComplete)
    val press = { d: Char ->
        if (enabled && current.length < maxLength) onValueChange(current + d)
    }
    val backspace = { if (enabled) onValueChange(current.dropLast(1)) }

    // Fire in an effect, so a hardware digit and a tapped digit take exactly one path.
    LaunchedEffect(value, maxLength) {
        if (value.length == maxLength) complete?.invoke(value)
    }

    Column(
        modifier.onKeyEvent { e ->
            if (!enabled || e.type != KeyEventType.KeyDown) return@onKeyEvent false
            val c = e.utf16CodePoint.toChar()
            when {
                c in '0'..'9' -> press(c).let { true }
                e.key == Key.Backspace -> backspace().let { true }
                else -> false
            }
        },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        KeyGrid(listOf('7', '8', '9', '4', '5', '6', '1', '2', '3', 'L', '0', '⌫')) { key ->
            when (key) {
                'L' -> if (leadingAction != null) leadingAction() else Spacer(Modifier.height(56.dp))
                '⌫' -> PadKey(onClick = backspace, surface = surface, enabled = enabled, contentDescription = "Delete last digit") {
                    Icon(LucideIcons.Delete, null, Modifier.size(20.dp), tint = colors.mutedForeground)
                }
                else -> PadKey(onClick = { press(key) }, surface = surface, enabled = enabled) {
                    Text(key.toString(), style = PadDigitStyle)
                }
            }
        }
    }
}

