package com.slowatcoding.finio.gallery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.slowatcoding.finio.core.model.AccountType
import com.slowatcoding.finio.core.model.TransactionType
import com.slowatcoding.finio.ui.components.AlertBand
import com.slowatcoding.finio.ui.components.BudgetHealthBadge
import com.slowatcoding.finio.ui.components.ButtonSize
import com.slowatcoding.finio.ui.components.ButtonVariant
import com.slowatcoding.finio.ui.components.CategoryGrid
import com.slowatcoding.finio.ui.components.CategoryTileData
import com.slowatcoding.finio.ui.components.ConfirmHost
import com.slowatcoding.finio.ui.components.EmptyState
import com.slowatcoding.finio.ui.components.FieldLabel
import com.slowatcoding.finio.ui.components.FinioAlertDialog
import com.slowatcoding.finio.ui.components.FinioButton
import com.slowatcoding.finio.ui.components.FinioCard
import com.slowatcoding.finio.ui.components.FinioCheckbox
import com.slowatcoding.finio.ui.components.FinioChip
import com.slowatcoding.finio.ui.components.FinioDatePicker
import com.slowatcoding.finio.ui.components.FinioDateTimePicker
import com.slowatcoding.finio.ui.components.FinioDialog
import com.slowatcoding.finio.ui.components.FinioDivider
import com.slowatcoding.finio.ui.components.FinioIconButton
import com.slowatcoding.finio.ui.components.FinioScreen
import com.slowatcoding.finio.ui.components.FinioSelect
import com.slowatcoding.finio.ui.components.FinioSwitch
import com.slowatcoding.finio.ui.components.FinioTextField
import com.slowatcoding.finio.ui.components.FinioToastHost
import com.slowatcoding.finio.ui.components.HeaderIconButton
import com.slowatcoding.finio.ui.components.HeaderIconTone
import com.slowatcoding.finio.ui.components.LocalConfirm
import com.slowatcoding.finio.ui.components.NumberPad
import com.slowatcoding.finio.ui.components.PadKey
import com.slowatcoding.finio.ui.components.PadSurface
import com.slowatcoding.finio.ui.components.PageTitle
import com.slowatcoding.finio.ui.components.PinDots
import com.slowatcoding.finio.ui.components.PinPad
import com.slowatcoding.finio.ui.components.RowLabel
import com.slowatcoding.finio.ui.components.SectionHeader
import com.slowatcoding.finio.ui.components.SegmentedPills
import com.slowatcoding.finio.ui.components.SelectOption
import com.slowatcoding.finio.ui.components.SwitchField
import com.slowatcoding.finio.ui.components.SwitchSize
import com.slowatcoding.finio.ui.components.ToastAction
import com.slowatcoding.finio.ui.components.TransactionRow
import com.slowatcoding.finio.ui.components.parseHexColor
import com.slowatcoding.finio.ui.components.toast
import com.slowatcoding.finio.ui.icons.LucideIcons
import com.slowatcoding.finio.ui.mudra.BudgetHealth
import com.slowatcoding.finio.ui.mudra.BudgetProgressBar
import com.slowatcoding.finio.ui.mudra.CoinFab
import com.slowatcoding.finio.ui.mudra.Guilloche
import com.slowatcoding.finio.ui.mudra.NoteCard
import com.slowatcoding.finio.ui.mudra.NoteChip
import com.slowatcoding.finio.ui.mudra.NoteTile
import com.slowatcoding.finio.ui.mudra.PaperBackground
import com.slowatcoding.finio.ui.mudra.RegisterBar
import com.slowatcoding.finio.ui.mudra.ThreadProgressBar
import com.slowatcoding.finio.ui.mudra.label
import com.slowatcoding.finio.ui.mudra.noteFigureStyle
import com.slowatcoding.finio.ui.theme.FinioTheme
import com.slowatcoding.finio.ui.theme.FinioType
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Debug-only visual catalogue of every Mudra primitive, switchable between light and dark — the
 * Android counterpart of eyeballing the PWA. Not part of the real navigation shell.
 */
@Composable
/**
 * [embedded]: rendered inside the app shell (Routes.DebugGallery), which already hosts the
 * toaster — skip this screen's own toast host so toasts don't render twice.
 */
