package com.slowatcoding.finio.core.rules

import java.util.BitSet
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

// JavaScript (ECMAScript + Annex B, non-unicode mode) regular expressions on top of
// java.util.regex. A user-typed category rule is a JS regex on the web, and the rule engine's
// contract is "an invalid pattern matches nothing" — so validity has to be decided by *JS* syntax
// rules, not Java's (`a{` and `\Q` are legal JS but not Java; `(?i)abc` and `a++` are the reverse).
// The pattern is parsed with JS grammar and re-emitted as an equivalent Java pattern:
//  - `i` is JS Canonicalize (simple upper-casing, never mapping non-ASCII onto ASCII), expanded
//    into explicit character classes rather than Java's CASE_INSENSITIVE folding,
//  - `\s`/`\S` use the JS whitespace set, `\b`/`\B` the ASCII word set, `.` excludes only
//    \n \r U+2028 U+2029, `$` is end-of-input (Java's `$` also matches before a final newline),
//  - Annex B leniencies: `{`/`}`/`]` literals, identity escapes (`\p` = "p"), legacy octal,
//    `\c` fallbacks, `\8`; plus named groups, duplicate names in alternatives and `(?ims-ims:)`.
//  - JS (non-unicode) matches UTF-16 *code units*, Java matches code points. Every surrogate unit
//    — in the pattern and in the input — is therefore remapped to its own supplementary code
//    point (U+F0000 + unit − 0xD800) so Java sees, e.g., an emoji as two "characters" exactly as
//    JS does; results are mapped back (see [toUnits]/[fromUnits]).
// Known, deliberately unhandled divergences (far outside what a note pattern does): a
// backreference to a group that has not participated matches empty in JS but fails in Java; and
// a lookbehind Java cannot bound fails to compile — such a JS-valid pattern then matches nothing
// (`isValid` still reports JS validity).

/** JS `WhiteSpace` + `LineTerminator` — what `String.prototype.trim` and regex `\s` use. */
fun isJsWhitespace(c: Char): Boolean = when (c) {
    '\t', '\n', '\u000B', '\u000C', '\r', ' ', ' ', ' ', ' ', ' ',
    ' ', ' ', '　', '﻿' -> true
    else -> c in ' '..' '
}

/** `String.prototype.trim` — unlike Kotlin's trim(), strips U+FEFF and keeps U+001C..U+001F. */
fun jsTrim(s: String): String {
    var start = 0
    var end = s.length
    while (start < end && isJsWhitespace(s[start])) start++
    while (end > start && isJsWhitespace(s[end - 1])) end--
    return s.substring(start, end)
}

class JsRegexSyntaxError(message: String) : IllegalArgumentException(message)

/** A compiled JS regex. `pattern` is null when Java cannot express a JS-valid pattern. */
class JsRegex private constructor(val source: String, val flags: String, private val pattern: Pattern?) {

    /** `RegExp.prototype.exec` result: `groups[0]` is the whole match; unset groups are null. */
    class Match(val index: Int, val groups: List<String?>) {
        val value: String get() = groups[0]!!
        operator fun get(i: Int): String? = groups.getOrNull(i)
    }

    /** `re.test(s)` for a non-global regex. */
    fun test(input: String): Boolean = pattern?.matcher(toUnits(input))?.find() ?: false

    /** `re.exec(s)` / `s.match(re)` for a non-global regex. */
    fun exec(input: String): Match? {
        val units = toUnits(input)
        val m = pattern?.matcher(units) ?: return null
        if (!m.find()) return null
        val index = units.codePointCount(0, m.start())
        return Match(index, (0..m.groupCount()).map { g -> m.group(g)?.let(::fromUnits) })
    }

    /** `s.replace(re, literal)` — first match only (or every match when [all], i.e. the `g` flag). */
    fun replace(input: String, replacement: String, all: Boolean = 'g' in flags): String {
        val m = pattern?.matcher(toUnits(input)) ?: return input
        val quoted = java.util.regex.Matcher.quoteReplacement(toUnits(replacement))
        return fromUnits(if (all) m.replaceAll(quoted) else m.replaceFirst(quoted))
    }

