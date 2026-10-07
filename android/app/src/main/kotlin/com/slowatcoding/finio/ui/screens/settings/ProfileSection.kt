package com.slowatcoding.finio.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.slowatcoding.finio.core.format.formatOrdinal
import com.slowatcoding.finio.core.js.nowInstant
import com.slowatcoding.finio.core.model.Theme
import com.slowatcoding.finio.core.period.MAX_MONTH_START_DAY
import com.slowatcoding.finio.core.period.MIN_MONTH_START_DAY
import com.slowatcoding.finio.core.period.PeriodType
import com.slowatcoding.finio.core.period.normalizeMonthStartDay
import com.slowatcoding.finio.core.period.periodLabel
import com.slowatcoding.finio.core.period.periodRange
import com.slowatcoding.finio.core.util.MAX_NAME_LENGTH
import com.slowatcoding.finio.core.util.cleanText
import com.slowatcoding.finio.core.util.stripLeading
import com.slowatcoding.finio.di.appContainer
import com.slowatcoding.finio.platform.api.UpdateProfileRequest
import com.slowatcoding.finio.ui.common.collectFinanceState
import com.slowatcoding.finio.ui.common.financeStore
import com.slowatcoding.finio.ui.components.FinioCard
import com.slowatcoding.finio.ui.components.FinioDialog
import com.slowatcoding.finio.ui.components.FinioDivider
import com.slowatcoding.finio.ui.components.SwitchField
import com.slowatcoding.finio.ui.components.FinioSelect
import com.slowatcoding.finio.ui.components.FinioTextField
import com.slowatcoding.finio.ui.components.SelectOption
import com.slowatcoding.finio.ui.icons.LucideIcons
import com.slowatcoding.finio.ui.screens.auth.toStoreUser
import com.slowatcoding.finio.ui.theme.FinioTheme
import com.slowatcoding.finio.ui.theme.FinioType
import kotlinx.coroutines.launch

private val themes = listOf(
    SelectOption(Theme.System, "System"),
    SelectOption(Theme.Light, "Light"),
    SelectOption(Theme.Dark, "Dark"),
)

private val monthStartDays = (MIN_MONTH_START_DAY..MAX_MONTH_START_DAY).toList()

/**
 * Port of web/src/components/settings/ProfileSection.tsx — the tap-to-edit name (synced to the
 * cloud profile when signed in), theme, and the financial month's start day.
 */
@Composable
fun ProfileSection() {
    val container = appContainer()
    val store = financeStore()
    val state by collectFinanceState()
    val settings = state.settings
    val scope = rememberCoroutineScope()
    val colors = FinioTheme.colors

    var editingName by remember { mutableStateOf(false) }
    var nameValue by remember { mutableStateOf(settings.userName) }
    var showMonthStartPicker by remember { mutableStateOf(false) }

    val monthStartDay = normalizeMonthStartDay(settings.monthStartDay)
    val currentCycleLabel = remember(monthStartDay) {
        periodLabel(periodRange(PeriodType.Monthly, nowInstant(), monthStartDay), monthStartDay)
    }

    fun handleNameSave(newName: String) {
        if (!editingName) return
        val trimmed = cleanText(newName, MAX_NAME_LENGTH).ifEmpty { "User" }
        store.updateSettings { it.copy(userName = trimmed) }
        nameValue = trimmed
        editingName = false
        val token = container.auth.current.token?.takeIf { it.isNotEmpty() } ?: return
        scope.launch {
            try {
                val res = container.api.updateProfile(token, UpdateProfileRequest(name = trimmed))
                container.auth.setAuth(res.token, res.user.toStoreUser())
            } catch (_: Exception) {
                // local save succeeded; backend sync failed silently
            }
        }
    }

    // Profile name
    FinioCard(contentPadding = PaddingValues(0.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(LucideIcons.User, null, Modifier.size(18.dp), tint = colors.mutedForeground)
            if (editingName) {
                val focus = remember { FocusRequester() }
                var hadFocus by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
                FinioTextField(
                    value = nameValue,
                    onValueChange = { nameValue = stripLeading(it).take(MAX_NAME_LENGTH) },
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focus)
                        .onFocusChanged {
                            // onBlur → save.
                            if (it.isFocused) hadFocus = true else if (hadFocus) handleNameSave(nameValue)
                        },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { handleNameSave(nameValue) }),
                )
            } else {
                Column(
                    Modifier
                        .weight(1f)
                        .clickable(role = Role.Button) {
                            nameValue = settings.userName
                            editingName = true
                        },
                ) {
                    Text(settings.userName, style = FinioType.bodyMedium, color = colors.foreground)
                    Text("Tap to edit name", style = FinioType.caption, color = colors.mutedForeground)
                }
            }
        }
    }

    // Preferences
    FinioCard(contentPadding = PaddingValues(0.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(Modifier.width(128.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(LucideIcons.Palette, null, Modifier.size(18.dp), tint = colors.mutedForeground)
                Text("Theme", style = FinioType.bodyMedium, color = colors.foreground)
            }
            FinioSelect(
                value = settings.theme,
                options = themes,
                onValueChange = { theme -> store.updateSettings { it.copy(theme = theme) } },
                modifier = Modifier.width(120.dp),
                title = "Theme",
            )
        }
        FinioDivider()
        SwitchField(
            title = "Use AMOLED colors in dark mode",
            description = "True black background that switches pixels off on OLED screens",
            checked = settings.amoledDark,
            onCheckedChange = { on -> store.updateSettings { it.copy(amoledDark = on) } },
            icon = { Icon(LucideIcons.Moon, null, Modifier.size(18.dp), tint = colors.mutedForeground) },
            modifier = Modifier.padding(16.dp),
        )
        FinioDivider()
        SettingsValueRow(LucideIcons.CalendarRange, "Month starts on", subtitle = "Current cycle: $currentCycleLabel") {
            // A grid beats a 28-item dropdown here — every day is one tap away.
            PillButton(
                formatOrdinal(monthStartDay),
                onClick = { showMonthStartPicker = true },
                contentDescription = "Change the day the month starts",
            )
        }
    }

    if (showMonthStartPicker) {
        FinioDialog(
            onDismissRequest = { showMonthStartPicker = false },
            title = "Month starts on",
            description = "Every \"this month\" total and monthly budget will run from this day to the day before it in the next month.",
        ) {
            ChoiceGrid(monthStartDays, columns = 7) { day, mod ->
                ChoicePill(
                    day.toString(),
                    selected = day == monthStartDay,
                    onClick = {
                        store.updateSettings { it.copy(monthStartDay = day) }
                        showMonthStartPicker = false
                    },
                    modifier = mod,
                )
            }
            Text(
                "Days after the 28th aren't offered — they don't exist in every month.",
                style = FinioType.caption,
                color = colors.mutedForeground,
            )
        }
    }
}
