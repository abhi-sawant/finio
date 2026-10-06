package com.slowatcoding.finio.ui.screens.rules

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slowatcoding.finio.core.calc.isCategoryValidForType
import com.slowatcoding.finio.core.calc.miscLast
import com.slowatcoding.finio.core.data.MISC_CATEGORY_ID
import com.slowatcoding.finio.core.model.Category
import com.slowatcoding.finio.core.model.CategoryRule
import com.slowatcoding.finio.core.model.CategoryType
import com.slowatcoding.finio.core.model.RuleMatchType
import com.slowatcoding.finio.core.model.RuleScope
import com.slowatcoding.finio.core.model.TransactionType
import com.slowatcoding.finio.core.rules.MATCH_TYPES
import com.slowatcoding.finio.core.rules.MATCH_TYPE_LABELS
import com.slowatcoding.finio.core.rules.ReplayOptions
import com.slowatcoding.finio.core.rules.isValidPattern
import com.slowatcoding.finio.core.rules.planRuleApplication
import com.slowatcoding.finio.core.store.MoveDirection
import com.slowatcoding.finio.core.store.NewRule
import com.slowatcoding.finio.core.util.MAX_PATTERN_LENGTH
import com.slowatcoding.finio.core.util.jsTrim
import com.slowatcoding.finio.ui.common.BackButton
import com.slowatcoding.finio.ui.common.collectFinanceState
import com.slowatcoding.finio.ui.common.financeStore
import com.slowatcoding.finio.ui.common.rememberDerived
import com.slowatcoding.finio.ui.common.undoToast
import com.slowatcoding.finio.ui.components.ButtonVariant
import com.slowatcoding.finio.ui.components.CategoryIcon
import com.slowatcoding.finio.ui.components.FieldLabel
import com.slowatcoding.finio.ui.components.FinioButton
import com.slowatcoding.finio.ui.components.FinioCard
import com.slowatcoding.finio.ui.components.FinioDialog
import com.slowatcoding.finio.ui.components.FinioDivider
import com.slowatcoding.finio.ui.components.FinioIconButton
import com.slowatcoding.finio.ui.components.FinioScreen
import com.slowatcoding.finio.ui.components.FinioSelect
import com.slowatcoding.finio.ui.components.FinioSwitch
import com.slowatcoding.finio.ui.components.FinioTextField
import com.slowatcoding.finio.ui.components.HeaderIconButton
import com.slowatcoding.finio.ui.components.HeaderIconTone
import com.slowatcoding.finio.ui.components.LocalConfirm
import com.slowatcoding.finio.ui.components.ScreenTitle
import com.slowatcoding.finio.ui.components.SelectOption
import com.slowatcoding.finio.ui.components.SwitchSize
import com.slowatcoding.finio.ui.components.parseHexColor
import com.slowatcoding.finio.ui.components.toast
import com.slowatcoding.finio.ui.icons.LucideIcons
import com.slowatcoding.finio.ui.navigation.FinioNavigator
import com.slowatcoding.finio.ui.screens.categories.DialogError
import com.slowatcoding.finio.ui.theme.CssShadow
import com.slowatcoding.finio.ui.theme.FinioShapes
import com.slowatcoding.finio.ui.theme.FinioTheme
import com.slowatcoding.finio.ui.theme.FinioType
import com.slowatcoding.finio.ui.theme.cssShadow
import kotlinx.coroutines.launch
import kotlin.math.ceil

private val SCOPES = listOf(
    RuleScope.Any to "Expenses & income",
    RuleScope.Expense to "Expenses only",
    RuleScope.Income to "Income only",
)

private fun RuleScope.asType(): TransactionType = if (this == RuleScope.Income) TransactionType.Income else TransactionType.Expense

/**
 * Port of web/src/pages/CategoryRules.tsx — route `/category-rules`.
 * @param prefillPattern Merchants' "Create a rule" prefill (web `location.state.pattern`); opens the form when set.
 * @param prefillScope the prefill's scope (web `location.state.scope`).
 */
