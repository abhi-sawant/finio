package com.slowatcoding.finio.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Which visual world the app is printed in. Mudra is the original and the default; the others
 * mirror the web theme stylesheets (web/src/themes/<id>.css). The id is the string stored on disk and
 * is the same one the web keeps under `finio-visual-theme`.
 *
 * A look is a per-device preference, so it lives in its own store (see `VisualThemeStore`), not in
 * `Settings` — settings travel in every backup and would change the backup contract.
 */
enum class VisualTheme(
    val id: String,
    val label: String,
    val tagline: String,
    /** Paper, ink, accent, signal — the picker's preview strips, light then dark. */
    val swatch: List<Color>,
    val swatchDark: List<Color>,
) {
    Mudra(
        "mudra", "Mudra", "Banknote printing. Frosted glass, guilloche engraving, one lavender accent.",
        listOf(Color(0xFFF1EEFB), Color(0xFF1D1747), Color(0xFF4B36C7), Color(0xFF0B7A55)),
        listOf(Color(0xFF161236), Color(0xFFEEEAFF), Color(0xFFB9ADFF), Color(0xFF5FE0AE)),
    ),
    Gulal(
        "gulal", "Gulal", "Holi-powder pop. Sticker-bright, chunky outlines, hard shadows, a colour per category.",
        listOf(Color(0xFFFFE14D), Color(0xFF1B1230), Color(0xFFFF5FA8), Color(0xFF4457FF)),
        listOf(Color(0xFF1C1233), Color(0xFFFFF3D6), Color(0xFFFF5FA8), Color(0xFFFFE14D)),
    ),
    Shunya(
        "shunya", "Shunya", "Zero. No cards, no fills — light numerals, hairlines and a month line that shows pace.",
        listOf(Color(0xFFFCFCFA), Color(0xFF0F0F10), Color(0xFF2740FF), Color(0xFF12805A)),
        listOf(Color(0xFF0A0A0B), Color(0xFFF3F3EF), Color(0xFF8F9DFF), Color(0xFF5FD3A0)),
    ),
    Bagh(
        "bagh", "Bagh", "A kitchen garden. Leaf corners, soft and tactile; goals grow as plants.",
        listOf(Color(0xFFE6EFDC), Color(0xFF16301F), Color(0xFF3A7A36), Color(0xFFF4B21B)),
        listOf(Color(0xFF0E1A12), Color(0xFFE9F3E0), Color(0xFF7FCF7A), Color(0xFFFFC83D)),
    ),

    // Each is one ThemeKit in ui/theme/kits/.
    Naksha(
        "naksha", "Naksha", "The architect’s plan. Prussian-blue cyanotype or tracing paper, 1px linework with crop marks, hatched selections, red-pencil revisions, and every budget dimensioned to scale.",
        listOf(Color(0xFFF6F9FC), Color(0xFF0D2C6B), Color(0xFF1647A6), Color(0xFFC4271B)),
        listOf(Color(0xFF12397A), Color(0xFFF4F8FF), Color(0xFFFFE98A), Color(0xFFFF9684)),
    );

    companion object {
        val Default = Mudra
        fun fromId(id: String?): VisualTheme = entries.firstOrNull { it.id == id } ?: Default
    }
}
