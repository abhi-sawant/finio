package com.slowatcoding.finio.core.store

import com.slowatcoding.finio.core.data.NEW_DEFAULT_CATEGORY_IDS
import com.slowatcoding.finio.core.data.defaultCategories
import com.slowatcoding.finio.core.data.defaultSettings
import com.slowatcoding.finio.core.js.nowInstant
import com.slowatcoding.finio.core.js.toIso
import com.slowatcoding.finio.core.model.Account
import com.slowatcoding.finio.core.model.FinanceState
import com.slowatcoding.finio.core.model.FinioJson
import com.slowatcoding.finio.core.model.Settings
import com.slowatcoding.finio.core.model.TransactionType
import com.slowatcoding.finio.core.money.roundMoney
import com.slowatcoding.finio.core.period.normalizeMonthStartDay
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.serializer
import java.time.Instant

// The persisted envelope of the finance store and the port of the Zustand `persist` options in
// web/src/store/useFinanceStore.ts (name `finio-storage`, version 17, the cumulative `migrate`).
// Zustand stores `{ state, version }`; an older version runs through every migration step in
// order, then the result is shallow-merged over the initial state (`{...initial, ...migrated}`).

/** The store's persisted schema version — bump together with a new step in [migrateFinance]. */
const val FINANCE_STORE_VERSION = 17

@Serializable
data class PersistedFinance(val state: FinanceState, val version: Int = FINANCE_STORE_VERSION)

/** The JSON the app layer writes on every state change. */
fun encodePersisted(state: FinanceState): String = FinioJson.encodeToString(PersistedFinance.serializer(), PersistedFinance(state))

/**
 * Read a persisted envelope (ours or a web `finio-storage` value), migrating older versions.
 * Unparseable input yields the fresh-install state, as Zustand keeps the initial state when
 * storage can't be read. [now] stamps `onboardedAt` in the v6 step.
 */
fun decodePersisted(json: String, now: Instant = nowInstant()): FinanceState {
    val root = runCatching { FinioJson.parseToJsonElement(json) }.getOrNull() as? JsonObject
        ?: return initialFinanceState()
    val raw = root["state"] as? JsonObject ?: JsonObject(emptyMap())
    // Zustand only migrates when the stored version is a number different from the current one.
    val version = (root["version"] as? JsonPrimitive)?.takeIf { !it.isString }?.doubleOrNull
    val migrated = if (version != null && version != FINANCE_STORE_VERSION.toDouble()) {
        migrateFinance(raw, version.toInt(), now)
    } else raw
    return financeStateFromJson(migrated)
}

private val defaultSettingsJson: JsonObject by lazy { FinioJson.encodeToJsonElement(defaultSettings).jsonObject }

private fun isNullish(e: JsonElement?) = e == null || e is JsonNull

private fun spread(vararg parts: Map<String, JsonElement>): LinkedHashMap<String, JsonElement> =
    LinkedHashMap<String, JsonElement>().apply { parts.forEach { putAll(it) } }

private fun dropLegacyCurrency(value: JsonElement): JsonElement =
    if (value is JsonObject) JsonObject(value.filterKeys { it != "currency" }) else value

/**
 * The TS `migrate(persistedState, version)` on the raw persisted state. Steps are cumulative: a
 * v1 state falls through every branch in order. Pure apart from [now].
 */
