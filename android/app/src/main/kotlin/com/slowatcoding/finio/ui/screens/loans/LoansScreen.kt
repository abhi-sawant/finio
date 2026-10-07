package com.slowatcoding.finio.ui.screens.loans

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slowatcoding.finio.core.format.formatShortDate
import com.slowatcoding.finio.core.format.todayKey
import com.slowatcoding.finio.core.js.localDate
import com.slowatcoding.finio.core.js.toIso
import com.slowatcoding.finio.core.loan.LoanPrepaymentInput
import com.slowatcoding.finio.core.loan.loanStatus
import com.slowatcoding.finio.core.loan.maxPrepayment
import com.slowatcoding.finio.core.loan.scheduleInput
import com.slowatcoding.finio.core.loan.simulatePrepaymentImpact
import com.slowatcoding.finio.core.model.Loan
import com.slowatcoding.finio.core.model.LoanPrepayment
import com.slowatcoding.finio.core.store.NewLoanPrepayment
import com.slowatcoding.finio.core.util.MAX_NOTE_LENGTH
import com.slowatcoding.finio.core.util.cleanText
import com.slowatcoding.finio.core.util.stripLeading
import com.slowatcoding.finio.ui.common.BackButton
import com.slowatcoding.finio.ui.common.HideAmountsToggle
import com.slowatcoding.finio.ui.common.MoneyFormatter
import com.slowatcoding.finio.ui.common.collectFinanceState
import com.slowatcoding.finio.ui.common.financeStore
import com.slowatcoding.finio.ui.common.rememberMoneyFormatter
import com.slowatcoding.finio.ui.common.undoToast
import com.slowatcoding.finio.ui.components.ButtonSize
import com.slowatcoding.finio.ui.components.ButtonVariant
import com.slowatcoding.finio.ui.components.FinioButton
import com.slowatcoding.finio.ui.components.FinioCard
import com.slowatcoding.finio.ui.components.FinioDatePicker
import com.slowatcoding.finio.ui.components.FinioDialog
import com.slowatcoding.finio.ui.components.FinioScreen
import com.slowatcoding.finio.ui.components.FinioTextField
import com.slowatcoding.finio.ui.components.HeaderIconButton
import com.slowatcoding.finio.ui.components.HeaderIconTone
import com.slowatcoding.finio.ui.components.LocalConfirm
import com.slowatcoding.finio.ui.components.NumberPad
import com.slowatcoding.finio.ui.components.ScreenTitle
import com.slowatcoding.finio.ui.components.toast
import com.slowatcoding.finio.ui.icons.LucideIcons
import com.slowatcoding.finio.ui.navigation.FinioNavigator
import com.slowatcoding.finio.ui.navigation.Routes
import com.slowatcoding.finio.ui.screens.budgets.AccentTintButton
import com.slowatcoding.finio.ui.screens.budgets.CardIconAction
import com.slowatcoding.finio.ui.screens.budgets.DisclosurePanel
import com.slowatcoding.finio.ui.screens.budgets.DisclosureToggle
import com.slowatcoding.finio.ui.screens.budgets.FormActions
import com.slowatcoding.finio.ui.screens.budgets.Micro
import com.slowatcoding.finio.ui.screens.budgets.PlanningEmpty
import com.slowatcoding.finio.ui.screens.budgets.TintButton
import com.slowatcoding.finio.ui.screens.goals.LedgerRow
import com.slowatcoding.finio.ui.theme.FinioShapes
import com.slowatcoding.finio.ui.theme.FinioTheme
import com.slowatcoding.finio.ui.theme.FinioType
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.min

/** `new Date(`${day}T00:00:00`).toISOString()` — local midnight of a picked day. */
internal fun LocalDate.isoAtLocalMidnight(): String = localDate(year, monthValue - 1, dayOfMonth).toIso()

internal const val DELETE_LOAN_DESCRIPTION =
    "Every prepayment logged against it and its auto-generated EMI rule will be removed too. EMI transactions already posted stay in your history. This cannot be undone."

/**
 * Port of web/src/pages/Loans.tsx — route `/loans`: the outstanding total, one card per open loan
 * (EMI, next due, installment progress, prepay / mark paid off, details with the schedule link and
 * prepayment ledger) and a collapsible "Paid off" group. The prepay dialog previews the saving with
 * core `simulatePrepaymentImpact` and is capped by `maxPrepayment`.
 */