fun GalleryScreen(embedded: Boolean = false) {
    var dark by remember { mutableStateOf(false) }
    FinioTheme(dark = dark) {
        ConfirmHost {
            PaperBackground {
                FinioScreen(header = {
                    PageTitle("Mudra")
                    HeaderIconButton(
                        icon = if (dark) LucideIcons.Eye else LucideIcons.EyeOff,
                        contentDescription = "Toggle dark mode",
                        onClick = { dark = !dark },
                        pressed = dark,
                    )
                }) {
                    SegmentedPills(
                        options = listOf(false to "Light", true to "Dark"),
                        selected = dark,
                        onSelect = { dark = it },
                    )
                    GalleryContent()
                }
                CoinFab(
                    onClick = { toast.info("Add transaction") },
                    onLongClick = { toast.message("Templates") },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(end = 16.dp, bottom = 88.dp),
                )
                if (!embedded) FinioToastHost()
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GalleryContent() {
    val colors = FinioTheme.colors
    val scope = rememberCoroutineScope()
    val confirm = LocalConfirm.current

    // ── Hero note ──
    NoteCard {
        Text("Safe to spend today", style = FinioType.bodyMedium, color = colors.mutedForeground)
        val figure = "₹1,240"
        Text(figure, Modifier.padding(top = 4.dp), style = noteFigureStyle(figure))
        Text("₹18,600 left this month · 15 days to go", Modifier.padding(top = 6.dp), style = FinioType.body)
        Text("After ₹2,400 of bills due this week", Modifier.padding(top = 4.dp), style = FinioType.caption, color = colors.mutedForeground)
        RegisterBar(percent = 62f, isOver = false, modifier = Modifier.padding(top = 12.dp))
        Text("₹31,400 of ₹50,000 spent", Modifier.padding(top = 6.dp), style = FinioType.caption, color = colors.mutedForeground)
    }

    // ── Note tiles + chips ──
    Column {
        SectionHeader("Where it sits", actionLabel = "See all", onAction = {})
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AccountType.entries.forEach { type ->
                NoteTile(
                    type = type,
                    name = when (type) {
                        AccountType.Checking -> "HDFC Salary"
                        AccountType.Credit -> "Amazon Pay ICICI card"
                        else -> type.label
                    },
                    amount = "₹42,180",
                    onClick = { toast.info(type.label) },
                    modifier = Modifier.width(160.dp).padding(bottom = 12.dp),
                )
            }
        }
    }
    FinioCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp)) {
        AccountType.entries.forEachIndexed { i, type ->
            if (i > 0) FinioDivider()
            Row(Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NoteChip(type, archived = type == AccountType.Wallet)
                Column(Modifier.weight(1f)) {
                    Text(type.label, style = FinioType.bodyMedium)
                    Text(if (type == AccountType.Wallet) "Closed · 4 transactions" else type.label, style = FinioType.caption, color = colors.mutedForeground)
                }
                Text("₹12,500", style = FinioType.rowValue)
            }
        }
    }

    // ── Buttons ──
    FinioCard {
        SectionHeader("Buttons")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ButtonVariant.entries.forEach { v -> FinioButton(v.name, onClick = {}, variant = v) }
        }
        Spacer(Modifier.height(12.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), itemVerticalAlignment = Alignment.CenterVertically) {
            FinioButton("Xs", onClick = {}, size = ButtonSize.Xs)
            FinioButton("Small", onClick = {}, size = ButtonSize.Sm)
            FinioButton("Add", onClick = {}, leadingIcon = LucideIcons.Plus)
            FinioButton("Large", onClick = {}, size = ButtonSize.Lg)
            FinioButton("Disabled", onClick = {}, enabled = false)
            FinioIconButton(LucideIcons.Pencil, "Edit", onClick = {}, variant = ButtonVariant.Outline)
            FinioIconButton(LucideIcons.Trash2, "Delete", onClick = {}, variant = ButtonVariant.Destructive)
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HeaderIconButton(LucideIcons.ArrowLeft, "Back", onClick = {})
            HeaderIconButton(LucideIcons.Plus, "Add", onClick = {}, tone = HeaderIconTone.Primary)
            HeaderIconButton(LucideIcons.Trash2, "Delete", onClick = {}, tone = HeaderIconTone.Destructive)
            HeaderIconButton(LucideIcons.Funnel, "Filters", onClick = {}, pressed = true)
        }
    }

    // ── Transaction list ──
    Column {
        SectionHeader("Latest", actionLabel = "See all", onAction = {})
        FinioCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
            TransactionRow("Swiggy", "Food · HDFC Salary", "₹450.50", TransactionType.Expense, labels = listOf(RowLabel("Work", parseHexColor("#3b82f6"))))
            FinioDivider()
            TransactionRow("Salary", "Salary · HDFC Salary", "₹85,000", TransactionType.Income, recurring = true)
            FinioDivider()
            TransactionRow("Card bill", "HDFC Salary → Amazon Pay ICICI", "₹12,000", TransactionType.Transfer, selectionMode = true, selected = true)
        }
    }

    // ── Fields ──
    FinioCard {
        SectionHeader("Fields")
        var name by remember { mutableStateOf("") }
        var note by remember { mutableStateOf("") }
        var account by remember { mutableStateOf<String?>(null) }
        var date by remember { mutableStateOf<LocalDate?>(LocalDate.now()) }
        var dateTime by remember { mutableStateOf<LocalDateTime?>(LocalDateTime.now()) }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column { FieldLabel("Name"); FinioTextField(name, { name = it }, placeholder = "e.g. HDFC Salary") }
            Column { FieldLabel("Note"); FinioTextField(note, { note = it }, placeholder = "Optional", singleLine = false, minLines = 3) }
            Column { FieldLabel("Invalid"); FinioTextField("abc", {}, isError = true) }
            Column { FieldLabel("Disabled"); FinioTextField("Locked", {}, enabled = false) }
            Column {
                FieldLabel("Account")
                FinioSelect(
                    value = account,
                    options = AccountType.entries.map { t -> SelectOption(t.name, t.label, leading = { NoteChip(t) }) },
                    onValueChange = { account = it },
                    placeholder = "Select account",
                    title = "Accounts",
                )
            }
            Column { FieldLabel("Date"); FinioDatePicker(date, { date = it }) }
            Column { FieldLabel("Date & time"); FinioDateTimePicker(dateTime, { dateTime = it }) }
        }
    }

    // ── Toggles ──
    FinioCard {
        SectionHeader("Toggles")
        var a by remember { mutableStateOf(true) }
        var b by remember { mutableStateOf(false) }
        var c by remember { mutableStateOf(true) }
        var type by remember { mutableStateOf(TransactionType.Expense) }
        var chip by remember { mutableStateOf("all") }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SwitchField("Bill reminders", a, { a = it }, description = "A day before each recurring bill")
            SwitchField("Daily nudge", b, { b = it }, interactiveRow = true, icon = { Icon(LucideIcons.Bell, null, Modifier.size(18.dp), tint = colors.mutedForeground) })
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                FinioSwitch(c, { c = it }, size = SwitchSize.Sm, contentDescription = "Small")
                FinioCheckbox(c, { c = it })
                FinioCheckbox(!c, { c = !it })
            }
            SegmentedPills(
                options = TransactionType.entries.map { it to it.name },
                selected = type,
                onSelect = { type = it },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("all", "expense", "income").forEach { FinioChip(it.replaceFirstChar(Char::uppercase), chip == it, { chip = it }) }
            }
        }
    }

    // ── Progress ──
    FinioCard {
        SectionHeader("Progress")
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                BudgetHealthBadge(BudgetHealth.Ok)
                BudgetHealthBadge(BudgetHealth.Near)
                BudgetHealthBadge(BudgetHealth.Over)
            }
            ThreadProgressBar(68f)
            BudgetProgressBar(40f, false, parseHexColor("#ef4444"))
            BudgetProgressBar(90f, false, parseHexColor("#ef4444"))
            BudgetProgressBar(120f, true, parseHexColor("#ef4444"))
            RegisterBar(55f, false)
        }
    }

    // ── Categories ──
    FinioCard {
        SectionHeader("Category")
        var selected by remember { mutableStateOf("cat-9") }
        CategoryGrid(SampleCategories, selected, { selected = it })
    }

    // ── Pads ──
    FinioCard {
        SectionHeader("Number pad")
        var amount by remember { mutableStateOf("") }
        NumberPad(amount, { amount = it })
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionHeader("PIN pad")
        var pin by remember { mutableStateOf("") }
        var error by remember { mutableStateOf(false) }
        PinDots(pin.length, 4, error = error)
        PinPad(
            value = pin,
            onValueChange = { pin = it; error = false },
            maxLength = 4,
            onComplete = { error = it != "1234"; pin = "" },
            leadingAction = {
                PadKey(onClick = { toast.info("Biometric") }, contentDescription = "Unlock with fingerprint") {
                    Icon(LucideIcons.FingerprintPattern, null, Modifier.size(22.dp), tint = colors.primary)
                }
            },
        )
        Text("PIN on a card surface", style = FinioType.label, color = colors.mutedForeground)
        var pin2 by remember { mutableStateOf("") }
        FinioCard { PinPad(pin2, { pin2 = it }, maxLength = 6, surface = PadSurface.Card) }
    }

    // ── Overlays ──
    FinioCard {
        SectionHeader("Overlays")
        var dialog by remember { mutableStateOf(false) }
        var alert by remember { mutableStateOf(false) }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FinioButton("Dialog", onClick = { dialog = true }, variant = ButtonVariant.Outline)
            FinioButton("Alert", onClick = { alert = true }, variant = ButtonVariant.Outline)
            FinioButton("confirm()", onClick = {
                scope.launch {
                    val ok = confirm.confirm("Delete this transaction?", "This can't be undone.", confirmLabel = "Delete")
                    if (ok) toast.success("Deleted") else toast.info("Kept")
                }
            }, variant = ButtonVariant.Outline)
            FinioButton("Success toast", onClick = {
                toast.success("Added \"Coffee\"", action = ToastAction("Undo") { toast.info("Undone") })
            }, variant = ButtonVariant.Secondary)
            FinioButton("Error toast", onClick = { toast.error("Backup failed", "Check your connection and try again.") }, variant = ButtonVariant.Secondary)
            FinioButton("Warning toast", onClick = { toast.warning("Near your Food budget") }, variant = ButtonVariant.Secondary)
        }
        if (dialog) {
            FinioDialog(
                onDismissRequest = { dialog = false },
                title = "Rename account",
                description = "Shown on every transaction row.",
                footer = {
                    FinioButton("Cancel", onClick = { dialog = false }, variant = ButtonVariant.Outline)
                    FinioButton("Save", onClick = { dialog = false })
                },
            ) {
                var v by remember { mutableStateOf("HDFC Salary") }
                FinioTextField(v, { v = it })
            }
        }
        if (alert) {
            FinioAlertDialog(
                title = "Reset all data?",
                description = "Every account, transaction and budget on this device is erased.",
                confirmLabel = "Reset",
                onConfirm = { alert = false },
                onDismissRequest = { alert = false },
            )
        }
    }

    AlertBand(icon = LucideIcons.TriangleAlert) {
        Text("Food is over budget by ₹1,200", style = FinioType.bodyMedium)
        Text("3 days left in this period", style = FinioType.caption)
    }

    FinioCard {
        EmptyState(
            title = "No transactions yet",
            description = "Log your first expense or income and it will show up here.",
            action = { FinioButton("Add transaction", onClick = {}, leadingIcon = LucideIcons.Plus) },
        )
    }

    // ── Icons + engraving ──
    FinioCard {
        SectionHeader("Lucide icons (${LucideIcons.names.size})")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            LucideIcons.names.sorted().forEach { n ->
                Icon(LucideIcons.byKebab(n)!!, n, Modifier.size(22.dp), tint = colors.foreground)
            }
        }
    }
    Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
        Guilloche(colors.primary.copy(alpha = 0.4f), Modifier.size(160.dp))
    }

    // ── Palette ──
    FinioCard {
        SectionHeader("Tokens")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(
                colors.primary, colors.accent, colors.positive, colors.warning, colors.destructive, colors.warningBand,
                colors.muted, colors.card, colors.chart1, colors.chart2, colors.chart3, colors.chart4, colors.chart5,
            ).forEach { c ->
                Box(Modifier.size(28.dp).clip(com.slowatcoding.finio.ui.theme.FinioShapes.sm).background(c))
            }
        }
    }
}

