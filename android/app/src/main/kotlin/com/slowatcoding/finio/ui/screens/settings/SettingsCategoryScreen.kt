package com.slowatcoding.finio.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.slowatcoding.finio.ui.components.FinioCard
import com.slowatcoding.finio.ui.components.FinioDivider
import com.slowatcoding.finio.ui.components.FinioScreen
import com.slowatcoding.finio.ui.components.HeaderIconButton
import com.slowatcoding.finio.ui.components.ScreenTitle
import com.slowatcoding.finio.ui.icons.LucideIcons
import com.slowatcoding.finio.ui.navigation.FinioNavigator
import com.slowatcoding.finio.ui.navigation.Routes

private val categoryTitles = linkedMapOf(
    "account" to "Cloud account",
    "backup" to "Backup & data",
    "profile" to "Profile & preferences",
    "security" to "Security",
    "notifications" to "Notifications",
    "organise" to "Categories, labels & rules",
)

/**
 * Port of web/src/pages/SettingsCategory.tsx — one Settings section under a left-aligned
 * back + title header. An unknown category replaces itself with Settings.
 */
@Composable
fun SettingsCategoryScreen(nav: FinioNavigator, category: String) {
    val title = categoryTitles[category]
    if (title == null) {
        LaunchedEffect(Unit) { nav.replace(Routes.Settings) }
        return
    }
    FinioScreen(header = {
        // `justify-start gap-2`: back and title sit together on the left.
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            // web: navigate('/settings') — replace lands on Settings even after a deep link.
            HeaderIconButton(LucideIcons.ArrowLeft, "Back to settings", onClick = { nav.replace(Routes.Settings) })
            ScreenTitle(title)
        }
    }) {
        when (category) {
            "account" -> CloudAccountSection(nav)
            "backup" -> BackupSection(nav)
            "profile" -> ProfileSection()
            "security" -> AppLockSection()
            "notifications" -> NotificationsSection()
            "organise" -> OrganiseLinks(nav)
        }
    }
}

@Composable
private fun OrganiseLinks(nav: FinioNavigator) {
    FinioCard(contentPadding = PaddingValues(0.dp)) {
        SettingsRow(LucideIcons.FolderOpen, "Manage categories", onClick = { nav.navigate(Routes.ManageCategories) }, chevron = true)
        FinioDivider()
        SettingsRow(LucideIcons.Tag, "Manage labels", onClick = { nav.navigate(Routes.ManageLabels) }, chevron = true)
        FinioDivider()
        SettingsRow(
            LucideIcons.Wand2,
            "Categorization rules",
            subtitle = "File transactions automatically from their note",
            onClick = { nav.navigate(Routes.CategoryRules()) },
            chevron = true,
        )
    }
}
