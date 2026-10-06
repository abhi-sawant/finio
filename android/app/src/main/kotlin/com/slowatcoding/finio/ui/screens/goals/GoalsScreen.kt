package com.slowatcoding.finio.ui.screens.goals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.foundation.clickable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.slowatcoding.finio.core.calc.GoalStatus
import com.slowatcoding.finio.core.calc.activeAccounts
import com.slowatcoding.finio.core.calc.computeGoalStatus
import com.slowatcoding.finio.core.data.COLOR_PALETTE
import com.slowatcoding.finio.core.format.formatDayMonth
import com.slowatcoding.finio.core.format.formatShortDate
import com.slowatcoding.finio.core.format.localDayKey
import com.slowatcoding.finio.core.format.todayKey
import com.slowatcoding.finio.core.js.localDate
import com.slowatcoding.finio.core.js.nowInstant
import com.slowatcoding.finio.core.js.toIso
import com.slowatcoding.finio.core.model.Goal
import com.slowatcoding.finio.core.model.GoalContribution
import com.slowatcoding.finio.core.store.NewContribution
import com.slowatcoding.finio.core.store.NewGoal
import com.slowatcoding.finio.core.util.MAX_NAME_LENGTH
import com.slowatcoding.finio.core.util.MAX_NOTE_LENGTH
import com.slowatcoding.finio.core.util.cleanText
import com.slowatcoding.finio.core.util.isPastDay
import com.slowatcoding.finio.core.util.stripLeading
import com.slowatcoding.finio.ui.common.BackButton
import com.slowatcoding.finio.ui.common.HideAmountsToggle
import com.slowatcoding.finio.ui.common.MoneyFormatter
import com.slowatcoding.finio.ui.common.collectFinanceState
import com.slowatcoding.finio.ui.common.financeStore
import com.slowatcoding.finio.ui.common.rememberMoneyFormatter
import com.slowatcoding.finio.ui.common.undoToast
import com.slowatcoding.finio.ui.components.ButtonVariant
import com.slowatcoding.finio.ui.components.FinioCard
import com.slowatcoding.finio.ui.components.FinioDatePicker
import com.slowatcoding.finio.ui.components.FinioDialog
import com.slowatcoding.finio.ui.components.FinioScreen
import com.slowatcoding.finio.ui.components.FinioSelect
import com.slowatcoding.finio.ui.components.FinioTextField
import com.slowatcoding.finio.ui.components.GoalIconNames
import com.slowatcoding.finio.ui.components.HeaderIconButton
import com.slowatcoding.finio.ui.components.HeaderIconTone
import com.slowatcoding.finio.ui.components.LocalConfirm
import com.slowatcoding.finio.ui.components.NumberPad
import com.slowatcoding.finio.ui.components.ScreenTitle
import com.slowatcoding.finio.ui.components.SelectOption
import com.slowatcoding.finio.ui.components.toast
import com.slowatcoding.finio.ui.icons.LucideIcons
import com.slowatcoding.finio.ui.mudra.ThreadProgressBar
import com.slowatcoding.finio.ui.navigation.FinioNavigator
import com.slowatcoding.finio.ui.screens.budgets.CardIconAction
import com.slowatcoding.finio.ui.screens.budgets.ColorSwatches
import com.slowatcoding.finio.ui.screens.budgets.DisclosurePanel
import com.slowatcoding.finio.ui.screens.budgets.DisclosureToggle
import com.slowatcoding.finio.ui.screens.budgets.FormActions
import com.slowatcoding.finio.ui.screens.budgets.FormField
import com.slowatcoding.finio.ui.screens.budgets.IconChoiceGrid
import com.slowatcoding.finio.ui.screens.budgets.Micro
import com.slowatcoding.finio.ui.screens.budgets.PlanningEmpty
import com.slowatcoding.finio.ui.screens.budgets.PositiveTintButton
import com.slowatcoding.finio.ui.screens.budgets.Tiny
import com.slowatcoding.finio.ui.screens.budgets.TintButton
import com.slowatcoding.finio.ui.screens.budgets.jsNumberString
import com.slowatcoding.finio.ui.theme.FinioShapes
import com.slowatcoding.finio.ui.theme.FinioTheme
import com.slowatcoding.finio.ui.theme.FinioType
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

private const val NO_ACCOUNT = "__none__"

private enum class ContributionMode { Add, Withdraw }

private data class ContributionTarget(val goal: Goal, val mode: ContributionMode)

private fun LocalDate.toIsoStartOfDay(): String = localDate(year, monthValue - 1, dayOfMonth).toIso()

/**
 * Port of web/src/pages/Goals.tsx — route `/goals`: the inline goal form (name, icon, colour,
 * target on the NumberPad, optional deadline and informational linked account) above one card per
 * goal — in progress first (closest to done first) — with the thread progress bar, Add funds /
 * Withdraw dialog and an expandable contribution history whose rows delete with Undo.
 */
