package com.slowatcoding.finio

import androidx.compose.runtime.Composable
import com.slowatcoding.finio.gallery.GalleryScreen

/**
 * Debug builds: the Mudra component gallery, reachable as `Routes.DebugGallery` (long-press the
 * Tools page title). The release source set defines this as null, so the gallery never ships.
 */
internal val debugGallery: (@Composable () -> Unit)? = { GalleryScreen(embedded = true) }
