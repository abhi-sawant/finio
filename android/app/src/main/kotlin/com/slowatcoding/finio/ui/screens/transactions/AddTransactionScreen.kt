package com.slowatcoding.finio.ui.screens.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import com.slowatcoding.finio.core.calc.findTransferCategory
import com.slowatcoding.finio.core.calc.isCategoryValidForType
import com.slowatcoding.finio.core.calc.miscLast
import com.slowatcoding.finio.core.format.toLocalDateTimeInputValue
import com.slowatcoding.finio.core.js.finioZone
import com.slowatcoding.finio.core.js.jsNumberToString
import com.slowatcoding.finio.core.js.nowInstant
import com.slowatcoding.finio.core.js.toIso
import com.slowatcoding.finio.core.model.CategoryRule
import com.slowatcoding.finio.core.model.TransactionSplit
import com.slowatcoding.finio.core.model.TransactionType
import com.slowatcoding.finio.core.money.roundMoney
import com.slowatcoding.finio.core.rules.findMatchingRule
import com.slowatcoding.finio.core.rules.mergeLabels
import com.slowatcoding.finio.core.share.SharedTransactionDraft
import com.slowatcoding.finio.core.store.NewTransaction
import com.slowatcoding.finio.core.util.MAX_NOTE_LENGTH
import com.slowatcoding.finio.core.util.cleanText
import com.slowatcoding.finio.ui.common.BackButton
import com.slowatcoding.finio.ui.common.collectFinanceState
import com.slowatcoding.finio.ui.common.financeStore
import com.slowatcoding.finio.ui.common.rememberMoneyFormatter
import com.slowatcoding.finio.ui.common.undoToast
import com.slowatcoding.finio.ui.components.ButtonSize
import com.slowatcoding.finio.ui.components.CategoryGrid
import com.slowatcoding.finio.ui.components.CategoryTileData
import com.slowatcoding.finio.ui.components.FieldLabel
import com.slowatcoding.finio.ui.components.FinioButton
import com.slowatcoding.finio.ui.components.FinioChip
import com.slowatcoding.finio.ui.components.FinioDateTimePicker
import com.slowatcoding.finio.ui.components.FinioIconButton
import com.slowatcoding.finio.ui.components.FinioScreen
import com.slowatcoding.finio.ui.components.FinioSelect
import com.slowatcoding.finio.ui.components.FinioTextField
import com.slowatcoding.finio.ui.components.HeaderIconButton
import com.slowatcoding.finio.ui.components.HeaderIconSpacer
import com.slowatcoding.finio.ui.components.HeaderIconTone
import com.slowatcoding.finio.ui.components.NumberPad
import com.slowatcoding.finio.ui.components.ScreenTitle
import com.slowatcoding.finio.ui.components.SegmentedPills
import com.slowatcoding.finio.ui.components.SelectOption
import com.slowatcoding.finio.ui.components.parseHexColor
import com.slowatcoding.finio.ui.components.toast
import com.slowatcoding.finio.ui.icons.LucideIcons
import com.slowatcoding.finio.ui.navigation.FinioNavigator
import com.slowatcoding.finio.ui.navigation.Routes
import com.slowatcoding.finio.ui.theme.FinioShapes
import com.slowatcoding.finio.ui.theme.FinioTheme
import com.slowatcoding.finio.ui.theme.FinioType
import java.time.LocalDateTime
import kotlin.math.abs

private data class SplitRow(val categoryId: String = "", val amount: String = "")

private data class AppliedRule(val rule: CategoryRule, val prevCategoryId: String, val prevLabels: List<String>)

private fun blankSplitRows() = listOf(SplitRow(), SplitRow())

