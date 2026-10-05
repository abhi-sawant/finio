package com.slowatcoding.finio.core.util

import com.slowatcoding.finio.core.golden.Golden
import com.slowatcoding.finio.core.golden.i
import com.slowatcoding.finio.core.golden.s
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import org.junit.Test

class ValidationGoldenTest {
    @Test
    fun matchesTypeScript() = Golden.verify("validation") { c ->
        when (c.fn) {
            "constants" -> buildJsonObject {
                put("MAX_NAME_LENGTH", JsonPrimitive(MAX_NAME_LENGTH))
                put("MAX_NOTE_LENGTH", JsonPrimitive(MAX_NOTE_LENGTH))
                put("MAX_PATTERN_LENGTH", JsonPrimitive(MAX_PATTERN_LENGTH))
            }
            "cleanText" -> JsonPrimitive(cleanText(c.arg(0).s, c.arg(1).i))
            "stripLeading" -> JsonPrimitive(stripLeading(c.arg(0).s))
            "isValidEmail" -> JsonPrimitive(isValidEmail(c.arg(0).s))
            "isPastDay" -> JsonPrimitive(isPastDay(c.arg(0).s, c.arg(1).s))
            "isRangeInverted" -> JsonPrimitive(isRangeInverted(c.arg(0).s, c.arg(1).s))
            "firstFreeScope" -> firstFreeScope(c.arg(0).jsonArray.map { it.s }, c.arg(1).jsonArray.map { it.s }.toSet())
                ?.let(::JsonPrimitive) ?: JsonNull
            else -> null
        }
    }
}
