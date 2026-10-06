package com.slowatcoding.finio.ui.screens.legal

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import com.slowatcoding.finio.ui.navigation.FinioNavigator
import com.slowatcoding.finio.ui.theme.FinioTheme

/**
 * Port of web/src/pages/legal/PrivacyPolicy.tsx — `/privacy`. Same text; "your browser's local
 * storage" / "clearing site data" read as this device's app storage on Android.
 */
@Composable
fun PrivacyScreen(nav: FinioNavigator) {
    val primary = FinioTheme.colors.primary
    LegalPage(
        nav,
        "Privacy Policy",
        AnnotatedString(
            "Last updated: 13 August 2026. This policy covers the hosted Finio app and the optional cloud backup " +
                "service. If you are using a self-hosted deployment, the person or organization operating that server " +
                "— not us — controls your data; ask them for their policy.",
        ),
    ) {
        LegalSection("1. Local-first by default") {
            LegalParagraph(
                "Finio is built to work entirely on your device. Accounts, transactions, budgets, categories, goals, " +
                    "debts, and every other record you create are stored in the app's private storage on this device. " +
                    "None of this data is transmitted anywhere unless you explicitly turn on cloud backup or cloud sync. " +
                    "Uninstalling the app or clearing its storage deletes it, since we never receive a copy in the first place.",
            )
        }

        LegalSection("2. What we collect if you create a cloud account") {
            LegalParagraph("Cloud backup is optional. If you register for one, we collect:")
            LegalList(
                "Your name and email address, to create and identify your account.",
                "A one-time password (OTP) sent to your email to verify it and to support password resets.",
                "Your password, which we never store in plain text — only a salted bcrypt hash.",
                "Your backup data. If you enable end-to-end encryption for backups, we only ever receive an opaque " +
                    "encrypted envelope and cannot read its contents. If you leave encryption off, the backup is your " +
                    "finance data in plain JSON, and our server operators can technically access it — treat that " +
                    "setting as a real trust decision.",
            )
        }

        LegalSection("3. What we don't do") {
            LegalList(
                "No cookies and no ad or analytics trackers of any kind.",
                "No selling, renting, or sharing your data with advertisers or data brokers.",
                "No behavioral profiling.",
            )
            LegalParagraph(
                "The only outside party involved in operating the hosted service is the email provider used to deliver " +
                    "OTP and password-reset messages, which sees your email address and the fact that you requested a " +
                    "code — nothing else.",
            )
        }

        LegalSection("4. Why we process this data") {
            LegalParagraph(
                "We process account and backup data to provide the service you asked for: creating your account, " +
                    "authenticating you, and storing and restoring your backups. We don't rely on this data for " +
                    "marketing or send you anything beyond transactional emails (OTPs, password resets, and " +
                    "account-related notices).",
            )
        }

        LegalSection("5. How long we keep it") {
            LegalParagraph(
                "Your account record is kept for as long as your account exists. Backup files are retained on a " +
                    "rolling basis (30 days by default on the hosted instance; a self-hoster can configure a different " +
                    "window) and older backups are deleted automatically. OTP and password-reset codes expire within " +
                    "minutes and are not reused.",
            )
            LegalParagraph("Deleting your account immediately and permanently deletes every backup tied to it.")
        }

        LegalSection("6. Your rights") {
            LegalParagraph(
                "If you are in the EU/EEA or UK, GDPR gives you rights over your data, all of which this app supports " +
                    "in practice, not just on paper:",
            )
            LegalList(
                buildAnnotatedString {
                    appendStrong("Access & portability")
                    append(
                        " — view your profile from Settings, and export your full data as JSON at any time (from the " +
                            "device, or from a downloaded cloud backup).",
                    )
                },
                buildAnnotatedString {
                    appendStrong("Erasure")
                    append(
                        " — delete your cloud account from Settings; this immediately deletes your account record and " +
                            "every backup on the server. Deleting the app or its data removes everything stored locally.",
                    )
                },
                buildAnnotatedString {
                    appendStrong("Rectification")
                    append(" — update your name or password from Settings.")
                },
                buildAnnotatedString {
                    appendStrong("Objection / restriction")
                    append(" — stop using cloud backup at any time; the app keeps working fully offline.")
                },
                AnnotatedString("You also have the right to lodge a complaint with your local data protection supervisory authority."),
            )
        }

        LegalSection("7. Security") {
            LegalParagraph(
                "Passwords are hashed with bcrypt, authentication uses short-lived signed tokens (JWT), and cloud " +
                    "backups can be end-to-end encrypted with a passphrase only you know — we never see it and cannot " +
                    "recover it if you lose it. No screen lock or encryption feature makes the app immune to a " +
                    "compromised device; it reduces risk, it doesn't eliminate it.",
            )
        }

        LegalSection("8. Children") {
            LegalParagraph(
                "Finio is not directed at children, and we do not knowingly collect data from anyone under 16.",
            )
        }

        LegalSection("9. Changes to this policy") {
            LegalParagraph(
                "If this policy changes materially, we'll update the \"last updated\" date above and, where required, " +
                    "notify account holders by email.",
            )
        }

        LegalSection("10. Contact") {
            LegalParagraph(
                buildAnnotatedString {
                    append("Questions about this policy, or to exercise a right not covered above, contact: ")
                    appendMailLink(primary)
                    append(".")
                },
            )
        }
    }
}
