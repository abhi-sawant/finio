package com.slowatcoding.finio.core.calc

import com.slowatcoding.finio.core.data.MISC_CATEGORY_ID
import com.slowatcoding.finio.core.deposit.depositCurrentValue
import com.slowatcoding.finio.core.deposit.isDepositAccount
import com.slowatcoding.finio.core.format.localDayKey
import com.slowatcoding.finio.core.format.shortestDecimal
import com.slowatcoding.finio.core.js.addDays
import com.slowatcoding.finio.core.js.addMonths
import com.slowatcoding.finio.core.js.dayOfMonth
import com.slowatcoding.finio.core.js.differenceInCalendarDays
import com.slowatcoding.finio.core.js.fullYear
import com.slowatcoding.finio.core.js.jsToFixed
import com.slowatcoding.finio.core.js.localDate
import com.slowatcoding.finio.core.js.month0
import com.slowatcoding.finio.core.js.nowInstant
import com.slowatcoding.finio.core.js.parseIso
import com.slowatcoding.finio.core.js.parseJsDate
import com.slowatcoding.finio.core.model.Account
import com.slowatcoding.finio.core.model.AccountType
import com.slowatcoding.finio.core.model.Budget
import com.slowatcoding.finio.core.model.Category
import com.slowatcoding.finio.core.model.CategoryType
import com.slowatcoding.finio.core.model.DebtEntry
import com.slowatcoding.finio.core.model.Goal
import com.slowatcoding.finio.core.model.GoalContribution
import com.slowatcoding.finio.core.model.Label
import com.slowatcoding.finio.core.model.Person
import com.slowatcoding.finio.core.model.Transaction
import com.slowatcoding.finio.core.model.TransactionSplit
import com.slowatcoding.finio.core.model.TransactionType
import com.slowatcoding.finio.core.period.DEFAULT_MONTH_START_DAY
import com.slowatcoding.finio.core.period.PeriodRange
import com.slowatcoding.finio.core.period.PeriodType
import com.slowatcoding.finio.core.period.daysElapsedInPeriod
import com.slowatcoding.finio.core.period.daysInPeriod
import com.slowatcoding.finio.core.period.isWithinPeriod
import com.slowatcoding.finio.core.period.normalizeMonthStartDay
import com.slowatcoding.finio.core.period.periodRange
import com.slowatcoding.finio.core.period.periodStart
import com.slowatcoding.finio.core.period.shiftPeriod
import com.slowatcoding.finio.core.period.toPeriodType
import com.slowatcoding.finio.core.util.jsTrim
import java.time.Instant
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

// Port of web/src/utils/calculations.ts — financial aggregations, budget status/history and CSV
// export. JS truthiness is mirrored where it matters: an empty-string `archivedAt`, `labelId`,
// `toAccountId` or `recurringId` counts as absent, exactly like the TS `!x` / `x ?` checks.

/** Stable reorder that keeps "Miscellaneous" at the bottom of any category listing. */
fun <T> miscLast(categories: List<T>, id: (T) -> String): List<T> =
    categories.filter { id(it) != MISC_CATEGORY_ID } + categories.filter { id(it) == MISC_CATEGORY_ID }

@JvmName("miscLastCategories")
fun miscLast(categories: List<Category>): List<Category> = miscLast(categories) { it.id }

/** The default "Transfer" category (`defaultData.ts`). Transfers file under it. */
const val TRANSFER_CATEGORY_ID = "cat-13"

/**
 * Whether a category may be filed under a transaction of [type]. Transfers only take the
 * neutral `both` categories; "Transfer" itself is never offered for an expense or income.
 */
fun isCategoryValidForType(category: Category, type: TransactionType): Boolean {
    if (type == TransactionType.Transfer) return category.type == CategoryType.Both
    if (category.id == TRANSFER_CATEGORY_ID) return false
    return category.type.wire == type.wire || category.type == CategoryType.Both
}

/** The category a transfer is filed under: the Transfer category, else the first neutral one. */
fun findTransferCategory(categories: List<Category>): Category? =
    categories.find { it.id == TRANSFER_CATEGORY_ID } ?: categories.find { it.type == CategoryType.Both }