    companion object {
        /** `new RegExp(source, flags)`; throws [JsRegexSyntaxError] wherever JS throws SyntaxError. */
        fun compile(source: String, flags: String = ""): JsRegex {
            for (f in flags) {
                if (f !in "gims") throw UnsupportedOperationException("JS regex flag '$f' is not supported")
            }
            if (flags.toSet().size != flags.length) throw JsRegexSyntaxError("Invalid flags")
            val java = Translator(source, 'i' in flags, 'm' in flags, 's' in flags).translate()
            val pattern = try { Pattern.compile(java) } catch (_: PatternSyntaxException) { null }
            return JsRegex(source, flags, pattern)
        }

        fun compileOrNull(source: String, flags: String = ""): JsRegex? =
            try { compile(source, flags) } catch (_: JsRegexSyntaxError) { null }

        fun isValid(source: String, flags: String = ""): Boolean = compileOrNull(source, flags) != null
    }
}

// ── Code units ↔ code points ───────────────────────────────────────────────────────────────────

private const val UNIT_BASE = 0xF0000

/** Each surrogate code unit → its own supplementary code point; everything else unchanged. */
private fun toUnits(s: String): String {
    if (s.none { it.isSurrogate() }) return s
    val sb = StringBuilder(s.length + 8)
    for (c in s) if (c.isSurrogate()) sb.appendCodePoint(UNIT_BASE + (c.code - 0xD800)) else sb.append(c)
    return sb.toString()
}

private fun fromUnits(s: String): String {
    if (s.none { it.isSurrogate() }) return s
    val sb = StringBuilder(s.length)
    var i = 0
    while (i < s.length) {
        val cp = s.codePointAt(i)
        if (cp >= UNIT_BASE && cp < UNIT_BASE + 0x800) sb.append((0xD800 + cp - UNIT_BASE).toChar()) else sb.appendCodePoint(cp)
        i += Character.charCount(cp)
    }
    return sb.toString()
}

private fun hexCp(cp: Int): String = "\\x{" + Integer.toHexString(cp) + "}"

// ── Case folding: JS Canonicalize (ES2024 22.2.2.7.3, non-unicode branch) ───────────────────────

private object JsCase {
    /** canon[c] = Canonicalize(c). */
    val canon: CharArray by lazy {
        CharArray(0x10000) { i ->
            val ch = i.toChar()
            if (ch.isSurrogate()) ch
            else {
                val u = ch.toString().uppercase()
                if (u.length != 1) ch
                else if (i >= 128 && u[0].code < 128) ch
                else u[0]
            }
        }
    }

    /** All chars sharing each canonical value, keyed by the canonical value. */
    val classes: Map<Char, CharArray> by lazy {
        val m = HashMap<Char, StringBuilder>()
        for (i in 0 until 0x10000) m.getOrPut(canon[i]) { StringBuilder() }.append(i.toChar())
        m.filterValues { it.length > 1 }.mapValues { it.value.toString().toCharArray() }
    }

    fun equivalents(c: Char): CharArray? = classes[canon[c.code]]

    /** { ch : Canonicalize(ch) ∈ Canonicalize(set) }. */
    fun close(set: BitSet): BitSet {
        val out = set.clone() as BitSet
        var i = set.nextSetBit(0)
        while (i >= 0 && i < 0x10000) {
            equivalents(i.toChar())?.forEach { out.set(it.code) }
            i = set.nextSetBit(i + 1)
        }
        return out
    }
}

// ── Translator ─────────────────────────────────────────────────────────────────────────────────

private const val WORD = "[A-Za-z0-9_]"
private const val LINE_TERMINATORS = "[\\n\\r\\u2028\\u2029]"

private class Flags(val i: Boolean, val m: Boolean, val s: Boolean)

private class Translator(val src: String, ignoreCase: Boolean, multiline: Boolean, dotAll: Boolean) {
    private val len = src.length
    private var pos = 0
    private val out = StringBuilder()
    private val rootFlags = Flags(ignoreCase, multiline, dotAll)

