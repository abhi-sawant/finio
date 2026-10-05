package com.slowatcoding.finio.core.data

import com.slowatcoding.finio.core.js.addDays
import com.slowatcoding.finio.core.js.fullYear
import com.slowatcoding.finio.core.js.localDate
import com.slowatcoding.finio.core.js.month0
import com.slowatcoding.finio.core.js.nowInstant
import com.slowatcoding.finio.core.js.subMonths
import com.slowatcoding.finio.core.js.toIso
import com.slowatcoding.finio.core.model.AccountType
import com.slowatcoding.finio.core.model.BudgetPeriod
import com.slowatcoding.finio.core.model.RecurrenceFrequency
import com.slowatcoding.finio.core.model.TransactionType
import kotlinx.serialization.Serializable
import java.time.Instant

// Port of web/src/data/sampleData.ts — the hand-designed sample dataset offered in onboarding
// (and a manual-QA fixture). Accounts, goals and people have no ids yet (the store mints those),
// so everything references them by a local `key` that [loadSampleData] resolves once each is
// created. Deliberately deterministic: the same `now` always produces the same dataset.

@Serializable
data class SampleAccountSpec(
    val key: String,
    val name: String,
    val type: AccountType,
    val color: String,
    val icon: String,
    val balance: Double,
    val creditLimit: Double? = null,
    val statementCloseDay: Int? = null,
    val paymentDueDays: Int? = null,
)

@Serializable
data class SampleTransactionSpec(
    val type: TransactionType,
    val amount: Double,
    val accountKey: String,
    val toAccountKey: String? = null,
    val categoryId: String,
    val date: String,
    val note: String,
    val labels: List<String>,
)

@Serializable
data class SampleBudgetSpec(
    val categoryId: String,
    val amount: Double,
    val period: BudgetPeriod,
    val rollover: Boolean,
)

@Serializable
data class SampleRecurringSpec(
    val type: TransactionType,
    val amount: Double,
    val accountKey: String,
    val toAccountKey: String? = null,
    val categoryId: String,
    val note: String,
    val labels: List<String>,
    val frequency: RecurrenceFrequency,
    val startDate: String,
    val goalKey: String? = null,
)

@Serializable
data class SampleGoalSpec(
    val key: String,
    val name: String,
    val icon: String,
    val color: String,
    val targetAmount: Double,
    val targetDate: String? = null,
)

@Serializable
data class SampleContributionSpec(val goalKey: String, val amount: Double, val date: String, val note: String)

@Serializable
data class SamplePersonSpec(val key: String, val name: String, val icon: String, val color: String)

@Serializable
data class SampleDebtEntrySpec(val personKey: String, val amount: Double, val date: String, val note: String)

@Serializable
data class SampleData(
    val accounts: List<SampleAccountSpec>,
    val transactions: List<SampleTransactionSpec>,
    val budgets: List<SampleBudgetSpec>,
    val recurring: List<SampleRecurringSpec>,
    val goals: List<SampleGoalSpec>,
    val contributions: List<SampleContributionSpec>,
    val people: List<SamplePersonSpec>,
    val debtEntries: List<SampleDebtEntrySpec>,
)

private const val CHECKING = "checking"
private const val SAVINGS = "savings"
private const val CARD = "card"
private const val VACATION_GOAL = "vacation"
private const val EMERGENCY_GOAL = "emergency"
private const val RAHUL = "rahul"

/** Day-of-month offsets used to spread a category's charges across a sample month. */
private val GROCERY_DAYS = listOf(3, 10, 17, 24)
private val FOOD_DAYS = listOf(2, 6, 13, 20, 27)

/** `new Date(base.getFullYear(), base.getMonth(), day, hour)` where base = subMonths(now, monthsAgo). */
private fun at(now: Instant, monthsAgo: Int, day: Int, hour: Int = 12): String {
    val base = subMonths(now, monthsAgo)
    return localDate(base.fullYear, base.month0, day, hour).toIso()
}

