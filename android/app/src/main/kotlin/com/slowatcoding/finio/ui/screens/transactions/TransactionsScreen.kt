package com.slowatcoding.finio.ui.screens.transactions

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.slowatcoding.finio.core.calc.DateGroup
import com.slowatcoding.finio.core.calc.activeAccounts
import com.slowatcoding.finio.core.calc.buildSearchIndex
import com.slowatcoding.finio.core.calc.groupTransactionsByDate
import com.slowatcoding.finio.core.calc.isCategoryValidForType
import com.slowatcoding.finio.core.calc.miscLast
import com.slowatcoding.finio.core.calc.transactionMatchesQuery
import com.slowatcoding.finio.core.calc.transactionsToCsv
import com.slowatcoding.finio.core.format.formatDate
import com.slowatcoding.finio.core.format.todayKey
import com.slowatcoding.finio.core.js.finioZone
import com.slowatcoding.finio.core.js.nowInstant
import com.slowatcoding.finio.core.js.parseJsDate
import com.slowatcoding.finio.core.js.toIso
import com.slowatcoding.finio.core.model.Transaction
import com.slowatcoding.finio.core.model.TransactionType
import com.slowatcoding.finio.core.store.NewTemplate
import com.slowatcoding.finio.core.store.NewTransaction
import com.slowatcoding.finio.core.util.MAX_NAME_LENGTH
import com.slowatcoding.finio.core.util.cleanText
import com.slowatcoding.finio.core.util.getErrorMessage
import com.slowatcoding.finio.core.util.isRangeInverted
import com.slowatcoding.finio.core.util.stripLeading
import com.slowatcoding.finio.platform.files.ExportResult
import com.slowatcoding.finio.platform.files.FinioMime
import com.slowatcoding.finio.platform.files.rememberDocumentExporter
import com.slowatcoding.finio.ui.common.HideAmountsToggle
import com.slowatcoding.finio.ui.common.SectionLoader
import com.slowatcoding.finio.ui.common.collectFinanceState
import com.slowatcoding.finio.ui.common.financeStore
import com.slowatcoding.finio.ui.common.rememberDerived
import com.slowatcoding.finio.ui.common.rememberMoneyFormatter
import com.slowatcoding.finio.ui.common.undoToast
import com.slowatcoding.finio.ui.components.ButtonSize
import com.slowatcoding.finio.ui.components.ButtonVariant
import com.slowatcoding.finio.ui.components.ContentMaxWidth
import com.slowatcoding.finio.ui.components.EmptyState
import com.slowatcoding.finio.ui.components.FieldLabel
import com.slowatcoding.finio.ui.components.FinioButton
import com.slowatcoding.finio.ui.components.FinioCard
import com.slowatcoding.finio.ui.components.FinioChip
import com.slowatcoding.finio.ui.components.FinioDatePicker
import com.slowatcoding.finio.ui.components.FinioDialog
import com.slowatcoding.finio.ui.components.FinioDivider
import com.slowatcoding.finio.ui.components.FinioHeader
import com.slowatcoding.finio.ui.components.FinioIconButton
import com.slowatcoding.finio.ui.components.FinioSelect
import com.slowatcoding.finio.ui.components.FinioTextField
import com.slowatcoding.finio.ui.components.HeaderIconButton
import com.slowatcoding.finio.ui.components.PageTitle
import com.slowatcoding.finio.ui.components.SelectOption
import com.slowatcoding.finio.ui.components.headerScrolled
import com.slowatcoding.finio.ui.components.isWideLayout
import com.slowatcoding.finio.ui.components.toast
import com.slowatcoding.finio.ui.icons.LucideIcons
import com.slowatcoding.finio.ui.navigation.FinioNavigator
import com.slowatcoding.finio.ui.navigation.Routes
import com.slowatcoding.finio.ui.shell.SuppressFab
import com.slowatcoding.finio.ui.theme.FinioShapes
import com.slowatcoding.finio.ui.theme.FinioTheme
import com.slowatcoding.finio.ui.theme.FinioType
import com.slowatcoding.finio.ui.theme.cssShadow
import com.slowatcoding.finio.ui.theme.mix
import kotlinx.coroutines.delay
import java.time.LocalDate

private const val ALL = "all"

/** The filtered ledger and its derived views, computed off the main thread. */
private class FilteredLedger(val filtered: List<Transaction>, val groups: List<DateGroup>, val income: Double, val expense: Double)

