package com.slowatcoding.finio.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

// Port of web/src/utils/chartTable.test.ts (the cases the golden grid doesn't already pin).
class ChartTableTest {
    @Test fun returnsTheSameListWhenItFits() {
        val items = listOf(1, 2, 3)
        val result = sampleForTable(items, 24)
        assertSame(items, result.rows)
        assertEquals(false, result.sampled)
    }

    @Test fun samplesInOrderWithoutRepeats() {
        val rows = sampleForTable(List(100) { it }, 10).rows
        assertEquals(rows.sorted(), rows)
        assertEquals(rows.size, rows.toSet().size)
        assertTrue(rows.first() == 0 && rows.last() == 99)
    }

    @Test fun degenerateCaps() {
        assertEquals(SampledRows(listOf(1), true), sampleForTable(listOf(1, 2, 3), 1))
        assertEquals(SampledRows(emptyList<Int>(), false), sampleForTable(emptyList<Int>(), 0))
    }
}
