package com.slowatcoding.finio.platform

import com.slowatcoding.finio.platform.api.ApiException
import com.slowatcoding.finio.platform.api.FinioApi
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class FinioApiTest {
    private fun expectError(status: Int, body: String): ApiException =
        try {
            FinioApi.interpretResponse(status, false, body)
            fail("expected ApiException"); throw IllegalStateException()
        } catch (e: ApiException) { e }

    @Test fun errorBodyMessageWins() {
        val e = expectError(401, """{"error":"Invalid credentials"}""")
        assertEquals("Invalid credentials", e.message)
        assertEquals(401, e.status)
    }

    @Test fun nonStringOrMissingErrorFallsBackToStatus() {
        assertEquals("Request failed (500)", expectError(500, """{"error":{"x":1}}""").message)
        assertEquals("Request failed (404)", expectError(404, """{"message":"nope"}""").message)
        assertEquals("Request failed (502)", expectError(502, "<html>Bad gateway</html>").message)
        assertEquals("Request failed (429)", expectError(429, "").message)
    }

    @Test fun okWithUnparseableBodyIsEmptyObject() {
        assertEquals(JsonObject(emptyMap()), FinioApi.interpretResponse(200, true, ""))
        assertEquals(JsonObject(emptyMap()), FinioApi.interpretResponse(200, true, "not json"))
        assertEquals(JsonPrimitive(true), FinioApi.interpretResponse(200, true, """{"success":true}""")["success"])
    }

    @Test fun encodeUriComponentMatchesJs() {
        assertEquals("2026-10-05", FinioApi.encodeURIComponent("2026-10-05"))
        assertEquals("a%20b%2Fc!~'()*", FinioApi.encodeURIComponent("a b/c!~'()*"))
    }
}
