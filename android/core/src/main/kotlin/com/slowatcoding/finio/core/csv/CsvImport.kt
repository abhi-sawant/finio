package com.slowatcoding.finio.core.csv

import com.slowatcoding.finio.core.js.jsToFixed
import com.slowatcoding.finio.core.model.Category
import com.slowatcoding.finio.core.model.CategoryRule
import com.slowatcoding.finio.core.model.CategoryType
import com.slowatcoding.finio.core.model.Transaction
import com.slowatcoding.finio.core.model.TransactionType
import com.slowatcoding.finio.core.rules.JsRegex
import com.slowatcoding.finio.core.rules.findMatchingRule
import com.slowatcoding.finio.core.rules.jsTrim
import com.slowatcoding.finio.core.rules.mergeLabels
import java.time.LocalDate
import kotlin.math.abs

// Port of web/src/utils/csvImport.ts. Bank CSV exports are free-form: unknown headers, unknown
// date format, and either a signed amount column or separate debit/credit columns. Everything is
// a pure function of (headers, rows, mapping). The web's regexes are JS regexes (JsRegex), and
// every `.trim()` is JS trim — a UTF-8 BOM or NBSP is whitespace there, unlike Kotlin's trim().

data class CsvParseResult(val headers: List<String>, val rows: List<List<String>>)

/** Papa.parse with auto-detected delimiter/newline (see CsvParser.kt), then header + data rows. */
fun parseCsvText(text: String, skipRows: Int = 0): CsvParseResult {
    val data = papaParse(jsTrim(text), skipEmptyLines = true).data
    val from = if (skipRows < 0) maxOf(0, data.size + skipRows) else minOf(skipRows, data.size)
    val lines = data.subList(from, data.size)
    val headerRow = lines.firstOrNull() ?: emptyList()
    return CsvParseResult(headerRow.map(::jsTrim), lines.drop(1))
}

enum class AmountMode(val wire: String) { Signed("signed"), DebitCredit("debitCredit") }

/** Columns the mapping step pre-selects from the header names alone. */
data class GuessedColumns(
    val dateCol: Int? = null,
    val amountMode: AmountMode,
    val amountCol: Int? = null,
    val debitCol: Int? = null,
    val creditCol: Int? = null,
    val noteCol: Int? = null,
    val categoryCol: Int? = null,
)

private val DEBIT_HEADER = JsRegex.compile("\\bdebit|withdraw|\\bdr\\b|money out|paid out", "i")
private val CREDIT_HEADER = JsRegex.compile("\\bcredit|deposit|\\bcr\\b|money in|paid in", "i")
private val AMOUNT_HEADER = JsRegex.compile("amount|\\bamt\\b", "i")
private val NOTE_HEADER = JsRegex.compile("note|desc|narration|particular|memo|details|remark|payee", "i")
private val CATEGORY_HEADER = JsRegex.compile("categ", "i")
private val DATE_HEADER = JsRegex.compile("date", "i")

/**
 * Best-effort auto-mapping from header names. Separate Debit/Credit (or Withdrawal/Deposit)
 * headers win over a single amount column ("Withdrawal Amt." is a debit column).
 */
fun guessColumnMapping(headers: List<String>): GuessedColumns {
    fun find(re: JsRegex, exclude: List<Int?> = emptyList()): Int? {
        val i = headers.indices.firstOrNull { re.test(headers[it]) && it !in exclude }
        return i
    }
    val dateCol = find(DATE_HEADER)
    val debitCol = find(DEBIT_HEADER, listOf(dateCol))
    val creditCol = find(CREDIT_HEADER, listOf(dateCol, debitCol))
    val taken = listOf(dateCol, debitCol, creditCol)
    val noteCol = find(NOTE_HEADER, taken)
    val categoryCol = find(CATEGORY_HEADER, taken + noteCol)

    if (debitCol != null && creditCol != null) {
        return GuessedColumns(dateCol, AmountMode.DebitCredit, debitCol = debitCol, creditCol = creditCol, noteCol = noteCol, categoryCol = categoryCol)
    }
    val amountCol = find(AMOUNT_HEADER, listOf(dateCol, noteCol, categoryCol))
    return GuessedColumns(dateCol, AmountMode.Signed, amountCol = amountCol, noteCol = noteCol, categoryCol = categoryCol)
}

