package com.slowatcoding.finio.core.importing

import com.slowatcoding.finio.core.backup.BACKUP_SCHEMA_VERSION
import com.slowatcoding.finio.core.backup.BackupMeta
import com.slowatcoding.finio.core.backup.jsDateParses
import com.slowatcoding.finio.core.backup.readBackupMeta
import com.slowatcoding.finio.core.data.defaultSettings
import com.slowatcoding.finio.core.js.jsNumberToString
import com.slowatcoding.finio.core.js.jsRound
import com.slowatcoding.finio.core.js.jsTrunc
import com.slowatcoding.finio.core.js.nowInstant
import com.slowatcoding.finio.core.js.toIso
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
import com.slowatcoding.finio.core.model.Goal
import com.slowatcoding.finio.core.model.GoalContribution
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
import com.slowatcoding.finio.core.model.Theme
import com.slowatcoding.finio.core.model.Transaction
import com.slowatcoding.finio.core.model.TransactionSplit
import com.slowatcoding.finio.core.model.TransactionTemplate
import com.slowatcoding.finio.core.model.TransactionType
import com.slowatcoding.finio.core.model.wireName
import com.slowatcoding.finio.core.notify.MAX_NOTIFY_LEAD_DAYS
import com.slowatcoding.finio.core.period.normalizeMonthStartDay
import com.slowatcoding.finio.core.rules.isValidPattern
import com.slowatcoding.finio.core.store.ImportedAccount
import com.slowatcoding.finio.core.util.jsTrim
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.time.Instant
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

// Port of web/src/utils/importValidation.ts. Backup files are user-supplied and completely
// replace (or merge into) every row of a money app, so nothing goes in unchecked. Rows that fail
// validation are dropped and counted rather than silently poisoning balances; cross-entity
// problems that are recoverable (a transaction pointing at an account that isn't in the file) are
// reported as warnings and kept.
//
// The input is raw kotlinx JSON, and every TS `typeof` check is mirrored on it exactly: a JSON
// string "12" is not a number, `true` is not 1, `null` is not a string. Two representation
// differences from the TS output, both forced by the Kotlin model:
//  - an account with no `openingBalance` carries a 0.0 placeholder; its id is listed in
//    ImportPayload.accountsMissingOpeningBalance (see [importedAccounts]) — the TS leaves the key out;
//  - the Int-typed fields (statementCloseDay, paymentDueDays) are truncated where the TS would keep
//    a fractional value verbatim.

enum class ImportEntity(val key: String) {
    Accounts("accounts"),
    Transactions("transactions"),
    Categories("categories"),
    Labels("labels"),
    Budgets("budgets"),
    Recurring("recurring"),
    Templates("templates"),
    Rules("rules"),
    Goals("goals"),
    GoalContributions("goalContributions"),
    People("people"),
    DebtEntries("debtEntries"),
    NetWorthSnapshots("netWorthSnapshots"),
    Loans("loans"),
    LoanPrepayments("loanPrepayments"),
}

val IMPORT_ENTITIES: List<ImportEntity> = ImportEntity.entries

val ENTITY_LABELS: Map<ImportEntity, String> = linkedMapOf(
    ImportEntity.Accounts to "Accounts",
    ImportEntity.Transactions to "Transactions",
    ImportEntity.Categories to "Categories",
    ImportEntity.Labels to "Labels",
    ImportEntity.Budgets to "Budgets",
    ImportEntity.Recurring to "Recurring rules",
    ImportEntity.Templates to "Templates",
    ImportEntity.Rules to "Categorization rules",
    ImportEntity.Goals to "Savings goals",
    ImportEntity.GoalContributions to "Goal contributions",
    ImportEntity.People to "People",
    ImportEntity.DebtEntries to "Debt entries",
    ImportEntity.NetWorthSnapshots to "Net worth snapshots",
    ImportEntity.Loans to "Loans",
    ImportEntity.LoanPrepayments to "Loan prepayments",
)

data class EntityReport(
    val present: Boolean = false,
    val total: Int = 0,
    val accepted: Int = 0,
    val rejected: Int = 0,
)

data class ImportReport(
    /** In [IMPORT_ENTITIES] order. */
    val counts: Map<ImportEntity, EntityReport>,
    val hasSettings: Boolean,
    /** Per-row rejection reasons, capped for display. */
    val issues: List<String>,
    /** Problems that don't drop data but the user should see before committing. */
    val warnings: List<String>,
)

/** True when the report holds at least one accepted row or a settings block worth importing. */
fun hasImportableData(report: ImportReport): Boolean =
    report.hasSettings || report.counts.values.any { it.accepted > 0 }

data class ValidatedBackup(
    val data: ImportPayload,
    val report: ImportReport,
    /** Provenance stamp, when the file carries one (empty on legacy exports). */
    val meta: BackupMeta,
)

