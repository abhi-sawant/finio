package com.slowatcoding.finio.ui.screens.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.slowatcoding.finio.core.format.formatDate
import com.slowatcoding.finio.core.format.formatPercentChange
import com.slowatcoding.finio.core.js.parseIso
import com.slowatcoding.finio.core.model.Account
import com.slowatcoding.finio.core.model.AccountType
import com.slowatcoding.finio.core.model.Category
import com.slowatcoding.finio.core.model.Label
import com.slowatcoding.finio.core.model.Transaction
import com.slowatcoding.finio.core.model.TransactionType
import com.slowatcoding.finio.core.deposit.isDepositAccount
import com.slowatcoding.finio.di.appContainer
import com.slowatcoding.finio.ui.common.HideAmountsToggle
import com.slowatcoding.finio.ui.common.MoneyFormatter
import com.slowatcoding.finio.ui.common.SectionLoader
import com.slowatcoding.finio.ui.common.collectFinanceState
import com.slowatcoding.finio.ui.common.rememberDerived
import com.slowatcoding.finio.ui.common.rememberMoneyFormatter
import com.slowatcoding.finio.ui.components.AlertBand
import com.slowatcoding.finio.ui.components.CategoryIcon
import com.slowatcoding.finio.ui.components.FinioButton
import com.slowatcoding.finio.ui.components.FinioCard
import com.slowatcoding.finio.ui.components.FinioDialog
import com.slowatcoding.finio.ui.components.FinioDivider
import com.slowatcoding.finio.ui.components.FinioScreen
import com.slowatcoding.finio.ui.components.HeaderIconButton
import com.slowatcoding.finio.ui.components.RowLabel
import com.slowatcoding.finio.ui.components.SectionHeader
import com.slowatcoding.finio.ui.components.TransactionRow
import com.slowatcoding.finio.ui.components.parseHexColor
import com.slowatcoding.finio.ui.icons.LucideIcons
import com.slowatcoding.finio.ui.mudra.NoteCard
import com.slowatcoding.finio.ui.mudra.NoteTile
import com.slowatcoding.finio.ui.mudra.RegisterBar
import com.slowatcoding.finio.ui.mudra.ThreadProgressBar
import com.slowatcoding.finio.ui.mudra.label
import com.slowatcoding.finio.ui.mudra.noteFigureStyle
import com.slowatcoding.finio.ui.navigation.FinioNavigator
import com.slowatcoding.finio.ui.navigation.FinioTab
import com.slowatcoding.finio.ui.navigation.Routes
import com.slowatcoding.finio.ui.theme.FinioShapes
import com.slowatcoding.finio.ui.theme.FinioTheme
import com.slowatcoding.finio.ui.theme.FinioType
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Port of web/src/pages/Dashboard.tsx — route `/` (tab). Header: the user's name with the
 * "kept on this device" line, HideAmountsToggle and Settings (mobile reaches Settings from here).
 */
