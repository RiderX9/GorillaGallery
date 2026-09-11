package com.gorilla.gallery.ui.theme

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette
import kotlin.math.max
import kotlin.math.min

/**
 * Builds [DynamicColors] from an album-art bitmap using the Palette API. Runs the
 * (synchronous) generation off the main thread at the call site. The chosen [accent]
 * is preserved; art colors fill the primary/secondary/background slots, with safe
 * fallbacks when the art is too uniform to extract swatches.
 */
fun paletteColorsFrom(bitmap: Bitmap?, accent: Color): DynamicColors {
    if (bitmap == null) {
        return DynamicColors(
            accent = accent,
            artPrimary = DefaultAccent,
            artSecondary = AccentCyan,
            artBackground = GorillaNight,
            adaptiveAccent = accent,
        )
    }
    val palette = Palette.from(bitmap).maximumColorCount(24).clearFilters().generate()

    val vibrant = palette.vibrantSwatch ?: palette.lightVibrantSwatch
    val muted = palette.mutedSwatch ?: palette.darkMutedSwatch
    val dominant = palette.dominantSwatch

    val primary = vibrant?.rgb?.let { Color(it) } ?: accent
    val secondary = muted?.rgb?.let { Color(it) } ?: AccentCyan
    val bgSource = palette.darkMutedSwatch ?: dominant
    val background = bgSource?.rgb?.let { Color(it).darken(0.55f) } ?: GorillaNight

    return DynamicColors(
        accent = accent,
        artPrimary = primary,
        artSecondary = secondary,
        artBackground = background,
        adaptiveAccent = accent,
    )
}

/** Mix toward black by [factor] (0 = unchanged, 1 = black). */
fun Color.darken(factor: Float): Color {
    val f = factor.coerceIn(0f, 1f)
    return Color(
        red = red * (1 - f),
        green = green * (1 - f),
        blue = blue * (1 - f),
        alpha = alpha,
    )
}

/** Mix toward white by [factor]. */
fun Color.lighten(factor: Float): Color {
    val f = factor.coerceIn(0f, 1f)
    return Color(
        red = red + (1 - red) * f,
        green = green + (1 - green) * f,
        blue = blue + (1 - blue) * f,
        alpha = alpha,
    )
}

/** Relative luminance helper used to pick readable on-colors. */
fun Color.isLight(): Boolean {
    val l = 0.299f * red + 0.587f * green + 0.114f * blue
    return l > 0.6f
}

internal fun clamp01(v: Float) = max(0f, min(1f, v))
