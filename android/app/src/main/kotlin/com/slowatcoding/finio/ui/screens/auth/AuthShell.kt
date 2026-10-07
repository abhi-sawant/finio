package com.slowatcoding.finio.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.slowatcoding.finio.core.store.AuthUser as StoreAuthUser
import com.slowatcoding.finio.platform.api.AuthUser as ApiAuthUser
import com.slowatcoding.finio.ui.components.ButtonVariant
import com.slowatcoding.finio.ui.components.FinioButton
import com.slowatcoding.finio.ui.components.FinioIconButton
import com.slowatcoding.finio.ui.components.FinioTextField
import com.slowatcoding.finio.ui.icons.LucideIcons
import com.slowatcoding.finio.ui.navigation.FinioNavigator
import com.slowatcoding.finio.ui.navigation.FinioTab
import com.slowatcoding.finio.ui.theme.FinioShapes
import com.slowatcoding.finio.ui.theme.FinioTheme
import com.slowatcoding.finio.ui.theme.FinioType
import com.slowatcoding.finio.ui.theme.InstrumentSans
import com.slowatcoding.finio.ui.theme.cssShadow
import com.slowatcoding.finio.ui.theme.CssShadow
import com.slowatcoding.finio.ui.theme.mix

/** The API's user (id as Long) as the auth store keeps it. */
fun ApiAuthUser.toStoreUser(): StoreAuthUser = StoreAuthUser(id.toInt(), name, email)

/**
 * Port of web/src/pages/auth/AuthShell.tsx — the shared frame of every cloud-account screen: the
 * Finio wordmark, a heading, the form, an optional footer, and the "Continue without an account"
 * escape (the app works fully signed out, so every screen offers to leave the flow).
 */
@Composable
fun AuthShell(
    nav: FinioNavigator,
    heading: String,
    description: (@Composable () -> Unit)? = null,
    footer: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = FinioTheme.colors
    Box(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding()
            .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .widthIn(max = 384.dp)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 48.dp),
        ) {
            Column(Modifier.fillMaxWidth().padding(bottom = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                // `text-grad-primary text-4xl font-extrabold` — the wordmark, decorative.
                Text(
                    "Finio",
                    style = FinioType.title.copy(fontFamily = InstrumentSans, fontSize = 36.sp, lineHeight = 40.sp, fontWeight = FontWeight.ExtraBold),
                    color = colors.primary,
                )
                Text(
                    heading,
                    Modifier.padding(top = 12.dp).semantics { heading() },
                    style = FinioType.headline.copy(fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold),
                    color = colors.foreground,
                    textAlign = TextAlign.Center,
                )
                if (description != null) {
                    Box(Modifier.padding(top = 8.dp)) { description() }
                }
            }

            content()

            if (footer != null) {
                Box(Modifier.fillMaxWidth().padding(top = 24.dp), contentAlignment = Alignment.Center) { footer() }
            }

            // `variant="ghost" text-muted-foreground`.
            FinioButton(
                onClick = { nav.openTab(FinioTab.Home) },
                variant = ButtonVariant.Ghost,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            ) {
                Text("Continue without an account", color = colors.mutedForeground, maxLines = 1)
            }
        }
    }
}

/** "Don't have an account? Sign up" — muted text with one lavender link, centred. */
@Composable
fun AuthFooterLink(prompt: String, link: String, onClick: () -> Unit) {
    val colors = FinioTheme.colors
    Text(
        buildAnnotatedString {
            append("$prompt ")
            withLink(
                LinkAnnotation.Clickable(
                    link,
                    TextLinkStyles(SpanStyle(color = colors.primary, fontWeight = FontWeight.Medium)),
                ) { onClick() },
            ) { append(link) }
        },
        style = FinioType.body,
        color = colors.mutedForeground,
        textAlign = TextAlign.Center,
    )
}

