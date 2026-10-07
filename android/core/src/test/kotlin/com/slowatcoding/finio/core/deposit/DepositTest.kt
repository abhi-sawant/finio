package com.slowatcoding.finio.core.deposit

import com.slowatcoding.finio.core.js.localDate
import com.slowatcoding.finio.core.js.toIso
import com.slowatcoding.finio.core.model.Account
import com.slowatcoding.finio.core.model.AccountType
import com.slowatcoding.finio.core.model.DepositTerms
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.pow

class DepositTest {
    private fun day(y: Int, m: Int, d: Int) = localDate(y, m - 1, d).toIso()
    private val terms = DepositTerms(amount = 50000.0, interestRate = 6.65, startDate = day(2026, 10, 1), tenureMonths = 33, linkedAccountId = "bank")
    private val rd = Account("rd-1", "RD", AccountType.Rd, "#000", "vault", 0.0, 0.0, day(2026, 1, 1), deposit = terms)

    @Test fun previewOverloadsAgreeWithAccountOverloads() {
        assertEquals(depositMaturityAmount(rd), depositMaturityAmount(AccountType.Rd, terms), 0.0)
        assertEquals(depositInvested(rd), depositInvested(AccountType.Rd, terms), 0.0)
        assertEquals(depositCaption(rd), depositCaption(AccountType.Rd, terms))
        assertEquals("RD · 6.65% · matures 1 Jul 2029", depositCaption(rd))
    }

    @Test fun onlyPaidInstallmentsCount() {
        val r = 0.0665 / 4
        val expected = 50000 * ((1 + r).pow(2.0 / 3) + (1 + r).pow(1.0 / 3) + 1)
        assertEquals(expected, depositCurrentValue(rd, localDate(2026, 11, 1)), 0.01)
    }

    @Test fun maturedDepositShowsBookBalance() {
        val matured = rd.copy(balance = 0.0, deposit = terms.copy(maturedAt = day(2029, 7, 1)))
        assertEquals(0.0, accountDisplayValue(matured, localDate(2027, 0, 1)), 0.0)
    }
}
