package com.slowatcoding.finio.ui.screens.recurring

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.slowatcoding.finio.core.calc.activeAccounts
import com.slowatcoding.finio.core.calc.findTransferCategory
import com.slowatcoding.finio.core.calc.isCategoryValidForType
import com.slowatcoding.finio.core.calc.miscLast
import com.slowatcoding.finio.core.format.formatShortDate
import com.slowatcoding.finio.core.format.toLocalDateTimeInputValue
import com.slowatcoding.finio.core.js.finioZone
import com.slowatcoding.finio.core.js.iso
import com.slowatcoding.finio.core.js.localDate
import com.slowatcoding.finio.core.js.nowInstant
import com.slowatcoding.finio.core.js.toIso
import com.slowatcoding.finio.core.model.CategoryType
import com.slowatcoding.finio.core.model.RecurrenceFrequency
import com.slowatcoding.finio.core.model.RecurringTransaction
import com.slowatcoding.finio.core.model.Transaction
import com.slowatcoding.finio.core.model.TransactionType
import com.slowatcoding.finio.core.store.BackfillPreview
import com.slowatcoding.finio.core.store.NewRecurring
import com.slowatcoding.finio.core.store.isRulePaused
import com.slowatcoding.finio.core.store.lastOccurrenceOnOrBefore
import com.slowatcoding.finio.core.store.nextDueDate
import com.slowatcoding.finio.core.store.previewBackfill
import com.slowatcoding.finio.core.util.MAX_NOTE_LENGTH
import com.slowatcoding.finio.core.util.cleanText
import com.slowatcoding.finio.core.util.stripLeading
import com.slowatcoding.finio.ui.common.BackButton
import com.slowatcoding.finio.ui.common.HideAmountsToggle
import com.slowatcoding.finio.ui.common.collectFinanceState
import com.slowatcoding.finio.ui.common.financeStore
import com.slowatcoding.finio.ui.common.rememberMoneyFormatter
import com.slowatcoding.finio.ui.common.undoToast
import com.slowatcoding.finio.ui.components.ButtonVariant
import com.slowatcoding.finio.ui.components.FinioButton
import com.slowatcoding.finio.ui.components.FinioCard
import com.slowatcoding.finio.ui.components.FinioDatePicker
import com.slowatcoding.finio.ui.components.FinioDateTimePicker
import com.slowatcoding.finio.ui.components.FinioDialog
import com.slowatcoding.finio.ui.components.FinioDivider
import com.slowatcoding.finio.ui.components.FinioScreen
import com.slowatcoding.finio.ui.components.FinioSelect
import com.slowatcoding.finio.ui.components.FinioTextField
import com.slowatcoding.finio.ui.components.HeaderIconButton
import com.slowatcoding.finio.ui.components.HeaderIconTone
import com.slowatcoding.finio.ui.components.LocalConfirm
import com.slowatcoding.finio.ui.components.NumberPad
import com.slowatcoding.finio.ui.components.ScreenTitle
import com.slowatcoding.finio.ui.components.SelectOption
import com.slowatcoding.finio.ui.components.toast
import com.slowatcoding.finio.ui.icons.LucideIcons
import com.slowatcoding.finio.ui.navigation.FinioNavigator
import com.slowatcoding.finio.ui.screens.budgets.CardIconAction
import com.slowatcoding.finio.ui.screens.budgets.FormActions
import com.slowatcoding.finio.ui.screens.budgets.FormField
import com.slowatcoding.finio.ui.screens.budgets.FormPills
import com.slowatcoding.finio.ui.screens.budgets.Micro
import com.slowatcoding.finio.ui.screens.budgets.PlanningEmpty
import com.slowatcoding.finio.ui.screens.budgets.jsNumberString
import com.slowatcoding.finio.ui.theme.FinioTheme
import com.slowatcoding.finio.ui.theme.FinioType
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

private val FREQ_LABEL = linkedMapOf(
    RecurrenceFrequency.Daily to "Daily",
    RecurrenceFrequency.Weekly to "Weekly",
    RecurrenceFrequency.Monthly to "Monthly",
    RecurrenceFrequency.Yearly to "Yearly",
)

private enum class EndMode { Never, On, After }

/**
 * Below this, a backfill isn't worth interrupting for — creating a rule dated today has always
 * generated today's transaction straight away.
 */
private const val BACKFILL_PROMPT_THRESHOLD = 2

/** The rule the form currently describes, plus how it should be committed. */
private data class PendingRule(val rule: RecurringTransaction, val editingId: String?, val preview: BackfillPreview)