@Composable
fun LoansScreen(nav: FinioNavigator) {
    val store = financeStore()
    val state by collectFinanceState()
    val money = rememberMoneyFormatter()
    val confirm = LocalConfirm.current
    val scope = rememberCoroutineScope()
    val colors = FinioTheme.colors

    val loans = state.loans
    val loanPrepayments = state.loanPrepayments

    var expandedId by rememberSaveable { mutableStateOf<String?>(null) }
    var prepayLoanId by rememberSaveable { mutableStateOf<String?>(null) }
    var prepayAmount by remember { mutableStateOf("0") }
    var prepayDate by remember { mutableStateOf<LocalDate?>(LocalDate.parse(todayKey())) }
    var prepayNote by remember { mutableStateOf("") }
    var showClosed by rememberSaveable { mutableStateOf(false) }

    val activeLoans = remember(loans) { loans.filter { it.closedAt.isNullOrEmpty() } }
    val closedLoans = remember(loans) { loans.filter { !it.closedAt.isNullOrEmpty() } }
    val prepaymentsByLoan = remember(loanPrepayments) { loanPrepayments.groupBy { it.loanId } }

    val totalOutstanding = remember(activeLoans, prepaymentsByLoan) {
        activeLoans.sumOf { loan -> loanStatus(loan.scheduleInput(prepaymentsByLoan[loan.id].orEmpty())).outstandingBalance }
    }

    val prepayLoan = prepayLoanId?.let { id -> loans.find { it.id == id } }

    fun openPrepay(loan: Loan) {
        prepayLoanId = loan.id
        prepayAmount = "0"
        prepayDate = LocalDate.parse(todayKey())
        prepayNote = ""
    }

    val prepayLimit = remember(prepayLoan, prepaymentsByLoan) {
        prepayLoan?.let { maxPrepayment(it.scheduleInput(prepaymentsByLoan[it.id].orEmpty())) } ?: 0.0
    }
    val parsedPrepay = prepayAmount.toDoubleOrNull()?.takeUnless { it.isNaN() } ?: 0.0
    val prepayOverLimit = parsedPrepay > prepayLimit + 0.005
    val prepayImpact = remember(prepayLoan, parsedPrepay, prepayDate, prepaymentsByLoan) {
        val loan = prepayLoan ?: return@remember null
        val day = prepayDate ?: return@remember null
        if (parsedPrepay <= 0) return@remember null
        simulatePrepaymentImpact(
            loan.scheduleInput(prepaymentsByLoan[loan.id].orEmpty()),
            LoanPrepaymentInput(parsedPrepay, day.isoAtLocalMidnight()),
        )
    }

    fun handlePrepaySubmit() {
        val loan = prepayLoan ?: return
        if (parsedPrepay <= 0) {
            toast.error("Enter a valid amount")
            return
        }
        if (prepayOverLimit) {
            toast.error("Only ${money(prepayLimit)} is still owed on this loan")
            return
        }
        val day = prepayDate ?: LocalDate.parse(todayKey())
        store.addLoanPrepayment(NewLoanPrepayment(loan.id, parsedPrepay, day.isoAtLocalMidnight(), cleanText(prepayNote, MAX_NOTE_LENGTH)))
        toast.success("Prepayment recorded on \"${loan.name}\"")
        prepayLoanId = null
    }

    fun handleDelete(loan: Loan) {
        scope.launch {
            val ok = confirm.confirm("Delete \"${loan.name}\"?", DELETE_LOAN_DESCRIPTION, confirmLabel = "Delete loan")
            if (ok) store.deleteLoan(loan.id)
        }
    }

    fun handleToggleClosed(loan: Loan) {
        if (!loan.closedAt.isNullOrEmpty()) {
            store.setLoanClosed(loan.id, false)
            toast.success("\"${loan.name}\" reopened")
            return
        }
        store.setLoanClosed(loan.id, true)
        undoToast("\"${loan.name}\" marked paid off") { store.setLoanClosed(loan.id, false) }
    }

    @Composable
    fun Card(loan: Loan) {
        LoanCard(
            loan = loan,
            prepayments = prepaymentsByLoan[loan.id].orEmpty(),
            money = money,
            expanded = expandedId == loan.id,
            onToggle = { expandedId = if (expandedId == loan.id) null else loan.id },
            onEdit = { nav.navigate(Routes.EditLoan(loan.id)) },
            onDelete = { handleDelete(loan) },
            onToggleClosed = { handleToggleClosed(loan) },
            onAddPrepayment = { openPrepay(loan) },
            onSchedule = { nav.navigate(Routes.LoanSchedule(loan.id)) },
            onDeletePrepayment = { id ->
                val removed = store.deleteLoanPrepayment(id)
                if (removed != null) undoToast("Prepayment entry removed") { store.restoreLoanPrepayment(removed) }
            },
        )
    }

    FinioScreen(header = {
        BackButton(nav)
        ScreenTitle("Loans")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HideAmountsToggle()
            HeaderIconButton(LucideIcons.Plus, "Add loan", onClick = { nav.navigate(Routes.AddLoan) }, tone = HeaderIconTone.Primary)
        }
    }) {
        if (activeLoans.isNotEmpty()) {
            FinioCard(Modifier.fillMaxWidth()) {
                Text(
                    "Outstanding across ${activeLoans.size} loan${if (activeLoans.size == 1) "" else "s"}",
                    style = FinioType.label,
                    color = colors.mutedForeground,
                )
                Text(
                    money(totalOutstanding, compact = true),
                    Modifier.padding(top = 4.dp),
                    style = FinioType.money.copy(fontSize = 20.sp, lineHeight = 28.sp),
                    color = colors.foreground,
                )
            }
        }

        if (activeLoans.isEmpty() && closedLoans.isNotEmpty()) {
            // Every loan is paid off — say so, rather than leaving the page blank above a
            // collapsed "Paid off" toggle.
            PlanningEmpty(
                icon = LucideIcons.CircleCheck,
                iconTint = colors.positive,
                title = "All loans paid off",
                message = "${closedLoans.size} loan${if (closedLoans.size == 1) "" else "s"} closed. Nothing outstanding.",
                actionLabel = "Add loan",
                onAction = { nav.navigate(Routes.AddLoan) },
            )
        }

        if (activeLoans.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { activeLoans.forEach { Card(it) } }
        }

        if (closedLoans.isNotEmpty()) {
            Column {
                val turn by animateFloatAsState(if (showClosed) 180f else 0f, tween(150), label = "closed")
                Row(
                    Modifier
                        .padding(bottom = 8.dp)
                        .clip(FinioShapes.sm)
                        .clickable(role = Role.Button) { showClosed = !showClosed },
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(LucideIcons.ChevronDown, null, Modifier.size(14.dp).rotate(turn), tint = colors.mutedForeground)
                    Text("Paid off (${closedLoans.size})", style = FinioType.bodyMedium, color = colors.mutedForeground)
                }
                if (showClosed) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { closedLoans.forEach { Card(it) } }
                }
            }
        }

        if (loans.isEmpty()) {
            PlanningEmpty(LucideIcons.Landmark, "No loans yet", "Add loan", { nav.navigate(Routes.AddLoan) })
        }
    }

    prepayLoan?.let { loan ->
        FinioDialog(onDismissRequest = { prepayLoanId = null }, title = "Prepay \"${loan.name}\"") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                NumberPad(prepayAmount, { prepayAmount = it })
                FinioDatePicker(prepayDate, { prepayDate = it }, Modifier.fillMaxWidth(), placeholder = "Pick a date")
                FinioTextField(
                    prepayNote,
                    { prepayNote = stripLeading(it).take(MAX_NOTE_LENGTH) },
                    Modifier.fillMaxWidth(),
                    placeholder = "Note (optional)",
                )
                Text(
                    "Outstanding: ${money(prepayLimit)} — the most you can prepay.",
                    style = FinioType.caption,
                    color = if (prepayOverLimit) colors.destructive else colors.mutedForeground,
                )
                val impact = prepayImpact
                if (impact != null && !prepayOverLimit) {
                    val strong = SpanStyle(color = colors.positive, fontWeight = FontWeight.Medium)
                    Text(
                        buildAnnotatedString {
                            append("This would save ")
                            withStyle(strong) { append("${impact.monthsSaved} month${if (impact.monthsSaved == 1) "" else "s"}") }
                            append(" and ")
                            withStyle(strong) { append(money(impact.interestSaved, compact = true)) }
                            append(" in interest.")
                        },
                        style = FinioType.caption,
                        color = colors.mutedForeground,
                    )
                }
                FormActions(
                    "Record prepayment",
                    ::handlePrepaySubmit,
                    { prepayLoanId = null },
                    cancelVariant = ButtonVariant.Secondary,
                    saveEnabled = !prepayOverLimit && prepayLimit > 0,
                )
            }
        }
    }
}