@Composable
fun GoalsScreen(nav: FinioNavigator) {
    val store = financeStore()
    val state by collectFinanceState()
    val money = rememberMoneyFormatter()
    val confirm = LocalConfirm.current
    val scope = rememberCoroutineScope()
    val colors = FinioTheme.colors

    val goals = state.goals
    val contributions = state.goalContributions
    val accounts = state.accounts

    var showForm by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var name by rememberSaveable { mutableStateOf("") }
    var icon by rememberSaveable { mutableStateOf(GoalIconNames.first()) }
    var color by rememberSaveable { mutableStateOf(COLOR_PALETTE.first()) }
    var targetAmount by rememberSaveable { mutableStateOf("") }
    var targetDate by remember { mutableStateOf<LocalDate?>(null) }
    var linkedAccountId by rememberSaveable { mutableStateOf(NO_ACCOUNT) }
    var expandedId by rememberSaveable { mutableStateOf<String?>(null) }
    var contributionGoal by remember { mutableStateOf<ContributionTarget?>(null) }
    var contributionAmount by remember { mutableStateOf("") }
    var contributionNote by remember { mutableStateOf("") }

    val openAccounts = remember(accounts) { activeAccounts(accounts) }
    val statuses = remember(goals, contributions) { goals.map { computeGoalStatus(it, contributions) } }
    // In-progress goals first (closest to done first), completed goals trail behind.
    val sortedStatuses = remember(statuses) {
        statuses.sortedWith { a, b ->
            if (a.isComplete != b.isComplete) (if (a.isComplete) 1 else -1) else b.percent.compareTo(a.percent)
        }
    }

    fun resetForm() {
        showForm = false
        editingId = null
        name = ""
        icon = GoalIconNames.first()
        color = COLOR_PALETTE.first()
        targetAmount = ""
        targetDate = null
        linkedAccountId = NO_ACCOUNT
    }

    fun startCreate() {
        resetForm()
        showForm = true
    }

    fun startEdit(goal: Goal) {
        editingId = goal.id
        name = goal.name
        icon = goal.icon
        color = goal.color
        targetAmount = jsNumberString(goal.targetAmount)
        targetDate = goal.targetDate?.takeIf { it.isNotEmpty() }?.let { LocalDate.parse(localDayKey(it)) }
        linkedAccountId = goal.linkedAccountId ?: NO_ACCOUNT
        showForm = true
    }

    fun handleSubmit() {
        val parsed = targetAmount.toDoubleOrNull() ?: 0.0
        val cleanName = cleanText(name, MAX_NAME_LENGTH)
        if (cleanName.isEmpty()) {
            toast.error("Enter a goal name")
            return
        }
        if (parsed.isNaN() || parsed <= 0) {
            toast.error("Enter a valid target amount")
            return
        }
        // A deadline in the past is meaningless — but don't block saving an old goal whose date is
        // simply unchanged.
        val existingGoal = editingId?.let { id -> goals.find { it.id == id } }
        val dayKey = targetDate?.toString() ?: ""
        val unchangedDate = !existingGoal?.targetDate.isNullOrEmpty() && localDayKey(existingGoal!!.targetDate!!) == dayKey
        if (dayKey.isNotEmpty() && !unchangedDate && isPastDay(dayKey, todayKey())) {
            toast.error("Target date must be today or later")
            return
        }
        val isoDate = targetDate?.toIsoStartOfDay()
        val linked = if (linkedAccountId != NO_ACCOUNT) linkedAccountId else null
        val id = editingId
        if (id != null) {
            store.updateGoal(id) {
                it.copy(name = cleanName, icon = icon, color = color, targetAmount = parsed, targetDate = isoDate, linkedAccountId = linked)
            }
            toast.success("Goal updated")
        } else {
            store.addGoal(NewGoal(cleanName, icon, color, parsed, isoDate, linked))
            toast.success("Goal created")
        }
        resetForm()
    }

    fun openContribution(goal: Goal, mode: ContributionMode) {
        contributionGoal = ContributionTarget(goal, mode)
        contributionAmount = ""
        contributionNote = ""
    }

    val withdrawLimit = contributionGoal?.let { t -> max(0.0, statuses.find { it.goal.id == t.goal.id }?.current ?: 0.0) } ?: 0.0

    fun handleContributionSubmit() {
        val target = contributionGoal ?: return
        val parsed = contributionAmount.toDoubleOrNull() ?: 0.0
        if (parsed.isNaN() || parsed <= 0) {
            toast.error("Enter a valid amount")
            return
        }
        if (target.mode == ContributionMode.Withdraw && parsed > withdrawLimit + 0.005) {
            toast.error(
                if (withdrawLimit > 0) "Only ${money(withdrawLimit)} is saved in this goal"
                else "Nothing saved in this goal to withdraw",
            )
            return
        }
        store.addContribution(
            NewContribution(
                goalId = target.goal.id,
                amount = if (target.mode == ContributionMode.Withdraw) -parsed else parsed,
                date = nowInstant().toIso(),
                note = cleanText(contributionNote, MAX_NOTE_LENGTH),
            ),
        )
        toast.success(if (target.mode == ContributionMode.Withdraw) "Withdrawal logged" else "Contribution added")
        contributionGoal = null
    }

    FinioScreen(header = {
        BackButton(nav)
        ScreenTitle("Savings goals")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HideAmountsToggle()
            HeaderIconButton(
                LucideIcons.Plus,
                "Add goal",
                onClick = { if (showForm) resetForm() else startCreate() },
                tone = HeaderIconTone.Primary,
            )
        }
    }) {
        if (showForm) {
            FinioCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    FormField("Goal name") {
                        FinioTextField(
                            name,
                            { name = stripLeading(it).take(MAX_NAME_LENGTH) },
                            Modifier.fillMaxWidth(),
                            placeholder = "e.g., Emergency Fund",
                        )
                    }
                    FormField("Icon") { IconChoiceGrid(GoalIconNames, icon) { icon = it } }
                    FormField("Color") { ColorSwatches(color) { color = it } }
                    FormField("Target amount") { NumberPad(targetAmount, { targetAmount = it }) }
                    FormField("Target date (optional)") {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            FinioDatePicker(
                                targetDate,
                                { targetDate = it },
                                Modifier.weight(1f),
                                placeholder = "No deadline",
                                minDate = LocalDate.parse(todayKey()),
                            )
                            if (targetDate != null) {
                                TintButton("Clear", { targetDate = null })
                            }
                        }
                    }
                    if (openAccounts.isNotEmpty()) {
                        FormField("Linked account (optional)") {
                            FinioSelect(
                                if (linkedAccountId == NO_ACCOUNT || openAccounts.any { it.id == linkedAccountId }) linkedAccountId else NO_ACCOUNT,
                                listOf(SelectOption(NO_ACCOUNT, "None")) + openAccounts.map { SelectOption(it.id, it.name) },
                                { linkedAccountId = it },
                                Modifier.fillMaxWidth(),
                                title = "Linked account",
                            )
                            Text(
                                "Just a label — this account's balance and transactions aren't affected.",
                                Modifier.padding(top = 6.dp),
                                style = Tiny,
                                color = colors.mutedForeground,
                            )
                        }
                    }
                    FormActions(
                        if (editingId != null) "Save changes" else "Save",
                        ::handleSubmit,
                        ::resetForm,
                        cancelVariant = ButtonVariant.Secondary,
                    )
                }
            }
        }

        if (sortedStatuses.isEmpty()) {
            PlanningEmpty(LucideIcons.PiggyBank, "No savings goals yet", "Create your first goal", ::startCreate)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                sortedStatuses.forEach { status ->
                    val goal = status.goal
                    GoalCard(
                        status = status,
                        linkedAccountName = goal.linkedAccountId?.let { id -> accounts.find { it.id == id }?.name },
                        contributions = contributions.filter { it.goalId == goal.id },
                        money = money,
                        expanded = expandedId == goal.id,
                        onToggleHistory = { expandedId = if (expandedId == goal.id) null else goal.id },
                        onAddFunds = { openContribution(goal, ContributionMode.Add) },
                        onWithdraw = { openContribution(goal, ContributionMode.Withdraw) },
                        onEdit = { startEdit(goal) },
                        onDelete = {
                            scope.launch {
                                val ok = confirm.confirm(
                                    "Delete \"${goal.name}\"?",
                                    "Every contribution logged against this goal will be deleted too. This cannot be undone.",
                                    confirmLabel = "Delete goal",
                                )
                                if (ok) store.deleteGoal(goal.id)
                            }
                        },
                        onDeleteContribution = { id ->
                            val removed = store.deleteContribution(id)
                            if (removed != null) undoToast("Contribution removed") { store.restoreContribution(removed) }
                        },
                    )
                }
            }
        }
    }

    // Add funds / withdraw dialog
    contributionGoal?.let { target ->
        val withdraw = target.mode == ContributionMode.Withdraw
        FinioDialog(
            onDismissRequest = { contributionGoal = null },
            title = "${if (withdraw) "Withdraw from" else "Add funds to"} ${target.goal.name}",
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                NumberPad(contributionAmount, { contributionAmount = it })
                if (withdraw) {
                    Text(
                        "${money(withdrawLimit)} saved — the most you can withdraw.",
                        style = FinioType.caption,
                        color = colors.mutedForeground,
                    )
                }
                FinioTextField(
                    contributionNote,
                    { contributionNote = stripLeading(it).take(MAX_NOTE_LENGTH) },
                    Modifier.fillMaxWidth(),
                    placeholder = "Note (optional)",
                )
                FormActions(
                    if (withdraw) "Withdraw" else "Add",
                    ::handleContributionSubmit,
                    { contributionGoal = null },
                    cancelVariant = ButtonVariant.Secondary,
                )
            }
        }
    }
}

