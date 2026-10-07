package com.slowatcoding.finio.platform

import com.slowatcoding.finio.platform.files.backupFileName
import com.slowatcoding.finio.platform.files.selectStaleBackups
import org.junit.Assert.assertEquals
import org.junit.Test

class BackupRotationTest {
    private fun day(i: Int) = backupFileName("2026-%02d-%02d".format(1 + i / 28, 1 + i % 28))

    @Test fun keepsTheNewestTenByDateNotByListingOrder() {
        val names = (0 until 13).map(::day).shuffled(java.util.Random(7))
        val stale = selectStaleBackups(names, keep = 10)
        assertEquals(listOf(day(2), day(1), day(0)), stale)
    }

    @Test fun neverTouchesFilesOutsideThePattern() {
        val names = listOf(
            "finio-backup-2026-10-01.json",
            "finio-backup-2026-10-02.json",
            "finio-backup-2026-10-02 (1).json",
            "my-notes.json",
            "finio-backup-2026-10-03.json.bak",
            "finio-backup-26-10-03.json",
        )
        assertEquals(listOf("finio-backup-2026-10-01.json"), selectStaleBackups(names, keep = 1))
    }

    @Test fun fewerThanKeepDeletesNothing() {
        assertEquals(emptyList<String>(), selectStaleBackups(listOf(day(0), day(1)), keep = 10))
    }

    @Test fun keepZeroDeletesEveryBackup() {
        assertEquals(2, selectStaleBackups(listOf(day(0), day(1)), keep = 0).size)
    }

    @Test fun fileNameMatchesTheWeb() {
        assertEquals("finio-backup-2026-10-05.json", backupFileName("2026-10-05"))
    }
}
