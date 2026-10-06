package com.slowatcoding.finio.ui.screens.accounts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.slowatcoding.finio.core.calc.activeAccounts
import com.slowatcoding.finio.core.calc.getTotalAccountBalance
import com.slowatcoding.finio.core.calc.getTotalCreditOutstanding
import com.slowatcoding.finio.core.calc.getTotalDepositValue
import com.slowatcoding.finio.core.deposit.isDepositAccount
import com.slowatcoding.finio.core.format.shouldCompactGroup
import com.slowatcoding.finio.core.model.Account
import com.slowatcoding.finio.core.model.AccountType
import com.slowatcoding.finio.ui.common.HideAmountsToggle
import com.slowatcoding.finio.ui.common.MoneyFormatter
import com.slowatcoding.finio.ui.common.collectFinanceState
import com.slowatcoding.finio.ui.common.financeStore
import com.slowatcoding.finio.ui.common.rememberMoneyFormatter
import com.slowatcoding.finio.ui.common.undoToast
import com.slowatcoding.finio.ui.components.FinioButton
import com.slowatcoding.finio.ui.components.FinioCard
import com.slowatcoding.finio.ui.components.FinioDivider
import com.slowatcoding.finio.ui.components.FinioScreen
import com.slowatcoding.finio.ui.components.HeaderIconButton
import com.slowatcoding.finio.ui.components.HeaderIconTone
import com.slowatcoding.finio.ui.components.LocalConfirm
import com.slowatcoding.finio.ui.components.PageTitle
import com.slowatcoding.finio.ui.components.toast
import com.slowatcoding.finio.ui.icons.LucideIcons
import com.slowatcoding.finio.ui.mudra.NoteCard
import com.slowatcoding.finio.ui.mudra.noteFigureStyle
import com.slowatcoding.finio.ui.navigation.FinioNavigator
import com.slowatcoding.finio.ui.navigation.Routes
import com.slowatcoding.finio.ui.theme.FinioTheme
import com.slowatcoding.finio.ui.theme.FinioType
import kotlinx.coroutines.launch

/**
 * Port of web/src/pages/Accounts.tsx — route `/accounts` (tab; the coin FAB is hidden here, the
 * page has its own add button). Net-balance NoteCard, then Accounts / Deposits / Credit cards
 * lists and a collapsed Archived section.
 */
