package com.slowatcoding.finio.ui.screens.loans

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slowatcoding.finio.core.format.formatShortDate
import com.slowatcoding.finio.core.format.todayKey
import com.slowatcoding.finio.core.js.fullYear
import com.slowatcoding.finio.core.js.iso
import com.slowatcoding.finio.core.loan.AmortizationRow
import com.slowatcoding.finio.core.loan.LoanPrepaymentInput
import com.slowatcoding.finio.core.loan.LoanScheduleInput
import com.slowatcoding.finio.core.loan.ScheduleYearGroup
import com.slowatcoding.finio.core.loan.buildAmortizationSchedule
import com.slowatcoding.finio.core.loan.groupScheduleByYear
import com.slowatcoding.finio.core.loan.loanStatus
import com.slowatcoding.finio.core.loan.maxPrepayment
import com.slowatcoding.finio.core.loan.scheduleInput
import com.slowatcoding.finio.core.loan.simulatePrepaymentImpact
import com.slowatcoding.finio.ui.common.BackButton
import com.slowatcoding.finio.ui.common.HideAmountsToggle
import com.slowatcoding.finio.ui.common.MoneyFormatter
import com.slowatcoding.finio.ui.common.collectFinanceState
import com.slowatcoding.finio.ui.common.rememberMoneyFormatter
import com.slowatcoding.finio.ui.components.FinioCard
import com.slowatcoding.finio.ui.components.FinioDatePicker
import com.slowatcoding.finio.ui.components.FinioDivider
import com.slowatcoding.finio.ui.components.FinioScreen
import com.slowatcoding.finio.ui.components.FinioTextField
import com.slowatcoding.finio.ui.components.ScreenTitle
import com.slowatcoding.finio.ui.icons.LucideIcons
import com.slowatcoding.finio.ui.mudra.NoteCard
import com.slowatcoding.finio.ui.mudra.noteFigureStyle
import com.slowatcoding.finio.ui.navigation.FinioNavigator
import com.slowatcoding.finio.ui.navigation.Routes
import com.slowatcoding.finio.ui.theme.FinioShapes
import com.slowatcoding.finio.ui.theme.FinioTheme
import com.slowatcoding.finio.ui.theme.FinioType
import com.slowatcoding.finio.ui.theme.mix
import java.time.LocalDate
import kotlin.math.max
import kotlin.math.min

/**
 * Port of web/src/pages/LoanSchedule.tsx — route `/loan-schedule/:id`: the outstanding figure on a
 * NoteCard with the principal-repaid register bar, a 2×2 stat grid, the amortization schedule
 * grouped by calendar year (the next-due year open by default; a collapsed year renders no rows)
 * and the read-only "What if I prepay?" calculator.
 *
 * @param loanId the loan (EditGuard-ed by the shell).
 */
