package com.slowatcoding.finio.core.rules

import com.slowatcoding.finio.core.golden.Golden
import com.slowatcoding.finio.core.golden.s
import com.slowatcoding.finio.core.model.CategoryRule
import com.slowatcoding.finio.core.model.RuleMatchType
import com.slowatcoding.finio.core.model.RuleScope
import com.slowatcoding.finio.core.model.TransactionCategorization
import com.slowatcoding.finio.core.model.TransactionType
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoCategorizeGoldenTest {
    private fun categorization(c: TransactionCategorization): JsonElement = buildJsonObject {
        put("id", c.id)
        put("categoryId", c.categoryId)
        putJsonArray("labels") { c.labels.forEach { add(it) } }
        c.splits?.let { put("splits", Golden.encode(it)) }
    }

    @Test
    fun matchesTypeScript() = Golden.verify("autoCategorize") { c ->
        when (c.fn) {
            "constants" -> buildJsonObject {
                putJsonArray("MATCH_TYPES") {
                    MATCH_TYPES.forEach { add(buildJsonObject { put("value", it.value.wire); put("label", it.label) }) }
                }
                putJsonObject("MATCH_TYPE_LABELS") { MATCH_TYPE_LABELS.forEach { (k, v) -> put(k.wire, v) } }
            }
            "isValidPattern" -> JsonPrimitive(isValidPattern(c.arg(0).s, Golden.decode(c.arg(1))))
            "ruleMatches" -> JsonPrimitive(ruleMatches(Golden.decode(c.arg(0)), c.arg(1).s, Golden.decode(c.arg(2))))
            "findMatchingRule" ->
                findMatchingRule(Golden.decode(c.arg(0)), c.arg(1).s, Golden.decode(c.arg(2)))?.let { Golden.encode(it) } ?: JsonNull
            "mergeLabels" -> buildJsonArray {
                mergeLabels(c.arg(0).jsonArray.map { it.s }, c.arg(1).jsonArray.map { it.s }).forEach { add(it) }
            }
            "planRuleApplication" -> {
                val opts = c.arg(2).let { if (it is JsonNull) ReplayOptions() else ReplayOptions(it.jsonObject["restrictToCategoryId"]?.s) }
                buildJsonArray {
                    planRuleApplication(Golden.decode(c.arg(0)), Golden.decode(c.arg(1)), opts).forEach { a ->
                        add(buildJsonObject {
                            put("transactionId", a.transactionId)
                            put("rule", Golden.encode(a.rule))
                            put("before", categorization(a.before))
                            put("after", categorization(a.after))
                        })
                    }
                }
            }
            else -> null
        }
    }

    private fun rule(pattern: String, matchType: RuleMatchType = RuleMatchType.Contains, scope: RuleScope = RuleScope.Any, id: String = "r") =
        CategoryRule(id, pattern, matchType, scope, "cat-transport", emptyList(), true, "2026-01-01T00:00:00.000Z")

    @Test
    fun firstMatchWinsAndTransfersNeverMatch() {
        val rules = listOf(rule("uber eats", id = "r1"), rule("uber", id = "r2"))
        assertEquals("r1", findMatchingRule(rules, "Uber Eats order", TransactionType.Expense)?.id)
        assertEquals("r2", findMatchingRule(rules, "Uber ride", TransactionType.Expense)?.id)
        assertNull(findMatchingRule(rules, "uber", TransactionType.Transfer))
    }

    @Test
    fun jsRegexSyntaxIsJsNotJava() {
        assertTrue(isValidPattern("a{", RuleMatchType.Regex)) // Java: illegal repetition
        assertTrue(isValidPattern("\\Q", RuleMatchType.Regex)) // Java: quote start
        assertFalse(isValidPattern("(?i)abc", RuleMatchType.Regex)) // Java: inline flag
        assertFalse(isValidPattern("a++", RuleMatchType.Regex)) // Java: possessive
        assertFalse(ruleMatches(rule("([", RuleMatchType.Regex), "([", TransactionType.Expense))
        assertFalse(ruleMatches(rule("uber$", RuleMatchType.Regex), "uber\n", TransactionType.Expense)) // Java `$`
        assertTrue(ruleMatches(rule("\\bola", RuleMatchType.Regex), "éola", TransactionType.Expense)) // ASCII \b
    }
}
