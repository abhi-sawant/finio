package com.slowatcoding.finio.core.crypto

import com.slowatcoding.finio.core.golden.Golden
import com.slowatcoding.finio.core.golden.i
import com.slowatcoding.finio.core.golden.s
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import javax.crypto.SecretKey

class BackupCryptoGoldenTest {
    private val keys = HashMap<Triple<String, String, Int?>, SecretKey>()

    private fun key(passphrase: String, salt: String, iterations: JsonElement): SecretKey {
        val n = if (iterations is JsonNull) null else iterations.i
        return keys.getOrPut(Triple(passphrase, salt, n)) {
            if (n == null) deriveEncryptionKey(passphrase, salt) else deriveEncryptionKey(passphrase, salt, n)
        }
    }

    private fun EncryptedJson.json() = buildJsonObject { put("iv", iv); put("ciphertext", ciphertext) }

    @Test
    fun matchesTypeScript() = Golden.verify("backupCrypto") { c ->
        when (c.fn) {
            "constants" -> buildJsonObject { put("BACKUP_KEY_ITERATIONS", BACKUP_KEY_ITERATIONS) }
            // (b) byte-identical ciphertext for the same passphrase/salt/iv/payload.
            "encryptJson" -> encryptJson(key(c.arg(0).s, c.arg(1).s, c.arg(2)), c.arg(4), c.arg(3).bytes()).json()
            // (a) the web's envelope decrypts to the same JSON.
            "decryptJson" -> attempt { decryptJson(key(c.arg(0).s, c.arg(1).s, c.arg(2)), c.arg(3).s, c.arg(4).s) }
            "createVerifier" -> createVerifier(key(c.arg(0).s, c.arg(1).s, c.arg(2)), c.arg(3).bytes()).json()
            "verifyPassphraseAgainstConfig" -> c.arg(3).jsonObject.let {
                JsonPrimitive(
                    verifyPassphraseAgainstConfig(key(c.arg(0).s, c.arg(1).s, c.arg(2)), it.getValue("verifierIv").s, it.getValue("verifierCiphertext").s),
                )
            }
            "generateBackupSalt" -> JsonPrimitive(toBase64Url(c.arg(0).bytes()))
            "packEnvelope" -> c.arg(0).jsonObject.let {
                Golden.encode(packEnvelope(it.getValue("salt").s, it.getValue("iterations").i, it.getValue("iv").s, it.getValue("ciphertext").s))
            }
            "decryptEnvelope" -> {
                val envelope = decodeEnvelope(c.arg(1))!!
                decryptJson(deriveEncryptionKey(c.arg(0).s, envelope.salt, envelope.iterations), envelope.iv, envelope.ciphertext)
            }
            "isEncryptedEnvelope" -> JsonPrimitive(isEncryptedEnvelope(c.arg(0)))
            else -> null
        }
    }

    @Test
    fun roundTripsWithARandomIv() {
        val key = deriveEncryptionKey("hunter2 hunter2", generateBackupSalt(), 1000)
        val payload = kotlinx.serialization.json.Json.parseToJsonElement("""{"a":[1,2.5,"ñ 🎉"],"b":null}""")
        val (iv, ciphertext) = encryptJson(key, payload)
        assertEquals(12, fromBase64Url(iv).size)
        assertEquals(payload, decryptJson(key, iv, ciphertext))
    }

    @Test
    fun wrongKeyThrowsAndVerifierRejectsIt() {
        val salt = generateBackupSalt()
        val key = deriveEncryptionKey("right", salt, 1000)
        val wrong = deriveEncryptionKey("wrong", salt, 1000)
        val enc = encryptJson(key, JsonPrimitive(true))
        assertThrows(Exception::class.java) { decryptJson(wrong, enc.iv, enc.ciphertext) }
        val v = createVerifier(key)
        assertTrue(verifyPassphraseAgainstConfig(key, v.iv, v.ciphertext))
        assertFalse(verifyPassphraseAgainstConfig(wrong, v.iv, v.ciphertext))
    }

    @Test
    fun legacyPlaintextIsNotAnEnvelope() {
        assertNull(decodeEnvelope(buildJsonObject { put("accounts", JsonNull) }))
    }

    @Test
    fun jsNumberStringMatchesV8() {
        val cases = mapOf(
            0.0 to "0", -0.0 to "0", 1.0 to "1", 4200.0 to "4200", 0.1 to "0.1", 1e21 to "1e+21",
            1e20 to "100000000000000000000", 1.5e-7 to "1.5e-7", 0.000001 to "0.000001", 1e-7 to "1e-7",
            123.456 to "123.456", -2.25 to "-2.25", 5e-324 to "5e-324", Double.MAX_VALUE to "1.7976931348623157e+308",
            0.30000000000000004 to "0.30000000000000004", 2.0 / 3 to "0.6666666666666666", 1e300 to "1e+300",
        )
        for ((x, s) in cases) assertEquals("$x", s, jsNumberString(x))
    }
}
