package com.slowatcoding.finio.ui.screens.categories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.slowatcoding.finio.core.calc.TRANSFER_CATEGORY_ID
import com.slowatcoding.finio.core.calc.miscLast
import com.slowatcoding.finio.core.data.COLOR_PALETTE
import com.slowatcoding.finio.core.model.Category
import com.slowatcoding.finio.core.model.CategoryType
import com.slowatcoding.finio.core.store.NewCategory
import com.slowatcoding.finio.core.store.isProtectedCategory
import com.slowatcoding.finio.core.util.MAX_NAME_LENGTH
import com.slowatcoding.finio.core.util.cleanText
import com.slowatcoding.finio.core.util.stripLeading
import com.slowatcoding.finio.ui.common.BackButton
import com.slowatcoding.finio.ui.common.collectFinanceState
import com.slowatcoding.finio.ui.common.financeStore
import com.slowatcoding.finio.ui.components.ButtonVariant
import com.slowatcoding.finio.ui.components.CategoryIcon
import com.slowatcoding.finio.ui.components.FieldLabel
import com.slowatcoding.finio.ui.components.FinioButton
import com.slowatcoding.finio.ui.components.FinioCard
import com.slowatcoding.finio.ui.components.FinioChip
import com.slowatcoding.finio.ui.components.FinioDialog
import com.slowatcoding.finio.ui.components.FinioDivider
import com.slowatcoding.finio.ui.components.FinioIconButton
import com.slowatcoding.finio.ui.components.FinioScreen
import com.slowatcoding.finio.ui.components.FinioTextField
import com.slowatcoding.finio.ui.components.HeaderIconButton
import com.slowatcoding.finio.ui.components.HeaderIconTone
import com.slowatcoding.finio.ui.components.LocalConfirm
import com.slowatcoding.finio.ui.components.ScreenTitle
import com.slowatcoding.finio.ui.components.parseHexColor
import com.slowatcoding.finio.ui.components.toast
import com.slowatcoding.finio.ui.icons.LucideIcons
import com.slowatcoding.finio.ui.navigation.FinioNavigator
import com.slowatcoding.finio.ui.theme.FinioTheme
import com.slowatcoding.finio.ui.theme.FinioType
import kotlinx.coroutines.launch

private enum class CategoryFilter(val label: String) { All("All"), Expense("Expense"), Income("Income"), Both("Both") }

private fun CategoryType.label() = when (this) {
    CategoryType.Expense -> "Expense"
    CategoryType.Income -> "Income"
    CategoryType.Both -> "Both"
}

