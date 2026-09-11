package com.gorilla.gallery.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** User-selectable global blur intensity tier (Settings → Appearance). */
enum class BlurIntensity { LOW, MEDIUM, HIGH }

/** Depth tier for an individual glass surface. */
enum class GlassDepth { LOW, MID, HIGH }

/**
 * All glass appearance constants. A single instance is provided through
 * [LocalGlassTokens]; changing the blur intensity in Settings rebuilds it,
 * so every LiquidGlassSurface updates at runtime.
 */
@Immutable
data class GlassTokens(
    val intensity: BlurIntensity = BlurIntensity.MEDIUM,
    val isDark: Boolean = true,
) {
    /** Real RenderEffect blur radius (px-independent dp) per depth tier. */
    fun blurRadius(depth: GlassDepth): Dp {
        val base = when (intensity) {
            BlurIntensity.LOW -> 0.6f
            BlurIntensity.MEDIUM -> 1.0f
            BlurIntensity.HIGH -> 1.5f
        }
        val tier = when (depth) {
            GlassDepth.LOW -> 12.dp
            GlassDepth.MID -> 24.dp
            GlassDepth.HIGH -> 40.dp
        }
        return tier * base
    }

    /** Tint alpha laid over the blurred content for legibility. */
    fun tintAlpha(depth: GlassDepth): Float {
        val d = when (depth) {
            GlassDepth.LOW -> 0.10f
            GlassDepth.MID -> 0.15f
            GlassDepth.HIGH -> 0.20f
        }
        // Light theme needs a touch more tint to stay readable.
        return if (isDark) d else d + 0.04f
    }

    /** Alpha of the soft top highlight rim. */
    fun rimAlpha(depth: GlassDepth): Float = when (depth) {
        GlassDepth.LOW -> 0.20f
        GlassDepth.MID -> 0.28f
        GlassDepth.HIGH -> 0.36f
    }

    /** Border stroke alpha — white 20% on dark gives the premium glass edge. */
    val borderAlpha: Float get() = if (isDark) 0.20f else 0.24f

    /** Depth shadow elevation per tier. */
    fun elevation(depth: GlassDepth): Dp = when (depth) {
        GlassDepth.LOW -> 6.dp
        GlassDepth.MID -> 14.dp
        GlassDepth.HIGH -> 26.dp
    }
}

val LocalGlassTokens = staticCompositionLocalOf { GlassTokens() }
