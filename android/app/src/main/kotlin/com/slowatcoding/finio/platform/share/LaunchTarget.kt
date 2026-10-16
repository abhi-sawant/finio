package com.slowatcoding.finio.platform.share

import com.slowatcoding.finio.core.share.SharedTransactionDraft
import com.slowatcoding.finio.core.share.parseSharePayload
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder

/*
 * Everything that can launch Finio "at" something — the share sheet (ACTION_SEND, the web's
 * `/share-target`), launcher shortcuts and notification clicks (`finio://open?path=…`) — reduced
 * to one sealed [LaunchTarget]. Pure JVM (java.net only) so it's unit-tested; the Android
 * `Intent` adapter is [IncomingIntent].
 *
 * Like the web's lock gate, nothing here navigates: the app layer holds the target until the
 * lock/onboarding gates lift, then routes to it.
 */
sealed interface LaunchTarget {
    /** Open Add Transaction seeded from [draft] (share text, or a shortcut's `type`). */
    data class AddTransaction(val draft: SharedTransactionDraft, val source: Source) : LaunchTarget

    /** Open an in-app route, e.g. `/budgets` or `/transactions?account=x`. Always starts with `/`. */
    data class Route(val path: String) : LaunchTarget

    enum class Source { Share, DeepLink }
}

object DeepLinks {
    const val SCHEME = "finio"
    const val HOST = "open"
    const val PATH_PARAM = "path"

    /** In-app paths that render Add Transaction from query params (web: `/share-target` too). */
    private val ADD_TRANSACTION_PATHS = setOf("/add-transaction", "/share-target")

    /** `finio://open?path=<encoded path>` for an in-app [path]. */
    fun forPath(path: String): String =
        "$SCHEME://$HOST?$PATH_PARAM=${URLEncoder.encode(path, Charsets.UTF_8.name())}"

    /**
     * Parse a `finio://open?path=…` URI. Returns null for anything else (other scheme/host, no
     * or unsafe path), so a crafted link can never point the app outside its own routes.
     */
    fun parse(uri: String): LaunchTarget? {
        val parsed = runCatching { URI(uri) }.getOrNull() ?: return null
        if (!parsed.scheme.equals(SCHEME, ignoreCase = true)) return null
        if (!parsed.host.equals(HOST, ignoreCase = true)) return null
        val path = queryParams(parsed.rawQuery)[PATH_PARAM] ?: return null
        return forInAppPath(path, LaunchTarget.Source.DeepLink)
    }

    /** Turn an in-app path (with optional query) into a target, or null if it isn't one. */
    fun forInAppPath(path: String, source: LaunchTarget.Source): LaunchTarget? {
        if (!isSafeInAppPath(path)) return null
        val q = path.indexOf('?')
        val pathname = (if (q >= 0) path.substring(0, q) else path).let { p ->
            p.trimEnd('/').ifEmpty { "/" }
        }
        if (pathname in ADD_TRANSACTION_PATHS) {
            val params = if (q >= 0) queryParams(path.substring(q + 1)) else emptyMap()
            return LaunchTarget.AddTransaction(
                parseSharePayload(
                    title = params["title"],
                    text = params["text"],
                    url = params["url"],
                    type = params["type"],
                ),
                source,
            )
        }
        return LaunchTarget.Route(path)
    }

    /** Same-origin only: `/x`, never `//host`, a scheme, a backslash or a control character. */
    fun isSafeInAppPath(path: String): Boolean =
        path.startsWith("/") &&
            !path.startsWith("//") &&
            '\\' !in path &&
            path.none { it.isISOControl() }

    /** `a=1&b=x%20y` → map; first occurrence wins (URLSearchParams.get). */
    fun queryParams(rawQuery: String?): Map<String, String> {
        if (rawQuery.isNullOrEmpty()) return emptyMap()
        val out = LinkedHashMap<String, String>()
        for (pair in rawQuery.split('&')) {
            if (pair.isEmpty()) continue
            val eq = pair.indexOf('=')
            val key = decode(if (eq >= 0) pair.substring(0, eq) else pair) ?: continue
            val value = decode(if (eq >= 0) pair.substring(eq + 1) else "") ?: continue
            out.putIfAbsent(key, value)
        }
        return out
    }

    private fun decode(s: String): String? =
        runCatching { URLDecoder.decode(s, Charsets.UTF_8.name()) }.getOrNull()
}

/**
 * The pure core of [IncomingIntent.parse]: the intent's action, data string, MIME type and
 * the ACTION_SEND extras. Returns null for a plain launcher start (or anything unrecognised).
 */
fun parseLaunch(
    action: String?,
    data: String?,
    mimeType: String? = null,
    subject: String? = null,
    text: String? = null,
): LaunchTarget? = when (action) {
    ACTION_SEND -> {
        val isText = mimeType == null || mimeType.startsWith("text/")
        if (!isText || (subject.isNullOrBlank() && text.isNullOrBlank())) null
        else LaunchTarget.AddTransaction(
            parseSharePayload(title = subject, text = text),
            LaunchTarget.Source.Share,
        )
    }
    ACTION_VIEW -> data?.let(DeepLinks::parse)
    else -> null
}

// String constants rather than android.content.Intent's, so this file stays JVM-testable.
internal const val ACTION_SEND = "android.intent.action.SEND"
internal const val ACTION_VIEW = "android.intent.action.VIEW"
