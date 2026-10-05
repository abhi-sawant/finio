package com.slowatcoding.finio.core.store

import com.slowatcoding.finio.core.backup.collectBackupPayload
import com.slowatcoding.finio.core.backup.withBackupMeta
import com.slowatcoding.finio.core.data.loadSampleData
import com.slowatcoding.finio.core.golden.Golden
import com.slowatcoding.finio.core.golden.b
import com.slowatcoding.finio.core.golden.d
import com.slowatcoding.finio.core.golden.s
import com.slowatcoding.finio.core.importing.validateBackup
import com.slowatcoding.finio.core.model.Account
import com.slowatcoding.finio.core.model.DebtEntry
import com.slowatcoding.finio.core.model.FinanceState
import com.slowatcoding.finio.core.model.FinioJson
import com.slowatcoding.finio.core.model.GoalContribution
import com.slowatcoding.finio.core.model.ImportMode
import com.slowatcoding.finio.core.model.ImportPayload
import com.slowatcoding.finio.core.model.LoanPrepayment
import com.slowatcoding.finio.core.model.Settings
import com.slowatcoding.finio.core.model.Transaction
import com.slowatcoding.finio.core.model.TransactionCategorization
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.serializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

/**
 * Replays spec/fixtures/financeStore.json — a scripted action sequence recorded against the real
 * Zustand store — and asserts every return value and every state change match. Also round-trips
 * the shared sample backup through the store.
 */
class FinanceStoreGoldenTest {
    private val stateKeys = listOf(
        "accounts", "transactions", "categories", "labels", "budgets", "recurring", "templates", "rules",
        "goals", "goalContributions", "people", "debtEntries", "netWorthSnapshots", "loans", "loanPrepayments",
        "settings", "lastLocalBackupAt",
    )

    private fun encodeState(state: FinanceState): JsonObject = FinioJson.encodeToJsonElement(state).jsonObject

    @Test
    fun scriptedActionsMatchTheWebStore() {
        var now = Instant.EPOCH
        var counter = 0
        val store = FinanceStore(
            clock = { now },
            idGen = { "f5000000-0000-4000-8000-" + (++counter).toString().padStart(12, '0') },
        )
        var previous: JsonObject? = null

        Golden.verify("financeStore") { c ->
            if (c.fn == "decodePersisted") {
                return@verify encodeState(decodePersisted(c.arg(0).s, Instant.parse(c.arg(1).s)))
            }
            now = Instant.parse(c.arg(0).s)
            val a = c.args.drop(1)
            val ret = dispatch(store, c.fn, a)
            val state = encodeState(store.current)
            val expectedChanged = (c.out as JsonObject)["changed"]?.jsonObject ?: JsonObject(emptyMap())
            val changed = LinkedHashMap<String, JsonElement>()
            for (key in stateKeys) {
                val value = state[key] ?: JsonNull
                val before = previous?.get(key) ?: JsonNull
                if (key in expectedChanged || previous == null || Golden.diff(before, value, key) != null) changed[key] = value
            }
            previous = state
            buildJsonObject {
                put("ret", ret)
                put("changed", JsonObject(changed))
            }
        }
    }

    private inline fun <reified T> dec(e: JsonElement): T = FinioJson.decodeFromJsonElement(e)

    /** Zustand's `{ ...target, ...updates }` over the JSON form of a typed row. */
    private fun <T> patch(value: T, serializer: KSerializer<T>, updates: JsonElement): T {
        val base = FinioJson.encodeToJsonElement(serializer, value).jsonObject
        return FinioJson.decodeFromJsonElement(serializer, JsonObject(base + updates.jsonObject))
    }

    private fun strings(e: JsonElement): List<String> = e.jsonArray.map { it.s }

