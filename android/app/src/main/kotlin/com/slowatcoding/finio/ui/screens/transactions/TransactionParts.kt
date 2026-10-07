package com.slowatcoding.finio.ui.screens.transactions

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.slowatcoding.finio.core.model.Account
import com.slowatcoding.finio.core.model.Category
import com.slowatcoding.finio.core.model.Label
import com.slowatcoding.finio.core.model.Transaction
import com.slowatcoding.finio.core.model.TransactionType
import com.slowatcoding.finio.ui.common.ActionMenu
import com.slowatcoding.finio.ui.common.MenuAction
import com.slowatcoding.finio.ui.common.MoneyFormatter
import com.slowatcoding.finio.ui.components.RowLabel
import com.slowatcoding.finio.ui.components.SelectOption
import com.slowatcoding.finio.ui.components.TransactionRow
import com.slowatcoding.finio.ui.components.parseHexColor
import com.slowatcoding.finio.ui.icons.LucideIcons
import com.slowatcoding.finio.ui.mudra.label

/** JS `parseFloat`: the longest numeric prefix after leading whitespace, NaN when there is none. */
internal fun parseFloatJs(raw: String): Double {
    val m = Regex("""^\s*[+-]?(\d+\.?\d*|\.\d+)([eE][+-]?\d+)?""").find(raw) ?: return Double.NaN
    return m.value.trim().toDoubleOrNull() ?: Double.NaN
}

/** An `<input maxLength>`: refuse the edit past [max] code points (keep the previous value). */
internal fun withinLength(value: String, max: Int): Boolean = value.codePointCount(0, value.length) <= max

/** An account in a picker: its name, with its human type name ("Credit card") muted — below it in the list, inline in the closed field (AddTransaction.tsx `accountOption`). */
internal fun accountOptions(accounts: List<Account>): List<SelectOption<String>> =
    accounts.map { SelectOption(it.id, it.name, description = it.type.label, selectedTrailing = it.type.label) }

/** TransactionItem.tsx's long-press actions. */
internal enum class TransactionRowAction { Select, Duplicate, Template, Delete }

/**
 * TransactionItem.tsx: the row's title/subtitle/labels derived from the lookups, plus the
 * long-press menu (Select / Duplicate / Save as template / Delete) — disabled in selection mode.
 */
@Composable
internal fun TransactionItem(
    transaction: Transaction,
    categories: Map<String, Category>,
    accounts: Map<String, Account>,
    labels: Map<String, Label>,
    money: MoneyFormatter,
    onClick: () -> Unit,
    onLongPressAction: ((TransactionRowAction, Transaction) -> Unit)?,
    selectionMode: Boolean = false,
    selected: Boolean = false,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val t = transaction
    val isSplit = !t.splits.isNullOrEmpty()
    val category = categories[t.categoryId]
    val account = accounts[t.accountId]
    val toAccount = t.toAccountId?.takeIf { it.isNotEmpty() }?.let { accounts[it] }
    val splitTitle = if (isSplit) t.splits!!.joinToString(" + ") { categories[it.categoryId]?.name ?: "Unknown" } else null
    val primary = t.note.ifEmpty { splitTitle ?: category?.name ?: "Transaction" }
    val secondary = if (t.type == TransactionType.Transfer && toAccount != null) {
        "${account?.name ?: "?"} → ${toAccount.name}"
    } else {
        "${splitTitle ?: category?.name ?: "Uncategorized"} · ${account?.name ?: "Unknown account"}"
    }
    val rowLabels = remember(t.labels, labels) {
        t.labels.mapNotNull { id -> labels[id]?.let { RowLabel(it.name, parseHexColor(it.color)) } }
    }
    val longPressEnabled = onLongPressAction != null && !selectionMode

    Box {
        TransactionRow(
            title = primary,
            subtitle = secondary,
            amount = money(t.amount),
            type = t.type,
            recurring = !t.recurringId.isNullOrEmpty(),
            labels = rowLabels,
            onClick = onClick,
            onLongClick = if (longPressEnabled) ({ menuOpen = true }) else null,
            selectionMode = selectionMode,
            selected = selected,
        )
        if (menuOpen && longPressEnabled) {
            val act = { a: TransactionRowAction -> onLongPressAction!!(a, t) }
            ActionMenu(
                onDismissRequest = { menuOpen = false },
                actions = listOf(
                    MenuAction("Select", LucideIcons.CheckSquare) { act(TransactionRowAction.Select) },
                    MenuAction("Duplicate", LucideIcons.Copy) { act(TransactionRowAction.Duplicate) },
                    MenuAction("Save as template", LucideIcons.BookmarkPlus) { act(TransactionRowAction.Template) },
                    MenuAction("Delete", LucideIcons.Trash2, destructive = true, separatorBefore = true) {
                        act(TransactionRowAction.Delete)
                    },
                ),
            )
        }
    }
}
