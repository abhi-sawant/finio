package com.slowatcoding.finio.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.slowatcoding.finio.core.crypto.BACKUP_KEY_ITERATIONS
import com.slowatcoding.finio.core.crypto.createVerifier
import com.slowatcoding.finio.core.crypto.deriveEncryptionKey
import com.slowatcoding.finio.core.crypto.generateBackupSalt
import com.slowatcoding.finio.core.crypto.isBackupCryptoSupported
import com.slowatcoding.finio.core.crypto.verifyPassphraseAgainstConfig
import com.slowatcoding.finio.core.format.formatCurrency
import com.slowatcoding.finio.core.format.formatFileSize
import com.slowatcoding.finio.core.format.formatFullDate
import com.slowatcoding.finio.core.format.formatShortDate
import com.slowatcoding.finio.core.format.formatTime
import com.slowatcoding.finio.core.importing.ENTITY_LABELS
import com.slowatcoding.finio.core.importing.IMPORT_ENTITIES
import com.slowatcoding.finio.core.importing.ValidatedBackup
import com.slowatcoding.finio.core.importing.hasImportableData
import com.slowatcoding.finio.core.importing.validateBackup
import com.slowatcoding.finio.core.js.nowInstant
import com.slowatcoding.finio.core.js.toIso
import com.slowatcoding.finio.core.model.BackupCryptoConfig
import com.slowatcoding.finio.core.model.FinioJson
import com.slowatcoding.finio.core.model.ImportMode
import com.slowatcoding.finio.core.util.getErrorMessage
import com.slowatcoding.finio.di.appContainer
import com.slowatcoding.finio.platform.api.BackupListEntry
import com.slowatcoding.finio.platform.backup.AutoBackup
import com.slowatcoding.finio.platform.files.BackupFolder
import com.slowatcoding.finio.platform.files.FinioMime
import com.slowatcoding.finio.platform.files.ImportResult
import com.slowatcoding.finio.platform.files.rememberBackupFolderPicker
import com.slowatcoding.finio.platform.files.rememberDocumentImporter
import com.slowatcoding.finio.ui.common.collectFinanceState
import com.slowatcoding.finio.ui.common.financeStore
import com.slowatcoding.finio.ui.components.ButtonSize
import com.slowatcoding.finio.ui.components.ButtonVariant
import com.slowatcoding.finio.ui.components.FinioButton
import com.slowatcoding.finio.ui.components.FinioCard
import com.slowatcoding.finio.ui.components.FinioDialog
import com.slowatcoding.finio.ui.components.FinioDivider
import com.slowatcoding.finio.ui.components.FinioIconButton
import com.slowatcoding.finio.ui.components.FinioTextField
import com.slowatcoding.finio.ui.components.LocalConfirm
import com.slowatcoding.finio.ui.components.SwitchField
import com.slowatcoding.finio.ui.components.toast
import com.slowatcoding.finio.ui.icons.LucideIcons
import com.slowatcoding.finio.ui.navigation.FinioNavigator
import com.slowatcoding.finio.ui.navigation.Routes
import com.slowatcoding.finio.ui.theme.FinioShapes
import com.slowatcoding.finio.ui.theme.FinioTheme
import com.slowatcoding.finio.ui.theme.FinioType
import com.slowatcoding.finio.ui.theme.mix
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

private enum class CryptoDialogKind { Set, Change, Disable, Unlock, Restore }
private enum class CryptoPhase { Current, Enter, Confirm }

/** The import dry-run being reviewed: the validated backup plus the picked file's name. */
private class ImportPreview(val validated: ValidatedBackup, val fileName: String?)

/**
 * Port of web/src/components/settings/BackupSection.tsx — cloud backup (back up, restore latest,
 * history), backup encryption (set / change / disable / unlock, and the restore passphrase
 * prompt), and the local data tools (daily backup + folder, export, import with a dry-run
 * preview, bank CSV, reconcile, reset).
 */