enum class DateFormatCode(val wire: String, val label: String) {
    YYYY_MM_DD("YYYY-MM-DD", "YYYY-MM-DD (2026-07-27)"),
    DD_SLASH_MM_YYYY("DD/MM/YYYY", "DD/MM/YYYY (27/07/2026)"),
    MM_SLASH_DD_YYYY("MM/DD/YYYY", "MM/DD/YYYY (07/27/2026)"),
    DD_DASH_MM_YYYY("DD-MM-YYYY", "DD-MM-YYYY (27-07-2026)"),
    MM_DASH_DD_YYYY("MM-DD-YYYY", "MM-DD-YYYY (07-27-2026)"),
    DD_DOT_MM_YYYY("DD.MM.YYYY", "DD.MM.YYYY (27.07.2026)");

    companion object {
        fun fromWire(s: String): DateFormatCode? = entries.firstOrNull { it.wire == s }
    }
}

/** In detection priority order, most-unambiguous first. */
val DATE_FORMATS: List<DateFormatCode> = DateFormatCode.entries

/** `new Date(Date.UTC(y, m-1, d)).toISOString()`, or null when it doesn't round-trip. */
private fun isoFromParts(year: Int, month: Int, day: Int): String? {
    if (month < 1 || month > 12 || day < 1 || day > 31) return null
    // Date.UTC maps years 0–99 to 1900–1999, so they never round-trip.
    if (year in 0..99) return null
    val date = try { LocalDate.of(year, month, day) } catch (_: java.time.DateTimeException) { return null }
    return String.format(java.util.Locale.ROOT, "%04d-%02d-%02dT00:00:00.000Z", date.year, date.monthValue, date.dayOfMonth)
}

private val YMD = JsRegex.compile("^(\\d{4})[-/](\\d{1,2})[-/](\\d{1,2})")
private val SLASH = JsRegex.compile("^(\\d{1,2})\\/(\\d{1,2})\\/(\\d{4})")
private val DASH = JsRegex.compile("^(\\d{1,2})-(\\d{1,2})-(\\d{4})")
private val DOT = JsRegex.compile("^(\\d{1,2})\\.(\\d{1,2})\\.(\\d{4})")

fun parseDateWithFormat(raw: String, format: DateFormatCode): String? {
    val trimmed = jsTrim(raw)
    if (trimmed.isEmpty()) return null
    fun g(m: JsRegex.Match, i: Int) = m[i]!!.toInt()
    return when (format) {
        DateFormatCode.YYYY_MM_DD -> YMD.exec(trimmed)?.let { isoFromParts(g(it, 1), g(it, 2), g(it, 3)) }
        DateFormatCode.DD_SLASH_MM_YYYY -> SLASH.exec(trimmed)?.let { isoFromParts(g(it, 3), g(it, 2), g(it, 1)) }
        DateFormatCode.MM_SLASH_DD_YYYY -> SLASH.exec(trimmed)?.let { isoFromParts(g(it, 3), g(it, 1), g(it, 2)) }
        DateFormatCode.DD_DASH_MM_YYYY -> DASH.exec(trimmed)?.let { isoFromParts(g(it, 3), g(it, 2), g(it, 1)) }
        DateFormatCode.MM_DASH_DD_YYYY -> DASH.exec(trimmed)?.let { isoFromParts(g(it, 3), g(it, 1), g(it, 2)) }
        DateFormatCode.DD_DOT_MM_YYYY -> DOT.exec(trimmed)?.let { isoFromParts(g(it, 3), g(it, 2), g(it, 1)) }
    }
}

data class DateFormatDetection(
    val format: DateFormatCode?,
    /** True when every sample has day and month both <= 12, so DD/MM vs MM/DD was a guess. */
    val ambiguous: Boolean,
)

/** Share of non-empty samples a format must parse to be accepted (tolerates a few junk rows). */
private const val DATE_MATCH_THRESHOLD = 0.9

private val SWAPPED_TWIN = mapOf(
    DateFormatCode.DD_SLASH_MM_YYYY to DateFormatCode.MM_SLASH_DD_YYYY,
    DateFormatCode.MM_SLASH_DD_YYYY to DateFormatCode.DD_SLASH_MM_YYYY,
    DateFormatCode.DD_DASH_MM_YYYY to DateFormatCode.MM_DASH_DD_YYYY,
    DateFormatCode.MM_DASH_DD_YYYY to DateFormatCode.DD_DASH_MM_YYYY,
)

private val DAY_MONTH_PREFIX = JsRegex.compile("^(\\d{1,2})[/-](\\d{1,2})")

private fun matchRatio(samples: List<String>, format: DateFormatCode): Double =
    samples.count { parseDateWithFormat(it, format) != null }.toDouble() / samples.size

/**
 * Tries formats most-unambiguous-first and picks the first that parses ≥ 90% of the samples.
 * `ambiguous` when the swapped twin fits equally well (every row has both parts <= 12).
 */