/** An input with a leading 20dp icon (`pl-11` + absolutely positioned lucide icon). */
@Composable
fun AuthField(
    value: String,
    onValueChange: (String) -> Unit,
    icon: ImageVector,
    placeholder: String?,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: (() -> Unit)? = null,
    isError: Boolean = false,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailing: (@Composable () -> Unit)? = null,
) {
    FinioTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        placeholder = placeholder,
        isError = isError,
        leading = { Icon(icon, null, Modifier.size(20.dp), tint = FinioTheme.colors.mutedForeground) },
        trailing = trailing,
        visualTransformation = visualTransformation,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = imeAction,
            capitalization = capitalization,
            autoCorrectEnabled = keyboardType == KeyboardType.Text,
        ),
        keyboardActions = if (onImeAction != null) KeyboardActions(onAny = { onImeAction() }) else KeyboardActions.Default,
    )
}

/** A password [AuthField] with the lock icon and, when [onToggleShow] is given, the eye toggle. */
@Composable
fun PasswordAuthField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String?,
    show: Boolean,
    onToggleShow: (() -> Unit)?,
    modifier: Modifier = Modifier,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: (() -> Unit)? = null,
    isError: Boolean = false,
) {
    AuthField(
        value = value,
        onValueChange = onValueChange,
        icon = LucideIcons.Lock,
        placeholder = placeholder,
        modifier = modifier,
        keyboardType = KeyboardType.Password,
        imeAction = imeAction,
        onImeAction = onImeAction,
        isError = isError,
        visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
        trailing = onToggleShow?.let { toggle ->
            {
                FinioIconButton(
                    if (show) LucideIcons.EyeOff else LucideIcons.Eye,
                    if (show) "Hide password" else "Show password",
                    onClick = toggle,
                    tint = FinioTheme.colors.mutedForeground,
                )
            }
        },
    )
}

/** The inline "Passwords do not match" line under a confirm field. */
@Composable
fun FieldError(text: String) {
    Text(text, Modifier.padding(top = 4.dp), style = FinioType.caption, color = FinioTheme.colors.destructive)
}

/**
 * The 6-digit OTP row (six `h-14 w-12 text-center text-xl font-bold` inputs). One numeric text
 * field drawn as six boxes, so typing advances, backspace steps back and a paste (or the
 * keyboard's SMS-code suggestion) fills every box at once — what the web wires by hand with refs.
 */
@Composable
fun OtpInput(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier, onDone: () -> Unit = {}) {
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    BasicTextField(
        value = value,
        onValueChange = { raw -> onValueChange(raw.filter { it.isDigit() }.take(OTP_LENGTH)) },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        interactionSource = source,
        cursorBrush = SolidColor(Color.Transparent),
        textStyle = FinioType.input.copy(color = Color.Transparent),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        decorationBox = { inner ->
            Box {
                // The real field stays in the tree (for IME + semantics) but draws nothing.
                Box(Modifier.size(1.dp)) { inner() }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
                    repeat(OTP_LENGTH) { i ->
                        val active = focused && (i == value.length || (i == OTP_LENGTH - 1 && value.length == OTP_LENGTH))
                        OtpBox(value.getOrNull(i)?.toString() ?: "", active)
                    }
                }
            }
        },
    )
}

@Composable
private fun OtpBox(digit: String, active: Boolean) {
    val colors = FinioTheme.colors
    val shape = FinioShapes.sm
    Box(
        Modifier
            .size(width = 48.dp, height = 56.dp)
            .cssShadow(shape, listOf(CssShadow(spread = 3.dp, color = if (active) colors.ring.mix(0.5f) else Color.Transparent)))
            .clip(shape)
            .background(colors.card)
            .border(1.dp, if (active) colors.ring else colors.input, shape),
        contentAlignment = Alignment.Center,
    ) {
        Text(digit, style = FinioType.input.copy(fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold), color = colors.foreground)
    }
}

const val OTP_LENGTH = 6