@Composable
fun AccountsScreen(nav: FinioNavigator) {
    val store = financeStore()
    val state by collectFinanceState()
    val money = rememberMoneyFormatter()
    val confirm = LocalConfirm.current
    val scope = rememberCoroutineScope()
    val colors = FinioTheme.colors

    val accounts = state.accounts
    val transactions = state.transactions
    var showArchived by rememberSaveable { mutableStateOf(false) }

    val totalBalance = remember(accounts) { getTotalAccountBalance(accounts) }
    val creditDue = remember(accounts) { getTotalCreditOutstanding(accounts) }
    val depositValue = remember(accounts) { getTotalDepositValue(accounts) }
    val open = remember(accounts) { activeAccounts(accounts) }
    val regularAccounts = remember(open) { open.filter { it.type != AccountType.Credit && !isDepositAccount(it) } }
    val depositAccounts = remember(open) { open.filter { isDepositAccount(it) } }
    val creditAccounts = remember(open) { open.filter { it.type == AccountType.Credit } }
    val archivedAccounts = remember(accounts) { accounts.filter { !it.archivedAt.isNullOrEmpty() } }
    // Open accounts are all visible together, so their balances compact as one group.
    val openCompact = remember(open) { shouldCompactGroup(open.map { it.balance }) }
    val archivedCompact = remember(archivedAccounts) { shouldCompactGroup(archivedAccounts.map { it.balance }) }
    val txCountByAccount = remember(transactions) {
        val counts = HashMap<String, Int>()
        for (t in transactions) {
            if (t.accountId.isNotEmpty()) counts[t.accountId] = (counts[t.accountId] ?: 0) + 1
            val to = t.toAccountId
            if (!to.isNullOrEmpty()) counts[to] = (counts[to] ?: 0) + 1
        }
        counts
    }

    val handleDelete: (Account) -> Unit = { account ->
        val current = store.current
        if (!showDeleteBlockedToast(current.accounts, account)) {
            val txCount = current.transactions.count { it.accountId == account.id || it.toAccountId == account.id }
            scope.launch {
                val confirmed = confirm.confirm(
                    title = "Delete \"${account.name}\"?",
                    description = if (txCount > 0) {
                        "$txCount transaction${if (txCount == 1) "" else "s"} on this account will be deleted too, and this cannot be undone. Archive it instead to close the account but keep its history."
                    } else {
                        "This cannot be undone."
                    },
                    confirmLabel = "Delete permanently",
                )
                if (confirmed) store.deleteAccount(account.id)
            }
        }
    }

    val handleToggleArchive: (Account) -> Unit = { account ->
        if (!account.archivedAt.isNullOrEmpty()) {
            store.setAccountArchived(account.id, false)
            toast.success("\"${account.name}\" reopened")
        } else {
            scope.launch {
                val confirmed = confirm.confirm(
                    title = "Archive \"${account.name}\"?",
                    description = "Its transactions stay in your history, but the account drops out of pickers and running totals. You can reopen it any time.",
                    confirmLabel = "Archive",
                    destructive = false,
                )
                if (confirmed) {
                    store.setAccountArchived(account.id, true)
                    undoToast("\"${account.name}\" archived") { store.setAccountArchived(account.id, false) }
                }
            }
        }
    }

    FinioScreen(header = {
        PageTitle("Accounts")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HideAmountsToggle()
            HeaderIconButton(
                LucideIcons.Plus,
                "Add account",
                onClick = { nav.navigate(Routes.AddAccount) },
                tone = HeaderIconTone.Primary,
            )
        }
    }) {
        NoteCard(Modifier.fillMaxWidth()) {
            Text("Net balance", style = FinioType.bodyMedium, color = colors.mutedForeground)
            val total = money(totalBalance)
            Text(total, Modifier.padding(top = 4.dp), style = noteFigureStyle(total), color = colors.foreground, maxLines = 1, softWrap = false)
            if (creditAccounts.isNotEmpty()) {
                Column(Modifier.padding(top = 6.dp)) {
                    Text(
                        "${money(creditDue)} owed on ${creditAccounts.size} card${if (creditAccounts.size == 1) "" else "s"}",
                        style = FinioType.caption,
                        color = colors.mutedForeground,
                    )
                    Text("${money(totalBalance - creditDue)} after dues", style = FinioType.caption, color = colors.mutedForeground)
                }
            }
            if (depositAccounts.isNotEmpty()) {
                Text(
                    "+ ${money(depositValue)} locked in ${depositAccounts.size} deposit${if (depositAccounts.size == 1) "" else "s"}",
                    Modifier.padding(top = 4.dp),
                    style = FinioType.caption,
                    color = colors.mutedForeground,
                )
            }
        }

        val rowsFor: @Composable (List<Account>, Boolean, Boolean) -> Unit = { list, compact, withCount ->
            AccountList(
                list,
                money,
                compact,
                transactionCount = { if (withCount) txCountByAccount[it.id] ?: 0 else 0 },
                onClick = { nav.navigate(Routes.EditAccount(it.id)) },
                onDelete = handleDelete,
                onToggleArchive = handleToggleArchive,
            )
        }

        if (regularAccounts.isNotEmpty()) {
            Column {
                SectionTitle("Accounts")
                rowsFor(regularAccounts, openCompact, false)
            }
        }
        // Fixed & recurring deposits — valued at what they're worth today.
        if (depositAccounts.isNotEmpty()) {
            Column {
                SectionTitle("Deposits")
                rowsFor(depositAccounts, openCompact, false)
            }
        }
        if (creditAccounts.isNotEmpty()) {
            Column {
                SectionTitle("Credit cards")
                rowsFor(creditAccounts, openCompact, false)
            }
        }
        // Archived — collapsed, since they are closed but still hold history.
        if (archivedAccounts.isNotEmpty()) {
            Column {
                Row(
                    Modifier
                        .padding(bottom = 12.dp)
                        .clickable(role = Role.Button, onClickLabel = if (showArchived) "Collapse" else "Expand") { showArchived = !showArchived },
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        if (showArchived) LucideIcons.ChevronDown else LucideIcons.ChevronRight,
                        null,
                        Modifier.size(14.dp),
                        tint = colors.mutedForeground,
                    )
                    Text("Archived (${archivedAccounts.size})", style = FinioType.bodyMedium, color = colors.mutedForeground)
                }
                if (showArchived) rowsFor(archivedAccounts, archivedCompact, true)
            }
        }

        if (accounts.isEmpty()) {
            Column(
                Modifier.fillMaxWidth().padding(vertical = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("No accounts yet", style = FinioType.input, color = colors.mutedForeground)
                FinioButton(
                    "Add Account",
                    onClick = { nav.navigate(Routes.AddAccount) },
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        Modifier.padding(bottom = 12.dp).semantics { heading() },
        style = FinioType.title,
        color = FinioTheme.colors.foreground,
    )
}

@Composable
private fun AccountList(
    accounts: List<Account>,
    money: MoneyFormatter,
    forceCompact: Boolean,
    transactionCount: (Account) -> Int,
    onClick: (Account) -> Unit,
    onDelete: (Account) -> Unit,
    onToggleArchive: (Account) -> Unit,
) {
    FinioCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 16.dp)) {
        accounts.forEachIndexed { i, account ->
            if (i > 0) FinioDivider()
            AccountRow(
                account = account,
                money = money,
                forceCompact = forceCompact,
                transactionCount = transactionCount(account),
                onClick = { onClick(account) },
                onDelete = { onDelete(account) },
                onToggleArchive = { onToggleArchive(account) },
            )
        }
    }
}