    private fun dispatch(store: FinanceStore, fn: String, a: List<JsonElement>): JsonElement {
        fun <T> encode(v: T, serializer: KSerializer<T>): JsonElement = FinioJson.encodeToJsonElement(serializer, v)
        fun str(v: String?): JsonElement = v?.let(::JsonPrimitive) ?: JsonNull
        return when (fn) {
            "setState" -> {
                val merged = JsonObject(encodeState(store.current) + a[0].jsonObject)
                store.setState { dec<FinanceState>(merged) }
                JsonNull
            }
            "addAccount" -> str(store.addAccount(dec(a[0])))
            "updateAccount" -> JsonNull.also { store.updateAccount(a[0].s) { patch(it, serializer(), a[1]) } }
            "setAccountArchived" -> JsonNull.also { store.setAccountArchived(a[0].s, a[1].b) }
            "deleteAccount" -> JsonPrimitive(store.deleteAccount(a[0].s))
            "addDeposit" -> str(store.addDeposit(dec(a[0])))
            "updateDeposit" -> JsonNull.also { store.updateDeposit(a[0].s, dec(a[1])) }
            "processMaturities" -> encode(store.processMaturities(), ListSerializer(Transaction.serializer()))
            "recomputeBalances" -> store.recomputeBalances().let {
                buildJsonObject { put("changed", it.changed); put("totalDrift", it.totalDrift) }
            }
            "addTransaction" -> str(store.addTransaction(dec(a[0])))
            "updateTransaction" -> JsonNull.also { store.updateTransaction(a[0].s) { patch(it, serializer(), a[1]) } }
            "deleteTransaction" -> store.deleteTransaction(a[0].s)?.let { encode(it, Transaction.serializer()) } ?: JsonNull
            "restoreTransaction" -> JsonNull.also { store.restoreTransaction(dec(a[0])) }
            "bulkDeleteTransactions" -> encode(store.bulkDeleteTransactions(strings(a[0])), ListSerializer(Transaction.serializer()))
            "restoreTransactions" -> JsonNull.also { store.restoreTransactions(dec(a[0])) }
            "bulkRecategorize" -> JsonPrimitive(store.bulkRecategorize(strings(a[0]), a[1].s))
            "bulkAddLabel" -> JsonNull.also { store.bulkAddLabel(strings(a[0]), a[1].s) }
            "bulkAddTransactions" -> JsonPrimitive(store.bulkAddTransactions(dec(a[0])))
            "addCategory" -> JsonNull.also { store.addCategory(dec(a[0])) }
            "updateCategory" -> JsonNull.also { store.updateCategory(a[0].s) { patch(it, serializer(), a[1]) } }
            "deleteCategory" -> JsonNull.also { store.deleteCategory(a[0].s) }
            "addLabel" -> JsonNull.also { store.addLabel(dec(a[0])) }
            "updateLabel" -> JsonNull.also { store.updateLabel(a[0].s) { patch(it, serializer(), a[1]) } }
            "deleteLabel" -> JsonNull.also { store.deleteLabel(a[0].s) }
            "addBudget" -> JsonNull.also { store.addBudget(dec(a[0])) }
            "updateBudget" -> JsonNull.also { store.updateBudget(a[0].s) { patch(it, serializer(), a[1]) } }
            "deleteBudget" -> JsonNull.also { store.deleteBudget(a[0].s) }
            "addRecurring" -> str(store.addRecurring(dec(a[0])))
            "updateRecurring" -> JsonNull.also { store.updateRecurring(a[0].s) { patch(it, serializer(), a[1]) } }
            "setRecurringPaused" -> JsonNull.also { store.setRecurringPaused(a[0].s, a[1].b) }
            "processRecurring" -> encode(store.processRecurring(), ListSerializer(Transaction.serializer()))
            "addTemplate" -> str(store.addTemplate(dec(a[0])))
            "deleteTemplate" -> JsonNull.also { store.deleteTemplate(a[0].s) }
            "addRule" -> str(store.addRule(dec(a[0])))
            "updateRule" -> JsonNull.also { store.updateRule(a[0].s) { patch(it, serializer(), a[1]) } }
            "moveRule" -> JsonNull.also { store.moveRule(a[0].s, if (a[1].s == "up") MoveDirection.Up else MoveDirection.Down) }
            "applyRulesToExisting" -> {
                val restrict = (a.getOrNull(0) as? JsonObject)?.get("restrictToCategoryId")?.s
                val result = store.applyRulesToExisting(restrict)
                buildJsonObject {
                    put("changed", result.changed)
                    put("previous", JsonArray(result.previous.map(::encodeCategorization)))
                }
            }
            "restoreCategorization" -> JsonNull.also { store.restoreCategorization(a[0].jsonArray.map(::decodeCategorization)) }
            "addGoal" -> str(store.addGoal(dec(a[0])))
            "updateGoal" -> JsonNull.also { store.updateGoal(a[0].s) { patch(it, serializer(), a[1]) } }
            "deleteGoal" -> JsonNull.also { store.deleteGoal(a[0].s) }
            "addContribution" -> str(store.addContribution(dec(a[0])))
            "deleteContribution" -> store.deleteContribution(a[0].s)?.let { encode(it, GoalContribution.serializer()) } ?: JsonNull
            "restoreContribution" -> JsonNull.also { store.restoreContribution(dec(a[0])) }
            "addPerson" -> str(store.addPerson(dec(a[0])))
            "updatePerson" -> JsonNull.also { store.updatePerson(a[0].s) { patch(it, serializer(), a[1]) } }
            "deletePerson" -> JsonNull.also { store.deletePerson(a[0].s) }
            "addDebtEntry" -> str(store.addDebtEntry(dec(a[0])))
            "deleteDebtEntry" -> store.deleteDebtEntry(a[0].s)?.let { encode(it, DebtEntry.serializer()) } ?: JsonNull
            "updateDebtEntry" -> JsonPrimitive(store.updateDebtEntry(a[0].s, dec(a[1])))
            "restoreDebtEntry" -> JsonNull.also { store.restoreDebtEntry(dec(a[0])) }
            "settleUp" -> store.settleUp(a[0].s, a[1].d, a[2].s, a[3].s)?.let {
                buildJsonObject { put("transactionId", it.transactionId); put("entryId", it.entryId) }
            } ?: JsonNull
            "captureNetWorthSnapshots" -> JsonPrimitive(store.captureNetWorthSnapshots())
            "addLoan" -> str(
                store.addLoan(dec(a[0]), (a.getOrNull(1) as? JsonObject)?.get("logPastEmis")?.b ?: false),
            )
            "updateLoan" -> JsonNull.also { store.updateLoan(a[0].s) { patch(it, serializer(), a[1]) } }
            "setLoanClosed" -> JsonNull.also { store.setLoanClosed(a[0].s, a[1].b) }
            "deleteLoan" -> JsonNull.also { store.deleteLoan(a[0].s) }
            "addLoanPrepayment" -> str(store.addLoanPrepayment(dec(a[0])))
            "deleteLoanPrepayment" -> store.deleteLoanPrepayment(a[0].s)?.let { encode(it, LoanPrepayment.serializer()) } ?: JsonNull
            "restoreLoanPrepayment" -> JsonNull.also { store.restoreLoanPrepayment(dec(a[0])) }
            "updateSettings" -> JsonNull.also { store.updateSettings { patch(it, Settings.serializer(), a[0]) } }
            "setLastLocalBackupAt" -> JsonNull.also { store.setLastLocalBackupAt(a[0].s) }
            "resetToDefaults" -> JsonNull.also { store.resetToDefaults() }
            "importData" -> JsonNull.also {
                val mode = if ((a[1] as JsonObject)["mode"]?.s == "replace") ImportMode.Replace else ImportMode.Merge
                store.importData(importPayload(a[0].jsonObject), mode)
            }
            else -> error("no dispatch for $fn")
        }
    }

