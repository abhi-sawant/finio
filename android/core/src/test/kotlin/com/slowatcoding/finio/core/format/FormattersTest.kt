package com.slowatcoding.finio.core.format

import com.slowatcoding.finio.core.js.localDate
import org.junit.Assert.assertEquals
import org.junit.Test

// Behaviour the golden grid can't carry: JSON has no -0, NaN or Infinity, and formatDate reads
// the clock in TS (here it takes `now`). Expected strings checked against Node 24's Intl.
class FormattersTest {
    @Test fun negativeZeroAndTinyNegativesKeepTheirSign() {
        assertEquals("-₹0", formatCurrency(-0.0))
        assertEquals("-₹0", formatCurrency(-0.001, precise = false))
        assertEquals("-₹0", formatCurrency(-0.04, compact = true, forceCompact = true))
        // …but the hidden mask only shows a minus for a value that is really < 0.
        assertEquals("₹••••", formatCurrency(-0.0, hidden = true))
    }

    @Test fun nonFiniteValues() {
        assertEquals("₹NaN", formatCurrency(Double.NaN))
        assertEquals("₹∞", formatCurrency(Double.POSITIVE_INFINITY))
        assertEquals("-₹∞", formatCurrency(Double.NEGATIVE_INFINITY, compact = true))
        assertEquals("₹••••", formatCurrency(Double.NaN, hidden = true))
    }

    @Test fun compactSuffixesAndReRounding() {
        assertEquals("₹2.3L", formatCurrency(230_000.0, compact = true))
        assertEquals("₹90K", formatCurrency(90_010.0, compact = true, forceCompact = true))
        assertEquals("₹1K", formatCurrency(999.95, forceCompact = true))
        assertEquals("₹1L", formatCurrency(99_950.0, forceCompact = true))
        assertEquals("₹1KCr", formatCurrency(1e10, compact = true))
        assertEquals("₹1000LCr", formatCurrency(1e15, compact = true))
        assertEquals("₹10,000LCr", formatCurrency(1e16, compact = true))
    }

    @Test fun shortestDecimalRounding() {
        // Intl rounds the shortest decimal half-up: 1.005 → 1.01 even though the double is 1.00499…
        assertEquals("₹1.01", formatCurrency(1.005))
        assertEquals("₹12,34,567.90", formatCurrency(1234567.895))
        // Quirk kept from the TS: `Math.round(x * 100) % 100` is float noise this large, so the
        // paise check fires and a whole number gets ".00".
        assertEquals("₹1,23,45,67,89,01,23,45,67,000.00", formatCurrency(12345678901234567890.0))
    }

    @Test fun formatDateUsesNow() {
        val now = localDate(2026, 9, 5, 10)
        assertEquals("Today", formatDate("2026-10-05T00:00:00", now))
        assertEquals("Today", formatDate("2026-10-05", now))
        assertEquals("Yesterday", formatDate("2026-10-04T23:59:00", now))
        assertEquals("Yesterday", formatDate("2026-10-03T18:30:00.000Z", now)) // 00:00 IST on the 4th
        assertEquals("Sat, 3 Oct", formatDate("2026-10-03T18:29:59.999Z", now))
    }

    @Test fun parseIntPrefix() {
        assertEquals(12.0, jsParseInt("  12abc"), 0.0)
        assertEquals(true, jsParseInt("abc").isNaN())
        assertEquals(true, (1.0 / jsParseInt("-0")) < 0)
        assertEquals("-0", formatInputAmount("-0"))
    }

    @Test fun ordinalsAndSizes() {
        assertEquals("25th", formatOrdinal(25))
        assertEquals("2.3 KB", formatFileSize(2_400L))
        assertEquals("0%", formatPercentChange(-0.004))
    }
}
