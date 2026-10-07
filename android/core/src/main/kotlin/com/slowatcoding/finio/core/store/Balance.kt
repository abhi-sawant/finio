package com.slowatcoding.finio.core.store

import com.slowatcoding.finio.core.model.Account
import com.slowatcoding.finio.core.model.TransactionType
import com.slowatcoding.finio.core.money.roundMoney

// Port of web/src/store/balance.ts. `Account.openingBalance` is the immutable starting point;
// `Account.balance` is a cache of `openingBalance + Σ(deltas)` — derivable, so drift is
// reconcilable.

/** The only fields of a transaction that can move an account balance. */
data class BalanceTx(
    val type: TransactionType,
    val accountId: String,
    val toAccountId: String?,
    val amount: Double,
)

fun com.slowatcoding.finio.core.model.Transaction.balanceTx() = BalanceTx(type, accountId, toAccountId, amount)

/** Apply (direction 1) or reverse (direction -1) one transaction's effect. Transfers move both sides. */
fun applyBalanceDelta(accounts: List<Account>, tx: BalanceTx, direction: Int): List<Account> =
    accounts.map { account ->
        when {
            tx.type == TransactionType.Expense && account.id == tx.accountId ->
                account.copy(balance = roundMoney(account.balance - direction * tx.amount))
            tx.type == TransactionType.Income && account.id == tx.accountId ->
                account.copy(balance = roundMoney(account.balance + direction * tx.amount))
            tx.type == TransactionType.Transfer && account.id == tx.accountId ->
                account.copy(balance = roundMoney(account.balance - direction * tx.amount))
            tx.type == TransactionType.Transfer && tx.toAccountId != null && account.id == tx.toAccountId ->
                account.copy(balance = roundMoney(account.balance + direction * tx.amount))
            else -> account
        }
    }

/** Net effect of [transactions] on each account, keyed by account id (insertion-ordered). */
fun sumTransactionDeltas(transactions: List<BalanceTx>): LinkedHashMap<String, Double> {
    val deltas = LinkedHashMap<String, Double>()
    fun add(id: String, amount: Double) { deltas[id] = (deltas[id] ?: 0.0) + amount }
    for (tx in transactions) {
        if (!tx.amount.isFinite()) continue
        when (tx.type) {
            TransactionType.Expense -> add(tx.accountId, -tx.amount)
            TransactionType.Income -> add(tx.accountId, tx.amount)
            TransactionType.Transfer -> {
                add(tx.accountId, -tx.amount)
                tx.toAccountId?.let { add(it, tx.amount) }
            }
        }
    }
    return deltas
}

/**
 * An account as read from storage or a backup, where `openingBalance` may be absent (pre-v5).
 * [openingBalance] null means "missing"; [account.openingBalance] is then ignored.
 */
data class ImportedAccount(val account: Account, val openingBalance: Double?)

private fun withOpeningBalances(accounts: List<ImportedAccount>, deltas: Map<String, Double>): List<Account> =
    accounts.map { (account, opening) ->
        if (opening != null && opening.isFinite()) account.copy(openingBalance = opening)
        else {
            val balance = if (account.balance.isFinite()) account.balance else 0.0
            account.copy(openingBalance = roundMoney(balance - (deltas[account.id] ?: 0.0)))
        }
    }

fun backfillOpeningBalances(accounts: List<ImportedAccount>, transactions: List<BalanceTx>): List<Account> =
    withOpeningBalances(accounts, sumTransactionDeltas(transactions))

/** Recompute every `balance` from `openingBalance` + transactions. The reconcile primitive. */
fun recomputeAccountBalances(accounts: List<ImportedAccount>, transactions: List<BalanceTx>): List<Account> {
    val deltas = sumTransactionDeltas(transactions)
    return withOpeningBalances(accounts, deltas).map { account ->
        val balance = roundMoney(account.openingBalance + (deltas[account.id] ?: 0.0))
        if (balance == account.balance) account else account.copy(balance = balance)
    }
}

/** Convenience for already-complete accounts. */
@JvmName("recomputeAccountBalancesComplete")
fun recomputeAccountBalances(accounts: List<Account>, transactions: List<BalanceTx>): List<Account> =
    recomputeAccountBalances(accounts.map { ImportedAccount(it, it.openingBalance) }, transactions)

/** `type` null means no gap. `amount` is always positive. */
data class ReconciliationAdjustment(val type: TransactionType?, val amount: Double)

fun reconciliationAdjustment(currentBalance: Double, statementBalance: Double): ReconciliationAdjustment {
    val gap = roundMoney(statementBalance - currentBalance)
    if (gap == 0.0) return ReconciliationAdjustment(null, 0.0)
    return ReconciliationAdjustment(if (gap > 0) TransactionType.Income else TransactionType.Expense, kotlin.math.abs(gap))
}

data class BalanceDiff(val changed: Int, val totalDrift: Double)

fun diffBalances(before: List<Account>, after: List<Account>): BalanceDiff {
    val previous = before.associate { it.id to it.balance }
    var changed = 0
    var totalDrift = 0.0
    for (account in after) {
        val old = previous[account.id] ?: continue
        if (old == account.balance) continue
        changed += 1
        totalDrift = roundMoney(totalDrift + (account.balance - old))
    }
    return BalanceDiff(changed, totalDrift)
}
