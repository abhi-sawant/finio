package com.slowatcoding.finio.platform.api

import com.slowatcoding.finio.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/*
 * Port of web/src/services/api.ts — the optional cloud-backup API. Same 14 endpoints, same
 * request bodies, same error semantics:
 *
 *  - a non-2xx response throws [ApiException] whose message is the body's `error` string when
 *    there is one, else `Request failed (<status>)`;
 *  - a 2xx response with an unparseable body resolves to `{}` (the web's `data = {}`);
 *  - a network failure surfaces as the underlying [IOException] (the web's fetch TypeError).
 *
 * Everything runs on Dispatchers.IO.
 */

class ApiException(message: String, val status: Int) : Exception(message)

@Serializable
data class AuthUser(val id: Long, val name: String, val email: String)

@Serializable
data class LoginResult(val token: String, val user: AuthUser)

@Serializable
data class BackupListEntry(
    @SerialName("backup_date") val backupDate: String,
    @SerialName("file_size") val fileSize: Long,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
data class BackupListResponse(val backups: List<BackupListEntry>)

@Serializable
data class ProfileResponse(val user: AuthUser)

/** `PUT /user/me` body — null fields are omitted, like the web's optional keys. */
@Serializable
data class UpdateProfileRequest(
    val name: String? = null,
    @SerialName("current_password") val currentPassword: String? = null,
    @SerialName("new_password") val newPassword: String? = null,
)

@Serializable
private data class RegisterBody(val name: String, val email: String, val password: String)

@Serializable
private data class EmailOtpBody(val email: String, val otp: String)

@Serializable
private data class EmailBody(val email: String)

@Serializable
private data class LoginBody(val email: String, val password: String)

@Serializable
private data class ResetPasswordBody(val email: String, val otp: String, val password: String)

@Serializable
private data class PasswordBody(val password: String)

class FinioApi(
    baseUrl: String = BuildConfig.API_URL,
    private val client: OkHttpClient = defaultClient(),
) {
    private val baseUrl = baseUrl.trimEnd('/')

    // ---- auth --------------------------------------------------------------------------------

    suspend fun register(name: String, email: String, password: String): JsonObject =
        post("/auth/register", encode(RegisterBody.serializer(), RegisterBody(name, email, password)))

    suspend fun verifyOtp(email: String, otp: String): LoginResult =
        decode(LoginResult.serializer(), post("/auth/verify-otp", encode(EmailOtpBody.serializer(), EmailOtpBody(email, otp))))

    suspend fun resendOtp(email: String): JsonObject =
        post("/auth/resend-otp", encode(EmailBody.serializer(), EmailBody(email)))

    suspend fun login(email: String, password: String): LoginResult =
        decode(LoginResult.serializer(), post("/auth/login", encode(LoginBody.serializer(), LoginBody(email, password))))

    suspend fun forgotPassword(email: String): JsonObject =
        post("/auth/forgot-password", encode(EmailBody.serializer(), EmailBody(email)))

    suspend fun resetPassword(email: String, otp: String, password: String): JsonObject =
        post(
            "/auth/reset-password",
            encode(ResetPasswordBody.serializer(), ResetPasswordBody(email, otp, password)),
        )

    // ---- backups -----------------------------------------------------------------------------

    /** Upload an already-serialized backup body (finance payload or encrypted envelope). */
    suspend fun uploadBackup(token: String, json: String): JsonObject =
        apiFetch("/backup/upload", "POST", token, json)

    suspend fun uploadBackup(token: String, data: JsonElement): JsonObject =
        uploadBackup(token, ApiJson.encodeToString(JsonElement.serializer(), data))

    suspend fun getLatestBackup(token: String): JsonObject = apiFetch("/backup/latest", "GET", token)

    suspend fun listBackups(token: String): BackupListResponse =
        decode(BackupListResponse.serializer(), apiFetch("/backup/list", "GET", token))

    suspend fun getBackup(token: String, date: String): JsonObject =
        apiFetch("/backup/${encodeURIComponent(date)}", "GET", token)

    suspend fun deleteBackup(token: String, date: String): JsonObject =
        apiFetch("/backup/${encodeURIComponent(date)}", "DELETE", token)

    // ---- user --------------------------------------------------------------------------------

    suspend fun getProfile(token: String): ProfileResponse =
        decode(ProfileResponse.serializer(), apiFetch("/user/me", "GET", token))

    /** Returns a fresh `{ token, user }` — a password change bumps `token_version`. */
    suspend fun updateProfile(token: String, data: UpdateProfileRequest): LoginResult =
        decode(
            LoginResult.serializer(),
            apiFetch("/user/me", "PUT", token, encode(UpdateProfileRequest.serializer(), data)),
        )

    suspend fun deleteAccount(token: String, password: String): JsonObject =
        apiFetch("/user/me", "DELETE", token, encode(PasswordBody.serializer(), PasswordBody(password)))

    // ---- plumbing ----------------------------------------------------------------------------

    private suspend fun post(path: String, body: String): JsonObject = apiFetch(path, "POST", null, body)

    private suspend fun apiFetch(
        path: String,
        method: String,
        token: String? = null,
        body: String? = null,
    ): JsonObject = withContext(Dispatchers.IO) {
        val builder = Request.Builder()
            .url("$baseUrl$path")
            .header("Content-Type", "application/json")
        if (token != null && token.isNotEmpty()) builder.header("Authorization", "Bearer $token")
        val requestBody = body?.toRequestBody(JSON_MEDIA)
        builder.method(method, requestBody)

        client.newCall(builder.build()).execute().use { res ->
            val text = res.body.string()
            interpretResponse(res.code, res.isSuccessful, text)
        }
    }

    private fun <T> encode(serializer: KSerializer<T>, value: T): String =
        ApiJson.encodeToString(serializer, value)

    /** A 2xx whose shape doesn't match is a server bug, not a user error — still an ApiException. */
    private fun <T> decode(serializer: KSerializer<T>, data: JsonObject): T =
        try {
            ApiJson.decodeFromJsonElement(serializer, data)
        } catch (e: IllegalArgumentException) {
            throw ApiException("Unexpected response from server", 200)
        }

    companion object {
        private val JSON_MEDIA = "application/json".toMediaType()

        val ApiJson: Json = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            isLenient = true
            coerceInputValues = true
        }

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        /**
         * The web's `apiFetch` result handling, pure so it is unit-testable: parse failure on a
         * non-OK response → `Request failed (status)`; parse failure on OK → `{}`; non-OK →
         * body `error` if it is a string. A non-object JSON body on success is also `{}`
         * (every endpoint returns an object).
         */
        fun interpretResponse(status: Int, ok: Boolean, text: String): JsonObject {
            val parsed: JsonElement? = try {
                if (text.isBlank()) null else ApiJson.parseToJsonElement(text)
            } catch (_: Exception) {
                null
            }
            if (parsed == null) {
                if (!ok) throw ApiException("Request failed ($status)", status)
                return JsonObject(emptyMap())
            }
            if (!ok) {
                val error = (parsed as? JsonObject)?.get("error") as? JsonPrimitive
                val message = if (error != null && error.isString) error.content else "Request failed ($status)"
                throw ApiException(message, status)
            }
            return parsed as? JsonObject ?: JsonObject(emptyMap())
        }

        /** JS `encodeURIComponent` — URLEncoder form-encodes spaces as `+` and escapes `~`. */
        fun encodeURIComponent(value: String): String =
            URLEncoder.encode(value, Charsets.UTF_8.name())
                .replace("+", "%20")
                .replace("%21", "!")
                .replace("%27", "'")
                .replace("%28", "(")
                .replace("%29", ")")
                .replace("%7E", "~")
    }
}
