package com.slowatcoding.finio.core.update

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Update check against the latest GitHub release. Pure: the network fetch lives in the app
 * module, so everything that decides *whether* to prompt is unit-tested here.
 *
 * Releases follow SemVer and the release notes are the matching CHANGELOG.md section (see
 * "Releasing" in the root CLAUDE.md), so the release body is the changelog shown to the user.
 */
data class ReleaseInfo(
    /** `2.1.0` — the tag without its leading `v`. */
    val version: String,
    /** The release body (CHANGELOG section), Markdown. Empty when the release has none. */
    val notes: String,
    /** The release page, where the APK is attached. */
    val url: String,
)

/** `v2.1.0` / `2.1.0` / `2.1.0-rc.1` → `[2, 1, 0]`; null when it is not `major.minor.patch`. */
fun parseVersion(raw: String): List<Int>? {
    val core = raw.trim().removePrefix("v").removePrefix("V").substringBefore('-').substringBefore('+')
    val parts = core.split('.')
    if (parts.size != 3) return null
    val numbers = parts.map { it.toIntOrNull() ?: return null }
    return if (numbers.any { it < 0 }) null else numbers
}

/** True only when [latest] is strictly newer than [current]; an unparseable version is never newer. */
fun isNewerVersion(latest: String, current: String): Boolean {
    val l = parseVersion(latest) ?: return false
    val c = parseVersion(current) ?: return false
    for (i in 0..2) {
        if (l[i] != c[i]) return l[i] > c[i]
    }
    return false
}

/**
 * Whether to show the update dialog: the release is newer than the running build, and it is not
 * the version the user chose to skip. Skipping only silences *that* version — a newer one prompts again.
 */
fun shouldPromptUpdate(release: ReleaseInfo, currentVersion: String, skippedVersion: String?): Boolean =
    isNewerVersion(release.version, currentVersion) && release.version != skippedVersion

/**
 * Parse the `GET /repos/{owner}/{repo}/releases/latest` body. Null for anything that isn't a
 * usable stable release (malformed JSON, no tag, a draft or prerelease, a non-SemVer tag).
 */
fun parseLatestRelease(json: String): ReleaseInfo? {
    val obj = runCatching { Json.parseToJsonElement(json).jsonObject }.getOrNull() ?: return null
    if (obj.flag("draft") || obj.flag("prerelease")) return null
    val tag = obj.str("tag_name") ?: return null
    if (parseVersion(tag) == null) return null
    return ReleaseInfo(
        version = tag.trim().removePrefix("v").removePrefix("V"),
        notes = obj.str("body")?.trim().orEmpty(),
        url = obj.str("html_url").orEmpty(),
    )
}

private fun JsonObject.str(key: String): String? =
    get(key)?.let { runCatching { it.jsonPrimitive.contentOrNull }.getOrNull() }

private fun JsonObject.flag(key: String): Boolean = str(key) == "true"

/**
 * Release notes (CHANGELOG Markdown) as plain display lines: headings lose their `#`, bullets
 * become `•`, and `**bold**`, `` `code` `` and `[text](url)` markers are stripped. Blank-line
 * runs collapse to one so the dialog stays compact.
 */
fun changelogLines(notes: String): List<String> {
    val out = mutableListOf<String>()
    for (raw in notes.lines()) {
        var line = raw.trimEnd()
        line = line.replace(Regex("""\[([^\]]*)]\([^)]*\)"""), "$1").replace("**", "").replace("`", "")
        line = when {
            line.trimStart().startsWith("#") -> line.trimStart().trimStart('#').trim()
            line.trimStart().startsWith("- ") || line.trimStart().startsWith("* ") -> "• " + line.trimStart().drop(2)
            else -> line
        }
        if (line.isBlank() && (out.isEmpty() || out.last().isBlank())) continue
        out += line
    }
    while (out.isNotEmpty() && out.last().isBlank()) out.removeAt(out.size - 1)
    return out
}
