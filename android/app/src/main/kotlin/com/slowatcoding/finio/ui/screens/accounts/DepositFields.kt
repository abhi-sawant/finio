package com.slowatcoding.finio.ui.screens.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slowatcoding.finio.core.deposit.DEPOSIT_COMPOUNDING_OPTIONS
import com.slowatcoding.finio.core.deposit.depositInvested
import com.slowatcoding.finio.core.deposit.depositMaturityAmount
import com.slowatcoding.finio.core.deposit.depositMaturityDate
import com.slowatcoding.finio.core.deposit.rdInstallmentsOnOrBefore
import com.slowatcoding.finio.core.format.formatShortDate
import com.slowatcoding.finio.core.js.localDate
import com.slowatcoding.finio.core.js.nowInstant
import com.slowatcoding.finio.core.js.parseIso
import com.slowatcoding.finio.core.js.toIso
import com.slowatcoding.finio.core.model.Account
import com.slowatcoding.finio.core.model.AccountType
import com.slowatcoding.finio.core.model.DepositCompounding
import com.slowatcoding.finio.core.model.DepositTerms
import com.slowatcoding.finio.ui.common.MoneyFormatter
import com.slowatcoding.finio.ui.components.FinioCard
import com.slowatcoding.finio.ui.components.FinioDatePicker
import com.slowatcoding.finio.ui.components.FinioSelect
import com.slowatcoding.finio.ui.components.FinioTextField
import com.slowatcoding.finio.ui.components.NumberPad
import com.slowatcoding.finio.ui.components.SelectOption
import com.slowatcoding.finio.ui.components.SwitchField
import com.slowatcoding.finio.ui.theme.FinioShapes
import com.slowatcoding.finio.ui.theme.FinioTheme
import com.slowatcoding.finio.ui.theme.FinioType
import java.time.LocalDate

/** depositForm.ts `DepositFormValues` — raw input; dates as calendar days (web: `yyyy-MM-dd`). */
@Immutable
data class DepositFormValues(
    val amount: String = "0",
    val interestRate: String = "",
    val startDate: LocalDate? = null,
    val maturityDate: LocalDate? = null,
    val tenureMonths: String = "",
    val compounding: DepositCompounding = DepositCompounding.Quarterly,
    val linkedAccountId: String = "",
    val deductPast: Boolean = false,
)

private fun isoDay(iso: String): LocalDate? = parseIso(iso)?.localDate()

/** depositForm.ts `depositFormFromAccount`. */
fun depositFormFromAccount(account: Account?, defaultLinkedId: String): DepositFormValues {
    val terms = account?.deposit
    return DepositFormValues(
        amount = terms?.amount?.let(::jsNumberString) ?: "0",
        interestRate = terms?.interestRate?.let(::jsNumberString) ?: "",
        startDate = terms?.startDate?.let(::isoDay),
        maturityDate = terms?.maturityDate?.takeIf { it.isNotEmpty() }?.let(::isoDay),
        tenureMonths = terms?.tenureMonths?.toString() ?: "",
        compounding = terms?.compounding ?: DepositCompounding.Quarterly,
        linkedAccountId = terms?.linkedAccountId ?: defaultLinkedId,
        deductPast = false,
    )
}

/** `new Date(\`${day}T00:00:00\`).toISOString()` — local midnight of [day]. */
private fun LocalDate.toIsoMidnight(): String = localDate(year, monthValue - 1, dayOfMonth).toIso()

/** depositForm.ts `depositTermsFromForm` — null while incomplete or inconsistent. */
fun depositTermsFromForm(type: AccountType, values: DepositFormValues): DepositTerms? {
    val amount = jsParseFloat(values.amount) ?: 0.0
    val interestRate = jsParseFloat(values.interestRate)
    if (amount <= 0 || interestRate == null || interestRate < 0) return null
    val start = values.startDate ?: return null
    if (values.linkedAccountId.isEmpty()) return null
    if (type == AccountType.Fd) {
        val maturity = values.maturityDate ?: return null
        if (!maturity.isAfter(start)) return null
        return DepositTerms(
            amount = amount,
            interestRate = interestRate,
            startDate = start.toIsoMidnight(),
            linkedAccountId = values.linkedAccountId,
            maturityDate = maturity.toIsoMidnight(),
            compounding = values.compounding,
        )
    }
    val tenure = jsParseInt(values.tenureMonths) ?: return null
    if (tenure < 1) return null
    return DepositTerms(
        amount = amount,
        interestRate = interestRate,
        startDate = start.toIsoMidnight(),
        linkedAccountId = values.linkedAccountId,
        tenureMonths = tenure,
    )
}

