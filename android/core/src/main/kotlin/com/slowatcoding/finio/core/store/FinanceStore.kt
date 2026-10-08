package com.slowatcoding.finio.core.store

import com.slowatcoding.finio.core.calc.PlannedSnapshot
import com.slowatcoding.finio.core.calc.SnapshotPlanInput
import com.slowatcoding.finio.core.calc.TRANSFER_CATEGORY_ID
import com.slowatcoding.finio.core.calc.budgetScopeKey
import com.slowatcoding.finio.core.calc.isCategoryValidForType
import com.slowatcoding.finio.core.calc.planNetWorthSnapshots
import com.slowatcoding.finio.core.data.MISC_CATEGORY_ID
import com.slowatcoding.finio.core.data.NewSampleAccount
import com.slowatcoding.finio.core.data.NewSampleBudget
import com.slowatcoding.finio.core.data.NewSampleContribution
import com.slowatcoding.finio.core.data.NewSampleDebtEntry
import com.slowatcoding.finio.core.data.NewSampleGoal
import com.slowatcoding.finio.core.data.NewSamplePerson
import com.slowatcoding.finio.core.data.NewSampleRecurring
import com.slowatcoding.finio.core.data.NewSampleTransaction
import com.slowatcoding.finio.core.data.SampleDataActions
import com.slowatcoding.finio.core.data.defaultCategories
import com.slowatcoding.finio.core.data.defaultLabels
import com.slowatcoding.finio.core.data.defaultSettings
import com.slowatcoding.finio.core.deposit.accountDeleteBlockers
import com.slowatcoding.finio.core.deposit.depositInvested
import com.slowatcoding.finio.core.deposit.depositMaturityAmount
import com.slowatcoding.finio.core.deposit.depositMaturityDate
import com.slowatcoding.finio.core.deposit.planMaturities
import com.slowatcoding.finio.core.deposit.rdInstallmentsOnOrBefore
import com.slowatcoding.finio.core.id.newId
import com.slowatcoding.finio.core.importing.importedAccounts
import com.slowatcoding.finio.core.js.nowInstant
import com.slowatcoding.finio.core.js.parseJsDate
import com.slowatcoding.finio.core.js.startOfDay
import com.slowatcoding.finio.core.js.toIso
import com.slowatcoding.finio.core.loan.LoanPrepaymentInput
import com.slowatcoding.finio.core.loan.LoanScheduleInput
import com.slowatcoding.finio.core.loan.calculateEmi
import com.slowatcoding.finio.core.loan.maxPrepayment
import com.slowatcoding.finio.core.model.Account
import com.slowatcoding.finio.core.model.AccountType
import com.slowatcoding.finio.core.model.Budget
import com.slowatcoding.finio.core.model.BudgetPeriod
import com.slowatcoding.finio.core.model.Category
import com.slowatcoding.finio.core.model.CategoryRule
import com.slowatcoding.finio.core.model.CategoryType
import com.slowatcoding.finio.core.model.DebtEntry
import com.slowatcoding.finio.core.model.DepositCompounding
import com.slowatcoding.finio.core.model.DepositTerms
import com.slowatcoding.finio.core.model.FinanceState
import com.slowatcoding.finio.core.model.Goal
import com.slowatcoding.finio.core.model.GoalContribution
import com.slowatcoding.finio.core.model.ImportMode
import com.slowatcoding.finio.core.model.ImportPayload
import com.slowatcoding.finio.core.model.Label
import com.slowatcoding.finio.core.model.Loan
import com.slowatcoding.finio.core.model.LoanPrepayment
import com.slowatcoding.finio.core.model.NetWorthSnapshot
import com.slowatcoding.finio.core.model.Person
import com.slowatcoding.finio.core.model.RecurrenceFrequency
import com.slowatcoding.finio.core.model.RecurringTransaction
import com.slowatcoding.finio.core.model.RuleMatchType
import com.slowatcoding.finio.core.model.RuleScope
import com.slowatcoding.finio.core.model.Settings
import com.slowatcoding.finio.core.model.Transaction
import com.slowatcoding.finio.core.model.TransactionCategorization
import com.slowatcoding.finio.core.model.TransactionSplit
import com.slowatcoding.finio.core.model.TransactionTemplate
import com.slowatcoding.finio.core.model.TransactionType
import com.slowatcoding.finio.core.money.roundMoney
import com.slowatcoding.finio.core.period.normalizeMonthStartDay
import com.slowatcoding.finio.core.rules.ReplayOptions
import com.slowatcoding.finio.core.rules.planRuleApplication
import com.slowatcoding.finio.core.util.MAX_NAME_LENGTH
import com.slowatcoding.finio.core.util.MAX_NOTE_LENGTH
import com.slowatcoding.finio.core.util.cleanText
import com.slowatcoding.finio.core.util.jsTrim
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import java.time.Instant
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign

// Port of web/src/store/useFinanceStore.ts — every finance action, as an atomic reducer over
// [FinanceState]. Persistence is the app layer's job (observe [FinanceStore.state] and write
// `encodePersisted`); the persisted envelope and the v1→v16 migrations live in Persistence.kt.
//
// Differences from the Zustand store, all deliberate:
//  - `Partial<T>` updates are `(T) -> T` transforms (the id is always kept). A transform can clear
//    an optional field by setting it to null, which the TS did with an explicit `undefined`.
//  - `addLoanPrepayment` closes a fully-repaid loan inside the same atomic step, where the TS used
//    a second `set`; the final state is identical.
//  - "Settle up" is a real atomic action ([FinanceStore.settleUp]) instead of two calls from
//    the Debts page.
//  - The settlement undo stashes are per store instance rather than module-level.

/** Default category for interest a deposit pays out — the built-in "Interest". */
private const val INTEREST_CATEGORY_ID = "cat-23"

/** The store state of a fresh install: default categories, labels and settings, no data. */
fun initialFinanceState(): FinanceState =
    FinanceState(categories = defaultCategories, labels = defaultLabels, settings = defaultSettings)

/**
 * Categories the app cannot work without: Transfer (what every transfer is filed under) and
 * Miscellaneous (the catch-all a deleted category's rows are reassigned to).
 */
fun isProtectedCategory(id: String): Boolean = id == TRANSFER_CATEGORY_ID || id == MISC_CATEGORY_ID

/** The category a system-posted transfer carries — the same pick `AddTransaction` makes. */
private fun transferCategoryId(categories: List<Category>): String =
    categories.find { it.type == CategoryType.Both }?.id ?: MISC_CATEGORY_ID

private fun interestCategoryId(categories: List<Category>): String {
    if (categories.any { it.id == INTEREST_CATEGORY_ID }) return INTEREST_CATEGORY_ID
    return categories.find { it.type == CategoryType.Income }?.id ?: MISC_CATEGORY_ID
}

/**
 * Reassign a transaction off a deleted category to [fallbackId] — including inside `splits`,
 * where two entries landing on the same category merge (summing their amounts). A merge that
 * collapses to a single entry folds back into a plain `categoryId`.
 */
private fun reassignTransactionCategory(t: Transaction, deletedId: String, fallbackId: String): Transaction {
    val splits = t.splits
    if (!splits.isNullOrEmpty()) {
        val merged = LinkedHashMap<String, Double>()
        for (split in splits) {
            val categoryId = if (split.categoryId == deletedId) fallbackId else split.categoryId
            merged[categoryId] = (merged[categoryId] ?: 0.0) + split.amount
        }
        val next = merged.map { (categoryId, amount) -> TransactionSplit(categoryId, amount) }
        if (next.size == 1) return t.copy(categoryId = next[0].categoryId, splits = null)
        return t.copy(splits = next)
    }
    return if (t.categoryId == deletedId) t.copy(categoryId = fallbackId) else t
}

/** Union two collections by id, with [incoming] winning on conflicts (JS Map insertion order). */
private fun <T> mergeById(existing: List<T>, incoming: List<T>?, id: (T) -> String): List<T> {
    if (incoming == null) return existing
    val byId = LinkedHashMap<String, T>()
    for (row in existing) byId[id(row)] = row
    for (row in incoming) byId[id(row)] = row
    return byId.values.toList()
}

