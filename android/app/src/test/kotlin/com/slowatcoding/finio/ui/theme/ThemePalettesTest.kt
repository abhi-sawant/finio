package com.slowatcoding.finio.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import kotlin.math.pow
import org.junit.Assert
import org.junit.Test

class ThemePalettesTest {
    private fun channel(v: Float) = if (v <= 0.03928f) v / 12.92f else ((v + 0.055f) / 1.055f).pow(2.4f)
    private fun luminance(c: Color) = 0.2126f * channel(c.red) + 0.7152f * channel(c.green) + 0.0722f * channel(c.blue)
    private fun contrast(a: Color, b: Color): Float {
        val (hi, lo) = luminance(a).let { la -> luminance(b).let { lb -> if (la >= lb) la to lb else lb to la } }
        return (hi + 0.05f) / (lo + 0.05f)
    }

    private fun assertEquals(expected: Any?, actual: Any?, message: String = "") = Assert.assertEquals(message, expected, actual)
    private fun assertTrue(condition: Boolean, message: String = "") = Assert.assertTrue(message, condition)

    private val combos = VisualTheme.entries.flatMap { v -> listOf(false, true).map { v to it } }

    @Test
    fun `ids round-trip and unknown ids fall back to Mudra`() {
        for (t in VisualTheme.entries) assertEquals(t, VisualTheme.fromId(t.id))
        assertEquals(VisualTheme.Mudra, VisualTheme.fromId("neon"))
        assertEquals(VisualTheme.Mudra, VisualTheme.fromId(null))
    }

    @Test
    fun `every theme in both modes carries its own spec`() {
        for ((v, dark) in combos) {
            val p = finioPalette(v, dark, amoled = false)
            assertEquals(v, p.spec.visual)
            assertEquals(dark, p.colors.isDark)
        }
    }

    @Test
    fun `body text is readable on the page and on cards`() {
        for ((v, dark) in combos) {
            val c = finioPalette(v, dark, amoled = false).colors
            assertTrue(contrast(c.foreground, c.background) >= 7f, "$v dark=$dark foreground/background")
            assertTrue(contrast(c.foreground, c.card.compositeOver(c.background)) >= 7f, "$v dark=$dark foreground/card")
            assertTrue(contrast(c.mutedForeground, c.background) >= 4.5f, "$v dark=$dark muted/background")
            assertTrue(contrast(c.mutedForeground, c.card.compositeOver(c.background)) >= 4.5f, "$v dark=$dark muted/card")
        }
    }

    @Test
    fun `primary buttons keep white-text contrast`() {
        // Call sites hardcode white text on gradPrimary, so primary-filled controls must carry it.
        for ((v, dark) in combos) {
            val c = finioPalette(v, dark, amoled = false).colors
            assertTrue(contrast(c.primaryForeground, c.primary) >= 3f, "$v dark=$dark primaryForeground/primary")
        }
    }

    @Test
    fun `amoled forces a black field on any theme`() {
        for (v in VisualTheme.entries) {
            val c = finioPalette(v, dark = true, amoled = true).colors
            assertEquals(Color.Black, c.background, "$v")
            assertTrue(c.isAmoled)
        }
    }
}