    private var captureCount = 0
    private val namesByIndex = HashMap<Int, String>()
    private var nextGroup = 0
    private var nextDisjunction = 0
    private val namedGroups = mutableListOf<Triple<String, Int, List<Pair<Int, Int>>>>()

    private fun fail(msg: String): Nothing = throw JsRegexSyntaxError("Invalid regular expression: /$src/: $msg")

    fun translate(): String {
        prescan()
        disjunction(rootFlags, emptyList())
        if (pos < len) {
            // Only an unmatched ')' can stop the top-level disjunction early.
            fail("Unmatched ')'")
        }
        return out.toString()
    }

    /** V8 counts capture groups (and collects names) before parsing, for `\N` and `\k`. */
    private fun prescan() {
        var i = 0
        var inClass = false
        while (i < len) {
            val c = src[i]
            when {
                c == '\\' -> i++
                inClass -> if (c == ']') inClass = false
                c == '[' -> inClass = true
                c == '(' -> {
                    if (i + 1 < len && src[i + 1] == '?') {
                        if (i + 2 < len && src[i + 2] == '<' && i + 3 < len && src[i + 3] != '=' && src[i + 3] != '!') {
                            captureCount++
                            val close = src.indexOf('>', i + 3)
                            if (close > 0) namesByIndex[captureCount] = src.substring(i + 3, close)
                        }
                    } else captureCount++
                }
            }
            i++
        }
    }

    private val hasNamedGroups get() = namesByIndex.isNotEmpty()

    private fun disjunction(f: Flags, path: List<Pair<Int, Int>>) {
        val id = nextDisjunction++
        var alt = 0
        alternative(f, path + (id to alt))
        while (pos < len && src[pos] == '|') {
            pos++
            out.append('|')
            alt++
            alternative(f, path + (id to alt))
        }
    }

    private fun alternative(f: Flags, path: List<Pair<Int, Int>>) {
        while (pos < len && src[pos] != '|' && src[pos] != ')') term(f, path)
    }

    private fun term(f: Flags, path: List<Pair<Int, Int>>) {
        val c = src[pos]
        when (c) {
            '^' -> {
                pos++
                out.append(if (f.m) "(?:^|(?<=$LINE_TERMINATORS))" else "^")
                noQuantifier("Nothing to repeat")
            }
            '$' -> {
                pos++
                out.append(if (f.m) "(?=$LINE_TERMINATORS|\\z)" else "\\z")
                noQuantifier("Nothing to repeat")
            }
            '\\' -> {
                if (pos + 1 < len && (src[pos + 1] == 'b' || src[pos + 1] == 'B')) {
                    val negate = src[pos + 1] == 'B'
                    pos += 2
                    out.append(
                        if (!negate) "(?:(?<=$WORD)(?!$WORD)|(?<!$WORD)(?=$WORD))"
                        else "(?:(?<=$WORD)(?=$WORD)|(?<!$WORD)(?!$WORD))",
                    )
                    noQuantifier("Nothing to repeat")
                } else {
                    atomEscape(f)
                    quantifier()
                }
            }
            '(' -> group(f, path)
            '*', '+', '?' -> fail("Nothing to repeat")
            '{' -> {
                if (braced(pos) != null) fail("Nothing to repeat")
                pos++
                emitChar('{', f)
                quantifier()
            }
            '[' -> {
                emitSet(charClass(f))
                quantifier()
            }
            '.' -> {
                pos++
                val set = BitSet(0x10000)
                set.set(0, 0x10000)
                if (!f.s) for (t in "\n\r  ") set.clear(t.code)
                emitSet(set)
                quantifier()
            }
            else -> {
                pos++
                emitChar(c, f)
                quantifier()
            }
        }
    }

