package com.slowatcoding.finio.platform.backup

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.slowatcoding.finio.platform.api.FinioApi
import java.util.concurrent.TimeUnit

/**
 * Daily local backup into the granted SAF folder. Runs every 12h so one run lands on each
 * calendar day; the day-key check makes the extra run a no-op.
 */
class LocalAutoBackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val host = applicationContext as? AutoBackupHost ?: return Result.success()
        val source = host.backupDataSource() ?: return Result.success()
        AutoBackup.autoLocalBackupIfNeeded(applicationContext, source)
        return Result.success()
    }
}

/**
 * Cloud backup at most once per 24h. Network-constrained; failures are best-effort (the next
 * periodic run, or the next app start, tries again).
 */
class CloudAutoBackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val host = applicationContext as? AutoBackupHost ?: return Result.success()
        val source = host.backupDataSource() ?: return Result.success()
        val session = host.cloudBackupSession() ?: return Result.success()
        runCatching { AutoBackup.autoBackupIfNeeded(source, session, FinioApi()) }
        return Result.success()
    }
}

object AutoBackupScheduler {
    const val LOCAL_WORK_NAME = "finio-auto-backup-local"
    const val CLOUD_WORK_NAME = "finio-auto-backup-cloud"

    /** Enqueue both periodic workers (KEEP — calling on every start doesn't reset them). */
    fun ensureScheduled(context: Context) {
        val wm = WorkManager.getInstance(context.applicationContext)
        wm.enqueueUniquePeriodicWork(
            LOCAL_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<LocalAutoBackupWorker>(12, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiresStorageNotLow(true).build())
                .build(),
        )
        wm.enqueueUniquePeriodicWork(
            CLOUD_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<CloudAutoBackupWorker>(6, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build(),
        )
    }

    fun cancel(context: Context) {
        val wm = WorkManager.getInstance(context.applicationContext)
        wm.cancelUniqueWork(LOCAL_WORK_NAME)
        wm.cancelUniqueWork(CLOUD_WORK_NAME)
    }
}
