package com.slowatcoding.finio.platform.storage

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/*
 * The Android twin of the web's localStorage keys. Every persisted blob — the finance store
 * (`finio-storage`), the lock config (`finio-lock`), the backup-crypto config, the auth session,
 * the lock's background timestamp and the reminder schedule/ledger — is one JSON file in
 * `filesDir`, written atomically:
 *
 *   1. write `<name>.tmp` and fsync it,
 *   2. move the current (known-good) file to `<name>.bak`,
 *   3. atomically rename `<name>.tmp` → `<name>`.
 *
 * A crash at any step leaves either the new file, or no main file plus a good `.bak` — never a
 * half-written main file. Reads validate the JSON and fall back to `.bak` when the main file is
 * missing or corrupt.
 *
 * Deliberately free of `Context` (beyond the [forContext] factory) so the whole class runs in
 * plain JVM unit tests.
 */

/** File names — one per web localStorage / IndexedDB key. */
object StoreFiles {
    const val FINANCE = "finio-storage.json"
    const val LOCK = "finio-lock.json"
    const val BACKUP_CRYPTO = "finio-backup-crypto.json"
    const val AUTH = "finio-auth.json"
    const val LOCK_BACKGROUNDED_AT = "finio-lock-bg.json"
    const val NOTIFY_SCHEDULE = "finio-notify-schedule.json"
    const val NOTIFY_FIRED = "finio-notify-fired.json"
}

/** Shared lenient-in, compact-out JSON configuration for platform files. */
val PlatformJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    explicitNulls = false
}

class JsonFileStore(dir: File, val name: String) {
    val file: File = File(dir, name)
    val backupFile: File = File(dir, "$name.bak")
    private val tempFile: File = File(dir, "$name.tmp")
    private val lock = Any()

    /**
     * Whether the main file on disk is one we wrote or successfully read. Only a known-good main
     * file is rotated into `.bak` — rotating a corrupt one would destroy the fallback.
     */
    @Volatile
    private var mainKnownGood: Boolean? = null

    init {
        dir.mkdirs()
    }

    /** True when either the main file or its backup exists. */
    fun exists(): Boolean = file.exists() || backupFile.exists()

    /**
     * The last good raw JSON, or null when nothing (valid) is stored. [isValid] defaults to "is
     * syntactically JSON"; pass a stricter check (e.g. a decode attempt) to treat a file that
     * parses but doesn't decode as corrupt too.
     */
    fun readText(isValid: (String) -> Boolean = ::isJson): String? = synchronized(lock) {
        val main = file.readTextOrNull()
        if (main != null && isValid(main)) {
            mainKnownGood = true
            return main
        }
        mainKnownGood = false
        val bak = backupFile.readTextOrNull()
        if (bak != null && isValid(bak)) bak else null
    }

    /** Decode with [serializer]; a main file that fails to decode falls back to `.bak`. */
    fun <T> read(serializer: KSerializer<T>, json: Json = PlatformJson): T? {
        var decoded: T? = null
        val text = readText { raw ->
            runCatching { json.decodeFromString(serializer, raw) }
                .onSuccess { decoded = it }
                .isSuccess
        } ?: return null
        return decoded ?: runCatching { json.decodeFromString(serializer, text) }.getOrNull()
    }

    /** Atomic, durable write. Blocking — call from an IO thread (or use [DebouncedJsonWriter]). */
    @Throws(IOException::class)
    fun writeText(text: String): Unit = synchronized(lock) {
        FileOutputStream(tempFile).use { out ->
            out.write(text.toByteArray(Charsets.UTF_8))
            out.flush()
            out.fd.sync()
        }
        val good = mainKnownGood ?: (file.readTextOrNull()?.let(::isJson) == true)
        if (file.exists() && good) {
            move(file, backupFile, atomic = false)
        }
        move(tempFile, file, atomic = true)
        mainKnownGood = true
    }

    fun <T> write(serializer: KSerializer<T>, value: T, json: Json = PlatformJson) =
        writeText(json.encodeToString(serializer, value))

    /** Remove the main file and its backup (e.g. sign-out wipes `finio-auth`). */
    fun delete(): Unit = synchronized(lock) {
        file.delete()
        backupFile.delete()
        tempFile.delete()
        mainKnownGood = null
    }

    private fun move(from: File, to: File, atomic: Boolean) {
        try {
            if (atomic) {
                Files.move(
                    from.toPath(), to.toPath(),
                    StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING,
                )
            } else {
                Files.move(from.toPath(), to.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(from.toPath(), to.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    companion object {
        /** A store in the app's private `filesDir`. */
        fun forContext(context: Context, name: String): JsonFileStore =
            JsonFileStore(context.applicationContext.filesDir, name)

        fun isJson(raw: String): Boolean =
            raw.isNotBlank() && runCatching { PlatformJson.parseToJsonElement(raw) }.isSuccess

        private fun File.readTextOrNull(): String? =
            if (isFile) runCatching { readText(Charsets.UTF_8) }.getOrNull() else null
    }
}

/**
 * Coalesces bursts of state changes into one write ~[delayMs] after the last change — the
 * Zustand `persist` middleware writes on every `set`, which on Android would mean a full fsync per
 * keystroke. The producer is evaluated lazily at write time, so only the latest state is ever
 * serialized.
 *
 * Call [flushBlocking] from the process ON_STOP (see `AppLifecycleWatcher`) so a pending write
 * survives the app being backgrounded and killed.
 */
class DebouncedJsonWriter(
    private val store: JsonFileStore,
    private val scope: CoroutineScope,
    private val delayMs: Long = DEFAULT_DEBOUNCE_MS,
    private val onError: (Throwable) -> Unit = {},
) {
    private val stateLock = Any()
    private var pending: (() -> String)? = null
    private var job: Job? = null
    private val writeMutex = Mutex()

    /** Queue a write of whatever [producer] returns when the debounce window closes. */
    fun schedule(producer: () -> String) {
        synchronized(stateLock) {
            pending = producer
            job?.cancel()
            job = scope.launch(Dispatchers.IO) {
                delay(delayMs)
                writePending()
            }
        }
    }

    /** Write any pending change now (suspending). */
    suspend fun flush() {
        synchronized(stateLock) { job?.cancel(); job = null }
        withContext(Dispatchers.IO) { writePending() }
    }

    /** Write any pending change now, blocking the caller. For lifecycle callbacks. */
    fun flushBlocking() {
        if (!hasPending()) return
        runBlocking { flush() }
    }

    fun hasPending(): Boolean = synchronized(stateLock) { pending != null }

    private suspend fun writePending() {
        writeMutex.withLock {
            val producer = synchronized(stateLock) { pending.also { pending = null } } ?: return
            try {
                store.writeText(producer())
            } catch (t: Throwable) {
                onError(t)
            }
        }
    }

    companion object {
        const val DEFAULT_DEBOUNCE_MS = 300L
    }
}
