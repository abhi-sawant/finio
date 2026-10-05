package com.slowatcoding.finio.core.csv

import com.slowatcoding.finio.core.golden.Golden
import com.slowatcoding.finio.core.golden.b
import com.slowatcoding.finio.core.golden.d
import com.slowatcoding.finio.core.golden.i
import com.slowatcoding.finio.core.golden.s
import com.slowatcoding.finio.core.model.TransactionType
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonArray
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CsvImportGoldenTest {
    private fun rows(e: JsonElement) = e.jsonArray.map { r -> r.jsonArray.map { it.s } }
    private fun rowsJson(rows: List<List<String>>) = buildJsonArray { rows.forEach { r -> addJsonArray { r.forEach { add(it) } } } }

    private fun JsonObject.int(k: String): Int? = this[k]?.let { if (it is JsonNull) null else it.i }

    private fun mapping(o: JsonObject) = ColumnMapping(
        dateCol = o.getValue("dateCol").i,
        noteCol = o.int("noteCol"),
        categoryCol = o.int("categoryCol"),
        amountMode = AmountMode.entries.first { it.wire == o.getValue("amountMode").s },
        amountCol = o.int("amountCol"),
        negativeIsExpense = o["negativeIsExpense"]?.b,
        debitCol = o.int("debitCol"),
        creditCol = o.int("creditCol"),
    )

    private fun options(e: JsonElement) = e.jsonObject.let { o ->
        CsvImportOptions(
            mapping = mapping(o.getValue("mapping").jsonObject),
            dateFormat = DateFormatCode.fromWire(o.getValue("dateFormat").s)!!,
            accountId = o.getValue("accountId").s,
            categories = Golden.decode(o.getValue("categories")),
            fallbackCategoryId = o.getValue("fallbackCategoryId").s,
            rules = o["rules"]?.let { Golden.decode(it) },
        )
    }

    private fun parsed(p: ParsedCsvTransaction) = buildJsonObject {
        put("rowIndex", p.rowIndex)
        put("categoryMatched", p.categoryMatched)
        p.matchedRuleId?.let { put("matchedRuleId", it) }
        put("transaction", buildJsonObject {
            val t = p.transaction
            put("type", t.type.wire); put("amount", t.amount); put("accountId", t.accountId)
            put("categoryId", t.categoryId); put("date", t.date); put("note", t.note)
            putJsonArray("labels") { t.labels.forEach { add(it) } }
        })
    }

    private fun result(r: CsvImportResult) = buildJsonObject {
        putJsonArray("accepted") { r.accepted.forEach { add(parsed(it)) } }
        put("totalRows", r.totalRows)
        putJsonArray("issues") { r.issues.forEach { add(it) } }
    }

    private fun guess(g: GuessedColumns) = buildJsonObject {
        g.dateCol?.let { put("dateCol", it) }
        put("amountMode", g.amountMode.wire)
        g.amountCol?.let { put("amountCol", it) }
        g.debitCol?.let { put("debitCol", it) }
        g.creditCol?.let { put("creditCol", it) }
        g.noteCol?.let { put("noteCol", it) }
        g.categoryCol?.let { put("categoryCol", it) }
    }

    private fun candidate(e: JsonElement) = e.jsonObject.let { o ->
        val t = o.getValue("transaction").jsonObject
        ParsedCsvTransaction(
            rowIndex = o.getValue("rowIndex").i,
            transaction = CsvTransactionDraft(
                Golden.decode<TransactionType>(t.getValue("type")), t.getValue("amount").d, t.getValue("accountId").s,
                t.getValue("categoryId").s, t.getValue("date").s, t.getValue("note").s, t.getValue("labels").jsonArray.map { it.s },
            ),
            categoryMatched = o.getValue("categoryMatched").b,
            matchedRuleId = o["matchedRuleId"]?.s,
        )
    }

    @Test
    fun matchesTypeScript() = Golden.verify("csvImport") { c ->
        when (c.fn) {
            "constants" -> buildJsonObject {
                putJsonArray("DATE_FORMATS") {
                    DATE_FORMATS.forEach { add(buildJsonObject { put("value", it.wire); put("label", it.label) }) }
                }
            }
            "Papa.parse" -> papaParse(c.arg(0).s, c.arg(1).b).let {
                buildJsonObject { put("data", rowsJson(it.data)); put("delimiter", it.delimiter); put("linebreak", it.linebreak) }
            }
            "parseCsvText" -> parseCsvText(c.arg(0).s, c.arg(1).i).let {
                buildJsonObject { putJsonArray("headers") { it.headers.forEach { h -> add(h) } }; put("rows", rowsJson(it.rows)) }
            }
            "guessColumnMapping" -> guess(guessColumnMapping(c.arg(0).jsonArray.map { it.s }))
            "parseDateWithFormat" -> parseDateWithFormat(c.arg(0).s, DateFormatCode.fromWire(c.arg(1).s)!!)?.let(::JsonPrimitive) ?: JsonNull
            "detectDateFormatInfo" -> detectDateFormatInfo(c.arg(0).jsonArray.map { it.s }).let {
                buildJsonObject { it.format?.let { f -> put("format", f.wire) }; put("ambiguous", it.ambiguous) }
            }
            "detectDateFormat" -> detectDateFormat(c.arg(0).jsonArray.map { it.s })?.let { JsonPrimitive(it.wire) } ?: JsonNull
            "parseAmount" -> parseAmount(c.arg(0).s)?.let(::JsonPrimitive) ?: JsonNull
            "buildTransactionsFromCsv" -> result(buildTransactionsFromCsv(rows(c.arg(0)), options(c.arg(1))))
            "findDuplicateRows" -> buildJsonArray {
                findDuplicateRows(c.arg(0).jsonArray.map(::candidate), Golden.decode(c.arg(1))).forEach { add(it) }
            }
            "pipeline" -> {
                val (headers, rows) = parseCsvText(c.arg(0).s)
                val g = guessColumnMapping(headers)
                val dateCol = g.dateCol!!
                val fmt = detectDateFormat(rows.map { it.getOrNull(dateCol) ?: "" }) ?: DateFormatCode.YYYY_MM_DD
                val opts = CsvImportOptions(
                    mapping = ColumnMapping(dateCol, g.noteCol, g.categoryCol, g.amountMode, g.amountCol, null, g.debitCol, g.creditCol),
                    dateFormat = fmt,
                    accountId = "acc-1",
                    categories = Golden.decode(c.arg(1)),
                    fallbackCategoryId = "cat-misc",
                    rules = Golden.decode(c.arg(2)),
                )
                buildJsonObject {
                    putJsonArray("headers") { headers.forEach { add(it) } }
                    put("guess", guess(g))
                    put("format", fmt.wire)
                    put("result", result(buildTransactionsFromCsv(rows, opts)))
                }
            }
            else -> null
        }
    }

    @Test
    fun parsesQuotedFieldsWithEmbeddedNewlines() {
        val (headers, rows) = parseCsvText("Date,Amount,Note\r\n2026-01-01,100,\"Rent, \"\"Jan\"\"\r\nline 2\"")
        assertEquals(listOf("Date", "Amount", "Note"), headers)
        assertEquals(listOf(listOf("2026-01-01", "100", "Rent, \"Jan\"\r\nline 2")), rows)
    }

    @Test
    fun guessesSemicolonsAndTabs() {
        assertEquals(";", papaParse("a;b;c\n1;2;3").delimiter)
        assertEquals("\t", papaParse("a\tb\n1\t2").delimiter)
    }

    @Test
    fun amountEdgeCases() {
        assertEquals(-500.0, parseAmount("(500.00)"))
        assertEquals(123456.78, parseAmount("₹1,23,456.78"))
        assertEquals(1000.0, parseAmount("1e3"))
        assertNull(parseAmount("12abc34"))
    }
}
