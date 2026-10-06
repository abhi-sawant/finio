package com.slowatcoding.finio.ui.screens.legal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slowatcoding.finio.ui.common.BackButton
import com.slowatcoding.finio.ui.components.FinioCard
import com.slowatcoding.finio.ui.components.FinioScreen
import com.slowatcoding.finio.ui.components.HeaderIconSpacer
import com.slowatcoding.finio.ui.components.ScreenTitle
import com.slowatcoding.finio.ui.navigation.FinioNavigator
import com.slowatcoding.finio.ui.theme.FinioTheme
import com.slowatcoding.finio.ui.theme.FinioType

const val CONTACT_EMAIL = "contact@finio.slowatcoding.com"

/** `text-sm leading-relaxed` — 14sp at 1.625. */
private val LegalBody = FinioType.body.copy(lineHeight = 22.75.sp)

/**
 * A legal page (PrivacyPolicy.tsx / TermsOfService.tsx): back · title · spacer header, then one
 * glass card (20dp padding, sections 24dp apart) starting with a muted "Last updated" note.
 */
@Composable
fun LegalPage(nav: FinioNavigator, title: String, intro: AnnotatedString, content: @Composable ColumnScope.() -> Unit) {
    FinioScreen(header = {
        BackButton(nav)
        ScreenTitle(title)
        HeaderIconSpacer()
    }) {
        FinioCard(contentPadding = PaddingValues(20.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                Text(intro, style = FinioType.caption, color = FinioTheme.colors.mutedForeground)
                content()
            }
        }
    }
}

/** One numbered section: a `text-base font-semibold` heading over 8dp-spaced body blocks. */
@Composable
fun LegalSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, Modifier.semantics { heading() }, style = FinioType.title, color = FinioTheme.colors.foreground)
        content()
    }
}

@Composable
fun LegalParagraph(text: String) = LegalParagraph(AnnotatedString(text))

@Composable
fun LegalParagraph(text: AnnotatedString) {
    Text(text, style = LegalBody, color = FinioTheme.colors.foreground)
}

/** `list-disc space-y-1 pl-5`. */
@Composable
fun LegalList(vararg items: AnnotatedString) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items.forEach { item ->
            Row {
                Text("•", Modifier.width(20.dp).padding(start = 6.dp), style = LegalBody, color = FinioTheme.colors.foreground)
                Text(item, Modifier.weight(1f), style = LegalBody, color = FinioTheme.colors.foreground)
            }
        }
    }
}

/** Plain-string list items. */
@Composable
fun LegalList(vararg items: String) = LegalList(*items.map { AnnotatedString(it) }.toTypedArray())

/** A lavender in-app link (`text-primary underline-offset-4`). */
fun AnnotatedString.Builder.appendLink(text: String, color: androidx.compose.ui.graphics.Color, bold: Boolean = false, onClick: () -> Unit) {
    withLink(
        LinkAnnotation.Clickable(text, TextLinkStyles(SpanStyle(color = color, fontWeight = if (bold) FontWeight.Medium else null))) { onClick() },
    ) { append(text) }
}

/** The `mailto:` contact link. */
fun AnnotatedString.Builder.appendMailLink(color: androidx.compose.ui.graphics.Color) {
    withLink(
        LinkAnnotation.Url("mailto:$CONTACT_EMAIL", TextLinkStyles(SpanStyle(color = color, fontWeight = FontWeight.Medium))),
    ) { append(CONTACT_EMAIL) }
}

/** `<strong>` inside a list item. */
fun AnnotatedString.Builder.appendStrong(text: String) {
    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(text) }
}
