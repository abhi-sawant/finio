package com.slowatcoding.finio.ui.screens.importcsv

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slowatcoding.finio.core.calc.activeAccounts
import com.slowatcoding.finio.core.csv.AmountMode
import com.slowatcoding.finio.core.csv.ColumnMapping
import com.slowatcoding.finio.core.csv.CsvImportOptions
import com.slowatcoding.finio.core.csv.CsvImportResult
import com.slowatcoding.finio.core.csv.CsvParseResult
import com.slowatcoding.finio.core.csv.DATE_FORMATS
import com.slowatcoding.finio.core.csv.DateFormatCode
import com.slowatcoding.finio.core.csv.buildTransactionsFromCsv
import com.slowatcoding.finio.core.csv.detectDateFormatInfo
import com.slowatcoding.finio.core.csv.findDuplicateRows
import com.slowatcoding.finio.core.csv.guessColumnMapping
import com.slowatcoding.finio.core.csv.parseCsvText
import com.slowatcoding.finio.core.data.MISC_CATEGORY_ID
import com.slowatcoding.finio.core.format.formatShortDate
import com.slowatcoding.finio.core.model.TransactionType
import com.slowatcoding.finio.core.store.NewTransaction
import com.slowatcoding.finio.platform.files.FinioMime
import com.slowatcoding.finio.platform.files.ImportResult
import com.slowatcoding.finio.platform.files.rememberDocumentImporter
import com.slowatcoding.finio.ui.common.BackButton
import com.slowatcoding.finio.ui.common.collectFinanceState
import com.slowatcoding.finio.ui.common.financeStore
import com.slowatcoding.finio.ui.common.rememberMoneyFormatter
import com.slowatcoding.finio.ui.common.undoToast
import com.slowatcoding.finio.ui.components.ButtonSize
import com.slowatcoding.finio.ui.components.FieldLabel
import com.slowatcoding.finio.ui.components.FinioButton
import com.slowatcoding.finio.ui.components.FinioCard
import com.slowatcoding.finio.ui.components.FinioDivider
import com.slowatcoding.finio.ui.components.FinioScreen
import com.slowatcoding.finio.ui.components.FinioSelect
import com.slowatcoding.finio.ui.components.FinioTextField
import com.slowatcoding.finio.ui.components.HeaderIconSpacer
import com.slowatcoding.finio.ui.components.ScreenTitle
import com.slowatcoding.finio.ui.components.SelectOption
import com.slowatcoding.finio.ui.components.SwitchField
import com.slowatcoding.finio.ui.components.toast
import com.slowatcoding.finio.ui.icons.LucideIcons
import com.slowatcoding.finio.ui.navigation.FinioNavigator
import com.slowatcoding.finio.ui.navigation.Routes
import com.slowatcoding.finio.ui.screens.rules.SmallSegmented
import com.slowatcoding.finio.ui.theme.FinioShapes
import com.slowatcoding.finio.ui.theme.FinioTheme
import com.slowatcoding.finio.ui.theme.FinioType

private enum class Step { Upload, Map, Preview }

/** Large statements shouldn't make the preview list itself the bottleneck. */
private const val MAX_PREVIEW_ROWS = 200

