package com.slowatcoding.finio.core.loan

import com.slowatcoding.finio.core.js.addMonths
import com.slowatcoding.finio.core.js.differenceInCalendarMonths
import com.slowatcoding.finio.core.js.fullYear
import com.slowatcoding.finio.core.js.isAfter
import com.slowatcoding.finio.core.js.iso
import com.slowatcoding.finio.core.js.nowInstant
import com.slowatcoding.finio.core.js.toIso
import com.slowatcoding.finio.core.model.Loan
import com.slowatcoding.finio.core.model.LoanPrepayment
import com.slowatcoding.finio.core.money.roundMoney
import kotlinx.serialization.Serializable
import java.time.Instant
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

// Port of web/src/utils/loan.ts — pure EMI amortization and the "what if I prepay" calculator.
// Nothing here reads or writes real transactions; it is a plan derived from principal / rate /
// tenure plus whatever prepayments actually happened.

@Serializable
data class LoanPrepaymentInput(
    val amount: Double,
    /** ISO date the extra payment landed. */
    val date: String,
)

@Serializable
data class LoanScheduleInput(
    val principal: Double,
    /** Annual interest rate as a percent, e.g. 8.5 for 8.5%. */
    val interestRate: Double,
    /** Number of EMIs at origination. */
    val tenureMonths: Int,
    /** ISO date of the first EMI. */
    val startDate: String,
    val prepayments: List<LoanPrepaymentInput>? = null,
)

/** A stored loan plus its recorded prepayments, as the schedule input. */
fun Loan.scheduleInput(prepayments: List<LoanPrepayment> = emptyList()): LoanScheduleInput =
    LoanScheduleInput(principal, interestRate, tenureMonths, startDate, prepayments.map { LoanPrepaymentInput(it.amount, it.date) })

@Serializable
data class AmortizationRow(
    /** 1-based installment number. */
    val month: Int,
    /** ISO date this installment is due. */
    val date: String,
    val openingBalance: Double,
    val interest: Double,
    val principal: Double,
    /** `principal + interest` — equal to the fixed EMI except possibly the last row. */
    val emi: Double,
    /** Extra principal applied this month from a dated prepayment, on top of the EMI. */
    val prepayment: Double,
    val closingBalance: Double,
)

@Serializable
data class LoanStatus(
    val emi: Double,
    /** Interest over the full schedule, including any prepayments already on record. */
    val totalInterest: Double,
    /** Length of the schedule — at or below `tenureMonths`. */
    val totalMonths: Int,
    /** Balance right now, i.e. after every installment on or before `now`. */
    val outstandingBalance: Double,
    val paidInstallments: Int,
    val totalInterestPaid: Double,
    val nextDueDate: String?,
    /** Due date of the schedule's last installment. */
    val payoffDate: String?,
    val isPaidOff: Boolean,
)

@Serializable
data class PrepaymentImpact(
    /** How many fewer installments the loan would take. */
    val monthsSaved: Int,
    val interestSaved: Double,
    val newPayoffDate: String?,
)

fun monthlyRate(annualRatePercent: Double): Double = annualRatePercent / 12 / 100

/** Standard EMI formula. Falls back to a flat split when the rate is 0. */
fun calculateEmi(principal: Double, annualRatePercent: Double, tenureMonths: Int): Double {
    if (principal <= 0 || tenureMonths <= 0) return 0.0
    val r = monthlyRate(annualRatePercent)
    if (r == 0.0) return roundMoney(principal / tenureMonths)
    val factor = (1 + r).pow(tenureMonths)
    return roundMoney((principal * r * factor) / (factor - 1))
}

/** Whole calendar months from [startDate] to [date] — which installment a date falls on. */
private fun monthsBetween(startDate: String, date: String): Int = differenceInCalendarMonths(iso(date), iso(startDate))

/**
 * Month-by-month amortization at a fixed EMI. Prepayments are extra principal on the installment
 * they fall on or after; the loan finishes early rather than the EMI shrinking.
 */
fun buildAmortizationSchedule(loan: LoanScheduleInput): List<AmortizationRow> {
    val emi = calculateEmi(loan.principal, loan.interestRate, loan.tenureMonths)
    val r = monthlyRate(loan.interestRate)

    val prepaymentsByMonth = HashMap<Int, Double>()
    for (p in loan.prepayments.orEmpty()) {
        if (p.amount <= 0) continue
        val month = max(1, monthsBetween(loan.startDate, p.date) + 1)
        prepaymentsByMonth[month] = roundMoney((prepaymentsByMonth[month] ?: 0.0) + p.amount)
    }

    val rows = mutableListOf<AmortizationRow>()
    var balance = loan.principal
    var month = 0
    val start = iso(loan.startDate)

    while (balance > 0.005 && month < loan.tenureMonths) {
        month += 1
        val openingBalance = balance
        val interest = roundMoney(openingBalance * r)
        val principal = roundMoney(min(emi - interest, openingBalance))
        var closingBalance = roundMoney(openingBalance - principal)

        val extra = min(prepaymentsByMonth[month] ?: 0.0, closingBalance)
        closingBalance = roundMoney(closingBalance - extra)

        rows += AmortizationRow(
            month = month,
            date = addMonths(start, month - 1).toIso(),
            openingBalance = openingBalance,
            interest = interest,
            principal = principal,
            emi = roundMoney(principal + interest),
            prepayment = extra,
            closingBalance = closingBalance,
        )
        balance = closingBalance
    }
    return rows
}

