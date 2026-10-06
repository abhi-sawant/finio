package com.slowatcoding.finio.ui.screens.debts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.slowatcoding.finio.core.calc.PersonBalance
import com.slowatcoding.finio.core.calc.activeAccounts
import com.slowatcoding.finio.core.calc.computePersonBalance
import com.slowatcoding.finio.core.data.COLOR_PALETTE
import com.slowatcoding.finio.core.format.formatDayMonth
import com.slowatcoding.finio.core.format.formatShortDate
import com.slowatcoding.finio.core.format.toLocalDateTimeInputValue
import com.slowatcoding.finio.core.js.finioZone
import com.slowatcoding.finio.core.js.nowInstant
import com.slowatcoding.finio.core.js.toIso
import com.slowatcoding.finio.core.model.DebtEntry
import com.slowatcoding.finio.core.model.Person
import com.slowatcoding.finio.core.store.DebtEntryUpdate
import com.slowatcoding.finio.core.store.NewDebtEntry
import com.slowatcoding.finio.core.store.NewPerson
import com.slowatcoding.finio.core.util.MAX_NAME_LENGTH
import com.slowatcoding.finio.core.util.MAX_NOTE_LENGTH
import com.slowatcoding.finio.core.util.cleanText
import com.slowatcoding.finio.core.util.stripLeading
import com.slowatcoding.finio.ui.common.BackButton
import com.slowatcoding.finio.ui.common.HideAmountsToggle
import com.slowatcoding.finio.ui.common.MoneyFormatter
import com.slowatcoding.finio.ui.common.collectFinanceState
import com.slowatcoding.finio.ui.common.financeStore
import com.slowatcoding.finio.ui.common.rememberMoneyFormatter
import com.slowatcoding.finio.ui.common.undoToast
import com.slowatcoding.finio.ui.components.ButtonVariant
import com.slowatcoding.finio.ui.components.FinioCard
import com.slowatcoding.finio.ui.components.FinioDateTimePicker
import com.slowatcoding.finio.ui.components.FinioDialog
import com.slowatcoding.finio.ui.components.FinioScreen
import com.slowatcoding.finio.ui.components.FinioSelect
import com.slowatcoding.finio.ui.components.FinioTextField
import com.slowatcoding.finio.ui.components.HeaderIconButton
import com.slowatcoding.finio.ui.components.HeaderIconTone
import com.slowatcoding.finio.ui.components.LocalConfirm
import com.slowatcoding.finio.ui.components.NumberPad
import com.slowatcoding.finio.ui.components.PersonIconNames
import com.slowatcoding.finio.ui.components.ScreenTitle
import com.slowatcoding.finio.ui.components.SelectOption
import com.slowatcoding.finio.ui.components.toast
import com.slowatcoding.finio.ui.icons.LucideIcons
import com.slowatcoding.finio.ui.navigation.FinioNavigator
import com.slowatcoding.finio.ui.screens.budgets.AccentTintButton
import com.slowatcoding.finio.ui.screens.budgets.CardIconAction
import com.slowatcoding.finio.ui.screens.budgets.ColorSwatches
import com.slowatcoding.finio.ui.screens.budgets.DisclosurePanel
import com.slowatcoding.finio.ui.screens.budgets.DisclosureToggle
import com.slowatcoding.finio.ui.screens.budgets.FormActions
import com.slowatcoding.finio.ui.screens.budgets.FormField
import com.slowatcoding.finio.ui.screens.budgets.IconChoiceGrid
import com.slowatcoding.finio.ui.screens.budgets.Micro
import com.slowatcoding.finio.ui.screens.budgets.PlanningEmpty
import com.slowatcoding.finio.ui.screens.budgets.PositiveTintButton
import com.slowatcoding.finio.ui.screens.budgets.TintButton
import com.slowatcoding.finio.ui.screens.budgets.jsNumberString
import com.slowatcoding.finio.ui.screens.goals.LedgerRow
import com.slowatcoding.finio.ui.theme.FinioShapes
import com.slowatcoding.finio.ui.theme.FinioTheme
import com.slowatcoding.finio.ui.theme.FinioType
import com.slowatcoding.finio.ui.theme.cssShadow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.abs

private enum class EntryMode { Lend, Borrow }

