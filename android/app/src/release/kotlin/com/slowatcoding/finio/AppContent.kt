package com.slowatcoding.finio

import androidx.compose.runtime.Composable
import com.slowatcoding.finio.ui.mudra.PaperBackground
import com.slowatcoding.finio.ui.theme.FinioTheme

/** Release builds show the bare note paper until the real navigation shell lands. */
@Composable
internal fun AppContent() = FinioTheme { PaperBackground() }