/**
 * Port of components/accounts/DepositFields.tsx — the FD/RD terms that replace the balance pad:
 * amount (locked once created), rate + compounding/tenure, start (+ maturity), the linked
 * account, the "deduct past" switch for back-dated deposits, and the maturity preview.
 */
@Composable
fun DepositFields(
    type: AccountType,
    values: DepositFormValues,
    onChange: (DepositFormValues) -> Unit,
    linkableAccounts: List<Account>,
    locked: Boolean,
    money: MoneyFormatter,
) {
    val colors = FinioTheme.colors
    val isFd = type == AccountType.Fd
    val terms = depositTermsFromForm(type, values)
    val maturity = terms?.let { depositMaturityDate(type, it) }
    val maturityAmount = terms?.let { depositMaturityAmount(type, it) } ?: 0.0
    val invested = terms?.let { depositInvested(type, it) } ?: 0.0
    val linkedName = linkableAccounts.find { it.id == values.linkedAccountId }?.name

    // Installments that already fell due before today — only matters for a new RD.
    val pastInstallments = if (!isFd && !locked && terms != null) rdInstallmentsOnOrBefore(terms, nowInstant()) else 0
    // A new FD dated before today — the funding transfer is optional for those.
    val fdStartedInPast = isFd && !locked && values.startDate != null && values.startDate.isBefore(LocalDate.now())

    val m = { n: Double -> money(n) }

    Column {
        FieldLabel(if (isFd) "Amount" else "Monthly Installment")
        if (locked) {
            LockedField(m(jsParseFloat(values.amount) ?: 0.0))
        } else {
            NumberPad(values.amount, { onChange(values.copy(amount = it)) })
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f)) {
            FieldLabel("Interest Rate (% p.a.)")
            FinioTextField(
                values.interestRate,
                { onChange(values.copy(interestRate = it)) },
                placeholder = "e.g. 6.65",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
        }
        Column(Modifier.weight(1f)) {
            if (isFd) {
                FieldLabel("Compounding")
                FinioSelect(
                    value = values.compounding,
                    options = DEPOSIT_COMPOUNDING_OPTIONS.map { SelectOption(it.value, it.label) },
                    onValueChange = { onChange(values.copy(compounding = it)) },
                    title = "Compounding",
                )
            } else {
                FieldLabel("Period (months)")
                FinioTextField(
                    values.tenureMonths,
                    { onChange(values.copy(tenureMonths = it)) },
                    placeholder = "e.g. 36",
                    enabled = !locked,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }
        }
    }

    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                FieldLabel(if (isFd) "Investment Date" else "First Installment")
                FinioDatePicker(values.startDate, { onChange(values.copy(startDate = it)) }, enabled = !locked)
            }
            if (isFd) {
                Column(Modifier.weight(1f)) {
                    FieldLabel("Maturity Date")
                    FinioDatePicker(values.maturityDate, { onChange(values.copy(maturityDate = it)) })
                }
            } else {
                Box(Modifier.weight(1f))
            }
        }
        if (!isFd) {
            // `-mt-2` against the form's 16dp gap → 8dp below the picker.
            Text(
                "The installment is deducted on this day every month.",
                Modifier.padding(top = 8.dp),
                style = FinioType.micro.copy(fontSize = 10.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Normal),
                color = colors.mutedForeground,
            )
        }
    }

    Column {
        FieldLabel(if (isFd) "Funded From & Redeemed To" else "Deduct From & Pay Out To")
        when {
            locked -> LockedField(linkedName ?: "Unknown account")
            linkableAccounts.isEmpty() -> Text(
                "Add a bank account first — a deposit needs one to fund it and receive the payout.",
                style = FinioType.caption,
                color = colors.mutedForeground,
            )
            else -> FinioSelect(
                value = values.linkedAccountId.takeIf { id -> linkableAccounts.any { it.id == id } },
                options = linkableAccounts.map { SelectOption(it.id, it.name) },
                onValueChange = { onChange(values.copy(linkedAccountId = it)) },
                placeholder = "Choose account",
                title = if (isFd) "Funded From & Redeemed To" else "Deduct From & Pay Out To",
            )
        }
    }

    if (pastInstallments > 0) {
        val n = pastInstallments
        val total = m(n * (terms?.amount ?: 0.0))
        val plural = if (n == 1) "" else "s"
        FinioCard(Modifier.fillMaxWidth()) {
            SwitchField(
                title = "Deduct past installments from account",
                description = if (values.deductPast) {
                    "$n installment$plural ($total) will be posted from ${linkedName ?: "the account"} and show in its history."
                } else {
                    "$n installment$plural ($total) already paid become the RD's opening balance."
                },
                checked = values.deductPast,
                onCheckedChange = { onChange(values.copy(deductPast = it)) },
            )
        }
    }

    if (isFd && fdStartedInPast) {
        val amount = m(terms?.amount ?: 0.0)
        FinioCard(Modifier.fillMaxWidth()) {
            SwitchField(
                title = "Deduct amount from account",
                description = if (values.deductPast) {
                    "$amount will be transferred from ${linkedName ?: "the account"} on the start date and show in its history."
                } else {
                    "$amount was already invested; it becomes the FD's opening balance and ${linkedName ?: "the account"} is left untouched."
                },
                checked = values.deductPast,
                onCheckedChange = { onChange(values.copy(deductPast = it)) },
            )
        }
    }

    if (terms != null && maturity != null) {
        FinioCard(Modifier.fillMaxWidth()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PreviewStat("Invested", m(invested), colors.foreground, Modifier.weight(1f))
                PreviewStat("Interest", m(maturityAmount - invested), colors.foreground, Modifier.weight(1f))
                PreviewStat("At maturity", m(maturityAmount), colors.positive, Modifier.weight(1f))
            }
            Text(
                "Matures ${formatShortDate(maturity)} — paid into ${linkedName ?: "the account"} automatically. Any TDS on the interest can be logged as an expense.",
                Modifier.fillMaxWidth().padding(top = 12.dp),
                style = FinioType.caption,
                color = colors.mutedForeground,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun PreviewStat(label: String, value: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = FinioType.label, color = FinioTheme.colors.mutedForeground, textAlign = TextAlign.Center)
        Text(value, style = FinioType.rowValue, color = color, textAlign = TextAlign.Center)
    }
}