@Composable
private fun LoanCard(
    loan: Loan,
    prepayments: List<LoanPrepayment>,
    money: MoneyFormatter,
    expanded: Boolean,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleClosed: () -> Unit,
    onAddPrepayment: () -> Unit,
    onSchedule: () -> Unit,
    onDeletePrepayment: (String) -> Unit,
) {
    val colors = FinioTheme.colors
    val status = remember(loan, prepayments) { loanStatus(loan.scheduleInput(prepayments)) }
    val isClosed = !loan.closedAt.isNullOrEmpty()
    val progress = if (status.totalMonths > 0) status.paidInstallments.toFloat() / status.totalMonths else 0f

    FinioCard(Modifier.fillMaxWidth().alpha(if (isClosed) 0.7f else 1f)) {
        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(loan.name, style = FinioType.bodyMedium, color = colors.foreground, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    when {
                        isClosed -> "Paid off"
                        status.isPaidOff -> "All installments due"
                        else -> "EMI ${money(status.emi, compact = true)}/mo · Next ${status.nextDueDate?.let { formatShortDate(it) } ?: "—"}"
                    },
                    style = Micro, color = colors.mutedForeground, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            CardIconAction(LucideIcons.Pencil, "Edit ${loan.name}", colors.mutedForeground, onEdit)
            CardIconAction(LucideIcons.Trash2, "Delete ${loan.name}", colors.destructive, onDelete)
        }

        Column(Modifier.padding(bottom = 8.dp)) {
            Box(Modifier.fillMaxWidth().height(6.dp).clip(FinioShapes.full).background(colors.muted)) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(min(progress, 1f))
                        .clip(FinioShapes.full)
                        .background(FinioTheme.brushes.gradPrimary),
                )
            }
            Text(
                "${status.paidInstallments} of ${status.totalMonths} installments · ${money(status.outstandingBalance, compact = true)} outstanding",
                Modifier.padding(top = 4.dp),
                style = Micro,
                color = colors.mutedForeground,
            )
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AccentTintButton("Add prepayment", onAddPrepayment, Modifier.weight(1f), enabled = !isClosed)
            TintButton(if (isClosed) "Reopen" else "Mark paid off", onToggleClosed, Modifier.weight(1f))
        }

        DisclosureToggle("Details", expanded, onToggle, leadingIcon = null)

        if (expanded) {
            DisclosurePanel {
                val pairs = listOf(
                    "Total interest (life of loan)" to money(status.totalInterest, compact = true),
                    "Interest paid so far" to money(status.totalInterestPaid, compact = true),
                    "Principal" to money(loan.principal, compact = true),
                    "Payoff date" to (status.payoffDate?.let { formatShortDate(it) } ?: "—"),
                )
                pairs.chunked(2).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { (label, value) ->
                            Column(Modifier.weight(1f)) {
                                Text(label, style = Micro, color = colors.mutedForeground)
                                Text(value, style = Micro.copy(fontWeight = FontWeight.Medium), color = colors.foreground)
                            }
                        }
                    }
                }
                FinioButton(
                    "Repayment schedule",
                    onSchedule,
                    variant = ButtonVariant.Outline,
                    size = ButtonSize.Sm,
                    leadingIcon = LucideIcons.CalendarRange,
                )
                Text(
                    "Prepayments",
                    Modifier.padding(top = 4.dp),
                    style = Micro.copy(fontWeight = FontWeight.Medium),
                    color = colors.mutedForeground,
                )
                if (prepayments.isEmpty()) {
                    Text("None logged yet.", style = Micro, color = colors.mutedForeground)
                } else {
                    prepayments.forEach { p ->
                        LedgerRow(
                            date = formatShortDate(p.date),
                            label = p.note,
                            amount = money(p.amount, compact = true),
                            negative = false,
                            dateWidth = 64.dp,
                            onDelete = { onDeletePrepayment(p.id) },
                            deleteLabel = "Delete prepayment",
                        )
                    }
                }
            }
        }
    }
}