fun migrateFinance(persisted: JsonObject, version: Int, now: Instant = nowInstant()): JsonObject {
    val s = LinkedHashMap<String, JsonElement>(persisted)
    fun settings(): JsonObject = s["settings"] as? JsonObject ?: JsonObject(emptyMap())
    fun arrayOrEmpty(key: String) {
        s[key] = s[key] as? JsonArray ?: JsonArray(emptyList())
    }
    fun mapArray(key: String, f: (JsonObject) -> JsonObject) {
        val arr = s[key] as? JsonArray ?: return
        s[key] = JsonArray(arr.map { if (it is JsonObject) f(it) else it })
    }
    fun withDefault(o: Map<String, JsonElement>, key: String, fallback: JsonElement): JsonElement =
        o[key].takeUnless { isNullish(it) } ?: fallback

    if (version < 2) {
        arrayOrEmpty("budgets")
        arrayOrEmpty("recurring")
    }

    if (version < 3) {
        s["lastLocalBackupAt"] = JsonNull
        s["settings"] = JsonObject(spread(defaultSettingsJson, settings(), mapOf("autoLocalBackup" to JsonPrimitive(false))))
    }

    if (version < 4) {
        // Multi-currency removed — the app is INR-only.
        s["settings"] = dropLegacyCurrency(JsonObject(spread(defaultSettingsJson, settings())))
        (s["accounts"] as? JsonArray)?.let { arr -> s["accounts"] = JsonArray(arr.map(::dropLegacyCurrency)) }
    }

    if (version < 5) {
        // Seed openingBalance from the stored balance minus the transactions that produced it.
        (s["accounts"] as? JsonArray)?.let { arr ->
            val deltas = sumTransactionDeltas(lenientBalanceTxs(s["transactions"]))
            s["accounts"] = JsonArray(
                arr.map { account ->
                    if (account !is JsonObject) return@map account
                    if (finiteNumber(account["openingBalance"]) != null) return@map account
                    val balance = finiteNumber(account["balance"]) ?: 0.0
                    val id = (account["id"] as? JsonPrimitive)?.takeIf { it.isString }?.content
                    val opening = roundMoney(balance - (deltas[id] ?: 0.0))
                    JsonObject(spread(account, mapOf("openingBalance" to JsonPrimitive(opening))))
                },
            )
        }
    }

    if (version < 6) {
        // Anyone with persisted state has been using the app — keep the first-run wizard away.
        val st = settings()
        s["settings"] = JsonObject(
            spread(defaultSettingsJson, st, mapOf("onboardedAt" to withDefault(st, "onboardedAt", JsonPrimitive(now.toIso())))),
        )
    }

    if (version < 7) {
        val st = settings()
        s["settings"] = JsonObject(
            spread(defaultSettingsJson, st, mapOf("monthStartDay" to JsonPrimitive(normalizeMonthStartDay(st["monthStartDay"])))),
        )
        mapArray("budgets") { b ->
            JsonObject(
                spread(
                    b,
                    mapOf(
                        "period" to withDefault(b, "period", JsonPrimitive("monthly")),
                        "rollover" to withDefault(b, "rollover", JsonPrimitive(false)),
                    ),
                ),
            )
        }
        mapArray("recurring") { r ->
            JsonObject(spread(r, mapOf("occurrenceCount" to withDefault(r, "occurrenceCount", JsonPrimitive(0)))))
        }
    }

    if (version < 8) {
        val st = settings()
        s["settings"] = JsonObject(
            spread(defaultSettingsJson, st, mapOf("hideAmounts" to withDefault(st, "hideAmounts", JsonPrimitive(false)))),
        )
        arrayOrEmpty("templates")
    }

    if (version < 9) {
        arrayOrEmpty("goals")
        arrayOrEmpty("goalContributions")
    }

    if (version < 10) {
        arrayOrEmpty("people")
        arrayOrEmpty("debtEntries")
    }

    if (version < 11) arrayOrEmpty("rules")

    if (version < 12) arrayOrEmpty("netWorthSnapshots")

    if (version < 13) {
        // Reminders are new and off; the per-trigger switches default on.
        val st = settings()
        s["settings"] = JsonObject(
            spread(
                defaultSettingsJson,
                st,
                mapOf(
                    "notificationsEnabled" to withDefault(st, "notificationsEnabled", JsonPrimitive(false)),
                    "notifyBills" to withDefault(st, "notifyBills", JsonPrimitive(true)),
                    "notifyBudgets" to withDefault(st, "notifyBudgets", JsonPrimitive(true)),
                    "notifyCreditDue" to withDefault(st, "notifyCreditDue", JsonPrimitive(true)),
                    "notifyLeadDays" to withDefault(st, "notifyLeadDays", JsonPrimitive(2)),
                ),
            ),
        )
    }

    if (version < 14) appendMissingDefaultCategories(s, NEW_DEFAULT_CATEGORY_IDS.toSet())

    if (version < 15) {
        arrayOrEmpty("loans")
        arrayOrEmpty("loanPrepayments")
    }

    if (version < 16) appendMissingDefaultCategories(s, setOf("cat-35", "cat-36"))

    if (version < 17) {
        // AMOLED dark mode is new and off — an upgrade never changes how dark mode looks.
        val st = settings()
        s["settings"] = JsonObject(
            spread(defaultSettingsJson, st, mapOf("amoledDark" to withDefault(st, "amoledDark", JsonPrimitive(false)))),
        )
    }

    return JsonObject(s)
}

