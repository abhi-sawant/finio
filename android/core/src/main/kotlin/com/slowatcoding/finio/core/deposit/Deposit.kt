package com.slowatcoding.finio.core.deposit

import com.slowatcoding.finio.core.format.formatShortDate
import com.slowatcoding.finio.core.js.addMonths
import com.slowatcoding.finio.core.js.differenceInCalendarDays
import com.slowatcoding.finio.core.js.isAfter
import com.slowatcoding.finio.core.js.jsNumberToString
import com.slowatcoding.finio.core.js.nowInstant
import com.slowatcoding.finio.core.js.parseIso
import com.slowatcoding.finio.core.model.Account
import com.slowatcoding.finio.core.model.AccountType
import com.slowatcoding.finio.core.model.DepositCompounding
import com.slowatcoding.finio.core.model.DepositTerms
import com.slowatcoding.finio.core.money.roundMoney
import com.slowatcoding.finio.core.period.differenceInMonths
import java.time.Instant
import kotlin.math.min
import kotlin.math.pow

// Port of web/src/utils/deposit.ts — fixed and recurring deposit math. Only a deposit's terms
// are stored; its maturity amount and current value are always derived here.
//
// Indian bank conventions:
//  - An FD compounds at its chosen frequency over the term counted in actual days / 365.
//  - An RD compounds quarterly, each installment earning only from the month it was paid:
//    Σ P(1 + r/4)^(monthsHeld / 3).
//
// The TS functions take `Pick<Account, 'type' | 'deposit'>` (the Add Account form previews a
// draft that isn't an account yet), so each has a (type, deposit) primary plus an Account overload.

data class CompoundingOption(val value: DepositCompounding, val label: String)

val DEPOSIT_COMPOUNDING_OPTIONS: List<CompoundingOption> = listOf(
    CompoundingOption(DepositCompounding.Quarterly, "Quarterly"),
    CompoundingOption(DepositCompounding.Monthly, "Monthly"),
    CompoundingOption(DepositCompounding.HalfYearly, "Half-yearly"),
    CompoundingOption(DepositCompounding.Yearly, "Yearly"),
    CompoundingOption(DepositCompounding.Simple, "Simple interest"),
)

private fun periodsPerYear(c: DepositCompounding): Int = when (c) {
    DepositCompounding.Monthly -> 12
    DepositCompounding.Quarterly -> 4
    DepositCompounding.HalfYearly -> 2
    DepositCompounding.Yearly -> 1
    DepositCompounding.Simple -> error("simple interest has no compounding periods")
}

fun isDepositAccount(type: AccountType): Boolean = type == AccountType.Fd || type == AccountType.Rd
fun isDepositAccount(account: Account): Boolean = isDepositAccount(account.type)

private fun parseDate(value: String?): Instant? = if (value.isNullOrEmpty()) null else parseIso(value)

/** [from] → [to] in months, fractional within the last partial month. Exact on whole months. */
private fun fractionalMonths(from: Instant, to: Instant): Double {
    if (!isAfter(to, from)) return 0.0
    val whole = differenceInMonths(to, from)
    val anchor = addMonths(from, whole)
    val next = addMonths(from, whole + 1)
    return whole + (to.toEpochMilli() - anchor.toEpochMilli()).toDouble() / (next.toEpochMilli() - anchor.toEpochMilli())
}

/** FD growth factor over [years] at [ratePercent]. */
private fun fdGrowth(ratePercent: Double, years: Double, compounding: DepositCompounding): Double {
    val r = ratePercent / 100
    if (years <= 0) return 1.0
    if (compounding == DepositCompounding.Simple) return 1 + r * years
    val m = periodsPerYear(compounding)
    return (1 + r / m).pow(m * years)
}

/** The date of RD installment [index] (0-based), anchored to the start date — no month-end drift. */
fun rdInstallmentDate(terms: DepositTerms, index: Int): Instant? = parseDate(terms.startDate)?.let { addMonths(it, index) }

/** An FD's stated maturity date, or `tenureMonths` after an RD's first installment. */
fun depositMaturityDate(type: AccountType, deposit: DepositTerms?): Instant? {
    val terms = deposit ?: return null
    if (type == AccountType.Rd) return rdInstallmentDate(terms, terms.tenureMonths ?: 0)
    return parseDate(terms.maturityDate)
}
fun depositMaturityDate(account: Account): Instant? = depositMaturityDate(account.type, account.deposit)

/** How many RD installments fall on or before [date], capped at the tenure. */
fun rdInstallmentsOnOrBefore(terms: DepositTerms, date: Instant): Int {
    val start = parseDate(terms.startDate)
    val tenure = terms.tenureMonths ?: 0
    if (start == null || isAfter(start, date)) return 0
    return min(tenure, differenceInMonths(date, start) + 1)
}