/**
 * The category/amount pairs a transaction counts against — its `splits` if it has any,
 * otherwise its own `categoryId`/`amount` as a single entry.
 */
fun transactionCategoryAmounts(categoryId: String, amount: Double, splits: List<TransactionSplit>?): List<TransactionSplit> =
    if (!splits.isNullOrEmpty()) splits else listOf(TransactionSplit(categoryId, amount))

fun transactionCategoryAmounts(t: Transaction): List<TransactionSplit> =
    transactionCategoryAmounts(t.categoryId, t.amount, t.splits)

fun getTotalIncome(transactions: List<Transaction>): Double =
    transactions.filter { it.type == TransactionType.Income }.fold(0.0) { sum, t -> sum + t.amount }

fun getTotalExpenses(transactions: List<Transaction>): Double =
    transactions.filter { it.type == TransactionType.Expense }.fold(0.0) { sum, t -> sum + t.amount }

/** Accounts still in use. Archived (closed) accounts keep their history but leave every total. */
fun activeAccounts(accounts: List<Account>): List<Account> = accounts.filter { it.archivedAt.isNullOrEmpty() }

/** Money that can actually be spent: not a credit line and not a fixed or recurring deposit. */
fun isLiquidAccount(type: AccountType): Boolean =
    type != AccountType.Credit && type != AccountType.Fd && type != AccountType.Rd

fun isLiquidAccount(account: Account): Boolean = isLiquidAccount(account.type)

fun getTotalAccountBalance(accounts: List<Account>): Double =
    activeAccounts(accounts).filter(::isLiquidAccount).fold(0.0) { sum, a -> sum + a.balance }

/** Today's accrued value across every open deposit — not their book balance. */
fun getTotalDepositValue(accounts: List<Account>, now: Instant = nowInstant()): Double =
    activeAccounts(accounts).filter(::isDepositAccount).fold(0.0) { sum, a -> sum + depositCurrentValue(a, now) }

/** Includes credit (negative balances reduce net worth). */
fun getNetWorth(accounts: List<Account>): Double = activeAccounts(accounts).fold(0.0) { sum, a -> sum + a.balance }

fun getTotalCreditOutstanding(accounts: List<Account>): Double =
    activeAccounts(accounts).filter { it.type == AccountType.Credit }
        .fold(0.0) { sum, a -> sum + abs(min(a.balance, 0.0)) }

/** Fraction of `creditLimit` currently drawn, 0 for non-credit or limit-less accounts. */
fun getCreditUtilization(account: Account): Double {
    val limit = account.creditLimit
    if (account.type != AccountType.Credit || limit == null || limit == 0.0 || limit.isNaN()) return 0.0
    return abs(min(account.balance, 0.0)) / limit
}

private const val DEFAULT_MINIMUM_DUE_PERCENT = 5.0

data class CreditCardDueInfo(
    val outstanding: Double,
    val minimumDue: Double,
    val dueDate: Instant,
    /** Negative once the due date has passed. */
    val daysUntilDue: Int,
    val isOverdue: Boolean,
)

/**
 * The bill from a credit account's most recently closed statement. "Outstanding" is today's
 * balance (there is no per-statement snapshot). Null when the account isn't an open credit
 * account, has no statement cycle configured, or has nothing outstanding.
 */
fun getCreditCardDueInfo(account: Account, now: Instant = nowInstant()): CreditCardDueInfo? {
    if (account.type != AccountType.Credit || !account.archivedAt.isNullOrEmpty()) return null
    val statementCloseDay = account.statementCloseDay
    val paymentDueDaysRaw = account.paymentDueDays
    if (statementCloseDay == null || statementCloseDay == 0 || paymentDueDaysRaw == null) return null

    val outstanding = abs(min(account.balance, 0.0))
    if (outstanding <= 0) return null

    val closeDay = normalizeMonthStartDay(statementCloseDay)
    val paymentDueDays = max(0, paymentDueDaysRaw)
    val closeThisMonth = localDate(now.fullYear, now.month0, closeDay)
    val recentClose = if (now.dayOfMonth >= closeDay) closeThisMonth else addMonths(closeThisMonth, -1)
    val dueDate = addDays(recentClose, paymentDueDays)
    val daysUntilDue = differenceInCalendarDays(dueDate, now)
    val minimumDuePercent = account.minimumDuePercent ?: DEFAULT_MINIMUM_DUE_PERCENT

    return CreditCardDueInfo(
        outstanding = outstanding,
        minimumDue = (outstanding * minimumDuePercent) / 100,
        dueDate = dueDate,
        daysUntilDue = daysUntilDue,
        isOverdue = daysUntilDue < 0,
    )
}