/** Append the default categories in [ids] that the stored list lacks (only if it is a list). */
private fun appendMissingDefaultCategories(s: MutableMap<String, JsonElement>, ids: Set<String>) {
    val arr = s["categories"] as? JsonArray ?: return
    val existing = arr.mapNotNull { ((it as? JsonObject)?.get("id") as? JsonPrimitive)?.content }.toSet()
    val missing = defaultCategories.filter { it.id in ids && it.id !in existing }
    s["categories"] = JsonArray(arr + missing.map { FinioJson.encodeToJsonElement(it) })
}

private fun finiteNumber(e: JsonElement?): Double? =
    (e as? JsonPrimitive)?.takeIf { !it.isString && it !is JsonNull }?.doubleOrNull?.takeIf { it.isFinite() }

/** The balance-relevant fields of whatever is stored, skipping rows a JS `typeof` check would skip. */
private fun lenientBalanceTxs(raw: JsonElement?): List<BalanceTx> {
    val arr = raw as? JsonArray ?: return emptyList()
    return arr.mapNotNull { row ->
        val o = row as? JsonObject ?: return@mapNotNull null
        val amount = finiteNumber(o["amount"]) ?: return@mapNotNull null
        val type = when ((o["type"] as? JsonPrimitive)?.content) {
            "expense" -> TransactionType.Expense
            "income" -> TransactionType.Income
            "transfer" -> TransactionType.Transfer
            else -> return@mapNotNull null
        }
        fun str(key: String) = (o[key] as? JsonPrimitive)?.takeIf { it.isString }?.content
        BalanceTx(type, str("accountId") ?: "", str("toAccountId")?.takeIf { it.isNotEmpty() }, amount)
    }
}

/**
 * `{...initialState, ...migrated}` decoded into the typed state. Each key is decoded on its own
 * and a list row that doesn't decode is dropped, so one bad row can't wipe a whole install. An
 * account still lacking an opening balance is backfilled from its balance (the v5 rule).
 */
fun financeStateFromJson(raw: JsonObject): FinanceState {
    val initial = initialFinanceState()
    fun <T> list(key: String, serializer: KSerializer<T>, fallback: List<T>): List<T> {
        val arr = raw[key] as? JsonArray ?: return fallback
        return arr.mapNotNull { runCatching { FinioJson.decodeFromJsonElement(serializer, it) }.getOrNull() }
    }
    val transactions = list("transactions", serializer(), initial.transactions)
    return FinanceState(
        accounts = decodeAccounts(raw["accounts"], transactions.map { it.balanceTx() }) ?: initial.accounts,
        transactions = transactions,
        categories = list("categories", serializer(), initial.categories),
        labels = list("labels", serializer(), initial.labels),
        budgets = list("budgets", serializer(), initial.budgets),
        recurring = list("recurring", serializer(), initial.recurring),
        templates = list("templates", serializer(), initial.templates),
        rules = list("rules", serializer(), initial.rules),
        goals = list("goals", serializer(), initial.goals),
        goalContributions = list("goalContributions", serializer(), initial.goalContributions),
        people = list("people", serializer(), initial.people),
        debtEntries = list("debtEntries", serializer(), initial.debtEntries),
        netWorthSnapshots = list("netWorthSnapshots", serializer(), initial.netWorthSnapshots),
        loans = list("loans", serializer(), initial.loans),
        loanPrepayments = list("loanPrepayments", serializer(), initial.loanPrepayments),
        settings = (raw["settings"] as? JsonObject)?.let {
            runCatching { FinioJson.decodeFromJsonElement(Settings.serializer(), it) }.getOrNull()
        } ?: initial.settings,
        lastLocalBackupAt = (raw["lastLocalBackupAt"] as? JsonPrimitive)?.takeIf { it.isString }?.content,
    )
}

private fun decodeAccounts(raw: JsonElement?, transactions: List<BalanceTx>): List<Account>? {
    val arr = raw as? JsonArray ?: return null
    val imported = arr.mapNotNull { row ->
        val o = row as? JsonObject ?: return@mapNotNull null
        val opening = finiteNumber(o["openingBalance"])
        val patched = if (opening == null) JsonObject(spread(o, mapOf("openingBalance" to JsonPrimitive(0.0)))) else o
        val account = runCatching { FinioJson.decodeFromJsonElement(Account.serializer(), patched) }.getOrNull()
            ?: return@mapNotNull null
        ImportedAccount(account, opening)
    }
    return backfillOpeningBalances(imported, transactions)
}
