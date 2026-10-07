package com.slowatcoding.finio.core.crypto

import com.slowatcoding.finio.core.model.AppLockConfig
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.CodingErrorAction
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

// Port of web/src/utils/pinCrypto.ts — PIN hashing for the app lock (a screen gate, not
// encryption: the data behind it is plaintext either way; see the TS file for the full caveat).
//
// Hashes are byte-identical with WebCrypto's PBKDF2-HMAC-SHA256, so a PIN record is portable.
// PBKDF2 is computed here over javax.crypto.Mac("HmacSHA256") rather than through
// SecretKeyFactory("PBKDF2WithHmacSHA256"), for two exactness reasons: the password bytes must be
// WHATWG TextEncoder UTF-8 (a lone surrogate → EF BF BD; the JCE providers encode it as '?' or
// throw, and differ between the JVM and Android), and WebCrypto accepts an empty password while
// SecretKeySpec rejects an empty key (HMAC zero-pads short keys, so 64 zero bytes is identical).
//
// Every function here is plain blocking code. 310k iterations take a few hundred ms — call
// derivePinHash/verifyPin from Dispatchers.Default, never the main thread.

/** Chosen to stay under ~¼ s on a low-end phone. Stored per record so it can be raised later. */
const val PIN_HASH_ITERATIONS = 310_000
const val PIN_SALT_BYTES = 16
const val PIN_HASH_BITS = 256
const val MIN_PIN_LENGTH = 4
const val MAX_PIN_LENGTH = 8

/** PIN lengths offered at setup. */
val PIN_LENGTH_OPTIONS: List<Int> = listOf(4, 6)

private val secureRandom = SecureRandom()

/** Always true on the JVM/Android; kept for parity with the web's secure-context gate. */
fun isPinCryptoSupported(): Boolean = true

/** base64url without padding — `btoa` + `+/` → `-_` + strip `=`. */
fun toBase64Url(bytes: ByteArray): String = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)

/**
 * The TS `fromBase64Url`: map `-_` → `+/`, pad to a multiple of 4 (counting every char, as the
 * TS does), then WHATWG `atob` — forgiving-base64: ASCII whitespace is dropped, a trailing `=`/`==`
 * is stripped only at a length divisible by 4, leftover bits are discarded. Throws
 * [IllegalArgumentException] wherever `atob` throws `InvalidCharacterError`.
 */
fun fromBase64Url(value: String): ByteArray {
    val padded = value.replace('-', '+').replace('_', '/')
    return atob(padded + "=".repeat((4 - (padded.length % 4)) % 4))
}

private fun atob(input: String): ByteArray {
    var data = input.filterNot { it == '\t' || it == '\n' || it == '\u000C' || it == '\r' || it == ' ' }
    if (data.length % 4 == 0) {
        data = when {
            data.endsWith("==") -> data.dropLast(2)
            data.endsWith("=") -> data.dropLast(1)
            else -> data
        }
    }
    require(data.length % 4 != 1) { "Invalid base64 length" }
    val out = java.io.ByteArrayOutputStream(data.length * 3 / 4)
    var buffer = 0
    var bits = 0
    for (c in data) {
        val v = when (c) {
            in 'A'..'Z' -> c - 'A'
            in 'a'..'z' -> c - 'a' + 26
            in '0'..'9' -> c - '0' + 52
            '+' -> 62
            '/' -> 63
            else -> throw IllegalArgumentException("Invalid base64 character")
        }
        buffer = (buffer shl 6) or v
        bits += 6
        if (bits >= 8) {
            bits -= 8
            out.write((buffer shr bits) and 0xFF)
        }
    }
    return out.toByteArray()
}

/**
 * ASCII digits only — rejects full-width digits (`１２３４`) a numeric parse would accept.
 * `pin.length` counts UTF-16 units, as in JS.
 */
fun isValidPin(pin: String): Boolean {
    if (pin.length < MIN_PIN_LENGTH || pin.length > MAX_PIN_LENGTH) return false
    return pin.all { it in '0'..'9' }
}

fun generateSalt(bytes: Int = PIN_SALT_BYTES): String =
    toBase64Url(ByteArray(bytes).also { secureRandom.nextBytes(it) })

/** WHATWG `TextEncoder.encode`: UTF-8, every unpaired surrogate → U+FFFD (EF BF BD). */
internal fun utf8(s: String): ByteArray {
    val encoder = Charsets.UTF_8.newEncoder()
        .onMalformedInput(CodingErrorAction.REPLACE)
        .onUnmappableCharacter(CodingErrorAction.REPLACE)
        .replaceWith(byteArrayOf(0xEF.toByte(), 0xBF.toByte(), 0xBD.toByte()))
    val buf: ByteBuffer = encoder.encode(CharBuffer.wrap(s))
    return ByteArray(buf.remaining()).also { buf.get(it) }
}

/** RFC 8018 PBKDF2 with HMAC-SHA256 — what WebCrypto `deriveBits({name:'PBKDF2'})` computes. */
internal fun pbkdf2Sha256(password: ByteArray, salt: ByteArray, iterations: Int, lengthBytes: Int): ByteArray {
    require(iterations > 0) { "PBKDF2 iterations must be positive" }
    val mac = Mac.getInstance("HmacSHA256")
    // HMAC pads a short key with zeros to the 64-byte block, so an empty password is exactly a
    // 64-zero-byte key — which SecretKeySpec, unlike WebCrypto, will accept.
    mac.init(SecretKeySpec(if (password.isEmpty()) ByteArray(64) else password, "HmacSHA256"))
    val hLen = mac.macLength
    val blocks = (lengthBytes + hLen - 1) / hLen
    val out = ByteArray(lengthBytes)
    for (block in 1..blocks) {
        mac.update(salt)
        mac.update(byteArrayOf((block ushr 24).toByte(), (block ushr 16).toByte(), (block ushr 8).toByte(), block.toByte()))
        var u = mac.doFinal()
        val t = u.copyOf()
        for (i in 2..iterations) {
            u = mac.doFinal(u)
            for (j in t.indices) t[j] = (t[j].toInt() xor u[j].toInt()).toByte()
        }
        val offset = (block - 1) * hLen
        System.arraycopy(t, 0, out, offset, minOf(hLen, lengthBytes - offset))
    }
    return out
}

/** Blocking. Throws on a malformed salt (as the TS rejects). */
fun derivePinHash(pin: String, saltB64: String, iterations: Int = PIN_HASH_ITERATIONS): String =
    toBase64Url(pbkdf2Sha256(utf8(pin), fromBase64Url(saltB64), iterations, PIN_HASH_BITS / 8))

/** Best-effort constant-time comparison of two base64url values; false if either is malformed. */
fun timingSafeEqualB64(a: String, b: String): Boolean {
    val left: ByteArray
    val right: ByteArray
    try {
        left = fromBase64Url(a)
        right = fromBase64Url(b)
    } catch (_: IllegalArgumentException) {
        return false
    }
    if (left.size != right.size) return false
    var diff = 0
    for (i in left.indices) diff = diff or (left[i].toInt() xor right[i].toInt())
    return diff == 0
}

/** The fields of a PIN record verification needs (an [AppLockConfig] supplies them). */
data class PinRecord(val salt: String, val hash: String, val iterations: Int)

fun AppLockConfig.pinRecord(): PinRecord = PinRecord(salt, hash, iterations)

/** Blocking. Never throws — anything unexpected is simply "wrong PIN". */
fun verifyPin(pin: String, record: PinRecord): Boolean = try {
    timingSafeEqualB64(derivePinHash(pin, record.salt, record.iterations), record.hash)
} catch (_: Exception) {
    false
}