/** Thrown when the input carries nothing importable at all. */
class InvalidBackupException : IllegalArgumentException("Not a Finio backup file")

/** The accounts with their opening balance as the store's balance math wants it (null = absent). */
fun ImportPayload.importedAccounts(): List<ImportedAccount>? = accounts?.map {
    ImportedAccount(it, if (it.id in accountsMissingOpeningBalance) null else it.openingBalance)
}

const val MAX_REPORTED_ISSUES = 8

private inline fun <reified E : Enum<E>> byWire(): Map<String, E> = enumValues<E>().associateBy { wireName(it) }

private val ACCOUNT_TYPES = byWire<AccountType>()
private val DEPOSIT_COMPOUNDINGS = byWire<DepositCompounding>()
private val TRANSACTION_TYPES = byWire<TransactionType>()
private val CATEGORY_TYPES = byWire<CategoryType>()
private val FREQUENCIES = byWire<RecurrenceFrequency>()
private val BUDGET_PERIODS = byWire<BudgetPeriod>()
private val MATCH_TYPES = byWire<RuleMatchType>()
private val RULE_SCOPES = byWire<RuleScope>()
private val THEMES = byWire<Theme>()

// ---- JS value checks on raw JSON -----------------------------------------------------------

/** `typeof v === 'string'` → its value. */
private fun str(v: JsonElement?): String? = (v as? JsonPrimitive)?.takeIf { it.isString }?.content

private fun isNumber(v: JsonElement?): Boolean =
    v is JsonPrimitive && v !is JsonNull && !v.isString && v.content != "true" && v.content != "false"

/** `typeof v === 'number'` (JSON can't carry NaN; an overflowing literal parses to ±Infinity). */
private fun num(v: JsonElement?): Double? = if (isNumber(v)) (v as JsonPrimitive).content.toDoubleOrNull() else null

private fun bool(v: JsonElement?): Boolean? =
    (v as? JsonPrimitive)?.takeIf { !it.isString && it !is JsonNull }?.content?.let {
        when (it) { "true" -> true; "false" -> false; else -> null }
    }

/** `String(v)`, where a missing key is `undefined`. */
internal fun jsString(v: JsonElement?): String = when (v) {
    null -> "undefined"
    is JsonNull -> "null"
    is JsonPrimitive -> if (v.isString) v.content else num(v)?.let(::jsNumberToString) ?: v.content
    // Array.prototype.toString → join(","), where null/undefined elements become "".
    is JsonArray -> v.joinToString(",") { if (it is JsonNull) "" else jsString(it) }
    is JsonObject -> "[object Object]"
}

private fun asId(v: JsonElement?): String? = str(v)?.takeIf { jsTrim(it) != "" }

private fun asString(v: JsonElement?, fallback: String): String = str(v) ?: fallback

private fun asFiniteNumber(v: JsonElement?): Double? = num(v)?.takeIf { it.isFinite() }

private fun asIsoDate(v: JsonElement?): String? {
    val s = str(v) ?: return null
    if (jsTrim(s) == "") return null
    return if (jsDateParses(s)) s else null
}

private fun asStringArray(v: JsonElement?): List<String> =
    (v as? JsonArray)?.mapNotNull { str(it) } ?: emptyList()

private fun <E> member(v: JsonElement?, set: Map<String, E>): E? = str(v)?.let { set[it] }

/** Double → Int the way a JS number would be stored in an Int-typed Kotlin field. */
private fun Double.truncInt(): Int = jsTrunc(this).toInt()

// ---- Row parsers ---------------------------------------------------------------------------

private class Reject(val reason: String) : Exception(reason, null, false, false)

private fun reject(reason: String): Nothing = throw Reject(reason)

/**
 * Splits are only kept when the shape is right and they add up — a malformed or mismatched-sum
 * split isn't a reason to drop the whole transaction, it just falls back to `categoryId`.
 */
private fun asSplits(value: JsonElement?, amount: Double): List<TransactionSplit>? {
    if (value !is JsonArray || value.size < 2) return null
    val splits = ArrayList<TransactionSplit>()
    for (row in value) {
        if (row !is JsonObject) return null
        val categoryId = asId(row["categoryId"])
        val splitAmount = asFiniteNumber(row["amount"])
        if (categoryId == null || splitAmount == null || splitAmount <= 0) return null
        splits += TransactionSplit(categoryId, splitAmount)
    }
    var total = 0.0
    for (s in splits) total += s.amount
    if (abs(total - amount) > 0.01) return null
    return splits
}