@Composable
fun CategoryRulesScreen(nav: FinioNavigator, prefillPattern: String?, prefillScope: RuleScope?) {
    val store = financeStore()
    val state by collectFinanceState()
    val confirm = LocalConfirm.current
    val scope = rememberCoroutineScope()
    val colors = FinioTheme.colors
    val rules = state.rules
    val categories = state.categories
    val labels = state.labels

    // Read once as each field's initial value, like the web reads location.state.
    var open by rememberSaveable { mutableStateOf(!prefillPattern.isNullOrEmpty()) }
    var editId by rememberSaveable { mutableStateOf<String?>(null) }
    var pattern by rememberSaveable { mutableStateOf(prefillPattern ?: "") }
    var matchType by rememberSaveable { mutableStateOf(RuleMatchType.Contains) }
    var ruleScope by rememberSaveable { mutableStateOf(prefillScope ?: RuleScope.Any) }
    var categoryId by rememberSaveable { mutableStateOf("") }
    var labelIds by rememberSaveable { mutableStateOf(listOf<String>()) }
    var replayOpen by rememberSaveable { mutableStateOf(false) }
    var onlyUncategorized by rememberSaveable { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    val patternValid = isValidPattern(pattern, matchType)

    fun resetForm() {
        open = false
        editId = null
        pattern = ""
        matchType = RuleMatchType.Contains
        ruleScope = RuleScope.Any
        categoryId = ""
        labelIds = emptyList()
        error = null
    }

    fun handleEdit(rule: CategoryRule) {
        editId = rule.id
        pattern = rule.pattern
        matchType = rule.matchType
        ruleScope = rule.scope
        categoryId = rule.categoryId
        labelIds = rule.labelIds
        error = null
        open = true
    }

    fun handleSubmit() {
        if (!patternValid) {
            val msg = if (matchType == RuleMatchType.Regex) "That regex is not valid" else "Enter something to match"
            error = msg
            toast.error(msg)
            return
        }
        if (categoryId.isEmpty()) {
            val msg = "Pick a category to file matches into"
            error = msg
            toast.error(msg)
            return
        }
        val id = editId
        val trimmed = jsTrim(pattern)
        if (id != null) {
            store.updateRule(id) {
                it.copy(pattern = trimmed, matchType = matchType, scope = ruleScope, categoryId = categoryId, labelIds = labelIds)
            }
        } else {
            store.addRule(NewRule(trimmed, matchType, ruleScope, categoryId, labelIds, enabled = true))
        }
        resetForm()
    }

    fun handleDelete(rule: CategoryRule) {
        scope.launch {
            val ok = confirm.confirm(
                title = "Delete this rule?",
                description = "New transactions whose note ${MATCH_TYPE_LABELS[rule.matchType]} \"${rule.pattern}\" will no longer be filed automatically. Transactions it has already categorized keep their category.",
                confirmLabel = "Delete rule",
            )
            if (ok) store.deleteRule(rule.id)
        }
    }

    fun handleReplay() {
        val result = store.applyRulesToExisting(if (onlyUncategorized) MISC_CATEGORY_ID else null)
        replayOpen = false
        if (result.changed == 0) {
            toast.message("Nothing to recategorize")
            return
        }
        undoToast("Recategorized ${result.changed} transaction${if (result.changed == 1) "" else "s"}") {
            store.restoreCategorization(result.previous)
        }
    }

    FinioScreen(header = {
        BackButton(nav)
        ScreenTitle("Categorization rules")
        HeaderIconButton(
            LucideIcons.Plus,
            "Add rule",
            onClick = {
                resetForm()
                open = true
            },
            tone = HeaderIconTone.Primary,
        )
    }) {
        if (rules.isEmpty()) {
            FinioCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(24.dp)) {
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(LucideIcons.Wand2, null, Modifier.size(28.dp), tint = colors.mutedForeground)
                    Text("No rules yet", style = FinioType.bodyMedium, color = colors.foreground)
                    Text(
                        "A rule files a transaction automatically from its note — \"contains Uber\" → Transport. " +
                            "Rules run when you add a transaction and when you import a bank CSV.",
                        style = FinioType.caption,
                        color = colors.mutedForeground,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        } else {
            Text(
                "Checked top to bottom — the first matching rule wins. Rules never touch transfers or split transactions.",
                Modifier.padding(horizontal = 4.dp),
                style = FinioType.caption,
                color = colors.mutedForeground,
            )
            FinioCard(contentPadding = PaddingValues(0.dp)) {
                rules.forEachIndexed { index, rule ->
                    if (index > 0) FinioDivider()
                    RuleRow(
                        rule = rule,
                        category = categories.find { it.id == rule.categoryId },
                        labels = rule.labelIds.mapNotNull { id -> labels.find { it.id == id } },
                        isFirst = index == 0,
                        isLast = index == rules.size - 1,
                        onMove = { store.moveRule(rule.id, it) },
                        onToggle = { enabled -> store.updateRule(rule.id) { it.copy(enabled = enabled) } },
                        onEdit = { handleEdit(rule) },
                        onDelete = { handleDelete(rule) },
                    )
                }
            }
            FinioButton(
                "Apply to existing transactions",
                onClick = { replayOpen = true },
                modifier = Modifier.fillMaxWidth(),
                variant = ButtonVariant.Secondary,
                leadingIcon = LucideIcons.Wand2,
            )
        }
    }

    if (open) {
        RuleDialog(
            editing = editId != null,
            pattern = pattern,
            onPattern = {
                pattern = it.take(MAX_PATTERN_LENGTH)
                error = null
            },
            matchType = matchType,
            onMatchType = { matchType = it },
            scope = ruleScope,
            onScope = { ruleScope = it },
            patternValid = patternValid,
            categories = categories,
            categoryId = categoryId,
            onCategory = {
                categoryId = it
                error = null
            },
            labels = labels,
            labelIds = labelIds,
            onToggleLabel = { id -> labelIds = if (id in labelIds) labelIds - id else labelIds + id },
            error = error,
            onDismiss = { resetForm() },
            onSubmit = { handleSubmit() },
        )
    }

    if (replayOpen) {
        val plan = rememberDerived(state.transactions, rules, onlyUncategorized) {
            planRuleApplication(state.transactions, rules, ReplayOptions(if (onlyUncategorized) MISC_CATEGORY_ID else null))
        }
        val count = plan?.size
        FinioDialog(
            onDismissRequest = { replayOpen = false },
            footer = {
                FinioButton("Cancel", onClick = { replayOpen = false }, variant = ButtonVariant.Outline)
                FinioButton("Apply", onClick = { handleReplay() }, enabled = (count ?: 0) > 0)
            },
        ) {
            DialogHeading(
                "Apply rules to existing transactions",
                "Runs every enabled rule over transactions already in your ledger. Transfers and split transactions are left alone, and you can undo the whole pass.",
            )
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SmallSegmented(
                    options = listOf(true to "Uncategorized only", false to "All transactions"),
                    selected = onlyUncategorized,
                    onSelect = { onlyUncategorized = it },
                )
                Text(
                    if (onlyUncategorized) {
                        "Only touches transactions currently filed under Miscellaneous — the usual state after a bank import."
                    } else {
                        "Re-files every matching transaction, including ones you categorized by hand."
                    },
                    style = FinioType.caption,
                    color = colors.mutedForeground,
                )
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold)) { append(count?.toString() ?: "…") }
                        append(" transaction${if (count == 1) "" else "s"} would change.")
                    },
                    style = FinioType.body,
                    color = colors.foreground,
                )
            }
        }
    }
}

