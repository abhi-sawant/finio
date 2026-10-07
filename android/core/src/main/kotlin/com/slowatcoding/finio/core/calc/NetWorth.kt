package com.slowatcoding.finio.core.calc

import com.slowatcoding.finio.core.js.format
import com.slowatcoding.finio.core.js.nowInstant
import com.slowatcoding.finio.core.js.parseIso
import com.slowatcoding.finio.core.js.toIso
import com.slowatcoding.finio.core.model.Account
import com.slowatcoding.finio.core.model.NetWorthSnapshot
import com.slowatcoding.finio.core.model.Transaction
import com.slowatcoding.finio.core.money.roundMoney
import com.slowatcoding.finio.core.period.DEFAULT_MONTH_START_DAY
import com.slowatcoding.finio.core.period.PeriodType
import com.slowatcoding.finio.core.period.periodLabel
import com.slowatcoding.finio.core.period.periodRange
import com.slowatcoding.finio.core.period.shiftPeriod
import com.slowatcoding.finio.core.store.balanceTx
import com.slowatcoding.finio.core.store.sumTransactionDeltas
import java.time.Instant
import kotlin.math.max

// Port of web/src/utils/netWorth.ts — net worth over time.
//
// Balances are derived, so any past value can be reconstructed by walking today's balances
// backwards through today's transactions — exact until history changes. Completed financial
// months are therefore read from a NetWorthSnapshot frozen when they closed; anything without a
// snapshot (the current month, pre-snapshot history) is reconstructed and marked as such.

/** How many financial months the chart covers by default. */
const val DEFAULT_NET_WORTH_MONTHS = 12

/** Cap on how many missing past months one capture pass may backfill. */
const val MAX_SNAPSHOT_BACKFILL = 12

data class NetWorthComponents(
    /** Sum of every positive balance across open accounts. */
    val assets: Double,
    /** Sum of every negative balance as a positive number — credit outstanding. */
    val liabilities: Double,
    /** `assets - liabilities`, identical to `getNetWorth`. */
    val netWorth: Double,
)

private fun componentsFromBalances(balances: Collection<Double>): NetWorthComponents {
    var assets = 0.0
    var liabilities = 0.0
    for (balance in balances) {
        if (balance > 0) assets = roundMoney(assets + balance)
        else liabilities = roundMoney(liabilities - balance)
    }
    return NetWorthComponents(assets, liabilities, roundMoney(assets - liabilities))
}

/** Split today's live balances into assets and liabilities. Archived accounts are excluded. */
fun netWorthComponents(accounts: List<Account>): NetWorthComponents =
    componentsFromBalances(activeAccounts(accounts).map { it.balance })

/** The identity of the financial month containing [date] — `yyyy-MM` of the month's start. */
fun snapshotPeriodKey(date: Instant, monthStartDay: Int = DEFAULT_MONTH_START_DAY): String =
    format(periodRange(PeriodType.Monthly, date, monthStartDay).start, "yyyy-MM")

/** Each open account's balance at the end of [asOf], by reversing everything recorded after it. */
fun accountBalancesAt(accounts: List<Account>, transactions: List<Transaction>, asOf: Instant): LinkedHashMap<String, Double> {
    val cutoff = asOf.toEpochMilli()
    val later = transactions.filter { t -> parseIso(t.date)?.let { it.toEpochMilli() > cutoff } ?: false }
    val deltas = sumTransactionDeltas(later.map { it.balanceTx() })

    val out = LinkedHashMap<String, Double>()
    for (a in activeAccounts(accounts)) out[a.id] = roundMoney(a.balance - (deltas[a.id] ?: 0.0))
    return out
}

/** Reconstructed assets/liabilities/net worth at the end of [asOf]. */
fun netWorthAt(accounts: List<Account>, transactions: List<Transaction>, asOf: Instant): NetWorthComponents =
    componentsFromBalances(accountBalancesAt(accounts, transactions, asOf).values)

enum class NetWorthSource(val wire: String) { Snapshot("snapshot"), Reconstructed("reconstructed") }

data class NetWorthPoint(
    /** `yyyy-MM` of the financial month's start. */
    val key: String,
    val label: String,
    /** Axis/table label (`MMM yy`) of the financial month's start. */
    val shortLabel: String,
    /** The instant the figures are as of — the period's end, or now for the live period. */
    val date: Instant,
    val assets: Double,
    val liabilities: Double,
    val netWorth: Double,
    /** Reconstructed points move if history is edited. */
    val source: NetWorthSource,
    /** True for the month still in progress. */
    val isCurrent: Boolean,
)