@Composable
private fun GoalCard(
    status: GoalStatus,
    linkedAccountName: String?,
    contributions: List<GoalContribution>,
    money: MoneyFormatter,
    expanded: Boolean,
    onToggleHistory: () -> Unit,
    onAddFunds: () -> Unit,
    onWithdraw: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDeleteContribution: (String) -> Unit,
) {
    val colors = FinioTheme.colors
    val goal = status.goal
    FinioCard {
        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(goal.name, style = FinioType.bodyMedium, color = colors.foreground, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    (if (linkedAccountName != null) "Linked · $linkedAccountName" else "No linked account") +
                        (goal.targetDate?.takeIf { it.isNotEmpty() }?.let { " · by ${formatShortDate(it)}" } ?: ""),
                    style = Micro, color = colors.mutedForeground, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            CardIconAction(LucideIcons.Pencil, "Edit ${goal.name}", colors.mutedForeground, onEdit)
            CardIconAction(LucideIcons.Trash2, "Delete ${goal.name}", colors.destructive, onDelete)
        }

        val tone = if (status.isComplete) colors.positive else colors.mutedForeground
        Row(Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
            Text(
                "${money(status.current)} of ${money(goal.targetAmount)}",
                Modifier.weight(1f),
                style = if (status.isComplete) FinioType.label else FinioType.caption,
                color = tone,
            )
            Text("${status.percent.roundToInt()}%", style = FinioType.label, color = tone)
        }
        ThreadProgressBar(status.percent.toFloat())
        Text(
            (if (status.isComplete) "Goal reached" else "${money(status.remaining)} to go") +
                (if (!status.isComplete && status.projectedDate != null) " · at this pace, by ${formatShortDate(status.projectedDate!!)}" else ""),
            Modifier.padding(top = 6.dp),
            style = Micro,
            color = colors.mutedForeground,
        )

        Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PositiveTintButton("Add funds", LucideIcons.Plus, onAddFunds, Modifier.weight(1f))
            TintButton("Withdraw", onWithdraw, Modifier.weight(1f), icon = LucideIcons.Minus)
        }

        DisclosureToggle("History", expanded, onToggleHistory)

        if (expanded) {
            DisclosurePanel {
                if (contributions.isEmpty()) {
                    Text("No contributions logged yet.", style = Micro, color = colors.mutedForeground)
                } else {
                    contributions.forEach { c ->
                        LedgerRow(
                            date = formatDayMonth(c.date),
                            label = c.note.ifEmpty { if (c.amount < 0) "Withdrawal" else "Contribution" },
                            amount = (if (c.amount < 0) "-" else "+") + money(abs(c.amount), compact = true),
                            negative = c.amount < 0,
                            onDelete = { onDeleteContribution(c.id) },
                            deleteLabel = "Delete contribution",
                        )
                    }
                }
            }
        }
    }
}