/** A deposit's terms, or null when they are unusable (the account is then rejected). */
private fun asDepositTerms(value: JsonElement?, type: AccountType): DepositTerms? {
    if (value !is JsonObject) return null
    val amount = asFiniteNumber(value["amount"])
    val interestRate = asFiniteNumber(value["interestRate"])
    val startDate = asIsoDate(value["startDate"])
    val linkedAccountId = asId(value["linkedAccountId"])
    if (amount == null || amount <= 0 || interestRate == null || interestRate < 0) return null
    if (startDate == null || linkedAccountId == null) return null

    val maturityDate = asIsoDate(value["maturityDate"])
    val tenureMonths = asFiniteNumber(value["tenureMonths"])
    if (type == AccountType.Fd && maturityDate == null) return null
    if (type == AccountType.Rd && (tenureMonths == null || tenureMonths < 1)) return null

    val compounding = member(value["compounding"], DEPOSIT_COMPOUNDINGS)
    return DepositTerms(
        amount = amount,
        interestRate = interestRate,
        startDate = startDate,
        linkedAccountId = linkedAccountId,
        maturityDate = if (type == AccountType.Fd) maturityDate else null,
        compounding = if (type == AccountType.Fd) compounding ?: DepositCompounding.Quarterly else null,
        tenureMonths = if (type == AccountType.Rd) jsRound(tenureMonths!!).toInt() else null,
        recurringId = asId(value["recurringId"]),
        maturedAt = asIsoDate(value["maturedAt"]),
    )
}

private fun parseAccount(row: JsonObject, nowIso: String): ImportedAccount {
    val id = asId(row["id"]) ?: reject("missing id")
    val name = asId(row["name"]) ?: reject("missing name")
    val type = member(row["type"], ACCOUNT_TYPES) ?: reject("unknown account type \"${jsString(row["type"])}\"")
    val balance = asFiniteNumber(row["balance"]) ?: reject("balance is not a number")

    val openingBalance = asFiniteNumber(row["openingBalance"])
    val isDeposit = type == AccountType.Fd || type == AccountType.Rd
    val deposit = if (isDeposit) asDepositTerms(row["deposit"], type) else null
    if (isDeposit && deposit == null) reject("deposit terms are missing or invalid")

    val account = Account(
        id = id,
        name = name,
        type = type,
        color = asString(row["color"], "#146b54"),
        icon = asString(row["icon"], "landmark"),
        balance = balance,
        openingBalance = openingBalance ?: 0.0,
        createdAt = asIsoDate(row["createdAt"]) ?: nowIso,
        creditLimit = asFiniteNumber(row["creditLimit"]),
        statementCloseDay = asFiniteNumber(row["statementCloseDay"])?.truncInt(),
        paymentDueDays = asFiniteNumber(row["paymentDueDays"])?.truncInt(),
        minimumDuePercent = asFiniteNumber(row["minimumDuePercent"]),
        deposit = deposit,
        // An unparseable value just means "not archived" — never a reason to drop the account.
        archivedAt = asIsoDate(row["archivedAt"]),
    )
    return ImportedAccount(account, openingBalance)
}

private fun parseTransaction(row: JsonObject): Transaction {
    val id = asId(row["id"]) ?: reject("missing id")
    val type = member(row["type"], TRANSACTION_TYPES) ?: reject("unknown transaction type \"${jsString(row["type"])}\"")
    val amount = asFiniteNumber(row["amount"]) ?: reject("amount is not a number")
    if (amount < 0) reject("negative amount")
    val accountId = asId(row["accountId"]) ?: reject("missing accountId")
    val date = asIsoDate(row["date"]) ?: reject("unparseable date \"${jsString(row["date"])}\"")

    val toAccountId = asId(row["toAccountId"])
    if (type == TransactionType.Transfer && toAccountId == null) reject("transfer has no destination account")

    val splits = if (type == TransactionType.Expense) asSplits(row["splits"], amount) else null
    return Transaction(
        id = id,
        type = type,
        amount = amount,
        accountId = accountId,
        categoryId = if (splits != null) "" else asString(row["categoryId"], ""),
        date = date,
        note = asString(row["note"], ""),
        labels = asStringArray(row["labels"]),
        createdAt = asIsoDate(row["createdAt"]) ?: date,
        // Kept on any type, like the TS (only recurring rules and templates restrict it).
        toAccountId = toAccountId,
        recurringId = asId(row["recurringId"]),
        splits = splits,
        merchant = asString(row["merchant"], "").trim().ifEmpty { null },
        forWhom = asString(row["forWhom"], "").trim().ifEmpty { null },
    )
}

private fun parseCategory(row: JsonObject): Category {
    val id = asId(row["id"]) ?: reject("missing id")
    val name = asId(row["name"]) ?: reject("missing name")
    val type = member(row["type"], CATEGORY_TYPES) ?: reject("unknown category type \"${jsString(row["type"])}\"")
    return Category(
        id = id,
        name = name,
        icon = asString(row["icon"], "circle-ellipsis"),
        color = asString(row["color"], "#94a3b8"),
        type = type,
    )
}