private val SampleCategories = listOf(
    Triple("cat-1", "Food", "utensils") to "#ef4444",
    Triple("cat-2", "Transport", "car") to "#f97316",
    Triple("cat-3", "Shopping", "shopping-bag") to "#8b5cf6",
    Triple("cat-4", "Entertainment", "film") to "#ec4899",
    Triple("cat-5", "Utilities", "zap") to "#06b6d4",
    Triple("cat-6", "Healthcare", "heart-pulse") to "#10b981",
    Triple("cat-7", "Education", "book-open") to "#3b82f6",
    Triple("cat-8", "Housing", "home") to "#64748b",
    Triple("cat-15", "Travel", "plane") to "#ef4444",
    Triple("cat-16", "Gifts", "gift") to "#f97316",
    Triple("cat-17", "Personal Care", "scissors") to "#8b5cf6",
    Triple("cat-18", "Subscriptions", "repeat") to "#ec4899",
    Triple("cat-19", "Vehicles", "truck") to "#06b6d4",
    Triple("cat-9", "Salary", "briefcase") to "#22c55e",
    Triple("cat-10", "Freelance", "laptop") to "#146b54",
    Triple("cat-12", "Business", "building-2") to "#a855f7",
    Triple("cat-25", "Groceries", "shopping-cart") to "#22c55e",
    Triple("cat-26", "Insurance", "umbrella") to "#0ea5e9",
    Triple("cat-27", "Loan / EMI", "banknote") to "#f43f5e",
    Triple("cat-29", "Fitness & Wellness", "dumbbell") to "#14b8a6",
    Triple("cat-30", "Pets", "paw-print") to "#f59e0b",
    Triple("x", "Unknown", "zzz") to "#94a3b8",
).map { (t, c) -> CategoryTileData(t.first, t.second, t.third, parseHexColor(c)) }

