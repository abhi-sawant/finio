package com.slowatcoding.finio.core.share

import java.io.ByteArrayOutputStream
import java.math.BigInteger
import java.net.IDN

// `new URL(input).hostname` (WHATWG URL Standard, no base URL), for shareTarget's hostnameOf.
// Only the parts of the basic URL parser that decide the hostname are implemented: scheme,
// special vs non-special authority (with `\` as a separator for special schemes), userinfo,
// port validation, and host parsing — percent-decoding, IPv4 (decimal/octal/hex, short forms),
// IPv6 compression, opaque hosts and file hosts. Returns null wherever `new URL` throws.
//
// Divergence: non-ASCII domains go through java.net.IDN (IDNA2003) instead of UTS #46, so the
// four "deviation" characters (ß ς ZWJ ZWNJ — WHATWG keeps ß as xn--zca, IDNA2003 maps it to
// "ss") and code points newer than the JDK's Unicode tables can come out differently.

private val SPECIAL = setOf("http", "https", "ws", "wss", "ftp", "file")

fun whatwgHostname(raw: String): String? {
    var input = raw.trim { it.code <= 0x20 }
    input = input.filterNot { it == '\t' || it == '\n' || it == '\r' }

    // Scheme.
    if (input.isEmpty() || !input[0].isAsciiLetter()) return null
    var i = 1
    while (i < input.length && (input[i].isAsciiLetter() || input[i] in '0'..'9' || input[i] in "+-.")) i++
    if (i >= input.length || input[i] != ':') return null
    val scheme = input.substring(0, i).lowercase()
    var rest = input.substring(i + 1)

    if (scheme == "file") {
        if (rest.length < 2 || rest[0] !in "/\\" || rest[1] !in "/\\") return ""
        rest = rest.substring(2)
        val end = rest.indexOfFirst { it in "/\\?#" }.let { if (it < 0) rest.length else it }
        val buffer = rest.substring(0, end)
        if (buffer.length == 2 && buffer[0].isAsciiLetter() && buffer[1] in ":|") return "" // drive letter
        if (buffer.isEmpty()) return ""
        val host = parseHost(buffer, special = true) ?: return null
        return if (host == "localhost") "" else host
    }

    val special = scheme in SPECIAL
    if (special) {
        rest = rest.trimStart('/', '\\')
    } else {
        if (!rest.startsWith("//")) return ""
        rest = rest.substring(2)
    }
    val delimiters = if (special) "/\\?#" else "/?#"
    val end = rest.indexOfFirst { it in delimiters }.let { if (it < 0) rest.length else it }
    var authority = rest.substring(0, end)
    val at = authority.lastIndexOf('@')
    if (at >= 0) {
        authority = authority.substring(at + 1)
        if (authority.isEmpty()) return null
    }

    // host[:port], where a ':' inside [...] belongs to an IPv6 literal.
    var inBrackets = false
    var colon = -1
    for ((k, c) in authority.withIndex()) {
        if (c == '[') inBrackets = true
        if (c == ']') inBrackets = false
        if (c == ':' && !inBrackets) { colon = k; break }
    }
    val hostText = if (colon >= 0) authority.substring(0, colon) else authority
    if (colon >= 0) {
        val port = authority.substring(colon + 1)
        if (port.any { it !in '0'..'9' }) return null
        if (port.isNotEmpty() && BigInteger(port) > BigInteger.valueOf(65535)) return null
    }
    if (hostText.isEmpty()) return if (special) null else ""
    return parseHost(hostText, special)
}

private fun Char.isAsciiLetter() = this in 'a'..'z' || this in 'A'..'Z'

private const val FORBIDDEN_HOST = "\u0000\t\n\r #/:<>?@[\\]^|"

private fun parseHost(input: String, special: Boolean): String? {
    if (input.startsWith("[")) {
        if (!input.endsWith("]")) return null
        return parseIpv6(input.substring(1, input.length - 1))?.let { "[${serializeIpv6(it)}]" }
    }
    if (!special) {
        if (input.any { it in FORBIDDEN_HOST }) return null
        return percentEncodeC0(input)
    }
    val domain = String(percentDecode(input), Charsets.UTF_8)
    val ascii = domainToAscii(domain) ?: return null
    if (ascii.any { it in FORBIDDEN_HOST || it.code <= 0x1F || it == '%' || it.code == 0x7F }) return null
    if (endsInANumber(ascii)) return parseIpv4(ascii)
    return ascii
}

private fun percentDecode(s: String): ByteArray {
    val bytes = s.toByteArray(Charsets.UTF_8)
    val out = ByteArrayOutputStream(bytes.size)
    var i = 0
    while (i < bytes.size) {
        val b = bytes[i]
        if (b == '%'.code.toByte() && i + 2 < bytes.size && hexVal(bytes[i + 1]) >= 0 && hexVal(bytes[i + 2]) >= 0) {
            out.write(hexVal(bytes[i + 1]) * 16 + hexVal(bytes[i + 2])); i += 3
        } else {
            out.write(b.toInt()); i++
        }
    }
    return out.toByteArray()
}

private fun hexVal(b: Byte): Int = when (val c = b.toInt().toChar()) {
    in '0'..'9' -> c - '0'
    in 'a'..'f' -> c - 'a' + 10
    in 'A'..'F' -> c - 'A' + 10
    else -> -1
}

private fun percentEncodeC0(s: String): String {
    val sb = StringBuilder()
    for (b in s.toByteArray(Charsets.UTF_8)) {
        val v = b.toInt() and 0xFF
        if (v <= 0x1F || v > 0x7E) sb.append('%').append(String.format(java.util.Locale.ROOT, "%02X", v)) else sb.append(v.toChar())
    }
    return sb.toString()
}

