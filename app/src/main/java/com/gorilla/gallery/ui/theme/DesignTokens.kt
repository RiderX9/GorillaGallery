package com.gorilla.gallery.ui.theme

import com.gorilla.gallery.ui.theme.BlurIntensity
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object DesignTokens {
    // Theme-aware surfaces & text — resolve from LocalAppColors so Light mode gets its own
    // palette (LightAppColors) instead of these dark values bleeding through. Screens read
    // these tokens directly, so wiring them here fixes the whole app in one place.
    //
    // Dark mode is unchanged: DarkAppColors holds the exact same hexes these were hardcoded
    // to (BgBase 0xFF111118, BgSurface 0xFF1C1C28, TextPrimary 0xFFE8E8F0, TextSecondary
    // 0xFF9999B0), so the dark theme renders pixel-identically.
    val BgBase: Color @Composable get() = LocalAppColors.current.bgBase
    val BgSurface: Color @Composable get() = LocalAppColors.current.bgSurface
    val TextPrimary: Color @Composable get() = LocalAppColors.current.textPrimary
    val TextSecondary: Color @Composable get() = LocalAppColors.current.textSecondary

    // Glass accents/borders stay static white-alpha (matches GorillaMusic's own tokens and
    // GlassControls already has explicit light branches where it matters).
    val BgGlass = Color.White.copy(alpha = 0.10f)
    val BgGlassStrong = Color.White.copy(alpha = 0.18f)
    val BorderGlass = Color.White.copy(alpha = 0.18f)
    val RadiusCard = 20.dp
    val RadiusSheet = 28.dp
    val RadiusChip = 100.dp
    val BlurLow = 8.dp
    val BlurMid = 16.dp
    val BlurHigh = 28.dp
}

fun blurAlphaForIntensity(intensity: BlurIntensity): Float = when (intensity) {
    BlurIntensity.LOW -> 0.04f
    BlurIntensity.MEDIUM -> 0.07f
    BlurIntensity.HIGH -> 0.13f
}

fun baseAlphaForIntensity(intensity: BlurIntensity): Float = when (intensity) {
    BlurIntensity.LOW -> 0.60f
    BlurIntensity.MEDIUM -> 0.75f
    BlurIntensity.HIGH -> 0.88f
}