/** Port of web/src/pages/ManageCategories.tsx — route `/manage-categories`. */
@Composable
fun ManageCategoriesScreen(nav: FinioNavigator) {
    val store = financeStore()
    val state by collectFinanceState()
    val confirm = LocalConfirm.current
    val scope = rememberCoroutineScope()
    val colors = FinioTheme.colors
    val categories = state.categories

    var open by rememberSaveable { mutableStateOf(false) }
    var editId by rememberSaveable { mutableStateOf<String?>(null) }
    var name by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf(CategoryType.Expense) }
    var color by rememberSaveable { mutableStateOf(COLOR_PALETTE[0]) }
    var icon by rememberSaveable { mutableStateOf("circle-ellipsis") }
    var error by remember { mutableStateOf<String?>(null) }
    var filter by rememberSaveable { mutableStateOf(CategoryFilter.All) }

    val filtered = remember(categories, filter) {
        when (filter) {
            CategoryFilter.All -> miscLast(categories)
            CategoryFilter.Expense -> miscLast(categories.filter { it.type == CategoryType.Expense || it.type == CategoryType.Both })
            CategoryFilter.Income -> miscLast(categories.filter { it.type == CategoryType.Income || it.type == CategoryType.Both })
            CategoryFilter.Both -> miscLast(categories.filter { it.type == CategoryType.Both })
        }
    }

    fun resetForm() {
        open = false
        editId = null
        name = ""
        type = CategoryType.Expense
        color = COLOR_PALETTE[0]
        icon = "circle-ellipsis"
        error = null
    }

    fun handleEdit(cat: Category) {
        editId = cat.id
        name = cat.name
        type = cat.type
        color = cat.color
        icon = cat.icon
        error = null
        open = true
    }

    fun handleSubmit() {
        val cleanName = cleanText(name, MAX_NAME_LENGTH)
        if (cleanName.isEmpty()) {
            error = "Enter a name"
            toast.error("Enter a name")
            return
        }
        val key = cleanName.lowercase()
        if (categories.any { it.id != editId && it.type == type && it.name.trim().lowercase() == key }) {
            val msg = "A ${type.wire} category named \"$cleanName\" already exists"
            error = msg
            toast.error(msg)
            return
        }
        val id = editId
        if (id != null) {
            store.updateCategory(id) { it.copy(name = cleanName, type = type, color = color, icon = icon) }
            toast.success("Category updated")
        } else {
            store.addCategory(NewCategory(name = cleanName, icon = icon, color = color, type = type))
            toast.success("Category added")
        }
        resetForm()
    }

    FinioScreen(header = {
        BackButton(nav)
        ScreenTitle("Categories")
        HeaderIconButton(
            LucideIcons.Plus,
            "Add category",
            onClick = {
                resetForm()
                open = true
            },
            tone = HeaderIconTone.Primary,
        )
    }) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CategoryFilter.entries.forEach { f ->
                FinioChip(f.label, selected = filter == f, onClick = { filter = f })
            }
        }

        FinioCard(contentPadding = PaddingValues(horizontal = 16.dp)) {
            filtered.forEachIndexed { index, cat ->
                if (index > 0) FinioDivider()
                val isProtected = isProtectedCategory(cat.id)
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CategoryIcon(cat.icon, size = 16.dp, tint = parseHexColor(cat.color))
                    Column(Modifier.weight(1f)) {
                        Text(cat.name, style = FinioType.bodyMedium, color = colors.foreground, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        val note = when {
                            !isProtected -> ""
                            cat.id == TRANSFER_CATEGORY_ID -> " · Built in, used by every transfer"
                            else -> " · Built in, the catch-all for deleted categories"
                        }
                        Text(cat.type.label() + note, style = FinioType.caption, color = colors.mutedForeground)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        FinioIconButton(
                            LucideIcons.Pencil,
                            "Edit ${cat.name}",
                            onClick = { handleEdit(cat) },
                            tint = colors.mutedForeground,
                        )
                        if (isProtected) {
                            val desc = "${cat.name} is built in and can't be deleted"
                            Box(Modifier.size(32.dp).semantics { contentDescription = desc }, contentAlignment = Alignment.Center) {
                                Icon(LucideIcons.Lock, null, Modifier.size(14.dp), tint = colors.mutedForeground)
                            }
                        } else {
                            FinioIconButton(
                                LucideIcons.Trash2,
                                "Delete ${cat.name}",
                                onClick = {
                                    scope.launch {
                                        val ok = confirm.confirm(
                                            title = "Delete \"${cat.name}\"?",
                                            description = "Transactions and recurring rules using it move to Miscellaneous, and any budget for it is removed.",
                                            confirmLabel = "Delete category",
                                        )
                                        if (ok) store.deleteCategory(cat.id)
                                    }
                                },
                                tint = colors.destructive,
                            )
                        }
                    }
                }
            }
        }
    }

    if (open) {
        FinioDialog(
            onDismissRequest = { resetForm() },
            title = if (editId != null) "Edit category" else "Add category",
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FinioTextField(
                    value = name,
                    onValueChange = {
                        name = stripLeading(it).take(MAX_NAME_LENGTH)
                        error = null
                    },
                    placeholder = "Category name",
                    isError = error != null,
                )
                DialogError(error)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(CategoryType.Expense, CategoryType.Income, CategoryType.Both).forEach { t ->
                        FinioChip(t.label(), selected = type == t, onClick = {
                            type = t
                            error = null
                        })
                    }
                }
                Column {
                    FieldLabel("Icon")
                    CategoryIconPicker(icon, onSelect = { icon = it })
                }
                Column {
                    FieldLabel("Color")
                    ColorPalettePicker(color, onSelect = { color = it })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FinioButton(if (editId != null) "Update" else "Add", onClick = { handleSubmit() }, modifier = Modifier.weight(1f))
                    FinioButton("Cancel", onClick = { resetForm() }, variant = ButtonVariant.Secondary)
                }
            }
        }
    }
}
