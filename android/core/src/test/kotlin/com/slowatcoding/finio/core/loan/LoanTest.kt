package com.slowatcoding.finio.core.loan

import com.slowatcoding.finio.core.js.iso
import com.slowatcoding.finio.core.model.Loan
import com.slowatcoding.finio.core.model.LoanPrepayment
import org.junit.Assert.assertEquals
import org.junit.Test

class LoanTest {
    private val loan = LoanScheduleInput(100000.0, 0.0, 10, "2026-01-05T00:00:00.000Z")

    @Test fun prepaymentThisMonthComesOffTheBalance() {
        val now = iso("2026-04-20T00:00:00.000Z")
        val withPrepay = loan.copy(prepayments = listOf(LoanPrepaymentInput(20000.0, "2026-04-19T00:00:00.000Z")))
        assertEquals(loanStatus(loan, now).outstandingBalance - 20000, loanStatus(withPrepay, now).outstandingBalance, 0.0)
    }

    @Test fun storedLoanMapsToScheduleInput() {
        val stored = Loan("l1", "Car", 50000.0, 9.5, 24, "2026-02-01T00:00:00.000Z", "acc", "cat-27", createdAt = "2026-01-01T00:00:00.000Z")
        val prepay = LoanPrepayment("p1", "l1", 1000.0, "2026-05-01T00:00:00.000Z", "", createdAt = "2026-05-01T00:00:00.000Z")
        val input = stored.scheduleInput(listOf(prepay))
        assertEquals(LoanScheduleInput(50000.0, 9.5, 24, "2026-02-01T00:00:00.000Z", listOf(LoanPrepaymentInput(1000.0, "2026-05-01T00:00:00.000Z"))), input)
        assertEquals(buildAmortizationSchedule(input).size, loanStatus(input, iso("2020-01-01")).totalMonths)
    }
}
