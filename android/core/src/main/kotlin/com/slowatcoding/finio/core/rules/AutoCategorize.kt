package com.slowatcoding.finio.core.rules

import com.slowatcoding.finio.core.model.CategoryRule
import com.slowatcoding.finio.core.model.RuleMatchType
import com.slowatcoding.finio.core.model.RuleScope
import com.slowatcoding.finio.core.model.Transaction
import com.slowatcoding.finio.core.model.TransactionCategorization
import com.slowatcoding.finio.core.model.TransactionType

// Port of web/src/utils/autoCategorize.ts. "Note contains Uber → Transport + Essential" as pure
// functions of (rules, transaction-ish input). Two invariants hold everywhere:
//  - first match wins: array order is priority; disabled rules are skipped entirely,
//  - rules never touch a transfer or a split.
// A `regex` rule is a *JavaScript* regex with the `i` flag — see JsRegex.kt for how its syntax
// and semantics are reproduced (an invalid pattern matches nothing rather than throwing).

data class MatchTypeOption(val value: RuleMatchType, val label: String)

val MATCH_TYPES: List<MatchTypeOption> = listOf(
    MatchTypeOption(RuleMatchType.Contains, "contains"),
    MatchTypeOption(RuleMatchType.StartsWith, "starts with"),
    MatchTypeOption(RuleMatchType.EndsWith, "ends with"),
    MatchTypeOption(RuleMatchType.Equals, "is exactly"),
    MatchTypeOption(RuleMatchType.Regex, "matches regex"),
)

val MATCH_TYPE_LABELS: Map<RuleMatchType, String> = MATCH_TYPES.associate { it.value to it.label }

/** Never throws; a blank pattern is invalid, and a regex must be valid *JS* syntax. */
fun isValidPattern(pattern: String, matchType: RuleMatchType): Boolean {
    if (jsTrim(pattern) == "") return false
    if (matchType != RuleMatchType.Regex) return true
    return JsRegex.isValid(pattern, "i")
}

private typealias Matcher = (String) -> Boolean

/** Compile a rule's pattern once, so a replay over thousands of rows doesn't rebuild it per row. */
private fun compilePattern(pattern: String, matchType: RuleMatchType): Matcher {
    if (matchType == RuleMatchType.Regex) {
        val re = JsRegex.compileOrNull(pattern, "i") ?: return { false }
        return { note -> re.test(note) }
    }
    val needle = jsTrim(pattern).lowercase()
    if (needle.isEmpty()) return { false }
    return when (matchType) {
        RuleMatchType.StartsWith -> { note -> note.lowercase().startsWith(needle) }
        RuleMatchType.EndsWith -> { note -> note.lowercase().endsWith(needle) }
        RuleMatchType.Equals -> { note -> jsTrim(note).lowercase() == needle }
        else -> { note -> note.lowercase().contains(needle) }
    }
}

private fun scopeAllows(rule: CategoryRule, type: TransactionType): Boolean {
    if (type == TransactionType.Transfer) return false
    return rule.scope == RuleScope.Any || rule.scope.wire == type.wire
}

/** Whether a single rule fires for this note and transaction type. */
fun ruleMatches(rule: CategoryRule, note: String, type: TransactionType): Boolean {
    if (!rule.enabled) return false
    if (!scopeAllows(rule, type)) return false
    return compilePattern(rule.pattern, rule.matchType)(note)
}

/** The first enabled rule that fires, or null. */
fun findMatchingRule(rules: List<CategoryRule>, note: String, type: TransactionType): CategoryRule? {
    if (jsTrim(note).isEmpty()) return null
    return rules.firstOrNull { ruleMatches(it, note, type) }
}

/** A rule's labels are additive — it tags a transaction, it doesn't replace the user's tags. */
fun mergeLabels(existing: List<String>, ruleLabels: List<String>): List<String> {
    val next = existing.toMutableList()
    for (id in ruleLabels) if (id !in next) next += id
    return next
}

data class RuleApplication(
    val transactionId: String,
    val rule: CategoryRule,
    /** The row exactly as it is now, so the caller can undo the whole pass. */
    val before: TransactionCategorization,
    val after: TransactionCategorization,
)

data class ReplayOptions(
    /** Only rewrite transactions currently sitting in this category. */
    val restrictToCategoryId: String? = null,
)

/**
 * What replaying the rules over existing history would change, without changing it. Only rows
 * that would actually move are returned, so the count is honest.
 */
fun planRuleApplication(
    transactions: List<Transaction>,
    rules: List<CategoryRule>,
    options: ReplayOptions = ReplayOptions(),
): List<RuleApplication> {
    val active = rules.filter { it.enabled && isValidPattern(it.pattern, it.matchType) }
    if (active.isEmpty()) return emptyList()

    val compiled = active.map { it to compilePattern(it.pattern, it.matchType) }
    val applications = mutableListOf<RuleApplication>()

    for (t in transactions) {
        if (t.type == TransactionType.Transfer) continue
        if (!t.splits.isNullOrEmpty()) continue
        if (jsTrim(t.note).isEmpty()) continue
        if (options.restrictToCategoryId != null && t.categoryId != options.restrictToCategoryId) continue

        val hit = compiled.firstOrNull { (rule, matches) -> scopeAllows(rule, t.type) && matches(t.note) }?.first
            ?: continue

        val labels = mergeLabels(t.labels, hit.labelIds)
        val categoryChanged = t.categoryId != hit.categoryId
        val labelsChanged = labels.size != t.labels.size
        if (!categoryChanged && !labelsChanged) continue

        applications += RuleApplication(
            transactionId = t.id,
            rule = hit,
            before = TransactionCategorization(t.id, t.categoryId, t.labels, t.splits),
            after = TransactionCategorization(t.id, hit.categoryId, labels, t.splits),
        )
    }
    return applications
}
