package com.slowatcoding.finio.ui.screens.loans

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.slowatcoding.finio.core.calc.activeAccounts
import com.slowatcoding.finio.core.calc.isCategoryValidForType
import com.slowatcoding.finio.core.calc.miscLast
import com.slowatcoding.finio.core.data.MISC_CATEGORY_ID
import com.slowatcoding.finio.core.format.localDayKey
import com.slowatcoding.finio.core.js.nowInstant
import com.slowatcoding.finio.core.js.toIso
import com.slowatcoding.finio.core.loan.calculateEmi
import com.slowatcoding.finio.core.model.RecurrenceFrequency
import com.slowatcoding.finio.core.model.RecurringTransaction
import com.slowatcoding.finio.core.model.TransactionType
import com.slowatcoding.finio.core.store.NewLoan
import com.slowatcoding.finio.core.store.previewBackfill
import com.slowatcoding.finio.core.util.MAX_NAME_LENGTH
import com.slowatcoding.finio.core.util.cleanText
import com.slowatcoding.finio.core.util.jsTrim
import com.slowatcoding.finio.core.util.stripLeading
import com.slowatcoding.finio.ui.common.BackButton
import com.slowatcoding.finio.ui.common.collectFinanceState
import com.slowatcoding.finio.ui.common.financeStore
import com.slowatcoding.finio.ui.common.rememberMoneyFormatter
import com.slowatcoding.finio.ui.common.undoToast
import com.slowatcoding.finio.ui.components.ButtonSize
import com.slowatcoding.finio.ui.components.CategoryGrid
import com.slowatcoding.finio.ui.components.CategoryTileData
import com.slowatcoding.finio.ui.components.FinioButton
import com.slowatcoding.finio.ui.components.FinioCard
import com.slowatcoding.finio.ui.components.FinioDatePicker
import com.slowatcoding.finio.ui.components.FinioDivider
import com.slowatcoding.finio.ui.components.FinioScreen
import com.slowatcoding.finio.ui.components.FinioSelect
import com.slowatcoding.finio.ui.components.FinioTextField
import com.slowatcoding.finio.ui.components.HeaderIconButton
import com.slowatcoding.finio.ui.components.HeaderIconSpacer
import com.slowatcoding.finio.ui.components.HeaderIconTone
import com.slowatcoding.finio.ui.components.LocalConfirm
import com.slowatcoding.finio.ui.components.NumberPad
import com.slowatcoding.finio.ui.components.ScreenTitle
import com.slowatcoding.finio.ui.components.SelectOption
import com.slowatcoding.finio.ui.components.SwitchField
import com.slowatcoding.finio.ui.components.parseHexColor
import com.slowatcoding.finio.ui.components.toast
import com.slowatcoding.finio.ui.icons.LucideIcons
import com.slowatcoding.finio.ui.navigation.FinioNavigator
import com.slowatcoding.finio.ui.screens.budgets.FormField
import com.slowatcoding.finio.ui.screens.budgets.jsNumberString
import com.slowatcoding.finio.ui.theme.FinioTheme
import com.slowatcoding.finio.ui.theme.FinioType
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Loan/EMI in the default set — the sane default for a new loan's category. */
private const val DEFAULT_LOAN_CATEGORY_ID = "cat-27"
private const val DEFAULT_LOAN_CATEGORY_NAME = "Loan / EMI"

private val FLOAT_PREFIX = Regex("""^[+-]?(\d+\.?\d*|\.\d+)([eE][+-]?\d+)?""")
private val INT_PREFIX = Regex("""^[+-]?\d+""")

/** JS `parseFloat(s) || 0`. */
internal fun parseFloatOrZero(s: String): Double =
    FLOAT_PREFIX.find(jsTrim(s))?.value?.toDoubleOrNull()?.takeIf { it.isFinite() } ?: 0.0

/** JS `parseInt(s, 10) || 0`. */
private fun parseIntOrZero(s: String): Int = INT_PREFIX.find(jsTrim(s))?.value?.toIntOrNull() ?: 0

/**
 * Port of web/src/pages/AddLoan.tsx — routes `/add-loan` and `/edit-loan/:id`: name, principal on
 * the NumberPad, rate and tenure with a live EMI estimate, first EMI date, paying account and
 * category tiles. A new loan whose first EMI is already past offers "Log past EMIs as
 * transactions" → `addLoan(logPastEmis = true)` then `processRecurring()` with an Undo toast.
 *
 * @param loanId non-null on `/edit-loan/:id` (EditGuard-ed by the shell).
 */