/** One dialog for logging a new entry and editing an existing one ([editing] set). */
private data class EntryTarget(val person: Person, val mode: EntryMode, val editing: DebtEntry? = null)

private fun inputValue(iso: String): LocalDateTime = LocalDateTime.parse(toLocalDateTimeInputValue(iso))

/**
 * Port of web/src/pages/Debts.tsx — route `/debts`: the inline person form above one card per
 * person (open balances first, biggest first) with They owe me / I owe them, Settle up (a real
 * transaction via core `settleUp`) and an expandable ledger whose entries edit and delete with
 * Undo. A settled entry keeps its direction; editing it also moves its linked transaction.
 */
@Composable
fun DebtsScreen(nav: FinioNavigator) {
    val store = financeStore()
    val state by collectFinanceState()
    val money = rememberMoneyFormatter()
    val confirm = LocalConfirm.current
    val scope = rememberCoroutineScope()
    val colors = FinioTheme.colors

    val people = state.people
    val debtEntries = state.debtEntries
    val accounts = state.accounts

    var showForm by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var name by rememberSaveable { mutableStateOf("") }
    var icon by rememberSaveable { mutableStateOf(PersonIconNames.first()) }
    var color by rememberSaveable { mutableStateOf(COLOR_PALETTE.first()) }
    var expandedId by rememberSaveable { mutableStateOf<String?>(null) }

    var entryPerson by remember { mutableStateOf<EntryTarget?>(null) }
    var entryAmount by remember { mutableStateOf("") }
    var entryNote by remember { mutableStateOf("") }
    var entryDate by remember { mutableStateOf<LocalDateTime?>(null) }

    var settlePerson by remember { mutableStateOf<PersonBalance?>(null) }
    var settleAmount by remember { mutableStateOf("") }
    var settleAccountId by remember { mutableStateOf("") }
    var settleNote by remember { mutableStateOf("") }

    val openAccounts = remember(accounts) { activeAccounts(accounts) }
    val balances = remember(people, debtEntries) { people.map { computePersonBalance(it, debtEntries) } }
    // Anyone with an open balance first (bigger balances first), settled-up people trail behind.
    val sortedBalances = remember(balances) {
        balances.sortedWith { a, b ->
            val aSettled = a.balance == 0.0
            val bSettled = b.balance == 0.0
            if (aSettled != bSettled) (if (aSettled) 1 else -1) else abs(b.balance).compareTo(abs(a.balance))
        }
    }

    fun resetForm() {
        showForm = false
        editingId = null
        name = ""
        icon = PersonIconNames.first()
        color = COLOR_PALETTE.first()
    }

    fun startCreate() {
        resetForm()
        showForm = true
    }

    fun startEdit(person: Person) {
        editingId = person.id
        name = person.name
        icon = person.icon
        color = person.color
        showForm = true
    }

    fun handleSubmit() {
        val clean = cleanText(name, MAX_NAME_LENGTH)
        if (clean.isEmpty()) {
            toast.error("Enter a name")
            return
        }
        val id = editingId
        if (id != null) {
            store.updatePerson(id) { it.copy(name = clean, icon = icon, color = color) }
            toast.success("Person updated")
        } else {
            store.addPerson(NewPerson(clean, icon, color))
            toast.success("Person added")
        }
        resetForm()
    }

    fun openEntry(person: Person, mode: EntryMode) {
        entryPerson = EntryTarget(person, mode)
        entryAmount = ""
        entryNote = ""
        entryDate = inputValue(nowInstant().toIso())
    }

    fun openEditEntry(person: Person, entry: DebtEntry) {
        entryPerson = EntryTarget(person, if (entry.amount < 0) EntryMode.Borrow else EntryMode.Lend, entry)
        entryAmount = jsNumberString(abs(entry.amount))
        entryNote = entry.note
        entryDate = inputValue(entry.date)
    }

    fun handleEntrySubmit() {
        val target = entryPerson ?: return
        val parsed = entryAmount.toDoubleOrNull() ?: 0.0
        if (parsed.isNaN() || parsed <= 0) {
            toast.error("Enter a valid amount")
            return
        }
        val whenLocal = entryDate
        if (whenLocal == null) {
            toast.error("Choose a date")
            return
        }
        val whenIso = whenLocal.atZone(finioZone).toInstant().toIso()
        val editing = target.editing
        if (editing != null) {
            val previous = DebtEntryUpdate(editing.amount, editing.date, editing.note)
            // A settled entry keeps its direction (the store enforces it too); a plain one can flip.
            val negative = if (!editing.settledTransactionId.isNullOrEmpty()) editing.amount < 0 else target.mode == EntryMode.Borrow
            store.updateDebtEntry(
                editing.id,
                DebtEntryUpdate(
                    amount = if (negative) -parsed else parsed,
                    // The picker is minute-precise: an untouched field keeps the stored timestamp
                    // exactly, so a note-only edit never nudges the linked transaction's date.
                    date = if (whenLocal == inputValue(editing.date)) editing.date else whenIso,
                    note = cleanText(entryNote, MAX_NOTE_LENGTH),
                ),
            )
            // Writing the old values back restores the linked transaction through the same sync.
            undoToast("Entry updated") { store.updateDebtEntry(editing.id, previous) }
            entryPerson = null
            return
        }
        store.addDebtEntry(
            NewDebtEntry(
                personId = target.person.id,
                // Lending them money increases what they owe you; borrowing increases what you owe them.
                amount = if (target.mode == EntryMode.Borrow) -parsed else parsed,
                date = whenIso,
                note = cleanText(entryNote, MAX_NOTE_LENGTH),
            ),
        )
        toast.success(if (target.mode == EntryMode.Borrow) "Borrowing logged" else "Lending logged")
        entryPerson = null
    }

    fun openSettle(status: PersonBalance) {
        settlePerson = status
        settleAmount = jsNumberString(abs(status.balance))
        settleAccountId = openAccounts.firstOrNull()?.id ?: ""
        settleNote = ""
    }

    // Settling more than is owed would flip the relationship (they owed you, now you owe them).
    val settleLimit = settlePerson?.let { abs(it.balance) } ?: 0.0
    val settleOverLimit = (settleAmount.toDoubleOrNull()?.takeUnless { it.isNaN() } ?: 0.0) > settleLimit + 0.005

    fun handleSettleSubmit() {
        val status = settlePerson ?: return
        val parsed = settleAmount.toDoubleOrNull() ?: 0.0
        if (parsed.isNaN() || parsed <= 0) {
            toast.error("Enter a valid amount")
            return
        }
        if (settleOverLimit) {
            toast.error("Only ${money(settleLimit)} is outstanding")
            return
        }
        if (settleAccountId.isEmpty()) {
            toast.error("Choose an account")
            return
        }
        val result = store.settleUp(status.person.id, parsed, settleAccountId, settleNote)
        if (result == null) {
            toast.error("Only ${money(settleLimit)} is outstanding")
            return
        }
        // Deleting the settlement transaction takes its balancing entry with it.
        undoToast("Settled ${money(parsed)} with ${status.person.name}") { store.deleteTransaction(result.transactionId) }
        settlePerson = null
    }

    FinioScreen(header = {
        BackButton(nav)
        ScreenTitle("Debts & lending")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HideAmountsToggle()
            HeaderIconButton(
                LucideIcons.Plus,
                "Add person",
                onClick = { if (showForm) resetForm() else startCreate() },
                tone = HeaderIconTone.Primary,
            )
        }
    }) {
        if (showForm) {
            FinioCard(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    FormField("Name") {
                        FinioTextField(
                            name,
                            { name = stripLeading(it).take(MAX_NAME_LENGTH) },
                            Modifier.fillMaxWidth(),
                            placeholder = "e.g., Rahul",
                        )
                    }
                    FormField("Icon") { IconChoiceGrid(PersonIconNames, icon) { icon = it } }
                    FormField("Color") { ColorSwatches(color) { color = it } }
                    FormActions(
                        if (editingId != null) "Save changes" else "Save",
                        ::handleSubmit,
                        ::resetForm,
                        cancelVariant = ButtonVariant.Secondary,
                    )
                }
            }
        }

        if (sortedBalances.isEmpty()) {
            PlanningEmpty(LucideIcons.HandCoins, "No one on your ledger yet", "Add your first person", ::startCreate)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                sortedBalances.forEach { status ->
                    val person = status.person
                    PersonCard(
                        status = status,
                        entries = debtEntries.filter { it.personId == person.id },
                        money = money,
                        expanded = expandedId == person.id,
                        onToggleHistory = { expandedId = if (expandedId == person.id) null else person.id },
                        onLend = { openEntry(person, EntryMode.Lend) },
                        onBorrow = { openEntry(person, EntryMode.Borrow) },
                        onSettle = { openSettle(status) },
                        onEdit = { startEdit(person) },
                        onDelete = {
                            scope.launch {
                                val ok = confirm.confirm(
                                    "Delete \"${person.name}\"?",
                                    "Every debt entry logged against this person will be deleted too. This cannot be undone.",
                                    confirmLabel = "Delete person",
                                )
                                if (ok) store.deletePerson(person.id)
                            }
                        },
                        onEditEntry = { openEditEntry(person, it) },
                        onDeleteEntry = { id ->
                            val removed = store.deleteDebtEntry(id)
                            if (removed != null) {
                                // A settle-up entry takes the real transaction it created with it.
                                val message = if (!removed.settledTransactionId.isNullOrEmpty()) "Settlement and its transaction removed" else "Entry removed"
                                undoToast(message) { store.restoreDebtEntry(removed) }
                            }
                        },
                    )
                }
            }
        }
    }

    // Lend / borrow entry dialog
    entryPerson?.let { target ->
        val editing = target.editing
        FinioDialog(
            onDismissRequest = { entryPerson = null },
            title = if (editing != null) "Edit entry"
            else "${if (target.mode == EntryMode.Borrow) "Borrowed from" else "Lent to"} ${target.person.name}",
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (editing != null && !editing.settledTransactionId.isNullOrEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "Settled up · " + if (editing.amount < 0) "received from ${target.person.name}" else "paid to ${target.person.name}",
                            style = FinioType.bodyMedium,
                            color = colors.foreground,
                        )
                        Text(
                            "Changing the amount or date also updates the linked transaction.",
                            style = FinioType.caption,
                            color = colors.mutedForeground,
                        )
                    }
                } else if (editing != null) {
                    DirectionPills(target.mode) { entryPerson = target.copy(mode = it) }
                }
                NumberPad(entryAmount, { entryAmount = it })
                FinioDateTimePicker(entryDate, { entryDate = it.truncatedTo(ChronoUnit.MINUTES) }, Modifier.fillMaxWidth())
                FinioTextField(
                    entryNote,
                    { entryNote = stripLeading(it).take(MAX_NOTE_LENGTH) },
                    Modifier.fillMaxWidth(),
                    placeholder = "Note (optional)",
                )
                FormActions(
                    if (editing != null) "Save changes" else "Save",
                    ::handleEntrySubmit,
                    { entryPerson = null },
                    cancelVariant = ButtonVariant.Secondary,
                )
            }
        }
    }

    // Settle up dialog
    settlePerson?.let { status ->
        FinioDialog(onDismissRequest = { settlePerson = null }, title = "Settle up with ${status.person.name}") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    if (status.balance > 0) "They owe you ${money(status.balance)}. Record what they paid you back."
                    else "You owe ${money(abs(status.balance))}. Record what you paid them.",
                    style = FinioType.caption,
                    color = colors.mutedForeground,
                )
                NumberPad(settleAmount, { settleAmount = it })
                if (settleOverLimit) {
                    Text(
                        "That's more than the ${money(settleLimit)} outstanding.",
                        style = FinioType.caption,
                        color = colors.destructive,
                    )
                }
                if (openAccounts.isEmpty()) {
                    Text(
                        "Add an account first — settling up records a real transaction.",
                        style = FinioType.caption,
                        color = colors.destructive,
                    )
                } else {
                    FinioSelect(
                        settleAccountId.ifEmpty { null },
                        openAccounts.map { SelectOption(it.id, it.name) },
                        { settleAccountId = it },
                        Modifier.fillMaxWidth(),
                        placeholder = "Choose account",
                        title = "Account",
                    )
                }
                FinioTextField(
                    settleNote,
                    { settleNote = stripLeading(it).take(MAX_NOTE_LENGTH) },
                    Modifier.fillMaxWidth(),
                    placeholder = "Note (optional)",
                )
                FormActions(
                    "Settle",
                    ::handleSettleSubmit,
                    { settlePerson = null },
                    cancelVariant = ButtonVariant.Secondary,
                    saveEnabled = openAccounts.isNotEmpty() && !settleOverLimit,
                )
            }
        }
    }
}