private fun parseLabel(row: JsonObject): Label {
    val id = asId(row["id"]) ?: reject("missing id")
    val name = asId(row["name"]) ?: reject("missing name")
    return Label(id, name, asString(row["color"], "#64748b"))
}

private fun parseBudget(row: JsonObject, nowIso: String): Budget {
    val id = asId(row["id"]) ?: reject("missing id")
    val categoryId = str(row["categoryId"]) ?: reject("missing categoryId")
    val amount = asFiniteNumber(row["amount"]) ?: reject("amount is not a number")
    if (amount <= 0) reject("amount must be greater than zero")
    return Budget(
        id = id,
        categoryId = categoryId,
        amount = amount,
        // Pre-v7 backups have neither field; both defaults reproduce the old behaviour exactly.
        period = member(row["period"], BUDGET_PERIODS) ?: BudgetPeriod.Monthly,
        rollover = bool(row["rollover"]) == true,
        createdAt = asIsoDate(row["createdAt"]) ?: nowIso,
        labelId = asId(row["labelId"]),
    )
}

private fun parseRecurring(row: JsonObject): RecurringTransaction {
    val id = asId(row["id"]) ?: reject("missing id")
    val type = member(row["type"], TRANSACTION_TYPES) ?: reject("unknown recurring type \"${jsString(row["type"])}\"")
    val amount = asFiniteNumber(row["amount"]) ?: reject("amount is not a number")
    if (amount < 0) reject("negative amount")
    val accountId = asId(row["accountId"]) ?: reject("missing accountId")
    val frequency = member(row["frequency"], FREQUENCIES) ?: reject("unknown frequency \"${jsString(row["frequency"])}\"")
    val startDate = asIsoDate(row["startDate"]) ?: reject("unparseable startDate \"${jsString(row["startDate"])}\"")

    val toAccountId = asId(row["toAccountId"])
    if (type == TransactionType.Transfer && toAccountId == null) reject("transfer rule has no destination account")

    val maxRaw = asFiniteNumber(row["maxOccurrences"])
    val occurrenceRaw = asFiniteNumber(row["occurrenceCount"])
    return RecurringTransaction(
        id = id,
        type = type,
        amount = amount,
        accountId = accountId,
        categoryId = asString(row["categoryId"], ""),
        note = asString(row["note"], ""),
        labels = asStringArray(row["labels"]),
        frequency = frequency,
        startDate = startDate,
        // Pre-v7 backups carry none of the lifecycle fields — the defaults mean "runs forever".
        occurrenceCount = if (occurrenceRaw != null && occurrenceRaw > 0) occurrenceRaw.truncInt() else 0,
        lastRunDate = asIsoDate(row["lastRunDate"]),
        createdAt = asIsoDate(row["createdAt"]) ?: startDate,
        toAccountId = if (type == TransactionType.Transfer) toAccountId else null,
        endDate = asIsoDate(row["endDate"]),
        maxOccurrences = if (maxRaw != null && maxRaw >= 1) maxRaw.truncInt() else null,
        pausedAt = asIsoDate(row["pausedAt"]),
        goalId = asId(row["goalId"]),
    )
}

private fun parseTemplate(row: JsonObject, nowIso: String): TransactionTemplate {
    val id = asId(row["id"]) ?: reject("missing id")
    val name = asId(row["name"]) ?: reject("missing name")
    val type = member(row["type"], TRANSACTION_TYPES) ?: reject("unknown template type \"${jsString(row["type"])}\"")
    val amount = asFiniteNumber(row["amount"]) ?: reject("amount is not a number")
    if (amount < 0) reject("negative amount")
    val accountId = asId(row["accountId"]) ?: reject("missing accountId")

    val toAccountId = asId(row["toAccountId"])
    if (type == TransactionType.Transfer && toAccountId == null) reject("transfer template has no destination account")

    val splits = if (type == TransactionType.Expense) asSplits(row["splits"], amount) else null
    return TransactionTemplate(
        id = id,
        name = name,
        type = type,
        amount = amount,
        accountId = accountId,
        categoryId = if (splits != null) "" else asString(row["categoryId"], ""),
        note = asString(row["note"], ""),
        labels = asStringArray(row["labels"]),
        createdAt = asIsoDate(row["createdAt"]) ?: nowIso,
        toAccountId = if (type == TransactionType.Transfer) toAccountId else null,
        splits = splits,
    )
}

