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
import com.slowatcoding.finio.core.model.DepositCompounding
import com.slowatcoding.finio.core.model.DepositTerms
import com.slowatcoding.finio.core.model.RecurrenceFrequency
import com.slowatcoding.finio.core.model.TransactionSplit
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
    val minimumDuePercent: Double? = null,
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
    /** ≥2 entries summing to [amount]; [categoryId] is then `""`. */
    val splits: List<TransactionSplit>? = null,
)

@Serializable
data class SampleBudgetSpec(
    val categoryId: String,
    /** Set for a label budget (with [categoryId] `""`). */
    val labelId: String? = null,
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
    val linkedAccountKey: String? = null,
)

@Serializable
data class SampleContributionSpec(val goalKey: String, val amount: Double, val date: String, val note: String)

@Serializable
data class SamplePersonSpec(val key: String, val name: String, val icon: String, val color: String)

@Serializable
data class SampleDebtEntrySpec(val personKey: String, val amount: Double, val date: String, val note: String)

@Serializable
data class SampleTemplateSpec(
    val name: String,
    val type: TransactionType,
    val amount: Double,
    val accountKey: String,
    val toAccountKey: String? = null,
    val categoryId: String,
    val note: String,
    val labels: List<String>,
    val splits: List<TransactionSplit>? = null,
)

@Serializable
data class SampleDepositSpec(
    /** [AccountType.Fd] or [AccountType.Rd]. */
    val type: AccountType,
    val name: String,
    val color: String,
    /** FD principal, or RD monthly installment. */
    val amount: Double,
    val interestRate: Double,
    val startDate: String,
    val maturityDate: String? = null,
    val tenureMonths: Int? = null,
    val compounding: DepositCompounding? = null,
    val linkedAccountKey: String,
    val deductPast: Boolean,
)

@Serializable
data class SampleLoanSpec(
    val key: String,
    val name: String,
    val principal: Double,
    val interestRate: Double,
    val tenureMonths: Int,
    val startDate: String,
    val accountKey: String,
    val categoryId: String,
)

@Serializable
data class SampleLoanPrepaymentSpec(val loanKey: String, val amount: Double, val date: String, val note: String)

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
    val templates: List<SampleTemplateSpec>,
    val deposits: List<SampleDepositSpec>,
    val loans: List<SampleLoanSpec>,
    val loanPrepayments: List<SampleLoanPrepaymentSpec>,
)

