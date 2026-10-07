package com.slowatcoding.finio.core.store

import com.slowatcoding.finio.core.js.addDays
import com.slowatcoding.finio.core.js.addMonths
import com.slowatcoding.finio.core.js.addWeeks
import com.slowatcoding.finio.core.js.addYears
import com.slowatcoding.finio.core.js.isAfter
import com.slowatcoding.finio.core.js.parseIso
import com.slowatcoding.finio.core.js.toIso
import com.slowatcoding.finio.core.model.RecurrenceFrequency
import com.slowatcoding.finio.core.model.RecurringTransaction
import com.slowatcoding.finio.core.model.TransactionType
import java.time.Instant
import kotlin.math.max
import kotlin.math.min

// Port of web/src/store/recurring.ts — the pure recurring planner behind processRecurring().

/** Safety cap on how many occurrences a single rule may generate in one pass. */
const val MAX_OCCURRENCES_PER_RULE = 365

/** Upper bound on the walk in [lastOccurrenceOnOrBefore] — ~55 years of a daily rule. */
private const val MAX_SCAN_STEPS = 20_000

fun nextOccurrence(date: Instant, freq: RecurrenceFrequency): Instant = when (freq) {
    RecurrenceFrequency.Daily -> addDays(date, 1)
    RecurrenceFrequency.Weekly -> addWeeks(date, 1)
    RecurrenceFrequency.Monthly -> addMonths(date, 1)
    RecurrenceFrequency.Yearly -> addYears(date, 1)
}

private fun parseDate(value: String?): Instant? = if (value.isNullOrEmpty()) null else parseIso(value)

fun isRulePaused(rule: RecurringTransaction): Boolean = !rule.pausedAt.isNullOrEmpty()

/**
 * How many occurrences the rule may still generate before hitting `maxOccurrences`.
 * `Double.POSITIVE_INFINITY` when the rule has no limit — the TS returns `Infinity` too.
 */
fun remainingOccurrences(rule: RecurringTransaction): Double {
    val maxOcc = rule.maxOccurrences ?: return Double.POSITIVE_INFINITY
    return max(0, maxOcc - rule.occurrenceCount).toDouble()
}

/**
 * The next date this rule is scheduled to fire, or null when it never will again — its
 * occurrence count is used up, its end date has passed, or its dates don't parse. Paused rules
 * still report a date; pausing is a state the UI shows, not a schedule change.
 */
fun nextDueDate(rule: RecurringTransaction): Instant? {
    if (remainingOccurrences(rule) <= 0) return null
    val last = parseDate(rule.lastRunDate)
    val next = (if (last != null) nextOccurrence(last, rule.frequency) else parseDate(rule.startDate)) ?: return null
    val end = parseDate(rule.endDate)
    if (end != null && isAfter(next, end)) return null
    return next
}

/** A rule that will never fire again — exhausted, ended, or with unusable dates. */
fun isRuleFinished(rule: RecurringTransaction): Boolean = nextDueDate(rule) == null

/**
 * The most recent occurrence on or before [now], following the rule's own cadence from its start
 * date. Used to start a rule "from today" without generating its history.
 */
fun lastOccurrenceOnOrBefore(startDate: String, frequency: RecurrenceFrequency, now: Instant): Instant? {
    val start = parseDate(startDate)
    if (start == null || isAfter(start, now)) return null
    var last: Instant = start
    repeat(MAX_SCAN_STEPS) {
        val next = nextOccurrence(last, frequency)
        if (isAfter(next, now)) return last
        last = next
    }
    return last
}

fun lastOccurrenceOnOrBefore(rule: RecurringTransaction, now: Instant): Instant? =
    lastOccurrenceOnOrBefore(rule.startDate, rule.frequency, now)

/**
 * Every date this rule will fire strictly after [from] and no later than [to] — a projection,
 * nothing is generated. A paused rule yields nothing; `endDate` / `maxOccurrences` bound the
 * walk; already-due-but-ungenerated occurrences consume the allowance without being returned.
 */