@Composable
fun BackupSection(nav: FinioNavigator) {
    val container = appContainer()
    val context = LocalContext.current
    val store = financeStore()
    val state by collectFinanceState()
    val settings = state.settings
    val auth by container.auth.state.collectAsStateWithLifecycle()
    val crypto by container.backupCrypto.state.collectAsStateWithLifecycle()
    val confirm = LocalConfirm.current
    val scope = rememberCoroutineScope()
    val colors = FinioTheme.colors

    val token = auth.token?.takeIf { it.isNotEmpty() }
    val lastBackupAt = auth.lastBackupAt

    var backingUp by remember { mutableStateOf(false) }
    var restoring by remember { mutableStateOf(false) }
    val folder = remember(context) { BackupFolder(context) }
    var backupFolderName by remember { mutableStateOf<String?>(null) }
    var showFolderSetupInfo by remember { mutableStateOf(false) }
    var preview by remember { mutableStateOf<ImportPreview?>(null) }

    var showBackupHistory by remember { mutableStateOf(false) }
    var backupList by remember { mutableStateOf<List<BackupListEntry>?>(null) }
    var backupListLoading by remember { mutableStateOf(false) }
    var backupListError by remember { mutableStateOf<String?>(null) }
    var busyBackupDate by remember { mutableStateOf<String?>(null) }

    val cryptoConfig = crypto.config
    val cryptoEnabled = cryptoConfig?.enabled == true
    // Enabled, but the in-memory key hasn't been (re-)entered this session.
    val cryptoLocked = cryptoEnabled && crypto.sessionKeySalt != cryptoConfig?.salt

    var cryptoDialog by remember { mutableStateOf<CryptoDialogKind?>(null) }
    var cryptoPhase by remember { mutableStateOf(CryptoPhase.Enter) }
    var passphraseEntry by remember { mutableStateOf("") }
    var firstPassphrase by remember { mutableStateOf("") }
    var passphraseError by remember { mutableStateOf<String?>(null) }
    var passphraseBusy by remember { mutableStateOf(false) }
    // null means "restore latest" — a date restores that dated backup instead.
    var pendingRestoreDate by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(folder) {
        backupFolderName = withContext(Dispatchers.IO) { if (folder.isGranted()) folder.displayName() ?: "" else null }
    }

    val handleExport = rememberBackupExport()

    val pickFolder = rememberBackupFolderPicker { uri ->
        if (uri == null) return@rememberBackupFolderPicker // cancelled
        scope.launch {
            val name = withContext(Dispatchers.IO) { folder.displayName() } ?: ""
            backupFolderName = name
            toast.success("Backups will be saved to \"$name\"")
        }
    }

    val pickImport = rememberDocumentImporter(FinioMime.JSON_IMPORT) { result ->
        when (result) {
            ImportResult.Cancelled -> Unit
            is ImportResult.Failed -> toast.error("Couldn't read that file")
            is ImportResult.Loaded -> scope.launch {
                val parsed = try {
                    withContext(Dispatchers.Default) { FinioJson.parseToJsonElement(result.text) }
                } catch (_: Exception) {
                    toast.error("That file isn't valid JSON")
                    return@launch
                }
                try {
                    // Validate and preview first — an import rewrites every row in the app.
                    val validated = withContext(Dispatchers.Default) { validateBackup(parsed) }
                    preview = ImportPreview(validated, result.displayName)
                } catch (e: Exception) {
                    toast.error(getErrorMessage(e, "Invalid backup file"))
                }
            }
        }
    }

    fun handleDisconnectBackupFolder() {
        folder.clear()
        backupFolderName = null
        toast.success("Backup folder disconnected")
    }

    fun runImport(mode: ImportMode) {
        val p = preview ?: return
        store.importData(p.validated.data, mode)
        preview = null
        // The pending schedule points at the budgets and rules that were just replaced.
        scope.launch { container.refreshReminders() }
        toast.success(if (mode == ImportMode.Merge) "Backup merged" else "Data replaced from backup")
    }

    fun handleReconcile() {
        val diff = store.recomputeBalances()
        if (diff.changed == 0) {
            toast.success("All balances already match their transactions")
            return
        }
        toast.success(
            "Reconciled ${diff.changed} account${if (diff.changed == 1) "" else "s"} · net " +
                "${if (diff.totalDrift >= 0) "+" else "−"}${formatCurrency(abs(diff.totalDrift))}",
        )
    }

    fun handleReset() {
        scope.launch {
            val confirmed = confirm.confirm(
                title = "Reset all data?",
                description = "Accounts, transactions, budgets, recurring rules, loans, savings goals, people/debts, " +
                    "categorization rules, templates and net-worth history will be erased, and categories and labels " +
                    "restored to defaults. Your name and settings are kept. This cannot be undone.",
                confirmLabel = "Reset everything",
            )
            if (confirmed) {
                store.resetToDefaults()
                // The reminder schedule and fired ledger live outside the store — without this a
                // reminder would fire for a budget that no longer exists.
                container.disableReminders()
                container.refreshReminders()
                toast.success("Reset complete")
            }
        }
    }

    fun openCryptoDialog(which: CryptoDialogKind) {
        passphraseEntry = ""
        firstPassphrase = ""
        passphraseError = null
        cryptoPhase = if (which == CryptoDialogKind.Set) CryptoPhase.Enter else CryptoPhase.Current
        cryptoDialog = which
    }

    fun closeCryptoDialog() {
        cryptoDialog = null
        cryptoPhase = CryptoPhase.Enter
        passphraseEntry = ""
        firstPassphrase = ""
        passphraseError = null
        passphraseBusy = false
        pendingRestoreDate = null
    }

    fun openRestorePassphrasePrompt(date: String?) {
        passphraseEntry = ""
        passphraseError = null
        pendingRestoreDate = date
        cryptoDialog = CryptoDialogKind.Restore
    }

    fun handleCloudBackup() {
        if (cryptoLocked) {
            openCryptoDialog(CryptoDialogKind.Unlock)
            return
        }
        backingUp = true
        scope.launch {
            try {
                AutoBackup.uploadBackup(container.backupDataSource, container.cloudBackupSession, container.api)
                toast.success("Backup uploaded successfully")
            } catch (e: Exception) {
                toast.error(getErrorMessage(e, "Backup failed"))
            } finally {
                backingUp = false
            }
        }
    }

    fun handleCloudRestore() {
        scope.launch {
            val confirmed = confirm.confirm(
                title = "Restore from cloud backup?",
                description = "Your current data will be replaced by the most recent backup on the server.",
                confirmLabel = "Restore",
            )
            if (!confirmed) return@launch
            restoring = true
            try {
                CloudRestore.restoreLatestBackup(container)
                toast.success("Data restored from cloud backup")
            } catch (e: PassphraseRequiredError) {
                openRestorePassphrasePrompt(null)
            } catch (e: Exception) {
                toast.error(getErrorMessage(e, "Restore failed"))
            } finally {
                restoring = false
            }
        }
    }

    fun loadBackupHistory() {
        backupListLoading = true
        backupListError = null
        scope.launch {
            try {
                backupList = CloudRestore.listCloudBackups(container)
            } catch (e: Exception) {
                backupList = null
                backupListError = getErrorMessage(e, "Couldn't load backup history")
            } finally {
                backupListLoading = false
            }
        }
    }

    fun openBackupHistory() {
        showBackupHistory = true
        loadBackupHistory()
    }

    fun handleRestoreBackupDate(date: String) {
        scope.launch {
            val confirmed = confirm.confirm(
                title = "Restore backup from ${formatFullDate(date)}?",
                description = "Your current data will be replaced by this backup.",
                confirmLabel = "Restore",
            )
            if (!confirmed) return@launch
            busyBackupDate = date
            try {
                CloudRestore.restoreBackupByDate(container, date)
                toast.success("Data restored from backup")
                showBackupHistory = false
            } catch (e: PassphraseRequiredError) {
                openRestorePassphrasePrompt(date)
            } catch (e: Exception) {
                toast.error(getErrorMessage(e, "Restore failed"))
            } finally {
                busyBackupDate = null
            }
        }
    }

    fun handleDeleteBackupDate(date: String) {
        scope.launch {
            val confirmed = confirm.confirm(
                title = "Delete backup from ${formatFullDate(date)}?",
                description = "This backup will be permanently removed from the server.",
                confirmLabel = "Delete",
            )
            if (!confirmed) return@launch
            busyBackupDate = date
            try {
                CloudRestore.deleteCloudBackup(container, date)
                backupList = backupList?.filter { it.backupDate != date }
                toast.success("Backup deleted")
            } catch (e: Exception) {
                toast.error(getErrorMessage(e, "Delete failed"))
            } finally {
                busyBackupDate = null
            }
        }
    }

    fun saveBackupPassphrase(passphrase: String) {
        passphraseBusy = true
        val wasChanging = cryptoDialog == CryptoDialogKind.Change
        scope.launch {
            try {
                val salt = generateBackupSalt()
                val (key, verifier) = withContext(Dispatchers.Default) {
                    val k = deriveEncryptionKey(passphrase, salt, BACKUP_KEY_ITERATIONS)
                    k to createVerifier(k)
                }
                container.backupCrypto.setConfig(
                    BackupCryptoConfig(
                        enabled = true,
                        salt = salt,
                        iterations = BACKUP_KEY_ITERATIONS,
                        verifierIv = verifier.iv,
                        verifierCiphertext = verifier.ciphertext,
                        createdAt = nowInstant().toIso(),
                    ),
                )
                container.backupCrypto.setSessionKey(key, salt)
                closeCryptoDialog()
                toast.success(if (wasChanging) "Backup passphrase changed" else "Cloud backups are now encrypted")
            } finally {
                passphraseBusy = false
            }
        }
    }

    fun handleCryptoPassphraseSubmit() {
        if (passphraseBusy || passphraseEntry.isEmpty()) return
        passphraseError = null
        val dialog = cryptoDialog ?: return

        if (dialog == CryptoDialogKind.Unlock || cryptoPhase == CryptoPhase.Current) {
            val config = cryptoConfig ?: return
            val entry = passphraseEntry
            passphraseBusy = true
            scope.launch {
                val (key, ok) = withContext(Dispatchers.Default) {
                    val k = deriveEncryptionKey(entry, config.salt, config.iterations)
                    k to verifyPassphraseAgainstConfig(k, config.verifierIv, config.verifierCiphertext)
                }
                passphraseBusy = false
                if (!ok) {
                    passphraseError = "Incorrect passphrase"
                    passphraseEntry = ""
                    return@launch
                }
                when (dialog) {
                    CryptoDialogKind.Disable -> {
                        container.backupCrypto.clearConfig()
                        closeCryptoDialog()
                        toast.success("Cloud backup encryption is off")
                    }
                    CryptoDialogKind.Unlock -> {
                        container.backupCrypto.setSessionKey(key, config.salt)
                        closeCryptoDialog()
                        toast.success("Cloud backup unlocked for this session")
                    }
                    else -> {
                        // 'change' — current passphrase verified, move on to choosing the new one.
                        passphraseEntry = ""
                        cryptoPhase = CryptoPhase.Enter
                    }
                }
            }
            return
        }

        when (cryptoPhase) {
            CryptoPhase.Enter -> {
                if (passphraseEntry.length < 8) {
                    passphraseError = "Use at least 8 characters"
                    return
                }
                firstPassphrase = passphraseEntry
                passphraseEntry = ""
                cryptoPhase = CryptoPhase.Confirm
            }
            CryptoPhase.Confirm -> {
                if (passphraseEntry != firstPassphrase) {
                    passphraseEntry = ""
                    cryptoPhase = CryptoPhase.Enter
                    passphraseError = "Those didn't match. Try again."
                    return
                }
                saveBackupPassphrase(passphraseEntry)
            }
            CryptoPhase.Current -> Unit
        }
    }

    fun handleRestorePassphraseSubmit() {
        if (passphraseBusy || passphraseEntry.isEmpty()) return
        passphraseError = null
        passphraseBusy = true
        val entry = passphraseEntry
        val date = pendingRestoreDate
        scope.launch {
            try {
                if (date == null) CloudRestore.restoreLatestBackup(container, entry)
                else CloudRestore.restoreBackupByDate(container, date, entry)
                closeCryptoDialog()
                showBackupHistory = false
                toast.success("Data restored from cloud backup")
            } catch (e: Exception) {
                passphraseError = getErrorMessage(e, "Restore failed")
            } finally {
                passphraseBusy = false
            }
        }
    }

    // ---- Cloud backup ------------------------------------------------------------------------
    if (token != null) {
        FinioCard(contentPadding = PaddingValues(0.dp)) {
            SettingsRow(
                LucideIcons.CloudUpload,
                if (backingUp) "Backing up…" else "Back up to cloud",
                subtitle = lastBackupAt?.let { "Last: ${formatShortDate(it)}, ${formatTime(it)}" },
                enabled = !backingUp,
                onClick = ::handleCloudBackup,
            )
            FinioDivider()
            SettingsRow(
                LucideIcons.Cloud,
                if (restoring) "Restoring…" else "Restore from cloud",
                enabled = !restoring,
                onClick = ::handleCloudRestore,
            )
            FinioDivider()
            SettingsRow(LucideIcons.History, "Backup history", onClick = ::openBackupHistory)
        }
    }

    // ---- Backup encryption -------------------------------------------------------------------
    if (token != null && isBackupCryptoSupported()) {
        FinioCard(contentPadding = PaddingValues(0.dp)) {
            SwitchField(
                title = "Encrypt cloud backups",
                description = "Encrypt backups with a passphrase before they leave this device. Finio never sees it " +
                    "and can't recover it if you forget it.",
                checked = cryptoEnabled,
                onCheckedChange = { next -> openCryptoDialog(if (next) CryptoDialogKind.Set else CryptoDialogKind.Disable) },
                icon = { Icon(LucideIcons.ShieldCheck, null, Modifier.size(18.dp), tint = colors.mutedForeground) },
                modifier = Modifier.padding(16.dp),
            )
            if (cryptoEnabled) {
                if (cryptoLocked) {
                    FinioDivider()
                    SettingsRow(
                        LucideIcons.LockKeyhole,
                        "Cloud backup locked",
                        subtitle = "Tap to enter your passphrase and resume automatic backups",
                        tone = RowTone.Warning,
                        onClick = { openCryptoDialog(CryptoDialogKind.Unlock) },
                    )
                }
                FinioDivider()
                SettingsRow(LucideIcons.KeyRound, "Change passphrase", chevron = true, onClick = { openCryptoDialog(CryptoDialogKind.Change) })
            }
        }
    }

    // ---- Data --------------------------------------------------------------------------------
    FinioCard(contentPadding = PaddingValues(0.dp)) {
        SwitchField(
            // Android: no browser download to fall back on, so the daily file goes to the folder below.
            title = "Auto-save daily backup",
            description = "Save a backup JSON to your backup folder once per day when the app opens",
            checked = settings.autoLocalBackup,
            onCheckedChange = { next -> store.updateSettings { it.copy(autoLocalBackup = next) } },
            icon = { Icon(LucideIcons.HardDrive, null, Modifier.size(18.dp), tint = colors.mutedForeground) },
            modifier = Modifier.padding(16.dp),
        )
        FinioDivider()
        SettingsValueRow(
            LucideIcons.Folder,
            "Backup folder",
            subtitle = backupFolderName?.let { "Saving to \"$it\" · keeps latest 10" }
                ?: "Not connected — choose a folder for daily backups",
        ) {
            PillButton(
                if (backupFolderName != null) "Disconnect" else "Choose folder",
                onClick = { if (backupFolderName != null) handleDisconnectBackupFolder() else showFolderSetupInfo = true },
                small = true,
            )
        }
        FinioDivider()
        SettingsRow(LucideIcons.Download, "Export data (JSON)", onClick = handleExport)
        FinioDivider()
        SettingsRow(LucideIcons.Upload, "Import data", onClick = pickImport)
        FinioDivider()
        SettingsRow(
            LucideIcons.FileSpreadsheet,
            "Import bank CSV",
            subtitle = "Map columns from a bank or card statement export",
            onClick = { nav.navigate(Routes.ImportCsv) },
        )
        FinioDivider()
        SettingsRow(
            LucideIcons.Scale,
            "Reconcile balances",
            subtitle = "Rebuild every account balance from its opening balance and transactions",
            onClick = ::handleReconcile,
        )
        FinioDivider()
        SettingsRow(LucideIcons.RotateCcw, "Reset to defaults", tone = RowTone.Destructive, onClick = ::handleReset)
    }

    // ---- Dialogs -----------------------------------------------------------------------------

    if (showFolderSetupInfo) {
        FinioDialog(onDismissRequest = { showFolderSetupInfo = false }, title = "Set up backup folder") {
            Text(
                buildAnnotatedString {
                    append("In the folder picker that opens next, create a new folder named ")
                    withStyle(SpanStyle(color = colors.foreground, fontWeight = FontWeight.Bold)) { append("\"Finio\"") }
                    append(
                        " inside your Downloads folder, then select it. This is the recommended setup — it keeps " +
                            "backups organized in one place and lets Finio automatically keep only the 10 most recent, " +
                            "deleting older ones for you.",
                    )
                },
                style = FinioType.body,
                color = colors.mutedForeground,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FinioButton(
                    "Continue",
                    onClick = {
                        showFolderSetupInfo = false
                        pickFolder()
                    },
                    size = ButtonSize.Lg,
                    modifier = Modifier.weight(1f),
                )
                FinioButton("Cancel", onClick = { showFolderSetupInfo = false }, variant = ButtonVariant.Secondary, size = ButtonSize.Lg)
            }
        }
    }

    val dialog = cryptoDialog
    if (dialog != null && dialog != CryptoDialogKind.Restore) {
        SecretDialogShell(
            onDismissRequest = ::closeCryptoDialog,
            title = when (dialog) {
                CryptoDialogKind.Disable -> "Turn off backup encryption"
                CryptoDialogKind.Unlock -> "Unlock cloud backup"
                CryptoDialogKind.Change -> "Change passphrase"
                else -> "Set a backup passphrase"
            },
            description = when {
                dialog == CryptoDialogKind.Disable ->
                    "Enter your current passphrase to turn off encryption. Future backups will be plaintext; existing " +
                        "encrypted backups still need this passphrase to restore."
                dialog == CryptoDialogKind.Unlock ->
                    "Enter your backup passphrase to resume automatic cloud backups for this session."
                cryptoPhase == CryptoPhase.Current -> "Enter your current passphrase first."
                cryptoPhase == CryptoPhase.Confirm -> "Enter it once more to confirm."
                else -> "Choose a passphrase you'll remember — it's separate from your account password and can't be recovered if lost."
            },
        ) {
            PassphraseForm(
                value = passphraseEntry,
                onValueChange = {
                    if (passphraseError != null) passphraseError = null
                    passphraseEntry = it
                },
                placeholder = if (cryptoPhase == CryptoPhase.Confirm) "Confirm passphrase" else "Passphrase",
                error = passphraseError,
                busy = passphraseBusy,
                buttonLabel = when {
                    dialog == CryptoDialogKind.Disable -> "Turn off"
                    cryptoPhase == CryptoPhase.Confirm -> "Confirm"
                    else -> "Continue"
                },
                // Re-focus the field on every phase change.
                focusKey = cryptoPhase,
                onSubmit = ::handleCryptoPassphraseSubmit,
            )
        }
    }

    if (dialog == CryptoDialogKind.Restore) {
        SecretDialogShell(
            onDismissRequest = ::closeCryptoDialog,
            title = "Enter backup passphrase",
            description = "This backup is encrypted. Enter the passphrase it was encrypted with to restore it.",
        ) {
            PassphraseForm(
                value = passphraseEntry,
                onValueChange = {
                    if (passphraseError != null) passphraseError = null
                    passphraseEntry = it
                },
                placeholder = "Passphrase",
                error = passphraseError,
                busy = passphraseBusy,
                buttonLabel = "Restore",
                focusKey = Unit,
                onSubmit = ::handleRestorePassphraseSubmit,
            )
        }
    }

    preview?.let { p -> ImportPreviewDialog(p, onDismiss = { preview = null }, onImport = ::runImport) }

    if (showBackupHistory) {
        FinioDialog(
            onDismissRequest = { showBackupHistory = false },
            title = "Backup history",
            description = "Every backup version stored on the server.",
        ) {
            val list = backupList
            val error = backupListError
            when {
                backupListLoading -> Text(
                    "Loading…",
                    Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    style = FinioType.body,
                    color = colors.mutedForeground,
                    textAlign = TextAlign.Center,
                )
                error != null -> Column(
                    Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(LucideIcons.AlertTriangle, null, Modifier.size(20.dp), tint = colors.destructive)
                    Text("Couldn’t load backup history", style = FinioType.bodyMedium, color = colors.foreground)
                    Text(error, style = FinioType.caption, color = colors.mutedForeground, textAlign = TextAlign.Center)
                    FinioButton("Retry", onClick = ::loadBackupHistory, variant = ButtonVariant.Secondary)
                }
                list.isNullOrEmpty() -> Text(
                    "No backups found",
                    Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    style = FinioType.body,
                    color = colors.mutedForeground,
                    textAlign = TextAlign.Center,
                )
                else -> Column {
                    list.forEachIndexed { i, backup ->
                        if (i > 0) FinioDivider()
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    formatFullDate(backup.backupDate),
                                    style = FinioType.bodyMedium,
                                    color = colors.foreground,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(formatFileSize(backup.fileSize), style = FinioType.caption, color = colors.mutedForeground)
                            }
                            val busy = busyBackupDate == backup.backupDate
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                FinioButton(
                                    "Restore",
                                    onClick = { handleRestoreBackupDate(backup.backupDate) },
                                    variant = ButtonVariant.Secondary,
                                    size = ButtonSize.Sm,
                                    enabled = !busy,
                                )
                                FinioIconButton(
                                    LucideIcons.Trash2,
                                    "Delete backup from ${formatFullDate(backup.backupDate)}",
                                    onClick = { handleDeleteBackupDate(backup.backupDate) },
                                    enabled = !busy,
                                    tint = colors.destructive,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** The passphrase input + error + submit button every passphrase phase shows. */
@Composable
private fun PassphraseForm(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    error: String?,
    busy: Boolean,
    buttonLabel: String,
    focusKey: Any,
    onSubmit: () -> Unit,
) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(focusKey) { runCatching { focus.requestFocus() } }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        FinioTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.focusRequester(focus),
            placeholder = placeholder,
            enabled = !busy,
            isError = error != null,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done, autoCorrectEnabled = false),
            keyboardActions = KeyboardActions(onDone = { onSubmit() }),
        )
        SecretDialogError(error)
        FinioButton(
            buttonLabel,
            onClick = onSubmit,
            enabled = !busy && value.isNotEmpty(),
            size = ButtonSize.Lg,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ImportPreviewDialog(p: ImportPreview, onDismiss: () -> Unit, onImport: (ImportMode) -> Unit) {
    val colors = FinioTheme.colors
    val report = p.validated.report
    val importable = hasImportableData(report)
    val exportedAt = p.validated.meta.exportedAt
    val description = buildString {
        append(p.fileName ?: "")
        if (exportedAt != null) append(" · Exported on ${formatShortDate(exportedAt)}")
    }.ifEmpty { null }

    FinioDialog(onDismissRequest = onDismiss, title = "Review import", description = description) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column {
                var first = true
                IMPORT_ENTITIES.filter { report.counts[it]?.present == true }.forEach { entity ->
                    val count = report.counts.getValue(entity)
                    if (!first) FinioDivider()
                    first = false
                    PreviewRow(ENTITY_LABELS.getValue(entity)) {
                        Text(count.accepted.toString(), style = FinioType.bodyMedium, color = colors.foreground)
                        if (count.rejected > 0) {
                            Text(
                                "${count.rejected} skipped",
                                Modifier.padding(start = 8.dp),
                                style = FinioType.caption,
                                color = colors.destructive,
                            )
                        }
                    }
                }
                if (report.hasSettings) {
                    if (!first) FinioDivider()
                    PreviewRow("Settings") { Text("included", style = FinioType.bodyMedium, color = colors.foreground) }
                }
            }

            if (report.warnings.isNotEmpty() || report.issues.isNotEmpty()) {
                Column(
                    Modifier.fillMaxWidth().clip(FinioShapes.sm).background(colors.muted.mix(0.5f)).padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    report.warnings.forEach { warning ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(LucideIcons.AlertTriangle, null, Modifier.padding(top = 1.dp).size(14.dp), tint = colors.warning)
                            Text(warning, style = FinioType.caption, color = colors.foreground)
                        }
                    }
                    report.issues.forEach { issue -> Text(issue, style = FinioType.caption, color = colors.mutedForeground) }
                }
            }

            if (!importable) Text("Nothing valid to import", style = FinioType.label, color = colors.destructive)

            Text("Balances are recalculated from transactions after either option.", style = FinioType.caption, color = colors.mutedForeground)

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FinioButton(
                    "Merge with existing data",
                    onClick = { onImport(ImportMode.Merge) },
                    enabled = importable,
                    size = ButtonSize.Lg,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FinioButton(
                        "Replace everything",
                        onClick = { onImport(ImportMode.Replace) },
                        enabled = importable,
                        variant = ButtonVariant.Destructive,
                        size = ButtonSize.Lg,
                        modifier = Modifier.weight(1f),
                    )
                    FinioButton("Cancel", onClick = onDismiss, variant = ButtonVariant.Secondary, size = ButtonSize.Lg)
                }
            }
        }
    }
}

@Composable
private fun PreviewRow(label: String, value: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = FinioType.body, color = FinioTheme.colors.mutedForeground)
        Row(verticalAlignment = Alignment.CenterVertically) { value() }
    }
}