private fun parseRule(row: JsonObject, nowIso: String): CategoryRule {
    val id = asId(row["id"]) ?: reject("missing id")
    val pattern = asId(row["pattern"]) ?: reject("missing pattern")
    val matchType = member(row["matchType"], MATCH_TYPES) ?: reject("unknown match type \"${jsString(row["matchType"])}\"")
    // A rule that files nowhere is not recoverable the way a stray label is — drop it.
    val categoryId = asId(row["categoryId"]) ?: reject("missing categoryId")
    // An unparseable regex would silently match nothing on every transaction forever.
    if (matchType == RuleMatchType.Regex && !isValidPattern(pattern, RuleMatchType.Regex)) {
        reject("invalid regex \"$pattern\"")
    }
    return CategoryRule(
        id = id,
        pattern = pattern,
        matchType = matchType,
        scope = member(row["scope"], RULE_SCOPES) ?: RuleScope.Any,
        categoryId = categoryId,
        labelIds = asStringArray(row["labelIds"]),
        // Anything but an explicit `false` stays on — a rule in a backup was presumably wanted.
        enabled = bool(row["enabled"]) != false,
        createdAt = asIsoDate(row["createdAt"]) ?: nowIso,
    )
}

private fun parseGoal(row: JsonObject, nowIso: String): Goal {
    val id = asId(row["id"]) ?: reject("missing id")
    val name = asId(row["name"]) ?: reject("missing name")
    val targetAmount = asFiniteNumber(row["targetAmount"]) ?: reject("targetAmount is not a number")
    if (targetAmount <= 0) reject("targetAmount must be greater than zero")
    return Goal(
        id = id,
        name = name,
        icon = asString(row["icon"], "target"),
        color = asString(row["color"], "#146b54"),
        targetAmount = targetAmount,
        createdAt = asIsoDate(row["createdAt"]) ?: nowIso,
        targetDate = asIsoDate(row["targetDate"]),
        linkedAccountId = asId(row["linkedAccountId"]),
    )
}

private fun parseGoalContribution(row: JsonObject): GoalContribution {
    val id = asId(row["id"]) ?: reject("missing id")
    val goalId = asId(row["goalId"]) ?: reject("missing goalId")
    val amount = asFiniteNumber(row["amount"]) ?: reject("amount is not a number")
    if (amount == 0.0) reject("amount cannot be zero")
    val date = asIsoDate(row["date"]) ?: reject("unparseable date \"${jsString(row["date"])}\"")
    return GoalContribution(id, goalId, amount, date, asString(row["note"], ""), asIsoDate(row["createdAt"]) ?: date)
}

private fun parsePerson(row: JsonObject, nowIso: String): Person {
    val id = asId(row["id"]) ?: reject("missing id")
    val name = asId(row["name"]) ?: reject("missing name")
    return Person(
        id = id,
        name = name,
        icon = asString(row["icon"], "user"),
        color = asString(row["color"], "#146b54"),
        createdAt = asIsoDate(row["createdAt"]) ?: nowIso,
    )
}

private fun parseDebtEntry(row: JsonObject): DebtEntry {
    val id = asId(row["id"]) ?: reject("missing id")
    val personId = asId(row["personId"]) ?: reject("missing personId")
    val amount = asFiniteNumber(row["amount"]) ?: reject("amount is not a number")
    if (amount == 0.0) reject("amount cannot be zero")
    val date = asIsoDate(row["date"]) ?: reject("unparseable date \"${jsString(row["date"])}\"")
    return DebtEntry(
        id = id,
        personId = personId,
        amount = amount,
        date = date,
        note = asString(row["note"], ""),
        createdAt = asIsoDate(row["createdAt"]) ?: date,
        settledTransactionId = asId(row["settledTransactionId"]),
    )
}

// matchEntire, not containsMatchIn: a Java `$` would also match before a trailing newline.
private val PERIOD_KEY = Regex("\\d{4}-(0[1-9]|1[0-2])")

private fun parseNetWorthSnapshot(row: JsonObject): NetWorthSnapshot {
    val id = asId(row["id"]) ?: reject("missing id")
    // The period key is the snapshot's real identity — a malformed one would land the point on
    // the wrong month of the trend, which is worse than not having it at all.
    val periodKey = asId(row["periodKey"])
    if (periodKey == null || PERIOD_KEY.matchEntire(periodKey) == null) {
        reject("unparseable period key \"${jsString(row["periodKey"])}\"")
    }
    val date = asIsoDate(row["date"]) ?: reject("unparseable date \"${jsString(row["date"])}\"")
    val assets = asFiniteNumber(row["assets"]) ?: reject("assets is not a number")
    val liabilities = asFiniteNumber(row["liabilities"]) ?: reject("liabilities is not a number")
    return NetWorthSnapshot(id, periodKey, date, assets, liabilities, asIsoDate(row["createdAt"]) ?: date)
}