/**
 * Port of web/src/pages/AddTransaction.tsx — routes `/add-transaction`, `/share-target` and
 * `/edit-transaction/:id` (same page, like the web).
 *
 * @param transactionId non-null on `/edit-transaction/:id` (already EditGuard-ed by the shell).
 * @param draft share-sheet / shortcut draft; seeds a blank form only, never an edit.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddTransactionScreen(nav: FinioNavigator, transactionId: String?, draft: SharedTransactionDraft?) {
    val store = financeStore()
    val state by collectFinanceState()
    val money = rememberMoneyFormatter()
    val colors = FinioTheme.colors
    val accounts = state.accounts
    val categories = state.categories
    val labels = state.labels
    val rules = state.rules

    // Snapshotted once: deleting the row from here must not flip the form into "add" mode.
    val existing = remember(transactionId) { transactionId?.let { id -> store.current.transactions.find { it.id == id } } }

    /** The OS-provided draft only ever seeds a blank form. */
    val shared = if (existing != null) null else draft

    /**
     * Auto-categorize the shared note up front, and seed `appliedRule` so the "filed by your
     * rule / Undo" hint shows — a shared transaction gets the same visible, reversible treatment.
     */
    val sharedRule = remember {
        shared?.note?.takeIf { it.isNotEmpty() }?.let { findMatchingRule(store.current.rules, it, shared.type) }
    }

    var type by remember { mutableStateOf(existing?.type ?: shared?.type ?: TransactionType.Expense) }
    var amount by remember { mutableStateOf(existing?.let { jsNumberToString(it.amount) } ?: shared?.amount ?: "") }
    var accountId by remember {
        mutableStateOf(existing?.accountId ?: store.current.accounts.firstOrNull { it.archivedAt.isNullOrEmpty() }?.id ?: "")
    }
    var toAccountId by remember { mutableStateOf(existing?.toAccountId ?: "") }
    var categoryId by remember { mutableStateOf(existing?.categoryId ?: sharedRule?.categoryId ?: "") }
    var date by remember {
        mutableStateOf(
            LocalDateTime.parse(
                existing?.let { toLocalDateTimeInputValue(it.date) } ?: toLocalDateTimeInputValue(nowInstant()),
            ),
        )
    }
    var note by remember { mutableStateOf(existing?.note ?: shared?.note ?: "") }
    var selectedLabels by remember { mutableStateOf(existing?.labels ?: mergeLabels(emptyList(), sharedRule?.labelIds ?: emptyList())) }

    var splitMode by remember { mutableStateOf(!existing?.splits.isNullOrEmpty()) }
    val splitRows = remember {
        mutableStateListOf<SplitRow>().apply {
            val splits = existing?.splits
            if (!splits.isNullOrEmpty()) addAll(splits.map { SplitRow(it.categoryId, jsNumberToString(it.amount)) }) else addAll(blankSplitRows())
        }
    }

    /**
     * Auto-categorization only fills a blank the user hasn't filled themselves. Once they touch
     * the category picker (or dismiss a suggestion) rules stop firing for this form, and editing
     * an existing transaction never triggers them at all.
     */
    val categoryTouched = remember { booleanArrayOf(false) }
    var appliedRule by remember {
        mutableStateOf(sharedRule?.let { AppliedRule(it, "", emptyList()) })
    }

    val goBack: () -> Unit = { if (!nav.back()) nav.replace(Routes.Dashboard) }

    val applyRulesToNote = { value: String, txType: TransactionType, splitting: Boolean, categoryCleared: Boolean ->
        run {
            if (existing != null || categoryTouched[0] || (txType == TransactionType.Expense && splitting)) return@run
            val rule = findMatchingRule(rules, value, txType)
            if (!categoryCleared && rule != null && appliedRule?.rule?.id == rule.id) return@run

            // Baseline is what the user had before *any* rule touched the form.
            val applied = appliedRule
            val baseCategoryId = when {
                categoryCleared -> ""
                applied != null -> applied.prevCategoryId
                else -> categoryId
            }
            val baseLabels = applied?.prevLabels ?: selectedLabels

            if (rule == null) {
                // The note no longer matches — back out rather than leave a category the user never chose.
                if (applied == null) return@run
                categoryId = baseCategoryId
                selectedLabels = baseLabels
                appliedRule = null
                return@run
            }
            categoryId = rule.categoryId
            selectedLabels = mergeLabels(baseLabels, rule.labelIds)
            appliedRule = AppliedRule(rule, baseCategoryId, baseLabels)
        }
    }

    val handleNoteChange = { value: String ->
        note = value
        applyRulesToNote(value, type, splitMode, false)
    }

    val handleTypeChange = { next: TransactionType ->
        if (next != type) {
            type = next
            // Clear anything the new type can't take, then settle which rule still applies.
            val isValid = { id: String -> categories.find { it.id == id }?.let { isCategoryValidForType(it, next) } == true }
            val staleCategory = categoryId.isNotEmpty() && !isValid(categoryId)
            if (staleCategory) categoryId = ""
            for (i in splitRows.indices) {
                val r = splitRows[i]
                if (r.categoryId.isNotEmpty() && !isValid(r.categoryId)) splitRows[i] = r.copy(categoryId = "")
            }
            val hadRule = appliedRule != null
            applyRulesToNote(note, next, splitMode, staleCategory)
            // No rule re-filed it: a leftover hint's Undo would restore a category of the old type.
            if (staleCategory && hadRule && findMatchingRule(rules, note, next) == null) appliedRule = null
        }
    }

    val dismissAppliedRule = {
        appliedRule?.let {
            categoryId = it.prevCategoryId
            selectedLabels = it.prevLabels
            appliedRule = null
            categoryTouched[0] = true
        }
    }

    val chooseCategory = { id: String ->
        categoryTouched[0] = true
        appliedRule = null
        categoryId = id
    }

    val noteSuggestions = remember(state.transactions) {
        val seen = LinkedHashSet<String>()
        for (t in state.transactions) {
            val n = t.note.trim()
            if (n.isNotEmpty()) seen += n
        }
        seen.toList()
    }

    // Archived accounts are hidden, but keep one an existing transaction already sits on.
    val selectableAccounts = remember(accounts, existing) {
        val inUse = setOfNotNull(existing?.accountId, existing?.toAccountId?.takeIf { it.isNotEmpty() })
        accounts.filter { it.archivedAt.isNullOrEmpty() || it.id in inUse }
    }

    val filteredCategories = remember(categories, type) { miscLast(categories.filter { isCategoryValidForType(it, type) }) }

    val useSplits = type == TransactionType.Expense && splitMode
    val splitTotal = splitRows.sumOf { parseFloatJs(it.amount).takeUnless(Double::isNaN) ?: 0.0 }
    val splitRemaining = roundMoney((parseFloatJs(amount).takeUnless(Double::isNaN) ?: 0.0) - splitTotal)

    fun splitRowIssue(): String? {
        if (splitRows.any { it.categoryId.isEmpty() }) return "Pick a category for every split row"
        if (splitRows.any { it.amount.isBlank() || parseFloatJs(it.amount).isNaN() }) return "Enter an amount for every split row"
        if (splitRows.any { parseFloatJs(it.amount) <= 0 }) return "Split amounts must be greater than zero"
        return null
    }
    val hasNonPositiveSplit = splitRows.any { it.amount.trim().isNotEmpty() && parseFloatJs(it.amount) <= 0 }

    val handleRemoveSplitRow = { idx: Int ->
        if (splitRows.size <= 2) {
            // Down to one row is just an unsplit category — fold back to the plain picker.
            val keep = splitRows.getOrNull(if (idx == 0) 1 else 0)
            categoryId = keep?.categoryId ?: ""
            splitRows.clear()
            splitRows.addAll(blankSplitRows())
            splitMode = false
        } else {
            splitRows.removeAt(idx)
        }
    }

    val toggleSplitMode = {
        // Either direction is the user taking charge of the category.
        categoryTouched[0] = true
        appliedRule = null
        if (splitMode) {
            splitRows.firstOrNull { it.categoryId.isNotEmpty() }?.let { categoryId = it.categoryId }
            splitMode = false
        } else {
            if (splitRows.none { it.categoryId.isNotEmpty() || it.amount.isNotEmpty() }) {
                splitRows.clear()
                splitRows.addAll(listOf(SplitRow(categoryId, ""), SplitRow()))
            }
            splitMode = true
        }
    }

    val submitting = remember { booleanArrayOf(false) }

    val handleSubmit = submit@{
        if (submitting[0]) return@submit
        val parsedAmount = parseFloatJs(amount)
        if (parsedAmount.isNaN() || parsedAmount <= 0) {
            toast.error("Enter an amount")
            return@submit
        }
        if (accountId.isEmpty()) {
            toast.error("Select an account")
            return@submit
        }
        if (type == TransactionType.Transfer) {
            if (toAccountId.isEmpty()) {
                toast.error("Select a destination account")
                return@submit
            }
            if (toAccountId == accountId) {
                toast.error("Source and destination must differ")
                return@submit
            }
        } else if (useSplits) {
            val issue = splitRowIssue()
            if (issue != null) {
                toast.error(issue)
                return@submit
            }
            if (abs(splitRemaining) > 0.01) {
                toast.error(
                    if (splitRemaining > 0) "${money(splitRemaining)} left to allocate" else "${money(-splitRemaining)} over the total",
                )
                return@submit
            }
        } else if (categoryId.isEmpty() || filteredCategories.none { it.id == categoryId }) {
            toast.error("Select a category")
            return@submit
        }

        submitting[0] = true
        val transferCategory = findTransferCategory(categories)
        val finalCategoryId = when {
            type == TransactionType.Transfer -> transferCategory?.id ?: categoryId
            useSplits -> ""
            else -> categoryId
        }
        val isoDate = date.atZone(finioZone).toInstant().toIso()
        val cleanNote = cleanText(note, MAX_NOTE_LENGTH)
        val splits = if (useSplits) splitRows.map { TransactionSplit(it.categoryId, roundMoney(parseFloatJs(it.amount))) } else null
        val to = if (type == TransactionType.Transfer) toAccountId else null

        if (existing != null) {
            store.updateTransaction(existing.id) {
                it.copy(
                    type = type, amount = parsedAmount, accountId = accountId, toAccountId = to,
                    categoryId = finalCategoryId, date = isoDate, note = cleanNote, labels = selectedLabels, splits = splits,
                )
            }
            toast.success("Transaction updated")
        } else {
            store.addTransaction(
                NewTransaction(
                    type = type, amount = parsedAmount, accountId = accountId, toAccountId = to,
                    categoryId = finalCategoryId, date = isoDate, note = cleanNote, labels = selectedLabels, splits = splits,
                ),
            )
            toast.success("Transaction added")
        }
        goBack()
    }

    val handleDelete: () -> Unit = {
        // Cheap and fully reversible, so undo beats a confirm prompt here (as on the web).
        existing?.let { tx ->
            val removed = store.deleteTransaction(tx.id)
            if (removed != null) {
                goBack()
                undoToast("Transaction deleted") { store.restoreTransaction(removed) }
            }
        }
    }

    Box(Modifier.fillMaxSize().imePadding()) {
        FinioScreen(
            header = {
                BackButton(nav, onBack = goBack)
                ScreenTitle(if (existing != null) "Edit transaction" else "Add transaction")
                if (existing != null) {
                    HeaderIconButton(LucideIcons.Trash2, "Delete", onClick = handleDelete, tone = HeaderIconTone.Destructive)
                } else {
                    HeaderIconSpacer()
                }
            },
        ) {
            // Type selector — one lavender selected state for every type.
            SegmentedPills(
                options = listOf(TransactionType.Expense to "Expense", TransactionType.Income to "Income", TransactionType.Transfer to "Transfer"),
                selected = type,
                onSelect = handleTypeChange,
            )

            Column {
                FieldLabel("Amount")
                NumberPad(value = amount, onValueChange = { amount = it })
            }

            Column {
                FieldLabel(if (type == TransactionType.Transfer) "From account" else "Account")
                FinioSelect(
                    value = accountId.ifEmpty { null },
                    options = accountOptions(selectableAccounts),
                    onValueChange = { accountId = it },
                    placeholder = "Select account",
                )
                if (selectableAccounts.isEmpty()) {
                    Text(
                        buildAnnotatedString {
                            append("You need an account before you can record a transaction. ")
                            withLink(
                                LinkAnnotation.Clickable(
                                    "add-account",
                                    TextLinkStyles(SpanStyle(fontWeight = FontWeight.Medium, textDecoration = TextDecoration.Underline)),
                                ) { nav.navigate(Routes.AddAccount) },
                            ) { append("Add an account") }
                        },
                        Modifier
                            .padding(top = 8.dp)
                            .fillMaxWidth()
                            .clip(FinioShapes.md)
                            .background(colors.warningBand)
                            .padding(12.dp),
                        style = FinioType.caption,
                        color = colors.warningBandForeground,
                    )
                }
            }

            if (type == TransactionType.Transfer) {
                Column {
                    FieldLabel("To account")
                    FinioSelect(
                        value = toAccountId.ifEmpty { null },
                        options = accountOptions(selectableAccounts.filter { it.id != accountId }),
                        onValueChange = { toAccountId = it },
                        placeholder = "Select account",
                    )
                }
            }

            if (type != TransactionType.Transfer) {
                Column {
                    Row(
                        Modifier.fillMaxWidth().padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FieldLabel("Category", bottomGap = 0.dp)
                        if (type == TransactionType.Expense) {
                            FinioChip("Split", selected = splitMode, onClick = toggleSplitMode, leadingIcon = LucideIcons.Split)
                        }
                    }

                    if (splitMode) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            splitRows.forEachIndexed { idx, row ->
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    FinioSelect(
                                        value = row.categoryId.ifEmpty { null },
                                        options = filteredCategories.map { SelectOption(it.id, it.name) },
                                        onValueChange = { v -> splitRows[idx] = splitRows[idx].copy(categoryId = v) },
                                        modifier = Modifier.weight(1f),
                                        placeholder = "Category",
                                    )
                                    FinioTextField(
                                        value = row.amount,
                                        onValueChange = { v ->
                                            if (v.all { it.isDigit() || it == '.' }) splitRows[idx] = splitRows[idx].copy(amount = v)
                                        },
                                        modifier = Modifier.width(96.dp),
                                        placeholder = "Amount",
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    )
                                    FinioIconButton(
                                        LucideIcons.X,
                                        "Remove split",
                                        onClick = { handleRemoveSplitRow(idx) },
                                        size = ButtonSize.IconSm,
                                        tint = colors.mutedForeground,
                                    )
                                }
                            }
                            Row(
                                Modifier.clip(FinioShapes.sm).clickable(role = Role.Button) { splitRows.add(SplitRow()) },
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(LucideIcons.Plus, null, Modifier.size(14.dp), tint = colors.primary)
                                Text("Add split", style = FinioType.label, color = colors.primary)
                            }
                            val allocated = abs(splitRemaining) < 0.01
                            Text(
                                when {
                                    hasNonPositiveSplit -> "Split amounts must be greater than zero"
                                    allocated -> "Fully allocated"
                                    splitRemaining > 0 -> "${money(splitRemaining)} left to allocate"
                                    else -> "${money(-splitRemaining)} over the total"
                                },
                                style = FinioType.caption,
                                color = if (allocated && !hasNonPositiveSplit) colors.mutedForeground else colors.destructive,
                            )
                        }
                    } else {
                        CategoryGrid(
                            categories = filteredCategories.map { CategoryTileData(it.id, it.name, it.icon, parseHexColor(it.color)) },
                            selectedId = categoryId.ifEmpty { null },
                            onSelect = chooseCategory,
                        )
                    }
                }
            }

            Column {
                FieldLabel("Date & time")
                FinioDateTimePicker(value = date, onValueChange = { date = it })
            }

            Column {
                FieldLabel("Note")
                val noteInteraction = remember { MutableInteractionSource() }
                val noteFocused by noteInteraction.collectIsFocusedAsState()
                FinioTextField(
                    value = note,
                    onValueChange = { v -> if (withinLength(v, MAX_NOTE_LENGTH)) handleNoteChange(v) },
                    placeholder = "Add a note...",
                    interactionSource = noteInteraction,
                )
                // The web's <datalist>: earlier notes that contain what has been typed.
                val query = note.trim().lowercase()
                val matches = if (noteFocused && query.isNotEmpty()) {
                    noteSuggestions.filter { it.lowercase().contains(query) && it != note }.take(5)
                } else {
                    emptyList()
                }
                if (matches.isNotEmpty()) {
                    NoteSuggestions(matches, onPick = handleNoteChange)
                }
                appliedRule?.let { applied ->
                    Row(
                        Modifier.padding(top = 6.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(LucideIcons.Wand2, null, Modifier.size(12.dp), tint = colors.primary)
                        Text(
                            "Filed as ${categories.find { it.id == applied.rule.categoryId }?.name} by your \"${applied.rule.pattern}\" rule",
                            Modifier.weight(1f),
                            style = FinioType.caption,
                            color = colors.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            "Undo",
                            Modifier.clickable(role = Role.Button) { dismissAppliedRule() },
                            style = FinioType.caption.copy(textDecoration = TextDecoration.Underline),
                            color = colors.mutedForeground,
                        )
                    }
                }
            }

            if (labels.isNotEmpty()) {
                Column {
                    FieldLabel("Labels")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        labels.forEach { label ->
                            val active = label.id in selectedLabels
                            LabelToggle(label.name, parseHexColor(label.color), active) {
                                selectedLabels = if (active) selectedLabels - label.id else selectedLabels + label.id
                            }
                        }
                    }
                }
            }
        }

        // Fixed glass submit bar.
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(colors.glassStrong)
                .drawBehind { drawLine(colors.glassBorder, Offset(0f, 0.5.dp.toPx()), Offset(size.width, 0.5.dp.toPx()), 1.dp.toPx()) }
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(12.dp),
            contentAlignment = Alignment.Center,
        ) {
            FinioButton(
                if (existing != null) "Update transaction" else "Add transaction",
                onClick = handleSubmit,
                modifier = Modifier.widthIn(max = 512.dp).fillMaxWidth(),
                size = ButtonSize.Lg,
            )
        }
    }
}

