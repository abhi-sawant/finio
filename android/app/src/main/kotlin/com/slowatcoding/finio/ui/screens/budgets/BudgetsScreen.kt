package com.slowatcoding.finio.ui.screens.budgets

import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.slowatcoding.finio.core.calc.BudgetPeriodOptions
import com.slowatcoding.finio.core.calc.BudgetStatus
import com.slowatcoding.finio.core.calc.budgetScopeKey
import com.slowatcoding.finio.core.calc.computeBudgetHistory
import com.slowatcoding.finio.core.calc.computeBudgetStatuses
import com.slowatcoding.finio.core.calc.isCategoryValidForType
import com.slowatcoding.finio.core.calc.miscLast
import com.slowatcoding.finio.core.model.Budget
import com.slowatcoding.finio.core.model.BudgetPeriod
import com.slowatcoding.finio.core.model.TransactionType
import com.slowatcoding.finio.core.period.PERIOD_LABELS
import com.slowatcoding.finio.core.period.normalizeMonthStartDay
import com.slowatcoding.finio.core.period.periodLabel
import com.slowatcoding.finio.core.period.periodShortLabel
import com.slowatcoding.finio.core.period.toPeriodType
import com.slowatcoding.finio.core.store.NewBudget
import com.slowatcoding.finio.core.util.firstFreeScope
import com.slowatcoding.finio.ui.common.BackButton
import com.slowatcoding.finio.ui.common.HideAmountsToggle
import com.slowatcoding.finio.ui.common.MoneyFormatter
import com.slowatcoding.finio.ui.common.SectionLoader
import com.slowatcoding.finio.ui.common.collectFinanceState
import com.slowatcoding.finio.ui.common.financeStore
import com.slowatcoding.finio.ui.common.rememberDerived
import com.slowatcoding.finio.ui.common.rememberMoneyFormatter
import com.slowatcoding.finio.ui.components.BudgetHealthBadge
import com.slowatcoding.finio.ui.components.FinioCard
import com.slowatcoding.finio.ui.components.FinioScreen
import com.slowatcoding.finio.ui.components.FinioSelect
import com.slowatcoding.finio.ui.components.HeaderIconButton
import com.slowatcoding.finio.ui.components.HeaderIconTone
import com.slowatcoding.finio.ui.components.LocalConfirm
import com.slowatcoding.finio.ui.components.NumberPad
import com.slowatcoding.finio.ui.components.ScreenTitle
import com.slowatcoding.finio.ui.components.SelectOption
import com.slowatcoding.finio.ui.components.SwitchField
import com.slowatcoding.finio.ui.components.parseHexColor
import com.slowatcoding.finio.ui.components.toast
import com.slowatcoding.finio.ui.icons.LucideIcons
import com.slowatcoding.finio.ui.mudra.BudgetProgressBar
import com.slowatcoding.finio.core.calc.budgetHealth
import com.slowatcoding.finio.ui.navigation.FinioNavigator
import com.slowatcoding.finio.ui.theme.FinioShapes
import com.slowatcoding.finio.ui.theme.FinioTheme
import com.slowatcoding.finio.ui.theme.FinioType
import com.slowatcoding.finio.ui.theme.mix
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private const val OVERALL_SCOPE = "__overall__"

/** How the scope select encodes its options — categories and labels share one list. */
private fun encodeScope(categoryId: String, labelId: String?): String = when {
    !labelId.isNullOrEmpty() -> "lbl:$labelId"
    categoryId == "" -> OVERALL_SCOPE
    else -> "cat:$categoryId"
}

private fun encodeScope(budget: Budget) = encodeScope(budget.categoryId, budget.labelId)

private data class Scope(val categoryId: String, val labelId: String? = null)

private fun decodeScope(value: String): Scope = when {
    value.startsWith("lbl:") -> Scope("", value.removePrefix("lbl:"))
    value.startsWith("cat:") -> Scope(value.removePrefix("cat:"))
    else -> Scope("")
}

/** Per-period wording, so a weekly budget doesn't say "left this month". */
private fun periodNoun(period: BudgetPeriod) = when (period) {
    BudgetPeriod.Weekly -> "this week"
    BudgetPeriod.Monthly -> "this month"
    BudgetPeriod.Yearly -> "this year"
}