private const val PRIMARY = "primary"
private const val SAVINGS = "savings"
private const val CARD = "card"
private const val CASH = "cash"
private const val VACATION_GOAL = "vacation"
private const val EMERGENCY_GOAL = "emergency"
private const val LAPTOP_GOAL = "laptop"
private const val RAHUL = "rahul"
private const val PRIYA = "priya"
private const val AMIT = "amit"
private const val CAR_LOAN = "car-loan"

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
        SampleAccountSpec(PRIMARY, "HDFC Savings", AccountType.Savings, "#146b54", "landmark", 35000.0),
        SampleAccountSpec(SAVINGS, "SBI Savings", AccountType.Savings, "#22c55e", "piggy-bank", 120000.0),
        SampleAccountSpec(CASH, "Cash Wallet", AccountType.Cash, "#f59e0b", "banknote", 4000.0),
        SampleAccountSpec(
            CARD, "Credit Card", AccountType.Credit, "#ef4444", "credit-card", 0.0,
            creditLimit = 150000.0, statementCloseDay = 28, paymentDueDays = 18, minimumDuePercent = 5.0,
        ),
    )

    val transactions = ArrayList<SampleTransactionSpec>()
    fun add(
        type: TransactionType, amount: Double, account: String, categoryId: String, date: String, note: String,
        labels: List<String> = emptyList(), toAccount: String? = null, splits: List<TransactionSplit>? = null,
    ) {
        transactions += SampleTransactionSpec(type, amount, account, toAccount, categoryId, date, note, labels, splits)
    }
    val income = TransactionType.Income
    val expense = TransactionType.Expense
    val transfer = TransactionType.Transfer

    // Month-by-month routine, oldest first. Salary, rent and bills run on the primary account;
    // the card takes fuel, shopping and subscriptions; small spends come out of the cash wallet.
    val electricity = mapOf(3 to 2200.0, 2 to 2650.0, 1 to 2400.0)
    val bigShop = mapOf(3 to 2400.0, 2 to 2850.0, 1 to 2600.0)
    val groceryAmounts = listOf(1800.0, 2200.0, 1500.0, 2000.0)
    val foodAmounts = listOf(350.0, 620.0, 480.0, 900.0, 275.0)
    for (m in 3 downTo 1) {
        add(income, 72000.0, PRIMARY, "cat-9", at(now, m, 1), "Monthly salary")
        add(expense, 18000.0, PRIMARY, "cat-8", at(now, m, 2), "Monthly rent", listOf("lbl-1", "lbl-5"))
        add(expense, 799.0, PRIMARY, "cat-5", at(now, m, 7), "Airtel broadband", listOf("lbl-1", "lbl-3"))
        add(expense, 5000.0, PRIMARY, "cat-20", at(now, m, 7), "Mutual fund SIP", listOf("lbl-6", "lbl-3"))
        add(expense, electricity.getValue(m), PRIMARY, "cat-5", at(now, m, 12), "Electricity bill", listOf("lbl-1"))
        add(transfer, 3000.0, PRIMARY, "cat-13", at(now, m, 10), "ATM withdrawal", toAccount = CASH)

        GROCERY_DAYS.forEachIndexed { i, day ->
            add(expense, groceryAmounts[i], PRIMARY, "cat-1", at(now, m, day), "Groceries", listOf("lbl-1"))
        }
        FOOD_DAYS.forEachIndexed { i, day ->
            add(
                expense, foodAmounts[i], PRIMARY, "cat-1", at(now, m, day),
                if (i % 2 == 0) "Lunch with friends" else "Zomato order",
                if (i % 2 == 0) listOf("lbl-8") else emptyList(),
            )
        }

        add(expense, 450.0, PRIMARY, "cat-2", at(now, m, 22), "Cab rides")
        add(expense, 350.0, CASH, "cat-1", at(now, m, 4), "Vegetable vendor", listOf("lbl-1"))
        add(expense, 180.0, CASH, "cat-2", at(now, m, 9), "Auto rickshaw")
        add(expense, 140.0, CASH, "cat-1", at(now, m, 18), "Street food", listOf("lbl-8"))

        // Card: a recognisable recurring-looking subscription (same note, same amount, monthly)
        // left uncovered by any recurring rule on purpose, so the Insights feed offers to turn it
        // into one; plus a split bill — one receipt, two categories.
        add(expense, 1500.0, CARD, "cat-2", at(now, m, 8), "Fuel")
        add(expense, 499.0, CARD, "cat-18", at(now, m, 5), "Netflix", listOf("lbl-3"))
        add(expense, 1400.0, CARD, "cat-3", at(now, m, 14), "Online shopping", listOf("lbl-8"))
        add(expense, 800.0, CARD, "cat-4", at(now, m, 19), "Movie night", listOf("lbl-8"))
        add(
            expense, bigShop.getValue(m), CARD, "", at(now, m, 16), "DMart monthly stock-up", listOf("lbl-14"),
            splits = listOf(TransactionSplit("cat-1", bigShop.getValue(m) - 600.0), TransactionSplit("cat-17", 600.0)),
        )
    }

    // Paying the card down, and the rest of the one-offs.
    for (m in 2 downTo 1) {
        add(transfer, 6500.0, PRIMARY, "cat-13", at(now, m, 20), "Credit card payment", toAccount = CARD)
    }
    add(income, 12000.0, PRIMARY, "cat-9", at(now, 2, 15), "Freelance project", listOf("lbl-11"))
    add(expense, 999.0, CARD, "cat-18", at(now, 2, 16), "Domain renewal", listOf("lbl-11"))
    add(income, 25000.0, PRIMARY, "cat-33", at(now, 2, 28), "Performance bonus")
    add(income, 1240.0, SAVINGS, "cat-23", at(now, 2, 28), "Savings interest")
    add(expense, 12500.0, PRIMARY, "cat-26", at(now, 3, 15), "Health insurance premium", listOf("lbl-5", "lbl-4"))
    add(expense, 6000.0, PRIMARY, "cat-20", at(now, 1, 15), "Advance tax", listOf("lbl-4", "lbl-5"))
    add(expense, 4200.0, CARD, "cat-19", at(now, 2, 18), "Car service", listOf("lbl-1"))
    add(expense, 650.0, PRIMARY, "cat-6", at(now, 2, 24), "Pharmacy", listOf("lbl-14"))
    add(expense, 800.0, PRIMARY, "cat-6", at(now, 1, 9), "Doctor consultation", listOf("lbl-14"))
    add(expense, 1500.0, CARD, "cat-16", at(now, 2, 10), "Birthday gift for Neha", listOf("lbl-9"))
    add(expense, 600.0, CASH, "cat-17", at(now, 2, 21), "Salon", listOf("lbl-8"))
    add(expense, 150.0, CASH, "cat-24", at(now, 1, 6), "Courier charges")
    add(expense, 12400.0, CARD, "cat-15", at(now, 1, 12), "Goa flights", listOf("lbl-14"))
    add(expense, 9600.0, CARD, "cat-15", at(now, 1, 13), "Goa hotel", listOf("lbl-14"))
    add(expense, 3000.0, PRIMARY, "cat-1", at(now, 1, 16), "Team dinner (paid for all)", listOf("lbl-7"))
    add(
        expense, 2799.0, CARD, "", at(now, 1, 22), "Amazon order",
        splits = listOf(TransactionSplit("cat-3", 1999.0), TransactionSplit("cat-16", 800.0)),
    )

    val budgets = listOf(
        SampleBudgetSpec("", null, 50000.0, BudgetPeriod.Monthly, false),
        SampleBudgetSpec("cat-1", null, 9000.0, BudgetPeriod.Monthly, false),
        SampleBudgetSpec("cat-3", null, 3000.0, BudgetPeriod.Monthly, true),
        SampleBudgetSpec("cat-2", null, 1500.0, BudgetPeriod.Weekly, false),
        SampleBudgetSpec("cat-16", null, 8000.0, BudgetPeriod.Yearly, false),
        SampleBudgetSpec("", "lbl-14", 10000.0, BudgetPeriod.Monthly, false),
    )

    val goals = listOf(
        SampleGoalSpec(VACATION_GOAL, "Vacation Fund", "plane", "#f59e0b", 60000.0, addDays(now, 240).toIso(), SAVINGS),
        SampleGoalSpec(EMERGENCY_GOAL, "Emergency Fund", "target", "#146b54", 150000.0, null, SAVINGS),
        SampleGoalSpec(LAPTOP_GOAL, "New Laptop", "laptop", "#6366f1", 90000.0, addDays(now, 150).toIso()),
    )

    val contributions = listOf(
        SampleContributionSpec(EMERGENCY_GOAL, 20000.0, at(now, 2, 5), "Starting balance"),
        SampleContributionSpec(EMERGENCY_GOAL, 10000.0, at(now, 1, 5), "Monthly top-up"),
        SampleContributionSpec(LAPTOP_GOAL, 15000.0, at(now, 2, 20), "From the bonus"),
        SampleContributionSpec(LAPTOP_GOAL, 10000.0, at(now, 1, 20), "Side-project income"),
        SampleContributionSpec(VACATION_GOAL, 6000.0, at(now, 1, 25), "Seed money"),
    )

    // Rules going forward (the matching past transactions above are their history): rent, the
    // SIP, a card subscription, and a goal-linked transfer that auto-funds the Vacation Fund.
    val recurring = listOf(
        SampleRecurringSpec(
            TransactionType.Expense, 18000.0, PRIMARY, null, "cat-8", "Monthly rent", listOf("lbl-1", "lbl-5"),
            RecurrenceFrequency.Monthly, at(now, 0, 2),
        ),
        SampleRecurringSpec(
            TransactionType.Expense, 5000.0, PRIMARY, null, "cat-20", "Mutual fund SIP", listOf("lbl-6", "lbl-3"),
            RecurrenceFrequency.Monthly, at(now, 0, 7),
        ),
        SampleRecurringSpec(
            TransactionType.Expense, 130.0, CARD, null, "cat-18", "Google One storage", listOf("lbl-3"),
            RecurrenceFrequency.Monthly, at(now, 0, 21),
        ),
        SampleRecurringSpec(
            TransactionType.Transfer, 3000.0, PRIMARY, SAVINGS, "cat-13", "Vacation savings", emptyList(),
            RecurrenceFrequency.Monthly, at(now, 0, 25), VACATION_GOAL,
        ),
    )

    val templates = listOf(
        SampleTemplateSpec(
            "DMart run", TransactionType.Expense, 2500.0, CARD, null, "", "DMart", listOf("lbl-14"),
            listOf(TransactionSplit("cat-1", 1900.0), TransactionSplit("cat-17", 600.0)),
        ),
        SampleTemplateSpec("Coffee", TransactionType.Expense, 180.0, CASH, null, "cat-1", "Coffee", listOf("lbl-8")),
        SampleTemplateSpec("Cab to office", TransactionType.Expense, 250.0, CARD, null, "cat-2", "Cab to office", emptyList()),
        SampleTemplateSpec(
            "ATM withdrawal", TransactionType.Transfer, 2000.0, PRIMARY, CASH, "cat-13", "ATM withdrawal", emptyList(),
        ),
    )

    // A fixed deposit (funded by a real transfer from SBI) and a recurring deposit (past
    // installments folded into its opening balance).
    val deposits = listOf(
        SampleDepositSpec(
            AccountType.Fd, "SBI Fixed Deposit", "#6366f1", 50000.0, 7.1, at(now, 4, 5),
            maturityDate = addDays(now, 600).toIso(), compounding = DepositCompounding.Quarterly,
            linkedAccountKey = SAVINGS, deductPast = true,
        ),
        SampleDepositSpec(
            AccountType.Rd, "Monthly RD", "#0ea5e9", 5000.0, 6.5, at(now, 3, 5),
            tenureMonths = 24, linkedAccountKey = PRIMARY, deductPast = false,
        ),
    )

    // A car loan whose EMIs due before today were paid outside Finio, plus one prepayment that
    // is a real expense.
    val loans = listOf(
        SampleLoanSpec(CAR_LOAN, "Car Loan", 450000.0, 9.2, 60, at(now, 3, 10), PRIMARY, "cat-27"),
    )
    val loanPrepayments = listOf(
        SampleLoanPrepaymentSpec(CAR_LOAN, 20000.0, at(now, 1, 26), "Part-payment from bonus"),
    )

    val people = listOf(
        SamplePersonSpec(RAHUL, "Rahul", "user", "#06b6d4"),
        SamplePersonSpec(PRIYA, "Priya", "user", "#ec4899"),
        SamplePersonSpec(AMIT, "Amit", "user", "#8b5cf6"),
    )
    val debtEntries = listOf(
        SampleDebtEntrySpec(RAHUL, 1500.0, at(now, 1, 16), "Lent for dinner"),
        SampleDebtEntrySpec(RAHUL, -500.0, at(now, 0, 3), "Paid back via UPI"),
        SampleDebtEntrySpec(PRIYA, -2200.0, at(now, 2, 9), "Concert tickets she booked"),
        SampleDebtEntrySpec(AMIT, 5000.0, at(now, 2, 25), "Short-term loan"),
    )

    return SampleData(
        accounts, transactions, budgets, recurring, goals, contributions, people, debtEntries,
        templates, deposits, loans, loanPrepayments,
    )
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
    val minimumDuePercent: Double? = null,
)

