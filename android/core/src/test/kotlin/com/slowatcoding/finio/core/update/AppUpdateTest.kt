package com.slowatcoding.finio.core.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateTest {
    @Test
    fun parsesVersions() {
        assertEquals(listOf(2, 0, 2), parseVersion("2.0.2"))
        assertEquals(listOf(2, 1, 0), parseVersion("v2.1.0"))
        assertEquals(listOf(2, 1, 0), parseVersion("2.1.0-rc.1"))
        assertNull(parseVersion("2.1"))
        assertNull(parseVersion("latest"))
        assertNull(parseVersion("2.x.0"))
    }

    @Test
    fun comparesNumerically() {
        assertTrue(isNewerVersion("2.0.3", "2.0.2"))
        assertTrue(isNewerVersion("2.1.0", "2.0.9"))
        assertTrue(isNewerVersion("10.0.0", "9.9.9"))
        assertTrue(isNewerVersion("2.0.10", "2.0.9"))
        assertFalse(isNewerVersion("2.0.2", "2.0.2"))
        assertFalse(isNewerVersion("2.0.1", "2.0.2"))
        assertFalse(isNewerVersion("garbage", "2.0.2"))
    }

    @Test
    fun skippedVersionOnlySilencesThatVersion() {
        val r = ReleaseInfo("2.1.0", "notes", "https://example.com")
        assertTrue(shouldPromptUpdate(r, "2.0.2", null))
        assertFalse(shouldPromptUpdate(r, "2.0.2", "2.1.0"))
        assertTrue(shouldPromptUpdate(r, "2.0.2", "2.0.3"))
        assertFalse(shouldPromptUpdate(r, "2.1.0", null))
        assertFalse(shouldPromptUpdate(r, "2.2.0", null))
    }

    @Test
    fun parsesLatestRelease() {
        val json = """{"tag_name":"v2.1.0","body":"### Added\n- Thing\n","html_url":"https://github.com/abhi-sawant/finio/releases/tag/v2.1.0","draft":false,"prerelease":false}"""
        assertEquals(
            ReleaseInfo("2.1.0", "### Added\n- Thing", "https://github.com/abhi-sawant/finio/releases/tag/v2.1.0"),
            parseLatestRelease(json),
        )
    }

    @Test
    fun rejectsUnusableReleases() {
        assertNull(parseLatestRelease("not json"))
        assertNull(parseLatestRelease("[]"))
        assertNull(parseLatestRelease("""{"message":"Not Found"}"""))
        assertNull(parseLatestRelease("""{"tag_name":"v2.1.0","prerelease":true}"""))
        assertNull(parseLatestRelease("""{"tag_name":"v2.1.0","draft":true}"""))
        assertNull(parseLatestRelease("""{"tag_name":"nightly"}"""))
        assertEquals("", parseLatestRelease("""{"tag_name":"2.1.0","body":null}""")?.notes)
    }

    @Test
    fun flattensChangelogMarkdown() {
        val md = "### Added\n\n\n- **Bold** thing with `code`\n- See [docs](https://x.y)\n\nPlain"
        assertEquals(
            listOf("Added", "", "• Bold thing with code", "• See docs", "", "Plain"),
            changelogLines(md),
        )
        assertEquals(emptyList<String>(), changelogLines("  \n"))
    }
}