private fun periodLabelOf(period: BudgetPeriod) = PERIOD_LABELS.getValue(period.toPeriodType())

/** A scope's display name and colour (null colour = the overall budget's register hatch). */
private data class ScopeLook(val name: String, val color: Color?)

/**
 * Port of web/src/pages/Budgets.tsx — route `/budgets`: an inline glass form (scope, period,
 * limit on the NumberPad, rollover) above one card per budget — overall first, then closest to
 * its limit — each with a health badge, a progress bar and an expandable 6-period history.
 */
@Composable
fun BudgetsScreen(nav: FinioNavigator) {
    val store = financeStore()
    val state by collectFinanceState()
    val money = rememberMoneyFormatter()
    val confirm = LocalConfirm.current
    val scope = rememberCoroutineScope()
    val colors = FinioTheme.colors

    val budgets = state.budgets
    val categories = state.categories
    val labels = state.labels
    val monthStartDay = normalizeMonthStartDay(state.settings.monthStartDay)

    var showForm by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var formScope by rememberSaveable { mutableStateOf(OVERALL_SCOPE) }
    var period by rememberSaveable { mutableStateOf(BudgetPeriod.Monthly) }
    var rollover by rememberSaveable { mutableStateOf(false) }
    var amount by rememberSaveable { mutableStateOf("") }
    var expandedId by rememberSaveable { mutableStateOf<String?>(null) }

    val expenseCategories = remember(categories) {
        miscLast(categories.filter { isCategoryValidForType(it, TransactionType.Expense) })
    }
    // Every scope value, in the order the picker lists them (Overall, categories, labels).
    val scopeCandidates = remember(expenseCategories, labels) {
        listOf(OVERALL_SCOPE) + expenseCategories.map { "cat:${it.id}" } + labels.map { "lbl:${it.id}" }
    }
    val takenScopes = remember(budgets) { budgets.map { encodeScope(it) }.toSet() }
    val allScopesTaken = firstFreeScope(scopeCandidates, takenScopes) == null

    val statuses = rememberDerived(budgets, state.transactions, monthStartDay) {
        computeBudgetStatuses(budgets, state.transactions, BudgetPeriodOptions(monthStartDay))
            // Overall first, then whatever is closest to its limit.
            .sortedWith { a, b ->
                val aOverall = a.budget.labelId.isNullOrEmpty() && a.budget.categoryId == ""
                val bOverall = b.budget.labelId.isNullOrEmpty() && b.budget.categoryId == ""
                if (aOverall != bOverall) (if (aOverall) -1 else 1) else b.percent.compareTo(a.percent)
            }
    }

    fun describe(categoryId: String, labelId: String?): ScopeLook {
        if (!labelId.isNullOrEmpty()) {
            val label = labels.find { it.id == labelId }
            return ScopeLook(label?.name ?: "Unknown label", label?.color?.let { parseHexColor(it) } ?: colors.mutedForeground)
        }
        if (categoryId == "") return ScopeLook("Overall expenses", null)
        val cat = categories.find { it.id == categoryId }
        return ScopeLook(cat?.name ?: "Unknown", cat?.color?.let { parseHexColor(it) } ?: colors.mutedForeground)
    }

    fun resetForm() {
        showForm = false
        editingId = null
        formScope = OVERALL_SCOPE
        period = BudgetPeriod.Monthly
        rollover = false
        amount = ""
    }

    fun startCreate() {
        resetForm()
        val free = firstFreeScope(scopeCandidates, takenScopes)
        if (free == null) {
            toast.error("Every category and label already has a budget")
            return
        }
        formScope = free
        showForm = true
    }

    fun startEdit(budget: Budget) {
        editingId = budget.id
        formScope = encodeScope(budget)
        period = budget.period
        rollover = budget.rollover
        amount = jsNumberString(budget.amount)
        showForm = true
    }

    // A scope already budgeted elsewhere can't be picked — except the budget being edited's own.
    val editingScope = editingId?.let { id -> budgets.find { it.id == id }?.let(::encodeScope) ?: OVERALL_SCOPE }
    fun isTaken(value: String) = value in takenScopes && value != editingScope
    fun takenSuffix(value: String) = if (isTaken(value)) " · already budgeted" else ""

    fun handleSubmit() {
        val parsed = amount.toDoubleOrNull() ?: 0.0
        if (parsed.isNaN() || parsed <= 0) {
            toast.error("Enter a valid budget amount")
            return
        }
        val decoded = decodeScope(formScope)
        val key = budgetScopeKey(decoded.categoryId, decoded.labelId)
        val clash = budgets.find { it.id != editingId && budgetScopeKey(it) == key }
        if (clash != null) {
            toast.error("${describe(clash.categoryId, clash.labelId).name} already has a budget")
            return
        }
        val id = editingId
        if (id != null) {
            store.updateBudget(id) {
                it.copy(categoryId = decoded.categoryId, labelId = decoded.labelId, amount = parsed, period = period, rollover = rollover)
            }
            toast.success("Budget updated")
        } else {
            store.addBudget(NewBudget(decoded.categoryId, decoded.labelId, parsed, period, rollover))
            toast.success("Budget saved")
        }
        resetForm()
    }

    FinioScreen(header = {
        BackButton(nav)
        ScreenTitle("Budgets")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HideAmountsToggle()
            HeaderIconButton(
                LucideIcons.Plus,
                "Add budget",
                onClick = { if (showForm) resetForm() else startCreate() },
                tone = HeaderIconTone.Primary,
                enabled = !(allScopesTaken && !showForm),
            )
        }
    }) {
        if (showForm) {
            FinioCard(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    FormField("Scope") {
                        // The closed field reads the scope's display name ("Overall expenses"), like
                        // the web's `<SelectValue>{describe(…).name}</SelectValue>`.
                        fun shown(v: String) = decodeScope(v).let { describe(it.categoryId, it.labelId).name }
                        val options = buildList {
                            add(SelectOption(OVERALL_SCOPE, "Overall (all expenses)${takenSuffix(OVERALL_SCOPE)}", enabled = !isTaken(OVERALL_SCOPE), selectedLabel = shown(OVERALL_SCOPE)))
                            expenseCategories.forEach { c ->
                                val v = "cat:${c.id}"
                                add(SelectOption(v, "${c.name}${takenSuffix(v)}", enabled = !isTaken(v), selectedLabel = shown(v)))
                            }
                            labels.forEach { l ->
                                val v = "lbl:${l.id}"
                                add(SelectOption(v, "Label · ${l.name}${takenSuffix(v)}", enabled = !isTaken(v), selectedLabel = shown(v)))
                            }
                        }
                        FinioSelect(formScope, options, { formScope = it }, Modifier.fillMaxWidth(), title = "Scope")
                    }
                    FormField("Period") {
                        FormPills(
                            options = listOf(BudgetPeriod.Weekly, BudgetPeriod.Monthly, BudgetPeriod.Yearly).map { it to periodLabelOf(it) },
                            selected = period,
                            onSelect = { period = it },
                        )
                    }
                    FormField("${periodLabelOf(period)} limit") {
                        NumberPad(amount, { amount = it })
                    }
                    SwitchField(
                        title = "Roll over unspent",
                        description = "Carry what's left (or overspent) into the next period",
                        checked = rollover,
                        onCheckedChange = { rollover = it },
                    )
                    FormActions(if (editingId != null) "Save changes" else "Save", ::handleSubmit, ::resetForm)
                }
            }
        }

        when {
            statuses == null -> SectionLoader()
            statuses.isEmpty() -> PlanningEmpty(LucideIcons.Target, "No budgets yet", "Create your first budget", ::startCreate)
            else -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                statuses.forEach { s ->
                    val look = describe(s.budget.categoryId, s.budget.labelId)
                    BudgetCard(
                        status = s,
                        monthStartDay = monthStartDay,
                        look = look,
                        money = money,
                        expanded = expandedId == s.budget.id,
                        onToggleHistory = { expandedId = if (expandedId == s.budget.id) null else s.budget.id },
                        onEdit = { startEdit(s.budget) },
                        onDelete = {
                            scope.launch {
                                val ok = confirm.confirm(
                                    com.slowatcoding.finio.ui.components.ConfirmOptions(
                                        title = "Delete budget for \"${look.name}\"?",
                                        description = "Your transactions are unaffected — only the limit is removed.",
                                        confirmLabel = "Delete budget",
                                    ),
                                )
                                if (ok) store.deleteBudget(s.budget.id)
                            }
                        },
                    )
                }
            }
        }
    }
}