// ───────────── Previews — each primitive in light and dark ─────────────

@Composable
private fun PreviewFrame(dark: Boolean, content: @Composable () -> Unit) {
    FinioTheme(dark = dark) {
        PaperBackground {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { content() }
        }
    }
}

@Preview(name = "NoteCard · light", widthDp = 380)
@Composable
private fun NoteCardLight() = PreviewFrame(false) {
    NoteCard {
        Text("Net balance", style = FinioType.bodyMedium, color = FinioTheme.colors.mutedForeground)
        Text("₹4,82,190", style = noteFigureStyle("₹4,82,190"))
    }
}

@Preview(name = "NoteCard · dark", widthDp = 380)
@Composable
private fun NoteCardDark() = PreviewFrame(true) {
    NoteCard {
        Text("Net balance", style = FinioType.bodyMedium, color = FinioTheme.colors.mutedForeground)
        Text("₹4,82,190", style = noteFigureStyle("₹4,82,190"))
    }
}

@Preview(name = "Note tiles", widthDp = 380)
@Composable
private fun NoteTilesPreview() = Column {
    listOf(false, true).forEach { d ->
        PreviewFrame(d) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NoteTile(AccountType.Checking, "HDFC Salary", "₹42,180", {}, Modifier.width(160.dp))
                NoteTile(AccountType.Credit, "Amazon Pay", "-₹8,200", {}, Modifier.width(160.dp))
            }
        }
    }
}

