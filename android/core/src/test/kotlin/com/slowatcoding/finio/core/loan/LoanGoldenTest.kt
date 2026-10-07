package com.slowatcoding.finio.core.loan

import com.slowatcoding.finio.core.golden.Golden
import com.slowatcoding.finio.core.golden.d
import com.slowatcoding.finio.core.golden.i
import com.slowatcoding.finio.core.period.instant
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Test

class LoanGoldenTest {
    private fun loan(c: Golden.Case) = Golden.decode<LoanScheduleInput>(c.arg(0))

    @Test
    fun matchesTypeScript() = Golden.verify("loan") { c ->
        when (c.fn) {
            "monthlyRate" -> JsonPrimitive(monthlyRate(c.arg(0).d))
            "calculateEmi" -> JsonPrimitive(calculateEmi(c.arg(0).d, c.arg(1).d, c.arg(2).i))
            "buildAmortizationSchedule" -> Golden.encode(buildAmortizationSchedule(loan(c)))
            // args = [loan] (the schedule is rebuilt), or [[]] for the empty schedule.
            "groupScheduleByYear" -> Golden.encode(
                if (c.arg(0) is JsonArray) groupScheduleByYear(emptyList()) else groupScheduleByYear(buildAmortizationSchedule(loan(c))),
            )
            "loanStatus" -> Golden.encode(loanStatus(loan(c), c.arg(1).instant))
            "maxPrepayment" -> JsonPrimitive(maxPrepayment(loan(c), c.arg(1).instant))
            "simulatePrepaymentImpact" -> Golden.encode(simulatePrepaymentImpact(loan(c), Golden.decode(c.arg(1))))
            else -> null
        }
    }
}