/** A history row (`text-[11px]`): date, note, signed amount, trailing icon actions. */
@Composable
fun LedgerRow(
    date: String,
    label: String,
    amount: String,
    negative: Boolean,
    onDelete: () -> Unit,
    deleteLabel: String,
    dateWidth: androidx.compose.ui.unit.Dp = 56.dp,
    onEdit: (() -> Unit)? = null,
    editLabel: String = "Edit entry",
    amountPositiveColor: androidx.compose.ui.graphics.Color? = null,
) {
    val colors = FinioTheme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(date, Modifier.width(dateWidth), style = Micro, color = colors.mutedForeground, maxLines = 1)
        Text(label, Modifier.weight(1f), style = Micro, color = colors.foreground, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            amount,
            style = Micro.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium),
            color = if (negative) colors.destructive else (amountPositiveColor ?: colors.positive),
        )
        if (onEdit != null) SmallRowIcon(LucideIcons.Pencil, editLabel, onEdit)
        SmallRowIcon(LucideIcons.Trash2, deleteLabel, onDelete)
    }
}

@Composable
private fun SmallRowIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    val colors = FinioTheme.colors
    androidx.compose.foundation.layout.Box(
        Modifier
            .size(24.dp)
            .clip(FinioShapes.full)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, Modifier.size(12.dp), tint = colors.mutedForeground)
    }
}