/** The edit dialog's direction switch (`bg-muted rounded-full p-1`, selected = card + shadow-sm + tone). */
@Composable
private fun DirectionPills(mode: EntryMode, onSelect: (EntryMode) -> Unit) {
    val colors = FinioTheme.colors
    Row(
        Modifier.fillMaxWidth().clip(FinioShapes.full).background(colors.muted).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        listOf(
            Triple(EntryMode.Lend, "They owe me", colors.positive),
            Triple(EntryMode.Borrow, "I owe them", colors.destructive),
        ).forEach { (value, label, tone) ->
            val active = mode == value
            Box(
                Modifier
                    .weight(1f)
                    .height(32.dp)
                    .cssShadow(FinioShapes.full, if (active) FinioTheme.shadows.sm else emptyList())
                    .clip(FinioShapes.full)
                    .background(if (active) colors.card else Color.Transparent)
                    .semantics { selected = active }
                    .clickable(role = Role.RadioButton) { onSelect(value) },
                contentAlignment = Alignment.Center,
            ) {
                Text(label, style = FinioType.label, color = if (active) tone else colors.mutedForeground)
            }
        }
    }
}

@Composable
private fun PersonCard(
    status: PersonBalance,
    entries: List<DebtEntry>,
    money: MoneyFormatter,
    expanded: Boolean,
    onToggleHistory: () -> Unit,
    onLend: () -> Unit,
    onBorrow: () -> Unit,
    onSettle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onEditEntry: (DebtEntry) -> Unit,
    onDeleteEntry: (String) -> Unit,
) {
    val colors = FinioTheme.colors
    val person = status.person
    val balance = status.balance
    val isSettled = balance == 0.0
    val theyOweYou = balance > 0
    FinioCard(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(person.name, style = FinioType.bodyMedium, color = colors.foreground, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    status.lastActivity?.let { "Last activity ${formatShortDate(it)}" } ?: "No activity yet",
                    style = Micro, color = colors.mutedForeground, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            CardIconAction(LucideIcons.Pencil, "Edit ${person.name}", colors.mutedForeground, onEdit)
            CardIconAction(LucideIcons.Trash2, "Delete ${person.name}", colors.destructive, onDelete)
        }
        Text(
            when {
                isSettled -> "Settled up"
                theyOweYou -> "Owes you ${money(balance)}"
                else -> "You owe ${money(abs(balance))}"
            },
            Modifier.padding(bottom = 12.dp),
            style = FinioType.bodyMedium,
            color = when {
                isSettled -> colors.mutedForeground
                theyOweYou -> colors.positive
                else -> colors.destructive
            },
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PositiveTintButton("They owe me", LucideIcons.Plus, onLend, Modifier.weight(1f))
            TintButton("I owe them", onBorrow, Modifier.weight(1f), icon = LucideIcons.Minus)
        }
        if (!isSettled) {
            AccentTintButton("Settle up", onSettle, Modifier.fillMaxWidth().padding(top = 8.dp))
        }

        DisclosureToggle("History", expanded, onToggleHistory)

        if (expanded) {
            DisclosurePanel {
                if (entries.isEmpty()) {
                    Text("No entries logged yet.", style = Micro, color = colors.mutedForeground)
                } else {
                    entries.forEach { e ->
                        LedgerRow(
                            date = formatDayMonth(e.date),
                            label = e.note.ifEmpty {
                                when {
                                    !e.settledTransactionId.isNullOrEmpty() -> "Settled up"
                                    e.amount < 0 -> "You owe more"
                                    else -> "They owe more"
                                }
                            },
                            amount = (if (e.amount < 0) "-" else "+") + money(abs(e.amount), compact = true),
                            negative = e.amount < 0,
                            onEdit = { onEditEntry(e) },
                            editLabel = "Edit entry",
                            onDelete = { onDeleteEntry(e.id) },
                            deleteLabel = "Delete entry",
                        )
                    }
                }
            }
        }
    }
}