@Preview(name = "Buttons", widthDp = 380)
@Composable
private fun ButtonsPreview() = Column {
    listOf(false, true).forEach { d ->
        PreviewFrame(d) {
            ButtonVariant.entries.forEach { FinioButton(it.name, onClick = {}, variant = it) }
        }
    }
}

@Preview(name = "Fields", widthDp = 380)
@Composable
private fun FieldsPreview() = Column {
    listOf(false, true).forEach { d ->
        PreviewFrame(d) {
            FinioTextField("", {}, placeholder = "Placeholder")
            FinioTextField("Invalid", {}, isError = true)
            FinioSelect<String>(null, emptyList(), {}, placeholder = "Select account")
            FinioDatePicker(LocalDate.of(2026, 10, 5), {})
        }
    }
}

@Preview(name = "Toggles", widthDp = 380)
@Composable
private fun TogglesPreview() = Column {
    listOf(false, true).forEach { d ->
        PreviewFrame(d) {
            SwitchField("Bill reminders", true, {}, description = "A day before each bill")
            SegmentedPills(listOf(0 to "Expense", 1 to "Income", 2 to "Transfer"), 0, {})
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { BudgetHealth.entries.forEach { BudgetHealthBadge(it) } }
            ThreadProgressBar(60f)
        }
    }
}

@Preview(name = "Rows", widthDp = 380)
@Composable
private fun RowsPreview() = Column {
    listOf(false, true).forEach { d ->
        PreviewFrame(d) {
            FinioCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                TransactionRow("Swiggy", "Food · HDFC", "₹450.50", TransactionType.Expense)
                FinioDivider()
                TransactionRow("Salary", "Salary · HDFC", "₹85,000", TransactionType.Income, recurring = true)
            }
        }
    }
}

@Preview(name = "Number pad", widthDp = 380)
@Composable
private fun NumberPadPreview() = Column {
    listOf(false, true).forEach { d -> PreviewFrame(d) { NumberPad("122999.5", {}) } }
}

@Preview(name = "PIN pad", widthDp = 380)
@Composable
private fun PinPadPreview() = Column {
    listOf(false, true).forEach { d ->
        PreviewFrame(d) {
            PinDots(2, 4)
            PinPad("12", {}, maxLength = 4)
        }
    }
}

@Preview(name = "Categories", widthDp = 380)
@Composable
private fun CategoriesPreview() = Column {
    listOf(false, true).forEach { d -> PreviewFrame(d) { CategoryGrid(SampleCategories, "cat-3", {}) } }
}

