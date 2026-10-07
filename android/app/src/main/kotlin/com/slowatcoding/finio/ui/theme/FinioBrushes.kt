package com.slowatcoding.finio.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor

/**
 * The `--grad-*`, `--paper`, `--coin`, `--register` and `--thread` gradients from index.css, per
 * mode, with CSS gradient geometry (see [CssLinearGradient]).
 *
 * [gradPrimary] stays deep indigo in **both** modes, because every caller puts white text on it.
 */
@Immutable
data class FinioBrushes(
    val gradPrimary: Brush,
    val gradPrimarySoft: Brush,
    val gradSuccess: Brush,
    val gradSuccessSoft: Brush,
    val gradDanger: Brush,
    val gradDangerSoft: Brush,
    val gradInfo: Brush,
    val gradWarning: Brush,
    /** The hero note, the number-pad display. */
    val gradSurface: Brush,
    /** Note paper — fixed behind every screen. */
    val paper: Brush,
    /** The lavender add-button coin (identical in both modes). */
    val coin: Brush,
    /** The budget "register" — an engraved 115deg hatch, 2px + 2.5px. Period is in px. */
    val register: (density: Float) -> Brush,
    /** The goal colour-shift thread, left to right. */
    val thread: Brush,
)

private fun two(angle: Float, a: Long, b: Long) =
    CssLinearGradient(angle, listOf(Stop(0f, Color(a)), Stop(1f, Color(b))))

/** `radial-gradient(circle at 30% 25%, #f8f5ff 0 8%, #d8ceff 30%, #a594f2 68%, #6c57d6)`. */
val CoinBrush: Brush = CssRadialGradient(
    0.3f,
    0.25f,
    listOf(
        Stop(0f, Color(0xFFF8F5FF)),
        Stop(0.08f, Color(0xFFF8F5FF)),
        Stop(0.30f, Color(0xFFD8CEFF)),
        Stop(0.68f, Color(0xFFA594F2)),
        Stop(1f, Color(0xFF6C57D6)),
    ),
)

private fun register(a: Long, b: Long): (Float) -> Brush = { density ->
    CssRepeatingLinearGradient(
        angleDegrees = 115f,
        periodPx = 4.5f * density,
        stops = listOf(
            Stop(0f, Color(a)),
            Stop(2f / 4.5f, Color(a)),
            Stop(2f / 4.5f, Color(b)),
            Stop(1f, Color(b)),
        ),
    )
}

val LightFinioBrushes = FinioBrushes(
    gradPrimary = two(135f, 0xFF7562EC, 0xFF4B36C7),
    gradPrimarySoft = two(135f, 0xFFF1EDFF, 0xFFE4DEFE),
    gradSuccess = two(135f, 0xFF17A777, 0xFF0B7A55),
    gradSuccessSoft = two(135f, 0xFFE6F7F0, 0xFFD3F0E4),
    gradDanger = two(135f, 0xFFE0418F, 0xFFB0125F),
    gradDangerSoft = two(135f, 0xFFFFEEF6, 0xFFFFDCEC),
    gradInfo = two(135f, 0xFF5AA2EC, 0xFF2F7FD1),
    gradWarning = two(135f, 0xFFE3AC2F, 0xFFB07B05),
    gradSurface = CssLinearGradient(
        135f,
        listOf(Stop(0f, Color(0xFFFEFDFF)), Stop(0.55f, Color(0xFFEFE9FF)), Stop(1f, Color(0xFFE2F3EC))),
    ),
    paper = CssLinearGradient(
        172f,
        listOf(Stop(0f, Color(0xFFF1EDFF)), Stop(0.5f, Color(0xFFEBF6F1)), Stop(1f, Color(0xFFFFF1E3))),
    ),
    coin = CoinBrush,
    register = register(0xFF3D2BB8, 0xFF6B58E8),
    thread = cssLinear(90f, listOf(hsl(150f, 0.70f, 0.38f), hsl(200f, 0.78f, 0.46f), hsl(255f, 0.70f, 0.56f))),
)

val DarkFinioBrushes = FinioBrushes(
    gradPrimary = two(135f, 0xFF7D6AF2, 0xFF4B36C7),
    gradPrimarySoft = two(135f, 0xFF2C2468, 0xFF241E57),
    gradSuccess = two(135f, 0xFF17A777, 0xFF0B7A55),
    gradSuccessSoft = two(135f, 0xFF123B33, 0xFF0F2F29),
    gradDanger = two(135f, 0xFFE0418F, 0xFFA8105A),
    gradDangerSoft = two(135f, 0xFF4A1534, 0xFF3A1029),
    gradInfo = two(135f, 0xFF4D8FDB, 0xFF2A6BB5),
    gradWarning = two(135f, 0xFFD9A227, 0xFF9C6C00),
    gradSurface = CssLinearGradient(
        135f,
        listOf(Stop(0f, Color(0xFF2C2468)), Stop(0.52f, Color(0xFF1E1A4C)), Stop(1f, Color(0xFF12302A))),
    ),
    paper = CssLinearGradient(
        172f,
        listOf(
            Stop(0f, Color(0xFF1B1546)),
            Stop(0.55f, Color(0xFF15113A)),
            Stop(0.85f, Color(0xFF171238)),
            Stop(1f, Color(0xFF22143A)),
        ),
    ),
    coin = CoinBrush,
    register = register(0xFF8F7DFF, 0xFFBDB1FF),
    thread = cssLinear(90f, listOf(hsl(150f, 0.65f, 0.52f), hsl(200f, 0.80f, 0.60f), hsl(255f, 0.85f, 0.75f))),
)

/** `.dark.amoled`: no paper gradient (and no maroon glow) — every pixel off unless it carries content. */
val AmoledFinioBrushes = DarkFinioBrushes.copy(
    gradPrimarySoft = two(135f, 0xFF1B1650, 0xFF120F38),
    gradSuccessSoft = two(135f, 0xFF0A2B24, 0xFF061A16),
    gradDangerSoft = two(135f, 0xFF2F0D20, 0xFF1D0714),
    gradSurface = CssLinearGradient(
        135f,
        listOf(Stop(0f, Color(0xFF1A1550)), Stop(0.52f, Color(0xFF0D0B26)), Stop(1f, Color(0xFF05120F))),
    ),
    paper = SolidColor(Color.Transparent),
)

val LocalFinioBrushes = staticCompositionLocalOf { LightFinioBrushes }