private fun parseLoan(row: JsonObject): Loan {
    val id = asId(row["id"]) ?: reject("missing id")
    val name = asId(row["name"]) ?: reject("missing name")
    val principal = asFiniteNumber(row["principal"])
    if (principal == null || principal <= 0) reject("principal must be greater than zero")
    val interestRate = asFiniteNumber(row["interestRate"])
    if (interestRate == null || interestRate < 0) reject("interestRate is not a number")
    val tenureMonths = asFiniteNumber(row["tenureMonths"])
    if (tenureMonths == null || tenureMonths <= 0) reject("tenureMonths must be greater than zero")
    val startDate = asIsoDate(row["startDate"]) ?: reject("unparseable startDate \"${jsString(row["startDate"])}\"")
    val accountId = asId(row["accountId"]) ?: reject("missing accountId")
    val categoryId = asId(row["categoryId"]) ?: reject("missing categoryId")
    return Loan(
        id = id,
        name = name,
        principal = principal,
        interestRate = interestRate,
        // Quirk preserved: 0 < tenure < 1 passes the check and truncates to 0.
        tenureMonths = tenureMonths.truncInt(),
        startDate = startDate,
        accountId = accountId,
        categoryId = categoryId,
        createdAt = asIsoDate(row["createdAt"]) ?: startDate,
        recurringId = asId(row["recurringId"]),
        closedAt = asIsoDate(row["closedAt"]),
    )
}

private fun parseLoanPrepayment(row: JsonObject): LoanPrepayment {
    val id = asId(row["id"]) ?: reject("missing id")
    val loanId = asId(row["loanId"]) ?: reject("missing loanId")
    val amount = asFiniteNumber(row["amount"])
    if (amount == null || amount <= 0) reject("amount must be greater than zero")
    val date = asIsoDate(row["date"]) ?: reject("unparseable date \"${jsString(row["date"])}\"")
    return LoanPrepayment(
        id = id,
        loanId = loanId,
        amount = amount,
        date = date,
        note = asString(row["note"], ""),
        createdAt = asIsoDate(row["createdAt"]) ?: date,
        transactionId = asId(row["transactionId"]),
    )
}

private fun parseSettings(value: JsonElement?): Settings? {
    if (value !is JsonObject) return null
    val d = defaultSettings
    fun flag(key: String, fallback: Boolean) = bool(value[key]) ?: fallback
    // Pick only known keys — this is also what strips the legacy `currency` field, and what
    // drops `onboardedAt` so a restore never re-runs or skips this device's onboarding.
    return Settings(
        theme = member(value["theme"], THEMES) ?: d.theme,
        amoledDark = flag("amoledDark", d.amoledDark),
        userName = asId(value["userName"]) ?: d.userName,
        autoLocalBackup = flag("autoLocalBackup", d.autoLocalBackup),
        monthStartDay = normalizeMonthStartDay(value["monthStartDay"]),
        hideAmounts = flag("hideAmounts", d.hideAmounts),
        notificationsEnabled = flag("notificationsEnabled", d.notificationsEnabled),
        notifyBills = flag("notifyBills", d.notifyBills),
        notifyBudgets = flag("notifyBudgets", d.notifyBudgets),
        notifyCreditDue = flag("notifyCreditDue", d.notifyCreditDue),
        notifyLeadDays = asFiniteNumber(value["notifyLeadDays"])
            ?.let { min(MAX_NOTIFY_LEAD_DAYS.toDouble(), max(0.0, jsTrunc(it))).toInt() }
            ?: d.notifyLeadDays,
        notifyDailyLog = flag("notifyDailyLog", d.notifyDailyLog),
    )
}

// ---- Collection ------------------------------------------------------------------------------

private class Collected<T>(val rows: List<T>?, val report: EntityReport, val issues: List<String>)

private fun <T> collect(raw: JsonElement?, entity: ImportEntity, idOf: (T) -> String, parse: (JsonObject) -> T): Collected<T> {
    val label = ENTITY_LABELS.getValue(entity)
    if (raw == null || raw is JsonNull) return Collected(null, EntityReport(), emptyList())
    if (raw !is JsonArray) {
        return Collected(null, EntityReport(present = true), listOf("$label: expected a list, ignoring it"))
    }
    val issues = ArrayList<String>()
    val rows = ArrayList<T>()
    val seen = HashSet<String>()
    var rejected = 0
    raw.forEachIndexed { index, row ->
        if (row !is JsonObject) {
            rejected += 1
            issues += "$label #${index + 1}: not an object"
            return@forEachIndexed
        }
        val parsed = try {
            parse(row)
        } catch (r: Reject) {
            rejected += 1
            issues += "$label #${index + 1}: ${r.reason}"
            return@forEachIndexed
        }
        val id = idOf(parsed)
        if (!seen.add(id)) {
            rejected += 1
            issues += "$label #${index + 1}: duplicate id $id"
            return@forEachIndexed
        }
        rows += parsed
    }
    return Collected(rows, EntityReport(true, raw.size, rows.size, rejected), issues)
}

private fun plural(n: Int, one: String, many: String) = if (n == 1) one else many

/**
 * Validate and sanitize a parsed backup object. Throws [InvalidBackupException] if the file
 * carries nothing importable at all; otherwise always returns data plus a report to preview.
 * [now] stamps `createdAt` on rows that have none (the TS uses `new Date()`).
 */
