package com.slowatcoding.finio.core.backup

import com.slowatcoding.finio.core.model.FinanceState
import com.slowatcoding.finio.core.model.FinioJson
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement

// Port of `collectBackupPayload` in web/src/services/backup.ts — the single source of truth for
// "what's in a backup" (spec/backup-format.md §1). File export wraps it in `withBackupMeta`;
// cloud upload sends it raw or encrypted. App-lock, backup-crypto, auth and
// `lastLocalBackupAt` are never part of it.

/** The 16 payload keys, in the order backup.ts writes them. */
val BACKUP_PAYLOAD_KEYS: List<String> = listOf(
    "accounts", "transactions", "categories", "labels", "budgets", "recurring", "templates", "rules",
    "goals", "goalContributions", "people", "debtEntries", "netWorthSnapshots", "loans", "loanPrepayments", "settings",
)

fun collectBackupPayload(state: FinanceState): JsonObject {
    val out = LinkedHashMap<String, JsonElement>()
    out["accounts"] = FinioJson.encodeToJsonElement(state.accounts)
    out["transactions"] = FinioJson.encodeToJsonElement(state.transactions)
    out["categories"] = FinioJson.encodeToJsonElement(state.categories)
    out["labels"] = FinioJson.encodeToJsonElement(state.labels)
    out["budgets"] = FinioJson.encodeToJsonElement(state.budgets)
    // `lastRunDate` is the one optional written as an explicit null (spec §1) — FinioJson omits nulls.
    out["recurring"] = JsonArray(
        FinioJson.encodeToJsonElement(state.recurring).let { it as JsonArray }.map { rule ->
            rule as JsonObject
            if ("lastRunDate" in rule) rule else JsonObject(rule + ("lastRunDate" to JsonNull))
        },
    )
    out["templates"] = FinioJson.encodeToJsonElement(state.templates)
    out["rules"] = FinioJson.encodeToJsonElement(state.rules)
    out["goals"] = FinioJson.encodeToJsonElement(state.goals)
    out["goalContributions"] = FinioJson.encodeToJsonElement(state.goalContributions)
    out["people"] = FinioJson.encodeToJsonElement(state.people)
    out["debtEntries"] = FinioJson.encodeToJsonElement(state.debtEntries)
    out["netWorthSnapshots"] = FinioJson.encodeToJsonElement(state.netWorthSnapshots)
    out["loans"] = FinioJson.encodeToJsonElement(state.loans)
    out["loanPrepayments"] = FinioJson.encodeToJsonElement(state.loanPrepayments)
    out["settings"] = FinioJson.encodeToJsonElement(state.settings)
    return JsonObject(out)
}
