package com.slowatcoding.finio.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.slowatcoding.finio.core.format.formatShortDate
import com.slowatcoding.finio.di.appContainer
import com.slowatcoding.finio.ui.common.collectFinanceState
import com.slowatcoding.finio.ui.components.FinioCard
import com.slowatcoding.finio.ui.components.FinioDivider
import com.slowatcoding.finio.ui.components.FinioScreen
import com.slowatcoding.finio.ui.components.PageTitle
import com.slowatcoding.finio.ui.icons.LucideIcons
import com.slowatcoding.finio.ui.navigation.FinioNavigator
import com.slowatcoding.finio.ui.navigation.Routes
import com.slowatcoding.finio.ui.theme.FinioTheme
import com.slowatcoding.finio.ui.theme.FinioType

private data class SettingsCategoryEntry(val key: String, val icon: ImageVector, val title: String, val status: String)

/**
 * Port of web/src/pages/Settings.tsx — the category list (one glass card of rows, each with a
 * live one-line status) and the legal footer.
 */
@Composable
fun SettingsScreen(nav: FinioNavigator) {
    val container = appContainer()
    val auth by container.auth.state.collectAsStateWithLifecycle()
    val lock by container.appLock.state.collectAsStateWithLifecycle()
    val state by collectFinanceState()
    val settings = state.settings
    val user = auth.user
    val lastBackupAt = auth.lastBackupAt

    val categories = listOf(
        SettingsCategoryEntry(
            "account", LucideIcons.Cloud, "Cloud account",
            if (!auth.token.isNullOrEmpty() && user != null) user.email else "Not signed in",
        ),
        SettingsCategoryEntry(
            "backup", LucideIcons.DatabaseBackup, "Backup & data",
            if (lastBackupAt != null) "Last cloud backup ${formatShortDate(lastBackupAt)}" else "Export, import, restore",
        ),
        SettingsCategoryEntry(
            "profile", LucideIcons.SlidersHorizontal, "Profile & preferences",
            settings.userName.ifEmpty { "Name, theme, month start" },
        ),
        SettingsCategoryEntry(
            "security", LucideIcons.ShieldCheck, "Security",
            if (lock.config?.enabled == true) "App lock on" else "App lock off",
        ),
        SettingsCategoryEntry(
            "notifications", LucideIcons.Bell, "Notifications",
            if (settings.notificationsEnabled) "Reminders on" else "Reminders off",
        ),
        SettingsCategoryEntry(
            "organise", LucideIcons.FolderOpen, "Categories, labels & rules",
            "Organise how transactions are filed",
        ),
    )

    FinioScreen(header = { PageTitle("Settings") }) {
        FinioCard(contentPadding = PaddingValues(0.dp)) {
            categories.forEachIndexed { i, c ->
                if (i > 0) FinioDivider()
                SettingsRow(
                    icon = c.icon,
                    title = c.title,
                    subtitle = c.status,
                    chevron = true,
                    truncateSubtitle = true,
                    onClick = { nav.navigate(Routes.SettingsCategory(c.key)) },
                )
            }
        }

        LegalFooter(nav)
    }
}

@Composable
private fun LegalFooter(nav: FinioNavigator) {
    val colors = FinioTheme.colors
    val small = FinioType.caption.copy(fontSize = 11.sp, lineHeight = 16.sp)
    Column(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FooterLink("Privacy Policy", small) { nav.navigate(Routes.Privacy) }
            Text(" · ", style = small, color = colors.mutedForeground)
            FooterLink("Terms of Service", small) { nav.navigate(Routes.Terms) }
        }
    }
    Text(
        "Finio · Personal Finance · v${com.slowatcoding.finio.BuildConfig.VERSION_NAME}",
        Modifier.fillMaxWidth(),
        style = small.copy(textAlign = androidx.compose.ui.text.style.TextAlign.Center),
        color = colors.mutedForeground,
    )
}

@Composable
private fun FooterLink(text: String, style: TextStyle, onClick: () -> Unit) {
    Text(
        text,
        Modifier.clickable(role = Role.Button, onClick = onClick).padding(vertical = 4.dp),
        style = style,
        color = FinioTheme.colors.mutedForeground,
    )
}