fun generateSampleData(now: Instant = nowInstant()): SampleData {
    val accounts = listOf(
        SampleAccountSpec(CHECKING, "HDFC Checking", AccountType.Checking, "#146b54", "landmark", 15000.0),
        SampleAccountSpec(SAVINGS, "Savings", AccountType.Savings, "#22c55e", "piggy-bank", 40000.0),
        SampleAccountSpec(
            CARD, "Credit Card", AccountType.Credit, "#ef4444", "credit-card", 0.0,
            creditLimit = 100000.0, statementCloseDay = 28, paymentDueDays = 18,
        ),
    )

    val transactions = ArrayList<SampleTransactionSpec>()
    fun expense(amount: Double, account: String, categoryId: String, date: String, note: String, labels: List<String>) {
        transactions += SampleTransactionSpec(TransactionType.Expense, amount, account, null, categoryId, date, note, labels)
    }

    // Salary + freelance income, one of each per sample month.
    for (m in 3 downTo 1) {
        transactions += SampleTransactionSpec(TransactionType.Income, 65000.0, CHECKING, null, "cat-9", at(now, m, 1), "Monthly salary", emptyList())
    }
    transactions += SampleTransactionSpec(TransactionType.Income, 12000.0, CHECKING, null, "cat-10", at(now, 2, 15), "Freelance project", emptyList())

    // Rent, paid from Checking, every sample month.
    for (m in 3 downTo 1) expense(15000.0, CHECKING, "cat-8", at(now, m, 2), "Monthly rent", listOf("lbl-1"))

    // Groceries and everyday food, on Checking, spread across the month.
    val groceryAmounts = listOf(1800.0, 2200.0, 1500.0, 2000.0)
    val foodAmounts = listOf(350.0, 620.0, 480.0, 900.0, 275.0)
    for (m in 3 downTo 1) {
        GROCERY_DAYS.forEachIndexed { i, day ->
            expense(groceryAmounts[i], CHECKING, "cat-25", at(now, m, day), "Groceries", listOf("lbl-1"))
        }
        FOOD_DAYS.forEachIndexed { i, day ->
            expense(foodAmounts[i], CHECKING, "cat-1", at(now, m, day), if (i % 2 == 0) "Lunch with friends" else "Zomato order", listOf("lbl-2"))
        }
    }

    // Transport, on Checking.
    for (m in 3 downTo 1) {
        expense(1200.0, CHECKING, "cat-2", at(now, m, 8), "Fuel", emptyList())
        expense(450.0, CHECKING, "cat-2", at(now, m, 22), "Cab rides", emptyList())
    }

    // Card spend, including a recurring-looking subscription deliberately left uncovered by any
    // recurring rule, so the Insights feed offers to turn it into one.
    for (m in 3 downTo 1) {
        expense(499.0, CARD, "cat-18", at(now, m, 5), "Netflix", listOf("lbl-3"))
        expense(1400.0, CARD, "cat-3", at(now, m, 14), "Online shopping", listOf("lbl-2"))
        expense(800.0, CARD, "cat-4", at(now, m, 19), "Movie night", listOf("lbl-2"))
    }

    // Utilities, paid from Checking.
    for (m in 3 downTo 1) expense(2200.0, CHECKING, "cat-5", at(now, m, 12), "Electricity bill", listOf("lbl-1"))

    // Paying down the card from Checking, each sample month.
    for (m in 2 downTo 1) {
        transactions += SampleTransactionSpec(TransactionType.Transfer, 5000.0, CHECKING, CARD, "cat-13", at(now, m, 20), "Credit card payment", emptyList())
    }

    val budgets = listOf(
        SampleBudgetSpec("", 40000.0, BudgetPeriod.Monthly, false),
        SampleBudgetSpec("cat-1", 8000.0, BudgetPeriod.Monthly, false),
        SampleBudgetSpec("cat-3", 3000.0, BudgetPeriod.Monthly, true),
    )

    val goals = listOf(
        SampleGoalSpec(VACATION_GOAL, "Vacation Fund", "plane", "#f59e0b", 60000.0, addDays(now, 240).toIso()),
        SampleGoalSpec(EMERGENCY_GOAL, "Emergency Fund", "target", "#146b54", 100000.0),
    )

    val contributions = listOf(SampleContributionSpec(EMERGENCY_GOAL, 20000.0, at(now, 2, 5), "Starting balance"))

    // Rent going forward as a real rule (the rent transactions above are its history), and a
    // goal-linked transfer that auto-funds the Vacation Fund.
    val recurring = listOf(
        SampleRecurringSpec(
            TransactionType.Expense, 15000.0, CHECKING, null, "cat-8", "Monthly rent", listOf("lbl-1"),
            RecurrenceFrequency.Monthly, at(now, 0, 2),
        ),
        SampleRecurringSpec(
            TransactionType.Transfer, 3000.0, CHECKING, SAVINGS, "cat-13", "Vacation savings", emptyList(),
            RecurrenceFrequency.Monthly, at(now, 0, 25), VACATION_GOAL,
        ),
    )

    val people = listOf(SamplePersonSpec(RAHUL, "Rahul", "user", "#06b6d4"))
    val debtEntries = listOf(SampleDebtEntrySpec(RAHUL, 1500.0, at(now, 1, 16), "Lent for dinner"))

    return SampleData(accounts, transactions, budgets, recurring, goals, contributions, people, debtEntries)
}

// ---- Loading through the store ----------------------------------------------------------------

/** Input of [SampleDataActions.addAccount] — the store's new-account shape. */
@Serializable
data class NewSampleAccount(
    val name: String,
    val type: AccountType,
    val color: String,
    val icon: String,
    val balance: Double,
    val creditLimit: Double? = null,
    val statementCloseDay: Int? = null,
    val paymentDueDays: Int? = null,
)

/** `Omit<Goal, 'id' | 'createdAt'>` (linkedAccountId is never set by the sample). */
@Serializable
data class NewSampleGoal(val name: String, val icon: String, val color: String, val targetAmount: Double, val targetDate: String? = null)

/** `Omit<Person, 'id' | 'createdAt'>`. */
@Serializable
data class NewSamplePerson(val name: String, val icon: String, val color: String)