private fun nowMinute(): LocalDateTime = LocalDateTime.parse(toLocalDateTimeInputValue(nowInstant()))

private fun LocalDateTime.toInstantLocal() = atZone(finioZone).toInstant()

/**
 * Port of web/src/pages/Recurring.tsx — route `/recurring`: the inline rule form (type, amount,
 * account(s), category, note, frequency, start, end by date or count, optional goal) above one
 * glass list of rules with pause/resume, edit and delete. A rule dated in the past opens the
 * "Add past transactions?" preview (core `previewBackfill`); "Start from today" parks
 * `lastRunDate` on `lastOccurrenceOnOrBefore`.
 */
@Composable
fun RecurringScreen(nav: FinioNavigator) {
    val store = financeStore()
    val state by collectFinanceState()
    val money = rememberMoneyFormatter()
    val confirm = LocalConfirm.current
    val scope = rememberCoroutineScope()
    val colors = FinioTheme.colors

    val recurring = state.recurring
    val allAccounts = state.accounts
    val categories = state.categories
    val goals = state.goals
    // A closed account cannot take new charges, so it must not back a new rule. Existing rules
    // still resolve their account name from the full list below.
    val accounts = remember(allAccounts) { activeAccounts(allAccounts) }

    var showForm by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var type by rememberSaveable { mutableStateOf(TransactionType.Expense) }
    var amount by rememberSaveable { mutableStateOf("") }
    var accountId by rememberSaveable { mutableStateOf(accounts.firstOrNull()?.id ?: "") }
    var toAccountId by rememberSaveable { mutableStateOf("") }
    var categoryId by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var frequency by rememberSaveable { mutableStateOf(RecurrenceFrequency.Monthly) }
    var startDate by remember { mutableStateOf<LocalDateTime?>(nowMinute()) }
    var endMode by rememberSaveable { mutableStateOf(EndMode.Never) }
    var endDate by remember { mutableStateOf<LocalDate?>(null) }
    var maxOccurrences by rememberSaveable { mutableStateOf("") }
    var goalId by rememberSaveable { mutableStateOf("") }
    var pending by remember { mutableStateOf<PendingRule?>(null) }

    val filteredCategories = remember(categories, type) {
        miscLast(
            if (type == TransactionType.Transfer) categories.filter { it.type == CategoryType.Both }
            else categories.filter { isCategoryValidForType(it, type) },
        )
    }

    fun resetForm() {
        showForm = false
        editingId = null
        type = TransactionType.Expense
        amount = ""
        accountId = accounts.firstOrNull()?.id ?: ""
        toAccountId = ""
        categoryId = ""
        note = ""
        frequency = RecurrenceFrequency.Monthly
        startDate = nowMinute()
        endMode = EndMode.Never
        endDate = null
        maxOccurrences = ""
        goalId = ""
    }

    fun startCreate() {
        resetForm()
        showForm = true
    }

    fun startEdit(rule: RecurringTransaction) {
        editingId = rule.id
        type = rule.type
        amount = jsNumberString(rule.amount)
        accountId = rule.accountId
        toAccountId = rule.toAccountId ?: ""
        categoryId = rule.categoryId
        note = rule.note
        frequency = rule.frequency
        startDate = LocalDateTime.parse(toLocalDateTimeInputValue(rule.startDate))
        endMode = when {
            !rule.endDate.isNullOrEmpty() -> EndMode.On
            rule.maxOccurrences != null -> EndMode.After
            else -> EndMode.Never
        }
        endDate = rule.endDate?.takeIf { it.isNotEmpty() }?.let { iso(it).atZone(finioZone).toLocalDate() }
        maxOccurrences = rule.maxOccurrences?.toString() ?: ""
        goalId = rule.goalId ?: ""
        showForm = true
    }

    /** Build the rule the form describes, or return the first validation error. */
    fun buildRule(): Any {
        val parsed = amount.toDoubleOrNull() ?: 0.0
        if (parsed.isNaN() || parsed <= 0) return "Enter a valid amount"
        if (accountId.isEmpty()) return "Select an account"
        if (type == TransactionType.Transfer) {
            if (toAccountId.isEmpty()) return "Select a destination account"
            if (toAccountId == accountId) return "Choose two different accounts"
        } else if (categoryId.isEmpty()) {
            return "Select a category"
        }
        val start = startDate?.toInstantLocal() ?: return "Pick a valid start date"

        var resolvedEndDate: String? = null
        var resolvedMax: Int? = null
        when (endMode) {
            EndMode.On -> {
                val day = endDate ?: return "Pick an end date"
                // The picker gives a date only — run the rule through the end of that day.
                val end = localDate(day.year, day.monthValue - 1, day.dayOfMonth, 23, 59, 59)
                if (end.isBefore(start)) return "The end date is before the start date"
                resolvedEndDate = end.toIso()
            }
            EndMode.After -> {
                val count = maxOccurrences.trim().takeWhile { it.isDigit() }.toIntOrNull()
                if (count == null || count < 1) return "Enter how many times it should run"
                resolvedMax = count
            }
            EndMode.Never -> {}
        }

        val existing = editingId?.let { id -> recurring.find { it.id == id } }
        val transferCategory = findTransferCategory(categories)
        return RecurringTransaction(
            id = existing?.id ?: "draft",
            type = type,
            amount = parsed,
            accountId = accountId,
            toAccountId = if (type == TransactionType.Transfer) toAccountId else null,
            categoryId = if (type == TransactionType.Transfer) (transferCategory?.id ?: categoryId) else categoryId,
            note = cleanText(note, MAX_NOTE_LENGTH),
            labels = existing?.labels ?: emptyList(),
            frequency = frequency,
            startDate = start.toIso(),
            endDate = resolvedEndDate,
            maxOccurrences = resolvedMax,
            occurrenceCount = existing?.occurrenceCount ?: 0,
            pausedAt = existing?.pausedAt?.takeIf { it.isNotEmpty() },
            lastRunDate = existing?.lastRunDate,
            createdAt = existing?.createdAt ?: nowInstant().toIso(),
            goalId = goalId.ifEmpty { null },
        )
    }

    fun announceGenerated(prefix: String, generated: List<Transaction>, noun: String) {
        if (generated.isEmpty()) {
            toast.success(prefix)
        } else {
            val ids = generated.map { it.id }
            undoToast("$prefix · added ${generated.size} $noun${if (generated.size == 1) "" else "s"}") {
                store.bulkDeleteTransactions(ids)
            }
        }
    }

    fun commit(rule: RecurringTransaction, ruleEditingId: String?, skipPast: Boolean) {
        // Skipping the backfill parks the schedule on the last occurrence that has already passed,
        // so the rule stays anchored to its start date but generates nothing for the past.
        val lastRunDate = if (skipPast) lastOccurrenceOnOrBefore(rule, nowInstant())?.toIso() ?: rule.lastRunDate else rule.lastRunDate
        // Written out field by field so clearing "ends on" / "ends after" (or switching away from a
        // transfer) actually unsets the old value instead of leaving it behind.
        if (ruleEditingId != null) {
            store.updateRecurring(ruleEditingId) {
                it.copy(
                    type = rule.type, amount = rule.amount, accountId = rule.accountId, toAccountId = rule.toAccountId,
                    categoryId = rule.categoryId, note = rule.note, labels = rule.labels, frequency = rule.frequency,
                    startDate = rule.startDate, endDate = rule.endDate, maxOccurrences = rule.maxOccurrences,
                    lastRunDate = lastRunDate, goalId = rule.goalId,
                )
            }
        } else {
            store.addRecurring(
                NewRecurring(
                    type = rule.type, amount = rule.amount, accountId = rule.accountId, toAccountId = rule.toAccountId,
                    categoryId = rule.categoryId, note = rule.note, labels = rule.labels, frequency = rule.frequency,
                    startDate = rule.startDate, endDate = rule.endDate, maxOccurrences = rule.maxOccurrences,
                    lastRunDate = lastRunDate, goalId = rule.goalId,
                ),
            )
        }
        val generated = store.processRecurring()
        announceGenerated(if (ruleEditingId != null) "Rule updated" else "Recurring rule created", generated, "transaction")
        pending = null
        resetForm()
    }

    fun handleSubmit() {
        val built = buildRule()
        if (built is String) {
            toast.error(built)
            return
        }
        val rule = built as RecurringTransaction
        val preview = previewBackfill(rule, accounts.map { it.id }, nowInstant())
        // A rule dated in the past moves real balances — show what it will do before it does it.
        if (preview.count >= BACKFILL_PROMPT_THRESHOLD) {
            pending = PendingRule(rule, editingId, preview)
            return
        }
        commit(rule, editingId, false)
    }

    fun accountName(id: String?) = allAccounts.find { it.id == id }?.name ?: "Unknown"

    FinioScreen(header = {
        BackButton(nav)
        ScreenTitle("Recurring")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HideAmountsToggle()
            HeaderIconButton(
                LucideIcons.Plus,
                "Add recurring",
                onClick = { if (showForm) resetForm() else startCreate() },
                tone = HeaderIconTone.Primary,
                enabled = accounts.isNotEmpty(),
            )
        }
    }) {
        if (accounts.isEmpty()) {
            Text(
                "Add an account first to create recurring rules.",
                Modifier.fillMaxWidth().padding(vertical = 32.dp),
                style = FinioType.body,
                color = colors.mutedForeground,
                textAlign = TextAlign.Center,
            )
        }

        if (showForm) {
            FinioCard(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    FormPills(
                        options = listOf(
                            TransactionType.Expense to "Expense",
                            TransactionType.Income to "Income",
                            TransactionType.Transfer to "Transfer",
                        ),
                        selected = type,
                        onSelect = {
                            type = it
                            categoryId = ""
                        },
                    )
                    FormField("Amount") { NumberPad(amount, { amount = it }) }
                    FormField(if (type == TransactionType.Transfer) "From account" else "Account") {
                        FinioSelect(
                            accountId.ifEmpty { null },
                            accounts.map { SelectOption(it.id, it.name) },
                            { accountId = it },
                            Modifier.fillMaxWidth(),
                            placeholder = "Account",
                        )
                    }
                    if (type == TransactionType.Transfer) {
                        FormField("To account") {
                            FinioSelect(
                                toAccountId.ifEmpty { null },
                                accounts.filter { it.id != accountId }.map { SelectOption(it.id, it.name) },
                                { toAccountId = it },
                                Modifier.fillMaxWidth(),
                                placeholder = "Destination",
                            )
                        }
                    } else {
                        FormField("Category") {
                            FinioSelect(
                                categoryId.ifEmpty { null },
                                filteredCategories.map { SelectOption(it.id, it.name) },
                                { categoryId = it },
                                Modifier.fillMaxWidth(),
                                placeholder = "Choose a category",
                                title = "Category",
                            )
                        }
                    }
                    FormField("Note") {
                        FinioTextField(
                            note,
                            { note = stripLeading(it).take(MAX_NOTE_LENGTH) },
                            Modifier.fillMaxWidth(),
                            placeholder = "e.g. Netflix",
                        )
                    }
                    FormField("Frequency") {
                        FinioSelect(
                            frequency,
                            FREQ_LABEL.map { (f, label) -> SelectOption(f, label) },
                            { frequency = it },
                            Modifier.fillMaxWidth(),
                            placeholder = "Frequency",
                            title = "Frequency",
                        )
                    }
                    FormField("Starts") {
                        FinioDateTimePicker(startDate, { startDate = it.truncatedTo(ChronoUnit.MINUTES) }, Modifier.fillMaxWidth())
                    }
                    FormField("Ends") {
                        FinioSelect(
                            endMode,
                            listOf(
                                SelectOption(EndMode.Never, "Never"),
                                SelectOption(EndMode.On, "On a date"),
                                SelectOption(EndMode.After, "After N times"),
                            ),
                            { endMode = it },
                            Modifier.fillMaxWidth(),
                            title = "Ends",
                        )
                        if (endMode == EndMode.On) {
                            FinioDatePicker(endDate, { endDate = it }, Modifier.fillMaxWidth().padding(top = 8.dp), placeholder = "End date")
                        }
                        if (endMode == EndMode.After) {
                            FinioTextField(
                                maxOccurrences,
                                { v -> maxOccurrences = v.filter { it.isDigit() }.take(6) },
                                Modifier.fillMaxWidth().padding(top = 8.dp),
                                placeholder = "Number of occurrences",
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            )
                        }
                    }
                    if (goals.isNotEmpty()) {
                        FormField("Fund a goal (optional)") {
                            FinioSelect(
                                goalId.ifEmpty { "none" },
                                listOf(SelectOption("none", "None")) + goals.map { SelectOption(it.id, it.name) },
                                { goalId = if (it == "none") "" else it },
                                Modifier.fillMaxWidth(),
                                title = "Fund a goal",
                            )
                            Text(
                                "Each occurrence also logs a contribution to this goal, automatically.",
                                Modifier.padding(top = 4.dp),
                                style = Micro,
                                color = colors.mutedForeground,
                            )
                        }
                    }
                    FormActions(if (editingId != null) "Save changes" else "Save rule", ::handleSubmit, ::resetForm)
                }
            }
        }

        if (recurring.isEmpty() && !showForm && accounts.isNotEmpty()) {
            PlanningEmpty(LucideIcons.Repeat, "No recurring rules yet", "Create a rule", ::startCreate)
        }

        if (recurring.isNotEmpty()) {
            FinioCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
                recurring.forEachIndexed { index, r ->
                    if (index > 0) FinioDivider()
                    RuleRow(
                        rule = r,
                        title = r.note.ifEmpty { categories.find { it.id == r.categoryId }?.name ?: "Recurring" },
                        accountsText = if (r.type == TransactionType.Transfer) "${accountName(r.accountId)} → ${accountName(r.toAccountId)}"
                        else accountName(r.accountId),
                        amountText = (when (r.type) {
                            TransactionType.Income -> "+"
                            TransactionType.Expense -> "-"
                            else -> ""
                        }) + money(r.amount, compact = true),
                        goalName = r.goalId?.let { id -> goals.find { it.id == id }?.name },
                        onTogglePause = {
                            val paused = isRulePaused(r)
                            store.setRecurringPaused(r.id, !paused)
                            if (!paused) {
                                toast.success("Rule paused")
                            } else {
                                announceGenerated("Rule resumed", store.processRecurring(), "due transaction")
                            }
                        },
                        onEdit = { startEdit(r) },
                        onDelete = {
                            scope.launch {
                                val ok = confirm.confirm(
                                    "Delete this recurring rule?",
                                    "Transactions it has already generated are kept — only future occurrences stop.",
                                    confirmLabel = "Delete rule",
                                )
                                if (ok) store.deleteRecurring(r.id)
                            }
                        },
                    )
                }
            }
        }
    }

    // Backfill preview — a past start date injects transactions and moves balances.
    pending?.let { p ->
        FinioDialog(onDismissRequest = { pending = null }, title = "Add past transactions?") {
            val strong = SpanStyle(color = colors.foreground, fontWeight = FontWeight.SemiBold)
            Text(
                buildAnnotatedString {
                    append("This rule started ")
                    p.preview.firstDate?.let { append(formatShortDate(it)) }
                    append(", so saving it will create ")
                    withStyle(strong) { append("${p.preview.count} transaction${if (p.preview.count == 1) "" else "s"}") }
                    append(" totalling ")
                    withStyle(strong) { append(money(p.preview.total)) }
                    append(" and move your balances.")
                    if (p.preview.capped) append(" More will be added the next time the app opens — the catch-up is capped per run.")
                },
                style = FinioType.body,
                color = colors.mutedForeground,
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FinioButton("Add them", { commit(p.rule, p.editingId, false) }, Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FinioButton("Start from today", { commit(p.rule, p.editingId, true) }, Modifier.weight(1f), variant = ButtonVariant.Outline)
                    FinioButton("Cancel", { pending = null }, variant = ButtonVariant.Outline)
                }
            }
        }
    }
}

