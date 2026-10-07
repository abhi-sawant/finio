package com.slowatcoding.finio.ui.screens.merchants

import androidx.compose.foundation.background
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.slowatcoding.finio.core.calc.MerchantSummary
import com.slowatcoding.finio.core.calc.summarizeMerchants
import com.slowatcoding.finio.core.format.formatShortDate
import com.slowatcoding.finio.core.format.shouldCompactGroup
import com.slowatcoding.finio.core.model.Account
import com.slowatcoding.finio.core.model.Category
import com.slowatcoding.finio.core.model.Transaction
import com.slowatcoding.finio.core.model.TransactionType
import com.slowatcoding.finio.ui.common.BackButton
import com.slowatcoding.finio.ui.common.HideAmountsToggle
import com.slowatcoding.finio.ui.common.MoneyFormatter
import com.slowatcoding.finio.ui.common.SectionLoader
import com.slowatcoding.finio.ui.common.collectFinanceState
import com.slowatcoding.finio.ui.common.rememberDerived
import com.slowatcoding.finio.ui.common.rememberMoneyFormatter
import com.slowatcoding.finio.ui.components.ButtonSize
import com.slowatcoding.finio.ui.components.ButtonVariant
import com.slowatcoding.finio.ui.components.FinioButton
import com.slowatcoding.finio.ui.components.FinioCard
import com.slowatcoding.finio.ui.components.FinioDivider
import com.slowatcoding.finio.ui.components.FinioScreen
import com.slowatcoding.finio.ui.components.ScreenTitle
import com.slowatcoding.finio.ui.components.TransactionRow
import com.slowatcoding.finio.ui.icons.LucideIcons
import com.slowatcoding.finio.ui.navigation.FinioNavigator
import com.slowatcoding.finio.ui.navigation.Routes
import com.slowatcoding.finio.ui.theme.FinioShapes
import com.slowatcoding.finio.ui.theme.FinioTheme
import com.slowatcoding.finio.ui.theme.FinioType
import com.slowatcoding.finio.ui.theme.mix

private val TypeChips = listOf(TransactionType.Expense to "Spending", TransactionType.Income to "Income")

/**
 * Port of web/src/pages/Merchants.tsx — route `/merchants`. Transactions grouped by their note
 * (Spending / Income), biggest first; a row expands to its transactions (each opens Edit
 * Transaction) and offers "Create a rule" → Category Rules prefilled with the merchant.
 */
@Composable
fun MerchantsScreen(nav: FinioNavigator) {
    val state by collectFinanceState()
    val money = rememberMoneyFormatter()
    val colors = FinioTheme.colors
    val transactions = state.transactions
    var type by rememberSaveable { mutableStateOf(TransactionType.Expense) }
    var expandedKey by rememberSaveable { mutableStateOf<String?>(null) }

    val merchants = rememberDerived(transactions, type) { summarizeMerchants(transactions, type) }

    FinioScreen(header = {
        BackButton(nav)
        ScreenTitle("Merchants")
        HideAmountsToggle()
    }) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TypeChips.forEach { (value, label) ->
                val selected = type == value
                FinioButton(
                    onClick = {
                        type = value
                        expandedKey = null
                    },
                    variant = if (selected) ButtonVariant.Default else ButtonVariant.Secondary,
                    size = ButtonSize.Sm,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    modifier = Modifier.semantics { stateDescription = if (selected) "Selected" else "Not selected" },
                ) {
                    Text(label, style = FinioType.label, color = if (selected) Color.White else colors.mutedForeground)
                }
            }
        }

        if (merchants == null) {
            SectionLoader()
            return@FinioScreen
        }

        if (merchants.isEmpty()) {
            Text(
                "No noted ${if (type == TransactionType.Expense) "spending" else "income"} yet — add a note to a transaction to see it show up here.",
                Modifier.fillMaxWidth().padding(vertical = 48.dp),
                style = FinioType.body,
                color = colors.mutedForeground,
                textAlign = TextAlign.Center,
            )
            return@FinioScreen
        }

        val compact = remember(merchants) { shouldCompactGroup(merchants.map { it.totalAmount }) }
        val total = remember(merchants) { merchants.sumOf { it.totalAmount } }

        FinioCard(Modifier.fillMaxWidth()) {
            Row(Modifier.padding(bottom = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(LucideIcons.Store, null, Modifier.size(12.dp), tint = colors.primary)
                Text(
                    "${merchants.size} merchant${if (merchants.size == 1) "" else "s"}",
                    style = FinioType.label,
                    color = colors.mutedForeground,
                )
            }
            Text(money(total, compact = true), style = FinioType.money, color = colors.foreground)
        }

        FinioCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
            merchants.forEachIndexed { index, merchant ->
                if (index > 0) FinioDivider()
                MerchantRow(
                    merchant = merchant,
                    expanded = expandedKey == merchant.key,
                    onToggle = { expandedKey = if (expandedKey == merchant.key) null else merchant.key },
                    compact = compact,
                    money = money,
                    categories = state.categories,
                    accounts = state.accounts,
                    onOpen = { nav.navigate(Routes.EditTransaction(it)) },
                    onCreateRule = {
                        nav.navigate(Routes.CategoryRules(pattern = merchant.displayName, scope = merchant.type.wire))
                    },
                )
            }
        }
    }
}