    private fun group(f: Flags, path: List<Pair<Int, Int>>) {
        fun body(flags: Flags) {
            disjunction(flags, path)
            if (pos >= len) fail("Unterminated group")
            pos++ // ')'
        }
        when {
            src.startsWith("(?:", pos) -> {
                pos += 3; out.append("(?:"); body(f); out.append(')'); quantifier()
            }
            src.startsWith("(?=", pos) || src.startsWith("(?!", pos) -> {
                out.append("(?:").append(src, pos, pos + 3); pos += 3; body(f); out.append("))"); quantifier()
            }
            src.startsWith("(?<=", pos) || src.startsWith("(?<!", pos) -> {
                out.append(src, pos, pos + 4); pos += 4; body(f); out.append(')')
                noQuantifier("Invalid quantifier")
            }
            src.startsWith("(?<", pos) -> {
                pos += 3
                val name = groupName() ?: fail("Invalid capture group name")
                val index = ++nextGroup
                for ((other, _, otherPath) in namedGroups) {
                    if (other == name && !canCoexist(path, otherPath)) fail("Duplicate capture group name")
                }
                namedGroups += Triple(name, index, path)
                out.append('('); body(f); out.append(')'); quantifier()
            }
            src.startsWith("(?", pos) -> {
                pos += 2
                val flags = modifiers(f)
                out.append("(?:"); body(flags); out.append(')'); quantifier()
            }
            else -> {
                pos++
                nextGroup++
                out.append('('); body(f); out.append(')'); quantifier()
            }
        }
    }

    /** Two same-named groups are legal only when they sit in different alternatives of one disjunction. */
    private fun canCoexist(a: List<Pair<Int, Int>>, b: List<Pair<Int, Int>>): Boolean {
        for (k in 0 until minOf(a.size, b.size)) {
            if (a[k] == b[k]) continue
            return a[k].first == b[k].first
        }
        return false
    }

    /** `(?ims-ims:` regexp modifiers (ES2025). Called with [pos] just after `(?`. */
    private fun modifiers(f: Flags): Flags {
        val add = StringBuilder()
        val remove = StringBuilder()
        var target = add
        var sawDash = false
        while (true) {
            if (pos >= len) fail("Invalid group")
            val c = src[pos]
            when {
                c == ':' -> { pos++; break }
                c == '-' && !sawDash -> { sawDash = true; target = remove; pos++ }
                c == 'i' || c == 'm' || c == 's' -> {
                    if (c in add || c in remove) fail("Repeated flag in flag group")
                    target.append(c); pos++
                }
                else -> fail("Invalid group")
            }
        }
        if (sawDash && add.isEmpty() && remove.isEmpty()) fail("Invalid flag group")
        if (!sawDash && add.isEmpty()) fail("Invalid group")
        fun pick(flag: Char, current: Boolean) = if (flag in add) true else if (flag in remove) false else current
        return Flags(pick('i', f.i), pick('m', f.m), pick('s', f.s))
    }

    /** Group name after `(?<` or `\k<`, consuming the closing `>`; null when malformed. */
    private fun groupName(): String? {
        val start = pos
        var first = true
        while (pos < len && src[pos] != '>') {
            val cp = src.codePointAt(pos)
            val ok = cp == '$'.code || cp == '_'.code ||
                (if (first) Character.isUnicodeIdentifierStart(cp)
                else Character.isUnicodeIdentifierPart(cp) || cp == 0x200C || cp == 0x200D) &&
                !Character.isIdentifierIgnorable(cp)
            if (!ok) return null
            first = false
            pos += Character.charCount(cp)
        }
        if (pos >= len || pos == start) return null
        val name = src.substring(start, pos)
        pos++ // '>'
        return name
    }

    // ── Quantifiers ──

    /** A braced quantifier `{n}`, `{n,}`, `{n,m}` at [at] → (min, max or -1 for ∞, end). */
    private fun braced(at: Int): Triple<Int, Int, Int>? {
        var i = at + 1
        fun digits(): Int? {
            val s = i
            var v = 0L
            while (i < len && src[i] in '0'..'9') { v = minOf(v * 10 + (src[i] - '0'), Int.MAX_VALUE.toLong()); i++ }
            return if (i == s) null else v.toInt()
        }
        val min = digits() ?: return null
        var max = min
        if (i < len && src[i] == ',') {
            i++
            max = digits() ?: -1
        }
        if (i >= len || src[i] != '}') return null
        return Triple(min, max, i + 1)
    }