fun transactionsInPeriod(transactions: List<Transaction>, range: PeriodRange): List<Transaction> =
    transactions.filter { t -> parseIso(t.date)?.let { isWithinPeriod(it, range) } ?: false }

/** "This month" everywhere in the app — the salary cycle when `monthStartDay` isn't 1. */
fun getCurrentMonthTransactions(
    transactions: List<Transaction>,
    monthStartDay: Int = DEFAULT_MONTH_START_DAY,
    now: Instant = nowInstant(),
): List<Transaction> = transactionsInPeriod(transactions, periodRange(PeriodType.Monthly, now, monthStartDay))

fun getMonthTransactions(
    transactions: List<Transaction>,
    monthDate: Instant,
    monthStartDay: Int = DEFAULT_MONTH_START_DAY,
): List<Transaction> = transactionsInPeriod(transactions, periodRange(PeriodType.Monthly, monthDate, monthStartDay))

/** `new Date(t.date).getTime()`; NaN for an unparseable date. */
private fun jsTime(date: String): Double = parseJsDate(date)?.toEpochMilli()?.toDouble() ?: Double.NaN

/** JS `sort((a, b) => time(b) - time(a))` — a NaN comparison counts as equal. */
private val dateDesc = Comparator<Transaction> { a, b ->
    val diff = jsTime(b.date) - jsTime(a.date)
    if (diff.isNaN() || diff == 0.0) 0 else if (diff > 0) 1 else -1
}

data class DateGroup(val date: String, val transactions: List<Transaction>)

fun groupTransactionsByDate(transactions: List<Transaction>): List<DateGroup> {
    val sorted = transactions.sortedWith(dateDesc)
    val map = LinkedHashMap<String, MutableList<Transaction>>()
    for (t in sorted) map.getOrPut(localDayKey(t.date)) { mutableListOf() }.add(t)
    return map.map { (date, txs) -> DateGroup(date, txs) }
}

/** Lookup tables for [transactionMatchesQuery], pre-lowercased, built once per search. */
data class SearchIndex(
    val categoryNames: Map<String, String>,
    val accountNames: Map<String, String>,
    val labelNames: Map<String, String>,
)

fun buildSearchIndex(categories: List<Category>, accounts: List<Account>, labels: List<Label>): SearchIndex =
    SearchIndex(
        categoryNames = categories.associate { it.id to it.name.lowercase() },
        accountNames = accounts.associate { it.id to it.name.lowercase() },
        labelNames = labels.associate { it.id to it.name.lowercase() },
    )

private val NON_NUMERIC = Regex("[^0-9.]")

/**
 * Match a transaction against a free-text query across note, category, account (both sides of a
 * transfer), labels, and amount. Amounts are compared digit-wise against the raw number, so
 * "₹1,200" still finds `1200`.
 */
fun transactionMatchesQuery(transaction: Transaction, rawQuery: String, index: SearchIndex): Boolean {
    val q = jsTrim(rawQuery).lowercase()
    if (q.isEmpty()) return true

    if (transaction.note.lowercase().contains(q)) return true
    if (index.categoryNames[transaction.categoryId]?.contains(q) == true) return true
    for (split in transaction.splits ?: emptyList()) {
        if (index.categoryNames[split.categoryId]?.contains(q) == true) return true
    }
    if (index.accountNames[transaction.accountId]?.contains(q) == true) return true
    val to = transaction.toAccountId
    if (!to.isNullOrEmpty() && index.accountNames[to]?.contains(q) == true) return true
    for (labelId in transaction.labels) {
        if (index.labelNames[labelId]?.contains(q) == true) return true
    }

    val numeric = q.replace(NON_NUMERIC, "")
    if (numeric.isNotEmpty() && numeric != "." && jsNumberString(transaction.amount).contains(numeric)) return true

    return false
}