/** Port of web/src/pages/ImportCsv.tsx — route `/import-csv`. */
@Composable
fun ImportCsvScreen(nav: FinioNavigator) {
    val store = financeStore()
    val state by collectFinanceState()
    val colors = FinioTheme.colors
    val money = rememberMoneyFormatter()
    val categories = state.categories
    val rules = state.rules
    val accounts = remember(state.accounts) { activeAccounts(state.accounts) }

    var step by rememberSaveable { mutableStateOf(Step.Upload) }
    var fileName by rememberSaveable { mutableStateOf("") }
    var skipRows by rememberSaveable { mutableStateOf("0") }
    var parsed by remember { mutableStateOf<CsvParseResult?>(null) }

    var accountId by rememberSaveable { mutableStateOf(accounts.firstOrNull()?.id ?: "") }
    var dateCol by rememberSaveable { mutableStateOf<Int?>(null) }
    var dateFormat by rememberSaveable { mutableStateOf(DateFormatCode.YYYY_MM_DD) }
    var detectedFormat by rememberSaveable { mutableStateOf<DateFormatCode?>(null) }
    var detectedAmbiguous by rememberSaveable { mutableStateOf(false) }
    var amountMode by rememberSaveable { mutableStateOf(AmountMode.Signed) }
    var amountCol by rememberSaveable { mutableStateOf<Int?>(null) }
    var negativeIsExpense by rememberSaveable { mutableStateOf(true) }
    var debitCol by rememberSaveable { mutableStateOf<Int?>(null) }
    var creditCol by rememberSaveable { mutableStateOf<Int?>(null) }
    var noteCol by rememberSaveable { mutableStateOf<Int?>(null) }
    var categoryCol by rememberSaveable { mutableStateOf<Int?>(null) }
    var applyRules by rememberSaveable { mutableStateOf(true) }

    var result by remember { mutableStateOf<CsvImportResult?>(null) }
    var duplicateRows by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var skipDuplicates by rememberSaveable { mutableStateOf(true) }

    // The wizard holds the parsed file in memory only; if the process was recreated, start over.
    if (step != Step.Upload && parsed == null) step = Step.Upload
    if (step == Step.Preview && result == null) step = Step.Map

    fun detectFrom(csv: CsvParseResult, col: Int) {
        val info = detectDateFormatInfo(csv.rows.map { it.getOrNull(col) ?: "" })
        info.format?.let { dateFormat = it }
        detectedFormat = info.format
        detectedAmbiguous = info.ambiguous
    }

    val chooseFile = rememberDocumentImporter(FinioMime.CSV_IMPORT) { res ->
        when (res) {
            is ImportResult.Cancelled -> Unit
            is ImportResult.Failed -> toast.error("Could not read that file as CSV")
            is ImportResult.Loaded -> {
                try {
                    val skip = maxOf(0, skipRows.trim().toIntOrNull() ?: 0)
                    val csv = parseCsvText(res.text, skip)
                    if (csv.headers.isEmpty() || csv.rows.isEmpty()) {
                        toast.error("No data rows found — check \"rows to skip\"")
                        return@rememberDocumentImporter
                    }
                    parsed = csv
                    fileName = res.displayName ?: ""
                    // Best-effort auto-mapping from the header names — the user can always override.
                    val guess = guessColumnMapping(csv.headers)
                    dateCol = guess.dateCol
                    amountMode = guess.amountMode
                    amountCol = guess.amountCol
                    debitCol = guess.debitCol
                    creditCol = guess.creditCol
                    noteCol = guess.noteCol
                    categoryCol = guess.categoryCol
                    detectedFormat = null
                    detectedAmbiguous = false
                    guess.dateCol?.let { detectFrom(csv, it) }
                    step = Step.Map
                } catch (_: Exception) {
                    toast.error("Could not read that file as CSV")
                }
            }
        }
    }

    // Why "Preview import" is disabled, in the user's terms — null when it's ready.
    val missingMapping = when {
        accountId.isEmpty() -> "Choose an account to import into."
        dateCol == null -> "Choose the date column."
        amountMode == AmountMode.Signed -> if (amountCol == null) "Choose the amount column." else null
        debitCol == null && creditCol == null -> "Choose a debit or a credit column."
        else -> null
    }
    val canPreview = parsed != null && missingMapping == null

    fun handlePreview() {
        val csv = parsed ?: return
        val date = dateCol ?: return
        if (!canPreview) return
        val built = buildTransactionsFromCsv(
            csv.rows,
            CsvImportOptions(
                mapping = ColumnMapping(
                    dateCol = date,
                    noteCol = noteCol,
                    categoryCol = categoryCol,
                    amountMode = amountMode,
                    amountCol = amountCol,
                    negativeIsExpense = negativeIsExpense,
                    debitCol = debitCol,
                    creditCol = creditCol,
                ),
                dateFormat = dateFormat,
                accountId = accountId,
                categories = categories,
                fallbackCategoryId = MISC_CATEGORY_ID,
                rules = if (applyRules) rules else null,
            ),
        )
        result = built
        duplicateRows = findDuplicateRows(built.accepted, state.transactions)
        step = Step.Preview
    }

    val toImport = remember(result, duplicateRows, skipDuplicates) {
        val r = result ?: return@remember emptyList()
        if (skipDuplicates) r.accepted.filter { it.rowIndex !in duplicateRows } else r.accepted
    }
    val ruleMatchedCount = toImport.count { it.matchedRuleId != null }

    fun handleImport() {
        if (toImport.isEmpty()) return
        val before = store.current.transactions.map { it.id }.toSet()
        val added = store.bulkAddTransactions(
            toImport.map {
                val t = it.transaction
                NewTransaction(
                    type = t.type, amount = t.amount, accountId = t.accountId, categoryId = t.categoryId,
                    date = t.date, note = t.note, labels = t.labels,
                )
            },
        )
        val newIds = store.current.transactions.map { it.id }.filter { it !in before }
        undoToast("Imported $added transaction${if (added == 1) "" else "s"}") { store.bulkDeleteTransactions(newIds) }
        nav.navigate(Routes.Transactions)
    }

    fun categoryName(id: String) = categories.find { it.id == id }?.name ?: "Miscellaneous"

    fun handleBack() {
        when (step) {
            Step.Upload -> nav.back()
            Step.Map -> step = Step.Upload
            Step.Preview -> step = Step.Map
        }
    }
    BackHandler(enabled = step != Step.Upload) { handleBack() }

    val stepTitle = when (step) {
        Step.Upload -> "Import bank CSV"
        Step.Map -> "Map columns"
        Step.Preview -> "Review & import"
    }

    FinioScreen(header = {
        BackButton(nav, onBack = { handleBack() })
        ScreenTitle(stepTitle)
        HeaderIconSpacer()
    }) {
        when (step) {
            Step.Upload -> {
                if (accounts.isEmpty()) {
                    FinioCard(Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Add an account first", style = FinioType.bodyMedium, color = colors.foreground)
                            Text("A CSV import needs somewhere to attach the transactions.", style = FinioType.caption, color = colors.mutedForeground)
                            FinioButton("Add account", onClick = { nav.navigate(Routes.AddAccount) }, modifier = Modifier.fillMaxWidth())
                        }
                    }
                } else {
                    FinioCard(Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                "Import transactions from a bank or card statement CSV. You'll map its columns to Finio's fields on the next step, and review everything before it's added.",
                                style = FinioType.body,
                                color = colors.foreground,
                            )
                            Column {
                                FieldLabel("Rows to skip before the header")
                                FinioTextField(
                                    value = skipRows,
                                    onValueChange = { v -> skipRows = v.filter { it.isDigit() }.take(4) },
                                    modifier = Modifier.width(96.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                )
                                Text(
                                    "Some statements have a few title lines before the real column headers.",
                                    Modifier.padding(top = 4.dp),
                                    style = FinioType.caption,
                                    color = colors.mutedForeground,
                                )
                            }
                        }
                    }
                    FinioButton(
                        "Choose CSV file",
                        onClick = chooseFile,
                        modifier = Modifier.fillMaxWidth(),
                        size = ButtonSize.Lg,
                        leadingIcon = LucideIcons.FileUp,
                    )
                }
            }

            Step.Map -> {
                val csv = parsed ?: return@FinioScreen
                Text(fileName, style = FinioType.caption, color = colors.mutedForeground, maxLines = 1, overflow = TextOverflow.Ellipsis)
                FinioCard(Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column {
                            FieldLabel("Account")
                            FinioSelect(
                                value = accountId.ifEmpty { null },
                                options = accounts.map { SelectOption(it.id, it.name) },
                                onValueChange = { accountId = it },
                                placeholder = "Select account",
                                title = "Account",
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column(Modifier.weight(1f)) {
                                FieldLabel("Date column")
                                ColumnSelect(csv.headers, dateCol, title = "Date column", onChange = { col ->
                                    dateCol = col
                                    if (col != null) detectFrom(csv, col)
                                })
                            }
                            Column(Modifier.weight(1f)) {
                                FieldLabel("Date format")
                                FinioSelect(
                                    value = dateFormat,
                                    // The trigger shows just the pattern, like the web's SelectValue; the example sits below it in the list.
                                    options = DATE_FORMATS.map { SelectOption(it, it.wire, description = it.label.substringAfter("(").removeSuffix(")")) },
                                    onValueChange = { dateFormat = it },
                                    title = "Date format",
                                )
                                if (detectedFormat != null && detectedFormat == dateFormat) {
                                    Text(
                                        "Detected ${detectedFormat!!.wire} — check the preview",
                                        Modifier.padding(top = 6.dp),
                                        style = FinioType.caption,
                                        color = if (detectedAmbiguous) colors.warning else colors.mutedForeground,
                                    )
                                }
                            }
                        }
                        Column {
                            FieldLabel("Amount columns")
                            SmallSegmented(
                                options = listOf(AmountMode.Signed to "Single (signed)", AmountMode.DebitCredit to "Debit & credit"),
                                selected = amountMode,
                                onSelect = { amountMode = it },
                                modifier = Modifier.padding(bottom = 8.dp),
                            )
                            if (amountMode == AmountMode.Signed) {
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    ColumnSelect(csv.headers, amountCol, Modifier.weight(1f), title = "Amount column", onChange = { amountCol = it })
                                    FinioSelect(
                                        value = negativeIsExpense,
                                        options = listOf(SelectOption(true, "Negative = expense"), SelectOption(false, "Negative = income")),
                                        onValueChange = { negativeIsExpense = it },
                                        modifier = Modifier.weight(1f).semantics { contentDescription = "Negative amounts are" },
                                        title = "Negative amounts are",
                                    )
                                }
                            } else {
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Column(Modifier.weight(1f)) {
                                        Text("Debit (money out)", Modifier.padding(bottom = 4.dp), style = FinioType.caption, color = colors.mutedForeground)
                                        ColumnSelect(csv.headers, debitCol, title = "Debit (money out)", onChange = { debitCol = it })
                                    }
                                    Column(Modifier.weight(1f)) {
                                        Text("Credit (money in)", Modifier.padding(bottom = 4.dp), style = FinioType.caption, color = colors.mutedForeground)
                                        ColumnSelect(csv.headers, creditCol, title = "Credit (money in)", onChange = { creditCol = it })
                                    }
                                }
                            }
                        }
                        Column {
                            FieldLabel("Note / description column (optional)")
                            ColumnSelect(csv.headers, noteCol, allowNone = true, title = "Note / description column", onChange = { noteCol = it })
                        }
                        Column {
                            FieldLabel("Category column (optional)")
                            ColumnSelect(csv.headers, categoryCol, allowNone = true, title = "Category column", onChange = { categoryCol = it })
                            Text(
                                "Matched by name to your existing categories; unmatched rows import as Miscellaneous.",
                                Modifier.padding(top = 4.dp),
                                style = FinioType.caption,
                                color = colors.mutedForeground,
                            )
                        }
                        if (rules.isNotEmpty()) {
                            val active = rules.count { it.enabled }
                            FinioDivider()
                            SwitchField(
                                title = "Apply categorization rules",
                                description = "$active active rule${if (active == 1) "" else "s"} will categorize rows the file doesn't already categorize",
                                checked = applyRules,
                                onCheckedChange = { applyRules = it },
                                icon = { Icon(LucideIcons.Wand2, null, Modifier.size(18.dp), tint = colors.mutedForeground) },
                            )
                        }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FinioButton(
                        "Preview import",
                        onClick = { handlePreview() },
                        modifier = Modifier.fillMaxWidth(),
                        size = ButtonSize.Lg,
                        enabled = canPreview,
                    )
                    if (missingMapping != null) {
                        Text(missingMapping, Modifier.fillMaxWidth(), style = FinioType.caption, color = colors.mutedForeground, textAlign = TextAlign.Center)
                    }
                }
            }

            Step.Preview -> {
                val r = result ?: return@FinioScreen
                FinioCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
                    Row(Modifier.height(IntrinsicSize.Min)) {
                        Stat("${r.totalRows}", "Rows in file", Modifier.weight(1f))
                        VDivider()
                        Stat("${r.accepted.size}", "Parsed OK", Modifier.weight(1f))
                        VDivider()
                        Stat("${duplicateRows.size}", "Possible duplicates", Modifier.weight(1f))
                    }
                }

                if (ruleMatchedCount > 0) {
                    Row(Modifier.padding(horizontal = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(LucideIcons.Wand2, null, Modifier.size(13.dp), tint = colors.primary)
                        Text(
                            "$ruleMatchedCount row${if (ruleMatchedCount == 1) "" else "s"} categorized by your rules",
                            style = FinioType.caption,
                            color = colors.mutedForeground,
                        )
                    }
                }

                if (duplicateRows.isNotEmpty()) {
                    FinioCard(Modifier.fillMaxWidth()) {
                        SwitchField(
                            title = "Skip duplicate transactions",
                            description = "Matched by same day, type, amount and note",
                            checked = skipDuplicates,
                            onCheckedChange = { skipDuplicates = it },
                            interactiveRow = true,
                        )
                    }
                }

                if (r.issues.isNotEmpty()) {
                    Column(
                        Modifier.fillMaxWidth().clip(FinioShapes.md).background(colors.warning.copy(alpha = 0.15f)).padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        r.issues.forEach { issue ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(LucideIcons.TriangleAlert, null, Modifier.padding(top = 1.dp).size(14.dp), tint = colors.warning)
                                Text(issue, style = FinioType.caption, color = colors.mutedForeground)
                            }
                        }
                    }
                }

                FinioCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
                    r.accepted.take(MAX_PREVIEW_ROWS).forEachIndexed { index, row ->
                        if (index > 0) FinioDivider()
                        val isDup = row.rowIndex in duplicateRows
                        val t = row.transaction
                        Row(
                            Modifier.fillMaxWidth().alpha(if (isDup && skipDuplicates) 0.4f else 1f).padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(t.note.ifEmpty { "—" }, style = FinioType.bodyMedium, color = colors.foreground, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    if (row.matchedRuleId != null) {
                                        Icon(LucideIcons.Wand2, "Categorized by a rule", Modifier.size(11.dp), tint = colors.primary)
                                    }
                                    Text(
                                        "${formatShortDate(t.date)} · ${categoryName(t.categoryId)}" + if (isDup) " · Duplicate" else "",
                                        style = FinioType.caption,
                                        color = colors.mutedForeground,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                            val expense = t.type == TransactionType.Expense
                            Text(
                                (if (expense) "-" else "+") + money(t.amount),
                                style = FinioType.rowValue,
                                color = if (expense) colors.foreground else colors.positive,
                                maxLines = 1,
                            )
                        }
                    }
                    if (r.accepted.size > MAX_PREVIEW_ROWS) {
                        FinioDivider()
                        Text(
                            "…and ${r.accepted.size - MAX_PREVIEW_ROWS} more, all will be imported",
                            Modifier.fillMaxWidth().padding(12.dp),
                            style = FinioType.caption,
                            color = colors.mutedForeground,
                            textAlign = TextAlign.Center,
                        )
                    }
                    if (r.accepted.isEmpty()) {
                        Text(
                            "Nothing to import — check the column mapping",
                            Modifier.fillMaxWidth().padding(24.dp),
                            style = FinioType.body,
                            color = colors.mutedForeground,
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                FinioButton(
                    "Import ${toImport.size} transaction${if (toImport.size == 1) "" else "s"}",
                    onClick = { handleImport() },
                    modifier = Modifier.fillMaxWidth(),
                    size = ButtonSize.Lg,
                    enabled = toImport.isNotEmpty(),
                )
            }
        }
    }
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier = Modifier) {
    val colors = FinioTheme.colors
    Column(modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = FinioType.title.copy(fontSize = 18.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold), color = colors.foreground)
        Text(label, style = FinioType.caption, color = colors.mutedForeground, textAlign = TextAlign.Center)
    }
}

@Composable
private fun VDivider() {
    androidx.compose.foundation.layout.Box(Modifier.width(1.dp).fillMaxHeight().background(FinioTheme.colors.border))
}

/** A CSV column picker: "None" (optional columns only) then each header, blank ones as "Column N". */
@Composable
private fun ColumnSelect(
    headers: List<String>,
    value: Int?,
    modifier: Modifier = Modifier,
    allowNone: Boolean = false,
    title: String? = null,
    onChange: (Int?) -> Unit,
) {
    // -1 is the "None" sentinel (the web's `__none__`).
    val options = buildList {
        if (allowNone) add(SelectOption(-1, "None"))
        headers.forEachIndexed { i, h -> add(SelectOption(i, h.ifEmpty { "Column ${i + 1}" })) }
    }
    FinioSelect(
        value = value ?: if (allowNone) -1 else null,
        options = options,
        onValueChange = { onChange(if (it == -1) null else it) },
        modifier = modifier,
        placeholder = "Select column",
        title = title,
    )
}
