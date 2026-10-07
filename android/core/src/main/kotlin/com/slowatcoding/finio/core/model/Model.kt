package com.slowatcoding.finio.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Kotlin twin of web/src/types/index.ts. Field names and JSON spellings are the backup contract
// (spec/backup-format.md): a file written by either client must read back in the other, so never
// rename a property without a @SerialName pinning the old spelling. Optional fields are nullable
// with a null default and are *omitted* from JSON (see FinioJson), matching the web's
// "delete the key" convention.

@Serializable
enum class AccountType {
    @SerialName("checking") Checking,
    @SerialName("savings") Savings,
    @SerialName("cash") Cash,
    @SerialName("credit") Credit,
    @SerialName("investment") Investment,
    @SerialName("wallet") Wallet,
    @SerialName("fd") Fd,
    @SerialName("rd") Rd;

    val wire: String get() = wireName(this)
}

@Serializable
enum class TransactionType {
    @SerialName("expense") Expense,
    @SerialName("income") Income,
    @SerialName("transfer") Transfer;

    val wire: String get() = wireName(this)
}

@Serializable
enum class CategoryType {
    @SerialName("expense") Expense,
    @SerialName("income") Income,
    @SerialName("both") Both;

    val wire: String get() = wireName(this)
}

@Serializable
enum class Theme {
    @SerialName("dark") Dark,
    @SerialName("light") Light,
    @SerialName("system") System,
}

@Serializable
enum class RecurrenceFrequency {
    @SerialName("daily") Daily,
    @SerialName("weekly") Weekly,
    @SerialName("monthly") Monthly,
    @SerialName("yearly") Yearly;

    val wire: String get() = wireName(this)
}

@Serializable
enum class BudgetPeriod {
    @SerialName("weekly") Weekly,
    @SerialName("monthly") Monthly,
    @SerialName("yearly") Yearly;

    val wire: String get() = wireName(this)
}

@Serializable
enum class RuleMatchType {
    @SerialName("contains") Contains,
    @SerialName("startsWith") StartsWith,
    @SerialName("endsWith") EndsWith,
    @SerialName("equals") Equals,
    @SerialName("regex") Regex;

    val wire: String get() = wireName(this)
}

/** Which transaction types a rule is allowed to fire on. Transfers are never matched. */
@Serializable
enum class RuleScope {
    @SerialName("expense") Expense,
    @SerialName("income") Income,
    @SerialName("any") Any;

    val wire: String get() = wireName(this)
}

/** How a fixed deposit compounds. `simple` pays flat interest at maturity. RDs are always quarterly. */
@Serializable
enum class DepositCompounding {
    @SerialName("monthly") Monthly,
    @SerialName("quarterly") Quarterly,
    @SerialName("half-yearly") HalfYearly,
    @SerialName("yearly") Yearly,
    @SerialName("simple") Simple;

    val wire: String get() = wireName(this)
}

enum class ImportMode { Replace, Merge }

@Serializable
data class DepositTerms(
    /** FD principal, or RD monthly installment. */
    val amount: Double,
    /** Annual rate, percent. */
    val interestRate: Double,
    /** FD investment date, or RD first installment date (ISO). */
    val startDate: String,
    /** FD only. */
    val maturityDate: String? = null,
    /** RD only — number of monthly installments. */
    val tenureMonths: Int? = null,
    /** FD only, default quarterly. */
    val compounding: DepositCompounding? = null,
    val linkedAccountId: String,
    /** RD only — the monthly transfer rule that posts installments. */
    val recurringId: String? = null,
    /** Set once, when the maturity payout is posted. */
    val maturedAt: String? = null,
)

@Serializable
data class Account(
    val id: String,
    val name: String,
    val type: AccountType,
    val color: String,
    val icon: String,
    /** Cache of `openingBalance + Σ(transaction deltas)`. Never the source of truth. */
    val balance: Double,
    /** The balance before any recorded transaction. */
    val openingBalance: Double,
    val createdAt: String,
    val creditLimit: Double? = null,
    /** Day of the month (1–28) a credit account's statement closes. */
    val statementCloseDay: Int? = null,
    val paymentDueDays: Int? = null,
    val minimumDuePercent: Double? = null,
    val archivedAt: String? = null,
    /** Present exactly when `type` is fd or rd. */
    val deposit: DepositTerms? = null,
)

