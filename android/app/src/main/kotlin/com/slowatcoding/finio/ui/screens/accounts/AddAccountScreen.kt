package com.slowatcoding.finio.ui.screens.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.slowatcoding.finio.core.calc.activeAccounts
import com.slowatcoding.finio.core.calc.isLiquidAccount
import com.slowatcoding.finio.core.data.COLOR_PALETTE
import com.slowatcoding.finio.core.deposit.isDepositAccount
import com.slowatcoding.finio.core.format.formatInputAmount
import com.slowatcoding.finio.core.model.AccountType
import com.slowatcoding.finio.core.store.DepositUpdate
import com.slowatcoding.finio.core.store.NewAccount
import com.slowatcoding.finio.core.store.NewDeposit
import com.slowatcoding.finio.core.util.MAX_NAME_LENGTH
import com.slowatcoding.finio.core.util.cleanText
import com.slowatcoding.finio.core.util.stripLeading
import com.slowatcoding.finio.ui.common.BackButton
import com.slowatcoding.finio.ui.common.collectFinanceState
import com.slowatcoding.finio.ui.common.financeStore
import com.slowatcoding.finio.ui.common.rememberMoneyFormatter
import com.slowatcoding.finio.ui.common.undoToast
import com.slowatcoding.finio.ui.components.ButtonSize
import com.slowatcoding.finio.ui.components.ButtonVariant
import com.slowatcoding.finio.ui.components.FinioButton
import com.slowatcoding.finio.ui.components.FinioScreen
import com.slowatcoding.finio.ui.components.FinioTextField
import com.slowatcoding.finio.ui.components.HeaderIconButton
import com.slowatcoding.finio.ui.components.HeaderIconSpacer
import com.slowatcoding.finio.ui.components.HeaderIconTone
import com.slowatcoding.finio.ui.components.LocalConfirm
import com.slowatcoding.finio.ui.components.NumberPad
import com.slowatcoding.finio.ui.components.ScreenTitle
import com.slowatcoding.finio.ui.components.toast
import com.slowatcoding.finio.ui.icons.LucideIcons
import com.slowatcoding.finio.ui.navigation.FinioNavigator
import com.slowatcoding.finio.ui.theme.FinioShapes
import com.slowatcoding.finio.ui.theme.FinioTheme
import com.slowatcoding.finio.ui.theme.FinioType
import com.slowatcoding.finio.ui.theme.cssShadow
import kotlinx.coroutines.launch
import kotlin.math.max

private data class AccountTypeOption(val value: AccountType, val label: String, val icon: String, val vector: ImageVector)

/** AddAccount.tsx `accountTypes` (FD's stored icon key stays 'vault'; it draws LockKeyhole). */
private val accountTypes: List<AccountTypeOption>
    get() = listOf(
        AccountTypeOption(AccountType.Checking, "Checking", "landmark", LucideIcons.Landmark),
        AccountTypeOption(AccountType.Savings, "Savings", "piggy-bank", LucideIcons.PiggyBank),
        AccountTypeOption(AccountType.Cash, "Cash", "banknote", LucideIcons.Banknote),
        AccountTypeOption(AccountType.Credit, "Credit card", "credit-card", LucideIcons.CreditCard),
        AccountTypeOption(AccountType.Investment, "Investment", "trending-up", LucideIcons.TrendingUp),
        AccountTypeOption(AccountType.Wallet, "Wallet", "wallet", LucideIcons.Wallet),
        AccountTypeOption(AccountType.Fd, "Fixed deposit", "vault", LucideIcons.LockKeyhole),
        AccountTypeOption(AccountType.Rd, "Recurring deposit", "calendar-clock", LucideIcons.CalendarClock),
    )

/**
 * Port of web/src/pages/AddAccount.tsx — routes `/add-account` and `/edit-account/:id`.
 * @param accountId non-null on `/edit-account/:id` (EditGuard-ed by the shell).
 */