    private fun encodeCategorization(c: TransactionCategorization): JsonElement = buildJsonObject {
        put("id", c.id)
        put("categoryId", c.categoryId)
        put("labels", JsonArray(c.labels.map(::JsonPrimitive)))
        c.splits?.let { put("splits", FinioJson.encodeToJsonElement(it)) }
    }

    private fun decodeCategorization(e: JsonElement): TransactionCategorization {
        val o = e.jsonObject
        return TransactionCategorization(
            id = o.getValue("id").s,
            categoryId = o.getValue("categoryId").s,
            labels = strings(o.getValue("labels")),
            splits = o["splits"]?.takeIf { it !is JsonNull }?.let { FinioJson.decodeFromJsonElement(it) },
        )
    }

    /** A test-side stand-in for validateBackup's output: accounts without an opening balance are flagged. */
    private fun importPayload(o: JsonObject): ImportPayload {
        val missing = HashSet<String>()
        val accounts = o["accounts"]?.jsonArray?.map { row ->
            val obj = row.jsonObject
            if ("openingBalance" !in obj) {
                missing += obj.getValue("id").s
                FinioJson.decodeFromJsonElement<Account>(JsonObject(obj + ("openingBalance" to JsonPrimitive(0))))
            } else FinioJson.decodeFromJsonElement<Account>(obj)
        }
        return ImportPayload(
            accounts = accounts,
            transactions = o["transactions"]?.let { dec(it) },
            categories = o["categories"]?.let { dec(it) },
            labels = o["labels"]?.let { dec(it) },
            budgets = o["budgets"]?.let { dec(it) },
            recurring = o["recurring"]?.let { dec(it) },
            templates = o["templates"]?.let { dec(it) },
            rules = o["rules"]?.let { dec(it) },
            goals = o["goals"]?.let { dec(it) },
            goalContributions = o["goalContributions"]?.let { dec(it) },
            people = o["people"]?.let { dec(it) },
            debtEntries = o["debtEntries"]?.let { dec(it) },
            netWorthSnapshots = o["netWorthSnapshots"]?.let { dec(it) },
            loans = o["loans"]?.let { dec(it) },
            loanPrepayments = o["loanPrepayments"]?.let { dec(it) },
            settings = o["settings"]?.let { dec(it) },
            accountsMissingOpeningBalance = missing,
        )
    }