@Composable
fun DashboardScreen(nav: FinioNavigator) {
    val state by collectFinanceState()
    val auth by appContainer().auth.state.collectAsStateWithLifecycle()
    val money = rememberMoneyFormatter()
    val colors = FinioTheme.colors

    val data = rememberDerived(
        state.accounts, state.transactions, state.categories, state.budgets, state.labels, state.recurring,
        state.goals, state.goalContributions, state.people, state.debtEntries, state.settings.monthStartDay,
    ) { computeDashboardData(state) }

    var alertsOpen by rememberSaveable { mutableStateOf(false) }
    var howOpen by rememberSaveable { mutableStateOf(false) }

    FinioScreen(header = {
        Column(Modifier.weight(1f).padding(end = 8.dp)) {
            Text(
                state.settings.userName,
                style = FinioType.headline,
                color = colors.foreground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(Modifier.padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(LucideIcons.ShieldCheck, null, Modifier.size(12.dp), tint = colors.positive)
                val lastBackup = auth.lastBackupAt?.let(::parseIso)
                Text(
                    when {
                        !auth.isSignedIn -> "Kept only on this device"
                        lastBackup != null -> "Kept on this device · backed up ${formatDistanceToNow(lastBackup)}"
                        else -> "Kept on this device · cloud backup on"
                    },
                    style = FinioType.caption,
                    color = colors.mutedForeground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HideAmountsToggle()
            HeaderIconButton(LucideIcons.Settings, "Settings", onClick = { nav.navigate(Routes.Settings) })
        }
    }) {
        if (data == null) {
            SectionLoader()
            return@FinioScreen
        }
        val hasAccounts = state.accounts.isNotEmpty()

        Hero(data, money, hasAccounts, nav, onHow = { howOpen = true })

        // One alert surfaced, the rest collapsed behind a review sheet.
        data.attentionItems.firstOrNull()?.let { top ->
            AlertBand(
                Modifier.clickable(remember { MutableInteractionSource() }, indication = null, role = Role.Button) { alertsOpen = true },
                icon = LucideIcons.AlertTriangle,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(describeAttention(top, money), style = FinioType.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = colors.warningBandForeground)
                        val more = data.attentionItems.size - 1
                        if (more > 0) {
                            Text(
                                "$more ${if (more == 1) "more thing needs" else "more things need"} attention this week",
                                Modifier.padding(top = 2.dp),
                                style = FinioType.caption,
                                color = colors.warningBandForeground.copy(alpha = colors.warningBandForeground.alpha * 0.8f),
                            )
                        }
                    }
                    Text(
                        "Review",
                        Modifier.padding(start = 12.dp),
                        style = FinioType.label.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.warningBandAccent,
                    )
                }
            }
        }

        WhereItSits(data, money, hasAccounts, nav)
        Latest(data.recentTxns, state.categories, state.accounts, state.labels, money, hasAccounts, nav)
        ThisMonth(data, money)
        AlsoTracking(data, money, nav)

        if (alertsOpen) {
            FinioDialog(onDismissRequest = { alertsOpen = false }, title = "Needs attention") {
                Text(
                    "This week only. Everything else is fine.",
                    Modifier.padding(top = 0.dp),
                    style = FinioType.caption,
                    color = colors.mutedForeground,
                )
                Column(Modifier.heightIn(max = 384.dp).verticalScroll(rememberScrollState())) {
                    data.attentionItems.forEachIndexed { i, item ->
                        if (i > 0) FinioDivider()
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clickable(role = Role.Button) {
                                    alertsOpen = false
                                    when (item) {
                                        is AlertItem.Budget -> nav.navigate(Routes.Budgets)
                                        is AlertItem.Credit -> nav.openTab(FinioTab.Accounts)
                                        is AlertItem.Recurring -> nav.navigate(Routes.Recurring)
                                    }
                                }
                                .padding(vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(describeAttention(item, money), style = FinioType.bodyMedium, color = colors.foreground)
                            Text(attentionDetail(item, money, state.accounts), style = FinioType.caption, color = colors.mutedForeground)
                        }
                    }
                }
            }
        }

        if (howOpen) {
            FinioDialog(onDismissRequest = { howOpen = false }, title = "How \"safe to spend\" is worked out") {
                data.overallBudget?.let { overall ->
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        HowRow("Budget left this ${data.periodNoun}", money(overall.remaining))
                        HowRow("Bills due in the next 7 days", "− ${money(data.upcomingBillsTotal)}")
                        HowRow("Days to go", "÷ ${data.daysLeftInPeriod}")
                        FinioDivider()
                        HowRow("Safe to spend each day", money(data.safePerDay, precise = false), total = true)
                    }
                }
                Text(
                    "Rounded down to the rupee. Only bills from your recurring list count, and card payments you haven't scheduled aren't included.",
                    style = FinioType.caption,
                    color = colors.mutedForeground,
                )
            }
        }
    }
}

@Composable
private fun HowRow(term: String, value: String, total: Boolean = false) {
    val colors = FinioTheme.colors
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            term,
            Modifier.weight(1f),
            style = if (total) FinioType.body.copy(fontWeight = FontWeight.SemiBold) else FinioType.body,
            color = if (total) colors.foreground else colors.mutedForeground,
        )
        Text(value, style = FinioType.body.copy(fontWeight = if (total) FontWeight.SemiBold else FontWeight.Medium), color = colors.foreground)
    }
}

/** `due in N days` etc. (lower-case: it follows the card name). */
private fun dueLabel(daysUntil: Int): String = when {
    daysUntil < 0 -> "Overdue by ${abs(daysUntil)} day${if (abs(daysUntil) == 1) "" else "s"}"
    daysUntil == 0 -> "due today"
    daysUntil == 1 -> "due tomorrow"
    else -> "due in $daysUntil days"
}

