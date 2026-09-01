package com.gorilla.gallery.ui.theme

import androidx.compose.ui.graphics.Color

// Core neutral palette (dark-first, glass relies on dark base).
// Deep blue-black base — rich, not a pure-black hole. Kept in lockstep with DesignTokens.
val GorillaBlack = Color(0xFF000000)        // darkest wash (gradient floor)
val GorillaNight = Color(0xFF000000)        // app background (DesignTokens.BgBase)
val GorillaSurface = Color(0xFF1C1C28)      // card surface (DesignTokens.BgSurface)
val GorillaSurfaceHigh = Color(0xFF242433)  // elevated surface

val GorillaWhite = Color(0xFFE8E8F0)        // primary text (near-white, never grey)
val GorillaMuted = Color(0xFF9999B0)        // secondary text (lavender-grey)
val GorillaFaint = Color(0xFF555570)        // disabled / hairline

// Default accent (overridable from Settings)
val AccentViolet = Color(0xFFB79CFF)
val AccentCyan = Color(0xFF7FE7FF)
val AccentMint = Color(0xFF8FF0C4)
val AccentRose = Color(0xFFFF9CC4)
val AccentAmber = Color(0xFFFFD27F)

val DefaultAccent = Color.White

// Light theme neutrals
val LightBackground = Color(0xFFF3F2FA)
val LightSurface = Color(0xFFFFFFFF)
val LightOnSurface = Color(0xFF14131C)
val LightMuted = Color(0xFF5A5870)

// Glass tint bases
val GlassTintDark = Color(0xFFFFFFFF)   // white tint over dark blur
val GlassTintLight = Color(0xFF0B0A12)  // dark tint over light blur
