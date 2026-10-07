package com.slowatcoding.finio.core.calc

import com.slowatcoding.finio.core.golden.Golden
import com.slowatcoding.finio.core.golden.i
import com.slowatcoding.finio.core.model.TransactionType
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.junit.Test

class MerchantsGoldenTest {
    private val ledger = FixtureLedger("merchants")

    private fun MerchantSummary.json(): JsonElement = buildJsonObject {
        put("key", JsonPrimitive(key))
        put("displayName", JsonPrimitive(displayName))
        put("type", JsonPrimitive(type.wire))
        put("totalAmount", JsonPrimitive(totalAmount))
        put("transactionCount", JsonPrimitive(transactionCount))
        put("lastDate", JsonPrimitive(lastDate))
        put("transactions", ids(transactions))
    }

    @Test
    fun matchesTypeScript() = Golden.verify("merchants") { c ->
        when (c.fn) {
            "ledger" -> c.out
            "summarizeMerchants" -> JsonArray(
                summarizeMerchants(ledger.decodeArg(c, 0), Golden.decode<TransactionType>(c.arg(1))).map { it.json() },
            )
            "topMerchants" -> JsonArray(
                topMerchants(ledger.decodeArg(c, 0), c.arg(1).i, Golden.decode<TransactionType>(c.arg(2))).map { it.json() },
            )
            else -> null
        }
    }
}