private fun parseDay(s: String): LocalDate? = if (s.isEmpty()) null else runCatching { LocalDate.parse(s) }.getOrNull()

/**
 * Port of web/src/pages/Transactions.tsx — route `/transactions` (tab): debounced search, the
 * Earned/Spent totals, the filter card, the day-grouped list (one glass card per day), the row
 * long-press menu, bulk selection with its floating glass toolbar, and CSV export.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TransactionsScreen(nav: FinioNavigator) {
    val store = financeStore()
    val state by collectFinanceState()
    val money = rememberMoneyFormatter()
    val colors = FinioTheme.colors
    val transactions = state.transactions
    val categories = state.categories
    val accounts = state.accounts
    val labels = state.labels

    var search by rememberSaveable { mutableStateOf("") }
    var debouncedSearch by rememberSaveable { mutableStateOf("") }
    var typeFilter by rememberSaveable { mutableStateOf(ALL) }
    var accountFilter by rememberSaveable { mutableStateOf(ALL) }
    var categoryFilter by rememberSaveable { mutableStateOf(ALL) }
    var labelFilter by rememberSaveable { mutableStateOf(ALL) }
    var fromDate by rememberSaveable { mutableStateOf("") }
    var toDate by rememberSaveable { mutableStateOf("") }
    var showFilters by rememberSaveable { mutableStateOf(false) }

    val filterableAccounts = remember(accounts) { activeAccounts(accounts) }
    val categoryById = remember(categories) { categories.associateBy { it.id } }
    val accountById = remember(accounts) { accounts.associateBy { it.id } }
    val labelById = remember(labels) { labels.associateBy { it.id } }

    // Debounce the search query — filtering the full list on every keystroke is fine at
    // hundreds of rows but not at thousands.
    LaunchedEffect(search) {
        delay(200)
        debouncedSearch = search
    }

    val searchIndex = remember(categories, accounts, labels) { buildSearchIndex(categories, accounts, labels) }

    // Bulk selection mode, entered via a row's long-press menu.
    var selectionMode by rememberSaveable { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    SuppressFab(active = selectionMode)

    var templateTx by remember { mutableStateOf<Transaction?>(null) }
    var templateName by remember { mutableStateOf("") }
    var recategorizeOpen by remember { mutableStateOf(false) }
    var recategorizeCategoryId by remember { mutableStateOf("") }
    var addLabelOpen by remember { mutableStateOf(false) }
    var addLabelId by remember { mutableStateOf("") }

    val ledger = rememberDerived(
        transactions, debouncedSearch, typeFilter, accountFilter, categoryFilter, labelFilter, searchIndex, fromDate, toDate,
    ) {
        val q = debouncedSearch.trim()
        val fromMs = parseDay(fromDate)?.atStartOfDay(finioZone)?.toInstant()?.toEpochMilli()
        val toMs = parseDay(toDate)?.atTime(23, 59, 59)?.atZone(finioZone)?.toInstant()?.toEpochMilli()
        val filtered = transactions.filter { t ->
            if (typeFilter != ALL && t.type.wire != typeFilter) return@filter false
            if (accountFilter != ALL && t.accountId != accountFilter && t.toAccountId != accountFilter) return@filter false
            if (categoryFilter != ALL) {
                val inSplits = t.splits?.any { it.categoryId == categoryFilter } == true
                if (t.categoryId != categoryFilter && !inSplits) return@filter false
            }
            if (labelFilter != ALL && labelFilter !in t.labels) return@filter false
            if (fromMs != null || toMs != null) {
                // An unparseable date compares false either way (NaN), like the web.
                val ts = parseJsDate(t.date)?.toEpochMilli()
                if (ts != null) {
                    if (fromMs != null && ts < fromMs) return@filter false
                    if (toMs != null && ts > toMs) return@filter false
                }
            }
            transactionMatchesQuery(t, q, searchIndex)
        }
        var income = 0.0
        var expense = 0.0
        for (t in filtered) {
            if (t.type == TransactionType.Income) income += t.amount
            else if (t.type == TransactionType.Expense) expense += t.amount
        }
        FilteredLedger(filtered, groupTransactionsByDate(filtered), income, expense)
    }

    // What the selection can be recategorized to. Transfers keep their Transfer category, so
    // they're skipped; a mixed expense + income selection can only move to a neutral category.
    val categorizable = remember(recategorizeOpen, transactions, selectedIds) {
        if (!recategorizeOpen) return@remember Triple(emptyList<String>(), 0, emptySet<TransactionType>())
        val ids = mutableListOf<String>()
        val types = mutableSetOf<TransactionType>()
        var skipped = 0
        for (t in transactions) {
            if (t.id !in selectedIds) continue
            if (t.type == TransactionType.Transfer) {
                skipped++
                continue
            }
            ids += t.id
            types += t.type
        }
        Triple(ids.toList(), skipped, types.toSet())
    }
    val recategorizeOptions = remember(categorizable, categories) {
        val types = categorizable.third
        if (types.isEmpty()) {
            emptyList()
        } else {
            miscLast(categories).filter { c ->
                if (types.size == 1) {
                    isCategoryValidForType(c, if (TransactionType.Income in types) TransactionType.Income else TransactionType.Expense)
                } else {
                    isCategoryValidForType(c, TransactionType.Expense) && isCategoryValidForType(c, TransactionType.Income)
                }
            }
        }
    }

    val hasActiveFilters = typeFilter != ALL || accountFilter != ALL || categoryFilter != ALL ||
        labelFilter != ALL || fromDate.isNotEmpty() || toDate.isNotEmpty()

    var exportCount by remember { mutableIntStateOf(0) }
    val exporter = rememberDocumentExporter(FinioMime.CSV) { result ->
        when (result) {
            is ExportResult.Saved -> toast.success("Exported $exportCount transactions")
            is ExportResult.Failed -> toast.error(getErrorMessage(result.error, "Export failed"))
            ExportResult.Cancelled -> Unit
        }
    }
    val handleExportCsv: () -> Unit = {
        val filtered = ledger?.filtered.orEmpty()
        if (filtered.isEmpty()) {
            toast.error("No transactions to export")
        } else {
            exportCount = filtered.size
            val cats = categories
            val accs = accounts
            exporter.export("finio-transactions-${todayKey()}.csv") {
                // Newest first, the same order as the list on screen.
                val sorted = filtered.sortedWith { a, b ->
                    val ta = parseJsDate(a.date)?.toEpochMilli() ?: 0L
                    val tb = parseJsDate(b.date)?.toEpochMilli() ?: 0L
                    if (tb != ta) tb.compareTo(ta) else b.createdAt.compareTo(a.createdAt)
                }
                transactionsToCsv(sorted, cats, accs)
            }
        }
    }

    val exitSelectionMode = {
        selectionMode = false
        selectedIds = emptySet()
    }
    BackHandler(enabled = selectionMode) { exitSelectionMode() }

    val handleRowAction = { action: TransactionRowAction, tx: Transaction ->
        when (action) {
            TransactionRowAction.Select -> {
                selectionMode = true
                selectedIds = setOf(tx.id)
            }
            TransactionRowAction.Duplicate -> {
                // A duplicate is a fresh, manually-entered transaction — dated now, not linked to
                // whatever recurring rule (if any) generated the original.
                val newId = store.addTransaction(
                    NewTransaction(
                        type = tx.type,
                        amount = tx.amount,
                        accountId = tx.accountId,
                        toAccountId = tx.toAccountId?.takeIf { it.isNotEmpty() },
                        categoryId = tx.categoryId,
                        date = nowInstant().toIso(),
                        note = tx.note,
                        labels = tx.labels,
                        splits = tx.splits,
                    ),
                )
                undoToast("Transaction duplicated") { store.deleteTransaction(newId) }
            }
            TransactionRowAction.Template -> {
                templateName = tx.note
                templateTx = tx
            }
            TransactionRowAction.Delete -> {
                val removed = store.deleteTransaction(tx.id)
                if (removed != null) undoToast("Transaction deleted") { store.restoreTransaction(removed) }
            }
        }
        Unit
    }

    val clearFilters = {
        typeFilter = ALL
        accountFilter = ALL
        categoryFilter = ALL
        labelFilter = ALL
        fromDate = ""
        toDate = ""
    }

    val wide = isWideLayout()
    val density = LocalDensity.current
    var headerHeight by remember { mutableIntStateOf(0) }
    val listState = rememberLazyListState()
    val side = if (wide) 32.dp else 12.dp

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = side,
                end = side,
                top = with(density) { headerHeight.toDp() } + 8.dp,
                bottom = if (wide) 32.dp else 160.dp,
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val column = Modifier.widthIn(max = ContentMaxWidth - side * 2).fillMaxWidth()

            item("search") {
                FinioTextField(
                    value = search,
                    onValueChange = { search = it },
                    modifier = column,
                    placeholder = "Search transactions",
                    leading = { Icon(LucideIcons.Search, "Search notes, categories, accounts, labels and amounts", Modifier.size(16.dp), tint = colors.mutedForeground) },
                )
            }

            item("totals") {
                Row(column.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)) {
                    Text(
                        buildAnnotatedString {
                            append("Earned ")
                            withStyle(SpanStyle(color = colors.positive, fontWeight = FontWeight.Bold)) { append(money(ledger?.income ?: 0.0)) }
                        },
                        style = FinioType.caption,
                        color = colors.mutedForeground,
                    )
                    Text(
                        buildAnnotatedString {
                            append("Spent ")
                            withStyle(SpanStyle(color = colors.foreground, fontWeight = FontWeight.Bold)) { append(money(ledger?.expense ?: 0.0)) }
                        },
                        style = FinioType.caption,
                        color = colors.mutedForeground,
                    )
                }
            }

            if (showFilters) {
                item("filters") {
                    FinioCard(column.padding(top = 16.dp), contentPadding = PaddingValues(12.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column {
                                FieldLabel("Type")
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf(ALL to "All", "expense" to "Expense", "income" to "Income", "transfer" to "Transfer").forEach { (value, label) ->
                                        FinioChip(label, selected = typeFilter == value, onClick = { typeFilter = value })
                                    }
                                }
                            }
                            Column {
                                FieldLabel("Account")
                                FinioSelect(
                                    value = accountFilter,
                                    options = listOf(SelectOption(ALL, "All accounts")) + filterableAccounts.map { SelectOption(it.id, it.name) },
                                    onValueChange = { accountFilter = it },
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Column(Modifier.weight(1f)) {
                                    FieldLabel("Category")
                                    FinioSelect(
                                        value = categoryFilter,
                                        options = listOf(SelectOption(ALL, "All categories")) + miscLast(categories).map { SelectOption(it.id, it.name) },
                                        onValueChange = { categoryFilter = it },
                                    )
                                }
                                Column(Modifier.weight(1f)) {
                                    FieldLabel("Label")
                                    FinioSelect(
                                        value = labelFilter,
                                        options = listOf(SelectOption(ALL, "All labels")) + labels.map { SelectOption(it.id, it.name) },
                                        onValueChange = { labelFilter = it },
                                    )
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Column(Modifier.weight(1f)) {
                                    FieldLabel("From")
                                    FinioDatePicker(
                                        value = parseDay(fromDate),
                                        onValueChange = { fromDate = it.toString() },
                                        placeholder = "Start date",
                                        maxDate = parseDay(toDate),
                                    )
                                }
                                Column(Modifier.weight(1f)) {
                                    FieldLabel("To")
                                    FinioDatePicker(
                                        value = parseDay(toDate),
                                        onValueChange = { toDate = it.toString() },
                                        placeholder = "End date",
                                        minDate = parseDay(fromDate),
                                    )
                                }
                            }
                            if (isRangeInverted(fromDate, toDate)) {
                                Text(
                                    "The from date is after the to date — no transactions can match.",
                                    style = FinioType.caption,
                                    color = colors.destructive,
                                )
                            }
                            if (hasActiveFilters) {
                                Row(
                                    Modifier.clip(FinioShapes.sm).clickable { clearFilters() },
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(LucideIcons.X, null, Modifier.size(12.dp), tint = colors.destructive)
                                    Text("Clear filters", style = FinioType.label, color = colors.destructive)
                                }
                            }
                        }
                    }
                }
            }

            item("gap") { Spacer(Modifier.height(16.dp)) }

            when {
                ledger == null -> item("loading") { SectionLoader(column) }
                ledger.groups.isEmpty() && transactions.isEmpty() -> item("empty") {
                    EmptyState(
                        title = "No transactions yet",
                        modifier = column,
                        description = "Log your first expense or income and it will show up here.",
                        action = {
                            FinioButton("Add transaction", onClick = { nav.navigate(Routes.AddTransaction()) }, leadingIcon = LucideIcons.Plus)
                        },
                    )
                }
                ledger.groups.isEmpty() -> item("no-match") {
                    EmptyState(
                        title = "No matching transactions",
                        modifier = column,
                        description = "Nothing matches your search or filters.",
                        action = {
                            FinioButton(
                                "Clear search and filters",
                                onClick = {
                                    clearFilters()
                                    search = ""
                                    debouncedSearch = ""
                                },
                                variant = ButtonVariant.Outline,
                                leadingIcon = LucideIcons.X,
                            )
                        },
                    )
                }
                else -> items(ledger.groups, key = { it.date }) { group ->
                    Column(column.padding(bottom = 12.dp)) {
                        Text(
                            formatDate(group.date),
                            Modifier.padding(start = 8.dp, bottom = 6.dp).semantics { heading() },
                            style = FinioType.label,
                            color = colors.mutedForeground,
                        )
                        FinioCard(contentPadding = PaddingValues(0.dp)) {
                            group.transactions.forEachIndexed { i, tx ->
                                if (i > 0) FinioDivider()
                                TransactionItem(
                                    transaction = tx,
                                    categories = categoryById,
                                    accounts = accountById,
                                    labels = labelById,
                                    money = money,
                                    selectionMode = selectionMode,
                                    selected = tx.id in selectedIds,
                                    onLongPressAction = handleRowAction,
                                    onClick = {
                                        if (selectionMode) {
                                            selectedIds = if (tx.id in selectedIds) selectedIds - tx.id else selectedIds + tx.id
                                        } else {
                                            nav.navigate(Routes.EditTransaction(tx.id))
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }

        FinioHeader(
            scrolled = listState.headerScrolled(),
            modifier = Modifier.onSizeChanged { headerHeight = it.height },
        ) {
            PageTitle("Transactions")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HideAmountsToggle()
                HeaderIconButton(LucideIcons.Download, "Export CSV", onClick = handleExportCsv)
                HeaderIconButton(LucideIcons.Filter, "Toggle filters", onClick = { showFilters = !showFilters }, pressed = hasActiveFilters)
            }
        }

        if (selectionMode) {
            SelectionToolbar(
                count = selectedIds.size,
                onAddLabel = { addLabelOpen = true },
                onRecategorize = { recategorizeOpen = true },
                onDelete = {
                    val removed = store.bulkDeleteTransactions(selectedIds.toList())
                    exitSelectionMode()
                    if (removed.isNotEmpty()) {
                        undoToast("Deleted ${removed.size} transaction${if (removed.size == 1) "" else "s"}") {
                            store.restoreTransactions(removed)
                        }
                    }
                },
                onCancel = exitSelectionMode,
                modifier = Modifier.align(Alignment.BottomStart),
            )
        }
    }

    // Save as template
    templateTx?.let { tx ->
        val close = {
            templateTx = null
            templateName = ""
        }
        val focus = remember { FocusRequester() }
        LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
        FinioDialog(
            onDismissRequest = { templateTx = null },
            title = "Save as template",
            description = "Reuse this transaction's account, category, amount and labels from the FAB later.",
            footer = {
                FinioButton("Cancel", onClick = { templateTx = null }, variant = ButtonVariant.Outline)
                FinioButton("Save", onClick = {
                    store.addTemplate(
                        NewTemplate(
                            name = cleanText(templateName, MAX_NAME_LENGTH).ifEmpty { cleanText(tx.note, MAX_NAME_LENGTH) }.ifEmpty { "Template" },
                            type = tx.type,
                            amount = tx.amount,
                            accountId = tx.accountId,
                            toAccountId = tx.toAccountId?.takeIf { it.isNotEmpty() },
                            categoryId = tx.categoryId,
                            note = tx.note,
                            labels = tx.labels,
                            splits = tx.splits,
                        ),
                    )
                    toast.success("Template saved")
                    close()
                })
            },
        ) {
            FinioTextField(
                value = templateName,
                onValueChange = { v -> stripLeading(v).let { if (withinLength(it, MAX_NAME_LENGTH)) templateName = it } },
                modifier = Modifier.focusRequester(focus),
                placeholder = "Template name",
            )
        }
    }

    // Bulk recategorize
    if (recategorizeOpen) {
        val (ids, skipped, types) = categorizable
        val close = { recategorizeOpen = false }
        FinioDialog(
            onDismissRequest = close,
            title = "Recategorize ${ids.size} transaction${if (ids.size == 1) "" else "s"}",
            description = when {
                ids.isEmpty() -> "Transfers keep their Transfer category, so there is nothing here to recategorize."
                types.size > 1 -> "Your selection mixes expenses and income, so only categories that fit both are offered."
                else -> "Every selected transaction moves to this category."
            },
            footer = {
                FinioButton("Cancel", onClick = close, variant = ButtonVariant.Outline)
                FinioButton(
                    "Apply",
                    onClick = {
                        if (recategorizeCategoryId.isNotEmpty() && ids.isNotEmpty()) {
                            store.bulkRecategorize(ids, recategorizeCategoryId)
                            val count = ids.size
                            toast.success(
                                "Recategorized $count transaction${if (count == 1) "" else "s"}" +
                                    if (skipped > 0) " · $skipped transfer${if (skipped == 1) "" else "s"} skipped" else "",
                            )
                            recategorizeOpen = false
                            recategorizeCategoryId = ""
                            exitSelectionMode()
                        }
                    },
                    enabled = recategorizeCategoryId.isNotEmpty() && recategorizeOptions.any { it.id == recategorizeCategoryId },
                )
            },
        ) {
            if (skipped > 0) {
                Text(
                    "$skipped transfer${if (skipped == 1) "" else "s"} in your selection will be skipped.",
                    Modifier.fillMaxWidth().clip(FinioShapes.md).background(colors.warning.mix(0.15f)).padding(horizontal = 12.dp, vertical = 8.dp),
                    style = FinioType.caption,
                    color = colors.foreground,
                )
            }
            FinioSelect(
                value = recategorizeCategoryId.ifEmpty { null },
                options = recategorizeOptions.map { SelectOption(it.id, it.name) },
                onValueChange = { recategorizeCategoryId = it },
                placeholder = "Select category",
                enabled = recategorizeOptions.isNotEmpty(),
            )
        }
    }

    // Bulk add label
    if (addLabelOpen) {
        val count = selectedIds.size
        val close = { addLabelOpen = false }
        FinioDialog(
            onDismissRequest = close,
            title = "Add label to $count transaction${if (count == 1) "" else "s"}",
            description = "Adds the label alongside any labels a transaction already has.",
            footer = {
                FinioButton("Cancel", onClick = close, variant = ButtonVariant.Outline)
                FinioButton(
                    "Apply",
                    onClick = {
                        if (addLabelId.isNotEmpty()) {
                            store.bulkAddLabel(selectedIds.toList(), addLabelId)
                            toast.success("Label added to $count transaction${if (count == 1) "" else "s"}")
                            addLabelOpen = false
                            addLabelId = ""
                            exitSelectionMode()
                        }
                    },
                    enabled = addLabelId.isNotEmpty(),
                )
            },
        ) {
            FinioSelect(
                value = addLabelId.ifEmpty { null },
                options = labels.map { SelectOption(it.id, it.name) },
                onValueChange = { addLabelId = it },
                placeholder = "Select label",
            )
        }
    }
}

/**
 * The bulk-selection toolbar: a floating glass pill level with where the coin FAB sits (which
 * is hidden while selecting), 12dp from the left and 84dp from the right, 56dp tall.
 */
@Composable
private fun SelectionToolbar(
    count: Int,
    onAddLabel: () -> Unit,
    onRecategorize: () -> Unit,
    onDelete: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = FinioTheme.colors
    val shape = FinioShapes.full
    Row(
        modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(start = 12.dp, end = 84.dp, bottom = 88.dp)
            .fillMaxWidth()
            .height(56.dp)
            .cssShadow(shape, FinioTheme.shadows.float)
            .clip(shape)
            .background(colors.glassStrong)
            .border(1.dp, colors.glassBorder, shape)
            .padding(start = 16.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("$count selected", style = FinioType.bodyMedium, color = colors.foreground, maxLines = 1)
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            FinioIconButton(LucideIcons.Tags, "Add label", onAddLabel, size = ButtonSize.IconLg, enabled = count > 0)
            FinioIconButton(LucideIcons.Tag, "Recategorize", onRecategorize, size = ButtonSize.IconLg, enabled = count > 0)
            FinioIconButton(LucideIcons.Trash2, "Delete selected", onDelete, size = ButtonSize.IconLg, enabled = count > 0, tint = colors.destructive)
            FinioIconButton(LucideIcons.X, "Cancel selection", onCancel, size = ButtonSize.IconLg)
        }
    }
}
