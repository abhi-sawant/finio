package com.slowatcoding.finio.core.period

import com.slowatcoding.finio.core.js.iso
import com.slowatcoding.finio.core.js.toIso
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.Instant

// Shared by this agent's golden tests: a fixture Date arrives as its ISO string.

val JsonElement.instant: Instant get() = iso(jsonPrimitive.content)
fun Instant?.json(): JsonElement = this?.let { JsonPrimitive(it.toIso()) } ?: JsonNull

fun PeriodRange.json(): JsonElement = buildJsonObject {
    put("type", JsonPrimitive(type.name.lowercase()))
    put("start", start.json())
    put("end", end.json())
}
