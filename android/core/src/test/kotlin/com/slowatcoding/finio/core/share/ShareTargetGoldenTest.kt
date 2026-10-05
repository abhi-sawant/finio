package com.slowatcoding.finio.core.share

import com.slowatcoding.finio.core.golden.Golden
import com.slowatcoding.finio.core.golden.s
import com.slowatcoding.finio.core.golden.sOrNull
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShareTargetGoldenTest {
    @Test
    fun matchesTypeScript() = Golden.verify("shareTarget") { c ->
        when (c.fn) {
            "constants" -> buildJsonObject { put("NOTE_MAX_LENGTH", NOTE_MAX_LENGTH) }
            "extractAmount" -> extractAmount(c.arg(0).s)?.let(::JsonPrimitive) ?: JsonNull
            "inferTransactionType" -> JsonPrimitive(inferTransactionType(c.arg(0).s).wire)
            "parseSharePayload" -> c.arg(0).jsonObject.let { p ->
                val d = parseSharePayload(p["title"]?.sOrNull, p["text"]?.sOrNull, p["url"]?.sOrNull, p["type"]?.sOrNull)
                buildJsonObject { put("type", d.type.wire); put("amount", d.amount); put("note", d.note) }
            }
            else -> null
        }
    }

    @Test
    fun picksTheMoneyOutOfAPaymentSms() {
        assertEquals(99.0, extractAmount("A/c XX1234 debited by Rs 99.00 at SWIGGY"))
        assertNull(extractAmount("Order 4821 confirmed"))
    }

    @Test
    fun hostnameOfWhatwgEdgeCases() {
        assertEquals("127.0.0.1", whatwgHostname("http://0x7f.1/"))
        assertEquals("[2001:db8::1]", whatwgHostname("http://[2001:DB8:0:0:0:0:0:1]/"))
        assertEquals("xn--mnchen-3ya.de", whatwgHostname("https://münchen.de/"))
        assertNull(whatwgHostname("https://"))
        assertEquals("", whatwgHostname("mailto:a@b.com"))
    }
}
