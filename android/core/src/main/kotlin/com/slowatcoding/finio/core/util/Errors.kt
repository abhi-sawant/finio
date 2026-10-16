package com.slowatcoding.finio.core.util

import java.io.IOException
import java.io.InterruptedIOException
import java.net.SocketTimeoutException
import kotlin.coroutines.cancellation.CancellationException

// Port of web/src/utils/errors.ts, adapted to JVM exceptions.
//
// The web tells a "never reached the server" failure apart by fetch's bare `TypeError`. On the
// JVM the equivalent is an IOException thrown by the HTTP client before a response exists
// (UnknownHostException, ConnectException, SocketTimeoutException, SSLException…). The web's
// `AbortError` exclusion maps to coroutine cancellation and to a plain InterruptedIOException
// (an aborted call), neither of which is a connectivity problem.

/** Copy shown when a request never reached the server (offline, DNS, blocked). */
const val NETWORK_ERROR_MESSAGE = "Can't reach the Finio server. Check your connection and try again."

private val NETWORK_PREFIX =
    Regex("^(failed to fetch|networkerror|load failed|network request failed)", RegexOption.IGNORE_CASE)

/** True for a failure that means the request never got a response. */
fun isNetworkError(err: Throwable?): Boolean {
    if (err == null) return false
    if (err is CancellationException) return false
    if (err is InterruptedIOException && err !is SocketTimeoutException) return false
    if (err is IOException) return true
    // Not an IOException: only a message that *starts* like a network failure counts — the same
    // rule the web applies to an `Error` that isn't fetch's TypeError.
    val message = err.message ?: return false
    return NETWORK_PREFIX.containsMatchIn(message)
}

/**
 * Narrows a caught error into a user-facing message, falling back when it has no message.
 * Raw network failures become [NETWORK_ERROR_MESSAGE].
 */
fun getErrorMessage(err: Throwable?, fallback: String): String {
    if (isNetworkError(err)) return NETWORK_ERROR_MESSAGE
    val message = err?.message
    return if (!message.isNullOrEmpty()) message else fallback
}