/** `Omit<Goal, 'id' | 'createdAt'>`. */
@Serializable
data class NewSampleGoal(
    val name: String,
    val icon: String,
    val color: String,
    val targetAmount: Double,
    val targetDate: String? = null,
    val linkedAccountId: String? = null,
)

/** `Omit<Person, 'id' | 'createdAt'>`. */
@Serializable
data class NewSamplePerson(val name: String, val icon: String, val color: String)

/** `Omit<Budget, 'id' | 'createdAt'>`. */
@Serializable
data class NewSampleBudget(
    val categoryId: String,
    val labelId: String? = null,
    val amount: Double,
    val period: BudgetPeriod,
    val rollover: Boolean,
)

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

/** The TS `NewDeposit`. */
@Serializable
data class NewSampleDeposit(
    val type: AccountType,
    val name: String,
    val color: String,
    val deductPast: Boolean,
    val terms: DepositTerms,
)

/** `Omit<TransactionTemplate, 'id' | 'createdAt'>`. */
@Serializable
data class NewSampleTemplate(
    val name: String,
    val type: TransactionType,
    val amount: Double,
    val accountId: String,
    val categoryId: String,
    val note: String,
    val labels: List<String>,
    val toAccountId: String? = null,
    val splits: List<TransactionSplit>? = null,
)

