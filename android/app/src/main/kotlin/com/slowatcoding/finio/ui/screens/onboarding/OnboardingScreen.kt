package com.slowatcoding.finio.ui.screens.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.slowatcoding.finio.core.data.COLOR_PALETTE
import com.slowatcoding.finio.core.data.loadSampleData
import com.slowatcoding.finio.core.js.nowInstant
import com.slowatcoding.finio.core.js.toIso
import com.slowatcoding.finio.core.model.AccountType
import com.slowatcoding.finio.core.store.NewAccount
import com.slowatcoding.finio.core.util.MAX_NAME_LENGTH
import com.slowatcoding.finio.core.util.cleanText
import com.slowatcoding.finio.core.util.stripLeading
import com.slowatcoding.finio.ui.common.financeStore
import com.slowatcoding.finio.ui.components.ButtonIcon
import com.slowatcoding.finio.ui.components.ButtonSize
import com.slowatcoding.finio.ui.components.ButtonVariant
import com.slowatcoding.finio.ui.components.FinioButton
import com.slowatcoding.finio.ui.components.FinioTextField
import com.slowatcoding.finio.ui.components.NumberPad
import com.slowatcoding.finio.ui.icons.LucideIcons
import com.slowatcoding.finio.ui.theme.FinioShapes
import com.slowatcoding.finio.ui.theme.FinioTheme
import com.slowatcoding.finio.ui.theme.FinioType
import com.slowatcoding.finio.ui.theme.cssShadow
import kotlin.math.abs

private data class AccountTypeOption(val value: AccountType, val label: String, val icon: String, val vector: ImageVector)

private val ACCOUNT_TYPES = listOf(
    AccountTypeOption(AccountType.Checking, "Checking", "landmark", LucideIcons.Landmark),
    AccountTypeOption(AccountType.Savings, "Savings", "piggy-bank", LucideIcons.PiggyBank),
    AccountTypeOption(AccountType.Cash, "Cash", "banknote", LucideIcons.Banknote),
    AccountTypeOption(AccountType.Credit, "Credit Card", "credit-card", LucideIcons.CreditCard),
    AccountTypeOption(AccountType.Investment, "Investment", "trending-up", LucideIcons.TrendingUp),
    AccountTypeOption(AccountType.Wallet, "Wallet", "wallet", LucideIcons.Wallet),
)

private enum class Step { Name, Account, Balance }

/**
 * Port of web/src/components/onboarding/Onboarding.tsx — the first-run wizard: name → first
 * account → opening balance, rendered by the shell instead of the app while
 * `settings.onboardedAt` is unset. Every step past the name is skippable (a reinstall can go
 * straight to Settings and restore), and the account step offers the deterministic sample data.
 * Finishing just stamps `onboardedAt`; the gate lifts by itself — nothing navigates.
 */