/** Sorted copy of transactions, date descending. */
fun sortTransactionsDateDesc(transactions: List<Transaction>): List<Transaction> = transactions.sortedWith(dateDesc)

/** How many past periods a rollover chain is allowed to accumulate over. */
const val MAX_ROLLOVER_LOOKBACK = 12

data class BudgetPeriodOptions(
    val monthStartDay: Int? = null,
    /** Overridable for tests and for previewing a period other than the live one. */
    val now: Instant? = null,
)

/** Identifies what a budget is a limit *for*. Two budgets may not share one scope. */
fun budgetScopeKey(categoryId: String, labelId: String?): String =
    if (!labelId.isNullOrEmpty()) "label:$labelId" else "category:$categoryId"

fun budgetScopeKey(budget: Budget): String = budgetScopeKey(budget.categoryId, budget.labelId)

/**
 * How much of this transaction counts against the budget. Income and transfers never count. A
 * label or overall budget counts the full amount; a category budget only the matching split portion.
 */
fun budgetMatchedAmount(budget: Budget, transaction: Transaction): Double {
    if (transaction.type != TransactionType.Expense) return 0.0
    val labelId = budget.labelId
    if (!labelId.isNullOrEmpty()) return if (labelId in transaction.labels) transaction.amount else 0.0
    if (budget.categoryId == "") return transaction.amount
    return transactionCategoryAmounts(transaction)
        .filter { it.categoryId == budget.categoryId }
        .fold(0.0) { sum, s -> sum + s.amount }
}

data class BudgetPeriodResult(
    val range: PeriodRange,
    val spent: Double,
    /** `budget.amount`, plus any rollover carried into this period. */
    val limit: Double,
    val isOver: Boolean,
)

data class BudgetStatus(
    val budget: Budget,
    val range: PeriodRange,
    val spent: Double,
    /** Unspent (or, when negative, overspent) amount carried in. Always 0 without rollover. */
    val carryover: Double,
    val limit: Double,
    val remaining: Double,
    val percent: Double,
    val isOver: Boolean,
)

/** The periods before [currentRange] a rollover chain runs through, oldest first. */
private fun priorPeriods(
    budget: Budget,
    transactions: List<Transaction>,
    currentRange: PeriodRange,
    monthStartDay: Int,
    lookback: Int,
): List<BudgetPeriodResult> {
    val created = parseIso(budget.createdAt)
    val createdStart = if (created == null) currentRange.start
    else periodStart(budget.period.toPeriodType(), created, monthStartDay)

    val ranges = mutableListOf<PeriodRange>()
    var cursor = shiftPeriod(currentRange, -1)
    while (ranges.size < lookback && cursor.start.toEpochMilli() >= createdStart.toEpochMilli()) {
        ranges.add(cursor)
        cursor = shiftPeriod(cursor, -1)
    }
    ranges.reverse()

    var carry = 0.0
    return ranges.map { range ->
        val limit = budget.amount + if (budget.rollover) carry else 0.0
        val spent = sumBudgetMatched(budget, transactionsInPeriod(transactions, range))
        if (budget.rollover) carry = limit - spent
        BudgetPeriodResult(range, spent, limit, spent > limit)
    }
}

private fun sumBudgetMatched(budget: Budget, transactions: List<Transaction>): Double =
    transactions.fold(0.0) { sum, t -> sum + budgetMatchedAmount(budget, t) }

/**
 * Status of every budget in its *own* current period — weekly, monthly (aligned to
 * `monthStartDay`) or yearly. Takes the full transaction list.
 */
