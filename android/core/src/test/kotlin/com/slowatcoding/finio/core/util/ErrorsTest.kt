package com.slowatcoding.finio.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException
import kotlin.coroutines.cancellation.CancellationException

// Port of web/src/utils/errors.test.ts, adapted: fetch's TypeError ↔ an IOException.
class ErrorsTest {
    @Test fun returnsTheMessage() = assertEquals("boom", getErrorMessage(IllegalStateException("boom"), "fallback"))
    @Test fun fallsBackForNull() = assertEquals("fallback", getErrorMessage(null, "fallback"))
    @Test fun fallsBackForEmptyMessage() {
        assertEquals("fallback", getErrorMessage(RuntimeException(""), "fallback"))
        assertEquals("fallback", getErrorMessage(RuntimeException(), "fallback"))
    }

    @Test fun mapsConnectivityFailuresToFriendlyCopy() {
        for (e in listOf(UnknownHostException("api.finio"), ConnectException("refused"), SocketTimeoutException("timeout"),
            SSLHandshakeException("bad cert"), IOException("unexpected end of stream"))) {
            assertEquals(e.toString(), NETWORK_ERROR_MESSAGE, getErrorMessage(e, "fallback"))
        }
    }

    @Test fun mapsBrowserStyleMessagesOnAnyThrowable() {
        for (msg in listOf("Failed to fetch", "NetworkError when attempting to fetch resource.", "Load failed", "Network request failed", "failed to fetch")) {
            assertEquals(NETWORK_ERROR_MESSAGE, getErrorMessage(RuntimeException(msg), "fallback"))
        }
    }

    @Test fun keepsAnUnrelatedMessage() {
        assertEquals("x is undefined", getErrorMessage(RuntimeException("x is undefined"), "fallback"))
        // Only a message that *starts* like a network failure counts for a non-IO error.
        assertFalse(isNetworkError(RuntimeException("The network is fine, but the server said no")))
    }

    @Test fun abortIsNotANetworkError() {
        assertFalse(isNetworkError(CancellationException("aborted")))
        assertFalse(isNetworkError(InterruptedIOException("Canceled")))
        assertTrue(isNetworkError(SocketTimeoutException()))
    }

    @Test fun serverSideErrorIsNotANetworkError() = assertFalse(isNetworkError(IllegalArgumentException("Invalid credentials")))
}
