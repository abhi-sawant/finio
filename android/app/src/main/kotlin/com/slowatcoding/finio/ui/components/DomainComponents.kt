package com.slowatcoding.finio.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slowatcoding.finio.core.model.TransactionType
import com.slowatcoding.finio.ui.icons.LucideIcons
import com.slowatcoding.finio.core.calc.BudgetHealth
import com.slowatcoding.finio.ui.theme.CssShadow
import com.slowatcoding.finio.ui.theme.FinioShapes
import com.slowatcoding.finio.ui.theme.FinioTheme
import com.slowatcoding.finio.ui.theme.FinioType
import com.slowatcoding.finio.ui.theme.cssShadow
import com.slowatcoding.finio.ui.theme.mix
import kotlin.math.ceil

/**
 * The 3-way pill switch (AddTransaction's type selector): a muted full-round track with 4dp
 * padding and equal cells 4dp apart; the active cell is the lavender gradient with its glow and
 * white text, the rest muted (14sp 500, 8dp tall padding).
 */
@Composable
fun <T> SegmentedPills(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = FinioTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            .clip(FinioShapes.full)
            .background(colors.muted)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { (value, label) ->
            val active = value == selected
            val text by animateColorAsState(if (active) Color.White else colors.mutedForeground, tween(150), label = "pill")
            Box(
                Modifier
                    .weight(1f)
                    .cssShadow(FinioShapes.full, if (active) FinioTheme.shadows.glowPrimary else emptyList())
                    .clip(FinioShapes.full)
                    .then(if (active) Modifier.background(FinioTheme.brushes.gradPrimary) else Modifier)
                    .semantics { this.selected = active }
                    .clickable(role = Role.Tab) { onSelect(value) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, style = FinioType.bodyMedium, color = text, maxLines = 1)
            }
        }
    }
}

/**
 * Names a budget's standing in words and an icon (BudgetHealthBadge.tsx): "Over budget" on the
 * magenta band, "Near limit" in amber at 15%, "On track" in green at 10%; 10sp 500, 10dp icon.
 */
@Composable
fun BudgetHealthBadge(health: BudgetHealth, modifier: Modifier = Modifier) {
    val colors = FinioTheme.colors
    val (label, icon, pair) = when (health) {
        BudgetHealth.Over -> Triple("Over budget", LucideIcons.TriangleAlert, colors.warningBand to colors.warningBandAccent)
        BudgetHealth.Near -> Triple("Near limit", LucideIcons.TrendingUp, colors.warning.mix(0.15f) to colors.warning)
        BudgetHealth.Ok -> Triple("On track", LucideIcons.Check, colors.positive.mix(0.1f) to colors.positive)
    }
    Row(
        modifier
            .clip(FinioShapes.full)
            .background(pair.first)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, Modifier.size(10.dp), tint = pair.second)
        Text(label, style = FinioType.micro, color = pair.second, maxLines = 1)
    }
}

/** CategoryIcon.tsx `CATEGORY_ICON_MAP` keys, in the web's order (the category icon picker offers these). */
val CategoryIconNames = listOf(
    "utensils", "car", "shopping-bag", "shopping-cart", "film", "zap", "heart-pulse", "book-open", "home",
    "plane", "gift", "scissors", "repeat", "truck", "dollar-sign", "trending-up", "briefcase", "laptop",
    "building-2", "circle-ellipsis", "coffee", "shirt", "gamepad-2", "music", "dumbbell", "graduation-cap",
    "paw-print", "wrench", "fuel", "bus", "wifi", "smartphone", "tv", "credit-card", "wallet", "piggy-bank",
    "hand-coins", "landmark", "receipt", "stethoscope", "pill", "palette", "camera", "umbrella", "cake", "bike",
    "users", "banknote", "droplet", "flame", "sparkles", "tag", "baby",
)

/** goalIcons.ts `GOAL_ICON_MAP` keys. */
val GoalIconNames = listOf(
    "target", "piggy-bank", "plane", "home", "car", "graduation-cap", "gift", "laptop", "heart-pulse",
    "umbrella", "briefcase", "smartphone",
)

/** personIcons.ts `PERSON_ICON_MAP` keys. */
val PersonIconNames = listOf(
    "user", "users", "handshake", "briefcase", "home", "heart", "graduation-cap", "store", "landmark", "smile",
)