fun validateBackup(raw: JsonElement?, now: Instant = nowInstant()): ValidatedBackup {
    if (raw !is JsonObject) throw InvalidBackupException()
    val nowIso = now.toIso()

    val accounts = collect(raw["accounts"], ImportEntity.Accounts, { it.account.id }) { parseAccount(it, nowIso) }
    val transactions = collect(raw["transactions"], ImportEntity.Transactions, Transaction::id, ::parseTransaction)
    val categories = collect(raw["categories"], ImportEntity.Categories, Category::id, ::parseCategory)
    val labels = collect(raw["labels"], ImportEntity.Labels, Label::id, ::parseLabel)
    val budgets = collect(raw["budgets"], ImportEntity.Budgets, Budget::id) { parseBudget(it, nowIso) }
    val recurring = collect(raw["recurring"], ImportEntity.Recurring, RecurringTransaction::id, ::parseRecurring)
    val templates = collect(raw["templates"], ImportEntity.Templates, TransactionTemplate::id) { parseTemplate(it, nowIso) }
    val rules = collect(raw["rules"], ImportEntity.Rules, CategoryRule::id) { parseRule(it, nowIso) }
    val goals = collect(raw["goals"], ImportEntity.Goals, Goal::id) { parseGoal(it, nowIso) }
    val goalContributions = collect(raw["goalContributions"], ImportEntity.GoalContributions, GoalContribution::id, ::parseGoalContribution)
    val people = collect(raw["people"], ImportEntity.People, Person::id) { parsePerson(it, nowIso) }
    val debtEntries = collect(raw["debtEntries"], ImportEntity.DebtEntries, DebtEntry::id, ::parseDebtEntry)
    val netWorthSnapshots = collect(raw["netWorthSnapshots"], ImportEntity.NetWorthSnapshots, NetWorthSnapshot::id, ::parseNetWorthSnapshot)
    val loans = collect(raw["loans"], ImportEntity.Loans, Loan::id, ::parseLoan)
    val loanPrepayments = collect(raw["loanPrepayments"], ImportEntity.LoanPrepayments, LoanPrepayment::id, ::parseLoanPrepayment)
    val settings = parseSettings(raw["settings"])

    val all: List<Pair<ImportEntity, Collected<*>>> = listOf(
        ImportEntity.Accounts to accounts,
        ImportEntity.Transactions to transactions,
        ImportEntity.Categories to categories,
        ImportEntity.Labels to labels,
        ImportEntity.Budgets to budgets,
        ImportEntity.Recurring to recurring,
        ImportEntity.Templates to templates,
        ImportEntity.Rules to rules,
        ImportEntity.Goals to goals,
        ImportEntity.GoalContributions to goalContributions,
        ImportEntity.People to people,
        ImportEntity.DebtEntries to debtEntries,
        ImportEntity.NetWorthSnapshots to netWorthSnapshots,
        ImportEntity.Loans to loans,
        ImportEntity.LoanPrepayments to loanPrepayments,
    )
    val counts = LinkedHashMap<ImportEntity, EntityReport>()
    for ((entity, c) in all) counts[entity] = c.report

    val anyPresent = counts.values.any { it.present } || settings != null
    if (!anyPresent) throw InvalidBackupException()

    val allIssues = all.flatMap { it.second.issues }
    val issues = allIssues.take(MAX_REPORTED_ISSUES).toMutableList()
    if (allIssues.size > issues.size) issues += "…and ${allIssues.size - issues.size} more"

    // Referential checks. Only meaningful when the file actually carries the other side —
    // when merging, the missing row may already exist locally, so these stay warnings.
    val warnings = ArrayList<String>()
    val accountRows = accounts.rows?.map { it.account }

    if (accountRows != null && transactions.rows != null) {
        val ids = accountRows.mapTo(HashSet()) { it.id }
        val orphans = transactions.rows.count { t -> t.accountId !in ids || (t.toAccountId != null && t.toAccountId !in ids) }
        if (orphans > 0) {
            warnings += "$orphans transaction${plural(orphans, "", "s")} reference an account that is not in this file"
        }
    }

    if (accountRows != null && recurring.rows != null) {
        val ids = accountRows.mapTo(HashSet()) { it.id }
        val orphans = recurring.rows.count { r -> r.accountId !in ids || (r.toAccountId != null && r.toAccountId !in ids) }
        if (orphans > 0) {
            warnings += "$orphans recurring rule${plural(orphans, "", "s")} reference an account that is not in this file — they will not generate transactions"
        }
    }

    if (categories.rows != null && budgets.rows != null) {
        val ids = categories.rows.mapTo(HashSet()) { it.id }
        val orphans = budgets.rows.count { b -> b.categoryId != "" && b.categoryId !in ids }
        if (orphans > 0) {
            warnings += "$orphans budget${plural(orphans, "", "s")} reference a category that is not in this file"
        }
    }

    if (categories.rows != null && rules.rows != null) {
        val ids = categories.rows.mapTo(HashSet()) { it.id }
        val orphans = rules.rows.count { r -> r.categoryId !in ids }
        if (orphans > 0) {
            warnings += "$orphans categorization rule${plural(orphans, "", "s")} reference a category that is not in this file"
        }
    }

    if (labels.rows != null && budgets.rows != null) {
        val ids = labels.rows.mapTo(HashSet()) { it.id }
        val orphans = budgets.rows.count { b -> b.labelId != null && b.labelId !in ids }
        if (orphans > 0) {
            warnings += "$orphans budget${plural(orphans, "", "s")} reference a label that is not in this file"
        }
    }

    val missingOpening = accounts.rows?.count { it.openingBalance == null } ?: 0
    if (missingOpening > 0) {
        warnings += "$missingOpening account${plural(missingOpening, "", "s")} have no opening balance — it will be derived from the imported transactions"
    }

    if (goals.rows != null && goalContributions.rows != null) {
        val ids = goals.rows.mapTo(HashSet()) { it.id }
        val orphans = goalContributions.rows.count { c -> c.goalId !in ids }
        if (orphans > 0) {
            warnings += "$orphans goal contribution${plural(orphans, "", "s")} reference a goal that is not in this file"
        }
    }

    if (goals.rows != null && recurring.rows != null) {
        val ids = goals.rows.mapTo(HashSet()) { it.id }
        val orphans = recurring.rows.count { r -> r.goalId != null && r.goalId !in ids }
        if (orphans > 0) {
            warnings += "$orphans recurring rule${plural(orphans, "", "s")} link to a goal that is not in this file — they will stop auto-funding it"
        }
    }

    if (people.rows != null && debtEntries.rows != null) {
        val ids = people.rows.mapTo(HashSet()) { it.id }
        val orphans = debtEntries.rows.count { e -> e.personId !in ids }
        if (orphans > 0) {
            warnings += "$orphans debt entr${plural(orphans, "y", "ies")} reference a person that is not in this file"
        }
    }

    if (accountRows != null) {
        val ids = accountRows.mapTo(HashSet()) { it.id }
        val orphans = accountRows.count { a -> a.deposit != null && a.deposit.linkedAccountId !in ids }
        if (orphans > 0) {
            warnings += "$orphans deposit${plural(orphans, "", "s")} pay out to an account that is not in this file"
        }
    }

    if (accountRows != null && loans.rows != null) {
        val ids = accountRows.mapTo(HashSet()) { it.id }
        val orphans = loans.rows.count { l -> l.accountId !in ids }
        if (orphans > 0) {
            warnings += "$orphans loan${plural(orphans, "", "s")} reference an account that is not in this file"
        }
    }

    if (categories.rows != null && loans.rows != null) {
        val ids = categories.rows.mapTo(HashSet()) { it.id }
        val orphans = loans.rows.count { l -> l.categoryId !in ids }
        if (orphans > 0) {
            warnings += "$orphans loan${plural(orphans, "", "s")} reference a category that is not in this file"
        }
    }

    if (loans.rows != null && loanPrepayments.rows != null) {
        val ids = loans.rows.mapTo(HashSet()) { it.id }
        val orphans = loanPrepayments.rows.count { p -> p.loanId !in ids }
        if (orphans > 0) {
            warnings += "$orphans loan prepayment${plural(orphans, "", "s")} reference a loan that is not in this file"
        }
    }

    val meta = readBackupMeta(raw)
    val version = meta.version
    if (version != null && version > BACKUP_SCHEMA_VERSION) {
        warnings += "This backup was made by a newer version of Finio (format v$version; this app understands v$BACKUP_SCHEMA_VERSION). It will import, but some data may be missed"
    }

    return ValidatedBackup(
        meta = meta,
        data = ImportPayload(
            accounts = accountRows,
            transactions = transactions.rows,
            categories = categories.rows,
            labels = labels.rows,
            budgets = budgets.rows,
            recurring = recurring.rows,
            templates = templates.rows,
            rules = rules.rows,
            goals = goals.rows,
            goalContributions = goalContributions.rows,
            people = people.rows,
            debtEntries = debtEntries.rows,
            netWorthSnapshots = netWorthSnapshots.rows,
            loans = loans.rows,
            loanPrepayments = loanPrepayments.rows,
            settings = settings,
            accountsMissingOpeningBalance = accounts.rows
                ?.filter { it.openingBalance == null }
                ?.mapTo(LinkedHashSet()) { it.account.id }
                ?: emptySet(),
        ),
        report = ImportReport(counts, settings != null, issues, warnings),
    )
}