    private fun quantifier() {
        if (pos >= len) return
        when (src[pos]) {
            '*', '+', '?' -> { out.append(src[pos]); pos++ }
            '{' -> {
                val (min, max, end) = braced(pos) ?: return
                if (max != -1 && max < min) fail("numbers out of order in {} quantifier")
                out.append('{').append(min)
                if (max != min) { out.append(','); if (max != -1 && max != Int.MAX_VALUE) out.append(max) }
                out.append('}')
                pos = end
            }
            else -> return
        }
        if (pos < len && src[pos] == '?') { out.append('?'); pos++ }
    }

    private fun noQuantifier(msg: String) {
        if (pos >= len) return
        val c = src[pos]
        if (c == '*' || c == '+' || c == '?' || (c == '{' && braced(pos) != null)) fail(msg)
    }

    // ── Escapes ──

    private fun atomEscape(f: Flags) {
        pos++ // '\'
        if (pos >= len) fail("\\ at end of pattern")
        val e = src[pos]
        when {
            e in "dDsSwW" -> { pos++; emitSet(classEscape(e).let { if (f.i) JsCase.close(it) else it }) }
            e == 'k' && hasNamedGroups -> {
                pos++
                if (pos >= len || src[pos] != '<') fail("Invalid named reference")
                pos++
                val name = groupName() ?: fail("Invalid capture group name")
                val indexes = namesByIndex.filterValues { it == name }.keys.sorted()
                if (indexes.isEmpty()) fail("Invalid named capture referenced")
                backref(indexes, f)
            }
            e in '1'..'9' -> {
                val start = pos
                var n = 0L
                while (pos < len && src[pos] in '0'..'9') { n = minOf(n * 10 + (src[pos] - '0'), Int.MAX_VALUE.toLong()); pos++ }
                if (n <= captureCount) backref(listOf(n.toInt()), f)
                else {
                    pos = start
                    if (e == '8' || e == '9') { pos++; emitChar(e, f) } else emitChar(legacyOctal(), f)
                }
            }
            else -> emitChar(characterEscape(inClass = false), f)
        }
    }

    private fun backref(indexes: List<Int>, f: Flags) {
        out.append(if (f.i) "(?iu:" else "(?:")
        out.append(indexes.joinToString("|") { "\\$it" })
        out.append(')')
    }

    /** Character escapes shared by atoms and classes; [pos] at the char after `\`. */
    private fun characterEscape(inClass: Boolean): Char {
        val e = src[pos]
        return when (e) {
            'f' -> { pos++; '\u000C' }
            'n' -> { pos++; '\n' }
            'r' -> { pos++; '\r' }
            't' -> { pos++; '\t' }
            'v' -> { pos++; '\u000B' }
            'c' -> {
                val next = src.getOrNull(pos + 1)
                val ok = next != null && (next in 'a'..'z' || next in 'A'..'Z' ||
                    (inClass && (next in '0'..'9' || next == '_')))
                if (ok) { pos += 2; (next!!.code % 32).toChar() } else '\\' // `\` literal; `c` is read next
            }
            '0' -> {
                val next = src.getOrNull(pos + 1)
                if (next == null || next !in '0'..'9') { pos++; '\u0000' } else legacyOctal()
            }
            in '1'..'7' -> legacyOctal()
            'x' -> hex(2) ?: run { pos++; 'x' }
            'u' -> hex(4) ?: run { pos++; 'u' }
            'k' -> if (hasNamedGroups) fail("Invalid escape") else { pos++; 'k' }
            else -> { pos++; e }
        }
    }

    private fun hex(n: Int): Char? {
        val end = pos + 1 + n
        if (end > len) return null
        val digits = src.substring(pos + 1, end)
        if (!digits.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) return null
        pos += 1 + n
        return digits.toInt(16).toChar()
    }

    private fun legacyOctal(): Char {
        fun octal(i: Int) = i < len && src[i] in '0'..'7'
        val d1 = src[pos] - '0'
        pos++
        var v = d1
        if (octal(pos)) {
            v = v * 8 + (src[pos] - '0'); pos++
            if (d1 <= 3 && octal(pos)) { v = v * 8 + (src[pos] - '0'); pos++ }
        }
        return v.toChar()
    }

