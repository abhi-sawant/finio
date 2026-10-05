package com.slowatcoding.finio.platform.files

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.slowatcoding.finio.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Hand an export (backup JSON, transactions CSV) to the share sheet — Drive, mail, a chat —
 * through FileProvider. Files go to `cacheDir/exports/`, the only directory `file_paths.xml`
 * exposes, and the directory is emptied first so earlier exports never linger in the cache.
 */
object ShareExport {
    fun authority(context: Context) = "${context.packageName}.fileprovider"

    /** Build the chooser intent; start it with `context.startActivity(...)`. */
    suspend fun shareIntent(
        context: Context,
        filename: String,
        content: String,
        mimeType: String,
    ): Intent = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "exports")
        dir.deleteRecursively()
        dir.mkdirs()
        // The name is user-visible in the receiving app; keep it a plain file name.
        val safeName = filename.replace(Regex("""[\\/:*?"<>|]"""), "_")
        val file = File(dir, safeName)
        file.writeText(content, Charsets.UTF_8)
        val uri = FileProvider.getUriForFile(context, authority(context), file)
        val send = Intent(Intent.ACTION_SEND)
            .setType(mimeType)
            .putExtra(Intent.EXTRA_STREAM, uri)
            .putExtra(Intent.EXTRA_SUBJECT, safeName)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        send.clipData = ClipData.newRawUri(safeName, uri)
        Intent.createChooser(send, context.getString(R.string.share_export_chooser))
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    suspend fun share(context: Context, filename: String, content: String, mimeType: String) {
        val intent = shareIntent(context, filename, content, mimeType)
        if (context !is android.app.Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        withContext(Dispatchers.Main) { context.startActivity(intent) }
    }
}