fun computeBudgetStatuses(
    budgets: List<Budget>,
    transactions: List<Transaction>,
    options: BudgetPeriodOptions = BudgetPeriodOptions(),
): List<BudgetStatus> {
    val now = options.now ?: nowInstant()
    val monthStartDay = options.monthStartDay ?: DEFAULT_MONTH_START_DAY

    return budgets.map { budget ->
        val range = periodRange(budget.period.toPeriodType(), now, monthStartDay)
        val spent = sumBudgetMatched(budget, transactionsInPeriod(transactions, range))

        var carryover = 0.0
        if (budget.rollover) {
            val priors = priorPeriods(budget, transactions, range, monthStartDay, MAX_ROLLOVER_LOOKBACK)
            priors.lastOrNull()?.let { carryover = it.limit - it.spent }
        }

        val limit = budget.amount + carryover
        val remaining = limit - spent
        BudgetStatus(
            budget = budget,
            range = range,
            spent = spent,
            carryover = carryover,
            limit = limit,
            remaining = remaining,
            percent = if (limit > 0) (spent / limit) * 100 else 0.0,
            isOver = spent > limit,
        )
    }
}

/** A budget this close to its limit is worth warning about (Dashboard + Budgets page). */
const val BUDGET_NEAR_LIMIT_PERCENT = 85

enum class BudgetHealth(val wire: String) { Over("over"), Near("near"), Ok("ok") }

/** Where a budget sits against its limit, as a value the UI can *name*. */
fun budgetHealth(isOver: Boolean, percent: Double): BudgetHealth {
    if (isOver) return BudgetHealth.Over
    return if (percent >= BUDGET_NEAR_LIMIT_PERCENT) BudgetHealth.Near else BudgetHealth.Ok
}

fun budgetHealth(status: BudgetStatus): BudgetHealth = budgetHealth(status.isOver, status.percent)

/** How this budget did over its recent completed periods, most recent first. */
fun computeBudgetHistory(
    budget: Budget,
    transactions: List<Transaction>,
    options: BudgetPeriodOptions = BudgetPeriodOptions(),
    count: Int = 6,
): List<BudgetPeriodResult> {
    val now = options.now ?: nowInstant()
    val monthStartDay = options.monthStartDay ?: DEFAULT_MONTH_START_DAY
    val range = periodRange(budget.period.toPeriodType(), now, monthStartDay)
    // Walk the full rollover window so carried-over limits are right, then show the tail.
    val lookback = max(count, if (budget.rollover) MAX_ROLLOVER_LOOKBACK else count)
    val priors = priorPeriods(budget, transactions, range, monthStartDay, lookback)
    return jsSliceTail(priors, count).reversed()
}

/** JS `array.slice(-count)`: `slice(-0)` is the whole array, a negative count drops from the front. */
private fun <T> jsSliceTail(list: List<T>, count: Int): List<T> = when {
    count == 0 -> list
    count > 0 -> list.takeLast(min(count, list.size))
    else -> list.drop(min(-count, list.size))
}

data class CategoryAmount(val category: Category, val amount: Double)

data class DashboardQuickStats(
    val dailyAverage: Double,
    val projectedMonth: Double,
    val biggestExpense: Transaction?,
    val topCategory: CategoryAmount?,
    /** -1..+inf, e.g. 0.12 = +12%. */
    val monthOverMonthChange: Double,
    /** (income - expense) / income. Negative when overspending; 0 when income == 0. */
    val savingsRate: Double,
    /** Percentage-point change vs the previous month; null when it had no income. */
    val savingsRateChange: Double?,
)