private fun domainToAscii(domain: String): String? {
    if (domain.all { it.code < 0x80 }) return domain.lowercase()
    return try {
        IDN.toASCII(domain, IDN.ALLOW_UNASSIGNED).lowercase()
    } catch (_: IllegalArgumentException) {
        null
    }
}

private fun endsInANumber(host: String): Boolean {
    val parts = host.split('.').toMutableList()
    if (parts.last().isEmpty()) {
        if (parts.size == 1) return false
        parts.removeAt(parts.size - 1)
    }
    val last = parts.last()
    if (last.isNotEmpty() && last.all { it in '0'..'9' }) return true
    return parseIpv4Number(last) != null
}

private fun parseIpv4Number(input: String): BigInteger? {
    if (input.isEmpty()) return null
    var s = input
    var radix = 10
    if (s.length >= 2 && (s.startsWith("0x") || s.startsWith("0X"))) { s = s.substring(2); radix = 16 }
    else if (s.length >= 2 && s.startsWith("0")) { s = s.substring(1); radix = 8 }
    if (s.isEmpty()) return BigInteger.ZERO
    val ok = s.all { Character.digit(it, radix) >= 0 && it.code < 0x80 }
    return if (ok) BigInteger(s, radix) else null
}

private fun parseIpv4(input: String): String? {
    val parts = input.split('.').toMutableList()
    if (parts.last().isEmpty() && parts.size > 1) parts.removeAt(parts.size - 1)
    if (parts.size > 4) return null
    val numbers = parts.map { parseIpv4Number(it) ?: return null }
    val b256 = BigInteger.valueOf(256)
    for (k in 0 until numbers.size - 1) if (numbers[k] > BigInteger.valueOf(255)) return null
    if (numbers.last() >= b256.pow(5 - numbers.size)) return null
    var ipv4 = numbers.last()
    for (k in 0 until numbers.size - 1) ipv4 += numbers[k] * b256.pow(3 - k)
    val v = ipv4.toLong()
    return "${(v shr 24) and 255}.${(v shr 16) and 255}.${(v shr 8) and 255}.${v and 255}"
}

/** WHATWG IPv6 parser → 8 pieces, or null on failure. */
private fun parseIpv6(input: String): IntArray? {
    val address = IntArray(8)
    var pieceIndex = 0
    var compress = -1
    var p = 0
    val c = { at: Int -> if (at < input.length) input[at] else '￿' }
    fun hex(ch: Char) = if (ch.code < 0x80) Character.digit(ch, 16) else -1
    if (c(p) == ':') {
        if (c(p + 1) != ':') return null
        p += 2
        pieceIndex++
        compress = pieceIndex
    }
    while (p < input.length) {
        if (pieceIndex == 8) return null
        if (c(p) == ':') {
            if (compress != -1) return null
            p++
            pieceIndex++
            compress = pieceIndex
            continue
        }
        var value = 0
        var length = 0
        while (length < 4 && hex(c(p)) >= 0) { value = value * 16 + hex(c(p)); p++; length++ }
        if (c(p) == '.') {
            if (length == 0) return null
            p -= length
            if (pieceIndex > 6) return null
            var numbersSeen = 0
            while (p < input.length) {
                var ipv4Piece = -1
                if (numbersSeen > 0) {
                    if (c(p) == '.' && numbersSeen < 4) p++ else return null
                }
                if (c(p) !in '0'..'9') return null
                while (c(p) in '0'..'9') {
                    val number = c(p) - '0'
                    ipv4Piece = when (ipv4Piece) {
                        -1 -> number
                        0 -> return null
                        else -> ipv4Piece * 10 + number
                    }
                    if (ipv4Piece > 255) return null
                    p++
                }
                address[pieceIndex] = address[pieceIndex] * 0x100 + ipv4Piece
                numbersSeen++
                if (numbersSeen == 2 || numbersSeen == 4) pieceIndex++
            }
            if (numbersSeen != 4) return null
            break
        } else if (c(p) == ':') {
            p++
            if (p >= input.length) return null
        } else if (p < input.length) return null
        address[pieceIndex] = value
        pieceIndex++
    }
    if (compress != -1) {
        var swaps = pieceIndex - compress
        pieceIndex = 7
        while (pieceIndex != 0 && swaps > 0) {
            val t = address[pieceIndex]
            address[pieceIndex] = address[compress + swaps - 1]
            address[compress + swaps - 1] = t
            pieceIndex--
            swaps--
        }
    } else if (pieceIndex != 8) return null
    return address
}

private fun serializeIpv6(address: IntArray): String {
    // Longest run (≥ 2) of zero pieces, first one on a tie.
    var bestStart = -1
    var bestLen = 1
    var k = 0
    while (k < 8) {
        if (address[k] == 0) {
            var j = k
            while (j < 8 && address[j] == 0) j++
            if (j - k > bestLen) { bestStart = k; bestLen = j - k }
            k = j
        } else k++
    }
    val sb = StringBuilder()
    var ignore0 = false
    for (idx in 0 until 8) {
        if (ignore0 && address[idx] == 0) continue
        ignore0 = false
        if (bestStart == idx) {
            sb.append(if (idx == 0) "::" else ":")
            ignore0 = true
            continue
        }
        sb.append(Integer.toHexString(address[idx]))
        if (idx != 7) sb.append(':')
    }
    return sb.toString()
}
