package com.slowatcoding.finio.ui.screens.labels

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.slowatcoding.finio.core.data.COLOR_PALETTE
import com.slowatcoding.finio.core.model.Label
import com.slowatcoding.finio.core.store.NewLabel
import com.slowatcoding.finio.core.util.MAX_NAME_LENGTH
import com.slowatcoding.finio.core.util.cleanText
import com.slowatcoding.finio.core.util.stripLeading
import com.slowatcoding.finio.ui.common.BackButton
import com.slowatcoding.finio.ui.common.collectFinanceState
import com.slowatcoding.finio.ui.common.financeStore
import com.slowatcoding.finio.ui.components.ButtonVariant
import com.slowatcoding.finio.ui.components.FieldLabel
import com.slowatcoding.finio.ui.components.FinioButton
import com.slowatcoding.finio.ui.components.FinioCard
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
import com.slowatcoding.finio.ui.screens.categories.ColorPalettePicker
import com.slowatcoding.finio.ui.screens.categories.DialogError
import com.slowatcoding.finio.ui.theme.FinioTheme
import com.slowatcoding.finio.ui.theme.FinioType
import kotlinx.coroutines.launch

/** Port of web/src/pages/ManageLabels.tsx — route `/manage-labels`. */
@Composable
fun ManageLabelsScreen(nav: FinioNavigator) {
    val store = financeStore()
    val state by collectFinanceState()
    val confirm = LocalConfirm.current
    val scope = rememberCoroutineScope()
    val colors = FinioTheme.colors
    val labels = state.labels

    var open by rememberSaveable { mutableStateOf(false) }
    var editId by rememberSaveable { mutableStateOf<String?>(null) }
    var name by rememberSaveable { mutableStateOf("") }
    var color by rememberSaveable { mutableStateOf(COLOR_PALETTE[0]) }
    var error by remember { mutableStateOf<String?>(null) }

    fun resetForm() {
        open = false
        editId = null
        name = ""
        color = COLOR_PALETTE[0]
        error = null
    }

    fun handleEdit(label: Label) {
        editId = label.id
        name = label.name
        color = label.color
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
        // Names compare case-insensitively, so "essential" can't sit beside "Essential".
        val key = cleanName.lowercase()
        if (labels.any { it.id != editId && it.name.trim().lowercase() == key }) {
            val msg = "A label named \"$cleanName\" already exists"
            error = msg
            toast.error(msg)
            return
        }
        val id = editId
        if (id != null) {
            store.updateLabel(id) { it.copy(name = cleanName, color = color) }
            toast.success("Label updated")
        } else {
            store.addLabel(NewLabel(name = cleanName, color = color))
            toast.success("Label added")
        }
        resetForm()
    }

    FinioScreen(header = {
        BackButton(nav)
        ScreenTitle("Labels")
        HeaderIconButton(
            LucideIcons.Plus,
            "Add label",
            onClick = {
                resetForm()
                open = true
            },
            tone = HeaderIconTone.Primary,
        )
    }) {
        if (labels.isNotEmpty()) {
            FinioCard(contentPadding = PaddingValues(horizontal = 16.dp)) {
                labels.forEachIndexed { index, label ->
                    if (index > 0) FinioDivider()
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(LucideIcons.Tag, null, Modifier.size(16.dp), tint = parseHexColor(label.color))
                        Text(
                            label.name,
                            Modifier.weight(1f),
                            style = FinioType.bodyMedium,
                            color = colors.foreground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            FinioIconButton(LucideIcons.Pencil, "Edit ${label.name}", onClick = { handleEdit(label) }, tint = colors.mutedForeground)
                            FinioIconButton(
                                LucideIcons.Trash2,
                                "Delete ${label.name}",
                                onClick = {
                                    scope.launch {
                                        val ok = confirm.confirm(
                                            title = "Delete \"${label.name}\"?",
                                            description = "It will be removed from every transaction tagged with it, and any budget for it is removed.",
                                            confirmLabel = "Delete label",
                                        )
                                        if (ok) store.deleteLabel(label.id)
                                    }
                                },
                                tint = colors.destructive,
                            )
                        }
                    }
                }
            }
        } else {
            Column(
                Modifier.fillMaxWidth().padding(vertical = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(LucideIcons.Tag, null, Modifier.padding(bottom = 12.dp).size(28.dp), tint = colors.mutedForeground)
                Text(
                    "No labels yet. Add one to tag your transactions.",
                    style = FinioType.body,
                    color = colors.mutedForeground,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }

    if (open) {
        FinioDialog(
            onDismissRequest = { resetForm() },
            title = if (editId != null) "Edit label" else "Add label",
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FinioTextField(
                    value = name,
                    onValueChange = {
                        name = stripLeading(it).take(MAX_NAME_LENGTH)
                        error = null
                    },
                    placeholder = "Label name",
                    isError = error != null,
                )
                DialogError(error)
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
