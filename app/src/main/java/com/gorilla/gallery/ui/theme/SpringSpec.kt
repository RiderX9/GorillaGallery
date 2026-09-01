package com.gorilla.gallery.ui.theme

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset

/**
 * Shared animation specs — iOS-style spring physics. Every member maps onto one of the
 * three canonical springs in [DesignTokens] so the whole app gets a fast initial response,
 * natural deceleration, and a subtle overshoot on larger transitions. No tween / easing.
 */
object SpringSpecs {

    /** Snappy press feedback for buttons / scale. */
    val Press: SpringSpec<Float> = spring(dampingRatio = 0.7f, stiffness = 400f)

    /** Standard UI element settle (alpha, small offsets). */
    val Standard: SpringSpec<Float> = spring(dampingRatio = 0.82f, stiffness = 380f)

    /** Chip / tab / nav selection. */
    val Bouncy: SpringSpec<Float> = spring(dampingRatio = 0.5f, stiffness = 300f)

    /** Sheet / large surface motion. */
    val Sheet: SpringSpec<Float> = spring(dampingRatio = 0.82f, stiffness = 380f)

    /** Gentle continuous values like blur radius. */
    val Smooth: SpringSpec<Float> = spring(dampingRatio = 0.82f, stiffness = 380f)

    /** Slide / placement offsets (action bars, animateItem). */
    val OffsetSpring: FiniteAnimationSpec<IntOffset> =
        spring(dampingRatio = 0.82f, stiffness = 380f, visibilityThreshold = IntOffset.VisibilityThreshold)

    /** Bouncy placement offsets (grid changes). */
    val BouncyOffsetSpring: FiniteAnimationSpec<IntOffset> =
        spring(dampingRatio = 0.5f, stiffness = 300f, visibilityThreshold = IntOffset.VisibilityThreshold)

    /** Dimension animations. */
    val DpSpring: SpringSpec<Dp> =
        spring(dampingRatio = 0.82f, stiffness = 380f, visibilityThreshold = Dp.VisibilityThreshold)

    /** Color crossfades — high damping so palette changes settle without an ugly overshoot. */
    val ColorSpring: SpringSpec<Color> = spring(dampingRatio = 0.9f, stiffness = 200f)
}
