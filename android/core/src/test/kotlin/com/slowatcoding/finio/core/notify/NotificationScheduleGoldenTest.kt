package com.slowatcoding.finio.core.notify

import com.slowatcoding.finio.core.calc.FixtureLedger
import com.slowatcoding.finio.core.calc.instantArg
import com.slowatcoding.finio.core.golden.Golden
import com.slowatcoding.finio.core.golden.i
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.Test

class NotificationScheduleGoldenTest {
    private val ledger = FixtureLedger("notificationSchedule")

    private inline fun <reified T> JsonObject.field(k: String): T = Golden.decode(ledger.resolve(getValue(k)))

    @Test
    fun matchesTypeScript() = Golden.verify("notificationSchedule") { c ->
        when (c.fn) {
            "ledger" -> c.out
            "buildNotificationSchedule" -> {
                val o = c.arg(0).jsonObject
                val input = NotificationScheduleInput(
                    recurring = o.field("recurring"),
                    budgets = o.field("budgets"),
                    transactions = o.field("transactions"),
                    accounts = o.field("accounts"),
                    categories = o.field("categories"),
                    labels = o.field("labels"),
                    monthStartDay = o.getValue("monthStartDay").i,
                    prefs = o.field("prefs"),
                )
                JsonArray(buildNotificationSchedule(input, c.arg(1).instantArg).map { Golden.encode(it) })
            }
            else -> null
        }
    }
}
