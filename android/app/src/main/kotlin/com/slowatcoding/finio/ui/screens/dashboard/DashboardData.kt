package com.slowatcoding.finio.ui.screens.dashboard

import com.slowatcoding.finio.core.calc.BUDGET_NEAR_LIMIT_PERCENT
import com.slowatcoding.finio.core.calc.BudgetPeriodOptions
import com.slowatcoding.finio.core.calc.BudgetStatus
import com.slowatcoding.finio.core.calc.CreditCardDueInfo
import com.slowatcoding.finio.core.calc.DashboardQuickStats
import com.slowatcoding.finio.core.calc.GoalStatus
import com.slowatcoding.finio.core.calc.PersonBalance
import com.slowatcoding.finio.core.calc.activeAccounts
import com.slowatcoding.finio.core.calc.computeBudgetStatuses
import com.slowatcoding.finio.core.calc.computeGoalStatus
import com.slowatcoding.finio.core.calc.computePersonBalance
import com.slowatcoding.finio.core.calc.getCreditCardDueInfo
import com.slowatcoding.finio.core.calc.getCurrentMonthTransactions
import com.slowatcoding.finio.core.calc.getDashboardStats
import com.slowatcoding.finio.core.calc.getPreviousMonthTransactions
import com.slowatcoding.finio.core.calc.getTotalAccountBalance
import com.slowatcoding.finio.core.calc.getTotalCreditOutstanding
import com.slowatcoding.finio.core.calc.getTotalDepositValue
import com.slowatcoding.finio.core.calc.getTotalExpenses
import com.slowatcoding.finio.core.calc.getTotalIncome
import com.slowatcoding.finio.core.calc.sortTransactionsDateDesc
import com.slowatcoding.finio.core.deposit.accountDisplayValue
import com.slowatcoding.finio.core.format.shouldCompactGroup
import com.slowatcoding.finio.core.js.differenceInCalendarDays
import com.slowatcoding.finio.core.js.format
import com.slowatcoding.finio.core.js.nowInstant
import com.slowatcoding.finio.core.model.Account
import com.slowatcoding.finio.core.model.AccountType
import com.slowatcoding.finio.core.model.BudgetPeriod
import com.slowatcoding.finio.core.model.FinanceState
import com.slowatcoding.finio.core.model.RecurringTransaction
import com.slowatcoding.finio.core.model.Transaction
import com.slowatcoding.finio.core.model.TransactionType
import com.slowatcoding.finio.core.period.PeriodType
import com.slowatcoding.finio.core.period.normalizeMonthStartDay
import com.slowatcoding.finio.core.period.periodRange
import com.slowatcoding.finio.core.period.toPeriodType
import com.slowatcoding.finio.core.store.isRulePaused
import com.slowatcoding.finio.core.store.nextDueDate
import kotlin.math.floor
import kotlin.math.max

/** Dashboard.tsx `AlertItem`. */
sealed interface AlertItem {
    data class Budget(val status: BudgetStatus, val label: String) : AlertItem
    data class Credit(val account: Account, val dueInfo: CreditCardDueInfo) : AlertItem
    data class Recurring(val rule: RecurringTransaction, val label: String, val daysUntil: Int) : AlertItem
}

/** Dashboard.tsx `BUDGET_PERIOD_NOUN`. */
fun BudgetPeriod.noun(): String = when (this) {
    BudgetPeriod.Weekly -> "week"
    BudgetPeriod.Monthly -> "month"
    BudgetPeriod.Yearly -> "year"
}

/** Every derived value the Dashboard renders — the page's `useMemo`s, computed off the main thread. */
data class DashboardData(
    val openAccounts: List<Account>,
    val accountValues: Map<String, Double>,
    val accountsCompact: Boolean,
    val totalBalance: Double,
    val creditOutstanding: Double,
    val depositValue: Double,
    val afterDues: Double,
    val monthTxnCount: Int,
    val prevMonthTxnCount: Int,
    val monthIncome: Double,
    val monthExpenses: Double,
    val prevMonthIncome: Double,
    val prevMonthExpenses: Double,
    val recentTxns: List<Transaction>,
    val stats: DashboardQuickStats,
    val overallBudget: BudgetStatus?,
    val daysLeftInPeriod: Int,
    val periodNoun: String,
    val upcomingBillsTotal: Double,
    val safePerDay: Double,
    val periodLabel: String,
    val topGoals: List<GoalStatus>,
    val topDebts: List<PersonBalance>,
    val attentionItems: List<AlertItem>,
)