@Composable
private fun RuleRow(
    rule: RecurringTransaction,
    title: String,
    accountsText: String,
    amountText: String,
    goalName: String?,
    onTogglePause: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = FinioTheme.colors
    val paused = isRulePaused(rule)
    val nextDue = nextDueDate(rule)
    val schedule = when {
        paused -> "Paused"
        nextDue != null -> "Next ${formatShortDate(nextDue)}"
        else -> "Ended"
    }
    val limit = when {
        rule.maxOccurrences != null -> "${rule.occurrenceCount} of ${rule.maxOccurrences}"
        !rule.endDate.isNullOrEmpty() -> "until ${formatShortDate(rule.endDate!!)}"
        else -> null
    }
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(
            Modifier.alpha(if (paused) 0.5f else 1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = FinioType.bodyMedium, color = colors.foreground, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${FREQ_LABEL.getValue(rule.frequency)} · $accountsText",
                    style = FinioType.caption, color = colors.mutedForeground, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            // Same rule as the transaction list: income green, everything else ink.
            Text(amountText, style = FinioType.rowValue, color = if (rule.type == TransactionType.Income) colors.positive else colors.foreground)
        }
        if (goalName != null) {
            Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(LucideIcons.PiggyBank, null, Modifier.size(11.dp), tint = colors.mutedForeground)
                Text("Funds “$goalName”", style = Micro, color = colors.mutedForeground)
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                schedule + (limit?.let { " · $it" } ?: ""),
                Modifier.weight(1f),
                style = Micro, color = colors.mutedForeground, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            CardIconAction(
                if (paused) LucideIcons.Play else LucideIcons.Pause,
                if (paused) "Resume rule" else "Pause rule",
                if (paused) colors.primary else colors.mutedForeground,
                onTogglePause,
            )
            CardIconAction(LucideIcons.Pencil, "Edit rule", colors.mutedForeground, onEdit)
            CardIconAction(LucideIcons.Trash2, "Delete rule", colors.destructive, onDelete)
        }
    }
}