fun futureOccurrences(
    rule: RecurringTransaction,
    from: Instant,
    to: Instant,
    cap: Int = MAX_OCCURRENCES_PER_RULE,
): List<Instant> {
    if (isRulePaused(rule)) return emptyList()
    val end = parseDate(rule.endDate)
    val dates = mutableListOf<Instant>()
    var remaining = remainingOccurrences(rule)
    var cursor = nextDueDate(rule)

    for (step in 0 until MAX_SCAN_STEPS) {
        val c = cursor
        if (c == null || remaining <= 0 || dates.size >= cap) break
        if (isAfter(c, to)) break
        if (end != null && isAfter(c, end)) break
        remaining -= 1
        if (isAfter(c, from)) dates += c
        cursor = nextOccurrence(c, rule.frequency)
    }
    return dates
}

data class PlannedOccurrence(val rule: RecurringTransaction, val date: Instant)

data class RecurringPlan(
    /** Due occurrences, in rule order then chronological order within a rule. */
    val occurrences: List<PlannedOccurrence>,
    /** The rule list with `lastRunDate` and `occurrenceCount` advanced for anything generated. */
    val rules: List<RecurringTransaction>,
    /** Rule ids that hit MAX_OCCURRENCES_PER_RULE and will continue on the next pass. */
    val cappedRuleIds: List<String>,
)

/**
 * Pure planner for `processRecurring`. Rules are skipped when paused, when their account (or a
 * transfer's destination) no longer exists, past their end date, and once their occurrence limit
 * is used up. The per-rule cap stops one long-overdue daily rule starving the others.
 */
fun planRecurring(rules: List<RecurringTransaction>, knownAccountIds: Iterable<String>, now: Instant): RecurringPlan {
    val accountIds = knownAccountIds.toSet()
    val occurrences = mutableListOf<PlannedOccurrence>()
    val cappedRuleIds = mutableListOf<String>()

    val nextRules = rules.map { rule ->
        if (isRulePaused(rule)) return@map rule
        if (rule.accountId !in accountIds) return@map rule
        if (rule.type == TransactionType.Transfer && (rule.toAccountId.isNullOrEmpty() || rule.toAccountId !in accountIds)) {
            return@map rule
        }

        val allowance = min(remainingOccurrences(rule), MAX_OCCURRENCES_PER_RULE.toDouble())
        if (allowance <= 0) return@map rule

        val last = parseDate(rule.lastRunDate)
        var next = (if (last != null) nextOccurrence(last, rule.frequency) else parseDate(rule.startDate))
            ?: return@map rule

        val end = parseDate(rule.endDate)
        var lastRun: Instant? = null
        var ruleGenerated = 0

        while (!isAfter(next, now) && ruleGenerated < allowance && !(end != null && isAfter(next, end))) {
            occurrences += PlannedOccurrence(rule, next)
            ruleGenerated += 1
            lastRun = next
            next = nextOccurrence(next, rule.frequency)
        }

        if (ruleGenerated == MAX_OCCURRENCES_PER_RULE && !isAfter(next, now) && !(end != null && isAfter(next, end))) {
            cappedRuleIds += rule.id
        }

        if (lastRun != null) {
            rule.copy(lastRunDate = lastRun.toIso(), occurrenceCount = rule.occurrenceCount + ruleGenerated)
        } else {
            rule
        }
    }

    return RecurringPlan(occurrences, nextRules, cappedRuleIds)
}

/** What creating (or editing) a rule would immediately generate. */
data class BackfillPreview(
    val count: Int,
    val total: Double,
    val firstDate: Instant?,
    val lastDate: Instant?,
    /** True when the cap was hit and a second pass would generate still more. */
    val capped: Boolean,
)

fun previewBackfill(rule: RecurringTransaction, knownAccountIds: Iterable<String>, now: Instant): BackfillPreview {
    val plan = planRecurring(listOf(rule), knownAccountIds, now)
    val dates = plan.occurrences.map { it.date }
    return BackfillPreview(
        count = dates.size,
        total = dates.size * rule.amount,
        firstDate = dates.firstOrNull(),
        lastDate = dates.lastOrNull(),
        capped = plan.cappedRuleIds.isNotEmpty(),
    )
}
