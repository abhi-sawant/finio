package com.slowatcoding.finio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slowatcoding.finio.ui.icons.LucideIcons
import com.slowatcoding.finio.ui.theme.FinioRadius
import com.slowatcoding.finio.ui.theme.FinioShapes
import com.slowatcoding.finio.ui.theme.FinioTheme
import com.slowatcoding.finio.ui.theme.FinioType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val ShortDate = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.US)
private val InputTime = DateTimeFormatter.ofPattern("hh:mm a", Locale.US)

/** formatters.ts `formatShortDate` — "5 Oct 2026", the canonical date everywhere a year shows. */
fun formatShortDate(date: LocalDate): String = date.format(ShortDate)

/**
 * The month calendar (calendar.tsx over react-day-picker): month/year dropdown captions between
 * ghost prev/next buttons, Sunday-first muted weekday initials, outside days shown muted, today
 * on a muted disc, the selected day on a lavender-gradient disc with white text, and days outside
 * [minDate]..[maxDate] at 50% and inert. The year dropdown spans this year −30…+40, as on the web
 * (future goal and maturity dates must stay reachable).
 *
 * Compromise: cells are [cellSize] (36dp) rather than the web's 28px, for touch.
 */
@Composable
fun FinioCalendar(
    selected: LocalDate?,
    onSelect: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    minDate: LocalDate? = null,
    maxDate: LocalDate? = null,
    today: LocalDate = LocalDate.now(),
    cellSize: Dp = 36.dp,
) {
    val colors = FinioTheme.colors
    val startMonth = YearMonth.of(today.year - 30, 1)
    val endMonth = YearMonth.of(today.year + 40, 12)
    var month by remember { mutableStateOf(YearMonth.from(selected ?: today)) }

    Column(modifier.padding(8.dp).width(cellSize * 7)) {
        Row(Modifier.height(cellSize), verticalAlignment = Alignment.CenterVertically) {
            FinioIconButton(
                LucideIcons.ChevronLeft,
                "Previous month",
                onClick = { month = month.minusMonths(1) },
                size = ButtonSize.IconSm,
                enabled = month > startMonth,
                modifier = Modifier.size(cellSize),
            )
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)) {
                CaptionDropdown(
                    label = month.month.getDisplayName(TextStyle.SHORT, Locale.US),
                    items = (1..12).map { YearMonth.of(month.year, it) }.filter { it in startMonth..endMonth },
                    itemLabel = { it.month.getDisplayName(TextStyle.SHORT, Locale.US) },
                    isCurrent = { it == month },
                    onPick = { month = it },
                    contentDescription = "Choose the month",
                )
                CaptionDropdown(
                    label = month.year.toString(),
                    items = (startMonth.year..endMonth.year).map { YearMonth.of(it, month.monthValue) },
                    itemLabel = { it.year.toString() },
                    isCurrent = { it.year == month.year },
                    onPick = { month = it },
                    contentDescription = "Choose the year",
                )
            }
            FinioIconButton(
                LucideIcons.ChevronRight,
                "Next month",
                onClick = { month = month.plusMonths(1) },
                size = ButtonSize.IconSm,
                enabled = month < endMonth,
                modifier = Modifier.size(cellSize),
            )
        }
        Spacer(Modifier.height(16.dp))
        Row {
            listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa").forEach {
                Text(
                    it,
                    Modifier.weight(1f),
                    style = FinioType.caption.copy(fontSize = 12.8.sp),
                    color = colors.mutedForeground,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
        val first = month.atDay(1)
        val gridStart = first.minusDays((first.dayOfWeek.value % 7).toLong())
        val last = month.atEndOfMonth()
        var weekStart = gridStart
        while (!weekStart.isAfter(last)) {
            Spacer(Modifier.height(8.dp))
            Row {
                for (i in 0 until 7) {
                    val day = weekStart.plusDays(i.toLong())
                    val disabled = (minDate != null && day.isBefore(minDate)) || (maxDate != null && day.isAfter(maxDate))
                    DayCell(
                        day = day,
                        size = cellSize,
                        outside = YearMonth.from(day) != month,
                        isToday = day == today,
                        isSelected = day == selected,
                        disabled = disabled,
                        onClick = {
                            month = YearMonth.from(day)
                            onSelect(day)
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            weekStart = weekStart.plusWeeks(1)
        }
    }
}

@Composable
private fun DayCell(
    day: LocalDate,
    size: Dp,
    outside: Boolean,
    isToday: Boolean,
    isSelected: Boolean,
    disabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val colors = FinioTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val cellShape = RoundedCornerShape(FinioRadius.md)
    Box(
        modifier
            .height(size)
            .alpha(if (disabled) 0.5f else 1f)
            .clip(if (isSelected || isToday) FinioShapes.full else cellShape)
            .then(
                when {
                    isSelected -> Modifier.background(FinioTheme.brushes.gradPrimary)
                    isToday || pressed -> Modifier.background(colors.muted)
                    else -> Modifier
                },
            )
            .semantics {
                this.selected = isSelected
                contentDescription = formatShortDate(day)
            }
            .clickable(interaction, null, enabled = !disabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            day.dayOfMonth.toString(),
            style = FinioType.body,
            color = when {
                isSelected -> Color.White
                outside || disabled -> colors.mutedForeground
                else -> colors.foreground
            },
        )
    }
}

@Composable
private fun <T> CaptionDropdown(
    label: String,
    items: List<T>,
    itemLabel: (T) -> String,
    isCurrent: (T) -> Boolean,
    onPick: (T) -> Unit,
    contentDescription: String,
) {
    val colors = FinioTheme.colors
    var open by remember { mutableStateOf(false) }
    val itemHeight = 36.dp
    val initialScroll = with(LocalDensity.current) {
        ((items.indexOfFirst(isCurrent) - 3).coerceAtLeast(0) * itemHeight.toPx()).toInt()
    }
    Box {
        Row(
            Modifier
                .clip(RoundedCornerShape(FinioRadius.md))
                .semantics { this.contentDescription = contentDescription }
                .clickable(role = Role.DropdownList) { open = true }
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(label, style = FinioType.bodyMedium, color = colors.popoverForeground)
            Icon(LucideIcons.ChevronDown, null, Modifier.size(14.dp), tint = colors.mutedForeground)
        }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            scrollState = rememberScrollState(initialScroll),
            shape = FinioShapes.sm,
            containerColor = colors.popover,
            tonalElevation = 0.dp,
            shadowElevation = 8.dp,
        ) {
            items.forEach { item ->
                val current = isCurrent(item)
                DropdownMenuItem(
                    text = {
                        Text(
                            itemLabel(item),
                            style = FinioType.body.copy(fontWeight = if (current) FontWeight.SemiBold else FontWeight.Normal),
                            color = if (current) colors.primary else colors.popoverForeground,
                        )
                    },
                    onClick = {
                        onPick(item)
                        open = false
                    },
                    modifier = Modifier.height(itemHeight),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                )
            }
        }
    }
}

/**
 * The date field (date-picker.tsx): an input-shaped trigger with a calendar icon and the
 * [formatShortDate] value (muted placeholder when empty) that opens [FinioCalendar] in an
 * anchored popover; picking a day closes it.
 */
@Composable
fun FinioDatePicker(
    value: LocalDate?,
    onValueChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Pick a date",
    enabled: Boolean = true,
    minDate: LocalDate? = null,
    maxDate: LocalDate? = null,
) {
    var open by remember { mutableStateOf(false) }
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    Box(modifier) {
        FieldTrigger(
            text = value?.let(::formatShortDate) ?: placeholder,
            isPlaceholder = value == null,
            onClick = { open = true },
            enabled = enabled,
            open = open,
            leadingIcon = LucideIcons.Calendar,
            showChevron = false,
        )
        if (open) {
            FinioPopover(onDismissRequest = { open = false; focusManager.clearFocus(force = true) }, contentPadding = PaddingValues(0.dp)) {
                FinioCalendar(
                    selected = value,
                    onSelect = {
                        onValueChange(it)
                        open = false
                        focusManager.clearFocus(force = true)
                    },
                    minDate = minDate,
                    maxDate = maxDate,
                )
            }
        }
    }
}

/**
 * The date + time field (date-time-picker.tsx): the date trigger beside a 112dp time field. The
 * date keeps the current time when changed; the time is disabled until there is a date, and
 * opens the Material time dial in a [FinioDialog], themed with the Mudra palette.
 *
 * Compromise: the web's time field is the browser's native `<input type="time">`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinioDateTimePicker(
    value: LocalDateTime?,
    onValueChange: (LocalDateTime) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Pick a date",
    enabled: Boolean = true,
) {
    val colors = FinioTheme.colors
    var timeOpen by remember { mutableStateOf(false) }
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FinioDatePicker(
            value = value?.toLocalDate(),
            onValueChange = { d ->
                val base = value?.toLocalTime() ?: LocalTime.now()
                onValueChange(LocalDateTime.of(d, LocalTime.of(base.hour, base.minute)))
            },
            modifier = Modifier.weight(1f),
            placeholder = placeholder,
            enabled = enabled,
        )
        FieldTrigger(
            text = value?.format(InputTime)?.lowercase(Locale.US) ?: "--:-- --",
            isPlaceholder = value == null,
            onClick = { timeOpen = true },
            modifier = Modifier.width(112.dp),
            enabled = enabled && value != null,
            open = timeOpen,
            showChevron = false,
        )
    }
    if (timeOpen && value != null) {
        val state = rememberTimePickerState(value.hour, value.minute, is24Hour = false)
        FinioDialog(
            onDismissRequest = { timeOpen = false },
            title = "Time",
            footer = {
                FinioButton("Cancel", onClick = { timeOpen = false }, variant = ButtonVariant.Outline)
                FinioButton("Done", onClick = {
                    onValueChange(value.withHour(state.hour).withMinute(state.minute).withSecond(0).withNano(0))
                    timeOpen = false
                })
            },
        ) {
            TimePicker(
                state = state,
                modifier = Modifier.align(Alignment.CenterHorizontally),
                colors = TimePickerDefaults.colors(
                    clockDialColor = colors.muted,
                    clockDialSelectedContentColor = Color.White,
                    clockDialUnselectedContentColor = colors.foreground,
                    selectorColor = colors.primary,
                    containerColor = colors.popover,
                    periodSelectorBorderColor = colors.input,
                    periodSelectorSelectedContainerColor = colors.accent,
                    periodSelectorUnselectedContainerColor = Color.Transparent,
                    periodSelectorSelectedContentColor = colors.accentForeground,
                    periodSelectorUnselectedContentColor = colors.mutedForeground,
                    timeSelectorSelectedContainerColor = colors.accent,
                    timeSelectorUnselectedContainerColor = colors.muted,
                    timeSelectorSelectedContentColor = colors.accentForeground,
                    timeSelectorUnselectedContentColor = colors.foreground,
                ),
            )
        }
    }
}
