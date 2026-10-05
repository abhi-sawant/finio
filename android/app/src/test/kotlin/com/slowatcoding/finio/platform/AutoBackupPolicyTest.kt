package com.slowatcoding.finio.platform

import com.slowatcoding.finio.platform.backup.shouldRunCloudBackup
import com.slowatcoding.finio.platform.backup.shouldRunLocalBackup
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class AutoBackupPolicyTest {
    @Test fun localRunsOncePerDayWhenEnabledWithData() {
        assertTrue(shouldRunLocalBackup(true, true, null, "2026-10-05"))
        assertTrue(shouldRunLocalBackup(true, true, "2026-10-04", "2026-10-05"))
        assertFalse(shouldRunLocalBackup(true, true, "2026-10-05", "2026-10-05"))
        assertFalse(shouldRunLocalBackup(false, true, null, "2026-10-05"))
        assertFalse(shouldRunLocalBackup(true, false, null, "2026-10-05"))
    }

    private val now = Instant.parse("2026-10-05T12:00:00.000Z").toEpochMilli()

    @Test fun cloudNeedsTokenUnlockedAndData() {
        assertFalse(shouldRunCloudBackup(false, false, true, null, now))
        assertFalse(shouldRunCloudBackup(true, true, true, null, now))
        assertFalse(shouldRunCloudBackup(true, false, false, null, now))
        assertTrue(shouldRunCloudBackup(true, false, true, null, now))
    }

    @Test fun cloudWaitsTwentyFourHours() {
        assertFalse(shouldRunCloudBackup(true, false, true, "2026-10-04T12:00:00.001Z", now))
        assertTrue(shouldRunCloudBackup(true, false, true, "2026-10-04T12:00:00.000Z", now))
        assertTrue(shouldRunCloudBackup(true, false, true, "2026-09-01T00:00:00.000Z", now))
    }

    @Test fun unparseableLastBackupNeverUploadsLikeTheWebNaN() {
        assertFalse(shouldRunCloudBackup(true, false, true, "garbage", now))
    }
}
