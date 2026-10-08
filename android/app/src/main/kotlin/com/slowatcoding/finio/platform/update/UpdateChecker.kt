package com.slowatcoding.finio.platform.update

import android.content.Context
import com.slowatcoding.finio.core.update.ReleaseInfo
import com.slowatcoding.finio.core.update.parseLatestRelease
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Asks GitHub for the latest published release. Unauthenticated (60 requests/hour/IP, and we make
 * one per cold start). Every failure — offline, rate-limited, no releases yet, a draft — is `null`:
 * an update check must never surface an error or slow the app down.
 */
class UpdateChecker(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build(),
) {
    suspend fun fetchLatest(): ReleaseInfo? = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url(LATEST_RELEASE_URL)
                .header("Accept", "application/vnd.github+json")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) null else parseLatestRelease(response.body.string())
            }
        }.getOrNull()
    }

    companion object {
        const val LATEST_RELEASE_URL = "https://api.github.com/repos/abhi-sawant/finio/releases/latest"
    }
}

/**
 * The version the user chose to "Skip". Device-local on purpose, in its own SharedPreferences file
 * rather than `FinanceState` — it is not finance data and must never travel in a backup.
 */
class SkippedUpdateStore(context: Context) {
    private val prefs = context.getSharedPreferences("finio-update", Context.MODE_PRIVATE)

    fun get(): String? = prefs.getString(KEY, null)

    fun set(version: String) {
        prefs.edit().putString(KEY, version).apply()
    }

    private companion object {
        const val KEY = "skippedVersion"
    }
}