fun detectDateFormatInfo(samples: List<String>): DateFormatDetection {
    val nonEmpty = samples.map(::jsTrim).filter { it.isNotEmpty() }
    if (nonEmpty.isEmpty()) return DateFormatDetection(null, false)
    val found = DATE_FORMATS.firstOrNull { matchRatio(nonEmpty, it) >= DATE_MATCH_THRESHOLD }
        ?: return DateFormatDetection(null, false)
    val twin = SWAPPED_TWIN[found]
    val ambiguous = twin != null && nonEmpty.all { s ->
        val m = DAY_MONTH_PREFIX.exec(s)
        m != null && m[1]!!.toInt() <= 12 && m[2]!!.toInt() <= 12
    }
    return DateFormatDetection(found, ambiguous)
}

fun detectDateFormat(samples: List<String>): DateFormatCode? = detectDateFormatInfo(samples).format

private const val CURRENCY_TOKEN = "(?:[A-Za-z]+\\.?|[\$₹€£¥]|\\s)+"
private val LEADING_CURRENCY = JsRegex.compile("^$CURRENCY_TOKEN")
private val TRAILING_CURRENCY = JsRegex.compile("$CURRENCY_TOKEN$")
private val PLAIN_NUMBER = JsRegex.compile("^(?:\\d+\\.?\\d*|\\.\\d+)(?:e[-+]?\\d+)?$", "i")
private val PARENTHESISED = JsRegex.compile("^\\(.*\\)$")
private val SIGN = JsRegex.compile("^([-+])\\s*")
private val WHITESPACE_RUN = JsRegex.compile("\\s+", "g")

/**
 * Parses a bank-export amount. Only currency symbols/words at the very start or end are dropped
 * ("Rs. 2,000", "450 USD", "$1,000.00"), along with thousand separators. Anything else left over
 * ("12abc34") is junk → null. Scientific notation is accepted; "(500.00)" is negative.
 */
fun parseAmount(raw: String?): Double? {
    if (raw == null) return null
    var s = jsTrim(raw)
    if (s.isEmpty()) return null

    var negative = false
    if (PARENTHESISED.test(s)) {
        negative = true
        s = jsTrim(s.substring(1, s.length - 1))
    }

    var sign = 1
    SIGN.exec(s)?.let { m ->
        if (m[1] == "-") sign = -1
        s = s.substring(m.value.length)
    }

    s = LEADING_CURRENCY.replace(s, "")
    // A sign may also follow the currency symbol ("$-5").
    SIGN.exec(s)?.let { m ->
        if (m[1] == "-") sign = -sign
        s = s.substring(m.value.length)
    }
    s = WHITESPACE_RUN.replace(TRAILING_CURRENCY.replace(s, "").replace(",", ""), "")
    if (!PLAIN_NUMBER.test(s)) return null

    val n = s.toDouble() * sign
    if (!n.isFinite()) return null
    return if (negative) -abs(n) else n
}

/** The column mapping the wizard settles on. */
data class ColumnMapping(
    val dateCol: Int,
    val noteCol: Int? = null,
    val categoryCol: Int? = null,
    val amountMode: AmountMode,
    /** Used when `amountMode == Signed`. */
    val amountCol: Int? = null,
    /** Whether a negative signed amount is an expense (null = true, the common convention). */
    val negativeIsExpense: Boolean? = null,
    /** Used when `amountMode == DebitCredit`. */
    val debitCol: Int? = null,
    val creditCol: Int? = null,
)

data class CsvImportOptions(
    val mapping: ColumnMapping,
    val dateFormat: DateFormatCode,
    val accountId: String,
    val categories: List<Category>,
    /** Category to fall back to when no category column is mapped, or its value matches nothing. */
    val fallbackCategoryId: String,
    /** Rules, in priority order; they fire only when the file's own category didn't match. */
    val rules: List<CategoryRule>? = null,
)

/** `Omit<Transaction, 'id' | 'createdAt'>` as built by the importer. */
data class CsvTransactionDraft(
    val type: TransactionType,
    val amount: Double,
    val accountId: String,
    val categoryId: String,
    val date: String,
    val note: String,
    val labels: List<String>,
)

data class ParsedCsvTransaction(
    /** Index into the original data rows (0-based). */
    val rowIndex: Int,
    val transaction: CsvTransactionDraft,
    /** False when a category column was mapped but its value didn't match any category. */
    val categoryMatched: Boolean,
    /** Set when an auto-categorization rule picked this row's category. */
    val matchedRuleId: String? = null,
)

data class CsvImportResult(
    val accepted: List<ParsedCsvTransaction>,
    val totalRows: Int,
    /** Per-row rejection reasons, capped for display. */
    val issues: List<String>,
)

private const val MAX_ISSUES = 8

private fun List<String>.cell(i: Int?): String = if (i == null || i < 0) "" else getOrNull(i) ?: ""

