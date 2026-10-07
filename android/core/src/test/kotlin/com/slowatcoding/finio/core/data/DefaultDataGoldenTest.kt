package com.slowatcoding.finio.core.data

import com.slowatcoding.finio.core.golden.Golden
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Test

class DefaultDataGoldenTest {
    @Test
    fun matchesTypeScript() = Golden.verify("defaultData") { c ->
        when (c.fn) {
            "MISC_CATEGORY_ID" -> JsonPrimitive(MISC_CATEGORY_ID)
            "defaultCategories" -> Golden.encode(defaultCategories)
            "NEW_DEFAULT_CATEGORY_IDS" -> Golden.encode(NEW_DEFAULT_CATEGORY_IDS)
            "defaultLabels" -> Golden.encode(defaultLabels)
            "defaultSettings" -> Golden.encode(defaultSettings)
            "COLOR_PALETTE" -> Golden.encode(COLOR_PALETTE)
            else -> null
        }
    }
}