/**
 * A category/goal/person icon by its stored kebab-case name (CategoryIcon.tsx); an unknown name
 * falls back to its first character at 80% of the size, as on the web.
 */
@Composable
fun CategoryIcon(icon: String, modifier: Modifier = Modifier, size: Dp = 16.dp, tint: Color = androidx.compose.material3.LocalContentColor.current) {
    val vector = LucideIcons.byKebab(icon)
    if (vector != null) {
        Icon(vector, null, modifier.size(size), tint = tint)
    } else {
        val sp = with(LocalDensity.current) { (size * 0.8f).toSp() }
        Text(icon.take(1), modifier, style = FinioType.body.copy(fontSize = sp, lineHeight = sp), color = tint)
    }
}

/** Parses a stored `#rrggbb` (or `#rgb`) colour; unparseable values fall back to [fallback]. */
fun parseHexColor(hex: String, fallback: Color = Color(0xFF94A3B8)): Color {
    val h = hex.removePrefix("#")
    val full = when (h.length) {
        3 -> h.map { "$it$it" }.joinToString("")
        6 -> h
        8 -> h.substring(0, 6)
        else -> return fallback
    }
    return full.toLongOrNull(16)?.let { Color(0xFF000000 or it) } ?: fallback
}

/** One category choice for [CategoryGrid]. */
@Immutable
data class CategoryTileData(val id: String, val name: String, val icon: String, val color: Color)

/**
 * The shared 4-column category tile grid (CategoryGrid.tsx + AddTransaction's tiles): 8dp gaps,
 * a fixed max height (216dp) that scrolls, and a "Scroll for N more" hint while tiles are hidden
 * below the fold. Each tile is a `sm`-radius bordered card with a 32dp disc in the category colour
 * carrying a white 16dp icon over a 10sp two-line name; the selected tile wears the lavender ring
 * (`ring-grad-primary`: 1.5dp primary + Float shadow) on the category colour at 13%. The selected
 * tile is scrolled into view on first show.
 */