fun computeDashboardData(s: FinanceState): DashboardData {
    val now = nowInstant()
    val monthStartDay = normalizeMonthStartDay(s.settings.monthStartDay)
    val transactions = s.transactions
    val accounts = s.accounts

    val monthTxns = getCurrentMonthTransactions(transactions, monthStartDay)
    val prevMonthTxns = getPreviousMonthTransactions(transactions, monthStartDay)
    val openAccounts = activeAccounts(accounts)
    val totalBalance = getTotalAccountBalance(accounts)
    val creditOutstanding = getTotalCreditOutstanding(accounts)
    val depositValue = getTotalDepositValue(accounts)
    val accountValues = openAccounts.associate { it.id to accountDisplayValue(it) }
    val accountsCompact = shouldCompactGroup(openAccounts.map { accountValues.getValue(it.id) })
    val recentTxns = sortTransactionsDateDesc(transactions).take(5)
    val stats = getDashboardStats(monthTxns, prevMonthTxns, s.categories, BudgetPeriodOptions(monthStartDay = monthStartDay))
    val allBudgetStatuses = computeBudgetStatuses(s.budgets, transactions, BudgetPeriodOptions(monthStartDay = monthStartDay))
    val overallBudget = allBudgetStatuses.find { it.budget.labelId.isNullOrEmpty() && it.budget.categoryId == "" }

    // Days left in the overall budget's *own* period.
    val budgetPeriod = overallBudget?.budget?.period ?: BudgetPeriod.Monthly
    val range = periodRange(budgetPeriod.toPeriodType(), now, monthStartDay)
    val daysLeftInPeriod = max(1, differenceInCalendarDays(range.end, now) + 1)

    // The overall budget has its own hero card, so it's excluded from the alert list.
    val nearLimitBudgets = allBudgetStatuses
        .filter { it.percent >= BUDGET_NEAR_LIMIT_PERCENT && it.budget.id != overallBudget?.budget?.id }
        .sortedWith(compareByDescending<BudgetStatus> { if (it.isOver) 1 else 0 }.thenByDescending { it.percent })

    // Paused and finished rules have no next bill to warn about.
    val upcomingRecurring = s.recurring
        .filter { !isRulePaused(it) }
        .mapNotNull { rule ->
            val nextDue = nextDueDate(rule) ?: return@mapNotNull null
            Triple(rule, nextDue, differenceInCalendarDays(nextDue, now))
        }
        .filter { (_, _, d) -> d in 0..7 }
        .sortedBy { (_, due, _) -> due.toEpochMilli() }

    // Only expense rules that land before the period ends draw down the budget.
    val upcomingBillsTotal = upcomingRecurring
        .filter { (rule, _, d) -> rule.type == TransactionType.Expense && d < daysLeftInPeriod }
        .fold(0.0) { sum, (rule, _, _) -> sum + rule.amount }

    // Floored, never rounded: a "safe" figure that rounds up could overspend by a rupee.
    val safePerDay = if (overallBudget != null) {
        floor(max(overallBudget.remaining - upcomingBillsTotal, 0.0) / daysLeftInPeriod)
    } else 0.0

    val monthRange = periodRange(PeriodType.Monthly, now, monthStartDay)
    val periodLabel = "${format(monthRange.start, "d MMM")} – ${format(monthRange.end, "d MMM")}"

    val topGoals = s.goals
        .map { computeGoalStatus(it, s.goalContributions) }
        .filter { !it.isComplete }
        .sortedByDescending { it.percent }
        .take(2)
    val topDebts = s.people
        .map { computePersonBalance(it, s.debtEntries) }
        .filter { it.balance != 0.0 }
        .sortedByDescending { kotlin.math.abs(it.balance) }
        .take(3)
    val creditDues = openAccounts
        .filter { it.type == AccountType.Credit }
        .mapNotNull { account -> getCreditCardDueInfo(account)?.takeIf { it.daysUntilDue <= 7 }?.let { account to it } }
        .sortedBy { it.second.daysUntilDue }

    fun budgetLabel(st: BudgetStatus): String {
        val labelId = st.budget.labelId
        return when {
            !labelId.isNullOrEmpty() -> s.labels.find { it.id == labelId }?.name ?: "Unknown label"
            st.budget.categoryId == "" -> "Overall expenses"
            else -> s.categories.find { it.id == st.budget.categoryId }?.name ?: "Unknown"
        }
    }

    val attention = buildList<AlertItem> {
        nearLimitBudgets.forEach { add(AlertItem.Budget(it, budgetLabel(it))) }
        creditDues.forEach { (account, info) -> add(AlertItem.Credit(account, info)) }
        upcomingRecurring.forEach { (rule, _, d) ->
            val cat = s.categories.find { it.id == rule.categoryId }
            add(AlertItem.Recurring(rule, rule.note.ifEmpty { cat?.name ?: "Recurring" }, d))
        }
    }

    return DashboardData(
        openAccounts = openAccounts,
        accountValues = accountValues,
        accountsCompact = accountsCompact,
        totalBalance = totalBalance,
        creditOutstanding = creditOutstanding,
        depositValue = depositValue,
        afterDues = totalBalance - creditOutstanding,
        monthTxnCount = monthTxns.size,
        prevMonthTxnCount = prevMonthTxns.size,
        monthIncome = getTotalIncome(monthTxns),
        monthExpenses = getTotalExpenses(monthTxns),
        prevMonthIncome = getTotalIncome(prevMonthTxns),
        prevMonthExpenses = getTotalExpenses(prevMonthTxns),
        recentTxns = recentTxns,
        stats = stats,
        overallBudget = overallBudget,
        daysLeftInPeriod = daysLeftInPeriod,
        periodNoun = budgetPeriod.noun(),
        upcomingBillsTotal = upcomingBillsTotal,
        safePerDay = safePerDay,
        periodLabel = periodLabel,
        topGoals = topGoals,
        topDebts = topDebts,
        attentionItems = attention,
    )
}