private fun describeAttention(item: AlertItem, money: MoneyFormatter): String = when (item) {
    is AlertItem.Budget -> if (item.status.isOver) {
        "${item.label} is ${money(item.status.spent - item.status.limit, compact = true)} over budget"
    } else {
        "${item.label} is near its limit"
    }
    is AlertItem.Credit -> "${item.account.name} ${dueLabel(item.dueInfo.daysUntilDue)}"
    is AlertItem.Recurring -> "${item.label} " + when (item.daysUntil) {
        0 -> "posts today"
        1 -> "posts tomorrow"
        else -> "posts in ${item.daysUntil} days"
    }
}

private fun attentionDetail(item: AlertItem, money: MoneyFormatter, accounts: List<Account>): String = when (item) {
    is AlertItem.Budget ->
        "${money(item.status.spent, compact = true)} of ${money(item.status.limit, compact = true)} · ${jsRound(item.status.percent)}%"
    is AlertItem.Credit ->
        "${money(item.dueInfo.outstanding, compact = true)} · min ${money(item.dueInfo.minimumDue, compact = true)}"
    is AlertItem.Recurring -> "Recurring · from ${accounts.find { it.id == item.rule.accountId }?.name ?: "account"}"
}

/** JS `Math.round` (halves round up, also for negatives). */
private fun jsRound(x: Double): Long = kotlin.math.floor(x + 0.5).toLong()

@Composable
private fun Hero(data: DashboardData, money: MoneyFormatter, hasAccounts: Boolean, nav: FinioNavigator, onHow: () -> Unit) {
    val colors = FinioTheme.colors
    NoteCard(Modifier.fillMaxWidth()) {
        val overall = data.overallBudget
        if (overall != null) {
            Text("Safe to spend today", style = FinioType.bodyMedium, color = colors.mutedForeground)
            val figure = money(data.safePerDay, precise = false)
            Text(
                figure,
                Modifier.padding(top = 4.dp),
                style = noteFigureStyle(figure),
                color = if (overall.isOver) colors.destructive else colors.foreground,
                maxLines = 1,
                softWrap = false,
            )
            val days = data.daysLeftInPeriod
            Text(
                (if (overall.isOver) "Over budget by ${money(abs(overall.remaining))}" else "${money(overall.remaining)} left this ${overall.budget.period.noun()}") +
                    " · $days day${if (days == 1) "" else "s"} to go",
                Modifier.padding(top = 6.dp),
                style = FinioType.body,
                color = colors.foreground,
            )
            if (data.upcomingBillsTotal > 0 && !overall.isOver) {
                Text(
                    "After ${money(data.upcomingBillsTotal, compact = true)} of bills due this week",
                    Modifier.padding(top = 4.dp),
                    style = FinioType.caption,
                    color = colors.mutedForeground,
                )
            }
            if (!overall.isOver) {
                Text(
                    "How this is worked out",
                    Modifier
                        .padding(top = 4.dp)
                        .clip(FinioShapes.sm)
                        .clickable(role = Role.Button, onClick = onHow)
                        .padding(vertical = 4.dp),
                    style = FinioType.label,
                    color = colors.primary,
                )
            }
            if (overall.spent > 0) {
                RegisterBar(
                    percent = overall.percent.toFloat(),
                    isOver = overall.isOver,
                    modifier = Modifier.padding(top = 12.dp),
                    valueText = "${money(overall.spent, compact = true)} of ${money(overall.limit, compact = true)} spent",
                )
                Text(
                    "Spent ${money(overall.spent, compact = true)} of ${money(overall.limit, compact = true)}",
                    Modifier.padding(top = 6.dp).clearAndSetSemantics { },
                    style = FinioType.caption,
                    color = colors.mutedForeground,
                )
            }
        } else {
            Text("Total balance", style = FinioType.bodyMedium, color = colors.mutedForeground)
            val figure = money(data.totalBalance)
            Text(figure, Modifier.padding(top = 4.dp), style = noteFigureStyle(figure), color = colors.foreground, maxLines = 1, softWrap = false)
            if (data.creditOutstanding > 0) {
                Text("${money(data.afterDues)} after card dues", Modifier.padding(top = 6.dp), style = FinioType.caption, color = colors.mutedForeground)
            }
            if (data.depositValue > 0) {
                Text("+ ${money(data.depositValue)} locked in deposits", Modifier.padding(top = 4.dp), style = FinioType.caption, color = colors.mutedForeground)
            }
            FinioButton(
                onClick = { nav.navigate(if (hasAccounts) Routes.Budgets else Routes.AddAccount) },
                modifier = Modifier.padding(top = 16.dp),
                contentPadding = PaddingValues(horizontal = 20.dp),
            ) {
                Text(if (hasAccounts) "Set a monthly budget" else "Add your first account")
            }
        }
    }
}