@Composable
fun CategoryGrid(
    categories: List<CategoryTileData>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    maxHeight: Dp = 216.dp,
    scrollState: ScrollState = rememberScrollState(),
) {
    val colors = FinioTheme.colors
    val density = LocalDensity.current
    var viewport by remember { mutableIntStateOf(0) }
    var rowHeight by remember { mutableIntStateOf(0) }
    val gap = with(density) { 8.dp.roundToPx() }
    val rows = ceil(categories.size / 4.0).toInt()

    // Scroll only the grid, never the page, so the selected tile sits mid-view.
    LaunchedEffect(rowHeight, viewport) {
        val index = categories.indexOfFirst { it.id == selectedId }
        if (index >= 0 && rowHeight > 0 && viewport > 0) {
            val top = (index / 4) * (rowHeight + gap)
            scrollState.scrollTo((top - (viewport - rowHeight) / 2).coerceAtLeast(0))
        }
    }

    val hidden = if (rowHeight == 0 || viewport == 0) {
        0
    } else {
        // A tile counts as hidden when its centre is below the visible bottom (CategoryGrid.tsx).
        val bottom = scrollState.value + viewport
        categories.indices.count { i ->
            val row = i / 4
            row * (rowHeight + gap) + rowHeight / 2 > bottom + 1
        }
    }

    Column(modifier) {
        Box(
            Modifier
                .fillMaxWidth()
                .heightIn(max = maxHeight)
                .onSizeChanged { viewport = it.height }
                .verticalScroll(scrollState)
                .padding(end = 4.dp),
        ) {
            Layout(
                content = {
                    categories.forEach { c ->
                        CategoryTile(c, selected = c.id == selectedId, onClick = { onSelect(c.id) })
                    }
                },
            ) { measurables, constraints ->
                val cell = (constraints.maxWidth - gap * 3) / 4
                val placeables = measurables.map { it.measure(constraints.copy(minWidth = cell, maxWidth = cell, minHeight = 0)) }
                val rowHeights = placeables.chunked(4).map { row -> row.maxOf { it.height } }
                if (rowHeights.isNotEmpty() && rowHeight != rowHeights.first()) rowHeight = rowHeights.first()
                val height = rowHeights.sum() + gap * (rows - 1).coerceAtLeast(0)
                layout(constraints.maxWidth, height) {
                    var y = 0
                    placeables.chunked(4).forEachIndexed { r, row ->
                        row.forEachIndexed { i, p -> p.place(i * (cell + gap), y) }
                        y += rowHeights[r] + gap
                    }
                }
            }
        }
        if (hidden > 0) {
            Text(
                "Scroll for $hidden more",
                Modifier.fillMaxWidth().padding(top = 6.dp),
                style = FinioType.caption.copy(fontSize = 11.sp),
                color = colors.mutedForeground,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun CategoryTile(c: CategoryTileData, selected: Boolean, onClick: () -> Unit) {
    val colors = FinioTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val shape = FinioShapes.sm
    val ring = remember(colors.primary, selected) {
        if (selected) listOf(CssShadow(spread = 1.5.dp, color = colors.primary)) else emptyList()
    }
    Column(
        Modifier
            .cssShadow(shape, ring + if (selected) FinioTheme.shadows.float else emptyList())
            .clip(shape)
            .background(
                when {
                    selected -> c.color.copy(alpha = 0x22 / 255f)
                    pressed -> colors.muted
                    else -> colors.card
                },
            )
            .border(1.dp, if (selected) Color.Transparent else colors.border, shape)
            .semantics { this.selected = selected }
            .clickable(interaction, null, role = Role.Button, onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(Modifier.size(32.dp).clip(FinioShapes.full).background(c.color), contentAlignment = Alignment.Center) {
            CategoryIcon(c.icon, size = 16.dp, tint = Color.White)
        }
        Text(
            c.name,
            style = FinioType.caption.copy(fontSize = 10.sp, lineHeight = 12.5.sp),
            color = colors.foreground,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** A label shown under a [TransactionRow]. */
@Immutable
data class RowLabel(val name: String, val color: Color)

/**
 * A transaction row (TransactionItem.tsx), presentational: [title] (note, split names or
 * category) 14sp 500 with a repeat glyph for recurring rows, a muted 12sp [subtitle]
 * ("Food · HDFC" or "HDFC → Cash"), optional label chips, and the amount in 14sp 600 Geist —
 * green and "+" for income, foreground and "−" for expense, muted and unsigned for transfers.
 * [amount] is the formatted figure without a sign. Rows pad 12dp × 12dp, tinted muted while
 * pressed; [onLongClick] opens the row menu; [selectionMode] shows a checkbox.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun TransactionRow(
    title: String,
    subtitle: String,
    amount: String,
    type: TransactionType,
    modifier: Modifier = Modifier,
    recurring: Boolean = false,
    labels: List<RowLabel> = emptyList(),
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    selectionMode: Boolean = false,
    selected: Boolean = false,
) {
    val colors = FinioTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val (amountColor, prefix, typeLabel) = when (type) {
        TransactionType.Income -> Triple(colors.positive, "+", "Income")
        TransactionType.Expense -> Triple(colors.foreground, "-", "Expense")
        TransactionType.Transfer -> Triple(colors.mutedForeground, "", "Transfer")
    }
    Row(
        modifier
            .fillMaxWidth()
            .background(if (pressed) colors.muted.mix(0.6f) else Color.Transparent)
            .semantics(mergeDescendants = true) { if (selectionMode) this.selected = selected }
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = { onClick?.invoke() },
                onLongClick = if (selectionMode) null else onLongClick,
            )
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selectionMode) FinioCheckbox(checked = selected, onCheckedChange = null)
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    title,
                    Modifier.weight(1f, fill = false),
                    style = FinioType.bodyMedium,
                    color = colors.foreground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (recurring) {
                    Icon(
                        LucideIcons.Repeat,
                        contentDescription = "Recurring",
                        modifier = Modifier.size(12.dp),
                        tint = colors.mutedForeground,
                    )
                }
            }
            Text(subtitle, style = FinioType.caption, color = colors.mutedForeground, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (labels.isNotEmpty()) {
                FlowRow(
                    Modifier.padding(top = 4.dp).semantics { contentDescription = "Labels" },
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    labels.forEach { LabelChip(it.name, it.color) }
                }
            }
        }
        Text(
            "$prefix$amount",
            Modifier.semantics { contentDescription = "$typeLabel: $prefix$amount" },
            style = FinioType.rowValue,
            color = amountColor,
            maxLines = 1,
        )
    }
}
