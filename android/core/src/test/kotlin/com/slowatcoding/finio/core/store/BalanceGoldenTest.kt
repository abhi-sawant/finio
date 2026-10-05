package com.slowatcoding.finio.core.store

import com.slowatcoding.finio.core.golden.Golden
import com.slowatcoding.finio.core.golden.d
import com.slowatcoding.finio.core.golden.i
import com.slowatcoding.finio.core.model.Account
import com.slowatcoding.finio.core.model.TransactionType
import com.slowatcoding.finio.core.model.wireName
import com.slowatcoding.finio.core.money.roundMoney
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.junit.Test

class BalanceGoldenTest {
    private fun tx(e: JsonElement) = e.jsonObject.let {
        BalanceTx(Golden.decode<TransactionType>(it.getValue("type")), it.getValue("accountId").jsonPrimitive.content,
            it["toAccountId"]?.jsonPrimitive?.content, it.getValue("amount").d)
    }
    private fun txs(e: JsonElement) = e.jsonArray.map(::tx)
    private fun imported(e: JsonElement) = e.jsonArray.map {
        val o = it.jsonObject
        val opening = o["openingBalance"]?.d
        val acc = Golden.decode<Account>(buildJsonObject { o.forEach { (k, v) -> put(k, v) }; if (opening == null) put("openingBalance", 0) })
        ImportedAccount(acc, opening)
    }

    @Test
    fun matchesTypeScript() = Golden.verify("balance") { c ->
        when (c.fn) {
            "roundMoney" -> JsonPrimitive(roundMoney(c.arg(0).d))
            "applyBalanceDelta" -> Golden.encode(applyBalanceDelta(Golden.decode(c.arg(0)), tx(c.arg(1)), c.arg(2).i))
            "sumTransactionDeltas" -> buildJsonObject { sumTransactionDeltas(txs(c.arg(0))).forEach { (k, v) -> put(k, v) } }
            "backfillOpeningBalances" -> Golden.encode(backfillOpeningBalances(imported(c.arg(0)), txs(c.arg(1))))
            "recomputeAccountBalances" -> Golden.encode(recomputeAccountBalances(imported(c.arg(0)), txs(c.arg(1))))
            "reconciliationAdjustment" -> reconciliationAdjustment(c.arg(0).d, c.arg(1).d).let {
                buildJsonObject { put("type", it.type?.let { t -> JsonPrimitive(wireName(t)) } ?: JsonNull); put("amount", it.amount) }
            }
            "diffBalances" -> diffBalances(Golden.decode(c.arg(0)), Golden.decode(c.arg(1))).let {
                buildJsonObject { put("changed", it.changed); put("totalDrift", it.totalDrift) }
            }
            else -> null
        }
    }
}
