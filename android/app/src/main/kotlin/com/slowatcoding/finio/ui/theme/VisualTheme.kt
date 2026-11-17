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
    Khata(
        "khata", "Khata", "The accountant’s ledger. Ruled paper, ink, a red margin, rupees and paise in columns.",
        listOf(Color(0xFFE3ECD9), Color(0xFF14234A), Color(0xFFB8322A), Color(0xFF1B6A42)),
        listOf(Color(0xFF0F1B17), Color(0xFFE4EEDE), Color(0xFFFF7B6C), Color(0xFF6FD79D)),
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

    // Round two — each is one ThemeKit in ui/theme/kits/.
    Parchi(
        "parchi", "Parchi", "The kirana receipt. Torn paper, mono line items, double-height totals, one receipt red.",
        listOf(Color(0xFFF7F4EC), Color(0xFF23211C), Color(0xFFC8242B), Color(0xFF2E6A3D)),
        listOf(Color(0xFF11131A), Color(0xFFDFE6F5), Color(0xFFFF7A6E), Color(0xFF7FCF9A)),
    ),
    Mahal(
        "mahal", "Mahal", "Bombay Art Deco. Peacock teal and gold, stepped frames, a sunburst over your figure.",
        listOf(Color(0xFFF4ECDC), Color(0xFF0F3A3F), Color(0xFF9A7A35), Color(0xFF1B7550)),
        listOf(Color(0xFF0C1F22), Color(0xFFF3EAD6), Color(0xFFD6B46A), Color(0xFF6FD3A3)),
    ),
    Ganak(
        "ganak", "Ganak", "The pocket calculator. Putty plastic, rubber keys and an LCD window.",
        listOf(Color(0xFFE7E1D3), Color(0xFF24211B), Color(0xFFD4541D), Color(0xFFB9C3A3)),
        listOf(Color(0xFF16171A), Color(0xFFEBE7DC), Color(0xFFFF7A3D), Color(0xFF5EF2C4)),
    ),
    Akhbaar(
        "akhbaar", "Akhbaar", "The morning paper. Newsprint, headline figures, halftone charts, one masthead red.",
        listOf(Color(0xFFF2EFE6), Color(0xFF151515), Color(0xFFC4161C), Color(0xFF1E6A3B)),
        listOf(Color(0xFF17181B), Color(0xFFECE8DC), Color(0xFFFF6259), Color(0xFF6FCF8F)),
    ),
    Khel(
        "khel", "Khel", "An 8-bit handheld. Pixel type, health-bar budgets and a coin to add.",
        listOf(Color(0xFFE0EFBE), Color(0xFF0F380F), Color(0xFF9BBC0F), Color(0xFFA01641)),
        listOf(Color(0xFF0D0B1E), Color(0xFFF6EDD3), Color(0xFFFFCF3F), Color(0xFFFF5C72)),
    ),

    // Round three — each is one ThemeKit in ui/theme/kits/.
    Tareekh(
        "tareekh", "Tareekh", "The household wall calendar. White coated paper, black ink, calendar red and postal blue; the month drawn as a real grid with today ringed and every day ahead carrying its allowance.",
        listOf(Color(0xFFFCFDFE), Color(0xFF0C0E12), Color(0xFFD51F2A), Color(0xFF1746B3)),
        listOf(Color(0xFF0A0F1F), Color(0xFFEDEFE8), Color(0xFFFF4A4D), Color(0xFF86ACFF)),
    ),
    Safar(
        "safar", "Safar", "The departures board. Split-flap figures on matte black, signal amber, green and red, coach-blue shell — and a chrome-yellow platform board with heavy black lettering in daylight.",
        listOf(Color(0xFFD9D8D1), Color(0xFF111213), Color(0xFFF6C400), Color(0xFF1C4A8A)),
        listOf(Color(0xFF0C0E11), Color(0xFFF2ECDC), Color(0xFFFFB224), Color(0xFF5BD08A)),
    ),
    Naksha(
        "naksha", "Naksha", "The architect’s plan. Prussian-blue cyanotype or tracing paper, 1px linework with crop marks, hatched selections, red-pencil revisions, and every budget dimensioned to scale.",
        listOf(Color(0xFFF6F9FC), Color(0xFF0D2C6B), Color(0xFF1647A6), Color(0xFFC4271B)),
        listOf(Color(0xFF12397A), Color(0xFFF4F8FF), Color(0xFFFFE98A), Color(0xFFFF9684)),
    ),
    Ajrakh(
        "ajrakh", "Ajrakh", "Block-printed cloth. Cotton panels on a madder ground by day, an indigo vat by night, printed borders, and budgets stamped out block by block.",
        listOf(Color(0xFF9A2B1F), Color(0xFF1B2A5C), Color(0xFFE2A419), Color(0xFF2C6A2B)),
        listOf(Color(0xFF121A4E), Color(0xFFF1E9D6), Color(0xFFE5553E), Color(0xFF93D47C)),
    ),
    Kolam(
        "kolam", "Kolam", "The threshold drawing. Swept stone and red oxide, one monoline weight, dot-row dividers, and the month drawn as a kolam whose line loops every day gone.",
        listOf(Color(0xFFE3DDD1), Color(0xFF2A1B15), Color(0xFF8F2D1C), Color(0xFF1C6436)),
        listOf(Color(0xFF651E14), Color(0xFFF8F0E4), Color(0xFFF4C24F), Color(0xFFAEE8C4)),
    ),
    Maidan(
        "maidan", "Maidan", "The cricket ground. Outfield green, a painted-tin scoreboard of black number plates, a worm chart of spend against par, and a red-ball add button.",
        listOf(Color(0xFF2A7436), Color(0xFF141A16), Color(0xFFB81D24), Color(0xFF1C7230)),
        listOf(Color(0xFF0D2217), Color(0xFFFFF1CF), Color(0xFFFF5AA8), Color(0xFF5FE07F)),
    ),
    Sector(
        "sector", "Sector", "Concrete and enamel. Flush colour blocks on a jointed grid, one heavy grotesque at poster scale, and accounts drawn as a façade whose bays are sized by their balances.",
        listOf(Color(0xFFC8C4BA), Color(0xFF141413), Color(0xFFF5C400), Color(0xFF1846B0)),
        listOf(Color(0xFF161617), Color(0xFFF1EEE7), Color(0xFFEAB700), Color(0xFF2450C0)),
    ),
    Daak(
        "daak", "Daak", "India Post stationery. Inland-letter blue with pillar-box red, accounts as perforated stamps, a struck date postmark on the hero and a carmine rubber stamp on a budget near its limit.",
        listOf(Color(0xFFC5DCEF), Color(0xFF13213D), Color(0xFFC4201B), Color(0xFF3D5412)),
        listOf(Color(0xFF0D1730), Color(0xFFD3E4F5), Color(0xFFF0473C), Color(0xFFA9C46A)),
    ),
    Sargam(
        "sargam", "Sargam", "The audio cassette. A five-stripe run that turns the corner of every label, Michroma faceplate lettering, piano-key controls, equaliser-strip budgets and a tape window whose reels show the month.",
        listOf(Color(0xFFECEBE6), Color(0xFF3A2315), Color(0xFFF26B1D), Color(0xFF188078)),
        listOf(Color(0xFF1A1310), Color(0xFFF4EADC), Color(0xFFFF8A3A), Color(0xFF31B7A8)),
    ),
    Takhti(
        "takhti", "Takhti", "Blackboard and chalk (whiteboard and marker by day): the hero writes out the real sum, days are tallied in gates of five, and budgets are hand-hatched bars.",
        listOf(Color(0xFFF4F7F9), Color(0xFF15171C), Color(0xFF1650CF), Color(0xFFCF222E)),
        listOf(Color(0xFF1D2B25), Color(0xFFF2EFE6), Color(0xFFF7D75E), Color(0xFFFF93A3)),
    );

    companion object {
        val Default = Mudra
        fun fromId(id: String?): VisualTheme = entries.firstOrNull { it.id == id } ?: Default
    }
}
