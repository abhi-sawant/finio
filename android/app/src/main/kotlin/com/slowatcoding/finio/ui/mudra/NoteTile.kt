package com.slowatcoding.finio.ui.mudra

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slowatcoding.finio.core.model.AccountType
import com.slowatcoding.finio.ui.theme.CssShadow
import com.slowatcoding.finio.ui.theme.EaseOutExpo
import com.slowatcoding.finio.ui.theme.FinioShapes
import com.slowatcoding.finio.ui.theme.FinioTheme
import com.slowatcoding.finio.ui.theme.FinioType
import com.slowatcoding.finio.ui.theme.cssInsetShadow
import com.slowatcoding.finio.ui.theme.cssShadow
import kotlin.math.max

/**
 * An open account printed as its type's denomination (`.note-tile` on the Dashboard): 108dp
 * minimum height, `md` radius, 14dp padding (34dp at the end, clear of its thread), glass hairline
 * and inset highlight, Tile shadow, a small rosette bottom-right at 45%, a 4dp windowed thread in
 * the note's ink at 35%. Name (14sp 600, two lines max), [caption] (12sp at 80%), and [amount] in
 * the money face pinned to the bottom. Press scales to 97%.
 *
 * The web strip sizes tiles at 160dp (`w-40`); pass that (or a grid cell) through [modifier].
 */
@Composable
fun NoteTile(
    type: AccountType,
    name: String,
    amount: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    caption: String = type.label,
) {
    val note = noteStyle(type)
    val colors = FinioTheme.colors
    val shadows = FinioTheme.shadows
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, tween(200, easing = EaseOutExpo), label = "tile")
    val shape = FinioShapes.md
    val highlight = remember(colors.glassHighlight) { listOf(CssShadow(y = 1.dp, color = colors.glassHighlight, inset = true)) }
    val rosette = remember { guillochePaths(14, 5) }

    CompositionLocalProvider(LocalContentColor provides note.ink) {
        Layout(
            content = {
                Column {
                    Text(
                        name,
                        style = FinioType.body.copy(fontWeight = FontWeight.SemiBold, lineHeight = 17.5.sp),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        caption,
                        style = FinioType.caption,
                        color = note.ink.copy(alpha = note.ink.alpha * 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(amount, style = FinioType.money, maxLines = 1, softWrap = false)
            },
            modifier = modifier
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .cssShadow(shape, shadows.tile)
                .clip(shape)
                .background(note.background)
                .cssInsetShadow(shape, highlight)
                .border(1.dp, colors.glassBorder, shape)
                .drawWithCache {
                    val box = 150.dp.toPx()
                    val rosetteTopLeft = Offset(size.width + 64.dp.toPx() - box, size.height + 78.dp.toPx() - box)
                    val threadWidth = 4.dp.toPx()
                    val threadLeft = size.width - 14.dp.toPx() - threadWidth
                    onDrawBehind {
                        drawGuilloche(rosette, note.ink.copy(alpha = note.ink.alpha * 0.45f), rosetteTopLeft, box)
                        drawWindows(
                            SolidColor(note.ink),
                            Offset(threadLeft, 0f),
                            Size(threadWidth, size.height),
                            on = 9.dp.toPx(),
                            off = 6.dp.toPx(),
                            vertical = true,
                            alpha = 0.35f,
                        )
                    }
                }
                .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
                .padding(start = 14.dp, top = 14.dp, end = 34.dp, bottom = 14.dp),
        ) { measurables, constraints ->
            val loose = constraints.copy(minWidth = 0, minHeight = 0)
            val top = measurables[0].measure(loose)
            val money = measurables[1].measure(loose)
            val gap = 12.dp.roundToPx() // pt-3 on the pinned figure
            val minHeight = (108.dp - 28.dp).roundToPx()
            val width = max(constraints.minWidth, max(top.width, money.width)).coerceAtMost(constraints.maxWidth)
            val height = max(max(minHeight, constraints.minHeight), top.height + gap + money.height)
            layout(width, height) {
                top.place(0, 0)
                money.place(0, height - money.height)
            }
        }
    }
}

/**
 * The 30×20dp miniature banknote that leads every account row (`.note-chip`): the type's tint,
 * a 1dp inner white ring, a small drop, and a 2dp dashed thread near the right edge in the ink
 * at 40%. Archived accounts show it greyscale at 50%.
 */
@Composable
fun NoteChip(type: AccountType, modifier: Modifier = Modifier, archived: Boolean = false, width: Dp = 30.dp, height: Dp = 20.dp) {
    val base = noteStyle(type)
    val note = if (archived) base.grayscale() else base
    val shadows = FinioTheme.shadows
    val shape = FinioShapes.chip
    Spacer(
        modifier
            .graphicsLayer { alpha = if (archived) 0.5f else 1f }
            .size(width, height)
            .cssShadow(shape, shadows.noteChip)
            .clip(shape)
            .background(note.background)
            .cssInsetShadow(shape, shadows.noteChip)
            .drawWithCache {
                val w = 2.dp.toPx()
                val left = size.width - 8.dp.toPx() - w
                onDrawBehind {
                    drawWindows(
                        SolidColor(note.ink),
                        Offset(left, 0f),
                        Size(w, size.height),
                        on = 3.dp.toPx(),
                        off = 2.dp.toPx(),
                        vertical = true,
                        alpha = 0.4f,
                    )
                }
            },
    )
}