@Composable
fun AddLoanScreen(nav: FinioNavigator, loanId: String?) {
    val store = financeStore()
    val state by collectFinanceState()
    val money = rememberMoneyFormatter()
    val confirm = LocalConfirm.current
    val scope = rememberCoroutineScope()
    val colors = FinioTheme.colors

    val categories = state.categories
    val existing = loanId?.let { id -> state.loans.find { it.id == id } }
    val openAccounts = remember(state.accounts) { activeAccounts(state.accounts) }
    val expenseCategories = remember(categories) {
        miscLast(categories.filter { isCategoryValidForType(it, TransactionType.Expense) })
    }

    var name by rememberSaveable { mutableStateOf(existing?.name ?: "") }
    var principal by rememberSaveable { mutableStateOf(existing?.principal?.let(::jsNumberString) ?: "0") }
    var interestRate by rememberSaveable { mutableStateOf(existing?.interestRate?.let(::jsNumberString) ?: "") }
    var tenureMonths by rememberSaveable { mutableStateOf(existing?.tenureMonths?.toString() ?: "") }
    var startDate by remember { mutableStateOf(existing?.startDate?.let { LocalDate.parse(localDayKey(it)) }) }
    var accountId by rememberSaveable { mutableStateOf(existing?.accountId ?: openAccounts.firstOrNull()?.id ?: "") }
    var categoryId by rememberSaveable {
        mutableStateOf(
            existing?.categoryId
                ?: categories.find { it.id == DEFAULT_LOAN_CATEGORY_ID }?.id
                ?: categories.find { it.name == DEFAULT_LOAN_CATEGORY_NAME }?.id
                ?: MISC_CATEGORY_ID,
        )
    }
    var logPastEmis by rememberSaveable { mutableStateOf(false) }
    var submitting by remember { mutableStateOf(false) }

    val parsedPrincipal = parseFloatOrZero(principal)
    val parsedRate = parseFloatOrZero(interestRate)
    val parsedTenure = parseIntOrZero(tenureMonths)
    val previewEmi = calculateEmi(parsedPrincipal, parsedRate, parsedTenure)

    // A new loan whose first EMI date is already behind us: by default those EMIs were paid
    // outside Finio; the switch posts them as real expenses instead (same choice FD/RD offer).
    val pastEmis = remember(existing == null, startDate, parsedTenure, previewEmi, accountId, categoryId) {
        val day = startDate
        if (existing != null || day == null || parsedTenure <= 0 || previewEmi <= 0 || accountId.isEmpty()) return@remember null
        val now = nowInstant()
        val preview = previewBackfill(
            RecurringTransaction(
                id = "draft", type = TransactionType.Expense, amount = previewEmi, accountId = accountId,
                categoryId = categoryId, note = "", labels = emptyList(), frequency = RecurrenceFrequency.Monthly,
                startDate = day.isoAtLocalMidnight(), maxOccurrences = parsedTenure, occurrenceCount = 0,
                lastRunDate = null, createdAt = now.toIso(),
            ),
            listOf(accountId),
            now,
        )
        preview.takeIf { it.count > 0 }
    }
    val accountName = openAccounts.find { it.id == accountId }?.name ?: "the account"

    fun handleSubmit() {
        if (submitting) return
        if (jsTrim(name).isEmpty()) {
            toast.error("Enter a loan name")
            return
        }
        if (parsedPrincipal <= 0) {
            toast.error("Enter the loan amount")
            return
        }
        if (parsedTenure <= 0) {
            toast.error("Enter the tenure in months")
            return
        }
        val day = startDate ?: run {
            toast.error("Select a start date")
            return
        }
        if (accountId.isEmpty()) {
            toast.error("Select an account")
            return
        }
        if (categoryId.isEmpty()) {
            toast.error("Select a category")
            return
        }
        submitting = true

        val data = NewLoan(
            name = cleanText(name, MAX_NAME_LENGTH),
            principal = parsedPrincipal,
            interestRate = parsedRate,
            tenureMonths = parsedTenure,
            startDate = day.isoAtLocalMidnight(),
            accountId = accountId,
            categoryId = categoryId,
        )
        if (existing != null) {
            store.updateLoan(existing.id) {
                it.copy(
                    name = data.name, principal = data.principal, interestRate = data.interestRate,
                    tenureMonths = data.tenureMonths, startDate = data.startDate, accountId = data.accountId,
                    categoryId = data.categoryId,
                )
            }
            toast.success("Loan updated")
        } else {
            val logPast = pastEmis != null && logPastEmis
            store.addLoan(data, logPastEmis = logPast)
            val posted = if (logPast) store.processRecurring() else emptyList()
            if (posted.isNotEmpty()) {
                val ids = posted.map { it.id }
                undoToast("Loan added · posted ${posted.size} past EMI${if (posted.size == 1) "" else "s"}") {
                    store.bulkDeleteTransactions(ids)
                }
            } else {
                toast.success("Loan added")
            }
        }
        nav.back()
    }

    fun handleDelete() {
        val loan = existing ?: return
        scope.launch {
            val ok = confirm.confirm("Delete \"${loan.name}\"?", DELETE_LOAN_DESCRIPTION, confirmLabel = "Delete")
            if (ok) {
                store.deleteLoan(loan.id)
                nav.back()
            }
        }
    }

    FinioScreen(header = {
        BackButton(nav)
        ScreenTitle(if (existing != null) "Edit loan" else "Add loan")
        if (existing != null) {
            HeaderIconButton(LucideIcons.Trash2, "Delete loan", onClick = ::handleDelete, tone = HeaderIconTone.Destructive)
        } else {
            HeaderIconSpacer()
        }
    }) {
        FinioCard(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                FormField("Loan name") {
                    FinioTextField(
                        name,
                        { name = stripLeading(it).take(MAX_NAME_LENGTH) },
                        Modifier.fillMaxWidth(),
                        placeholder = "e.g., Home Loan — HDFC",
                    )
                }
                FormField("Principal") { NumberPad(principal, { principal = it }) }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FormField("Interest rate (% p.a.)", Modifier.weight(1f)) {
                        FinioTextField(
                            interestRate,
                            { v -> interestRate = v.filter { it.isDigit() || it == '.' } },
                            Modifier.fillMaxWidth(),
                            placeholder = "e.g. 8.5",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        )
                    }
                    FormField("Tenure (months)", Modifier.weight(1f)) {
                        FinioTextField(
                            tenureMonths,
                            { v -> tenureMonths = v.filter { it.isDigit() }.take(4) },
                            Modifier.fillMaxWidth(),
                            placeholder = "e.g. 240",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        )
                    }
                }
                if (previewEmi > 0) {
                    Column {
                        FinioDivider()
                        Row(
                            Modifier.fillMaxWidth().padding(top = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Bottom,
                        ) {
                            Text("Estimated EMI", Modifier.weight(1f), style = FinioType.label, color = colors.mutedForeground)
                            Text(
                                buildAnnotatedString {
                                    append(money(previewEmi))
                                    withStyle(FinioType.label.toSpanStyle().copy(color = colors.mutedForeground)) { append("/month") }
                                },
                                style = FinioType.money,
                                color = colors.foreground,
                            )
                        }
                    }
                }
            }
        }

        FinioCard(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                FormField("First EMI date") {
                    FinioDatePicker(startDate, { startDate = it }, Modifier.fillMaxWidth(), placeholder = "Pick a date")
                }
                FormField("Pay EMI from") {
                    if (openAccounts.isEmpty()) {
                        Text("Add an account first.", style = FinioType.caption, color = colors.destructive)
                    } else {
                        FinioSelect(
                            accountId.ifEmpty { null },
                            openAccounts.map { SelectOption(it.id, it.name) },
                            { accountId = it },
                            Modifier.fillMaxWidth(),
                            placeholder = "Choose account",
                            title = "Pay EMI from",
                        )
                    }
                }
                FormField("Category") {
                    CategoryGrid(
                        categories = expenseCategories.map { CategoryTileData(it.id, it.name, it.icon, parseHexColor(it.color)) },
                        selectedId = categoryId,
                        onSelect = { categoryId = it },
                        maxHeight = 160.dp,
                    )
                }
            }
        }

        val past = pastEmis
        if (past != null) {
            FinioCard(Modifier.fillMaxWidth()) {
                val count = "${past.count} EMI${if (past.count == 1) "" else "s"} (${money(past.total)})"
                SwitchField(
                    title = "Log past EMIs as transactions",
                    description = if (logPastEmis) "$count will be posted from $accountName and show in its history."
                    else "$count already paid count towards the loan; $accountName is left untouched.",
                    checked = logPastEmis,
                    onCheckedChange = { logPastEmis = it },
                )
            }
        }

        FinioButton(
            if (existing != null) "Update loan" else "Add loan",
            ::handleSubmit,
            Modifier.fillMaxWidth(),
            size = ButtonSize.Lg,
        )
    }
}

