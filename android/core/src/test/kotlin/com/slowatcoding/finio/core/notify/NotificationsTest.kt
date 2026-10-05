package com.slowatcoding.finio.core.notify

import com.slowatcoding.finio.core.model.FinioJson
import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationsTest {
    @Test fun serializesWithTheWebShape() {
        val n = ScheduledNotification("daily:log:2026-06-15", NotificationKind.Daily, 1_781_524_800_000, 1_781_611_200_000, "t", "b", "/add-transaction")
        val json = FinioJson.encodeToString(ScheduledNotification.serializer(), n)
        assertEquals(
            """{"id":"daily:log:2026-06-15","kind":"daily","fireAt":1781524800000,"expiresAt":1781611200000,"title":"t","body":"b","url":"/add-transaction"}""",
            json,
        )
        assertEquals(n, FinioJson.decodeFromString(ScheduledNotification.serializer(), json))
    }

    @Test fun capsAndSortsStably() {
        val now = 10_000L
        val entries = (0 until 6).map { ScheduledNotification("e$it", NotificationKind.Bill, if (it < 3) 5L else 1L, now + 1, "", "", "/") }
        assertEquals(listOf("e3", "e4", "e5"), selectDueNotifications(entries, emptySet(), now).map { it.id })
    }
}