@Composable
fun AddAccountScreen(nav: FinioNavigator, accountId: String?) {
    val store = financeStore()
    val state by collectFinanceState()
    val money = rememberMoneyFormatter()
    val confirm = LocalConfirm.current
    val scope = rememberCoroutineScope()
    val colors = FinioTheme.colors

    val accounts = state.accounts
    // Live, so a reconcile adjustment posted from here is reflected; frozen once deleted.
    val existing = accountId?.let { id -> accounts.find { it.id == id } }
    // The account as first opened seeds the form (the web's useState initialisers).
    val initial = remember { accountId?.let { id -> store.current.accounts.find { it.id == id } } }

    val owedOn = { balance: Double -> jsNumberString(max(-balance, 0.0)) }
    var name by rememberSaveable { mutableStateOf(initial?.name ?: "") }
    var type by rememberSaveable { mutableStateOf(initial?.type ?: AccountType.Checking) }
    var balance by rememberSaveable {
        mutableStateOf(if (initial == null || initial.type == AccountType.Credit) "0" else jsNumberString(initial.balance))
    }
    var due by rememberSaveable { mutableStateOf(if (initial?.type == AccountType.Credit) owedOn(initial.balance) else "0") }
    // `balance`/`due` hold only what was typed; until then the form follows the store.
    var balanceDirty by rememberSaveable { mutableStateOf(false) }
    val followStore = existing != null && !balanceDirty && existing.type == type
    val shownBalance = if (followStore && type != AccountType.Credit) jsNumberString(existing!!.balance) else balance
    val shownDue = if (followStore && type == AccountType.Credit) owedOn(existing!!.balance) else due
    // Accounts no longer pick a colour (the tint comes from the type); keep the stored one.
    val color = initial?.color ?: COLOR_PALETTE[0]
    var creditLimit by rememberSaveable { mutableStateOf(initial?.creditLimit?.let(::jsNumberString) ?: "0") }
    var statementCloseDay by rememberSaveable { mutableStateOf(initial?.statementCloseDay?.toString() ?: "") }
    var paymentDueDays by rememberSaveable { mutableStateOf(initial?.paymentDueDays?.toString() ?: "") }
    var minimumDuePercent by rememberSaveable { mutableStateOf(initial?.minimumDuePercent?.let(::jsNumberString) ?: "") }
    var showReconcile by rememberSaveable { mutableStateOf(false) }
    var nameError by rememberSaveable { mutableStateOf<String?>(null) }
    var creditFieldIsDue by rememberSaveable { mutableStateOf(true) }
    val nameFocus = remember { FocusRequester() }
    val showNameError = { message: String ->
        nameError = message
        runCatching { nameFocus.requestFocus() }
        Unit
    }

    // Spendable accounts only; an existing deposit keeps showing its linked account even if closed.
    val linkableAccounts = remember(accounts, initial) {
        accounts.filter { isLiquidAccount(it) && (it.archivedAt.isNullOrEmpty() || it.id == initial?.deposit?.linkedAccountId) }
    }
    var depositForm by remember {
        mutableStateOf(
            depositFormFromAccount(initial, activeAccounts(store.current.accounts).filter { isLiquidAccount(it) }.firstOrNull()?.id ?: ""),
        )
    }

    val isDepositType = type == AccountType.Fd || type == AccountType.Rd
    val depositTerms = if (isDepositType) depositTermsFromForm(type, depositForm) else null
    var submitting by remember { mutableStateOf(false) }

    val isDuplicateName = {
        val key = name.trim().lowercase()
        accounts.any { it.id != existing?.id && it.archivedAt.isNullOrEmpty() && it.name.trim().lowercase() == key }
    }
    // Converting between a deposit and a regular account would orphan its terms or its history.
    val typeOptions = when {
        initial == null -> accountTypes
        isDepositAccount(initial) -> accountTypes.filter { it.value == initial.type }
        else -> accountTypes.filter { !isDepositAccount(it.value) }
    }

    val submitDeposit = submit@{
        val terms = depositTerms ?: return@submit
        submitting = true
        if (existing != null) {
            store.updateDeposit(
                existing.id,
                DepositUpdate(
                    name = cleanText(name, MAX_NAME_LENGTH),
                    color = color,
                    interestRate = terms.interestRate,
                    compounding = terms.compounding,
                    maturityDate = terms.maturityDate,
                ),
            )
            toast.success("Account updated")
            nav.back()
            return@submit
        }
        store.addDeposit(
            NewDeposit(
                type = type,
                name = cleanText(name, MAX_NAME_LENGTH),
                color = color,
                terms = terms,
                deductPast = depositForm.deductPast,
            ),
        )
        if (type == AccountType.Rd && depositForm.deductPast) {
            val posted = store.processRecurring()
            if (posted.isNotEmpty()) {
                val ids = posted.map { it.id }
                undoToast("Posted ${posted.size} past installment${if (posted.size == 1) "" else "s"}") {
                    store.bulkDeleteTransactions(ids)
                }
            }
        }
        toast.success("Account added")
        nav.back()
    }

    val handleSubmit = submit@{
        if (submitting) return@submit
        if (name.isBlank()) {
            showNameError("Enter a name")
            return@submit
        }
        if (isDuplicateName()) {
            showNameError("An account with this name already exists")
            return@submit
        }
        if (isDepositType) {
            if (depositTerms == null) {
                toast.error(
                    if (type == AccountType.Fd) {
                        "Enter the amount, rate, linked account, and a maturity date after the start date"
                    } else {
                        "Enter the installment, rate, linked account, and tenure in months"
                    },
                )
                return@submit
            }
            submitDeposit()
            return@submit
        }
        submitting = true

        val isCredit = type == AccountType.Credit
        val data = NewAccount(
            name = cleanText(name, MAX_NAME_LENGTH),
            type = type,
            // Untouched balance on an existing account is written back exactly as stored.
            balance = when {
                followStore && existing != null -> existing.balance
                isCredit -> 0.0 - (jsParseFloat(shownDue) ?: 0.0)
                else -> jsParseFloat(shownBalance) ?: 0.0
            },
            color = color,
            icon = existing?.icon ?: accountTypes.find { it.value == type }?.icon ?: "landmark",
            creditLimit = if (isCredit) jsParseFloat(creditLimit)?.takeIf { it != 0.0 } else null,
            statementCloseDay = if (isCredit && statementCloseDay.isNotBlank()) jsParseInt(statementCloseDay)?.coerceIn(1, 28) else null,
            paymentDueDays = if (isCredit && paymentDueDays.isNotBlank()) jsParseInt(paymentDueDays)?.let { max(0, it) } else null,
            minimumDuePercent = if (isCredit && minimumDuePercent.isNotBlank()) jsParseFloat(minimumDuePercent)?.let { max(0.0, it) } else null,
        )

        if (existing != null) {
            store.updateAccount(existing.id) {
                it.copy(
                    name = data.name,
                    type = data.type,
                    balance = data.balance,
                    color = data.color,
                    icon = data.icon,
                    creditLimit = data.creditLimit,
                    statementCloseDay = data.statementCloseDay,
                    paymentDueDays = data.paymentDueDays,
                    minimumDuePercent = data.minimumDuePercent,
                )
            }
            toast.success("Account updated")
        } else {
            store.addAccount(data)
            toast.success("Account added")
        }
        nav.back()
    }

    val handleDelete: () -> Unit = delete@{
        val target = existing ?: return@delete
        if (showDeleteBlockedToast(store.current.accounts, target)) return@delete
        scope.launch {
            val confirmed = confirm.confirm(
                title = "Delete \"${target.name}\"?",
                description = if (isDepositAccount(target)) {
                    "Every transaction on this deposit will be deleted, including the money moved into it — that money returns to the linked account. This cannot be undone."
                } else {
                    "Every transaction on this account will be deleted as well. This cannot be undone."
                },
                confirmLabel = "Delete",
            )
            if (confirmed) {
                store.deleteAccount(target.id)
                nav.back()
            }
        }
    }

    FinioScreen(header = {
        BackButton(nav)
        ScreenTitle(if (initial != null) "Edit Account" else "Add Account")
        if (initial != null) {
            HeaderIconButton(LucideIcons.Trash2, "Delete", onClick = handleDelete, tone = HeaderIconTone.Destructive)
        } else {
            HeaderIconSpacer()
        }
    }) {
        // Name
        Column {
            FieldLabel("Account name")
            FinioTextField(
                value = name,
                onValueChange = {
                    name = stripLeading(it).take(MAX_NAME_LENGTH)
                    nameError = null
                },
                modifier = Modifier.focusRequester(nameFocus),
                placeholder = "e.g., HDFC Savings",
                isError = nameError != null,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            )
            nameError?.let {
                Text(it, Modifier.padding(top = 6.dp), style = FinioType.label, color = colors.destructive)
            }
        }

        // Type
        Column {
            FieldLabel("Account type")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                typeOptions.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { t ->
                            TypeTile(t, selected = type == t.value, onClick = { type = t.value }, modifier = Modifier.weight(1f))
                        }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }

        // Deposit terms replace the balance — the deposit is funded by real transfers.
        when {
            isDepositType -> DepositFields(
                type = type,
                values = depositForm,
                onChange = { depositForm = it },
                linkableAccounts = linkableAccounts,
                locked = initial != null,
                money = money,
            )
            type == AccountType.Credit -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CreditFieldToggle(
                    dueSelected = creditFieldIsDue,
                    dueValue = shownDue,
                    limitValue = creditLimit,
                    onSelect = { creditFieldIsDue = it },
                )
                if (creditFieldIsDue) {
                    NumberPad(shownDue, {
                        balanceDirty = true
                        due = it
                    })
                } else {
                    NumberPad(creditLimit, { creditLimit = it })
                }
            }
            else -> Column {
                FieldLabel("Current balance")
                NumberPad(shownBalance, {
                    balanceDirty = true
                    balance = it
                })
            }
        }

        // Statement cycle — optional, unlocks the Dashboard payment-due card.
        if (type == AccountType.Credit) {
            Column {
                FieldLabel("Statement cycle (optional)")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CycleField("Closes on", statementCloseDay, { statementCloseDay = it }, "e.g. 5", KeyboardType.Number, Modifier.weight(1f))
                    CycleField("Due after (days)", paymentDueDays, { paymentDueDays = it }, "e.g. 20", KeyboardType.Number, Modifier.weight(1f))
                    CycleField("Min due %", minimumDuePercent, { minimumDuePercent = it }, "5", KeyboardType.Decimal, Modifier.weight(1f))
                }
                Text(
                    "Set a close day and due offset to see a \"payment due\" reminder on the Dashboard.",
                    Modifier.padding(top = 6.dp),
                    style = FinioType.caption,
                    color = colors.mutedForeground,
                )
            }
        }

        FinioButton(
            if (initial != null) "Update Account" else "Add Account",
            onClick = handleSubmit,
            modifier = Modifier.fillMaxWidth(),
            size = ButtonSize.Lg,
        )

        if (existing != null && !isDepositAccount(existing)) {
            FinioButton(
                "Reconcile Balance",
                onClick = { showReconcile = true },
                modifier = Modifier.fillMaxWidth(),
                variant = ButtonVariant.Outline,
                size = ButtonSize.Lg,
                leadingIcon = LucideIcons.Scale,
            )
        }
    }

    if (showReconcile && existing != null) {
        ReconcileAccountDialog(existing, onDismissRequest = { showReconcile = false })
    }
}