/** Where the loan actually stands [now], from its schedule (including any real prepayments). */
fun loanStatus(loan: LoanScheduleInput, now: Instant = nowInstant()): LoanStatus {
    val schedule = buildAmortizationSchedule(loan)
    val emi = calculateEmi(loan.principal, loan.interestRate, loan.tenureMonths)
    val totalInterest = roundMoney(schedule.fold(0.0) { sum, row -> sum + row.interest })

    val paidRows = schedule.filter { !isAfter(iso(it.date), now) }
    val remainingRows = schedule.drop(paidRows.size)
    val totalInterestPaid = roundMoney(paidRows.fold(0.0) { sum, row -> sum + row.interest })
    val afterInstallments = if (paidRows.isNotEmpty()) paidRows.last().closingBalance else (schedule.firstOrNull()?.openingBalance ?: 0.0)
    // A prepayment dated on or before `now` has already left the account, but the schedule files
    // it under its installment's row, which may not be due yet — take it off here.
    val pendingPrepaid = loan.prepayments.orEmpty().fold(0.0) { sum, p ->
        if (p.amount <= 0 || isAfter(iso(p.date), now)) return@fold sum
        val month = max(1, monthsBetween(loan.startDate, p.date) + 1)
        if (month > paidRows.size) sum + p.amount else sum
    }
    val outstandingBalance = roundMoney(max(0.0, afterInstallments - pendingPrepaid))

    return LoanStatus(
        emi = emi,
        totalInterest = totalInterest,
        totalMonths = schedule.size,
        outstandingBalance = outstandingBalance,
        paidInstallments = paidRows.size,
        totalInterestPaid = totalInterestPaid,
        nextDueDate = remainingRows.firstOrNull()?.date,
        payoffDate = schedule.lastOrNull()?.date,
        isPaidOff = schedule.isNotEmpty() && remainingRows.isEmpty(),
    )
}

/** The most a new prepayment can be: what is still owed right now. */
fun maxPrepayment(loan: LoanScheduleInput, now: Instant = nowInstant()): Double = loanStatus(loan, now).outstandingBalance

/** "What if I paid an extra ₹X on this date?" — schedule with vs without one more prepayment. */
fun simulatePrepaymentImpact(loan: LoanScheduleInput, extra: LoanPrepaymentInput): PrepaymentImpact {
    val baseline = buildAmortizationSchedule(loan)
    val withExtra = buildAmortizationSchedule(loan.copy(prepayments = loan.prepayments.orEmpty() + extra))
    fun interestOf(schedule: List<AmortizationRow>) = roundMoney(schedule.fold(0.0) { sum, row -> sum + row.interest })
    return PrepaymentImpact(
        monthsSaved = baseline.size - withExtra.size,
        interestSaved = roundMoney(interestOf(baseline) - interestOf(withExtra)),
        newPayoffDate = withExtra.lastOrNull()?.date,
    )
}

@Serializable
data class ScheduleYearGroup(
    /** Calendar year (local time) of the installments' due dates. */
    val year: Int,
    val rows: List<AmortizationRow>,
    /** Sum of every EMI plus any prepayment due in this year. */
    val totalPaid: Double,
    /** Sum of the interest portion of this year's EMIs. */
    val totalInterest: Double,
    /** Sum of the principal portion of this year's EMIs, prepayments included. */
    val totalPrincipal: Double,
)

/** Groups a schedule by the calendar year each installment falls due in, keeping order. */
fun groupScheduleByYear(schedule: List<AmortizationRow>): List<ScheduleYearGroup> {
    data class Acc(val year: Int, val rows: MutableList<AmortizationRow>, var paid: Double, var interest: Double, var principal: Double)
    val groups = mutableListOf<Acc>()
    for (row in schedule) {
        val year = iso(row.date).fullYear
        var group = groups.lastOrNull()
        if (group == null || group.year != year) {
            group = Acc(year, mutableListOf(), 0.0, 0.0, 0.0)
            groups += group
        }
        group.rows += row
        group.paid = roundMoney(group.paid + row.emi + row.prepayment)
        group.interest = roundMoney(group.interest + row.interest)
        group.principal = roundMoney(group.principal + row.principal + row.prepayment)
    }
    return groups.map { ScheduleYearGroup(it.year, it.rows.toList(), it.paid, it.interest, it.principal) }
}
