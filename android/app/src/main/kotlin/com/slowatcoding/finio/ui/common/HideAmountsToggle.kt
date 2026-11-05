package com.slowatcoding.finio.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.slowatcoding.finio.di.appContainer
import com.slowatcoding.finio.ui.components.HeaderIconButton
import com.slowatcoding.finio.ui.icons.LucideIcons

/**
 * Port of web/src/components/HideAmountsToggle.tsx — the header eye button that masks every
 * rendered amount (`settings.hideAmounts`). Put it in the header of every data-bearing page.
 */
@Composable
fun HideAmountsToggle(modifier: Modifier = Modifier) {
    val state by collectFinanceState()
    val hidden = state.settings.hideAmounts
    HeaderIconButton(
        icon = if (hidden) LucideIcons.EyeOff else LucideIcons.Eye,
        contentDescription = if (hidden) "Show amounts" else "Hide amounts",
        onClick = appContainer()::requestToggleHideAmounts,
        modifier = modifier,
        pressed = hidden,
    )
}
