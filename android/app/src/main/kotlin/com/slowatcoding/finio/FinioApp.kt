package com.slowatcoding.finio

import android.app.Application
import com.slowatcoding.finio.di.AppContainer
import com.slowatcoding.finio.platform.backup.AutoBackupHost
import com.slowatcoding.finio.platform.backup.BackupDataSource
import com.slowatcoding.finio.platform.backup.CloudBackupSession

/**
 * The process. Builds the one [AppContainer] (manual DI), hydrates every store from its JSON file
 * and installs the lifecycle hooks before any activity or worker runs.
 *
 * Implements [AutoBackupHost] because WorkManager instantiates the backup workers itself — this is
 * the one place they can reach the live stores from. The sources wait for hydration internally,
 * so a worker that starts the process in the background still sees the real data.
 */
class FinioApp : Application(), AutoBackupHost {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.start()
    }

    override fun backupDataSource(): BackupDataSource? =
        if (::container.isInitialized) container.backupDataSource else null

    override fun cloudBackupSession(): CloudBackupSession? =
        if (::container.isInitialized) container.cloudBackupSession else null
}