/** `Omit<Budget, 'id' | 'createdAt'>` (labelId is never set by the sample). */
@Serializable
data class NewSampleBudget(val categoryId: String, val amount: Double, val period: BudgetPeriod, val rollover: Boolean)

/** `Omit<RecurringTransaction, 'id' | 'createdAt' | 'occurrenceCount' | 'lastRunDate'>`, as the sample fills it. */
@Serializable
data class NewSampleRecurring(
    val type: TransactionType,
    val amount: Double,
    val accountId: String,
    val categoryId: String,
    val note: String,
    val labels: List<String>,
    val frequency: RecurrenceFrequency,
    val startDate: String,
    val toAccountId: String? = null,
    val goalId: String? = null,
)

/** `Omit<GoalContribution, 'id' | 'createdAt'>`. */
@Serializable
data class NewSampleContribution(val goalId: String, val amount: Double, val date: String, val note: String)

/** `Omit<DebtEntry, 'id' | 'createdAt'>` (never settled). */
@Serializable
data class NewSampleDebtEntry(val personId: String, val amount: Double, val date: String, val note: String)

/** `Omit<Transaction, 'id' | 'createdAt'>`, as the sample fills it (never recurring or split). */
@Serializable
data class NewSampleTransaction(
    val type: TransactionType,
    val amount: Double,
    val accountId: String,
    val categoryId: String,
    val date: String,
    val note: String,
    val labels: List<String>,
    val toAccountId: String? = null,
)

/** The store actions [loadSampleData] needs — mirrors the TS `SampleDataActions`. */
interface SampleDataActions {
    fun addAccount(account: NewSampleAccount): String
    fun addGoal(goal: NewSampleGoal): String
    fun addPerson(person: NewSamplePerson): String
    fun addBudget(budget: NewSampleBudget)
    fun addRecurring(rule: NewSampleRecurring): String
    fun addContribution(contribution: NewSampleContribution): String
    fun addDebtEntry(entry: NewSampleDebtEntry): String
    fun bulkAddTransactions(transactions: List<NewSampleTransaction>): Int
}

/**
 * Applies [generateSampleData] through the store's own actions — the same path manual entry
 * takes, so balances, budgets and the recurring schedule end up exactly as consistent.
 * Call order matches the TS: accounts, goals, people, budgets, recurring, contributions, debts,
 * then one bulk transaction insert.
 */
fun loadSampleData(actions: SampleDataActions, now: Instant = nowInstant()) {
    val data = generateSampleData(now)

    val accountIds = LinkedHashMap<String, String>()
    for (a in data.accounts) {
        accountIds[a.key] = actions.addAccount(
            NewSampleAccount(a.name, a.type, a.color, a.icon, a.balance, a.creditLimit, a.statementCloseDay, a.paymentDueDays),
        )
    }
    fun resolveAccount(key: String): String =
        accountIds[key]?.takeIf { it.isNotEmpty() } ?: error("Sample data referenced an unknown account key \"$key\"")

    val goalIds = LinkedHashMap<String, String>()
    for (g in data.goals) {
        goalIds[g.key] = actions.addGoal(NewSampleGoal(g.name, g.icon, g.color, g.targetAmount, g.targetDate?.takeIf { it.isNotEmpty() }))
    }

    val personIds = LinkedHashMap<String, String>()
    for (p in data.people) personIds[p.key] = actions.addPerson(NewSamplePerson(p.name, p.icon, p.color))

    for (b in data.budgets) actions.addBudget(NewSampleBudget(b.categoryId, b.amount, b.period, b.rollover))

    for (rule in data.recurring) {
        actions.addRecurring(
            NewSampleRecurring(
                type = rule.type,
                amount = rule.amount,
                accountId = resolveAccount(rule.accountKey),
                categoryId = rule.categoryId,
                note = rule.note,
                labels = rule.labels,
                frequency = rule.frequency,
                startDate = rule.startDate,
                toAccountId = rule.toAccountKey?.takeIf { it.isNotEmpty() }?.let(::resolveAccount),
                goalId = rule.goalKey?.takeIf { it.isNotEmpty() }?.let { goalIds[it] },
            ),
        )
    }

    for (c in data.contributions) {
        val goalId = goalIds[c.goalKey]?.takeIf { it.isNotEmpty() } ?: continue
        actions.addContribution(NewSampleContribution(goalId, c.amount, c.date, c.note))
    }

    for (e in data.debtEntries) {
        val personId = personIds[e.personKey]?.takeIf { it.isNotEmpty() } ?: continue
        actions.addDebtEntry(NewSampleDebtEntry(personId, e.amount, e.date, e.note))
    }

    actions.bulkAddTransactions(
        data.transactions.map { t ->
            NewSampleTransaction(
                type = t.type,
                amount = t.amount,
                accountId = resolveAccount(t.accountKey),
                categoryId = t.categoryId,
                date = t.date,
                note = t.note,
                labels = t.labels,
                toAccountId = t.toAccountKey?.takeIf { it.isNotEmpty() }?.let(::resolveAccount),
            )
        },
    )
}