@Serializable
data class TransactionSplit(val categoryId: String, val amount: Double)

@Serializable
data class Transaction(
    val id: String,
    val type: TransactionType,
    val amount: Double,
    val accountId: String,
    val toAccountId: String? = null,
    /** `''` when [splits] is present — aggregate through transactionCategoryAmounts(). */
    val categoryId: String,
    val date: String,
    val note: String,
    val labels: List<String> = emptyList(),
    val createdAt: String,
    val recurringId: String? = null,
    val splits: List<TransactionSplit>? = null,
)

@Serializable
data class Category(
    val id: String,
    val name: String,
    val icon: String,
    val color: String,
    val type: CategoryType,
)

@Serializable
data class Label(val id: String, val name: String, val color: String)

@Serializable
data class Budget(
    val id: String,
    /** `''` = overall budget across all expenses. Ignored when [labelId] is set. */
    val categoryId: String,
    val labelId: String? = null,
    val amount: Double,
    val period: BudgetPeriod,
    val rollover: Boolean,
    val createdAt: String,
)

@Serializable
data class RecurringTransaction(
    val id: String,
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
    val occurrenceCount: Int = 0,
    val pausedAt: String? = null,
    /** ISO date of the most recent auto-generated occurrence, or null. Always written (as null). */
    val lastRunDate: String? = null,
    val createdAt: String,
    val goalId: String? = null,
)

@Serializable
data class Settings(
    val theme: Theme = Theme.System,
    /** True-black dark mode for AMOLED panels; inert unless the resolved theme is dark. */
    val amoledDark: Boolean = false,
    val userName: String = "",
    val autoLocalBackup: Boolean = false,
    val monthStartDay: Int = 1,
    val onboardedAt: String? = null,
    val hideAmounts: Boolean = false,
    val notificationsEnabled: Boolean = false,
    val notifyBills: Boolean = true,
    val notifyBudgets: Boolean = true,
    val notifyCreditDue: Boolean = true,
    val notifyLeadDays: Int = 2,
    val notifyDailyLog: Boolean = true,
)

/** App lock. Deliberately *not* in [Settings]: settings travel in every backup. */
@Serializable
data class AppLockConfig(
    val enabled: Boolean,
    val salt: String,
    val hash: String,
    val iterations: Int,
    val pinLength: Int,
    val autoLockMinutes: Int,
    /** Android: whether biometric unlock is on (web: the WebAuthn rawId). */
    val webauthnCredentialId: String? = null,
    val createdAt: String,
)

/** Cloud-backup encryption config. Same reason as [AppLockConfig] for living outside Settings. */
@Serializable
data class BackupCryptoConfig(
    val enabled: Boolean,
    val salt: String,
    val iterations: Int,
    val verifierIv: String,
    val verifierCiphertext: String,
    val createdAt: String,
)

@Serializable
data class Goal(
    val id: String,
    val name: String,
    val icon: String,
    val color: String,
    val targetAmount: Double,
    val targetDate: String? = null,
    val linkedAccountId: String? = null,
    val createdAt: String,
)

@Serializable
data class GoalContribution(
    val id: String,
    val goalId: String,
    /** Positive = contribution, negative = withdrawal. */
    val amount: Double,
    val date: String,
    val note: String,
    val createdAt: String,
)

@Serializable
data class Person(
    val id: String,
    val name: String,
    val icon: String,
    val color: String,
    val createdAt: String,
)

@Serializable
data class DebtEntry(
    val id: String,
    val personId: String,
    /** + = they owe you. */
    val amount: Double,
    val date: String,
    val note: String,
    val settledTransactionId: String? = null,
    val createdAt: String,
)