@Composable
fun LoanScheduleScreen(nav: FinioNavigator, loanId: String) {
    val state by collectFinanceState()
    val money = rememberMoneyFormatter()
    val colors = FinioTheme.colors

    val loan = state.loans.find { it.id == loanId }
    // The exact shape LoansScreen builds, so this schedule always agrees with the loan card.
    val input = remember(loan, state.loanPrepayments) {
        loan?.scheduleInput(state.loanPrepayments.filter { it.loanId == loan.id })
    }
    val schedule = remember(input) { input?.let(::buildAmortizationSchedule).orEmpty() }
    val status = remember(input) { input?.let { loanStatus(it) } }
    val groups = remember(schedule) { groupScheduleByYear(schedule) }

    val nextDueRow = status?.let { schedule.getOrNull(it.paidInstallments) }
    val defaultYear = nextDueRow?.let { iso(it.date).fullYear } ?: groups.lastOrNull()?.year
    var openYears by remember { mutableStateOf<Set<Int>?>(null) }
    val expandedYears = openYears ?: setOfNotNull(defaultYear)
    fun toggleYear(year: Int) {
        val next = (openYears ?: setOfNotNull(defaultYear)).toMutableSet()
        if (!next.add(year)) next.remove(year)
        openYears = next
    }

    FinioScreen(header = {
        BackButton(nav)
        ScreenTitle("Repayment schedule")
        HideAmountsToggle()
    }) {
        if (loan == null || input == null || status == null) return@FinioScreen

        val isPaidOff = !loan.closedAt.isNullOrEmpty() || status.isPaidOff
        val repaid = max(0.0, loan.principal - status.outstandingBalance)
        val repaidPct = if (loan.principal > 0) min(100.0, repaid / loan.principal * 100) else 0.0
        val outstandingText = money(if (isPaidOff) 0.0 else status.outstandingBalance)

        NoteCard {
            Text(loan.name, style = FinioType.bodyMedium, color = colors.mutedForeground, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (isPaidOff) {
                Row(
                    Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(LucideIcons.CircleCheck, null, Modifier.size(32.dp), tint = colors.positive)
                    Text("Paid off", style = FinioType.displayMoney.copy(fontSize = 36.sp, lineHeight = 37.8.sp), color = colors.positive)
                }
            } else {
                Text(outstandingText, Modifier.padding(top = 4.dp), style = noteFigureStyle(outstandingText), color = colors.foreground, maxLines = 1)
                Text("Outstanding", style = FinioType.caption, color = colors.mutedForeground)
            }
            Text(
                "EMI ${money(status.emi)} · ${status.paidInstallments} of ${status.totalMonths} paid",
                Modifier.padding(top = 8.dp),
                style = FinioType.body,
                color = colors.foreground,
            )
            val density = LocalDensity.current.density
            val repaidText = "${money(repaid, compact = true)} of ${money(loan.principal, compact = true)} principal repaid"
            Box(
                Modifier
                    .padding(top = 12.dp)
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(FinioShapes.full)
                    .background(colors.muted)
                    .semantics { contentDescription = "Principal repaid: $repaidText" },
            ) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth((repaidPct / 100).toFloat())
                        .clip(FinioShapes.full)
                        .background(FinioTheme.brushes.register(density)),
                )
            }
            Text(repaidText, Modifier.padding(top = 6.dp), style = FinioType.caption, color = colors.mutedForeground)
        }

        val stats = listOf(
            "Total interest" to money(status.totalInterest),
            "Interest paid so far" to money(status.totalInterestPaid),
            "Next EMI" to when {
                isPaidOff -> "Paid off"
                status.nextDueDate != null -> formatShortDate(status.nextDueDate!!)
                else -> "—"
            },
            // A loan marked paid off early closed on that day, not on the schedule's last EMI.
            (if (!loan.closedAt.isNullOrEmpty()) "Closed on" else "Payoff date") to when {
                !loan.closedAt.isNullOrEmpty() -> formatShortDate(loan.closedAt!!)
                status.payoffDate != null -> formatShortDate(status.payoffDate!!)
                else -> "—"
            },
        )
        FinioCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                stats.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        row.forEach { (label, value) ->
                            Column(Modifier.weight(1f)) {
                                Text(label, style = FinioType.label, color = colors.mutedForeground, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    value,
                                    Modifier.padding(top = 2.dp),
                                    style = FinioType.rowValue,
                                    color = if (value == "Paid off") colors.positive else colors.foreground,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Schedule", Modifier.weight(1f), style = FinioType.title, color = colors.foreground)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    LegendKey(colors.primary, "Principal")
                    LegendKey(colors.warning.mix(0.6f), "Interest")
                }
            }
            if (groups.isEmpty()) {
                Text("No installments to show.", style = FinioType.body, color = colors.mutedForeground)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    groups.forEach { g ->
                        ScheduleYearSection(
                            group = g,
                            expanded = g.year in expandedYears,
                            onToggle = ::toggleYear,
                            paidInstallments = status.paidInstallments,
                            nextDueMonth = if (isPaidOff) null else nextDueRow?.month,
                            money = money,
                        )
                    }
                }
            }
        }

        if (!isPaidOff) {
            PrepayCalculator(input, money, onRecord = { nav.navigate(Routes.Loans) })
        }
    }
}