fun buildTransactionsFromCsv(rows: List<List<String>>, options: CsvImportOptions): CsvImportResult {
    val mapping = options.mapping
    val accepted = mutableListOf<ParsedCsvTransaction>()
    val allIssues = mutableListOf<String>()

    rows.forEachIndexed { index, row ->
        val rowLabel = "Row ${index + 1}"
        val rawDate = row.cell(mapping.dateCol)
        val date = parseDateWithFormat(rawDate, options.dateFormat)
        if (date == null) {
            allIssues += "$rowLabel: unparseable date \"$rawDate\""
            return@forEachIndexed
        }

        val amount: Double
        val type: TransactionType
        if (mapping.amountMode == AmountMode.Signed) {
            val raw = if (mapping.amountCol != null) row.cell(mapping.amountCol) else ""
            val parsed = parseAmount(raw)
            if (parsed == null || parsed == 0.0) {
                allIssues += "$rowLabel: unparseable amount \"$raw\""
                return@forEachIndexed
            }
            val negativeIsExpense = mapping.negativeIsExpense ?: true
            val isExpense = if (negativeIsExpense) parsed < 0 else parsed > 0
            type = if (isExpense) TransactionType.Expense else TransactionType.Income
            amount = abs(parsed)
        } else {
            val rawDebit = if (mapping.debitCol != null) row.cell(mapping.debitCol) else ""
            val rawCredit = if (mapping.creditCol != null) row.cell(mapping.creditCol) else ""
            val debit = abs(parseAmount(rawDebit) ?: 0.0)
            val credit = abs(parseAmount(rawCredit) ?: 0.0)
            if (debit > 0 && credit > 0) {
                allIssues += "$rowLabel: both debit and credit are filled"
                return@forEachIndexed
            }
            if (debit <= 0 && credit <= 0) {
                allIssues += "$rowLabel: no debit or credit amount"
                return@forEachIndexed
            }
            type = if (debit > 0) TransactionType.Expense else TransactionType.Income
            amount = if (debit > 0) debit else credit
        }

        val note = if (mapping.noteCol != null) jsTrim(row.cell(mapping.noteCol)) else ""

        var categoryId = options.fallbackCategoryId
        var categoryMatched = false
        if (mapping.categoryCol != null) {
            val rawCategory = jsTrim(row.cell(mapping.categoryCol))
            if (rawCategory.isNotEmpty()) {
                val match = options.categories.firstOrNull { c ->
                    (c.type.wire == type.wire || c.type == CategoryType.Both) &&
                        c.name.lowercase() == rawCategory.lowercase()
                }
                if (match != null) {
                    categoryId = match.id
                    categoryMatched = true
                }
            }
        }

        // The statement had nothing to say about this row's category — let the rules try.
        var labels = emptyList<String>()
        var matchedRuleId: String? = null
        if (!categoryMatched && !options.rules.isNullOrEmpty()) {
            val rule = findMatchingRule(options.rules, note, type)
            if (rule != null) {
                categoryId = rule.categoryId
                labels = mergeLabels(labels, rule.labelIds)
                matchedRuleId = rule.id
            }
        }

        accepted += ParsedCsvTransaction(
            rowIndex = index,
            transaction = CsvTransactionDraft(type, amount, options.accountId, categoryId, date, note, labels),
            categoryMatched = categoryMatched,
            matchedRuleId = matchedRuleId?.ifEmpty { null },
        )
    }

    val issues = allIssues.take(MAX_ISSUES).toMutableList()
    if (allIssues.size > issues.size) issues += "…and ${allIssues.size - issues.size} more"
    return CsvImportResult(accepted, rows.size, issues)
}

private fun dedupeKey(date: String, amount: Double, note: String, type: TransactionType): String =
    "${date.take(10)}|${type.wire}|${jsToFixed(amount, 2)}|${jsTrim(note).lowercase()}"

/**
 * Rows that look already in the ledger — same day, type, amount and note — whether against
 * existing transactions or an earlier row in the same file. Insertion-ordered.
 */
fun findDuplicateRows(candidates: List<ParsedCsvTransaction>, existing: List<Transaction>): Set<Int> {
    val existingKeys = existing.map { dedupeKey(it.date, it.amount, it.note, it.type) }.toHashSet()
    val seenInBatch = HashSet<String>()
    val duplicates = LinkedHashSet<Int>()
    for ((rowIndex, transaction) in candidates) {
        val key = dedupeKey(transaction.date, transaction.amount, transaction.note, transaction.type)
        if (key in existingKeys || key in seenInBatch) duplicates += rowIndex
        seenInBatch += key
    }
    return duplicates
}
