package com.gorilla.gallery.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.gorilla.gallery.ui.theme.DesignTokens
import com.gorilla.gallery.ui.theme.GlassDepth
import com.gorilla.gallery.ui.theme.LiquidGlassSurface
import com.gorilla.gallery.ui.theme.LocalDynamicColors
import com.gorilla.gallery.ui.theme.pressScale
import kotlinx.coroutines.delay

@Composable
fun AnimatedGlassDialog(
    onDismissRequest: () -> Unit,
    content: @Composable (scale: Float) -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    val handleDismiss = {
        isVisible = false
    }

    if (showDialog) {
        Dialog(
            onDismissRequest = handleDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
        ) {
            LaunchedEffect(isVisible) {
                if (!isVisible) {
                    delay(200)
                    showDialog = false
                    onDismissRequest()
                }
            }

            val scale by animateFloatAsState(
                targetValue = if (isVisible) 1f else 0.85f,
                animationSpec = spring(dampingRatio = 0.75f, stiffness = 300f),
                label = "dialogScale"
            )

            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(200)),
                exit = fadeOut(animationSpec = tween(200))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = handleDismiss,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    content(scale)
                }
            }
        }
    }
}

@Composable
fun GlassAlertDialog(
    onDismissRequest: () -> Unit,
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    dismissLabel: String? = null,
    onDismiss: (() -> Unit)? = null,
    icon: ImageVector? = null,
) {
    val accent = LocalDynamicColors.current.accent
    val currentBackdrop = com.gorilla.gallery.ui.theme.LocalLiquidGlassContentBackdrop.current ?: com.gorilla.gallery.ui.theme.LocalLiquidGlassBackdrop.current

    AnimatedGlassDialog(onDismissRequest = onDismissRequest) { scale ->
        LiquidGlassSurface(
            depth = GlassDepth.MID,
            shape = RoundedCornerShape(28.dp),
            backdrop = currentBackdrop,
            surfaceColor = DesignTokens.BgSurface.copy(alpha = 0.92f),
            saturationOverride = 1.55f,
            tintAlphaOverride = 0.07f,
            modifier = Modifier
                .width(300.dp)
                .scale(scale)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                )
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = accent
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = DesignTokens.TextPrimary,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = DesignTokens.TextSecondary,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (dismissLabel != null && onDismiss != null) {
                        TextButton(
                            onClick = {
                                onDismiss()
                            },
                            modifier = Modifier.pressScale(scale = 0.94f)
                        ) {
                            Text(
                                text = dismissLabel,
                                style = MaterialTheme.typography.labelLarge,
                                color = DesignTokens.TextSecondary
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    TextButton(
                        onClick = {
                            onConfirm()
                        },
                        modifier = Modifier.pressScale(scale = 0.94f)
                    ) {
                        Text(
                            text = confirmLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = accent
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GlassCustomDialog(
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit
) {
    val currentBackdrop = com.gorilla.gallery.ui.theme.LocalLiquidGlassContentBackdrop.current ?: com.gorilla.gallery.ui.theme.LocalLiquidGlassBackdrop.current

    AnimatedGlassDialog(onDismissRequest = onDismissRequest) { scale ->
        LiquidGlassSurface(
            depth = GlassDepth.MID,
            shape = RoundedCornerShape(28.dp),
            backdrop = currentBackdrop,
            surfaceColor = DesignTokens.BgSurface.copy(alpha = 0.92f),
            saturationOverride = 1.55f,
            tintAlphaOverride = 0.07f,
            modifier = Modifier
                .width(300.dp)
                .scale(scale)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                )
        ) {
            content()
        }
    }
}
