package com.slowatcoding.finio.core.calc

import com.slowatcoding.finio.core.golden.Golden
import com.slowatcoding.finio.core.golden.i
import com.slowatcoding.finio.core.model.Account
import com.slowatcoding.finio.core.model.NetWorthSnapshot
import com.slowatcoding.finio.core.model.Transaction
import com.slowatcoding.finio.core.period.json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.Test

class NetWorthGoldenTest {
    private val ledger = FixtureLedger("netWorth")

    private fun NetWorthComponents.json(): JsonElement = buildJsonObject {
        put("assets", JsonPrimitive(assets))
        put("liabilities", JsonPrimitive(liabilities))
        put("netWorth", JsonPrimitive(netWorth))
    }

    private fun JsonObject.intOrNull(k: String): Int? = (this[k] as? JsonPrimitive)?.content?.toDouble()?.toInt()
    private inline fun <reified T> JsonObject.field(k: String): T = Golden.decode(ledger.resolve(getValue(k)))

    @Test
    fun matchesTypeScript() = Golden.verify("netWorth") { c ->
        when (c.fn) {
            "ledger" -> c.out
            "constants" -> buildJsonObject {
                put("DEFAULT_NET_WORTH_MONTHS", JsonPrimitive(DEFAULT_NET_WORTH_MONTHS))
                put("MAX_SNAPSHOT_BACKFILL", JsonPrimitive(MAX_SNAPSHOT_BACKFILL))
            }
            "netWorthComponents" -> netWorthComponents(ledger.decodeArg(c, 0)).json()
            "snapshotPeriodKey" -> JsonPrimitive(snapshotPeriodKey(c.arg(0).instantArg, c.arg(1).i))
            "accountBalancesAt" -> buildJsonObject {
                accountBalancesAt(ledger.decodeArg(c, 0), ledger.decodeArg(c, 1), c.arg(2).instantArg)
                    .forEach { (k, v) -> put(k, JsonPrimitive(v)) }
            }
            "netWorthAt" -> netWorthAt(ledger.decodeArg(c, 0), ledger.decodeArg(c, 1), c.arg(2).instantArg).json()
            "buildNetWorthSeries" -> c.arg(0).jsonObject.let { o ->
                val input = NetWorthSeriesInput(
                    accounts = o.field<List<Account>>("accounts"),
                    transactions = o.field<List<Transaction>>("transactions"),
                    snapshots = o.field<List<NetWorthSnapshot>>("snapshots"),
                    now = o["now"]?.instantArg,
                    monthStartDay = o.intOrNull("monthStartDay"),
                    months = o.intOrNull("months"),
                )
                JsonArray(
                    buildNetWorthSeries(input).map { p ->
                        buildJsonObject {
                            put("key", JsonPrimitive(p.key))
                            put("label", JsonPrimitive(p.label))
                            put("shortLabel", JsonPrimitive(p.shortLabel))
                            put("date", p.date.json())
                            put("assets", JsonPrimitive(p.assets))
                            put("liabilities", JsonPrimitive(p.liabilities))
                            put("netWorth", JsonPrimitive(p.netWorth))
                            put("source", JsonPrimitive(p.source.wire))
                            put("isCurrent", JsonPrimitive(p.isCurrent))
                        }
                    },
                )
            }
            "planNetWorthSnapshots" -> c.arg(0).jsonObject.let { o ->
                val input = SnapshotPlanInput(
                    accounts = o.field<List<Account>>("accounts"),
                    transactions = o.field<List<Transaction>>("transactions"),
                    snapshots = o.field<List<NetWorthSnapshot>>("snapshots"),
                    now = o["now"]?.instantArg,
                    monthStartDay = o.intOrNull("monthStartDay"),
                    maxBackfill = o.intOrNull("maxBackfill"),
                )
                JsonArray(
                    planNetWorthSnapshots(input).map { s ->
                        buildJsonObject {
                            put("periodKey", JsonPrimitive(s.periodKey))
                            put("date", JsonPrimitive(s.date))
                            put("assets", JsonPrimitive(s.assets))
                            put("liabilities", JsonPrimitive(s.liabilities))
                        }
                    },
                )
            }
            else -> null
        }
    }
}