@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier.semantics { heading() }, style = FinioType.title, color = FinioTheme.colors.foreground)
}

@Composable
private fun WhereItSits(data: DashboardData, money: MoneyFormatter, hasAccounts: Boolean, nav: FinioNavigator) {
    val colors = FinioTheme.colors
    Column {
        SectionHeader("Where it sits", actionLabel = "See all", onAction = { nav.openTab(FinioTab.Accounts) })
        if (data.overallBudget != null && hasAccounts) {
            Row(
                Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Text("Total balance", style = FinioType.caption, color = colors.mutedForeground)
                Column(horizontalAlignment = Alignment.End) {
                    Text(money(data.totalBalance), style = FinioType.money.copy(fontSize = 20.sp, lineHeight = 28.sp), color = colors.foreground)
                    if (data.creditOutstanding > 0) {
                        Text("${money(data.afterDues)} after card dues", style = FinioType.caption, color = colors.mutedForeground)
                    }
                    if (data.depositValue > 0) {
                        Text("+ ${money(data.depositValue)} locked in deposits", style = FinioType.caption, color = colors.mutedForeground)
                    }
                }
            }
        }
        if (!hasAccounts) {
            AddFirstAccount(onClick = { nav.navigate(Routes.AddAccount) })
        } else {
            val listState = rememberLazyListState()
            val gutter = 12.dp
            LazyRow(
                // `-mx-3 px-3`: the strip bleeds to the screen edge, tiles snap to the gutter.
                modifier = Modifier.layout { measurable, constraints ->
                    val extra = (gutter * 2).roundToPx()
                    val placeable = measurable.measure(
                        constraints.copy(minWidth = constraints.maxWidth + extra, maxWidth = constraints.maxWidth + extra),
                    )
                    layout(constraints.maxWidth, placeable.height) { placeable.place(-extra / 2, 0) }
                },
                state = listState,
                contentPadding = PaddingValues(start = gutter, end = gutter, top = 4.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                flingBehavior = rememberSnapFlingBehavior(listState, SnapPosition.Start),
            ) {
                items(data.openAccounts, key = { it.id }) { account ->
                    val value = data.accountValues[account.id] ?: account.balance
                    NoteTile(
                        type = account.type,
                        name = account.name,
                        caption = if (isDepositAccount(account)) {
                            "${if (account.type == AccountType.Rd) "RD" else "FD"} · current value"
                        } else {
                            account.type.label
                        },
                        amount = money(value, compact = true, forceCompact = data.accountsCompact),
                        onClick = { nav.navigate(Routes.EditAccount(account.id)) },
                        modifier = Modifier.width(160.dp),
                    )
                }
            }
        }
    }
}

/** The dashed "Add your first account" button (`border-2 border-dashed rounded-md p-6`). */
@Composable
private fun AddFirstAccount(onClick: () -> Unit) {
    val colors = FinioTheme.colors
    val border = colors.border
    Column(
        Modifier
            .fillMaxWidth()
            .drawBehind {
                val w = 2.dp.toPx()
                val r = 17.6.dp.toPx()
                drawRoundRect(
                    color = border,
                    topLeft = androidx.compose.ui.geometry.Offset(w / 2, w / 2),
                    size = androidx.compose.ui.geometry.Size(size.width - w, size.height - w),
                    cornerRadius = CornerRadius(r, r),
                    style = Stroke(w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 6.dp.toPx()))),
                )
            }
            .clip(FinioShapes.md)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(LucideIcons.Plus, null, Modifier.size(24.dp), tint = colors.primary)
        Text("Add your first account", style = FinioType.bodyMedium, color = colors.primary)
        Text(
            "You need at least one account to record transactions.",
            style = FinioType.caption,
            color = colors.mutedForeground,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun Latest(
    recent: List<Transaction>,
    categories: List<Category>,
    accounts: List<Account>,
    labels: List<Label>,
    money: MoneyFormatter,
    hasAccounts: Boolean,
    nav: FinioNavigator,
) {
    val colors = FinioTheme.colors
    Column {
        SectionHeader(
            "Latest",
            actionLabel = if (recent.isNotEmpty()) "See all" else null,
            onAction = if (recent.isNotEmpty()) ({ nav.openTab(FinioTab.Transactions) }) else null,
        )
        if (recent.isEmpty()) {
            Column(Modifier.fillMaxWidth().padding(vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("No transactions yet.", style = FinioType.body, color = colors.mutedForeground)
                if (hasAccounts) {
                    Text(
                        "Add your first transaction",
                        Modifier
                            .padding(top = 4.dp)
                            .clip(FinioShapes.sm)
                            .clickable(role = Role.Button) { nav.navigate(Routes.AddTransaction()) }
                            .padding(horizontal = 8.dp, vertical = 12.dp),
                        style = FinioType.bodyMedium,
                        color = colors.primary,
                    )
                }
            }
        } else {
            FinioCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
                recent.forEachIndexed { i, tx ->
                    if (i > 0) FinioDivider()
                    DashboardTransactionRow(tx, categories, accounts, labels, money) {
                        nav.navigate(Routes.EditTransaction(tx.id))
                    }
                }
            }
        }
    }
}

/** TransactionItem.tsx with `showDate` (relative), no long-press menu — the Dashboard's rows. */
@Composable
private fun DashboardTransactionRow(
    tx: Transaction,
    categories: List<Category>,
    accounts: List<Account>,
    labels: List<Label>,
    money: MoneyFormatter,
    onClick: () -> Unit,
) {
    val splits = tx.splits
    val isSplit = !splits.isNullOrEmpty()
    val category = categories.find { it.id == tx.categoryId }
    val account = accounts.find { it.id == tx.accountId }
    val toAccount = tx.toAccountId?.let { id -> accounts.find { it.id == id } }
    val splitTitle = if (isSplit) splits!!.joinToString(" + ") { s -> categories.find { it.id == s.categoryId }?.name ?: "Unknown" } else null
    val title = tx.note.ifEmpty { splitTitle ?: category?.name ?: "Transaction" }
    val secondary = if (tx.type == TransactionType.Transfer && toAccount != null) {
        "${account?.name ?: "?"} → ${toAccount.name}"
    } else {
        "${splitTitle ?: category?.name ?: "Uncategorized"} · ${account?.name ?: "Unknown account"}"
    }
    val rowLabels = tx.labels.mapNotNull { id -> labels.find { it.id == id } }.map { RowLabel(it.name, parseHexColor(it.color)) }
    TransactionRow(
        title = title,
        subtitle = "$secondary · ${formatDate(tx.date)}",
        amount = money(tx.amount),
        type = tx.type,
        recurring = !tx.recurringId.isNullOrEmpty(),
        labels = rowLabels,
        onClick = onClick,
    )
}

@Composable
private fun ThisMonth(data: DashboardData, money: MoneyFormatter) {
    val colors = FinioTheme.colors
    Column {
        Row(
            Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            SectionTitle("This month")
            Text(data.periodLabel, style = FinioType.caption, color = colors.mutedForeground)
        }
        FinioCard(Modifier.fillMaxWidth()) {
            if (data.monthTxnCount > 0) {
                val stats = data.stats
                val cells = buildList<@Composable (Modifier) -> Unit> {
                    add { m -> Stat("In", money(data.monthIncome, compact = true), if (data.monthIncome > 0) colors.positive else colors.foreground, m) }
                    add { m -> Stat("Out", money(data.monthExpenses, compact = true), colors.foreground, m) }
                    add { m -> Stat("Daily avg", money(stats.dailyAverage, compact = true, precise = false), colors.foreground, m) }
                    add { m ->
                        Column(m) {
                            Text("Saved", style = FinioType.label, color = colors.mutedForeground)
                            Text(
                                buildAnnotatedString {
                                    append("${jsRound(stats.savingsRate * 100)}%")
                                    stats.savingsRateChange?.let {
                                        withStyle(
                                            SpanStyle(
                                                fontFamily = FinioType.caption.fontFamily,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Normal,
                                                letterSpacing = 0.sp,
                                                color = colors.mutedForeground,
                                            ),
                                        ) { append(" ${formatPercentChange(it)}") }
                                    }
                                },
                                Modifier.padding(top = 2.dp),
                                style = FinioType.money.copy(fontSize = 16.sp, lineHeight = 24.sp),
                                color = colors.foreground,
                            )
                        }
                    }
                    stats.topCategory?.let { top ->
                        add { m ->
                            Column(m) {
                                Text("Top", style = FinioType.label, color = colors.mutedForeground)
                                Text(
                                    top.category.name,
                                    Modifier.padding(top = 2.dp),
                                    style = FinioType.rowValue,
                                    color = colors.foreground,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    cells.chunked(3).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            row.forEach { it(Modifier.weight(1f)) }
                            repeat(3 - row.size) { androidx.compose.foundation.layout.Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            } else {
                Text(
                    if (data.overallBudget != null) "Nothing logged yet this month." else "Set a budget and this becomes “safe to spend”",
                    Modifier.fillMaxWidth(),
                    style = FinioType.body,
                    color = colors.mutedForeground,
                    textAlign = TextAlign.Center,
                )
                if (data.overallBudget != null && data.prevMonthTxnCount > 0) {
                    Text(
                        "Last month: ${money(data.prevMonthIncome, compact = true)} in · ${money(data.prevMonthExpenses, compact = true)} out",
                        Modifier.fillMaxWidth().padding(top = 4.dp),
                        style = FinioType.caption,
                        color = colors.mutedForeground,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    Column(modifier) {
        Text(label, style = FinioType.label, color = FinioTheme.colors.mutedForeground)
        Text(value, Modifier.padding(top = 2.dp), style = FinioType.money.copy(fontSize = 16.sp, lineHeight = 24.sp), color = color, maxLines = 1, softWrap = false)
    }
}

@Composable
private fun AlsoTracking(data: DashboardData, money: MoneyFormatter, nav: FinioNavigator) {
    if (data.topGoals.isEmpty() && data.topDebts.isEmpty()) return
    val colors = FinioTheme.colors
    Column {
        SectionTitle("Also tracking", Modifier.padding(bottom = 12.dp))
        FinioCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
            if (data.topGoals.isNotEmpty()) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.Button) { nav.navigate(Routes.Goals) }
                        .padding(16.dp),
                ) {
                    TrackingHeader(LucideIcons.PiggyBank, "Savings goals")
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        data.topGoals.forEach { s ->
                            Column {
                                Row(
                                    Modifier.fillMaxWidth().padding(bottom = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Row(
                                        Modifier.weight(1f),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        CategoryIcon(s.goal.icon, size = 12.dp, tint = parseHexColor(s.goal.color))
                                        Text(s.goal.name, style = FinioType.label, color = colors.foreground, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    Text(
                                        "${money(s.current)} / ${money(s.goal.targetAmount)}",
                                        style = FinioType.label.copy(fontWeight = FontWeight.SemiBold),
                                        color = colors.mutedForeground,
                                        maxLines = 1,
                                    )
                                }
                                ThreadProgressBar(s.percent.toFloat())
                            }
                        }
                    }
                }
            }
            if (data.topGoals.isNotEmpty() && data.topDebts.isNotEmpty()) FinioDivider()
            if (data.topDebts.isNotEmpty()) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.Button) { nav.navigate(Routes.Debts) }
                        .padding(16.dp),
                ) {
                    TrackingHeader(LucideIcons.HandCoins, "Debts & lending")
                    data.topDebts.forEachIndexed { i, s ->
                        if (i > 0) FinioDivider()
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(top = if (i == 0) 0.dp else 8.dp, bottom = if (i == data.topDebts.lastIndex) 0.dp else 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CategoryIcon(s.person.icon, size = 14.dp, tint = parseHexColor(s.person.color))
                            Text(
                                s.person.name,
                                Modifier.weight(1f),
                                style = FinioType.label,
                                color = colors.foreground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                (if (s.balance > 0) "Owes you " else "You owe ") + money(abs(s.balance), compact = true),
                                style = FinioType.label.copy(fontWeight = FontWeight.SemiBold),
                                color = if (s.balance > 0) colors.positive else colors.destructive,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackingHeader(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
    val colors = FinioTheme.colors
    Row(
        Modifier.fillMaxWidth().padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, Modifier.size(16.dp), tint = colors.primary)
        Text(title, Modifier.weight(1f).semantics { heading() }, style = FinioType.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = colors.foreground)
        Icon(LucideIcons.ChevronRight, null, Modifier.size(14.dp), tint = colors.mutedForeground)
    }
}