fun getDashboardStats(
    monthTxns: List<Transaction>,
    previousMonthTxns: List<Transaction>,
    categories: List<Category>,
    options: BudgetPeriodOptions = BudgetPeriodOptions(),
): DashboardQuickStats {
    val expenses = monthTxns.filter { it.type == TransactionType.Expense }
    val income = monthTxns.filter { it.type == TransactionType.Income }.fold(0.0) { s, t -> s + t.amount }
    val expensesTotal = expenses.fold(0.0) { s, t -> s + t.amount }

    val now = options.now ?: nowInstant()
    // Pace against the *cycle* the totals cover, not the calendar month.
    val range = periodRange(PeriodType.Monthly, now, options.monthStartDay ?: DEFAULT_MONTH_START_DAY)
    val elapsed = daysElapsedInPeriod(range, now)
    val dailyAverage = if (elapsed > 0) expensesTotal / elapsed else 0.0
    val projectedMonth = dailyAverage * daysInPeriod(range)

    var biggestExpense: Transaction? = null
    for (t in expenses) {
        if (biggestExpense == null || t.amount > biggestExpense.amount) biggestExpense = t
    }

    val byCat = LinkedHashMap<String, Double>()
    for (t in expenses) {
        for ((categoryId, splitAmount) in transactionCategoryAmounts(t)) {
            byCat[categoryId] = (byCat[categoryId] ?: 0.0) + splitAmount
        }
    }
    var topCategory: CategoryAmount? = null
    for ((catId, amount) in byCat) {
        val category = categories.find { it.id == catId } ?: continue
        if (topCategory == null || amount > topCategory.amount) topCategory = CategoryAmount(category, amount)
    }

    val prevExpenses = previousMonthTxns.filter { it.type == TransactionType.Expense }.fold(0.0) { s, t -> s + t.amount }
    val monthOverMonthChange = if (prevExpenses > 0) (expensesTotal - prevExpenses) / prevExpenses else 0.0

    // Deliberately not clamped: overspending reads as negative, not as a flat 0%.
    val savingsRate = if (income > 0) (income - expensesTotal) / income else 0.0

    val prevIncome = previousMonthTxns.filter { it.type == TransactionType.Income }.fold(0.0) { s, t -> s + t.amount }
    val prevSavingsRate = if (prevIncome > 0) (prevIncome - prevExpenses) / prevIncome else null
    val savingsRateChange = prevSavingsRate?.let { savingsRate - it }

    return DashboardQuickStats(
        dailyAverage, projectedMonth, biggestExpense, topCategory, monthOverMonthChange, savingsRate, savingsRateChange,
    )
}

fun getPreviousMonthTransactions(
    transactions: List<Transaction>,
    monthStartDay: Int = DEFAULT_MONTH_START_DAY,
    now: Instant = nowInstant(),
): List<Transaction> =
    transactionsInPeriod(transactions, shiftPeriod(periodRange(PeriodType.Monthly, now, monthStartDay), -1))

data class GoalStatus(
    val goal: Goal,
    /** Sum of every contribution logged against this goal (withdrawals subtract). */
    val current: Double,
    /** `targetAmount - current`. Negative once overshot. */
    val remaining: Double,
    /** Unclamped — can exceed 100. */
    val percent: Double,
    val isComplete: Boolean,
    /** Paced by the average daily contribution since the earliest contribution; null when complete or no positive pace. */
    val projectedDate: Instant?,
)

/** Progress toward a savings goal, from its own contribution ledger. */
fun computeGoalStatus(goal: Goal, contributions: List<GoalContribution>, now: Instant = nowInstant()): GoalStatus {
    val own = contributions.filter { it.goalId == goal.id }
    val current = own.fold(0.0) { sum, c -> sum + c.amount }
    val remaining = goal.targetAmount - current
    val percent = if (goal.targetAmount > 0) (current / goal.targetAmount) * 100 else 0.0
    val isComplete = current >= goal.targetAmount

    var projectedDate: Instant? = null
    if (!isComplete && current > 0) {
        // Not `goal.createdAt`: a back-dated opening contribution would otherwise project in days.
        val start = own.fold(goal.createdAt) { min, c -> if (c.date < min) c.date else min }
        // An unparseable start is NaN on the web, which makes the rate NaN and skips the projection.
        val startDate = parseIso(start)
        if (startDate != null) {
            val daysElapsed = max(1, differenceInCalendarDays(now, startDate))
            val dailyRate = current / daysElapsed
            if (dailyRate > 0) projectedDate = jsDateOrNull { addDays(now, ceil(remaining / dailyRate).toInt()) }
        }
    }

    return GoalStatus(goal, current, remaining, percent, isComplete, projectedDate)
}

/** JS Dates stop at ±8.64e15 ms; past that the web gets an Invalid Date (JSON null). */
private const val JS_MAX_DATE_MS = 8_640_000_000_000_000L

private inline fun jsDateOrNull(make: () -> Instant): Instant? =
    runCatching(make).getOrNull()?.takeIf { abs(it.toEpochMilli()) <= JS_MAX_DATE_MS }

data class PersonBalance(
    val person: Person,
    /** Positive = they owe you; negative = you owe them; zero = settled up. */
    val balance: Double,
    /** ISO date of the most recent entry, or null if there are none. */
    val lastActivity: String?,
)

