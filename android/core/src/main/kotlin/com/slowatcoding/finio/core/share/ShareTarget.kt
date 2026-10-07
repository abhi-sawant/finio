package com.slowatcoding.finio.core.share

import com.slowatcoding.finio.core.crypto.jsNumberString
import com.slowatcoding.finio.core.model.TransactionType
import com.slowatcoding.finio.core.money.roundMoney
import com.slowatcoding.finio.core.rules.JsRegex
import com.slowatcoding.finio.core.rules.jsTrim

// Port of web/src/utils/shareTarget.ts. Parsing for shared text (Android ACTION_SEND / the web
// Share Target) and launcher shortcuts: given whatever the share sheet handed us, produce a draft
// transaction the add form can seed itself from. `parseSharePayload` takes the title/text/url
// strings directly instead of URLSearchParams.

/** Notes longer than this are a wall of SMS boilerplate, not a description. */
const val NOTE_MAX_LENGTH = 140

/** Anything above this is a parse error, not a transaction. */
private const val MAX_SHARED_AMOUNT = 1e9

/** A number is only an amount if it is next to a currency marker (beats card masks, OTPs, dates). */
private val CURRENCY_AMOUNT = JsRegex.compile(
    "(?:₹|\\bRs\\.?|\\bINR\\b)\\s*([\\d,]+(?:\\.\\d{1,2})?)|\\b([\\d,]+(?:\\.\\d{1,2})?)\\s*(?:₹|\\bINR\\b)",
    "i",
)

/** Indian bank SMS say credited/received for inflow and debited/spent/paid for outflow. */
private val INCOME_WORDS = JsRegex.compile("\\b(credited|received|deposited|refund(?:ed)?|salary|cashback)\\b", "i")

private val LOOKS_LIKE_URL = JsRegex.compile("^https?:\\/\\/\\S+$", "i")

/** A transaction seeded from shared text. `amount` is a string because the number pad is. */
data class SharedTransactionDraft(val type: TransactionType, val amount: String, val note: String)

/** A money amount from shared text, or null when nothing is definitely money. */
fun extractAmount(text: String): Double? {
    val match = CURRENCY_AMOUNT.exec(text) ?: return null
    val raw = match[1] ?: match[2]
    if (raw.isNullOrEmpty()) return null
    val parsed = raw.replace(",", "").toDoubleOrNull() ?: return null
    if (!parsed.isFinite() || parsed <= 0 || parsed > MAX_SHARED_AMOUNT) return null
    return roundMoney(parsed)
}

/** Expense unless the text clearly describes money arriving — by far the common share. */
fun inferTransactionType(text: String): TransactionType =
    if (INCOME_WORDS.test(text)) TransactionType.Income else TransactionType.Expense

private fun transactionTypeOf(value: String?): TransactionType? =
    TransactionType.entries.firstOrNull { it.wire == value }

/** `https://swiggy.com/order/123` → `swiggy.com`, so a bare link still reads as something. */
private fun hostnameOf(url: String): String {
    val host = whatwgHostname(url) ?: return ""
    return if (host.startsWith("www.")) host.substring(4) else host
}

/**
 * Turn the share sheet's title/text/url (and a shortcut's `type`) into a draft. An explicit
 * [type] always beats inference: it comes from a shortcut the user deliberately tapped.
 */
fun parseSharePayload(
    title: String? = null,
    text: String? = null,
    url: String? = null,
    type: String? = null,
): SharedTransactionDraft {
    val t = title?.let(::jsTrim) ?: ""
    val x = text?.let(::jsTrim) ?: ""
    val u = url?.let(::jsTrim) ?: ""

    val combined = jsTrim(listOf(t, x).filter { it.isNotEmpty() }.joinToString(" "))

    // Many share sheets put the link in `text` rather than `url`; a raw URL is a terrible note.
    val note = if (LOOKS_LIKE_URL.test(combined)) hostnameOf(combined)
    else combined.ifEmpty { if (u.isNotEmpty()) hostnameOf(u) else "" }

    val searchable = listOf(combined, u).filter { it.isNotEmpty() }.joinToString(" ")
    val amount = if (searchable.isNotEmpty()) extractAmount(searchable) else null

    return SharedTransactionDraft(
        type = transactionTypeOf(type) ?: inferTransactionType(searchable),
        amount = amount?.let(::jsNumberString) ?: "",
        note = jsTrim(note.substring(0, minOf(note.length, NOTE_MAX_LENGTH))),
    )
}