@Composable
private fun MerchantRow(
    merchant: MerchantSummary,
    expanded: Boolean,
    onToggle: () -> Unit,
    compact: Boolean,
    money: MoneyFormatter,
    categories: List<Category>,
    accounts: List<Account>,
    onOpen: (String) -> Unit,
    onCreateRule: () -> Unit,
) {
    val colors = FinioTheme.colors
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .semantics { stateDescription = if (expanded) "Expanded" else "Collapsed" }
                .clickable(role = Role.Button, onClick = onToggle)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                Text(merchant.displayName, style = FinioType.bodyMedium, color = colors.foreground, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${merchant.transactionCount} transaction${if (merchant.transactionCount == 1) "" else "s"} · last on ${formatShortDate(merchant.lastDate)}",
                    style = FinioType.caption,
                    color = colors.mutedForeground,
                )
            }
            Row(Modifier.padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    money(merchant.totalAmount, compact = true, forceCompact = compact),
                    style = FinioType.rowValue,
                    color = if (merchant.type == TransactionType.Income) colors.positive else colors.foreground,
                )
                Icon(if (expanded) LucideIcons.ChevronUp else LucideIcons.ChevronDown, null, Modifier.size(16.dp), tint = colors.mutedForeground)
            }
        }

        if (expanded) {
            Column(Modifier.fillMaxWidth().background(colors.muted.mix(0.3f))) {
                FinioDivider()
                Column(Modifier.padding(start = 4.dp, end = 4.dp, bottom = 4.dp)) {
                    merchant.transactions.forEach { t ->
                        MerchantTransaction(t, categories, accounts, money) { onOpen(t.id) }
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(FinioShapes.full)
                            .clickable(role = Role.Button, onClick = onCreateRule)
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(LucideIcons.Wand2, null, Modifier.size(13.dp), tint = colors.primary)
                        Text("Create a rule for \"${merchant.displayName}\"", style = FinioType.label, color = colors.primary)
                    }
                }
            }
        }
    }
}

/** TransactionItem with `showDate dateStyle="short"` and no labels. */
@Composable
private fun MerchantTransaction(
    t: Transaction,
    categories: List<Category>,
    accounts: List<Account>,
    money: MoneyFormatter,
    onClick: () -> Unit,
) {
    val splits = t.splits?.takeIf { it.isNotEmpty() }
    val splitTitle = splits?.joinToString(" + ") { s -> categories.firstOrNull { it.id == s.categoryId }?.name ?: "Unknown" }
    val category = categories.firstOrNull { it.id == t.categoryId }
    val account = accounts.firstOrNull { it.id == t.accountId }
    val toAccount = t.toAccountId?.let { id -> accounts.firstOrNull { it.id == id } }
    val title = t.note.ifEmpty { splitTitle ?: category?.name ?: "Transaction" }
    val secondary = if (t.type == TransactionType.Transfer && toAccount != null) {
        "${account?.name ?: "?"} → ${toAccount.name}"
    } else {
        "${splitTitle ?: category?.name ?: "Uncategorized"} · ${account?.name ?: "Unknown account"}"
    }
    TransactionRow(
        title = title,
        subtitle = "$secondary · ${formatShortDate(t.date)}",
        amount = money(t.amount),
        type = t.type,
        recurring = t.recurringId != null,
        onClick = onClick,
    )
}
