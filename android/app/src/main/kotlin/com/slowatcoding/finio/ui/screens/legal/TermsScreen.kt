package com.slowatcoding.finio.ui.screens.legal

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.buildAnnotatedString
import com.slowatcoding.finio.ui.navigation.FinioNavigator
import com.slowatcoding.finio.ui.navigation.Routes
import com.slowatcoding.finio.ui.theme.FinioTheme

/** Port of web/src/pages/legal/TermsOfService.tsx — `/terms`. Same text. */
@Composable
fun TermsScreen(nav: FinioNavigator) {
    val primary = FinioTheme.colors.primary
    val openPrivacy = { nav.navigate(Routes.Privacy) }
    LegalPage(
        nav,
        "Terms of Service",
        buildAnnotatedString {
            append("Last updated: 13 August 2026. These terms cover the hosted Finio app and its optional cloud backup service. See the ")
            appendLink("Privacy Policy", primary, onClick = openPrivacy)
            append(" for how your data is handled.")
        },
    ) {
        LegalSection("1. Acceptance") {
            LegalParagraph(
                "By creating a cloud account, or by using Finio at all, you agree to these terms. If you don't agree, " +
                    "you can still use the app fully offline without an account — no terms apply to purely local use.",
            )
        }

        LegalSection("2. The service") {
            LegalParagraph(
                "Finio is a personal finance tracker. Cloud backup is an optional add-on that stores a copy of your " +
                    "data on a server so you can restore it on another device; it is not required for the app to work. " +
                    "Finio does not connect to your bank, does not initiate or process payments, and does not provide " +
                    "investment or financial advice — it is a record-keeping tool, and any totals, forecasts, or " +
                    "insights it shows are informational estimates based only on the data you entered.",
            )
        }

        LegalSection("3. Your account") {
            LegalParagraph("You're responsible for:")
            LegalList(
                "Providing a working email address and keeping your password confidential.",
                "Everything entered under your account, and for setting a passphrase you can remember if you enable " +
                    "encrypted backups — it cannot be recovered if lost.",
                "Notifying us if you believe your account has been compromised.",
            )
        }

        LegalSection("4. Your data") {
            LegalParagraph(
                buildAnnotatedString {
                    append(
                        "You own the data you put into Finio. We don't claim any rights over it, we don't use it for " +
                            "anything other than providing the backup/restore feature you asked for, and you can export " +
                            "or delete it at any time (see the ",
                    )
                    appendLink("Privacy Policy", primary, onClick = openPrivacy)
                    append(" for how).")
                },
            )
        }

        LegalSection("5. Acceptable use") {
            LegalParagraph("Don't use Finio to:")
            LegalList(
                "Attempt to access another user's account or data.",
                "Interfere with, overload, or probe the service's infrastructure.",
                "Upload unlawful content into a backup, or use the account system for anything other than backing up " +
                    "your own finance data.",
            )
        }

        LegalSection("6. Availability and no warranty") {
            LegalParagraph(
                "The hosted service, including cloud backup, is provided free of charge, \"as is\" and \"as " +
                    "available,\" without warranties of any kind. We don't guarantee uninterrupted availability, and " +
                    "while backups are retained on a rolling basis, they are a convenience feature, not a guaranteed " +
                    "archival service — keep your own local export of anything you can't afford to lose.",
            )
        }

        LegalSection("7. Limitation of liability") {
            LegalParagraph(
                "To the fullest extent permitted by law, the service is provided without liability for indirect, " +
                    "incidental, or consequential damages, including loss of data, arising from your use of it. Nothing " +
                    "in these terms limits liability that cannot be excluded under applicable law, including EU " +
                    "consumer-protection law.",
            )
        }

        LegalSection("8. Termination") {
            LegalParagraph(
                "You may stop using the service and delete your account at any time from Settings, which permanently " +
                    "removes your account and all associated backups. We may suspend or terminate accounts used in " +
                    "violation of section 5.",
            )
        }

        LegalSection("9. Self-hosted deployments") {
            LegalParagraph(
                "Finio's source is open, and these terms apply only to the officially hosted instance. If you connect " +
                    "the app to a self-hosted backend, your relationship — and these terms — are with whoever operates " +
                    "that server, not with us.",
            )
        }

        LegalSection("10. Changes") {
            LegalParagraph(
                "We may update these terms from time to time; material changes will be reflected in the \"last " +
                    "updated\" date above.",
            )
        }

        LegalSection("11. Contact") {
            LegalParagraph(
                buildAnnotatedString {
                    append("Questions about these terms: ")
                    appendMailLink(primary)
                    append(".")
                },
            )
        }
    }
}
