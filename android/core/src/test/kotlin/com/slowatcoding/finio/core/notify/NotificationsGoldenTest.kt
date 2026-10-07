package com.slowatcoding.finio.core.notify

import com.slowatcoding.finio.core.golden.Golden
import com.slowatcoding.finio.core.golden.d
import com.slowatcoding.finio.core.golden.s
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import org.junit.Test

class NotificationsGoldenTest {
    @Test
    fun matchesTypeScript() = Golden.verify("notifications") { c ->
        when (c.fn) {
            "constants" -> buildJsonObject {
                put("NOTIFICATION_SYNC_TAG", JsonPrimitive(NOTIFICATION_SYNC_TAG))
                put("NOTIFICATION_HORIZON_DAYS", JsonPrimitive(NOTIFICATION_HORIZON_DAYS))
                put("MAX_NOTIFY_LEAD_DAYS", JsonPrimitive(MAX_NOTIFY_LEAD_DAYS))
                put("NOTIFY_HOUR", JsonPrimitive(NOTIFY_HOUR))
                put("DAILY_LOG_HOUR", JsonPrimitive(DAILY_LOG_HOUR))
                put("MAX_NOTIFICATIONS_PER_RUN", JsonPrimitive(MAX_NOTIFICATIONS_PER_RUN))
            }
            "selectDueNotifications" -> Golden.encode(
                selectDueNotifications(
                    Golden.decode<List<ScheduledNotification>>(c.arg(0)),
                    c.arg(1).jsonArray.map { it.s }.toSet(),
                    c.arg(2).d.toLong(),
                ),
            )
            else -> null
        }
    }
}