@Composable
private fun LegendKey(color: androidx.compose.ui.graphics.Color, label: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(12.dp).height(4.dp).clip(FinioShapes.full).background(color))
        Text(label, style = FinioType.caption, color = FinioTheme.colors.mutedForeground)
    }
}

/**
 * Port of components/loans/ScheduleYearSection.tsx: one calendar year — a disclosure header and,
 * only while open, a single hairline-divided list of its EMIs.
 */
@Composable
private fun ScheduleYearSection(
    group: ScheduleYearGroup,
    expanded: Boolean,
    onToggle: (Int) -> Unit,
    paidInstallments: Int,
    nextDueMonth: Int?,
    money: MoneyFormatter,
) {
    val colors = FinioTheme.colors
    val count = group.rows.size
    val turn by animateFloatAsState(if (expanded) 180f else 0f, tween(150), label = "year")
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(FinioShapes.full)
                .clickable(role = Role.Button) { onToggle(group.year) }
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = colors.foreground)) { append("${group.year}") }
                    withStyle(SpanStyle(color = colors.mutedForeground)) {
                        append(" · $count EMI${if (count == 1) "" else "s"} · ${money(group.totalPaid, precise = false)} paid")
                    }
                },
                Modifier.weight(1f),
                style = FinioType.body,
            )
            Icon(LucideIcons.ChevronDown, null, Modifier.size(16.dp).rotate(turn), tint = colors.mutedForeground)
        }
        if (expanded) {
            FinioCard(Modifier.padding(top = 4.dp), contentPadding = PaddingValues(0.dp)) {
                group.rows.forEachIndexed { index, row ->
                    if (index > 0) FinioDivider()
                    ScheduleRow(row, row.month <= paidInstallments, row.month == nextDueMonth, money)
                }
            }
        }
    }
}

@Composable
private fun ScheduleRow(row: AmortizationRow, isPaid: Boolean, isNext: Boolean, money: MoneyFormatter) {
    val colors = FinioTheme.colors
    val principalShare = if (row.emi > 0) (row.principal / row.emi).toFloat().coerceIn(0f, 1f) else 0f
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (isNext) colors.accent.mix(0.4f) else androidx.compose.ui.graphics.Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "EMI ${row.month}, ${formatShortDate(row.date)}. Principal ${money(row.principal)}, interest ${money(row.interest)}."
            },
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Row(Modifier.alpha(if (isPaid) 0.7f else 1f), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                if (isNext) Box(Modifier.size(6.dp).clip(FinioShapes.full).background(colors.primary))
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.Medium, color = colors.foreground)) { append("EMI ${row.month}") }
                        withStyle(SpanStyle(color = colors.mutedForeground)) { append(" · ${formatShortDate(row.date)}") }
                    },
                    style = FinioType.body,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(
                Modifier
                    .padding(top = 6.dp)
                    .widthIn(max = 160.dp)
                    .fillMaxWidth()
                    .height(4.dp)
                    .alpha(if (isPaid) 0.6f else 1f)
                    .clip(FinioShapes.full)
                    .background(colors.muted),
            ) {
                if (principalShare > 0f) Box(Modifier.weight(principalShare).fillMaxHeight().background(colors.primary))
                if (principalShare < 1f) Box(Modifier.weight(1f - principalShare).fillMaxHeight().background(colors.warning.mix(0.6f)))
            }
            if (isPaid || isNext) {
                Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (isPaid) {
                        Icon(LucideIcons.Check, null, Modifier.size(12.dp), tint = colors.positive)
                        Text("Paid", style = FinioType.caption, color = colors.mutedForeground)
                    } else {
                        Text("Next due", style = FinioType.label, color = colors.primary)
                    }
                }
            }
            if (row.prepayment > 0) {
                Text("+${money(row.prepayment)} prepaid", Modifier.padding(top = 4.dp), style = FinioType.label, color = colors.positive)
            }
        }
        Column(Modifier.alpha(if (isPaid) 0.7f else 1f), horizontalAlignment = Alignment.End) {
            Text(money(row.emi), style = FinioType.rowValue, color = colors.foreground)
            Text("Balance ${money(row.closingBalance, precise = false)}", style = FinioType.caption, color = colors.mutedForeground)
        }
    }
}

