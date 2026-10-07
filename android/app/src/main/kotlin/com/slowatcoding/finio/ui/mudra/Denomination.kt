package com.slowatcoding.finio.ui.mudra

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.slowatcoding.finio.core.model.AccountType
import com.slowatcoding.finio.ui.theme.CssLinearGradient
import com.slowatcoding.finio.ui.theme.FinioTheme
import com.slowatcoding.finio.ui.theme.Stop

/**
 * Every account is printed like a rupee denomination, decided by its *type* — never its
 * user-chosen colour (web/src/components/accounts/note.ts). Tints are the `--note-<value>` 135deg
 * gradients and `--note-<value>-ink` colours of index.css, each with its dark pair.
 */
enum class Denomination(
    val light: Pair<Long, Long>,
    val lightInk: Long,
    val dark: Pair<Long, Long>,
    val darkInk: Long,
    /** `.dark.amoled`: same hue and ink, each gradient sinking to near-black on its far corner. */
    val amoled: Pair<Long, Long>,
) {
    /** ₹10 chocolate — cash. */
    Rs10(0xFFF6E8DC to 0xFFE6C9B0, 0xFF5A3215, 0xFF4A2C18 to 0xFF2F1C0F, 0xFFF6D9C0, 0xFF3B2312 to 0xFF100905),
    /** ₹20 green-yellow — investment. */
    Rs20(0xFFF2F8D8 to 0xFFDCEBAA, 0xFF485A0C, 0xFF3F4C0F to 0xFF29320A, 0xFFE8F5B8, 0xFF323D0B to 0xFF0B0F03),
    /** ₹50 cyan — wallet. */
    Rs50(0xFFE0F6FB to 0xFFB8E8F2, 0xFF0D4F60, 0xFF0F4652 to 0xFF0A2F37, 0xFFC4F1FA, 0xFF0C3A45 to 0xFF031014),
    /** ₹100 lavender — bank account. */
    Rs100(0xFFECE6FF to 0xFFD9CFFC, 0xFF3B2A86, 0xFF3A2F86 to 0xFF251C5C, 0xFFDCD3FF, 0xFF2E2470 to 0xFF0C0A22),
    /** ₹200 yellow — fixed and recurring deposits. */
    Rs200(0xFFFFF4C4 to 0xFFFFE48A, 0xFF6B4D00, 0xFF5E4A0C to 0xFF3C2F07, 0xFFFFE9A3, 0xFF4C3A08 to 0xFF130E02),
    /** ₹500 stone — savings. */
    Rs500(0xFFEEF0E6 to 0xFFD8DCCB, 0xFF3D4630, 0xFF3A4030 to 0xFF262A1F, 0xFFE2E6D2, 0xFF2E3324 to 0xFF0B0D08),
    /** ₹2000 magenta — credit card. */
    Rs2000(0xFFFFE3F1 to 0xFFFBC6E0, 0xFF8A1550, 0xFF6A1846 to 0xFF43102D, 0xFFFFD0E6, 0xFF561239 to 0xFF190610);

    private val lightStyle = NoteStyle(Color(light.first), Color(light.second), Color(lightInk))
    private val darkStyle = NoteStyle(Color(dark.first), Color(dark.second), Color(darkInk))

    private val amoledStyle = NoteStyle(Color(amoled.first), Color(amoled.second), Color(darkInk))

    fun style(dark: Boolean, amoled: Boolean = false): NoteStyle =
        if (dark && amoled) amoledStyle else if (dark) darkStyle else lightStyle
}

/**
 * A denomination's paint: the 135deg [background] gradient ([from] → [to]) and its matching
 * [ink] (the CSS `color`, which the note's thread and rosette also use).
 */
@Immutable
data class NoteStyle(val from: Color, val to: Color, val ink: Color) {
    val background: Brush = CssLinearGradient(135f, listOf(Stop(0f, from), Stop(1f, to)))

    /** CSS `filter: grayscale(1)` — archived accounts' chips. */
    fun grayscale(): NoteStyle = NoteStyle(from.gray(), to.gray(), ink.gray())
}

private fun Color.gray(): Color {
    val l = 0.2126f * red + 0.7152f * green + 0.0722f * blue
    return Color(l, l, l, alpha)
}

val AccountType.denomination: Denomination
    get() = when (this) {
        AccountType.Checking -> Denomination.Rs100
        AccountType.Savings -> Denomination.Rs500
        AccountType.Credit -> Denomination.Rs2000
        AccountType.Fd, AccountType.Rd -> Denomination.Rs200
        AccountType.Cash -> Denomination.Rs10
        AccountType.Wallet -> Denomination.Rs50
        AccountType.Investment -> Denomination.Rs20
    }

/** `noteStyle(type)` — the account type's note tint in the current mode. */
@Composable
@ReadOnlyComposable
fun noteStyle(type: AccountType): NoteStyle = type.denomination.style(FinioTheme.colors.isDark, FinioTheme.colors.isAmoled)

/** Human labels for account types — row captions and note tiles show these, never the raw key. */
val AccountType.label: String
    get() = when (this) {
        AccountType.Checking -> "Bank account"
        AccountType.Savings -> "Savings"
        AccountType.Cash -> "Cash"
        AccountType.Credit -> "Credit card"
        AccountType.Investment -> "Investment"
        AccountType.Wallet -> "Wallet"
        AccountType.Fd -> "Fixed deposit"
        AccountType.Rd -> "Recurring deposit"
    }
