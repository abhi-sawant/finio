package com.slowatcoding.finio.core.csv

import com.slowatcoding.finio.core.rules.jsTrim

// Port of the slice of papaparse 5.5.x that web/src/utils/csvImport.ts uses:
// `Papa.parse(string, { skipEmptyLines })` with no delimiter, newline, header, comments or
// dynamic typing. That means papaparse's own auto-detection, reproduced here line for line:
//  - the newline is guessed (`guessLineEndings`: quoted text removed, then \n vs \r vs \r\n),
//  - the delimiter is guessed among , \t | ; RS US from the first 10 rows (`guessDelimiter`:
//    the candidate with the steadiest field count, averaging > 1.99 fields, wins; ',' otherwise),
//  - the core parser's quirks are kept: a field is only "quoted" if the quote is its first char,
//    `""` unescapes, spaces between a closing quote and the delimiter/newline are dropped, a
//    malformed closing quote keeps scanning, an unterminated quote swallows the rest of the input,
//    and a quote-free input takes the plain split fast path,
//  - `skipEmptyLines: true` drops only rows that are exactly one empty field.
// Errors are not reported (csvImport never reads them).

data class PapaResult(val data: List<List<String>>, val delimiter: String, val linebreak: String)

private const val QUOTE = '"'
private val DELIMITERS_TO_GUESS = listOf(",", "\t", "|", ";", "\u001E", "\u001F")

/** `Papa.parse(input, { skipEmptyLines })` for a string input. */
fun papaParse(input: String, skipEmptyLines: Boolean = false): PapaResult {
    val text = if (input.startsWith('﻿')) input.substring(1) else input
    val newline = guessLineEndings(text)
    val delimiter = guessDelimiter(text, newline, skipEmptyLines) ?: ","
    var data = CoreParser(delimiter, newline, preview = 0).parse(text)
    if (skipEmptyLines) data = data.filterNot(::isEmptyLine)
    return PapaResult(data, delimiter, newline)
}

private fun isEmptyLine(row: List<String>) = row.size == 1 && row[0].isEmpty()

/** JS `String.prototype.substring`: clamps to [0, length] and swaps reversed bounds. */
private fun String.jsSubstring(a: Int, b: Int = length): String {
    val s = a.coerceIn(0, length)
    val e = b.coerceIn(0, length)
    return if (s <= e) substring(s, e) else substring(e, s)
}

/** JS `String.prototype.substr(start, length)` for start ≥ 0. */
private fun String.jsSubstr(start: Int, len: Int): String {
    if (start >= length) return ""
    return substring(start, minOf(length, start + len))
}

internal fun guessLineEndings(raw: String): String {
    val head = raw.jsSubstring(0, 1024 * 1024)
    // input.replace(/"([^]*?)"/gm, '')
    val sb = StringBuilder()
    var i = 0
    while (i < head.length) {
        val open = head.indexOf(QUOTE, i)
        if (open < 0) { sb.append(head, i, head.length); break }
        val close = head.indexOf(QUOTE, open + 1)
        if (close < 0) { sb.append(head, i, head.length); break }
        sb.append(head, i, open)
        i = close + 1
    }
    val input = sb.toString()
    val r = input.split('\r')
    val n = input.split('\n')
    val nAppearsFirst = n.size > 1 && n[0].length < r[0].length
    if (r.size == 1 || nAppearsFirst) return "\n"
    var numWithN = 0
    for (part in r) if (part.isNotEmpty() && part[0] == '\n') numWithN++
    return if (numWithN >= r.size / 2.0) "\r\n" else "\r"
}

internal fun guessDelimiter(input: String, newline: String, skipEmptyLines: Boolean): String? {
    var bestDelim: String? = null
    var bestDelta: Double? = null
    var maxFieldCount: Double? = null
    for (delim in DELIMITERS_TO_GUESS) {
        var delta = 0.0
        var avgFieldCount = 0.0
        var emptyLinesCount = 0
        var fieldCountPrevRow: Int? = null
        val preview = CoreParser(delim, newline, preview = 10).parse(input)
        for (row in preview) {
            if (skipEmptyLines && isEmptyLine(row)) { emptyLinesCount++; continue }
            val fieldCount = row.size
            avgFieldCount += fieldCount
            if (fieldCountPrevRow == null) { fieldCountPrevRow = fieldCount; continue }
            else if (fieldCount > 0) {
                delta += kotlin.math.abs(fieldCount - fieldCountPrevRow)
                fieldCountPrevRow = fieldCount
            }
        }
        if (preview.isNotEmpty()) avgFieldCount /= (preview.size - emptyLinesCount).toDouble()
        if ((bestDelta == null || delta <= bestDelta) &&
            (maxFieldCount == null || avgFieldCount > maxFieldCount) && avgFieldCount > 1.99
        ) {
            bestDelta = delta
            bestDelim = delim
            maxFieldCount = avgFieldCount
        }
    }
    return bestDelim
}

