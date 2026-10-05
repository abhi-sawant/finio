package com.slowatcoding.finio.core.util

import com.slowatcoding.finio.core.golden.Golden
import com.slowatcoding.finio.core.golden.i
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.junit.Test

class ChartTableGoldenTest {
    @Test
    fun matchesTypeScript() = Golden.verify("chartTable") { c ->
        when (c.fn) {
            // args = [n, max?]: the items are the indices 0 until n.
            "sampleForTable" -> {
                val items = List(c.arg(0).i) { it }
                val r = if (c.args.size > 1) sampleForTable(items, c.arg(1).i) else sampleForTable(items)
                buildJsonObject {
                    put("rows", JsonArray(r.rows.map { JsonPrimitive(it) }))
                    put("sampled", JsonPrimitive(r.sampled))
                }
            }
            else -> null
        }
    }
}
