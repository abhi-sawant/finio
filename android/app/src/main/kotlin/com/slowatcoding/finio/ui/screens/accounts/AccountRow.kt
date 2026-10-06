package com.slowatcoding.finio.ui.screens.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.slowatcoding.finio.core.calc.getCreditCardDueInfo
import com.slowatcoding.finio.core.calc.getCreditUtilization
import com.slowatcoding.finio.core.deposit.accountDeleteBlockers
import com.slowatcoding.finio.core.deposit.accountDisplayValue
import com.slowatcoding.finio.core.deposit.depositCaption
import com.slowatcoding.finio.core.deposit.isDepositAccount
import com.slowatcoding.finio.core.model.Account
import com.slowatcoding.finio.core.model.AccountType
import com.slowatcoding.finio.ui.common.MoneyFormatter
import com.slowatcoding.finio.ui.components.toast
import com.slowatcoding.finio.ui.icons.LucideIcons
import com.slowatcoding.finio.ui.mudra.NoteChip
import com.slowatcoding.finio.ui.mudra.label
import com.slowatcoding.finio.ui.theme.FinioShapes
import com.slowatcoding.finio.ui.theme.FinioTheme
import com.slowatcoding.finio.ui.theme.FinioType
import kotlin.math.abs
import kotlin.math.roundToInt

/** AccountCard.tsx `dueLabel` (sentence-start capitalised, unlike the Dashboard's inline one). */
private fun dueLabel(daysUntilDue: Int): String = when {
    daysUntilDue < 0 -> "Overdue by ${abs(daysUntilDue)} day${if (abs(daysUntilDue) == 1) "" else "s"}"
    daysUntilDue == 0 -> "Due today"
    daysUntilDue == 1 -> "Due tomorrow"
    else -> "Due in $daysUntilDue days"
}

/**
 * Port of components/accounts/AccountCard.tsx — one hairline row inside a `card-elevated divide-y`
 * list: the type's note chip, name + caption (+ the card's due line), the balance on the right,
 * then Archive/Delete circles (or Reopen + Delete when archived).
 */
@Composable
fun AccountRow(
    account: Account,
    money: MoneyFormatter,
    onClick: () -> Unit,
    onDelete: (() -> Unit)?,
    onToggleArchive: (() -> Unit)?,
    modifier: Modifier = Modifier,
    transactionCount: Int = 0,
    forceCompact: Boolean = false,
) {
    val colors = FinioTheme.colors
    val isCredit = account.type == AccountType.Credit
    val isArchived = !account.archivedAt.isNullOrEmpty()
    val utilization = getCreditUtilization(account)
    val dueInfo = getCreditCardDueInfo(account)
    val isDeposit = isDepositAccount(account)
    val shownBalance = accountDisplayValue(account)

    Row(
        modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier
                .weight(1f)
                .clickable(remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = onClick),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NoteChip(account.type, archived = isArchived)
            Column(Modifier.weight(1f)) {
                Text(
                    account.name,
                    style = FinioType.bodyMedium,
                    color = colors.foreground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    when {
                        isArchived -> "Closed · $transactionCount transaction${if (transactionCount == 1) "" else "s"}"
                        isDeposit -> depositCaption(account)
                        else -> account.type.label
                    },
                    style = FinioType.caption,
                    color = colors.mutedForeground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (dueInfo != null) {
                    Text(
                        "${dueLabel(dueInfo.daysUntilDue)} · Min ${money(dueInfo.minimumDue, compact = true)}",
                        Modifier.padding(top = 2.dp),
                        style = FinioType.label,
                        color = when {
                            dueInfo.isOverdue -> colors.destructive
                            dueInfo.daysUntilDue <= 7 -> colors.warning
                            else -> colors.mutedForeground
                        },
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    money(shownBalance, compact = true, forceCompact = forceCompact),
                    style = FinioType.rowValue,
                    color = if (shownBalance < 0) colors.destructive else colors.foreground,
                    maxLines = 1,
                    textAlign = TextAlign.End,
                )
                if (isDeposit && !isArchived) {
                    Text("Current value", Modifier.padding(top = 2.dp), style = FinioType.caption, color = colors.mutedForeground)
                }
                if (isCredit && (account.creditLimit ?: 0.0) != 0.0) {
                    Text(
                        "${(utilization * 100).roundToInt()}% used",
                        Modifier.padding(top = 2.dp),
                        style = FinioType.caption,
                        color = colors.mutedForeground,
                    )
                }
            }
        }
        if (isArchived && onToggleArchive != null) {
            Text(
                "Reopen",
                Modifier
                    .clip(FinioShapes.sm)
                    .clickable(role = Role.Button, onClick = onToggleArchive)
                    .padding(horizontal = 2.dp, vertical = 8.dp),
                style = FinioType.label,
                color = colors.primary,
            )
        }
        if (!isArchived && (onToggleArchive != null || onDelete != null)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (onToggleArchive != null) {
                    RoundIconButton(LucideIcons.Archive, "Archive ${account.name}", destructive = false, onClick = onToggleArchive)
                }
                if (onDelete != null) {
                    RoundIconButton(LucideIcons.Trash2, "Delete ${account.name}", destructive = true, onClick = onDelete)
                }
            }
        }
        if (isArchived && onDelete != null) {
            RoundIconButton(LucideIcons.Trash2, "Delete ${account.name}", destructive = true, onClick = onDelete)
        }
    }
}

/** The 36dp bordered circle (`border-border bg-card h-9 w-9 rounded-full border`). */
@Composable
private fun RoundIconButton(icon: ImageVector, description: String, destructive: Boolean, onClick: () -> Unit) {
    val colors = FinioTheme.colors
    Box(
        Modifier
            .size(36.dp)
            .clip(FinioShapes.full)
            .background(colors.card)
            .border(1.dp, colors.border, FinioShapes.full)
            .clickable(role = Role.Button, onClickLabel = description, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            description,
            Modifier.size(16.dp),
            tint = if (destructive) colors.destructive else colors.mutedForeground,
        )
    }
}

/**
 * The deposit delete-blocker error toast (Accounts + AddAccount `handleDelete`). Returns true when
 * the account is blocked and the toast was shown.
 */
fun showDeleteBlockedToast(accounts: List<Account>, account: Account): Boolean {
    val blockers = accountDeleteBlockers(accounts, account.id)
    if (blockers.isEmpty()) return false
    val one = blockers.size == 1
    toast.error(
        "Can't delete \"${account.name}\"",
        "${blockers.joinToString(", ") { "\"$it\"" }} pay${if (one) "s" else ""} out to this account. " +
            "Delete ${if (one) "that deposit" else "those deposits"} first.",
    )
    return true
}