/** One snapshot per financial month; the later capture wins. Sorted by period key. */
internal fun dedupeSnapshotsByPeriod(snapshots: List<NetWorthSnapshot>): List<NetWorthSnapshot> {
    val byPeriod = LinkedHashMap<String, NetWorthSnapshot>()
    for (snapshot in snapshots) {
        val existing = byPeriod[snapshot.periodKey]
        if (existing == null || snapshot.createdAt > existing.createdAt) byPeriod[snapshot.periodKey] = snapshot
    }
    return byPeriod.values.sortedBy { it.periodKey }
}

private fun List<Transaction>.balanceTxs(): List<BalanceTx> = map { it.balanceTx() }

private fun applyBalanceDelta(accounts: List<Account>, tx: Transaction, direction: Int): List<Account> =
    applyBalanceDelta(accounts, tx.balanceTx(), direction)

// ---- Action inputs (the TS `Omit<T, 'id' | 'createdAt' …>` shapes) ---------------------------

/** `Omit<Account, 'id' | 'createdAt' | 'openingBalance'>`. */
@Serializable
data class NewAccount(
    val name: String,
    val type: AccountType,
    val color: String,
    val icon: String,
    val balance: Double,
    val creditLimit: Double? = null,
    val statementCloseDay: Int? = null,
    val paymentDueDays: Int? = null,
    val minimumDuePercent: Double? = null,
    val archivedAt: String? = null,
    val deposit: DepositTerms? = null,
)

/** `Omit<Transaction, 'id' | 'createdAt'>`. */
@Serializable
data class NewTransaction(
    val type: TransactionType,
    val amount: Double,
    val accountId: String,
    val toAccountId: String? = null,
    val categoryId: String,
    val date: String,
    val note: String = "",
    val merchant: String? = null,
    val forWhom: String? = null,
    val labels: List<String> = emptyList(),
    val recurringId: String? = null,
    val splits: List<TransactionSplit>? = null,
) {
    fun toTransaction(id: String, createdAt: String, note: String = this.note) = Transaction(
        id = id, type = type, amount = amount, accountId = accountId, toAccountId = toAccountId,
        categoryId = categoryId, date = date, note = note, merchant = merchant, forWhom = forWhom,
        labels = labels, createdAt = createdAt, recurringId = recurringId, splits = splits,
    )
}

/** `Omit<TransactionTemplate, 'id' | 'createdAt'>`. */
@Serializable
data class NewTemplate(
    val name: String,
    val type: TransactionType,
    val amount: Double,
    val accountId: String,
    val toAccountId: String? = null,
    val categoryId: String,
    val note: String,
    val labels: List<String> = emptyList(),
    val splits: List<TransactionSplit>? = null,
)

/** `Omit<Category, 'id'>`. */
@Serializable
data class NewCategory(val name: String, val icon: String, val color: String, val type: CategoryType)

/** `Omit<Label, 'id'>`. */
@Serializable
data class NewLabel(val name: String, val color: String)

/** `Omit<Budget, 'id' | 'createdAt'>`. */
@Serializable
data class NewBudget(
    val categoryId: String,
    val labelId: String? = null,
    val amount: Double,
    val period: BudgetPeriod,
    val rollover: Boolean,
)

/** `Omit<RecurringTransaction, 'id' | 'createdAt' | 'occurrenceCount' | 'lastRunDate'> & { lastRunDate? }`. */
@Serializable
data class NewRecurring(
    val type: TransactionType,
    val amount: Double,
    val accountId: String,
    val toAccountId: String? = null,
    val categoryId: String,
    val note: String,
    val labels: List<String> = emptyList(),
    val frequency: RecurrenceFrequency,
    val startDate: String,
    val endDate: String? = null,
    val maxOccurrences: Int? = null,
    val pausedAt: String? = null,
    /** Start the schedule mid-stream: occurrences on or before this date are never generated. */
    val lastRunDate: String? = null,
    val goalId: String? = null,
)

/** `Omit<CategoryRule, 'id' | 'createdAt'>`. */
@Serializable
data class NewRule(
    val pattern: String,
    val matchType: RuleMatchType,
    val scope: RuleScope,
    val categoryId: String,
    val labelIds: List<String> = emptyList(),
    val enabled: Boolean,
)

/** `Omit<Goal, 'id' | 'createdAt'>`. */
@Serializable
data class NewGoal(
    val name: String,
    val icon: String,
    val color: String,
    val targetAmount: Double,
    val targetDate: String? = null,
    val linkedAccountId: String? = null,
)

/** `Omit<GoalContribution, 'id' | 'createdAt'>`. Negative amount = withdrawal. */
@Serializable
data class NewContribution(val goalId: String, val amount: Double, val date: String, val note: String)

/** `Omit<Person, 'id' | 'createdAt'>`. */
@Serializable
data class NewPerson(val name: String, val icon: String, val color: String)

/** `Omit<DebtEntry, 'id' | 'createdAt'>`. Positive amount = they owe you. */
@Serializable
data class NewDebtEntry(
    val personId: String,
    val amount: Double,
    val date: String,
    val note: String,
    val settledTransactionId: String? = null,
)

/** `Omit<Loan, 'id' | 'createdAt' | 'recurringId'>`. */
@Serializable
data class NewLoan(
    val name: String,
    val principal: Double,
    val interestRate: Double,
    val tenureMonths: Int,
    val startDate: String,
    val accountId: String,
    val categoryId: String,
    val closedAt: String? = null,
)

/** `Omit<LoanPrepayment, 'id' | 'createdAt' | 'transactionId'>`. */
@Serializable
data class NewLoanPrepayment(val loanId: String, val amount: Double, val date: String, val note: String)

/** The TS `NewDeposit`. [terms] never carries `recurringId`/`maturedAt`. */
@Serializable
data class NewDeposit(
    /** [AccountType.Fd] or [AccountType.Rd]. */
    val type: AccountType,
    val name: String,
    val color: String,
    val terms: DepositTerms,
    /** Start date in the past: post the FD funding / past RD installments (true), or treat them as already in the deposit (false). */
    val deductPast: Boolean = false,
)

/** `updateDeposit` updates — null = leave unchanged. Compounding and maturity apply to FDs only. */
@Serializable
data class DepositUpdate(
    val name: String? = null,
    val color: String? = null,
    val interestRate: Double? = null,
    val compounding: DepositCompounding? = null,
    val maturityDate: String? = null,
)

/** `updateDebtEntry` updates — null = leave unchanged. */
@Serializable
data class DebtEntryUpdate(val amount: Double? = null, val date: String? = null, val note: String? = null)

enum class MoveDirection { Up, Down }

data class ApplyRulesResult(val changed: Int, val previous: List<TransactionCategorization>)

/** What [FinanceStore.settleUp] created. Undo is `deleteTransaction(transactionId)` — it removes both. */
data class SettleUpResult(val transactionId: String, val entryId: String)

/**
 * The finance store. Every public method is one atomic step: it reads the current state, computes
 * the next one and publishes it under a lock, so no observer ever sees a half-applied action.
 *
 * [clock] and [idGen] are the TS `new Date()` and `crypto.randomUUID()`; tests inject
 * deterministic ones. Ids are minted in the same order as the TS store mints them.
 */