/** `String(n)` for an amount being put back into the NumberPad: 500 → "500", 499.5 → "499.5". */
internal fun jsNumberString(n: Double): String = com.slowatcoding.finio.core.js.jsNumberToString(n)

@Composable
private fun BudgetCard(
    status: BudgetStatus,
    monthStartDay: Int,
    look: ScopeLook,
    money: MoneyFormatter,
    expanded: Boolean,
    onToggleHistory: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = FinioTheme.colors
    val state by collectFinanceState()
    val budget = status.budget
    val history = rememberDerived(expanded, budget, state.transactions, monthStartDay) {
        if (expanded) computeBudgetHistory(budget, state.transactions, BudgetPeriodOptions(monthStartDay), 6) else emptyList()
    }
    val density = LocalDensity.current.density
    // The overall budget wears the register hatch (as on the Dashboard); a scoped one its own colour.
    val okFill: Brush = if (look.color == null) FinioTheme.brushes.register(density)
    else Brush.horizontalGradient(listOf(look.color, look.color.mix(0.8f)))
    val spentOf = "${money(status.spent)} of ${money(status.limit)}"

    FinioCard(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(look.name, style = FinioType.bodyMedium, color = colors.foreground, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${periodLabelOf(budget.period)} · ${periodLabel(status.range, monthStartDay)}",
                    style = Micro, color = colors.mutedForeground, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            CardIconAction(LucideIcons.Pencil, "Edit budget for ${look.name}", colors.mutedForeground, onEdit)
            CardIconAction(LucideIcons.Trash2, "Delete budget for ${look.name}", colors.destructive, onDelete)
        }

        Row(
            Modifier.fillMaxWidth().padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                spentOf,
                Modifier.weight(1f),
                style = if (status.isOver) FinioType.label else FinioType.caption,
                color = if (status.isOver) colors.destructive else colors.mutedForeground,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                BudgetHealthBadge(budgetHealth(status))
                Text(
                    "${status.percent.roundToInt()}%",
                    style = FinioType.label,
                    color = if (status.isOver) colors.destructive else colors.mutedForeground,
                )
            }
        }
        BudgetProgressBar(status.percent.toFloat(), status.isOver, okFill, valueText = "$spentOf spent")
        Text(
            if (status.isOver) "Over by ${money(-status.remaining)}" else "${money(status.remaining)} left ${periodNoun(budget.period)}",
            Modifier.padding(top = 6.dp),
            style = Micro,
            color = colors.mutedForeground,
        )
        if (budget.rollover && status.carryover != 0.0) {
            Text(
                if (status.carryover > 0) "Includes ${money(status.carryover)} rolled over"
                else "Includes ${money(-status.carryover)} overspend carried in",
                style = Micro,
                color = colors.warning,
            )
        }

        DisclosureToggle("History", expanded, onToggleHistory)

        if (expanded) {
            DisclosurePanel {
                val rows = history
                when {
                    rows == null -> {}
                    rows.isEmpty() -> Text(
                        "No completed periods yet — this budget started ${periodLabel(status.range, monthStartDay)}.",
                        style = Micro,
                        color = colors.mutedForeground,
                    )
                    else -> rows.forEach { h ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(periodShortLabel(h.range), Modifier.width(56.dp), style = Micro, color = colors.mutedForeground, maxLines = 1)
                            Box(
                                Modifier.weight(1f).height(6.dp).clip(FinioShapes.full).background(colors.muted),
                            ) {
                                val fraction = if (h.limit > 0) min(h.spent / h.limit, 1.0).toFloat() else 0f
                                Box(
                                    Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(max(0f, fraction))
                                        .clip(FinioShapes.full)
                                        .background(if (h.isOver) colors.destructive else colors.primary),
                                )
                            }
                            val tone = if (h.isOver) colors.destructive else colors.mutedForeground
                            Row(
                                Modifier.width(128.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    if (h.isOver) LucideIcons.TriangleAlert else LucideIcons.Check,
                                    if (h.isOver) "Over budget" else "Within budget",
                                    Modifier.size(10.dp),
                                    tint = tone,
                                )
                                Text(
                                    "${money(h.spent, compact = true)} / ${money(h.limit, compact = true)}",
                                    style = Micro,
                                    color = tone,
                                    textAlign = TextAlign.End,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
