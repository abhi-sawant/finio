package com.slowatcoding.finio.platform.files

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/*
 * Port of web/src/services/backupFolder.ts. The web keeps a File System Access directory handle
 * in IndexedDB and re-checks its permission; Android keeps an ACTION_OPEN_DOCUMENT_TREE URI in
 * SharedPreferences and holds a *persistable* read/write grant on it. "Has permission" is simply
 * whether that grant is still in `persistedUriPermissions` — there is no silent re-prompt, which
 * is exactly the web's `{ prompt: false }` path for background backups.
 */
class BackupFolder(context: Context) {
    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** The saved tree URI, whether or not its grant is still valid. */
    val savedUri: Uri? get() = prefs.getString(KEY_TREE_URI, null)?.let(Uri::parse)

    /** Saved and still writable — the web's `hasWritePermission(handle, { prompt: false })`. */
    fun isGranted(): Boolean {
        val uri = savedUri ?: return false
        return app.contentResolver.persistedUriPermissions.any {
            it.uri == uri && it.isWritePermission && it.isReadPermission
        }
    }

    /** Human-readable folder name for Settings ("Backups in: Documents/Finio"). */
    fun displayName(): String? = savedUri?.let { DocumentFile.fromTreeUri(app, it)?.name }

    /** Persist the grant for a tree the user just picked (the web's `chooseBackupFolder`). */
    fun save(treeUri: Uri) {
        savedUri?.takeIf { it != treeUri }?.let(::releaseGrant)
        app.contentResolver.takePersistableUriPermission(treeUri, GRANT_FLAGS)
        prefs.edit { putString(KEY_TREE_URI, treeUri.toString()) }
    }

    /** Forget the folder and release the grant (`clearBackupFolder`). */
    fun clear() {
        savedUri?.let(::releaseGrant)
        prefs.edit { remove(KEY_TREE_URI) }
    }

    /**
     * Write [contents] as [filename] (overwriting a same-named file) and delete Finio backups
     * beyond the newest [keep]. Throws [IOException] if the folder is gone or not writable.
     */
    suspend fun writeBackupAndRotate(
        filename: String,
        contents: String,
        keep: Int = MAX_LOCAL_BACKUPS,
    ): Unit = withContext(Dispatchers.IO) {
        val uri = savedUri ?: throw IOException("No backup folder chosen")
        if (!isGranted()) throw IOException("Backup folder permission was revoked")
        val tree = DocumentFile.fromTreeUri(app, uri)
            ?.takeIf { it.isDirectory && it.canWrite() }
            ?: throw IOException("Backup folder is unavailable")

        val target = tree.findFile(filename)?.takeIf { it.isFile }
            ?: tree.createFile(JSON_MIME, filename)
            ?: throw IOException("Could not create $filename")
        // "wt" truncates — a shorter backup must not leave the tail of a longer one behind.
        app.contentResolver.openOutputStream(target.uri, "wt")?.use { out ->
            out.write(contents.toByteArray(Charsets.UTF_8))
            out.flush()
        } ?: throw IOException("Could not open $filename for writing")

        val existing = tree.listFiles().filter { it.isFile }.mapNotNull { file -> file.name?.let { it to file } }
        val stale = selectStaleBackups(existing.map { it.first }, keep).toSet()
        for ((name, file) in existing) {
            if (name in stale) runCatching { DocumentsContract.deleteDocument(app.contentResolver, file.uri) }
        }
    }

    private fun releaseGrant(uri: Uri) {
        runCatching { app.contentResolver.releasePersistableUriPermission(uri, GRANT_FLAGS) }
    }

    companion object {
        private const val PREFS = "finio-backup-folder"
        private const val KEY_TREE_URI = "treeUri"
        private const val JSON_MIME = "application/json"
        private const val GRANT_FLAGS =
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
    }
}

/**
 * Compose: a launcher for the folder picker. On a pick, the grant is persisted via
 * [BackupFolder.save] before [onResult] runs; null means the user backed out.
 */
@Composable
fun rememberBackupFolderPicker(onResult: (Uri?) -> Unit): () -> Unit {
    val context = LocalContext.current
    val callback = rememberUpdatedState(onResult)
    val launcher = rememberLauncherForActivityResult(PersistableOpenDocumentTree()) { uri ->
        if (uri != null) {
            val saved = runCatching { BackupFolder(context).save(uri) }.isSuccess
            callback.value(if (saved) uri else null)
        } else {
            callback.value(null)
        }
    }
    val folder = remember(context) { BackupFolder(context) }
    return { launcher.launch(folder.savedUri) }
}

/** OpenDocumentTree that asks for a persistable read+write grant. */
private class PersistableOpenDocumentTree : ActivityResultContracts.OpenDocumentTree() {
    override fun createIntent(context: Context, input: Uri?): Intent =
        super.createIntent(context, input).addFlags(
            Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                Intent.FLAG_GRANT_PREFIX_URI_PERMISSION,
        )
}