class FinanceStore(
    initial: FinanceState = initialFinanceState(),
    private val clock: () -> Instant = ::nowInstant,
    private val idGen: () -> String = ::newId,
) {
    private val lock = Any()
    private val _state = MutableStateFlow(initial)
    private val _isHydrated = MutableStateFlow(false)

    val state: StateFlow<FinanceState> = _state.asStateFlow()
    val isHydrated: StateFlow<Boolean> = _isHydrated.asStateFlow()

    /** Snapshot of the current state (Zustand's `getState()`). */
    val current: FinanceState get() = _state.value

    /**
     * Transactions removed alongside a deleted "Settle up" debt entry, keyed by entry id, so
     * `restoreDebtEntry` (the undo) can put both back. In-memory only.
     */
    private val removedSettlementTransactions = HashMap<String, Transaction>()

    /** The reverse: settled entries removed because their transaction was deleted, keyed by transaction id. */
    private val removedSettlementEntries = HashMap<String, DebtEntry>()

    private inline fun <R> atomically(block: (FinanceState) -> R): R = synchronized(lock) { block(_state.value) }

    private fun publish(next: FinanceState) {
        _state.value = next
    }

    // ---- Hydration / plumbing ---------------------------------------------------------------

    fun setHydrated(hydrated: Boolean) {
        _isHydrated.value = hydrated
    }

    /** Replace the whole state — e.g. with the result of `decodePersisted` on app start. */
    fun replaceState(next: FinanceState) = atomically { publish(next) }

    /** Zustand's `setState(fn)` — a raw state transform with no domain rules. Tests and hydration only. */
    fun setState(transform: (FinanceState) -> FinanceState) = atomically { publish(transform(it)) }

    fun setLastLocalBackupAt(date: String?) = atomically { publish(it.copy(lastLocalBackupAt = date)) }

    // ---- Accounts ---------------------------------------------------------------------------

    fun addAccount(account: NewAccount): String = atomically { s ->
        val created = Account(
            id = idGen(),
            name = cleanText(account.name, MAX_NAME_LENGTH),
            type = account.type,
            color = account.color,
            icon = account.icon,
            balance = account.balance,
            // A brand-new account has no transactions, so the balance typed *is* the opening balance.
            openingBalance = account.balance,
            createdAt = clock().toIso(),
            creditLimit = account.creditLimit,
            statementCloseDay = account.statementCloseDay,
            paymentDueDays = account.paymentDueDays,
            minimumDuePercent = account.minimumDuePercent,
            archivedAt = account.archivedAt,
            deposit = account.deposit,
        )
        publish(s.copy(accounts = s.accounts + created))
        created.id
    }

    /**
     * Editing the balance is a statement about the *current* balance: when [updates] changes
     * `balance` but leaves `openingBalance` alone, the opening balance shifts by the same amount
     * so `balance === openingBalance + Σ deltas` stays true. Set `openingBalance` explicitly
     * in the transform to override that.
     */
    fun updateAccount(id: String, updates: (Account) -> Account) = atomically { s ->
        val target = s.accounts.find { it.id == id } ?: return@atomically
        var next = updates(target).copy(id = target.id)
        if (next.balance != target.balance && next.openingBalance == target.openingBalance) {
            val delta = sumTransactionDeltas(s.transactions.balanceTxs())[id] ?: 0.0
            next = next.copy(openingBalance = roundMoney(next.balance - delta))
        }
        publish(s.copy(accounts = s.accounts.map { if (it.id == id) next else it }))
    }

    /** Close or reopen an account — non-destructive, unlike [deleteAccount]. */
    fun setAccountArchived(id: String, archived: Boolean) = atomically { s ->
        val now = clock().toIso()
        publish(
            s.copy(
                accounts = s.accounts.map { a ->
                    when {
                        a.id != id -> a
                        archived -> a.copy(archivedAt = now)
                        else -> a.copy(archivedAt = null)
                    }
                },
            ),
        )
    }

    /** Refuses (returns false) while an open deposit pays out into this account. */
    fun deleteAccount(id: String): Boolean = atomically { s ->
        if (accountDeleteBlockers(s.accounts, id).isNotEmpty()) return@atomically false
        val removed = s.transactions.filter { it.accountId == id || it.toAccountId == id }
        // Reverse each removed transaction first, or the other side of a transfer stays inflated.
        var accounts = s.accounts
        for (tx in removed) accounts = applyBalanceDelta(accounts, tx, -1)
        val orphanedLoanIds = s.loans.filter { it.accountId == id }.map { it.id }.toSet()
        publish(
            s.copy(
                accounts = accounts.filter { it.id != id },
                transactions = s.transactions.filter { it.accountId != id && it.toAccountId != id },
                recurring = s.recurring.filter { it.accountId != id && it.toAccountId != id },
                loans = s.loans.filter { it.accountId != id },
                loanPrepayments = s.loanPrepayments.filter { it.loanId !in orphanedLoanIds },
                goals = s.goals.map { if (it.linkedAccountId == id) it.copy(linkedAccountId = null) else it },
            ),
        )
        true
    }

    // ---- Deposits ---------------------------------------------------------------------------

    /** Creates a deposit account plus its funding transfer (FD) or installment rule (RD). Returns the account id. */
    fun addDeposit(input: NewDeposit): String = atomically { s ->
        val now = clock()
        val createdAt = now.toIso()
        val accountId = idGen()
        val terms = input.terms

        if (input.type == AccountType.Fd) {
            val startedInPast = parseJsDate(terms.startDate)?.isBefore(startOfDay(now)) ?: false
            val alreadyFunded = startedInPast && !input.deductPast
            val account = Account(
                id = accountId,
                name = input.name,
                type = input.type,
                color = input.color,
                icon = "vault",
                balance = if (alreadyFunded) terms.amount else 0.0,
                openingBalance = if (alreadyFunded) terms.amount else 0.0,
                createdAt = createdAt,
                deposit = terms.copy(compounding = terms.compounding ?: DepositCompounding.Quarterly),
            )
            if (alreadyFunded) {
                publish(s.copy(accounts = s.accounts + account))
                return@atomically accountId
            }
            val funding = Transaction(
                id = idGen(),
                type = TransactionType.Transfer,
                amount = terms.amount,
                accountId = terms.linkedAccountId,
                toAccountId = accountId,
                categoryId = transferCategoryId(s.categories),
                date = terms.startDate,
                note = "Fixed deposit — ${input.name}",
                labels = emptyList(),
                createdAt = createdAt,
            )
            publish(
                s.copy(
                    accounts = applyBalanceDelta(s.accounts + account, funding, 1),
                    transactions = listOf(funding) + s.transactions,
                ),
            )
            return@atomically accountId
        }

        var recurring = RecurringTransaction(
            id = idGen(),
            type = TransactionType.Transfer,
            amount = terms.amount,
            accountId = terms.linkedAccountId,
            toAccountId = accountId,
            categoryId = transferCategoryId(s.categories),
            note = "RD installment — ${input.name}",
            labels = emptyList(),
            frequency = RecurrenceFrequency.Monthly,
            startDate = terms.startDate,
            maxOccurrences = terms.tenureMonths,
            occurrenceCount = 0,
            lastRunDate = null,
            createdAt = createdAt,
        )
        // Installments already paid before the RD was entered: either let processRecurring post
        // them, or treat them as money already sitting in the RD.
        var openingBalance = 0.0
        if (!input.deductPast) {
            val paid = rdInstallmentsOnOrBefore(terms, now)
            if (paid > 0) {
                openingBalance = roundMoney(terms.amount * paid)
                recurring = recurring.copy(
                    occurrenceCount = paid,
                    lastRunDate = lastOccurrenceOnOrBefore(recurring, now)?.toIso(),
                )
            }
        }
        val account = Account(
            id = accountId,
            name = input.name,
            type = input.type,
            color = input.color,
            icon = "calendar-clock",
            balance = openingBalance,
            openingBalance = openingBalance,
            createdAt = createdAt,
            deposit = terms.copy(recurringId = recurring.id),
        )
        publish(s.copy(accounts = s.accounts + account, recurring = s.recurring + recurring))
        accountId
    }

    /** Only fields that don't rewrite posted history: name, color, rate, and an FD's compounding/maturity. */
    fun updateDeposit(id: String, updates: DepositUpdate) = atomically { s ->
        val target = s.accounts.find { it.id == id } ?: return@atomically
        val deposit = target.deposit ?: return@atomically
        val isFd = target.type == AccountType.Fd
        val next = target.copy(
            name = updates.name ?: target.name,
            color = updates.color ?: target.color,
            deposit = deposit.copy(
                interestRate = updates.interestRate ?: deposit.interestRate,
                compounding = if (isFd && updates.compounding != null) updates.compounding else deposit.compounding,
                maturityDate = if (isFd && !updates.maturityDate.isNullOrEmpty()) updates.maturityDate else deposit.maturityDate,
            ),
        )
        val ruleId = deposit.recurringId
        publish(
            s.copy(
                accounts = s.accounts.map { if (it.id == id) next else it },
                // Keep the installment rule's note in step with a rename.
                recurring = if (!ruleId.isNullOrEmpty() && updates.name != null) {
                    s.recurring.map { if (it.id == ruleId) it.copy(note = "RD installment — ${updates.name}") else it }
                } else s.recurring,
            ),
        )
    }

    /** Pay out every matured deposit (interest income, transfer to the linked account, archive). Runs once each. */
    fun processMaturities(): List<Transaction> = atomically { s ->
        val due = planMaturities(s.accounts, clock())
        if (due.isEmpty()) return@atomically emptyList()

        val createdAt = clock().toIso()
        val interestCat = interestCategoryId(s.categories)
        val transferCat = transferCategoryId(s.categories)
        val posted = mutableListOf<Transaction>()
        val matured = LinkedHashMap<String, Account>()
        val pausedRuleIds = HashSet<String>()

        for (account in due) {
            val terms = account.deposit!!
            val maturedAt = depositMaturityDate(account)!!.toIso()
            val interest = roundMoney(depositMaturityAmount(account) - depositInvested(account))
            if (interest > 0) {
                posted += Transaction(
                    id = idGen(), type = TransactionType.Income, amount = interest, accountId = account.id,
                    categoryId = interestCat, date = maturedAt, note = "Interest — ${account.name}",
                    labels = emptyList(), createdAt = createdAt,
                )
            }
            // Pay out whatever the deposit holds once interest lands.
            val payout = roundMoney(account.balance + max(0.0, interest))
            if (payout > 0) {
                posted += Transaction(
                    id = idGen(), type = TransactionType.Transfer, amount = payout, accountId = account.id,
                    toAccountId = terms.linkedAccountId, categoryId = transferCat, date = maturedAt,
                    note = "Maturity — ${account.name}", labels = emptyList(), createdAt = createdAt,
                )
            }
            terms.recurringId?.takeIf { it.isNotEmpty() }?.let { pausedRuleIds += it }
            matured[account.id] = account.copy(archivedAt = maturedAt, deposit = terms.copy(maturedAt = maturedAt))
        }

        var accounts = s.accounts.map { a -> matured[a.id]?.copy(balance = a.balance) ?: a }
        for (tx in posted) accounts = applyBalanceDelta(accounts, tx, 1)
        publish(
            s.copy(
                accounts = accounts,
                transactions = posted + s.transactions,
                recurring = s.recurring.map {
                    if (it.id in pausedRuleIds && it.pausedAt.isNullOrEmpty()) it.copy(pausedAt = createdAt) else it
                },
            ),
        )
        posted.toList()
    }

    /** Rebuild every balance from opening balance + transactions. Reports what moved. */
    fun recomputeBalances(): BalanceDiff = atomically { s ->
        val after = recomputeAccountBalances(s.accounts, s.transactions.balanceTxs())
        val result = diffBalances(s.accounts, after)
        if (result.changed > 0) publish(s.copy(accounts = after))
        result
    }

    // ---- Transactions -----------------------------------------------------------------------

    fun addTransaction(tx: NewTransaction): String = atomically { s ->
        val transaction = tx.toTransaction(idGen(), clock().toIso(), note = cleanText(tx.note, MAX_NOTE_LENGTH))
        publish(
            s.copy(
                transactions = listOf(transaction) + s.transactions,
                accounts = applyBalanceDelta(s.accounts, transaction, 1),
            ),
        )
        transaction.id
    }

    /** Swap one transaction for an edited copy, reversing the original's delta and applying the edit's. */
    private fun replaceTransaction(s: FinanceState, original: Transaction, updated: Transaction): FinanceState {
        val afterReverse = applyBalanceDelta(s.accounts, original, -1)
        return s.copy(
            transactions = s.transactions.map { if (it.id == original.id) updated else it },
            accounts = applyBalanceDelta(afterReverse, updated, 1),
        )
    }

    /**
     * Balance-safe edit. A settlement transaction's amount/date/type change carries over to its
     * "Settled up" entry in the same step (income → entry < 0, expense → entry > 0).
     */
    fun updateTransaction(id: String, updates: (Transaction) -> Transaction) = atomically { s ->
        val original = s.transactions.find { it.id == id } ?: return@atomically
        val updated = updates(original).copy(id = original.id)
        val synced = updated.amount != original.amount || updated.date != original.date || updated.type != original.type
        val linked = synced && s.debtEntries.any { it.settledTransactionId == id }
        var next = replaceTransaction(s, original, updated)
        if (linked) {
            next = next.copy(
                debtEntries = s.debtEntries.map { e ->
                    if (e.settledTransactionId != id) return@map e
                    val direction = when (updated.type) {
                        TransactionType.Income -> -1.0
                        TransactionType.Expense -> 1.0
                        else -> if (e.amount != 0.0 && !e.amount.isNaN()) sign(e.amount) else 1.0
                    }
                    e.copy(amount = direction * abs(updated.amount), date = updated.date)
                },
            )
        }
        publish(next)
    }

    /** Removes the transaction (and any settled entry linked to it) and reverses its delta. Returns it for undo. */
    fun deleteTransaction(id: String): Transaction? = atomically { s ->
        val tx = s.transactions.find { it.id == id } ?: return@atomically null
        val settled = s.debtEntries.filter { it.settledTransactionId == id }
        for (e in settled) removedSettlementEntries[id] = e
        publish(
            s.copy(
                transactions = s.transactions.filter { it.id != id },
                accounts = applyBalanceDelta(s.accounts, tx, -1),
                debtEntries = if (settled.isNotEmpty()) s.debtEntries.filter { it.settledTransactionId != id } else s.debtEntries,
            ),
        )
        tx
    }

    private fun restoreSettlementEntries(entries: List<DebtEntry>, transactionIds: List<String>): List<DebtEntry> {
        val back = mutableListOf<DebtEntry>()
        for (id in transactionIds) {
            val entry = removedSettlementEntries.remove(id)
            if (entry != null && entries.none { it.id == entry.id }) back += entry
        }
        return if (back.isNotEmpty()) back + entries else entries
    }

    /** Re-insert a deleted transaction verbatim (same id, same delta). A repeat is a no-op. */
    fun restoreTransaction(transaction: Transaction) = atomically { s ->
        if (s.transactions.any { it.id == transaction.id }) return@atomically
        publish(
            s.copy(
                transactions = listOf(transaction) + s.transactions,
                accounts = applyBalanceDelta(s.accounts, transaction, 1),
                debtEntries = restoreSettlementEntries(s.debtEntries, listOf(transaction.id)),
            ),
        )
    }

    fun bulkDeleteTransactions(ids: Collection<String>): List<Transaction> = atomically { s ->
        val idSet = ids.toSet()
        val removed = s.transactions.filter { it.id in idSet }
        if (removed.isEmpty()) return@atomically emptyList()
        var accounts = s.accounts
        for (tx in removed) accounts = applyBalanceDelta(accounts, tx, -1)
        for (e in s.debtEntries) {
            val txId = e.settledTransactionId
            if (!txId.isNullOrEmpty() && txId in idSet) removedSettlementEntries[txId] = e
        }
        publish(
            s.copy(
                transactions = s.transactions.filter { it.id !in idSet },
                accounts = accounts,
                debtEntries = s.debtEntries.filter { e ->
                    !(!e.settledTransactionId.isNullOrEmpty() && e.settledTransactionId in idSet)
                },
            ),
        )
        removed
    }

    fun restoreTransactions(transactions: List<Transaction>) = atomically { s ->
        val existingIds = s.transactions.map { it.id }.toSet()
        val toRestore = transactions.filter { it.id !in existingIds }
        if (toRestore.isEmpty()) return@atomically
        var accounts = s.accounts
        for (tx in toRestore) accounts = applyBalanceDelta(accounts, tx, 1)
        publish(
            s.copy(
                transactions = toRestore + s.transactions,
                accounts = accounts,
                debtEntries = restoreSettlementEntries(s.debtEntries, toRestore.map { it.id }),
            ),
        )
    }

    /**
     * Move every listed transaction to one category. Transfers and rows the category isn't valid
     * for are skipped; a split row is flattened. Returns how many rows changed.
     */
    fun bulkRecategorize(ids: Collection<String>, categoryId: String): Int = atomically { s ->
        val idSet = ids.toSet()
        val category = s.categories.find { it.id == categoryId } ?: return@atomically 0
        var changed = 0
        val transactions = s.transactions.map { t ->
            if (t.id !in idSet || t.type == TransactionType.Transfer) return@map t
            if (!isCategoryValidForType(category, t.type)) return@map t
            if (t.categoryId == categoryId && t.splits == null) return@map t
            changed += 1
            t.copy(categoryId = categoryId, splits = null)
        }
        publish(s.copy(transactions = transactions))
        changed
    }

    fun bulkAddLabel(ids: Collection<String>, labelId: String) = atomically { s ->
        val idSet = ids.toSet()
        publish(
            s.copy(
                transactions = s.transactions.map { t ->
                    if (t.id in idSet && labelId !in t.labels) t.copy(labels = t.labels + labelId) else t
                },
            ),
        )
    }

    /** Insert many transactions at once (CSV import, sample data). Notes are taken as-is. */
    fun bulkAddTransactions(rows: List<NewTransaction>): Int = atomically { s ->
        if (rows.isEmpty()) return@atomically 0
        val createdAt = clock().toIso()
        val newTxns = rows.map { it.toTransaction(idGen(), createdAt) }
        var accounts = s.accounts
        for (tx in newTxns) accounts = applyBalanceDelta(accounts, tx, 1)
        publish(s.copy(transactions = newTxns + s.transactions, accounts = accounts))
        newTxns.size
    }

    // ---- Categories & labels ----------------------------------------------------------------

    fun addCategory(category: NewCategory) = atomically { s ->
        val created = Category(idGen(), category.name, category.icon, category.color, category.type)
        publish(s.copy(categories = s.categories + created))
    }

    fun updateCategory(id: String, updates: (Category) -> Category) = atomically { s ->
        publish(s.copy(categories = s.categories.map { if (it.id == id) updates(it).copy(id = id) else it }))
    }

    /**
     * Transfer and Miscellaneous are permanent. Everything pointing at the deleted category —
     * transactions (splits included), recurring rules, loans, rules — moves to the catch-all;
     * its budgets are dropped.
     */
    fun deleteCategory(id: String) = atomically { s ->
        if (isProtectedCategory(id)) return@atomically
        val remaining = s.categories.filter { it.id != id }
        val fallbackId = remaining.find { it.id == MISC_CATEGORY_ID }?.id
            ?: remaining.find { it.type == CategoryType.Both && it.id != TRANSFER_CATEGORY_ID }?.id
            ?: MISC_CATEGORY_ID
        publish(
            s.copy(
                categories = remaining,
                budgets = s.budgets.filter { it.categoryId != id },
                transactions = s.transactions.map { reassignTransactionCategory(it, id, fallbackId) },
                recurring = s.recurring.map { if (it.categoryId == id) it.copy(categoryId = fallbackId) else it },
                loans = s.loans.map { if (it.categoryId == id) it.copy(categoryId = fallbackId) else it },
                rules = s.rules.map { if (it.categoryId == id) it.copy(categoryId = fallbackId) else it },
            ),
        )
    }

    fun addLabel(label: NewLabel) = atomically { s ->
        publish(s.copy(labels = s.labels + Label(idGen(), label.name, label.color)))
    }

    fun updateLabel(id: String, updates: (Label) -> Label) = atomically { s ->
        publish(s.copy(labels = s.labels.map { if (it.id == id) updates(it).copy(id = id) else it }))
    }

    /** Strips the label from transactions and rules, and drops budgets scoped to it. */
    fun deleteLabel(id: String) = atomically { s ->
        val anyHasLabel = s.transactions.any { id in it.labels }
        publish(
            s.copy(
                labels = s.labels.filter { it.id != id },
                transactions = if (anyHasLabel) {
                    s.transactions.map { t -> if (id in t.labels) t.copy(labels = t.labels.filter { it != id }) else t }
                } else s.transactions,
                rules = s.rules.map { r -> if (id in r.labelIds) r.copy(labelIds = r.labelIds.filter { it != id }) else r },
                budgets = s.budgets.filter { it.labelId != id },
            ),
        )
    }

    // ---- Budgets ----------------------------------------------------------------------------

    /** Adding a budget for a scope that already has one replaces it — one limit per scope. */
    fun addBudget(budget: NewBudget) = atomically { s ->
        val created = Budget(
            id = idGen(), categoryId = budget.categoryId, labelId = budget.labelId, amount = budget.amount,
            period = budget.period, rollover = budget.rollover, createdAt = clock().toIso(),
        )
        val scope = budgetScopeKey(created)
        publish(s.copy(budgets = s.budgets.filter { budgetScopeKey(it) != scope } + created))
    }

    fun updateBudget(id: String, updates: (Budget) -> Budget) = atomically { s ->
        publish(s.copy(budgets = s.budgets.map { if (it.id == id) updates(it).copy(id = id) else it }))
    }

    fun deleteBudget(id: String) = atomically { s -> publish(s.copy(budgets = s.budgets.filter { it.id != id })) }

    // ---- Recurring --------------------------------------------------------------------------

    fun addRecurring(rule: NewRecurring): String = atomically { s ->
        val created = RecurringTransaction(
            id = idGen(), type = rule.type, amount = rule.amount, accountId = rule.accountId,
            toAccountId = rule.toAccountId, categoryId = rule.categoryId, note = rule.note, labels = rule.labels,
            frequency = rule.frequency, startDate = rule.startDate, endDate = rule.endDate,
            maxOccurrences = rule.maxOccurrences, occurrenceCount = 0, pausedAt = rule.pausedAt,
            lastRunDate = rule.lastRunDate, createdAt = clock().toIso(), goalId = rule.goalId,
        )
        publish(s.copy(recurring = s.recurring + created))
        created.id
    }

    fun updateRecurring(id: String, updates: (RecurringTransaction) -> RecurringTransaction) = atomically { s ->
        publish(s.copy(recurring = s.recurring.map { if (it.id == id) updates(it).copy(id = id) else it }))
    }

    fun setRecurringPaused(id: String, paused: Boolean) = atomically { s ->
        val now = clock().toIso()
        publish(s.copy(recurring = s.recurring.map { if (it.id == id) it.copy(pausedAt = if (paused) now else null) else it }))
    }

    fun deleteRecurring(id: String) = atomically { s -> publish(s.copy(recurring = s.recurring.filter { it.id != id })) }

    /**
     * Generate every due recurring occurrence. Goal-linked rules also post a matching
     * contribution (skipped if the goal no longer exists). Returns the generated rows for undo.
     */
    fun processRecurring(): List<Transaction> = atomically { s ->
        val now = clock()
        val plan = planRecurring(s.recurring, s.accounts.map { it.id }, now)
        if (plan.occurrences.isEmpty()) return@atomically emptyList()

        val createdAt = now.toIso()
        val newTxns = plan.occurrences.map { (rule, date) ->
            Transaction(
                id = idGen(), type = rule.type, amount = rule.amount, accountId = rule.accountId,
                toAccountId = if (rule.type == TransactionType.Transfer && !rule.toAccountId.isNullOrEmpty()) rule.toAccountId else null,
                categoryId = rule.categoryId, date = date.toIso(), note = rule.note, labels = rule.labels.toList(),
                createdAt = createdAt, recurringId = rule.id,
            )
        }
        var accounts = s.accounts
        for (tx in newTxns) accounts = applyBalanceDelta(accounts, tx, 1)

        val goalIds = s.goals.map { it.id }.toSet()
        val newContributions = plan.occurrences
            .filter { (rule) -> !rule.goalId.isNullOrEmpty() && rule.goalId in goalIds }
            .map { (rule, date) ->
                GoalContribution(
                    id = idGen(), goalId = rule.goalId!!, amount = rule.amount, date = date.toIso(),
                    note = rule.note, createdAt = createdAt,
                )
            }

        publish(
            s.copy(
                transactions = newTxns + s.transactions,
                accounts = accounts,
                recurring = plan.rules,
                goalContributions = if (newContributions.isNotEmpty()) newContributions + s.goalContributions else s.goalContributions,
            ),
        )
        newTxns
    }

    // ---- Templates --------------------------------------------------------------------------

    fun addTemplate(template: NewTemplate): String = atomically { s ->
        val created = TransactionTemplate(
            id = idGen(), name = template.name, type = template.type, amount = template.amount,
            accountId = template.accountId, toAccountId = template.toAccountId, categoryId = template.categoryId,
            note = template.note, labels = template.labels, createdAt = clock().toIso(), splits = template.splits,
        )
        publish(s.copy(templates = s.templates + created))
        created.id
    }

    fun deleteTemplate(id: String) = atomically { s -> publish(s.copy(templates = s.templates.filter { it.id != id })) }

    // ---- Categorization rules ---------------------------------------------------------------

    /** Appended, not prepended: a new rule must not silently outrank every existing one. */
    fun addRule(rule: NewRule): String = atomically { s ->
        val created = CategoryRule(
            id = idGen(), pattern = rule.pattern, matchType = rule.matchType, scope = rule.scope,
            categoryId = rule.categoryId, labelIds = rule.labelIds, enabled = rule.enabled, createdAt = clock().toIso(),
        )
        publish(s.copy(rules = s.rules + created))
        created.id
    }

    fun updateRule(id: String, updates: (CategoryRule) -> CategoryRule) = atomically { s ->
        publish(s.copy(rules = s.rules.map { if (it.id == id) updates(it).copy(id = id) else it }))
    }

    fun deleteRule(id: String) = atomically { s -> publish(s.copy(rules = s.rules.filter { it.id != id })) }

    /** Swap a rule with its neighbour; a no-op at either end. */
    fun moveRule(id: String, direction: MoveDirection) = atomically { s ->
        val index = s.rules.indexOfFirst { it.id == id }
        if (index == -1) return@atomically
        val target = if (direction == MoveDirection.Up) index - 1 else index + 1
        if (target < 0 || target >= s.rules.size) return@atomically
        val rules = s.rules.toMutableList()
        rules[index] = s.rules[target]
        rules[target] = s.rules[index]
        publish(s.copy(rules = rules))
    }

    /** Replay the rules over existing transactions. Returns the count and the prior rows for undo. */
    fun applyRulesToExisting(restrictToCategoryId: String? = null): ApplyRulesResult = atomically { s ->
        val applications = planRuleApplication(s.transactions, s.rules, ReplayOptions(restrictToCategoryId))
        if (applications.isEmpty()) return@atomically ApplyRulesResult(0, emptyList())
        val byId = applications.associate { it.transactionId to it.after }
        publish(
            s.copy(
                transactions = s.transactions.map { t ->
                    val after = byId[t.id] ?: return@map t
                    t.copy(categoryId = after.categoryId, labels = after.labels)
                },
            ),
        )
        ApplyRulesResult(applications.size, applications.map { it.before })
    }

    /** Undo for a bulk categorization change: restores category, labels and splits by id. */
    fun restoreCategorization(rows: List<TransactionCategorization>) = atomically { s ->
        if (rows.isEmpty()) return@atomically
        val byId = rows.associateBy { it.id }
        publish(
            s.copy(
                transactions = s.transactions.map { t ->
                    val row = byId[t.id] ?: return@map t
                    t.copy(categoryId = row.categoryId, labels = row.labels, splits = row.splits)
                },
            ),
        )
    }

    // ---- Goals ------------------------------------------------------------------------------

    fun addGoal(goal: NewGoal): String = atomically { s ->
        val created = Goal(
            id = idGen(), name = cleanText(goal.name, MAX_NAME_LENGTH), icon = goal.icon, color = goal.color,
            targetAmount = goal.targetAmount, targetDate = goal.targetDate, linkedAccountId = goal.linkedAccountId,
            createdAt = clock().toIso(),
        )
        publish(s.copy(goals = s.goals + created))
        created.id
    }

    fun updateGoal(id: String, updates: (Goal) -> Goal) = atomically { s ->
        publish(s.copy(goals = s.goals.map { if (it.id == id) updates(it).copy(id = id) else it }))
    }

    /** Removes the goal and its contributions; recurring rules funding it just lose the link. */
    fun deleteGoal(id: String) = atomically { s ->
        publish(
            s.copy(
                goals = s.goals.filter { it.id != id },
                goalContributions = s.goalContributions.filter { it.goalId != id },
                recurring = s.recurring.map { if (it.goalId == id) it.copy(goalId = null) else it },
            ),
        )
    }

    /** A withdrawal is clamped to what the goal holds. */
    fun addContribution(contribution: NewContribution): String = atomically { s ->
        var amount = contribution.amount
        if (amount < 0) {
            val saved = s.goalContributions.filter { it.goalId == contribution.goalId }.fold(0.0) { sum, c -> sum + c.amount }
            amount = max(amount, -max(0.0, saved))
        }
        val created = GoalContribution(
            id = idGen(), goalId = contribution.goalId, amount = amount, date = contribution.date,
            note = contribution.note, createdAt = clock().toIso(),
        )
        publish(s.copy(goalContributions = listOf(created) + s.goalContributions))
        created.id
    }

    fun deleteContribution(id: String): GoalContribution? = atomically { s ->
        val contribution = s.goalContributions.find { it.id == id } ?: return@atomically null
        publish(s.copy(goalContributions = s.goalContributions.filter { it.id != id }))
        contribution
    }

    fun restoreContribution(contribution: GoalContribution) = atomically { s ->
        if (s.goalContributions.any { it.id == contribution.id }) return@atomically
        publish(s.copy(goalContributions = listOf(contribution) + s.goalContributions))
    }

    // ---- People & debts ---------------------------------------------------------------------

    fun addPerson(person: NewPerson): String = atomically { s ->
        val created = Person(idGen(), person.name, person.icon, person.color, clock().toIso())
        publish(s.copy(people = s.people + created))
        created.id
    }

    fun updatePerson(id: String, updates: (Person) -> Person) = atomically { s ->
        publish(s.copy(people = s.people.map { if (it.id == id) updates(it).copy(id = id) else it }))
    }

    /** Removes the person and every debt entry logged against them. */
    fun deletePerson(id: String) = atomically { s ->
        publish(s.copy(people = s.people.filter { it.id != id }, debtEntries = s.debtEntries.filter { it.personId != id }))
    }

    fun addDebtEntry(entry: NewDebtEntry): String = atomically { s ->
        val created = DebtEntry(
            id = idGen(), personId = entry.personId, amount = entry.amount, date = entry.date, note = entry.note,
            settledTransactionId = entry.settledTransactionId, createdAt = clock().toIso(),
        )
        publish(s.copy(debtEntries = listOf(created) + s.debtEntries))
        created.id
    }

    /**
     * "Settle up" (web: Debts.tsx `handleSettleSubmit`, made one atomic action here). They owe
     * you (balance > 0) → an income into [accountId]; you owe them → an expense out of it. Creates
     * the real transaction and a balancing "Settled up" entry stamped with its id.
     *
     * Returns null — and changes nothing — when the person is unknown, nothing is outstanding, the
     * amount isn't positive or exceeds what is outstanding (by more than half a paisa), or
     * [accountId] is blank. Undo is `deleteTransaction(result.transactionId)`, which removes both.
     */
    fun settleUp(personId: String, amount: Double, accountId: String, note: String = ""): SettleUpResult? = atomically { s ->
        val person = s.people.find { it.id == personId } ?: return@atomically null
        val balance = s.debtEntries.filter { it.personId == personId }.fold(0.0) { sum, e -> sum + e.amount }
        if (amount.isNaN() || amount <= 0) return@atomically null
        if (amount > abs(balance) + 0.005) return@atomically null
        if (accountId.isEmpty()) return@atomically null

        val type = if (balance > 0) TransactionType.Income else TransactionType.Expense
        val entryNote = cleanText(note, MAX_NOTE_LENGTH).ifEmpty { "Settled up with ${person.name}" }
        val now = clock().toIso()

        val transaction = Transaction(
            id = idGen(), type = type, amount = amount, accountId = accountId, categoryId = MISC_CATEGORY_ID,
            date = now, note = cleanText(entryNote, MAX_NOTE_LENGTH), labels = emptyList(), createdAt = now,
        )
        val entry = DebtEntry(
            id = idGen(), personId = personId, amount = if (balance > 0) -amount else amount, date = now,
            note = entryNote, settledTransactionId = transaction.id, createdAt = now,
        )
        publish(
            s.copy(
                transactions = listOf(transaction) + s.transactions,
                accounts = applyBalanceDelta(s.accounts, transaction, 1),
                debtEntries = listOf(entry) + s.debtEntries,
            ),
        )
        SettleUpResult(transaction.id, entry.id)
    }

    /**
     * Removes the entry. A "Settle up" entry takes its transaction with it (balance reversed),
     * stashed so [restoreDebtEntry] brings both back. Returns the removed entry for undo.
     */
    fun deleteDebtEntry(id: String): DebtEntry? = atomically { s ->
        val entry = s.debtEntries.find { it.id == id } ?: return@atomically null
        val linked = entry.settledTransactionId?.takeIf { it.isNotEmpty() }?.let { txId -> s.transactions.find { it.id == txId } }
        if (linked == null) {
            publish(s.copy(debtEntries = s.debtEntries.filter { it.id != id }))
        } else {
            removedSettlementTransactions[entry.id] = linked
            publish(
                s.copy(
                    debtEntries = s.debtEntries.filter { it.id != id },
                    transactions = s.transactions.filter { it.id != linked.id },
                    accounts = applyBalanceDelta(s.accounts, linked, -1),
                ),
            )
        }
        entry
    }

    /**
     * Edit an entry's amount (signed), date or note. A settled entry keeps its sign (only the
     * magnitude changes) and its transaction follows, balance-safely. A zero or non-finite amount
     * is ignored. Returns false for an unknown id.
     */
    fun updateDebtEntry(id: String, updates: DebtEntryUpdate): Boolean = atomically { s ->
        val entry = s.debtEntries.find { it.id == id } ?: return@atomically false
        var next = entry
        updates.note?.let { next = next.copy(note = cleanText(it, MAX_NOTE_LENGTH)) }
        updates.date?.takeIf { it.isNotEmpty() }?.let { next = next.copy(date = it) }
        val rawAmount = updates.amount
        if (rawAmount != null && rawAmount.isFinite() && roundMoney(rawAmount) != 0.0) {
            val amount = roundMoney(rawAmount)
            next = next.copy(
                amount = if (!entry.settledTransactionId.isNullOrEmpty()) {
                    (if (entry.amount != 0.0) sign(entry.amount) else 1.0) * abs(amount)
                } else amount,
            )
        }
        val linked = entry.settledTransactionId?.takeIf { it.isNotEmpty() }?.let { txId -> s.transactions.find { it.id == txId } }
        val entries = s.debtEntries.map { if (it.id == id) next else it }
        if (linked == null) {
            publish(s.copy(debtEntries = entries))
        } else {
            // The note stays on the entry: the transaction's note is the user's to edit on its own screen.
            val updatedTx = linked.copy(amount = abs(next.amount), date = next.date)
            publish(replaceTransaction(s, linked, updatedTx).copy(debtEntries = entries))
        }
        true
    }

    fun restoreDebtEntry(entry: DebtEntry) = atomically { s ->
        if (s.debtEntries.any { it.id == entry.id }) return@atomically
        val linked = removedSettlementTransactions.remove(entry.id)
        if (linked == null || s.transactions.any { it.id == linked.id }) {
            publish(s.copy(debtEntries = listOf(entry) + s.debtEntries))
        } else {
            publish(
                s.copy(
                    debtEntries = listOf(entry) + s.debtEntries,
                    transactions = listOf(linked) + s.transactions,
                    accounts = applyBalanceDelta(s.accounts, linked, 1),
                ),
            )
        }
    }

    // ---- Net worth --------------------------------------------------------------------------

    /** Freeze every completed financial month that has no snapshot yet. Returns how many were added. */
    fun captureNetWorthSnapshots(): Int = atomically { s ->
        val now = clock()
        val planned: List<PlannedSnapshot> = planNetWorthSnapshots(
            SnapshotPlanInput(
                accounts = s.accounts,
                transactions = s.transactions,
                snapshots = s.netWorthSnapshots,
                now = now,
                monthStartDay = normalizeMonthStartDay(s.settings.monthStartDay),
            ),
        )
        if (planned.isEmpty()) return@atomically 0
        val createdAt = now.toIso()
        val snapshots = planned.map {
            NetWorthSnapshot(idGen(), it.periodKey, it.date, it.assets, it.liabilities, createdAt)
        }
        publish(s.copy(netWorthSnapshots = dedupeSnapshotsByPeriod(s.netWorthSnapshots + snapshots)))
        snapshots.size
    }

    // ---- Loans ------------------------------------------------------------------------------

    /**
     * Also creates the recurring rule that posts the EMI monthly. EMIs already due are treated as
     * paid outside Finio (the rule is advanced past them) unless [logPastEmis], in which case
     * the caller runs [processRecurring] to post them.
     */
    fun addLoan(loan: NewLoan, logPastEmis: Boolean = false): String = atomically { s ->
        val now = clock()
        val createdAt = now.toIso()
        var recurring = RecurringTransaction(
            id = idGen(),
            type = TransactionType.Expense,
            amount = calculateEmi(loan.principal, loan.interestRate, loan.tenureMonths),
            accountId = loan.accountId,
            categoryId = loan.categoryId,
            note = "EMI — ${loan.name}",
            labels = emptyList(),
            frequency = RecurrenceFrequency.Monthly,
            startDate = loan.startDate,
            maxOccurrences = loan.tenureMonths,
            occurrenceCount = 0,
            lastRunDate = null,
            createdAt = createdAt,
        )
        if (!logPastEmis) {
            val past = previewBackfill(recurring, listOf(loan.accountId), now).count
            if (past > 0) {
                recurring = recurring.copy(occurrenceCount = past, lastRunDate = lastOccurrenceOnOrBefore(recurring, now)?.toIso())
            }
        }
        val created = Loan(
            id = idGen(), name = loan.name, principal = loan.principal, interestRate = loan.interestRate,
            tenureMonths = loan.tenureMonths, startDate = loan.startDate, accountId = loan.accountId,
            categoryId = loan.categoryId, recurringId = recurring.id, closedAt = loan.closedAt, createdAt = createdAt,
        )
        publish(s.copy(loans = s.loans + created, recurring = s.recurring + recurring))
        created.id
    }

    /** Keeps the linked EMI rule's amount, account, category, start, note and cap in step. */
    fun updateLoan(id: String, updates: (Loan) -> Loan) = atomically { s ->
        val target = s.loans.find { it.id == id } ?: return@atomically
        val next = updates(target).copy(id = target.id, recurringId = target.recurringId)
        val ruleId = next.recurringId
        val recurring = if (!ruleId.isNullOrEmpty()) {
            s.recurring.map { r ->
                if (r.id != ruleId) r else r.copy(
                    amount = calculateEmi(next.principal, next.interestRate, next.tenureMonths),
                    accountId = next.accountId,
                    categoryId = next.categoryId,
                    startDate = next.startDate,
                    note = "EMI — ${next.name}",
                    maxOccurrences = next.tenureMonths,
                )
            }
        } else s.recurring
        publish(s.copy(loans = s.loans.map { if (it.id == id) next else it }, recurring = recurring))
    }

    /** Marks the loan paid off (or reopens it) and pauses/resumes its EMI rule. */
    fun setLoanClosed(id: String, closed: Boolean) = atomically { s -> withLoanClosed(s, id, closed)?.let(::publish) }

    private fun withLoanClosed(s: FinanceState, id: String, closed: Boolean): FinanceState? {
        val target = s.loans.find { it.id == id } ?: return null
        val now = clock().toIso()
        val loans = s.loans.map { if (it.id != id) it else it.copy(closedAt = if (closed) now else null) }
        val ruleId = target.recurringId
        val recurring = if (!ruleId.isNullOrEmpty()) {
            s.recurring.map { if (it.id != ruleId) it else it.copy(pausedAt = if (closed) now else null) }
        } else s.recurring
        return s.copy(loans = loans, recurring = recurring)
    }

    /** Removes the loan, its prepayments and its EMI rule — never the EMIs already posted. */
    fun deleteLoan(id: String) = atomically { s ->
        val target = s.loans.find { it.id == id }
        val ruleId = target?.recurringId
        publish(
            s.copy(
                loans = s.loans.filter { it.id != id },
                loanPrepayments = s.loanPrepayments.filter { it.loanId != id },
                recurring = if (!ruleId.isNullOrEmpty()) s.recurring.filter { it.id != ruleId } else s.recurring,
            ),
        )
    }

    /**
     * Records the ledger row plus a real expense. The amount is capped at what is still owed, and
     * clearing it closes the loan. Returns null when the loan is unknown or nothing is left.
     */
    fun addLoanPrepayment(prepayment: NewLoanPrepayment): String? = atomically { s ->
        val loan = s.loans.find { it.id == prepayment.loanId } ?: return@atomically null
        val now = clock()
        val owed = maxPrepayment(
            LoanScheduleInput(
                principal = loan.principal,
                interestRate = loan.interestRate,
                tenureMonths = loan.tenureMonths,
                startDate = loan.startDate,
                prepayments = s.loanPrepayments.filter { it.loanId == loan.id }.map { LoanPrepaymentInput(it.amount, it.date) },
            ),
            now,
        )
        val amount = roundMoney(min(prepayment.amount, owed))
        if (amount <= 0) return@atomically null
        val createdAt = now.toIso()
        val transaction = Transaction(
            id = idGen(), type = TransactionType.Expense, amount = amount, accountId = loan.accountId,
            categoryId = loan.categoryId, date = prepayment.date,
            note = jsTrim(prepayment.note).ifEmpty { "Prepayment — ${loan.name}" }, labels = emptyList(), createdAt = createdAt,
        )
        val created = LoanPrepayment(
            id = idGen(), loanId = prepayment.loanId, amount = amount, date = prepayment.date, note = prepayment.note,
            transactionId = transaction.id, createdAt = createdAt,
        )
        var next = s.copy(
            transactions = listOf(transaction) + s.transactions,
            accounts = applyBalanceDelta(s.accounts, transaction, 1),
            loanPrepayments = listOf(created) + s.loanPrepayments,
        )
        // Clearing the whole balance finishes the loan, same as closing it by hand.
        if (amount >= owed) next = withLoanClosed(next, loan.id, true) ?: next
        publish(next)
        created.id
    }

    /** Removes the ledger row only; the expense it created is real money and stays. */
    fun deleteLoanPrepayment(id: String): LoanPrepayment? = atomically { s ->
        val prepayment = s.loanPrepayments.find { it.id == id } ?: return@atomically null
        publish(s.copy(loanPrepayments = s.loanPrepayments.filter { it.id != id }))
        prepayment
    }

    fun restoreLoanPrepayment(prepayment: LoanPrepayment) = atomically { s ->
        if (s.loanPrepayments.any { it.id == prepayment.id }) return@atomically
        publish(s.copy(loanPrepayments = listOf(prepayment) + s.loanPrepayments))
    }

    // ---- Settings, reset, import ------------------------------------------------------------

    fun updateSettings(updates: (Settings) -> Settings) = atomically { s -> publish(s.copy(settings = updates(s.settings))) }

    /** Finance data only — settings (incl. `onboardedAt`) and `lastLocalBackupAt` survive. */
    fun resetToDefaults() = atomically { s ->
        publish(initialFinanceState().copy(settings = s.settings, lastLocalBackupAt = s.lastLocalBackupAt))
    }

    /**
     * Restore a validated backup (spec/backup-format.md §5). merge = union by id, incoming wins;
     * replace = every collection swapped, absent ones emptied — except categories and labels,
     * which keep the current set. Then: snapshots deduped by period, missing opening balances
     * backfilled, every balance recomputed, settings merged.
     */
    fun importData(data: ImportPayload, mode: ImportMode = ImportMode.Merge) = atomically { s ->
        val incomingAccounts = data.importedAccounts()
        val next: FinanceState
        val accounts: List<ImportedAccount>
        if (mode == ImportMode.Merge) {
            accounts = mergeById(s.accounts.map { ImportedAccount(it, it.openingBalance) }, incomingAccounts) { it.account.id }
            next = s.copy(
                transactions = mergeById(s.transactions, data.transactions) { it.id },
                categories = mergeById(s.categories, data.categories) { it.id },
                labels = mergeById(s.labels, data.labels) { it.id },
                budgets = mergeById(s.budgets, data.budgets) { it.id },
                recurring = mergeById(s.recurring, data.recurring) { it.id },
                templates = mergeById(s.templates, data.templates) { it.id },
                rules = mergeById(s.rules, data.rules) { it.id },
                goals = mergeById(s.goals, data.goals) { it.id },
                goalContributions = mergeById(s.goalContributions, data.goalContributions) { it.id },
                people = mergeById(s.people, data.people) { it.id },
                debtEntries = mergeById(s.debtEntries, data.debtEntries) { it.id },
                netWorthSnapshots = mergeById(s.netWorthSnapshots, data.netWorthSnapshots) { it.id },
                loans = mergeById(s.loans, data.loans) { it.id },
                loanPrepayments = mergeById(s.loanPrepayments, data.loanPrepayments) { it.id },
            )
        } else {
            accounts = incomingAccounts ?: emptyList()
            next = s.copy(
                transactions = data.transactions ?: emptyList(),
                categories = data.categories ?: s.categories,
                labels = data.labels ?: s.labels,
                budgets = data.budgets ?: emptyList(),
                recurring = data.recurring ?: emptyList(),
                templates = data.templates ?: emptyList(),
                rules = data.rules ?: emptyList(),
                goals = data.goals ?: emptyList(),
                goalContributions = data.goalContributions ?: emptyList(),
                people = data.people ?: emptyList(),
                debtEntries = data.debtEntries ?: emptyList(),
                netWorthSnapshots = data.netWorthSnapshots ?: emptyList(),
                loans = data.loans ?: emptyList(),
                loanPrepayments = data.loanPrepayments ?: emptyList(),
            )
        }
        val txs = next.transactions.balanceTxs()
        // `{...state.settings, ...data.settings}`: a validated backup never carries
        // `onboardedAt`, so this device's own value survives.
        val settings = data.settings?.let { it.copy(onboardedAt = it.onboardedAt ?: s.settings.onboardedAt) } ?: s.settings
        publish(
            next.copy(
                netWorthSnapshots = dedupeSnapshotsByPeriod(next.netWorthSnapshots),
                accounts = recomputeAccountBalances(backfillOpeningBalances(accounts, txs), txs),
                settings = settings,
            ),
        )
    }

    // ---- Sample data ------------------------------------------------------------------------

    /** The store actions `loadSampleData` drives — the same path manual entry takes. */
    fun sampleDataActions(): SampleDataActions = object : SampleDataActions {
        override fun addAccount(account: NewSampleAccount): String = this@FinanceStore.addAccount(
            NewAccount(
                name = account.name, type = account.type, color = account.color, icon = account.icon,
                balance = account.balance, creditLimit = account.creditLimit,
                statementCloseDay = account.statementCloseDay, paymentDueDays = account.paymentDueDays,
            ),
        )

        override fun addGoal(goal: NewSampleGoal): String =
            this@FinanceStore.addGoal(NewGoal(goal.name, goal.icon, goal.color, goal.targetAmount, goal.targetDate))

        override fun addPerson(person: NewSamplePerson): String =
            this@FinanceStore.addPerson(NewPerson(person.name, person.icon, person.color))

        override fun addBudget(budget: NewSampleBudget) =
            this@FinanceStore.addBudget(NewBudget(budget.categoryId, null, budget.amount, budget.period, budget.rollover))

        override fun addRecurring(rule: NewSampleRecurring): String = this@FinanceStore.addRecurring(
            NewRecurring(
                type = rule.type, amount = rule.amount, accountId = rule.accountId, toAccountId = rule.toAccountId,
                categoryId = rule.categoryId, note = rule.note, labels = rule.labels, frequency = rule.frequency,
                startDate = rule.startDate, goalId = rule.goalId,
            ),
        )

        override fun addContribution(contribution: NewSampleContribution): String = this@FinanceStore.addContribution(
            NewContribution(contribution.goalId, contribution.amount, contribution.date, contribution.note),
        )

        override fun addDebtEntry(entry: NewSampleDebtEntry): String =
            this@FinanceStore.addDebtEntry(NewDebtEntry(entry.personId, entry.amount, entry.date, entry.note))

        override fun bulkAddTransactions(transactions: List<NewSampleTransaction>): Int =
            this@FinanceStore.bulkAddTransactions(
                transactions.map {
                    NewTransaction(
                        type = it.type, amount = it.amount, accountId = it.accountId, toAccountId = it.toAccountId,
                        categoryId = it.categoryId, date = it.date, note = it.note, labels = it.labels,
                    )
                },
            )
    }
}