@Serializable
data class Loan(
    val id: String,
    val name: String,
    val principal: Double,
    val interestRate: Double,
    val tenureMonths: Int,
    /** ISO date of the first EMI. */
    val startDate: String,
    val accountId: String,
    val categoryId: String,
    val recurringId: String? = null,
    val closedAt: String? = null,
    val createdAt: String,
)

@Serializable
data class LoanPrepayment(
    val id: String,
    val loanId: String,
    val amount: Double,
    val date: String,
    val note: String,
    val transactionId: String? = null,
    val createdAt: String,
)

@Serializable
data class CategoryRule(
    val id: String,
    val pattern: String,
    val matchType: RuleMatchType,
    val scope: RuleScope,
    val categoryId: String,
    val labelIds: List<String> = emptyList(),
    val enabled: Boolean,
    val createdAt: String,
)

@Serializable
data class NetWorthSnapshot(
    val id: String,
    /** `yyyy-MM` of the financial month's start — the snapshot's real identity. */
    val periodKey: String,
    val date: String,
    val assets: Double,
    val liabilities: Double,
    val createdAt: String,
)

@Serializable
data class TransactionTemplate(
    val id: String,
    val name: String,
    val type: TransactionType,
    val amount: Double,
    val accountId: String,
    val toAccountId: String? = null,
    val categoryId: String,
    val note: String,
    val labels: List<String> = emptyList(),
    val createdAt: String,
    val splits: List<TransactionSplit>? = null,
)

/** The only fields a rule may rewrite on an existing transaction — enough to undo one pass. */
data class TransactionCategorization(
    val id: String,
    val categoryId: String,
    val labels: List<String>,
    val splits: List<TransactionSplit>?,
)

/** Sanitized backup contents accepted by importData. A null collection = absent from the file. */
data class ImportPayload(
    val accounts: List<Account>? = null,
    val transactions: List<Transaction>? = null,
    val categories: List<Category>? = null,
    val labels: List<Label>? = null,
    val budgets: List<Budget>? = null,
    val recurring: List<RecurringTransaction>? = null,
    val templates: List<TransactionTemplate>? = null,
    val rules: List<CategoryRule>? = null,
    val goals: List<Goal>? = null,
    val goalContributions: List<GoalContribution>? = null,
    val people: List<Person>? = null,
    val debtEntries: List<DebtEntry>? = null,
    val netWorthSnapshots: List<NetWorthSnapshot>? = null,
    val loans: List<Loan>? = null,
    val loanPrepayments: List<LoanPrepayment>? = null,
    val settings: Settings? = null,
    /** Ids of imported accounts that had no openingBalance (pre-v5 data) — backfilled on import. */
    val accountsMissingOpeningBalance: Set<String> = emptySet(),
)

/** The persisted finance data — the web's `finio-storage` state minus actions. */
@Serializable
data class FinanceState(
    val accounts: List<Account> = emptyList(),
    val transactions: List<Transaction> = emptyList(),
    val categories: List<Category> = emptyList(),
    val labels: List<Label> = emptyList(),
    val budgets: List<Budget> = emptyList(),
    val recurring: List<RecurringTransaction> = emptyList(),
    val templates: List<TransactionTemplate> = emptyList(),
    val rules: List<CategoryRule> = emptyList(),
    val goals: List<Goal> = emptyList(),
    val goalContributions: List<GoalContribution> = emptyList(),
    val people: List<Person> = emptyList(),
    val debtEntries: List<DebtEntry> = emptyList(),
    val netWorthSnapshots: List<NetWorthSnapshot> = emptyList(),
    val loans: List<Loan> = emptyList(),
    val loanPrepayments: List<LoanPrepayment> = emptyList(),
    val settings: Settings = Settings(),
    /** ISO date (YYYY-MM-DD) of the last automatic local backup, or null. */
    val lastLocalBackupAt: String? = null,
)

/** The `@SerialName` of an enum constant — its spelling in JSON and in the web code. */
fun wireName(value: Enum<*>): String =
    value.declaringJavaClass.getField(value.name).getAnnotation(SerialName::class.java)?.value
        ?: value.name