private val AMOUNT_INPUT = Regex("""^\d*(\.\d{0,2})?$""")

/**
 * Port of components/loans/PrepayCalculator.tsx — "What if I prepay?": a read-only simulation
 * against the loan's real schedule. It records nothing; the real flow lives on the Loans page.
 */
@Composable
private fun PrepayCalculator(loan: LoanScheduleInput, money: MoneyFormatter, onRecord: () -> Unit) {
    val colors = FinioTheme.colors
    var amount by remember { mutableStateOf("") }
    var date by remember { mutableStateOf<LocalDate?>(LocalDate.parse(todayKey())) }
    val limit = remember(loan) { maxPrepayment(loan) }
    val parsed = parseFloatOrZero(amount)
    val overLimit = parsed > limit + 0.005
    val impact = remember(loan, parsed, overLimit, date) {
        val day = date
        if (parsed <= 0 || overLimit || day == null) null
        else simulatePrepaymentImpact(loan, LoanPrepaymentInput(parsed, day.isoAtLocalMidnight()))
    }

    FinioCard {
        Text("What if I prepay?", style = FinioType.title, color = colors.foreground)
        Text(
            "See how an extra payment shortens this loan.",
            Modifier.padding(top = 2.dp),
            style = FinioType.caption,
            color = colors.mutedForeground,
        )
        Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Extra amount", style = FinioType.caption, color = colors.mutedForeground)
                FinioTextField(
                    amount,
                    { raw ->
                        val v = raw.filter { it.isDigit() || it == '.' }
                        // one decimal point, at most two paise digits
                        if (AMOUNT_INPUT.matches(v)) amount = v
                    },
                    Modifier.fillMaxWidth(),
                    placeholder = "e.g. 50000",
                    isError = overLimit,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Paid on", style = FinioType.label, color = colors.mutedForeground)
                FinioDatePicker(date, { date = it }, Modifier.fillMaxWidth())
            }
        }
        Box(Modifier.padding(top = 12.dp)) {
            when {
                overLimit -> Text(
                    "Only ${money(limit)} is still owed — enter that or less.",
                    style = FinioType.caption,
                    color = colors.destructive,
                )
                impact != null -> if (impact.monthsSaved > 0 || impact.interestSaved > 0) {
                    Text(
                        buildAnnotatedString {
                            append("You'd finish ")
                            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                                append("${impact.monthsSaved} month${if (impact.monthsSaved == 1) "" else "s"} earlier")
                            }
                            append(" and save ")
                            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = colors.positive)) {
                                append(money(impact.interestSaved, precise = false))
                            }
                            append(" in interest.")
                        },
                        style = FinioType.body,
                        color = colors.foreground,
                    )
                } else {
                    Text(
                        "That date falls after the loan ends, so it wouldn't change anything.",
                        style = FinioType.body,
                        color = colors.mutedForeground,
                    )
                }
                else -> Text(
                    "Up to ${money(limit)} — what's still owed today.",
                    style = FinioType.caption,
                    color = colors.mutedForeground,
                )
            }
        }
        Column(Modifier.padding(top = 12.dp)) {
            FinioDivider()
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("This is a preview — nothing is recorded.", Modifier.weight(1f), style = FinioType.caption, color = colors.mutedForeground)
                Text(
                    "Record a prepayment",
                    Modifier
                        .clip(FinioShapes.sm)
                        .clickable(role = Role.Button, onClick = onRecord)
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    style = FinioType.label,
                    color = colors.primary,
                )
            }
        }
    }
}