/** Total money put in over the deposit's life: the FD principal, or installment × tenure. */
fun depositInvested(type: AccountType, deposit: DepositTerms?): Double {
    val terms = deposit ?: return 0.0
    if (type == AccountType.Rd) return roundMoney(terms.amount * (terms.tenureMonths ?: 0))
    return terms.amount
}
fun depositInvested(account: Account): Double = depositInvested(account.type, account.deposit)

/**
 * The deposit's accrued value on [date], capped at maturity. An FD is the principal grown to
 * [date]; an RD counts only installments already paid, each grown by how long it was held.
 */
fun depositValueAt(type: AccountType, deposit: DepositTerms?, date: Instant): Double {
    val terms = deposit ?: return 0.0
    val start = parseDate(terms.startDate)
    val maturity = depositMaturityDate(type, terms)
    if (start == null || maturity == null) return 0.0
    val at = if (isAfter(date, maturity)) maturity else date
    if (isAfter(start, at)) return 0.0

    if (type == AccountType.Rd) {
        val paid = rdInstallmentsOnOrBefore(terms, at)
        val quarterly = terms.interestRate / 100 / 4
        var value = 0.0
        for (i in 0 until paid) {
            val paidOn = addMonths(start, i)
            value += terms.amount * (1 + quarterly).pow(fractionalMonths(paidOn, at) / 3)
        }
        return roundMoney(value)
    }

    val years = differenceInCalendarDays(at, start) / 365.0
    val growth = fdGrowth(terms.interestRate, years, terms.compounding ?: DepositCompounding.Quarterly)
    return roundMoney(terms.amount * growth)
}
fun depositValueAt(account: Account, date: Instant): Double = depositValueAt(account.type, account.deposit, date)

/** What the deposit pays out at maturity. */
fun depositMaturityAmount(type: AccountType, deposit: DepositTerms?): Double =
    depositMaturityDate(type, deposit)?.let { depositValueAt(type, deposit, it) } ?: 0.0
fun depositMaturityAmount(account: Account): Double = depositMaturityAmount(account.type, account.deposit)

/** Today's value of a deposit — what the account cards show instead of the book balance. */
fun depositCurrentValue(type: AccountType, deposit: DepositTerms?, now: Instant = nowInstant()): Double =
    depositValueAt(type, deposit, now)
fun depositCurrentValue(account: Account, now: Instant = nowInstant()): Double = depositValueAt(account, now)

/** An open deposit's value today, otherwise the book balance (a paid-out deposit's 0 is the truth). */
fun accountDisplayValue(account: Account, now: Instant = nowInstant()): Double =
    if (isDepositAccount(account) && account.deposit != null && account.deposit.maturedAt.isNullOrEmpty()) {
        depositCurrentValue(account, now)
    } else {
        account.balance
    }

/** Open deposits whose maturity has arrived, whose payout account still exists, not yet paid out. */
fun planMaturities(accounts: List<Account>, now: Instant): List<Account> {
    val ids = accounts.map { it.id }.toSet()
    return accounts.filter { account ->
        val deposit = account.deposit
        if (!isDepositAccount(account) || deposit == null || !deposit.maturedAt.isNullOrEmpty()) return@filter false
        if (deposit.linkedAccountId !in ids) return@filter false
        val maturity = depositMaturityDate(account)
        maturity != null && !isAfter(maturity, now)
    }
}

/** Names of open deposits that pay out into [accountId] — non-empty means it can't be deleted. */
fun accountDeleteBlockers(accounts: List<Account>, accountId: String): List<String> =
    accounts.filter {
        it.id != accountId && isDepositAccount(it) && it.deposit?.linkedAccountId == accountId &&
            it.deposit.maturedAt.isNullOrEmpty()
    }.map { it.name }

/** One-line caption for a deposit row, e.g. "FD · 6.65% · matures 1 Sep 2029". */
fun depositCaption(type: AccountType, deposit: DepositTerms?): String {
    val kind = if (type == AccountType.Rd) "RD" else "FD"
    val terms = deposit ?: return kind
    val rate = jsNumberToString(terms.interestRate)
    if (!terms.maturedAt.isNullOrEmpty()) return "$kind · $rate% · matured"
    val maturity = depositMaturityDate(type, terms)
    return "$kind · $rate%${if (maturity != null) " · matures ${formatShortDate(maturity)}" else ""}"
}
fun depositCaption(account: Account): String = depositCaption(account.type, account.deposit)
