package com.slowatcoding.finio.ui.screens.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slowatcoding.finio.ui.components.ButtonSize
import com.slowatcoding.finio.ui.components.FinioIconButton
import com.slowatcoding.finio.ui.components.formatShortDate
import com.slowatcoding.finio.ui.icons.LucideIcons
import com.slowatcoding.finio.ui.theme.FinioRadius
import com.slowatcoding.finio.ui.theme.FinioShapes
import com.slowatcoding.finio.ui.theme.FinioTheme
import com.slowatcoding.finio.ui.theme.FinioType
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/** react-day-picker's `DateRange`. */
@Immutable
data class DayRange(val from: LocalDate, val to: LocalDate? = null)

/** react-day-picker v9 `addToRange` for the common cases: pick a start, then an end. */
private fun addToRange(day: LocalDate, range: DayRange?): DayRange = when {
    range == null -> DayRange(day)
    range.to == null -> if (day.isBefore(range.from)) DayRange(day, range.from) else DayRange(range.from, day)
    day == range.from && day == range.to -> DayRange(day)
    day.isBefore(range.from) -> DayRange(day, range.to)
    day.isAfter(range.to) -> DayRange(range.from, day)
    else -> DayRange(range.from, day)
}

/**
 * The range calendar (calendar.tsx `mode="range"`): the month/year dropdown caption between
 * ghost prev/next buttons, Sunday-first initials; the range ends on a primary disc-square with
 * primary-foreground text, the days between on a muted band, today on a muted disc.
 */
@Composable
fun RangeCalendar(
    selected: DayRange?,
    onSelect: (DayRange) -> Unit,
    modifier: Modifier = Modifier,
    today: LocalDate = LocalDate.now(),
    cellSize: Dp = 36.dp,
) {
    val colors = FinioTheme.colors
    val startMonth = YearMonth.of(today.year - 30, 1)
    val endMonth = YearMonth.of(today.year + 40, 12)
    var month by remember { mutableStateOf(YearMonth.from(selected?.from ?: today)) }

    Column(modifier.padding(8.dp).width(cellSize * 7)) {
        Row(Modifier.height(cellSize), verticalAlignment = Alignment.CenterVertically) {
            FinioIconButton(
                LucideIcons.ChevronLeft, "Previous month", onClick = { month = month.minusMonths(1) },
                size = ButtonSize.IconSm, enabled = month > startMonth, modifier = Modifier.size(cellSize),
            )
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)) {
                Caption(
                    label = month.month.getDisplayName(TextStyle.SHORT, Locale.US),
                    items = (1..12).map { YearMonth.of(month.year, it) }.filter { it in startMonth..endMonth },
                    itemLabel = { it.month.getDisplayName(TextStyle.SHORT, Locale.US) },
                    isCurrent = { it == month },
                    onPick = { month = it },
                    description = "Choose the month",
                )
                Caption(
                    label = month.year.toString(),
                    items = (startMonth.year..endMonth.year).map { YearMonth.of(it, month.monthValue) },
                    itemLabel = { it.year.toString() },
                    isCurrent = { it.year == month.year },
                    onPick = { month = it },
                    description = "Choose the year",
                )
            }
            FinioIconButton(
                LucideIcons.ChevronRight, "Next month", onClick = { month = month.plusMonths(1) },
                size = ButtonSize.IconSm, enabled = month < endMonth, modifier = Modifier.size(cellSize),
            )
        }
        Spacer(Modifier.height(16.dp))
        Row {
            listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa").forEach {
                Text(it, Modifier.weight(1f), style = FinioType.caption.copy(fontSize = 12.8.sp), color = colors.mutedForeground, textAlign = TextAlign.Center)
            }
        }
        val first = month.atDay(1)
        var weekStart = first.minusDays((first.dayOfWeek.value % 7).toLong())
        val last = month.atEndOfMonth()
        val from = selected?.from
        val to = selected?.to ?: selected?.from
        while (!weekStart.isAfter(last)) {
            Spacer(Modifier.height(8.dp))
            Row {
                for (i in 0 until 7) {
                    val day = weekStart.plusDays(i.toLong())
                    val inRange = from != null && to != null && !day.isBefore(from) && !day.isAfter(to)
                    val isStart = day == from
                    val isEnd = day == to
                    RangeDay(
                        day = day,
                        size = cellSize,
                        outside = YearMonth.from(day) != month,
                        isToday = day == today,
                        isEdge = isStart || isEnd,
                        inRange = inRange,
                        bandLeft = inRange && !isStart,
                        bandRight = inRange && !isEnd,
                        onClick = {
                            month = YearMonth.from(day)
                            onSelect(addToRange(day, selected))
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
private fun RangeDay(
    day: LocalDate,
    size: Dp,
    outside: Boolean,
    isToday: Boolean,
    isEdge: Boolean,
    inRange: Boolean,
    bandLeft: Boolean,
    bandRight: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val colors = FinioTheme.colors
    val radius = FinioRadius.md
    Box(
        modifier
            .height(size)
            .semantics {
                this.selected = inRange
                contentDescription = formatShortDate(day)
            }
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (inRange) {
            // The muted band joining the ends (range_start/middle/end + their `after:` bridges).
            Row(Modifier.fillMaxWidth().fillMaxHeight()) {
                Box(Modifier.weight(1f).fillMaxHeight().background(if (bandLeft) colors.muted else Color.Transparent))
                Box(Modifier.weight(1f).fillMaxHeight().background(if (bandRight) colors.muted else Color.Transparent))
            }
        }
        val fill = when {
            isEdge -> colors.primary
            inRange -> colors.muted
            isToday -> colors.muted
            else -> Color.Transparent
        }
        val shape = if (isToday && !inRange) FinioShapes.full else RoundedCornerShape(if (inRange && !isEdge) 0.dp else radius)
        Box(Modifier.fillMaxWidth().fillMaxHeight().clip(shape).background(fill), contentAlignment = Alignment.Center) {
            Text(
                day.dayOfMonth.toString(),
                style = FinioType.body,
                color = when {
                    isEdge -> colors.primaryForeground
                    outside && !inRange -> colors.mutedForeground
                    else -> colors.foreground
                },
            )
        }
    }
}

@Composable
private fun <T> Caption(
    label: String,
    items: List<T>,
    itemLabel: (T) -> String,
    isCurrent: (T) -> Boolean,
    onPick: (T) -> Unit,
    description: String,
) {
    val colors = FinioTheme.colors
    var open by remember { mutableStateOf(false) }
    val itemHeight = 36.dp
    val initialScroll = with(LocalDensity.current) { ((items.indexOfFirst(isCurrent) - 3).coerceAtLeast(0) * itemHeight.toPx()).toInt() }
    Box {
        Row(
            Modifier
                .clip(RoundedCornerShape(FinioRadius.md))
                .semantics { contentDescription = description }
                .clickable(role = Role.DropdownList) { open = true }
                .padding(4.dp),
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
                    onClick = { onPick(item); open = false },
                    modifier = Modifier.height(itemHeight),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                )
            }
        }
    }
}
