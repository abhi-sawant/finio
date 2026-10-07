package com.slowatcoding.finio.core.crypto

import com.slowatcoding.finio.core.model.FinioJson
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.doubleOrNull
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

// Port of web/src/utils/backupCrypto.ts — client-side encryption for cloud backups.
//
// PBKDF2-SHA256 (600k) → AES-256-GCM, 12-byte IV, 128-bit tag appended to the ciphertext (the
// WebCrypto layout, which is also javax.crypto's). The plaintext is the UTF-8 of
// `JSON.stringify(data)`, reproduced byte-for-byte by [jsonStringify], so a backup encrypted on
// the web decrypts here and the same (key, iv, payload) yields the identical ciphertext. The salt
// travels inside the envelope so a fresh device can derive the key from the passphrase alone;
// there is deliberately no recovery.
//
// Every function here is plain blocking code (600k iterations ≈ a second): call from
// Dispatchers.Default. The derived key is a plain SecretKeySpec — keep it in memory only.

const val BACKUP_KEY_ITERATIONS = 600_000
private const val GCM_IV_BYTES = 12
private const val GCM_TAG_BITS = 128
private const val BACKUP_SALT_BYTES = 16

/** Encrypted and compared back to confirm a passphrase without needing an actual backup. */
private const val VERIFIER_PLAINTEXT = "finio-backup-verify-v1"

private val secureRandom = SecureRandom()

@Serializable
data class BackupEnvelope(
    val v: Int = 1,
    val enc: Boolean = true,
    val kdf: String = "PBKDF2-SHA256",
    /** base64url, travels with the backup so a new device can derive the same key. */
    val salt: String,
    val iterations: Int,
    /** base64url, 12 random bytes. */
    val iv: String,
    /** base64url (ciphertext ‖ 16-byte tag). */
    val ciphertext: String,
)

/** base64url `iv` + `ciphertext` — the TS `{ iv, ciphertext }` result. */
data class EncryptedJson(val iv: String, val ciphertext: String)

fun isBackupCryptoSupported(): Boolean = true

fun generateBackupSalt(): String = generateSalt(BACKUP_SALT_BYTES)

/** Blocking. The AES-256 key WebCrypto's `deriveKey(PBKDF2 → AES-GCM 256)` produces. */
fun deriveEncryptionKey(passphrase: String, saltB64: String, iterations: Int = BACKUP_KEY_ITERATIONS): SecretKey =
    SecretKeySpec(pbkdf2Sha256(utf8(passphrase), fromBase64Url(saltB64), iterations, 32), "AES")

/** Encrypt `JSON.stringify(data)`. [iv] is random unless given (tests / golden fixtures only). */
fun encryptJson(key: SecretKey, data: JsonElement, iv: ByteArray = randomIv()): EncryptedJson {
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
    val sealed = cipher.doFinal(utf8(jsonStringify(data)))
    return EncryptedJson(toBase64Url(iv), toBase64Url(sealed))
}

private fun randomIv() = ByteArray(GCM_IV_BYTES).also { secureRandom.nextBytes(it) }

/**
 * Throws on a wrong key or tampered ciphertext (GCM's tag is the "incorrect passphrase" signal),
 * on malformed base64, and on a plaintext that isn't JSON — as the TS rejects in each case.
 */
fun decryptJson(key: SecretKey, iv: String, ciphertext: String): JsonElement {
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, fromBase64Url(iv)))
    val plain = cipher.doFinal(fromBase64Url(ciphertext))
    return FinioJson.parseToJsonElement(textDecode(plain))
}

/** WHATWG `TextDecoder().decode`: UTF-8, U+FFFD for malformed input, leading BOM dropped. */
private fun textDecode(bytes: ByteArray): String {
    val decoder = Charsets.UTF_8.newDecoder()
        .onMalformedInput(CodingErrorAction.REPLACE)
        .onUnmappableCharacter(CodingErrorAction.REPLACE)
    val s = decoder.decode(ByteBuffer.wrap(bytes)).toString()
    return if (s.startsWith('﻿')) s.substring(1) else s
}

fun createVerifier(key: SecretKey, iv: ByteArray = randomIv()): EncryptedJson =
    encryptJson(key, JsonPrimitive(VERIFIER_PLAINTEXT), iv)

/** Never throws — a wrong passphrase or a corrupted verifier is simply "not verified". */
fun verifyPassphraseAgainstConfig(key: SecretKey, verifierIv: String, verifierCiphertext: String): Boolean =
    try {
        val plaintext = decryptJson(key, verifierIv, verifierCiphertext)
        plaintext is JsonPrimitive && plaintext.isString && plaintext.content == VERIFIER_PLAINTEXT
    } catch (_: Exception) {
        false
    }

fun packEnvelope(salt: String, iterations: Int, iv: String, ciphertext: String): BackupEnvelope =
    BackupEnvelope(salt = salt, iterations = iterations, iv = iv, ciphertext = ciphertext)

/** The TS type guard: `enc === true && v === 1` and string salt/iv/ciphertext, numeric iterations. */
fun isEncryptedEnvelope(raw: JsonElement?): Boolean {
    if (raw !is JsonObject) return false
    fun str(k: String) = (raw[k] as? JsonPrimitive)?.let { it !is JsonNull && it.isString } == true
    fun num(k: String) = (raw[k] as? JsonPrimitive)?.let { it !is JsonNull && !it.isString && it.doubleOrNull != null } == true
    val enc = raw["enc"] as? JsonPrimitive
    val v = raw["v"] as? JsonPrimitive
    return enc != null && enc !is JsonNull && !enc.isString && enc.content == "true" &&
        v != null && v !is JsonNull && !v.isString && v.doubleOrNull == 1.0 &&
        str("salt") && str("iv") && str("ciphertext") && num("iterations")
}

/** The envelope, or null when [raw] is a legacy plaintext backup (or anything else). */
fun decodeEnvelope(raw: JsonElement?): BackupEnvelope? =
    if (!isEncryptedEnvelope(raw)) null
    else try { FinioJson.decodeFromJsonElement<BackupEnvelope>(raw!!) } catch (_: Exception) { null }
