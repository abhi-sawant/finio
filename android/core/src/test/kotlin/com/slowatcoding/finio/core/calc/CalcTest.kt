package com.slowatcoding.finio.core.calc

import com.slowatcoding.finio.core.model.Transaction
import com.slowatcoding.finio.core.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Behaviour the golden fixtures can't express (JSON has no NaN/1e21 round-trip, Kotlin-only guards).
class CalcTest {
    private fun tx(id: String, amount: Double, note: String = "x", type: TransactionType = TransactionType.Expense) =
        Transaction(id = id, type = type, amount = amount, accountId = "a", categoryId = "cat-1", date = "2026-06-05T00:00:00.000Z", note = note, createdAt = "2026-06-05T00:00:00.000Z")

    @Test
    fun jsNumberStringUsesExponentFormLikeJs() {
        assertEquals("1e-7", jsNumberString(1e-7))
        assertEquals("0.000001", jsNumberString(1e-6))
        assertEquals("1.5e-7", jsNumberString(1.5e-7))
        assertEquals("1e+21", jsNumberString(1e21))
        assertEquals("-2.5e+22", jsNumberString(-2.5e22))
        assertEquals("123456789.125", jsNumberString(123456789.125))
        assertEquals("0.30000000000000004", jsNumberString(0.1 + 0.2))
    }

    @Test
    fun amountSearchMatchesExponentRendering() {
        val index = buildSearchIndex(emptyList(), emptyList(), emptyList())
        assertTrue(transactionMatchesQuery(tx("t", 1e-7), "1", index))
        assertFalse(transactionMatchesQuery(tx("t", 1e-7), "0.0000001", index))
    }

    @Test
    fun normalizeNoteKeepsOnlyAsciiLetters() {
        assertEquals("upi spotify", normalizeNote("UPI/Spotify/9921"))
        assertEquals("caf coffee", normalizeNote("  Café  Coffee "))
        assertEquals("", normalizeNote("१२३ स्विगी"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun merchantsRejectTransfers() {
        summarizeMerchants(listOf(tx("t", 1.0)), TransactionType.Transfer)
    }

    @Test
    fun csvEscapesFormulaCells() {
        val csv = transactionsToCsv(listOf(tx("t", 5.0, note = "=1+1")), emptyList(), emptyList())
        assertEquals("2026-06-05T00:00:00.000Z,expense,5,\"\",\"\",\"\",\"'=1+1\",\"\"", csv.lines()[1])
    }
}