/** A label toggle: muted when off; on, a soft tint of the label's colour with its colour as the border. */
@Composable
private fun LabelToggle(name: String, color: Color, active: Boolean, onClick: () -> Unit) {
    val colors = FinioTheme.colors
    Row(
        Modifier
            .clip(FinioShapes.full)
            .background(if (active) color.copy(alpha = 0.18f) else colors.muted)
            .border(1.dp, if (active) color else Color.Transparent, FinioShapes.full)
            .semantics { selected = active }
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(8.dp).clip(FinioShapes.full).background(color))
        Text(name, style = FinioType.label, color = if (active) colors.foreground else colors.mutedForeground)
    }
}

/** The `<datalist>` stand-in: up to five earlier notes under the field, tap to use one. */
@Composable
private fun NoteSuggestions(matches: List<String>, onPick: (String) -> Unit) {
    val colors = FinioTheme.colors
    Column(
        Modifier
            .padding(top = 4.dp)
            .fillMaxWidth()
            .clip(FinioShapes.md)
            .background(colors.popover)
            .border(1.dp, colors.border, FinioShapes.md)
            .padding(4.dp),
    ) {
        matches.forEach { m ->
            Text(
                m,
                Modifier
                    .fillMaxWidth()
                    .clip(FinioShapes.sm)
                    .clickable { onPick(m) }
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                style = FinioType.body,
                color = colors.popoverForeground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
