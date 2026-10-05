package com.slowatcoding.finio.core.calc

import com.slowatcoding.finio.core.model.Transaction
import com.slowatcoding.finio.core.model.TransactionType
import com.slowatcoding.finio.core.money.roundMoney
import com.slowatcoding.finio.core.util.jsTrim

// Port of web/src/utils/merchants.ts — "spending by merchant" without a schema change. Groups
// on normalizeNote() (the same key subscription detection uses), so notes that differ only by
// digits/punctuation collapse; the display name is the group's most common raw note, ties broken
// by recency. A view, not an entity: there is no merchant id anywhere.
//
// The web sorts dates with `localeCompare`; ordinal comparison agrees for ISO strings of the same
// shape, which is all the store ever writes.

/** Expense or income — a transfer has no merchant. */
data class MerchantSummary(
    /** The normalizeNote() grouping key. */
    val key: String,
    /** The group's most common raw note. */
    val displayName: String,
    val type: TransactionType,
    val totalAmount: Double,
    val transactionCount: Int,
    /** ISO date of the most recent transaction in the group. */
    val lastDate: String,
    /** Newest first. */
    val transactions: List<Transaction>,
)

private class NoteCount(var count: Int, var lastDate: String)

private fun pickDisplayName(transactions: List<Transaction>): String {
    val counts = LinkedHashMap<String, NoteCount>()
    for (t in transactions) {
        val raw = jsTrim(t.note)
        val entry = counts[raw]
        if (entry != null) {
            entry.count += 1
            if (t.date > entry.lastDate) entry.lastDate = t.date
        } else {
            counts[raw] = NoteCount(1, t.date)
        }
    }

    var best = ""
    var bestCount = -1
    var bestDate = ""
    for ((raw, entry) in counts) {
        if (entry.count > bestCount || (entry.count == bestCount && entry.lastDate > bestDate)) {
            best = raw
            bestCount = entry.count
            bestDate = entry.lastDate
        }
    }
    return best
}

/**
 * Group expense or income transactions into merchants. Transfers and blank notes are excluded;
 * splits count by their total `amount`. [type] must be expense or income.
 */
fun summarizeMerchants(transactions: List<Transaction>, type: TransactionType = TransactionType.Expense): List<MerchantSummary> {
    require(type != TransactionType.Transfer) { "a transfer has no merchant" }
    val groups = LinkedHashMap<String, MutableList<Transaction>>()
    for (t in transactions) {
        if (t.type != type) continue
        val key = normalizeNote(t.note)
        if (key.isEmpty()) continue
        groups.getOrPut(key) { mutableListOf() }.add(t)
    }

    val summaries = groups.map { (key, rows) ->
        val sorted = rows.sortedWith { a, b -> b.date.compareTo(a.date) }
        MerchantSummary(
            key = key,
            displayName = pickDisplayName(rows),
            type = type,
            totalAmount = roundMoney(rows.fold(0.0) { sum, t -> sum + t.amount }),
            transactionCount = rows.size,
            lastDate = sorted[0].date,
            transactions = sorted,
        )
    }

    return summaries.sortedWith { a, b -> b.totalAmount.compareTo(a.totalAmount) }
}

/** The [n] biggest merchants by total amount — for a compact "top merchants" card. */
fun topMerchants(transactions: List<Transaction>, n: Int, type: TransactionType = TransactionType.Expense): List<MerchantSummary> {
    val all = summarizeMerchants(transactions, type)
    // JS `slice(0, n)`: a negative n counts from the end.
    return if (n >= 0) all.take(n) else all.take(maxOf(0, all.size + n))
}
