package com.gorilla.gallery.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Colors derived dynamically from the current track's album art (Palette API),
 * plus the user-chosen accent. Propagated app-wide via [LocalDynamicColors] and
 * animated whenever the track changes (see GorillaTheme).
 */
@Immutable
data class DynamicColors(
    val accent: Color = Color.White,
    /** Dominant vibrant color from album art, falls back to accent. */
    val artPrimary: Color = DefaultAccent,
    /** Secondary muted color from album art. */
    val artSecondary: Color = AccentCyan,
    /** Deep background-tinted color for Now Playing wash. */
    val artBackground: Color = GorillaNight,
    /** Adaptive accent color derived from album art. */
    val adaptiveAccent: Color = Color.White,
)

val LocalDynamicColors = compositionLocalOf { DynamicColors() }