    private fun classEscape(e: Char): BitSet {
        val set = BitSet(0x10000)
        when (e.lowercaseChar()) {
            'd' -> set.set('0'.code, '9'.code + 1)
            'w' -> { set.set('a'.code, 'z'.code + 1); set.set('A'.code, 'Z'.code + 1); set.set('0'.code, '9'.code + 1); set.set('_'.code) }
            's' -> for (i in 0 until 0x10000) if (isJsWhitespace(i.toChar())) set.set(i)
        }
        if (e.isUpperCase()) set.flip(0, 0x10000)
        return set
    }

    // ── Character classes ──

    private sealed interface ClassAtom
    private class Single(val c: Char) : ClassAtom
    private class Many(val set: BitSet) : ClassAtom

    private fun classAtom(): ClassAtom {
        val c = src[pos]
        if (c != '\\') { pos++; return Single(c) }
        pos++
        if (pos >= len) fail("\\ at end of pattern")
        val e = src[pos]
        return when {
            e == 'b' -> { pos++; Single('\b') }
            e == '-' -> { pos++; Single('-') }
            e in "dDsSwW" -> { pos++; Many(classEscape(e)) }
            e == '8' || e == '9' -> { pos++; Single(e) }
            else -> Single(characterEscape(inClass = true))
        }
    }

    private fun charClass(f: Flags): BitSet {
        pos++ // '['
        var negate = false
        if (pos < len && src[pos] == '^') { negate = true; pos++ }
        val set = BitSet(0x10000)
        fun add(a: ClassAtom) = when (a) { is Single -> set.set(a.c.code); is Many -> set.or(a.set) }
        while (true) {
            if (pos >= len) fail("Unterminated character class")
            if (src[pos] == ']') { pos++; break }
            val a = classAtom()
            if (pos + 1 < len && src[pos] == '-' && src[pos + 1] != ']') {
                pos++
                val b = classAtom()
                if (a is Single && b is Single) {
                    if (a.c > b.c) fail("Range out of order in character class")
                    set.set(a.c.code, b.c.code + 1)
                } else {
                    add(a); set.set('-'.code); add(b)
                }
            } else add(a)
        }
        val closed = if (f.i) JsCase.close(set) else set
        if (negate) closed.flip(0, 0x10000)
        return closed
    }

    // ── Emission ──

    private fun emitChar(c: Char, f: Flags) {
        if (f.i && !c.isSurrogate()) {
            val eq = JsCase.equivalents(c)
            if (eq != null) {
                val set = BitSet(0x10000)
                eq.forEach { set.set(it.code) }
                emitSet(set)
                return
            }
        }
        emitLiteral(c)
    }

    private fun emitLiteral(c: Char) {
        when {
            c in 'a'..'z' || c in 'A'..'Z' || c in '0'..'9' -> out.append(c)
            c.isSurrogate() -> out.append(hexCp(UNIT_BASE + (c.code - 0xD800)))
            else -> out.append(hexCp(c.code))
        }
    }

    private fun emitSet(set: BitSet) {
        if (set.isEmpty) { out.append("(?:(?!))"); return }
        val sb = StringBuilder("[")
        fun range(from: Int, to: Int) {
            sb.append(hexCp(from))
            if (to > from) sb.append('-').append(hexCp(to))
        }
        var i = set.nextSetBit(0)
        while (i >= 0 && i < 0x10000) {
            val end = minOf(set.nextClearBit(i) - 1, 0xFFFF)
            // Split around the surrogate block, whose units live at UNIT_BASE in the Java pattern.
            var from = i
            while (from <= end) {
                val to = when {
                    from < 0xD800 -> minOf(end, 0xD7FF)
                    from <= 0xDFFF -> minOf(end, 0xDFFF)
                    else -> end
                }
                if (from in 0xD800..0xDFFF) range(UNIT_BASE + from - 0xD800, UNIT_BASE + to - 0xD800) else range(from, to)
                from = to + 1
            }
            i = set.nextSetBit(end + 1)
        }
        sb.append(']')
        out.append(sb)
    }
}
