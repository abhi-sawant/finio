package com.slowatcoding.finio.core.crypto

import com.slowatcoding.finio.core.golden.Golden
import com.slowatcoding.finio.core.golden.i
import com.slowatcoding.finio.core.golden.s
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** `{ threw: true }` mirrors the fixture's `attempt()`. */
internal inline fun attempt(block: () -> JsonElement): JsonElement = try {
    block()
} catch (_: Exception) {
    buildJsonObject { put("threw", true) }
}

internal fun bytesJson(b: ByteArray) = buildJsonArray { b.forEach { add(it.toInt() and 0xFF) } }
internal fun JsonElement.bytes(): ByteArray = jsonArray.map { it.i.toByte() }.toByteArray()

class PinCryptoGoldenTest {
    @Test
    fun matchesTypeScript() = Golden.verify("pinCrypto") { c ->
        when (c.fn) {
            "constants" -> buildJsonObject {
                put("PIN_HASH_ITERATIONS", PIN_HASH_ITERATIONS)
                put("PIN_SALT_BYTES", PIN_SALT_BYTES)
                put("PIN_HASH_BITS", PIN_HASH_BITS)
                put("MIN_PIN_LENGTH", MIN_PIN_LENGTH)
                put("MAX_PIN_LENGTH", MAX_PIN_LENGTH)
                putJsonArray("PIN_LENGTH_OPTIONS") { PIN_LENGTH_OPTIONS.forEach { add(it) } }
            }
            "toBase64Url" -> JsonPrimitive(toBase64Url(c.arg(0).bytes()))
            "fromBase64Url" -> attempt { bytesJson(fromBase64Url(c.arg(0).s)) }
            "isValidPin" -> JsonPrimitive(isValidPin(c.arg(0).s))
            "derivePinHash" -> attempt {
                val iterations = c.arg(2).let { if (it is JsonNull) null else it.i }
                JsonPrimitive(
                    if (iterations == null) derivePinHash(c.arg(0).s, c.arg(1).s)
                    else derivePinHash(c.arg(0).s, c.arg(1).s, iterations),
                )
            }
            "timingSafeEqualB64" -> JsonPrimitive(timingSafeEqualB64(c.arg(0).s, c.arg(1).s))
            "verifyPin" -> c.arg(1).jsonObject.let {
                JsonPrimitive(verifyPin(c.arg(0).s, PinRecord(it.getValue("salt").s, it.getValue("hash").s, it.getValue("iterations").i)))
            }
            else -> null
        }
    }

    @Test
    fun matchesTheJceProviderForWellFormedPins() {
        // Our Mac-based PBKDF2 must equal the platform's for ordinary (ASCII, non-empty) input.
        val salt = fromBase64Url("q83vASNFZ4mrze8BI0VniQ")
        val jce = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(PBEKeySpec("1234".toCharArray(), salt, 1000, 256)).encoded
        assertEquals(toBase64Url(jce), derivePinHash("1234", "q83vASNFZ4mrze8BI0VniQ", 1000))
    }

    @Test
    fun saltsAreRandomAndSixteenBytes() {
        assertEquals(16, fromBase64Url(generateSalt()).size)
        assertNotEquals(generateSalt(), generateSalt())
    }

    @Test
    fun base64UrlRoundTripsAndIsUrlSafe() {
        for (n in 0..40) {
            val b = ByteArray(n) { (it * 37 + 251).toByte() }
            assertTrue(b.contentEquals(fromBase64Url(toBase64Url(b))))
        }
        assertFalse(toBase64Url(byteArrayOf(0xfb.toByte(), 0xff.toByte(), 0xbf.toByte(), 0xfe.toByte())).any { it in "+/=" })
    }

    @Test
    fun verifyRoundTripsWithAFreshSalt() {
        val salt = generateSalt()
        val hash = derivePinHash("2580", salt, 1000)
        assertTrue(verifyPin("2580", PinRecord(salt, hash, 1000)))
        assertFalse(verifyPin("0852", PinRecord(salt, hash, 1000)))
    }
}
