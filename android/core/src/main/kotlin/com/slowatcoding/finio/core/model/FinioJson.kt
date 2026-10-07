package com.slowatcoding.finio.core.model

import kotlinx.serialization.json.Json

/**
 * The one JSON configuration both persistence and backups use. `explicitNulls = false` drops
 * absent optionals (the web deletes those keys), `encodeDefaults = true` keeps required-but-
 * defaulted fields such as `labels: []` in the output, and unknown keys from a newer client are
 * ignored rather than fatal.
 */
val FinioJson: Json = Json {
    encodeDefaults = true
    explicitNulls = false
    ignoreUnknownKeys = true
    coerceInputValues = true
}

/** Pretty-printed with 2-space indent, matching `JSON.stringify(data, null, 2)` on the web. */
val FinioPrettyJson: Json = Json(FinioJson) {
    prettyPrint = true
    prettyPrintIndent = "  "
}
