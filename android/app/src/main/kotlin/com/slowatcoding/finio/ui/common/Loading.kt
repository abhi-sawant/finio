package com.slowatcoding.finio.ui.common

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.slowatcoding.finio.ui.theme.FinioTheme

/**
 * The web's spinner (`h-8 w-8 animate-spin rounded-full border-4 border-primary
 * border-t-transparent`): a primary ring with its top quarter missing, one turn per second.
 */
@Composable
fun Spinner(modifier: Modifier = Modifier, size: Dp = 32.dp, stroke: Dp = 4.dp) {
    val color = FinioTheme.colors.primary
    val angle by rememberInfiniteTransition(label = "spin").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Restart),
        label = "angle",
    )
    Canvas(modifier.size(size).semantics { contentDescription = "Loading" }) {
        val w = stroke.toPx()
        rotate(angle) {
            // border-t-transparent: the ring minus the top 90° (centred on 12 o'clock).
            drawArc(
                color = color,
                startAngle = -45f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(w / 2, w / 2),
                size = androidx.compose.ui.geometry.Size(this.size.width - w, this.size.height - w),
                style = Stroke(w),
            )
        }
    }
}

/** App.tsx `PageLoader`: the spinner centred on the whole screen (hydration gate, lazy data). */
@Composable
fun PageLoader(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Spinner() }
}

/**
 * A spinner centred in a content-height block — for a section still computing. The block holds
 * its space immediately, but the spinner only appears if the wait outlasts [LOADER_DELAY_MS]: a
 * result that lands within a few frames never flashes a loader that is gone before it is read.
 */
@Composable
fun SectionLoader(modifier: Modifier = Modifier) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(LOADER_DELAY_MS)
        visible = true
    }
    Box(modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
        if (visible) Spinner() else Box(Modifier.size(32.dp))
    }
}

private const val LOADER_DELAY_MS = 250L