/** papaparse's `Parser` (no comments, no step, no header, quoteChar = escapeChar = `"`). */
private class CoreParser(val delim: String, val newline: String, val preview: Int) {

    fun parse(input: String): List<List<String>> {
        val inputLen = input.length
        val delimLen = delim.length
        val newlineLen = newline.length
        var cursor = 0
        val data = mutableListOf<List<String>>()
        var row = mutableListOf<String>()

        if (input.isEmpty()) return data

        if (input.indexOf(QUOTE) == -1) {
            val rows = input.split(newline)
            for ((i, line) in rows.withIndex()) {
                data.add(line.split(delim))
                if (preview > 0 && i >= preview) return data.subList(0, preview).toList()
            }
            return data
        }

        var nextDelim = input.indexOf(delim, cursor)
        var nextNewline = input.indexOf(newline, cursor)
        var quoteSearch: Int

        fun unescape(s: String) = s.replace("\"\"", "\"")
        fun saveRow(newCursor: Int) {
            cursor = newCursor
            data.add(row)
            row = mutableListOf()
            nextNewline = input.indexOf(newline, cursor)
        }
        fun finish(value: String? = null): List<List<String>> {
            row.add(value ?: input.jsSubstring(cursor))
            cursor = inputLen
            data.add(row)
            return data
        }

        while (true) {
            if (input.getOrNull(cursor) == QUOTE) {
                quoteSearch = cursor
                cursor++
                while (true) {
                    quoteSearch = input.indexOf(QUOTE, quoteSearch + 1)
                    if (quoteSearch == -1) return finish()
                    if (quoteSearch == inputLen - 1) return finish(unescape(input.jsSubstring(cursor, quoteSearch)))
                    if (input.getOrNull(quoteSearch + 1) == QUOTE) { quoteSearch++; continue }

                    if (nextDelim != -1 && nextDelim < quoteSearch + 1) nextDelim = input.indexOf(delim, quoteSearch + 1)
                    if (nextNewline != -1 && nextNewline < quoteSearch + 1) nextNewline = input.indexOf(newline, quoteSearch + 1)
                    val checkUpTo = if (nextNewline == -1) nextDelim else minOf(nextDelim, nextNewline)
                    val qs = quoteSearch
                    fun extraSpaces(index: Int): Int {
                        if (index == -1) return 0
                        val between = input.jsSubstring(qs + 1, index)
                        return if (between.isNotEmpty() && jsTrim(between) == "") between.length else 0
                    }
                    val spacesToDelim = extraSpaces(checkUpTo)
                    if (input.jsSubstr(quoteSearch + 1 + spacesToDelim, delimLen) == delim) {
                        row.add(unescape(input.jsSubstring(cursor, quoteSearch)))
                        cursor = quoteSearch + 1 + spacesToDelim + delimLen
                        if (input.getOrNull(quoteSearch + 1 + spacesToDelim + delimLen) != QUOTE) {
                            quoteSearch = input.indexOf(QUOTE, cursor)
                        }
                        nextDelim = input.indexOf(delim, cursor)
                        nextNewline = input.indexOf(newline, cursor)
                        break
                    }
                    val spacesToNewline = extraSpaces(nextNewline)
                    val afterSpaces = quoteSearch + 1 + spacesToNewline
                    if (input.jsSubstring(afterSpaces, afterSpaces + newlineLen) == newline) {
                        row.add(unescape(input.jsSubstring(cursor, quoteSearch)))
                        saveRow(afterSpaces + newlineLen)
                        nextDelim = input.indexOf(delim, cursor)
                        quoteSearch = input.indexOf(QUOTE, cursor)
                        if (preview > 0 && data.size >= preview) return data
                        break
                    }
                    // Malformed closing quote: treat it as data and keep looking (papaparse:
                    // `quoteSearch++; continue;`, so the next search starts two past it).
                    quoteSearch++
                }
                continue
            }

            if (nextDelim != -1 && (nextDelim < nextNewline || nextNewline == -1)) {
                row.add(input.jsSubstring(cursor, nextDelim))
                cursor = nextDelim + delimLen
                nextDelim = input.indexOf(delim, cursor)
                continue
            }
            if (nextNewline != -1) {
                row.add(input.jsSubstring(cursor, nextNewline))
                saveRow(nextNewline + newlineLen)
                if (preview > 0 && data.size >= preview) return data
                continue
            }
            break
        }
        return finish()
    }
}