/** Net balance owed to/by a person, from their own debt-entry ledger. */
fun computePersonBalance(person: Person, entries: List<DebtEntry>): PersonBalance {
    val own = entries.filter { it.personId == person.id }
    val balance = own.fold(0.0) { sum, e -> sum + e.amount }
    val lastActivity = own.fold<DebtEntry, String?>(null) { latest, e ->
        if (latest.isNullOrEmpty() || e.date > latest) e.date else latest
    }
    return PersonBalance(person, balance, lastActivity)
}

/** Sum of every positive per-person balance — the total other people owe you. */
fun getTotalOwedToYou(people: List<Person>, entries: List<DebtEntry>): Double =
    people.fold(0.0) { sum, p ->
        val balance = computePersonBalance(p, entries).balance
        if (balance > 0) sum + balance else sum
    }

/** Sum of every negative per-person balance, as a positive number — the total you owe others. */
fun getTotalYouOwe(people: List<Person>, entries: List<DebtEntry>): Double =
    people.fold(0.0) { sum, p ->
        val balance = computePersonBalance(p, entries).balance
        if (balance < 0) sum - balance else sum
    }

/**
 * `String(n)` — JS Number::toString, exponent form (`1e-7`, `1e+21`) included. Digits come from
 * [shortestDecimal] rather than `Double.toString`, which is not always shortest before JDK 19
 * (core/js `jsNumberToString` has both gaps; reported to the orchestrator).
 */
internal fun jsNumberString(x: Double): String {
    if (x.isNaN()) return "NaN"
    if (x.isInfinite()) return if (x > 0) "Infinity" else "-Infinity"
    if (x == 0.0) return "0"
    val bd = shortestDecimal(abs(x)).stripTrailingZeros()
    val digits = bd.unscaledValue().toString()
    val k = digits.length
    val n = k - bd.scale() // value = 0.digits × 10^n
    val body = when {
        n in k..21 -> digits + "0".repeat(n - k)
        n in 1..21 -> digits.substring(0, n) + "." + digits.substring(n)
        n in -5..0 -> "0." + "0".repeat(-n) + digits
        else -> {
            val e = n - 1
            val mantissa = if (k == 1) digits else digits[0] + "." + digits.substring(1)
            mantissa + "e" + (if (e >= 0) "+" else "-") + abs(e)
        }
    }
    return if (x < 0) "-$body" else body
}

private val FORMULA_START = Regex("^[=+\\-@\t\r]")

/** Convert transactions to a CSV string, neutralising spreadsheet formulas (OWASP CSV injection). */
fun transactionsToCsv(transactions: List<Transaction>, categories: List<Category>, accounts: List<Account>): String {
    val catMap = categories.associate { it.id to it.name }
    val accMap = accounts.associate { it.id to it.name }
    // A cell opening with = + - @ tab or CR is run as a formula by Excel/Sheets; a leading `'`
    // makes it plain text.
    fun escape(v: String): String {
        val safe = if (FORMULA_START.containsMatchIn(v)) "'$v" else v
        return "\"${safe.replace("\"", "\"\"")}\""
    }
    val header = listOf("Date", "Type", "Amount", "Account", "To Account", "Category", "Note", "Split Detail").joinToString(",")
    val rows = transactions.map { t ->
        val splits = t.splits
        val isSplit = !splits.isNullOrEmpty()
        val splitDetail = if (isSplit) {
            splits!!.joinToString(" | ") { s -> "${catMap[s.categoryId] ?: "Unknown"}: ${jsToFixed(s.amount, 2)}" }
        } else ""
        val to = t.toAccountId
        listOf(
            t.date,
            t.type.wire,
            jsNumberString(t.amount),
            escape(accMap[t.accountId] ?: ""),
            escape(if (!to.isNullOrEmpty()) accMap[to] ?: "" else ""),
            escape(if (isSplit) "Split (${splits!!.size})" else catMap[t.categoryId] ?: ""),
            escape(t.note),
            escape(splitDetail),
        ).joinToString(",")
    }
    return (listOf(header) + rows).joinToString("\n")
}
