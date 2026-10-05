package com.slowatcoding.finio.ui.mudra

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.slowatcoding.finio.ui.icons.LucideIcons
import com.slowatcoding.finio.ui.theme.FinioShapes
import com.slowatcoding.finio.ui.theme.FinioTheme
import com.slowatcoding.finio.ui.theme.TailwindEase
import com.slowatcoding.finio.ui.theme.cssInsetShadow
import com.slowatcoding.finio.ui.theme.cssShadow

/** `bg-coin`'s fixed indigo ink — the coin is the same in both modes. */
val CoinInk = Color(0xFF1D1747)

/**
 * The single-hue lavender coin (`bg-coin`): radial gradient lit from the top-left, indigo ink,
 * inset top light and a lavender drop. 56dp on the mobile FAB, 36dp as the sidebar's mark.
 */
@Composable
fun Coin(modifier: Modifier = Modifier, size: Dp = 56.dp, content: @Composable BoxScope.() -> Unit = {}) {
    val shadows = FinioTheme.shadows
    CompositionLocalProvider(LocalContentColor provides CoinInk) {
        Box(
            modifier
                .size(size)
                .cssShadow(FinioShapes.full, shadows.coin)
                .clip(FinioShapes.full)
                .background(FinioTheme.brushes.coin)
                .cssInsetShadow(FinioShapes.full, shadows.coin),
            contentAlignment = Alignment.Center,
            content = content,
        )
    }
}

/**
 * The add-transaction FAB: a 56dp [Coin] with a 26dp `+` at stroke 2.4, pressing to 95%.
 * [onLongClick] opens templates. Placement (16dp from the right, 5.5rem above the safe-area
 * inset) belongs to the layout shell.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CoinFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    contentDescription: String = "Add transaction. Long-press for templates.",
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.95f else 1f, tween(150, easing = TailwindEase), label = "coin")
    Coin(
        modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClickLabel = null,
                onLongClick = onLongClick,
                onClick = onClick,
            ),
    ) {
        Icon(
            LucideIcons.byKebab("plus", strokeWidth = 2.4f)!!,
            contentDescription = contentDescription,
            modifier = Modifier.size(26.dp),
        )
    }
}
