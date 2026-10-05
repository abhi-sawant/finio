package com.slowatcoding.finio.core.golden

import com.slowatcoding.finio.core.model.FinioJson
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.fail
import java.io.File
import kotlin.math.abs
import kotlin.math.max

/**
 * Reads spec/fixtures/<module>.json (generated from the TS implementation by
 * `cd web && npm run gen:fixtures`) and compares Kotlin results against it.
 *
 * Equality is JSON-structural with three deliberate looseness rules, all of which describe
 * representation differences rather than behaviour:
 *  - numbers compare with a 1e-9 relative tolerance (15000 vs 15000.0, float noise),
 *  - a missing key equals an explicit null (FinioJson omits nulls; JS keeps `null`),
 *  - nothing else — key sets, array lengths and order must all match.
 */
object Golden {
    data class Case(val fn: String, val name: String?, val args: JsonArray, val out: JsonElement) {
        fun arg(i: Int): JsonElement = args[i]
        override fun toString(): String = name?.let { "$fn[$it]" } ?: "$fn${args.toString().take(160)}"
    }

    private val dir: File = File(
        System.getProperty("finio.fixtures") ?: error("finio.fixtures system property not set"),
    )

    fun cases(module: String): List<Case> {
        val file = File(dir, "$module.json")
        check(file.exists()) { "Missing fixture $file — run `cd web && npm run gen:fixtures`" }
        val root = FinioJson.parseToJsonElement(file.readText()).jsonObject
        return root.getValue("cases").jsonArray.map {
            val o = it.jsonObject
            Case(
                fn = o.getValue("fn").jsonPrimitive.content,
                name = (o["name"] as? JsonPrimitive)?.content,
                args = o["args"]?.jsonArray ?: JsonArray(emptyList()),
                out = o["out"] ?: JsonNull,
            )
        }
    }

    /**
     * Run every case of [module] through [dispatch] (fn name + case → Kotlin result as JSON, or
     * null when the fn isn't ported/handled yet, which fails). Collects all mismatches before
     * failing so one run reports everything.
     */
    fun verify(module: String, dispatch: (Case) -> JsonElement?) {
        val failures = mutableListOf<String>()
        val all = cases(module)
        check(all.isNotEmpty()) { "$module.json has no cases" }
        for (c in all) {
            val actual = try {
                dispatch(c) ?: run { failures += "$c: no Kotlin dispatch for '${c.fn}'"; continue }
            } catch (e: Throwable) {
                failures += "$c threw ${e::class.simpleName}: ${e.message}"; continue
            }
            val diff = diff(c.out, actual, "$")
            if (diff != null) failures += "$c\n    $diff\n    expected=${c.out.toString().take(400)}\n    actual  =${actual.toString().take(400)}"
        }
        if (failures.isNotEmpty()) {
            fail("${failures.size}/${all.size} golden cases failed in $module:\n" + failures.joinToString("\n"))
        }
    }

    /** First structural difference between [expected] and [actual], or null. */
    fun diff(expected: JsonElement, actual: JsonElement, path: String): String? {
        if (expected is JsonNull && actual is JsonNull) return null
        when (expected) {
            is JsonObject -> {
                if (actual !is JsonObject) return "$path: expected object, got $actual"
                val keys = (expected.keys + actual.keys)
                for (k in keys) {
                    val e = expected[k] ?: JsonNull
                    val a = actual[k] ?: JsonNull
                    diff(e, a, "$path.$k")?.let { return it }
                }
                return null
            }
            is JsonArray -> {
                if (actual !is JsonArray) return "$path: expected array, got $actual"
                if (expected.size != actual.size) return "$path: expected ${expected.size} items, got ${actual.size}"
                expected.indices.forEach { i -> diff(expected[i], actual[i], "$path[$i]")?.let { return it } }
                return null
            }
            is JsonPrimitive -> {
                if (actual !is JsonPrimitive) return "$path: expected $expected, got $actual"
                if (expected is JsonNull || actual is JsonNull) return "$path: expected $expected, got $actual"
                if (expected.isString || actual.isString) {
                    return if (expected.isString && actual.isString && expected.content == actual.content) null
                    else "$path: expected $expected, got $actual"
                }
                expected.booleanOrNull?.let { b ->
                    return if (actual.booleanOrNull == b) null else "$path: expected $b, got $actual"
                }
                val e = expected.doubleOrNull; val a = actual.doubleOrNull
                if (e == null || a == null) return "$path: expected $expected, got $actual"
                val tol = 1e-9 * max(1.0, max(abs(e), abs(a)))
                return if (abs(e - a) <= tol) null else "$path: expected $e, got $a"
            }
        }
    }

    inline fun <reified T> decode(e: JsonElement): T = FinioJson.decodeFromJsonElement<T>(e)
    inline fun <reified T> encode(v: T): JsonElement = FinioJson.encodeToJsonElement(v)
}

val JsonElement.d: Double get() = jsonPrimitive.doubleOrNull ?: error("not a number: $this")
val JsonElement.i: Int get() = d.toInt()
val JsonElement.s: String get() = jsonPrimitive.content
val JsonElement.b: Boolean get() = jsonPrimitive.booleanOrNull ?: error("not a boolean: $this")
val JsonElement.sOrNull: String? get() = if (this is JsonNull) null else jsonPrimitive.content
val JsonElement.dOrNull: Double? get() = if (this is JsonNull) null else jsonPrimitive.doubleOrNull