/** `Omit<Loan, 'id' | 'createdAt' | 'recurringId' | 'closedAt'>`. */
@Serializable
data class NewSampleLoan(
    val name: String,
    val principal: Double,
    val interestRate: Double,
    val tenureMonths: Int,
    val startDate: String,
    val accountId: String,
    val categoryId: String,
)

/** `Omit<LoanPrepayment, 'id' | 'createdAt' | 'transactionId'>`. */
@Serializable
data class NewSampleLoanPrepayment(val loanId: String, val amount: Double, val date: String, val note: String)

/** `Omit<Transaction, 'id' | 'createdAt'>`, as the sample fills it (never recurring). */
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
    val splits: List<TransactionSplit>? = null,
)

/** The store actions [loadSampleData] needs — mirrors the TS `SampleDataActions`. */
interface SampleDataActions {
    fun addAccount(account: NewSampleAccount): String
    fun addGoal(goal: NewSampleGoal): String
    fun addPerson(person: NewSamplePerson): String
    fun addBudget(budget: NewSampleBudget)
    fun addRecurring(rule: NewSampleRecurring): String
    fun addTemplate(template: NewSampleTemplate): String
    fun addDeposit(deposit: NewSampleDeposit): String
    fun addLoan(loan: NewSampleLoan): String
    fun addLoanPrepayment(prepayment: NewSampleLoanPrepayment): String?
    fun addContribution(contribution: NewSampleContribution): String
    fun addDebtEntry(entry: NewSampleDebtEntry): String
    fun bulkAddTransactions(transactions: List<NewSampleTransaction>): Int
}

