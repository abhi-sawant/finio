package com.slowatcoding.finio.ui.screens.settings

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import com.slowatcoding.finio.core.backup.collectBackupPayload
import com.slowatcoding.finio.core.backup.withBackupMeta
import com.slowatcoding.finio.core.format.todayKey
import com.slowatcoding.finio.platform.backup.AutoBackup
import com.slowatcoding.finio.platform.files.ExportResult
import com.slowatcoding.finio.platform.files.FinioMime
import com.slowatcoding.finio.platform.files.backupFileName
import com.slowatcoding.finio.platform.files.rememberDocumentExporter
import com.slowatcoding.finio.ui.common.financeStore
import com.slowatcoding.finio.ui.components.FinioDialog
import com.slowatcoding.finio.ui.components.toast
import kotlinx.serialization.json.JsonObject

/**
 * Port of web/src/components/settings/SecretDialogShell.tsx — the shared chrome of the PIN
 * dialog (AppLockSection) and the backup-passphrase dialogs (BackupSection).
 *
 * Compromise: the web anchors this dialog by its top edge so it doesn't jump between phases of
 * different heights; the shared Mudra dialog window is centred.
 */
@Composable
fun SecretDialogShell(
    onDismissRequest: () -> Unit,
    title: String,
    description: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    FinioDialog(onDismissRequest = onDismissRequest, title = title, description = description, content = content)
}

/**
 * `exportLocalBackup()` + its toasts, as the Data section's "Export data" and the PIN dialog's
 * "Export a backup first?" call it: a save-as picker (Android's stand-in for a browser download)
 * pre-filled with `finio-backup-<today>.json`, written as the web's pretty JSON with the meta stamp.
 */
@Composable
fun rememberBackupExport(): () -> Unit {
    val store = financeStore()
    val exporter = rememberDocumentExporter(FinioMime.JSON) { result ->
        when (result) {
            is ExportResult.Saved -> toast.success("Backup saved")
            is ExportResult.Failed -> toast.error("Export failed")
            ExportResult.Cancelled -> Unit
        }
    }
    return {
        exporter.export(backupFileName(todayKey())) {
            AutoBackup.PrettyJson.encodeToString(JsonObject.serializer(), withBackupMeta(collectBackupPayload(store.current)))
        }
    }
}
