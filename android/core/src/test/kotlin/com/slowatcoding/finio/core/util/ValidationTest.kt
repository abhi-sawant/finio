package com.slowatcoding.finio.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidationTest {
    @Test fun jsWhitespaceIsNotKotlinWhitespace() {
        // Kotlin trims U+001C–U+001F and keeps U+FEFF; JS does the opposite.
        assertEquals("\u001cx\u001f", jsTrim("\u001cx\u001f"))
        assertEquals("x", jsTrim("﻿x﻿"))
        assertEquals("x", jsTrim("  x　"))
        assertTrue(' '.isWhitespace() && isJsWhitespace(' '))
        assertFalse(isJsWhitespace('​'))
    }

    @Test fun cleanTextCapsByCodePoint() {
        assertEquals("😀😀", cleanText("  😀😀😀 ", 2))
        assertEquals("ab", cleanText("abc", -1))
        assertEquals("", cleanText("abc", 0))
    }

    @Test fun emailCountsUtf16Units() {
        // JS (non-unicode regex) sees 😀 as two units, so it satisfies `{2,}`.
        assertTrue(isValidEmail("a@b.😀"))
        assertFalse(isValidEmail("a@b.c"))
        assertTrue(isValidEmail("  first.last@sub.example.com "))
    }
}