data class NetWorthSeriesInput(
    val accounts: List<Account>,
    val transactions: List<Transaction>,
    val snapshots: List<NetWorthSnapshot>,
    val now: Instant? = null,
    val monthStartDay: Int? = null,
    /** Number of financial months to include, ending with the one in progress. */
    val months: Int? = null,
)

/** The net-worth trend, oldest first, ending with the (always live) month in progress. */
fun buildNetWorthSeries(input: NetWorthSeriesInput): List<NetWorthPoint> {
    val now = input.now ?: nowInstant()
    val monthStartDay = input.monthStartDay ?: DEFAULT_MONTH_START_DAY
    val months = max(1, input.months ?: DEFAULT_NET_WORTH_MONTHS)
    val current = periodRange(PeriodType.Monthly, now, monthStartDay)
    val byKey = input.snapshots.associateBy { it.periodKey }

    val points = mutableListOf<NetWorthPoint>()
    for (i in months - 1 downTo 0) {
        val range = if (i == 0) current else shiftPeriod(current, -i)
        val key = format(range.start, "yyyy-MM")
        val label = periodLabel(range, monthStartDay)
        val shortLabel = format(range.start, "MMM yy")
        val isCurrent = i == 0

        val snapshot = if (isCurrent) null else byKey[key]
        if (snapshot != null) {
            points += NetWorthPoint(
                key = key,
                label = label,
                shortLabel = shortLabel,
                // The web yields an Invalid Date for an unparseable snapshot date; fall back to
                // the period end rather than carry a non-date.
                date = parseIso(snapshot.date) ?: range.end,
                assets = snapshot.assets,
                liabilities = snapshot.liabilities,
                netWorth = roundMoney(snapshot.assets - snapshot.liabilities),
                source = NetWorthSource.Snapshot,
                isCurrent = isCurrent,
            )
            continue
        }

        // The live month is "as of now", not as of a period end that hasn't arrived.
        val asOf = if (isCurrent) now else range.end
        val c = netWorthAt(input.accounts, input.transactions, asOf)
        points += NetWorthPoint(key, label, shortLabel, asOf, c.assets, c.liabilities, c.netWorth, NetWorthSource.Reconstructed, isCurrent)
    }
    return points
}

data class SnapshotPlanInput(
    val accounts: List<Account>,
    val transactions: List<Transaction>,
    val snapshots: List<NetWorthSnapshot>,
    val now: Instant? = null,
    val monthStartDay: Int? = null,
    /** How many missing past months this pass may fill in. */
    val maxBackfill: Int? = null,
)

/** A snapshot with its store-assigned fields (`id`, `createdAt`) still to come. */
data class PlannedSnapshot(
    val periodKey: String,
    val date: String,
    val assets: Double,
    val liabilities: Double,
)

/**
 * Snapshots missing for completed financial months, oldest first. Only closed months are
 * captured, and the walk stops once it reaches back past the oldest transaction.
 */
fun planNetWorthSnapshots(input: SnapshotPlanInput): List<PlannedSnapshot> {
    val now = input.now ?: nowInstant()
    val monthStartDay = input.monthStartDay ?: DEFAULT_MONTH_START_DAY
    val maxBackfill = max(0, input.maxBackfill ?: MAX_SNAPSHOT_BACKFILL)
    if (maxBackfill == 0 || input.accounts.isEmpty()) return emptyList()

    val existing = input.snapshots.map { it.periodKey }.toSet()
    val earliest = earliestTransactionTime(input.transactions) ?: return emptyList()

    val current = periodRange(PeriodType.Monthly, now, monthStartDay)
    val planned = mutableListOf<PlannedSnapshot>()

    for (i in 1..maxBackfill) {
        val range = shiftPeriod(current, -i)
        // Nothing had happened yet, so there is no meaningful net worth to freeze.
        if (range.end.toEpochMilli() < earliest) break

        val key = format(range.start, "yyyy-MM")
        if (key in existing) continue

        val c = netWorthAt(input.accounts, input.transactions, range.end)
        planned += PlannedSnapshot(key, range.end.toIso(), c.assets, c.liabilities)
    }
    return planned.reversed()
}

private fun earliestTransactionTime(transactions: List<Transaction>): Long? {
    var earliest: Long? = null
    for (t in transactions) {
        val time = parseIso(t.date)?.toEpochMilli() ?: continue
        if (earliest == null || time < earliest) earliest = time
    }
    return earliest
}