@Composable
private fun TypeTile(option: AccountTypeOption, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val colors = FinioTheme.colors
    val shape = FinioShapes.md
    val content = if (selected) Color.White else colors.foreground
    Column(
        modifier
            .semantics { this.selected = selected }
            .cssShadow(shape, if (selected) FinioTheme.shadows.glowPrimary else emptyList())
            .clip(shape)
            .then(
                if (selected) Modifier.background(FinioTheme.brushes.gradPrimary)
                else Modifier.background(colors.glassStrong).border(1.dp, colors.glassBorder, shape),
            )
            .clickable(remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(option.vector, null, Modifier.padding(bottom = 4.dp).size(20.dp), tint = content)
        Text(option.label, style = FinioType.caption, color = content, textAlign = TextAlign.Center, maxLines = 2)
    }
}

/** The Current due / Credit limit pill switch (`bg-muted grid grid-cols-2 rounded-full p-1`). */
@Composable
private fun CreditFieldToggle(dueSelected: Boolean, dueValue: String, limitValue: String, onSelect: (Boolean) -> Unit) {
    val colors = FinioTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(FinioShapes.full)
            .background(colors.muted)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        listOf(Triple(true, "Current due", dueValue), Triple(false, "Credit limit", limitValue)).forEach { (isDue, label, value) ->
            val active = dueSelected == isDue
            Column(
                Modifier
                    .weight(1f)
                    .semantics { selected = active }
                    .cssShadow(FinioShapes.full, if (active) FinioTheme.shadows.glowPrimary else emptyList())
                    .clip(FinioShapes.full)
                    .then(if (active) Modifier.background(FinioTheme.brushes.gradPrimary) else Modifier)
                    .clickable(remember { MutableInteractionSource() }, indication = null, role = Role.Button) { onSelect(isDue) }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val ink = if (active) Color.White else colors.mutedForeground
                Text(label, style = FinioType.label, color = ink)
                Text(formatInputAmount(value).ifEmpty { "0" }, style = FinioType.rowValue, color = ink)
            }
        }
    }
}

@Composable
private fun CycleField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboard: KeyboardType,
    modifier: Modifier,
) {
    Column(modifier) {
        Text(label, Modifier.padding(bottom = 4.dp), style = FinioType.caption, color = FinioTheme.colors.mutedForeground, maxLines = 1)
        FinioTextField(
            value,
            onValueChange,
            placeholder = placeholder,
            keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        )
    }
}
