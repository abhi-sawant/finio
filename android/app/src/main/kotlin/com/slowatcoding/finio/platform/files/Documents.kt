package com.slowatcoding.finio.platform.files

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

/*
 * Storage Access Framework helpers — the Android replacement for the web's `downloadBlob()`
 * (export) and `<input type="file">` (import). Both are Compose-first: remember the handle in a
 * composable, call it from a click handler.
 */

/** MIME types used by exports/imports. */
object FinioMime {
    const val JSON = "application/json"
    const val CSV = "text/csv"

    /** Import pickers accept these for CSV — providers label .csv files inconsistently. */
    val CSV_IMPORT = arrayOf("text/csv", "text/comma-separated-values", "application/csv", "text/plain", "application/vnd.ms-excel")
    val JSON_IMPORT = arrayOf("application/json", "application/octet-stream", "text/plain")
}

/** Result of an export: the written document, or why it failed. Null `uri` = user cancelled. */
sealed interface ExportResult {
    data class Saved(val uri: Uri) : ExportResult
    data object Cancelled : ExportResult
    data class Failed(val error: Throwable) : ExportResult
}

/** Result of an import: file text plus its display name. */
sealed interface ImportResult {
    data class Loaded(val text: String, val displayName: String?) : ImportResult
    data object Cancelled : ImportResult
    data class Failed(val error: Throwable) : ImportResult
}

class DocumentExporter internal constructor(private val start: (String, () -> String) -> Unit) {
    /**
     * Ask where to save [suggestedName], then write [content] there. [content] is evaluated only
     * after the user picks a location (big backups aren't serialized for nothing).
     */
    fun export(suggestedName: String, content: () -> String) = start(suggestedName, content)
}

/** Compose: `ACTION_CREATE_DOCUMENT` for one [mimeType] (e.g. [FinioMime.JSON]). */
@Composable
fun rememberDocumentExporter(mimeType: String, onResult: (ExportResult) -> Unit): DocumentExporter {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val callback = rememberUpdatedState(onResult)
    val pending = remember { arrayOfNulls<() -> String>(1) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(mimeType)) { uri ->
        val producer = pending[0]
        pending[0] = null
        if (uri == null || producer == null) {
            callback.value(ExportResult.Cancelled)
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            val result = runCatching { writeText(context, uri, producer()) }
            callback.value(result.fold({ ExportResult.Saved(uri) }, { ExportResult.Failed(it) }))
        }
    }
    return remember(launcher) {
        DocumentExporter { name, content ->
            pending[0] = content
            launcher.launch(name)
        }
    }
}

/** Compose: `ACTION_OPEN_DOCUMENT` → file text. Call the returned function to open the picker. */
@Composable
fun rememberDocumentImporter(
    mimeTypes: Array<String>,
    onResult: (ImportResult) -> Unit,
): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val callback = rememberUpdatedState(onResult)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) {
            callback.value(ImportResult.Cancelled)
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            val result = runCatching { readText(context, uri) to displayName(context, uri) }
            callback.value(
                result.fold({ ImportResult.Loaded(it.first, it.second) }, { ImportResult.Failed(it) }),
            )
        }
    }
    return { launcher.launch(mimeTypes) }
}

/** Upper bound on an imported file — a backup is a few MB; anything bigger is a mistake. */
const val MAX_IMPORT_BYTES: Long = 50L * 1024 * 1024

suspend fun writeText(context: Context, uri: Uri, text: String) = withContext(Dispatchers.IO) {
    context.contentResolver.openOutputStream(uri, "wt")?.use { out ->
        out.write(text.toByteArray(Charsets.UTF_8))
        out.flush()
    } ?: throw IOException("Could not open the file for writing")
}

suspend fun readText(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
    context.contentResolver.openInputStream(uri)?.use { input ->
        val bytes = input.readNBytesCompat(MAX_IMPORT_BYTES + 1)
        if (bytes.size > MAX_IMPORT_BYTES) throw IOException("File is too large to import")
        // Strip a UTF-8 BOM (Excel-exported CSVs carry one).
        String(bytes, Charsets.UTF_8).removePrefix("\uFEFF")
    } ?: throw IOException("Could not open the file")
}

fun displayName(context: Context, uri: Uri): String? =
    runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    }.getOrNull()

private fun java.io.InputStream.readNBytesCompat(limit: Long): ByteArray {
    val out = java.io.ByteArrayOutputStream()
    val buf = ByteArray(64 * 1024)
    var total = 0L
    while (total < limit) {
        val n = read(buf, 0, minOf(buf.size.toLong(), limit - total).toInt())
        if (n < 0) break
        out.write(buf, 0, n)
        total += n
    }
    return out.toByteArray()
}
