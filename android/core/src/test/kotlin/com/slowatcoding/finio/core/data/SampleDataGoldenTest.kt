package com.slowatcoding.finio.core.data

import com.slowatcoding.finio.core.golden.Golden
import com.slowatcoding.finio.core.js.parseJsDate
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.junit.Test

/** Fake store: ids like the TS recorder (`acc-0`, `goal-3`…), every call recorded in order. */
internal class RecordingActions : SampleDataActions {
    private var next = 0
    val calls = ArrayList<JsonElement>()
    private fun rec(fn: String, arg: JsonElement) {
        calls += buildJsonObject { put("fn", fn); put("arg", arg) }
    }
    override fun addAccount(account: NewSampleAccount): String { rec("addAccount", Golden.encode(account)); return "acc-${next++}" }
    override fun addGoal(goal: NewSampleGoal): String { rec("addGoal", Golden.encode(goal)); return "goal-${next++}" }
    override fun addPerson(person: NewSamplePerson): String { rec("addPerson", Golden.encode(person)); return "person-${next++}" }
    override fun addBudget(budget: NewSampleBudget) { rec("addBudget", Golden.encode(budget)) }
    override fun addRecurring(rule: NewSampleRecurring): String { rec("addRecurring", Golden.encode(rule)); return "recurring-${next++}" }
    override fun addContribution(contribution: NewSampleContribution): String { rec("addContribution", Golden.encode(contribution)); return "contribution-${next++}" }
    override fun addDebtEntry(entry: NewSampleDebtEntry): String { rec("addDebtEntry", Golden.encode(entry)); return "debt-${next++}" }
    override fun bulkAddTransactions(transactions: List<NewSampleTransaction>): Int {
        rec("bulkAddTransactions", Golden.encode(transactions)); return transactions.size
    }
}

class SampleDataGoldenTest {
    @Test
    fun matchesTypeScript() = Golden.verify("sampleData") { c ->
        val now = parseJsDate(c.arg(0).jsonPrimitive.content)!!
        when (c.fn) {
            "generateSampleData" -> Golden.encode(generateSampleData(now))
            "loadSampleData" -> RecordingActions().also { loadSampleData(it, now) }.calls.let(::JsonArray)
            else -> null
        }
    }
}