@Composable
fun OnboardingScreen() {
    val store = financeStore()
    val colors = FinioTheme.colors

    var step by rememberSaveable { mutableStateOf(Step.Name) }
    var name by rememberSaveable { mutableStateOf("") }
    var accountName by rememberSaveable { mutableStateOf("") }
    var accountType by rememberSaveable { mutableStateOf(AccountType.Savings) }
    var balance by rememberSaveable { mutableStateOf("") }
    // The account's tint comes from its type; the stored colour is just a default.
    val color = COLOR_PALETTE[0]

    val trimmedName = cleanText(name, MAX_NAME_LENGTH)
    val trimmedAccountName = cleanText(accountName, MAX_NAME_LENGTH)

    BackHandler(enabled = step != Step.Name) {
        step = if (step == Step.Balance) Step.Account else Step.Name
    }

    fun completeOnboarding() {
        store.updateSettings { it.copy(userName = trimmedName.ifEmpty { "there" }, onboardedAt = nowInstant().toIso()) }
    }

    fun finish(withAccount: Boolean) {
        if (withAccount && trimmedAccountName.isNotEmpty()) {
            val opening = balance.toDoubleOrNull()?.takeIf { it.isFinite() } ?: 0.0
            store.addAccount(
                NewAccount(
                    name = trimmedAccountName,
                    type = accountType,
                    color = color,
                    icon = ACCOUNT_TYPES.find { it.value == accountType }?.icon ?: "landmark",
                    // A credit card's "balance" is money owed, so it starts negative.
                    balance = if (accountType == AccountType.Credit) -abs(opening) else opening,
                ),
            )
        }
        completeOnboarding()
    }

    // A few months of realistic data, so a first look isn't an empty dashboard.
    fun loadSample() {
        loadSampleData(store.sampleDataActions())
        completeOnboarding()
    }

    Box(
        Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).widthIn(max = 384.dp).fillMaxWidth().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            // Progress
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Step.entries.forEach { s ->
                    Box(
                        Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(FinioShapes.full)
                            .then(
                                if (s.ordinal <= step.ordinal) Modifier.background(FinioTheme.brushes.gradPrimary)
                                else Modifier.background(colors.muted),
                            ),
                    )
                }
            }

            when (step) {
                Step.Name -> StepColumn {
                    Heading("Welcome to Finio", "Everything stays on this device. Let’s start with your name.")
                    Column {
                        FieldCaption("What should we call you?")
                        val focus = remember { FocusRequester() }
                        LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
                        FinioTextField(
                            value = name,
                            onValueChange = { name = stripLeading(it).take(MAX_NAME_LENGTH) },
                            modifier = Modifier.focusRequester(focus),
                            placeholder = "Your name",
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { if (trimmedName.isNotEmpty()) step = Step.Account }),
                        )
                    }
                    FinioButton(
                        "Continue",
                        onClick = { step = Step.Account },
                        modifier = Modifier.fillMaxWidth(),
                        size = ButtonSize.Lg,
                        enabled = trimmedName.isNotEmpty(),
                        trailingIcon = LucideIcons.ArrowRight,
                    )
                }

                Step.Account -> StepColumn {
                    Heading("Add your first account", "A bank account, a card, or just the cash in your wallet.")
                    Column {
                        FieldCaption("Account name")
                        val focus = remember { FocusRequester() }
                        LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
                        FinioTextField(
                            value = accountName,
                            onValueChange = { accountName = stripLeading(it).take(MAX_NAME_LENGTH) },
                            modifier = Modifier.focusRequester(focus),
                            placeholder = "e.g. HDFC Savings",
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                        )
                    }
                    Column {
                        FieldCaption("Type")
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ACCOUNT_TYPES.chunked(3).forEach { row ->
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    row.forEach { option ->
                                        TypeTile(
                                            option = option,
                                            selected = accountType == option.value,
                                            onClick = { accountType = option.value },
                                            modifier = Modifier.weight(1f),
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        FinioButton(
                            "Continue",
                            onClick = { step = Step.Balance },
                            modifier = Modifier.fillMaxWidth(),
                            size = ButtonSize.Lg,
                            enabled = trimmedAccountName.isNotEmpty(),
                            trailingIcon = LucideIcons.ArrowRight,
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            FinioButton("Back", onClick = { step = Step.Name }, variant = ButtonVariant.Ghost, size = ButtonSize.Sm, leadingIcon = LucideIcons.ArrowLeft)
                            FinioButton("Skip for now", onClick = { finish(withAccount = false) }, variant = ButtonVariant.Ghost, size = ButtonSize.Sm)
                        }
                        FinioButton(
                            onClick = ::loadSample,
                            modifier = Modifier.fillMaxWidth(),
                            variant = ButtonVariant.Ghost,
                            size = ButtonSize.Sm,
                        ) {
                            ButtonIcon(LucideIcons.Sparkles, tint = colors.primary)
                            Text("Explore with sample data instead", color = colors.primary, maxLines = 1)
                        }
                    }
                }

                Step.Balance -> StepColumn {
                    val credit = accountType == AccountType.Credit
                    Heading(
                        if (credit) "How much do you owe?" else "What’s in it right now?",
                        if (credit) "The current outstanding balance on $trimmedAccountName. You can change it later."
                        else "The opening balance for $trimmedAccountName. You can change it later.",
                    )
                    NumberPad(value = balance, onValueChange = { balance = it })
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        FinioButton("Start tracking", onClick = { finish(withAccount = true) }, modifier = Modifier.fillMaxWidth(), size = ButtonSize.Lg)
                        FinioButton("Back", onClick = { step = Step.Account }, variant = ButtonVariant.Ghost, size = ButtonSize.Sm, leadingIcon = LucideIcons.ArrowLeft)
                    }
                }
            }
        }
    }
}

@Composable
private fun StepColumn(content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp)) { content() }
}

@Composable
private fun Heading(title: String, description: String) {
    val colors = FinioTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, Modifier.semantics { heading() }, style = FinioType.pageTitle, color = colors.foreground)
        Text(description, style = FinioType.body, color = colors.mutedForeground)
    }
}

/** `<Label className="mb-1.5 block text-xs font-medium">` — foreground, unlike the muted FieldLabel. */
@Composable
private fun FieldCaption(text: String) {
    Text(text, Modifier.padding(bottom = 6.dp), style = FinioType.label, color = FinioTheme.colors.foreground)
}

@Composable
private fun TypeTile(option: AccountTypeOption, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val colors = FinioTheme.colors
    val shape = FinioShapes.sm
    val content = if (selected) Color.White else colors.mutedForeground
    Column(
        modifier
            .then(if (selected) Modifier.cssShadow(shape, FinioTheme.shadows.sm) else Modifier)
            .clip(shape)
            .then(if (selected) Modifier.background(FinioTheme.brushes.gradPrimary) else Modifier.background(colors.card))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .semantics { this.selected = selected }
            .padding(horizontal = 8.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(option.vector, null, Modifier.size(16.dp), tint = content)
        Text(option.label, style = FinioType.label, color = content, maxLines = 1)
    }
}
