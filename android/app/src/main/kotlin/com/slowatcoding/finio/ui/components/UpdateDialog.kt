package com.slowatcoding.finio.ui.components

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.slowatcoding.finio.BuildConfig
import com.slowatcoding.finio.core.update.ReleaseInfo
import com.slowatcoding.finio.core.update.changelogLines
import com.slowatcoding.finio.ui.theme.FinioTheme
import com.slowatcoding.finio.ui.theme.FinioType

/**
 * "New version available" — shown once per app open when GitHub has a newer release than the
 * running build. Update opens the release page (the APK is attached there; Finio is sideloaded,
 * so the app cannot install itself). Not now hides it until the next open; Skip this version
 * hides it until a newer release exists.
 */
@Composable
fun UpdateDialog(
    release: ReleaseInfo,
    onNotNow: () -> Unit,
    onSkip: () -> Unit,
) {
    val context = LocalContext.current
    val colors = FinioTheme.colors
    val lines = remember(release.notes) { changelogLines(release.notes) }
    FinioDialog(
        onDismissRequest = onNotNow,
        title = "Update available",
        description = "Finio ${release.version} is out — you have ${BuildConfig.VERSION_NAME}.",
        footer = {
            // Phones stack these in reverse, so Update lands on top.
            FinioButton("Skip this version", onClick = onSkip, variant = ButtonVariant.Ghost)
            FinioButton("Not now", onClick = onNotNow, variant = ButtonVariant.Outline)
            FinioButton(
                "Update",
                onClick = {
                    val url = release.url.ifBlank { "https://github.com/abhi-sawant/finio/releases/latest" }
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) }
                    onNotNow()
                },
            )
        },
    ) {
        if (lines.isNotEmpty()) {
            Column(
                Modifier.heightIn(max = 240.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("What's new", style = FinioType.bodyMedium, color = colors.popoverForeground)
                lines.forEach { line ->
                    if (line.isBlank()) Text("", style = FinioType.caption)
                    else Text(line, style = FinioType.body, color = colors.mutedForeground)
                }
            }
        }
    }
}
