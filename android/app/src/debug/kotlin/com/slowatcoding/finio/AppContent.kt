package com.slowatcoding.finio

import androidx.compose.runtime.Composable
import com.slowatcoding.finio.gallery.GalleryScreen

/** Debug builds open the Mudra component gallery until the real navigation shell lands. */
@Composable
internal fun AppContent() = GalleryScreen()
