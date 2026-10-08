package com.slowatcoding.finio.di

import android.app.Application
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import com.slowatcoding.finio.core.backup.collectBackupPayload
import com.slowatcoding.finio.core.format.formatCurrency
import com.slowatcoding.finio.core.js.nowInstant
import com.slowatcoding.finio.core.model.AccountType
import com.slowatcoding.finio.core.model.FinanceState
import com.slowatcoding.finio.core.model.TransactionType
import com.slowatcoding.finio.core.notify.NotificationPrefs
import com.slowatcoding.finio.core.notify.NotificationScheduleInput
import com.slowatcoding.finio.core.notify.buildNotificationSchedule
import com.slowatcoding.finio.core.period.normalizeMonthStartDay
import com.slowatcoding.finio.core.store.AppLockState
import com.slowatcoding.finio.core.store.AuthState
import com.slowatcoding.finio.core.store.BackupCryptoState
import com.slowatcoding.finio.core.store.FinanceStore
import com.slowatcoding.finio.core.store.PersistedAppLock
import com.slowatcoding.finio.core.store.PersistedAuth
import com.slowatcoding.finio.core.store.PersistedBackupCrypto
import com.slowatcoding.finio.core.store.decodePersisted
import com.slowatcoding.finio.core.update.ReleaseInfo
import com.slowatcoding.finio.core.update.shouldPromptUpdate
import com.slowatcoding.finio.core.store.encodePersisted
import com.slowatcoding.finio.platform.api.FinioApi
import com.slowatcoding.finio.platform.backup.AutoBackup
import com.slowatcoding.finio.platform.backup.AutoBackupScheduler
import com.slowatcoding.finio.platform.backup.BackupDataSource
import com.slowatcoding.finio.platform.backup.CloudBackupSession
import com.slowatcoding.finio.platform.backup.CloudEncryption
import com.slowatcoding.finio.platform.lock.AppLifecycleWatcher
import com.slowatcoding.finio.platform.lock.BackgroundedAtStore
import com.slowatcoding.finio.platform.notify.NotificationPermission
import com.slowatcoding.finio.platform.notify.NotificationScheduler
import com.slowatcoding.finio.platform.notify.NotificationStore
import com.slowatcoding.finio.platform.share.LaunchTarget
import com.slowatcoding.finio.platform.update.SkippedUpdateStore
import com.slowatcoding.finio.platform.update.UpdateChecker
import com.slowatcoding.finio.platform.storage.DebouncedJsonWriter
import com.slowatcoding.finio.platform.storage.JsonFileStore
import com.slowatcoding.finio.platform.storage.PlatformJson
import com.slowatcoding.finio.platform.storage.StoreFiles
import com.slowatcoding.finio.BuildConfig
import com.slowatcoding.finio.ui.components.ToastAction
import com.slowatcoding.finio.ui.components.toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Manual DI: the one object that owns every store and platform service for the process. Built
 * once by [com.slowatcoding.finio.FinioApp] and reached from Compose through [LocalAppContainer]
 * / [appContainer] — never construct one anywhere else.
 *
 * It also owns the process-level behaviour the web spreads across `App.tsx`, `Layout.tsx` and
 * `useAutoLock`:
 *
 *  - **Hydration.** The lock, auth and backup-crypto files are tiny and read synchronously in
 *    [start] so the lock decision lands before the first frame (web `onRehydrateStorage`). The
 *    finance store is decoded on IO; [FinanceStore.isHydrated] flips when it lands, and the
 *    hydration gate shows the page loader until then.
 *  - **Persistence.** Every store change is written through a [DebouncedJsonWriter] (finance:
 *    300ms; the lock: immediately, so a failed-attempt count can't be reset by killing the app),
 *    and every writer is flushed synchronously when the app goes to the background.
 *  - **Auto-lock** ([onBackground]/[onForeground]) — the port of `useAutoLock`.
 *  - **The foreground pass** — `Layout.tsx`'s hydration effects, in the same order:
 *    processRecurring (Undo toast) → processMaturities (toast) → captureNetWorthSnapshots →
 *    reminder schedule → auto backups (cloud + local). It runs on cold start AND on every return
 *    from the background, but only once the gates have lifted (hydrated, unlocked, onboarded) —
 *    on the web it lives in `Layout`, which only mounts behind those gates, and running it under
 *    the lock screen would put an amount-bearing toast over it.
 *  - **Launch targets** — a share, shortcut or notification click is parked in
 *    [pendingLaunch] until the app shell (rendered only once the gates lift) consumes it.
 */
class AppContainer(private val app: Application) {

    /** Main-thread scope for the container's own work; lives as long as the process. */
    val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val dir = app.filesDir
    private val mainHandler = Handler(Looper.getMainLooper())

    // ---- Files + writers --------------------------------------------------------------------

    private val financeFile = JsonFileStore(dir, StoreFiles.FINANCE)
    private val lockFile = JsonFileStore(dir, StoreFiles.LOCK)
    private val authFile = JsonFileStore(dir, StoreFiles.AUTH)
    private val backupCryptoFile = JsonFileStore(dir, StoreFiles.BACKUP_CRYPTO)

    private val financeWriter = DebouncedJsonWriter(financeFile, scope)
    private val lockWriter = DebouncedJsonWriter(lockFile, scope, delayMs = 0)
    private val authWriter = DebouncedJsonWriter(authFile, scope, delayMs = 0)
    private val backupCryptoWriter = DebouncedJsonWriter(backupCryptoFile, scope, delayMs = 0)
    private val writers = listOf(financeWriter, lockWriter, authWriter, backupCryptoWriter)

    /** `finio-lock-bg` — when the app was last backgrounded (see [onBackground]). */
    val backgroundedAt = BackgroundedAtStore(dir)

    // ---- Stores -----------------------------------------------------------------------------

    /** All finance data and actions (web `useFinanceStore`). */
    val financeStore = FinanceStore()

    /** The app lock (web `useAppLockStore`) — never part of a backup. */
    val appLock = AppLockState(
        persist = { p -> lockWriter.schedule { PlatformJson.encodeToString(PersistedAppLock.serializer(), p) } },
        onLock = {
            // Toasts render above the lock gate; an "Added ₹250" fired just before backgrounding
            // must not sit on the lock screen. And no stale grace window may survive a lock.
            runOnMain { toast.dismissAll() }
            backgroundedAt.clear()
        },
    )

    /** Cloud account session (web `useAuthStore`). */
    val auth = AuthState(
        persist = { p -> authWriter.schedule { PlatformJson.encodeToString(PersistedAuth.serializer(), p) } },
    )

    /** Backup passphrase config + in-memory session key (web `useBackupCryptoStore`). */
    val backupCrypto = BackupCryptoState(
        persist = { p -> backupCryptoWriter.schedule { PlatformJson.encodeToString(PersistedBackupCrypto.serializer(), p) } },
    )

    /** The optional PHP backend. */
    val api = FinioApi()

    // ---- Launch targets ---------------------------------------------------------------------

    private val _pendingLaunch = MutableStateFlow<LaunchTarget?>(null)

    /** A share / shortcut / notification target waiting for the gates to lift. */
    val pendingLaunch: StateFlow<LaunchTarget?> = _pendingLaunch.asStateFlow()

    /** Park [target] (MainActivity: onCreate with no saved state, and onNewIntent). */
    fun offerLaunch(target: LaunchTarget) {
        _pendingLaunch.value = target
    }

    /** Take the parked target, if it is still [target] (the shell, once it has navigated). */
    fun consumeLaunch(target: LaunchTarget) {
        _pendingLaunch.compareAndSet(target, null)
    }

    // ---- Startup ----------------------------------------------------------------------------

    private var started = false

    /** Hydrate every store and install the lifecycle hooks. Main thread, from `Application.onCreate`. */
    fun start() {
        if (started) return
        started = true

        // Small files, read synchronously: the lock decision must be made before the first frame.
        auth.hydrate(authFile.read(PersistedAuth.serializer()))
        backupCrypto.hydrate(backupCryptoFile.read(PersistedBackupCrypto.serializer()))
        appLock.hydrate(
            persisted = lockFile.read(PersistedAppLock.serializer()),
            backgroundedAt = backgroundedAt.read(),
            now = System.currentTimeMillis(),
        )

        scope.launch {
            val text = withContext(Dispatchers.IO) { financeFile.readText() }
            val state = if (text != null) withContext(Dispatchers.Default) { decodePersisted(text) } else null
            if (state != null) financeStore.replaceState(state)
            financeStore.setHydrated(true)
            // Persist every change from here on (the hydrated value itself is skipped).
            financeStore.state.drop(1).collect {
                financeWriter.schedule { encodePersisted(financeStore.current) }
            }
        }

        AppLifecycleWatcher(onBackground = ::onBackground, onForeground = ::onForeground).install()
        watchGatesForForegroundPass()
    }

    /** Suspend until the finance store has hydrated (workers may start before it has). */
    suspend fun awaitHydrated() {
        financeStore.isHydrated.first { it }
    }

    // ---- Lifecycle (web useAutoLock + Layout's visibility listener) -------------------------

    private fun onBackground() {
        val lockState = appLock.current
        val config = lockState.config
        if (config?.enabled != true) {
            backgroundedAt.clear()
        } else if (lockState.isLocked) {
            // A locked app stays locked across a restart: never hand it a fresh grace window.
            backgroundedAt.clear()
        } else {
            backgroundedAt.write(System.currentTimeMillis())
            if (config.autoLockMinutes <= 0) appLock.lock()
        }
        writers.forEach { it.flushBlocking() }
    }

    private fun onForeground() {
        val lockState = appLock.current
        val config = lockState.config
        if (config?.enabled == true && !lockState.isLocked &&
            com.slowatcoding.finio.core.applock.shouldLockOnResume(
                backgroundedAt.read(),
                config.autoLockMinutes,
                System.currentTimeMillis(),
            )
        ) {
            appLock.lock()
        }
        foregroundPassPending.value = true
    }

    // ---- The foreground pass (web Layout.tsx effects) ---------------------------------------

    /** True from process start and after every return to the foreground, until the pass runs. */
    private val foregroundPassPending = MutableStateFlow(true)

    private fun watchGatesForForegroundPass() {
        scope.launch {
            combine(
                financeStore.isHydrated,
                appLock.state.map { it.isReady && !it.isLocked }.distinctUntilChanged(),
                financeStore.state.map { it.settings.onboardedAt != null }.distinctUntilChanged(),
                foregroundPassPending,
            ) { hydrated, unlocked, onboarded, pending -> hydrated && unlocked && onboarded && pending }
                .filter { it }
                .collect {
                    foregroundPassPending.value = false
                    runForegroundPass()
                }
        }
    }

    private fun runForegroundPass() {
        // Recurring rules post silently; the Undo toast doubles as the "recurring inbox".
        val generated = financeStore.processRecurring()
        if (generated.isNotEmpty()) {
            val ids = generated.map { it.id }
            val n = generated.size
            toast.success(
                "Added $n recurring transaction${if (n == 1) "" else "s"}",
                action = ToastAction("Undo") { financeStore.bulkDeleteTransactions(ids) },
            )
        }

        // Deposits that reached maturity. After recurring (an RD's last installment first) and
        // before snapshots (the month counts the money where it landed). No Undo: it's a fact.
        val posted = financeStore.processMaturities()
        if (posted.isNotEmpty()) {
            val s = financeStore.current
            for (tx in posted) {
                if (tx.type != TransactionType.Transfer) continue
                val deposit = s.accounts.find { it.id == tx.accountId }
                val target = s.accounts.find { it.id == tx.toAccountId }
                val kind = if (deposit?.type == AccountType.Rd) "RD" else "FD"
                toast.success(
                    "$kind \"${deposit?.name ?: ""}\" matured — " +
                        "${formatCurrency(tx.amount, compact = false, hidden = s.settings.hideAmounts)} credited to " +
                        (target?.name ?: "its account"),
                )
            }
        }

        // Freeze any financial month that closed since the last visit. Silent.
        financeStore.captureNetWorthSnapshots()

        scope.launch { refreshReminders() }

        checkForUpdate()

        AutoBackupScheduler.ensureScheduled(app)
        scope.launch(Dispatchers.IO) {
            runCatching { AutoBackup.autoBackupIfNeeded(backupDataSource, cloudBackupSession, api) }
            runCatching { AutoBackup.autoLocalBackupIfNeeded(app, backupDataSource) }
        }
    }

    // ---- App updates ------------------------------------------------------------------------

    private val updateChecker = UpdateChecker()
    private val skippedUpdate = SkippedUpdateStore(app)
    private var updateChecked = false

    private val _availableUpdate = MutableStateFlow<ReleaseInfo?>(null)

    /** A newer, not-skipped release to offer; the dialog shows while this is non-null. */
    val availableUpdate: StateFlow<ReleaseInfo?> = _availableUpdate.asStateFlow()

    /** Once per process (every app open), after the gates lift — never over the lock screen. */
    private fun checkForUpdate() {
        if (updateChecked) return
        updateChecked = true
        scope.launch {
            val release = updateChecker.fetchLatest() ?: return@launch
            if (shouldPromptUpdate(release, BuildConfig.VERSION_NAME, skippedUpdate.get())) {
                _availableUpdate.value = release
            }
        }
    }

    /** "Not now": hide the dialog; the next app open asks again. */
    fun dismissUpdate() {
        _availableUpdate.value = null
    }

    /** "Skip this version": hide it and stay quiet until a newer release is published. */
    fun skipUpdate(release: ReleaseInfo) {
        skippedUpdate.set(release.version)
        _availableUpdate.value = null
    }

    // ---- Reminders (web services/notifications.ts) ------------------------------------------

    /**
     * Rebuild the reminder schedule from the current data and show anything already due —
     * `refreshNotificationSchedule()`. Call it after a reminder preference changes too. Without
     * permission (or with reminders off) it writes an empty schedule, keeping the fired ledger,
     * exactly like the web; use [disableReminders] when the user turns reminders off.
     */
    suspend fun refreshReminders() {
        runCatching {
            val state = financeStore.current
            if (!NotificationPermission.canNotify(app, state.settings.notificationsEnabled)) {
                withContext(Dispatchers.IO) {
                    NotificationStore.forContext(app).writeSchedule(emptyList())
                    NotificationScheduler.scheduleNext(app)
                }
                return
            }
            val schedule = withContext(Dispatchers.Default) {
                buildNotificationSchedule(reminderInput(state), nowInstant())
            }
            NotificationScheduler.publish(app, schedule)
        }
    }

    /** Reminders turned off: wipe the schedule + ledger and cancel the workers (`teardownNotifications`). */
    suspend fun disableReminders() {
        runCatching { NotificationScheduler.disable(app) }
    }

    private fun reminderInput(state: FinanceState) = NotificationScheduleInput(
        recurring = state.recurring,
        budgets = state.budgets,
        transactions = state.transactions,
        accounts = state.accounts,
        categories = state.categories,
        labels = state.labels,
        monthStartDay = normalizeMonthStartDay(state.settings.monthStartDay),
        prefs = NotificationPrefs(
            notificationsEnabled = state.settings.notificationsEnabled,
            notifyBills = state.settings.notifyBills,
            notifyBudgets = state.settings.notifyBudgets,
            notifyCreditDue = state.settings.notifyCreditDue,
            notifyLeadDays = state.settings.notifyLeadDays,
            notifyDailyLog = state.settings.notifyDailyLog,
            hideAmounts = state.settings.hideAmounts,
        ),
    )

    // ---- Backup seams (workers + "Back up now") ---------------------------------------------

    /** The finance store, as the backup code reads it. Waits for hydration. */
    val backupDataSource: BackupDataSource = object : BackupDataSource {
        override suspend fun hasBackupWorthyData(): Boolean {
            awaitHydrated()
            val s = financeStore.current
            return s.accounts.isNotEmpty() || s.transactions.isNotEmpty() || s.budgets.isNotEmpty() ||
                s.recurring.isNotEmpty() || s.goals.isNotEmpty() || s.people.isNotEmpty()
        }

        override suspend fun payload() = awaitHydrated().let { collectBackupPayload(financeStore.current) }

        override suspend fun autoLocalBackupEnabled(): Boolean {
            awaitHydrated()
            return financeStore.current.settings.autoLocalBackup
        }

        override suspend fun lastLocalBackupAt(): String? {
            awaitHydrated()
            return financeStore.current.lastLocalBackupAt
        }

        override suspend fun setLastLocalBackupAt(dayKey: String) {
            awaitHydrated()
            financeStore.setLastLocalBackupAt(dayKey)
        }
    }

    /** The auth + backup-crypto slice the cloud upload reads. */
    val cloudBackupSession: CloudBackupSession = object : CloudBackupSession {
        override suspend fun token(): String? = auth.current.token?.takeIf { it.isNotEmpty() }

        override suspend fun lastBackupAt(): String? = auth.current.lastBackupAt

        override suspend fun setLastBackupAt(iso: String) = auth.setLastBackupAt(iso)

        override suspend fun encryption(): CloudEncryption {
            val config = backupCrypto.current.config
            if (config == null || !config.enabled) return CloudEncryption.Off
            val key = backupCrypto.cachedKey(config.salt) ?: return CloudEncryption.Locked
            return CloudEncryption.Ready(key, config.salt, config.iterations)
        }
    }

    private fun runOnMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) block() else mainHandler.post(block)
    }
}

/** The process's [AppContainer]. Provided once, at the root of the composition. */
val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("LocalAppContainer used outside FinioRoot")
}

/** Shorthand for `LocalAppContainer.current`. */
@Composable
@ReadOnlyComposable
fun appContainer(): AppContainer = LocalAppContainer.current