/**
 * Applies [generateSampleData] through the store's own actions — the same path manual entry
 * takes, so balances, budgets and the recurring schedule end up exactly as consistent.
 * Call order matches the TS: accounts, deposits, goals, people, budgets, recurring, templates,
 * loans, loan prepayments, contributions, debts, then one bulk transaction insert.
 */
fun loadSampleData(actions: SampleDataActions, now: Instant = nowInstant()) {
    val data = generateSampleData(now)

    val accountIds = LinkedHashMap<String, String>()
    for (a in data.accounts) {
        accountIds[a.key] = actions.addAccount(
            NewSampleAccount(
                a.name, a.type, a.color, a.icon, a.balance, a.creditLimit, a.statementCloseDay, a.paymentDueDays,
                a.minimumDuePercent,
            ),
        )
    }
    fun resolveAccount(key: String): String =
        accountIds[key]?.takeIf { it.isNotEmpty() } ?: error("Sample data referenced an unknown account key \"$key\"")

    for (d in data.deposits) {
        actions.addDeposit(
            NewSampleDeposit(
                type = d.type, name = d.name, color = d.color, deductPast = d.deductPast,
                terms = DepositTerms(
                    amount = d.amount, interestRate = d.interestRate, startDate = d.startDate,
                    maturityDate = d.maturityDate, tenureMonths = d.tenureMonths, compounding = d.compounding,
                    linkedAccountId = resolveAccount(d.linkedAccountKey),
                ),
            ),
        )
    }

    val goalIds = LinkedHashMap<String, String>()
    for (g in data.goals) {
        goalIds[g.key] = actions.addGoal(
            NewSampleGoal(
                g.name, g.icon, g.color, g.targetAmount, g.targetDate?.takeIf { it.isNotEmpty() },
                g.linkedAccountKey?.takeIf { it.isNotEmpty() }?.let(::resolveAccount),
            ),
        )
    }

    val personIds = LinkedHashMap<String, String>()
    for (p in data.people) personIds[p.key] = actions.addPerson(NewSamplePerson(p.name, p.icon, p.color))

    for (b in data.budgets) actions.addBudget(NewSampleBudget(b.categoryId, b.labelId, b.amount, b.period, b.rollover))

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

    for (t in data.templates) {
        actions.addTemplate(
            NewSampleTemplate(
                name = t.name, type = t.type, amount = t.amount, accountId = resolveAccount(t.accountKey),
                categoryId = t.categoryId, note = t.note, labels = t.labels,
                toAccountId = t.toAccountKey?.takeIf { it.isNotEmpty() }?.let(::resolveAccount),
                splits = t.splits,
            ),
        )
    }

    val loanIds = LinkedHashMap<String, String>()
    for (l in data.loans) {
        loanIds[l.key] = actions.addLoan(
            NewSampleLoan(
                l.name, l.principal, l.interestRate, l.tenureMonths, l.startDate,
                resolveAccount(l.accountKey), l.categoryId,
            ),
        )
    }

    for (p in data.loanPrepayments) {
        val loanId = loanIds[p.loanKey]?.takeIf { it.isNotEmpty() } ?: continue
        actions.addLoanPrepayment(NewSampleLoanPrepayment(loanId, p.amount, p.date, p.note))
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
                splits = t.splits,
            )
        },
    )
}