/** `text-muted-foreground mb-1.5 block text-xs font-medium`. */
@Composable
internal fun FieldLabel(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier.padding(bottom = 6.dp), style = FinioType.label, color = FinioTheme.colors.mutedForeground)
}

/** The read-only value box (`bg-muted border-border h-10 rounded-sm border px-3`). */
@Composable
private fun LockedField(text: String) {
    val colors = FinioTheme.colors
    Box(
        Modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(FinioShapes.sm)
            .background(colors.muted)
            .border(1.dp, colors.border, FinioShapes.sm)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(text, style = FinioType.input, color = colors.foreground, maxLines = 1)
    }
}

/** JS `parseFloat`: the longest numeric prefix, or null (NaN). */
internal fun jsParseFloat(raw: String): Double? {
    val s = raw.trimStart()
    val m = Regex("^[+-]?(\\d+\\.?\\d*|\\.\\d+)([eE][+-]?\\d+)?").find(s) ?: return null
    return m.value.toDoubleOrNull()
}

/** JS `parseInt(s, 10)`: optional sign then the longest digit prefix, or null (NaN). */
internal fun jsParseInt(raw: String): Int? {
    val m = Regex("^[+-]?\\d+").find(raw.trimStart()) ?: return null
    return m.value.toBigInteger().coerceIn(Int.MIN_VALUE.toBigInteger(), Int.MAX_VALUE.toBigInteger()).toInt()
}

/** JS `Number.prototype.toString` for the amounts a form pre-fills (`1500`, `6.65`). */
internal fun jsNumberString(x: Double): String =
    if (x == Math.floor(x) && kotlin.math.abs(x) < 1e15) x.toLong().toString() else x.toBigDecimal().stripTrailingZeros().toPlainString()