    // ---- The shared sample backup (spec/fixtures/sampleBackup.json) --------------------------

    private val sampleNow = Instant.parse("2026-06-15T12:00:00.000Z")

    private fun sampleFile(): JsonObject = Golden.cases("sampleBackup").single().out.jsonObject

    @Test
    fun loadingTheSampleThroughTheStoreWritesTheWebsBackupFile() {
        var counter = 0
        val store = FinanceStore(
            clock = { sampleNow },
            idGen = { "00000000-0000-4000-8000-" + (++counter).toString().padStart(12, '0') },
        )
        loadSampleData(store.sampleDataActions(), sampleNow)
        val file = withBackupMeta(collectBackupPayload(store.current), sampleNow)
        assertEquals(sampleFile().keys.toList(), file.keys.toList())
        assertNull(Golden.diff(sampleFile(), file, "$"))
        // `lastRunDate` is written as an explicit null, as the web does.
        val rule = file.getValue("recurring").jsonArray.first().jsonObject
        assertEquals(JsonNull, rule["lastRunDate"])
    }

    @Test
    fun restoringTheSampleBackupReproducesItsPayload() {
        val file = sampleFile()
        val validated = validateBackup(file, sampleNow)
        val store = FinanceStore(clock = { sampleNow })
        store.importData(validated.data, ImportMode.Replace)
        val payload = collectBackupPayload(store.current)
        val expected = JsonObject(file.filterKeys { it != "version" && it != "exportedAt" })
        assertNull(Golden.diff(expected, payload, "$"))
    }
}