/**
 * `DialogHeader className="pr-8"`: the dialog title and description kept 32dp clear of the close
 * button (FinioDialog's own heading runs under it).
 */
@Composable
private fun DialogHeading(title: String, description: String) {
    Column(
        Modifier.fillMaxWidth().padding(end = 32.dp).semantics { heading() },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, style = FinioType.dialogTitle, color = FinioTheme.colors.popoverForeground)
        Text(description, style = FinioType.body, color = FinioTheme.colors.mutedForeground)
    }
}

/** The compact 2-way pill switch (`bg-muted grid grid-cols-2 gap-1 rounded-full p-1`, 12sp cells). */
@Composable
internal fun <T> SmallSegmented(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    val colors = FinioTheme.colors
    Row(
        modifier.fillMaxWidth().clip(FinioShapes.full).background(colors.muted).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { (value, label) ->
            val active = value == selected
            Box(
                Modifier
                    .weight(1f)
                    .cssShadow(FinioShapes.full, if (active) FinioTheme.shadows.glowPrimary else emptyList())
                    .clip(FinioShapes.full)
                    .then(if (active) Modifier.background(FinioTheme.brushes.gradPrimary) else Modifier)
                    .semantics { this.selected = active }
                    .clickable(role = Role.Button) { onSelect(value) }
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, style = FinioType.label, color = if (active) Color.White else colors.mutedForeground, maxLines = 1)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RuleRow(
    rule: CategoryRule,
    category: Category?,
    labels: List<com.slowatcoding.finio.core.model.Label>,
    isFirst: Boolean,
    isLast: Boolean,
    onMove: (MoveDirection) -> Unit,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = FinioTheme.colors
    Row(
        Modifier.fillMaxWidth().padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column {
            MoveButton(LucideIcons.ChevronUp, "Move rule \"${rule.pattern}\" up", enabled = !isFirst) { onMove(MoveDirection.Up) }
            MoveButton(LucideIcons.ChevronDown, "Move rule \"${rule.pattern}\" down", enabled = !isLast) { onMove(MoveDirection.Down) }
        }
        Column(Modifier.weight(1f).alpha(if (rule.enabled) 1f else 0.6f)) {
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = colors.mutedForeground)) { append("Note ") }
                    append(MATCH_TYPE_LABELS[rule.matchType] ?: "")
                    append(" ")
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append("\"${rule.pattern}\"") }
                },
                style = FinioType.body,
                color = colors.foreground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            FlowRow(
                Modifier.padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (category != null) {
                    val tint = parseHexColor(category.color)
                    Row(
                        Modifier.clip(FinioShapes.full).background(tint.copy(alpha = 0.16f)).padding(horizontal = 8.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CategoryIcon(category.icon, size = 12.dp, tint = tint)
                        Text(category.name, style = FinioType.label, color = colors.foreground, maxLines = 1)
                    }
                }
                labels.forEach { label ->
                    Row(
                        Modifier.clip(FinioShapes.full).background(colors.muted).padding(horizontal = 8.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(6.dp).clip(FinioShapes.full).background(parseHexColor(label.color)))
                        Text(label.name, style = FinioType.label, color = colors.foreground, maxLines = 1)
                    }
                }
                if (rule.scope != RuleScope.Any) {
                    Text(
                        if (rule.scope == RuleScope.Expense) "Expenses" else "Income",
                        Modifier.clip(FinioShapes.full).background(colors.muted).padding(horizontal = 8.dp, vertical = 2.dp),
                        style = FinioType.caption,
                        color = colors.mutedForeground,
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            FinioSwitch(
                checked = rule.enabled,
                onCheckedChange = onToggle,
                size = SwitchSize.Sm,
                contentDescription = "Rule \"${rule.pattern}\" enabled",
            )
            FinioIconButton(LucideIcons.Pencil, "Edit rule \"${rule.pattern}\"", onClick = onEdit, tint = colors.mutedForeground)
            FinioIconButton(LucideIcons.Trash2, "Delete rule \"${rule.pattern}\"", onClick = onDelete, tint = colors.destructive)
        }
    }
}

@Composable
private fun MoveButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    val colors = FinioTheme.colors
    Box(
        Modifier
            .clip(FinioShapes.full)
            .alpha(if (enabled) 1f else 0.25f)
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = label, onClick = onClick)
            .padding(2.dp),
    ) {
        Icon(icon, label, Modifier.size(16.dp), tint = colors.mutedForeground)
    }
}

/**
 * Add / edit rule. The header (title + description) and footer stay put; only the form body
 * scrolls on a short screen (`max-h-[calc(100dvh-2rem)] flex-col overflow-hidden`). The body's
 * height cap is the window minus the insets, keyboard and the fixed chrome around it.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RuleDialog(
    editing: Boolean,
    pattern: String,
    onPattern: (String) -> Unit,
    matchType: RuleMatchType,
    onMatchType: (RuleMatchType) -> Unit,
    scope: RuleScope,
    onScope: (RuleScope) -> Unit,
    patternValid: Boolean,
    categories: List<Category>,
    categoryId: String,
    onCategory: (String) -> Unit,
    labels: List<com.slowatcoding.finio.core.model.Label>,
    labelIds: List<String>,
    onToggleLabel: (String) -> Unit,
    error: String?,
    onDismiss: () -> Unit,
    onSubmit: () -> Unit,
) {
    val colors = FinioTheme.colors
    val density = LocalDensity.current
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val insets = with(density) {
        (WindowInsets.safeDrawing.getTop(this) + maxOf(WindowInsets.safeDrawing.getBottom(this), WindowInsets.ime.getBottom(this))).toDp()
    }
    // 32 outer margin + 32 panel padding + 16 gap + ~84 header + ~121 stacked footer.
    val bodyMax = (screenHeight - insets - 285.dp).coerceAtLeast(120.dp)

    // A rule can file into an expense or income category, so the picker offers both.
    val selectable = remember(categories, scope) {
        miscLast(
            categories.filter { c ->
                if (scope == RuleScope.Any) {
                    isCategoryValidForType(c, TransactionType.Expense) || isCategoryValidForType(c, TransactionType.Income)
                } else {
                    isCategoryValidForType(c, scope.asType())
                }
            },
        )
    }

    // The validation line sits at the top of the body; bring it into view when it appears.
    val bodyScroll = rememberScrollState()
    LaunchedEffect(error) { if (error != null) bodyScroll.animateScrollTo(0) }

    FinioDialog(
        onDismissRequest = onDismiss,
        footer = {
            FinioButton("Cancel", onClick = onDismiss, variant = ButtonVariant.Outline)
            FinioButton(if (editing) "Update rule" else "Add rule", onClick = onSubmit)
        },
    ) {
        DialogHeading(
            if (editing) "Edit rule" else "New rule",
            "When a transaction's note matches, file it into a category and tag it.",
        )
        Column(
            Modifier.fillMaxWidth().heightIn(max = bodyMax).verticalScroll(bodyScroll).padding(bottom = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DialogError(error)
            Column {
                FieldLabel("Note")
                FinioSelect(
                    value = matchType,
                    options = MATCH_TYPES.map { SelectOption(it.value, it.label) },
                    onValueChange = onMatchType,
                    title = "Note",
                )
            }
            Column {
                FieldLabel("Applies to")
                FinioSelect(
                    value = scope,
                    options = SCOPES.map { SelectOption(it.first, it.second) },
                    onValueChange = onScope,
                    title = "Applies to",
                )
            }
            Column {
                FieldLabel("Pattern")
                FinioTextField(
                    value = pattern,
                    onValueChange = onPattern,
                    placeholder = if (matchType == RuleMatchType.Regex) "e\\.g\\. uber|ola" else "e.g. Uber",
                    isError = matchType == RuleMatchType.Regex && jsTrim(pattern).isNotEmpty() && !patternValid,
                )
                if (matchType == RuleMatchType.Regex && jsTrim(pattern).isNotEmpty() && !patternValid) {
                    Text("Not a valid regular expression", Modifier.padding(top = 4.dp), style = FinioType.caption, color = colors.destructive)
                }
                Text(
                    "Matching ignores case." + if (matchType == RuleMatchType.Regex) " Regex runs against the whole note." else "",
                    Modifier.padding(top = 4.dp),
                    style = FinioType.caption,
                    color = colors.mutedForeground,
                )
            }
            Column {
                FieldLabel("File into")
                RuleCategoryGrid(selectable, categoryId, showType = scope == RuleScope.Any, onSelect = onCategory)
            }
            if (labels.isNotEmpty()) {
                Column {
                    FieldLabel("Also tag with (optional)")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        labels.forEach { label ->
                            val active = label.id in labelIds
                            val tint = parseHexColor(label.color)
                            Row(
                                Modifier
                                    .clip(FinioShapes.full)
                                    .background(if (active) tint.copy(alpha = 0.18f) else colors.muted)
                                    .border(1.dp, if (active) tint else Color.Transparent, FinioShapes.full)
                                    .semantics { selected = active }
                                    .clickable(role = Role.Button) { onToggleLabel(label.id) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(Modifier.size(8.dp).clip(FinioShapes.full).background(tint))
                                Text(label.name, style = FinioType.label, color = if (active) colors.foreground else colors.mutedForeground, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * CategoryGrid with `max-h-40` (160dp) and the rules page's tiles: like the shared grid's tile,
 * plus the category's type under its name when the rule applies to both expenses and income.
 */
@Composable
private fun RuleCategoryGrid(categories: List<Category>, selectedId: String, showType: Boolean, onSelect: (String) -> Unit) {
    val colors = FinioTheme.colors
    val density = LocalDensity.current
    val scrollState = rememberScrollState()
    var viewport by remember { mutableIntStateOf(0) }
    var rowHeight by remember { mutableIntStateOf(0) }
    val gap = with(density) { 8.dp.roundToPx() }
    val rows = ceil(categories.size / 4.0).toInt()

    LaunchedEffect(rowHeight, viewport) {
        val index = categories.indexOfFirst { it.id == selectedId }
        if (index >= 0 && rowHeight > 0 && viewport > 0) {
            val top = (index / 4) * (rowHeight + gap)
            scrollState.scrollTo((top - (viewport - rowHeight) / 2).coerceAtLeast(0))
        }
    }
    val hidden = if (rowHeight == 0 || viewport == 0) {
        0
    } else {
        val bottom = scrollState.value + viewport
        categories.indices.count { i -> (i / 4) * (rowHeight + gap) + rowHeight / 2 > bottom + 1 }
    }

    Column {
        Box(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 160.dp)
                .onSizeChanged { viewport = it.height }
                .verticalScroll(scrollState)
                .padding(end = 4.dp),
        ) {
            Layout(content = {
                categories.forEach { c ->
                    RuleCategoryTile(c, selected = c.id == selectedId, showType = showType && c.type != CategoryType.Both) { onSelect(c.id) }
                }
            }) { measurables, constraints ->
                val cell = (constraints.maxWidth - gap * 3) / 4
                val placeables = measurables.map { it.measure(constraints.copy(minWidth = cell, maxWidth = cell, minHeight = 0)) }
                val rowHeights = placeables.chunked(4).map { row -> row.maxOf { it.height } }
                if (rowHeights.isNotEmpty() && rowHeight != rowHeights.first()) rowHeight = rowHeights.first()
                val height = rowHeights.sum() + gap * (rows - 1).coerceAtLeast(0)
                layout(constraints.maxWidth, height) {
                    var y = 0
                    placeables.chunked(4).forEachIndexed { r, row ->
                        row.forEachIndexed { i, p -> p.place(i * (cell + gap), y) }
                        y += rowHeights[r] + gap
                    }
                }
            }
        }
        if (hidden > 0) {
            Text(
                "Scroll for $hidden more",
                Modifier.fillMaxWidth().padding(top = 6.dp),
                style = FinioType.caption.copy(fontSize = 11.sp),
                color = colors.mutedForeground,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun RuleCategoryTile(c: Category, selected: Boolean, showType: Boolean, onClick: () -> Unit) {
    val colors = FinioTheme.colors
    val shape = FinioShapes.sm
    val tint = parseHexColor(c.color)
    val ring = remember(colors.primary, selected) {
        if (selected) listOf(CssShadow(spread = 1.5.dp, color = colors.primary)) else emptyList()
    }
    Column(
        Modifier
            .cssShadow(shape, ring + if (selected) FinioTheme.shadows.float else emptyList())
            .clip(shape)
            .background(if (selected) tint.copy(alpha = 0x22 / 255f) else colors.card)
            .border(1.dp, if (selected) Color.Transparent else colors.border, shape)
            .semantics { this.selected = selected }
            .clickable(remember { MutableInteractionSource() }, null, role = Role.Button, onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(Modifier.size(28.dp).clip(FinioShapes.full).background(tint), contentAlignment = Alignment.Center) {
            CategoryIcon(c.icon, size = 14.dp, tint = Color.White)
        }
        com.slowatcoding.finio.ui.components.TileLabel(c.name, color = colors.foreground)
        if (showType) {
            Text(
                c.type.wire.replaceFirstChar { it.uppercase() },
                style = FinioType.caption.copy(fontSize = 10.sp, lineHeight = 10.sp),
                color = colors.mutedForeground,
            )
        }
    }
}
