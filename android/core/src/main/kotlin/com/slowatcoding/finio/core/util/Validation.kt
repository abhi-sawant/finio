package com.slowatcoding.finio.core.util

// Port of web/src/utils/validation.ts — shared input limits and small pure validators.

/** Names of users, accounts, goals, categories, labels, people, templates, loans. */
const val MAX_NAME_LENGTH = 40
/** Free-text notes on transactions, recurring rules, goal/debt/loan entries. */
const val MAX_NOTE_LENGTH = 200
/** A categorization rule's match pattern. */
const val MAX_PATTERN_LENGTH = 100

/**
 * JS whitespace as `String.prototype.trim` and regex `\s` define it (WhiteSpace + LineTerminator).
 * Kotlin's `Char.isWhitespace` differs (it includes U+001C–U+001F and excludes U+FEFF), and a
 * Java regex `\s` is ASCII-only, so ported code must use this instead.
 */
fun isJsWhitespace(c: Char): Boolean = when (c) {
    '\u0009', '\u000A', '\u000B', '\u000C', '\u000D', ' ', ' ', ' ',
    ' ', ' ', ' ', ' ', '　', '﻿' -> true
    else -> c in ' '..' '
}

/** JS `String.prototype.trim()`. */
fun jsTrim(value: String): String {
    var start = 0
    var end = value.length
    while (start < end && isJsWhitespace(value[start])) start++
    while (end > start && isJsWhitespace(value[end - 1])) end--
    return value.substring(start, end)
}

/** Trim and cap a free-text value. Caps by code point so an emoji is never split. */
fun cleanText(value: String, max: Int): String {
    val trimmed = jsTrim(value)
    val count = trimmed.codePointCount(0, trimmed.length)
    if (count <= max) return trimmed
    // Array.slice(0, max): a negative end counts back from the end.
    val keep = if (max < 0) maxOf(count + max, 0) else max
    return trimmed.substring(0, trimmed.offsetByCodePoints(0, keep))
}

/** For `onChange`: drop leading whitespace while typing (trailing is kept so words can be typed). */
fun stripLeading(value: String): String {
    var i = 0
    while (i < value.length && isJsWhitespace(value[i])) i++
    return value.substring(i)
}

/**
 * A pragmatic client-side email check — the server remains the authority. Hand-rolled
 * equivalent of `/^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/` (non-unicode JS regex, so `{2,}` counts
 * UTF-16 units — a Java regex would count code points).
 */
fun isValidEmail(value: String): Boolean {
    val s = jsTrim(value)
    if (s.any { isJsWhitespace(it) }) return false
    val at = s.indexOf('@')
    if (at <= 0 || s.indexOf('@', at + 1) >= 0) return false
    val domain = s.substring(at + 1)
    // Some '.' with at least one unit before it and at least two after it.
    for (i in 1..domain.length - 3) if (domain[i] == '.') return true
    return false
}

/** Whether a `yyyy-MM-dd` day is before `today` (also `yyyy-MM-dd`). Plain string compare is safe. */
fun isPastDay(dayKey: String, today: String): Boolean = dayKey < today

/** Whether both ends of a `yyyy-MM-dd` range are set and From is after To. */
fun isRangeInverted(from: String, to: String): Boolean = from.isNotEmpty() && to.isNotEmpty() && from > to

/** The first budget scope value not already taken, in [candidates] order. Null when all are used. */
fun firstFreeScope(candidates: List<String>, used: Set<String>): String? = candidates.firstOrNull { it !in used }
