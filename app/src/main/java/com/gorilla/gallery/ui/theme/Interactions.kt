package com.gorilla.gallery.ui.theme

import androidx.compose.runtime.setValue

import androidx.compose.runtime.getValue

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp

/**
 * Press-scale micro-animation + haptic feedback for every interactive element.
 * Spring-driven (no linear easing). Combine with a clickable from the call site.
 */
fun Modifier.pressScale(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.95f,
): Modifier = composed {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = SpringSpecs.Press,
        label = "pressScale",
    )
    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

@Composable
fun Modifier.pressScale(
    scale: Float = 0.94f,
    enabled: Boolean = true,
): Modifier {
    var isPressed by remember { mutableStateOf(false) }
    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed) scale else 1f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 400f),
        label = "pressScale",
    )
    return this
        .graphicsLayer {
            scaleX = animatedScale
            scaleY = animatedScale
        }
        .pointerInput(enabled) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                if (enabled) isPressed = true
                waitForUpOrCancellation()
                isPressed = false
            }
        }
}

/**
 * Soft accent "bloom" behind an active element — a 0.3-alpha colored shadow that makes
 * active tabs / rows / controls glow against the dark base. No-op when [active] is false.
 * Colored shadows are honoured on API 28+ (project minSdk 31).
 */
fun Modifier.accentBloom(
    accent: Color,
    active: Boolean,
    shape: Shape = RoundedCornerShape(20.dp),
): Modifier = if (!active) this else composed {
    // Light mode dampens the bloom — a 0.3-alpha colored shadow reads as a smudge on a
    // light surface, so scale it down via the theme's glowScale.
    val glowScale = LocalAppColors.current.glowScale
    val isGrey = accent.red == accent.green && accent.green == accent.blue
    val baseAlpha = if (isGrey) 0.15f else 0.3f
    val alpha = baseAlpha * glowScale
    shadow(
        elevation = 12.dp,
        shape = shape,
        clip = false,
        ambientColor = accent.copy(alpha = alpha),
        spotColor = accent.copy(alpha = alpha),
    )
}

/** Fires a light haptic tick; use inside onClick handlers for tactile feedback. */
@Composable
fun rememberHaptic(): () -> Unit {
    val haptic = LocalHapticFeedback.current
    return remember(haptic) {
        { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
    }
}

/**
 * GPU hardware layer rendering helper. Uses Jetpack Compose graphicsLayer to move the view
 * to the GPU, caching the drawing commands.
 */
fun Modifier.renderToHardwareTextureAndroid(): Modifier = this.graphicsLayer()
