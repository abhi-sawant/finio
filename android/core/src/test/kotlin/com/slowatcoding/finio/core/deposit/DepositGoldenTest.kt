package com.slowatcoding.finio.core.deposit

import com.slowatcoding.finio.core.golden.Golden
import com.slowatcoding.finio.core.golden.i
import com.slowatcoding.finio.core.model.Account
import com.slowatcoding.finio.core.model.AccountType
import com.slowatcoding.finio.core.model.DepositTerms
import com.slowatcoding.finio.core.model.wireName
import com.slowatcoding.finio.core.period.differenceInMonths
import com.slowatcoding.finio.core.period.instant
import com.slowatcoding.finio.core.period.json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.Test
import java.time.Instant

class DepositGoldenTest {
    private fun account(e: JsonElement): Account = Golden.decode(e)
    private fun accounts(e: JsonElement): List<Account> = e.jsonArray.map(::account)
    private fun dates(e: JsonElement): List<Instant> = e.jsonArray.map { it.instant }
    private fun each(e: JsonElement, f: (Instant) -> Number): JsonArray = JsonArray(dates(e).map { JsonPrimitive(f(it)) })

    @Test
    fun matchesTypeScript() = Golden.verify("deposit") { c ->
        when (c.fn) {
            "DEPOSIT_COMPOUNDING_OPTIONS" -> JsonArray(DEPOSIT_COMPOUNDING_OPTIONS.map {
                buildJsonObject { put("value", JsonPrimitive(wireName(it.value))); put("label", JsonPrimitive(it.label)) }
            })
            "differenceInMonths[]" -> c.arg(0).instant.let { a -> JsonArray(dates(c.arg(1)).map { JsonPrimitive(differenceInMonths(a, it)) }) }
            "isDepositAccount" -> JsonPrimitive(isDepositAccount(Golden.decode<AccountType>(c.arg(0))))
            "depositMaturityDate" -> depositMaturityDate(account(c.arg(0))).json()
            "depositInvested" -> JsonPrimitive(depositInvested(account(c.arg(0))))
            // The 'preview' case is a Pick<Account, 'type' | 'deposit'>, not a full account.
            "depositMaturityAmount" -> JsonPrimitive(
                if (c.name == "preview") c.arg(0).jsonObject.let { depositMaturityAmount(Golden.decode<AccountType>(it.getValue("type")), Golden.decode<DepositTerms>(it.getValue("deposit"))) }
                else depositMaturityAmount(account(c.arg(0))),
            )
            "depositCaption" -> JsonPrimitive(depositCaption(account(c.arg(0))))
            "depositValueAt[]" -> account(c.arg(0)).let { a -> each(c.arg(1)) { depositValueAt(a, it) } }
            "depositCurrentValue[]" -> account(c.arg(0)).let { a -> each(c.arg(1)) { depositCurrentValue(a, it) } }
            "accountDisplayValue[]" -> account(c.arg(0)).let { a -> each(c.arg(1)) { accountDisplayValue(a, it) } }
            "rdInstallmentsOnOrBefore[]" -> Golden.decode<DepositTerms>(c.arg(0)).let { t -> each(c.arg(1)) { rdInstallmentsOnOrBefore(t, it) } }
            "rdInstallmentDate" -> rdInstallmentDate(Golden.decode(c.arg(0)), c.arg(1).i).json()
            "planMaturities" -> JsonArray(planMaturities(accounts(c.arg(0)), c.arg(1).instant).map { JsonPrimitive(it.id) })
            "accountDeleteBlockers" -> JsonArray(accountDeleteBlockers(accounts(c.arg(0)), c.arg(1).jsonPrimitiveContent()).map(::JsonPrimitive))
            else -> null
        }
    }

    private fun JsonElement.jsonPrimitiveContent(): String = (this as JsonPrimitive).content
}
